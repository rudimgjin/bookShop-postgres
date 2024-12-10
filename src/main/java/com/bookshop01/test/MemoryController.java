package com.bookshop01.test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/test")
public class MemoryController {

    @RequestMapping(value = "/stressMemory.do", method = RequestMethod.GET)
    public void stressMemory(HttpServletResponse response) throws IOException {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 1000;

        List<byte[]> memoryList = new ArrayList<byte[]>(); 
        
        while (System.currentTimeMillis() < endTime) {

            byte[] largeArray = new byte[128]; 
            memoryList.add(largeArray);
            
       
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            System.out.println("Used memory (bytes): " + usedMemory);
        }

      
        response.sendRedirect("/bookShop/main/main.do");
    }
}
