package com.xd.pre.modules.px.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.R;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.data.tenant.PreTenantContextHolder;
import com.xd.pre.modules.px.appstorePc.PcAppStoreService;
import com.xd.pre.modules.px.douyin.DouyinService;
import com.xd.pre.modules.px.service.*;
import com.xd.pre.modules.px.task.ProductProxyTask;
import com.xd.pre.modules.px.vo.reqvo.TokenKeyVo;
import com.xd.pre.modules.px.vo.resvo.TokenKeyResVo;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.mapper.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.activemq.ScheduledMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.Queue;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TopicConsumerListener {

    @Autowired
    private NewWeiXinPayUrl newWeiXinPayUrl;
    @Autowired
    private Queue queue;

    @Resource(name = "findQueue")
    private Queue findQueue;

    @Resource(name = "product_stock_queue")
    private Queue product_stock_queue;

    @Resource(name = "product_proxy_task")
    private Queue product_proxy_task;

    @Resource(name = "notify_success")
    private Queue notify_success_queue;

    @Resource(name = "product_ip_queue")
    private Queue product_ip_queue;

    @Resource(name = "match2_queue")
    private Queue match2_queue;

    @Resource(name = "check_data_queue")
    private Queue check_data_queue;

    @Resource(name = "cancel_queue")
    private Queue cancel_queue;

    @Resource(name = "product_douyin_stock_queue")
    private Queue product_douyin_stock_queue;
    /**
     * ????????????
     */
    @Resource(name = "activate_queue")
    private Queue activate_queue;
    /**
     * ????????????
     */
    @Resource(name = "activate_meituan_queue")
    private Queue activate_meituan_queue;

    @Resource(name = "ios_product_queue")
    private Queue ios_product_queue;


    @Autowired
    private ProductProxyTask productProxyTask;

    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private JdMchOrderMapper jdMchOrderMapper;

    @Resource
    private JdOrderPtMapper jdOrderPtMapper;

    @Autowired
    private TokenKeyService tokenKeyService;


    @Autowired
    private ProxyProductService proxyProductService;

    @Resource
    private JdCkMapper jdCkMapper;

    @Resource
    private JdAppStoreConfigMapper jdAppStoreConfigMapper;

    @Autowired
    private JdDjService jdDjService;


    /**
     * ??????service
     */
    @Autowired
    private ActivateService activateService;

    @Resource
    private DouyinRechargePhoneMapper douyinRechargePhoneMapper;

    @Autowired
    private DouyinService douyinService;

    @Autowired
    @Lazy
    private PcAppStoreService pcAppStoreService;

    @JmsListener(destination = "ios_product_queue", containerFactory = "queueListener", concurrency = "2")
    public void ios_product_queue(String skuId) {
        String lock = redisTemplate.opsForValue().get(PreConstant.????????????IOS?????? + skuId);
        if (StrUtil.isNotBlank(lock) && Integer.valueOf(lock) <= 0) {
            log.info("????????????");
            redisTemplate.opsForValue().set(PreConstant.????????????IOS?????? + skuId, "0");
            return;
        }
        Boolean aBoolean = activateService.productIosOrder(skuId);
        if (ObjectUtil.isNull(aBoolean)) {
            return;
        }
        if (!aBoolean) {
            log.info("????????????");
            sendMessageNotTime(ios_product_queue, skuId);
        }
    }

    //??????ip?????????
    @JmsListener(destination = "cancel_queue", containerFactory = "queueListener", concurrency = "2")
    public void cancel_queue(String message) {
        JdMchOrder jdMchOrder = JSON.parseObject(message, JdMchOrder.class);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        JdMchOrder jdMchOrderDb = this.jdMchOrderMapper.selectById(jdMchOrder.getId());
        if (jdMchOrderDb.getStatus() == PreConstant.TWO) {
            return;
        }
        jdMchOrderDb.setStatus(0);
        jdMchOrderMapper.updateById(jdMchOrderDb);
        if (Integer.valueOf(jdMchOrderDb.getPassCode()) == PreConstant.THREE) {
            this.jdDjService.deleteOrderByOrderId(jdMchOrderDb);
        }
        if (Integer.valueOf(jdMchOrderDb.getPassCode()) == PreConstant.SIX && StrUtil.isNotBlank(jdMchOrder.getOriginalTradeNo())) {
            log.info("??????????????????");
            JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
            jdOrderPt.setIsMatch(PreConstant.ZERO);
            this.jdOrderPtMapper.updateById(jdOrderPt);
            log.info("??????????????????msg:{}", jdOrderPt.getOrderId());
        }
        if (Integer.valueOf(jdMchOrderDb.getPassCode()) == PreConstant.SEVEN && StrUtil.isNotBlank(jdMchOrder.getOriginalTradeNo())) {
            log.info("??????????????????");
            JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
            jdOrderPt.setIsMatch(PreConstant.ZERO);
            this.jdOrderPtMapper.updateById(jdOrderPt);
            log.info("??????????????????msg:{}", jdOrderPt.getOrderId());
        }
        return;
    }

    //??????ip?????????
    @JmsListener(destination = "check_data_queue", containerFactory = "queueListener", concurrency = "3")
    public void check_data_queue(String message) {
        JdCk jdCk = JSON.parseObject(message, JdCk.class);
        JdOrderPt jdOrderPtPre = null;
        List<JdOrderPt> jdOrderPts = newWeiXinPayUrl.checkCkAndMatch(jdCk.getCk(), jdCk.getSkuId(), jdCk.getFileName());
        if (CollUtil.isNotEmpty(jdOrderPts)) {
            for (JdOrderPt jdOrderPtInsert : jdOrderPts) {
                try {
                    inserOrUpdateData(jdOrderPtInsert);
                } catch (Exception e) {
                    log.error("??????????????????????????????msg:{}", e.getMessage());
                }
            }
            jdOrderPtPre = jdOrderPts.get(0);
        }
        JdCk jdCkDb = jdCkMapper.selectOne(Wrappers.<JdCk>lambdaQuery().eq(JdCk::getPtPin, PreUtils.get_pt_pin(jdCk.getCk())));
        if (ObjectUtil.isNotNull(jdCkDb)) {
            if (ObjectUtil.isNotNull(jdOrderPtPre) && ObjectUtil.isNotNull(jdOrderPtPre.getIsEnable())) {
                if (ObjectUtil.isNotNull(jdCkDb)) {
                    jdCkDb.setIsEnable(jdOrderPtPre.getIsEnable());
                    jdCkDb.setFileName(jdCk.getFileName());
                    jdCkMapper.updateById(jdCkDb);
                }
            } else {
                JdProxyIpPort oneIp = proxyProductService.getOneIp(0, 0, false);
                TokenKeyVo build = TokenKeyVo.builder().cookie(jdCkDb.getCk()).build();
                TokenKeyResVo tokenKey = tokenKeyService.getTokenKey(build, oneIp, jdCkDb.getFileName());
                if (ObjectUtil.isNotNull(tokenKey)) {
                    jdCkDb.setIsEnable(1);
                    jdCkDb.setFileName(jdCk.getFileName());
                    jdCkMapper.updateById(jdCkDb);
                } else {
                    jdCkDb.setIsEnable(0);
                    jdCkDb.setFileName(jdCk.getFileName());
                    jdCkMapper.updateById(jdCkDb);
                }
            }
        }
        return;
    }

    private void inserOrUpdateData(JdOrderPt jdOrderPtInsert) {
        if (jdOrderPtInsert.getIsEnable() == PreConstant.FIVE) {
            log.info("??????????????????,????????????");
            JdOrderPt jdOrderPtDb = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, jdOrderPtInsert.getOrderId()));
            if (ObjectUtil.isNotNull(jdOrderPtDb)) {
                log.info("?????????????????????????????????");
                redisTemplate.opsForValue().set(PreConstant.???????????????????????? + jdOrderPtDb.getOrderId(), jdOrderPtDb.getWeixinUrl(), 4, TimeUnit.MINUTES);
                jdOrderPtDb.setIsWxSuccess(1);
                jdOrderPtDb.setExpireTime(jdOrderPtInsert.getExpireTime());
                jdOrderPtDb.setCurrentCk(jdOrderPtInsert.getCurrentCk());
                jdOrderPtDb.setSkuPrice(jdOrderPtInsert.getSkuPrice());
                jdOrderPtDb.setSkuName(jdOrderPtInsert.getSkuName());
                jdOrderPtMapper.updateById(jdOrderPtDb);
            } else {
                jdOrderPtInsert.setIsWxSuccess(1);
                jdOrderPtInsert.setCreateTime(new Date());
                jdOrderPtInsert.setExpireTime(jdOrderPtInsert.getExpireTime());
                jdOrderPtMapper.insert(jdOrderPtInsert);
            }
        }
    }


    //??????
    @JmsListener(destination = "notify_success", containerFactory = "queueListener", concurrency = "1")
    public void notify_success(String message) {
        //??????????????????
        JdMchOrder jdMchOrder = JSON.parseObject(message, JdMchOrder.class);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdMchOrder = jdMchOrderMapper.selectById(jdMchOrder.getId());
        if (jdMchOrder.getNotifySucc() == PreConstant.ONE) {
            return;
        }
        Boolean isSuccess = productProxyTask.notifySuccess(jdMchOrder);
        if (isSuccess) {
            return;
        }
        if (DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 10).getTime() > System.currentTimeMillis()) {
            this.sendMessage(this.notify_success_queue, JSON.toJSONString(jdMchOrder), 1);
            log.info("?????????:{},????????????10???????????????", jdMchOrder.getTradeNo());
            return;
        }
        if (DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 20).getTime() > System.currentTimeMillis()) {
            log.info("?????????:{},????????????20", jdMchOrder.getTradeNo());
            this.sendMessage(this.notify_success_queue, JSON.toJSONString(jdMchOrder), 2);
            return;
        }
        if (DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 180).getTime() > System.currentTimeMillis()) {
            log.info("?????????:{},????????????180");
            this.sendMessage(this.notify_success_queue, JSON.toJSONString(jdMchOrder), 2);
            return;
        }
        if (Integer.valueOf(jdMchOrder.getPassCode()) == PreConstant.NINE &&
                DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 240).getTime() > System.currentTimeMillis()) {
            this.sendMessage(this.notify_success_queue, JSON.toJSONString(jdMchOrder), 2);
        }
        return;
    }


    //topic??????????????????
    @JmsListener(destination = "${spring.activemq.topic-name}", containerFactory = "topicListener")
    public void readActiveQueue(String message) {
        System.out.println("topic????????????" + message);
    }

    //queue??????????????????
    @JmsListener(destination = "product_douyin_stock_queue", containerFactory = "queueListener", concurrency = "20")
    public void product_douyin_stock_queue(String message) {
        log.info("????????????appstore");
        JdAppStoreConfig jdAppStoreConfig = JSON.parseObject(message, JdAppStoreConfig.class);
        log.info("??????????????????????????????????????????????????????");
        PreTenantContextHolder.setCurrentTenantId(Long.valueOf(jdAppStoreConfig.getMark()));
        if (Integer.valueOf(jdAppStoreConfig.getGroupNum()) == PreConstant.EIGHT) {
            log.info("??????appstore");
            LambdaQueryWrapper<JdOrderPt> stockWrapper = Wrappers.lambdaQuery();
            stockWrapper.eq(JdOrderPt::getSkuPrice, jdAppStoreConfig.getSkuPrice());
            Set<String> stockNums = redisTemplate.keys("????????????????????????:*");
            if (CollUtil.isNotEmpty(stockNums)) {
                List<String> sockIds = stockNums.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
                stockWrapper.notIn(JdOrderPt::getId, sockIds);
            }
            stockWrapper.isNull(JdOrderPt::getPaySuccessTime).gt(JdOrderPt::getWxPayExpireTime, new Date());
            List<JdOrderPt> jdOrderPtStocks = jdOrderPtMapper.selectList(stockWrapper);
            if (jdOrderPtStocks.size() >= jdAppStoreConfig.getProductStockNum()) {
                log.info("???????????????????????????");
                return;
            }
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("?????????????????????????????????????????????:" + jdAppStoreConfig.getId(), "1", 5, TimeUnit.SECONDS);
            if (!ifAbsent) {
                log.info("??????????????????????????????");
                return;
            }
            Page<JdMchOrder> jdMchOrderPage = jdMchOrderMapper.selectPage(new Page<>(1, 1), Wrappers.<JdMchOrder>lambdaQuery()
                    .eq(JdMchOrder::getPassCode, jdAppStoreConfig.getGroupNum()).eq(JdMchOrder::getSkuId, jdAppStoreConfig.getSkuId())
                    .gt(JdMchOrder::getCreateTime, DateUtil.offsetMinute(new Date(), -20)));
            if (jdMchOrderPage.getRecords().size() > 0) {
                int sufStoke = jdAppStoreConfig.getProductStockNum() - jdOrderPtStocks.size();
                for (int i = 0; i < sufStoke; i++) {
                    if (i == 0) {
                        sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(jdAppStoreConfig), 10);
                    } else {
                        sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(jdAppStoreConfig), PreUtils.randomCommon(1, 20, 1)[0]);
                    }
                }
                log.info("??????????????????");
                JdMchOrder jdMchOrder = jdMchOrderPage.getRecords().get(PreConstant.ZERO);
                String orderId = PreUtils.getRandomString(5).toUpperCase();
                jdMchOrder.setTradeNo(orderId);
                jdMchOrder.setId(null);
                log.info("?????????:{}??????????????????msg:{}", jdMchOrder);
                JdLog jdLog = JdLog.builder().ip("210.16.122.101").orderId(orderId).build();
                TimeInterval timer = DateUtil.timer();
                OkHttpClient client = pcAppStoreService.buildClient();
                R r = douyinService.douyinProductNewOrder(jdMchOrder, jdAppStoreConfig, jdLog, timer, client, "");
                if (ObjectUtil.isNotNull(r) && r.getCode() == HttpStatus.HTTP_OK && r.getData() instanceof JdOrderPt) {
                    log.info("?????????:{},??????????????????????????????????????????msg:{}", jdMchOrder.getTradeNo(), JSON.toJSONString(r.getData()));
                }
            } else {
                log.error("????????????????????????");
                return;
            }

        }
        if (Integer.valueOf(jdAppStoreConfig.getGroupNum()) == PreConstant.TEN) {
            //TODO
            log.info("????????????TODO");
            return;
        }
        if (Integer.valueOf(jdAppStoreConfig.getGroupNum()) == PreConstant.NINE) {
            //TODO
            log.info("????????????TODo");
            return;
        }


    }

    //queue??????????????????
    @JmsListener(destination = "product_stock_queue", containerFactory = "queueListener", concurrency = "1")
    public void product_stock(String message) {
        log.info("apptore+++?????????????????????????????????");
        JdAppStoreConfig jdAppStoreConfig = JSON.parseObject(message, JdAppStoreConfig.class);
        if (Integer.valueOf(jdAppStoreConfig.getGroupNum()) == PreConstant.EIGHT || Integer.valueOf(jdAppStoreConfig.getGroupNum()) == PreConstant.NINE) {
            sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(jdAppStoreConfig), 20);
            return;
        }
        String cc = redisTemplate.opsForValue().get("stock:" + jdAppStoreConfig.getSkuId());
        if (StrUtil.isBlank(cc)) {
            redisTemplate.opsForValue().set("stock:" + jdAppStoreConfig.getSkuId(), "0");
        }
        if (Integer.valueOf(cc) <= 0) {
            return;
        }
        LambdaQueryWrapper<JdCk> wrapper = Wrappers.<JdCk>lambdaQuery().in(JdCk::getIsEnable, Arrays.asList(1, 5));
        Set<String> keys = redisTemplate.keys(PreConstant.??????CK?????????????????? + "*");
        if (CollUtil.isNotEmpty(keys) && jdAppStoreConfig.getGroupNum() == PreConstant.ONE) {
            List<String> lockKeys = keys.stream().map(it -> it.split(":")[1]).map(it -> URLDecoder.decode(it)).collect(Collectors.toList());
            log.debug("??????????????????ck???msg:{}", lockKeys);
            wrapper.notIn(JdCk::getPtPin, lockKeys);
        }
        keys = redisTemplate.keys(PreConstant.????????????CK?????????????????? + "*");
        if (CollUtil.isNotEmpty(keys) && jdAppStoreConfig.getGroupNum() == PreConstant.TWO) {
            List<String> lockKeys = keys.stream().map(it -> it.split(":")[1]).map(it -> URLDecoder.decode(it)).collect(Collectors.toList());
            wrapper.notIn(JdCk::getPtPin, lockKeys);
        }


        Set<String> currKeys = redisTemplate.keys(PreConstant.??????CK?????????????????? + "*");
        if (CollUtil.isNotEmpty(currKeys) && jdAppStoreConfig.getGroupNum() == PreConstant.ONE) {
            List<String> lockKeys = currKeys.stream().map(it -> it.split(":")[1]).map(it -> URLDecoder.decode(it)).collect(Collectors.toList());
            log.info("????????????????????????appstore");
            wrapper.notIn(JdCk::getPtPin, lockKeys);
        }

        Set<String> products = redisTemplate.keys(PreConstant.?????????????????????CK + "*");
        if (CollUtil.isNotEmpty(products) && jdAppStoreConfig.getGroupNum() == PreConstant.ONE) {
            List<String> lockKeys = products.stream().map(it -> it.split(":")[1]).map(it -> URLDecoder.decode(it)).collect(Collectors.toList());
            log.debug("????????????:{}", lockKeys);
            wrapper.notIn(JdCk::getPtPin, lockKeys);
        }
        Integer count = jdCkMapper.selectCount(wrapper);
        if (count < 200) {
            log.error("??????ck??????????????????,????????????????????????0");
            List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.emptyWrapper());
            for (JdAppStoreConfig appStoreConfig : jdAppStoreConfigs) {
                redisTemplate.opsForValue().set("stock:" + appStoreConfig.getSkuId(), "0");
            }
            return;
        }
        int pageTotalSize = count / 5;
        int currrent = PreUtils.randomCommon(0, pageTotalSize, 1)[0];
        Page<JdCk> jdCkPage = new Page<>(currrent, 5);
        jdCkPage = jdCkMapper.selectPage(jdCkPage, wrapper);
        List<JdCk> records = jdCkPage.getRecords();
        for (JdCk jdCk : records) {
            String pt_pin = URLDecoder.decode(jdCk.getPtPin());
            String readyLock = redisTemplate.opsForValue().get(PreConstant.?????????????????????CK + pt_pin);
            if (StrUtil.isNotBlank(readyLock)) {
                log.info("???????????????????????????");
                sendMessageSenc(product_stock_queue, JSON.toJSONString(jdAppStoreConfig), 10);
                log.debug("??????ck?????????????????????????????????");
                return;
            }
            List<JdOrderPt> allJd = new ArrayList<>();

            List<JdOrderPt> orderPtsFirst = newWeiXinPayUrl.checkCkAndMatch(jdCk.getCk(), jdAppStoreConfig.getSkuId(), jdCk.getFileName());
            if (CollUtil.isNotEmpty(orderPtsFirst)) {
                allJd.addAll(orderPtsFirst);
                if (jdAppStoreConfig.getGroupNum() == PreConstant.TWO) {
                    for (int i = 0; i < jdAppStoreConfig.getProductNum(); i++) {
                        log.info("???????????????????????????");
                        List<JdOrderPt> orderPts = newWeiXinPayUrl.checkCkAndMatch(jdCk.getCk(), jdAppStoreConfig.getSkuId(), jdCk.getFileName());
                        if (CollUtil.isNotEmpty(orderPts)) {
                            allJd.addAll(orderPts);
                        }
                    }
                }

            }
            //??????,???????????????????????????
            if (CollUtil.isEmpty(allJd)) {
                jdAppStoreConfig.setMark(PreUtils.getRandomString(100));
                sendMessageSenc(product_stock_queue, JSON.toJSONString(jdAppStoreConfig), 10);
                return;
            }
            if (CollUtil.isNotEmpty(allJd)) {
                for (JdOrderPt jdOrderPtInsert : allJd) {
                    try {
//                        log.debug("???????????????????????????????????????????????????????????????????????????");
                        inserOrUpdateData(jdOrderPtInsert);
                        jdCk.setIsEnable(5);
                        jdCk.setFailTime(0);
                        Long increment = redisTemplate.opsForValue().increment("stock:" + jdAppStoreConfig.getSkuId(), -1);
                        if (increment < 0) {
                            redisTemplate.opsForValue().set("stock:" + jdAppStoreConfig.getSkuId(), "0");
                        }
                        jdCkMapper.updateById(jdCk);
                    } catch (Exception e) {
                        log.error("??????????????????????????????msg:{}", e.getMessage());
                    }
                }
            } else {
                sendMessageSenc(product_stock_queue, JSON.toJSONString(jdAppStoreConfig), 10);
            }
            return;
        }
    }


    //????????????
    @JmsListener(destination = "findQueue", containerFactory = "queueListener", concurrency = "10")
    public void findQueue(String message) {
        JdMchOrder jdMchOrder = JSON.parseObject(message, JdMchOrder.class);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        log.info("??????????????????{}?????????????????????????????????", jdMchOrder.getTradeNo());
        R r = newWeiXinPayUrl.getCartNumAndMy(jdMchOrder);
        JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
        if ((ObjectUtil.isNotNull(r) && (Boolean) r.getData()) || jdMchOrderDb.getStatus() == PreConstant.TWO) {
            log.info("?????????{}???????????????++++++++????????????????????????", jdMchOrder.getTradeNo());
            redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), 30, TimeUnit.DAYS);
            this.sendMessageNotTime(this.notify_success_queue, JSON.toJSONString(jdMchOrder));
            return;
        }

        if (DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 4).getTime() > System.currentTimeMillis()) {
            this.sendMessageSenc(this.findQueue, JSON.toJSONString(jdMchOrder), 30);
            log.info("?????????{}????????????4????????????", jdMchOrder.getTradeNo());
            return;
        }
        if (DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 5).getTime() > System.currentTimeMillis()) {
            this.sendMessageSenc(this.findQueue, JSON.toJSONString(jdMchOrder), 30);
            log.info("?????????{}????????????5????????????", jdMchOrder.getTradeNo());
            return;
        }
        if (DateUtil.offset(jdMchOrder.getCreateTime(), DateField.MINUTE, 6).getTime() > System.currentTimeMillis() &&
                Integer.valueOf(jdMchOrder.getPassCode()) != PreConstant.NINE) {
            log.info("?????????{}?????????????????????10????????????,????????????????????????", jdMchOrder.getTradeNo());
            jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
            if (jdMchOrderDb.getStatus() == 2) {
                return;
            } else {
                this.sendMessageSenc(this.cancel_queue, JSON.toJSONString(jdMchOrderDb), 10);
                return;
            }
        }
        if (Integer.valueOf(jdMchOrder.getPassCode()) == PreConstant.NINE) {
            JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdMchOrder.getOriginalTradeId());
            DouyinRechargePhone douyinRechargePhone = JSON.parseObject(jdOrderPt.getWphCardPhone(), DouyinRechargePhone.class);
            douyinRechargePhone = douyinRechargePhoneMapper.selectById(douyinRechargePhone.getId());
            if (douyinRechargePhone.getOrderStatus() == PreConstant.ONE) {
                log.info("????????????{}????????????:{}????????????", jdMchOrder.getTradeNo(), douyinRechargePhone.getRechargePhone());
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), 20, TimeUnit.SECONDS);
                if (ifAbsent) {
                    this.sendMessageSenc(this.findQueue, JSON.toJSONString(jdMchOrder), 30);
                }
            }
        }
    }

    //queue??????????????????
    @JmsListener(destination = "match2_queue", containerFactory = "queueListener", concurrency = "1")
    public void readActiveQueueQueue2(String message) {
        this.readActiveQueueQueue(message);
    }


    @JmsListener(destination = "activate_meituan_queue", containerFactory = "queueListener", concurrency = "1")
    public void activate_meituan_queue(String message) {
        JdOrderPt jdOrderPt = JSON.parseObject(message, JdOrderPt.class);
        this.activateService.reSetMeiTuan(jdOrderPt);
    }


    //??????????????????
    @JmsListener(destination = "${spring.activemq.queue-name}", containerFactory = "queueListener", concurrency = "20")
    public void readActiveQueueQueue(String message) {
        JdMchOrder jdMchOrder = JSON.parseObject(message, JdMchOrder.class);
        try {
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            if (DateUtil.offsetMinute(jdMchOrder.getCreateTime(), 2).getTime() < System.currentTimeMillis()) {
                log.info("?????????:{},???????????????????????????", jdMchOrder.getTradeNo());
                return;
            }
            log.info("?????????:{}??????????????????,????????????", jdMchOrder.getTradeNo());
            String matchLockOk = redisTemplate.opsForValue().get("??????????????????:" + jdMchOrder.getTradeNo());
            if (StrUtil.isNotBlank(matchLockOk)) {
                this.sendMessageNotTime(this.product_ip_queue, "??????ip");
                log.info("?????????:{}?????????????????????????????????????????????", jdMchOrder.getTradeNo());
                return;
            }
            log.info("????????????msg:[jdClientOrder:{}]", jdMchOrder);
            R r = newWeiXinPayUrl.match(jdMchOrder);
            log.info("?????????:{}?????????????????????????????????", jdMchOrder.getTradeNo());
            if (ObjectUtil.isNull(r)) {
                log.info("?????????????????????????????????????????????,?????????????????????????????????????????????????????????????????????????????????");
                jdMchOrder.setQrcodeUrl(PreUtils.getRandomString(10));
                if (jdMchOrder.getPassCode().equals(PreConstant.EIGHT)) {
                    this.sendMessageNotTime(this.queue, JSON.toJSONString(jdMchOrder));
                } else {
                    this.sendMessageSenc(this.queue, JSON.toJSONString(jdMchOrder), 1);
                }
                return;
            }
            JdMchOrder jdMchOrderData = JSON.parseObject(JSON.toJSONString(r.getData()), JdMchOrder.class);
            if (ObjectUtil.isNull(jdMchOrderData) || StrUtil.isBlank(jdMchOrderData.getOriginalTradeNo())) {
                jdMchOrder.setQrcodeUrl(PreUtils.getRandomString(10));
                this.sendMessageSenc(this.queue, JSON.toJSONString(jdMchOrder), 1);
            }
            log.info("?????????:{}???????????? ,????????????????????????", jdMchOrder.getTradeNo());
            this.sendMessageSenc(this.findQueue, JSON.toJSONString(jdMchOrder), 10);
        } catch (Exception e) {
            log.error("????????????msg:{} ", e.getStackTrace());
        }
        jdMchOrder.setQrcodeUrl(PreUtils.getRandomString(10));
        this.sendMessageSenc(this.queue, JSON.toJSONString(jdMchOrder), 3);
    }

    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessageSenc(Destination destination, final String message, Integer minit) {
        Map<String, Object> headers = new HashMap<>();
        //???????????????????????????10???,????????????
        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 1000);
        jmsMessagingTemplate.convertAndSend(destination, message, headers);
    }

    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessage(Destination destination, final String message, Integer minit) {
        Map<String, Object> headers = new HashMap<>();
        //???????????????????????????10???,????????????
        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 60 * 1000);
        jmsMessagingTemplate.convertAndSend(destination, message, headers);
    }

    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessageNotTime(Destination destination, final String message) {
        jmsMessagingTemplate.convertAndSend(destination, message);
    }
}