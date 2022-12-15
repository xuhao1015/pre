package com.xd.pre.dy23;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.common.utils.px.dto.UrlEntity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.HashMap;
import java.util.Map;

public class InserData {

    public static Db db = Db.use();

    public static void main(String[] args) throws Exception {
        String url = "https://ken.snssdk.com/order/buyRender?b_type_new=3&sub_b_type=13&ecom_appid=7386&webcast_appid=6822&live_request_from_jsb=1&live_sdk_version=912&webcast_sdk_version=2120&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&webcast_app_id=6822&app_name=news_article&openlive_personal_recommend=1&device_platform=android&os=android&ssmix=a&_rticket=1671079140758&cdid=1eb38d33-632f-4846-8f61-8e14fba747c6&channel=xiaomi_13_64&aid=13&version_code=912&version_name=9.1.2&manifest_version_code=9120&update_version_code=91209&ab_version=1859936%2C668774%2C5175916%2C662176%2C5175909%2C662099%2C5175875%2C660830%2C5107937%2C5175925%2C5213382%2C668779%2C5175922%2C668776%2C5175917%2C668775%2C5175927%2C4697194%2C5182109%2C5186988&ab_feature=94563%2C102749&resolution=1080*2135&dpi=440&device_type=MI+9&device_brand=Xiaomi&language=zh&os_api=29&os_version=10&ac=wifi&dq_param=0&plugin=0&client_vid=3383553%2C5019683&isTTWebView=0&session_id=d8c9f57e-bea4-4992-b399-54ff04023849&host_abi=arm64-v8a&tma_jssdk_version=2.53.0&rom_version=miui_v12_v12.0.6.0.qfacnxm&immerse_pool_type=101&iid=1157139784800461&device_id=1086768878988638";
        String ck = "store-region-src=did; store-region=cn-sc; passport_csrf_token=9657bb0acb71aa646b5428ff090c7e57; passport_csrf_token_default=9657bb0acb71aa646b5428ff090c7e57; install_id=1157139784800461; ttreq=1$6914fa44819269a089706936d23ace699883e56c; n_mh=1hokyxE5929bz5tSK_OctjBN8uDAtFkjzEFGy6N5g6Q; d_ticket=1cb7fcf070ced2f9c3f9c22f39e66d8cd1a34; sid_guard=9ac0514b32e345ebc4a4cf51eb37989c%7C1671040142%7C5184000%7CSun%2C+12-Feb-2023+17%3A49%3A02+GMT; uid_tt=fcd9ad9fb9b3d4b02fdad2d6917b7eb9; uid_tt_ss=fcd9ad9fb9b3d4b02fdad2d6917b7eb9; sid_tt=9ac0514b32e345ebc4a4cf51eb37989c; sessionid=9ac0514b32e345ebc4a4cf51eb37989c; sessionid_ss=9ac0514b32e345ebc4a4cf51eb37989c; WIN_WH=393_804; PIXIEL_RATIO=2.75; FRM=new; odin_tt=5317c9c406cf3dcb9293fa2048630dced17c64cc0e1dc4f1828d2c2165fb4d330eb0fe197ae8234842dd43b615c9fed440b5d2edec3b1c2dc29e773e2174923699f1b1a50e44a6f830408802b185f8a1; msToken=u4Zr6s6o70A9_nMPg377XmhFbBPYF89wmt2k21T0-UhczIyFwyepOA5fYNrWuHlAc6IaqiaIqO8o24zHxTpLubhrsRfgzzyBARh23BC-jyA=";
        UrlEntity urlEntity = PreUtils.parseUrl(url);
        Map<String, String> params = urlEntity.getParams();
        String device_id = params.get("device_id").toString();
        String iid = params.get("iid").toString();
        Map<String, String> ex_param = new HashMap<>();
        for (String s : params.keySet()) {
            if (s.equals("device_id") || s.equals("iid")) {
                continue;
            }
            if (ObjectUtil.isNotNull(params.get(s)) && StrUtil.isNotBlank(params.get(s))) {
                ex_param.put(s, params.get(s));
            }
        }
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://ecom.snssdk.com/aweme/v2/commerce/mall/favorite/feed?count=1")
                .get()
                .addHeader("Cookie", ck)
                .addHeader("cache-control", "no-cache")
                .build();
        Response response = client.newCall(request).execute();
        JSONArray item_cards = JSON.parseObject(response.body().string()).getJSONArray("item_cards");
        String recommend_info = JSON.parseObject(item_cards.get(0).toString()).getJSONObject("product").getJSONObject("recommend_info").toString();
        String uid = JSON.parseObject(recommend_info).getString("uid").trim();
        System.err.println(uid);
        db.execute(" insert  INTO  douyin_app_ck  (uid, device_id ,iid,ex_param,ck ,tenant_id) VALUES (?,?,?,?,?,?)", uid, device_id, iid, JSON.toJSONString(ex_param), ck, 1);

    }
}
