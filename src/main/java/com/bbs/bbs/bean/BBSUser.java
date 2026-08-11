package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
//映射数据库表 tb_bbs_user
@TableName("tb_bbs_user")
public class BBSUser {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String loginName;

    private String passwordMd5;

    private String nickName;

    private String headImgUrl;

    private String gender;

    private String location;

    private String introduce;

    private Byte userStatus;

    private Date lastLoginTime;

    private Date createTime;
}
