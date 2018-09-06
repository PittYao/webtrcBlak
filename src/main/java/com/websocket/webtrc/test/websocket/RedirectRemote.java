package com.websocket.webtrc.test.websocket;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.websocket.server.PathParam;
import javax.xml.ws.RequestWrapper;

@Controller
public class RedirectRemote {
    @RequestMapping(value = "/remote/{username}", method = RequestMethod.GET)
    public String redirectRemote(@PathVariable  String username, Model model) {
        System.out.println(username);
        model.addAttribute("username",username);
        return "testRemote";
    }
}
