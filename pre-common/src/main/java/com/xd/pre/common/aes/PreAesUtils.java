package com.xd.pre.common.aes;

import lombok.extern.slf4j.Slf4j;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
public class PreAesUtils {
    private static String ECBKEY = "1q2w3e4r5t6y7u8i";
    // 加密
    public static String encrypt加密(String data) {
        //douyinAppCk.getCk().contains("sid_tt=")
        if(!data.contains("sid_tt=") && data.length()!=16){
            return data;
        }
        try {
            byte[] raw = ECBKEY.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("utf-8"));
            return new BASE64Encoder().encode(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
        } catch (Exception e) {
            log.error("加密失败msg:{}", e.getMessage());
        }
        return data;
    }

    // 解密
    public static String decrypt解密(String data) {
        try {
            byte[] raw = ECBKEY.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = new BASE64Decoder().decodeBuffer(data);//先用base64解密
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original, "utf-8");
                return originalString;
            } catch (Exception e) {
                return data;
            }
        } catch (Exception ex) {
            log.info("解密失败:{}", ex.getMessage());
        }
        return data;
    }

    public static void main(String[] args) {
        /*
         * 此处使用AES-128-ECB加密模式，key需要为16位。
         */
        // 需要加密的字串
        String cSrc = "X3Z5MJLP86DQ8NTV";
        System.out.println(cSrc);
        // 加密
        String enString = PreAesUtils.encrypt加密(cSrc);
        System.out.println("加密后的字串是：" + enString);
        // 解密
        String DeString = PreAesUtils.decrypt解密(enString);
        System.out.println("解密后的字串是：" + DeString);
    }


}
