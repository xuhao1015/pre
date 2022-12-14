package com.xd.pre.modules.px.douyin.toutiao;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import com.xd.pre.modules.sys.domain.DouyinAppCk;
import com.xd.pre.modules.sys.domain.DouyinMethodNameParam;
import okhttp3.FormBody;

import java.net.URLEncoder;

public class BuildDouYinUrlUtils {

    public static String buildSearchAndPackUrl(JSONObject searchParamJson, DouyinMethodNameParam douyinMethodNameParam, DouyinAppCk douyinAppCk) {
        StringBuilder returnStrB = new StringBuilder();
        String exParam = douyinAppCk.getExParam();
        JSONObject paramJsons = JSON.parseObject(exParam);
        returnStrB.append(douyinMethodNameParam.getMethodUrl());
        if (ObjectUtil.isNotNull(douyinAppCk)) {
            returnStrB.append(String.format("device_id=%s&iid=%s&", douyinAppCk.getDeviceId(), douyinAppCk.getIid()));
        }
        if (ObjectUtil.isNotNull(searchParamJson)) {
            for (String key : searchParamJson.keySet()) {
                returnStrB.append(key + "=" + searchParamJson.getString(key) + "&");
            }
        }
        for (String key : paramJsons.keySet()) {
            returnStrB.append(key + "=" + paramJsons.getString(key) + "&");
        }

        returnStrB.append("_rticket=" + System.currentTimeMillis());
        return returnStrB.toString();
    }

    public static FormBody formPack(DouyinMethodNameParam douyinMethodNameParam, GidAndShowdPrice gidAndShowdPrice) {
        FormBody.Builder builder = new FormBody.Builder();
        String data = String.format(douyinMethodNameParam.getMethodParam(), gidAndShowdPrice.getGid());
        JSONObject packJson = JSON.parseObject(data);
        for (String key : packJson.keySet()) {
            if (StrUtil.isNotBlank(packJson.getString(key))) {
                if (StrUtil.isNotBlank(packJson.getString(key))) {
                    builder.add(key, URLEncoder.encode(packJson.getString(key)));
                }
            }
        }
        FormBody build = builder.build();
        return build;
    }

    public static String buildPackPostData(DouyinMethodNameParam douyinMethodNameParam, GidAndShowdPrice gidAndShowdPrice) {
        FormBody.Builder builder = new FormBody.Builder();
               /* .add("json_form", buyRenderBody)
                .build();
        */
        StringBuilder returnStrB = new StringBuilder();

        String data = String.format(douyinMethodNameParam.getMethodParam(), gidAndShowdPrice.getGid());
        JSONObject packJson = JSON.parseObject(data);
        for (String key : packJson.keySet()) {
            if (StrUtil.isNotBlank(packJson.getString(key))) {
                returnStrB.append(key + "=" + URLEncoder.encode(packJson.getString(key)) + "&");
            }
        }
        return returnStrB.toString().substring(0, returnStrB.toString().length() - 1);
    }


    public static String buildBuyRender(GidAndShowdPrice gidAndShowdPrice) {
        String buildData = String.format("{\"address\":null,\"display_scene\":\"buy_again\",\"platform_coupon_id\":null,\"kol_coupon_id\":null,\"auto_select_best_coupons\":true,\"customize_pay_type\":\"{\\\"checkout_id\\\":1,\\\"bio_type\\\":\\\"1\\\"}\",\"first_enter\":true,\"source_type\":\"2\",\"shape\":0,\"marketing_channel\":\"\",\"forbid_redpack\":false,\"support_redpack\":true,\"use_marketing_combo\":false,\"entrance_params\":\"" +
                        "{\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"is_groupbuying\\\":0}\",\"shop_requests\":[{\"shop_id\":\"GceCTPIk\"," +
                        "\"product_requests\":[{\"product_id\":\"%s\"," +
                        "\"sku_id\":\"%s\"," +
                        "\"sku_num\":1,\"ecom_scene_id\":\"%s\",\"new_source_type\":\"order_detail\",\"select_privilege_properties\":[]}]}]}",
                gidAndShowdPrice.getEcom_scene_id(),
                gidAndShowdPrice.getProduct_id(),
                gidAndShowdPrice.getSku_id(),
                gidAndShowdPrice.getEcom_scene_id()
        );
        System.err.println("============" + buildData);
        return buildData;
    }

    public static String buildCreatenew(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk) {
        String counStr = "{}";
        if (gidAndShowdPrice.getTotal_amount().intValue() != gidAndShowdPrice.getTotal_origin_amount()) {
            counStr = String.format("{\"shop\":{\"coupon_id\":\"%s\",\"coupon_meta_id\":\"%s\"}}", gidAndShowdPrice.getCoupon_info_id(), gidAndShowdPrice.getCoupon_meta_id());
        }

        String buildData = String.format("{\"area_type\":\"169\",\"receive_type\":1,\"travel_info\":{\"departure_time\":0,\"trave_type\":1,\"trave_no\":\"\"},\"pickup_station\":\"\",\"traveller_degrade\":\"\",\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\",\"origin_type\":\"0\",\"origin_id\":\"0\",\"new_source_type\":\"order_detail\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{\\\"page_type\\\":\\\"lynx\\\"," +
                        "\\\"render_track_id\\\":\\\"%s\\\",\\\"risk_info\\\":\\\"{\\\\\\\"biometric_params\\\\\\\":\\\\\\\"1\\\\\\\",\\\\\\\"is_jailbreak\\\\\\\":\\\\\\\"2\\\\\\\",\\\\\\\"openudid\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"order_page_style\\\\\\\":0,\\\\\\\"checkout_id\\\\\\\":1,\\\\\\\"ecom_payapi\\\\\\\":true," +
                        "\\\\\\\"ip\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"sub_order_info\\\\\\\":[]}\\\"}\"," +
                        "\"marketing_plan_id\":\"%s\",\"s_type\":\"\",\"entrance_params\":\"" +
                        "{\\\"ecom_scene_id\\\":\\\"%s\\\",\\\"is_groupbuying\\\":0,\\\"extra_campaign_type\\\":\\\"\\\"}\",\"sub_b_type\":\"3\",\"gray_feature\":\"PlatformFullDiscount\",\"sub_way\":0,\"pay_type\":2,\"new_year_festival_scene\":\"buy_again\",\"post_addr\":{\"province\":{},\"city\":{},\"town\":{},\"street\":{\"id\":\"\",\"name\":\"\"}}," +

                        "\"post_tel\":\"%s\",\"address_id\":\"0\"," +
                        "\"price_info\":{\"origin\":%d,\"freight\":0," +
                        "\"coupon\":%d," +
                        "\"pay\":%d,\"packing_charge_amount\":0},\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\"," +
                        "\\\"dev_info\\\":{\\\"reqIp\\\":\\\"%s\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.5.1\\\",\\\"aid\\\":\\\"13\\\",\\\"ua\\\":\\\"com.ss.android.article.news/9100+(Linux;+U;+Android+11;+zh_CN;+MI+9;+Build/RKQ1.200826.002;+Cronet/TTNetVersion:a911d6f2+2022-11-14+QuicVersion:585d7967+2022-11-14)\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\"," +

                        "\\\"deviceId\\\":\\\"%s\\\",\\\"osVersion\\\":\\\"11\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"9.1.0\\\",\\\"appName\\\":\\\"news_article\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"MI+9\\\",\\\"channel\\\":\\\"xiaomi_13_64\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"910\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":" +
                        "\\\"Xiaomi\\\",\\\"iid\\\":\\\"%s\\\",\\\"bioType\\\":\\\"1\\\"},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"voucher_no_list\\\":[],\\\"zg_ext_param\\\":" +
                        "\\\"{\\\\\\\"decision_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\",\\\"jh_ext_info\\\":" +
                        "\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"%s\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":1,\\\"pay_amount_composition\\\":[]}\"," +
                        "\"render_token\":\"%s\",\"win_record_id\":\"\",\"marketing_channel\":\"\",\"identity_card_id\":\"\",\"pay_amount_composition\":[],\"user_account\":{},\"queue_count\":0,\"store_id\":\"\",\"shop_stock_out_handle_infos\":null,\"shop_id\":\"GceCTPIk\"" +

                        ",\"combo_id\":\"%s\",\"combo_num\":1," +
                        "\"product_id\":\"%s\",\"buyer_words\":\"\",\"stock_info\":[{\"stock_type\":1,\"stock_num\":1," +
                        "\"sku_id\":\"%s\",\"warehouse_id\":\"0\"}],\"warehouse_id\":0," +
                        "\"coupon_info\":%s,\"freight_insurance\":false,\"cert_insurance\":false,\"allergy_insurance\":false,\"room_id\":\"\",\"author_id\":\"\",\"content_id\":\"\",\"promotion_id\":\"\"," +
                        "\"ecom_scene_id\":\"%s\",\"shop_user_id\":\"\",\"group_id\":\"\",\"privilege_tag_keys\":[],\"select_privilege_properties\":[],\"platform_deduction_info\":{},\"win_record_info\":{\"win_record_id\":\"\",\"win_record_type\":\"\"}}",
                gidAndShowdPrice.getRender_track_id(), gidAndShowdPrice.getPayIp(), gidAndShowdPrice.getMarketing_plan_id(), gidAndShowdPrice.getEcom_scene_id(),
                gidAndShowdPrice.getPost_tel(), gidAndShowdPrice.getTotal_origin_amount(), gidAndShowdPrice.getTotal_coupon_amount(), gidAndShowdPrice.getTotal_amount(), gidAndShowdPrice.getPayIp(),
                douyinAppCk.getDeviceId(), douyinAppCk.getIid(), gidAndShowdPrice.getDecision_id(), gidAndShowdPrice.getPayapi_cache_id(), gidAndShowdPrice.getRender_token(),
                gidAndShowdPrice.getSku_id(), gidAndShowdPrice.getProduct_id(), gidAndShowdPrice.getSku_id(), counStr, gidAndShowdPrice.getEcom_scene_id()
        );
        return buildData;
    }

    public static String buildCreatepay(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk) {
        String format = String.format("{\"b_type\":3,\"env_type\":\"2\",\"activity_id\":\"\",\"origin_type\":\"0\",\"origin_id\":\"0\",\"new_source_type\":\"0\",\"new_source_id\":\"0\",\"source_type\":\"0\",\"source_id\":\"0\",\"schema\":\"snssdk143://\",\"extra\":\"{}\",\"entrance_params\":\"{}" +
                        "\",\"order_id\":\"%s\",\"sub_way\":0,\"pay_type\":2,\"pay_risk_info\":\"{\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\",\\\"openudid\\\":\\\"\\\",\\\"order_page_style\\\":0,\\\"checkout_id\\\":3,\\\"ecom_payapi\\\":true," +
                        "\\\"ip\\\":\\\"%s\\\"}\",\"pay_amount_composition\":[],\"pay_info\":\"{\\\"sdk_version\\\":\\\"v2\\\"," +
                        "\\\"dev_info\\\":{\\\"reqIp\\\":\\\"%s\\\",\\\"os\\\":\\\"android\\\",\\\"isH5\\\":false,\\\"cjSdkVersion\\\":\\\"6.5.1\\\",\\\"aid\\\":\\\"13\\\",\\\"ua\\\":\\\"com.ss.android.article.news/9100+(Linux;+U;+Android+11;+zh_CN;+MI+9;+Build/RKQ1.200826.002;+Cronet/TTNetVersion:a911d6f2+2022-11-14+QuicVersion:585d7967+2022-11-14)\\\",\\\"riskUa\\\":\\\"\\\",\\\"lang\\\":\\\"zh-Hans\\\"," +
                        "\\\"deviceId\\\":\\\"%s\\\",\\\"osVersion\\\":\\\"11\\\",\\\"vendor\\\":\\\"\\\",\\\"model\\\":\\\"\\\",\\\"netType\\\":\\\"\\\",\\\"appVersion\\\":\\\"9.1.0\\\",\\\"appName\\\":\\\"news_article\\\",\\\"devicePlatform\\\":\\\"android\\\",\\\"deviceType\\\":\\\"MI+9\\\",\\\"channel\\\":\\\"xiaomi_13_64\\\",\\\"openudid\\\":\\\"\\\",\\\"versionCode\\\":\\\"910\\\",\\\"ac\\\":\\\"wifi\\\",\\\"brand\\\":\\\"Xiaomi\\\"," +
                        "\\\"iid\\\":\\\"%s\\\",\\\"bioType\\\":\\\"1\\\"},\\\"bank_card_info\\\":{},\\\"credit_pay_info\\\":{\\\"installment\\\":\\\"1\\\"},\\\"zg_ext_param\\\"" +
                        ":\\\"{\\\\\\\"decision_id\\\\\\\":\\\\\\\"%s\\\\\\\",\\\\\\\"qt_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\",\\\\\\\"retain_c_pay_url\\\\\\\":\\\\\\\"\\\\\\\"}\\\",\\\"voucher_no_list\\\":[],\\\"jh_ext_info\\\":\\\"{\\\\\\\"payapi_cache_id\\\\\\\":\\\\\\\"xxxxx\\\\\\\"}\\\",\\\"sub_ext\\\":\\\"\\\",\\\"biometric_params\\\":\\\"1\\\",\\\"is_jailbreak\\\":\\\"2\\\",\\\"checkout_id\\\":3,\\\"pay_amount_composition\\\":[]}\"}",
                gidAndShowdPrice.getOrderId(), gidAndShowdPrice.getPayIp(), gidAndShowdPrice.getPayIp(), douyinAppCk.getDeviceId(), douyinAppCk.getIid(), gidAndShowdPrice.getDecision_id()
        );
        return format;
    }


}
