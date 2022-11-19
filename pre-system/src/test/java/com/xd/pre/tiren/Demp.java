package com.xd.pre.tiren;


import okhttp3.*;

public class Demp {
    public static void main(String[] args)throws Exception {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "source=1&business_line=2&app_name=aweme&channel=dy_tiny_juyouliang_dy_and24&device_platform=android&order_id=5001571948287938115&action_id=100030&undefined=");
        Request request = new Request.Builder()
                .url("https://aweme.snssdk.com/aweme/v1/commerce/order/action/postExec/?aid=1128&channel=dy_tiny_juyouliang_dy_and24&device_platform=android")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Cookie", "sid_tt=140a336dd81551eaa30bc0e9e8d336fd; ")
                .addHeader("cache-control", "no-cache")
                .build();
        Response response = client.newCall(request).execute();
        String string = response.body().string();
        System.out.println(string);
    }
}
