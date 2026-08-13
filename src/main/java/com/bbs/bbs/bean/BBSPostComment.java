package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;
import com.bbs.bbs.web.model.ContentStatus;

@Data
@TableName("tb_bbs_post_comment")
public class BBSPostComment {

    @TableId(type = IdType.AUTO)
    private Long commentId;

    private Long postId;

    private Long commentUserId;

    private String commentBody;

    private Long parentCommentUserId;

    private Date commentCreateTime;

    @TableLogic
    private Byte isDeleted;

    private ContentStatus contentStatus;

    @Version
    private Integer version;

    @TableField(exist = false)
    private String nickName;

    @TableField(exist = false)
    private String headImgUrl;

    @TableField(exist = false)
    private BBSUser user;

}
