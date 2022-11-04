package com.xd.pre.tiren;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.db.Entity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IgnoreAccount {

    public static void main(String[] args) throws Exception {
        List<Entity> query1 = FindOrder.db.query("SELECT * FROM pre.douyin_app_ck WHERE is_enable = '1' AND fail_reason LIKE '%当前下单人数过多%'");
        List<Entity> query2 = FindOrder.db.query("SELECT * FROM pre.douyin_app_ck WHERE is_enable = '1' AND fail_reason LIKE '%设备存在异常%'");
        List<Entity> query3 = FindOrder.db.query("SELECT * FROM pre.douyin_app_ck WHERE is_enable = '1' AND fail_reason LIKE '%挤爆了%'");
        List<Entity> query4 = FindOrder.db.query("SELECT * FROM pre.douyin_app_ck WHERE is_enable = '1' AND fail_reason LIKE '%当前订单存在交易风险%'");
        ArrayList<Entity> query = new ArrayList<>();
        query.addAll(query1);
        query.addAll(query2);
        query.addAll(query3);
        query.addAll(query4);
        if (CollUtil.isEmpty(query)) {
            log.info("当前账号全部正常+++++++++++++++++");
            return;
        }
        try {
            for (Entity entity : query) {
                String uid = entity.getStr("uid");
                BlackService.jedis.del("抖音和设备号关联:" + uid);
                log.info("删除成功关联成功:{},", uid);
                FindOrder.db.execute("update douyin_app_ck  set is_enable =0 ,fail_reason  = '' where id = ?", entity.getInt("id"));
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

    }
}
