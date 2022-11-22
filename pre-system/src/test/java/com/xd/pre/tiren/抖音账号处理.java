package com.xd.pre.tiren;

import cn.hutool.core.io.FileUtil;

import java.util.List;

public class 抖音账号处理 {
    public static void main(String[] args) {
        List<String> STRINGS = FileUtil.readLines("C:\\Users\\Administrator\\Downloads\\Telegram Desktop\\1123.txt", "UTF-8");
        for (String string : STRINGS) {
            String[] split = string.split("\\|");
            for (int i =0;i<split.length;i++){
               if( split[i].contains("sid_tt=")){
                   System.out.println(split[i]);
               }
            }
        }
    }
}
