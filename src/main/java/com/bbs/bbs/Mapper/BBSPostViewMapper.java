package com.bbs.bbs.Mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface BBSPostViewMapper {

    @Insert("""
            INSERT INTO tb_bbs_post_view (post_id, user_id, create_time)
            VALUES (#{postId}, #{userId}, CURRENT_TIMESTAMP(3))
            """)
    int insertView(@Param("postId") Long postId,
                   @Param("userId") Long userId);
}
