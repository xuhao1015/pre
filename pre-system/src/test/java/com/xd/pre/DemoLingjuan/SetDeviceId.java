package com.xd.pre.DemoLingjuan;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSON;
import com.xd.pre.register.SetRelaship;
import com.xd.pre.tiren.FindOrder;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SetDeviceId {
    public static void main(String[] args) throws Exception {
        Jedis jedis = SetRelaship.jedis;
        Set<String> keys = jedis.keys("抖音和设备号关联:*");
        Db db = FindOrder.db;
        List<Entity> query = db.query("select * from  douyin_app_ck where device_id is null  ");
        List<String> uids = query.stream().map(it -> it.getStr("uid")).collect(Collectors.toList());
        for (String key : keys) {
            String uid = key.replace("抖音和设备号关联:", "").trim();
            if (!uids.contains(uid)) {
                continue;
            }
            String s = jedis.get(key);
            String deviceId = JSON.parseObject(s).getString("deviceId");
            String iid = JSON.parseObject(s).getString("iid");
            db.execute("update  douyin_app_ck  set device_id = ? ,iid = ? where uid  =? ", deviceId, iid, uid);
            log.info("uid:{}", uid);
        }

        for (String key : keys) {
            String s = jedis.get(key);
            String id = JSON.parseObject(s).getString("id");
            db.execute("update  douyin_device_iid  set is_enable = ?   where id  =? ", 2, id);
            log.info("id:{}", id);
        }

    }
}
