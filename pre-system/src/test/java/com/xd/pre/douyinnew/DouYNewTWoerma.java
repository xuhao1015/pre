package com.xd.pre.douyinnew;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.jddj.douy.Douyin3;
import com.xd.pre.modules.px.douyin.buyRender.BuyRenderParamDto;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import com.xd.pre.modules.px.douyin.submit.SubmitUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.net.URLEncoder;
import java.util.Map;

@Slf4j
public class DouYNewTWoerma {
    public static void main(String[] args) throws Exception {

  /*    String device_id = "3496879052572183";
        String iid = "1983951053784861";
        String ck = "sid_tt=140a336dd81551eaa30bc0e9e8d336fd;";//我的账号
        */
/*        String device_id = "device_id_str=395471702731629";
        String iid = "install_id_str=3936683709967309";
        String ck = "sid_tt=e14aeb9bf964c67040d69e62fe6082b3;";*/


/*        String device_id = "device_id_str=4288526263274574";
        String iid = "install_id_str=1385815566978734";
        String ck = "sid_tt=a72737227f6a2f24471827aa74924cc4;";*/

        Integer payType = 2;
        String payIp = "159.138.54.221";
/*
        String device_id = "device_id_str=729717026068446";
        String iid = "install_id_str=770090229574599";
        String ck = "sid_tt=27485c2d482d15e1812bfcfaefdb5fbd;";*/
/*        String device_id = "device_id_str=2335794807449870";
        String iid = "install_id_str=1350632389747998";
        String ck = "sid_tt=94826d4ba343e649910a64b4f8168ed5;";*/
/*        String device_id = "device_id_str=3817920068335960";
        String iid = "install_id_str=3215404117004782";
        String ck = "sid_tt=641b46a1f5f66bb7c23ee252d9787805;";*/
/*        String device_id = "device_id_str=4499633908752878";
        String iid = "install_id_str=2634862189901805";
        String ck = "sid_tt=d337e16db3483f51fb656d94542b2f9c;";*/
/*        String device_id = "device_id_str=910828102761496";
        String iid = "install_id_str=858051546974221";
        String ck = "sid_tt=955494eb3012cc4edbfd8f499781277b;";*/
/*        String device_id = "device_id_str=2212649920104615";
        String iid = "install_id_str=3690393549678381";
        String ck = "sid_tt=a49fc00a89ad7b3871d3b2244b1cc4c8;";*/
        String device_id = "device_id_str=699724647905134";
        String iid = "install_id_str=1508965206724398";
        String ck = "sid_tt=80d2134c60441b05c98e74ff1ad06edc;";

        if (device_id.contains("device_id_str=")) {
            device_id = device_id.replace("device_id_str=", "");
        }
        if (iid.contains("install_id_str=")) {
            iid = iid.replace("install_id_str=", "");
        }
/*        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3556357046087622442").sku_id("1736502463777799").author_id("4051040200033531")
                .ecom_scene_id("1041").shop_id("GceCTPIk").origin_id("4051040200033531_3556357046087622442").origin_type("3002070010")
                .new_source_type("product_detail").build();*/
/*        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3561751789252519688").sku_id("1739136614382624").author_id("4051040200033531")
                .ecom_scene_id("1003").origin_id("4051040200033531_3561751789252519688").origin_type("3002002002").new_source_type("product_detail").build();*/
        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3580988989001303640").sku_id("1749558839368723").author_id("96373965278")
                .ecom_scene_id("1041").origin_id("96373965278_3580988989001303640").origin_type("3002070010").shop_id("DiSLOxMc").new_source_type("product_detail").build();
        System.err.println(JSON.toJSONString(buyRenderParamDto));
/*     BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3574327743640429367").sku_id("1745277214000191").author_id("4051040200033531")
                .ecom_scene_id("").origin_id("4051040200033531_3574327743640429367").origin_type("3002002002").new_source_type("product_detail").build();
        System.err.println(JSON.toJSONString(buyRenderParamDto));*/
//        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3574327743640429367").sku_id("1745277214000191").author_id("4051040200033531")
//                .ecom_scene_id("").origin_id("4051040200033531_3574327743640429367").origin_type("3002002002").new_source_type("product_detail").build();
//        System.err.println(JSON.toJSONString(buyRenderParamDto));
        String body = SubmitUtils.buildBuyRenderParamData(buyRenderParamDto);
        Map<String, String> ipAndPort = Douyin3.getIpAndPort();
        OkHttpClient client = null;
//       client =  Demo.getOkHttpClient(ipAndPort.get("ip"), Integer.valueOf(ipAndPort.get("port")));
        client = new OkHttpClient().newBuilder().build();

//        String body = "{\"address\":null,\"platform_coupon_id\":null,\"kol_coupon_id\":null,\"auto_select_best_coupons\":true,\"customize_pay_type\":\"{\\\"checkout_id\\\":1,\\\"bio_type\\\":\\\"1\\\"}\",\"first_enter\":true,\"source_type\":\"1\",\"shape\":0,\"marketing_channel\":\"\",\"forbid_redpack\":false,\"support_redpack\":true,\"use_marketing_combo\":false,\"entrance_params\":\"{\\\"order_status\\\":3,\\\"previous_page\\\":\\\"order_list_page\\\",\\\"carrier_source\\\":\\\"order_detail\\\",\\\"ecom_scene_id\\\":\\\"1041\\\",\\\"room_id\\\":\\\"\\\",\\\"promotion_id\\\":\\\"\\\",\\\"author_id\\\":\\\"\\\",\\\"group_id\\\":\\\"\\\",\\\"anchor_id\\\":\\\"4051040200033531\\\",\\\"source_method\\\":\\\"open_url\\\",\\\"ecom_group_type\\\":\\\"video\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"warm_up_status\\\":\\\"0\\\",\\\"coupon_id\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"label_name\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\"}\",\"shop_requests\":[{\"shop_id\":\"GceCTPIk\",\"product_requests\":[{\"product_id\":\"3556357046087622442\",\"sku_id\":\"1736502463777799\",\"sku_num\":1,\"author_id\":\"4051040200033531\",\"ecom_scene_id\":\"1041\",\"origin_id\":\"4051040200033531_3556357046087622442\",\"origin_type\":\"3002070010\",\"new_source_type\":\"product_detail\",\"select_privilege_properties\":[]}]}]}";
        String url = "https://ken.snssdk.com/order/buyRender?b_type_new=2&request_tag_from=lynx&os_api=25&device_type=SM-G973N&ssmix=a&manifest_version_code=169&dpi=240&is_guest_mode=0&uuid=354730528934825&app_name=aweme&version_name=17.3.0&ts=1664384063&cpu_support64=false&app_type=normal&appTheme=dark&ac=wifi&host_abi=arm64-v8a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1664384064117&device_platform=android&iid=" + iid + "&version_code=170300&cdid=78d30492-1201-49ea-b86a-1246a704711d&os=android&is_android_pad=0&openudid=199d79fbbeff0e58&device_id=" + device_id + "&resolution=720%2A1280&os_version=5.1.1&language=zh&device_brand=Xiaomi&aid=1128&minor_status=0&mcc_mnc=46011";
        String X_SS_STUB = SecureUtil.md5("json_form=" + URLEncoder.encode(body)).toUpperCase();
        String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                X_SS_STUB, url
        );
        String signHt = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData).execute().body();
        String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
        String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
        RequestBody requestBody = new FormBody.Builder()
                .add("json_form", body)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("X-SS-STUB", X_SS_STUB)
                .addHeader("Cookie", ck)
                .addHeader("X-Gorgon", x_gorgon)
                .addHeader("X-Khronos", x_khronos)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = client.newCall(request).execute();
        String resBody = response.body().string();
        log.info("预下单数据msg:{}", resBody);
        response.close();
        if (false) {
            return;
        }
        String tel = PreUtils.getTel();
        BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(resBody).getString("data"), BuyRenderRoot.class);
        String url1 = "https://ec.snssdk.com/order/newcreate/vtl?can_queue=1&b_type_new=2&request_tag_from=lynx&os_api=5&device_type=ELE-AL00&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=354730528931234&app_name=aweme&version_name=17.3.0&ts=1664384138&cpu_support64=false&app_type=normal&appTheme=dark&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&device_platform=android&iid=" + iid + "&version_code=170300&cdid=78d30492-1201-49ea-b86a-1246a704711d&os=android&is_android_pad=0&openudid=27b54460b6dbb870&device_id=" + device_id + "&resolution=720*1280&os_version=7.1.1&language=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc=46007";
        String bodyData1 = String.format("{\"area_type\":\"169\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"},\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":2,\"env_type\":\"2\",\"activity_id\":\"\",\"origin_type\":\"3002070010\",\"origin_id\":\"96373965278_3580988989001303640\",\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\",\\\"alkey\\\":\\\"1128_96373965278_0_3580988989001303640_010\\\",\\\"c_biz_combo\\\":\\\"8\\\",\\\"render_track_id\\\":\\\"20221115195859010208017154151FCBD8\\\",\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true,\\\\\\\"ip\\\\\\\":\\\\\\\"159.138.54.221\\\\\\\",\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\",\"marketing_plan_id\":\"%s\",\"s_type\":\"\",\"entrance_params\":\"{\\\"order_status\\\":4,\\\"previous_page\\\":\\\"order_list_page\\\",\\\"carrier_source\\\":\\\"order_detail\\\",\\\"ecom_scene_id\\\":\\\"1041\\\",\\\"room_id\\\":\\\"\\\",\\\"promotion_id\\\":\\\"\\\",\\\"author_id\\\":\\\"\\\",\\\"group_id\\\":\\\"\\\",\\\"anchor_id\\\":\\\"96373965278\\\",\\\"source_method\\\":\\\"open_url\\\",\\\"ecom_group_type\\\":\\\"video\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"discount_type\\\":\\\"{130:7164954765662355719,100:7166049817193021730}\\\",\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":1,\\\"coupon_id\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"label_name\\\":\\\"\\\",\\\"with_sku\\\":\\\"1\\\",\\\"is_replay\\\":\\\"0\\\",\\\"label_name_live\\\":\\\"\\\",\\\"is_with_video\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"new_source_type\\\":\\\"product_detail\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"extra_campaign_type\\\":\\\"\\\"}\",\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0,\"pay_type\":2,\"post_addr\":null,\"post_receiver\":null,\"post_tel\":null,\"address_id\":\"0\",\"price_info\":{\"origin\":10000,\"freight\":0,\"coupon\":0,\"pay\":10000,\"packing_charge_amount\":0},\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\",\\\"dev_info\\\":{\\\"reqIp\\\":\\\"159.138.54.221\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"5.9.1\\\",\\\"aid\\\":\\\"1128\\\",\\\"ua\\\":\\\"okhttp/3.10.0.1\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\",\\\"deviceId\\\":\\\"2538093503847412\\\",\\\"osVersion\\\":\\\"5.1.1\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"17.3.0\\\",\\\"appName\\\":\\\"aweme\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"SM-G955N\\\",\\\"channel\\\":\\\"dy_tiny_juyouliang_dy_and24\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"170300\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"samsung\\\",\\\"iid\\\":\\\"3743163984904813\\\",\\\"bioType\\\":\\\"1\\\"},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[],\\\"zg_ext_param\\\":\\\"{\\\\\\\"activity_id\\\\\\\":\\\\\\\"AC221111211427902696954628\\\\\\\",\\\\\\\"credit_pay_param\\\\\\\":{\\\\\\\"fee_rate_per_day\\\\\\\":\\\\\\\"0.0267\\\\\\\",\\\\\\\"has_credit_param\\\\\\\":true,\\\\\\\"has_trade_time\\\\\\\":false,\\\\\\\"installment_starting_amount\\\\\\\":1000,\\\\\\\"is_credit_activate\\\\\\\":false,\\\\\\\"remaining_credit\\\\\\\":0,\\\\\\\"trade_time\\\\\\\":0},\\\\\\\"decision_id\\\\\\\":\\\\\\\"659356656346136_1668513539277436\\\\\\\",\\\\\\\"jr_uid\\\\\\\":\\\\\\\"1715702027264720\\\\\\\",\\\\\\\"merchant_info\\\\\\\":{\\\\\\\"app_id\\\\\\\":\\\\\\\"NA202208012041063016245258\\\\\\\",\\\\\\\"ext_uid_type\\\\\\\":0,\\\\\\\"jh_app_id\\\\\\\":\\\\\\\"8000104428743\\\\\\\",\\\\\\\"jh_merchant_id\\\\\\\":\\\\\\\"100000010442\\\\\\\",\\\\\\\"merchant_id\\\\\\\":\\\\\\\"8020220801671981\\\\\\\",\\\\\\\"merchant_name\\\\\\\":\\\\\\\"上海格物致品网络科技有限公司\\\\\\\",\\\\\\\"merchant_short_to_customer\\\\\\\":\\\\\\\"抖音电商商家\\\\\\\"},\\\\\\\"promotion_ext\\\\\\\":\\\\\\\"{\\\\\\\\\\\\\\\"IsZjyFlag\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"true\\\\\\\\\\\\\\\",\\\\\\\\\\\\\\\"ParamOrderId\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"202211151958564005675807\\\\\\\\\\\\\\\",\\\\\\\\\\\\\\\"PromotionActivityIDs\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"AC221114142041904185932574\\\\\\\\\\\\\\\",\\\\\\\\\\\\\\\"sub_order_info_list\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"\\\\\\\\\\\\\\\"}\\\\\\\",\\\\\\\"promotion_process\\\\\\\":{\\\\\\\"create_time\\\\\\\":1668513539,\\\\\\\"process_id\\\\\\\":\\\\\\\"bcf04ee4de757ff4291d9e126a2b5d515d\\\\\\\",\\\\\\\"process_info\\\\\\\":\\\\\\\"\\\\\\\"},\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\",\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"20221115195859277424a0zb71cy2x23\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\",\"render_token\":\"%s\",\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\",\"pay_amount_composition\":[],\"user_account\":{\"account_list\":[{\"account_info_type\":\"Mobile\",\"account_info_name\":\"手机号\",\"account_info_value\":\"13568502833\",\"extra\":\"{\\\"template_id\\\":\\\"1\\\",\\\"type_id\\\":\\\"1\\\",\\\"account_id\\\":\\\"1\\\",\\\"type_name\\\":\\\"手机号\\\"}\"}]},\"queue_count\":0,\"store_id\":\"\",\"shop_stock_out_handle_infos\":null,\"shop_id\":\"DiSLOxMc\",\"combo_id\":\"1749558839368723\",\"combo_num\":1,\"product_id\":\"3580988989001303640\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_num\":1,\"sku_id\":\"1749558839368723\",\"warehouse_id\":\"0\",\"stock_type\":1}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false,\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"96373965278\",\"content_id\":\"0\",\"promotion_id\":\"\",\"ecom_scene_id\":\"1041\",\"shop_user_id\":\"\",\"group_id\":\"\",\"privilege_tag_keys\":[],\"select_privilege_properties\":[],\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                buyRenderRoot.getTotal_price_result().getMarketing_plan_id(),
                buyRenderRoot.getRender_token()
                );
        System.out.println(bodyData1);
        String X_SS_STUB1 = SecureUtil.md5("json_form=" + URLEncoder.encode(bodyData1)).toUpperCase();
        String signData1 = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                X_SS_STUB1, url1
        );
        String signHt1 = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData1).execute().body();
        log.info("msg:{}", signHt1);
        String x_gorgon1 = JSON.parseObject(signHt1).getString("x-gorgon");
        String x_khronos1 = JSON.parseObject(signHt1).getString("x-khronos");
        String tarceid1 = JSON.parseObject(signHt1).getString("tarceid");
        RequestBody requestBody1 = new FormBody.Builder()
                .add("json_form", bodyData1)
                .build();
        Map<String, String> headers = PreUtils.buildIpMap(payIp);
        Request.Builder builder = new Request.Builder();
        for (String s : headers.keySet()) {
            builder.header(s, headers.get(s));
        }
        /**
         * x-tt-dt: AAA47HUS7TM7WXDTGING7ABL3JWMMLVWJGFXG437STXOQQ4UM4RTGZNRO7TKGDPQAANCMJCT2QUF7HMBCPQVVTFZG6AHNSP6W5ESC34Y5HE264TSZHQKCR6B2DVMQQSDIPFE4KCXLGCZVMGSN7GVJII
         * activity_now_client: 1665499105735
         * passport-sdk-version: 20353
         * X-Tt-Token: 00140a336dd81551eaa30bc0e9e8d336fd033efe6990f814c672b8aad9af6fff1d434b54377df5ec09628b8fb650416eb5a93ebb308a134f4d571c60a49d83d5394af0cdfdca3d70e20c168da5b9158f11e6ebc4fe282449dcf3ca1ac6165a643a9af-1.0.1
         * sdk-version: 2
         * X-SS-REQ-TICKET: 1665499105867
         * x-bd-client-key: 1fea6750bd5480b0f9c7e26e758639cbd4f6513a35cc7ba06f97e0f79bac532c84609486196db2930052c33d23c14e2f7ff21463cb983c77898fe100b34a3cd8
         * x-bd-kmsv: 1
         */
        Request request1 = builder.url(url1)
                .post(requestBody1)
                .addHeader("Cookie", ck)
                .addHeader("X-SS-STUB", X_SS_STUB1)
                .addHeader("x-tt-trace-id", tarceid1)
                .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                .addHeader("X-Gorgon", x_gorgon1)
                .addHeader("X-Khronos", x_khronos1)
                .build();
        Response response1 = client.newCall(request1).execute();
        String bodyRes1 = response1.body().string();
        response1.close();
        log.info("msg:{}", bodyRes1);
    }
}
