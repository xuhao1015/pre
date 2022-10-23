package com.xd.pre.tiren;

import cn.hutool.core.collection.CollUtil;
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

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@Slf4j
public class FindOrder {

    public static Db db = Db.use();

    public static void main(String[] args) throws Exception {
        noticy("P1583147207349174272");
        for (int i = 0; i < 1000000; i++) {
            Thread.sleep(60 * 1000);
            List<Entity> query = null;
            try {
                query = db.use().query(" " +
                        "SELECT " +
                        " mo.trade_no,mo.out_trade_no,mo.original_trade_no  " +
                        "FROM " +
                        " jd_mch_order mo " +
                        " LEFT JOIN jd_order_pt op ON op.id = mo.original_trade_id  " +
                        "WHERE " +
                        " mo.create_time > DATE_SUB( SYSDATE( ), INTERVAL 100 MINUTE )  " +
                        " and mo.create_time < DATE_SUB( SYSDATE( ), INTERVAL 7 MINUTE )  " +
                        " and mo.click_pay is not null  " +
                        " and mo.click_pay !='1970-01-01 08:00:00' " +
                        " AND op.html IS NULL " +
                        " and mo.`status`!=2 ");
            } catch (Exception e) {
                log.info("创建数据库链接失败");
            }
            if (CollUtil.isEmpty(query)) {
                log.info("没有数据需要补单");
                continue;
            }
            for (Entity entity : query) {
                try {
                    String outOrder = entity.getStr("out_trade_no");
                    noticy(outOrder);
                } catch (Exception e) {
                    log.info("当前数据修改失败");
                }
            }
        }
    }

    private static void noticy(String outOrder) throws SQLException, IOException {
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
        if (CollUtil.isEmpty(voucher_info_list)) {
            db.use().execute("update jd_order_pt set org_app_ck = ?,html=? where order_id = ?",
                    DateUtil.formatDateTime(new Date()), html, original_trade_no);
            return;
        }
        JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
        String code = voucher_info.getString("code");
        if (StrUtil.isBlank(code)) {
            log.info("没有支付");
            return;
        }
        log.info("支付成功:{}", original_trade_no);
        db.use().execute("update jd_mch_order set status = ? where out_trade_no = ?", 2, outOrder);
        db.use().execute("update jd_order_pt set card_number = ? ,car_my = ?,pay_success_time = ?,org_app_ck = ?,html=? where order_id = ?",
                code, code, DateUtil.formatDateTime(new Date()), DateUtil.formatDateTime(new Date()), html, original_trade_no);
    }
}
