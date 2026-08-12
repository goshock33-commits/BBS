package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @TableField("password_hash")
    @JsonIgnore
    @ToString.Exclude
    private String passwordHash;

    private String role;

    private String nickName;

    private String headImgUrl;

    private String gender;

    private String location;

    private String introduce;

    private Byte userStatus;

    private Date lastLoginTime;

    private Date createTime;

    private Date updateTime;
}
