package com.bbs.bbs.bean;

import org.springframework.stereotype.Component;

/**
 * @Author qq
 * @Description
 * @Date 2026-04-08 21:29:22
 * @Version 1.0
 */@Component
public class otherUtil {
     public int  calculateHotScore(BBSPost post){
int score = 0;
score = (int)(post.getPostViews()*0.2 + post.getPostComments()*0.4 + post.getPostCollects()*0.4);
return score;
     }

}
