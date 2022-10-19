package com.xd.pre;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.common.utils.px.PreUtils;

public class Test {
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
        String a = "{\"amount\":\"100.00\",\"body\":\"goods\",\"client_ip\":\"68.178.160.76\",\"mch_id\":\"1\",\"notify_url\":\"http://68.178.160.76/api/notify/notify/channel/DouyinPay\",\"out_trade_no\":\"2210191801121498\",\"pass_code\":\"8\",\"sign\":\"ebadce0d8b13cb33c6d5ec5f20735300\",\"subject\":\"goods\",\"timestamp\":\"2022-10-19 18:01:12\"}";
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
