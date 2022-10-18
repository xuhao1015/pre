package com.xd.pre.modules.px.douyin.pay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BalanceRedisDto {
    private String uid;
    private Integer balance;
}
