package com.xd.pre.dy23;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.common.utils.px.dto.UrlEntity;
import okhttp3.*;

import java.util.HashMap;
import java.util.Map;

public class InserData {

    public static Db db = Db.use();

    public static void main(String[] args)throws Exception {
        String url = "https://ken.snssdk.com/order/buyRender?b_type_new=3&sub_b_type=13&ecom_appid=7386&webcast_appid=6822&live_request_from_jsb=1&live_sdk_version=910&webcast_sdk_version=2120&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=2&webcast_app_id=6822&app_name=news_article&openlive_personal_recommend=1&device_platform=android&os=android&ssmix=a&_rticket=1670351635055&cdid=587713e0-2c73-45dd-aa5e-85e9cd10b401&channel=update&aid=13&version_code=910&version_name=9.1.0&manifest_version_code=9095&update_version_code=91006&ab_version=1859937%2C668779%2C5175922%2C668774%2C5175916%2C668776%2C5175917%2C662176%2C5175909%2C662099%2C5175875%2C660830%2C5107919%2C5114922%2C5175925%2C668775%2C4091914%2C4407627%2C5158114%2C5175927%2C3746951%2C5179111%2C5082917%2C5092860%2C5113788%2C5181070&ab_group=94565%2C102751&ab_feature=94563%2C102749&resolution=1080*2245&dpi=480&device_type=PGBM10&device_brand=OPPO&language=zh&os_api=31&os_version=12&ac=wifi&dq_param=0&plugin=0&isTTWebView=0&session_id=a112205b-ed5e-488c-a878-096e2fb089a6&host_abi=armeabi-v7a&tma_jssdk_version=2.53.0&rom_version=coloros__pgbm10_11_a.26&immerse_pool_type=101&iid=4464468410775031&device_id=805293711950046";
        String         ck = "store-region-src=did; store-region=cn-sc; install_id=4464468410775031; ttreq=1$ed38a851458de89f9c4d9765994f134ef431758f; passport_csrf_token=d562c7889e80fd931bcad9d97dd18cc1; passport_csrf_token_default=d562c7889e80fd931bcad9d97dd18cc1; odin_tt=cace92ad1530d8b3a031508ee6c0debc584e4ee31ec5705593fc0ca8feaf4677f2bde543aa8e3e91883b9846852342f94ce1999f45ad36090b60ccadfad143848cb1f8a9445e2c9d7afdeac48f479b32; n_mh=NxhccezDNSfWT7XZ9_SYiJM7GCOhj7lIOkzeDu7Rg2M; d_ticket=7dd988ab2d0337b067091d8748866f2c9ae3d; sid_guard=1106d1998096588f0b15e7d3fd9cb952%7C1670438284%7C5184000%7CSun%2C+05-Feb-2023+18%3A38%3A04+GMT; uid_tt=603962dbd7917b7fb8aa6d15d22f9fe8; uid_tt_ss=603962dbd7917b7fb8aa6d15d22f9fe8; sid_tt=1106d1998096588f0b15e7d3fd9cb952; sessionid=1106d1998096588f0b15e7d3fd9cb952; sessionid_ss=1106d1998096588f0b15e7d3fd9cb952; msToken=HK9QV0_yFO7uqnZowRhJ_MMRH5W7VEFIcZJ60JF-Lsamzs_1gAudM7K5FKJ8T55yd1kvesf8Uf0rtDsSITarwXCXabU8wVQNjfxfQq8g1VA=";
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
                ex_param.put(s,params.get(s));
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
        db.execute(" insert  INTO  douyin_app_ck_all_param  (uid, device_id ,iid,ex_param,ck ,tenant_id) VALUES (?,?,?,?,?,?)",uid,device_id,iid, JSON.toJSONString(ex_param),ck,1 );

    }
}
