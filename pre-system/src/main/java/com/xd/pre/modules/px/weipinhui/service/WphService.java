package com.xd.pre.modules.px.weipinhui.service;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.IPUtil;
import com.xd.pre.common.utils.R;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.px.jddj.pay.PayCombine;
import com.xd.pre.modules.px.service.JdDjService;
import com.xd.pre.modules.px.weipinhui.ApiSign;
import com.xd.pre.modules.px.weipinhui.AuthService;
import com.xd.pre.modules.px.weipinhui.CaptchaData;
import com.xd.pre.modules.px.weipinhui.CaptchaXY;
import com.xd.pre.modules.px.weipinhui.aes.*;
import com.xd.pre.modules.px.weipinhui.baiduyun.Location;
import com.xd.pre.modules.px.weipinhui.baiduyun.WordsResult;
import com.xd.pre.modules.px.weipinhui.create.CreateCaptchaFlow;
import com.xd.pre.modules.px.weipinhui.create.OrderData;
import com.xd.pre.modules.px.weipinhui.create.WphCreateDto;
import com.xd.pre.modules.px.weipinhui.create.WphParamCaptchaTokenAndUUID;
import com.xd.pre.modules.px.weipinhui.findOrder.CreateOrderSyn;
import com.xd.pre.modules.px.weipinhui.findOrder.CreateWphAccountSyn;
import com.xd.pre.modules.px.weipinhui.findOrder.FindSyn;
import com.xd.pre.modules.px.weipinhui.token.VipTank;
import com.xd.pre.modules.px.weipinhui.yezi.YeZiGetMobileDto;
import com.xd.pre.modules.px.yezijiema.YeZiUtils;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.mapper.JdAppStoreConfigMapper;
import com.xd.pre.modules.sys.mapper.JdMchOrderMapper;
import com.xd.pre.modules.sys.mapper.JdOrderPtMapper;
import com.xd.pre.modules.sys.mapper.WphAccountMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.activemq.ScheduledMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.Queue;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WphService {
    /**
     * //        Jedis jedis = RedisDS.create().getJedis();
     * //        jedis.set("?????????????????????:" + captchaXY.getPhone(), JSON.toJSONString(captchaXY));
     * //        jedis.expire("?????????????????????:" + captchaXY.getPhone(), captchaXY.getVipTank().getTANK_EXPIRE() - 3600);
     */


    @Resource
    private WphAccountMapper wphAccountMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdDjService jdDjService;

    @Resource
    private JdOrderPtMapper jdOrderPtMapper;

    @Resource
    private JdAppStoreConfigMapper jdAppStoreConfigMapper;

    @Autowired
    private PayCombine payCombine;

    @Resource
    private JdMchOrderMapper jdMchOrderMapper;


    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Resource(name = "findwph_queue")
    private Queue findwph_queue;
    @Resource(name = "findwph_queue_code")
    private Queue findwph_queue_code;


    @Resource(name = "create_order_wph_queue")
    private Queue create_order_wph_queue;
    @Resource(name = "create_order_wph_queue_code")
    private Queue create_order_wph_queue_code;

    @Resource(name = "create_account_wph_queue")
    private Queue create_account_wph_queue;
    @Resource(name = "create_account_wph_queue_code")
    private Queue create_account_wph_queue_code;

    /**
     * match??????
     *
     * @param jdMchOrder
     * @param jdLog
     */
    public R match(JdMchOrder jdMchOrder, JdAppStoreConfig jdAppStoreConfig, JdLog jdLog) {
        TimeInterval timer = DateUtil.timer();
        OkHttpClient client = buildClient();
        log.info("????????????ip??????client??????:?????????{}", timer.interval());
        log.info("???????????????????????????????????????????????????????????????????????????????????????????????????????????????");
        LambdaQueryWrapper<JdOrderPt> wrapper = getJdOrderPtLambdaQueryWrapper(jdAppStoreConfig);
        Integer count = jdOrderPtMapper.selectCount(wrapper);
        if (count < PreConstant.TWENTY) {
            log.info("?????????????????????????????????msg:{}", count);
            String lockWph = redisTemplate.opsForValue().get("????????????????????????:" + jdAppStoreConfig.getSkuId());
            if (StrUtil.isBlank(lockWph)) {
                sendMessageSenc(this.create_order_wph_queue, JSON.toJSONString(jdAppStoreConfig), 10);
                sendMessageSenc(this.create_account_wph_queue, JSON.toJSONString(jdAppStoreConfig), 10);
            }
        }
        if (count <= PreConstant.TWO) {
            log.error("??????????????????????????????????????????????????????");
            return null;
        }
        List<Integer> pageIndexList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int indexOne = PreUtils.randomCommon(1, count - 1, 1)[0];
            pageIndexList.add(indexOne);
        }
        Page<JdOrderPt> pageIndex = new Page<>(pageIndexList.stream().sorted().collect(Collectors.toList()).get(PreConstant.ZERO), 1);
        Page<JdOrderPt> page = jdOrderPtMapper.selectPage(pageIndex, wrapper);
        log.info("??????????????????????????????????????????");
        JdOrderPt jdOrderPtDb = page.getRecords().get(PreConstant.ZERO);
        log.info("?????????????????????????????????msg:{}", jdOrderPtDb);
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("?????????????????????:" + jdOrderPtDb.getPtPin(), JSON.toJSONString(jdOrderPtDb), 20, TimeUnit.SECONDS);
        if (!ifAbsent) {
            log.error("?????????????????????????????????msg:{}", jdOrderPtDb.getOrderId());
            return null;
        }
        ifAbsent = redisTemplate.opsForValue().setIfAbsent("?????????????????????:" + jdOrderPtDb.getOrderId(), JSON.toJSONString(jdOrderPtDb), 5, TimeUnit.MINUTES);
        if (!ifAbsent) {
            log.error("????????????????????????????????????????????????msg:{}", jdOrderPtDb.getPtPin());
            return null;
        }
        log.info("????????????????????????????????????????????????");
        Map<String, String> headerMap = PreUtils.buildIpMap(jdLog.getIp());
        WphCreateDto wphCreateDto = JSON.parseObject(jdOrderPtDb.getMark(), WphCreateDto.class);
        CaptchaXY captchaXY = JSON.parseObject(wphCreateDto.getWphAccount().getLoginInfo(), CaptchaXY.class);
        String ck = captchaXY.getVipTank().getVIP_TANK();
        log.info("????????????????????????:?????????{}", timer.interval());
        try {
            String payUrl = getPayUrl(jdOrderPtDb.getOrderId(), client, ck, headerMap);
            log.info("????????????payUrl1:?????????{}", timer.interval());
            log.info("???????????????");
            String npayvid = cashier(payUrl, client, ck, headerMap);
            log.info("????????????npayvid2:?????????{}", timer.interval());
            log.info("????????????????????????ck?????????msg:{}", npayvid);
            log.info("?????????????????????");
            BigDecimal preview = preview(client, ck, npayvid, headerMap);
            log.info("????????????preview3:?????????{}", timer.interval());
            if (preview.compareTo(jdOrderPtDb.getSkuPrice()) != 0) {
                log.info("???????????????????????????????????????msg:{}", jdOrderPtDb.getOrderId());
            }
            log.info("?????????????????????");
            log.info("??????dopay");
            String redirectUrl = doPay(client, ck, npayvid, headerMap);
            log.info("????????????redirectUrl4:?????????{}", timer.interval());
            if (StrUtil.isBlank(redirectUrl)) {
                return null;
            }
            String tenpay = getTenpay(client, ck, npayvid, redirectUrl, headerMap);
            log.info("????????????redirectUrl5:?????????{}", timer.interval());
            log.info("???????????????????????????msg:{}", tenpay);
            if (StrUtil.isBlank(tenpay)) {
                return null;
            }
            String hrefUrl = null;
            for (int i = 0; i < 4; i++) {
                hrefUrl = payCombine.weixinUrl(tenpay, headerMap, "https://h5.vip.com/order/list.html?_show_header=1");
                if (StrUtil.isNotBlank(hrefUrl)) {
                    break;
                }
            }
            log.info("????????????hrefUrl6:?????????{}", timer.interval());
            if (StrUtil.isBlank(hrefUrl)) {
                log.info("??????????????????????????????msg:{}", tenpay);
                return null;
            }
            ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), "????????????", 10, TimeUnit.MINUTES);
            if (!ifAbsent) {
                log.error("????????????????????????msg:{}");
                return null;
            }
            jdMchOrder.setOriginalTradeId(jdOrderPtDb.getId());
            jdMchOrder.setOriginalTradeNo(jdOrderPtDb.getOrderId());
            long l = (System.currentTimeMillis() - jdMchOrder.getCreateTime().getTime()) / 1000;
            jdMchOrder.setMatchTime(l);
            this.jdMchOrderMapper.updateById(jdMchOrder);
            jdOrderPtDb.setIsMatch(PreConstant.ONE);
            jdOrderPtDb.setHrefUrl(hrefUrl);
            jdOrderPtDb.setWeixinUrl(hrefUrl);
            jdOrderPtDb.setIsWxSuccess(PreConstant.ONE);
            this.jdOrderPtMapper.updateById(jdOrderPtDb);
            log.info("?????????????????????????????????msg:{}", timer.interval());
            return R.ok(jdMchOrder);
        } catch (Exception e) {
            redisTemplate.delete("?????????????????????:" + jdOrderPtDb.getPtPin());
            redisTemplate.delete("?????????????????????:" + jdOrderPtDb.getOrderId());
            log.error("????????????msg:{}", e.getMessage());
        }
        return null;

    }

    private LambdaQueryWrapper<JdOrderPt> getJdOrderPtLambdaQueryWrapper(JdAppStoreConfig jdAppStoreConfig) {
        LambdaQueryWrapper<JdOrderPt> wrapper = Wrappers.<JdOrderPt>lambdaQuery()
                .eq(JdOrderPt::getSkuId, jdAppStoreConfig.getSkuId())
                .gt(JdOrderPt::getExpireTime, new Date())
                .eq(JdOrderPt::getIsMatch, PreConstant.ZERO)
                .isNull(JdOrderPt::getPaySuccessTime);
        Set<String> keys = redisTemplate.keys("?????????????????????:*");
        Set<String> keysLockAccountKeys = redisTemplate.keys("?????????????????????:*");
        if (CollUtil.isNotEmpty(keys)) {
            List<String> orderIdsLock = keys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
            log.info("??????????????????????????????msg:{}", orderIdsLock);
            wrapper.notIn(JdOrderPt::getOrderId, orderIdsLock);
        }
        if (CollUtil.isNotEmpty(keysLockAccountKeys)) {
            List<String> orderAccountLock = keysLockAccountKeys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
            log.info("?????????????????????msg:{}", orderAccountLock);
            wrapper.notIn(JdOrderPt::getPtPin, orderAccountLock);
        }
        return wrapper;
    }


    @Async("asyncPoolRet")
    public R createOrder(String skuId) {
        try {
            JdAppStoreConfig skuConfig = jdAppStoreConfigMapper.selectOne(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getSkuId, skuId));
            log.info("??????????????????");
            Map<String, String> headerMap = PreUtils.buildIpMap(IPUtil.getRandomIp());
            log.info("????????????");
//            List<String> orderMax = this.jdOrderPtMapper.selectwphOrderMax(20);
            LambdaQueryWrapper<WphAccount> wrapper = Wrappers.<WphAccount>lambdaQuery().eq(WphAccount::getIsEnable, PreConstant.ONE)
                    .gt(WphAccount::getExpireTime, new Date())/*.notIn(WphAccount::getPhone, orderMax)*/;
            Integer count = wphAccountMapper.selectCount(wrapper);
            if (count <= PreConstant.TWENTY) {
                log.info("????????????????????????");
                sendMessageSenc(this.create_account_wph_queue, JSON.toJSONString(skuConfig), 30);
            }
            if (count <= PreConstant.THREE) {
                log.info("???????????????????????????msg:{}", count);
                return null;
            }
            Set<String> keys = redisTemplate.keys("?????????????????????????????????:*");
            if (CollUtil.isNotEmpty(keys)) {
                List<String> productLockAccount = keys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
                log.debug("?????????????????????msg:{}", productLockAccount);
                wrapper.notIn(WphAccount::getPhone, productLockAccount);
            }
            count = wphAccountMapper.selectCount(wrapper);
            List<Integer> pageNum = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int i1 = PreUtils.randomCommon(1, count, 1)[0];
                pageNum.add(i1);
            }
            pageNum = pageNum.stream().sorted().collect(Collectors.toList());
            Integer pageIndex = pageNum.get(PreConstant.ZERO);
            Page<WphAccount> wphAccountPage = new Page<>(pageIndex, 1);
            wphAccountPage = wphAccountMapper.selectPage(wphAccountPage, wrapper);
            WphAccount wphAccountDb = wphAccountPage.getRecords().get(PreConstant.ZERO);
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("?????????????????????????????????:" + wphAccountDb.getPhone(), wphAccountDb.getPhone(), 1, TimeUnit.MINUTES);
            if (!ifAbsent) {
                log.info("????????????????????????????????????");
                sendMessageSenc(this.create_order_wph_queue, JSON.toJSONString(skuConfig), 70);
                return null;
            }
            log.info("??????????????????msg:{}", wphAccountDb);
            log.info("??????mars_cid???mars_sid");
            String hexByLenth = ParamAes.getHexByLenth(32);
            String mars_cid = String.format("mars_cid=%s_%s", System.currentTimeMillis() + "", hexByLenth);
            String mars_sid = hexByLenth;
            OkHttpClient client = buildClient();
            log.info("???????????????????????????????????????");
            String phone = YeZiUtils.get_mobileByDto(YeZiGetMobileDto.getWphRandom(redisTemplate), redisTemplate);
            if (StrUtil.isNotBlank(phone)) {
                redisTemplate.opsForValue().set("???????????????:" + phone, System.currentTimeMillis() + "", 180, TimeUnit.SECONDS);
            } else {
                return null;
            }
            log.info("???????????????????????????msg:{}", phone);
            WphCreateDto wphCreateDto = WphCreateDto.builder().mars_cid(mars_cid).mars_sid(mars_sid).phone(phone).wphAccount(wphAccountDb).build();
            log.info("??????????????????msg:{}", wphCreateDto);
            wphCreateDto = captchaTokenAndUUID(client, wphCreateDto, skuConfig, headerMap);
            wphCreateDto.setJdAppStoreConfig(skuConfig);
            if (ObjectUtil.isNull(wphCreateDto)) {
                log.error("??????????????????");
                return null;
            }
            CreateOrderSyn createOrderSyn = CreateOrderSyn.builder().
                    skuConfig(skuConfig).headerMap(headerMap).wphAccountDb(wphAccountDb).phone(phone).wphCreateDto(wphCreateDto)
                    .createDate(new Date())
                    .build();
            log.info("????????????????????????msg:{}", phone);
            sendMessageSenc(create_order_wph_queue_code, JSON.toJSONString(createOrderSyn), 10);
//            createOrderSyn(skuConfig, headerMap, wphAccountDb, phone, wphCreateDto, code);
            return R.ok();
        } catch (Exception e) {
            log.error("??????????????????msg:{}", e.getMessage());
        }
        return null;
    }

    private Boolean createOrderSyn(CreateOrderSyn createOrderSyn) {
        //JdAppStoreConfig skuConfig, Map<String, String> headerMap,
        //                                WphAccount wphAccountDb, String phone, WphCreateDto wphCreateDto, String code

        JdAppStoreConfig skuConfig = createOrderSyn.getSkuConfig();
        Map<String, String> headerMap = createOrderSyn.getHeaderMap();
        WphAccount wphAccountDb = createOrderSyn.getWphAccountDb();
        String phone = createOrderSyn.getPhone();
        WphCreateDto wphCreateDto = createOrderSyn.getWphCreateDto();
        String code = createOrderSyn.getCode();
        if (StrUtil.isBlank(code)) {
            log.error("???????????????????????????msg:{}", phone);
            return false;
        }
        OkHttpClient client = buildClient();

        wphCreateDto.setCode(code);
        log.info("?????????????????????");
        WphCreateDto wphCreateDtoT = wphCreateDto;

        wphCreateDto = createCaptchaFlow(client, wphCreateDto, headerMap);
        if (ObjectUtil.isNull(wphCreateDto)) {
            log.info("????????????????????????????????? createCaptchaFlow");
            wphCreateDto = createCaptchaFlow(client, wphCreateDtoT, headerMap);
        }
        log.info("???????????????????????????");
        log.info("??????????????????????????????msg:{}", JSON.toJSONString(wphCreateDto));
        wphCreateDtoT = wphCreateDto;
        wphCreateDto = ecouponOrderCreate(client, wphCreateDto, headerMap);
        if (ObjectUtil.isNull(wphCreateDto)) {
            log.info("???????????????ecouponOrderCreate");
            wphCreateDto = ecouponOrderCreate(client, wphCreateDtoT, headerMap);
        }
        log.info("??????????????????");
        log.info("????????????????????????????????????msg:{}", wphCreateDto);
        if (ObjectUtil.isNull(wphCreateDto) || ObjectUtil.isNull(wphCreateDto.getOrderData())) {
            log.error("????????????????????????????????????msg:{}??????????????????Msg:{}", wphAccountDb.getPhone(), wphCreateDto.getPhone());
            return false;
        }
        JdOrderPt jdOrderPtInsert = JdOrderPt.builder().orderId(wphCreateDto.getOrderData().getOrderId()).ptPin(wphAccountDb.getPhone()).success(PreConstant.ZERO)
                .expireTime(DateUtil.offsetSecond(new Date(), 1500)).createTime(new Date()).skuPrice(skuConfig.getSkuPrice())
                .skuName(skuConfig.getSkuName()).skuId(skuConfig.getSkuId()).prerId(wphCreateDto.getOrderData().getOrderNo())
                .currentCk(wphCreateDto.getCidAndSidAndTank()).orgAppCk(wphCreateDto.getCidAndSidAndTank())
                .wphCardPhone(wphCreateDto.getPhone())
                .mark(JSON.toJSONString(wphCreateDto)).build();
        int insert = jdOrderPtMapper.insert(jdOrderPtInsert);
        log.info("?????????????????????????????????msg???{}", JSON.toJSONString(jdOrderPtInsert));
        return true;
    }

    private WphCreateDto ecouponOrderCreate(OkHttpClient client, WphCreateDto wphCreateDto, Map<String, String> headerMap) {
        try {
            String url = "https://h5.vip.com/api/virtual/EcouponOrder/create";
            RequestBody requestBody = new FormBody.Builder()
                    .add("captchaId", wphCreateDto.getCreateCaptchaFlow().getCaptchaId())
                    .add("product_id", wphCreateDto.getJdAppStoreConfig().getSkuId())
                    .add("order_src", "VIP_SPE_WAP")
                    .add("buy_num", "1")
                    .add("num", "1")
                    .add("type", "vipCard")
                    .add("cardPasswdMobile", wphCreateDto.getPhone())
                    .add("useBindMobile", "0")
                    .add("captchaToken", wphCreateDto.getWphParamCaptchaTokenAndUUID().getCaptchaToken())
                    .add("uuid", wphCreateDto.getWphParamCaptchaTokenAndUUID().getUuid())
                    .add("verifyCode", wphCreateDto.getCode())
                    .add("openInvoice", "0")
                    .build();

            Request.Builder builder = new Request.Builder().url(url)
                    .header("cookie", wphCreateDto.getCidAndSidAndTank())
                    .post(requestBody);
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????ecouponOrderCreate??????msg:{}", resStr);
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 0) {
                if (JSON.parseObject(resStr).getInteger("code") == 400219) {
                    log.info("?????????????????????msg (400219)????????????????????????????????????????????????????????????????????????");
                    log.info("??????????????????????????????????????????msg:{}", wphCreateDto.getWphAccount().getPhone());
                    WphAccount wphAccount = wphCreateDto.getWphAccount();
                    wphAccount.setIsEnable(PreConstant.ZERO);
                    wphAccountMapper.updateById(wphAccount);
                    return null;
                }
                log.error("??????ecouponOrderCreate?????????");
                return null;
            }
            OrderData orderData = JSON.parseObject(JSON.parseObject(resStr).getString("data"), OrderData.class);
//            vipTank.setCreateDate(DateUtil.formatDateTime(new Date()));
            wphCreateDto.setOrderData(orderData);
            return wphCreateDto;
        } catch (Exception e) {
            log.error("??????ticketLogin??????msg:{}", e.getMessage());
        }
        return null;
    }

    private WphCreateDto createCaptchaFlow(OkHttpClient client, WphCreateDto wphCreateDto, Map<String, String> headerMap) {
        try {
            String url = String.format("https://h5.vip.com/api/virtual/EcouponOrder/createCaptchaFlow",
                    wphCreateDto.getPhone());
            Request.Builder builder = new Request.Builder().url(url)
                    .get()
                    .header("cookie", wphCreateDto.getCidAndSidAndTank());
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????createCaptchaFlow???????????????msg:{}", resStr);
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 0) {
                log.error("createCaptchaFlow msg:{}", wphCreateDto);
                return null;
            }
            String data = JSON.parseObject(resStr).getString("data");
            CreateCaptchaFlow createCaptchaFlow = JSON.parseObject(data, CreateCaptchaFlow.class);
            wphCreateDto.setCreateCaptchaFlow(createCaptchaFlow);
            return wphCreateDto;
        } catch (Exception e) {
            log.error("createCaptchaFlow??????msg:{}", e.getMessage());
        }
        return null;
    }

    private WphParamCaptchaTokenAndUUID captchaTokenAndUUIDByPic(OkHttpClient client, WphCreateDto wphCreateDto, WordsResult wordsResult, Integer x,
                                                                 JdAppStoreConfig config, Map<String, String> headerMap) {
        if (x >= 5) {
            return null;
        }
        x = x + 1;
        try {
            String url = String.format("https://h5.vip.com/api/virtual/Ecoupon/checkSmsPicCaptcha?mobile=%s&useBindMobile=0&captchaCode=%s&picUuid=%s",
                    wphCreateDto.getPhone(), wordsResult.getWords().replace(" ", ""), wphCreateDto.getWphParamCaptchaTokenAndUUID().getPicUuid());
            Request.Builder builder = new Request.Builder().url(url)
                    .header("cookie", wphCreateDto.getCidAndSidAndTank())
                    .get();
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("captchaTokenAndUUIDByPic???????????????msg:{}", resStr);
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 0) {
                log.error("?????????????????????captchaTokenAndUUIDByPicmsg:{}", wphCreateDto);
                return null;
            }
            String data = JSON.parseObject(resStr).getString("data");
            WphParamCaptchaTokenAndUUID wphParamCaptchaTokenAndUUID = JSON.parseObject(data, WphParamCaptchaTokenAndUUID.class);
            if (ObjectUtil.isNotNull(wphParamCaptchaTokenAndUUID) && StrUtil.isNotBlank(wphParamCaptchaTokenAndUUID.getCaptchaToken())) {
                log.info("WphParamCaptchaTokenAndUUID???????????????msg:{}", wphParamCaptchaTokenAndUUID);
                return wphParamCaptchaTokenAndUUID;
            }
            if (ObjectUtil.isNotNull(wphParamCaptchaTokenAndUUID) && StrUtil.isNotBlank(wphParamCaptchaTokenAndUUID.getPicUuid())) {
                log.info("????????????");
                String picData = wphParamCaptchaTokenAndUUID.getPic().replace("\\", "");
                List<WordsResult> wordsResultsQp = getWordsResultsByPic(config, picData);
                if (CollUtil.isNotEmpty(wordsResultsQp)) {
                    return captchaTokenAndUUIDByPic(client, wphCreateDto, wordsResultsQp.get(PreConstant.ZERO), x, config, PreUtils.buildIpMap(IPUtil.getRandomIp()));
                }
            }
        } catch (Exception e) {
            log.error("??????captchaTokenAndUUID??????msg:{}", e.getMessage());
        }
        return null;
    }

    private static void setHeader(Map<String, String> headerMap, Request.Builder builder) {
        if (CollUtil.isNotEmpty(headerMap)) {
            for (String key : headerMap.keySet()) {
                builder.header(key, headerMap.get(key));
            }
        }
    }

    private WphCreateDto captchaTokenAndUUID(OkHttpClient client, WphCreateDto wphCreateDto, JdAppStoreConfig jdAppStoreConfig, Map<String, String> headerMap) {
        try {
            String url = String.format("https://h5.vip.com/api/virtual/Ecoupon/getSmsCaptcha?mobile=%s&useBindMobile=0",
                    wphCreateDto.getPhone());
            Request.Builder builder = new Request.Builder().url(url)
                    .header("cookie", wphCreateDto.getCidAndSidAndTank())
                    .get();
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????captchaTokenAndUUID???????????????msg:{}", resStr);
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 0) {
                log.error("?????????????????????msg:{}", wphCreateDto);
                return null;
            }
            String data = JSON.parseObject(resStr).getString("data");
            WphParamCaptchaTokenAndUUID wphParamCaptchaTokenAndUUID = JSON.parseObject(data, WphParamCaptchaTokenAndUUID.class);
            if (ObjectUtil.isNull(wphParamCaptchaTokenAndUUID) || StrUtil.isNotBlank(wphParamCaptchaTokenAndUUID.getPic())) {
                log.info("??????????????????????????????++++++++++");
                String picData = wphParamCaptchaTokenAndUUID.getPic().replace("\\", "");
                wphParamCaptchaTokenAndUUID.setPic(picData);
                List<WordsResult> wordsResultsQp = getWordsResultsByPic(jdAppStoreConfig, picData);
                if (ObjectUtil.isNull(wordsResultsQp)) {
                    log.info("????????????");
                    return null;
                }
                log.info("????????????");
                wphCreateDto.setWphParamCaptchaTokenAndUUID(wphParamCaptchaTokenAndUUID);
                log.info("??????????????????????????????????????????");
                wphParamCaptchaTokenAndUUID = captchaTokenAndUUIDByPic(client, wphCreateDto, wordsResultsQp.get(PreConstant.ZERO), PreConstant.ZERO, jdAppStoreConfig, headerMap);
            }
            if (ObjectUtil.isNotNull(wphParamCaptchaTokenAndUUID) && StrUtil.isNotBlank(wphParamCaptchaTokenAndUUID.getCaptchaToken())) {
                wphCreateDto.setWphParamCaptchaTokenAndUUID(wphParamCaptchaTokenAndUUID);
                return wphCreateDto;
            }
            log.error("??????captchaTokenAndUUID??????msg:{}", wphParamCaptchaTokenAndUUID);
        } catch (Exception e) {
            log.error("??????captchaTokenAndUUID??????msg:{}", e.getMessage());
        }
        return null;
    }

    private List<WordsResult> getWordsResultsByPic(JdAppStoreConfig jdAppStoreConfig, String picData) {
        List<WordsResult> wordsResultsQp = AuthService.parseCapData(null, picData, redisTemplate);
        if (CollUtil.isEmpty(wordsResultsQp) || wordsResultsQp.size() != 1) {
            log.error("???????????????msg:{}");
//            return null;
        }
        List<WordsResult> returnw = new ArrayList<>();
        if (ObjectUtil.isNotNull(wordsResultsQp) && wordsResultsQp.size() >= 2) {
            log.info("???????????????????????????????????????");
            StringBuilder stringBuilder = new StringBuilder();
            for (WordsResult wordsResult : wordsResultsQp) {
                stringBuilder.append(wordsResult.getWords());
            }
            WordsResult wordsResultOne = new WordsResult();
            wordsResultOne.setWords(stringBuilder.toString());
            returnw.add(wordsResultOne);
            return returnw;
        }

        return wordsResultsQp;
    }


    @Async("asyncPoolRet")
    public R register() {
        try {
            OkHttpClient client = buildClient();
//        String phone = "17821011781";
            String phone = YeZiUtils.get_mobileByDto(YeZiGetMobileDto.getWphRandomShiKa(redisTemplate), redisTemplate);
            if (StrUtil.isNotBlank(phone)) {
                redisTemplate.opsForValue().set("???????????????:" + phone, System.currentTimeMillis() + "", 180, TimeUnit.SECONDS);
            }
            log.info("???????????????msg???{}", phone);
            //????????????????????????
            Integer count = wphAccountMapper.selectCount(Wrappers.<WphAccount>lambdaQuery().eq(WphAccount::getPhone, phone).gt(WphAccount::getExpireTime, new Date()));
            if (count == PreConstant.ONE) {
                log.error("?????????????????????????????????,???????????????");
                Boolean free_mobile = YeZiUtils.free_mobile(phone, redisTemplate);
                log.error("?????????????????????msg:{}", free_mobile);
                return null;
            }
            Map<String, String> headerMap = PreUtils.buildIpMap(IPUtil.getRandomIp());
            log.info("???????????????IPMsg:{}", headerMap);
            CaptchaXY postMsgLogin = postMsg(client, phone, headerMap);
            if (ObjectUtil.isNull(postMsgLogin)) {
                log.error("????????????");
                YeZiUtils.free_mobile(phone, redisTemplate);
                log.info("???????????????");
                return null;
            }
            log.info("?????????????????????????????????????????????msg:{}", JSON.toJSONString(postMsgLogin));
            CreateWphAccountSyn createWphAccountSynData = CreateWphAccountSyn.builder().headerMap(headerMap).postMsgLogin(postMsgLogin)
                    .phone(phone).createDate(new Date()).build();
            sendMessageSenc(this.create_account_wph_queue_code, JSON.toJSONString(createWphAccountSynData), 10);
//            WphAccount wphAccount = createWphAccountSyn(createWphAccountSynData);
            return R.ok();
        } catch (Exception e) {

            log.error("??????????????????msg:{}", e.getMessage());
        }
        return null;
    }

    /**
     * @param createWphAccountSyn
     * @return
     */
    private WphAccount createWphAccountSyn(CreateWphAccountSyn createWphAccountSyn) {
        //Map<String, String> headerMap, CaptchaXY postMsgLogin, String code

        Map<String, String> headerMap = createWphAccountSyn.getHeaderMap();
        CaptchaXY postMsgLogin = createWphAccountSyn.getPostMsgLogin();
        String code = createWphAccountSyn.getCode();
        OkHttpClient client = buildClient();
        if (StrUtil.isBlank(code)) {
            log.error("?????????????????????????????????,msg:{}", postMsgLogin.getPhone());
            return null;
        }
        postMsgLogin.setCaptchaCode(code);
        PointsDto.dataCaptchaCode(postMsgLogin);
        CaptchaXY ticketMsg = getTicket(client, postMsgLogin, PreConstant.TWO, headerMap);
        log.info("???????????????ticketMsgMsg:{}", JSON.toJSONString(ticketMsg));
        CaptchaXY captchaXY = ticketLogin(client, ticketMsg);
//        ticketMsg.getCaptchaRes().setSid("02300bfbea4244e9b3ae96aa0e01cb7");
        if (ObjectUtil.isNull(captchaXY) || ObjectUtil.isNull(captchaXY.getVipTank())) {
            log.error("???????????????????????????");
        }
        redisTemplate.opsForValue().set("?????????????????????:" + captchaXY.getPhone(), JSON.toJSONString(captchaXY),
                captchaXY.getVipTank().getTANK_EXPIRE() - 3600, TimeUnit.SECONDS);
        WphAccount wphAccount = WphAccount.builder().phone(captchaXY.getPhone()).createTime(new Date())
                .expireTime(DateUtil.offsetSecond(new Date(), captchaXY.getVipTank().getTANK_EXPIRE() - 3600))
                .loginInfo(JSON.toJSONString(captchaXY)).isEnable(PreConstant.ONE).build();
        log.info("????????????????????????????????????????????????????????????????????????");
        WphAccount wphAccountDb = wphAccountMapper.selectOne(Wrappers.<WphAccount>lambdaQuery().eq(WphAccount::getPhone, captchaXY.getPhone()));
        if (ObjectUtil.isNotNull(wphAccountDb)) {
            wphAccount.setId(wphAccountDb.getId());
            wphAccount.setMark(wphAccountDb.getMark());
            wphAccount.setIsEnable(wphAccountDb.getIsEnable());
            wphAccount.setCreateTime(wphAccountDb.getCreateTime());
            this.wphAccountMapper.updateById(wphAccount);
        } else {
            this.wphAccountMapper.insert(wphAccount);
            log.info("????????????msg:{}", wphAccount.getPhone());
        }
        return wphAccount;
    }

    public OkHttpClient buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        String isAble = redisTemplate.opsForValue().get("??????????????????");
        if (StrUtil.isNotBlank(isAble) && Integer.valueOf(isAble) == PreConstant.ONE) {
            JdProxyIpPort zhiLianIp = jdDjService.getZhiLianIp();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(zhiLianIp.getIp(), Integer.valueOf(zhiLianIp.getPort())));
            builder.proxy(proxy);
            log.info("?????????????????????msg:{}", zhiLianIp);
        }
        OkHttpClient client = builder.connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).followRedirects(false).build();
        return client;
    }


    private CaptchaXY postMsg(OkHttpClient client, String phone, Map<String, String> headerMap) {
        log.info("????????????????????????captchaId");
        CaptchaRes captchaRes = getCaptchaId(client, phone, headerMap);
        log.info("?????????????????????");
        CaptchaXY captchaXY = parseQPAP(client, captchaRes.getCaptchaId(), headerMap);
        captchaXY.setPhone(phone);
        captchaXY.setCaptchaRes(captchaRes);
        log.info("????????????????????????msg:{}", captchaXY);
        log.info("????????????ticket");
//        String dataPoints = PointsDto.dataPoints(captchaXY);
        CaptchaXY ticket = getTicket(client, captchaXY, PreConstant.ONE, headerMap);
        log.info("????????????msg:{}", ticket);
        log.info("????????????getCheckmobileV1");
        CaptchaXY checkmobileV1 = getCheckmobileV1(client, captchaXY, headerMap);
        log.info("????????????getCheckmobileV1:{}", checkmobileV1);
        log.info("??????????????????");
        CaptchaXY postMsgLogin = postMsgLogin(client, captchaXY, headerMap);
        if (ObjectUtil.isNull(postMsgLogin)) {
            return null;
        }
        log.info("??????????????????msg:{}", postMsgLogin.getPostMsg());
        return postMsgLogin;
    }

    private String buildApiSign(CaptchaXY captchaXY) {
        JSONObject paramJson = new JSONObject();
        paramJson.put("app_version", "4.0");
        paramJson.put("mars_cid", captchaXY.getCaptchaRes().getMinaEdataDto().getMars_cid());
        paramJson.put("pid", captchaXY.getCheckmobileV1Dto().getPid());
        paramJson.put("ticket", captchaXY.getTicket());
        log.info("????????????????????????");
        String api = ApiSign.replaceHost("https://mlogin-api.vip.com/ajaxapi/user/ticketLogin");
        String hashParam = ApiSign.hashParam(paramJson, "https://mlogin-api.vip.com/ajaxapi/user/ticketLogin");
        String cid = captchaXY.getCaptchaRes().getMinaEdataDto().getMars_cid();
        String sid = captchaXY.getCaptchaRes().getSid();
        String secret = ApiSign.getSecret();
        StringBuilder sb = new StringBuilder();
        // rs = this.sha1(api + hashParam + cid + sid + secret);
        String rs = sb.append(api).append(hashParam).append(cid).append(sid).append(secret).toString();
        String sha1 = ApiSign.getSha1(rs);
        return sha1;
    }

    private CaptchaXY ticketLogin(OkHttpClient client, CaptchaXY captchaXY) {
        try {
            String api_sign = buildApiSign(captchaXY);
            String url = "https://mlogin-api.vip.com/ajaxapi/user/ticketLogin";
            RequestBody requestBody = new FormBody.Builder()
                    .add("api_key", "8cec5243ade04ed3a02c5972bcda0d3f")
                    .add("app_version", "4.0")
                    .add("mars_cid", captchaXY.getCaptchaRes().getMinaEdataDto().getMars_cid())
                    .add("pid", captchaXY.getCheckmobileV1Dto().getPid())
                    .add("ticket", captchaXY.getTicket())
                    .build();
            Request request = new Request.Builder().url(url)
                    .header("authorization", "OAuth api_sign=" + api_sign)
                    .header("cookie", String.format("mars_cid=%s;mars_sid=%s", captchaXY.getCaptchaRes().getMinaEdataDto().getMars_cid(),
                            captchaXY.getCaptchaRes().getSid()))
                    .post(requestBody)
                    .build();
            Response response = client.newCall(request).execute();
            String resStr = response.body().string();
            log.info("??????ticketLogin???????????????msg:{}", resStr);
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 1) {
                log.error("??????VIP_TANK?????????");
            }
            VipTank vipTank = JSON.parseObject(JSON.parseObject(resStr).getString("data"), VipTank.class);
            vipTank.setCreateDate(DateUtil.formatDateTime(new Date()));
            captchaXY.setVipTank(vipTank);
            return captchaXY;
        } catch (Exception e) {
            log.error("??????ticketLogin??????msg:{}", e.getMessage());
        }
        return null;
    }


    private CaptchaXY postMsgLogin(OkHttpClient client, CaptchaXY captchaXY, Map<String, String> headerMap) {
        try {
            String url = "https://captcha.vip.com/getURL";
            RequestBody requestBody = new FormBody.Builder()
                    .add("v", "1")
                    .add("source", "1")
                    .add("captchaType", "2")
                    .add("data", "{}")
                    .add("captchaId", captchaXY.getCheckmobileV1Dto().getCaptchaId())
                    .build();
            Request.Builder builder = new Request.Builder().url(url).post(requestBody);
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????postMsgLogin???????????????msg:{}", resStr);
            response.close();
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 0) {
                log.error("postMsgLogin???????????????????????????");
                return null;
            }
            captchaXY.setPostMsg(Boolean.TRUE);
            return captchaXY;
        } catch (Exception e) {
            log.error("??????postMsgLogin??????msg:{}", e.getMessage());
        }
        return null;
    }

    private CaptchaXY getCheckmobileV1(OkHttpClient client, CaptchaXY captchaXY, Map<String, String> headerMap) {
        try {
            MinaEdataDto minaEdataDto = ParamAes.convenient_login_wap_after_captcha(captchaXY);
            String url = "https://mapi.vip.com/vips-mobile/rest/auth/quicklogin/wap/checkmobile/v1";
            RequestBody requestBody = new FormBody.Builder()
                    .add("api_key", "8cec5243ade04ed3a02c5972bcda0d3f")
                    .add("app_version", "4.0")
                    .add("mars_cid", captchaXY.getCaptchaRes().getMinaEdataDto().getMars_cid())
                    .add("skey", "9cf2380318f54f31acfb1d6e274f5555")
                    .add("mina_eversion", "0")
                    .add("mina_edata", minaEdataDto.getMina_edata())
                    .build();
            Request.Builder builder = new Request.Builder().url(url)
                    .post(requestBody);
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????getCheckmobileV1???????????????msg:{}", resStr);
            if (StrUtil.isNotBlank(resStr) && resStr.contains("80001")) {
                log.error("????????????????????????msg:{}", captchaXY.getPhone());
                //TODO ???????????????
                return null;
            }
            response.close();
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 1) {
                log.error("getTicket???????????????????????????");
            }
            CheckmobileV1Dto checkmobileV1Dto = JSON.parseObject(JSON.parseObject(resStr).getString("data"), CheckmobileV1Dto.class);
            String authType = checkmobileV1Dto.getAuthType();
            if (Integer.valueOf(authType) == PreConstant.ONE) {
                log.error("??????????????????????????????");
                Boolean free_mobile = YeZiUtils.free_mobile(captchaXY.getPhone(), redisTemplate);
                log.info("??????????????????msg:{}", free_mobile);
                return null;
            }
            captchaXY.setCheckmobileV1Dto(checkmobileV1Dto);
            return captchaXY;
        } catch (Exception e) {
            log.error("??????getCheckmobileV1??????msg:{}", e.getMessage());
        }
        return null;
    }

    private CaptchaXY getTicket(OkHttpClient client, CaptchaXY captchaXY, Integer ticktetNum, Map<String, String> headerMap) {
        try {
            String dataPoints = null;
            if (ticktetNum == 1) {
                dataPoints = PointsDto.dataPoints(captchaXY);
            }
            if (ticktetNum == 2) {
                dataPoints = PointsDto.dataCaptchaCode(captchaXY);
            }
            String url = "https://captcha.vip.com/check";
            RequestBody requestBody = new FormBody.Builder()
                    .add("v", "1")
                    .add("source", "0")
                    .add("captchaId", ticktetNum == 1 ? captchaXY.getCaptchaRes().getCaptchaId() : captchaXY.getCheckmobileV1Dto().getCaptchaId())
                    .add("captchaType", ticktetNum == 1 ? "7" : "2")
                    .add("data", dataPoints)
                    .add("templateId", captchaXY.getCaptchaRes().getTemplateId())
                    .build();
            Request.Builder builder = new Request.Builder().url(url)
                    .post(requestBody);
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????getTicket???????????????msg:{}", resStr);
            response.close();
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 0) {
                log.error("getTicket???????????????????????????");
            }
            String ticket = JSON.parseObject(JSON.parseObject(resStr).getString("data")).getString("ticket");
            captchaXY.setTicket(ticket);

            return captchaXY;
        } catch (Exception e) {
            log.error("??????getCaptchaId??????msg:{}", e.getMessage());
            if (e.getMessage().contains("Failed to connect to")) {

            }
        }
        return null;
    }

    private CaptchaRes getCaptchaId(OkHttpClient client, String phone, Map<String, String> headerMap) {
        try {
            MinaEdataDto minaEdataDto = ParamAes.convenient_login_wap_img_captcha(phone);
            String url = "https://mapi.vip.com/vips-mobile/rest/auth/captcha/mp/flow/v1";
            RequestBody requestBody = new FormBody.Builder()
                    .add("api_key", "8cec5243ade04ed3a02c5972bcda0d3f")
                    .add("app_version", "4.0")
                    .add("mars_cid", minaEdataDto.getMars_cid())
                    .add("mina_eversion", "0")
                    .add("skey", "9cf2380318f54f31acfb1d6e274f5555")
                    .add("mina_edata", minaEdataDto.getMina_edata())
                    .build();
            Request.Builder builder = new Request.Builder().url(url)
                    .post(requestBody);
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            log.info("??????getCaptchaId??????????????????msg:{}", resStr);
            response.close();
            if (StrUtil.isBlank(resStr) || JSON.parseObject(resStr).getInteger("code") != 1) {
                log.error("???????????????????????????");
            }
            CaptchaRes captchaRes = JSON.parseObject(JSON.parseObject(resStr).getString("data"), CaptchaRes.class);
            captchaRes.setMinaEdataDto(minaEdataDto);
            return captchaRes;
        } catch (Exception e) {
            log.error("??????getCaptchaId??????msg:{}", e.getMessage());
        }
        return null;

    }


    private static List<WordsResult> parseWords(List<WordsResult> wordsResulAp) {
        List<WordsResult> returnWords = new ArrayList<>();
        for (WordsResult wordsResult : wordsResulAp) {
            String words = wordsResult.getWords();
            if (words.length() == 1) {
                returnWords.add(wordsResult);
                continue;
            }
            if (words.length() == 2) {
                for (int i = 0; i < words.length(); i++) {
                    String indexI = words.charAt(i) + "";
                    WordsResult objI = new WordsResult();
                    objI.setWords(indexI);
                    Location location = new Location();
                    objI.setLocation(location);
                    location.setHeight(wordsResult.getLocation().getHeight());
                    location.setTop(wordsResult.getLocation().getTop());
                    location.setHeight(0);
                    if (i == 0) {
                        location.setLeft(wordsResult.getLocation().getLeft() + 10);
                    }
                    if (i == 1) {
                        location.setLeft(wordsResult.getLocation().getLeft() + wordsResult.getLocation().getHeight() - 10);
                    }
                    returnWords.add(objI);
                }
            }
        }
        return returnWords;
    }


    private CaptchaXY parseQPAP(OkHttpClient client, String captchaId, Map<String, String> headerMap) {
        try {
            mark:
            for (int i = 0; i < 20; i++) {
                log.info("?????????????????????");
                CaptchaData yanzhengma = getYanzhengma(client, captchaId, headerMap);
                List<WordsResult> wordsResulAp = AuthService.parseCapData("https://captcha.vip.com/getImage?v=1&captchaType=7&imageId=" + yanzhengma.getAp(),
                        null, redisTemplate);
                if (CollUtil.isEmpty(wordsResulAp) || wordsResulAp.size() < 5) {
                    log.error("??????????????????5????????????????????????????????????");
                    wordsResulAp = parseWords(wordsResulAp);
                    log.info("????????????msg:{}", wordsResulAp.size());
                }
                if (CollUtil.isEmpty(wordsResulAp) || wordsResulAp.size() < 5) {
                    log.error("??????????????????5????????????????????????????????????5?????????");
                    continue;
                }

                Map<String, WordsResult> wordsResultMap = wordsResulAp.stream().collect(Collectors.toMap(it -> it.getWords(), it -> it));
                List<WordsResult> sortWordReSult = wordsResulAp.stream().sorted(Comparator.comparing(it -> it.getLocation().getLeft())).collect(Collectors.toList());
                Map<String, Integer> sortWordReSultIndexMap = sortWordReSult.stream().collect(Collectors.toMap(it -> it.getWords(), it -> sortWordReSult.indexOf(it)));
                List<WordsResult> wordsResultsQp = AuthService.parseCapData("https://captcha.vip.com/getImage?v=1&captchaType=7&imageId=" + yanzhengma.getQp(), null, redisTemplate);
                if (CollUtil.isEmpty(wordsResultsQp) || wordsResultsQp.size() != 1) {
                    log.info("???????????????????????????????????????msg:{}");
                    continue;
                }
                //?????????????????????????????????
                log.info("????????????");
                int dianjiIndex = wordsResultsQp.get(PreConstant.ZERO).getWords().indexOf("??????????????????");
                String zic = wordsResultsQp.get(PreConstant.ZERO).getWords().substring(dianjiIndex + 6);
                List<String> dianjiArrays = Arrays.asList(zic.split("???"));
                CaptchaXY.CaptchaXYBuilder builder = CaptchaXY.builder();
                for (String dianjiArray : dianjiArrays) {
                    int x = dianjiArrays.indexOf(dianjiArray);
                    WordsResult wordsResult = wordsResultMap.get(dianjiArray);
                    if (ObjectUtil.isNull(wordsResult)) {
                        continue mark;
                    }
                    Integer index = sortWordReSultIndexMap.get(dianjiArray);
                    if (x == 0) {
                        builder.x1(Double.valueOf((wordsResult.getLocation().getLeft() + (wordsResult.getLocation().getWidth() / 2))).intValue()).y1(36);
                    }
                    if (x == 1) {
                        builder.x2(Double.valueOf((wordsResult.getLocation().getLeft() + (wordsResult.getLocation().getWidth() / 2))).intValue()).y2(36);
                    }
                    if (x == 2) {
                        builder.x3(Double.valueOf((wordsResult.getLocation().getLeft() + (wordsResult.getLocation().getWidth() / 2))).intValue()).y3(36);
                    }
                }
                CaptchaXY captchaXY = builder.build();
                return captchaXY;
            }
        } catch (Exception e) {
            log.error("????????????:{}", e.getMessage());
        }
        return null;
    }

    private Integer getLeftIndex(Integer index) {
        if (index == 0) {
            return 47;
        }
        if (index == 1) {
            return 85;
        }
        if (index == 2) {
            return 146;
        }
        if (index == 3) {
            return 188;
        }
        if (index == 4) {
            return 240;
        }
        return null;
    }

    private CaptchaData getYanzhengma(OkHttpClient client, String captchaId, Map<String, String> headerMap) {
        try {
//            Jedis jedis = RedisDS.create().getJedis();
//            Set<String> keys = jedis.keys("???????????????????????????:*");
//            int i = PreUtils.randomCommon(0, keys.size() - 1, 1)[0];
//            String yanzhengmakey = keys.stream().collect(Collectors.toList()).get(i);
//            String captchaId = jedis.get(yanzhengmakey);
            String url = "https://captcha.vip.com/getURL";
            RequestBody requestBody = new FormBody.Builder()
                    .add("v", "1")
                    .add("source", "0")
                    .add("captchaId", captchaId)
                    .add("captchaType", "7")
                    .add("data", "{}")
                    .build();
            Request.Builder builder = new Request.Builder().url(url)
                    .post(requestBody);
            setHeader(headerMap, builder);
            Response response = client.newCall(builder.build()).execute();
            String resStr = response.body().string();
            response.close();
            log.info("??????getYanzhengma>>>>msg:{}", resStr);
            if (!resStr.contains("qp") || !resStr.contains("ap")) {
                return null;
            }
            JSONObject parseObject = JSON.parseObject(JSONObject.parseObject(resStr).getString("data"));
            CaptchaData captchaData = new CaptchaData(parseObject.getString("qp"), parseObject.getString("ap"), captchaId);
            return captchaData;
        } catch (Exception e) {
            log.error("??????getYanzhengma??????:{}", e.getMessage());
        }
        return null;

    }


    private static String getTenpay(OkHttpClient client, String ck, String npayvid, String redirectUrl, Map<String, String> headerMap) {
        try {
            String url = "https://npay.vip.com" + redirectUrl;
            Request.Builder header = new Request.Builder().url(url)
                    .addHeader("Cookie", String.format("VIP_TANK=%s;", ck) + String.format(";%s;", npayvid))
                    .get();
            setHeader(headerMap, header);
            Response response = client.newCall(header.build()).execute();
            String resStr = response.body().string();
            response.close();
            log.info("??????getTenpay>>>>msg:{}", resStr);
            if (!resStr.contains("https://wx.tenpay.com")) {
                return null;
            }
            int i1 = resStr.indexOf("\"https://");
            int i2 = resStr.indexOf("}}");
            String tenpayUrl = resStr.substring(i1 + 1, i2 - 1);
            log.info("???????????????????????????msg:{}", tenpayUrl);
            return tenpayUrl;
        } catch (Exception e) {
            log.error("??????getTenpay??????msg:{}", e.getMessage());
        }
        return null;

    }

    private static String doPay(OkHttpClient client, String ck, String npayvid, Map<String, String> headerMap) {
        try {
            String url = "https://npay.vip.com/wap/cashier/api/pay";
            RequestBody requestBody = new FormBody.Builder()
                    .add("payId", "1118")
                    .add("payType", "181")
                    .build();
            Request.Builder header = new Request.Builder().url(url)
                    .addHeader("Cookie", String.format("VIP_TANK=%s;", ck) + String.format(";%s;", npayvid))
                    .post(requestBody);
            setHeader(headerMap, header);
            Request request = header.build();
            Response response = client.newCall(request).execute();
            String resStr = response.body().string();
            log.info("??????dopay>>>>>>msg:{}", resStr);
            response.close();
            if (!resStr.contains("redirectUrl")) {
                return null;
            }
            String redirectUrl = JSONObject.parseObject(JSONObject.parseObject(resStr).getString("data")).getString("redirectUrl");
            return redirectUrl;
        } catch (Exception e) {
            log.error("??????dopay??????");
        }
        return null;
    }

    private static BigDecimal preview(OkHttpClient client, String ck, String npayvid, Map<String, String> headerMap) {
        try {
            String url = "https://npay.vip.com/wap/cashier/api/preview?payId=1118&payType=181";
            Request.Builder header = new Request.Builder().url(url)
                    .get()
                    .addHeader("User-Agent", " Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .addHeader("Cookie", String.format("VIP_TANK=%s;", ck) + String.format(";%s;", npayvid));
            setHeader(headerMap, header);

            Response response = client.newCall(header.build()).execute();
            String resStr = response.body().string();
            response.close();
            log.info("???????????????????????????msg:{}", resStr);
            if (!resStr.contains("totalPayAmount")) {
                return null;
            }
            String totalPayAmount = JSONObject.parseObject(JSONObject.parseObject(JSONObject.parseObject(resStr).getString("data"))
                    .getString("preview")).getString("totalPayAmount");
            BigDecimal totalPayAmountBig = new BigDecimal(totalPayAmount);
            return totalPayAmountBig;
        } catch (Exception e) {
            log.info("???????????????msg:{}", e);
        }
        return null;
    }

    private String cashier(String payUrl, OkHttpClient client, String ck, Map<String, String> headerMap) {
        try {

            Request.Builder builder = new Request.Builder().url(payUrl)
                    .get()
                    .addHeader("Cookie", String.format("VIP_TANK=%s;", ck));
            setHeader(headerMap, builder);
            Request request = builder.build();
            log.info("????????????");
            Response response = client.newCall(request).execute();
            String resStr = response.body().string();
            response.close();
            List<String> headers = response.headers("set-cookie");
            log.info("??????????????????msg:{}", resStr);
            if (!resStr.contains("?????????")) {
                return null;
            }
            if (CollUtil.isNotEmpty(headers)) {
                for (String header : headers) {
                    //NPAYVID=F37811D37B32DF6E96484B65C1208AF5B7DD09C7;path=/;domain=npay.vip.com;httponly
                    if (header.contains("NPAYVID")) {
                        int i = header.indexOf(";");
                        String NPAYVID = header.substring(0, i);
                        return NPAYVID;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("??????????????????msg:{}", e.getMessage());
        }
        return null;
    }

    private String getPayUrl(String orderId, OkHttpClient client, String ck, Map<String, String> headerMap) {
        try {
            String url = "https://h5.vip.com/api/virtual/VirtualPayJump/getPayUrl";
            MediaType JSON = MediaType.parse("application/json;charset=utf-8");
            String body = String.format("{\"orderId\":\"%s\",\"channelId\":\"default-wap\",\"isApp\":0,\"extraParams\":{\"_show_header\":1,\"order_id\":\"%s\",\"type\":\"vipcard\"}}",
                    orderId, orderId);
            RequestBody requestBody = RequestBody.create(JSON, body);
            Request.Builder header = new Request.Builder().url(url)
                    .post(requestBody)
                    .addHeader("Cookie", String.format("VIP_TANK=%s;", ck));
            setHeader(headerMap, header);
            Request request = header.build();
            log.info("???????????????");
            Response response = client.newCall(request).execute();
            String resStr = response.body().string();
            log.info("??????????????????????????????msg:{}", resStr);
            response.close();
            JSONObject resJson = com.alibaba.fastjson.JSON.parseObject(resStr);
            Integer code = resJson.getInteger("code");
            if (code != PreConstant.ZERO) {
                log.info("getPayUrl???????????????????????????????????????");
                return null;
            }
            String data = resJson.getString("data");
            String payUrl = com.alibaba.fastjson.JSON.parseObject(data).getString("payUrl");
            if (StrUtil.isNotBlank(payUrl) && payUrl.contains("appKey")) {
                log.info("????????????url???msg:{}", payUrl);
                return payUrl;
            }
        } catch (Exception e) {
            log.error("getPayUrl????????????url?????????{}", e.getMessage());
        }
        return null;
    }

    public void selectOrderStataus(JdOrderPt jdOrderPt, JdMchOrder jdMchOrder) {
        log.info("??????????????????");
        String url = String.format("https://h5.vip.com/api/virtual/EcouponOrder/get?order_id=%s", jdOrderPt.getOrderId());
        String body = HttpRequest.get(url)
                .header(Header.COOKIE, jdOrderPt.getCurrentCk())//??????????????????????????????????????????????????????
                .timeout(20000)//???????????????
                .execute().body();
        log.info("????????????????????????msg:{}", body);
        String data = JSON.parseObject(body).getString("data");
        JSONObject parseObject = JSON.parseObject(data);
        jdOrderPt.setHtml(JSON.toJSONString(parseObject));
        if (parseObject.getInteger("orderStatus") == 60) {
            log.info("???????????????????????????,???????????????++++++++++");
            String payInfoStr = parseObject.getString("payInfo");
            String ecardsStr = parseObject.getString("ecards");
            JSONObject ecardsJson = JSON.parseArray(ecardsStr, JSONObject.class).get(PreConstant.ZERO);
            String payTimeStr = JSON.parseObject(payInfoStr).getString("payTime");
            DateTime payTime = DateUtil.parse(payTimeStr);
            jdOrderPt.setPaySuccessTime(payTime);
            jdOrderPt.setCardNumber(ecardsJson.getString("code"));
            log.info("TODO????????????");
            sendMessageSenc(findwph_queue, JSON.toJSONString(jdOrderPt), 15);
            jdMchOrder.setStatus(PreConstant.TWO);
            this.jdMchOrderMapper.updateById(jdMchOrder);
            log.info("??????????????????");
        }
        this.jdOrderPtMapper.updateById(jdOrderPt);
    }

    //??????ip?????????
    @JmsListener(destination = "create_account_wph_queue", containerFactory = "queueListener", concurrency = "20")
    public void create_account_wph_queue(String message) {
        Boolean lockT = redisTemplate.opsForValue().setIfAbsent("?????????????????????????????????", "1", 3, TimeUnit.SECONDS);
        if (!lockT) {
            return;
        }
        log.info("?????????????????????????????????");
        log.info("??????10?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????");
        JdAppStoreConfig storeConfig = JSON.parseObject(message, JdAppStoreConfig.class);
        List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getGroupNum, PreConstant.SIX));
        List<String> skuids = jdAppStoreConfigs.stream().map(it -> it.getSkuId()).collect(Collectors.toList());
        Integer offOrder = jdMchOrderMapper.selectCount(Wrappers.<JdMchOrder>lambdaQuery().gt(JdMchOrder::getCreateTime, DateUtil.offsetMinute(new Date(), -10)).in(JdMchOrder::getSkuId, skuids));
        storeConfig = jdAppStoreConfigMapper.selectById(storeConfig.getId());
     /*   List<String> max20 = jdOrderPtMapper.selectwphOrderMax(20);
        log.debug("????????????20?????????????????????msg:{}", max20);

        if (CollUtil.isNotEmpty(max20)) {
            wrapper.notIn(WphAccount::getPhone, max20);
        }*/
        LambdaQueryWrapper<WphAccount> wrapper = Wrappers.<WphAccount>lambdaQuery().eq(WphAccount::getIsEnable, PreConstant.ONE).gt(WphAccount::getExpireTime, new Date());
        Integer count = wphAccountMapper.selectCount(wrapper);
        Integer productStockNum = storeConfig.getProductStockNum();
        int productAccountNum = productStockNum - count;
        if (productAccountNum <= PreConstant.ZERO) {
            log.info("??????????????????????????????msg:{}", productAccountNum);
            return;
        }
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("????????????????????????:??????", "0", 2, TimeUnit.MINUTES);
        if (!ifAbsent) {
            return;
        }
        if (offOrder >= 1) {
            sendMessageSenc(this.create_account_wph_queue, JSON.toJSONString(storeConfig), 135);
        }
        for (int i = 0; i < productStockNum * 5; i++) {
            count = wphAccountMapper.selectCount(wrapper);
            productAccountNum = productStockNum - count;
            if (productAccountNum <= PreConstant.ZERO) {
                log.info("??????????????????????????????msg:{}", productAccountNum);
                return;
            }
            register();
        }

    }


    //??????ip?????????
    @JmsListener(destination = "create_order_wph_queue", containerFactory = "queueListener", concurrency = "20")
    public void create_order_wph_queue(String message) {
        Boolean lockT = redisTemplate.opsForValue().setIfAbsent("?????????????????????????????????", "1", 2, TimeUnit.SECONDS);
        if (!lockT) {
            return;
        }
        log.info("??????????????????,?????????20??????????????????msg:{}", message);
        JdAppStoreConfig storeConfig = JSON.parseObject(message, JdAppStoreConfig.class);
        storeConfig = jdAppStoreConfigMapper.selectById(storeConfig.getId());
        List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.<JdAppStoreConfig>lambdaQuery()
                .eq(JdAppStoreConfig::getGroupNum, PreConstant.SIX));
        List<String> skuids = jdAppStoreConfigs.stream().map(it -> it.getSkuId()).collect(Collectors.toList());
        log.info("??????10???????????????????????????????????????");
        Integer offOrder = jdMchOrderMapper.selectCount(Wrappers.<JdMchOrder>lambdaQuery()
                .gt(JdMchOrder::getCreateTime, DateUtil.offsetMinute(new Date(), -10)).in(JdMchOrder::getSkuId, skuids));
        LambdaQueryWrapper<JdOrderPt> wrapper = getJdOrderPtLambdaQueryWrapper(storeConfig);
        Integer count = jdOrderPtMapper.selectCount(wrapper);
        int productNum = storeConfig.getProductStockNum() - count;
        if (productNum <= PreConstant.ZERO) {
            log.info("???????????????????????????");
            return;
        }
        if (offOrder >= 1) {
            sendMessageSenc(this.create_order_wph_queue, JSON.toJSONString(storeConfig), new Random().nextInt(70));
            sendMessageSenc(this.create_account_wph_queue, JSON.toJSONString(storeConfig), 90);
        }
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("????????????????????????:" + storeConfig.getSkuId(), JSON.toJSONString(storeConfig), 70, TimeUnit.SECONDS);
        if (ifAbsent) {
            for (int i = 0; i < productNum * 2; i++) {
                if (storeConfig.getProductStockNum() - jdOrderPtMapper.selectCount(wrapper) <= PreConstant.ZERO) {
                    log.info("???????????????????????????");
                    return;
                }
                createOrder(storeConfig.getSkuId());
            }
        }
    }

    //??????ip?????????
    @JmsListener(destination = "create_account_wph_queue_code", containerFactory = "queueListener", concurrency = "20")
    public void create_account_wph_queue_code(String message) {
        CreateWphAccountSyn createWphAccountSyn = JSON.parseObject(message, CreateWphAccountSyn.class);
        long between = DateUtil.between(createWphAccountSyn.getCreateDate(), new Date(), DateUnit.SECOND);
        if (between > 50) {
            log.info("??????????????????????????????");
            YeZiUtils yeZiUtils = new YeZiUtils();
            Boolean freeMobile = yeZiUtils.free_mobile(createWphAccountSyn.getPhone(), redisTemplate);
            return;
        }
        String code = new YeZiUtils().get_message(createWphAccountSyn.getPhone(), redisTemplate);
        if (StrUtil.isBlank(code)) {
            sendMessageSenc(create_account_wph_queue_code, message, 10);
        }
        createWphAccountSyn.setCode(code);
        createWphAccountSyn(createWphAccountSyn);
    }

    //??????????????????
    @JmsListener(destination = "create_order_wph_queue_code", containerFactory = "queueListener", concurrency = "32")
    public void create_order_wph_queue_code(String message) {
        // CreateOrderSyn createOrderSyn = CreateOrderSyn.builder().
        //                    skuConfig(skuConfig).headerMap(headerMap).wphAccountDb(wphAccountDb).phone(phone).wphCreateDto(wphCreateDto).build();
        CreateOrderSyn createOrderSyn = JSON.parseObject(message, CreateOrderSyn.class);
        long between = DateUtil.between(createOrderSyn.getCreateDate(), new Date(), DateUnit.SECOND);
        if (between > 50) {
            YeZiUtils yeZiUtils = new YeZiUtils();
            Boolean freeMobile = yeZiUtils.free_mobile(createOrderSyn.getPhone(), redisTemplate);
            return;
        }
        String code = new YeZiUtils().get_message(createOrderSyn.getPhone(), redisTemplate);
        if (StrUtil.isBlank(code)) {
            sendMessageSenc(create_order_wph_queue_code, message, 10);
            return;
        }
        createOrderSyn.setCode(code);
        for (int i = 0; i < 5; i++) {
            try {
                Boolean orderSyn = createOrderSyn(createOrderSyn);
                if (orderSyn) {
                    return;
                }
            } catch (Exception e) {
                log.error("??????????????????msg:{}", e);
            }
        }
    }

    //??????????????????
    @JmsListener(destination = "findwph_queue_code", containerFactory = "queueListener", concurrency = "32")
    public void findwph_queue_code(String message) {
        FindSyn findSyn = JSON.parseObject(message, FindSyn.class);
        long between = DateUtil.between(findSyn.getCreateTime(), new Date(), DateUnit.SECOND);
        if (between > 100) {
            YeZiUtils yeZiUtils = new YeZiUtils();
            Boolean freeMobile = yeZiUtils.free_mobile(findSyn.getJdOrderPt().getWphCardPhone(), redisTemplate);
            return;
        }
        String code = new YeZiUtils().get_message(findSyn.getJdOrderPt().getWphCardPhone(), redisTemplate);
        if (StrUtil.isBlank(code)) {
            sendMessageSenc(findwph_queue_code, JSON.toJSONString(findSyn), 10);
            return;
        }
        findSyn.setCode(code);
        for (int i = 0; i < 5; i++) {
            try {
                Boolean aBoolean = codeFindMyCardSyn(findSyn);
                if (aBoolean) {
                    return;
                }
            } catch (Exception e) {
                log.error("????????????????????????msg:{}", e.getMessage());
            }

        }

    }

    //??????????????????
    @JmsListener(destination = "findwph_queue", containerFactory = "queueListener", concurrency = "60")
    public void findwph_queue_m(String message) {
        log.info("????????????msg:{}", message);
        JdOrderPt jdOrderPt = JSON.parseObject(message, JdOrderPt.class);
        jdOrderPt = this.jdOrderPtMapper.selectById(jdOrderPt.getId());
        try {
            log.info("??????????????????????????????msg:{}", jdOrderPt.getWphCardPhone());
            if (StrUtil.isNotBlank(jdOrderPt.getCarMy())) {
                log.info("??????????????????????????????????????????????????????msg:{}", jdOrderPt.getCardNumber());
                return;
            }
            //TODO ????????????????????????
            long between = DateUtil.between(jdOrderPt.getPaySuccessTime(), new Date(), DateUnit.MINUTE);
            if (between >= 120) {
                return;
            }
            sendMessageSenc(findwph_queue, JSON.toJSONString(jdOrderPt), 3 * 60);
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("?????????????????????:" + jdOrderPt.getOrderId(), JSON.toJSONString(message), 2, TimeUnit.MINUTES);
            if (!ifAbsent) {
                log.info("?????????????????????????????????????????????msg:{}", jdOrderPt.getOrderId());
                return;
            }
            log.info("?????????????????????????????????");
            log.info("????????????????????????");
            log.info("??????????????????????????????msg:{}", jdOrderPt.getWphCardPhone());
            Boolean free_mobile = YeZiUtils.free_mobile(jdOrderPt.getWphCardPhone(), redisTemplate);
            log.info("??????????????????msg:{}", free_mobile);
            String mobileByDto = YeZiUtils.get_mobileByDto(YeZiGetMobileDto.getZhiDingPhoneDuanXin(jdOrderPt.getWphCardPhone(), redisTemplate), redisTemplate);
            if (StrUtil.isBlank(mobileByDto)) {
                //??????????????????????????????
                log.info("???????????????????????????msg:{}", mobileByDto);
                free_mobile = YeZiUtils.free_mobile(jdOrderPt.getWphCardPhone(), redisTemplate);
                redisTemplate.delete("?????????????????????:" + jdOrderPt.getOrderId());
                log.info("????????????????????????????????????,??????redis?????????????????????");
                return;
            }
            redisTemplate.opsForValue().set("???????????????:" + mobileByDto, System.currentTimeMillis() + "", 180, TimeUnit.SECONDS);
            log.info("????????????");
            String hexByLenth = ParamAes.getHexByLenth(32);
            String mars_cid = String.format("mars_cid=%s_%s", System.currentTimeMillis() + "", hexByLenth);
            String mars_sid = hexByLenth;
            WphCreateDto wphCreateDto = JSON.parseObject(jdOrderPt.getMark(), WphCreateDto.class);
            String loginInfo = wphCreateDto.getWphAccount().getLoginInfo();
            CaptchaXY captchaXY = JSON.parseObject(loginInfo, CaptchaXY.class);
            String ckCidAndSidAndTank = String.format("%s;%s;VIP_TANK=%s;", mars_cid, mars_sid, captchaXY.getVipTank().getVIP_TANK());
            String url = String.format("https://h5.vip.com/api/virtual/EcouponOrder/getSmsCaptchaByOrderId?orderId=%s", jdOrderPt.getOrderId());
/*            String result2 = HttpRequest.get(url)
                    .header(Header.COOKIE, ckCidAndSidAndTank)//??????????????????????????????????????????????????????
                    .timeout(20000)//???????????????
                    .execute().body();*/

            OkHttpClient client = buildClient();
            Request.Builder header = new Request.Builder().url(url)
                    .get()
                    .addHeader("Cookie", ckCidAndSidAndTank);
            Request request = header.build();
            Response response = client.newCall(request).execute();
            String result2 = response.body().string();


            log.info("????????????SmsCaptchaByOrderIdmsg:{}", result2);
            WphParamCaptchaTokenAndUUID wphParamCaptchaTokenAndUUID = JSON.parseObject(JSON.parseObject(result2).getString("data"), WphParamCaptchaTokenAndUUID.class);
            log.info("??????????????????????????????????????????");
            if (ObjectUtil.isNotNull(wphParamCaptchaTokenAndUUID) && StrUtil.isNotBlank(wphParamCaptchaTokenAndUUID.getPic())) {
                log.info("??????????????????????????????????????????msg:{}", wphParamCaptchaTokenAndUUID.getUuid());
                String picData = wphParamCaptchaTokenAndUUID.getPic().replace("\\", "");
                wphParamCaptchaTokenAndUUID.setPic(picData);
                List<WordsResult> wordsResultsQp = AuthService.parseCapData(null, picData, redisTemplate);
                if (CollUtil.isEmpty(wordsResultsQp) || wordsResultsQp.size() != 1) {
                    log.error("????????????msg:{}", wordsResultsQp);
                    redisTemplate.delete("?????????????????????:" + jdOrderPt.getOrderId());
                    return;
                }

                String words = wordsResultsQp.get(PreConstant.ZERO).getWords();
                words = words.replace(" ", "");


                String checkSmsPicCaptchaByOrderId = String.format("https://h5.vip.com/api/virtual/EcouponOrder/checkSmsPicCaptchaByOrderId?orderId=%s&captchaCode=%s&picUuid=%s",
                        jdOrderPt.getOrderId(), words, wphParamCaptchaTokenAndUUID.getPicUuid());
                Response checkSmsPicCaptchaByOrderIdResultResponse = client.newCall(new Request.Builder().url(checkSmsPicCaptchaByOrderId)
                        .get()
                        .addHeader("Cookie", ckCidAndSidAndTank).build()).execute();
                String checkSmsPicCaptchaByOrderIdResult = checkSmsPicCaptchaByOrderIdResultResponse.body().string();

       /*         String checkSmsPicCaptchaByOrderIdResult = HttpRequest.get(checkSmsPicCaptchaByOrderId)
                        .header(Header.COOKIE, ckCidAndSidAndTank)//??????????????????????????????????????????????????????
                        .timeout(20000)//???????????????
                        .execute().body();*/
                log.info("?????????????????????checkSmsPicCaptchaByOrderIdResultmsg:{}", checkSmsPicCaptchaByOrderIdResult);
                wphParamCaptchaTokenAndUUID = JSON.parseObject(JSON.parseObject(checkSmsPicCaptchaByOrderIdResult).getString("data"), WphParamCaptchaTokenAndUUID.class);
                if (ObjectUtil.isNull(wphParamCaptchaTokenAndUUID) || StrUtil.isBlank(wphParamCaptchaTokenAndUUID.getCaptchaToken())) {
                    redisTemplate.delete("?????????????????????:" + jdOrderPt.getOrderId());
                    return;
                }
            }
            log.info("???????????????msg:{}", wphParamCaptchaTokenAndUUID);
//            String code = new YeZiUtils().get_message(jdOrderPt.getWphCardPhone(), redisTemplate);
            FindSyn findSyn = FindSyn.builder().ckCidAndSidAndTank(ckCidAndSidAndTank).jdOrderPt(jdOrderPt).wphParamCaptchaTokenAndUUID(wphParamCaptchaTokenAndUUID).build();
            findSyn.setCreateTime(new Date());
            sendMessageSenc(findwph_queue_code, JSON.toJSONString(findSyn), 10);
            // codeFindMyCard(findSyn);
        } catch (Exception e) {
            redisTemplate.delete("?????????????????????:" + jdOrderPt.getOrderId());
            log.error("??????????????????Msg:{}", e.getMessage());
        }
        return;
    }


    private Boolean codeFindMyCardSyn(FindSyn findSyn) {
        JdOrderPt jdOrderPt = findSyn.getJdOrderPt();
        String ckCidAndSidAndTank = findSyn.getCkCidAndSidAndTank();
        WphParamCaptchaTokenAndUUID wphParamCaptchaTokenAndUUID = findSyn.getWphParamCaptchaTokenAndUUID();
        String code = findSyn.getCode();
        if (StrUtil.isBlank(code)) {
            log.info("??????????????????msg:{}");
            redisTemplate.delete("?????????????????????:" + jdOrderPt.getOrderId());
        }
        log.info("??????????????????msg:{}", code);
        String getVipCardInfoUrl = String.format("https://h5.vip.com/api/virtual/EcouponOrder/getVipCardInfo?channel=wap&isGoOn=0&uuid=%s&captchaToken=%s&orderId=%s&verifyCode=%s&cardNos=%s",
                wphParamCaptchaTokenAndUUID.getUuid(), wphParamCaptchaTokenAndUUID.getCaptchaToken(),
                jdOrderPt.getOrderId(), code, jdOrderPt.getCardNumber()
        );
        String getVipCardInfoBody = HttpRequest.get(getVipCardInfoUrl)
                .header(Header.COOKIE, ckCidAndSidAndTank)//??????????????????????????????????????????????????????
                .timeout(20000)//???????????????
                .execute().body();
        log.info("????????????msg???{}", getVipCardInfoBody);
        if (!getVipCardInfoBody.contains("activateNo")) {
            log.error("????????????");
            return false;
        }
        String data = JSON.parseObject(getVipCardInfoBody).getString("data");
        JSONObject jsonObject = JSON.parseArray(JSON.parseObject(data).getString("cards"), JSONObject.class).get(PreConstant.ZERO);
        String activateNo = jsonObject.getString("activateNo");
        jdOrderPt.setCarMy(activateNo);
        jdOrderPt.setSuccess(PreConstant.ONE);
        log.info("????????????????????????msg:{}", jdOrderPt.getOrderId());
        this.jdOrderPtMapper.updateById(jdOrderPt);
        return true;
    }

    //    @Async("asyncPool")
//    @Scheduled(cron = "0 0/1 * * * ?")
    public void taskFindMyCard() {
        log.info("?????????????????????????????????msg:{}");
        List<JdAppStoreConfig> jdAppStoreConfigs = this.jdAppStoreConfigMapper.selectList(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getGroupNum, PreConstant.SIX));
        List<String> skus = jdAppStoreConfigs.stream().map(it -> it.getSkuId()).collect(Collectors.toList());
        DateTime dateTime = DateUtil.offsetMinute(new Date(), -120);
        List<JdOrderPt> jdOrderPts = this.jdOrderPtMapper.selectList(Wrappers.<JdOrderPt>lambdaQuery().gt(JdOrderPt::getPaySuccessTime, dateTime)
                .in(JdOrderPt::getSkuId, skus).isNull(JdOrderPt::getCarMy));
        if (CollUtil.isNotEmpty(jdOrderPts)) {
            for (JdOrderPt jdOrderPt : jdOrderPts) {
                Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("?????????????????????????????????:" + jdOrderPt.getOrderId(), "", 2, TimeUnit.MINUTES);
                if (!ifAbsent) {
                    return;
                }
                sendMessageSenc(findwph_queue, JSON.toJSONString(jdOrderPt), 2 * 60);
            }
        }

    }

    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void deleteFreePhone() {
        Set<String> keys = redisTemplate.keys("???????????????:*");
        if (CollUtil.isEmpty(keys)) {
            return;
        }
        for (String key : keys) {
            String timeStr = redisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(timeStr)) {
                continue;
            }
            String phone = key.split(":")[1];
            Date date = new Date(Long.valueOf(timeStr));
            long between = DateUtil.between(date, new Date(), DateUnit.SECOND);
            if (between > 100) {
                Boolean free_mobile = YeZiUtils.free_mobile(phone, redisTemplate);
                if (free_mobile) {
                    redisTemplate.delete(key);
                }
            }
        }
    }

    //   redisTemplate.opsForValue().set("???????????????:" + phone, System.currentTimeMillis() + "", 180, TimeUnit.SECONDS);
    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessageSenc(Destination destination, final String message, Integer minit) {
        Map<String, Object> headers = new HashMap<>();
        //???????????????????????????10???,????????????
        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 1000);
        jmsMessagingTemplate.convertAndSend(destination, message, headers);
    }

}
