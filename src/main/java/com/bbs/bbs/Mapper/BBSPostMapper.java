package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSPost;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 17:08:36
 * @Version 1.0
 */

public interface BBSPostMapper extends BaseMapper<BBSPost> {

    @Select("SELECT * FROM tb_bbs_post WHERE post_id = #{postId} LIMIT 1")
    BBSPost selectPostIncludingDeleted(@Param("postId") Long postId);

    @Update("""
            UPDATE tb_bbs_post
            SET post_title = #{title},
                post_content = #{content},
                post_category_id = #{categoryId},
                post_category_name = #{categoryName},
                version = version + 1,
                last_update_time = CURRENT_TIMESTAMP(3)
            WHERE post_id = #{postId}
              AND publish_user_id = #{authorId}
              AND content_status = 'NORMAL'
              AND is_deleted = 0
              AND version = #{version}
            """)
    int updateOwnedPost(@Param("postId") Long postId,
                        @Param("authorId") Long authorId,
                        @Param("title") String title,
                        @Param("content") String content,
                        @Param("categoryId") Integer categoryId,
                        @Param("categoryName") String categoryName,
                        @Param("version") Long version);

    @Update("""
            UPDATE tb_bbs_post
            SET content_status = 'DELETED',
                post_status = 0,
                is_deleted = 1,
                version = version + 1,
                last_update_time = CURRENT_TIMESTAMP(3)
            WHERE post_id = #{postId}
              AND publish_user_id = #{authorId}
              AND content_status IN ('NORMAL', 'HIDDEN')
              AND version = #{version}
            """)
    int deleteOwnedPost(@Param("postId") Long postId,
                        @Param("authorId") Long authorId,
                        @Param("version") Long version);
}
