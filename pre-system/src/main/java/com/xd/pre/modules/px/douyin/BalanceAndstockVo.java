package com.xd.pre.modules.px.douyin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalanceAndstockVo {
    private Integer suf;
    private List<Map<String, Object>> stock;
}
