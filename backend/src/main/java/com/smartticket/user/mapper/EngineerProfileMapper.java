package com.smartticket.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.user.entity.EngineerProfile;
import com.smartticket.user.vo.EngineerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EngineerProfileMapper extends BaseMapper<EngineerProfile> {

    /**
     * 按类别技能选负载最低的工程师（AI 派单建议 / 自动分配复用）。
     * category 为空时在全部工程师里选负载最低者。
     */
    @Select("""
            SELECT u.id AS userId, u.real_name AS realName, u.dept AS dept,
                   ep.current_load AS currentLoad, ep.category_skills AS categorySkills
            FROM engineer_profile ep
            JOIN sys_user u ON u.id = ep.user_id AND u.deleted = 0 AND u.status = 1
            WHERE (#{category} IS NULL OR ep.category_skills LIKE CONCAT('%', #{category}, '%'))
            ORDER BY ep.current_load ASC
            LIMIT 1
            """)
    EngineerVO selectBestByCategory(@Param("category") String category);

    /** 全部工程师负载（派单页/工程师管理复用）。 */
    @Select("""
            SELECT u.id AS userId, u.real_name AS realName, u.dept AS dept,
                   ep.current_load AS currentLoad, ep.category_skills AS categorySkills
            FROM engineer_profile ep
            JOIN sys_user u ON u.id = ep.user_id AND u.deleted = 0
            ORDER BY ep.current_load ASC
            """)
    List<EngineerVO> selectAllLoad();
}
