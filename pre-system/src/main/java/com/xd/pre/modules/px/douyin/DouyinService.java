package com.xd.pre.modules.px.douyin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.R;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.data.tenant.PreTenantContextHolder;
import com.xd.pre.modules.px.appstorePc.PcAppStoreService;
import com.xd.pre.modules.px.douyin.buyRender.BuyRenderParamDto;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import com.xd.pre.modules.px.douyin.pay.BalanceRedisDto;
import com.xd.pre.modules.px.douyin.pay.PayDto;
import com.xd.pre.modules.px.douyin.pay.PayRiskInfoAndPayInfoUtils;
import com.xd.pre.modules.px.douyin.submit.DouyinAsynCService;
import com.xd.pre.modules.px.douyin.submit.SubmitUtils;
import com.xd.pre.modules.px.task.ProductProxyTask;
import com.xd.pre.modules.px.utils.SysUtils;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.mapper.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.activemq.ScheduledMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.Queue;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DouyinService {


    @Resource
    private DouyinAppCkMapper douyinAppCkMapper;


    @Resource
    private JdAppStoreConfigMapper jdAppStoreConfigMapper;

    @Resource
    private DouyinDeviceIidMapper douyinDeviceIidMapper;

    @Autowired
    private PcAppStoreService pcAppStoreService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private JdOrderPtMapper jdOrderPtMapper;
    @Resource
    private JdMchOrderMapper jdMchOrderMapper;
    @Resource
    private JdProxyIpPortMapper jdProxyIpPortMapper;
    @Autowired
    @Lazy()
    private ProductProxyTask productProxyTask;
    @Autowired
    @Lazy()
    private DouyinAsynCService douyinAsynCService;
    @Resource(name = "product_douyin_stock_queue")
    private Queue product_douyin_stock_queue;
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Resource
    private JdLogMapper jdLogMapper;

    public R match(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog) {
        Boolean checkIp = checkIp(jdMchOrder, storeConfig, jdLog);
        if (!checkIp) {
            return null;
        }
        try {
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            storeConfig.setMark(jdMchOrder.getTenantId() + "");
            TimeInterval timer = DateUtil.timer();
            log.info("订单号{}，用户ip:{},开始抖音匹配订单", jdMchOrder.getTradeNo(), JSON.toJSONString(jdLog));
            OkHttpClient client = null;
            for (int i = 0; i < 3; i++) {
                try {
                    client = pcAppStoreService.buildClient();
                } catch (Exception e) {
                    log.info("获取代理ip报错:{}", jdMchOrder.getTradeNo());
                }
                if (ObjectUtil.isNotNull(client)) {
                    break;
                }
            }
            if (ObjectUtil.isNull(client)) {
                client = pcAppStoreService.buildClient();
            }
            log.info("订单号:{},判断是否存在已经存在的库存，重复利用", jdMchOrder.getTradeNo());
            //  redisTemplate.opsForValue().set("锁定抖音库存订单:" + jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo(), 5, TimeUnit.MINUTES);
            LambdaQueryWrapper<JdOrderPt> stockWrapper = Wrappers.lambdaQuery();
            stockWrapper.gt(JdOrderPt::getWxPayExpireTime, new Date());
            stockWrapper.eq(JdOrderPt::getSkuPrice, storeConfig.getSkuPrice());
            stockWrapper.eq(JdOrderPt::getIsWxSuccess, PreConstant.ONE);
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            Set<String> stockNums = redisTemplate.keys("锁定抖音库存订单:*");
            if (CollUtil.isNotEmpty(stockNums)) {
                List<String> sockIds = stockNums.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
                stockWrapper.notIn(JdOrderPt::getId, sockIds);
            }
            stockWrapper.isNull(JdOrderPt::getPaySuccessTime);
            List<JdOrderPt> jdOrderPtStocks = jdOrderPtMapper.selectList(stockWrapper);
            String payReUrl = "";
            JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
            if (CollUtil.isNotEmpty(jdOrderPtStocks) && ObjectUtil.isNotNull(jdMchOrderDb)) {
                log.info("订单号:{}.使用库存", jdMchOrder.getTradeNo());
                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 3);
                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 3);
                return douyinUseStock(jdMchOrder, storeConfig, jdLog, timer, client, jdOrderPtStocks, payReUrl);
            } else {
                log.info("订单号:{}.异步生成一下订单", jdMchOrder.getTradeNo());
                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 5);
                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 15);
                log.info("订单号:{},新下单", jdMchOrder.getTradeNo());
                return douyinProductNewOrder(jdMchOrder, storeConfig, jdLog, timer, client, payReUrl);
            }
        } catch (Exception e) {
            log.error("订单号：{}，匹配订单报错:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;

    }

    private Boolean checkIp(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog) {
        try {
            log.info("订单号:{}查询是否是ip黑名单", jdMchOrder.getTradeNo());
            Set<String> ipblack = redisTemplate.keys("IP黑名单:*");
            if (CollUtil.isNotEmpty(ipblack)) {
                List<String> ipBlackList = ipblack.stream().map(it -> it.replace("IP黑名单:", "")).collect(Collectors.toList());
                if (ipBlackList.contains(jdLog.getIp())) {
                    log.info("订单号{}再ip黑名单之内:,不匹配", jdMchOrder.getTradeNo());
                    jdMchOrder.setClickPay(new Date(0L));
                    Boolean isLockMath = redisTemplate.opsForValue().setIfAbsent("匹配锁定成功:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder),
                            storeConfig.getExpireTime(), TimeUnit.MINUTES);
                    if (isLockMath) {
                        jdMchOrderMapper.updateById(jdMchOrder);
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            log.info("订单号:{}黑名单出现错误:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return true;
    }

    // 发送消息，destination是发送到的队列，message是待发送的消息
    private void sendMessageSenc(Destination destination, final String message, Integer minit) {
        Map<String, Object> headers = new HashMap<>();
        //发送延迟队列，延迟10秒,单位毫秒
        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 1000);
        jmsMessagingTemplate.convertAndSend(destination, message, headers);
    }

    private R douyinUseStock(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog, TimeInterval timer, OkHttpClient client, List<JdOrderPt> jdOrderPtStocks, String payReUrl) {
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        JdOrderPt jdOrderPtDb = jdOrderPtStocks.get(PreUtils.randomCommon(0, jdOrderPtStocks.size() - 1, 1)[0]);
        if (jdOrderPtStocks.size() >= 20) {
            int[] ints = PreUtils.randomCommon(0, jdOrderPtStocks.size() - 1, 10);
            for (int i = 0; i < ints.length; i++) {
                int anInt = ints[i];
                JdOrderPt jdOrderPtT = jdOrderPtStocks.get(anInt);
                if (jdOrderPtT.getId() < jdOrderPtDb.getId()) {
                    jdOrderPtDb = jdOrderPtT;
                }
            }
        }
        String ptPin = jdOrderPtDb.getPtPin();
        String balanceStr = redisTemplate.opsForValue().get("抖音各个账号剩余额度:" + ptPin);
        if (StrUtil.isNotBlank(balanceStr) && JSON.parseObject(balanceStr).getInteger("balance") < 0) {
            return null;
        }
        PayDto payDto = JSON.parseObject(jdOrderPtDb.getMark(), PayDto.class);
        for (int i = 0; i < 2; i++) {
            log.info("订单号：{}第{}，次循环", jdMchOrder.getTradeNo(), i);
            payReUrl = payByOrderId(client, payDto, jdLog, jdMchOrder);
            JdProxyIpPort t = SysUtils.parseOkHttpClent(client, null);
            log.info("订单号:{},执行的ip:{}", jdMchOrder.getTradeNo(), t.getIp());
            if (StrUtil.isNotBlank(payReUrl)) {
                break;
            } else {
                deleleIPByClent(jdMchOrder, client);
                log.info("切换ip:{}", jdMchOrder.getTradeNo());
                client = pcAppStoreService.buildClient();
                continue;
            }
        }
        log.info("订单号{}，获取支付链接成功:时间戳{}", jdMchOrder.getTradeNo(), timer.interval());
        if (StrUtil.isBlank(payReUrl)) {
            return null;
        }
        Boolean ifLockStock = redisTemplate.opsForValue().setIfAbsent("锁定抖音库存订单:" + jdOrderPtDb.getId(), jdMchOrder.getTradeNo(),
                50, TimeUnit.MINUTES);
        if (!ifLockStock) {
            log.error("订单号{}，有人已经使用库存,请查看数据库msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo());
            return null;
        }
        Boolean isLockMath = redisTemplate.opsForValue().setIfAbsent("匹配锁定成功:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder),
                storeConfig.getExpireTime(), TimeUnit.MINUTES);
        if (!isLockMath) {
            log.error("订单号{}，库存当前已经匹配了,请查看数据库msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo());
            return null;
        }
        log.info("订单号:{},外部订单号是:{},有库存,匹配的订单内置订单号是:{},", jdMchOrder.getTradeNo(), jdMchOrder.getOutTradeNo(), jdOrderPtDb.getOrderId());
        log.info("订单号:{},当前库存存在支付连接", jdMchOrder.getTradeNo());
        //  .hrefUrl(payReUrl).weixinUrl(payReUrl).wxPayUrl(payReUrl)
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdOrderPtDb.setHrefUrl(payReUrl);
        jdOrderPtDb.setWeixinUrl(payReUrl);
        jdOrderPtDb.setWxPayUrl(payReUrl);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdOrderPtMapper.updateById(jdOrderPtDb);
        long l = (System.currentTimeMillis() - jdMchOrder.getCreateTime().getTime()) / 1000;
        jdMchOrder.setOriginalTradeId(jdOrderPtDb.getId());
        jdMchOrder.setMatchTime(l);
        jdMchOrder.setOriginalTradeNo(jdOrderPtDb.getOrderId());
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdMchOrderMapper.updateById(jdMchOrder);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
        JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdOrderPtDb.getId());
        if (ObjectUtil.isNull(jdMchOrderDb.getOriginalTradeId()) || !jdOrderPt.getHrefUrl().contains(jdMchOrderDb.getTradeNo())) {
            log.info("订单号:{},重新匹配", jdMchOrderDb.getTradeNo());
            redisTemplate.delete("锁定抖音库存订单:" + jdOrderPtDb.getId());
            jdMchOrder.setMatchTime(-5L);
            jdMchOrder.setOriginalTradeNo("-1");
            jdMchOrder.setOriginalTradeId(-1);
            jdMchOrderMapper.updateById(jdMchOrder);
            return null;
        }
        return R.ok(jdMchOrder);
    }

    public void deleleIPByClent(JdMchOrder jdMchOrder, OkHttpClient client) {
        try {
            JdProxyIpPort jdProxyIpPort = SysUtils.parseOkHttpClent(client, jdProxyIpPortMapper);
            if (ObjectUtil.isNotNull(jdProxyIpPort) && ObjectUtil.isNotNull(jdProxyIpPort.getId())) {
                log.info("订单号删除缓存IP数据:{},id:{}", jdMchOrder.getTradeNo(), jdProxyIpPort.getId());
                redisTemplate.delete("IP缓存池:" + jdProxyIpPort.getId());
                jdProxyIpPortMapper.deleteById(jdProxyIpPort.getId());
            }
        } catch (Exception e) {
            log.info("删除报错msg:{},报错信息:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
    }

    public DouyinAppCk randomDouyinAppCk(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, Boolean isAppStore) {
        Integer lockDouYinCkTime = Integer.valueOf(redisTemplate.opsForValue().get("抖音ck锁定分钟数"));
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        log.info("订单号{},开始查询可以使用的抖音账号msg:{}", jdMchOrder.getTradeNo());
        LambdaQueryWrapper<DouyinAppCk> wrapper = Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getIsEnable, PreConstant.ONE);
        log.info("查询剩余额度，并且只能在有效额度范围内的数据才能被查询出来");
        if (isAppStore) {
            //查询了锁定账号
            buildNotUseAccout(storeConfig, wrapper, jdMchOrder.getTradeNo());
        }
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        Integer count = douyinAppCkMapper.selectCount(wrapper);
        int pageIndex = PreUtils.randomCommon(0, count, 1)[0];
        List<Integer> accounts = new ArrayList<>();
        if (count > 30) {
            int[] ints = PreUtils.randomCommon(0, count, 25);
            for (int anInt : ints) {
                accounts.add(anInt);
            }
            accounts = accounts.stream().sorted().collect(Collectors.toList());
            pageIndex = accounts.get(PreConstant.ZERO);
        }
        if (count == 0) {
            log.info("订单号:{}，没有ck，请导入ck", jdMchOrder.getTradeNo());
            return null;
        }

        if (storeConfig.getGroupNum() == PreConstant.EIGHT && CollUtil.isNotEmpty(accounts)) {
            DouyinAppCk douyinAppCk = null;
            Integer let = Integer.valueOf(redisTemplate.opsForValue().get("抖音苹果卡最大下单金额"));
            Integer maxEd = let;
            for (Integer account : accounts) {
                Page<DouyinAppCk> douyinAppCkPage = new Page<>(account, PreConstant.ONE);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                douyinAppCkPage = douyinAppCkMapper.selectPage(douyinAppCkPage, wrapper);
                DouyinAppCk douyinAppCkT = douyinAppCkPage.getRecords().get(PreConstant.ZERO);
                String edStr = redisTemplate.opsForValue().get("抖音各个账号剩余额度:" + douyinAppCkT.getUid());
                String ed = null;
                if (StrUtil.isNotBlank(edStr)) {
                    ed = JSON.parseObject(edStr, BalanceRedisDto.class).getBalance() + "";
                }
                if (StrUtil.isNotBlank(ed) && Integer.valueOf(ed) >= storeConfig.getSkuPrice().intValue()) {
                    if (let >= Integer.valueOf(ed) && Integer.valueOf(ed) == 200) {
                        douyinAppCk = douyinAppCkT;
                        break;
                    }
                    if (let >= Integer.valueOf(ed) && Integer.valueOf(ed) <= maxEd) {
                        // 8000
                        maxEd = Integer.valueOf(ed);//8000
                        douyinAppCk = douyinAppCkT;
                    }
                }
            }
            if (ObjectUtil.isNotNull(douyinAppCk)) {
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("抖音ck锁定3分钟:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockDouYinCkTime, TimeUnit.MINUTES);
                if (ifAbsent) {
                    return douyinAppCk;
                }
            }
        }
        Page<DouyinAppCk> douyinAppCkPage = new Page<>(pageIndex, PreConstant.ONE);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        douyinAppCkPage = douyinAppCkMapper.selectPage(douyinAppCkPage, wrapper);
        DouyinAppCk douyinAppCk = douyinAppCkPage.getRecords().get(PreConstant.ZERO);
        log.info("订单号{}，当前执行的ckmsg:{}", jdMchOrder.getTradeNo(), JSON.toJSONString(douyinAppCk));
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("抖音ck锁定3分钟:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockDouYinCkTime, TimeUnit.MINUTES);
        if (ifAbsent) {
            return douyinAppCk;
        }
        return null;
    }

    public Integer getPayType() {
        Integer payType = 0;
        String 抖音苹果卡Pay = redisTemplate.opsForValue().get("抖音苹果卡Pay");
        if (StrUtil.isNotBlank(抖音苹果卡Pay)) {
            payType = Integer.valueOf(抖音苹果卡Pay);
        } else {
            payType = 2;
            redisTemplate.opsForValue().set("抖音苹果卡Pay", "2");
        }
        return payType;
    }

    public R douyinProductNewOrder(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog, TimeInterval timer, OkHttpClient client, String payReUrl) {
        DouyinAppCk douyinAppCk = randomDouyinAppCk(jdMchOrder, storeConfig, true);
        if (ObjectUtil.isNull(douyinAppCk)) {
            return null;
        }
        //
        if (douyinAppCk.getIsOld() == 1) {
            log.info("woaini1:替换ck让同一个账号同时下单如果发现超过3个老号存在。就让老号继续下单");
            Set<String> oldruning = redisTemplate.keys("老号正在下单:*");
            String oldRuningTime = redisTemplate.opsForValue().get("老号持续下单个数");
            if (CollUtil.isEmpty(oldruning) || oldruning.size() < Integer.valueOf(oldRuningTime)) {
                Integer 老号锁定分钟数 = Integer.valueOf(redisTemplate.opsForValue().get("老号锁定分钟数"));
                log.info("woaini:2替换ck让老号继续下单");
                redisTemplate.opsForValue().set("老号正在下单:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), Integer.valueOf(老号锁定分钟数), TimeUnit.MINUTES);
            } else {
                log.info("woaini:3替换ck让老号继续下单");
                Integer i = 0;
                if (oldruning.size() > 1) {
                    i = PreUtils.randomCommon(0, oldruning.size() - 1, 1)[0];
                }
                log.info("woaini:4第二步");
                String key = oldruning.stream().collect(Collectors.toList()).get(i);
                String s = redisTemplate.opsForValue().get(key);
                DouyinAppCk douyinAppCkT = JSON.parseObject(s, DouyinAppCk.class);
                douyinAppCkT = douyinAppCkMapper.selectById(douyinAppCkT.getId());
                if (douyinAppCkT.getIsEnable() == PreConstant.ONE) {
                    douyinAppCk = douyinAppCkT;
                    log.info("woaini:5第三步");
                }
            }
        } else {
            log.info("绕开1点到2点的时间");
            DateTime beginOfDay = DateUtil.beginOfDay(new Date());
            DateTime dateTime0 = DateUtil.offsetMinute(beginOfDay, 0);
            String 首单开始跑时间 = redisTemplate.opsForValue().get("首单开始跑时间");
            DateTime dateTime4 = DateUtil.offsetMinute(beginOfDay, Integer.valueOf(首单开始跑时间) * 60);
            if (System.currentTimeMillis() > dateTime0.getTime() && System.currentTimeMillis() < dateTime4.getTime()) {
                long l = (dateTime4.getTime() - System.currentTimeMillis()) / 1000;
                redisTemplate.opsForValue().set("抖音ck锁定3分钟:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), l, TimeUnit.SECONDS);
                log.info("当前不跑首单号锁定号uid:{}", douyinAppCk.getUid());
                return null;
            }
        }
        String config = storeConfig.getConfig();
        BuyRenderParamDto buyRenderParamDto = JSON.parseObject(config, BuyRenderParamDto.class);
        log.info("订单号{}，开始下单,执行双端支付信息msg:{}", jdMchOrder.getTradeNo());
        Integer payType = getPayType();
        log.info("订单号{}，初始化完成:时间戳{}", jdMchOrder.getTradeNo(), timer.interval());
        String tel = PreUtils.getTel();
        douyinAppCk.setCk(PreAesUtils.decrypt解密(douyinAppCk.getCk()));
        try {
            List<PayDto> payDtos = createOrder(client, buyRenderParamDto, payType, douyinAppCk, jdLog, jdMchOrder, timer, tel, storeConfig);
        } catch (Exception e) {
            log.info("释放当前线程的锁");
        }
        return R.ok();
    }

    public List<DouyinDeviceIid> getDouyinDeviceIids(JdMchOrder jdMchOrder) {
        Set<String> lockDouyinDeviceIds = redisTemplate.keys("抖音锁定设备:*");
        LambdaQueryWrapper<DouyinDeviceIid> douyinDeviceWrapper = Wrappers.lambdaQuery();
        if (CollUtil.isNotEmpty(lockDouyinDeviceIds)) {
            List<String> lockIds = lockDouyinDeviceIds.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
            douyinDeviceWrapper.notIn(DouyinDeviceIid::getId, lockIds);
        }
        log.info("订单号{}，开始执行获取device_id和iid，查询已经锁定的设备号", jdMchOrder.getTradeNo());
        List<DouyinDeviceIid> douyinDeviceIids = douyinDeviceIidMapper.selectList(douyinDeviceWrapper);
        JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
        Integer douyinDeviceIidSize = 2;
        if (ObjectUtil.isNotNull(jdMchOrderDb)) {
            douyinDeviceIidSize = 5;
        } else {
            douyinDeviceIidSize = 5;
        }
        int[] deviceRInts = PreUtils.randomCommon(0, douyinDeviceIids.size() - 1, douyinDeviceIids.size() - 1 > douyinDeviceIidSize ? douyinDeviceIidSize : douyinDeviceIids.size() - 1);
        List<DouyinDeviceIid> douyinDeviceIUseids = new ArrayList();
        for (int i = 0; i < deviceRInts.length; i++) {
            DouyinDeviceIid douyinDeviceIid = douyinDeviceIids.get(deviceRInts[i]);
            if (douyinDeviceIid.getDeviceId().contains("device_id=") || douyinDeviceIid.getIid().contains("install_id")
                    || douyinDeviceIid.getDeviceId().contains("device_id_str") || douyinDeviceIid.getIid().contains("install_id_str")) {
                if (douyinDeviceIid.getDeviceId().contains("device_id=")) {
                    douyinDeviceIid.setDeviceId(douyinDeviceIid.getDeviceId().split("=")[1]);
                }
                if (douyinDeviceIid.getIid().contains("install_id")) {
                    douyinDeviceIid.setIid(douyinDeviceIid.getIid().split("=")[1]);
                }
                douyinDeviceIidMapper.updateById(douyinDeviceIid);
            }
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("抖音锁定设备:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 3, TimeUnit.MINUTES);
            if (ifAbsent) {
                douyinDeviceIUseids.add(douyinDeviceIid);
            }
            if (CollUtil.isNotEmpty(douyinDeviceIids) && douyinDeviceIids.size() > 2) {
                return douyinDeviceIUseids;
            }

        }
        return douyinDeviceIUseids;
    }

    private void buildNotUseAccout(JdAppStoreConfig storeConfig, LambdaQueryWrapper<DouyinAppCk> wrapper, String no) {
        Set<String> edus = redisTemplate.keys("抖音各个账号剩余额度:*");
        Set<String> locks = redisTemplate.keys("抖音ck锁定3分钟:*");
        if (CollUtil.isNotEmpty(locks)) {
            List<String> locksData = locks.stream().map(it -> it.replace("抖音ck锁定3分钟:", "")).collect(Collectors.toList());
            wrapper.notIn(DouyinAppCk::getUid, locksData);
        }
        List<String> allData = redisTemplate.opsForValue().multiGet(edus);
        Map<String, Integer> balanceMap = allData.stream().map(it -> JSON.parseObject(it, BalanceRedisDto.class)).collect(Collectors.toMap(it -> it.getUid(), it -> it.getBalance()));
        log.info("新用户只能下一单");
        if (CollUtil.isNotEmpty(edus)) {
            List<String> noUseData = new ArrayList<>();
            for (String edu : edus) {
                edu = edu.replace("抖音各个账号剩余额度:", "");
                Integer sufEdu = balanceMap.get(edu);
                if (sufEdu - storeConfig.getSkuPrice().intValue() >= 0) {
                    continue;
                } else {
                    log.debug("{},这个账号不存在额度", edu);
                    noUseData.add(edu);
                }
            }
            if (CollUtil.isNotEmpty(noUseData)) {
                log.info("订单号{},不能使用的账号:{}", no, JSON.toJSONString(noUseData));
                wrapper.notIn(DouyinAppCk::getUid, noUseData);
            }
        }

    }

    public String getPayReUrl(JdMchOrder jdMchOrder, JdLog jdLog, TimeInterval timer, OkHttpClient client, PayDto payDto) {
        String payReUrl = "";
        log.info("订单号{}，创建订单完成:时间戳{}", jdMchOrder.getTradeNo(), timer.interval());
        for (int i = 0; i < 3; i++) {
            log.info("订单号：{}第{}，次循环", jdMchOrder.getTradeNo(), i);
            log.info("订单号:{},第一次获取支付数据", jdMchOrder.getTradeNo());
            payReUrl = payByOrderId(client, payDto, jdLog, jdMchOrder);
            if (StrUtil.isNotBlank(payReUrl)) {
                break;
            } else {
                client = pcAppStoreService.buildClient();
            }
        }
        log.info("订单号{}，获取支付链接成功:时间戳{}", jdMchOrder.getTradeNo(), timer.interval());
        if (StrUtil.isBlank(payReUrl) && ObjectUtil.isNotNull(jdMchOrder)) {
            return null;
        }
        return payReUrl;
    }


    public String payByOrderId(OkHttpClient client, PayDto payDto, JdLog jdLog, JdMchOrder jdMchOrder) {
        try {
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            String bodyData = PayRiskInfoAndPayInfoUtils.buildPayForm(payDto);
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            // String bodyData = "app_name=aweme&channel=dy_tiny_juyouliang_dy_and24&device_platform=android&iid=3743163984904813&order_id=4983651837194409539&os=android&device_id=2538093503847412&aid=1128&pay_type=1";
//            String url = "https://ec.snssdk.com/order/createpay?device_id=2538093503847412&aid=1128&device_platform=android&device_type=SM-G955N&request_tag_from=h5&app_name=aweme&version_name=17.3.0&app_type=normal&channel=dy_tiny_juyouliang_dy_and24&iid=3743163984904813&version_code=170300&os=android&os_version=5.1.1";
            String url = PayRiskInfoAndPayInfoUtils.buidPayUrl(payDto);
            String X_SS_STUB = SecureUtil.md5(bodyData).toUpperCase();
            String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    X_SS_STUB, url
            );
            String signUrl = getSignUrl();
            log.info("订单号{}，签证地址msg:{}", jdMchOrder.getTradeNo(), signUrl);
            String signHt = HttpRequest.post(signUrl).body(signData).timeout(4000).execute().body();
            String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
            String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
            RequestBody body = RequestBody.create(mediaType, bodyData);
            Request.Builder builder = new Request.Builder();
            Map<String, String> header = PreUtils.buildIpMap(jdLog.getIp());
            for (String s : header.keySet()) {
                builder.header(s, header.get(s));
            }
            Request request = builder.url(url)
                    .post(body)
                    .addHeader("X-SS-STUB", X_SS_STUB)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(payDto.getCk()))
                    .addHeader("X-Gorgon", x_gorgon)
                    .addHeader("X-Khronos", x_khronos)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response = client.newCall(request).execute();
            String payData = response.body().string();
            if (payData.contains("订单已被支付")) {
                JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, payDto.getOrderId()));
                jdOrderPt.setWxPayExpireTime(DateUtil.offsetMinute(new Date(), -100));
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                jdOrderPtMapper.updateById(jdOrderPt);
                return null;
            }
            log.info("订单号{}，原始订单号:{}支付消息返回数据msg:{}", jdMchOrder.getTradeNo(), payDto.getOrderId(), payData);
            String payUrl = JSON.parseObject(JSON.parseObject(JSON.parseObject(JSON.parseObject(payData).getString("data")).getString("data"))
                    .getString("sdk_info")).getString("url");
            redisTemplate.opsForValue().set("阿里支付数据:" + jdMchOrder.getTradeNo(), payUrl, 3, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("阿里支付数据:" + jdMchOrder.getTradeNo(), payUrl, 3, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("阿里支付数据:" + jdMchOrder.getTradeNo(), payUrl, 3, TimeUnit.MINUTES);
            log.info("订单号:{}设置阿里支付数据成功", jdMchOrder.getTradeNo());
            String param = redisTemplate.opsForValue().get("阿里支付数据:" + jdMchOrder.getTradeNo().trim());
            if (StrUtil.isNotBlank(param)) {
                log.info("订单号:{},查询成功设置阿里支付数据成功,查询成功", jdMchOrder.getTradeNo());
            }
            response.close();
            String local = redisTemplate.opsForValue().get("服务器地址");
            //alipays://platformapi/startapp?appId=20000067&url=http%3A%2F%2F134.122.134.69%3A8082%2Frecharge%2Fzfb%3Forder_id%3DSP2210012316069040391319127864
            String payReUrl = String.format("alipays://platformapi/startapp?appId=20000067&url=%s",
                    URLEncoder.encode("http://" + local + "/api/alipay/payHtml?orderId=" + jdMchOrder.getTradeNo()));
            log.info("订单号{}，封装url数据为msg:{}", jdMchOrder.getTradeNo(), payReUrl);
            return payReUrl;
        } catch (Exception e) {
            if (StrUtil.isNotBlank(e.getMessage()) && e.getMessage().contains("Failed")) {
                log.info("订单号{}", jdMchOrder.getTradeNo());
            }
            log.error("订单号{}，请求报错msg:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;
    }

    private String geSuccessOrder(OkHttpClient client, BuyRenderParamDto buyRenderParamDto, Integer payType, DouyinAppCk douyinAppCk, JdLog jdLog, JdMchOrder jdMchOrder, TimeInterval timer, String phone, DouyinDeviceIid douyinDeviceIid) throws IOException {
        try {
            log.info("订单号:{},锁定设备号:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
               /* Boolean isLockDeviceId = redisTemplate.opsForValue().setIfAbsent("抖音锁定设备:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 1, TimeUnit.MINUTES);
                if (!isLockDeviceId) {
                    log.info("订单号{}，当前设备号已经锁定:deviceId:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
                    continue;
                }*/
            BuyRenderRoot buyRenderRoot = getAndBuildBuyRender(client, douyinAppCk, buyRenderParamDto, douyinDeviceIid, jdMchOrder);
            log.info("订单号:{},循环次数,预下单时间戳:{}", jdMchOrder.getTradeNo(), timer.interval());
            if (ObjectUtil.isNull(buyRenderRoot)) {
                log.info("订单号{}，预下单失败", jdMchOrder.getTradeNo());
                return null;
            }
            if (ObjectUtil.isNotNull(buyRenderRoot) && ObjectUtil.isNotNull(buyRenderRoot.getCheckIp()) && buyRenderRoot.getCheckIp()) {
                return "checkIp";
            }
            //PreUtils.getTel()
            buyRenderRoot.setPost_tel(phone);
            String url1 = "https://ec.snssdk.com/order/newcreate/vtl?can_queue=1&b_type_new=2&request_tag_from=lynx&os_api=5&device_type=ELE-AL00&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=354730528931234&app_name=aweme&version_name=17.3.0&cpu_support64=false&app_type=normal&appTheme=dark&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy&device_platform=android&iid=" + douyinDeviceIid.getIid() + "&version_code=170300&cdid=78d30492-1201-49ea-b86a-1246a704711d&os=android&is_android_pad=0&openudid=27b54460b6dbb870&device_id="
                    + douyinDeviceIid.getDeviceId() + "&resolution=720*1280&os_version=7.1.1&language=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc=46007";
            String bodyData1 = String.format("{\"area_type\":\"170\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"}," +
                            "\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\"," +
                            "\"origin_type\":\"%s\"," +
                            "\"origin_id\":\"%s\"," +
                            "\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\"," +
                            "\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\"," +
                            "\\\"alkey\\\":\\\"1128_99514375927_0_3556357046087622442_010\\\"," +
                            "\\\"c_biz_combo\\\":\\\"8\\\"," +
                            "\\\"render_track_id\\\":\\\"%s\\\"," +
                            "\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\"" +
                            ",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\"," +
                            "\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true," +
                            "\\\\\\\"ip\\\\\\\":\\\\\\\"%s\\\\\\\"," +
                            "\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\"," +
                            "\"marketing_plan_id\":\"%s\"," +
                            "\"s_type\":\"\"" +
                            ",\"entrance_params\":\"{\\\"order_status\\\":4,\\\"previous_page\\\":\\\"toutiao_mytab__order_list_page\\\"," +
                            "\\\"carrier_source\\\":\\\"order_detail\\\"," +
                            "\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"room_id\\\":\\\"\\\"," +
                            "\\\"promotion_id\\\":\\\"\\\",\\\"author_id\\\":\\\"\\\",\\\"group_id\\\":\\\"\\\",\\\"anchor_id\\\":\\\"\\\"," +
                            "\\\"source_method\\\":\\\"open_url\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\"," +
                            "\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\"," +
                            "\\\"is_activity_banner\\\":0," +
                            "\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\"," +
                            "\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"0\\\",\\\"is_with_video\\\":1,\\\"label_name\\\":\\\"\\\"," +
                            "\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0," +
                            "\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0," +
                            "\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\"," +
                            "\\\"commodity_id\\\":\\\"%s\\\",\\\"commodity_type\\\":6," +
                            "\\\"product_id\\\":\\\"%s\\\",\\\"extra_campaign_type\\\":\\\"\\\"}\"," +
                            "\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0," +
                            "\"pay_type\":%d," +
                            "\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}}," +
                            "\"post_tel\":\"%s\",\"address_id\":\"0\",\"price_info\":{\"origin\":1000,\"freight\":0,\"coupon\":0," +
                            "\"pay\":1000}," +
                            "\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\",\\\"dev_info\\\":{\\\"reqIp\\\":\\\"39.144.42.162\\\",\\\"os\\\":\\\"android\\\"," +
                            "\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.3.5\\\",\\\"aid\\\":\\\"13\\\"," +
                            "\\\"ua\\\":\\\"com.ss.android.article.news/8960+(Linux;+U;+Android+10;+zh_CN;" +
                            "+PACT00;+Build/QP1A.190711.020;+Cronet/TTNetVersion:68deaea9+2022-07-19+QuicVersion:12a1d5c5+2022-06-27)\\\"," +
                            "\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\"," +
                            "\\\"deviceId\\\":\\\"%s\\\",\\\"osVersion\\\":\\\"10\\\"," +
                            "\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\"," +
                            "\\\"appVersion\\\":\\\"8.9.6\\\",\\\"appName\\\":\\\"aweme\\\"," +
                            "\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"PACT00\\\"," +
                            "\\\"channel\\\":\\\"oppo_13_64\\\",\\\"openudid\\\":\\\"\\\"," +
                            "\\\"versionCode\\\":\\\"896\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"OPPO\\\",\\\"iid\\\":\\\"%s\\\",\\\"bioType\\\":\\\"1\\\"}," +
                            "\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[]," +
                            "\\\"zg_ext_param\\\":" +
                            "\\\"{\\\\\\\"decision_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"," +
                            "\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\"," +
                            "\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"%s\\\\\\\"}\\\"," +
                            "\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\"," +
                            "\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\"," +
                            "\"render_token\":\"%s\"," +
                            "\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\"," +
                            "\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\"," +
                            "\"shop_id\":\"GceCTPIk\"," +
                            "\"combo_id\":\"%s\"," +
                            "\"combo_num\":1," +
                            "\"product_id\":\"%s\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1," +
                            "\"sku_id\":\"%s\"" +
                            ",\"warehouse_id\":\"0\"}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false," +
                            "\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"0\",\"promotion_id\":\"\"," +
                            "\"ecom_scene_id\":\"%s\"," +
                            "\"shop_user_id\":\"\",\"group_id\":\"\"," +
                            "\"privilege_tag_keys\":[],\"select_privilege_properties\":[]," +
                            "\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                    buyRenderParamDto.getOrigin_type(),
                    buyRenderParamDto.getOrigin_id(),
                    buyRenderRoot.getRender_track_id(),
                    jdLog.getIp(),
                    buyRenderRoot.getTotal_price_result().getMarketing_plan_id(),
                    buyRenderParamDto.getEcom_scene_id(),
                    buyRenderParamDto.getProduct_id(),
                    buyRenderParamDto.getProduct_id(),
                    payType,
                    PreUtils.getTel(),
                    douyinDeviceIid.getDeviceId(),
                    douyinDeviceIid.getIid(),
                    buyRenderRoot.getPay_method().getDecision_id(),
                    buyRenderRoot.getPay_method().getPayapi_cache_id(),
                    buyRenderRoot.getRender_token(),
                    buyRenderParamDto.getSku_id(),
                    buyRenderParamDto.getProduct_id(),
                    buyRenderParamDto.getSku_id(),
                    buyRenderParamDto.getEcom_scene_id()
            );
            String X_SS_STUB1 = SecureUtil.md5("json_form=" + URLEncoder.encode(bodyData1)).toUpperCase();
            String signData1 = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    X_SS_STUB1, url1
            );
            String signUrl = getSignUrl();
            log.info("订单号{}，签证地址msg:{}", jdMchOrder.getTradeNo(), signUrl);
            String signHt1 = HttpRequest.post(signUrl).body(signData1).timeout(2000).execute().body();
            String x_gorgon1 = JSON.parseObject(signHt1).getString("x-gorgon");
            String x_khronos1 = JSON.parseObject(signHt1).getString("x-khronos");
            String tarceid1 = JSON.parseObject(signHt1).getString("tarceid");
            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", bodyData1)
                    .build();
            Map<String, String> headers = PreUtils.buildIpMap(jdLog.getIp());
            Request.Builder builder = new Request.Builder();
            for (String s : headers.keySet()) {
                builder.header(s, headers.get(s));
            }
            Request request1 = builder.url(url1)
                    .post(requestBody1)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", X_SS_STUB1)
                    .addHeader("x-tt-trace-id", tarceid1)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", x_gorgon1)
                    .addHeader("X-Khronos", x_khronos1)
                    .build();
            Response response1 = client.newCall(request1).execute();
            String bodyRes1 = response1.body().string();
            response1.close();
            log.info("订单号{},下单时间循环次数msg,时间戳：{},下单结果信息结果：{},uid:{}", jdMchOrder.getTradeNo(),
                    timer.interval(),
                    bodyRes1, douyinAppCk.getUid());
            return bodyRes1;
        } catch (Exception e) {
            log.error("订单号msg:{},失败:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;
    }

    public List<PayDto> createOrder(OkHttpClient client, BuyRenderParamDto buyRenderParamDto, Integer payType,
                                    DouyinAppCk douyinAppCk, JdLog jdLog, JdMchOrder jdMchOrder, TimeInterval timer, String phone, JdAppStoreConfig storeConfig) {
        List<PayDto> payDtos = new ArrayList<>();
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
//        redisTemplate.opsForValue().set("抖音和设备号关联:" + douyinAppCk.getUid(), JSON.toJSONString(douyinDeviceIid), 4, TimeUnit.HOURS);
        String deviceBangDing = redisTemplate.opsForValue().get("抖音和设备号关联:" + douyinAppCk.getUid());
        List<DouyinDeviceIid> douyinDeviceIids = new ArrayList<>();
   /*     if (StrUtil.isNotBlank(deviceBangDing)) {
            DouyinDeviceIid douyinDeviceIid = JSON.parseObject(deviceBangDing, DouyinDeviceIid.class);
            log.info("订单号：{}管理，关联设备号:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
            douyinDeviceIids.add(douyinDeviceIid);
            douyinDeviceIids.add(douyinDeviceIid);
            douyinDeviceIids.add(douyinDeviceIid);
        }*/
        if (StrUtil.isBlank(deviceBangDing)) {
            log.info("订单号:{},ck:udi:{},关闭账号未待使用", jdMchOrder.getTradeNo(), douyinAppCk.getUid());
            douyinAppCk.setIsEnable(PreConstant.ZERO);
            douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
            redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
            douyinAppCkMapper.updateById(douyinAppCk);
            return null;
        }
        Integer sufMeny = getSufMeny(douyinAppCk.getUid(), jdMchOrder);
        if (sufMeny < 100) {
            redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
            log.info("删除额度不够的数据");
            return null;
        }
        if (sufMeny > 200) {
            int times = sufMeny / new BigDecimal(jdMchOrder.getMoney()).intValue();//96
            DouyinDeviceIid douyinDeviceIid = JSON.parseObject(deviceBangDing, DouyinDeviceIid.class);
            if (times > 10) {
                times = times - 5;
            }
            for (int i = 0; i < times; i++) {
                douyinDeviceIids.add(douyinDeviceIid);
            }
        } else {
            DouyinDeviceIid douyinDeviceIid = JSON.parseObject(deviceBangDing, DouyinDeviceIid.class);
            douyinDeviceIids.add(douyinDeviceIid);
        }

//        redisTemplate.opsForValue().setIfAbsent("老号锁定线程账号IP:" + douyinAppCk.getUid(), JSON.toJSONString(jdProxyIpPort), suf / 1000, TimeUnit.SECONDS);
        for (DouyinDeviceIid douyinDeviceIid : douyinDeviceIids) {
            if (isProductElef(storeConfig) && douyinAppCk.getIsOld() != 1) {
                log.info("订单号:{},已经够了库存。", jdMchOrder.getTradeNo());
                redisTemplate.delete("当前账号循环额度:" + douyinAppCk.getUid());
                redisTemplate.delete("抖音ck锁定3分钟:" + douyinAppCk.getUid());
                return null;
            }
            douyinAppCk = douyinAppCkMapper.selectById(douyinAppCk.getId());
            if (douyinAppCk.getIsEnable() != 1) {
                redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
                log.info("订单号:{},当前ck已经失效 uid:{}", jdMchOrder.getTradeNo(), douyinAppCk.getUid());
                return null;
            }
            try {
                sufMeny = getSufMeny(douyinAppCk.getUid(), jdMchOrder);
                if (sufMeny - new BigDecimal(jdMchOrder.getMoney()).intValue() < 0) {
                    log.info("当前ck出现了余额不足的情况");
                    redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
                    synProductMaxPrirce();
                    return null;
                }
                String bodyRes1 = geSuccessOrder(client, buyRenderParamDto, payType, douyinAppCk, jdLog, jdMchOrder, timer, phone, douyinDeviceIid);
                if (StrUtil.isNotBlank(bodyRes1) & bodyRes1.equals("checkIp")) {
                    client = pcAppStoreService.buildClient();
                    continue;
                }
                if (bodyRes1 == null) {
                    Long failOldTimes = redisTemplate.opsForValue().increment("老号失败次数:" + douyinAppCk.getUid(), 1);
                    if (failOldTimes >= 30) {
                        redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
                        douyinAppCk.setIsEnable(88);
                        this.douyinAppCkMapper.updateById(douyinAppCk);
                        return null;
                    }
                    continue;
                }
                if (bodyRes1.contains("order_id")) {
                    redisTemplate.delete("老号失败次数:" + douyinAppCk.getUid());///重置下单失败次数
                    redisTemplate.delete("抖音下单次数过多:" + douyinAppCk.getUid());//重置下单次数
                    douyinAppCk.setSuccessTime(new Date());
                    douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
                    douyinAppCkMapper.updateById(douyinAppCk);
                    log.info("订单号:{},设备号重复使用查询和删除", jdMchOrder.getTradeNo());
//                    deleteLockCk(douyinAppCk, douyinDeviceIid);
                    redisTemplate.opsForValue().set("抖音锁定设备:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    redisTemplate.opsForValue().set("抖音锁定设备:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    log.info("订单号:{},当前设备号和uid绑定其他人不能使用msg:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getId());
                    redisTemplate.opsForValue().set("抖音和设备号关联:" + douyinAppCk.getUid(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    redisTemplate.opsForValue().set("抖音和设备号关联:" + douyinAppCk.getUid(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    log.info("订单号{}，下单成功", jdMchOrder);
                    String orderId = JSON.parseObject(JSON.parseObject(bodyRes1).getString("data")).getString("order_id");
                    log.info("订单号{}，当前订单号msg:{}", jdMchOrder.getTradeNo(), orderId);
                    redisTemplate.opsForValue().increment("抖音设备号成功次数:" + douyinDeviceIid.getDeviceId());
                    redisTemplate.opsForValue().increment("抖音账号成功次数:" + douyinAppCk.getUid());
                    douyinDeviceIid.setSuccess(douyinDeviceIid.getSuccess() == null ? 1 : douyinDeviceIid.getSuccess() + 1);
                    log.info("订单号:{}设置上次成功时间msg:{}", jdMchOrder.getTradeNo(), new Date().toLocaleString());
                    douyinDeviceIid.setLastSuccessTime(new Date());
                    douyinDeviceIidMapper.updateById(douyinDeviceIid);
                    PayDto payDto = PayDto.builder().ck(PreAesUtils.encrypt加密(douyinAppCk.getCk())).device_id(douyinDeviceIid.getDeviceId()).iid(douyinDeviceIid.getIid()).pay_type(payType + "")
                            .orderId(orderId).userIp(jdLog.getIp()).build();
                    JdOrderPt jdOrderPtDb = JdOrderPt.builder().orderId(payDto.getOrderId()).ptPin(douyinAppCk.getUid()).success(PreConstant.ZERO)
                            .expireTime(DateUtil.offsetMinute(new Date(), storeConfig.getPayIdExpireTime())).createTime(new Date()).skuPrice(storeConfig.getSkuPrice())
                            .skuName(storeConfig.getSkuName()).skuId(storeConfig.getSkuId())
                            .wxPayExpireTime(DateUtil.offsetMinute(new Date(), storeConfig.getPayIdExpireTime()))
                            .createTime(new Date()).skuPrice(storeConfig.getSkuPrice())
                            .isWxSuccess(PreConstant.ONE).isMatch(PreConstant.ONE).currentCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()))
                            .mark(JSON.toJSONString(payDto))
                            .build();
                    PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                    log.info("+++++++++++++订单号:{},{}", jdMchOrder.getTradeNo(), JSON.toJSONString(jdOrderPtDb));
                    sufMeny = getSufMeny(douyinAppCk.getUid(), jdMchOrder);
                    Map<String, Object> balanceDto = new HashMap<>();
                    balanceDto.put("balance", sufMeny - storeConfig.getSkuPrice().intValue());
                    balanceDto.put("uid", douyinAppCk.getUid());
                    redisTemplate.opsForValue().set("抖音各个账号剩余额度:" + douyinAppCk.getUid(), JSON.toJSONString(balanceDto));
                    jdOrderPtMapper.insert(jdOrderPtDb);
                } else {
                    Long failOldTimes = redisTemplate.opsForValue().increment("老号失败次数:" + douyinAppCk.getUid(), 1);
                    if (failOldTimes >= 30) {
                        douyinAppCk.setIsEnable(88);
                        douyinAppCkMapper.updateById(douyinAppCk);
                        return null;
                    }
                    douyinAppCk.setFailReason(douyinAppCk.getFailReason() + bodyRes1);
                    PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                    if (bodyRes1.contains("设备存在异常")) {
                        redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
                        douyinAppCk.setIsEnable(-44);
                    }
                    if (bodyRes1.contains("当前下单人数过")) {
                        Long increment = redisTemplate.opsForValue().increment("抖音下单次数过多:" + douyinAppCk.getUid(), 1);
                        if (increment >= 10) {
                            redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
                            douyinAppCk.setIsEnable(-10);
                        }
                    }
                    douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
                    douyinAppCkMapper.updateById(douyinAppCk);
                }
            } catch (Exception e) {
                redisTemplate.opsForValue().increment("老号失败次数:" + douyinAppCk.getUid(), 1);
                log.error("订单号{}，当前抖音报错:{},时间戳:{}", jdMchOrder.getTradeNo(), e.getMessage(), timer.interval());
            }
        }
        if (CollUtil.isEmpty(payDtos)) {
            log.info("订单号{}，当前下单失败,请查看原因", jdMchOrder.getTradeNo());
            return null;
        }
        redisTemplate.delete("当前账号循环额度:" + douyinAppCk.getUid());
        redisTemplate.delete("抖音ck锁定3分钟:" + douyinAppCk.getUid());
        return payDtos;
    }

    private boolean isProductElef(JdAppStoreConfig storeConfig) {
        LambdaQueryWrapper<JdOrderPt> stockWrapper = Wrappers.lambdaQuery();
        stockWrapper.eq(JdOrderPt::getSkuPrice, storeConfig.getSkuPrice());
        Set<String> stockNums = redisTemplate.keys("锁定抖音库存订单:*");
        if (CollUtil.isNotEmpty(stockNums)) {
            List<String> sockIds = stockNums.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
            stockWrapper.notIn(JdOrderPt::getId, sockIds);
        }
        stockWrapper.eq(JdOrderPt::getIsWxSuccess, PreConstant.ONE);
        stockWrapper.isNull(JdOrderPt::getPaySuccessTime).gt(JdOrderPt::getWxPayExpireTime, new Date());
        List<JdOrderPt> jdOrderPtStocks = jdOrderPtMapper.selectList(stockWrapper);
        if (jdOrderPtStocks.size() >= storeConfig.getProductStockNum()) {
            log.info("超过库存。不用生产");
            return true;
        }
        return false;
    }


    public BuyRenderRoot getAndBuildBuyRender(OkHttpClient client, DouyinAppCk douyinAppCk, BuyRenderParamDto buyRenderParamDto,
                                              DouyinDeviceIid douyinDeviceIid, JdMchOrder jdMchOrder) {
        try {
            String body = SubmitUtils.buildBuyRenderParamData(buyRenderParamDto);
            if (Integer.valueOf(jdMchOrder.getPassCode()) == PreConstant.TEN) {
                body = SubmitUtils.buildBuyRenderYongHui(buyRenderParamDto);
            }
            String url = "https://ken.snssdk.com/order/buyRender?b_type_new=2&request_tag_from=lynx&os_api=25&device_type=SM-G973N&ssmix=a&manifest_version_code=169&dpi=240&is_guest_mode=0&uuid=354730528934825&app_name=aweme&version_name=17.3.0&ts=1664384063&cpu_support64=false&app_type=normal&appTheme=dark&ac=4G&host_abi=arm64-v8a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1664384064117&device_platform=android&iid="
                    + douyinDeviceIid.getIid() + "&version_code=170300&cdid=78d30492-1201-49ea-b86a-1246a704711d&os=android&is_android_pad=0&openudid=199d79fbbeff0e58&device_id=" + douyinDeviceIid.getDeviceId() + "&resolution=720%2A1280&os_version=5.1.1&language=zh&device_brand=Xiaomi&aid=1128&minor_status=0&mcc_mnc=46011";
            String X_SS_STUB = SecureUtil.md5("json_form=" + URLEncoder.encode(body)).toUpperCase();
            String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    X_SS_STUB, url
            );
            String signUrl = getSignUrl();
            String signHt = HttpRequest.post(signUrl).body(signData).timeout(2000).execute().body();
            String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
            String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
            RequestBody requestBody = new FormBody.Builder()
                    .add("json_form", body)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("X-SS-STUB", X_SS_STUB)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-Gorgon", x_gorgon)
                    .addHeader("X-Khronos", x_khronos)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response = client.newCall(request).execute();
            String resBody = response.body().string();
            log.info("订单号{}，预下单数据msg:{}", jdMchOrder.getTradeNo(), resBody);
            if (StrUtil.isNotBlank(resBody) && (resBody.contains("用户信息获取失败") || resBody.contains("用户未登录"))) {
                log.error("订单号{}，当前账号ck过期", jdMchOrder.getTradeNo());
                douyinAppCk.setIsEnable(PreConstant.FUYI_1);
                douyinAppCk.setFailReason(douyinAppCk.getFailReason() + resBody);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
                douyinAppCkMapper.updateById(douyinAppCk);
                return null;
            }
            if (StrUtil.isNotBlank(resBody) && resBody.contains("商品不可购买，请返回上级页面重新选购")) {
                douyinAppCk.setIsEnable(-2);
                douyinAppCk.setFailReason(douyinAppCk.getFailReason() + resBody);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
                douyinAppCkMapper.updateById(douyinAppCk);
                return null;
            }
            response.close();
            BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(resBody).getString("data"), BuyRenderRoot.class);
            return buyRenderRoot;
        } catch (Exception e) {
            if (StrUtil.isNotBlank(e.getMessage()) && e.getMessage().contains("out")) {
                log.error("订单号:{},预下单超时，切换client", jdMchOrder.getTradeNo());
                BuyRenderRoot buyRenderRoot = new BuyRenderRoot();
                buyRenderRoot.setCheckIp(true);
                return buyRenderRoot;
            }
            log.error("订单号{}，预下单失败请查看详情msg:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;
    }

    private String getSignUrl() {
        String signUrl = redisTemplate.opsForValue().get("抖音签证地址");
        if (StrUtil.isBlank(signUrl)) {
            signUrl = "http://1.15.184.191:8292/dy22";
            redisTemplate.opsForValue().set("抖音签证地址", "http://1.15.184.191:8292/dy22");
        }
        return signUrl;
    }


    public void selectOrderStataus(JdOrderPt jdOrderPt, JdMchOrder jdMchOrder) {
        String isfindOrderStatus = redisTemplate.opsForValue().get("是否查询阿里支付数据:" + jdMchOrder.getTradeNo().trim());
        if (StrUtil.isBlank(isfindOrderStatus)) {
            log.debug("订单号:{}没有访问数据。不需要查询", jdMchOrder.getTradeNo());
            return;
        }
        Integer sufClickPay = Integer.valueOf(redisTemplate.opsForValue().get("查单点击时间相隔时间"));
        if (ObjectUtil.isNull(jdMchOrder.getClickPay()) || DateUtil.offsetSecond(jdMchOrder.getClickPay(), sufClickPay).getTime() > new Date().getTime()) {
            log.info("订单号:{},在40秒之内。不用查询", jdMchOrder.getTradeNo());
            return;
        }
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("当前查询订单:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), 25, TimeUnit.SECONDS);
        if (!ifAbsent) {
            log.debug("当前订单,{},已经被锁定。请骚后查询", jdMchOrder.getTradeNo());
            return;
        }
        if (jdMchOrder.getStatus() == PreConstant.THREE) {
            log.info("订单号:{}已经退款。不回调", jdMchOrder.getTradeNo());
            return;
        }
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        OkHttpClient client = builder.build();
//        Response response = client.newCall(request).execute();
        String dali = redisTemplate.opsForValue().get("查询订单代理");
        if (Integer.valueOf(dali) == 1) {
            client = pcAppStoreService.buildClient();
        }
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        String findOrderTime = redisTemplate.opsForValue().get("查询订单次数");
        for (int i = 0; i < Integer.valueOf(findOrderTime); i++) {
            client = pcAppStoreService.buildClient();
            jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
            if (StrUtil.isNotBlank(jdOrderPt.getOrgAppCk())) {
                DateTime dateTime = DateUtil.parseDateTime(jdOrderPt.getOrgAppCk());
                String 成功查询订单数据不用查单 = redisTemplate.opsForValue().get("成功查询订单数据不用查单");
                if (DateUtil.offsetMinute(dateTime, -Integer.valueOf(成功查询订单数据不用查单)).getTime() > jdMchOrder.getCreateTime().getTime()) {
                    log.debug("订单号：{}+{},分钟都大于创建时间》》》》》》》》已经查询过了。没必要继续查询", 成功查询订单数据不用查单, jdMchOrder.getTradeNo());
                    return;
                }
            }
            String ptPin = jdOrderPt.getPtPin();
            log.info("订单号{}，查询订单循环次数:{}", jdMchOrder.getTradeNo(), i);
            String url = String.format("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?" +
                            "aid=%s",
                    PreUtils.randomCommon(100, 1000000, 1)[0] + "", jdOrderPt.getOrderId());
            String s = redisTemplate.opsForValue().get("抖音和设备号关联:" + ptPin);
            if (StrUtil.isNotBlank(s)) {
                DouyinDeviceIid douyinDeviceIid = JSON.parseObject(s, DouyinDeviceIid.class);
                url = String.format("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?" +
                                "aid=%s&order_id=%s&device_id=%s&iid=%s&channel=dy_tiny_juyouliang_dy_and24&app_name=news_article",
                        PreUtils.randomCommon(100, 1000000, 1)[0] + "", jdOrderPt.getOrderId(), douyinDeviceIid.getDeviceId(), douyinDeviceIid.getIid());
            }
            String x_gorgon = "8404d4860000775655c5b8f6315f8a608a802f3a78e4891a08cc";
            String x_khronos = "1665697911";
            try {
                String signData1 = String.format("{\"header\": {\"X-SS-STUB\": \"\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}", url);
                String signUrl = getSignUrl();
                log.info("订单号{}查询订单，签证地址msg:{}", jdMchOrder.getTradeNo(), signUrl);
                String signHt1 = HttpRequest.post(signUrl).body(signData1).timeout(3000).execute().body();
                log.info("订单号：{}，返回数据:{}", jdMchOrder.getTradeNo(), signHt1);
                x_gorgon = JSON.parseObject(signHt1).getString("x-gorgon");
                x_khronos = JSON.parseObject(signHt1).getString("x-khronos");
                log.info("订单号:{},xk和xg数据为:{},{}", jdMchOrder.getTradeNo(), x_khronos, x_gorgon);
            } catch (Exception e) {
                log.info("查询订单xg和xk报错:{}", jdMchOrder.getTradeNo());
            }
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "okhttp/3.10.0.1")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("Cookie", PreAesUtils.decrypt解密(jdOrderPt.getCurrentCk()))
                    .addHeader("X-Khronos", x_khronos)
                    .addHeader("X-Gorgon", x_gorgon)
                    .build();
            String body = null;
            try {
                Response response = client.newCall(request).execute();
                body = response.body().string();
                response.close();
            } catch (Exception e) {
//                redisTemplate.delete("")
                log.info("订单号{},订单号查询订单详情错误错误-----,切换ip查询", jdMchOrder.getTradeNo());
            }
            log.info("订单号{}，查询订单数据订单结果msg:有值", jdMchOrder.getTradeNo());
            if (StrUtil.isBlank(body)) {
                log.info("订单号{}，查询订单结果为空。。。。。。。XXXXXXXXXXXXXXX", jdMchOrder.getTradeNo(), body);
                continue;
            }
            String findOrderData = DateUtil.formatDateTime(new Date());
            log.info("订单号：{}，查询成功时间:{}", jdMchOrder.getTradeNo(), findOrderData);
            String html = JSON.parseObject(body).getString("order_detail_info");
            if (StrUtil.isNotBlank(html)) {
                String shop_order_status_info = JSON.parseObject(html).getString("shop_order_status_info");
                jdOrderPt.setHtml(shop_order_status_info);
                jdOrderPt.setOrgAppCk(findOrderData);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                jdOrderPtMapper.updateById(jdOrderPt);
            }
            String voucher_info_listStr = JSON.parseObject(html).getString("voucher_info_list");
            if (StrUtil.isBlank(voucher_info_listStr) || !voucher_info_listStr.contains("voucher_status")) {
                return;
            }
            List<JSONObject> voucher_info_list = JSON.parseArray(voucher_info_listStr, JSONObject.class);
            if (CollUtil.isNotEmpty(voucher_info_list)) {
                JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
                String code = voucher_info.getString("code");
                if (StrUtil.isNotBlank(code)) {
                    updateSuccess(jdMchOrder, jdOrderPt, code, client);
                    log.info("订单号：{}，开始计算成功金额,pin:{}", jdMchOrder.getTradeNo());
                    return;
                }
            }
            return;
        }
    }

    private Integer getSufMeny(String uid, JdMchOrder jdMchOrder) {
        LambdaQueryWrapper<JdOrderPt> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(JdOrderPt::getPtPin, uid.trim());
        wrapper.lt(JdOrderPt::getPaySuccessTime, DateUtil.beginOfDay(new Date()));
        wrapper.isNotNull(JdOrderPt::getCardNumber);
        String sdata = redisTemplate.opsForValue().get("抖音各个账号剩余额度:" + uid);
        if (StrUtil.isNotBlank(sdata)) {
            Integer balance = JSON.parseObject(sdata, BalanceRedisDto.class).getBalance();
            return balance;
        } else {
            log.info("查询当前账号是否有存在的订单。如果存在就返回余额0");
            DateTime endOfDay = DateUtil.endOfDay(new Date());
            DateTime beginOfDay = DateUtil.beginOfDay(new Date());
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            List<Map<String, Object>> mapList = jdOrderPtMapper.selectDouYinByStartTimeAndEndAndUidGroup(beginOfDay, endOfDay);
            if (CollUtil.isNotEmpty(mapList)) {
                Map<String, Map<String, Object>> pt_pins = mapList.stream().collect(Collectors.toMap(it -> it.get("pt_pin").toString(), it -> it));
                Map<String, Object> stringObjectMap = pt_pins.get(uid);
                if (CollUtil.isNotEmpty(stringObjectMap)) {
                    return PreConstant.ZERO;
                }
            }
            return 200;
        }
    }

    @Scheduled(cron = "0/30 * * * * ?")
    @Async("asyncPool")
    public void callBack() {
//        redisTemplate.opsForValue().setIfAbsent("回调触发器:{}");
    }

    @Scheduled(cron = "0/30 * * * * ?")
    @Async("asyncPool")
    public void blackBai() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("删除黑名单任务", "1", 2, TimeUnit.MINUTES);
        if (!ifAbsent) {
            return;
        }
        Set<String> keys = redisTemplate.keys("IP黑名单:*");
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        log.info("开始重置黑名单");
        List<String> collect = keys.stream().map(it -> it.replace("IP黑名单:", "")).collect(Collectors.toList());
        for (String ip : collect) {
            String data = redisTemplate.opsForValue().get("IP黑名单:" + ip);
            DateTime beginOfDay = DateUtil.beginOfDay(new Date());
            Integer count = jdMchOrderMapper.selectBlackDataByIp(beginOfDay, ip);
            if (count > 0) {
                redisTemplate.delete("IP黑名单:" + ip);
            }
        }

    }

    @Scheduled(cron = "0/30 * * * * ?")
    @Async("asyncPool")
    public void black() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("拉黑名单任务", "1", 3, TimeUnit.MINUTES);
        if (!ifAbsent) {
            return;
        }
        log.info("执行拉黑名单");
        DateTime beginOfDay = DateUtil.beginOfDay(new Date());
        PreTenantContextHolder.setCurrentTenantId(1L);
        List<Map<String, Object>> list = jdMchOrderMapper.selectBlackData(beginOfDay);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (Map<String, Object> map : list) {
            if (ObjectUtil.isNull(map.get("ip"))) {
                continue;
            }
            Integer count = jdMchOrderMapper.selectBlackDataByIp(beginOfDay, map.get("ip").toString());
            String ipuse = redisTemplate.opsForValue().get("IP白名单:" + map.get("ip"));
            if (count == 0 && StrUtil.isBlank(ipuse)) {
                String count1 = map.get("count").toString();
                redisTemplate.opsForValue().set("IP黑名单:" + map.get("ip"), count1);
            } else {
                redisTemplate.delete("IP黑名单:" + map.get("ip"));
            }
        }
    }

    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void budan() {
        PreTenantContextHolder.setCurrentTenantId(1L);
        Integer time = Integer.valueOf(redisTemplate.opsForValue().get("补单时间"));
        List<JdMchOrder> jdMchOrders = jdMchOrderMapper.selectBuDan(time);
        if (CollUtil.isEmpty(jdMchOrders)) {
            return;
        }
        findOrderStatusByOutOrder(jdMchOrders);
    }

    public void findOrderStatusByOutOrder(List<JdMchOrder> jdMchOrders) {
        for (JdMchOrder jdMchOrder : jdMchOrders) {
            try {
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("补单执行:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), 40, TimeUnit.SECONDS);
                if (!ifAbsent) {
                    continue;
                }
                JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?aid=45465&order_id=" + jdOrderPt.getOrderId().trim())
                        .get()
                        .addHeader("Cookie", PreAesUtils.decrypt解密(jdOrderPt.getCurrentCk()))
                        .addHeader("X-Khronos", "1665697911")
                        .addHeader("X-Gorgon", "8404d4860000775655c5b8f6315f8a608a802f3a78e4891a08cc")
                        .addHeader("User-Agent", "okhttp/3.10.0.1")
                        .addHeader("cache-control", "no-cache")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                System.out.println(body);
                if (StrUtil.isBlank(body)) {
                    log.info("对不起，没有查询成");
                    return;
                }
                log.info("订单号:{},查询成功数据:有数据", jdMchOrder.getTradeNo());
                jdOrderPt.setHtml(body);
                jdOrderPt.setOrgAppCk(DateUtil.formatDateTime(new Date()));
                /**
                 *    String html = JSON.parseObject(body).getString("order_detail_info");
                 *             if (StrUtil.isNotBlank(html)) {
                 *                 String shop_order_status_info = JSON.parseObject(html).getString("shop_order_status_info");
                 */
                if (StrUtil.isNotBlank(body) && body.contains("order_detail_info") && body.contains("shop_order_status_info")) {
                    String html = JSON.parseObject(JSON.parseObject(body).getString("order_detail_info")).getString("shop_order_status_info");
                    jdOrderPt.setHtml(html);
                }
                jdOrderPtMapper.updateById(jdOrderPt);
                String html = JSON.parseObject(body).getString("order_detail_info");
                String voucher_info_listStr = JSON.parseObject(html).getString("voucher_info_list");
                List<JSONObject> voucher_info_list = JSON.parseArray(voucher_info_listStr, JSONObject.class);
                if (CollUtil.isEmpty(voucher_info_list)) {
                    log.info("订单号:{},补单没有支付补单成功,没有支付", jdMchOrder.getTradeNo());
                    return;
                }
                JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
                String code = voucher_info.getString("code");
                if (StrUtil.isBlank(code)) {
                    log.info("没有支付");
                    return;
                }
                log.info("订单号:{}支付成功msg:", jdMchOrder.getTradeNo());
                if (StrUtil.isNotBlank(code)) {
                    updateSuccess(jdMchOrder, jdOrderPt, code, client);
                    return;
                }
            } catch (Exception e) {
                log.info("订单补单查询失败:{},{}", jdMchOrder.getTradeNo(), e.getMessage());
            }

        }
    }

    private void updateSuccess(JdMchOrder jdMchOrder, JdOrderPt jdOrderPt, String code, OkHttpClient client) {
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        log.info("订单号{}，当前获取的卡密成功", jdMchOrder.getTradeNo());
        jdOrderPt.setCardNumber(PreAesUtils.encrypt加密(code));
        jdOrderPt.setCarMy(PreAesUtils.encrypt加密(code));
        jdOrderPt.setSuccess(PreConstant.ONE);
        jdOrderPt.setPaySuccessTime(new Date());
        jdOrderPtMapper.updateById(jdOrderPt);
        jdMchOrder.setStatus(PreConstant.TWO);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdMchOrderMapper.updateById(jdMchOrder);
        jdOrderPtMapper.updateById(jdOrderPt);
        jdOrderPtMapper.updateById(jdOrderPt);
        try {
            List<JdLog> jdLogs = jdLogMapper.selectList(Wrappers.<JdLog>lambdaQuery().eq(JdLog::getOrderId, jdMchOrder.getTradeNo()));
            if (CollUtil.isNotEmpty(jdLogs)) {
                log.info("订单号:{}删除redis黑名单:{}", jdMchOrder.getTradeNo(), jdLogs.get(PreConstant.ZERO).getIp());
                redisTemplate.delete("IP黑名单:" + jdLogs.get(PreConstant.ZERO).getIp());
                log.info("删除黑名单成功:{}", jdMchOrder.getTradeNo());
                redisTemplate.opsForValue().set("IP白名单:" + jdLogs.get(PreConstant.ZERO).getIp(), "1", 5, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("删除黑名单报错:{}", jdMchOrder.getTradeNo());
        }
        log.info("执行删除订单msg:{}", jdMchOrder.getTradeNo());
        try {
            isDelete(client, jdMchOrder, jdOrderPt);
        } catch (Exception e) {
            log.info("删除订单报错:{},e：{}", jdMchOrder.getTradeNo(), e.getMessage());
        }

    }

    public void isDelete(OkHttpClient client, JdMchOrder jdMchOrder, JdOrderPt jdOrderPt) {
        jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
        if (jdOrderPt.getActionId().equals(100040)) {
            log.info("当前状态为100040。不需要修改", jdMchOrder.getTradeNo());
            return;
        }
        if (jdOrderPt.getActionId().equals(0)) {
            for (int i = 0; i < 2; i++) {
                Boolean isac100030 = isac100030Zr100040(client, jdMchOrder.getTradeNo(), jdOrderPt.getOrderId(), jdOrderPt.getCurrentCk(), "100030");
                if (isac100030) {
                    log.info("设置使用成功msg:{}", jdMchOrder.getTradeNo());
                    log.info("修改订单号的状态为msg:{}", jdMchOrder.getTradeNo(), "100030");
                    jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
                    jdOrderPt.setActionId(100030);
                    jdOrderPtMapper.updateById(jdOrderPt);
                    break;
                }
            }
        }
        jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
        if (jdOrderPt.getActionId().equals(100030)) {
            for (int i = 0; i < 2; i++) {
                Boolean isac100040 = isac100030Zr100040(client, jdMchOrder.getTradeNo(), jdOrderPt.getOrderId(), jdOrderPt.getCurrentCk(), "100040");
                if (isac100040) {
                    log.info("设置删除成功msg:{}", jdMchOrder.getTradeNo());
                    log.info("修改订单号的状态为msg:{}", jdMchOrder.getTradeNo(), "100040");
                    jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
                    jdOrderPt.setActionId(100040);
                    jdOrderPtMapper.updateById(jdOrderPt);
                    return;
                }
            }
        }
    }

    public Boolean isac100030Zr100040(OkHttpClient client, String tradeNo, String originalTradeNo, String currentCk, String ac) {
        try {
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, String.format("source=1&business_line=2&app_name=aweme&channel=dy_tiny_juyouliang_dy_and24&device_platform=android&order_id=%s&action_id=%s",
                    originalTradeNo, ac));
            Request request = new Request.Builder()
                    .url("https://aweme.snssdk.com/aweme/v1/commerce/order/action/postExec/?aid=1128&channel=dy_tiny_juyouliang_dy_and24&device_platform=android")
                    .post(body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Cookie", PreAesUtils.decrypt解密(currentCk))
                    .build();
            Response response = client.newCall(request).execute();
            String str100030 = response.body().string();
            if (str100030.contains("用户未登录")) {
                return true;
            }
            if (StrUtil.isNotBlank(str100030) && str100030.contains(ac) && JSON.parseObject(str100030).getInteger("status_code") == 0) {
                log.info("订单使用成功:{},msg:{},原始订单号:{}", ac, tradeNo, originalTradeNo);
                return true;
            }
        } catch (Exception e) {
            log.info("提交使用订单ac:{},数据报错msg:{},e:{}", ac, tradeNo, e.getMessage());
        }
        return false;
    }

    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void deleteChoufengShu() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("deleteChoufengShu", "deleteChoufengShu", 15, TimeUnit.SECONDS);
        if (!ifAbsent) {
            return;
        }
        PreTenantContextHolder.setCurrentTenantId(1L);
        LambdaQueryWrapper<JdOrderPt> wrapper = Wrappers.<JdOrderPt>lambdaQuery().gt(JdOrderPt::getCreateTime, DateUtil.beginOfDay(new Date()));
        wrapper.like(JdOrderPt::getHtml, "待发券");
        List<JdOrderPt> jdOrderPts = jdOrderPtMapper.selectList(wrapper);
        if (CollUtil.isEmpty(jdOrderPts)) {
            return;
        }
        Map<String, List<JdOrderPt>> groupByuid = jdOrderPts.stream().collect(Collectors.groupingBy(JdOrderPt::getPtPin));
        Set<String> choufengAccounts = redisTemplate.keys("抽风账号锁定:*");
        if (CollUtil.isNotEmpty(choufengAccounts)) {
            for (String choufengAccount : choufengAccounts) {
                String uid = choufengAccount.replace("抽风账号锁定:", "");
                LambdaQueryWrapper<JdOrderPt> wrapperLockW = Wrappers.<JdOrderPt>lambdaQuery().gt(JdOrderPt::getCreateTime, DateUtil.beginOfDay(new Date())).eq(JdOrderPt::getPtPin, uid);
                wrapperLockW.like(JdOrderPt::getHtml, "待发券");
                List<JdOrderPt> lockStocks = jdOrderPtMapper.selectList(wrapperLockW);
                if (CollUtil.isEmpty(lockStocks) || lockStocks.size() <= 2) {
                    redisTemplate.delete("抽风账号锁定:" + uid);
                    redisTemplate.delete("抖音ck锁定3分钟:" + uid);
                    List<JdOrderPt> jdOrderPtsDbByUid = jdOrderPtMapper.selectList(Wrappers.<JdOrderPt>lambdaQuery()
                            .gt(JdOrderPt::getCreateTime, DateUtil.beginOfDay(new Date())).eq(JdOrderPt::getPtPin, uid).eq(JdOrderPt::getIsWxSuccess, PreConstant.ZERO));
                    if (CollUtil.isNotEmpty(jdOrderPtsDbByUid)) {
                        for (JdOrderPt jdOrderPtByUid : jdOrderPtsDbByUid) {
                            redisTemplate.delete("锁定抖音库存订单:" + jdOrderPtByUid.getId());
                            jdOrderPtByUid.setIsWxSuccess(PreConstant.ONE);
                            jdOrderPtMapper.updateById(jdOrderPtByUid);
                        }
                    }
                }
            }
        }
        for (String uid : groupByuid.keySet()) {
            List<JdOrderPt> jdOrderPtByUids = groupByuid.get(uid);
            if (jdOrderPtByUids.size() >= 3) {
                log.info("uid:{},这批库存全部禁用", uid);
                redisTemplate.delete("老号正在下单:" + uid);
                DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, uid));
                redisTemplate.opsForValue().set("抖音ck锁定3分钟:" + uid, JSON.toJSONString(douyinAppCk), 5, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set("抽风账号锁定:" + uid, uid, 5, TimeUnit.MINUTES);
                List<JdOrderPt> jdOrderPtsDbByUid = jdOrderPtMapper.selectList(Wrappers.<JdOrderPt>lambdaQuery()
                        .gt(JdOrderPt::getCreateTime, DateUtil.beginOfDay(new Date())).eq(JdOrderPt::getPtPin, uid).eq(JdOrderPt::getIsWxSuccess, PreConstant.ONE));
                for (JdOrderPt jdOrderPtByUid : jdOrderPtsDbByUid) {
                    Boolean ifLockStock = redisTemplate.opsForValue().setIfAbsent("锁定抖音库存订单:" + jdOrderPtByUid.getId(), jdOrderPtByUid.getOrderId(),
                            50, TimeUnit.MINUTES);
                    if (ifLockStock) {
                        jdOrderPtByUid.setIsWxSuccess(PreConstant.ZERO);
                        jdOrderPtMapper.updateById(jdOrderPtByUid);
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void deleteOrderData() {
        List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getGroupNum, PreConstant.EIGHT)
                .eq(JdAppStoreConfig::getIsProduct, PreConstant.ONE));
        if (CollUtil.isEmpty(jdAppStoreConfigs)) {
            return;
        }
        List<String> skus = jdAppStoreConfigs.stream().map(it -> it.getSkuId()).collect(Collectors.toList());
        String shijianM = redisTemplate.opsForValue().get("订单删除有效期时间");
        DateTime dateTime = DateUtil.offsetMinute(new Date(), -Integer.valueOf(shijianM));
        LambdaQueryWrapper<JdOrderPt> ac0 = Wrappers.<JdOrderPt>lambdaQuery().gt(JdOrderPt::getCreateTime, dateTime).eq(JdOrderPt::getActionId, 0).isNotNull(JdOrderPt::getCarMy).in(JdOrderPt::getSkuId, skus);
        LambdaQueryWrapper<JdOrderPt> ac100030 = Wrappers.<JdOrderPt>lambdaQuery().gt(JdOrderPt::getCreateTime, dateTime).eq(JdOrderPt::getActionId, 100030).isNotNull(JdOrderPt::getCarMy).in(JdOrderPt::getSkuId, skus);
        List<JdOrderPt> jdOrderPtAc0 = jdOrderPtMapper.selectList(ac0);
        List<JdOrderPt> jdOrderPtAc100030 = jdOrderPtMapper.selectList(ac100030);
        if (CollUtil.isNotEmpty(jdOrderPtAc0)) {
            for (JdOrderPt jdOrderPt : jdOrderPtAc0) {
                taskSetDelete(jdOrderPt);
            }
        }
        if (CollUtil.isNotEmpty(jdOrderPtAc100030)) {
            for (JdOrderPt jdOrderPt : jdOrderPtAc100030) {
                taskSetDelete(jdOrderPt);
            }
        }
    }

    private void taskSetDelete(JdOrderPt jdOrderPt) {
        OkHttpClient client = pcAppStoreService.buildClient();
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("删除订单任务:" + jdOrderPt.getOrderId(), "查询订单删除", 20, TimeUnit.SECONDS);
        if (!ifAbsent) {
            return;
        }
        JdMchOrder jdMchOrder = jdMchOrderMapper.selectOne(Wrappers.<JdMchOrder>lambdaQuery().eq(JdMchOrder::getOriginalTradeNo, jdOrderPt.getOrderId()));
        if (ObjectUtil.isNull(jdMchOrder)) {
            return;
        }
        isDelete(client, jdMchOrder, jdOrderPt);
    }

    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void synProductMaxPrirce() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("同步订单金额", "2", 30, TimeUnit.SECONDS);
        if (!ifAbsent) {
            return;
        }
        PreTenantContextHolder.setCurrentTenantId(1L);
        Integer maxPrice = douyinMaxPrice();
        DateTime endOfDay = DateUtil.endOfDay(new Date());
        DateTime beginOfDay = DateUtil.beginOfDay(new Date());
        List<Map<String, Object>> mapList = jdOrderPtMapper.selectDouYinByStartTimeAndEndAndUidGroup(beginOfDay, endOfDay);
        List<DouyinAppCk> douyinAppCks = douyinAppCkMapper.selectList(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getIsEnable, PreConstant.ONE));
        List<String> skuyesterdays = jdOrderPtMapper.selectOrderSuccessYesterday(beginOfDay);
        log.info("抖音定时任务同步订单金额");
        if (CollUtil.isNotEmpty(douyinAppCks)) {
            for (DouyinAppCk douyinAppCk : douyinAppCks) {
                Integer sku_price_total = PreConstant.ZERO;
                String pt_pin = douyinAppCk.getUid();
                if (CollUtil.isNotEmpty(mapList)) {
                    Map<String, Map<String, Object>> pt_pins = mapList.stream().collect(Collectors.toMap(it -> it.get("pt_pin").toString(), it -> it));
                    Map<String, Object> stringObjectMap = pt_pins.get(pt_pin);
                    if (CollUtil.isNotEmpty(stringObjectMap)) {
                        sku_price_total = new BigDecimal(stringObjectMap.get("sku_price_total").toString()).intValue();
                    }
                }
//                String pt_pin = data.get("pt_pin").toString();
//                Integer sku_price_total = new BigDecimal(data.get("sku_price_total").toString()).intValue();
                if (CollUtil.isNotEmpty(skuyesterdays) && skuyesterdays.contains(pt_pin)) {
                    BalanceRedisDto build = BalanceRedisDto.builder().uid(pt_pin).balance((maxPrice - sku_price_total)).build();
                    redisTemplate.opsForValue().set("抖音各个账号剩余额度:" + pt_pin, JSON.toJSONString(build));
                } else if (douyinAppCk.getIsOld() == PreConstant.ONE) {
                    BalanceRedisDto build = BalanceRedisDto.builder().uid(pt_pin).balance((maxPrice - sku_price_total)).build();
                    redisTemplate.opsForValue().set("抖音各个账号剩余额度:" + pt_pin, JSON.toJSONString(build));
                } else {
                    if (sku_price_total == 100) {
                        BalanceRedisDto build = BalanceRedisDto.builder().uid(pt_pin).balance((100 - sku_price_total)).build();
                        redisTemplate.opsForValue().set("抖音各个账号剩余额度:" + pt_pin, JSON.toJSONString(build));
                    } else {
                        BalanceRedisDto build = BalanceRedisDto.builder().uid(pt_pin).balance((200 - sku_price_total)).build();
                        redisTemplate.opsForValue().set("抖音各个账号剩余额度:" + pt_pin, JSON.toJSONString(build));
                    }
                }
            }
        }
    }

    private Integer douyinMaxPrice() {
        String douyinMaxPrice = redisTemplate.opsForValue().get("抖音苹果卡最大下单金额");
        if (StrUtil.isBlank(douyinMaxPrice)) {
            redisTemplate.opsForValue().set("抖音苹果卡最大下单金额", "5000");
            return 5000;
        } else {
            return Integer.valueOf(douyinMaxPrice);
        }
    }

}
