package com.bbs.bbs.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bbs.bbs.bean.BBSUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


public interface BBSUserMapper extends BaseMapper<BBSUser> {
    @Select("SELECT COUNT(*) FROM tb_bbs_user")
    long countAll();

    @Update("UPDATE tb_bbs_user SET user_status = #{target}, update_time = CURRENT_TIMESTAMP(3) WHERE user_id = #{userId} AND user_status = #{expected}")
    int updateStatusIfCurrent(@org.apache.ibatis.annotations.Param("userId") Long userId,
                              @org.apache.ibatis.annotations.Param("expected") Byte expected,
                              @org.apache.ibatis.annotations.Param("target") Byte target);
}
