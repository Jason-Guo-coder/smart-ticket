package com.smartticket.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.ticket.entity.TicketSolution;
import com.smartticket.ticket.vo.TicketSolutionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TicketSolutionMapper extends BaseMapper<TicketSolution> {

    /** 工单的解决方案（关联工程师姓名，按提交时间升序）。 */
    @Select("""
            SELECT s.engineer_id AS engineerId, u.real_name AS engineerName,
                   s.solution_text AS solutionText, s.image_url AS imageUrl,
                   s.create_time AS createTime
            FROM ticket_solution s
            LEFT JOIN sys_user u ON u.id = s.engineer_id
            WHERE s.ticket_id = #{ticketId}
            ORDER BY s.create_time ASC, s.id ASC
            """)
    List<TicketSolutionVO> selectByTicket(@Param("ticketId") Long ticketId);
}
