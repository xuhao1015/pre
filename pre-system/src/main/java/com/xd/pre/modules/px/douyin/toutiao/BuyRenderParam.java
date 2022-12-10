package com.xd.pre.modules.px.douyin.toutiao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BuyRenderParam {
    private String b_type_new = "3";
    private String sub_b_type = "13";
    private String live_sdk_version = "910";
    private String webcast_appid = "6822";
    private String live_request_from_jsb = "1";
    private String ecom_appid = "7386";
    private String webcast_sdk_version = "2120";
    private String webcast_app_id = "6822";

    public static BuyRenderParam buildBuyRenderParam() {
        BuyRenderParam buyRenderParam = new BuyRenderParam();
        return buyRenderParam;
    }
}
