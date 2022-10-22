package com.xd.pre.tiren;

import okhttp3.*;

public class Tiren {
    public static void main(String[] args) throws Exception {
        String a = "os_api=22&device_type=SM-G955N&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=351564016880114&app_name=aweme&delete_device_type=1&version_name=17.3.0&ts=1666260009&cpu_support64=false&app_type=normal&appTheme=light&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1666260009610&device_platform=android&iid=3743163984904813&version_code=170300&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&openudid=199d79fbbeff0e58&device_id=2538093503847412&del_did=321506381413262&resolution=720*1280&os_version=5.1.1&language=zh&device_brand=samsung&kick_app_id=2329&aid=1128&minor_status=0&mcc_mnc=46007";
        String[] split = a.split("&");
        for (String s : split) {
            if (s.contains("=")) {
                String s0 = s.split("=")[0];
                String s1 = s.split("=")[1];
                System.out.println(s0 + ":" + s1);
            }
        }
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "os_api=22&device_type=SM-G955N&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=351564016880114&app_name=aweme&delete_device_type=1&version_name=17.3.0&ts=1666260009&cpu_support64=false&app_type=normal&appTheme=light&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1666260009610&device_platform=android&iid=3743163984904813&version_code=170300&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&openudid=199d79fbbeff0e58&device_id=2538093503847412&del_did=321506381413262&resolution=720*1280&os_version=5.1.1&language=zh&device_brand=samsung&kick_app_id=2329&aid=1128&minor_status=0&mcc_mnc=46007");
        Request request = new Request.Builder()
                .url("https://aweme-hl.snssdk.com/passport/safe/login_device/del/?host_abi=armeabi-v7a&iid=3743163984904813&device_id=2538093503847412&aid=1128")
                .post(body)
                .addHeader("Cookie", "install_id=3743163984904813; ttreq=1$c937432add1ac5543b40dc8b95cb769bf024bf3a; passport_csrf_token=dc084fdfd9182b2006ac015d23d5094e; passport_csrf_token_default=dc084fdfd9182b2006ac015d23d5094e; d_ticket=5d8498d9c5c57a18f23083f8b948b45743690; multi_sids=659356656346136%3A140a336dd81551eaa30bc0e9e8d336fd; n_mh=8nysT__BxDL_VpPZTRMYKZZSN1pywPhZ9o63MSmzGLg; passport_assist_user=CkCRydX49tKRiP6NfppL8EZXqhP7I0lHXjcq-1NuFi9tetbHhO7j8WgKWcNY0u1c2_pwQmIxWsLy25zu5vuCS4y2GkgKPEawcjcdGGFhQ7XJU9Cvcme37ad7_x2LoXTiOHQl20bPqoQm-Xexq_YwQPA0X1fytaQzn-aCrETNNkRTDBDpip0NGImv1lQiAQMRQLcc; sid_guard=140a336dd81551eaa30bc0e9e8d336fd%7C1664383681%7C5183999%7CSun%2C+27-Nov-2022+16%3A48%3A00+GMT; uid_tt=1e4686eabe61b69fd57f1db3639b39b0; uid_tt_ss=1e4686eabe61b69fd57f1db3639b39b0; sid_tt=140a336dd81551eaa30bc0e9e8d336fd; sessionid=140a336dd81551eaa30bc0e9e8d336fd; sessionid_ss=140a336dd81551eaa30bc0e9e8d336fd; odin_tt=e4fe7820f9460c7f6aafa983074e42756992f2db8efcc316a479eb6366b67499e34798997156865a5ca2ee5c6ad2133d24df49a54e0bef46337796e28c3f222eb3ec22bbf8584e091d40f3b5bdae5161; MONITOR_WEB_ID=80de3f8a-fd12-4ba7-8e84-6244f8f91d6c")
                .addHeader("X-Khronos", "1666260302")
                .addHeader("X-Gorgon", "0404a99d0000fea71dccc24e65003505050567832fc1e025ff14")
                .build();
        Response response = client.newCall(request).execute();
        String string = response.body().string();
        System.out.println(string);
    }
}
