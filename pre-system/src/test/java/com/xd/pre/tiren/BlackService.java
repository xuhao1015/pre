package com.xd.pre.tiren;

import cn.hutool.db.Entity;
import cn.hutool.db.nosql.redis.RedisDS;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Set;

@Slf4j
public class BlackService {

    public static Jedis jedis = RedisDS.create().getJedis();

    public static void main(String[] args) throws Exception {
        Set<String> keys = jedis.keys("IP黑名单:*");
        for (String key : keys) {
            String ip = key.replace("IP黑名单:", "");
            Entity entity = FindOrder.db.queryOne("SELECT " +
                    " count(1) as count " +
                    "FROM " +
                    " jd_mch_order mo " +
                    " LEFT JOIN jd_log lo ON lo.order_id = mo.trade_no  " +
                    "WHERE " +
                    " mo.create_time > '2022-10-23 00:00:00' " +
                    " and mo.`status` =2 and lo.ip = ?", ip);
            Integer count = entity.getInt("count");
            if (count > 0) {
                log.info("ip检查++++++++++++++++++++++++++++");
                jedis.set("IP白名单:" + ip, count + "");
                jedis.del("IP黑名单:" + ip);
            }
        }
    }
}
