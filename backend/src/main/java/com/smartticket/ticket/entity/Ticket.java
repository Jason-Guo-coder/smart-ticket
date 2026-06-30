package com.smartticket.ticket.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/** 工单（ARCHITECTURE §4 ticket）。 */
@Data
@TableName("ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;
    private String title;
    private String content;
    private String imageUrl;

    /** 类别枚举名 NETWORK/HARDWARE/ACCOUNT/SOFTWARE/LOGISTICS。 */
    private String category;
    /** 优先级 HIGH/MID/LOW。 */
    private String priority;
    /** 状态机 PENDING/PROCESSING/ACCEPTING/DONE/RATED。 */
    private String status;

    private Long creatorId;
    private Long assigneeId;

    /** 乐观锁（派单并发安全 B3）。 */
    @Version
    private Integer version;

    private LocalDateTime slaDeadline;
    private Integer slaOverdue;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
