package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 维修工单状态。完整闭环：待派工→已派工→维修中→待验收→已完成，
 * 前三个环节可取消；验收阶段不允许取消（要返工应走验收驳回，演示仓库未展开）。
 * 合法迁移边定义在 RepairOrderStateMachine。
 */
@Getter
@AllArgsConstructor
public enum EamRepairStatus {

    PENDING_DISPATCH(1, "待派工"),
    DISPATCHED(2, "已派工"),
    REPAIRING(3, "维修中"),
    PENDING_ACCEPT(4, "待验收"),
    DONE(5, "已完成"),
    CANCELLED(6, "已取消");

    private final Integer code;
    private final String name;
}
