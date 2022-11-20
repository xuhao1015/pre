package com.xd.pre.tiren;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.common.aes.PreAesUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;

public class 查订单 {
    public static void main(String[] args) throws Exception{
        List<Entity> query = FindOrder.db.query("select * from douyin_app_ck where  uid =?","1561735893092860");
        for (Entity entity : query) {
            String ck = PreAesUtils.decrypt解密(entity.getStr("ck"));
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://aweme.snssdk.com/aweme/v1/commerce/order/newList/?is_lynx=true&page_size=10&page_anchor=&level_one_business_line=0&tab=3&page=1&b_type_new=2&live_request_from_jsb=1&live_sdk_version=170301&webcast_sdk_version=2140&webcast_language=zh&webcast_locale=zh_CN&webcast_gps_access=1&current_network_quality_info=%7B%7D&address_book_access=1&os_api=22&device_type=SM-G955N&ssmix=a&manifest_version_code=170301&dpi=240&is_guest_mode=0&uuid=351564016880114&app_name=aweme&version_name=17.3.0&ts=1668881128&cpu_support64=false&app_type=normal&appTheme=light&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1668881129295&device_platform=android&iid=3743163984904813&version_code=170300&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&is_android_pad=0&openudid=199d79fbbeff0e58&device_id=2538093503847412&resolution=720%2A1280&os_version=5.1.1&language=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc=46007")
                    .get()
                    .addHeader("Cookie", ck)
                    .addHeader("cache-control", "no-cache")
                    .build();
            Response response = client.newCall(request).execute();
            String orderData = response.body().string();
            if(orderData.contains("用户未登录")){
                continue;
            }
            System.err.println(orderData);
            String orders = JSON.parseObject(orderData).getString("orders");
            List<JSONObject> jsonObjects = JSON.parseArray(orders, JSONObject.class);
            if(CollUtil.isEmpty(jsonObjects)){
                continue;
            }
            for (JSONObject jsonObject : jsonObjects) {
                String order_id = jsonObject.getString("order_id");
                boolean isc = JSON.toJSONString(jsonObject).contains("充值卡");
                if(isc){
                    Entity entityOrder = FindOrder.db.queryOne("select * from jd_order_pt where order_id = ?", order_id);
                    if(ObjectUtil.isNull(entityOrder)){
                        System.out.println(order_id);
                        FindOrder.db.execute("insert into  temp_c  VALUES (?,?)",order_id,ck);
                    }
                    else {
//                        System.err.println("我的订单:"+order_id);
                    }
                }
                else {
                    System.err.println("不晓得啥订单:"+order_id);
                }
            }
        }

    }
}
