package com.bbs.bbs.bean;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * @Author qq
 * @Description
 * @Date 2026-04-14 16:24:58
 * @Version 1.0
 */
@Data
@ToString
public class RecentCommentListEntity {
    private Long postId;

    private String postTitle;

    private String commentBody;

    private Date commentCreateTime;
}
