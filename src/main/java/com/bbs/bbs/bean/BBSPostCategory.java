package com.bbs.bbs.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tb_bbs_post_category")
public class BBSPostCategory {

    @TableId(type = IdType.AUTO)
    private Integer categoryId;

    private String categoryName;

    private Integer categoryRank;
    private Boolean enabled;
    private Date createTime;
}
