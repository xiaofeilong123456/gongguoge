package com.gongguoge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gongguoge.pojo.MeritRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface MeritRecordMapper extends BaseMapper<MeritRecord> {

    @Select("SELECT * FROM t_merit_record WHERE user_id = #{userId} AND deleted = 1 ORDER BY deleted_at DESC")
    List<MeritRecord> selectRecycleByUser(@Param("userId") Long userId);

    @Update("UPDATE t_merit_record SET deleted = 0, deleted_at = NULL WHERE id = #{id} AND user_id = #{userId}")
    int restoreById(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM t_merit_record WHERE id = #{id} AND user_id = #{userId}")
    int physicalDeleteById(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM t_merit_record WHERE user_id = #{userId}")
    int physicalDeleteByUser(@Param("userId") Long userId);

    @Update("UPDATE t_merit_record SET deleted = 1, deleted_at = NOW() WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    int softDeleteById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 按 clientId 查询，包含已逻辑删除的记录。
     * MyBatis-Plus 的自动查询会过滤 deleted=1，导致「彻底删除」找不到回收站里的记录，
     * 因此这里用显式 SQL 绕过逻辑删除。
     */
    @Select("SELECT * FROM t_merit_record WHERE user_id = #{userId} AND client_id = #{clientId} LIMIT 1")
    MeritRecord selectByClientIdIncludingDeleted(@Param("userId") Long userId, @Param("clientId") String clientId);
}

