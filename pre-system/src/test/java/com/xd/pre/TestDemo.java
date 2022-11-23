package com.xd.pre;

import cn.hutool.core.io.FileUtil;
import com.xd.pre.common.utils.px.PreUtils;

import java.util.List;

public class TestDemo {


    public static void main(String[] args) throws Exception{
        while (true){
            Integer i = PreUtils.randomCommon(0, 2 - 1, 1)[0];
            System.out.println(i);
            Thread.sleep(1000);
        }
    }
}
