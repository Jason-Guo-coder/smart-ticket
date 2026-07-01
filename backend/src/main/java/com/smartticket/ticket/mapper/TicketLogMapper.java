package com.smartticket.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.ticket.entity.TicketLog;
import com.smartticket.ticket.vo.TicketLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TicketLogMapper extends BaseMapper<TicketLog> {

    /** 时间线（关联操作人姓名，按发生时间升序）。 */
    @Select("""
            SELECT l.action, l.operator_id AS operatorId, u.real_name AS operatorName,
                   l.remark, l.create_time AS createTime
            FROM ticket_log l
            LEFT JOIN sys_user u ON u.id = l.operator_id
            WHERE l.ticket_id = #{ticketId}
            ORDER BY l.create_time ASC, l.id ASC
            """)
    List<TicketLogVO> selectTimeline(@Param("ticketId") Long ticketId);
}
