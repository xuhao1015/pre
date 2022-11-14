package com.xd.pre.modules.sys.controller;

import cn.hutool.core.util.ObjectUtil;
import com.xd.pre.common.msg.MsgPushVo;
import com.xd.pre.common.utils.R;
import com.xd.pre.modules.sys.domain.DouyinWoerma;
import com.xd.pre.modules.sys.mapper.DouyinWoermaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;

@RestController
@RequestMapping("/sms")
@Slf4j
public class SmsController {

    @Resource
    private DouyinWoermaMapper douyinWoermaMapper;

    @GetMapping("/pushMsg")
    public R pushMsg(String phone, String content, String password) {
        log.info("收到的短信信息为:phone:{},content:{},password:{}", phone, content, password);
        if (ObjectUtil.isNull(password) || !password.equals(MsgPushVo.miyao)) {
            return R.error("密钥不正常");
        }
        DouyinWoerma build = DouyinWoerma.builder().content(content).createTime(new Date()).phone(phone).build();
        douyinWoermaMapper.insert(build);
        return R.ok();
    }

}
