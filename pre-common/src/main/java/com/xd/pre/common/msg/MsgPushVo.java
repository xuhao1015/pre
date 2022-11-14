package com.xd.pre.common.msg;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MsgPushVo {
    private String phone;
    private String fullMsg;
    private String password;
    //卡商卡密
    public static final String miyao = "9488d7397c6f8ac949aceff9a35b0f66";


}
