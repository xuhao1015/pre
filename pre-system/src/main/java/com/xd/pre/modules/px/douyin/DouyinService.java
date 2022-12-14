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
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.R;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.data.tenant.PreTenantContextHolder;
import com.xd.pre.modules.px.appstorePc.PcAppStoreService;
import com.xd.pre.modules.px.douyin.buyRender.BuyRenderParamDto;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import com.xd.pre.modules.px.douyin.pay.PayDto;
import com.xd.pre.modules.px.douyin.pay.PayRiskInfoAndPayInfoUtils;
import com.xd.pre.modules.px.douyin.submit.DouyinAsynCService;
import com.xd.pre.modules.px.douyin.submit.SubmitUtils;
import com.xd.pre.modules.px.task.ProductProxyTask;
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


    public R match(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog) {
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        storeConfig.setMark(jdMchOrder.getTenantId() + "");
        TimeInterval timer = DateUtil.timer();
        log.info("?????????{}?????????ip:{},????????????????????????", jdMchOrder.getTradeNo(), JSON.toJSONString(jdLog));
        OkHttpClient client = pcAppStoreService.buildClient();
        log.info("?????????:{},??????????????????????????????????????????????????????", jdMchOrder.getTradeNo());
        //  redisTemplate.opsForValue().set("????????????????????????:" + jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo(), 5, TimeUnit.MINUTES);
        LambdaQueryWrapper<JdOrderPt> stockWrapper = Wrappers.lambdaQuery();
        stockWrapper.isNull(JdOrderPt::getPaySuccessTime).gt(JdOrderPt::getWxPayExpireTime, new Date());
        Set<String> stockNums = redisTemplate.keys("????????????????????????:*");
        if (CollUtil.isNotEmpty(stockNums)) {
            List<String> sockIds = stockNums.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
            stockWrapper.notIn(JdOrderPt::getId, sockIds);
        }
        stockWrapper.eq(JdOrderPt::getSkuPrice, storeConfig.getSkuPrice());
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        List<JdOrderPt> jdOrderPtStocks = jdOrderPtMapper.selectList(stockWrapper);
        String payReUrl = "";
        if (CollUtil.isNotEmpty(jdOrderPtStocks)) {
            log.info("?????????:{}.????????????", jdMchOrder.getTradeNo());
            sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 10);
            sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 10);
            return douyinUseStock(jdMchOrder, storeConfig, jdLog, timer, client, jdOrderPtStocks, payReUrl);
        } else {
            log.info("?????????:{}.????????????????????????", jdMchOrder.getTradeNo());
            sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 5);
            sendMessageSenc(product_douyin_stock_queue, JSON.toJSONString(storeConfig), 15);
            log.info("?????????:{},?????????", jdMchOrder.getTradeNo());
            return douyinProductNewOrder(jdMchOrder, storeConfig, jdLog, timer, client, payReUrl);
        }
    }

    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessageSenc(Destination destination, final String message, Integer minit) {
        Map<String, Object> headers = new HashMap<>();
        //???????????????????????????10???,????????????
        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 1000);
        jmsMessagingTemplate.convertAndSend(destination, message, headers);
    }

    private R douyinUseStock(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog, TimeInterval timer, OkHttpClient client, List<JdOrderPt> jdOrderPtStocks, String payReUrl) {
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        JdOrderPt jdOrderPtDb = jdOrderPtStocks.get(PreUtils.randomCommon(0, jdOrderPtStocks.size() - 1, 1)[0]);
        PayDto payDto = JSON.parseObject(jdOrderPtDb.getMark(), PayDto.class);
        log.info("?????????:{},?????????,?????????????????????????????????:{},", jdMchOrder.getTradeNo(), jdOrderPtDb.getOrderId());
        for (int i = 0; i < 3; i++) {
            log.info("????????????{}???{}????????????", jdMchOrder.getTradeNo(), i);
            payReUrl = payByOrderId(client, payDto, jdLog, jdMchOrder);
            if (StrUtil.isNotBlank(payReUrl)) {
                break;
            } else {
                client = pcAppStoreService.buildClient();
                continue;
            }
        }
        log.info("?????????{}???????????????????????????:?????????{}", jdMchOrder.getTradeNo(), timer.interval());
        if (StrUtil.isBlank(payReUrl)) {
            return null;
        }
        String lockOrderTime = redisTemplate.opsForValue().get("???????????????????????????????????????");
        Boolean ifLockStock = redisTemplate.opsForValue().setIfAbsent("????????????????????????:" + jdOrderPtDb.getId(), jdMchOrder.getTradeNo(),
                Integer.valueOf(lockOrderTime), TimeUnit.MINUTES);
        if (!ifLockStock) {
            log.error("?????????{}???????????????????????????,??????????????????msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo());
            return null;
        }
        Boolean isLockMath = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder),
                storeConfig.getExpireTime(), TimeUnit.MINUTES);
        if (!isLockMath) {
            log.error("?????????{}??????????????????????????????,??????????????????msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo());
            return null;
        }
        log.info("?????????:{},??????????????????????????????", jdMchOrder.getTradeNo());
        //  .hrefUrl(payReUrl).weixinUrl(payReUrl).wxPayUrl(payReUrl)
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdOrderPtDb.setHrefUrl(payReUrl);
        jdOrderPtDb.setWeixinUrl(payReUrl);
        jdOrderPtDb.setWxPayUrl(payReUrl);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdOrderPtMapper.updateById(jdOrderPtDb);
        long l = (System.currentTimeMillis() - jdMchOrder.getCreateTime().getTime()) / 1000;
        jdMchOrder.setOriginalTradeId(jdOrderPtDb.getId());
        jdMchOrder.setMatchTime(l >= 1 ? l - 1 : l);
        jdMchOrder.setOriginalTradeNo(jdOrderPtDb.getOrderId());
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdMchOrderMapper.updateById(jdMchOrder);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
        JdOrderPt jdOrderPt = jdOrderPtMapper.selectById(jdOrderPtDb.getId());
        if (ObjectUtil.isNull(jdMchOrderDb.getOriginalTradeId()) || !jdOrderPt.getHrefUrl().contains(jdMchOrderDb.getTradeNo())) {
            log.info("?????????:{},????????????", jdMchOrderDb.getTradeNo());
            redisTemplate.delete("????????????????????????:" + jdOrderPtDb.getId());
            jdMchOrder.setMatchTime(-5L);
            jdMchOrder.setOriginalTradeNo("-1");
            jdMchOrder.setOriginalTradeId(-1);
            jdMchOrderMapper.updateById(jdMchOrder);
            return null;
        }
        return R.ok(jdMchOrder);
    }

    public DouyinAppCk randomDouyinAppCk(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, Boolean isAppStore) {
        Integer lockDouYinCkTime = Integer.valueOf(redisTemplate.opsForValue().get("??????ck???????????????"));
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        log.info("?????????{},???????????????????????????????????????msg:{}", jdMchOrder.getTradeNo());
        LambdaQueryWrapper<DouyinAppCk> wrapper = Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getIsEnable, PreConstant.ONE);
        log.info("???????????????????????????????????????????????????????????????????????????????????????");
        if (isAppStore) {
            buildNotUseAccout(storeConfig, wrapper, jdMchOrder.getTradeNo());
        }
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        Integer count = douyinAppCkMapper.selectCount(wrapper);
        int pageIndex = PreUtils.randomCommon(0, count, 1)[0];
        List<Integer> accounts = new ArrayList<>();
        if (count > 5) {
            int[] ints = PreUtils.randomCommon(0, count, 4);
            for (int anInt : ints) {
                accounts.add(anInt);
            }
            accounts = accounts.stream().sorted().collect(Collectors.toList());
            pageIndex = accounts.get(PreConstant.ZERO);

        }
        if (count == 0) {
            log.info("?????????:{}?????????ck????????????ck", jdMchOrder.getTradeNo());
            return null;
        }
        if (storeConfig.getGroupNum() == PreConstant.EIGHT && CollUtil.isNotEmpty(accounts)) {
            DouyinAppCk douyinAppCk = null;
            Integer let = Integer.valueOf(redisTemplate.opsForValue().get("?????????????????????????????????"));
            for (Integer account : accounts) {
                Page<DouyinAppCk> douyinAppCkPage = new Page<>(account, PreConstant.ONE);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                douyinAppCkPage = douyinAppCkMapper.selectPage(douyinAppCkPage, wrapper);
                DouyinAppCk douyinAppCkT = douyinAppCkPage.getRecords().get(PreConstant.ZERO);
                String ed = redisTemplate.opsForValue().get("??????????????????????????????:" + douyinAppCkT.getUid());
                if (StrUtil.isNotBlank(ed) && Integer.valueOf(ed) >= storeConfig.getSkuPrice().intValue()) {
                    if (let >= Integer.valueOf(ed) && Integer.valueOf(ed) == 200) {
                        let = Integer.valueOf(ed);
                        douyinAppCk = douyinAppCkT;
                    }
                }
            }
            if (ObjectUtil.isNotNull(douyinAppCk)) {
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????ck??????3??????:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockDouYinCkTime, TimeUnit.MINUTES);
                if (ifAbsent) {
                    return douyinAppCk;
                }
            }
        }
        Page<DouyinAppCk> douyinAppCkPage = new Page<>(pageIndex, PreConstant.ONE);
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        douyinAppCkPage = douyinAppCkMapper.selectPage(douyinAppCkPage, wrapper);
        DouyinAppCk douyinAppCk = douyinAppCkPage.getRecords().get(PreConstant.ZERO);
        log.info("?????????{}??????????????????ckmsg:{}", jdMchOrder.getTradeNo(), JSON.toJSONString(douyinAppCk));
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????ck??????3??????:" + douyinAppCk.getUid(), JSON.toJSONString(douyinAppCk), lockDouYinCkTime, TimeUnit.MINUTES);
        if (ifAbsent) {
            return douyinAppCk;
        }
        return null;
    }

    public Integer getPayType() {
        Integer payType = 0;
        String ???????????????Pay = redisTemplate.opsForValue().get("???????????????Pay");
        if (StrUtil.isNotBlank(???????????????Pay)) {
            payType = Integer.valueOf(???????????????Pay);
        } else {
            payType = 2;
            redisTemplate.opsForValue().set("???????????????Pay", "2");
        }
        return payType;
    }

    public R douyinProductNewOrder(JdMchOrder jdMchOrder, JdAppStoreConfig storeConfig, JdLog jdLog, TimeInterval timer, OkHttpClient client, String payReUrl) {
        DouyinAppCk douyinAppCk = randomDouyinAppCk(jdMchOrder, storeConfig, true);
        if (ObjectUtil.isNull(douyinAppCk)) {
            return null;
        }
        JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
        List<DouyinDeviceIid> douyinDeviceIUseids = getDouyinDeviceIids(jdMchOrder);
        if (CollUtil.isEmpty(douyinDeviceIUseids)) {
            String deviceBangDing = redisTemplate.opsForValue().get("????????????????????????:" + douyinAppCk.getUid());
            log.info("?????????:{},?????????????????????ck???????????????????????????????????????:{}", jdMchOrder.getTradeNo(), deviceBangDing);
            if (StrUtil.isBlank(deviceBangDing)) {
                return null;
            }
        }
        String config = storeConfig.getConfig();
        BuyRenderParamDto buyRenderParamDto = JSON.parseObject(config, BuyRenderParamDto.class);
        log.info("?????????{}???????????????,????????????????????????msg:{}", jdMchOrder.getTradeNo());
        Integer payType = getPayType();
        log.info("?????????{}??????????????????:?????????{}", jdMchOrder.getTradeNo(), timer.interval());
        String tel = PreUtils.getTel();
        PayDto payDto = createOrder(client, buyRenderParamDto, payType, douyinAppCk, jdLog, jdMchOrder, douyinDeviceIUseids, timer, tel);
        if (ObjectUtil.isNull(payDto)) {
            log.info("?????????{}?????????????????????,???????????????", jdMchOrder.getTradeNo());
            return null;
        }
        if (ObjectUtil.isNotNull(jdMchOrderDb)) {
            payReUrl = getPayReUrl(jdMchOrder, jdLog, timer, client, payDto);
        }
        log.info("?????????{}???????????????????????????:?????????{}", jdMchOrder.getTradeNo(), timer.interval());
        if (StrUtil.isBlank(payReUrl) && ObjectUtil.isNotNull(jdMchOrderDb)) {
            return null;
        }
//        ??????????????????msg:{}
        Boolean isLockMath = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder),
                storeConfig.getExpireTime(), TimeUnit.MINUTES);
        if (!isLockMath && ObjectUtil.isNotNull(jdMchOrderDb)) {
            log.error("?????????{}????????????????????????,??????????????????msg:{}", jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo());
            return null;
        }
        JdOrderPt jdOrderPtDb = JdOrderPt.builder().orderId(payDto.getOrderId()).ptPin(douyinAppCk.getUid()).success(PreConstant.ZERO)
                .expireTime(DateUtil.offsetMinute(new Date(), storeConfig.getPayIdExpireTime())).createTime(new Date()).skuPrice(storeConfig.getSkuPrice())
                .skuName(storeConfig.getSkuName()).skuId(storeConfig.getSkuId())
                .wxPayExpireTime(DateUtil.offsetMinute(new Date(), storeConfig.getPayIdExpireTime()))
                .createTime(new Date()).skuPrice(storeConfig.getSkuPrice())
                .isWxSuccess(PreConstant.ONE).isMatch(PreConstant.ONE).currentCk(douyinAppCk.getCk())
                .hrefUrl(payReUrl).weixinUrl(payReUrl).wxPayUrl(payReUrl)
                .mark(JSON.toJSONString(payDto))
                .build();
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        jdOrderPtMapper.insert(jdOrderPtDb);
        log.info("?????????{}????????????????????????msg:{}", jdMchOrder.getTradeNo(), JSON.toJSONString(jdOrderPtDb));
        if (ObjectUtil.isNotNull(jdMchOrderDb)) {
            long l = (System.currentTimeMillis() - jdMchOrder.getCreateTime().getTime()) / 1000;
            jdMchOrder.setMatchTime(l - 1);
            jdMchOrder.setOriginalTradeId(jdOrderPtDb.getId());
            jdMchOrder.setOriginalTradeNo(jdOrderPtDb.getOrderId());
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            jdMchOrderMapper.updateById(jdMchOrder);
            String lockOrderTime = redisTemplate.opsForValue().get("???????????????????????????????????????");
            redisTemplate.opsForValue().set("????????????????????????:" + jdOrderPtDb.getId(), jdMchOrder.getTradeNo(), Integer.valueOf(lockOrderTime), TimeUnit.MINUTES);
            log.info("?????????{}???????????????:?????????{}", jdMchOrder.getTradeNo(), timer.interval());
            return R.ok(jdOrderPtDb);
        } else {
            return R.ok(jdOrderPtDb);
        }

    }

    public List<DouyinDeviceIid> getDouyinDeviceIids(JdMchOrder jdMchOrder) {
        Set<String> lockDouyinDeviceIds = redisTemplate.keys("??????????????????:*");
        LambdaQueryWrapper<DouyinDeviceIid> douyinDeviceWrapper = Wrappers.lambdaQuery();
        if (CollUtil.isNotEmpty(lockDouyinDeviceIds)) {
            List<String> lockIds = lockDouyinDeviceIds.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
            douyinDeviceWrapper.notIn(DouyinDeviceIid::getId, lockIds);
        }
        log.info("?????????{}?????????????????????device_id???iid?????????????????????????????????", jdMchOrder.getTradeNo());
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
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 3, TimeUnit.MINUTES);
            if (ifAbsent) {
                douyinDeviceIUseids.add(douyinDeviceIid);
            }
            if (CollUtil.isNotEmpty(douyinDeviceIids) && douyinDeviceIids.size() > 2) {
                log.info("????????????{}?????????????????????{}", jdMchOrder.getTradeNo(), JSON.toJSONString(douyinDeviceIids));
                return douyinDeviceIUseids;
            }

        }
        return douyinDeviceIUseids;
    }

    private void buildNotUseAccout(JdAppStoreConfig storeConfig, LambdaQueryWrapper<DouyinAppCk> wrapper, String no) {
        Set<String> edus = redisTemplate.keys("??????????????????????????????:*");
        Set<String> locks = redisTemplate.keys("??????ck??????3??????:*");
        if (CollUtil.isNotEmpty(locks)) {
            List<String> locksData = locks.stream().map(it -> it.replace("??????ck??????3??????:", "")).collect(Collectors.toList());
            wrapper.notIn(DouyinAppCk::getUid, locksData);
        }
        log.info("????????????????????????");
        if (CollUtil.isNotEmpty(edus)) {
            List<String> noUseData = new ArrayList<>();
            for (String edu : edus) {
                Integer sufEdu = Integer.valueOf(redisTemplate.opsForValue().get(edu));
                if (sufEdu - storeConfig.getSkuPrice().intValue() >= 0) {
                    continue;
                } else {
                    log.debug("{},???????????????????????????", edu);
                    noUseData.add(edu.replace("??????????????????????????????:", ""));
                }
            }
            if (CollUtil.isNotEmpty(noUseData)) {
                log.info("?????????{},?????????????????????:{}", no, JSON.toJSONString(noUseData));
                wrapper.notIn(DouyinAppCk::getUid, noUseData);
            }
        }

    }

    public String getPayReUrl(JdMchOrder jdMchOrder, JdLog jdLog, TimeInterval timer, OkHttpClient client, PayDto payDto) {
        String payReUrl = "";
        log.info("?????????{}?????????????????????:?????????{}", jdMchOrder.getTradeNo(), timer.interval());
        for (int i = 0; i < 3; i++) {
            log.info("????????????{}???{}????????????", jdMchOrder.getTradeNo(), i);
            log.info("?????????:{},???????????????????????????", jdMchOrder.getTradeNo());
            payReUrl = payByOrderId(client, payDto, jdLog, jdMchOrder);
            if (StrUtil.isNotBlank(payReUrl)) {
                break;
            } else {
                client = pcAppStoreService.buildClient();
            }
        }
        log.info("?????????{}???????????????????????????:?????????{}", jdMchOrder.getTradeNo(), timer.interval());
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
            log.info("?????????{}???????????????msg:{}", jdMchOrder.getTradeNo(), signUrl);
            String signHt = HttpRequest.post(signUrl).body(signData).timeout(2000).execute().body();
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
                    .addHeader("Cookie", payDto.getCk())
                    .addHeader("X-Gorgon", x_gorgon)
                    .addHeader("X-Khronos", x_khronos)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response = client.newCall(request).execute();
            String payData = response.body().string();
            if (payData.contains("??????????????????")) {
                JdOrderPt jdOrderPt = jdOrderPtMapper.selectOne(Wrappers.<JdOrderPt>lambdaQuery().eq(JdOrderPt::getOrderId, payDto.getOrderId()));
                jdOrderPt.setWxPayExpireTime(DateUtil.offsetMinute(new Date(), -100));
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                jdOrderPtMapper.updateById(jdOrderPt);
                return null;
            }
            log.info("?????????{}???????????????????????????msg:{}", jdMchOrder.getTradeNo(), payData);
            String payUrl = JSON.parseObject(JSON.parseObject(JSON.parseObject(JSON.parseObject(payData).getString("data")).getString("data"))
                    .getString("sdk_info")).getString("url");
            redisTemplate.opsForValue().set("??????????????????:" + jdMchOrder.getTradeNo(), payUrl, 3, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("??????????????????:" + jdMchOrder.getTradeNo(), payUrl, 3, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("??????????????????:" + jdMchOrder.getTradeNo(), payUrl, 3, TimeUnit.MINUTES);
            log.info("?????????:{}??????????????????????????????", jdMchOrder.getTradeNo());
            String param = redisTemplate.opsForValue().get("??????????????????:" + jdMchOrder.getTradeNo().trim());
            if (StrUtil.isNotBlank(param)) {
                log.info("?????????:{},??????????????????????????????????????????,????????????", jdMchOrder.getTradeNo());
            }
            response.close();
            String local = redisTemplate.opsForValue().get("???????????????");
            //alipays://platformapi/startapp?appId=20000067&url=http%3A%2F%2F134.122.134.69%3A8082%2Frecharge%2Fzfb%3Forder_id%3DSP2210012316069040391319127864
            String payReUrl = String.format("alipays://platformapi/startapp?appId=20000067&url=%s",
                    URLEncoder.encode("http://" + local + "/api/alipay/payHtml?orderId=" + jdMchOrder.getTradeNo()));
            log.info("?????????{}?????????url?????????msg:{}", jdMchOrder.getTradeNo(), payReUrl);
            return payReUrl;
        } catch (Exception e) {
            log.error("?????????{}???????????????msg:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;
    }

    public PayDto createOrder(OkHttpClient client, BuyRenderParamDto buyRenderParamDto, Integer payType,
                              DouyinAppCk douyinAppCk, JdLog jdLog, JdMchOrder jdMchOrder, List<DouyinDeviceIid> douyinDeviceIids, TimeInterval timer, String phone) {
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
//        redisTemplate.opsForValue().set("????????????????????????:" + douyinAppCk.getUid(), JSON.toJSONString(douyinDeviceIid), 4, TimeUnit.HOURS);
        String deviceBangDing = redisTemplate.opsForValue().get("????????????????????????:" + douyinAppCk.getUid());
        List<DouyinDeviceIid> douyinDeviceIidsT = new ArrayList<>();
        if (StrUtil.isNotBlank(deviceBangDing)) {
            DouyinDeviceIid douyinDeviceIid = JSON.parseObject(deviceBangDing, DouyinDeviceIid.class);
            log.info("????????????{}????????????????????????:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
            douyinDeviceIidsT.add(douyinDeviceIid);
            douyinDeviceIidsT.add(douyinDeviceIid);
            douyinDeviceIidsT.add(douyinDeviceIid);
        }
        for (DouyinDeviceIid douyinDeviceIid : douyinDeviceIids) {
            douyinDeviceIidsT.add(douyinDeviceIid);
        }
        douyinDeviceIids = douyinDeviceIidsT;
        for (DouyinDeviceIid douyinDeviceIid : douyinDeviceIids) {
            try {
                Integer sufMeny = getSufMeny(douyinAppCk.getUid(), jdMchOrder);
                if (sufMeny - new BigDecimal(jdMchOrder.getMoney()).intValue() < 0) {
                    synProductMaxPrirce();
                    return null;
                }
                log.info("?????????:{},???????????????:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
               /* Boolean isLockDeviceId = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 1, TimeUnit.MINUTES);
                if (!isLockDeviceId) {
                    log.info("?????????{}??????????????????????????????:deviceId:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
                    continue;
                }*/
                BuyRenderRoot buyRenderRoot = getAndBuildBuyRender(client, douyinAppCk, buyRenderParamDto, douyinDeviceIids.get(PreConstant.ZERO), jdMchOrder);
                log.info("?????????:{},???????????????{},??????????????????:{}", jdMchOrder.getTradeNo(), douyinDeviceIids.indexOf(douyinDeviceIid), timer.interval());
                if (ObjectUtil.isNull(buyRenderRoot)) {
                    log.info("?????????{}??????????????????", jdMchOrder.getTradeNo());
                    continue;
                }
                //PreUtils.getTel()
                buyRenderRoot.setPost_tel(phone);
                String url1 = "https://ec.snssdk.com/order/newcreate/vtl?can_queue=1&b_type_new=2&request_tag_from=lynx&os_api=22&device_type=SM-G973N&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=354730528934825&app_name=aweme&version_name=17.3.0&ts=1664384138&cpu_support64=false&app_type=normal&appTheme=dark&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&device_platform=android&iid="
                        + douyinDeviceIid.getIid() + "&version_code=170300&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&is_android_pad=0&openudid=199d79fbbeff0e58&device_id="
                        + douyinDeviceIid.getDeviceId() + "&resolution=720*1280&os_version=5.1.1&language=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc=46007";
                String bodyData1 = String.format("{\"area_type\":\"169\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"}," +
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
                                "\\\"appVersion\\\":\\\"8.9.6\\\",\\\"appName\\\":\\\"news_article\\\"," +
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
                                "\"shop_id\":\"%s\"," +
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
                        buyRenderRoot.getPost_tel(),
                        douyinDeviceIid.getDeviceId(),
                        douyinDeviceIid.getIid(),
                        buyRenderRoot.getPay_method().getDecision_id(),
                        buyRenderRoot.getPay_method().getPayapi_cache_id(),
                        buyRenderRoot.getRender_token(),
                        buyRenderParamDto.getShop_id(),
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
                log.info("?????????{}???????????????msg:{}", jdMchOrder.getTradeNo(), signUrl);
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
                        .addHeader("Cookie", douyinAppCk.getCk())
                        .addHeader("X-SS-STUB", X_SS_STUB1)
                        .addHeader("x-tt-trace-id", tarceid1)
                        .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                        .addHeader("X-Gorgon", x_gorgon1)
                        .addHeader("X-Khronos", x_khronos1)
                        .build();
                Response response1 = client.newCall(request1).execute();
                String bodyRes1 = response1.body().string();
                response1.close();
                log.info("?????????{},????????????????????????msg:{},????????????{},???????????????????????????{}", jdMchOrder.getTradeNo(), douyinDeviceIids.indexOf(douyinDeviceIid),
                        timer.interval(),
                        bodyRes1);
                if (bodyRes1.contains("order_id")) {
                    log.info("?????????:{},????????????????????????????????????", jdMchOrder.getTradeNo());
                    deleteLockCk(douyinAppCk, douyinDeviceIid);
                    redisTemplate.opsForValue().set("??????????????????:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    redisTemplate.opsForValue().set("??????????????????:" + douyinDeviceIid.getId(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    log.info("?????????:{},??????????????????uid???????????????????????????msg:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getId());
                    redisTemplate.opsForValue().set("????????????????????????:" + douyinAppCk.getUid(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    redisTemplate.opsForValue().set("????????????????????????:" + douyinAppCk.getUid(), JSON.toJSONString(douyinDeviceIid), 2000, TimeUnit.HOURS);
                    String proxyString = client.proxy().toString().split("HTTP @ /")[1];
                    String ip = proxyString.split(":")[0];
                    String port = proxyString.split(":")[1];
                    JdProxyIpPort oneIp = jdProxyIpPortMapper.selectOne(Wrappers.<JdProxyIpPort>lambdaQuery().eq(JdProxyIpPort::getIp, ip).eq(JdProxyIpPort::getPort, port));
                    if (ObjectUtil.isNotNull(oneIp)) {
                        log.info("?????????:{}????????????????????????,ip:{},port:{}", jdMchOrder.getTradeNo(), ip, port);
                        productProxyTask.buildStaticIp(oneIp);
                    }
                    log.info("?????????{}???????????????", jdMchOrder);
                    String orderId = JSON.parseObject(JSON.parseObject(bodyRes1).getString("data")).getString("order_id");
                    log.info("?????????{}??????????????????msg:{}", jdMchOrder.getTradeNo(), orderId);
                    redisTemplate.opsForValue().increment("???????????????????????????:" + douyinDeviceIid.getDeviceId());
                    redisTemplate.opsForValue().increment("????????????????????????:" + douyinAppCk.getUid());
                    douyinDeviceIid.setSuccess(douyinDeviceIid.getSuccess() == null ? 1 : douyinDeviceIid.getSuccess() + 1);
                    log.info("?????????:{}????????????????????????msg:{}", jdMchOrder.getTradeNo(), new Date().toLocaleString());
                    douyinDeviceIid.setLastSuccessTime(new Date());
                    douyinDeviceIidMapper.updateById(douyinDeviceIid);
                    PayDto payDto = PayDto.builder().ck(douyinAppCk.getCk()).device_id(douyinDeviceIid.getDeviceId()).iid(douyinDeviceIid.getIid()).pay_type(payType + "")
                            .orderId(orderId).userIp(jdLog.getIp()).build();
                    return payDto;
                } else {
                    douyinAppCk.setFailReason(douyinAppCk.getFailReason() + bodyRes1);
                    if (StrUtil.isNotBlank(bodyRes1) && bodyRes1.contains("????????????????????????")) {
                        log.info("?????????{}??????????????????????????????????????????????????????id???,????????????ip", jdMchOrder.getTradeNo());
//                        client = pcAppStoreService.buildClient();
                        //??????????????????????????????ck???????????????
                        redisTemplate.opsForValue().increment("???????????????????????????:" + douyinDeviceIid.getDeviceId());
                        redisTemplate.opsForValue().increment("????????????????????????:" + douyinAppCk.getUid());
                        douyinDeviceIid.setFail(douyinDeviceIid.getFail() + 1);
//                        douyinAppCk.setFailReason(douyinAppCk.getFailReason() + "??????????????????" + JSON.toJSONString(douyinDeviceIid));
                        douyinDeviceIidMapper.updateById(douyinDeviceIid);
                        if (douyinDeviceIid.getFail() > 10 && ObjectUtil.isNotNull(douyinDeviceIid.getLastSuccessTime())) {
                            log.info("?????????:{}??????????????????????????????deviceId:{}", jdMchOrder.getTradeNo(), douyinDeviceIid.getDeviceId());
                            douyinDeviceIid.setIsEnable(PreConstant.TWO);
                            douyinDeviceIidMapper.updateById(douyinDeviceIid);
                        }
                    }
                    PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                    douyinAppCkMapper.updateById(douyinAppCk);
                }
            } catch (Exception e) {
                if (StrUtil.isNotBlank(e.getMessage()) && e.getMessage().contains("out")) {
                    log.error("?????????:{},????????????", jdMchOrder.getTradeNo());
                    client = pcAppStoreService.buildClient();
                }
                log.error("?????????{}?????????????????????:{},?????????:{}", jdMchOrder.getTradeNo(), e.getMessage(), timer.interval());
            }
        }
        return null;
    }

    private void deleteLockCk(DouyinAppCk douyinAppCk, DouyinDeviceIid douyinDeviceIid) {
        Set<String> keys = redisTemplate.keys("????????????????????????:*");
        if (CollUtil.isNotEmpty(keys)) {
            for (String key : keys) {
                String s = redisTemplate.opsForValue().get(key);
                if (StrUtil.isNotBlank(s)) {
                    DouyinDeviceIid douyinDeviceIidLock = JSON.parseObject(s, DouyinDeviceIid.class);
                    if (douyinDeviceIidLock.getId().equals(douyinDeviceIid.getId()) && !key.contains(douyinAppCk.getUid())) {
                        log.info("???????????????????????????:{},?????????:{},{}", key, douyinAppCk.getUid(), douyinDeviceIid.getDeviceId());
                        redisTemplate.delete(key);
                    }
                }
            }
        }
    }

    public BuyRenderRoot getAndBuildBuyRender(OkHttpClient client, DouyinAppCk douyinAppCk, BuyRenderParamDto buyRenderParamDto,
                                              DouyinDeviceIid douyinDeviceIid, JdMchOrder jdMchOrder) {
        try {
            String body = SubmitUtils.buildBuyRenderParamData(buyRenderParamDto);
            if (Integer.valueOf(jdMchOrder.getPassCode()) == PreConstant.TEN) {
                body = SubmitUtils.buildBuyRenderYongHui(buyRenderParamDto);
            }
            String url = "https://ken.snssdk.com/order/buyRender?b_type_new=2&request_tag_from=lynx&os_api=22&" +
                    "device_type=SM-G973N&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=354730528934825" +
                    "&app_name=aweme&version_name=17.3.0&ts=1664384063&cpu_support64=false&app_type=normal&appTheme=dark" +
                    "&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1664384064117&device_platform=android&iid="
                    + douyinDeviceIid.getIid() +
                    "&version_code=170300&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&is_android_pad=0&openudid=199d79fbbeff0e58&device_id="
                    + douyinDeviceIid.getDeviceId() + "&resolution=720%2A1280&os_version=5.1.1&language" +
                    "=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc=46007";
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
                    .addHeader("Cookie", douyinAppCk.getCk())
                    .addHeader("X-Gorgon", x_gorgon)
                    .addHeader("X-Khronos", x_khronos)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            Response response = client.newCall(request).execute();
            String resBody = response.body().string();
            log.info("?????????{}??????????????????msg:{}", jdMchOrder.getTradeNo(), resBody);
            if (StrUtil.isNotBlank(resBody) && resBody.contains("??????")) {
                log.error("?????????{}???????????????ck??????", jdMchOrder.getTradeNo());
                douyinAppCk.setIsEnable(PreConstant.FUYI_1);
                douyinAppCk.setFailReason(douyinAppCk.getFailReason() + resBody);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                douyinAppCkMapper.updateById(douyinAppCk);
            }
            response.close();
            BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(resBody).getString("data"), BuyRenderRoot.class);
            return buyRenderRoot;
        } catch (Exception e) {
            if (StrUtil.isNotBlank(e.getMessage()) && e.getMessage().contains("out")) {
                log.error("?????????:{},????????????????????????client", jdMchOrder.getTradeNo());
                client = pcAppStoreService.buildClient();
            }
            log.error("?????????{}?????????????????????????????????msg:{}", jdMchOrder.getTradeNo(), e.getMessage());
        }
        return null;
    }

    private String getSignUrl() {
        String signUrl = redisTemplate.opsForValue().get("??????????????????");
        if (StrUtil.isBlank(signUrl)) {
            signUrl = "http://110.42.246.12:8191/tt1213";
            redisTemplate.opsForValue().set("??????????????????", "http://110.42.246.12:8191/tt1213");
        }
        return signUrl;
    }


    public void selectOrderStataus(JdOrderPt jdOrderPt, JdMchOrder jdMchOrder) {
        String isfindOrderStatus = redisTemplate.opsForValue().get("??????????????????????????????:" + jdMchOrder.getTradeNo().trim());
        if (StrUtil.isBlank(isfindOrderStatus)) {
            log.info("?????????:{}????????????????????????????????????", jdMchOrder.getTradeNo());
            return;
        }
        if (DateUtil.offsetSecond(jdMchOrder.getCreateTime(), 40).getTime() > new Date().getTime()) {
            log.info("?????????:{},???40????????????????????????", jdMchOrder.getTradeNo());
            return;
        }
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdOrderPt), 1, TimeUnit.MINUTES);
        if (!ifAbsent) {
            log.info("????????????,{},?????????????????????????????????", jdMchOrder.getTradeNo());
            return;
        }
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        OkHttpClient client = builder.build();
//        Response response = client.newCall(request).execute();
        String dali = redisTemplate.opsForValue().get("??????????????????");
        if (Integer.valueOf(dali) == 1) {
            client = pcAppStoreService.buildClient();
        }
        PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
        log.info("?????????{}?????????????????????", jdMchOrder.getTradeNo());
        DouyinDeviceIid douyinDeviceIid = JSON.parseObject(jdOrderPt.getMark(), DouyinDeviceIid.class);
        String findOrderTime = redisTemplate.opsForValue().get("??????????????????");
        for (int i = 0; i < Integer.valueOf(findOrderTime); i++) {
            jdOrderPt = jdOrderPtMapper.selectById(jdOrderPt.getId());
            if (StrUtil.isNotBlank(jdOrderPt.getOrgAppCk())) {
                DateTime dateTime = DateUtil.parseDateTime(jdOrderPt.getOrgAppCk());
                if (DateUtil.offsetMinute(dateTime, -12).getTime() > jdMchOrder.getCreateTime().getTime()) {
                    log.info("????????????{}+10?????????????????????????????????????????????????????????????????????????????????????????????", jdMchOrder.getTradeNo());
                    return;
                }
            }
            log.info("?????????{}???????????????????????????:{}", jdMchOrder.getTradeNo(), i);
            if (i >= 10) {
                Set<String> keys = redisTemplate.keys("??????????????????:*");
                List<String> ids = keys.stream().map(it -> it.replace("??????????????????:", "")).collect(Collectors.toList());
                douyinDeviceIid = douyinDeviceIidMapper.selectById(Integer.valueOf(ids.get(PreUtils.randomCommon(0, ids.size() - 1, 1)[0])));
            }
            String url = String.format("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?" +
                            "aid=%s&order_id=%s",
                    PreUtils.randomCommon(100, 1000000, 1)[0] + "", jdOrderPt.getOrderId());
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Cookie", jdOrderPt.getCurrentCk())
                    .build();
            String body = null;
            try {
                Response response = client.newCall(request).execute();
                body = response.body().string();
                response.close();
            } catch (Exception e) {
                log.info("?????????{},???????????????????????????????????????-----", jdMchOrder.getTradeNo());
            }
//            String body = HttpRequest.get(url).header("cookie", jdOrderPt.getCurrentCk()).execute().body();
            log.info("?????????{}?????????????????????????????????msg:{}", jdMchOrder.getTradeNo(), body);
            if (StrUtil.isBlank(body)) {
                log.info("?????????{}????????????????????????????????????????????????XXXXXXXXXXXXXXX", jdMchOrder.getTradeNo(), body);
                continue;
            }
            String findOrderData = DateUtil.formatDateTime(new Date());
            log.info("????????????{}?????????????????????:{}", jdMchOrder.getTradeNo(), findOrderData);
            String html = JSON.parseObject(body).getString("order_detail_info");
            jdOrderPt.setHtml(html);
            jdOrderPt.setOrgAppCk(findOrderData);
            PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
            jdOrderPtMapper.updateById(jdOrderPt);
            String voucher_info_listStr = JSON.parseObject(html).getString("voucher_info_list");
            if (StrUtil.isBlank(voucher_info_listStr) || !voucher_info_listStr.contains("voucher_status")) {
                return;
            }
            List<JSONObject> voucher_info_list = JSON.parseArray(voucher_info_listStr, JSONObject.class);
            if (CollUtil.isNotEmpty(voucher_info_list)) {
                JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
                String code = voucher_info.getString("code");
                if (StrUtil.isNotBlank(code)) {
                    PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                    log.info("?????????{}??????????????????????????????msg:{}", jdMchOrder.getTradeNo(), code);
                    jdOrderPt.setCardNumber(code);
                    jdOrderPt.setCarMy(code);
                    jdOrderPt.setSuccess(PreConstant.ONE);
                    jdOrderPt.setPaySuccessTime(new Date());
                    jdOrderPtMapper.updateById(jdOrderPt);
                    jdMchOrder.setStatus(PreConstant.TWO);
                    PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                    jdMchOrderMapper.updateById(jdMchOrder);
                    jdOrderPtMapper.updateById(jdOrderPt);
                    jdOrderPtMapper.updateById(jdOrderPt);
                    log.info("????????????{}???????????????????????????,pin:{}", jdMchOrder.getTradeNo());
//                    Integer maxPrice = douyinMaxPrice();
//                    DateTime endOfDay = DateUtil.endOfDay(new Date());
//                    DateTime beginOfDay = DateUtil.beginOfDay(new Date());
//                    Integer sku_price_total = jdOrderPtMapper.selectDouYinByStartTimeAndEndAndUid(jdOrderPt.getPtPin(), beginOfDay, endOfDay);
//                    log.info("?????????:{},????????????:{},????????????:{}", jdMchOrder.getTradeNo(), jdOrderPt.getPtPin(), maxPrice - sku_price_total);
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
        Integer count = jdOrderPtMapper.selectCount(wrapper);
        String s = redisTemplate.opsForValue().get("??????????????????????????????:" + uid);
        if (count > 0) {
            return Integer.valueOf(s);
        } else {
            log.info("????????????????????????????????????????????????????????????????????????0");
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
//        redisTemplate.opsForValue().setIfAbsent("???????????????:{}");
    }

    @Scheduled(cron = "0/10 * * * * ?")
    @Async("asyncPool")
    public void synProductMaxPrirce() {
        for (int i = 0; i < 4; i++) {
            PreTenantContextHolder.setCurrentTenantId(i % 2 == 0 ? 1L : 2L);
            Integer maxPrice = douyinMaxPrice();
            DateTime endOfDay = DateUtil.endOfDay(new Date());
            DateTime beginOfDay = DateUtil.beginOfDay(new Date());
            List<Map<String, Object>> mapList = jdOrderPtMapper.selectDouYinByStartTimeAndEndAndUidGroup(beginOfDay, endOfDay);
            List<DouyinAppCk> douyinAppCks = douyinAppCkMapper.selectList(Wrappers.<DouyinAppCk>lambdaQuery().eq(DouyinAppCk::getIsEnable, PreConstant.ONE));
            List<String> skuyesterdays = jdOrderPtMapper.selectOrderSuccessYesterday(beginOfDay);
            log.info("????????????????????????????????????");
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
                        redisTemplate.opsForValue().set("??????????????????????????????:" + pt_pin, (maxPrice - sku_price_total) + "");
                    } else {
                        if (sku_price_total == 100) {
                            redisTemplate.opsForValue().set("??????????????????????????????:" + pt_pin, (100 - sku_price_total) + "");
                        } else {
                            redisTemplate.opsForValue().set("??????????????????????????????:" + pt_pin, (200 - sku_price_total) + "");
                        }
                    }
                }
            }
        }
    }

    private Integer douyinMaxPrice() {
        String douyinMaxPrice = redisTemplate.opsForValue().get("?????????????????????????????????");
        if (StrUtil.isBlank(douyinMaxPrice)) {
            redisTemplate.opsForValue().set("?????????????????????????????????", "5000");
            return 5000;
        } else {
            return Integer.valueOf(douyinMaxPrice);
        }
    }

}
