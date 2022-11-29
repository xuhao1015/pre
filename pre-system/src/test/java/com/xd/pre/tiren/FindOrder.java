package com.xd.pre.tiren;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.common.constant.PreConstant;
import com.xd.pre.modules.px.douyin.DouyinService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class FindOrder {

    public static Db db = Db.use();

    public static void main(String[] args) throws Exception {
        List<String> outOrders = new ArrayList<>();
        outOrders.add("P1597479328217747456");
        for (String outOrder : outOrders) {
            noticy(outOrder);
        }
        System.err.println("============================================");
        for (int i = 0; i < 1000000; i++) {
            List<Entity> query = null;
            try {
                query = db.use().query("SELECT " +
                        " mo.trade_no, " +
                        " mo.out_trade_no, " +
                        " mo.original_trade_no  " +
                        "FROM " +
                        " jd_mch_order mo " +
                        " LEFT JOIN jd_order_pt op ON op.id = mo.original_trade_id  " +
                        "WHERE " +
                        " mo.create_time > DATE_SUB( SYSDATE( ), INTERVAL 30 MINUTE )  " +
                        " AND mo.click_pay < DATE_SUB( SYSDATE( ), INTERVAL 2 MINUTE )  " +
                        " AND mo.click_pay IS NOT NULL and op.html is null  " +
                        " AND mo.click_pay != '1970-01-01 08:00:00'  " +
                        " AND mo.`status` != 2  order by mo.click_pay ;");
            } catch (Exception e) {
                log.info("创建数据库链接失败");
            }
            if (CollUtil.isEmpty(query)) {
                log.info("没有数据需要补单,全部查询完毕");
                Thread.sleep(10 * 1000);
                continue;
            } else {
                for (Entity entity : query) {
                    try {
                        String outOrder = entity.getStr("out_trade_no");
                        noticy(outOrder);
                    } catch (Exception e) {
                        log.info("当前数据修改失败");
                    }
                }
            }
            log.info("全部查询完毕++++++++++=");
            Thread.sleep(20 * 1000);
        }
    }

    private static void noticy(String outOrder) throws SQLException, IOException {
        Entity entity = db.use().queryOne(String.format("select * from jd_mch_order where out_trade_no = '%s'", outOrder));
        if (entity.getInt("status") == 2) {
            log.info("当前订单已经成功");
            return;
        }
        String original_trade_no = entity.getStr("original_trade_no");
        Entity entity1 = db.use().queryOne(String.format("select * from jd_order_pt where order_id = %s", original_trade_no));
        String current_ck = entity1.getStr("current_ck");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://aweme.snssdk.com/aweme/v1/commerce/order/detailInfo/?aid=45465&order_id=" + original_trade_no)
                .get()
                .addHeader("Cookie", PreAesUtils.decrypt解密(current_ck))
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
        if (body.contains("用户未登录")) {
            return;
        }
        String html = JSON.parseObject(body).getString("order_detail_info");
        String voucher_info_listStr = JSON.parseObject(html).getString("voucher_info_list");
        String shop_order_status_info = JSON.parseObject(html).getString("shop_order_status_info");
        List<JSONObject> voucher_info_list = JSON.parseArray(voucher_info_listStr, JSONObject.class);
        if (CollUtil.isEmpty(voucher_info_list)) {
            db.use().execute("update jd_order_pt set org_app_ck = ?,html=? where order_id = ?",
                    DateUtil.formatDateTime(new Date()), shop_order_status_info, original_trade_no);
            return;
        }
        JSONObject voucher_info = voucher_info_list.get(PreConstant.ZERO);
        String code = voucher_info.getString("code");
        if (StrUtil.isBlank(code)) {
            log.info("没有支付");
            return;
        }
        log.info("当前订单支付成功：{}", outOrder);
        if (entity1.getStr("href_url").contains(entity.getStr("trade_no"))) {
            db.use().execute("update jd_mch_order set status = ? where out_trade_no = ?", 2, outOrder);
        }
        db.use().execute("update jd_order_pt set card_number = ? ,car_my = ?,pay_success_time = ?,org_app_ck = ?,html=? where order_id = ?",
                PreAesUtils.encrypt加密(code), PreAesUtils.encrypt加密(code), DateUtil.formatDateTime(new Date()), DateUtil.formatDateTime(new Date()), shop_order_status_info, original_trade_no);
        DouyinService douyinService = new DouyinService();
        Boolean ac100030 = douyinService.isac100030Zr100040(client, entity.getStr("trade_no"), original_trade_no, current_ck, "100030");
        if (ac100030) {
            db.use().execute("update jd_order_pt set action_id = ? where order_id = ?", 100030, original_trade_no);
            Boolean ac100040 = douyinService.isac100030Zr100040(client, entity.getStr("trade_no"), original_trade_no, current_ck, "100040");
            if (ac100040) {
                db.use().execute("update jd_order_pt set action_id = ? where order_id = ?", 100040, original_trade_no);
                log.info("删除订单成功:{}", outOrder);
            }
        }
    }
}
