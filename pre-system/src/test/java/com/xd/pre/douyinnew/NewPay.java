package com.xd.pre.douyinnew;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import okhttp3.*;

public class NewPay {
    public static void main(String[] args) throws Exception{
        String bodyData="os_api=22&origin_id=0&device_type=SM-G955N&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=351564016880114&schema=snssdk143%3A%2F%2F&app_name=aweme&version_name=17.3.0&ts=1666858666&source_type=0&cpu_support64=false&app_type=normal&appTheme=light&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&activity_id=&_rticket=1666858667405&pay_risk_info=%7B%22biometric_params%22%3A%221%22%2C%22is_jailbreak%22%3A%222%22%2C%22openudid%22%3A%22%22%2C%22order_page_style%22%3A0%2C%22checkout_id%22%3A3%2C%22ecom_payapi%22%3Atrue%2C%22ip%22%3A%22159.138.43.236%22%7D&new_source_id=0&device_platform=android&iid=3743163984904813&pay_info=%7B%22sdk_version%22%3A%22v2%22%2C%22dev_info%22%3A%7B%22reqIp%22%3A%22159.138.43.236%22%2C%22os%22%3A%22android%22%2C%22isH5%22%3Afalse%2C%22cjSdkVersion%22%3A%225.9.1%22%2C%22aid%22%3A%221128%22%2C%22ua%22%3A%22okhttp%2F3.10.0.1%22%2C%22riskUa%22%3A%22%22%2C%22lang%22%3A%22zh-Hans%22%2C%22deviceId%22%3A%222538093503847412%22%2C%22osVersion%22%3A%225.1.1%22%2C%22vendor%22%3A%22%22%2C%22model%22%3A%22%22%2C%22netType%22%3A%22%22%2C%22appVersion%22%3A%2217.3.0%22%2C%22appName%22%3A%22aweme%22%2C%22devicePlatform%22%3A%22android%22%2C%22deviceType%22%3A%22SM-G955N%22%2C%22channel%22%3A%22dy_tiny_juyouliang_dy_and24%22%2C%22openudid%22%3A%22%22%2C%22versionCode%22%3A%22170300%22%2C%22ac%22%3A%22wifi%22%2C%22brand%22%3A%22samsung%22%2C%22iid%22%3A%223743163984904813%22%2C%22bioType%22%3A%221%22%7D%2C%22bank_card_info%22%3A%7B%7D%2C%22credit_pay_info%22%3A%7B%22installment%22%3A%221%22%7D%2C%22zg_ext_param%22%3A%22%7B%5C%22credit_pay_param%5C%22%3A%7B%5C%22fee_rate_per_day%5C%22%3A%5C%22%5C%22%2C%5C%22has_credit_param%5C%22%3Afalse%2C%5C%22has_trade_time%5C%22%3Afalse%2C%5C%22installment_starting_amount%5C%22%3A0%2C%5C%22is_credit_activate%5C%22%3Afalse%2C%5C%22remaining_credit%5C%22%3A0%2C%5C%22trade_time%5C%22%3A0%7D%2C%5C%22decision_id%5C%22%3A%5C%22659356656346136_1666858661101996%5C%22%2C%5C%22jr_uid%5C%22%3A%5C%221715702027264720%5C%22%2C%5C%22merchant_info%5C%22%3A%7B%5C%22app_id%5C%22%3A%5C%22NA202208012041063016245258%5C%22%2C%5C%22ext_uid_type%5C%22%3A0%2C%5C%22jh_app_id%5C%22%3A%5C%228000104428743%5C%22%2C%5C%22jh_merchant_id%5C%22%3A%5C%22100000010442%5C%22%2C%5C%22merchant_id%5C%22%3A%5C%228020220801671981%5C%22%2C%5C%22merchant_name%5C%22%3A%5C%22%E4%B8%8A%E6%B5%B7%E6%A0%BC%E7%89%A9%E8%87%B4%E5%93%81%E7%BD%91%E7%BB%9C%E7%A7%91%E6%8A%80%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8%5C%22%2C%5C%22merchant_short_to_customer%5C%22%3A%5C%22%E6%8A%96%E9%9F%B3%E7%94%B5%E5%95%86%E5%95%86%E5%AE%B6%5C%22%7D%2C%5C%22promotion_ext%5C%22%3A%5C%22%7B%5C%5C%5C%22IsZjyFlag%5C%5C%5C%22%3A%5C%5C%5C%22true%5C%5C%5C%22%2C%5C%5C%5C%22ParamOrderId%5C%5C%5C%22%3A%5C%5C%5C%22202210271617383338715941%5C%5C%5C%22%2C%5C%5C%5C%22PromotionActivityIDs%5C%5C%5C%22%3A%5C%5C%5C%22AC220929171013900012599847%5C%5C%5C%22%7D%5C%22%2C%5C%22promotion_process%5C%22%3A%7B%5C%22create_time%5C%22%3A1666858661%2C%5C%22process_id%5C%22%3A%5C%22bce48bb5ef48f094cd847eaa1fa80fc170%5C%22%2C%5C%22process_info%5C%22%3A%5C%22%5C%22%7D%2C%5C%22qt_c_pay_url%5C%22%3A%5C%22%5C%22%2C%5C%22retain_c_pay_url%5C%22%3A%5C%22%5C%22%7D%22%2C%22voucher_no_list%22%3A%5B%5D%2C%22jh_ext_info%22%3A%22%7B%5C%22payapi_cache_id%5C%22%3A%5C%2220221027161741101983az7y2x30w8b1%5C%22%7D%22%7D&b_type=2&new_source_type=0&version_code=170300&order_id=4994029934178342467&sub_way=0&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&extra=%7B%22render_track_id%22%3A%22%22%7D&env_type=2&openudid=199d79fbbeff0e58&device_id=2538093503847412&resolution=720*1280&origin_type=9902090000&os_version=5.1.1&language=zh&device_brand=samsung&source_id=0&entrance_params=%7B%7D&aid=1128&minor_status=0&mcc_mnc=46007&pay_type=2";
        String X_SS_STUB = SecureUtil.md5(bodyData).toUpperCase();
        String url ="https://ec.snssdk.com/order/createpay?device_id=2538093503847412&aid=1128&device_platform=android&request_tag_from=h5&os_api=22&manifest_version_code=170301&app_name=aweme&version_name=17.3.0&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&iid=3743163984904813&version_code=170300&os=android&os_version=5.1.1";
        String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                X_SS_STUB, url
        );
        String signHt = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData).execute().body();
        String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
        String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, bodyData);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-SS-STUB", "7DDB6101B0C34E66077BABD4295CD92E")
                .addHeader("Cookie", "install_id=3743163984904813; ttreq=1$c937432add1ac5543b40dc8b95cb769bf024bf3a; passport_csrf_token=dc084fdfd9182b2006ac015d23d5094e; passport_csrf_token_default=dc084fdfd9182b2006ac015d23d5094e; d_ticket=5d8498d9c5c57a18f23083f8b948b45743690; multi_sids=659356656346136%3A140a336dd81551eaa30bc0e9e8d336fd; n_mh=8nysT__BxDL_VpPZTRMYKZZSN1pywPhZ9o63MSmzGLg; passport_assist_user=CkCRydX49tKRiP6NfppL8EZXqhP7I0lHXjcq-1NuFi9tetbHhO7j8WgKWcNY0u1c2_pwQmIxWsLy25zu5vuCS4y2GkgKPEawcjcdGGFhQ7XJU9Cvcme37ad7_x2LoXTiOHQl20bPqoQm-Xexq_YwQPA0X1fytaQzn-aCrETNNkRTDBDpip0NGImv1lQiAQMRQLcc; sid_guard=140a336dd81551eaa30bc0e9e8d336fd%7C1664383681%7C5183999%7CSun%2C+27-Nov-2022+16%3A48%3A00+GMT; uid_tt=1e4686eabe61b69fd57f1db3639b39b0; uid_tt_ss=1e4686eabe61b69fd57f1db3639b39b0; sid_tt=140a336dd81551eaa30bc0e9e8d336fd; sessionid=140a336dd81551eaa30bc0e9e8d336fd; sessionid_ss=140a336dd81551eaa30bc0e9e8d336fd; odin_tt=e086275c7865abfd97eb3b201259506b0e6fbeffcf7304a49eef8c29267555987501384d9d996b88d09451482cc01c6438ac0ec341265182fa97e98bc7bb4134; msToken=ONMwfi2pO1GuokpfgxN9mKWzeIHTbIzfwTv4K139Q1CuaMQZmGFSVF7elksiZcOUFpT3IAV458UTN5ze2tjq3X_4ANOSXOGfrSCrj7aa1kg=")
                .addHeader("X-Khronos", x_khronos)
                .addHeader("X-Gorgon", x_gorgon)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Host", "ec.snssdk.com")
                .addHeader("Connection", "Keep-Alive")
                .addHeader("User-Agent", "okhttp/3.10.0.1")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }
}
