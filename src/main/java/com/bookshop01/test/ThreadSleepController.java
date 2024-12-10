package com.bookshop01.test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/test")
public class ThreadSleepController {

    @RequestMapping(value = "/threadSleep2sec.do", method = RequestMethod.GET)
    public void threadSleep2sec(HttpServletResponse response) throws IOException {
        try {
         
            Thread.sleep(2000);
        } catch (InterruptedException e) {
         
            e.printStackTrace();
        }

        response.sendRedirect("/bookShop/main/main.do");
    }

    @RequestMapping(value = "/threadSleep3sec.do", method = RequestMethod.GET)
    public void threadSleep3sec(HttpServletResponse response) throws IOException {
        try {
         
            Thread.sleep(3000);
        } catch (InterruptedException e) {
         
            e.printStackTrace();
        }

        response.sendRedirect("/bookShop/main/main.do");
    }

    @RequestMapping(value = "/threadSleep4sec.do", method = RequestMethod.GET)
    public void threadSleep4sec(HttpServletResponse response) throws IOException {
        try {
         
            Thread.sleep(4000);
        } catch (InterruptedException e) {
         
            e.printStackTrace();
        }

        response.sendRedirect("/bookShop/main/main.do");
    }
}