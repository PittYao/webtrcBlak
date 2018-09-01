package com.websocket.webtrc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class TestController {
    @RequestMapping(value = "local",method = RequestMethod.GET)
    public String test (){
        return "local";
    }
    @RequestMapping(value = "haha",method = RequestMethod.GET)
    public String haha (){
        return "haha";
    }
}
