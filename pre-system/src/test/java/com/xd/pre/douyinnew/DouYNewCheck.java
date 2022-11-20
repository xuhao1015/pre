package com.xd.pre.douyinnew;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.jddj.douy.Douyin3;
import com.xd.pre.modules.px.douyin.buyRender.BuyRenderParamDto;
import com.xd.pre.modules.px.douyin.submit.SubmitUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Slf4j
public class DouYNewCheck {

    public static Db db = Db.use();
    public static void main(String[] args) throws Exception {

//        List<Entity> appCks = db.use().query("select * from douyin_app_ck where  file_name ='221116.txt'   ");
        List<Entity> appCks = db.use().query("select * from douyin_app_ck where is_enable =-44 and id < 5863 ");
        for (Entity appCk : appCks) {
            String ck = PreAesUtils.decrypt解密(appCk.getStr("ck"));
            Integer id = appCk.getInt("id");
            int i = appCks.indexOf(appCk);
            log.info("当前执行位置msg:{},剩余个数:{}",i,appCks.size()-i-1);
            try {
                checkCk(ck,id);
            }catch (Exception e){

            }
        }

    }

    private static void checkCk(String ck,Integer id) throws Exception {
        String device_id = "device_id_str=4182986699862839";
        String iid = "install_id_str=2599689955581773";
//        String ck = "sid_tt=2211c0c78b4d6593ab75a37ad9b89766;";
        if (device_id.contains("device_id_str=")) {
            device_id = device_id.replace("device_id_str=", "");
        }
        if (iid.contains("install_id_str=")) {
            iid = iid.replace("install_id_str=", "");
        }
/*        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3556357046087622442").sku_id("1736502463777799").author_id("4051040200033531")
                .ecom_scene_id("1041").shop_id("GceCTPIk").origin_id("4051040200033531_3556357046087622442").origin_type("3002070010")
                .new_source_type("product_detail").build();*/
/*        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3561751789252519688").sku_id("1739136614382624").author_id("4051040200033531")
                .ecom_scene_id("1003").origin_id("4051040200033531_3561751789252519688").origin_type("3002002002").new_source_type("product_detail").build();*/
        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3556357230829939771").sku_id("1736502553929735").author_id("4051040200033531")
                .ecom_scene_id("1003").origin_id("4051040200033531_3556357230829939771").origin_type("3002002002").shop_id("GceCTPIk").new_source_type("product_detail").build();
        System.err.println(JSON.toJSONString(buyRenderParamDto));
/*     BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3574327743640429367").sku_id("1745277214000191").author_id("4051040200033531")
                .ecom_scene_id("").origin_id("4051040200033531_3574327743640429367").origin_type("3002002002").new_source_type("product_detail").build();
        System.err.println(JSON.toJSONString(buyRenderParamDto));*/
//        BuyRenderParamDto buyRenderParamDto = BuyRenderParamDto.builder().product_id("3574327743640429367").sku_id("1745277214000191").author_id("4051040200033531")
//                .ecom_scene_id("").origin_id("4051040200033531_3574327743640429367").origin_type("3002002002").new_source_type("product_detail").build();
//        System.err.println(JSON.toJSONString(buyRenderParamDto));
        String body = SubmitUtils.buildBuyRenderParamData(buyRenderParamDto);
        Map<String, String> ipAndPort = Douyin3.getIpAndPort();
        OkHttpClient client = null;
//       client =  Demo.getOkHttpClient(ipAndPort.get("ip"), Integer.valueOf(ipAndPort.get("port")));
        client = new OkHttpClient().newBuilder().build();

//        String body = "{\"address\":null,\"platform_coupon_id\":null,\"kol_coupon_id\":null,\"auto_select_best_coupons\":true,\"customize_pay_type\":\"{\\\"checkout_id\\\":1,\\\"bio_type\\\":\\\"1\\\"}\",\"first_enter\":true,\"source_type\":\"1\",\"shape\":0,\"marketing_channel\":\"\",\"forbid_redpack\":false,\"support_redpack\":true,\"use_marketing_combo\":false,\"entrance_params\":\"{\\\"order_status\\\":3,\\\"previous_page\\\":\\\"order_list_page\\\",\\\"carrier_source\\\":\\\"order_detail\\\",\\\"ecom_scene_id\\\":\\\"1041\\\",\\\"room_id\\\":\\\"\\\",\\\"promotion_id\\\":\\\"\\\",\\\"author_id\\\":\\\"\\\",\\\"group_id\\\":\\\"\\\",\\\"anchor_id\\\":\\\"4051040200033531\\\",\\\"source_method\\\":\\\"open_url\\\",\\\"ecom_group_type\\\":\\\"video\\\",\\\"discount_type\\\":\\\"\\\",\\\"full_return\\\":\\\"0\\\",\\\"is_exist_size_tab\\\":\\\"0\\\",\\\"rank_id_source\\\":\\\"\\\",\\\"show_rank\\\":\\\"not_in_rank\\\",\\\"warm_up_status\\\":\\\"0\\\",\\\"coupon_id\\\":\\\"\\\",\\\"brand_verified\\\":\\\"0\\\",\\\"label_name\\\":\\\"\\\",\\\"with_sku\\\":\\\"0\\\",\\\"is_replay\\\":\\\"0\\\",\\\"is_package_sale\\\":\\\"0\\\",\\\"is_groupbuying\\\":\\\"0\\\"}\",\"shop_requests\":[{\"shop_id\":\"GceCTPIk\",\"product_requests\":[{\"product_id\":\"3556357046087622442\",\"sku_id\":\"1736502463777799\",\"sku_num\":1,\"author_id\":\"4051040200033531\",\"ecom_scene_id\":\"1041\",\"origin_id\":\"4051040200033531_3556357046087622442\",\"origin_type\":\"3002070010\",\"new_source_type\":\"product_detail\",\"select_privilege_properties\":[]}]}]}";
        String url = "https://ken.snssdk.com/order/buyRender?b_type_new=2&request_tag_from=lynx&os_api=25&device_type=SM-G973N&ssmix=a&manifest_version_code=169&dpi=240&is_guest_mode=0&uuid=354730528934825&app_name=aweme&version_name=17.3.0&ts=1664384063&cpu_support64=false&app_type=normal&appTheme=dark&ac=wifi&host_abi=arm64-v8a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1664384064117&device_platform=android&iid=" + iid + "&version_code=170300&cdid=78d30492-1201-49ea-b86a-1246a704711d&os=android&is_android_pad=0&openudid=199d79fbbeff0e58&device_id=" + device_id + "&resolution=720%2A1280&os_version=5.1.1&language=zh&device_brand=Xiaomi&aid=1128&minor_status=0&mcc_mnc=46011";
        String X_SS_STUB = SecureUtil.md5("json_form=" + URLEncoder.encode(body)).toUpperCase();
        String signData = String.format("{\"header\": {\"X-SS-STUB\": \"%s\",\"deviceid\": \"\",\"ktoken\": \"\",\"cookie\" : \"\"},\"url\": \"%s\"}",
                X_SS_STUB, url
        );
        String signHt = HttpRequest.post("http://1.15.184.191:8292/dy22").body(signData).execute().body();
        String x_gorgon = JSON.parseObject(signHt).getString("x-gorgon");
        String x_khronos = JSON.parseObject(signHt).getString("x-khronos");
        RequestBody requestBody = new FormBody.Builder()
                .add("json_form", body)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("X-SS-STUB", X_SS_STUB)
                .addHeader("Cookie", ck)
                .addHeader("X-Gorgon", x_gorgon)
                .addHeader("X-Khronos", x_khronos)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = client.newCall(request).execute();
        String resBody = response.body().string();
        log.info("预下单数据msg:{}", resBody);
        if (ObjectUtil.isNotNull(resBody)  && !resBody.contains("部分商品无法购买，请在无效商品中查看")) {
            db.use().execute("update douyin_app_ck set is_enable = ? where id = ?", -44, id);
        }
        if(StrUtil.isNotBlank(resBody) && resBody.contains("部分商品无法购买，请在无效商品中查看")){
            db.use().execute("update douyin_app_ck set is_enable = ? where id = ?", -2, id);
        }
        if(StrUtil.isNotBlank(resBody) && resBody.contains("用户信息获取失败")){
            db.use().execute("update douyin_app_ck set is_enable = ? where id = ?", -1, id);
        }
    }
}
