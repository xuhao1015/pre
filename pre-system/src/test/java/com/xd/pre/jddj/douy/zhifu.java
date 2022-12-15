package com.xd.pre.jddj.douy;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.net.URLEncoder;

public class zhifu {
    public static void main(String[] args) {
        String bodyRes = "{\"st\":0,\"msg\":\"\",\"data\":{\"order_id\":\"5012162349672520758\",\"create_time\":\"1671084804\",\"total_amount\":\"1000\",\"post_amount\":\"0\",\"sub_way\":\"0\",\"data\":{\"sdk_info\":{\"appid\":\"wx50d801314d9eb858\",\"noncestr\":\"U9aTTXOllncN9hQ5yT5x3ivKtTA8OhPF\",\"package\":\"Sign=WXPay\",\"partnerid\":\"1588790401\",\"prepayid\":\"up_wx151413250358320f834bfdce14ab320000\",\"sign\":\"J49om7JiPYP9eGbi49Wh3o+XvxCrxmU0eNkONaXjSGlUlFuHxG0bK3QckDToRw3yaesw/XymMrx1d+VATzxK5UzjpPXvdAvjSjh04gpEc98AetInE9iPbIkW3Pf925E7sdOAL1KYwyxlmDoQMtfhvCRrI+JJZJj1POjhcPGkZc/l6tqmAhfsr4ECG4AUcsLtO21pMyNfl+Qtu5BXlRc+YCy3bDXvW8DTFpg5NN9fkWHWlbtOJ7Dv4yV6a4MxpHIAnMcYdDH3gcCSoj+3zb4uNMF8aOJhNxMaFrI4J94FaP+dUeW2vNf8yQtekY9sHkz5f+E/xFatJGb78v6j3GEdpg==\",\"timestamp\":\"1671084805\"},\"trade_info\":{\"tt_sign\":\"L96vy1aXEWHBKFuCa1/+EQR2c6OXiYhOTQthpov23GQvcfHw4fDdUeXjK6nza8u+Y2CCoqYvr2RE1yfQ1wlueBte+aCXgsuPc9A7HheaTZRUjCP0xeEipoywvjMC1lg/5vnQRx3/vgHaTbwqFhsT3M5zfyoAPqXJvkmpiPEpqCc=\",\"tt_sign_type\":\"RSA\",\"way\":\"1\"}},\"message\":\"success\",\"phase_order_id\":\"\",\"extra\":\"\"}}\n";
        String sdk_info = JSON.parseObject(JSON.parseObject(JSON.parseObject(bodyRes).getString("data")).getString("data")).getString("sdk_info");
        JSONObject parseObject = JSON.parseObject(sdk_info);
        String weixinPay = String.format("weixin://app/%s/pay/?timeStamp=%s&partnerId=1588790401&prepayId=%s&nonceStr=%s&sign=%s",
                parseObject.getString("appid"), parseObject.getString("timestamp")
                , parseObject.getString("prepayid"), parseObject.getString("noncestr"), URLEncoder.encode(parseObject.getString("sign")) + "&package=Sign%3dWXPay");
        System.out.println(weixinPay);
    }
}
