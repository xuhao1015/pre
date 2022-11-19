package com.xd.pre.tiren;

import com.xd.pre.common.aes.PreAesUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class FindOhterOrder {
    public static void main(String[] args) {
        try {
                String original_trade_no = "5000186232926541196";
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?aid=45465&order_id=" + original_trade_no)
                        .get()
                        .addHeader("Cookie", PreAesUtils.decrypt解密("sid_tt=d70b9d75fa4bafd4cc8d0e8fcdd07369;sessionid=d70b9d75fa4bafd4cc8d0e8fcdd07369;sessionid_ss=d70b9d75fa4bafd4cc8d0e8fcdd07369;"))
                        .addHeader("X-Khronos", "1665697911")
                        .addHeader("X-Gorgon", "8404d4860000775655c5b8f6315f8a608a802f3a78e4891a08cc")
                        .addHeader("User-Agent", "okhttp/3.10.0.1")
                        .addHeader("cache-control", "no-cache")
                        .build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                System.out.println(body);

        }catch (Exception e){

        }

    }
}
