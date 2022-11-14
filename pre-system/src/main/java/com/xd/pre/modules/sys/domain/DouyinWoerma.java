package com.xd.pre.modules.sys.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("douyin_woerma")
public class DouyinWoerma {


    /**
     * CREATE TABLE `douyin_woerma` (
     *   `id` int(11) NOT NULL AUTO_INCREMENT,
     *   `phone` varchar(20) DEFAULT NULL COMMENT ' 接受手机号',
     *   `trade_no` varchar(100) DEFAULT NULL COMMENT '订单号保留字段',
     *   `content` text COMMENT '沃尔玛内容',
     *   `card_number` varchar(255) DEFAULT NULL COMMENT '沃尔玛账号',
     *   `car_my` varchar(255) DEFAULT NULL COMMENT '沃尔玛卡密',
     *   `create_time` datetime DEFAULT NULL COMMENT '创建时间',
     *   PRIMARY KEY (`id`),
     *   UNIQUE KEY `card_number` (`card_number`) USING BTREE
     * ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String phone;
    /**
     * 保留字段
     */
    private String tradeNo;
    private String content;
    private String cardNumber;
    private String carMy;
    private Date createTime;


}
