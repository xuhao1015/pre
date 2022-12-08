package com.xd.pre.modules.px.douyin.toutiao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URLEncoder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchParam {
    private String action_type = "input_keyword_search";
    private String search_start_time;
    private String search_sug = "1";
    private String from = "search_bar_ecom_bottom";
    private String keyword;
    private String search_position = "search_input_list";
    private String cur_tab_title = "search_tab";

    public static SearchParam buildSearchParam(String keyword) {
        SearchParam searchParam = new SearchParam();
        searchParam.setSearch_start_time(System.currentTimeMillis() + "");
        searchParam.setKeyword(URLEncoder.encode(keyword));
        return searchParam;
    }
}
