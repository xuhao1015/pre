package com.xd.pre.modules.sys.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DouyinMethodNameParam {
    /**
     * CREATE TABLE `douyin_method_name_param` (
     * `id` int(11) NOT NULL AUTO_INCREMENT,
     * `method_name` text,
     * `method_param` text,
     * `method_url` text,
     * PRIMARY KEY (`id`)
     * ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String method_name;
    private String method_param;
    private String method_url;
}
