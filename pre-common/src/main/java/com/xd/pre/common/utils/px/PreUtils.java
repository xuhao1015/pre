package com.xd.pre.common.utils.px;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.qrcode.BufferedImageLuminanceSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.HybridBinarizer;
import com.xd.pre.common.utils.px.dto.UrlEntity;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PreUtils {


    /*
     * 随机生成国内IP地址
     */
    public static String getRandomIp() {

        // ip范围
        int[][] range = {{607649792, 608174079},// 36.56.0.0-36.63.255.255
                {1038614528, 1039007743},// 61.232.0.0-61.237.255.255
                {1783627776, 1784676351},// 106.80.0.0-106.95.255.255
                {2035023872, 2035154943},// 121.76.0.0-121.77.255.255
                {2078801920, 2079064063},// 123.232.0.0-123.235.255.255
                {-1950089216, -1948778497},// 139.196.0.0-139.215.255.255
                {-1425539072, -1425014785},// 171.8.0.0-171.15.255.255
                {-1236271104, -1235419137},// 182.80.0.0-182.92.255.255
                {-770113536, -768606209},// 210.25.0.0-210.47.255.255
                {-569376768, -564133889}, // 222.16.0.0-222.95.255.255
        };

        Random rdint = new Random();
        int index = rdint.nextInt(10);
        String ip = num2ip(range[index][0] + new Random().nextInt(range[index][1] - range[index][0]));
        return ip;
    }

    /*
     * 将十进制转换成ip地址
     */
    private static String num2ip(int ip) {
        int[] b = new int[4];
        String x = "";

        b[0] = (int) ((ip >> 24) & 0xff);
        b[1] = (int) ((ip >> 16) & 0xff);
        b[2] = (int) ((ip >> 8) & 0xff);
        b[3] = (int) (ip & 0xff);
        x = Integer.toString(b[0]) + "." + Integer.toString(b[1]) + "." + Integer.toString(b[2]) + "." + Integer.toString(b[3]);

        return x;
    }


    /**
     * 随机指定范围内N个不重复的数
     * 最简单最基本的方法
     *
     * @param min 指定范围最小值
     * @param max 指定范围最大值
     * @param n   随机数个数
     */
    public static int[] randomCommon(int min, int max, int n) {
        if (min == 0 && max == 0 && n == 1) {
            int[] ints = new int[1];
            ints[0] = 0;
            return ints;
        }
        if (min == 0 && max == 1 && n == 1) {
            int i = new Random().nextInt(100) % 2;
            if (i == 1) {
                int[] ints = new int[1];
                ints[0] = 0;
                return ints;
            } else {
                int[] ints = new int[1];
                ints[0] = 1;
                return ints;
            }
        }
        if (n > (max - min + 1) || max < min) {
            return null;
        }
        int[] result = new int[n];
        int count = 0;
        while (count < n) {
            int num = (int) (Math.random() * (max - min)) + min;
            boolean flag = true;
            for (int j = 0; j < n; j++) {
                if (num == result[j]) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                result[count] = num;
                count++;
            }
        }
        return result;
    }

    /**
     * 随机生成字符串
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String getRandomNum(int length) {
        String str = "0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(10);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static String getWskey(String Cookie) {
        try {
            String[] split = Cookie.split(";");
            if (split.length == 1) {
                return split[0];
            }
            for (int i = 0; i < split.length; i++) {
                String trim = split[i].trim();
                if (StrUtil.isNotBlank(trim) && trim.contains("wskey=")) {
                    return trim.split("wskey=")[1];
                }
            }
        } catch (Exception e) {
            log.error("解析ck失败msg:[Cookie:{}]", Cookie);
        }
        return null;

    }


    public static String get_pt_pin(String Cookie) {
        try {
            String[] split = Cookie.split(";");
            if (split.length == 1) {
                return split[0];
            }
            for (int i = 0; i < split.length; i++) {
                String trim = split[i].trim();
                if (StrUtil.isNotBlank(trim) && trim.contains("pt_pin=")) {
                    return trim.split("pt_pin=")[1];
                }
                if (StrUtil.isNotBlank(trim) && trim.contains("pin=")) {
                    String s = trim.split("pin=")[0];
                    if (StrUtil.isBlank(s)) {
                        return trim.split("pin=")[1];
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析ck失败msg:[Cookie:{}]", Cookie);
        }
        return null;

    }

    /**
     * 获取字符串中的数字 分段提取
     *
     * @param str
     * @return
     */
    public static List<String> getNum(String str) {
        String regex = "(\\d+)";
        List<String> nums = new LinkedList<>();
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(str);
        while (m.find()) {
            nums.add(m.group());
        }
        return nums;
    }

    public static Map<String, String> getCookies(String ckContext) {
        Map<String, String> cookies = new HashMap<>();
        String[] split = ckContext.split(";");
        for (String ckKey : split) {
            String[] keyAndValue = ckKey.trim().split("=");
            if (keyAndValue.length == 2) {
                cookies.put(keyAndValue[0], keyAndValue[1]);
            }
        }
        return cookies;
    }


    // 正确的IP拿法，即优先拿site-local地址
    public static InetAddress getLocalHostLANAddress() {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getIPAddress(HttpServletRequest request) {
        String ip = null;
        log.info("       ip = request.getRemoteAddr()msg:{}", ip = request.getRemoteAddr());
        //X-Forwarded-For：Squid 服务代理
        String ipAddresses = request.getHeader("X-Forwarded-For");
        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //Proxy-Client-IP：apache 服务代理
            ipAddresses = request.getHeader("Proxy-Client-IP");
        }

        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //WL-Proxy-Client-IP：weblogic 服务代理
            ipAddresses = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //HTTP_CLIENT_IP：有些代理服务器
            ipAddresses = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ipAddresses == null || ipAddresses.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            //X-Real-IP：nginx服务代理
            ipAddresses = request.getHeader("X-Real-IP");
            log.info("       request.getHeader(\"X-Real-IP\")msg:{}", request.getHeader("X-Real-IP"));
        }

        //有些网络通过多层代理，那么获取到的ip就会有多个，一般都是通过逗号（,）分割开来，并且第一个ip为客户端的真实IP
        if (ipAddresses != null && ipAddresses.length() != 0) {
            log.info("       ip = request.getRemoteAddr() msg:{}", ipAddresses);
            ip = ipAddresses.split(",")[0];
        }
        String remoteAddr = request.getRemoteAddr();
        //还是不能获取到，最后再通过request.getRemoteAddr();获取
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ipAddresses)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static int getNum(int start, int end) {
        return (int) (Math.random() * (end - start + 1) + start);
    }

    /**
     * 返回手机号码
     */
    private static String[] telFirst = "134,135,136,137,138,139,150,151,152,157,158,159,130,131,132,155,156,133,153".split(",");

    public static String getTel() {
        int index = getNum(0, telFirst.length - 1);
        String first = telFirst[index];
        String second = String.valueOf(getNum(1, 888) + 10000).substring(1);
        String third = String.valueOf(getNum(1, 9100) + 10000).substring(1);
        return first + second + third;
    }

    /**
     * 解析url
     *
     * @param url
     * @return
     */
    public static UrlEntity parseUrl(String url) {
        try {
            UrlEntity entity = new UrlEntity();
            if (url == null) {
                return entity;
            }
            url = url.trim();
            if (url.equals("")) {
                return entity;
            }
            String[] urlParts = url.split("\\?");
            entity.baseUrl = urlParts[0];
            //没有参数
            if (urlParts.length == 1) {
                return entity;
            }
            //有参数
            String[] params = urlParts[1].split("&");
            entity.params = new HashMap<>();
            for (String param : params) {
                int i = param.indexOf("=");
//                String[] keyValue = param.split("=");
                entity.params.put(param.substring(0, i), param.substring(i + 1, param.length()));
            }
            return entity;
        } catch (Exception e) {
            log.error("解析错误:{}", e.getMessage());
        }
        return null;

    }


    public static String getUseCk(String ck) {
        String[] split = ck.split(";");
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : split) {
            if (StrUtil.isBlank(s)) {
                continue;
            }
            if (s.contains("pin=") && !s.contains("pt_pin=")) {
                stringBuilder.append(s + ";");
            }
            if (s.contains("wskey=")) {
                stringBuilder.append(s + ";");
            }
            //mck
            if (s.contains("pt_pin=")) {
                stringBuilder.append(s + ";");
            }
            if (s.contains("pt_key")) {
                stringBuilder.append(s + ";");
            }

        }
        ck = stringBuilder.toString();
        if (StrUtil.isBlank(ck)) {
            return null;
        }
        return ck;
    }

    public static String jumpIosHrefUrl(String payData) {
        JSONObject parseObject = JSON.parseObject(payData);
        String appid = "wxe75a2e68877315fb";
        JSONObject body = JSON.parseObject(parseObject.get("body").toString());
        String payUrl = String.format("weixin://app/%s/pay/?nonceStr=%s&package=Sign%%3DWXPay&partnerId=%s&prepayId=%s&timeStamp=%s&sign=%s",
                appid, body.get("nonceStr"), body.get("partnerId"), body.get("prepayId"), body.get("timeStamp"), body.get("sign")
        );
        return URLEncoder.encode(payUrl);
    }

    public static String parsePayUrl(InputStream inputStream) {
        MultiFormatReader reader = null;
        BufferedImage image;
        try {
            image = ImageIO.read(inputStream);
            if (image == null) {
                throw new Exception("cannot read image from inputstream.");
            }
            final LuminanceSource source = new BufferedImageLuminanceSource(image);
            final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            final Map<DecodeHintType, String> hints = new HashMap<DecodeHintType, String>();
            hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
            // 解码设置编码方式为：utf-8，
            reader = new MultiFormatReader();
            String payurlData = reader.decode(bitmap, hints).getText();
            return payurlData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, String> buildIpMap(String ip) {
        HashMap<String, String> ipmap = new HashMap<>();
        ipmap.put("X-Forwarded-For", ip);
        ipmap.put("Proxy-Client-IP", ip);
        ipmap.put("WL-Proxy-Client-IP", ip);
        ipmap.put("HTTP_CLIENT_IP", ip);
        ipmap.put("X-REMOTE-ADDR", ip);
        ipmap.put("X-REMOTE-IP", ip);
        ipmap.put("X-Real-IP", ip);
        ipmap.put("Client-ip", ip);
        ipmap.put("X-Client-IP", ip);
        ipmap.put("REMOTE_ADDR", ip);
        ipmap.put("X-Originating-IP", ip);
        return ipmap;
    }

    private static boolean isTab = true;


    public static String stringToFormatJSON(String strJson) {
        // 计数tab的个数
        int tabNum = 0;
        StringBuffer jsonFormat = new StringBuffer();
        int length = strJson.length();

        for (int i = 0; i < length; i++) {
            char c = strJson.charAt(i);
            if (c == '{') {
                tabNum++;
                jsonFormat.append(c + "\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
            } else if (c == '}') {
                tabNum--;
                jsonFormat.append("\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
                jsonFormat.append(c);
            } else if (c == ',') {
                jsonFormat.append(c + "\n");
                jsonFormat.append(getSpaceOrTab(tabNum));
            } else {
                jsonFormat.append(c);
            }
        }
        return jsonFormat.toString();
    }

    // 是空格还是tab
    private static String getSpaceOrTab(int tabNum) {
        StringBuffer sbTab = new StringBuffer();
        for (int i = 0; i < tabNum; i++) {
            if (isTab) {
                sbTab.append('\t');
            } else {
                sbTab.append("    ");
            }
        }
        return sbTab.toString();
    }


    public static String getSign(String orderId) {
        String md5 = SecureUtil.md5(orderId);
        return md5;
    }


    public static String getAsciiSort(Map<String, Object> map) {
        // 移除值为空的
        map.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()) || "".equals(entry.getValue()));

        List<Map.Entry<String, Object>> infoIds = new ArrayList<Map.Entry<String, Object>>(map.entrySet());
        // 对所有传入参数按照字段名的 ASCII 码从小到大排序（字典序）
        infoIds.sort((o1, o2) -> o1.getKey().compareToIgnoreCase(o2.getKey()));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> infoId : infoIds) {
            if (infoId.getKey().equals("sign")) {
                continue;
            }
            sb.append(infoId.getKey());
            sb.append("=");
            sb.append(infoId.getValue());
            sb.append("&");
        }
        return sb.substring(0, sb.length() - 1);
    }

}
