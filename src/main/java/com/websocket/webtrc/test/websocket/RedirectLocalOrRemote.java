package com.websocket.webtrc.test.websocket;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Class description
 *
 * 模板模拟多个用户
 * @version        1.0, 18/09/10
 * @author         pitt
 */
@Controller
public class RedirectLocalOrRemote {

    /**
     * Method description
     * 模拟local用户
     * @param username
     * @param model
     *
     * @return
     */
    @RequestMapping(value = "/local/{username}")
    public String redirectLocal(@PathVariable String username, Model model) {
        model.addAttribute("userName", username);
        return "local";
    }
    /**
     * Method description
     * 模拟local用户
     * @param username
     * @param model
     *
     * @return
     */
    @RequestMapping(value = "/remote/{username}")
    public String redirectRemote(@PathVariable String username, Model model) {
        model.addAttribute("userName", username);
        return "remote";
    }
}


