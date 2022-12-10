package com.xd.pre.dy23;

import okhttp3.*;

public class test {
    public static void main(String[] args) throws Exception {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "user_id=_000qYxWcyUyLtu-qZ4X7BJGY_bCy7jW4TrV&sec_user_id=&author_id=&author_open_id=&sec_author_id=&promotion_ids=3556357230829939771&item_id=&enter_from=order_detail&enter_from=&meta_param=&width=&height=&rank_id=&use_new_price=&gps_on=&product_id=&creative_id=&promotion_id=&bff_type=&ui_params=&user_avatar_shrink=&goods_header_shrink=&goods_comment_shrink=&shop_avatar_shrink=&common_large_shrink=&ecom_scene_id=&ecom_scene_id=&same_product_scene=&is_preload_req=");
        Request request = new Request.Builder()
                .url("https://isaas.ecombdapi.com/aweme/v2/shop/promotion/pack/saas/?device_platform=android,android&os=android,android&ssmix=a&_rticket=1670680796675,1670680796684&cdid=12b16515-b220-46d4-a288-276c835071d5,12b16515-b220-46d4-a288-276c835071d5&channel=juyouliang_toutiao_and17,juyouliang_toutiao_and17&aid=13,13&app_name=news_article,news_article&version_code=908,908&version_name=9.0.8,9.0.8&manifest_version_code=9080,9080&update_version_code=90811,90811&ab_version=1859936%2C660830%2C5108085%2C5175925%2C5213517%2C668779%2C5175922%2C668774%2C5175916%2C662176%2C5175909%2C662099%2C5175875%2C668775%2C4329661%2C5158112%2C5175927%2C668776%2C5175917%2C5203086%2C2235007%2C5113788,1859936%2C660830%2C5108085%2C5175925%2C5213517%2C668779%2C5175922%2C668774%2C5175916%2C662176%2C5175909%2C662099%2C5175875%2C668775%2C4329661%2C5158112%2C5175927%2C668776%2C5175917%2C5203086%2C2235007%2C5113788&ab_group=94566%2C102751,94566%2C102751&ab_feature=94563%2C102749,94563%2C102749&resolution=1080%2A2245,1080%2A2245&dpi=480,480&device_type=PGBM10,PGBM10&device_brand=OPPO,OPPO&language=zh,zh&os_api=31,31&os_version=12,12&ac=wifi,wifi&dq_param=0,0&plugin=0,0&client_vid=3194525%2C3383553%2C4539073%2C2827921%2C4977978%2C5019683,3194525%2C3383553%2C4539073%2C2827921%2C4977978%2C5019683&isTTWebView=0,0&session_id=e5bf7b36-fb97-4f72-b2e5-0681ec0c571d,e5bf7b36-fb97-4f72-b2e5-0681ec0c571d&host_abi=armeabi-v7a,armeabi-v7a&tma_jssdk_version=2.53.0,2.53.0&rom_version=coloros__pgbm10_11_a.26,coloros__pgbm10_11_a.26&immerse_pool_type=101,101&iid=4218177821088199,4218177821088199&device_id=2916356054606526,2916356054606526&webcast_gps_access=2&openlive_personal_recommend=1&webcast_app_id=6822&webcast_locale=zh_CN&webcast_sdk_version=2120&webcast_language=zh&ecom_page_type=native&ecom_appid=7386&ecom_sdk_version=27500")
                .post(body)
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", "store-region-src=did; store-region=cn-sc; install_id=4218177821088199; ttreq=1$63c5a79b320081053c68809bfd377fac2130c016; passport_csrf_token=e505ae03cef3c5e580e1d3c0ddade4fc; passport_csrf_token_default=e505ae03cef3c5e580e1d3c0ddade4fc; store-region-src=did; store-region=cn-sc; install_id=4218177821088199; ttreq=1$63c5a79b320081053c68809bfd377fac2130c016; odin_tt=9e3b8618d8ac23af5f59a97577327a86705e6e444a900052d6ac5b9550b2e3f8cb21c2c3f62001f675a4b897266fb9e3124598d916ea0388c3917ca258ad2631e4d8563e1f65a5f1460c458cd872cb8b; passport_csrf_token=e505ae03cef3c5e580e1d3c0ddade4fc; d_ticket=88b22cb483feb6886602a6d4eec3d14ba5fee; n_mh=3XDB0FNV6oLHN9AbcQdSGWNFy6ilWHxYSVZ6t0iU8vo; sid_guard=886118d8f02b79fcfdf0174a1438e455%7C1670680563%7C5184000%7CWed%2C+08-Feb-2023+13%3A56%3A03+GMT; uid_tt=ba97c27fdc4c49dde7e0031c3e3044c9; uid_tt_ss=ba97c27fdc4c49dde7e0031c3e3044c9; sid_tt=886118d8f02b79fcfdf0174a1438e455; sessionid=886118d8f02b79fcfdf0174a1438e455; sessionid_ss=886118d8f02b79fcfdf0174a1438e455; msToken=7dG-Ee99RIbJR3z4q2yI6F9z7dywtkaJMEVl1wumLpGSXlXdHa86R-s5V72TCE12vV9In7H1MvRjbTcBDRwwdKGQWHOBzPzGFpNDApDBTrk=; odin_tt=1dfe161c13ee3bedd02f8f8f07e62622d4836c106f78a8b259811d1eaea034f12eb2551725a994940665f2b272ed4bcd1bf95a0580eff990498fcf4368900000")
                .addHeader("Authorization", "Bearer act.3.x1MEUexHuzdSBXPrZXGMNsRC6WT4brwyOcWT1e-ab7G7w_7QJdb6OIJ3WoCqjcbzRBaw3WxN9MSXkBZts-uD6JYFbmnhdkqQASSbCVAisbUfHIut86KTtDbGyks-KPsLJGrK7Y0diKgUj-oCN7ytvb5HkV9eNt3vc6XXOg==")
                .addHeader("OpenId", "_000qYxWcyUyLtu-qZ4X7BJGY_bCy7jW4TrV")
                .addHeader("ClientKey", "awikua6yvbqai0ht")
                .addHeader("odin-tt", "1dfe161c13ee3bedd02f8f8f07e62622d4836c106f78a8b259811d1eaea034f12eb2551725a994940665f2b272ed4bcd1bf95a0580eff990498fcf4368900000")
                .addHeader("ecom_page_type", "native")
                .addHeader("ecom_appid", "7386")
                .addHeader("ecom_sdk_version", "27500")
                .addHeader("x-vc-bdturing-sdk-version", "3.2.0.cn")
                .addHeader("x-tt-dt", "AAAX5QPEB3NRK7WTRR4GORFLLQDXU2Y73OIJ5JSKOPI6K3LNIUCRDCCCTLZVVP3QR4UP7HNXHCCSG6NDUZ4CNWRYEJHM5LPW5YRLPKMSPWBQVI7CYLZ7E35OWLLKO7UTW2KGHLWPERWCE32AJYTUJKI")
                .addHeader("sdk-version", "2")
                .addHeader("X-Tt-Token", "00886118d8f02b79fcfdf0174a1438e45501a877185ecd71efdb193da78dcd542dc1785700686a8e195a67471351134147c68930fe534265a4ad83ec67988143a3b7f7534b1d363949e663c76e9aa82c164b291d7a41ed04b6189d92b302cdf8b35e7-1.0.1")
                .addHeader("passport-sdk-version", "30863")
                .addHeader("x-tt-request-tag", "n=0;s=-1;p=0")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("X-SS-STUB", "EBCBEB29B348533CAB0DC61323CACB94")
                .addHeader("x-tt-trace-id", "00-fc55df980da5c690eac62be7295e000d-fc55df980da5c690-01")
                .addHeader("User-Agent", "com.ss.android.article.news/9080 (Linux; U; Android 12; zh_CN; PGBM10; Build/SP1A.210812.016; Cronet/TTNetVersion:a911d6f2 2022-11-14 QuicVersion:585d7967 2022-11-14)")
                .addHeader("X-Argus", "hgQijFaO9jTusFdzbvcykEy1t2sF94SKlqocxY48Oxr++CcBf1eFZBiWL1YssmrRwmLISZl6bl53F7jPc0gPHvcB9v4hWJ3sZESKsTDpXEFy7vTHlJCGgl7y6bINPmsQGkORa2Khfy0nXAbSfr4XsC0X1Nwz4S8fX6+RfOMq0D1xX2SuQ3wpoubUEKRL15auSmzswNr34+D+ukZw/blhMT72xLLBetfVF3VOh3fb35Xn7gRZrqdsVT0LDQSr2KAj8fykR8Vrs6OesR4OD7ik6tnP")
                .addHeader("X-Gorgon", "04045614000033ce82c1c6fd412c06009b903128638554ac58a2")
                .addHeader("X-Khronos", "1670681174")
                .addHeader("X-Ladon", "TrGjASUhxT9MvMyGzThiIGoYIfLE/SQOsNYhPae81VY1D9sf")
                .addHeader("cache-control", "no-cache")
                .build();

        Response response = client.newCall(request).execute();
        String string = response.body().string();
        System.out.println(string);
    }
}
