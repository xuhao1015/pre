package com.xd.pre.modules.px.douyin;

import cn.hutool.core.bean.BeanUtil;
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
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import com.xd.pre.modules.px.douyin.pay.BalanceRedisDto;
import com.xd.pre.modules.px.douyin.pay.PayDto;
import com.xd.pre.modules.px.douyin.submit.DouyinAsynCService;
import com.xd.pre.modules.px.douyin.toutiao.BuildDouYinUrlUtils;
import com.xd.pre.modules.px.douyin.toutiao.BuyRenderParam;
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

    @Resource
    private DouyinMethodNameParamMapper douyinMethodNameParamMapper;

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

    public Boolean checkIp(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog) {
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

    public R douyinUseStock(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog, TimeInterval timer, OkHttpClient client, List<JdOrderPt> jdOrderPtStocks, String payReUrl) {
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        JdOrderPt jdOrderPtDb = jdOrderPtStocks.get(PreUtils.randomCommon(0, jdOrderPtStocks.size() - 1, 1)[0]);
        if (jdOrderPtStocks.size() >= 15) {
            int[] ints = PreUtils.randomCommon(0, jdOrderPtStocks.size() - 1, 10);
            for (int i = 0; i < ints.length; i++) {
                int anInt = ints[i];
                JdOrderPt jdOrderPtT = jdOrderPtStocks.get(anInt);
                if (jdOrderPtT.getId() < jdOrderPtDb.getId()) {
                    jdOrderPtDb = jdOrderPtT;
                }
            }
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
        Integer lockDouYinCkTime = Integer.valueOf(redisTemplate.opsForValue().get("抖音ck锁定秒数"));
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
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("抖音ck锁定3分钟:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockDouYinCkTime, TimeUnit.SECONDS);
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
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("抖音ck锁定3分钟:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockDouYinCkTime, TimeUnit.SECONDS);
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
        DateTime beginOfDay = DateUtil.beginOfDay(new Date());
        DateTime dateTime0 = DateUtil.offsetMinute(beginOfDay, 0);
        String 首单开始跑时间 = redisTemplate.opsForValue().get("首单开始跑时间");
        DateTime dateTime4 = DateUtil.offsetMinute(beginOfDay, Integer.valueOf(首单开始跑时间));
        if (System.currentTimeMillis() > dateTime0.getTime() && System.currentTimeMillis() < dateTime4.getTime()) {
            long l = (dateTime4.getTime() - System.currentTimeMillis()) / 1000;
            redisTemplate.opsForValue().set("抖音ck锁定3分钟:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), l, TimeUnit.SECONDS);
            log.info("当前不跑首单号锁定号uid:{}", douyinAppCk.getUid());
            return null;
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

    public void buildNotUseAccout(JdAppStoreConfig storeConfig, LambdaQueryWrapper<DouyinAppCk> wrapper, String no) {
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
                log.debug("订单号{},不能使用的账号:{}", no, JSON.toJSONString(noUseData));
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

    private String buildCreatepay(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameCreatenew) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String newcreate_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameCreatenew, douyinAppCk);
            String newcreate_body = BuildDouYinUrlUtils.buildCreatepay(gidAndShowdPrice, douyinAppCk);
            log.info("请求参数:{}", newcreate_body);
            String create_md5 = SecureUtil.md5("json_form=" + URLEncoder.encode(newcreate_body)).toUpperCase();
            String create_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    create_md5, douyinAppCk.getDeviceId(), newcreate_url
            );
            String create_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(create_body_sign).execute().body();
            String create_x_gorgon = JSON.parseObject(create_sign_body).getString("x-gorgon");
            String create_x_khronos = JSON.parseObject(create_sign_body).getString("x-khronos");
            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", newcreate_body)
                    .build();
            Request.Builder builder = new Request.Builder();
            Request request_create = builder.url(newcreate_url)
                    .post(requestBody1)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", create_md5)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", create_x_gorgon)
                    .addHeader("X-Khronos", create_x_khronos)
                    .build();
            Response execute = client.newCall(request_create).execute();
            String createbody = execute.body().string();
            log.info("支付数据数据:{}", createbody);
            return createbody;
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;
    }

    public String payByOrderId(OkHttpClient client, PayDto payDto, JdLog jdLog, JdMchOrder jdMchOrder) {
        try {
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            GidAndShowdPrice gidAndShowdPrice = new GidAndShowdPrice();
            gidAndShowdPrice.setOrderId(payDto.getOrderId());
            gidAndShowdPrice.setPayIp(jdLog.getIp());
            gidAndShowdPrice.setDecision_id("2779179332155096_1671023222741733");
            JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, payDto.getOrderId()));
            DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, jdOrderPt.getPtPin()));
            DouyinMethodNameParam methodNameCreatepay = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "createpay"));
            String payData = buildCreatepay(gidAndShowdPrice, douyinAppCk, client, methodNameCreatepay);
            if (payData.contains("订单已被支付")) {
                jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, payDto.getOrderId()));
                jdOrderPt.setWxPayExpireTime(DateUtil.offsetMinute(new Date(), -100));
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                jdOrderPtMapper.updateById(jdOrderPt);
                return null;
            }
            log.info("订单号{}，原始订单号:{}支付消息返回数据msg:{}", jdMchOrder.getTradeNo(), payDto.getOrderId(), payData);
            if (StrUtil.isNotBlank(payData) && payData.contains("订单不能被支付")) {
                jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, payDto.getOrderId()));
                jdOrderPt.setWxPayExpireTime(DateUtil.offsetMinute(new Date(), -100));
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                jdOrderPt.setIsWxSuccess(PreConstant.ZERO);
                jdOrderPtMapper.updateById(jdOrderPt);
                return null;
            }
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
            String local = redisTemplate.opsForValue().get("服务器地址");
            //alipays://platformapi/startapp?appId=20000067&url=http%3A%2F%2F134.122.134.69%3A8082%2Frecharge%2Fzfb%3Forder_id%3DSP2210012316069040391319127864
            String payReUrl = String.format("alipays://platformapi/startapp?appId=20000067&url=%s",
                    URLEncoder.encode("http://" + local + ":7891/alipay/payHtml?orderId=" + jdMchOrder.getTradeNo()));
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

    private String geSuccessOrder(OkHttpClient client, BuyRenderParamDto buyRenderParamDto, Integer payType, DouyinAppCk douyinAppCk, JdLog jdLog, JdMchOrder jdMchOrder, TimeInterval timer,
                                  String phone, DouyinDeviceIid douyinDeviceIid) {
        try {
            log.info("订单号:{},锁定设备号:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
               /* Boolean isLockDeviceId = redisTemplate.opsForValue().setIfAbsent("抖音锁定设备:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 1, TimeUnit.MINUTES);
                if (!isLockDeviceId) {
                    log.info("订单号{}，当前设备号已经锁定:deviceId:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
                    continue;
                }*/
            GidAndShowdPrice gidAndShowdPrice = getAndBuildBuyRender(client, douyinAppCk, buyRenderParamDto, jdMchOrder, jdLog);
            log.info("订单号:{},循环次数,预下单时间戳:{}", jdMchOrder.getTradeNo(), timer.interval());
            if (ObjectUtil.isNull(gidAndShowdPrice)) {
                log.info("订单号{}，预下单失败", jdMchOrder.getTradeNo());
                return null;
            }
            if (ObjectUtil.isNotNull(gidAndShowdPrice) && ObjectUtil.isNotNull(gidAndShowdPrice.getCheckIp()) && gidAndShowdPrice.getCheckIp()) {
                return "checkIp";
            }
            gidAndShowdPrice.setPost_tel(phone);
            DouyinMethodNameParam methodNameCreatenew = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery()
                    .eq(DouyinMethodNameParam::getMethodName, "newcreate"));
            String bodyRes1 = buildCreateOrder(gidAndShowdPrice, douyinAppCk, client, methodNameCreatenew);
            return bodyRes1;
        } catch (Exception e) {
            log.error("订单号msg:{},失败:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;
    }

    private String buildCreateOrder(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameCreatenew) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String newcreate_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameCreatenew, douyinAppCk);
            //TODO
            String newcreate_body = BuildDouYinUrlUtils.buildCreatenew(gidAndShowdPrice, douyinAppCk);
            log.info("请求参数:{}", newcreate_body);
            String create_md5 = SecureUtil.md5("json_form=" + URLEncoder.encode(newcreate_body)).toUpperCase();
            String create_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    create_md5, douyinAppCk.getDeviceId(), newcreate_url
            );
            String create_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(create_body_sign).execute().body();
            String create_x_gorgon = JSON.parseObject(create_sign_body).getString("x-gorgon");
            String create_x_khronos = JSON.parseObject(create_sign_body).getString("x-khronos");
            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", newcreate_body)
                    .build();
            Request.Builder builder = new Request.Builder();
            Request request_create = builder.url(newcreate_url)
                    .post(requestBody1)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", create_md5)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", create_x_gorgon)
                    .addHeader("X-Khronos", create_x_khronos)
                    .build();
            Response execute = client.newCall(request_create).execute();
            String createbody = execute.body().string();
            log.info("下单数据:{}", createbody);
            return createbody;
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;
    }


    public List<PayDto> createOrder(OkHttpClient client, BuyRenderParamDto buyRenderParamDto, Integer payType,
                                    DouyinAppCk douyinAppCk, JdLog jdLog, JdMchOrder jdMchOrder, TimeInterval timer, String phone, JdAppStoreConfig storeConfig) {
        List<PayDto> payDtos = new ArrayList<>();
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        List<DouyinDeviceIid> douyinDeviceIids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DouyinDeviceIid douyinDeviceIid = DouyinDeviceIid.builder().deviceId(douyinAppCk.getDeviceId()).iid(douyinAppCk.getIid()).build();
            douyinDeviceIids.add(douyinDeviceIid);
        }
        Integer sufMeny = 0;
        for (DouyinDeviceIid douyinDeviceIid : douyinDeviceIids) {
            douyinAppCk = douyinAppCkMapper.selectById(douyinAppCk.getId());
            try {
                sufMeny = getSufMeny(douyinAppCk.getUid(), jdMchOrder);
                if (sufMeny - new BigDecimal(jdMchOrder.getMoney()).intValue() < 0) {
                    log.info("订单号:当前ck出现了余额不足的情况", jdMchOrder.getTradeNo());
                    redisTemplate.delete("老号正在下单:" + douyinAppCk.getUid());
                    synProductMaxPrirce();
                    return null;
                }
                String bodyRes1 = geSuccessOrder(client, buyRenderParamDto, payType, douyinAppCk, jdLog, jdMchOrder, timer, phone, douyinDeviceIid);
                if (StrUtil.isNotBlank(bodyRes1) & bodyRes1.equals("checkIp")) {
                    client = pcAppStoreService.buildClient();
                    continue;
                }
                if (bodyRes1.contains("order_id")) {
                    log.info("当前成功:{}", jdMchOrder.getTradeNo());
                    douyinAppCk.setSuccessTime(new Date());
                    douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
                    douyinAppCk.setUpdateTime(new Date());
                    douyinAppCkMapper.updateById(douyinAppCk);
                    log.info("订单号:{},设备号重复使用查询和删除", jdMchOrder.getTradeNo());
//                    deleteLockCk(douyinAppCk, douyinDeviceIid);
                    log.info("订单号:{},当前设备号和uid绑定其他人不能使用msg:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getId());
                    log.info("订单号{}，下单成功", jdMchOrder);
                    String orderId = JSON.parseObject(JSON.parseObject(bodyRes1).getString("data")).getString("order_id");
                    log.info("订单号{}，当前订单号msg:{}", jdMchOrder.getTradeNo(), orderId);
                    douyinDeviceIid.setSuccess(douyinDeviceIid.getSuccess() == null ? 1 : douyinDeviceIid.getSuccess() + 1);
                    log.info("订单号:{}设置上次成功时间msg:{}", jdMchOrder.getTradeNo(), new Date().toLocaleString());
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
                    douyinAppCk.setFailReason(douyinAppCk.getFailReason() + bodyRes1);
                    douyinAppCk.setCk(PreAesUtils.encrypt加密(douyinAppCk.getCk()));
                    douyinAppCk.setUpdateTime(new Date());
                    douyinAppCkMapper.updateById(douyinAppCk);
                }
            } catch (Exception e) {
                log.error("订单号{}，当前抖音报错:{},时间戳:{}", jdMchOrder.getTradeNo(), e.getMessage(), timer.interval());
            }
        }
        if (CollUtil.isEmpty(payDtos)) {
            log.info("订单号{}，当前下单失败,请查看原因", jdMchOrder.getTradeNo());
            return null;
        }
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


    private GidAndShowdPrice buildBuRender(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameBuyRender) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String buyRenderUrl = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameBuyRender, douyinAppCk);
            String buyRenderBody = BuildDouYinUrlUtils.buildBuyRender(gidAndShowdPrice);
            String buyRenderMd5 = SecureUtil.md5("json_form=" + URLEncoder.encode(buyRenderBody)).toUpperCase();

            String pack_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    buyRenderMd5, douyinAppCk.getDeviceId(), buyRenderUrl
            );
            String pack_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(pack_body_sign).execute().body();
            String buyRender_x_gorgon = JSON.parseObject(pack_sign_body).getString("x-gorgon");
            String buyRender_x_khronos = JSON.parseObject(pack_sign_body).getString("x-khronos");
            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", buyRenderBody)
                    .build();
            Request.Builder builder = new Request.Builder();
            Request requestBuyRender = builder.url(buyRenderUrl)
                    .post(requestBody1)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", buyRenderMd5)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", buyRender_x_gorgon)
                    .addHeader("X-Khronos", buyRender_x_khronos)
                    .build();
            Response execute = client.newCall(requestBuyRender).execute();
            String buyRenderbody = execute.body().string();
            log.info("预下单数据:{}", buyRenderbody);
            execute.close();

            String zg_ext_info_str = JSON.parseObject(buyRenderbody).getJSONObject("data").getJSONObject("pay_method").getString("zg_ext_info");
            BeanUtil.copyProperties(JSON.parseObject(zg_ext_info_str), gidAndShowdPrice);
            BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(buyRenderbody).getString("data"), BuyRenderRoot.class);
            String decision_id = buyRenderRoot.getPay_method().getDecision_id();
            String payapi_cache_id = buyRenderRoot.getPay_method().getPayapi_cache_id();
            String render_token = buyRenderRoot.getRender_token();
            String render_track_id = buyRenderRoot.getRender_track_id();
            String marketing_plan_id = buyRenderRoot.getTotal_price_result().getMarketing_plan_id();
            gidAndShowdPrice.setDecision_id(decision_id);
            gidAndShowdPrice.setPayapi_cache_id(payapi_cache_id);
            gidAndShowdPrice.setRender_token(render_token);
            gidAndShowdPrice.setRender_track_id(render_track_id);
            gidAndShowdPrice.setMarketing_plan_id(marketing_plan_id);

            log.info("处理价格");
            JSONObject total_price_result_json = JSON.parseObject(buyRenderbody).getJSONObject("data").getJSONObject("total_price_result");
            Integer total_amount = total_price_result_json.getInteger("total_amount");
            gidAndShowdPrice.setTotal_amount(total_amount);
            Integer total_origin_amount = total_price_result_json.getInteger("total_origin_amount");
            gidAndShowdPrice.setTotal_origin_amount(total_origin_amount);
            Integer total_coupon_amount = total_price_result_json.getInteger("total_coupon_amount");
            gidAndShowdPrice.setTotal_coupon_amount(total_coupon_amount);

            if (total_origin_amount.intValue() != total_amount.intValue()) {
                log.info("当前数据有优惠卷");
                JSONObject coupon_info = total_price_result_json.getJSONObject("shop_sku_map").getJSONObject("GceCTPIk").getJSONObject("sku_list")
                        .getJSONObject(gidAndShowdPrice.getSku_id()).getJSONObject("shop_discount").getJSONObject("coupon_info");
                String coupon_info_id = coupon_info.getString("id");
                String coupon_meta_id = coupon_info.getString("coupon_meta_id");
                gidAndShowdPrice.setCoupon_info_id(coupon_info_id);
                gidAndShowdPrice.setCoupon_meta_id(coupon_meta_id);
            }
            System.err.println(JSON.toJSONString(gidAndShowdPrice));
            return gidAndShowdPrice;
        } catch (Exception e) {
            log.error("预下单报错msg:{}", e.getMessage());
        }
        return null;
    }


    public GidAndShowdPrice getAndBuildBuyRender(OkHttpClient client, DouyinAppCk douyinAppCk, BuyRenderParamDto buyRenderParamDto, JdMchOrder jdMchOrder, JdLog jdLog) {
        try {
            GidAndShowdPrice gidAndShowdPrice = new GidAndShowdPrice();
            gidAndShowdPrice.setPost_tel(PreUtils.getTel());
            gidAndShowdPrice.setPayIp(jdLog.getIp());
            gidAndShowdPrice.setEcom_scene_id(buyRenderParamDto.getEcom_scene_id());
            gidAndShowdPrice.setProduct_id(buyRenderParamDto.getProduct_id());
            gidAndShowdPrice.setSku_id(buyRenderParamDto.getSku_id());
            DouyinMethodNameParam methodNameBuyRender = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "buyRender"));
            gidAndShowdPrice = buildBuRender(gidAndShowdPrice, douyinAppCk, client, methodNameBuyRender);
            return gidAndShowdPrice;
        } catch (Exception e) {
            if (StrUtil.isNotBlank(e.getMessage()) && e.getMessage().contains("out")) {
                log.error("订单号:{},预下单超时，切换client", jdMchOrder.getTradeNo());
                GidAndShowdPrice gidAndShowdPrice = new GidAndShowdPrice();
                gidAndShowdPrice.setCheckIp(true);
                return gidAndShowdPrice;
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
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, jdOrderPt.getPtPin()));
            DouyinMethodNameParam methodNameDetailInfo = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "detailInfo"));
            String info_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameDetailInfo, douyinAppCk) + "&order_id=" + jdOrderPt.getOrderId();

            String x_gorgon = "8404d4860000775655c5b8f6315f8a608a802f3a78e4891a08cc";
            String x_khronos = "1665697911";
            try {
                String signData1 = String.format("{\"header\": {\"X-SS-STUB\": \"\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}", info_url);
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
                    .url(info_url)
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

    //    @Scheduled(cron = "0/30 * * * * ?")
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
                JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, jdMchOrder.getOriginalTradeNo()));
                BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
                DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, jdOrderPt.getPtPin()));
                DouyinMethodNameParam methodNameDetailInfo = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "detailInfo"));
                String info_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameDetailInfo, douyinAppCk) + "&order_id=" + jdOrderPt.getOrderId();
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(info_url)
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
        if (jdMchOrder.getPassCode().equals("8") && !jdOrderPt.getHrefUrl().contains(jdMchOrder.getTradeNo())) {
            return;
        }
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
        DouyinAppCk douyinAppCk = douyinAppCkMapper.selectOne(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getUid, jdOrderPt.getPtPin()));
        jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
        DouyinMethodNameParam methodNamemethod_postExec = douyinMethodNameParamMapper.selectOne(Wrappers.<DouyinMethodNameParam>lambdaQuery().eq(DouyinMethodNameParam::getMethodName, "postExec"));

        if (jdOrderPt.getActionId().equals(100040)) {
            log.info("当前状态为100040。不需要修改", jdMchOrder.getTradeNo());
            return;
        }
        if (jdOrderPt.getActionId().equals(0)) {
            for (int i = 0; i < 2; i++) {
                Boolean isac100030 = isac100030Zr100040(client, jdMchOrder.getTradeNo(), jdOrderPt.getOrderId(), jdOrderPt.getCurrentCk(), "100030", douyinAppCk, methodNamemethod_postExec);
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

                Boolean isac100040 = isac100030Zr100040(client, jdMchOrder.getTradeNo(), jdOrderPt.getOrderId(), jdOrderPt.getCurrentCk(), "100040", douyinAppCk, methodNamemethod_postExec);
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

    public Boolean isac100030Zr100040(OkHttpClient client, String tradeNo, String originalTradeNo, String currentCk, String ac, DouyinAppCk douyinAppCk, DouyinMethodNameParam methodNamemethod_postExec) {
        try {


            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String postExec_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNamemethod_postExec, douyinAppCk);
            String datafromOri = "common_params=%7B%22enter_from%22%3A%22order_list_page%22%2C%22previous_page%22%3A%22mine_tab_order_list__order_homepage%22%7D&action_id=" + ac
                    + "&business_line=2&trade_type=0&source=1&ecom_appid=7386&lynx_support_version=1&order_id=" + originalTradeNo + "&page_size=15";
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, datafromOri);
            Request request = new Request.Builder()
                    .url(postExec_url)
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

    @Scheduled(cron = "0/40 * * * * ?")
    @Async("asyncPool")
    public void freeOrderStock() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("freeOrderStock", "freeOrderStock", 20, TimeUnit.SECONDS);
        if (!ifAbsent) {
            return;
        }
        PreTenantContextHolder.setCurrentTenantId(1L);
        DateTime dateTimes = DateUtil.offsetMinute(new Date(), -5);
        DateTime dateTimee = DateUtil.offsetMinute(new Date(), -30);
        LambdaQueryWrapper<JdMchOrder> notClickPay = Wrappers.<JdMchOrder>lambdaQuery().gt(JdMchOrder::getCreateTime, dateTimee).lt(JdMchOrder::getCreateTime, dateTimes)
                .isNotNull(JdMchOrder::getOriginalTradeId)
                .isNull(JdMchOrder::getClickPay);
        List<JdMchOrder> jdMchOrders = jdMchOrderMapper.selectList(notClickPay);
        if (CollUtil.isEmpty(jdMchOrders)) {
            return;
        }
        for (JdMchOrder jdMchOrder : jdMchOrders) {
            Boolean ifAbsent1 = redisTemplate.opsForValue().setIfAbsent("删除订单库存:" + jdMchOrder.getId(), JSON.toJSONString(jdMchOrder), 40, TimeUnit.MINUTES);
            if (!ifAbsent1) {
                continue;
            }
            redisTemplate.delete("锁定抖音库存订单:" + jdMchOrder.getOriginalTradeId());
            JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
            jdOrderPt.setHrefUrl("");
            this.jdOrderPtMapper.updateById(jdOrderPt);
        }
    }

    @Scheduled(cron = "0/10 * * * * ?")
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
                if (CollUtil.isEmpty(lockStocks) || lockStocks.size() <= 1) {
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
            if (jdOrderPtByUids.size() >= 2) {
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

    @Scheduled(cron = "0/15 * * * * ?")
    @Async("asyncPool")
    public void synProductMaxPrirce() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("同步订单金额", "2", 10, TimeUnit.SECONDS);
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
                if (douyinAppCk.getIsOld() == PreConstant.ONE) {
                    BalanceRedisDto build = BalanceRedisDto.builder().uid(pt_pin).balance((maxPrice - sku_price_total)).build();
                    redisTemplate.opsForValue().set("抖音各个账号剩余额度:" + pt_pin, JSON.toJSONString(build));
                    continue;
                }
                if (CollUtil.isNotEmpty(skuyesterdays) && skuyesterdays.contains(pt_pin)) {
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
