package com.xd.pre.modules.px.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.data.tenant.PreTenantContextHolder;
import com.xd.pre.modules.px.service.NewWeiXinPayUrl;
import com.xd.pre.modules.px.service.ProxyProductService;
import com.xd.pre.modules.px.vo.sys.NotifyVo;
import com.xd.pre.modules.sys.domain.*;
import com.xd.pre.modules.sys.mapper.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.activemq.ScheduledMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.Queue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductProxyTask {

    @Autowired
    private ProxyProductService proxyProductService;


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private JdAppStoreConfigMapper jdAppStoreConfigMapper;

    @Autowired
    private NewWeiXinPayUrl newWeiXinPayUrl;

    @Resource(name = "product_stock_queue")
    private Queue product_stock_queue;


    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Resource
    private JdMchOrderMapper jdMchOrderMapper;

    @Resource
    private JdProxyIpPortMapper jdProxyIpPortMapper;


    @Autowired
    private NewWeiXinPayUrl weiXinPayUrl;


    @Resource
    private JdOrderPtMapper jdOrderPtMapper;

    /**
     * ????????????
     */
    @Resource(name = "activate_meituan_queue")
    private Queue activate_meituan_queue;
    /**
     * ????????????
     */
    @Resource(name = "activate_queue")
    private Queue activate_queue;

    @Resource
    private JdCkMapper jdCkMapper;


    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessage(Destination destination, final String message) {
        //???????????????????????????10???,????????????
        jmsMessagingTemplate.convertAndSend(destination, message);
    }


    public void productWxNumMeiTuan(JdAppStoreConfig jdAppStoreConfig) {
        log.info("??????????????????????????????");
        List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.<JdAppStoreConfig>lambdaQuery()
                .eq(JdAppStoreConfig::getIsProduct, 1).eq(JdAppStoreConfig::getSkuId, jdAppStoreConfig.getSkuId()));
        if (CollUtil.isEmpty(jdAppStoreConfigs)) {
            log.info("?????????????????????");
            return;
        }
        for (int i = 0; i < jdAppStoreConfig.getProductStockNum(); i++) {
            redisTemplate.opsForValue().increment("stock:" + jdAppStoreConfig.getSkuId(), 1);
            sendMessage(this.product_stock_queue, JSON.toJSONString(jdAppStoreConfig));
        }
        log.info("??????????????????????????????????????????");
    }

    @Resource
    private AreaIpMapper areaIpMapper;


    @Scheduled(cron = "0 0/1 * * * ? ")
    @Async("asyncPool")
    public void productAll() {
        String ipLock = redisTemplate.opsForValue().get("????????????IP??????");
        if (StrUtil.isNotBlank(ipLock)) {
            return;
        }
        log.info("??????????????????ip50");
        redisTemplate.opsForValue().set("????????????IP??????", "??????IP??????", 3, TimeUnit.MINUTES);
        proxyProductService.productIpAndPort1();
        proxyProductService.productIpAndPort2();
    }


    //    @Scheduled(cron = "0 0/1 * * * ? ")
    @Async("asyncPool")
    public void productfindMaxOrder() {
        log.info("????????????????????????????????????2???????????????");
        JdAppStoreConfig jdAppStoreConfig = this.jdAppStoreConfigMapper.selectOne(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getSkuId, "11183343342"));
        List<String> pins = jdOrderPtMapper.selectMax2Data(jdAppStoreConfig.getProductNum());
        //???????????????????????????????????????????????????????????????????????????
        List<JdOrderPt> jdOrderPts = jdOrderPtMapper.selectList(Wrappers.<JdOrderPt>lambdaQuery()
                .in(JdOrderPt::getPtPin, pins).gt(JdOrderPt::getExpireTime, new Date()));
        if (CollUtil.isNotEmpty(pins)) {
            Map<String, List<JdOrderPt>> orders = jdOrderPts.stream().collect(Collectors.groupingBy(JdOrderPt::getPtPin));
            for (String pin : pins) {
                //?????????????????????2????????????????????????????????????????????????????????????????????????????????????
                List<JdOrderPt> jdOrderPtsPay = orders.get(pin);
                if (CollUtil.isNotEmpty(jdOrderPtsPay)) {
                    //??????????????????????????????lock
                    List<JdOrderPt> notPayOrders = jdOrderPtsPay.stream().filter(it -> ObjectUtil.isNull(it.getCarMy())).collect(Collectors.toList());
                    if (CollUtil.isNotEmpty(notPayOrders) && notPayOrders.size() >= jdAppStoreConfig.getProductNum()) {
                        redisTemplate.opsForValue().set("????????????2?????????CK:" + URLDecoder.decode(pin), pin, 5, TimeUnit.MINUTES);
                    } else {
                        String lockpinRe = redisTemplate.opsForValue().get("??????????????????:" + pin);
                        if (StrUtil.isNotBlank(lockpinRe)) {
                            continue;
                        }
                        redisTemplate.opsForValue().set("??????????????????:" + pin, pin, 2, TimeUnit.MINUTES);
                        redisTemplate.delete("????????????2?????????CK:" + URLDecoder.decode(pin));
                        jdCkMapper.updateByPin(pin);
                    }
                }
            }
        }
//        List<String> pinPays = jdOrderPtMapper.selectMax2DataPay(jdAppStoreConfig.getProductNum());
        log.info("????????????????????????????????????2???????????????");
    }

    //    @Scheduled(cron = "0 0/1 * * * ? ")
    @Async("asyncPool")
    public void autoDeleteFailAccount() {
        List<JdOrderPt> orderPts = jdOrderPtMapper.selectList(Wrappers.<JdOrderPt>lambdaQuery()
                .gt(JdOrderPt::getCreateTime, DateUtil.offsetHour(new Date(), -24))
                .like(JdOrderPt::getHtml, "????????????,???????????????"));
        if (CollUtil.isEmpty(orderPts)) {
            return;
        }
        List<String> orderIds = orderPts.stream().map(it -> it.getOrderId()).distinct().collect(Collectors.toList());

        List<JdMchOrder> jdMchOrders = jdMchOrderMapper.selectList(Wrappers.<JdMchOrder>lambdaQuery().in(JdMchOrder::getOriginalTradeNo, orderIds)
                .notIn(JdMchOrder::getStatus, PreConstant.THREE));
        if (CollUtil.isNotEmpty(jdMchOrders)) {
            for (JdMchOrder jdMchOrder : jdMchOrders) {
                jdMchOrder.setStatus(PreConstant.THREE);
                jdMchOrderMapper.updateById(jdMchOrder);
            }
        }
        List<String> pins = orderPts.stream().map(JdOrderPt::getPtPin).distinct().collect(Collectors.toList());
        List<JdOrderPt> jdOrderPts = jdOrderPtMapper.selectList(Wrappers.<JdOrderPt>lambdaQuery().in(JdOrderPt::getPtPin, pins));
        for (JdOrderPt jdOrderPt : jdOrderPts) {
            jdOrderPt.setIsEnable(PreConstant.ZERO);
            jdOrderPt.setIsWxSuccess(PreConstant.ZERO);
            jdOrderPt.setFailTime(PreConstant.HUNDRED);
            jdOrderPt.setRetryTime(PreConstant.HUNDRED);
            redisTemplate.delete(PreConstant.???????????????????????? + jdOrderPt.getOrderId());
//            jdOrderPtMapper.deleteById(jdOrderPt.getId());
            jdOrderPtMapper.updateById(jdOrderPt);
        }
        List<JdCk> jdCks = this.jdCkMapper.selectList(Wrappers.<JdCk>lambdaQuery().in(JdCk::getPtPin, pins).in(JdCk::getIsEnable, Arrays.asList(1, 5)));
        if (CollUtil.isNotEmpty(jdCks)) {
            for (JdCk jdCk : jdCks) {
                jdCk.setIsEnable(PreConstant.ZERO);
                jdCk.setFailTime(PreConstant.HUNDRED);
                jdCkMapper.updateById(jdCk);
//                jdCkMapper.deleteById(jdCk.getId());
            }
        }
    }

    @Async("asyncPool")
//    @Scheduled(cron = "0/30 * * * * ?")
    public void activationMeiTuan() {
        List<JdAppStoreConfig> jdAppStoreConfigs = this.jdAppStoreConfigMapper.selectList(Wrappers.<JdAppStoreConfig>lambdaQuery().eq(JdAppStoreConfig::getGroupNum, PreConstant.TWO));
        List<String> meituanSkuIds = jdAppStoreConfigs.stream().map(it -> it.getSkuId()).distinct().collect(Collectors.toList());
        LambdaQueryWrapper<JdOrderPt> wrapper = Wrappers.<JdOrderPt>lambdaQuery()
                .in(JdOrderPt::getSkuId, meituanSkuIds)
                .gt(JdOrderPt::getExpireTime, new Date())
                .eq(JdOrderPt::getIsWxSuccess, PreConstant.ONE);
        List<JdOrderPt> jdOrderPts = this.jdOrderPtMapper.selectList(wrapper);
        if (CollUtil.isEmpty(jdOrderPts)) {
            return;
        }
        Map<String, List<JdOrderPt>> pinMaps = jdOrderPts.stream().collect(Collectors.groupingBy(JdOrderPt::getPtPin));
        for (String pin : pinMaps.keySet()) {
            List<JdOrderPt> jdOrderPtsIns = pinMaps.get(pin);
            int i = PreUtils.randomCommon(0, jdOrderPtsIns.size(), 1)[0];
            JdOrderPt jdOrderPtDb = jdOrderPtsIns.get(i);
            Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent(PreConstant.???????????? + pin, JSON.toJSONString(jdOrderPtsIns), 3, TimeUnit.MINUTES);
            if (!ifAbsent) {
                continue;
            }
            this.sendMessageSenc(this.activate_meituan_queue, JSON.toJSONString(jdOrderPtDb), PreUtils.randomCommon(1, 150, 1)[0]);
        }
    }


    //    @Scheduled(cron = "0/30 * * * * ?")
    @Async("asyncPool")
    public void activation() {
        Set<String> keys = redisTemplate.keys("??????:*");
        LambdaQueryWrapper<JdOrderPt> wrapper = Wrappers.<JdOrderPt>lambdaQuery()
                .gt(JdOrderPt::getExpireTime, new Date())
                .le(JdOrderPt::getFailTime, PreConstant.TEN)
                .eq(JdOrderPt::getIsWxSuccess, PreConstant.ONE)
                .isNull(JdOrderPt::getCardNumber);
        if (CollUtil.isNotEmpty(keys)) {
            List<String> orderIds = new ArrayList<>();
            for (String key : keys) {
                String orderId = key.split(":")[1];
                orderIds.add(orderId);
            }
            wrapper.notIn(JdOrderPt::getOrderId, orderIds);
        }
//        keys = redisTemplate.keys(PreConstant.????????????CK?????????????????? + "*);
//        if (CollUtil.isNotEmpty(keys) && jdAppStoreConfig.getGroupNum() == PreConstant.TWO) {
//            List<String> lockKeys = keys.stream().map(it -> it.split(":")[1]).map(it -> URLDecoder.decode(it)).collect(Collectors.toList());
//            wrapper.notIn(JdOrderPt::getPtPin, lockKeys);
//        }
        List<JdOrderPt> orderPts = this.jdOrderPtMapper.selectList(wrapper);
        mark:
        for (JdOrderPt jdOrderPtDb : orderPts) {
            String jihuo = redisTemplate.opsForValue().get("??????:" + jdOrderPtDb.getOrderId());
            if (StrUtil.isNotBlank(jihuo)) {
                continue;
            }
            String config = redisTemplate.opsForValue().get("????????????:" + jdOrderPtDb.getSkuId());
            if (StrUtil.isBlank(config)) {
                List<JdAppStoreConfig> jdAppStoreConfigs = jdAppStoreConfigMapper.selectList(Wrappers.emptyWrapper());
                for (JdAppStoreConfig jdAppStoreConfig : jdAppStoreConfigs) {
                    redisTemplate.opsForValue().set("????????????:" + jdAppStoreConfig.getSkuId(), JSON.toJSONString(jdAppStoreConfig), 1, TimeUnit.DAYS);
                }
                config = redisTemplate.opsForValue().get("????????????:" + jdOrderPtDb.getSkuId());
            }
            JdAppStoreConfig jdAppStoreConfig = JSON.parseObject(config, JdAppStoreConfig.class);
            String lock = redisTemplate.opsForValue().get(PreConstant.????????????CK?????????????????? + jdOrderPtDb.getPtPin());
            if (jdAppStoreConfig.getGroupNum() == PreConstant.TWO && StrUtil.isNotBlank(lock) && jdOrderPtDb.getIsWxSuccess() == PreConstant.ONE) {
             /*   Map<String, List<JdOrderPt>> mapKeys = orderPts.stream().collect(Collectors.groupingBy(JdOrderPt::getPtPin));
                List<JdOrderPt> jdOrderPtsByPin = mapKeys.get(jdOrderPtDb.getPtPin());
                for (JdOrderPt jdOrderPtByPin : jdOrderPtsByPin) {
                    String bypin = redisTemplate.opsForValue().get(PreConstant.???????????????????????? + jdOrderPtByPin.getOrderId());
                    if (StrUtil.isNotBlank(bypin)) {
                        continue mark;
                    }
                }
                R r = activateService.reSetNoAsync(jdOrderPtDb, activateService.getIp(), jdOrderPtsByPin);
                if (ObjectUtil.isNotNull(r) && r.getCode() == HttpStatus.HTTP_OK) {
                    if (CollUtil.isNotEmpty(jdOrderPtsByPin)) {
                        for (JdOrderPt jdOrderPtByPin : jdOrderPtsByPin) {
                            redisTemplate.opsForValue().set(PreConstant.???????????????????????? + jdOrderPtByPin.getOrderId(), "1111", 4, TimeUnit.MINUTES);
                            jdOrderPtByPin.setRetryTime(PreConstant.ZERO);
                            jdOrderPtByPin.setIsWxSuccess(PreConstant.ONE);
                            jdOrderPtByPin.setFailTime(PreConstant.ZERO);
                            this.jdOrderPtMapper.updateById(jdOrderPtByPin);
                        }
                    }
                    redisTemplate.delete(PreConstant.????????????CK?????????????????? + jdOrderPtDb.getPtPin());
                    continue;
                }*/
                redisTemplate.opsForValue().set("??????????????????:" + jdOrderPtDb.getOrderId(), jdOrderPtDb.getOrderId(), 12, TimeUnit.HOURS);
                jdOrderPtDb.setIsWxSuccess(PreConstant.ZERO);
                jdOrderPtDb.setFailTime(1200);
                this.jdOrderPtMapper.updateById(jdOrderPtDb);
                continue;
            }
            redisTemplate.opsForValue().set("??????:" + jdOrderPtDb.getOrderId(), jdOrderPtDb.getOrderId(), 3, TimeUnit.MINUTES);
            this.sendMessageSenc(this.activate_queue, JSON.toJSONString(jdOrderPtDb), PreUtils.randomCommon(1, 140, 1)[0]);
        }
    }

    // ???????????????destination????????????????????????message?????????????????????
    private void sendMessageSenc(Destination destination, final String message, Integer minit) {
        Map<String, Object> headers = new HashMap<>();
        //???????????????????????????10???,????????????
        headers.put(ScheduledMessage.AMQ_SCHEDULED_DELAY, minit * 1000);
        jmsMessagingTemplate.convertAndSend(destination, message, headers);
    }


    @Scheduled(cron = "0 0/5 * * * ? ")
    @Async("asyncPool")
    public void deleteIp() {
        log.info("?????????????????????IP");
        jdProxyIpPortMapper.delete(Wrappers.<JdProxyIpPort>lambdaQuery().lt(JdProxyIpPort::getExpirationTime, new Date()));
    }

    public static Map<Integer, OkHttpClient> okClient = new HashMap();

    @Scheduled(cron = "0 0/1 * * * ? ")
    @Async("asyncPool")
    public void productOkHttpClient() {
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("??????OkHttpClient", "1", 50, TimeUnit.SECONDS);
        if (!ifAbsent) {
            return;
        }
        if (CollUtil.isNotEmpty(okClient)) {
            Set<Integer> exs = okClient.keySet();
            ArrayList<Integer> removes = new ArrayList<>();
            for (Integer ex : exs) {
                String isAble = redisTemplate.opsForValue().get("IP?????????:" + ex);
                if (StrUtil.isBlank(isAble)) {
                    removes.add(ex);
                }
            }
            if (CollUtil.isNotEmpty(removes)) {
                for (Integer removeId : removes) {
                    redisTemplate.delete("????????????OkHttpClient:" + removeId);
                    okClient.remove(removeId);
                }
            }
            if (CollUtil.isNotEmpty(okClient)) {
                for (Integer ex : exs) {
                    redisTemplate.opsForValue().set("????????????OkHttpClient:" + ex, JSON.toJSONString(okClient.get(ex)), 1, TimeUnit.MINUTES);
                }
            }
        }

        if (CollUtil.isNotEmpty(okClient) && okClient.size() > 20) {
            return;
        }
        Integer count = jdMchOrderMapper.selectCount(Wrappers.<JdMchOrder>lambdaQuery().gt(JdMchOrder::getCreateTime, DateUtil.offsetMinute(new Date(), -10)));
        if (count == 0) {
            return;
        }
        Integer forData = 5;
        int mi = count / 5;
        if (mi > forData) {
            forData = mi * 2;
        }
        for (int i = 0; i < forData; i++) {
            JdProxyIpPort oneIp = this.proxyProductService.getOneIp(PreConstant.ZERO, PreConstant.ZERO, false);
            try {
                log.info("10?????????????????????,????????????OkHttpClinet");
                buildStaticIp(oneIp);
            } catch (Exception e) {
                log.error("????????????okHttpClient:{}", e.getMessage());
            }
        }
    }

    @Async("asyncPool")
    public void buildStaticIp(JdProxyIpPort oneIp) throws IOException {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
        String isAble = redisTemplate.opsForValue().get("??????????????????");
        if (StrUtil.isNotBlank(isAble) && Integer.valueOf(isAble) == PreConstant.ONE) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(oneIp.getIp(), Integer.valueOf(oneIp.getPort())));
            builder.proxy(proxy);
        } else {
            redisTemplate.opsForValue().set("??????????????????", "1");
        }
        OkHttpClient client = builder.connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS)
                .followRedirects(false).build();
        Request request = new Request.Builder()
                .url("http://210.16.122.100")
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Mobile Safari/537.36")
                .build();
        Response response = client.newCall(request).execute();
        String resStr = response.body().string();
        response.close();
        if (StrUtil.isNotBlank(resStr) && resStr.contains("????????????")) {
            okClient.put(oneIp.getId(), client);
            redisTemplate.opsForValue().set("????????????OkHttpClient:" + oneIp.getId(), JSON.toJSONString(oneIp), 3, TimeUnit.MINUTES);
        }
    }


    @Scheduled(cron = "0/20 * * * * ?")
    @Async("asyncPool")
    public void callBack() {
        Integer callBack = -300;
        String callBackStr = redisTemplate.opsForValue().get("???????????????");
        if (StrUtil.isBlank(callBackStr)) {
            redisTemplate.opsForValue().set("???????????????", "-300");
        } else {
            callBack = Integer.valueOf(callBackStr);
        }
        DateTime dateTime = DateUtil.offsetSecond(new Date(), callBack);
        List<JdMchOrder> jdMchOrders = jdMchOrderMapper.selectList(Wrappers.<JdMchOrder>lambdaQuery()
                .ge(JdMchOrder::getCreateTime, dateTime).isNotNull(JdMchOrder::getOriginalTradeNo)
                .eq(JdMchOrder::getPassCode, PreConstant.EIGHT));
        for (JdMchOrder jdMchOrder : jdMchOrders) {
            try {
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                String data = redisTemplate.opsForValue().get("????????????:" + jdMchOrder.getTradeNo());
                if (StrUtil.isNotBlank(data)) {
                    return;
                }
                log.info("?????????:{}????????????????????????", jdMchOrder.getTradeNo());
                redisTemplate.opsForValue().set("????????????:" + jdMchOrder.getTradeNo(), jdMchOrder.getTradeNo(), 10, TimeUnit.SECONDS);
                if (jdMchOrder.getStatus() != 2) {
                    weiXinPayUrl.getCartNumAndMy(jdMchOrder);
                    JdMchOrder jdMchOrderIn = jdMchOrderMapper.selectById(jdMchOrder.getId());
                    if (jdMchOrderIn.getStatus() == 2) {
                        notifySuccess(jdMchOrder);
                    }
                }
            } catch (Exception e) {
                log.info("??????????????????msg:{}", e.getStackTrace());
            }
        }
    }

    /**
     * ??????
     *
     * @param jdMchOrder
     */
    public Boolean notifySuccess(JdMchOrder jdMchOrder) {
        try {
            JdMchOrder jdMchOrderDb = jdMchOrderMapper.selectById(jdMchOrder.getId());
            if (ObjectUtil.isNull(jdMchOrder) || jdMchOrder.getStatus() != PreConstant.TWO) {
                log.info("?????????:{},????????????????????????", jdMchOrder.getTradeNo());
                return false;
            }
            NotifyVo notifyVo = NotifyVo.builder()
                    .mch_id(jdMchOrderDb.getMchId())
                    .trade_no(jdMchOrderDb.getTradeNo())
                    .out_trade_no(jdMchOrderDb.getOutTradeNo())
                    .money(jdMchOrderDb.getMoney())
                    .notify_time(DateUtil.formatDateTime(new Date()))
                    .status(jdMchOrderDb.getStatus() + "").build();
            cn.hutool.json.JSON json = new JSONObject(notifyVo);
            String result = HttpRequest.post(jdMchOrderDb.getNotifyUrl())
                    .body(JSON.toJSONString(json))
                    .timeout(5000)
                    .execute().body();
            log.info("?????????:{},??????????????????:{}",jdMchOrder.getTradeNo(),result);
            if (StrUtil.isNotBlank(result) && result.toLowerCase().equals("success")) {
                log.info("?????????:{}??????????????????", jdMchOrder.getTradeNo());
                jdMchOrderDb.setNotifySucc(PreConstant.ONE);
                PreTenantContextHolder.setCurrentTenantId(jdMchOrder.getTenantId());
                log.info("????????????{}??????????????????????????????:{},??????:{}", jdMchOrder.getTradeNo(), JSON.toJSONString(jdMchOrderDb), jdMchOrder.getTenantId());
                jdMchOrderMapper.updateByIdNotSuccess(jdMchOrder.getId(), PreConstant.ONE);
                return true;
            }
        } catch (Exception e) {
            log.error("?????????{}????????????????????????", jdMchOrder.getTradeNo());
        }
        return false;
    }


}
