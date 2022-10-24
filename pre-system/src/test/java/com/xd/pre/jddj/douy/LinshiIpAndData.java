package com.xd.pre.jddj.douy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinshiIpAndData {
    private String ip;
    private Integer port;
}
