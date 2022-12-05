package com.xd.pre.modules.px.douyin.deal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.xd.pre.common.utils.px.PreUtils;
import com.xd.pre.common.utils.px.dto.UrlEntity;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DealSearchDataUtils {

    public static List<GidAndShowdPrice> gidAndShowPrice(String search_data, Integer buyPrice) {
        List<GidAndShowdPrice> gidAndShowdPrices = new ArrayList<>();
        try {
            if (StrUtil.isBlank(search_data)) {
                return null;
            }
            JSONArray items = JSON.parseObject(search_data).getJSONArray("items");
            if (CollUtil.isEmpty(items)) {
                return null;
            }
            for (Object item : items) {
                String itemStr = JSON.toJSONString(item);
                if (!itemStr.contains("App Store 充值卡") || !itemStr.contains("product_info") || !itemStr.contains("gid") || !itemStr.contains("promotion_id")) {
                    continue;
                }
                Integer show_price = JSON.parseObject(itemStr).getJSONObject("product_info").getInteger("show_price");
                String gid = JSON.parseObject(itemStr).getJSONObject("product_info").getString("gid");
                String schema = JSON.parseObject(itemStr).getJSONObject("product_info").getString("schema");
                if (show_price.intValue() == 10000 || show_price.intValue() == 20000 || show_price.intValue() == 50000 || show_price.intValue() == 100000) {
                    schema = URLDecoder.decode(schema);
                    UrlEntity urlEntity = PreUtils.parseUrl(schema);
                    if (CollUtil.isEmpty(urlEntity.getParams()) || !urlEntity.getParams().containsKey("meta_params")) {
                        continue;
                    }
                    if (show_price.intValue() != buyPrice.intValue()) {
                        continue;
                    }
                    String ecom_scene_id = JSON.parseObject(urlEntity.getParams().get("meta_params")).getJSONObject("entrance_info").getString("ecom_scene_id");
                    GidAndShowdPrice gidAndShowdPrice = GidAndShowdPrice.builder().gid(gid).show_price(show_price).schema(ecom_scene_id).ecom_scene_id(ecom_scene_id).build();
                    gidAndShowdPrices.add(gidAndShowdPrice);
                }
            }
            return gidAndShowdPrices;
        } catch (Exception e) {
            log.error("解析搜索数据的对照表出错:{}", e.getMessage());
        }
        return null;
    }
}
