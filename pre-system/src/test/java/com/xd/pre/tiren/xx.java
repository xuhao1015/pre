package com.xd.pre.tiren;

import com.xd.pre.common.aes.PreAesUtils;

public class xx {
    public static void main(String[] args) {
        String a ="200\trO5U53XKUBGF47aoEoH3W2Hxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 01:09:41\n" +
                "100\tHi8ZPYcmto1qiESrurpDN2Hxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 01:01:01\n" +
                "200\tPDsIOEh0L4RGXZ+Wqs5pzGHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:59:35\n" +
                "100\t8K1neipnK/xRYecx3Gv2aWHxj5Vpv4U/QoA1rJNuENY=\t1605728871922062\t2022-11-21 00:58:15\n" +
                "200\tKk3KfxLFHI/WbxnKuU5admHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:57:42\n" +
                "100\tYlEyAyHks4BNEryvJdkw/mHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:56:51\n" +
                "100\tF9+tIPwsLjKOGqFH8IaBQWHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:55:58\n" +
                "200\trdBmtb55091+VozvstjVXmHxj5Vpv4U/QoA1rJNuENY=\t1816020819129948\t2022-11-21 00:55:45\n" +
                "100\tmy5wp5JkFA9PAXw6Wr/BFmHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:53:07\n" +
                "100\tMNw/aCsrsD1LT2pFkIOkhWHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:52:41\n" +
                "200\ttboqXxoc0PzeOq2xy6qqgmHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:50:01\n" +
                "500\t1sh8OjM9jveOH+PKpl3yjWHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:49:12\n" +
                "100\tvM3CfMa5ihAjUK8bPFMa8mHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:47:21\n" +
                "100\tsGejue+PSP9QJnaxgP8knGHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:47:01\n" +
                "200\tkfCcw6tU8fWjYyCS0D+ACWHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:46:42\n" +
                "500\tVMEtvV6q7bAZH1kgloq2Q2Hxj5Vpv4U/QoA1rJNuENY=\t1605728871922062\t2022-11-21 00:44:13\n" +
                "100\tVR4Xn16q5WEfwVKrEEjIOmHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:43:32\n" +
                "200\t2hI4Pcoi82EHos3nOpDIJ2Hxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:43:21\n" +
                "100\tywwsySGoOyC/xaZtvDQNBWHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:43:16\n" +
                "200\t424A4DOmgX2uuMaJQ2TE+WHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:43:16\n" +
                "100\t3KemZ6giXnMkCX447R02rWHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:42:32\n" +
                "100\tK6U7W7nyoXlPTtl/xGL3KGHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:42:27\n" +
                "100\tkbbJtEWGufamyg4lQ2E4Y2Hxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:41:39\n" +
                "200\tgu57fOAe9jwZiOhXorr63GHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:39:37\n" +
                "100\tYU2NnS5KPXiBdaq/PZTn8mHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:39:24\n" +
                "200\tp7IQj80r2X9febseRrGTK2Hxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:38:04\n" +
                "200\tboR+DskgnkjNM5RqmpilaGHxj5Vpv4U/QoA1rJNuENY=\t1816020819129948\t2022-11-21 00:36:24\n" +
                "100\tqhcHU7va4srwFCJzXM2j8GHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:35:52\n" +
                "100\t3UB+nR9EN8kYQV72jYrbgWHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:35:04\n" +
                "100\tzOsucXpLhl4k+K9B6k6YfGHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:34:00\n" +
                "200\twW3XeVpKBePHyv1C0MCG9GHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:33:51\n" +
                "100\tAb1oeFWxsJCoLL9vOC1zs2Hxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:32:35\n" +
                "100\ttQKP5qpYkAtNPDx7LqZRvGHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:31:58\n" +
                "100\tOSy7JyZ1171H3CLYjh2WPmHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:31:36\n" +
                "200\tGjJ92eh0N7LhB8lf5REf3GHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:31:23\n" +
                "100\t4qUkEPShpBB0WJdYFumsFmHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:29:53\n" +
                "200\twPwx14rjpAIEwcVk5geRqWHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:29:02\n" +
                "200\t+wMNG0QLJgrcnG1hCHlDZ2Hxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:28:08\n" +
                "100\tiT+CIzLv2Jiy2QNkg1T1TGHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:26:56\n" +
                "100\tYiEJyLzmOZMwQVbRnP7B5mHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:26:46\n" +
                "100\tya7ZVWqcHji5/3wW8ITUiWHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:26:17\n" +
                "100\t6aqvug4RlaG7+qfhqFLTwWHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:25:58\n" +
                "200\tykRvwcov0/5VAan9j624kWHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:24:41\n" +
                "200\tGtAUOM0/4CPGOMwdu7+ZcGHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:23:14\n" +
                "100\tK8x2uQhB8BO7A3BmOqoODWHxj5Vpv4U/QoA1rJNuENY=\t108201346462\t2022-11-21 00:23:11\n" +
                "100\tl98ULv+oOWqUR1IhUZfKuGHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:22:19\n" +
                "200\tn0Pg014t+IQ0VNJ877BngGHxj5Vpv4U/QoA1rJNuENY=\t1605728871922062\t2022-11-21 00:20:52\n" +
                "100\t95lkCt9ZUbnzfbDml6Y/oGHxj5Vpv4U/QoA1rJNuENY=\t10806103129\t2022-11-21 00:20:47\n" +
                "100\tgofYGULhZS6n6XYiocXummHxj5Vpv4U/QoA1rJNuENY=\t108025724401\t2022-11-21 00:19:59\n" +
                "500\tWOW5v7jqQOez4+oOurcQNGHxj5Vpv4U/QoA1rJNuENY=\t103020901100\t2022-11-21 00:19:50";
        String[] split = a.split("\n");
        for (String s : split) {
            String[] split1 = s.split("\t");
            String s1 = split1[1];
            String s2 = PreAesUtils.decrypt解密(s1);
            System.out.println(split1[0]+"   "+s2+"    "+split1[2]+"    "+split1[3]);
        }
        System.out.println(split.length);
    }
}
