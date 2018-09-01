package com.websocket.webtrc.test.websocket;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 测试webSocket跳转
 */
@Controller
public class Redirect {
    @RequestMapping("redirect")
    public  String redirect(){
        return "myWebsocket";
    }
}
