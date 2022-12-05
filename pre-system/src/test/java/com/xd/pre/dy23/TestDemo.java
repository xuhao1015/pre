package com.xd.pre.dy23;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.nosql.redis.RedisDS;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import com.xd.pre.modules.px.douyin.deal.DealSearchDataUtils;
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import redis.clients.jedis.Jedis;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class TestDemo {
    public static Db db = Db.use();
    public static Jedis jedis = RedisDS.create().getJedis();

    public static void main(String[] args) throws Exception {
        Integer buyPrice = 10000;
        String payIp = PreUtils.getRandomIp();
        String post_tel = PreUtils.getTel();
        Entity appCk = db.use().queryOne("select * from douyin_app_ck where is_enable = 0 and file_name = '20221204_300.txt'and id = 12169 ");
        Entity devicesBd = db.use().queryOne("select * from douyin_device_iid where   is_enable = 0 and id > 19514 ");
        String uid = appCk.getStr("uid");
        String ck = PreAesUtils.decrypt解密(appCk.getStr("ck"));
        log.info("当前ck:{}", ck);
        String device_id = devicesBd.getStr("device_id");
        String iid = devicesBd.getStr("iid");
        log.info("当前device_id:{},iid:{}", device_id, iid);
        String seach_url = "https://ecom3-normal-hl.ecombdapi.com/aweme/v2/shop/search/aggregate/shopping/?iid=" + iid + "&device_id=" + device_id + "&ac=wifi&channel=aweGW&aid=1128&app_name=aweme&version_code=230400&version_name=23.4.0&device_platform=android&os=android&ssmix=a&device_type=Redmi+8A&device_brand=Xiaomi&language=zh&os_api=28&os_version=9&openudid=c02541edace07c13&manifest_version_code=230401&resolution=720*1369&dpi=320&update_version_code=23409900&_rticket=1670240113925&package=com.ss.android.ugc.aweme&mcc_mnc=46001&cpu_support64=false&host_abi=armeabi-v7a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&need_personal_recommend=1&is_android_pad=0&ts=1670240113&cdid=0bce1d89-65cb-4885-ae6d-527ae9a1143c&oaid=93a7d5f0816be2ca&md=0";
        OkHttpClient client = new OkHttpClient();
        String seach_body = "cursor=0&from_group_id=&no_trace_search_switch=off&enter_from=search_order_center&request_type=1&search_scene=douyin_search&search_channel=search_order_center&count=10&is_after_locate=false&ecom_theme=light&enter_from_second=order_homepage__personal_homepage&query_correct_type=1&search_source=search_history&ecom_scene_id=1031&token=search&large_font_mode=0&address_book_access=2&location_access=2&extra=%7B%22recommend_word_id%22%3A%22%22%2C%22recommend_word_session_id%22%3A%22%22%7D&search_filter=1&shown_count=0&keyword=%E6%8A%96%E9%9F%B3%E8%8B%B9%E6%9E%9C%E5%8D%A1&device_score=5.523&current_page=order_center";
        String searchMd5 = SecureUtil.md5(seach_body).toUpperCase();
        String search_body = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                searchMd5, device_id, seach_url
        );
        TimeInterval timer = DateUtil.timer();
        String search_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(search_body).execute().body();
        String search_x_gorgon = JSON.parseObject(search_sign_body).getString("x-gorgon");
        String search_x_khronos = JSON.parseObject(search_sign_body).getString("x-khronos");
        String search_x_argus = JSON.parseObject(search_sign_body).getString("x-argus");
        String search_x_ladon = JSON.parseObject(search_sign_body).getString("x-ladon");

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody searchbodyFrom = RequestBody.create(mediaType, seach_body);
        Request request = new Request.Builder()
                .url(seach_url)
                .post(searchbodyFrom)
                .addHeader("X-SS-STUB", searchMd5)
                .addHeader("Cookie", ck)
                .addHeader("X-Gorgon", search_x_gorgon)
                .addHeader("X-Khronos", search_x_khronos)
                .addHeader("X-Argus", search_x_argus)
                .addHeader("X-Ladon", search_x_ladon)
                .addHeader("user-agent", "com.ss.android.ugc.aweme/200001 (Linux; U; Android 9; zh_CN; Redmi 8A; Build/PKQ1.190319.001; Cronet/TTNetVersion:3a37693c 2022-02-10 QuicVersion:775bd845 2021-12-24)")
                .addHeader("Cookie", ck)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
        Response searchResponse = client.newCall(request).execute();
        String searchResBody = searchResponse.body().string();
        System.err.println(searchResBody);
        log.info("搜索话费毫秒数:{}", timer.interval());
        if (StrUtil.isBlank(searchResBody) || !searchResBody.contains("App Store 充值卡")) {
            log.error("没有搜索到苹果充值卡");
            return;
        }
        if (!searchResponse.headers().names().contains("x-tt-logid")) {
            log.error("当前没有获取到搜索的x-tt-logid:{}");
            return;
        }
        List<String> headers = searchResponse.headers("x-tt-logid");
        log.info("搜索到的seaId为:{}", headers.get(PreConstant.ZERO));
        List<GidAndShowdPrice> gidAndShowdPrices = DealSearchDataUtils.gidAndShowPrice(searchResBody, buyPrice);
        if (CollUtil.isEmpty(gidAndShowdPrices)) {
            return;
        }
        Set<String> notUseProducts = jedis.keys("不能使用product_id:*");
        GidAndShowdPrice gidAndShowdPrice = gidAndShowdPrices.get(PreConstant.ZERO);
        log.info("不能使用的productId:{}", notUseProducts);
        if (CollUtil.isNotEmpty(notUseProducts)) {
            for (GidAndShowdPrice gidAndShowdPriceOne : gidAndShowdPrices) {
                if (!JSON.toJSONString(notUseProducts).contains(gidAndShowdPriceOne.getGid())) {
                    gidAndShowdPrice = gidAndShowdPriceOne;
                }
            }
        }
        String searchId = headers.get(PreConstant.ZERO);
        gidAndShowdPrice.setSearch_id(searchId);
        log.info("搜索数据为msg:{}", gidAndShowdPrices);
        gidAndShowdPrice.setPayIp(payIp);
        gidAndShowdPrice.setBuyPrice(buyPrice);
        gidAndShowdPrice.setPost_tel(post_tel);
        searchResponse.close();
        log.info("根据查询结果点击进去查看数据");
        gidAndShowdPrice = getSearchPack(uid, device_id, iid, gidAndShowdPrice, ck, client);
        if (ObjectUtil.isNull(gidAndShowdPrice)) {
            log.error("封装数据失败");
            return;
        }
        gidAndShowdPrice = buyRenderData(uid, device_id, iid, gidAndShowdPrice, ck, client);
        if (ObjectUtil.isNull(gidAndShowdPrice)) {
            log.error("预下单数据失败");
            return;
        }
        gidAndShowdPrice = newcreate(uid, device_id, iid, gidAndShowdPrice, ck, client);
        log.info("结束查询查询结果");
    }

    private static GidAndShowdPrice newcreate(String uid, String device_id, String iid, GidAndShowdPrice gidAndShowdPrice, String ck, OkHttpClient client) {
        try {
            Long _rticket = System.currentTimeMillis();
            String newcreate_url = "https://ec3-core-hl.ecombdapi.com/order/newcreate/vtl?can_queue=1&b_type_new=2&sub_b_type=3&live_request_from_jsb=1&live_sdk_version=230401&webcast_sdk_version=2750&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&current_network_quality_info=%7B%22http_rtt%22%3A92%2C%22tcp_rtt%22%3A92%2C%22quic_rtt%22%3A92%2C%22downstream_throughput_kbps%22%3A6812%2C%22net_effective_connection_type%22%3A5%2C%22video_download_speed%22%3A0%2C%22quic_receive_loss_rate%22%3A-1%2C%22quic_send_loss_rate%22%3A-1%7D&address_book_access=2&user_id=" + uid + "&is_pad=false&is_android_pad=0&is_landscape=false&carrier_region=CN&iid=" + iid + "&device_id=" + device_id + "&ac=wifi&channel=aweGW&aid=1128&app_name=aweme&version_code=230400&version_name=23.4.0&device_platform=android&os=android&ssmix=a&device_type=Redmi+8A&device_brand=Xiaomi&language=zh&os_api=28&os_version=9&manifest_version_code=230401&resolution=720*1369&dpi=320&update_version_code=23409900&_rticket=" + _rticket + "&package=com.ss.android.ugc.aweme&mcc_mnc=46001&cpu_support64=false&host_abi=armeabi-v7a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&need_personal_recommend=1&ts=" + _rticket / 1000 + "&cdid=0bce1d89-65cb-4885-ae6d-527ae9a1143c&md=0";
            String newcreate_body = String.format("{\"area_type\":\"169\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"},\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\"," +
                            "\"origin_type\":\"%s\"," +
                            "\"origin_id\":\"%s\",\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\"," +
                            "\\\"alkey\\\":\\\"%s\\\",\\\"c_biz_combo\\\":\\\"8\\\"," +
                            "\\\"render_track_id\\\":\\\"%s\\\",\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true," +
                            "\\\\\\\"ip\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\"," +

                            "\"marketing_plan_id\":\"%s\",\"s_type\":\"\",\"entrance_params\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"source_method\\\":\\\"product_card\\\"," +
                            "\\\"search_id\\\":\\\"%s\\\",\\\"product_activity_type\\\":\\\"nonactivity\\\",\\\"EVENT_ORIGIN_FEATURE\\\":\\\"TEMAI\\\"," +
                            "\\\"search_params\\\":{\\\"search_id\\\":\\\"%s\\\",\\\"search_passthrough_params\\\":\\\"{\\\\\\\"previous_page\\\\\\\":\\\\\\\"personal_homepage\\\\\\\"}\\\"," +
                            "\\\"search_result_id\\\":\\\"%s\\\"},\\\"search_keyword\\\":\\\"抖音苹果卡\\\"," +
                            "\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_activity_banner\\\":0,\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"1\\\",\\\"is_with_video\\\":1,\\\"label_name\\\":\\\"\\\",\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0,\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0,\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\"," +

                            "\\\"commodity_id\\\":\\\"%s\\\",\\\"commodity_type\\\":6," +
                            "\\\"product_id\\\":\\\"%s\\\",\\\"btm_list\\\":\\\"[\\\\\\\"a1.b8094.c0.d0\\\\\\\",\\\\\\\"a1128.b4421.c0.d0\\\\\\\",\\\\\\\"a1.b6388.c2432.d1304\\\\\\\",\\\\\\\"a1128.b6207.c0.d0\\\\\\\",\\\\\\\"a1128.b0438.c0.d0\\\\\\\"]\\\",\\\"app_version\\\":\\\"23.4.0\\\",\\\"in_search_track_graph\\\":0,\\\"in_sec_graph\\\":0,\\\"extra_campaign_type\\\":\\\"\\\"}\",\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0,\"pay_type\":2,\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}}," +
                            "\"post_tel\":\"%s\",\"address_id\":\"0\",\"" +
                            "price_info\":{\"origin\":%d,\"freight\":0,\"coupon\":0," +
                            "\"pay\":%d,\"packing_charge_amount\":0},\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\"," +

                            "\\\"dev_info\\\":{\\\"reqIp\\\":\\\"%s\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.5.2\\\",\\\"aid\\\":\\\"1128\\\",\\\"ua\\\":\\\"com.ss.android.ugc.aweme/230401 (Linux; U; Android 9; zh_CN; Redmi 8A; Build/PKQ1.190319.001; Cronet/TTNetVersion:3db9a759 2022-11-23 QuicVersion:22f74f01 2022-10-11)\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\"," +
                            "\\\"deviceId\\\":\\\"%s\\\",\\\"osVersion\\\":\\\"9\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"23.4.0\\\",\\\"appName\\\":\\\"aweme\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"Redmi 8A\\\",\\\"channel\\\":\\\"aweGW\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"230400\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"Xiaomi\\\"," +
                            "\\\"iid\\\":\\\"%s\\\",\\\"bioType\\\":\\\"0\\\"},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[],\\\"zg_ext_param\\\":" +
                            "\\\"{\\\\\\\"activity_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"credit_pay_param\\\\\\\":{\\\\\\\"fee_rate_per_day\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"has_credit_param\\\\\\\":false,\\\\\\\"has_trade_time\\\\\\\":false,\\\\\\\"installment_starting_amount\\\\\\\":0,\\\\\\\"is_credit_activate\\\\\\\":false,\\\\\\\"remaining_credit\\\\\\\":0,\\\\\\\"trade_time\\\\\\\":0}," +
                            "\\\\\\\"decision_id\\\\\\\":\\\\\\\"%s\\\\\\\"," +

                            "\\\\\\\"jr_uid\\\\\\\":\\\\\\\"%s\\\\\\\"," +
                            "\\\\\\\"merchant_info\\\\\\\":{\\\\\\\"app_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"ext_uid_type\\\\\\\":0," +
                            "\\\\\\\"jh_app_id\\\\\\\":\\\\\\\"%s\\\\\\\"," +
                            "\\\\\\\"jh_merchant_id\\\\\\\":\\\\\\\"%s\\\\\\\"," +
                            "\\\\\\\"merchant_id\\\\\\\":\\\\\\\"%s\\\\\\\"," +

                            "\\\\\\\"merchant_name\\\\\\\":\\\\\\\"%s\\\\\\\"," +
                            "\\\\\\\"merchant_short_to_customer\\\\\\\":\\\\\\\"%s\\\\\\\"},\\\\\\\"promotion_ext\\\\\\\":\\\\\\\"{\\\\\\\\\\\\\\\"IsZjyFlag\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"true\\\\\\\\\\\\\\\"," +
                            "\\\\\\\\\\\\\\\"ParamOrderId\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"%s\\\\\\\\\\\\\\\"," +
                            "\\\\\\\\\\\\\\\"PromotionActivityIDs\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"%s\\\\\\\\\\\\\\\"}\\\\\\\",\\\\\\\"promotion_process\\\\\\\":{\\\\\\\"create_time\\\\\\\":1670240146," +
                            "\\\\\\\"process_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"process_info\\\\\\\":\\\\\\\"\\\\\\\"},\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"sdk_show_info\\\\\\\":{\\\\\\\"loading_style_info\\\\\\\":{\\\\\\\"loading_style\\\\\\\":\\\\\\\"crude\\\\\\\",\\\\\\\"min_time\\\\\\\":\\\\\\\"450\\\\\\\",\\\\\\\"show_pay_result\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"trade_confirm_paying_show_info\\\\\\\":{\\\\\\\"min_time\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"text\\\\\\\":\\\\\\\"抖音支付中\\\\\\\"},\\\\\\\"trade_confirm_pre_show_info\\\\\\\":{\\\\\\\"min_time\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"text\\\\\\\":\\\\\\\"安全检测通过\\\\\\\"}},\\\\\\\"verify_pwd_safe_info\\\\\\\":\\\\\\\"no\\\\\\\"}}\\\"," +

                            "\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"%s\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"1\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\"," +
                            "\"render_token\":\"%s\",\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\",\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\",\"shop_stock_out_handle_infos\":null,\"shop_id\":\"GceCTPIk\"," +
                            "\"combo_id\":\"%s\",\"combo_num\":1," +
                            "\"product_id\":\"%s\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1," +
                            "\"sku_id\":\"%s\",\"warehouse_id\":\"0\"}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false,\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"0\",\"promotion_id\":\"\"," +

                            "\"ecom_scene_id\":\"%s\",\"shop_user_id\":\"\",\"group_id\":\"\",\"privilege_tag_keys\":[],\"select_privilege_properties\":[],\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                    gidAndShowdPrice.getOrigin_type(),
                    gidAndShowdPrice.getOrigin_id(),
                    gidAndShowdPrice.getAlkey(),
                    gidAndShowdPrice.getRender_track_id(),
                    gidAndShowdPrice.getPayIp(),

                    gidAndShowdPrice.getMarketing_plan_id(),
                    gidAndShowdPrice.getSearch_id(),
                    gidAndShowdPrice.getSearch_id(),
                    gidAndShowdPrice.getSearch_id(),
                    gidAndShowdPrice.getEcom_scene_id(),

                    gidAndShowdPrice.getGid(),
                    gidAndShowdPrice.getGid(),
                    gidAndShowdPrice.getPost_tel(),
                    gidAndShowdPrice.getBuyPrice(),
                    gidAndShowdPrice.getBuyPrice(),

                    gidAndShowdPrice.getPayIp(),
                    device_id,
                    iid,
                    gidAndShowdPrice.getActivity_id(),
                    gidAndShowdPrice.getDecision_id(),

                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("jh_app_id"),
                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("app_id"),
                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("jh_app_id"),
                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("jh_merchant_id"),
                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("merchant_id"),

                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("merchant_name"),
                    JSON.parseObject(gidAndShowdPrice.getMerchant_info()).getString("merchant_short_to_customer"),
                    JSON.parseObject(gidAndShowdPrice.getPromotion_ext()).getString("ParamOrderId"),
                    JSON.parseObject(gidAndShowdPrice.getPromotion_ext()).getString("PromotionActivityIDs"),
                    JSON.parseObject(gidAndShowdPrice.getPromotion_process()).getString("process_id"),

                    gidAndShowdPrice.getPayapi_cache_id(),
                    gidAndShowdPrice.getRender_token(),
                    gidAndShowdPrice.getCombo_id(),
                    gidAndShowdPrice.getGid(),
                    gidAndShowdPrice.getCombo_id(),

                    gidAndShowdPrice.getEcom_scene_id()
            );
//            newcreate_body="{\"area_type\":\"169\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"},\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":2,\"env_type\":\"2\",\"activity_id\":\"\",\"origin_type\":\"3002070010\",\"origin_id\":\"99514375927_3586219653703310433\",\"new_source_type\":\"product_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\",\\\"alkey\\\":\\\"1128_99514375927_0_3586219653703310433_010\\\",\\\"c_biz_combo\\\":\\\"8\\\",\\\"render_track_id\\\":\\\"2022120519354601015803907426ECEC82\\\",\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true,\\\\\\\"ip\\\\\\\":\\\\\\\"222.129.35.215\\\\\\\",\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\",\"marketing_plan_id\":\"7173626756895604748\",\"s_type\":\"\",\"entrance_params\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"source_method\\\":\\\"product_card\\\",\\\"search_id\\\":\\\"202212051935140101590311401C1519A4\\\",\\\"product_activity_type\\\":\\\"nonactivity\\\",\\\"EVENT_ORIGIN_FEATURE\\\":\\\"TEMAI\\\",\\\"search_params\\\":{\\\"search_id\\\":\\\"202212051935140101590311401C1519A4\\\",\\\"search_passthrough_params\\\":\\\"{\\\\\\\"previous_page\\\\\\\":\\\\\\\"personal_homepage\\\\\\\"}\\\",\\\"search_result_id\\\":\\\"3586219653703310433\\\"},\\\"search_keyword\\\":\\\"抖音苹果卡\\\",\\\"ecom_scene_id\\\":\\\"1031,1082\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_activity_banner\\\":0,\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"1\\\",\\\"is_with_video\\\":1,\\\"label_name\\\":\\\"\\\",\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0,\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0,\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\",\\\"commodity_id\\\":\\\"3586219653703310433\\\",\\\"commodity_type\\\":6,\\\"product_id\\\":\\\"3586219653703310433\\\",\\\"btm_list\\\":\\\"[\\\\\\\"a1.b8094.c0.d0\\\\\\\",\\\\\\\"a1128.b4421.c0.d0\\\\\\\",\\\\\\\"a1.b6388.c2432.d1304\\\\\\\",\\\\\\\"a1128.b6207.c0.d0\\\\\\\",\\\\\\\"a1128.b0438.c0.d0\\\\\\\"]\\\",\\\"app_version\\\":\\\"23.4.0\\\",\\\"in_search_track_graph\\\":0,\\\"in_sec_graph\\\":0,\\\"extra_campaign_type\\\":\\\"\\\"}\",\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0,\"pay_type\":2,\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}},\"post_tel\":\"18910573310\",\"address_id\":\"0\",\"price_info\":{\"origin\":10000,\"freight\":0,\"coupon\":0,\"pay\":10000,\"packing_charge_amount\":0},\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\",\\\"dev_info\\\":{\\\"reqIp\\\":\\\"222.129.35.215\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.5.2\\\",\\\"aid\\\":\\\"1128\\\",\\\"ua\\\":\\\"com.ss.android.ugc.aweme/230401+(Linux;+U;+Android+9;+zh_CN;+Redmi+8A;+Build/PKQ1.190319.001;+Cronet/TTNetVersion:3db9a759+2022-11-23+QuicVersion:22f74f01+2022-10-11)\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\",\\\"deviceId\\\":\\\"70662667346\\\",\\\"osVersion\\\":\\\"9\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"23.4.0\\\",\\\"appName\\\":\\\"aweme\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"Redmi+8A\\\",\\\"channel\\\":\\\"aweGW\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"230400\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"Xiaomi\\\",\\\"iid\\\":\\\"1086767867631422\\\",\\\"bioType\\\":\\\"0\\\"},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[],\\\"zg_ext_param\\\":\\\"{\\\\\\\"activity_id\\\\\\\":\\\\\\\"AC221130171839902491433252\\\\\\\",\\\\\\\"credit_pay_param\\\\\\\":{\\\\\\\"fee_rate_per_day\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"has_credit_param\\\\\\\":false,\\\\\\\"has_trade_time\\\\\\\":false,\\\\\\\"installment_starting_amount\\\\\\\":0,\\\\\\\"is_credit_activate\\\\\\\":false,\\\\\\\"remaining_credit\\\\\\\":0,\\\\\\\"trade_time\\\\\\\":0},\\\\\\\"decision_id\\\\\\\":\\\\\\\"68624060367_1670240146572841\\\\\\\",\\\\\\\"jr_uid\\\\\\\":\\\\\\\"2859187631295679\\\\\\\",\\\\\\\"merchant_info\\\\\\\":{\\\\\\\"app_id\\\\\\\":\\\\\\\"NA202208012041063016245258\\\\\\\",\\\\\\\"ext_uid_type\\\\\\\":0,\\\\\\\"jh_app_id\\\\\\\":\\\\\\\"8000104428743\\\\\\\",\\\\\\\"jh_merchant_id\\\\\\\":\\\\\\\"100000010442\\\\\\\",\\\\\\\"merchant_id\\\\\\\":\\\\\\\"8020220801671981\\\\\\\",\\\\\\\"merchant_name\\\\\\\":\\\\\\\"上海格物致品网络科技有限公司\\\\\\\",\\\\\\\"merchant_short_to_customer\\\\\\\":\\\\\\\"抖音电商商家\\\\\\\"},\\\\\\\"promotion_ext\\\\\\\":\\\\\\\"{\\\\\\\\\\\\\\\"IsZjyFlag\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"true\\\\\\\\\\\\\\\",\\\\\\\\\\\\\\\"ParamOrderId\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"202212051935451640087819\\\\\\\\\\\\\\\",\\\\\\\\\\\\\\\"PromotionActivityIDs\\\\\\\\\\\\\\\":\\\\\\\\\\\\\\\"AC221114142041904185932574\\\\\\\\\\\\\\\"}\\\\\\\",\\\\\\\"promotion_process\\\\\\\":{\\\\\\\"create_time\\\\\\\":1670240146,\\\\\\\"process_id\\\\\\\":\\\\\\\"bc85758d1bba6ac63823f396b231d2a24f\\\\\\\",\\\\\\\"process_info\\\\\\\":\\\\\\\"\\\\\\\"},\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"sdk_show_info\\\\\\\":{\\\\\\\"loading_style_info\\\\\\\":{\\\\\\\"loading_style\\\\\\\":\\\\\\\"crude\\\\\\\",\\\\\\\"min_time\\\\\\\":\\\\\\\"450\\\\\\\",\\\\\\\"show_pay_result\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"trade_confirm_paying_show_info\\\\\\\":{\\\\\\\"min_time\\\\\\\":\\\\\\\"0\\\\\\\",\\\\\\\"text\\\\\\\":\\\\\\\"抖音支付中\\\\\\\"},\\\\\\\"trade_confirm_pre_show_info\\\\\\\":{\\\\\\\"min_time\\\\\\\":\\\\\\\"-1\\\\\\\",\\\\\\\"text\\\\\\\":\\\\\\\"安全检测通过\\\\\\\"}},\\\\\\\"verify_pwd_safe_info\\\\\\\":\\\\\\\"no\\\\\\\"}}\\\",\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"20221205193546572824az70yb21c2xd\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"1\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\",\"render_token\":\""+gidAndShowdPrice.getRender_token()+"\",\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\",\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\",\"shop_stock_out_handle_infos\":null,\"shop_id\":\"GceCTPIk\",\"combo_id\":\"1751083813772300\",\"combo_num\":1,\"product_id\":\"3586219653703310433\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1,\"sku_id\":\"1751083813772300\",\"warehouse_id\":\"0\"}],\"warehouse_id\":0,\"coupon_info\":{},\"freight_insurance\":false,\"cert_insurance\":false,\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"0\",\"promotion_id\":\"\",\"ecom_scene_id\":\"1031,1082\",\"shop_user_id\":\"\",\"group_id\":\"\",\"privilege_tag_keys\":[],\"select_privilege_properties\":[],\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}";
            log.info("请求数据Msg:{}", newcreate_body);
            String create_md5 = SecureUtil.md5("json_form=" + URLEncoder.encode(newcreate_body)).toUpperCase();
            String create_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    create_md5, newcreate_url
            );
            String create_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(create_body_sign).execute().body();
            String create_x_gorgon = JSON.parseObject(create_sign_body).getString("x-gorgon");
            String create_x_khronos = JSON.parseObject(create_sign_body).getString("x-khronos");
            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", newcreate_body)
                    .build();
            Request.Builder builder = new Request.Builder();
            Request request_create = builder.url(newcreate_url)
                    .post(requestBody1)
                    .addHeader("Cookie", ck)
                    .addHeader("X-SS-STUB", create_md5)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", create_x_gorgon)
                    .addHeader("X-Khronos", create_x_khronos)
                    .build();
            Response execute = client.newCall(request_create).execute();
            String createbody = execute.body().string();
            log.info("下单数据:{}", createbody);
            return null;
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;
    }

    private static GidAndShowdPrice buyRenderData(String uid, String device_id, String iid, GidAndShowdPrice gidAndShowdPrice, String ck, OkHttpClient client) {
        try {
            Long _rticket = System.currentTimeMillis();
            String buyRenderUrl = "https://ec3-core-hl.ecombdapi.com/order/buyRender?b_type_new=2&sub_b_type=3&live_request_from_jsb=1&live_sdk_version=230401&webcast_sdk_version=2750&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&current_network_quality_info=%7B%22http_rtt%22%3A92%2C%22tcp_rtt%22%3A92%2C%22quic_rtt%22%3A92%2C%22downstream_throughput_kbps%22%3A6812%2C%22net_effective_connection_type%22%3A5%2C%22video_download_speed%22%3A0%2C%22quic_receive_loss_rate%22%3A-1%2C%22quic_send_loss_rate%22%3A-1%7D&address_book_access=2&user_id=" + uid + "&is_pad=false&is_android_pad=0&is_landscape=false&carrier_region=CN&iid=" + iid + "&device_id=" + device_id + "&ac=wifi&channel=aweGW&aid=1128&app_name=aweme&version_code=230400&version_name=23.4.0&device_platform=android&os=android&ssmix=a&device_type=Redmi+8A&device_brand=Xiaomi&language=zh&os_api=28&os_version=9&manifest_version_code=230401&resolution=720*1369&dpi=320&update_version_code=23409900&_rticket=" + _rticket + "&package=com.ss.android.ugc.aweme&mcc_mnc=46001&cpu_support64=false&host_abi=armeabi-v7a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&need_personal_recommend=1&ts=" + _rticket / 1000 + "&cdid=0bce1d89-65cb-4885-ae6d-527ae9a1143c&md=0";
            String buyRenderBody = String.format("{\"address\":null,\"platform_coupon_id\":null,\"kol_coupon_id\":null,\"auto_select_best_coupons\":true,\"customize_pay_type\":\"{\\\"checkout_id\\\":1,\\\"bio_type\\\":\\\"0\\\"}\",\"first_enter\":true,\"source_type\":\"1\",\"shape\":0,\"marketing_channel\":\"\",\"forbid_redpack\":false,\"support_redpack\":true,\"use_marketing_combo\":false,\"entrance_params\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"source_method\\\":\\\"product_card\\\"," +
                            "\\\"search_id\\\":\\\"%s\\\",\\\"product_activity_type\\\":\\\"nonactivity\\\",\\\"EVENT_ORIGIN_FEATURE\\\":\\\"TEMAI\\\",\\\"search_params\\\":" +
                            "{\\\"search_id\\\":\\\"%s\\\",\\\"search_passthrough_params\\\":\\\"{\\\\\\\"previous_page\\\\\\\":\\\\\\\"personal_homepage\\\\\\\"}\\\",\\\"" +
                            "search_result_id\\\":\\\"%s\\\"},\\\"search_keyword\\\":\\\"抖音苹果卡\\\"," +
                            "\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_activity_banner\\\":0,\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_short_screen\\\":\\\"1\\\",\\\"is_with_video\\\":1,\\\"label_name\\\":\\\"\\\",\\\"market_channel_hot_fix\\\":\\\"\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_dou_campaign\\\":0,\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"upfront_presell\\\":0,\\\"warm_up_status\\\":\\\"0\\\",\\\"auto_coupon\\\":0,\\\"coupon_id\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"item_id\\\":\\\"0\\\"," +
                            "\\\"commodity_id\\\":\\\"%s\\\",\\\"commodity_type\\\":6," +

                            "\\\"product_id\\\":\\\"%s\\\",\\\"btm_list\\\":\\\"[\\\\\\\"a1.b8094.c0.d0\\\\\\\",\\\\\\\"a1128.b4421.c0.d0\\\\\\\",\\\\\\\"a1.b6388.c2432.d1304\\\\\\\",\\\\\\\"a1128.b6207.c0.d0\\\\\\\",\\\\\\\"a1128.b0438.c0.d0\\\\\\\"]\\\",\\\"app_version\\\":\\\"23.4.0\\\",\\\"in_search_track_graph\\\":0,\\\"in_sec_graph\\\":0}\"," +
                            "\"shop_requests\":[{\"shop_id\":\"%s\",\"product_requests\":" +
                            "[{\"product_id\":\"%s\"," +
                            "\"sku_id\":\"%s\",\"sku_num\":1," +
                            "\"ecom_scene_id\":\"%s\"," +
                            "\"origin_id\":\"%s\"," +
                            "\"origin_type\":\"%s\",\"new_source_type\":\"product_detail\",\"select_privilege_properties\":[]}]}]}",
                    gidAndShowdPrice.getSearch_id(), gidAndShowdPrice.getSearch_id(), gidAndShowdPrice.getSearch_id(), gidAndShowdPrice.getEcom_scene_id(), gidAndShowdPrice.getGid(),
                    gidAndShowdPrice.getGid(), gidAndShowdPrice.getShop_id(), gidAndShowdPrice.getGid(), gidAndShowdPrice.getCombo_id(), gidAndShowdPrice.getEcom_scene_id(),
                    gidAndShowdPrice.getOrigin_id(), gidAndShowdPrice.getOrigin_type());
            String buyRenderMd5 = SecureUtil.md5("json_form=" + URLEncoder.encode(buyRenderBody)).toUpperCase();
            String pack_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    buyRenderMd5, device_id, buyRenderUrl
            );
            String pack_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(pack_body_sign).execute().body();
            String buyRender_x_gorgon = JSON.parseObject(pack_sign_body).getString("x-gorgon");
            String buyRender_x_khronos = JSON.parseObject(pack_sign_body).getString("x-khronos");
            String buyRender_x_argus = JSON.parseObject(pack_sign_body).getString("x-argus");
            String buyRender_x_ladon = JSON.parseObject(pack_sign_body).getString("x-ladon");

            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", buyRenderBody)
                    .build();
            Request.Builder builder = new Request.Builder();
            Request requestBuyRender = builder.url(buyRenderUrl)
                    .post(requestBody1)
                    .addHeader("Cookie", ck)
                    .addHeader("X-SS-STUB", buyRenderMd5)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", buyRender_x_gorgon)
                    .addHeader("X-Khronos", buyRender_x_khronos)
                    .addHeader("X-Argus", buyRender_x_argus)
                    .addHeader("X-Ladon", buyRender_x_ladon)
                    .build();
            Response execute = client.newCall(requestBuyRender).execute();
            String buyRenderbody = execute.body().string();
            log.info("预下单数据:{}", buyRenderbody);
            execute.close();
            String zg_ext_info_str = JSON.parseObject(buyRenderbody).getJSONObject("data").getJSONObject("pay_method").getString("zg_ext_info");
            BeanUtil.copyProperties(JSON.parseObject(zg_ext_info_str), gidAndShowdPrice);
            BuyRenderRoot buyRenderRoot = JSON.parseObject(JSON.parseObject(buyRenderbody).getString("data"), BuyRenderRoot.class);
            String decision_id = buyRenderRoot.getPay_method().getDecision_id();
            String payapi_cache_id = buyRenderRoot.getPay_method().getPayapi_cache_id();
            String render_token = buyRenderRoot.getRender_token();
            String render_track_id = buyRenderRoot.getRender_track_id();
            String marketing_plan_id = buyRenderRoot.getTotal_price_result().getMarketing_plan_id();
            gidAndShowdPrice.setDecision_id(decision_id);
            gidAndShowdPrice.setPayapi_cache_id(payapi_cache_id);
            gidAndShowdPrice.setRender_token(render_token);
            gidAndShowdPrice.setRender_track_id(render_track_id);
            gidAndShowdPrice.setMarketing_plan_id(marketing_plan_id);
            return gidAndShowdPrice;
        } catch (Exception e) {
            log.error("预下单报错msg:{}", e.getMessage());
        }
        return null;
    }

    private static GidAndShowdPrice getSearchPack(String uid, String device_id, String iid, GidAndShowdPrice gidAndShowdPrice, String ck, OkHttpClient client) {
        try {
            Long _rticket = System.currentTimeMillis();
            String pack_url = "https://ecom3-normal-hl.ecombdapi.com/aweme/v2/shop/promotion/pack/?iid=" + iid + "&device_id=" + device_id + "&ac=wifi&channel=aweGW&aid=1128&app_name=aweme&version_code=230400&version_name=23.4.0&device_platform=android&os=android&ssmix=a&device_type=Redmi+8A&device_brand=Xiaomi&language=zh&os_api=28&os_version=9&manifest_version_code=230401&resolution=720*1369&dpi=320&update_version_code=23409900&_rticket=" + _rticket + "&package=com.ss.android.ugc.aweme&mcc_mnc=46001&cpu_support64=false&host_abi=armeabi-v7a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&need_personal_recommend=1&is_android_pad=0&ts=" + _rticket / 1000 + "&cdid=0bce1d89-65cb-4885-ae6d-527ae9a1143c&md=0";
            String pack_body = "user_id=" + uid + "&author_id=&author_open_id=&sec_author_id=" +
                    "&promotion_ids=" + gidAndShowdPrice.getGid() + "&item_id=&enter_from=order_merge_page&meta_param=%7B%22entrance_info%22%3A%22%7B%5C%22carrier_source%5C%22%3A%5C%22search_order_center%5C%22%2C%5C%22source_method%5C%22%3A%5C%22product_card%5C%22%2C%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22product_activity_type%5C%22%3A%5C%22nonactivity%5C%22%2C%5C%22EVENT_ORIGIN_FEATURE%5C%22%3A%5C%22TEMAI%5C%22%2C%5C%22search_params%5C%22%3A%7B%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22search_passthrough_params%5C%22%3A%5C%22%7B%5C%5C%5C%22previous_page%5C%5C%5C%22%3A%5C%5C%5C%22personal_homepage%5C%5C%5C%22%7D%5C%22%2C%5C%22search_result_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getGid() + "%5C%22%7D%2C%5C%22search_keyword%5C%22%3A%5C%22%E6%8A%96%E9%9F%B3%E8%8B%B9%E6%9E%9C%E5%8D%A1%5C%22%2C%5C%22ecom_scene_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getEcom_scene_id() + "%5C%22%2C%5C%22ecom_group_type%5C%22%3A%5C%22%5C%22%2C%5C%22card_status%5C%22%3A%5C%22%5C%22%2C%5C%22module_label%5C%22%3A%5C%22%5C%22%2C%5C%22ecom_icon%5C%22%3A%5C%22%5C%22%7D%22%7D&width=720&height=720&rank_id=&use_new_price=1&gps_on=0&product_id=&creative_id=&promotion_id=&bff_type=1&ui_params=%7B%22action_type%22%3A%22%22%2C%22carrier_source%22%3A%22search_order_center%22%2C%22client_abs%22%3A%22%7B%5C%22iesec_new_goods_detail_edition%5C%22%3A6%2C%5C%22iesec_detail_head_search_plan%5C%22%3A2%7D%22%2C%22ecom_entrance_form%22%3A%22product_card%22%2C%22enter_method%22%3A%22ec_result%22%2C%22entrance_info%22%3A%22%7B%5C%22carrier_source%5C%22%3A%5C%22search_order_center%5C%22%2C%5C%22source_method%5C%22%3A%5C%22product_card%5C%22%2C%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22product_activity_type%5C%22%3A%5C%22nonactivity%5C%22%2C%5C%22EVENT_ORIGIN_FEATURE%5C%22%3A%5C%22TEMAI%5C%22%2C%5C%22search_params%5C%22%3A%7B%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22search_passthrough_params%5C%22%3A%5C%22%7B%5C%5C%5C%22previous_page%5C%5C%5C%22%3A%5C%5C%5C%22personal_homepage%5C%5C%5C%22%7D%5C%22%2C%5C%22search_result_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getGid() + "%5C%22%7D%2C%5C%22search_keyword%5C%22%3A%5C%22%E6%8A%96%E9%9F%B3%E8%8B%B9%E6%9E%9C%E5%8D%A1%5C%22%2C%5C%22ecom_scene_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getEcom_scene_id() + "%5C%22%2C%5C%22ecom_group_type%5C%22%3A%5C%22%5C%22%2C%5C%22card_status%5C%22%3A%5C%22%5C%22%2C%5C%22module_label%5C%22%3A%5C%22%5C%22%2C%5C%22ecom_icon%5C%22%3A%5C%22%5C%22%7D%22%2C%22follow_status%22%3A0%2C%22font_scale%22%3A1.0%2C%22from_live%22%3Afalse%2C%22from_video%22%3Afalse%2C%22full_mode%22%3Atrue%2C%22iesec_new_goods_detail_edition%22%3A6%2C%22icon_type%22%3A%22%22%2C%22is_luban%22%3Afalse%2C%22is_recommend_enable%22%3Atrue%2C%22is_short_screen%22%3Atrue%2C%22window_reposition%22%3Afalse%2C%22large_font_scale%22%3Afalse%2C%22live_room_id%22%3A%22%22%2C%22module_name%22%3A%22%22%2C%22page_id%22%3A%22%22%2C%22product_id%22%3A%22%22%2C%22promotion_id%22%3A%22" +
                    gidAndShowdPrice.getEcom_scene_id() + "%22%2C%22request_additions%22%3A%22%7B%5C%22sec_author_id%5C%22%3A%5C%22%5C%22%2C%5C%22ecom_scene_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getEcom_scene_id() + "%5C%22%2C%5C%22enter_from%5C%22%3A%5C%22order_merge_page%5C%22%7D%22%2C%22show_sku_panel%22%3A0%2C%22source_method%22%3A%22product_card%22%2C%22source_page%22%3A%22search_order_center%22%2C%22three_d_log_data%22%3A%22%22%2C%22useful_screen_width%22%3A360%2C%22v3_events_additions%22%3A%22%7B%5C%22search_params%5C%22%3A%7B%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22search_passthrough_params%5C%22%3A%5C%22%7B%5C%5C%5C%22previous_page%5C%5C%5C%22%3A%5C%5C%5C%22personal_homepage%5C%5C%5C%22%7D%5C%22%2C%5C%22search_result_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getEcom_scene_id() + "%5C%22%7D%7D%22%7D&user_avatar_shrink=96_96&goods_header_shrink=720_720&goods_comment_shrink=338_338&shop_avatar_shrink=74_74&common_large_shrink=2160_2160&ecom_scene_id=" +
                    gidAndShowdPrice.getEcom_scene_id() + "&sec_author_id=&" +
                    "ecom_scene_id=" + gidAndShowdPrice.getEcom_scene_id() + "&enter_from=order_merge_page&same_product_scene=0&is_preload_req=false";
            String packMd5 = SecureUtil.md5(pack_body).toUpperCase();
            String pack_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    packMd5, device_id, pack_url
            );
            String pack_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(pack_body_sign).execute().body();
            String pack_x_gorgon = JSON.parseObject(pack_sign_body).getString("x-gorgon");
            String pack_x_khronos = JSON.parseObject(pack_sign_body).getString("x-khronos");
            String pack_x_argus = JSON.parseObject(pack_sign_body).getString("x-argus");
            String pack_x_ladon = JSON.parseObject(pack_sign_body).getString("x-ladon");
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody searchbodyFrom = RequestBody.create(mediaType, pack_body);
            Request request = new Request.Builder()
                    .url(pack_url)
                    .post(searchbodyFrom)
                    .addHeader("X-SS-STUB", packMd5)
                    .addHeader("Cookie", ck)
                    .addHeader("X-Gorgon", pack_x_gorgon)
                    .addHeader("X-Khronos", pack_x_khronos)
                    .addHeader("X-Argus", pack_x_argus)
                    .addHeader("X-Ladon", pack_x_ladon)
                    .addHeader("user-agent", "com.ss.android.ugc.aweme/200001 (Linux; U; Android 9; zh_CN; Redmi 8A; Build/PKQ1.190319.001; Cronet/TTNetVersion:3a37693c 2022-02-10 QuicVersion:775bd845 2021-12-24)")
                    .addHeader("Cookie", ck)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("cache-control", "no-cache")
                    .build();
            Response searchResponse = client.newCall(request).execute();
            String packResBody = searchResponse.body().string();
            log.info("packResBody：{}", JSON.toJSONString(packResBody));
            if (ObjectUtil.isNotNull(packResBody) && !packResBody.contains("combo_id") && packResBody.contains(gidAndShowdPrice.getGid())) {
                jedis.set("不能使用product_id:" + gidAndShowdPrice.getGid(), gidAndShowdPrice.getGid());
                return null;
            }
            String h5_url = JSON.parseObject(packResBody).getJSONObject("promotion_v3").getJSONObject("bottom_button").getJSONObject("vo").getJSONObject("meta").getJSONObject("buy").getString("h5_url");
            Map<String, String> params = PreUtils.parseUrl(h5_url).getParams();
            BeanUtil.copyProperties(params, gidAndShowdPrice);
            log.info("封装数据msg:{}", gidAndShowdPrice);
            return gidAndShowdPrice;
        } catch (Exception e) {
            log.error("点击查询结果详情报错:{}", e.getMessage());
        }
        return null;
    }


}
