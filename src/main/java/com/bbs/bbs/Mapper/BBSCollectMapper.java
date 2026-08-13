package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSPostCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 17:10:07
 * @Version 1.0
 */

public interface BBSCollectMapper extends BaseMapper<BBSPostCollect> {
    @Select("SELECT * FROM tb_bbs_post_collect WHERE user_id = #{userId} AND post_id = #{postId} LIMIT 1")
    BBSPostCollect findByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
    @Insert("INSERT IGNORE INTO tb_bbs_post_collect(user_id, post_id, create_time) VALUES(#{userId}, #{postId}, NOW())")
    int insertIgnore(BBSPostCollect collect);
    @Delete("DELETE FROM tb_bbs_post_collect WHERE user_id = #{userId} AND post_id = #{postId}")
    int deleteByUserAndPost(@Param("userId") Long userId, @Param("postId") Long postId);
}
