package com.xd.pre.tiren;

import com.xd.pre.common.aes.PreAesUtils;

public class xx {
    public static void main(String[] args) {
        String a ="100\t3K/+3kEoCsKcBENNA8Wpf2Hxj5Vpv4U/QoA1rJNuENY=\t3465263180422270\n" +
                "100\thQV8pT2dRs9kbgaLgnjRiWHxj5Vpv4U/QoA1rJNuENY=\t3465263180422270\n" +
                "100\tMxM8wb/dx3nImFCuQ7ah7mHxj5Vpv4U/QoA1rJNuENY=\t108025724401\n" +
                "500\tKYT/JeAxtYcfwDU24pGOfGHxj5Vpv4U/QoA1rJNuENY=\t108025724401\n" +
                "200\tCOxoKwZaXuL7K5Lf1FrPzmHxj5Vpv4U/QoA1rJNuENY=\t108025724401\n" +
                "100\tIokqSxqqqcQZQOWBltonGGHxj5Vpv4U/QoA1rJNuENY=\t108201346462\n" +
                "200\tg9awqdofeTtxaMeetpLqpmHxj5Vpv4U/QoA1rJNuENY=\t10806103129\n" +
                "200\tklPnz891Csbpk7ZCAbLwdmHxj5Vpv4U/QoA1rJNuENY=\t108025724401\n" +
                "200\tAwWuEpe1KZZYPdDQ66XJxWHxj5Vpv4U/QoA1rJNuENY=\t108025724401\n" +
                "200\trs5oHmS2TTPV2n5ARiX4AGHxj5Vpv4U/QoA1rJNuENY=\t103020901100\n" +
                "200\tjBq5s7gArBwUsnnVwH6AU2Hxj5Vpv4U/QoA1rJNuENY=\t10806103129\n" +
                "200\tFqC//n6vshLT350c7ezremHxj5Vpv4U/QoA1rJNuENY=\t103020901100\n" +
                "100\tO9bmyp42tCOSvuMtVe0fA2Hxj5Vpv4U/QoA1rJNuENY=\t108025724401\n" +
                "100\thlgYPEKwZi+utYby0EYCiGHxj5Vpv4U/QoA1rJNuENY=\t108201346462\n" +
                "100\tG8gS4Lj2ms6q8Nmvc/oGu2Hxj5Vpv4U/QoA1rJNuENY=\t103020901100\n" +
                "200\tr3/euPchP+O9ZKCFEVRpVWHxj5Vpv4U/QoA1rJNuENY=\t10806103129\n" +
                "100\tdMsQlrq6ezpqgWaeI5V+MmHxj5Vpv4U/QoA1rJNuENY=\t10806103129";
        String[] split = a.split("\n");
        for (String s : split) {
            String[] split1 = s.split("\t");
            String s1 = split1[1];
            String s2 = PreAesUtils.decrypt解密(s1);
            System.out.println(split1[0]+"   "+s2+"    "+split1[2]);
        }
        System.out.println(split.length);
    }
}
