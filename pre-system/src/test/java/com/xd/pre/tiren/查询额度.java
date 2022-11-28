package com.xd.pre.tiren;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSON;
import com.xd.pre.douyinnew.TestResoData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class 查询额度 {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100000; i++) {
            List<Entity> query = FindOrder.db.query("select * from douyin_app_ck where is_enable =1 ");
            List<Entity> query1= FindOrder.db.query("SELECT count( 1 ), sku_price,tenant_id \n" +
                    "FROM\n" +
                    "\tjd_order_pt \n" +
                    "WHERE wx_pay_expire_time > now( ) AND success = 0  AND  (href_url = '' or  href_url is null )\n" +
                    "GROUP BY sku_price,tenant_id \n" +
                    "ORDER BY\n" +
                    "\ttenant_id,\n" +
                    "\tsku_price;");
            Integer ed = 0;
            for (Entity entity : query) {
                String uid = TestResoData.jedis.get("抖音各个账号剩余额度:" + entity.getStr("uid"));
                if (StrUtil.isBlank(uid)) {
                    continue;
                }
                Integer balance = JSON.parseObject(uid).getInteger("balance");
                ed = balance + ed;
            }
            String s = JSON.toJSONString(query1);
            System.err.println(ed+"=========="+s);
            log.info("剩余额度======================" + ed);

            Thread.sleep(2000);
        }

    }
}
