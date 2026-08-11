package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.util.PageQueryUtil;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 17:10:27
 * @Version 1.0
 */

public interface BBSCommentMapper extends BaseMapper<BBSPostComment> {

}
