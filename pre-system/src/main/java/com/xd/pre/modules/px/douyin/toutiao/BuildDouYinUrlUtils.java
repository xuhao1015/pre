package com.xd.pre.modules.px.douyin.toutiao;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xd.pre.modules.px.douyin.deal.GidAndShowdPrice;
import com.xd.pre.modules.sys.domain.DouyinAppCk;
import com.xd.pre.modules.sys.domain.DouyinMethodNameParam;

public class BuildDouYinUrlUtils {

    public static String buildSearchAndPackUrl(SearchParam searchParam, DouyinMethodNameParam douyinMethodNameParam, DouyinAppCk douyinAppCk) {
        StringBuilder returnStrB = new StringBuilder();
        String exParam = douyinAppCk.getExParam();
        JSONObject paramJsons = JSON.parseObject(exParam);
        returnStrB.append(douyinMethodNameParam.getMethod_url());
        if (ObjectUtil.isNotNull(douyinAppCk)) {
            returnStrB.append(String.format("device_id=%s&iid=%s&", douyinAppCk.getDeviceId(), douyinAppCk.getIid()));
        }
        for (String key : paramJsons.keySet()) {
            returnStrB.append(key + "=" + paramJsons.getString(key) + "&");
        }
        JSONObject searchParamJson = JSON.parseObject(JSON.toJSONString(searchParam));
        if (ObjectUtil.isNotNull(searchParam)) {
            for (String key : searchParamJson.keySet()) {
                returnStrB.append(key + "=" + searchParamJson.getString(key) + "&");
            }
        }
        returnStrB.append("_rticket=" + System.currentTimeMillis());
        return returnStrB.toString();
    }

    public static String buildPackPostData(DouyinMethodNameParam douyinMethodNameParam, GidAndShowdPrice gidAndShowdPrice) {
        StringBuilder returnStrB = new StringBuilder();
        String data = String.format(douyinMethodNameParam.getMethod_param(), gidAndShowdPrice.getGid());
        JSONObject packJson = JSON.parseObject(data);
        for (String key : packJson.keySet()) {
            returnStrB.append(key + "=" + packJson.getString(key) + "&");
        }
        return returnStrB.toString();
    }
}
