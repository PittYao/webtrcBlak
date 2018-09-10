package com.websocket.webtrc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Class description
 *  测试模板
 *
 * @version        1.0, 18/09/10
 * @author         pitt
 */
@Controller
public class TestTemplate {

    /**
     * Method description
     * @return
     */
    @RequestMapping(
        value  = "/",
        method = RequestMethod.GET
    )
    public String test() {
        return "test";
    }
}


