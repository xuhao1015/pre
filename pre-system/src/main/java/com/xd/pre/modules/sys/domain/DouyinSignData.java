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
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("douyin_sign_data")
public class DouyinSignData {
    /**
     * CREATE TABLE `douyin_sign_data` (
     *   `id` int(11) NOT NULL AUTO_INCREMENT,
     *   `order_id` varchar(50) NOT NULL,
     *   `create_time` datetime NOT NULL,
     *   `user_agent` varchar(255) NOT NULL,
     *   `browser_sign` varchar(255) NOT NULL,
     *   `ip` varchar(255) NOT NULL,
     *   PRIMARY KEY (`id`)
     * ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
     */
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String orderId;
    private Date createTime;
    private String userAgent;
    private String browserSign;
    private String ip;

}
