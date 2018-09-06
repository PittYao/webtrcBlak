package com.websocket.webtrc.test.websocket;

import javax.websocket.server.PathParam;

import javax.xml.ws.RequestWrapper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Class description
 * 根据url模拟多个local用户
 * 生成对应的local HTML
 *
 * @author pitt
 * @version 1.0, 18/09/06
 */
@Controller
public class RedirectLocalOrRemote {

    /**
     * Method description
     *
     * @param username 远端用户名
     * @param model    template的数据容器
     * @return 模板页面
     */
    @RequestMapping(
            value = "/local/{username}",
            method = RequestMethod.GET
    )
    public String redirectLocal(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "testLocal";
    }

    /**
     * Method description
     *
     * @param username 远端用户名
     * @param model    template的数据容器
     * @return 模板页面
     */
    @RequestMapping(
            value = "/remote/{username}",
            method = RequestMethod.GET
    )
    public String redirectRemote(@PathVariable String username, Model model) {
        model.addAttribute("username", username);
        return "testRemote";
    }
}


