package com.bbs.bbs.handller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 16:13:40
 * @Version 1.0
 */@RestController
public class helloController {
     public String hello(){
         return "hello";
     }
}
