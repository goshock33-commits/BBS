package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSPostComment;
import com.bbs.util.PageQueryUtil;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import com.bbs.bbs.web.model.ContentStatus;
import org.apache.ibatis.annotations.Update;

/**
 * @Author qq
 * @Description
 * @Date 2026-03-19 17:10:27
 * @Version 1.0
 */

public interface BBSCommentMapper extends BaseMapper<BBSPostComment> {
    @Select("SELECT COUNT(*) FROM tb_bbs_post_comment")
    long countAll();

    @Update("""
            UPDATE tb_bbs_post_comment
            SET content_status = #{target}, is_deleted = #{isDeleted},
                version = version + 1
            WHERE comment_id = #{commentId}
              AND content_status = #{expected}
              AND is_deleted = CASE WHEN #{expected} = 'DELETED' THEN 1 ELSE 0 END
            """)
    int updateModerationStatus(@Param("commentId") Long commentId,
                               @Param("expected") ContentStatus expected,
                               @Param("target") ContentStatus target,
                               @Param("isDeleted") Byte isDeleted);

    @Select("""
            <script>
            SELECT c.*, u.nick_name AS nick_name
            FROM tb_bbs_post_comment c LEFT JOIN tb_bbs_user u ON u.user_id = c.comment_user_id
            WHERE (#{request.status} IS NULL OR c.content_status = #{request.status})
              AND (#{request.postId} IS NULL OR c.post_id = #{request.postId})
              AND (#{request.authorId} IS NULL OR c.comment_user_id = #{request.authorId})
              AND (#{request.from} IS NULL OR c.comment_create_time &gt;= #{request.from})
              AND (#{request.to} IS NULL OR c.comment_create_time &lt;= #{request.to})
            ORDER BY c.comment_create_time DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    java.util.List<BBSPostComment> selectAdminPage(@Param("request") com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest request,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM tb_bbs_post_comment c
            WHERE (#{request.status} IS NULL OR c.content_status = #{request.status})
              AND (#{request.postId} IS NULL OR c.post_id = #{request.postId})
              AND (#{request.authorId} IS NULL OR c.comment_user_id = #{request.authorId})
              AND (#{request.from} IS NULL OR c.comment_create_time &gt;= #{request.from})
              AND (#{request.to} IS NULL OR c.comment_create_time &lt;= #{request.to})
            </script>
            """)
    long countAdminPage(@Param("request") com.bbs.bbs.web.dto.admin.AdminCommentFilterRequest request);
    @Select("SELECT * FROM tb_bbs_post_comment WHERE comment_id = #{id} LIMIT 1")
    BBSPostComment selectIncludingDeleted(@Param("id") Long id);
    @Delete("DELETE FROM tb_bbs_post_comment WHERE comment_id = #{commentId} AND comment_user_id = #{userId}")
    int deleteByOwnerOrPostOwner(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
