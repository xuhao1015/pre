package com.xd.pre.douyinnew;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.px.douyin.pay.PayDto;
import com.xd.pre.modules.px.douyin.pay.PayRiskInfoAndPayInfoUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.net.URLEncoder;
import java.util.Map;

@Slf4j
public class DouYinPayByOrderId {

    public static void main(String[] args) {
        try {
            String ck = "d_ticket=06ce6a40e1e23922c61139ced34be465a4ff1; odin_tt=a69dfbe8e3d3062383ee3792ca7e597020b3ef4208cd407aa45d68e53340d373869e3d46638ba613a3b13b6679184ca3152340bf337a3ea23247a447458b8b184418bca7a1ae6bc792e38a8505f82756; odin_tt=b06f0307eb0ba7435f89bbeb6fdc485941619cf74c3f64e539e0b664a559285b4a479867c2f37ef9e904ff6fb502cdceebfb1d2fdafafb69814100009fa6054b8a90eac9c91e1fa3cef9ee1813f1ea44; n_mh=ywIx1LEQKciLqNkbkQhDVJ6osOoS7s3NxqgPl--glYA; sid_guard=6cd760481c7b4891545fe8538da56d05%7C1665393800%7C5183999%7CFri%2C+09-Dec-2022+09%3A23%3A19+GMT; uid_tt=a23d39d0267c5cfb870fb1aea7ca0998; uid_tt_ss=a23d39d0267c5cfb870fb1aea7ca0998; sid_tt=6cd760481c7b4891545fe8538da56d05; sessionid=6cd760481c7b4891545fe8538da56d05; sessionid_ss=6cd760481c7b4891545fe8538da56d05; reg-store-region=;";
            PayDto payDto = PayDto.builder().ck(ck).device_id("4024641661181144").iid("2353383987492685").pay_type("2")
                    .orderId("4994580643473702263").userIp("183.221.16.53").build();
            String bodyData = PayRiskInfoAndPayInfoUtils.buildPayForm(payDto);
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            // String bodyData = "app_name=aweme&channel=dy_tiny_juyouliang_dy_and24&device_platform=android&iid=3743163984904813&order_id=4983651837194409539&os=android&device_id=2538093503847412&aid=1128&pay_type=1";
//            String url = "https://ec.snssdk.com/order/createpay?device_id=2538093503847412&aid=1128&device_platform=android&device_type=SM-G955N&request_tag_from=h5&app_name=aweme&version_name=17.3.0&app_type=normal&channel=dy_tiny_juyouliang_dy_and24&iid=3743163984904813&version_code=170300&os=android&os_version=5.1.1";
            String url = PayRiskInfoAndPayInfoUtils.buidPayUrl(payDto);
            String X_SS_STUB = SecureUtil.md5(bodyData).toUpperCase();
            String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    X_SS_STUB, url
            );
            String signHt = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData).execute().body();
            String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
            String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
            RequestBody body = RequestBody.create(mediaType, bodyData);
            Request.Builder builder = new Request.Builder();
            Map<String, String> header = PreUtils.buildIpMap("223.104.24.246");
            for (String s : header.keySet()) {
                builder.header(s, header.get(s));
            }
            Request request = builder.url(url)
                    .post(body)
                    .addHeader("X-SS-STUB", X_SS_STUB)
                    .addHeader("Cookie", ck)
                    .addHeader("X-Gorgon", x_gorgon)
                    .addHeader("X-Khronos", x_khronos)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            Response response = client.newCall(request).execute();
            String payData = response.body().string();
            log.info("支付消息返回数据msg:{}", payData);
            String payUrl = JSON.parseObject(JSON.parseObject(JSON.parseObject(JSON.parseObject(payData).getString("data")).getString("data"))
                    .getString("sdk_info")).getString("url");
            System.out.println(payUrl);
            response.close();
            String payReUrl = "https://ds.alipay.com/?from=mobilecodec&scheme="
                    + URLEncoder.encode("alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=") + payUrl;
//            System.err.println(payReUrl);
            //alipays://platformapi/startapp?appId=20000067&url=http%3A%2F%2F134.122.134.69%3A8082%2Frecharge%2Fzfb%3Forder_id%3DSP2210012316069040391319127864
            payReUrl = String.format("alipays://platformapi/startapp?appId=20000067&url=%s", URLEncoder.encode("http://auc2a9.natappfree.cc/1.html"));
            System.err.println(payReUrl);

        } catch (Exception e) {
            log.error("支付报错msg:{}", e.getMessage());
        }
    }
}
