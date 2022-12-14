//package com.xd.pre.modules.px.douyin;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.date.TimeInterval;
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.core.util.StrUtil;
//import com.alibaba.fastjson.JSON;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.core.toolkit.Wrappers;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import com.xd.pre.common.constant.PreConstant;
//import com.xd.pre.common.utils.R;
//import com.xd.pre.common.utils.px.PreUtils;
//import com.xd.pre.modules.data.tenant.PreTenantContextHolder;
//import com.xd.pre.modules.px.appstorePc.PcAppStoreService;
//import com.xd.pre.modules.px.douyin.pay.BalanceRedisDto;
//import com.xd.pre.modules.px.douyin.submit.DouyinAsynCService;
//import com.xd.pre.modules.px.task.ProductProxyTask;
//import com.xd.pre.modules.sys.domain.*;
//import com.xd.pre.modules.sys.mapper.*;
//import lombok.extern.slf4j.Slf4j;
//import okhttp3.OkHttpClient;
//import org.apache.activemq.ScheduledMessage;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.jms.core.JmsMessagingTemplate;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import javax.jms.Destination;
//import javax.jms.Queue;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class JinRiTouTiaoService {
//    @Resource
//    private DouyinAppCkMapper douyinAppCkMapper;
//
//
//    @Resource
//    private JdAppStoreConfigMapper jdAppStoreConfigMapper;
//
//    @Resource
//    private DouyinDeviceIidMapper douyinDeviceIidMapper;
//
//    @Autowired
//    private PcAppStoreService pcAppStoreService;
//    @Autowired
//    private StringRedisTemplate redisTemplate;
//    @Resource
//    private JdOrderPtMapper jdOrderPtMapper;
//    @Resource
//    private JdMchOrderMapper jdMchOrderMapper;
//    @Resource
//    private JdProxyIpPortMapper jdProxyIpPortMapper;
//    @Autowired
//    @Lazy()
//    private ProductProxyTask productProxyTask;
//    @Autowired
//    @Lazy()
//    private DouyinAsynCService douyinAsynCService;
//    @Resource(name = "product_douyin_stock_queue")
//    private Queue product_douyin_stock_queue;
//    @Autowired
//    private JmsMessagingTemplate jmsMessagingTemplate;
//
//    @Autowired
//    @Lazy
//    private DouyinService douyinService;
//
//    public R match(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog) {
//        Boolean checkIp = douyinService.checkIp(jdMchOrder, storeConfig, jdLog);
//        if (!checkIp) {
//            return null;
//        }
//        try {
//            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
//            storeConfig.setMark(jdMchOrder.getTenantId() + "");
//            TimeInterval timer = DateUtil.timer();
//            log.info("订单号{}，用户ip:{},开始匹配今日头条订单", jdMchOrder.getTradeNo(), JSON.toJSONString(jdLog));
//            OkHttpClient client = pcAppStoreService.buildClient();
//            if (ObjectUtil.isNull(client)) {
//                client = pcAppStoreService.buildClient();
//            }
//            LambdaQueryWrapper<JdOrderPt> stockWrapper = Wrappers.lambdaQuery();
//            stockWrapper.gt(JdOrderPt::getCreateTime, DateUtil.offsetMinute(new Date(), -storeConfig.getPayIdExpireTime()));
//            stockWrapper.gt(JdOrderPt::getWxPayExpireTime, new Date());
//            stockWrapper.eq(JdOrderPt::getIsWxSuccess, PreConstant.ONE);
//            stockWrapper.eq(JdOrderPt::getSkuPrice, storeConfig.getSkuPrice());
//            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
//            Set<String> stockNums = redisTemplate.keys("今日头条库存:*");
//            if (CollUtil.isNotEmpty(stockNums)) {
//                stockWrapper.notIn(JdOrderPt::getId, stockNums.stream().map(it -> it.split(":")[1]).collect(Collectors.toList()));
//            }
//            stockWrapper.isNull(JdOrderPt::getPaySuccessTime);
//            List<JdOrderPt> jdOrderPtStocks = jdOrderPtMapper.selectList(stockWrapper);
//            String payReUrl = "";
//            JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
//            if (CollUtil.isNotEmpty(jdOrderPtStocks) && ObjectUtil.isNotNull(jdMchOrderDb)) {
//                log.info("订单号:{}.使用库存", jdMchOrder.getTradeNo());
//                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 3);
//                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 3);
//                return douyinService.douyinUseStock(jdMchOrder, storeConfig, jdLog, timer, client, jdOrderPtStocks, payReUrl);
//            } else {
//                log.info("订单号:{}.异步生成一下订单", jdMchOrder.getTradeNo());
//                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 5);
//                sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 15);
//                log.info("订单号:{},新下单", jdMchOrder.getTradeNo());
//                return douyinProductNewOrder(jdMchOrder, storeConfig, jdLog, timer, client);
//            }
//        } catch (Exception e) {
//            log.error("匹配报错:{},信息:{}", jdMchOrder.getTradeNo(), e.getMessage());
//        }
//        return null;
//    }
//
//    private R douyinProductNewOrder(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog, TimeInterval timer, OkHttpClient client) {
//        log.info("异步生成今日头条订单:{}", jdMchOrder.getTradeNo());
//        DouyinAppCk douyinAppCk = randomDouyinAppCk(jdMchOrder, storeConfig);
//        return null;
//    }
//
//    private DouyinAppCk randomDouyinAppCk(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig) {
//        String lockCkTime = redisTemplate.opsForValue().get("今日头条锁定分钟数");
//        LambdaQueryWrapper<DouyinAppCk> wrapper = Wrappers.<DouyinAppCk>lambdaQuery();
//        wrapper.eq(DouyinAppCk::getIsEnable, PreConstant.ONE);
//        Set<String> keys = redisTemplate.keys("今日头条锁定账号:*");
//        if (CollUtil.isNotEmpty(keys)) {
//            buildNotUseAccout(storeConfig, wrapper, jdMchOrder.getTradeNo());
//        }
//        log.info("订单号:{},额度是否今日头条判断额度是否满了", jdMchOrder.getTradeNo());
//        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
//        Integer count = douyinAppCkMapper.selectCount(wrapper);
//        int pageIndex = PreUtils.randomCommon(0, count, 1)[0];
//        if (count == 0) {
//            log.info("订单号:{}，没有ck，请导入ck", jdMchOrder.getTradeNo());
//            return null;
//        }
//        List<Integer> accounts = new ArrayList<>();
//        if (count >= 5) {
//            int[] ints = PreUtils.randomCommon(0, count, 4);
//            for (int anInt : ints) {
//                accounts.add(anInt);
//            }
//            accounts = accounts.stream().sorted().collect(Collectors.toList());
//            pageIndex = accounts.get(PreConstant.ZERO);
//        }
//        if (storeConfig.getGroupNum() == PreConstant.TWENTY && CollUtil.isNotEmpty(accounts)) {
//            DouyinAppCk douyinAppCk = null;
//            Integer let = Integer.valueOf(redisTemplate.opsForValue().get("抖音苹果卡最大下单金额"));
//            Integer maxEd = let;
//            for (Integer account : accounts) {
//                Page<DouyinAppCk> douyinAppCkPage = new Page<>(account, PreConstant.ONE);
//                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
//                douyinAppCkPage = douyinAppCkMapper.selectPage(douyinAppCkPage, wrapper);
//                DouyinAppCk douyinAppCkT = douyinAppCkPage.getRecords().get(PreConstant.ZERO);
//                String edStr = redisTemplate.opsForValue().get("今日头条各个账号剩余额度:" + douyinAppCkT.getUid());
//                String ed = null;
//                if (StrUtil.isNotBlank(edStr)) {
//                    ed = JSON.parseObject(edStr, BalanceRedisDto.class).getBalance() + "";
//                }
//                if (StrUtil.isNotBlank(ed) && Integer.valueOf(ed) >= storeConfig.getSkuPrice().intValue()) {
//                    if (let >= Integer.valueOf(ed) && Integer.valueOf(ed) == 200) {
//                        douyinAppCk = douyinAppCkT;
//                        break;
//                    }
//                    if (let >= Integer.valueOf(ed) && Integer.valueOf(ed) <= maxEd) {
//                        // 8000
//                        maxEd = Integer.valueOf(ed);//8000
//                        douyinAppCk = douyinAppCkT;
//                    }
//                }
//            }
//            if (ObjectUtil.isNotNull(douyinAppCk)) {
//                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("今日头条锁定分钟数:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockCkTime, TimeUnit.MINUTES);
//                if (ifAbsent) {
//                    return douyinAppCk;
//                }
//            }
//
//        }
//        return null;
//    }
//
//    // 发送消息，destination是发送到的队列，message是待发送的消息
//    private void sendMessageSenc(Destination destination, final String message, Integer minit) {
//        Map<String, Object> headers = new HashMap<>();
//        //发送延迟队列，延迟10秒,单位毫秒
//        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 1000);
//        jmsMessagingTemplate.convertAndSend(destination, message, headers);
//    }
//
//    public void buildNotUseAccout(JdAppStoreConfig storeConfig, LambdaQueryWrapper<DouyinAppCk> wrapper, String no) {
//        Set<String> edus = redisTemplate.keys("今日头条各个账号剩余额度:*");
//        Set<String> locks = redisTemplate.keys("今日头条锁定账号:*");
//        if (CollUtil.isNotEmpty(locks)) {
//            List<String> locksData = locks.stream().map(it -> it.replace("今日头条锁定账号:", "")).collect(Collectors.toList());
//            wrapper.notIn(DouyinAppCk::getUid, locksData);
//        }
//        List<String> allData = redisTemplate.opsForValue().multiGet(edus);
//        Map<String, Integer> balanceMap = allData.stream().map(it -> JSON.parseObject(it, BalanceRedisDto.class)).collect(Collectors.toMap(it -> it.getUid(), it -> it.getBalance()));
//        if (CollUtil.isNotEmpty(edus)) {
//            List<String> noUseData = new ArrayList<>();
//            for (String edu : edus) {
//                edu = edu.replace("今日头条各个账号剩余额度:", "");
//                Integer sufEdu = balanceMap.get(edu);
//                if (sufEdu - storeConfig.getSkuPrice().intValue() >= 0) {
//                    continue;
//                } else {
//                    log.debug("订单号{},这个账号不存在额度", no, edu);
//                    noUseData.add(edu);
//                }
//            }
//            if (CollUtil.isNotEmpty(noUseData)) {
//                log.debug("订单号{},不能使用的账号:{}", no, JSON.toJSONString(noUseData));
//                wrapper.notIn(DouyinAppCk::getUid, noUseData);
//            }
//        }
//    }
//
//
//}
