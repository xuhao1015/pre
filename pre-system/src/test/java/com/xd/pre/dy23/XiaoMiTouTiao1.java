package com.xd.pre.dy23;

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
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import com.xd.pre.modules.px.douyin.toutiao.BuildDouYinUrlUtils;
import com.xd.pre.modules.px.douyin.toutiao.SearchParam;
import com.xd.pre.modules.sys.domain.DouyinAppCk;
import com.xd.pre.modules.sys.domain.DouyinMethodNameParam;
import com.xd.pre.modules.sys.domain.JdMchOrder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class XiaoMiTouTiao1 {

    public static Db db = Db.use();
    public static Jedis jedis = RedisDS.create().getJedis();


    public static void main(String[] args) throws Exception {
        String uid = "3250607754582456";


        Entity appCk = db.use().queryOne("select * from douyin_app_ck_all_param where uid=?", uid);
        Entity method_db = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "search");
        Entity method_pack = db.use().queryOne("select * from douyin_method_name_param where method_name=?", "pack");

        String payIp = PreUtils.getRandomIp();
        String post_tel = PreUtils.getTel();
        Integer buyPrice = 200;

        OkHttpClient client = new OkHttpClient();
        DouyinAppCk douyinAppCk = DouyinAppCk.builder().uid(uid).exParam(appCk.getStr("ex_param")).ck(appCk.getStr("ck"))
                .deviceId(appCk.getStr("device_id")).iid(appCk.getStr("iid")).build();

        DouyinMethodNameParam methodNameSearch = DouyinMethodNameParam.builder().method_name(method_db.getStr("method_name")).method_url(method_db.getStr("method_url")).build();

        DouyinMethodNameParam methodNamePack = DouyinMethodNameParam.builder().method_name(method_pack.getStr("method_name"))
                .method_param(method_pack.getStr("method_param")).method_url(method_pack.getStr("method_url")).build();

        JdMchOrder jdMchOrder = JdMchOrder.builder().tradeNo("202211111112222").outTradeNo("P123456456").build();
        log.info("执行,当前订单号:{}ck的uid:{},device:{},iid:{} ", jdMchOrder.getTradeNo(), douyinAppCk.getUid(), douyinAppCk.getDeviceId(), douyinAppCk.getIid());
        log.info("开始执行搜索:{}", jdMchOrder.getTradeNo());
        List<GidAndShowdPrice> gidAndShowdPriceList = getGidAndShowdPrices(buyPrice, client, douyinAppCk, methodNameSearch);
        log.info(JSON.toJSONString(gidAndShowdPriceList));
        //TODO
        GidAndShowdPrice gidAndShowdPrice = gidAndShowdPriceList.get(0);


        String pack_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(null, methodNamePack, douyinAppCk);
        String pack_body = BuildDouYinUrlUtils.buildPackPostData(methodNamePack, gidAndShowdPrice);

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

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody packRqform = RequestBody.create(mediaType, pack_body);
        Request request = new Request.Builder()
                .url(pack_url)
                .post(packRqform)
                .addHeader("X-SS-STUB", packMd5)
                .addHeader("Cookie", PreAesUtils.decrypt解密(douyinAppCk.getCk()))
                .addHeader("X-Gorgon", pack_x_gorgon)
                .addHeader("X-Khronos", pack_x_khronos)
                .addHeader("X-Argus", pack_x_argus)
                .addHeader("X-Ladon", pack_x_ladon)
                //TODO 缺少请求头的设置
                .addHeader("user-agent", "com.ss.android.ugc.aweme/200001 (Linux; U; Android 9; zh_CN; Redmi 8A; Build/PKQ1.190319.001; Cronet/TTNetVersion:3a37693c 2022-02-10 QuicVersion:775bd845 2021-12-24)")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .build();
        Response searchResponse = client.newCall(request).execute();
        String packResBody = searchResponse.body().string();
        log.info("packResBody：{}", packResBody);

    }

    private static List<GidAndShowdPrice> getGidAndShowdPrices(Integer buyPrice, OkHttpClient client, DouyinAppCk douyinAppCk, DouyinMethodNameParam methodNameSearch) throws IOException {
        SearchParam searchParam = SearchParam.buildSearchParam("苹果充值卡" + buyPrice);
        String search_url = BuildDouYinUrlUtils.buildSearchAndPackUrl(searchParam, methodNameSearch, douyinAppCk);
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
