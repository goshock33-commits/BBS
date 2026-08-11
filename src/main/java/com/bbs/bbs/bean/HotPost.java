package com.bbs.bbs.bean;

import lombok.Data;
import lombok.ToString;

/**
 * @Author qq
 * @Description
 * @Date 2026-04-08 11:35:51
 * @Version 1.0
 */
@Data
@ToString
public class HotPost {
    private Long postId;

    private String postTitle;

    private Long postComments;
}
