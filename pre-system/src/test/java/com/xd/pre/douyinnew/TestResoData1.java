package com.xd.pre.douyinnew;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.nosql.redis.RedisDS;
import com.alibaba.fastjson.JSON;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.modules.sys.domain.DouyinDeviceIid;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.List;

@Slf4j
public class TestResoData1 {
    public static Db db = Db.use();
    public static Jedis jedis = RedisDS.create().getJedis();

    public static void main(String[] args) throws Exception {
        List<Entity> appCks = db.use().query("select * from douyin_app_ck where is_enable = 1 and file_name = '221115.txt' ");
        for (Entity appCk : appCks) {
            String ck_device_lock = jedis.get("抖音和设备号关联:" + appCk.getStr("uid"));
            if(StrUtil.isNotBlank(ck_device_lock)){
                DouyinDeviceIid douyinDeviceIid = JSON.parseObject(ck_device_lock, DouyinDeviceIid.class);
                String ck = PreAesUtils.decrypt解密(appCk.getStr("ck"));
                System.out.println(String.format("account:%s,device:%s,iid:%s",ck,douyinDeviceIid.getDeviceId(),douyinDeviceIid.getIid()));

//
            }
        }
    }
}
