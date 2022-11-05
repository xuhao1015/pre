package com.xd.pre.tiren;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;

public class AEU {
    public static void main(String[] args) {
        String content = "X6Z4MJQFV8CLTGZ9";
        byte[] key = "1Q2W3E4R5T6Y7U8I".getBytes();
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, key);
        String encryptHex = aes.encryptHex(content);
        System.out.println(encryptHex);
        String decryptStr = aes.decryptStr(encryptHex, CharsetUtil.CHARSET_UTF_8);
        System.out.println(decryptStr);
    }
}
