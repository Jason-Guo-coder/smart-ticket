package com.smartticket.dispatch.vo;

import com.smartticket.user.vo.EngineerVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 派单页数据：待派单卡片列表 + 工程师负载面板。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DispatchBoardVO {
    private List<PendingTicketVO> pending;
    private List<EngineerVO> engineers;
}
