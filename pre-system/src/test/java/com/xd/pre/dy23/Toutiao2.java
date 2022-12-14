package com.xd.pre.dy23;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.nosql.redis.RedisDS;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import redis.clients.jedis.Jedis;

import java.net.URLEncoder;
import java.util.Map;

@Slf4j
public class Toutiao2 {
    public static Db db = Db.use();
    public static Jedis jedis = RedisDS.create().getJedis();

    public static void main(String[] args) throws Exception {
        Integer buyPrice = 200 * 100;
        String payIp = PreUtils.getRandomIp();
        String post_tel = PreUtils.getTel();
        Entity appCk = db.use().queryOne("select * from douyin_app_ck where is_enable = 0 and file_name = '20221204_300.txt'and id = 12173 ");
        Entity devicesBd = db.use().queryOne("select * from douyin_device_iid where   is_enable = 0 and id > 19522 ");
        String uid = appCk.getStr("uid");
        String ck = PreAesUtils.decrypt解密(appCk.getStr("ck"));
        log.info("当前ck:{}", ck);
        String device_id = devicesBd.getStr("device_id");
        String iid = devicesBd.getStr("iid");
        device_id = "523809217059773";
        iid = "3672819887514510";
        ck = "passport_csrf_token=755728712b80a919a24126d40a2dd364; passport_csrf_token_default=755728712b80a919a24126d40a2dd364; n_mh=JXOM2wuD370AknLlRIFSR-17elfrrfBB9y9y-FVE_NM; d_ticket=5cf3220b574c7a9f9bf12c8582207b46e3bf5; msToken=U38DUFlwU_sQN9Xh1HgQE7JTT3AwdJE9w18i4RIor7eDF-gAVlpANxChOEuWBb9ksm4007fGMunhxA-3BWw_ffOGbQrlfTrKMVFeKobNTCQ=; odin_tt=94824f04b1138d7ed432d8413bc8dd338085e3ac7fe9d60a9feaa8c22f107c87af1465d3bc81f95129f1fc67fb8a0225213265cc7d53fa582fadfb84dca9fefd20fb8cda5cf521e0a00b570d7c2ac4e2; sid_guard=0ed6d15d88e9dcf61edf1a56e1471547%7C1670402642%7C5183999%7CSun%2C+05-Feb-2023+08%3A44%3A01+GMT; uid_tt=0ed66b850f9519672cc2fbca942c33ef; uid_tt_ss=0ed66b850f9519672cc2fbca942c33ef; sid_tt=0ed6d15d88e9dcf61edf1a56e1471547; sessionid=0ed6d15d88e9dcf61edf1a56e1471547; sessionid_ss=0ed6d15d88e9dcf61edf1a56e1471547";
        OkHttpClient client = new OkHttpClient();
        String url = "https://ken.snssdk.com/order/buyRender?b_type_new=3&sub_b_type=13&ecom_appid=7386&webcast_appid=6822&live_request_from_jsb=1&live_sdk_version=910&webcast_sdk_version=2120&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&webcast_app_id=6822&app_name=news_article&openlive_personal_recommend=1&device_platform=android&os=android&ssmix=a&_rticket=1670351635055&cdid=587713e0-2c73-45dd-aa5e-85e9cd10b401&channel=update&aid=13&version_code=910&version_name=9.1.0&manifest_version_code=9095&update_version_code=91006&ab_version=1859937%2C668779%2C5175922%2C668774%2C5175916%2C668776%2C5175917%2C662176%2C5175909%2C662099%2C5175875%2C660830%2C5107919%2C5114922%2C5175925%2C668775%2C4091914%2C4407627%2C5158114%2C5175927%2C3746951%2C5179111%2C5082917%2C5092860%2C5113788%2C5181070&ab_group=94565%2C102751&ab_feature=94563%2C102749&resolution=1080*2245&dpi=480&device_type=PGBM10&device_brand=OPPO&language=zh&os_api=31&os_version=12&ac=wifi&dq_param=0&plugin=0&isTTWebView=0&session_id=a112205b-ed5e-488c-a878-096e2fb089a6&host_abi=armeabi-v7a&tma_jssdk_version=2.53.0&rom_version=coloros__pgbm10_11_a.26&immerse_pool_type=101&iid=3672819887514510&device_id=523809217059773";
        String body = "{\"address\":null,\"platform_coupon_id\":null,\"kol_coupon_id\":null,\"auto_select_best_coupons\":true,\"customize_pay_type\":\"{\\\"checkout_id\\\":1,\\\"bio_type\\\":\\\"1\\\"}\",\"first_enter\":true,\"source_type\":\"1\",\"shape\":0,\"marketing_channel\":\"\",\"forbid_redpack\":false,\"support_redpack\":true,\"use_marketing_combo\":false,\"entrance_params\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"search_params\\\":{\\\"search_id\\\":\\\"202212070122360101501062141AADB9F0\\\",\\\"search_result_id\\\":\\\"3586218895557658254\\\"},\\\"source_method\\\":\\\"product_card\\\",\\\"source_page\\\":\\\"search_order_center\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_activity_banner\\\":0,\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"0\\\",\\\"is_with_video\\\":0,\\\"label_name\\\":\\\"\\\",\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0,\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0,\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\",\\\"commodity_id\\\":\\\"3586218895557658254\\\",\\\"commodity_type\\\":6,\\\"product_id\\\":\\\"3586218895557658254\\\"}\",\"shop_requests\":[{\"shop_id\":\"GceCTPIk\",\"product_requests\":[{\"product_id\":\"3586218895557658254\",\"sku_id\":\"1751083441416206\",\"sku_num\":1,\"origin_id\":\"99514375927_3586218895557658254\",\"origin_type\":\"3002070010\",\"new_source_type\":\"product_detail\",\"select_privilege_properties\":[]}]}]}";
        String X_SS_STUB = SecureUtil.md5("json_form=" + URLEncoder.encode(body)).toUpperCase();
        String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                X_SS_STUB, device_id, url
        );
        String signHt = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData).execute().body();
        String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
        String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
        String x_argus = JSON.parseObject(signHt).getString("x-argus");
        String x_ladon = JSON.parseObject(signHt).getString("x-ladon");
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
                .addHeader("User-Agent", "com.ss.android.article.news/9070 (Linux; U; Android 12; zh_CN; PGBM10; Build/SP1A.210812.016; Cronet/TTNetVersion:f6f1f7ad 2022-10-31 QuicVersion:22f74f01 2022-10-11)")
                .build();
        Response response = client.newCall(request).execute();
        String buyRebderData = response.body().string();
        log.info("预下单数据:{}", buyRebderData);
        BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(buyRebderData).getString("data"), BuyRenderRoot.class);
        String bodyData1 = String.format("{\"area_type\":\"169\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"},\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\",\"origin_type\":\"3002070010\",\"origin_id\":\"99514375927_3586218895557658254\",\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\",\\\"alkey\\\":\\\"1128_99514375927_0_3586218895557658254_010\\\",\\\"c_biz_combo\\\":\\\"8\\\",\\\"render_track_id\\\":\\\"%s\\\",\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true,\\\\\\\"ip\\\\\\\":\\\\\\\"159.138.32.243\\\\\\\",\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\",\"marketing_plan_id\":\"%s\",\"s_type\":\"\",\"entrance_params\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"search_params\\\":{\\\"search_id\\\":\\\"202212070122360101501062141AADB9F0\\\",\\\"search_result_id\\\":\\\"3586218895557658254\\\"},\\\"source_method\\\":\\\"product_card\\\",\\\"source_page\\\":\\\"search_order_center\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_activity_banner\\\":0,\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"0\\\",\\\"is_with_video\\\":0,\\\"label_name\\\":\\\"\\\",\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0,\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0,\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\",\\\"commodity_id\\\":\\\"3586218895557658254\\\",\\\"commodity_type\\\":6,\\\"product_id\\\":\\\"3586218895557658254\\\",\\\"extra_campaign_type\\\":\\\"\\\"}\",\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0,\"pay_type\":10,\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}},\"post_tel\":\"15828287462\",\"address_id\":\"0\",\"price_info\":{\"origin\":20000,\"freight\":0,\"coupon\":0,\"pay\":20000,\"packing_charge_amount\":0},\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\",\\\"dev_info\\\":{\\\"reqIp\\\":\\\"159.138.32.243\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.5.1\\\",\\\"aid\\\":\\\"13\\\",\\\"ua\\\":\\\"com.ss.android.article.news/9095+(Linux;+U;+Android+12;+zh_CN;+PGBM10;+Build/SP1A.210812.016;+Cronet/TTNetVersion:a911d6f2+2022-11-14+QuicVersion:585d7967+2022-11-14)\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\",\\\"deviceId\\\":\\\"523809217059773\\\",\\\"osVersion\\\":\\\"12\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"9.1.0\\\",\\\"appName\\\":\\\"news_article\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"PGBM10\\\",\\\"channel\\\":\\\"update\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"910\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"OPPO\\\",\\\"iid\\\":\\\"3672819887514510\\\",\\\"bioType\\\":\\\"1\\\"},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[],\\\"zg_ext_param\\\":\\\"{\\\\\\\"decision_id\\\\\\\":\\\\\\\"659356656346136_1670351636651120\\\\\\\",\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\",\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"%s\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\",\"render_token\":\"%s\",\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\",\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\",\"shop_stock_out_handle_infos\":null,\"shop_id\":\"GceCTPIk\",\"combo_id\":\"1751083441416206\",\"combo_num\":1,\"product_id\":\"3586218895557658254\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1,\"sku_id\":\"1751083441416206\",\"warehouse_id\":\"0\"}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false,\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"0\",\"promotion_id\":\"\",\"ecom_scene_id\":\"\",\"shop_user_id\":\"\",\"group_id\":\"\",\"privilege_tag_keys\":[],\"select_privilege_properties\":[],\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                buyRenderRoot.getRender_track_id(),
                buyRenderRoot.getTotal_price_result().getMarketing_plan_id(),
                buyRenderRoot.getPay_method().getPayapi_cache_id(),
                buyRenderRoot.getRender_token()
        );
        String url1 = "https://ec.snssdk.com/order/newcreate/vtl?can_queue=1&b_type_new=3&sub_b_type=13&ecom_appid=7386&webcast_appid=6822&live_request_from_jsb=1&live_sdk_version=910&webcast_sdk_version=2120&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&webcast_app_id=6822&app_name=news_article&openlive_personal_recommend=1&device_platform=android&os=android&ssmix=a&_rticket=1670351643899&cdid=587713e0-2c73-45dd-aa5e-85e9cd10b401&channel=update&aid=13&version_code=910&version_name=9.1.0&manifest_version_code=9095&update_version_code=91006&ab_version=1859937%2C668779%2C5175922%2C668774%2C5175916%2C668776%2C5175917%2C662176%2C5175909%2C662099%2C5175875%2C660830%2C5107919%2C5114922%2C5175925%2C668775%2C4091914%2C4407627%2C5158114%2C5175927%2C3746951%2C5179111%2C5082917%2C5092860%2C5113788%2C5181070&ab_group=94565%2C102751&ab_feature=94563%2C102749&resolution=1080*2245&dpi=480&device_type=PGBM10&device_brand=OPPO&language=zh&os_api=31&os_version=12&ac=wifi&dq_param=0&plugin=0&isTTWebView=0&session_id=a112205b-ed5e-488c-a878-096e2fb089a6&host_abi=armeabi-v7a&tma_jssdk_version=2.53.0&rom_version=coloros__pgbm10_11_a.26&immerse_pool_type=101&iid=3672819887514510&device_id=523809217059773";
        String X_SS_STUB1 = SecureUtil.md5("json_form=" + URLEncoder.encode(bodyData1)).toUpperCase();
        String signData1 = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                X_SS_STUB1, url1
        );
        String signHt1 = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData1).execute().body();
        log.info("msg:{}", signHt1);
        String x_gorgon1 = JSON.parseObject(signHt1).getString("x-gorgon");
        String x_khronos1 = JSON.parseObject(signHt1).getString("x-khronos");
        String x_argus1 = JSON.parseObject(signHt1).getString("x-argus");
        String x_ladon1 = JSON.parseObject(signHt1).getString("x-ladon");

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
                .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                .addHeader("X-Gorgon", x_gorgon1)
                .addHeader("X-Khronos", x_khronos1)
                .addHeader("Authorization", "Bearer act.3.-lBtn50OulxOWy1HljnuqW2OJMU2EtYM3O9N8ot0cCxtIzFLdZJYv_n6_UlmjJvx6XtmJ_pWlmpHafD3HkkvJpCkaeiFOF-QISDASAJy4pvTj8HFgGUJsKzpkd-a3g8Bg_AZND8x6BaZ0TTrRJkrUsqqiRr5fJ9TnZWrBQ==")
                .build();
        Response response1 = client.newCall(request1).execute();
        String bodyRes1 = response1.body().string();
        response1.close();
        log.info("下单结果:{}", bodyRes1);

    }
}
