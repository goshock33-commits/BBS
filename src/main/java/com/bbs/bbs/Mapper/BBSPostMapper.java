package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSPost;
import org.apache.ibatis.annotations.Param;
import com.bbs.bbs.web.model.ContentStatus;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 17:08:36
 * @Version 1.0
 */

public interface BBSPostMapper extends BaseMapper<BBSPost> {
    @Update("""
            UPDATE tb_bbs_post
            SET content_status = #{target}, is_deleted = #{isDeleted},
                post_status = CASE WHEN #{target} = 'DELETED' THEN 0 ELSE 1 END,
                version = version + 1, last_update_time = CURRENT_TIMESTAMP(3)
            WHERE post_id = #{postId}
              AND content_status = #{expected}
              AND is_deleted = CASE WHEN #{expected} = 'DELETED' THEN 1 ELSE 0 END
            """)
    int updateModerationStatus(@Param("postId") Long postId,
                               @Param("expected") ContentStatus expected,
                               @Param("target") ContentStatus target,
                               @Param("isDeleted") Byte isDeleted);

    @Select("SELECT COUNT(*) FROM tb_bbs_post")
    long countAll();

    @Select("""
            <script>
            SELECT p.*, u.nick_name AS nick_name
            FROM tb_bbs_post p LEFT JOIN tb_bbs_user u ON u.user_id = p.publish_user_id
            WHERE (#{request.status} IS NULL OR p.content_status = #{request.status})
              AND (#{request.categoryId} IS NULL OR p.post_category_id = #{request.categoryId})
              AND (#{request.authorId} IS NULL OR p.publish_user_id = #{request.authorId})
              AND (#{request.from} IS NULL OR p.create_time &gt;= #{request.from})
              AND (#{request.to} IS NULL OR p.create_time &lt;= #{request.to})
            ORDER BY p.create_time DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    java.util.List<BBSPost> selectAdminPage(@Param("request") com.bbs.bbs.web.dto.admin.AdminPostFilterRequest request,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM tb_bbs_post p
            WHERE (#{request.status} IS NULL OR p.content_status = #{request.status})
              AND (#{request.categoryId} IS NULL OR p.post_category_id = #{request.categoryId})
              AND (#{request.authorId} IS NULL OR p.publish_user_id = #{request.authorId})
              AND (#{request.from} IS NULL OR p.create_time &gt;= #{request.from})
              AND (#{request.to} IS NULL OR p.create_time &lt;= #{request.to})
            </script>
            """)
    long countAdminPage(@Param("request") com.bbs.bbs.web.dto.admin.AdminPostFilterRequest request);
    @Update("UPDATE tb_bbs_post SET post_comments = post_comments + 1 WHERE post_id = #{postId}")
    int incrementCommentCount(@Param("postId") Long postId);
    @Update("UPDATE tb_bbs_post SET post_comments = CASE WHEN post_comments > 0 THEN post_comments - 1 ELSE 0 END WHERE post_id = #{postId}")
    int decrementCommentCount(@Param("postId") Long postId);
    @Update("UPDATE tb_bbs_post SET post_collects = post_collects + 1 WHERE post_id = #{postId}")
    int incrementCollectCount(@Param("postId") Long postId);
    @Update("UPDATE tb_bbs_post SET post_collects = CASE WHEN post_collects > 0 THEN post_collects - 1 ELSE 0 END WHERE post_id = #{postId}")
    int decrementCollectCount(@Param("postId") Long postId);
    @Update("UPDATE tb_bbs_post SET post_views = post_views + 1 WHERE post_id = #{postId} AND content_status = 'NORMAL' AND is_deleted = 0")
    int incrementViewCount(@Param("postId") Long postId);

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
