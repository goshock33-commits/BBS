package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import com.bbs.bbs.web.model.ContentStatus;

import java.util.Date;

@Data
@TableName("tb_bbs_post")
public class BBSPost {

    @TableId(type = IdType.AUTO)
    private Long postId;

    private Long publishUserId;

    private String postTitle;

    private String postContent;

    private Integer postCategoryId;

    private String postCategoryName;

    private Byte postStatus;

    private ContentStatus contentStatus;

    private Long postViews;

    private Long postComments;

    private Long postCollects;

    @Version
    private Integer version;

    private Date lastUpdateTime;

    private Date createTime;
    //@TableLogic 注解告诉 MyBatis-Plus：
//
//这个字段是逻辑删除标记字段
//在执行删除操作时，自动改为更新操作
//在查询时，自动过滤掉已标记为删除的记录
    //默认值为 0，表示未删除
    //删除时，自动改为为 1，表示已删除
    @TableLogic
    private Byte isDeleted;
    /**
     * 发帖人昵称
     */
    @TableField(exist = false)
    private String nickName;
    /**
     * 发帖人头像
     */
    @TableField(exist = false)
    private String headImgUrl;
    /**
     * 发帖人信息
     */
    @TableField(exist = false)
    private BBSUser user;
}
