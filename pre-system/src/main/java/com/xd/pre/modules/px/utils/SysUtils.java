package com.xd.pre.modules.px.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xd.pre.modules.sys.domain.JdProxyIpPort;
import com.xd.pre.modules.sys.mapper.JdProxyIpPortMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

@Slf4j
public class SysUtils {
    public static JdProxyIpPort parseOkHttpClent(OkHttpClient client, JdProxyIpPortMapper jdProxyIpPortMapper) {
        try {
            if (ObjectUtil.isNull(client.proxy())) {
                return null;
            }
            String s = client.proxy().toString();
            String[] split = s.split(":");
            String[] s0 = split[0].split("/");
            String ip = s0[1].trim();
            String port = split[1].trim();
            if (ObjectUtil.isNull(jdProxyIpPortMapper)) {
                log.debug("当前代理为空,不需要查询数据");
                return JdProxyIpPort.builder().ip(ip).port(port).build();
            }
            JdProxyIpPort jdProxyIpPortDb = jdProxyIpPortMapper.selectOne(Wrappers.<JdProxyIpPort>lambdaQuery().eq(JdProxyIpPort::getIp, ip).eq(JdProxyIpPort::getPort, port));
            if (ObjectUtil.isNull(jdProxyIpPortDb)) {
                return JdProxyIpPort.builder().ip(ip).port(port).build();
            }
            return jdProxyIpPortDb;
        } catch (Exception e) {
            log.error("解析代理出错了msg:{}", e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {

    }

}
