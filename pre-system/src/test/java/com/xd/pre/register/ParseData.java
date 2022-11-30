package com.xd.pre.register;

import cn.hutool.core.io.FileUtil;

import java.util.List;

public class ParseData {

    public static void main(String[] args) {
        List<String> STRINGS = FileUtil.readLines("C:\\Users\\Administrator\\Downloads\\Telegram Desktop\\苹果570.txt", "UTF-8");
        for (String line : STRINGS) {
            String[] split = line.split("\\|");
            for (String s : split) {
                if (s.contains("sid_tt=")) {
                    String[] split1 = s.split(";");
                    for (String s1 : split1) {
                        if (s1.contains("sid_tt=")) {
                            System.out.println(s1);
                        }
                    }
                }
            }
        }
    }

}
