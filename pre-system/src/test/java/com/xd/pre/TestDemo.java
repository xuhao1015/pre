package com.xd.pre;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.common.utils.px.dto.UrlEntity;

import java.util.Date;
import java.util.List;

public class TestDemo {


    public static void main(String[] args) {
        List<String> STRINGS = FileUtil.readLines("C:\\Users\\Administrator\\Downloads\\Telegram Desktop\\kele300.txt", "UTF-8");
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : STRINGS) {
            boolean contains = line.contains("Set-Cookie:") && line.contains("sid_tt=");
            if (contains) {
                String[] split = line.split(";")[0].split(":");
                stringBuilder.append(split[1] + ";");
            }
            if (line.equals("====================================")) {
                System.out.println(stringBuilder.toString().trim());
                stringBuilder = new StringBuilder();
            }
        }
        StringBuilder stringBuilder1 = new StringBuilder();
    }
}
