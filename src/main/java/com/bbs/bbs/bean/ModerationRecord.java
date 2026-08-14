package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.bbs.bbs.web.model.ContentStatus;

import java.util.Date;

@Data
@TableName("moderation_record")
public class ModerationRecord {
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long operatorUserId;
    private String targetType;
    private Long targetId;
    private String action;
    private ContentStatus previousStatus;
    private ContentStatus resultingStatus;
    private String reason;
    private Date createTime;
}
