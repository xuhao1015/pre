package com.xd.pre.aes;

import cn.hutool.db.Entity;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.tiren.FindOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CkDataChange {
    public static void main(String[] args) throws Exception {
        List<Entity> query = FindOrder.db.use().query("select * from douyin_app_ck_aes ");
        for (Entity entity : query) {
            Integer id = entity.getInt("id");
            String ck = entity.getStr("ck");
            String ckaes = PreAesUtils.encrypt加密(ck);
            FindOrder.db.execute("update douyin_app_ck_aes set ck = ? where id = ?",ckaes,id);
            log.info("设置成功:{}",id);
        }
    }
}
