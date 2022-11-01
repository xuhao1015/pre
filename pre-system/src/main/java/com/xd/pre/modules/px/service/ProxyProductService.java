package com.xd.pre.modules.px.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.sign.JdSgin;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.common.utils.px.dto.SignVoAndDto;
import com.xd.pre.modules.data.tenant.PreTenantContextHolder;
import com.xd.pre.modules.px.vo.tmpvo.sysvo.ProxyAnalysisVo;
import com.xd.pre.modules.sys.domain.JdMchOrder;
import com.xd.pre.modules.sys.domain.JdPathConfig;
import com.xd.pre.modules.sys.domain.JdProxyIpPort;
import com.xd.pre.modules.sys.domain.ProxyAddressProduct;
import com.xd.pre.modules.sys.mapper.JdMchOrderMapper;
import com.xd.pre.modules.sys.mapper.JdPathConfigMapper;
import com.xd.pre.modules.sys.mapper.JdProxyIpPortMapper;
import com.xd.pre.modules.sys.mapper.ProxyAddressProductMapper;
import com.xd.pre.modules.sys.service.IJdProxyIpPortService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProxyProductService {


    @Resource
    private JdProxyIpPortMapper jdProxyIpPortMapper;
    @Resource
    private ProxyAddressProductMapper proxyAddressProductMapper;
    @Resource
    private JdPathConfigMapper jdPathConfigMapper;

    @Autowired
    private IJdProxyIpPortService jdProxyIpPortService;

    @Resource
    private JdMchOrderMapper jdMchOrderMapper;

    public void productIpAndPort1() {
        try {
            ProxyAddressProduct proxyAddressProduct = proxyAddressProductMapper.selectOne(Wrappers.<ProxyAddressProduct>lambdaQuery().eq(ProxyAddressProduct::getIsProduct, 1));
            if (proxyAddressProduct.getType() != 1) {
                return;
            }
            if (proxyAddressProduct.getIsProduct() == 0) {
                log.info("ip不启用");
                return;
            }
            String producUrl = String.format(proxyAddressProduct.getAgentAddress(), proxyAddressProduct.getNum());
            String s = HttpUtil.get(producUrl);
            log.info("当前生成的ip为msg:[data:{}]", s);
            JSONObject jsonObject = JSON.parseObject(s);
            Integer success = Integer.valueOf(jsonObject.get("code").toString());
            if (success == 0) {
                insertData(proxyAddressProduct, producUrl, jsonObject);
            } else {
                log.info("失败的url为msg:{}", producUrl);
                s = HttpUtil.get(producUrl);
                jsonObject = JSON.parseObject(s);
                insertData(proxyAddressProduct, producUrl, jsonObject);
            }
        } catch (Exception e) {
            log.info("生产报错，msg:{}", e.getMessage());
        }

    }

    private void insertData(ProxyAddressProduct proxyAddressProduct, String producUrl, JSONObject jsonObject) {
        List<ProxyAnalysisVo> data = JSON.parseArray(jsonObject.get("data").toString(), ProxyAnalysisVo.class);
        if (CollUtil.isNotEmpty(data)) {
            List<JdProxyIpPort> jdProxyIpPorts = new ArrayList<>();
            for (ProxyAnalysisVo it : data) {
                Integer expirationTime = proxyAddressProduct.getExpirationTime();
                DateTime date = DateUtil.parse(it.getExpire_time());
                DateTime ex = DateUtil.offsetMinute(date, -expirationTime);
                JdProxyIpPort jdProxyIpPort = new JdProxyIpPort().builder().agentAddress(producUrl).ip(it.getIp())
                        .city(it.getCity())
                        .port(it.getPort() + "").createTime(new Date()).isUse(0).expirationTime(ex).build();
                jdProxyIpPorts.add(jdProxyIpPort);
            }
            jdProxyIpPortService.saveBatch(jdProxyIpPorts, 100);
        }
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    public synchronized JdProxyIpPort getOneIp(Integer isUse, Integer index, Boolean isAc) {
        if (isAc) {
            return getJdProxyIpPort_TrueAc(isUse, index, isAc);
        } else {
            return getJdProxyIpPort_falseAc(isUse, index, isAc);
        }
    }

    public void productIpAndPort2() {
        log.info("执行1");
        ProxyAddressProduct proxyAddressProduct = proxyAddressProductMapper.selectOne(Wrappers.<ProxyAddressProduct>lambdaQuery().eq(ProxyAddressProduct::getIsProduct, 1));
        if (proxyAddressProduct.getType() != 2) {
            return;
        }
        log.info("执行2");
       /* Integer count = jdProxyIpPortMapper.selectCount(Wrappers.<JdProxyIpPort>lambdaQuery().gt(JdProxyIpPort::getExpirationTime, new Date()));
        if (count >= Integer.valueOf(proxyNumStr)) {
            return;
        }*/
        PreTenantContextHolder.setCurrentTenantId(1L);
        String 代理生产管理订单时间相差多少分钟 = redisTemplate.opsForValue().get("代理生产管理订单时间相差多少分钟");
        Integer integer = Integer.valueOf(代理生产管理订单时间相差多少分钟);
        Integer count1 = jdMchOrderMapper.selectCount(Wrappers.<JdMchOrder>lambdaQuery().gt(JdMchOrder::getCreateTime, DateUtil.offsetMinute(new Date(), -integer)));
        if (count1 == 0) {
            return;
        }
        log.info("执行4");
        log.info("执行5");
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("熊猫代理", "锁定", 12, TimeUnit.SECONDS);
        if (!ifAbsent) {
            log.info("执行6");
            return;
        }
        String producUrl = String.format(proxyAddressProduct.getAgentAddress(), proxyAddressProduct.getNum());
        String s = HttpUtil.get(producUrl);
        JSONObject jsonObject = JSON.parseObject(s);
        log.info("执行ip结果为msg:{}", JSON.toJSONString(jsonObject));
        Integer success = Integer.valueOf(jsonObject.get("code").toString());
        if (success == 0) {
            log.info("执行7");
            log.info("当前生成的ip为msg:[data:{}]", s);
            JSONArray obj = JSON.parseArray(JSON.toJSONString(jsonObject.get("obj")));
            for (Object o : obj) {
                JSONObject parseObject = JSON.parseObject(JSON.toJSONString(o));
                String port = parseObject.get("port").toString();
                String ip = parseObject.get("ip").toString();
                String ip过期时间 = redisTemplate.opsForValue().get("IP过期时间");
                DateTime validTime = DateUtil.offsetSecond(new Date(), Integer.valueOf(ip过期时间));
                if (parseObject.containsKey("validTime")) {
                    validTime = DateUtil.parseDateTime(parseObject.getString("validTime"));
                }
                JdProxyIpPort jdProxyIpPort = new JdProxyIpPort().builder().agentAddress(producUrl).ip(ip)
                        .port(port).createTime(new Date()).isUse(0).expirationTime(validTime).build();
                jdProxyIpPortMapper.insert(jdProxyIpPort);
            }
        }
        return;
    }

    private JdProxyIpPort getJdProxyIpPort_TrueAc(Integer isUse, Integer index, Boolean isAc) {
        try {
            index = index + 1;
            JdProxyIpPort jdProxyIpPort = null;
            if (isUse == 1) {
                LambdaQueryWrapper<JdProxyIpPort> wrapper = Wrappers.<JdProxyIpPort>lambdaQuery();
                wrapper.gt(JdProxyIpPort::getExpirationTime, new Date());
                wrapper.isNull(JdProxyIpPort::getProvinceId);
                Integer count = jdProxyIpPortMapper.selectCount(wrapper);
                if (count < 10) {
                    this.productIpAndPort2();
//                    this.productIpAndPort1(areaIp);
                    return getOneIp(isUse, index, isAc);
                }
                int i = PreUtils.randomCommon(1, count - 1, 1)[0];
                Page<JdProxyIpPort> page = new Page<>(i, 1);
                page = jdProxyIpPortMapper.selectPage(page, wrapper);
                jdProxyIpPort = page.getRecords().get(0);
                long l = (jdProxyIpPort.getExpirationTime().getTime() - System.currentTimeMillis()) / 1000;
                redisTemplate.opsForValue().set("IP缓存池AC:" + jdProxyIpPort.getId(), jdProxyIpPort.getId() + "", l, TimeUnit.SECONDS);
            }
            if (isUse == 0) {
                Set<String> keys = redisTemplate.keys("IP缓存池AC:*");
                if (CollUtil.isEmpty(keys)) {
                    return getOneIp(1, index, isAc);
                }
                List<String> ids = keys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
                if (ids.size() < 30) {
                    return getOneIp(1, index, isAc);
                }
                int ra = PreUtils.randomCommon(1, ids.size() - 1, 1)[0];
                Integer id = Integer.valueOf(ids.get(ra));
                jdProxyIpPort = jdProxyIpPortMapper.selectById(id);
            }
//        JdProxyIpPort jdProxyIpPort = jdProxyIpPortMapper.selectOneAndNotUse();
            if (ObjectUtil.isNotNull(jdProxyIpPort)) {
                jdProxyIpPort.setIsUse(isUse);
                jdProxyIpPort.setUseTime(new Date());
                jdProxyIpPortMapper.updateById(jdProxyIpPort);
            }
            return jdProxyIpPort;
        } catch (Exception e) {
            return getOneIp(isUse, index, isAc);
        }
    }

    private JdProxyIpPort getJdProxyIpPort_falseAc(Integer isUse, Integer index, Boolean isAc) {
        Integer proxyNum = Integer.valueOf(redisTemplate.opsForValue().get("代理个数"));
        try {
            if (index >= 7) {
                return null;
            }
            index = index + 1;
            JdProxyIpPort jdProxyIpPort = null;
            if (isUse == 1) {
                LambdaQueryWrapper<JdProxyIpPort> wrapper = Wrappers.lambdaQuery();
                wrapper.gt(JdProxyIpPort::getExpirationTime, new Date())
                        .eq(JdProxyIpPort::getIsUse, 0);
                Integer count = jdProxyIpPortMapper.selectCount(wrapper);
                int i = PreUtils.randomCommon(1, count - 1, 1)[0];
                Page<JdProxyIpPort> page = new Page<>(i, 1);
                page = jdProxyIpPortMapper.selectPage(page, wrapper);
                jdProxyIpPort = page.getRecords().get(0);
                long l = (jdProxyIpPort.getExpirationTime().getTime() - System.currentTimeMillis()) / 1000;
                log.debug("检查ip缓存池是否有用有用就放入。没有用就放入");
                redisTemplate.opsForValue().set("IP缓存池:" + jdProxyIpPort.getId(), jdProxyIpPort.getId() + "", l, TimeUnit.SECONDS);
                jdProxyIpPort.setIsUse(PreConstant.ONE);
                jdProxyIpPortMapper.updateById(jdProxyIpPort);
                return jdProxyIpPort;
            }
            if (isUse == 0) {
                Set<String> keys = redisTemplate.keys("IP缓存池:*");
                if (CollUtil.isEmpty(keys)) {
                    return getOneIp(1, index, isAc);
                }
                List<String> ids = keys.stream().map(it -> it.split(":")[1]).collect(Collectors.toList());
                if (ids.size() < proxyNum) {
                    return getOneIp(1, index, isAc);
                }
                int ra = PreUtils.randomCommon(1, ids.size() - 1, 1)[0];
                Integer id = Integer.valueOf(ids.get(ra));
                jdProxyIpPort = jdProxyIpPortMapper.selectById(id);
            }
            return jdProxyIpPort;
        } catch (Exception e) {
            return getOneIp(isUse, index, isAc);
        }
    }

    private static Boolean checkIp(Proxy proxy) {
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().proxy(proxy).build();
            RequestBody requestBody = new FormBody.Builder()
                    .add("body", "{\"action\":\"to\",\"to\":\"https%3A%2F%2Fcard.m.jd.com%2F\"}")
                    .build();
            SignVoAndDto signVoAndDto = new SignVoAndDto("genToken", "{\"action\":\"to\",\"to\":\"https%3A%2F%2Fcard.m.jd.com%2F\"}");
            signVoAndDto = JdSgin.newSign(signVoAndDto);
//            String ck = "pin=jd_7ee93ffcf66197;wskey=AAJibywIAED4SCF4HuX3xEDSTEc0xwd2bmiy0MbSghwOl-Nl5RHg5ukAVBIG4Uepsscj9NziNOgQhGIkfxSe44fCc7Tz0gRl;";
            String ck = String.format("pin=%s;wskey=%s;", PreUtils.getRandomString(17), PreUtils.getRandomString(70));
            Request request = new Request.Builder()
                    .url(String.format("http://api.m.jd.com/client.action?functionId=genToken&clientVersion=9.4.4&client=android&uuid=%s&st=%s&sign=%s&sv=%s", signVoAndDto.getUuid(), signVoAndDto.getSt(),
                            signVoAndDto.getSign(), signVoAndDto.getSv()))
                    .post(requestBody)
                    .addHeader("Cookie", ck)
                    .addHeader("User-Agent", "okhttp/3.12.1")
                    .build();
            Response response = client.newCall(request).execute();
            JSONObject parseObject = JSON.parseObject(response.body().string());
            String tokenKey = parseObject.getString("tokenKey");
            if (tokenKey.length() > 20) {
                return true;
            } else if (tokenKey.length() == 3) {
                return false;
            }
        } catch (Exception e) {
            log.error("检查ip出错了");
        }
        return false;
    }

    public JdPathConfig getPath() {
        String hostAddress = PreUtils.getLocalHostLANAddress().getHostAddress();
        log.info("ip：msg:[data:{}]", hostAddress);
        JdPathConfig jdPathConfig = jdPathConfigMapper.selectOne(Wrappers.<JdPathConfig>lambdaQuery().eq(JdPathConfig::getIp, hostAddress));
        return jdPathConfig;
    }

    public void productIpAndPort3() {
        ProxyAddressProduct proxyAddressProduct = proxyAddressProductMapper.selectOne(Wrappers.<ProxyAddressProduct>lambdaQuery().eq(ProxyAddressProduct::getIsProduct, 1));
        if (proxyAddressProduct.getType() != 3) {
            return;
        }
        PreTenantContextHolder.setCurrentTenantId(1L);
        String 代理生产管理订单时间相差多少分钟 = redisTemplate.opsForValue().get("代理生产管理订单时间相差多少分钟");
        Integer integer = Integer.valueOf(代理生产管理订单时间相差多少分钟);
        Integer count1 = jdMchOrderMapper.selectCount(Wrappers.<JdMchOrder>lambdaQuery().gt(JdMchOrder::getCreateTime, DateUtil.offsetMinute(new Date(), -integer)));
        if (count1 == 0) {
            return;
        }
        Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("熊猫代理", "锁定", 12, TimeUnit.SECONDS);
        if (!ifAbsent) {
            log.info("执行6");
            return;
        }
        log.info("开始生产代理");
        String producUrl = String.format(proxyAddressProduct.getAgentAddress(), proxyAddressProduct.getNum());
        String s = HttpUtil.get(producUrl);
        log.info("生产代理成功msg:{}", s);
        if (StrUtil.isBlank(s)) {
            return;
        }
        JSONObject parseObject = JSON.parseObject(s);
        if (parseObject.getInteger("code").equals(200)) {
            log.info("生产代理成功");
            String data = parseObject.getString("data");
            List<JSONObject> jsonObjects = JSON.parseArray(data, JSONObject.class);
            if (CollUtil.isNotEmpty(jsonObjects)) {
                for (JSONObject jsonObject : jsonObjects) {
                    String ip = jsonObject.getString("ip");
                    Integer port = jsonObject.getInteger("port");
                    DateTime expire_time = DateUtil.parseDateTime(jsonObject.getString("expire_time"));
                    expire_time = DateUtil.offsetMinute(expire_time, -2);
                    JdProxyIpPort jdProxyIpPort = new JdProxyIpPort().builder().agentAddress(producUrl).ip(ip)
                            .port(port + "").createTime(new Date()).isUse(0).expirationTime(expire_time).build();
                    jdProxyIpPortMapper.insert(jdProxyIpPort);
                }
            }
        }

    }
}
