package com.xd.pre.tiren;

import cn.hutool.db.Entity;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SetOldAccount {
    public static void main(String[] args) throws Exception {
        List<Entity> query = FindOrder.db.query("select pt_pin ,count(1) as count from jd_order_pt   where create_time < '2022-11-13 22:00:00'  and    pay_success_time is not null   GROUP BY pt_pin");
        Set<String> pt_pins = query.stream().map(it -> it.getStr("pt_pin")).collect(Collectors.toSet());
        for (String pt_pin : pt_pins) {
            FindOrder.db.execute("update douyin_app_ck set is_old =1 where uid =?", pt_pin);
            log.info("设置老号成功：{}", pt_pin);
        }
    }
}
