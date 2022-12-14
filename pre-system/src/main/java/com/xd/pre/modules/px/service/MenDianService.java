package com.xd.pre.modules.px.service;

import cn.hutool.core.collection.CollUtil;
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
import com.xd.pre.common.sign.JdSgin;
import com.xd.pre.common.utils.R;
import com.xd.pre.common.utils.px.MenDianRandomUtil;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.common.utils.px.dto.SignVoAndDto;
import com.xd.pre.modules.px.mendian.address.AddressVo;
import com.xd.pre.modules.px.mendian.callBack.CallBackVo;
import com.xd.pre.modules.px.mendian.order_settlement_detail.AddressDetailTO;
import com.xd.pre.modules.px.mendian.order_settlement_detail.DeliveryWayList;
import com.xd.pre.modules.px.mendian.order_settlement_detail.OrderSettlementDetail;
import com.xd.pre.modules.px.mendian.order_settlement_detail.SettlemnetSku;
import com.xd.pre.modules.px.mendian.submit.OrderSubmit;
import com.xd.pre.modules.px.mendian.submit.SkuList;
import com.xd.pre.modules.px.vo.reqvo.TokenKeyVo;
import com.xd.pre.modules.px.vo.resvo.TokenKeyResVo;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.mapper.JdAddressMapper;
import com.xd.pre.modules.sys.mapper.JdCkMapper;
import com.xd.pre.modules.sys.mapper.JdMchOrderMapper;
import com.xd.pre.modules.sys.mapper.JdOrderPtMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MenDianService {

    @Autowired
    private ProxyProductService proxyProductService;

    @Resource
    private JdCkMapper jdCkMapper;

    @Autowired
    private NewWeiXinPayUrl newWeiXinPayUrl;

    @Autowired
    private TokenKeyService tokenKeyService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private JdAddressMapper jdAddressMapper;

    @Autowired
    private JdDjService jdDjService;

    @Resource
    private JdOrderPtMapper jdOrderPtMapper;

    @Resource
    private JdMchOrderMapper jdMchOrderMapper;


    public R match(JdMchOrder jdMchOrder, JdAppStoreConfig jdAppStoreConfig, JdLog jdLog) {
        try {
            TimeInterval timer = DateUtil.timer();
            log.info("????????????????????????");
            LambdaQueryWrapper<JdCk> wrapper = Wrappers.<JdCk>lambdaQuery()
                    .in(JdCk::getIsEnable, Arrays.asList(PreConstant.ONE, PreConstant.FIVE));
            Integer count = jdCkMapper.selectCount(wrapper);
            if (count <= 1) {
                log.error("??????ck??????.???????????????");
            }
            int i = PreUtils.randomCommon(0, count - 1, 1)[0];
            if (count > 15) {
                if (count > 15) {
                    int[] ints = PreUtils.randomCommon(0, count, 13);
                    List<Integer> chongzhiaccount = new ArrayList<>();
                    for (int anInt : ints) {
                        chongzhiaccount.add(anInt);
                    }
                    chongzhiaccount = chongzhiaccount.stream().sorted().collect(Collectors.toList());
                    i = chongzhiaccount.get(PreConstant.ZERO);
                }
            }
            Page<JdCk> jdCkPage = new Page<>(i, 1);
            jdCkPage = this.jdCkMapper.selectPage(jdCkPage, wrapper);
            if (CollUtil.isEmpty(jdCkPage.getRecords())) {
                log.error("??????ck????????????????????????");
            }
            JdCk jdCk = jdCkPage.getRecords().get(PreConstant.ZERO);
/*            JdProxyIpPort zhiLianIp = jdDjService.getZhiLianIp();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(zhiLianIp.getIp(), Integer.valueOf(zhiLianIp.getPort())));
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.proxy(proxy);
            OkHttpClient client = builder.connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).followRedirects(false).build();*/
            OkHttpClient client = buildClient();
            InetSocketAddress address = (InetSocketAddress)client.proxy().address();
            String hostName = address.getHostName();
            int port = address.getPort();
            JdProxyIpPort zhiLianIp  =  JdProxyIpPort.builder().ip(hostName).port(port+"").build();
            log.info("???????????????????????????ck??????:?????????{}", timer.interval());
            Map<String, String> headerMap = PreUtils.buildIpMap(jdLog.getIp());
            if (StrUtil.isBlank(jdCk.getMck()) || ObjectUtil.isNull(jdCk.getMckCreateTime()) ||
                    jdCk.getMckCreateTime().getTime() > DateUtil.offsetHour(new Date(), -5).getTime()) {
                log.info("????????????mck,{}", timer.interval());
                jdCk = buildJdMck(zhiLianIp, jdCk);
            }
            OrderSubmit orderSubmit = orderSubmit(jdCk.getMck(), client, jdAppStoreConfig, headerMap);
            log.info("??????????????????????????????????????????msg orderSubmit :{}", orderSubmit);
            if (ObjectUtil.isNull(orderSubmit)) {
                log.error("??????????????????????????????????????????");
                return null;
            }
            log.info("OrderSubmit:?????????{}", timer.interval());
            String orderId = build_order_submit(jdCk.getMck(), client, orderSubmit, headerMap);
            log.info("????????????:?????????{}", timer.interval());
            if (StrUtil.isBlank(orderId)) {
                log.error("????????????,???????????????mesg:{}", orderId);
            }
            if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.ONE ||
                    Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.TWO ||
                    Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.FIVE) {
                log.info("?????????????????????????????????????????????msg:{}", jdAppStoreConfig.getConfig());
                return getPay1And2(jdMchOrder, jdAppStoreConfig, timer, jdCk, client, headerMap, orderId);
            }
            if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.THREE) {
                log.info("????????????????????????");
                return jingxiPay(jdMchOrder, jdAppStoreConfig, timer, jdCk, client, headerMap, orderId);
            }
            if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.FOUR) {
                log.info("??????????????????");
                return jingxiPay(jdMchOrder, jdAppStoreConfig, timer, jdCk, client, headerMap, orderId);
            }
        } catch (Exception e) {
            log.error("????????????,???????????????msg:{},????????????:{}", e.getMessage(), jdMchOrder.getTradeNo());
        }
        return null;
    }

    public OkHttpClient buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        String isAble = redisTemplate.opsForValue().get("??????????????????");
        if (StrUtil.isNotBlank(isAble) && Integer.valueOf(isAble) == PreConstant.ONE) {
            JdProxyIpPort oneIp = this.proxyProductService.getOneIp(PreConstant.ZERO, PreConstant.ZERO, false);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(oneIp.getIp(), Integer.valueOf(oneIp.getPort())));
            builder.proxy(proxy);
            log.info("?????????????????????msg:{}", oneIp);
        }
        OkHttpClient client = builder.connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).followRedirects(false).build();
        return client;
    }

    private R jingxiPay(JdMchOrder jdMchOrder, JdAppStoreConfig jdAppStoreConfig, TimeInterval timer, JdCk jdCk, OkHttpClient client, Map<String, String> headerMap, String orderId) {
        String payUrl = build_weixin_jx_pay_url(jdCk.getMck(), client, orderId, headerMap);
        if (StrUtil.isBlank(payUrl)) {
            payUrl = build_weixin_jx_pay_url(jdCk.getMck(), client, orderId, headerMap);
        }
        if (StrUtil.isBlank(payUrl)) {
            return null;
        }
        log.info("????????????????????????????????????????????????msg:{}", payUrl);
        redisTemplate.opsForValue().set("??????????????????:" + orderId, payUrl, 10, TimeUnit.MINUTES);
        JdOrderPt.JdOrderPtBuilder jdOrderPtBuilder = JdOrderPt.builder();
        JdOrderPt jdOrderPtDb = jdOrderPtBuilder.orderId(orderId)
                .ptPin(PreUtils.get_pt_pin(jdCk.getCk()))
                .createTime(new Date()).skuPrice(jdAppStoreConfig.getSkuPrice()).skuName(jdAppStoreConfig.getSkuName())
                .expireTime(DateUtil.offsetMinute(new Date(), jdAppStoreConfig.getPayIdExpireTime()))
                .skuId(jdAppStoreConfig.getSkuId())
                .isWxSuccess(PreConstant.ONE).isMatch(PreConstant.ONE).isMatch(PreConstant.ONE).currentCk(jdCk.getMck())
                .orgAppCk(jdCk.getCk())
                .build();
        boolean buildHrefRed = false;
        if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.THREE) {
            buildHrefRed = buildHrefRed(jdOrderPtDb, jdAppStoreConfig);
        } else {
            buildHrefRed = buildHrefRedWx(jdOrderPtDb, jdAppStoreConfig, payUrl, headerMap);
        }
        log.info("????????????:?????????{}", timer.interval());
        if (!buildHrefRed) {
            log.error("??????????????????");
        }
        this.jdOrderPtMapper.insert(jdOrderPtDb);
        Boolean isLockMath = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), jdAppStoreConfig.getExpireTime(), TimeUnit.MINUTES);
        if (!isLockMath) {
            return null;
        }
        log.info("????????????????????????");
        long l = (System.currentTimeMillis() - jdMchOrder.getCreateTime().getTime()) / 1000;
        jdMchOrder.setMatchTime(l);
        jdMchOrder.setOriginalTradeNo(jdOrderPtDb.getOrderId());
        jdMchOrder.setOriginalTradeId(jdOrderPtDb.getId());
        jdMchOrderMapper.updateById(jdMchOrder);
        return R.ok(jdMchOrder);
    }

    private R getPay1And2(JdMchOrder jdMchOrder, JdAppStoreConfig jdAppStoreConfig, TimeInterval timer, JdCk jdCk, OkHttpClient client, Map<String, String> headerMap, String orderId) {
        log.info("??????payId");
        String payId = build_order_payId(jdCk.getMck(), client, orderId, headerMap);
        log.info("??????payId:?????????{}", timer.interval());
        if (StrUtil.isBlank(payId)) {
            return null;
        }
        log.info("???????????????????????????");
        boolean checkStatus = build_order_check(jdCk.getMck(), client, orderId, payId, headerMap);
        log.info("???????????????????????????:?????????{}", timer.interval());
        if (!checkStatus) {
            return null;
        }
        log.info("????????????????????????");
        String payUrl = build_weixin_pay_url(jdCk.getMck(), client, orderId, payId, headerMap);
        log.info("??????????????????:?????????{}", timer.interval());
        if (StrUtil.isBlank(payUrl)) {
            return null;
        }
        log.info("????????????????????????????????????????????????msg:{}", payUrl);
        redisTemplate.opsForValue().set("??????????????????:" + orderId, payUrl, 10, TimeUnit.MINUTES);
        JdOrderPt.JdOrderPtBuilder jdOrderPtBuilder = JdOrderPt.builder();
        JdOrderPt jdOrderPtDb = jdOrderPtBuilder.orderId(orderId)
                .ptPin(PreUtils.get_pt_pin(jdCk.getCk()))
                .expireTime(DateUtil.offsetMinute(new Date(), jdAppStoreConfig.getPayIdExpireTime()))
                .createTime(new Date()).skuPrice(jdAppStoreConfig.getSkuPrice()).skuName(jdAppStoreConfig.getSkuName())
                .isWxSuccess(PreConstant.ONE).isMatch(PreConstant.ONE).isMatch(PreConstant.ONE).currentCk(jdCk.getMck())
                .skuId(jdAppStoreConfig.getSkuId())
                .orgAppCk(jdCk.getCk())
                .prerId(payId)
                .build();
        boolean buildHrefRed = false;
        if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.ONE) {
            buildHrefRed = buildHrefRed(jdOrderPtDb, jdAppStoreConfig);
        } else if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.TWO) {
            buildHrefRed = buildHrefRedWx(jdOrderPtDb, jdAppStoreConfig, payUrl, headerMap);
        } else if (Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.FIVE) {
            buildHrefRed = buildHrefRed(jdOrderPtDb, jdAppStoreConfig);
        } else {
            return null;
        }
        log.info("????????????:?????????{}", timer.interval());
        if (!buildHrefRed) {
            log.error("??????????????????");
        }
        this.jdOrderPtMapper.insert(jdOrderPtDb);
        Boolean isLockMath = redisTemplate.opsForValue().setIfAbsent("??????????????????:" + jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrder), jdAppStoreConfig.getExpireTime(), TimeUnit.MINUTES);
        if (!isLockMath) {
            return null;
        }
        log.info("????????????????????????");
        long l = (System.currentTimeMillis() - jdMchOrder.getCreateTime().getTime()) / 1000;
        jdMchOrder.setOriginalTradeNo(jdOrderPtDb.getOrderId());
        jdMchOrder.setOriginalTradeId(jdOrderPtDb.getId());
        jdMchOrder.setMatchTime(l);
        jdMchOrderMapper.updateById(jdMchOrder);
        return R.ok(jdMchOrder);
    }

    private boolean buildHrefRedWx(JdOrderPt jdOrderPtDb, JdAppStoreConfig jdAppStoreConfig, String mweb_url, Map<String, String> headerMap) {
        for (int i = 0; i < 5; i++) {
            try {
                JdProxyIpPort oneIp = newWeiXinPayUrl.getJdProxyIpPort(null);
                String weixinxxx = weixinUrl(mweb_url, headerMap, oneIp);
                if (StrUtil.isNotBlank(weixinxxx)) {
                    jdOrderPtDb.setHrefUrl(weixinxxx);
                    return true;
                }
            } catch (Exception e) {
                log.info("????????????????????????");
            }
        }
        return false;
    }

    public String weixinUrl(String mweb_url, Map<String, String> headerMap, JdProxyIpPort oneIp) {
        try {
            if (ObjectUtil.isNull(mweb_url)) {
                return null;
            }
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(oneIp.getIp(), Integer.valueOf(oneIp.getPort())));
            OkHttpClient client = builder.proxy(proxy).connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).build();
            Request.Builder header = new Request.Builder()
                    .url(mweb_url)
                    .get()
                    .addHeader("Referer", "https://daojia.jd.com");
            if (CollUtil.isNotEmpty(headerMap)) {
                for (String key : headerMap.keySet()) {
                    header.header(key, headerMap.get(key));
                }
            }
            Request request = header.build();
            Response response = client.newCall(request).execute();
            String jingdonghtml = response.body().string();
            log.debug("?????????????????????msg:{}", jingdonghtml);
            response.close();
            String P_COMM = "[a-zA-z]+://[^\\s]*";
            Pattern pattern = Pattern.compile(P_COMM);
            Matcher matcher = pattern.matcher(jingdonghtml);
            if (matcher.find()) {
                String group = matcher.group();
                String replace = group.replace("\"", "");
                return replace;
            }
        } catch (Exception e) {
            log.error("?????????????????????????????????msg:{}", e.getMessage());
        }
        return null;
    }


    private boolean buildHrefRed(JdOrderPt jdOrderPtDb, JdAppStoreConfig jdAppStoreConfig) {
        try {
            JdProxyIpPort oneIp = newWeiXinPayUrl.getJdProxyIpPort(null);
            JdCk jdCk = this.jdCkMapper.selectOne(Wrappers.<JdCk>lambdaQuery().eq(JdCk::getPtPin, jdOrderPtDb.getPtPin()));
            TokenKeyVo build = TokenKeyVo.builder().cookie(jdCk.getCk().trim()).build();
            String body = URLEncoder.encode(String.format("https://pay.m.jd.com/pay/weixin-pay.html?appId=d_m_mdbang&payId=%s", jdOrderPtDb.getPrerId()));
            //String genAppPayId = JdSgin.getJdSgin("genToken", String.format("{\"action\":\"to\",\"to\":\"%s\"}", URLEncoder.encode(body)));
            build.setBody(String.format("{\"action\":\"to\",\"to\":\"%s\"}", URLEncoder.encode(body)));
            TokenKeyResVo tokenKeyVO = tokenKeyService.getTokenKey(build, oneIp, jdCk.getFileName());
            for (int i = 0; i < 5; i++) {
                if (ObjectUtil.isNull(tokenKeyVO) || StrUtil.isBlank(tokenKeyVO.getTokenKey())) {
                    oneIp = newWeiXinPayUrl.getJdProxyIpPort(null);
                    tokenKeyVO = tokenKeyService.getTokenKey(build, oneIp, jdCk.getFileName());
                }
            }
            String tokenKey = tokenKeyVO.getTokenKey();
            if (jdAppStoreConfig.getGroupNum() == PreConstant.FIVE && Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.ONE) {
                String href = "https://un.m.jd.com/cgi-bin/app/appjmp?tokenKey=" + tokenKey + "&to=https%3A%2F%2Fpay.m.jd.com%2Fcpay%2FnewPay-index.html%3FappId%3Dd_m_mdbang%26payId%3D" + jdOrderPtDb.getPrerId();
                jdOrderPtDb.setHrefUrl(href);
                return true;
            }
            if (jdAppStoreConfig.getGroupNum() == PreConstant.FIVE && Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.THREE) {
                String href = "https://un.m.jd.com/cgi-bin/app/appjmp?tokenKey=" + tokenKey + "&to=https%3A%2F%2Fst.jingxi.com%2Forder%2Fn_detail_v2.shtml%3FappCode%3Dmsc588d6d5%26deal_id%3D" + jdOrderPtDb.getOrderId();
                jdOrderPtDb.setHrefUrl(href);
                return true;
            }
            if (jdAppStoreConfig.getGroupNum() == PreConstant.FIVE && Integer.valueOf(jdAppStoreConfig.getConfig()) == PreConstant.FIVE) {
                String href = "https://un.m.jd.com/cgi-bin/app/appjmp?tokenKey=" + tokenKey;
                jdOrderPtDb.setHrefUrl(href);
                return true;
            }

        } catch (Exception e) {
            log.error("????????????ck");
        }
        return false;
    }

    private OrderSubmit orderSubmit(String mck, OkHttpClient client, JdAppStoreConfig jdAppStoreConfig, Map<String, String> headerMap) {
        String paylink = PreUtils.parseUrl(jdAppStoreConfig.getUrl()).getParams().get("paylink");
        try {
            String url = "https://api.m.jd.com/api?appid=mdb&functionId=order_address_detail";
//            RequestBody requestBody = new FormBody.Builder().build();

            Request.Builder requstBuild = new Request.Builder()
                    .url(url);
            log.debug("?????????????????????");
            buildHeader(headerMap, requstBuild);
            log.debug("?????????????????????");
            Request request = requstBuild.get()
                    .addHeader(Header.COOKIE.toString(), mck)
                    .addHeader(Header.REFERER.toString(), "https://thunder.jd.com/")
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            response.close();
            log.info("?????????????????????:{}", body);
            JSONObject parseObject = JSON.parseObject(body);
            if (!parseObject.getBoolean("success")) {
                log.error("?????????????????????.???????????????");
                return null;
            }
            List<JSONObject> value = JSON.parseArray(parseObject.getString("value"), JSONObject.class);
            JSONObject jsonObject = null;
            if (CollUtil.isEmpty(value)) {
                log.info("??????????????????????????????????????????");
                jsonObject = build_insert_user_address(mck, client);
            } else {
                jsonObject = value.get(PreConstant.ZERO);
            }
            if (ObjectUtil.isNull(jsonObject)) {
                log.error("??????????????????msg");
            }
            String telephone = jsonObject.getString("telephone");
            String consignee = jsonObject.getString("consignee");
            String orderSettlementDetailStr = redisTemplate.opsForValue().get("????????????:" + paylink);
            if (StrUtil.isBlank(orderSettlementDetailStr)) {
                OrderSettlementDetail orderSettlementDetail = buildOrderSettlementDetailRedis(mck, client, paylink);
                if (ObjectUtil.isNotNull(orderSettlementDetail)) {
                    orderSettlementDetailStr = JSON.toJSONString(orderSettlementDetail);
                }
            }
            if (StrUtil.isBlank(orderSettlementDetailStr)) {
                log.info("????????????????????????");
                return null;
            }
            OrderSettlementDetail orderSettlementDetail = JSON.parseObject(orderSettlementDetailStr, OrderSettlementDetail.class);
            OrderSubmit orderSubmit = new OrderSubmit();
            orderSubmit.setPayLink(paylink);
            orderSubmit.setDeliveryWay(orderSettlementDetail.getDeliveryWayList().getDeliveryWay());
            orderSubmit.setConsignee(consignee);
            orderSubmit.setTelephone(telephone);
            orderSubmit.setAddressDetail(orderSettlementDetail.getAddressDetailTO().getAddressDetail());
            orderSubmit.setCode1(orderSettlementDetail.getAddressDetailTO().getProvinceId());
            orderSubmit.setCode2(orderSettlementDetail.getAddressDetailTO().getCityId());
            orderSubmit.setCode3(orderSettlementDetail.getAddressDetailTO().getCountyId());
            orderSubmit.setCode4(orderSettlementDetail.getAddressDetailTO().getTownId());
            SkuList skuList = new SkuList();
            skuList.setSkuCount(PreConstant.ONE);
            skuList.setSkuId(orderSettlementDetail.getSettlemnetSku().getSkuId());
            orderSubmit.setSkuList(Arrays.asList(skuList));
            return orderSubmit;
        } catch (Exception e) {
            if (StrUtil.isNotBlank(e.getMessage()) && (e.getMessage().contains("Failed to connect to /") || e.getMessage().contains("connect timed out"))) {
                if (ObjectUtil.isNotNull(redisTemplate)) {
                    String msg = e.getMessage().replace("Failed to connect to /", "");
                    redisTemplate.delete(PreConstant.??????IP + msg.split(":")[0]);
                }
            }
            log.error("??????????????????msg:{}", e.getMessage());
        }
        return null;
    }

    private String build_weixin_jx_pay_url(String mck, OkHttpClient client, String orderId, Map<String, String> headerMap) {
        try {
            log.info("??????????????????????????????");
            log.info("????????????msg:{}", PreUtils.get_pt_pin(mck));
            String url = String.format("https://m.jingxi.com/jdpaygw/wxmwebpay?dealId=%s&appid=wxae3e8056daea8727", orderId);
            Request.Builder requstBuild = new Request.Builder()
                    .url(url);
            buildHeader(headerMap, requstBuild);
            Request request = requstBuild.get()
                    .addHeader(Header.USER_AGENT.toString(), "Mozilla/5.0 (Linux; Android 10; PACT00) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Mobile Safari/537.36")
                    .addHeader(Header.REFERER.toString(), "https://st.jingxi.com/")
                    .addHeader(Header.COOKIE.toString(), mck)
                    .addHeader("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"102\", \"Google Chrome\";v=\"102\"")
                    .addHeader("sec-ch-ua-platform", "\"Android\"")
                    .addHeader("sec-fetch-dest", "script")
                    .addHeader("sec-fetch-mode", "no-cors")
                    .addHeader("sec-fetch-site", "same-site")
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            response.close();
            log.info("?????????????????????msg:{}", body);
            if (StrUtil.isNotBlank(body) && body.contains("prepay_id")) {
                return JSON.parseObject(JSON.parseObject(body).getString("data")).getString("mweb_url");
            }
            if (body.contains("???????????????????????????") || body.contains("???????????????????????????") || body.contains("????????????") || body.contains("??????????????????")) {
                setFailReason(mck, body);
            }
        } catch (Exception e) {
            log.error("????????????????????????????????????msg:{},payId:{}", e.getMessage(), orderId);
        }
        log.error("????????????????????????????????????orderId:{},pin:{},payId:{}", orderId, PreUtils.get_pt_pin(mck), orderId);
        return null;
    }

    private String build_weixin_pay_url(String mck, OkHttpClient client, String orderId, String payId, Map<String, String> headerMap) {
        try {
            log.info("????????????????????????");
            log.info("????????????msg:{}", PreUtils.get_pt_pin(mck));
            String url = String.format("https://pay.m.jd.com/index.action?functionId=wapWeiXinPay&body={\"payId\":\"%s\",\"appId\":\"d_m_mdbang\",\"eid\":\"\"}", payId);
            Request.Builder requstBuild = new Request.Builder()
                    .url(url);
            buildHeader(headerMap, requstBuild);
            Request request = requstBuild.get()
                    .addHeader(Header.USER_AGENT.toString(), "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1")
                    .addHeader(Header.REFERER.toString(), "https://pay.m.jd.com/cpay/newPay-index.html?appId=d_m_mdbang&payId=" + payId)
                    .addHeader(Header.COOKIE.toString(), mck)
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            log.info("?????????????????????msg:{}", body);
            response.close();
            if (StrUtil.isNotBlank(body) && body.contains("https://wx.tenpay.com")) {
                return JSON.parseObject(body).getString("mweb_url");
            }
            if (body.contains("???????????????????????????") || body.contains("???????????????????????????") || body.contains("????????????") || body.contains("??????????????????")) {
                setFailReason(mck, body);
            }
        } catch (Exception e) {
            log.error("????????????????????????????????????msg:{},payId:{}", e.getMessage(), payId);
        }
        log.error("????????????????????????????????????orderId:{},pin:{},payId:{}", orderId, PreUtils.get_pt_pin(mck), payId);
        return null;
    }

    private boolean build_order_check(String mck, OkHttpClient client, String orderId, String payId, Map<String, String> headerMap) {
        try {
            log.info("????????????msg:{}", PreUtils.get_pt_pin(mck));
            String url = "https://pay.m.jd.com/newpay/index.action";
            RequestBody requestBody = new FormBody.Builder()
                    .add("lastPage", "https://wx.tenpay.com/")
                    .add("appId", "d_m_mdbang")
                    .add("payId", payId)
                    .add("_format_", "JSON")
                    .build();
            Request.Builder requstBuild = new Request.Builder()
                    .url(url);
            buildHeader(headerMap, requstBuild);
            Request request = requstBuild.post(requestBody)
                    .addHeader(Header.USER_AGENT.toString(), "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1")
                    .addHeader(Header.REFERER.toString(), "https://pay.m.jd.com/cpay/newPay-index.html?appId=d_m_mdbang&payId=" + payId)
                    .addHeader(Header.COOKIE.toString(), mck)
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            log.info("???????????????????????????msg:{}", body);
            response.close();
            List<JSONObject> jsonObjects = JSON.parseArray(JSON.parseObject(JSON.parseObject(body).getString("payParamsObject")).getString("payChannelList"), JSONObject.class);
            for (JSONObject jsonObject : jsonObjects) {
                if (jsonObject.getString("code").equals("wapWeiXinPay")) {
                    Integer status = jsonObject.getInteger("status");
                    if (status == 1) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("????????????msg:orderId{},payId:{},msg:{}", orderId, payId, e.getMessage());
        }
        log.error("?????????????????????????????????msg:{}", orderId);
        log.info("??????????????????????????????msg:{}", orderId);
        setFailReason(mck, "????????????????????????????????????");
        JdCk jdCk = this.jdCkMapper.selectOne(Wrappers.<JdCk>lambdaQuery().eq(JdCk::getPtPin, PreUtils.get_pt_pin(mck)));
        if (ObjectUtil.isNotNull(jdCk)) {
            jdCk.setIsEnable(PreConstant.FUYI_1);
            this.jdCkMapper.updateById(jdCk);
        }
        return false;
    }

    private String build_order_payId(String mck, OkHttpClient client, String orderId, Map<String, String> headerMap) {
        try {
            log.info("??????????????????msg:{}", PreUtils.get_pt_pin(mck));
            String url = String.format("https://api.m.jd.com/api?appid=mdb&functionId=order_pay_url&body={\"orderId\":%s,\"isJDwxapp\":false}", orderId);
            Request.Builder requstBuild = new Request.Builder()
                    .url(url);
            buildHeader(headerMap, requstBuild);
            Request request = requstBuild.addHeader(Header.REFERER.toString(), "https://thunder.jd.com/")
                    .addHeader(Header.COOKIE.toString(), mck)
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            response.close();
            log.info("???????????????????????????payId???msg:{}", body);
            Boolean success = JSON.parseObject(body).getBoolean("success");
            if (!success) {
                setFailReason(mck, body);
                return null;
            }
            String payUrl = JSON.parseObject(body).getString("value");
            String payId = PreUtils.parseUrl(payUrl).getParams().get("payId");
//            https://api.m.jd.com/client.action?functionId=platPayChannel&appid=mcashier
            return payId;
        } catch (Exception e) {
            log.error("????????????????????????");
        }
        return null;
    }


    private String build_order_submit(String mck, OkHttpClient client, OrderSubmit orderSubmit, Map<String, String> headerMap) {
        try {
            log.info("????????????msg:{}", PreUtils.get_pt_pin(mck));
            String url = "https://api.m.jd.com/api?";
            RequestBody requestBody = new FormBody.Builder()
                    .add("appid", "mdb")
                    .add("functionId", "order_submit")
                    .add("body", JSON.toJSONString(orderSubmit))
                    .build();

            Request.Builder requstBuild = new Request.Builder()
                    .url(url);

            buildHeader(headerMap, requstBuild);
            Request request = requstBuild.post(requestBody)
                    .addHeader(Header.COOKIE.toString(), mck)
                    .addHeader(Header.REFERER.toString(), "https://thunder.jd.com/")
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            log.info("????????????msg:{}", body);
            response.close();
            Boolean success = JSON.parseObject(body).getBoolean("success");
            if (!success) {
                setFailReason(mck, body);
                return null;
            }
            log.info("????????????msg:{}");
            String orderId = JSON.parseObject(JSON.parseObject(body).getString("value")).getString("orderId");
            return orderId;
        } catch (Exception e) {
            log.error("????????????");
        }
        return null;
    }

    /**
     * ???????????????
     *
     * @param headerMap
     * @param requstBuild
     */
    private void buildHeader(Map<String, String> headerMap, Request.Builder requstBuild) {
        if (CollUtil.isNotEmpty(headerMap)) {
            for (String key : headerMap.keySet()) {
                requstBuild.header(key, headerMap.get(key));
            }
        }
    }

    private void setFailReason(String mck, String body) {
        JdCk jdCk = this.jdCkMapper.selectOne(Wrappers.<JdCk>lambdaQuery().eq(JdCk::getPtPin, PreUtils.get_pt_pin(mck)));
        if (ObjectUtil.isNotNull(jdCk)) {
            if (body.contains("???????????????????????????") || body.contains("???????????????????????????") || body.contains("????????????") || body.contains("??????????????????")) {
                jdCk.setIsEnable(PreConstant.ZERO);
            }
            jdCk.setFailReason(jdCk.getFailReason() + body);
            this.jdCkMapper.updateById(jdCk);
        }
    }

    private JSONObject build_insert_user_address(String ck, OkHttpClient client) {
        try {
            String add = "{\"addressDefault\":false,\"addressDetail\":\"?????????171???????????????11???31???9???\",\"cityId\":128,\"consignee\":\"?????????\",\"countyId\":10005,\"provinceId\":4,\"telephone\":\"13208568894\",\"townId\":0}";
            Set<String> keys = redisTemplate.keys(PreConstant.?????????????????? + "*");
            if (CollUtil.isNotEmpty(keys)) {
                int i = PreUtils.randomCommon(0, keys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList()).size() - 1, 1)[0];
                String adddId = keys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList()).get(i);
                add = redisTemplate.opsForValue().get(PreConstant.?????????????????? + adddId);
            }
            AddressVo addressVo = JSON.parseObject(add, AddressVo.class);
            String url = "https://api.m.jd.com/api?";
            RequestBody requestBody = new FormBody.Builder()
                    .add("appid", "mdb")
                    .add("functionId", "insert_user_address")
                    .add("body", add)
                    .build();
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader(Header.COOKIE.toString(), ck)
                    .post(requestBody)
                    .addHeader(Header.REFERER.toString(), "https://thunder.jd.com/")
                    .build();
            Response response = client.newCall(request).execute();
            String body = response.body().string();
            log.info("?????????????????????msg:{}", body);
            response.close();
            if (JSON.parseObject(body).getBoolean("success")) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("telephone", addressVo.getTelephone());
                jsonObject.put("consignee", addressVo.getConsignee());
                return jsonObject;
            }
        } catch (Exception e) {
            log.error("?????????????????????");
        }
        return null;
    }

    private OrderSettlementDetail buildOrderSettlementDetailRedis(String ck, OkHttpClient client, String paylink) throws IOException {
        String url = String.format("https://api.m.jd.com/api?appid=mdb&functionId=order_settlement_detail&body={\"payLink\":\"%s\"}",
                paylink);
        Request request = new Request.Builder()
                .url(url)
                .addHeader(Header.COOKIE.toString(), ck)
                .get()
                .addHeader(Header.REFERER.toString(), "https://thunder.jd.com/")
                .build();
        Response response = client.newCall(request).execute();
        String body = response.body().string();
        log.info("?????????????????????msg:{}", body);
        response.close();
        JSONObject addressDto = JSON.parseObject(body);
        if (!addressDto.getBoolean("success")) {
            log.error("????????????????????????");
            return null;
        }
        JSONObject OrderSettlementDetailJson = JSON.parseObject(addressDto.getString("value"));
        SettlemnetSku settlemnetSku = JSON.parseArray(OrderSettlementDetailJson.getString("skuList"), SettlemnetSku.class).get(PreConstant.ZERO);
        AddressDetailTO addressDetailTO = JSON.parseObject(OrderSettlementDetailJson.getString("addressDetailTO"), AddressDetailTO.class);
        DeliveryWayList deliveryWayList = JSON.parseArray(OrderSettlementDetailJson.getString("deliveryWayList"), DeliveryWayList.class).get(PreConstant.ZERO);
        OrderSettlementDetail orderSettlementDetail = new OrderSettlementDetail(settlemnetSku, addressDetailTO, deliveryWayList);
        redisTemplate.opsForValue().set("????????????:" + paylink, JSON.toJSONString(orderSettlementDetail), 12, TimeUnit.HOURS);
        return orderSettlementDetail;
    }


//    @Scheduled(cron = "0/20 * * * * ?")
//    @Async("asyncPool")
    public void synAddress() {
        log.info("?????????????????????redis????????????redis?????????");
        log.info("???????????????????????????");
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("????????????", "????????????", 3, TimeUnit.MINUTES);
        if (!ifAbsent) {
            log.info("???????????????????????????");
            return;
        }
        List<JdAddress> jdAddresses1 = jdAddressMapper.selectList(Wrappers.<JdAddress>lambdaQuery().eq(JdAddress::getAddressLevel, PreConstant.ONE));
        Map<Integer, JdAddress> map1 = jdAddresses1.stream().collect(Collectors.toMap(it -> it.getAddressId(), it -> it));
        List<JdAddress> jdAddresses2 = jdAddressMapper.selectList(Wrappers.<JdAddress>lambdaQuery().eq(JdAddress::getAddressLevel, PreConstant.TWO));
        Map<Integer, JdAddress> map2 = jdAddresses2.stream().collect(Collectors.toMap(it -> it.getAddressId(), it -> it));
        List<JdAddress> jdAddresses3 = jdAddressMapper.selectList(Wrappers.<JdAddress>lambdaQuery().eq(JdAddress::getAddressLevel, PreConstant.THREE));
        for (JdAddress jdAddress : jdAddresses3) {
            String chineseName = MenDianRandomUtil.getChineseName();
            String telephone = MenDianRandomUtil.getTelephone();
            Integer countyId = jdAddress.getAddressId();
            Integer cityId = jdAddress.getParentId();
            JdAddress jdAddressMap2 = map2.get(cityId);
            Integer provinceId = map1.get(jdAddressMap2.getParentId()).getAddressId();
            AddressVo build = AddressVo.builder().consignee(chineseName).telephone(telephone).provinceId(provinceId).cityId(cityId).countyId(countyId)
                    .addressDetail(MenDianRandomUtil.getRandomAddress())
                    .townId(PreConstant.ZERO).addressDefault(false)
                    .build();
            redisTemplate.opsForValue().set(PreConstant.?????????????????? + build.getCountyId(), JSON.toJSONString(build), 5, TimeUnit.MINUTES);
        }
        log.info("?????????????????????redis????????????redis?????????");
    }


    //    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void buildJdDjCk() {
        JdProxyIpPort oneIp = this.proxyProductService.getOneIp(PreConstant.ZERO, PreConstant.ZERO, false);
        LambdaQueryWrapper<JdCk> aNull = Wrappers.<JdCk>lambdaQuery()
                .isNull(JdCk::getMckCreateTime)
                .in(JdCk::getIsEnable, Arrays.asList(PreConstant.ONE, PreConstant.FIVE));
        List<JdCk> jdCks = jdCkMapper.selectList(aNull);
        for (JdCk jdCk : jdCks) {
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("????????????MCK:" + jdCk.getPtPin(), jdCk.getPtPin(), 3, TimeUnit.MINUTES);
            if (ifAbsent) {
                Boolean ifAbsent1 = redisTemplate.opsForValue().setIfAbsent("IP?????????:" + oneIp.getId(), oneIp.getId() + "", 2, TimeUnit.MINUTES);
                if (!ifAbsent1) {
                    buildJdMck(oneIp, jdCk);
                } else {
                    log.debug("??????ip??????");
                    oneIp = this.proxyProductService.getOneIp(PreConstant.ZERO, PreConstant.ZERO, false);
                }
            }
        }
        aNull = Wrappers.<JdCk>lambdaQuery()
                .le(JdCk::getMckCreateTime, DateUtil.offsetHour(new Date(), -5))
                .in(JdCk::getIsEnable, Arrays.asList(PreConstant.ONE, PreConstant.FIVE));
        jdCks = jdCkMapper.selectList(aNull);
        for (JdCk jdCk : jdCks) {
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("????????????MCK:" + jdCk.getPtPin(), jdCk.getPtPin(), 3, TimeUnit.MINUTES);
            if (!ifAbsent) {
                continue;
            }
            Boolean ifAbsent1 = redisTemplate.opsForValue().setIfAbsent("IP?????????:" + oneIp.getId(), oneIp.getId() + "", 2, TimeUnit.MINUTES);
            if (!ifAbsent1) {
                buildJdMck(oneIp, jdCk);
            } else {
                oneIp = this.proxyProductService.getOneIp(PreConstant.ZERO, PreConstant.ZERO, false);
            }
        }
    }

    private JdCk buildJdMck(JdProxyIpPort oneIp, JdCk jdCk) {
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(oneIp.getIp(), Integer.valueOf(oneIp.getPort())));
            TokenKeyVo build = TokenKeyVo.builder().cookie(jdCk.getCk().trim()).build();
            TokenKeyResVo tokenKeyVO = tokenKeyService.getTokenKey(build, oneIp, "");
            if (ObjectUtil.isNull(tokenKeyVO)) {
                log.error("appck????????????????????????");
                return null;
            }
            String mck = newWeiXinPayUrl.getMck(proxy, tokenKeyVO.getTokenKey());
            if (StrUtil.isBlank(mck) || !mck.contains("app_openAA")) {
                log.error("??????ckmsg:{},??????mck??????", jdCk.getCk());
                return null;
            }
            jdCk.setMck(mck);
            jdCk.setMckCreateTime(new Date());
            this.jdCkMapper.updateById(jdCk);
            return jdCk;
        } catch (Exception e) {
            log.error("????????????mck??????,mckMsg:{}", jdCk.getPtPin());
        }
        return null;
    }


    public void selectOrderStataus(JdOrderPt jdOrderPt, JdMchOrder jdMchOrder) {
        CallBackVo callBackVo = new CallBackVo(jdOrderPt.getOrderId());
        String url = String.format("https://api.m.jd.com/client.action?loginType=2&appid=m_core&functionId=order_detail_m&body=%s", JSON.toJSONString(callBackVo));
        String result2 = HttpRequest.get(url)
                .header(Header.COOKIE, jdOrderPt.getCurrentCk())//??????????????????????????????????????????????????????
                .header(Header.REFERER, "https://wqs.jd.com/")//??????????????????????????????????????????????????????
                .timeout(20000)//???????????????
                .execute().body();
        if (StrUtil.isBlank(result2)) {
            return;
        }
        String body = JSON.parseObject(result2).getString("body");
        String orderStatusInfo = JSON.parseObject(body).getString("orderStatusInfo");
        log.info("????????????OrderId:{},????????????msg:{}", jdMchOrder.getTradeNo(), orderStatusInfo);
        jdOrderPt.setHtml(orderStatusInfo);
        JSONObject parseObject = JSON.parseObject(orderStatusInfo);
        if (parseObject.getInteger("status") == PreConstant.FIVE) {
            log.info("????????????++++++++++++++++++++++++????????????");
            jdOrderPt.setPaySuccessTime(new Date());
            jdOrderPt.setCardNumber(jdOrderPt.getOrderId());
            jdOrderPt.setCarMy(jdOrderPt.getOrderId());
            jdMchOrder.setStatus(PreConstant.TWO);
            this.jdMchOrderMapper.updateById(jdMchOrder);
        }
        this.jdOrderPtMapper.updateById(jdOrderPt);


    }
}
