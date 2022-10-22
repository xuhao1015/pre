package com.xd.pre.tiren;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Qu重复 {
    public static void main(String[] args) {
        ExcelReader reader = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 05_10_08.xls");
        List<List<Object>> readAll = reader.read();
        readAll.remove(0);
        List<String> collect = readAll.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader1 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 10_55_41.xls");
        List<List<Object>> readAll1 = reader1.read();
        readAll1.remove(0);
        List<String> collect1 = readAll1.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader2 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 13_41_11.xls");
        List<List<Object>> readAll2 = reader2.read();
        readAll2.remove(0);
        List<String> collect2 = readAll2.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader3 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 14_59_09.xls");
        List<List<Object>> readAll3 = reader3.read();
        readAll3.remove(0);
        List<String> collect3 = readAll3.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader4 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 17_58_05.xls");
        List<List<Object>> readAll4 = reader4.read();
        readAll4.remove(0);
        List<String> collect4 = readAll4.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader5 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 19_42_44.xls");
        List<List<Object>> readAll5 = reader5.read();
        readAll5.remove(0);
        List<String> collect5 = readAll5.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader6 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\2022-10-22 21_40_35.xls");
        List<List<Object>> readAll6 = reader6.read();
        readAll6.remove(0);
        List<String> collect6 = readAll6.stream().map(it -> it.get(4).toString()).collect(Collectors.toList());

        ExcelReader reader7 = ExcelUtil.getReader("C:\\Users\\Administrator\\Desktop\\quc\\jd_order_pt.xls");
        List<List<Object>> readAll7 = reader7.read();
        readAll7.remove(0);
        List<String> collect7 = readAll7.stream().map(it -> it.get(3).toString().toString()).collect(Collectors.toList());

        collect7.addAll(collect);
        collect7.addAll(collect1);
        collect7.addAll(collect2);
        collect7.addAll(collect3);
        collect7.addAll(collect4);
        collect7.addAll(collect5);
        collect7.addAll(collect6);
        Map<String, List<String>> collect8 = collect7.stream().collect(Collectors.groupingBy(it -> it));
        for (String s : collect8.keySet()) {
            List<String> strings = collect8.get(s);
            if(strings.size()==1){
                System.out.println(s);
            }
        }

    }
}
