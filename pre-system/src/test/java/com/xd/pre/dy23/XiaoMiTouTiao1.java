package com.xd.pre.dy23;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.nosql.redis.RedisDS;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.px.douyin.buyRender.res.BuyRenderRoot;
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import com.xd.pre.modules.px.douyin.pay.PayDto;
import com.xd.pre.modules.px.douyin.toutiao.BuildDouYinUrlUtils;
import com.xd.pre.modules.px.douyin.toutiao.BuyRenderParam;
import com.xd.pre.modules.px.douyin.toutiao.SearchParam;
import com.xd.pre.modules.sys.domain.DouyinAppCk;
import com.xd.pre.modules.sys.domain.DouyinMethodNameParam;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Slf4j
public class XiaoMiTouTiao1 {

    public static Db db = Db.use();
    public static Jedis jedis = RedisDS.create().getJedis();


    public static DouyinMethodNameParam getDouyinParam(String method_name) throws Exception {
        Entity method_detailInfo = db.use().queryOne("select * from douyin_method_name_param where method_name=?", method_name);

        DouyinMethodNameParam methodNamemethod_postExec = DouyinMethodNameParam.builder().methodName(method_detailInfo.getStr("method_name"))
                .methodParam(method_detailInfo.getStr("method_param")).methodUrl(method_detailInfo.getStr("method_url")).build();
        return methodNamemethod_postExec;
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 2; i++) {
            String uid = "3250607754582456";
            Entity appCk = db.use().queryOne("select * from douyin_app_ck where uid=?", uid);

            Entity method_db = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "search");
            Entity method_pack = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "pack");
            Entity method_buyRender = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "buyRender");
            Entity method_newcreate = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "newcreate");
            Entity method_createpay = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "createpay");
            Entity method_detailInfo = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "detailInfo");
            Entity method_postExec = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "postExec");

            String post_tel = PreUtils.getTel();
            Integer price = 200;
            Entity jd_app_store_config_entity = db.queryOne("select * from jd_app_store_config where group_num =8 and is_product =1 and sku_price  = ? ", price);

            OkHttpClient client = new OkHttpClient();
            DouyinAppCk douyinAppCk = DouyinAppCk.builder().uid(uid).exParam(appCk.getStr("ex_param")).ck(appCk.getStr("ck"))
                    .deviceId(appCk.getStr("device_id")).iid(appCk.getStr("iid")).build();

            DouyinMethodNameParam methodNameSearch = DouyinMethodNameParam.builder().methodName(method_db.getStr("method_name")).methodUrl(method_db.getStr("method_url")).build();

            DouyinMethodNameParam methodNamePack = DouyinMethodNameParam.builder().methodName(method_pack.getStr("method_name"))
                    .methodParam(method_pack.getStr("method_param")).methodUrl(method_pack.getStr("method_url")).build();


            DouyinMethodNameParam methodNameBuyRender = DouyinMethodNameParam.builder().methodName(method_buyRender.getStr("method_name"))
                    .methodParam(method_buyRender.getStr("method_param")).methodUrl(method_buyRender.getStr("method_url")).build();

            DouyinMethodNameParam methodNameCreatenew = DouyinMethodNameParam.builder().methodName(method_newcreate.getStr("method_name"))
                    .methodParam(method_newcreate.getStr("method_param")).methodUrl(method_newcreate.getStr("method_url")).build();

            DouyinMethodNameParam methodNameCreatePay = DouyinMethodNameParam.builder().methodName(method_createpay.getStr("method_name"))
                    .methodParam(method_createpay.getStr("method_param")).methodUrl(method_createpay.getStr("method_url")).build();


            DouyinMethodNameParam methodNameDetailInfo = DouyinMethodNameParam.builder().methodName(method_detailInfo.getStr("method_name"))
                    .methodParam(method_detailInfo.getStr("method_param")).methodUrl(method_detailInfo.getStr("method_url")).build();

            DouyinMethodNameParam methodNamemethod_postExec = DouyinMethodNameParam.builder().methodName(method_postExec.getStr("method_name"))
                    .methodParam(method_postExec.getStr("method_param")).methodUrl(method_postExec.getStr("method_url")).build();

            //TODO
            GidAndShowdPrice gidAndShowdPrice = new GidAndShowdPrice();
            gidAndShowdPrice.setPost_tel(post_tel);
            gidAndShowdPrice.setPayIp("182.147.57.114");
            gidAndShowdPrice.setEcom_scene_id("1031,1041");
            JSONObject config = JSON.parseObject(jd_app_store_config_entity.getStr("config"));
            gidAndShowdPrice.setProduct_id(config.getString("product_id"));
            gidAndShowdPrice.setSku_id(config.getString("sku_id"));
            gidAndShowdPrice = buildBuRender(gidAndShowdPrice, douyinAppCk, client, methodNameBuyRender);
            gidAndShowdPrice = buildCreateOrder(gidAndShowdPrice, douyinAppCk, client, methodNameCreatenew);
            if (ObjectUtil.isNull(gidAndShowdPrice)) {
                log.info("=================订单报错");
                return;
            }
            log.info("开始入库:{}", gidAndShowdPrice.getOrderId());
            DateTime dateTime = DateUtil.offsetMinute(new Date(), 25);
            PayDto payDto = PayDto.builder().ck(PreAesUtils.encrypt加密(douyinAppCk.getCk())).device_id(douyinAppCk.getDeviceId()).iid(douyinAppCk.getIid()).pay_type(2 + "")
                    .orderId(gidAndShowdPrice.getOrderId()).userIp(gidAndShowdPrice.getPayIp()).build();
            db.execute("INSERT INTO jd_order_pt (order_id,pt_pin,expire_time,create_time,sku_price,sku_name,sku_id,wx_pay_expire_time,current_ck,mark, " +
                            "tenant_id)VALUES( ?,?,?,?,?,?,?,?,?,?,?)", gidAndShowdPrice.getOrderId(), uid, dateTime, new Date(),
                    jd_app_store_config_entity.getBigDecimal("sku_price"), jd_app_store_config_entity.getStr("sku_name"),
                    jd_app_store_config_entity.getStr("sku_id"), dateTime, douyinAppCk.getCk(), JSON.toJSONString(payDto), 1);
            log.info("订单入库成功");
            if (true) {
                return;
            }
            gidAndShowdPrice = buildCreatepay(gidAndShowdPrice, douyinAppCk, client, methodNameCreatePay);
            String a = buildDetailInfo(gidAndShowdPrice, douyinAppCk, client, methodNameDetailInfo);
            gidAndShowdPrice.setAction_id("100030");
            gidAndShowdPrice = buildPostExec(gidAndShowdPrice, douyinAppCk, client, methodNamemethod_postExec);
            gidAndShowdPrice.setAction_id("100040");
            gidAndShowdPrice = buildPostExec(gidAndShowdPrice, douyinAppCk, client, methodNamemethod_postExec);
            Thread.sleep(10 * 10000);
        }

    }

    public static GidAndShowdPrice buildPostExec(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNamemethod_postExec) {
        BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String postExec_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNamemethod_postExec, douyinAppCk);
        String datafromOri = "common_params=%7B%22enter_from%22%3A%22order_list_page%22%2C%22previous_page%22%3A%22mine_tab_order_list__order_homepage%22%7D&action_id=" + gidAndShowdPrice.getAction_id()
                + "&business_line=2&trade_type=0&source=1&ecom_appid=7386&lynx_support_version=1&order_id=" + gidAndShowdPrice.getOrderId() + "&page_size=15";
        RequestBody body = RequestBody.create(mediaType, datafromOri);
        try {
            Request.Builder builder = new Request.Builder();
            Request request_create = builder.url(postExec_url)
                    .post(body)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .build();
            Response execute = client.newCall(request_create).execute();
            String deleteDataRes = execute.body().string();
            log.info("删除数据结果:{}", deleteDataRes);
            return null;
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;

    }

    public static String buildDetailInfo(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameDetailInfo) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String newcreate_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameDetailInfo, douyinAppCk) + "&order_id=" + gidAndShowdPrice.getOrderId();
            Request.Builder builder = new Request.Builder();
            Request request_create = builder.url(newcreate_url)
                    .get()
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .build();
            Response execute = client.newCall(request_create).execute();
            String buildDetailInfodata = execute.body().string();
            log.info("支付详情:{}", buildDetailInfodata);
            return buildDetailInfodata;
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;
    }

    public static GidAndShowdPrice buildCreatepay(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameCreatenew) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String newcreate_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameCreatenew, douyinAppCk);
            String newcreate_body = BuildDouYinUrlUtils.buildCreatepay(gidAndShowdPrice, douyinAppCk);
            log.info("请求参数:{}", newcreate_body);
            String create_md5 = SecureUtil.md5("json_form=" + URLEncoder.encode(newcreate_body)).toUpperCase();
            String create_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    create_md5, douyinAppCk.getDeviceId(), newcreate_url
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
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", create_md5)
                    //TODO
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", create_x_gorgon)
                    .addHeader("X-Khronos", create_x_khronos)
                    .build();
            Response execute = client.newCall(request_create).execute();
            String createbody = execute.body().string();
            log.info("支付数据数据:{}", createbody);
            if (StrUtil.isNotBlank(createbody) && createbody.contains("order_id")) {
                String orderId = JSON.parseObject(createbody).getJSONObject("data").getString("order_id");
                gidAndShowdPrice.setOrderId(orderId);
                return gidAndShowdPrice;
            }
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;
    }

    public static GidAndShowdPrice buildCreateOrder(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameCreatenew) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String newcreate_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameCreatenew, douyinAppCk);
            //TODO
            String newcreate_body = BuildDouYinUrlUtils.buildCreatenew(gidAndShowdPrice, douyinAppCk);
            log.info("请求参数:{}", newcreate_body);
            String create_md5 = SecureUtil.md5("json_form=" + URLEncoder.encode(newcreate_body)).toUpperCase();
            String create_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    create_md5, douyinAppCk.getDeviceId(), newcreate_url
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
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", create_md5)
                    //TODO
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", create_x_gorgon)
                    .addHeader("X-Khronos", create_x_khronos)
                    .build();
            Response execute = client.newCall(request_create).execute();
            String createbody = execute.body().string();
            log.info("下单数据:{}", createbody);
            if (StrUtil.isNotBlank(createbody) && createbody.contains("order_id")) {
                String orderId = JSON.parseObject(createbody).getJSONObject("data").getString("order_id");
                gidAndShowdPrice.setOrderId(orderId);
                return gidAndShowdPrice;
            }
        } catch (Exception e) {
            log.error("创建订单报错:{}", e.getMessage());
        }
        return null;
    }

    public static GidAndShowdPrice buildBuRender(GidAndShowdPrice gidAndShowdPrice, DouyinAppCk douyinAppCk, OkHttpClient client, DouyinMethodNameParam methodNameBuyRender) {
        try {
            BuyRenderParam buyRenderParam = BuyRenderParam.buildBuyRenderParam();
            String buyRenderUrl = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(buyRenderParam)), methodNameBuyRender, douyinAppCk);
            String buyRenderBody = BuildDouYinUrlUtils.buildBuyRender(gidAndShowdPrice);
            String buyRenderMd5 = SecureUtil.md5("json_form=" + URLEncoder.encode(buyRenderBody)).toUpperCase();

            String pack_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                    buyRenderMd5, douyinAppCk.getDeviceId(), buyRenderUrl
            );
            String pack_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(pack_body_sign).execute().body();
            String buyRender_x_gorgon = JSON.parseObject(pack_sign_body).getString("x-gorgon");
            String buyRender_x_khronos = JSON.parseObject(pack_sign_body).getString("x-khronos");
            RequestBody requestBody1 = new FormBody.Builder()
                    .add("json_form", buyRenderBody)
                    .build();
            Request.Builder builder = new Request.Builder();
            Request requestBuyRender = builder.url(buyRenderUrl)
                    .post(requestBody1)
                    .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                    .addHeader("X-SS-STUB", buyRenderMd5)
                    .addHeader("User-Agent", "com.ss.android.article.news/8960 (Linux; U; Android 10; zh_CN; PACT00; Build/QP1A.190711.020; Cronet/TTNetVersion:68deaea9 2022-07-19 QuicVersion:12a1d5c5 2022-06-22)")
                    .addHeader("X-Gorgon", buyRender_x_gorgon)
                    .addHeader("X-Khronos", buyRender_x_khronos)
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

            log.info("处理价格");
            JSONObject total_price_result_json = JSON.parseObject(buyRenderbody).getJSONObject("data").getJSONObject("total_price_result");
            Integer total_amount = total_price_result_json.getInteger("total_amount");
            gidAndShowdPrice.setTotal_amount(total_amount);
            Integer total_origin_amount = total_price_result_json.getInteger("total_origin_amount");
            gidAndShowdPrice.setTotal_origin_amount(total_origin_amount);
            Integer total_coupon_amount = total_price_result_json.getInteger("total_coupon_amount");
            gidAndShowdPrice.setTotal_coupon_amount(total_coupon_amount);

            if (total_origin_amount.intValue() != total_amount.intValue()) {
                log.info("当前数据有优惠卷");
                JSONObject coupon_info = total_price_result_json.getJSONObject("shop_sku_map").getJSONObject("GceCTPIk").getJSONObject("sku_list")
                        .getJSONObject(gidAndShowdPrice.getSku_id()).getJSONObject("shop_discount").getJSONObject("coupon_info");
                String coupon_info_id = coupon_info.getString("id");
                String coupon_meta_id = coupon_info.getString("coupon_meta_id");
                gidAndShowdPrice.setCoupon_info_id(coupon_info_id);
                gidAndShowdPrice.setCoupon_meta_id(coupon_meta_id);
            }
            return gidAndShowdPrice;
        } catch (Exception e) {
            log.error("预下单报错msg:{}", e.getMessage());
        }
        return null;
    }

    public static GidAndShowdPrice buildPackSkuData(OkHttpClient client, DouyinAppCk douyinAppCk, DouyinMethodNameParam methodNamePack, GidAndShowdPrice gidAndShowdPrice) {
        for (int i = 0; i < 100; i++) {
            try {
                String pack_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(null, methodNamePack, douyinAppCk);
                String pack_body = BuildDouYinUrlUtils.buildPackPostData(methodNamePack, gidAndShowdPrice);
                FormBody formBody = BuildDouYinUrlUtils.formPack(methodNamePack, gidAndShowdPrice);
                // pack_body="user_id=_00017yEywhDwoBBVLzWnw_W7HF2_U73NLhJ&sec_user_id=&author_id=&author_open_id=&sec_author_id=&promotion_ids=3586218895557658254&item_id=&enter_from=search_order_center&meta_param={\"entrance_info\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"search_params\\\":{\\\"search_id\\\":\\\"20221209130101010142041019267C0D92\\\",\\\"search_result_id\\\":\\\"3586218895557658254\\\"},\\\"source_method\\\":\\\"product_card\\\",\\\"source_page\\\":\\\"search_order_center\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\"}\"}&width=1080&height=1080&rank_id=&use_new_price=1&gps_on=0&bff_type=1&ui_params={\"action_type\":\"\",\"carrier_source\":\"search_order_center\",\"client_abs\":\"{\\\"iesec_new_goods_detail_edition\\\":6,\\\"iesec_detail_head_search_plan\\\":-1}\",\"ecom_entrance_form\":\"open_url\",\"enter_method\":\"\",\"entrance_info\":\"{\\\"carrier_source\\\":\\\"search_order_center\\\",\\\"search_params\\\":{\\\"search_id\\\":\\\"20221209130101010142041019267C0D92\\\",\\\"search_result_id\\\":\\\"3586218895557658254\\\"},\\\"source_method\\\":\\\"product_card\\\",\\\"source_page\\\":\\\"search_order_center\\\",\\\"ecom_group_type\\\":\\\"\\\",\\\"card_status\\\":\\\"\\\",\\\"module_label\\\":\\\"\\\",\\\"ecom_icon\\\":\\\"\\\"}\",\"follow_status\":0,\"font_scale\":1.0,\"from_live\":false,\"from_video\":false,\"full_mode\":true,\"iesec_new_goods_detail_edition\":6,\"icon_type\":\"\",\"is_luban\":false,\"is_recommend_enable\":true,\"is_short_screen\":false,\"window_reposition\":false,\"large_font_scale\":false,\"live_room_id\":\"\",\"module_name\":\"\",\"page_id\":\"\",\"product_id\":\"\",\"promotion_id\":\"3586218895557658254\",\"show_sku_panel\":0,\"source_method\":\"product_card\",\"source_page\":\"search_order_center\",\"three_d_log_data\":\"\",\"useful_screen_width\":392,\"v3_events_additions\":\"{\\\"search_params\\\":{\\\"search_id\\\":\\\"20221209130101010142041019267C0D92\\\",\\\"search_result_id\\\":\\\"3586218895557658254\\\"}}\"}&user_avatar_shrink=132_132&goods_header_shrink=1080_1080&goods_comment_shrink=464_464&shop_avatar_shrink=101_101&common_large_shrink=3240_3240&ecom_scene_id=&is_preload_req=false";
                log.info("pack_url：{},buildPackPostData:{}", pack_url, pack_body);
                String packMd5 = SecureUtil.md5(pack_body).toUpperCase();
                String pack_body_sign = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"%s\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                        packMd5, douyinAppCk.getDeviceId(), pack_url
                );
                String pack_sign_body = HttpRequest.post("http://1.15.184.191:8292/dy22").body(pack_body_sign).execute().body();
                String pack_x_gorgon = JSON.parseObject(pack_sign_body).getString("x-gorgon");
                String pack_x_khronos = JSON.parseObject(pack_sign_body).getString("x-khronos");
                String pack_x_argus = JSON.parseObject(pack_sign_body).getString("x-argus");
                String pack_x_ladon = JSON.parseObject(pack_sign_body).getString("x-ladon");
//                MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
//                RequestBody packRqform = RequestBody.create(mediaType, pack_body);


                Request request = new Request.Builder()
                        .url(pack_url)
                        .post(formBody)
                        .addHeader("X-SS-STUB", packMd5)
                        .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                        .addHeader("X-Gorgon", pack_x_gorgon)
                        .addHeader("X-Khronos", pack_x_khronos)
                        //TODO 缺少请求头的设置
                        .addHeader("user-agent", "com.ss.android.ugc.aweme/200001 (Linux; U; Android 9; zh_CN; Redmi 8A; Build/PKQ1.190319.001; Cronet/TTNetVersion:3a37693c 2022-02-10 QuicVersion:775bd845 2021-12-24)")
                        .build();
                Response searchResponse = client.newCall(request).execute();
                String packResBody = searchResponse.body().string();
                log.info("packResBody：{}", packResBody);
                String h5_url = JSON.parseObject(JSON.toJSONString(JSON.parseObject(packResBody).getJSONArray("promotions").get(PreConstant.ZERO))).getJSONObject("base_info").getJSONObject("links").getString("h5_url");
                Map<String, String> params = PreUtils.parseUrl(h5_url).getParams();
                BeanUtil.copyProperties(params, gidAndShowdPrice);
                if (StrUtil.isNotBlank(gidAndShowdPrice.getCombo_id())) {
                    return gidAndShowdPrice;
                }
            } catch (Exception e) {
                log.error("buildPackSkuData错误:{}", e.getMessage());
            }
        }
        return null;

    }

    public static List<GidAndShowdPrice> getGidAndShowdPrices(Integer buyPrice, OkHttpClient client, DouyinAppCk douyinAppCk, DouyinMethodNameParam methodNameSearch) throws IOException {
        SearchParam searchParam = SearchParam.buildSearchParam("苹果充值卡" + buyPrice);
        String search_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(JSON.parseObject(JSON.toJSONString(searchParam)), methodNameSearch, douyinAppCk);
        log.info("查询链接:{}", search_url);
        Request request = new Request.Builder()
                .url(search_url)
                .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                .addHeader("Referer", "https://is.snssdk.com/search/?inner_resolution=1080*2400&search_sug=1")
                .addHeader("user-agent", "Mozilla/5.0 (Linux; Android 12; PGBM10 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/97.0.4692.98 Mobile Safari/537.36 JsSdk/2 NewsArticle/9.1.0 NetType/wifi")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response searchResponse = client.newCall(request).execute();
        String searchResBody = searchResponse.body().string();
        Document serach_document = Jsoup.parse(searchResBody);
        Elements elementsByClass = serach_document.getElementsByClass("l-view block l-flex bg-card flex flex-col");
        List<GidAndShowdPrice> gidAndShowdPriceList = new ArrayList();
        for (Element byClass : elementsByClass) {
            String data_log_extra = byClass.attr("data-log-extra");
            String cr_params = byClass.attr("cr-params");
            try {
                JSONObject data_log_extra_json = JSON.parseObject(data_log_extra);
                JSONObject cr_params_json = JSON.parseObject(cr_params);
                if (ObjectUtil.isNotNull(data_log_extra) && StrUtil.isNotBlank(data_log_extra_json.getString("search_result_id")) && ObjectUtil.isNotNull(cr_params_json.getString("title"))) {
                    String title = cr_params_json.getString("title");
                    String show_price = title.replace("App Store 充值卡 ", "").replace("元（电子卡）- Apple ID 充值 / iOS 充值", "");
                    if (Integer.valueOf(show_price).intValue() != buyPrice.intValue()) {
                        continue;
                    }
                    GidAndShowdPrice gidAndShowdPrice = GidAndShowdPrice.builder().search_id(data_log_extra_json.getString("search_id")).gid(cr_params_json.getString("gid"))
                            .query_id(data_log_extra_json.getString("query_id")).title(title)
                            .show_price(Integer.valueOf(show_price))
                            .build();
                    gidAndShowdPriceList.add(gidAndShowdPrice);
                }
            } catch (Exception e) {
                log.error("=================");
            }
        }
        return gidAndShowdPriceList;
    }
}
