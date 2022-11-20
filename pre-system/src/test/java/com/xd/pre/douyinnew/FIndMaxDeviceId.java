package com.xd.pre.douyinnew;

import com.alibaba.fastjson.JSON;

import java.util.Set;

public class FIndMaxDeviceId {

    public static void main(String[] args) {
        Set<String> keys = TestResoData.jedis.keys("抖音和设备号关联:*");
        for (String key : keys) {
            String s = TestResoData.jedis.get(key);
            Integer id = JSON.parseObject(s).getInteger("id");

        }
    }
}
