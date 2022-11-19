package com.xd.pre;

import cn.hutool.core.io.FileUtil;

import java.util.List;

public class TestDemo {


    public static void main(String[] args) {
        List<String> STRINGS = FileUtil.readLines("C:\\Users\\Administrator\\Downloads\\Telegram Desktop\\1152.txt", "UTF-8");
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : STRINGS) {
            String[] split = line.split("\\|");
            for (String s : split) {
                if(s.contains("sid_tt=")){
                    System.out.println(s);
                }
            }
        }
    }
}
