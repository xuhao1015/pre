package com.xd.pre.tiren;


import com.alibaba.fastjson.JSON;
import com.xd.pre.modules.px.utils.SysUtils;
import com.xd.pre.modules.sys.domain.JdProxyIpPort;
import com.xd.pre.pcScan.Demo;
import okhttp3.OkHttpClient;

import java.net.Proxy;

public class Qu重复 {
    public static void main(String[] args) throws Exception {
        OkHttpClient okHttpClient = Demo.getOkHttpClient("27.159.190.78",12185);
        JdProxyIpPort jdProxyIpPort = SysUtils.parseOkHttpClent(okHttpClient, null);
        System.out.println(JSON.toJSONString(jdProxyIpPort));
    }
}
