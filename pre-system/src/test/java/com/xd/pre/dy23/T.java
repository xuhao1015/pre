package com.xd.pre.dy23;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
@Slf4j
public class T {
    private static void getSearchPackDetail(String device_id, String iid, GidAndShowdPrice gidAndShowdPrice, String ck, OkHttpClient client) {
        try {
            gidAndShowdPrice.setGid("3586219653703310433");
            Long _rticket = System.currentTimeMillis();
            String pack_url = "https://ecom3-normal-hl.ecombdapi.com/aweme/v2/shop/promotion/pack/detail/?iid=" + iid + "&device_id=" + device_id + "&ac=wifi&channel=aweGW&aid=1128&app_name=aweme&version_code=230400&version_name=23.4.0&device_platform=android&os=android&ssmix=a&device_type=Redmi+8A&device_brand=Xiaomi&language=zh&os_api=28&os_version=9&manifest_version_code=230401&resolution=720*1369&dpi=320&update_version_code=23409900&_rticket=" + _rticket + "&package=com.ss.android.ugc.aweme&mcc_mnc=46001&cpu_support64=false&host_abi=armeabi-v7a&is_guest_mode=0&app_type=normal&minor_status=0&appTheme=light&need_personal_recommend=1&is_android_pad=0&ts=" + _rticket / 1000 + "&cdid=0bce1d89-65cb-4885-ae6d-527ae9a1143c&md=0";
            String pack_body = "promotion_id=" +
                    gidAndShowdPrice.getGid() + "&enter_from=search_order_center&meta_param=%7B%22entrance_info%22%3A%22%7B%5C%22carrier_source%5C%22%3A%5C%22search_order_center%5C%22%2C%5C%22source_method%5C%22%3A%5C%22product_card%5C%22%2C%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22product_activity_type%5C%22%3A%5C%22nonactivity%5C%22%2C%5C%22EVENT_ORIGIN_FEATURE%5C%22%3A%5C%22TEMAI%5C%22%2C%5C%22search_params%5C%22%3A%7B%5C%22search_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getSearch_id() + "%5C%22%2C%5C%22search_passthrough_params%5C%22%3A%5C%22%7B%5C%5C%5C%22previous_page%5C%5C%5C%22%3A%5C%5C%5C%22personal_homepage%5C%5C%5C%22%7D%5C%22%2C%5C%22search_result_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getGid() + "%5C%22%7D%2C%5C%22search_keyword%5C%22%3A%5C%22%E6%8A%96%E9%9F%B3%E8%8B%B9%E6%9E%9C%E5%8D%A1%5C%22%2C%5C%22ecom_scene_id%5C%22%3A%5C%22" +
                    gidAndShowdPrice.getEcom_scene_id() + "%5C%22%2C%5C%22ecom_group_type%5C%22%3A%5C%22%5C%22%2C%5C%22card_status%5C%22%3A%5C%22%5C%22%2C%5C%22module_label%5C%22%3A%5C%22%5C%22%2C%5C%22ecom_icon%5C%22%3A%5C%22%5C%22%7D%22%7D&goods_content_shrink=720_-1";

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
            log.info("getSearchPackDetail：{}", packResBody);
        } catch (Exception e) {
            log.error("点击查询结果详情报错:{}", e.getMessage());
        }
    }
}
