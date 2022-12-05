package com.xd.pre.modules.px.douyin.deal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GidAndShowdPrice {
    private String payIp;
    private String post_tel;
    private Integer buyPrice;

    private String gid;
    private Integer show_price;
    private String schema;
    private String ecom_scene_id;
    private String search_id;
    /**
     * 搜索封装
     */
    private String top_level;
    private String screen_size_adaptation;
    private String mega_object;
    private String origin_id;
    private String type;
    private String enable_font_scale;
    private String ec_order_prefetch;
    private String id;
    private String web_bg_color;
    private String alkey;
    private String trans_status_bar;
    private String add_safe_area_height;
    private String android_soft_input_mode;
    private String hide_nav_bar;
    private String combo_id;
    private String url;
    private String origin_type;
    private String new_source_type;
    private String shop_id;
    private String combo_num;
    private String group_id;
    private String author_id;
    private String status_bar_color;
    private String c_biz_combo;
    private String status_bar_bg_color;
    /**
     * 预下单数据
     */
    private String decision_id;
    private String render_track_id;
    private String marketing_plan_id;
    private String payapi_cache_id;
    private String render_token;
    private String promotion_ext;
    private String promotion_process;
    private String activity_id;
    private String merchant_info;
}
