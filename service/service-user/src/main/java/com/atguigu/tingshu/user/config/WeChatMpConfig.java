package com.atguigu.tingshu.user.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author atguigu-mqx
 * @ClassName WeChatMpConfig
 * @description: TODO
 * @date 2026年02月09日
 * @version: 1.0
 */
@Component
public class WeChatMpConfig {

    @Autowired
    private WechatAccountConfig wechatAccountConfig;

    @Bean
    public WxMaService wxMaService(){
        //  创建一个WxMaColnfig 对象;
        WxMaDefaultConfigImpl wxMaConfig = new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wechatAccountConfig.getAppId());
        wxMaConfig.setSecret(wechatAccountConfig.getAppSecret());
        //  创建对象;
        WxMaServiceImpl wxMaService = new WxMaServiceImpl();
        wxMaService.setWxMaConfig(wxMaConfig);
        //  返回数据;
        return wxMaService;
    }
}
