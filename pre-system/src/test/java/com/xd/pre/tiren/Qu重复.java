package com.xd.pre.tiren;


import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.xd.pre.common.aes.PreAesUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Qu重复 {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\Administrator\\Desktop\\卡密垃圾库\\卡密去重\\";
        File file = new File(path);
        String[] list = file.list();
        List<String> data = new ArrayList<>();
        for (String s : list) {
            if (s.contains(".xls")) {
                String filePathR = path + s;
                ExcelReader reader = ExcelUtil.getReader(filePathR);
                List<Map<String, Object>> readAll = reader.readAll();
                for (Map<String, Object> stringObjectMap : readAll) {
                    data.add(stringObjectMap.get("账号").toString());
                }
            }
        }
        Map<String, List<String>> collect = data.stream().collect(Collectors.groupingBy(it -> it));
        Db db = FindOrder.db;
        for (String s : collect.keySet()) {
            if (collect.get(s).size() == 1) {
                List<Entity> query = db.query("select * from jd_order_pt where car_my = ?", PreAesUtils.encrypt加密(s));
                String sku_price = query.get(0).getStr("sku_price");
                System.out.println(sku_price+"---缺少---"+s);
            }
        }
        System.out.println("================================");
        for (String s : collect.keySet()) {
            if (collect.get(s).size() >= 3) {
                List<Entity> query = db.query("select * from jd_order_pt where car_my = ?", PreAesUtils.encrypt加密(s));
                String sku_price = query.get(0).getStr("sku_price");
                System.out.println(sku_price+"---重复---"+s);
            }
        }
    }
}
