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
public class DouYNewJinritout {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100; i++) {
            Integer payType = 2;
            String payIp = "189.222.12.272";
            String device_id = "device_id_str=1931189210327885";
            String iid = "install_id_str=2159887630741838";
//            String ck = "install_id=1456200346246680; ttreq=1$3acac8344d7f24cba28b45bd1ddc34760eb00437; passport_csrf_token=db3f8f82ee72f0df428f645a20f0a028; passport_csrf_token_default=db3f8f82ee72f0df428f645a20f0a028; d_ticket=f5818bdf926247223211b4d51df09898e3bf5; n_mh=8nysT__BxDL_VpPZTRMYKZZSN1pywPhZ9o63MSmzGLg; sid_guard=e98c2540d634b3cdd74ab4291b9cb777%7C1669478776%7C5184000%7CWed%2C+25-Jan-2023+16%3A06%3A16+GMT; uid_tt=7115aa552cf0142ea6b0b55c88a4fccd; uid_tt_ss=7115aa552cf0142ea6b0b55c88a4fccd; sid_tt=e98c2540d634b3cdd74ab4291b9cb777; sessionid=e98c2540d634b3cdd74ab4291b9cb777; sessionid_ss=e98c2540d634b3cdd74ab4291b9cb777; msToken=rQqW-8h2GqDyp04uFeMr5-YxsYjod1vcZV3Kj6sd2vLKitWIm5nivLPrJjEZaLjjru2Eo22OPio-bsfp9jT4XDelNDZ3Hq1vUa85xj4bFAg=; odin_tt=c2afc3dbfbf2ba1a0fc127c2607d5475fc6088dff214c5d0a9c6b3e9df861f954913dcf3db3f0799b8603fe2b75b9a4894c73fa0587eae5af1928284a37e2b9f";
            String ck = "sid_tt=92e131d47528c86440b6beac280e4be8;";
            if (device_id.contains("device_id_str=")) {
                device_id = device_id.replace("device_id_str=", "");
            }
            if (iid.contains("install_id_str=")) {
                iid = iid.replace("install_id_str=", "");
            }

            BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3578244605646345985").sku_id("1747189749393459").author_id("")
                    .ecom_scene_id("1041").origin_id("99514375927_3578244605646345985").origin_type("3002070010").shop_id("GceCTPIk").new_source_type("product_detail").build();
            System.err.println(JSON.toJSONString(buyRenderParamDto));
            String body = SubmitUtils.buildBuyRenderParamData(buyRenderParamDto);
            Map<String, String> ipAndPort = Douyin3.getIpAndPort();
            OkHttpClient client = null;
            client = new OkHttpClient().newBuilder().build();
            String url = "https://ken.snssdk.com/order/buyRender?b_type_new=3&sub_b_type=13&ecom_appid=7386&webcast_appid=6822&live_request_from_jsb=1&live_sdk_version=907&webcast_sdk_version=2110&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&webcast_app_id=6822&app_name=news_article&openlive_personal_recommend=1&device_platform=android&os=android&ssmix=a&_rticket=1669478815085&cdid=587713e0-2c73-45dd-aa5e-85e9cd10b401&channel=oppo_13_64&aid=13&version_code=907&version_name=9.0.7&manifest_version_code=9070&update_version_code=90711&ab_version=668774%2C5008580%2C662176%2C5008573%2C660830%2C5008589%2C5107938%2C5115003%2C668775%2C4304847%2C5008591%2C5059449%2C5091026%2C668779%2C5008586%2C1859936%2C662099%2C5008539%2C668776%2C5008581%2C3746951%2C4786300%2C5107696%2C5092860&ab_group=94565%2C102751&ab_feature=94563%2C102749&resolution=1080*2245&dpi=480&device_type=PGBM10&device_brand=OPPO&language=zh&os_api=31&os_version=12&ac=wifi&dq_param=0&plugin=0&client_vid=4977983%2C4539074%2C3194525%2C3383553%2C2827921%2C5019683&isTTWebView=0&session_id=379890cb-b6b5-425f-b3d6-ff4f7c753614&host_abi=arm64-v8a&tma_jssdk_version=2.53.0&rom_version=coloros__pgbm10_11_a.26&immerse_pool_type=101&iid="+iid+"&device_id="+device_id+"";
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
            BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(resBody).getString("data"), BuyRenderRoot.class);
            String url1 = "https://ec.snssdk.com/order/newcreate/vtl?can_queue=1&b_type_new=3&sub_b_type=13&ecom_appid=7386&webcast_appid=6822&live_request_from_jsb=1&live_sdk_version=907&webcast_sdk_version=2110&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&webcast_app_id=6822&app_name=news_article&openlive_personal_recommend=1&device_platform=android&os=android&ssmix=a&_rticket=1669478827851&cdid=587713e0-2c73-45dd-aa5e-85e9cd10b401&channel=oppo_13_64&aid=13&version_code=907&version_name=9.0.7&manifest_version_code=9070&update_version_code=90711&ab_version=668774%2C5008580%2C662176%2C5008573%2C660830%2C5008589%2C5107938%2C5115003%2C668775%2C4304847%2C5008591%2C5059449%2C5091026%2C668779%2C5008586%2C1859936%2C662099%2C5008539%2C668776%2C5008581%2C3746951%2C4786300%2C5107696%2C5092860&ab_group=94565%2C102751&ab_feature=94563%2C102749&resolution=1080*2245&dpi=480&device_type=PGBM10&device_brand=OPPO&language=zh&os_api=31&os_version=12&ac=wifi&dq_param=0&plugin=0&client_vid=4977983%2C4539074%2C3194525%2C3383553%2C2827921%2C5019683&isTTWebView=0&session_id=379890cb-b6b5-425f-b3d6-ff4f7c753614&host_abi=arm64-v8a&tma_jssdk_version=2.53.0&rom_version=coloros__pgbm10_11_a.26&immerse_pool_type=101&iid="+iid+"&device_id="+device_id+"";
            System.out.println(url1);
            String bodyData1 = String.format("{\"area_type\":\"170\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"},\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\"," +
                            "\"origin_type\":\"%s\"," +
                            "\"origin_id\":\"%s\",\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\",\\\"alkey\\\":\\\"1128_99514375927_0_3578244605646345985_010\\\",\\\"c_biz_combo\\\":\\\"8\\\"," +
                            "\\\"render_track_id\\\":\\\"%s\\\",\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true," +
                            "\\\\\\\"ip\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\"," +
                            "\"marketing_plan_id\":\"%s\",\"s_type\":\"\",\"entrance_params\":\"{\\\"order_status\\\":3,\\\"previous_page\\\":\\\"toutiao_mytab__order_list_page\\\",\\\"carrier_source\\\":\\\"order_detail\\\"," +
                            "\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"room_id\\\":\\\"\\\",\\\"promotion_id\\\":\\\"\\\",\\\"author_id\\\":\\\"\\\",\\\"group_id\\\":\\\"\\\",\\\"anchor_id\\\":\\\"\\\",\\\"source_method\\\":\\\"open_url\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_activity_banner\\\":0,\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"0\\\",\\\"is_with_video\\\":1,\\\"label_name\\\":\\\"\\\",\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0,\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0,\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\"," +
                            "\\\"commodity_id\\\":\\\"%s\\\",\\\"commodity_type\\\":6," +
                            "\\\"product_id\\\":\\\"%s\\\",\\\"extra_campaign_type\\\":\\\"\\\"}\",\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0,\"pay_type\":2,\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}},\"post_tel\":\"13568504862\",\"address_id\":\"0\"," +
                            "\"price_info\":{\"origin\":%d,\"freight\":0,\"coupon\":0" +
                            ",\"pay\":%d,\"packing_charge_amount\":0},\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\"," +
                            "\\\"dev_info\\\":{\\\"reqIp\\\":\\\"%s\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.5.1\\\",\\\"aid\\\":\\\"13\\\",\\\"ua\\\":\\\"com.ss.android.article.news/9070+(Linux;+U;+Android+12;+zh_CN;+PGBM10;+Build/SP1A.210812.016;+Cronet/TTNetVersion:f6f1f7ad+2022-10-31+QuicVersion:22f74f01+2022-10-11)\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\"," +
                            "\\\"deviceId\\\":\\\"%s\\\",\\\"osVersion\\\":\\\"12\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"9.0.7\\\",\\\"appName\\\":\\\"news_article\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"PGBM10\\\",\\\"channel\\\":\\\"oppo_13_64\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"907\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"OPPO\\\"," +
                            "\\\"iid\\\":\\\"%s\\\",\\\"bioType\\\":\\\"1\\\"},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[],\\\"zg_ext_param\\\":\\\"{\\\\\\\"" +
                            "decision_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\",\\\"jh_ext_info\\\":\\\"" +
                            "{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"%s\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\"," +
                            "\"render_token\":\"%s\",\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\",\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\",\"shop_stock_out_handle_infos\":null,\"shop_id\":\"GceCTPIk\"," +
                            "\"combo_id\":\"%s\",\"combo_num\":1," +
                            "\"product_id\":\"%s\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1," +
                            "\"sku_id\":\"%s\",\"warehouse_id\":\"0\"}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false,\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"0\",\"promotion_id\":\"\"," +
                            "\"ecom_scene_id\":\"%s\",\"shop_user_id\":\"\",\"group_id\":\"\",\"privilege_tag_keys\":[],\"select_privilege_properties\":[],\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                    buyRenderParamDto.getOrigin_type(),
                    buyRenderParamDto.getOrigin_id(),
                    buyRenderRoot.getRender_track_id(),
                    payIp,
                    buyRenderRoot.getTotal_price_result().getMarketing_plan_id(),
                    buyRenderParamDto.getEcom_scene_id(),
                    buyRenderParamDto.getProduct_id(),
                    buyRenderParamDto.getProduct_id(),
                    10000,
                    10000,
                    payIp,
                    device_id,
                    iid,
                    "659356656346136_1669478816601188",
                    buyRenderRoot.getPay_method().getPayapi_cache_id(),
                    buyRenderRoot.getRender_token(),
                    buyRenderParamDto.getSku_id(),
                    buyRenderParamDto.getProduct_id(),
                    buyRenderParamDto.getSku_id(),
                    buyRenderParamDto.getEcom_scene_id()
            );
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
            Thread.sleep(1 * 1000);
        }


    }
}
