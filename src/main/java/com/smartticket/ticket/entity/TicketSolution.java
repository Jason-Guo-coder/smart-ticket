package com.smartticket.ticket.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 解决方案（ARCHITECTURE §4 ticket_solution）。 */
@Data
@TableName("ticket_solution")
public class TicketSolution {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    private Long engineerId;
    private String solutionText;
    private String imageUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
