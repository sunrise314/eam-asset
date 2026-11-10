package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 保养工单状态。流转是线性的：待执行→执行中→已完成，任意非终态可取消。
 * 只有 3 条迁移边，Service 层直接 if 校验即可，没有再抽状态机
 * （维修工单 6 状态 5 边才有 RepairOrderStateMachine）。
 */
@Getter
@AllArgsConstructor
public enum EamMaintainOrderStatus {

    PENDING(1, "待执行"),
    IN_PROGRESS(2, "执行中"),
    DONE(3, "已完成"),
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String name;
}
