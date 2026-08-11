package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_bbs_post_collect")
public class BBSPostCollect {
    @TableId(type = IdType.AUTO)
    private Long recordId;

    private Long postId;

    private Long userId;

    private Date createTime;
}