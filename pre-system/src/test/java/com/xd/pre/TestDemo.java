package com.xd.pre;

import cn.hutool.core.io.FileUtil;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TestDemo {


    public static void main(String[] args) {
        Set<String> sets = new TreeSet<>();

        List<String> STRINGS = FileUtil.readLines("C:\\Users\\Administrator\\Downloads\\Telegram Desktop\\500+.txt", "UTF-8");
        for (String line : STRINGS) {
            boolean contains = line.contains("sid_tt=");
            if (contains) {
                String[] split = line.split(";");
                for (String s : split) {
                    if(s.contains("sid_tt=")){
                        if(s.contains("Set-Cookie:")){
                            sets.add(s.replace("Set-Cookie:", "").trim() + ";");

                        }else {
                            sets.add(s.trim()+";");
                        }
                    }
                }
            }
        }
        for (String set : sets) {
            System.out.println(set);
        }
    }
}
