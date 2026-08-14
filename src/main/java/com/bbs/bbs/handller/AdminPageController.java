package com.bbs.bbs.handller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {
    @GetMapping({"", "/"})
    public String index() {
        return "admin/index";
    }

    @GetMapping("/posts")
    public String posts() {
        return "admin/posts";
    }

    @GetMapping("/comments")
    public String comments() {
        return "admin/comments";
    }

    @GetMapping("/users")
    public String users() {
        return "admin/users";
    }

    @GetMapping("/categories")
    public String categories() {
        return "admin/categories";
    }
}
