package com.xd.pre.modules.px.douyin.toutiao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PackParamVo {
    private String rank_id;
    private String enter_from;
    private String item_id;
    private String user_avatar_shrink;
    private String goods_header_shrink;
    private String common_large_shrink;
    private String ecom_scene_id;
//    private String user_id;
    private String author_open_id;
    private String sec_user_id;
    private String gps_on;
    private String promotion_ids;
    private String width;
    private String use_new_price;
    private String goods_comment_shrink;
    private String sec_author_id;
    private String bff_type;
    private String is_preload_req;
    private String author_id;
    private String height;
    private String shop_avatar_shrink;
}
