package com.xd.pre.tiren;

import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSON;
import com.xd.pre.douyinnew.TestResoData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class 查询额度 {

    public static void main(String[] args) throws Exception {
        for(int i =0;i<100000;i++){
            List<Entity> query = FindOrder.db.query("select * from douyin_app_ck where is_enable =1 ");
            Integer ed = 0;
            for (Entity entity : query) {
                String uid = TestResoData.jedis.get("抖音各个账号剩余额度:" + entity.getStr("uid"));
                Integer balance = JSON.parseObject(uid).getInteger("balance");
                ed = balance + ed;
            }
            System.err.println(ed);
            log.info("剩余额度======================"+ed);
            Thread.sleep(2000);
        }

    }
}
