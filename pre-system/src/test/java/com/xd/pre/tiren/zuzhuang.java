package com.xd.pre.tiren;

public class zuzhuang {
    public static void main(String[] args) {
        String a ="os_api=22&device_type=SM-G955N&ssmix=a&manifest_version_code=170301&source=1&dpi=240&is_guest_mode=0&business_line=2&uuid=351564016880114&app_name=aweme&version_name=17.3.0&ts=1668887629&cpu_support64=false&page_size=15&app_type=normal&appTheme=light&ac=wifi&host_abi=armeabi-v7a&update_version_code=17309900&channel=dy_tiny_juyouliang_dy_and24&_rticket=1668887629490&device_platform=android&iid=3743163984904813&lynx_support_version=1&version_code=170300&order_id=4997385875593645635&cdid=481a445f-aeb7-4365-b0cd-4d82727bb775&os=android&openudid=199d79fbbeff0e58&device_id=2538093503847412&action_id=100040&resolution=720*1280&os_version=5.1.1&language=zh&device_brand=samsung&aid=1128&minor_status=0&mcc_mnc=46007";
        String[] split = a.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            System.out.println(split1[0]+":"+split1[1]);
        }
    }
}
