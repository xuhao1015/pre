package com.xd.pre.aes;

import cn.hutool.db.Entity;
import com.xd.pre.common.aes.PreAesUtils;
import com.xd.pre.tiren.FindOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CkDataChange {
    public static void main(String[] args) throws Exception {
        if(false){
            List<Entity> query = FindOrder.db.use().query("select * from douyin_app_ck where ck like '%sid_tt=%' ");
            for (Entity entity : query) {
                Integer id = entity.getInt("id");
                String ck = entity.getStr("ck");
                String ckaes = PreAesUtils.encrypt加密(ck);
                FindOrder.db.execute("update douyin_app_ck set ck = ? where id = ?",ckaes,id);
                log.info("设置成功:{}",id);
            }
        }
        if(true){
            List<Entity> query = FindOrder.db.use().query("select * from douyin_app_ck where ck  not  like '%sid_tt=%' ");
            for (Entity entity : query) {
                Integer id = entity.getInt("id");
                String ck = entity.getStr("ck");
                String ckaes = PreAesUtils.decrypt解密(ck);
                FindOrder.db.execute("update douyin_app_ck set ck = ? where id = ?",ckaes,id);
                log.info("设置成功:{}",id);
            }
        }

    }
}
