package com.xd.pre.modules.px.douyin.dto;

import com.xd.pre.modules.sys.domain.JdMchOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class IsCheckShipDto {

    private List<JdMchOrder> jdMchOrders;
    private Integer count;
    private Date startTime;
    private Date endTime;
}
