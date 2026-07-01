package com.smartticket.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.ai.model.SimilarTicket;
import com.smartticket.ticket.entity.Ticket;
import com.smartticket.ticket.vo.TicketDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 工程师待办：待接单(PENDING，任何人可抢) + 本人处理中/待验收，
     * 按优先级(高→低，null 最后)、创建时间升序。
     */
    @Select("""
            SELECT * FROM ticket
            WHERE deleted = 0
              AND ( status = 'PENDING'
                    OR (assignee_id = #{engineerId} AND status IN ('PROCESSING', 'ACCEPTING')) )
            ORDER BY FIELD(priority, 'LOW', 'MID', 'HIGH') DESC, create_time ASC
            """)
    List<Ticket> selectTodoList(@Param("engineerId") Long engineerId);

    /** 本人已完成工单的平均处理时长（分钟；无数据返回 null）。 */
    @Select("""
            SELECT AVG(TIMESTAMPDIFF(MINUTE, create_time, update_time))
            FROM ticket
            WHERE deleted = 0 AND assignee_id = #{engineerId} AND status IN ('DONE', 'RATED')
            """)
    Double selectAvgHandleMinutes(@Param("engineerId") Long engineerId);

    /** 工单详情表头（关联报修人/受理人姓名；标签与时间线在服务层补齐）。 */
    @Select("""
            SELECT t.id, t.ticket_no AS ticketNo, t.title, t.content, t.image_url AS imageUrl,
                   t.category, t.priority, t.status,
                   t.creator_id AS creatorId, cu.real_name AS creatorName,
                   t.assignee_id AS assigneeId, au.real_name AS assigneeName,
                   t.sla_deadline AS slaDeadline, t.sla_overdue AS slaOverdue,
                   t.create_time AS createTime, t.update_time AS updateTime,
                   ev.score AS evalScore, ev.tags AS evalTags,
                   ev.comment AS evalComment, ev.create_time AS evalTime
            FROM ticket t
            LEFT JOIN sys_user cu ON cu.id = t.creator_id
            LEFT JOIN sys_user au ON au.id = t.assignee_id
            LEFT JOIN evaluation ev ON ev.ticket_id = t.id
            WHERE t.id = #{id} AND t.deleted = 0
            """)
    TicketDetailVO selectDetail(@Param("id") Long id);

    /**
     * 相似历史工单（MVP：类别 + 关键词 LIKE，取已完成且有解决方案的，Top N）。
     * category 为空时退化为仅按关键词匹配。
     */
    @Select("""
            SELECT t.ticket_no AS ticketNo, t.title AS title, s.solution_text AS solution
            FROM ticket t
            LEFT JOIN ticket_solution s ON s.ticket_id = t.id
            WHERE t.deleted = 0
              AND t.status IN ('DONE', 'RATED')
              AND (#{category} IS NULL OR t.category = #{category})
              AND (#{keyword} IS NULL OR t.title LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY t.create_time DESC
            LIMIT #{limit}
            """)
    List<SimilarTicket> selectSimilar(@Param("category") String category,
                                      @Param("keyword") String keyword,
                                      @Param("limit") int limit);
}
