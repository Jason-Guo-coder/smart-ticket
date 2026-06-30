package com.smartticket.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.ai.model.SimilarTicket;
import com.smartticket.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

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
