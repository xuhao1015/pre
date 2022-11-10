package com.xd.pre.tiren;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class demoChaChong {
    public static void main(String[] args) {
        ExcelReader reader = ExcelUtil.getReader(FileUtil.file("C:\\Users\\Administrator\\Desktop\\excel.xls"));
        List<Map<String, Object>> maps = reader.readAll();
        List<String> allData = new ArrayList<>();

        for (Map<String, Object> map : maps) {
            Set<String> strings = map.keySet();
            for (String key: strings) {
                if(ObjectUtil.isNull(map.get(key)) || StrUtil.isBlank(map.get(key).toString())){
                    continue;
                }
                String trim = map.get(key).toString().trim();
                allData.add(trim);
            }
        }
        Map<String, List<String>> collect = allData.stream().collect(Collectors.groupingBy(it -> it));
        for (String s : collect.keySet()) {
            if(collect.get(s).size()<=1){
                System.out.println(s);
            }
        }

    }
}
