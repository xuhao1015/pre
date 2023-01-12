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
        String url = "https://api.toutiaoapi.com/api/feed/profile/v1/?category=profile_article&visited_uid=5770041412&stream_api_version=88&count=20&offset=0&max_behot_time=0&app_extra_params=%7B%22entrance_gid%22%3A%227177987568753967627%22%7D&client_extra_params=%7B%22playparam%22%3A%22codec_type%3A0%2Ccdn_type%3A6%2Cresolution%3A540*960%2Cttm_version%3A-1%2Cenable_dash%3A0%2Cunwatermark%3A1%2Cv1_fitter_info%3A1%2Ctt_net_energy%3A0%2Cis_order_flow%3A-1%2Ctt_device_score%3A7.0%2Ctt_enable_adaptive%3A2%22%2C%22catower_net_quality%22%3A2%2C%22catower_device_overall_performance%22%3A1%7D&device_platform=android&os=android&ssmix=a&_rticket=1671256316729&cdid=96d542e8-9cdc-41ae-b59c-a0b4044fcffb&channel=tengxun_jg_tt_0706&aid=13&app_name=news_article&version_code=896&version_name=8.9.6&manifest_version_code=8960&update_version_code=89607&ab_version=660830%2C5108100%2C5175925%2C5213369%2C668779%2C5175922%2C668774%2C5175916%2C662176%2C5175909%2C662099%2C5175875%2C1859937%2C668776%2C5175917%2C668775%2C4407627%2C5175927%2C5256952%2C2235007&ab_group=94568%2C102749&ab_feature=102749%2C94563&resolution=540*960&dpi=160&device_type=SM-G955N&device_brand=samsung&language=zh&os_api=25&os_version=7.1.2&ac=wifi&dq_param=0&immerse_pool_type=-2&session_id=12307c22-58fe-4913-af5e-838b6fd431fe&rom_version=25&plugin=0&isTTWebView=0&host_abi=armeabi-v7a&client_vid=4539073%2C4681420%2C3383553%2C2827921%2C3194525&iid=3444124972488557&device_id=787704879562295&openudid=c423544b49b198a5";
        String ck = "store-region-src=did; store-region=cn-sc; passport_csrf_token=835d1130314e3910555178d78a6aa6f5; passport_csrf_token_default=835d1130314e3910555178d78a6aa6f5; install_id=1051586667752616; ttreq=1$21830cf964712c0232263fc3b7135dc1b8402521; n_mh=ZDuIUeEszBF_fmDGMJBA8hw-Ro9RtZfXUsHpY6LWVTY; d_ticket=270b8fee9f431d88da032705beb692a9d1a88; sid_guard=76167f65c287d9a4ad69dc9bfaec4d18%7C1671046008%7C5184000%7CSun%2C+12-Feb-2023+19%3A26%3A48+GMT; uid_tt=f8d86879d23628ea1d7bb0732e1bb808; uid_tt_ss=f8d86879d23628ea1d7bb0732e1bb808; sid_tt=76167f65c287d9a4ad69dc9bfaec4d18; sessionid=76167f65c287d9a4ad69dc9bfaec4d18; sessionid_ss=76167f65c287d9a4ad69dc9bfaec4d18; WIN_WH=393_835; PIXIEL_RATIO=2.75; FRM=new; msToken=ClWKintoTnMfz-0OIDMJ54ihP69S3WOr4tEnjNclRaeEvtT8PrzJxh2ESSITP39SAiH2-tGJkV6O9PQsjQ9oEC-PWM3rws-3eIDprBMe1LQ=; odin_tt=91ea054ec17d91be79e82bd1b6baf9cdbd73fc8bde9d7af9f51802a02418d1a7a2cb77c97a256dc3828eaaedbd444fd1b08bcf5f1be938a15671c7ab3d023624";
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
