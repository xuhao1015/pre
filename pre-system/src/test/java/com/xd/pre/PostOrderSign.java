package com.xd.pre;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.common.utils.px.PreUtils;

public class PostOrderSign {
    public static void main(String[] args) {
        创建订单();
//        查询订单();

    }

    private static void 查询订单() {
        String a = "{\t\n" +
                "\t\"mch_id\":\"1\",\n" +
                "\t\"out_trade_no\":\"750\",\n" +
                "\t\"sign\":\"04e68dccc9b4e011b0ccd2ab23733542\"\n" +
                "}";
        JSONObject parseObject = JSON.parseObject(a);
        String asciiSort = PreUtils.getAsciiSort(parseObject);
        String s = asciiSort + "&sign=" + "04e68dccc9b4e011b0ccd2ab23733542";
        String encode = Base64.encode(s);
        String sign = PreUtils.getSign(encode);
        cn.hutool.json.JSONObject hutoolsJson = new cn.hutool.json.JSONObject(a);
        hutoolsJson.put("sign", sign);
        System.out.println(JSON.toJSONString(hutoolsJson));
        HttpResponse execute = HttpRequest.post("http://103.235.174.176/api/px/payFindStatusByOderId").body(hutoolsJson).execute();
        String body = execute.body();
        System.out.println(body);
    }

    private static void 创建订单() {
        String a = "{\n" +
                "\t\"amount\": \"200.00\",\n" +
                "\t\"out_trade_no\": \"2000\",\n" +
                "\t\"subject\": \"支付1000元\",\n" +
                "\t\"sign\": \"%s\",\n" +
                "\t\"client_ip\": \"192.168.2.1\",\n" +
                "\t\"mch_id\": \"1\",\n" +
                "\t\"body\": \"支付1000元\",\n" +
                "\t\"notify_url\": \"http://103.235.174.176/pre/jd/callbackTemp\",\n" +
                "\t\"pass_code\": \"8\",\n" +
                "\t\"timestamp\": \"2014-07-24 03:07:50\"\n" +
                "}";
        JSONObject parseObject = JSON.parseObject(a);
        String asciiSort = PreUtils.getAsciiSort(parseObject);
        String s = asciiSort + "&sign=" + "64f1a1ccc1da0745c52719a9d896d869";
        String encode = Base64.encode(s);
        String md5 = SecureUtil.md5(encode);
        System.out.println(md5);
        cn.hutool.json.JSONObject hutoolsJson = new cn.hutool.json.JSONObject(a);
        hutoolsJson.put("sign", md5);
        System.out.println(JSON.toJSONString(hutoolsJson));
    }

}
