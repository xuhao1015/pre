package com.xd.pre.douyinnew;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.jddj.douy.Douyin3;
import com.xd.pre.modules.px.douyin.buyRender.BuyRenderParamDto;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import com.xd.pre.modules.px.douyin.submit.SubmitUtils;
import com.xd.pre.modules.sys.util.PreUtil;
import com.xd.pre.register.SetRelaship;
import com.xd.pre.tiren.FindOrder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Slf4j
//DouYNewJinritout
public class DouYNewCheckBangding1 {
    public static void main(String[] args) throws Exception {
        Db db = FindOrder.db;
        List<Entity> query = db.query("select * from douyin_app_ck where is_enable =5 and id > 8130 and id < 8225");
        for (Entity entity : query) {
            Integer payType = 2;
            String payIp = PreUtils.getRandomIp();
            String uid = entity.getStr("uid");
            String bangdingData = SetRelaship.jedis.get("抖音和设备号关联:" + uid);
            if (StrUtil.isBlank(bangdingData)) {
                continue;
            }
            String device_id = "device_id_str=" + JSON.parseObject(bangdingData).getString("deviceId");
            String iid = "install_id_str=" + JSON.parseObject(bangdingData).getString("iid");
            String ck = PreAesUtils.decrypt解密(entity.getStr("ck"));
            if (device_id.contains("device_id_str=")) {
                device_id = device_id.replace("device_id_str=", "");
            }
            if (iid.contains("install_id_str=")) {
                iid = iid.replace("install_id_str=", "");
            }

            BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3556357230829939771").sku_id("1736502553929735").author_id("4051040200033531")
                    .ecom_scene_id("1003").origin_id("4051040200033531_3556357230829939771").origin_type("3002002002").shop_id("GceCTPIk").new_source_type("product_detail").build();
            System.err.println(JSON.toJSONString(buyRenderParamDto));
            String body = SubmitUtils.buildBuyRenderParamData(buyRenderParamDto);
            OkHttpClient client = Douyin3.getIpAndPort20();
            String url = "https://ken.snssdk.com/order/buyRender?b_type_new=2&request_tag_from=lynx&os_api=25&device_type=XiMe&ssmix=a&manifest_version_code=169&dpi=240&is_guest_mode=0&uuid=354730528934825&app_name=aweme&version_name=17.3.0&ts=1664384063&cpu_support64=false&app_type=normal&appTheme=dark&ac=4G&host_abi=arm64-v8a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1664384064117&device_platform=android&iid=" + iid + "&version_code=170300&cdid=78d30492-1201-49ea-b86a-1246a704711d&os=android&is_android_pad=0&openudid="+PreUtils.getRandomString(16)+"&device_id=" + device_id + "&resolution=720%2A1280&os_version="+PreUtils.getRandomString(5)+"&language=zh&device_brand=Xiaomi&aid=1128&minor_status=0&mcc_mnc="+ PreUtils.getRandomNum(5);
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
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (Exception e) {
                continue;
            }
            String resBody = response.body().string();
            log.info("预下单数据msg:{}", resBody);
            if (resBody.contains("用户信息获取失败")) {
                db.use().execute("update douyin_app_ck set is_enable = ? where uid = ?", -1, uid);
                continue;
            }
            if(resBody.contains("获取商品规格信息失败")){
                db.use().execute("update douyin_app_ck set is_enable = ? where uid = ?", 6, uid);
                continue;
            }
            response.close();
            if (false) {
                return;
            }
            BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(resBody).getString("data"), BuyRenderRoot.class);
            String url1 = "https://ec.snssdk.com/order/newcreate/vtl?can_queue=1&b_type_new=2&request_tag_from=lynx&os_api=31&device_type=XiMe&ssmix=a&manifest_version_code=" + PreUtils.getRandomNum(5) + "&dpi=240&is_guest_mode=0&app_name=aweme&version_name=" + PreUtils.getRandomNum(5) + "&cpu_support64=false&app_type=normal&appTheme=dark&ac=wifi&host_abi=armeabi-v7a&update_version_code=" + PreUtils.getRandomNum(8) + "&channel="+PreUtils.getRandomString(10)+"&device_platform=android&iid=" + iid + "&version_code="+PreUtils.getRandomNum(5)+"&cdid="+PreUtils.getRandomString(36)+"d&os=android&is_android_pad=0&openudid="+PreUtils.getRandomNum(16)+"&device_id="
                    + device_id + "&resolution=720*1280&os_version=" + PreUtils.getRandomString(5) + "&language=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc="+PreUtils.getRandomNum(5);
            String bodyData1 = String.format("{\"area_type\":\"170\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"}," +
                            "\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\"," +
                            "\"origin_type\":\"%s\"," +
                            "\"origin_id\":\"%s\"," +
                            "\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\"," +
                            "\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\"," +
                            "\\\"alkey\\\":\\\"1128_99514375927_0_3556357046087622442_010\\\"," +
                            "\\\"c_biz_combo\\\":\\\"8\\\"," +
                            "\\\"render_track_id\\\":\\\"%s\\\"," +
                            "\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\"" +
                            ",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\"," +
                            "\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true," +
                            "\\\\\\\"ip\\\\\\\":\\\\\\\"%s\\\\\\\"," +
                            "\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\"," +
                            "\"marketing_plan_id\":\"%s\"," +
                            "\"s_type\":\"\"" +
                            ",\"entrance_params\":\"{\\\"order_status\\\":4,\\\"previous_page\\\":\\\"toutiao_mytab__order_list_page\\\"," +
                            "\\\"carrier_source\\\":\\\"order_detail\\\"," +
                            "\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"room_id\\\":\\\"\\\"," +
                            "\\\"promotion_id\\\":\\\"\\\",\\\"author_id\\\":\\\"\\\",\\\"group_id\\\":\\\"\\\",\\\"anchor_id\\\":\\\"\\\"," +
                            "\\\"source_method\\\":\\\"open_url\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\"," +
                            "\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\"," +
                            "\\\"is_activity_banner\\\":0," +
                            "\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\"," +
                            "\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"0\\\",\\\"is_with_video\\\":1,\\\"label_name\\\":\\\"\\\"," +
                            "\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0," +
                            "\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0," +
                            "\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\"," +
                            "\\\"commodity_id\\\":\\\"%s\\\",\\\"commodity_type\\\":6," +
                            "\\\"product_id\\\":\\\"%s\\\",\\\"extra_campaign_type\\\":\\\"\\\"}\"," +
                            "\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0," +
                            "\"pay_type\":%d," +
                            "\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}}," +
                            "\"post_tel\":\"%s\",\"address_id\":\"0\",\"price_info\":{\"origin\":1000,\"freight\":0,\"coupon\":0," +
                            "\"pay\":1000}," +
                            "\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\",\\\"dev_info\\\":{\\\"reqIp\\\":\\\"39.144.42.162\\\",\\\"os\\\":\\\"android\\\"," +
                            "\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.3.5\\\",\\\"aid\\\":\\\"13\\\"," +
                            "\\\"ua\\\":\\\"com.ss.android.article.news/9070+(Linux;+U;+Android+12;+zh_CN;+PGBM10;+Build/SP1A.210812.016;+Cronet/TTNetVersion:f6f1f7ad+2022-10-31+QuicVersion:22f74f01+2022-10-11)\\\"," +
                            "\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\"," +
                            "\\\"deviceId\\\":\\\"%s\\\",\\\"osVersion\\\":\\\"10\\\"," +
                            "\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\"," +
                            "\\\"appVersion\\\":\\\"8.9.6\\\",\\\"appName\\\":\\\"aweme\\\"," +
                            "\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"PACT00\\\"," +
                            "\\\"channel\\\":\\\"oppo_13_64\\\",\\\"openudid\\\":\\\"\\\"," +
                            "\\\"versionCode\\\":\\\"896\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"OPPO\\\",\\\"iid\\\":\\\"%s\\\",\\\"bioType\\\":\\\"1\\\"}," +
                            "\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[]," +
                            "\\\"zg_ext_param\\\":" +
                            "\\\"{\\\\\\\"decision_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"," +
                            "\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\"," +
                            "\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"%s\\\\\\\"}\\\"," +
                            "\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\"," +
                            "\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\"," +
                            "\"render_token\":\"%s\"," +
                            "\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\"," +
                            "\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\"," +
                            "\"shop_id\":\"GceCTPIk\"," +
                            "\"combo_id\":\"%s\"," +
                            "\"combo_num\":1," +
                            "\"product_id\":\"%s\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1," +
                            "\"sku_id\":\"%s\"" +
                            ",\"warehouse_id\":\"0\"}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false," +
                            "\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"0\",\"promotion_id\":\"\"," +
                            "\"ecom_scene_id\":\"%s\"," +
                            "\"shop_user_id\":\"\",\"group_id\":\"\"," +
                            "\"privilege_tag_keys\":[],\"select_privilege_properties\":[]," +
                            "\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                    buyRenderParamDto.getOrigin_type(),
                    buyRenderParamDto.getOrigin_id(),
                    buyRenderRoot.getRender_track_id(),
                    payIp,
                    buyRenderRoot.getTotal_price_result().getMarketing_plan_id(),
                    buyRenderParamDto.getEcom_scene_id(),
                    buyRenderParamDto.getProduct_id(),
                    buyRenderParamDto.getProduct_id(),
                    payType,
                    PreUtils.getTel(),
                    device_id,
                    iid,
                    buyRenderRoot.getPay_method().getDecision_id(),
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
            Response response1 = null;
            try {
                response1 = client.newCall(request1).execute();
            } catch (Exception e) {
                continue;
            }
            String bodyRes1 = response1.body().string();
            response1.close();
            log.info("uid:{}:{}", uid, bodyRes1);
            if (bodyRes1.contains("当前下单人数过多")) {
                db.use().execute("update douyin_app_ck set is_enable = ? where uid = ?", 3, uid);
            }
            if (bodyRes1.contains("设备存在异常")) {
                db.use().execute("update douyin_app_ck set is_enable = ? where uid = ?", 4, uid);
            }
            if (bodyRes1.contains("order_id")) {
                db.use().execute("update douyin_app_ck set is_enable = ? where uid = ?", 5, uid);
            }
            Thread.sleep(1 * 1000);
        }
    }
}
