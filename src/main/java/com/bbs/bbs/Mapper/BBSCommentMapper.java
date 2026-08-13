package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.util.PageQueryUtil;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 17:10:27
 * @Version 1.0
 */

public interface BBSCommentMapper extends BaseMapper<BBSPostComment> {
    @Select("SELECT * FROM tb_bbs_post_comment WHERE comment_id = #{id} LIMIT 1")
    BBSPostComment selectIncludingDeleted(@Param("id") Long id);
    @Delete("DELETE FROM tb_bbs_post_comment WHERE comment_id = #{commentId} AND comment_user_id = #{userId}")
    int deleteByOwnerOrPostOwner(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
