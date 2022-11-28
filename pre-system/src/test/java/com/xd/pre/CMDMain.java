package com.xd.pre;

import com.xd.pre.common.aes.PreAesUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class CMDMain {
    public static void main(String[] args) throws Exception {
        String xktf97DNLGWK8CD7 = PreAesUtils.encrypt加密("XKTF97DNLGWK8CD7");
        System.out.println(xktf97DNLGWK8CD7);
    }
}
