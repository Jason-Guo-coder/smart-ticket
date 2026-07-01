package com.smartticket.evaluation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 工单评价（ARCHITECTURE §4 evaluation，ticket_id 唯一）。 */
@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;
    /** 1-5 星。 */
    private Integer score;
    /** 标签，逗号分隔。 */
    private String tags;
    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
