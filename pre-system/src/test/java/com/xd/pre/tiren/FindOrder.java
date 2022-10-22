package com.xd.pre.tiren;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.common.constant.PreConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Date;
import java.util.List;

@Slf4j
public class FindOrder {

    public static Db db = Db.use();

    public static void main(String[] args) throws Exception {
        String outOrder = "P1583911318634823680";
        Entity entity = db.use().queryOne(String.format("select * from jd_mch_order where out_trade_no = '%s'", outOrder));
        String original_trade_no = entity.getStr("original_trade_no");
        Entity entity1 = db.use().queryOne(String.format("select * from jd_order_pt where order_id = %s", original_trade_no));
        String current_ck = entity1.getStr("current_ck");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?aid=45465&order_id=" + original_trade_no)
                .get()
                .addHeader("Cookie", current_ck)
                .addHeader("X-Khronos", "1665697911")
                .addHeader("X-Gorgon", "8404d4860000775655c5b8f6315f8a608a802f3a78e4891a08cc")
                .addHeader("User-Agent", "okhttp/3.10.0.1")
                .addHeader("cache-control", "no-cache")
                .build();
        Response response = client.newCall(request).execute();
        String body = response.body().string();
        System.out.println(body);
        if (StrUtil.isBlank(body)) {
            log.info("对不起，没有查询成");
            return;
        }
        String html = JSON.parseObject(body).getString("order_detail_info");
        String voucher_info_listStr = JSON.parseObject(html).getString("voucher_info_list");
        List<JSONObject> voucher_info_list = JSON.parseArray(voucher_info_listStr, JSONObject.class);
        JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
        String code = voucher_info.getString("code");
        if (StrUtil.isBlank(code)) {
            log.info("没有支付");
            return;
        }
        log.info("支付成功:{}", original_trade_no);
        db.use().execute("update jd_mch_order set status = ? where out_trade_no = ?", 2, outOrder);
        db.use().execute("update jd_order_pt set card_number = ? ,car_my = ?,pay_success_time = ? where order_id = ?",
                code, code, DateUtil.formatDateTime(new Date()), original_trade_no);

    }
}
