package com.bookshop01.test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/test")
public class CpuController {

    @RequestMapping(value = "/stressCpu.do", method = RequestMethod.GET)
    public void stressCpu(HttpServletResponse response) throws IOException {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 20000; 

        int outerIterations = 300; 
        int middleIterations = 300; 
        int innerIterations = 300; 

        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < outerIterations; i++) {
                for (int j = 0; j < middleIterations; j++) {
                    for (int k = 0; k < innerIterations; k++) {
                      
                        double value = Math.pow(Math.random(), Math.random());
                    }
                }
            }
        }

    
        response.sendRedirect("/bookShop/main/main.do");
    }
}
