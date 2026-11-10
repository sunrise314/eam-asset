package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 维修工单优先级。紧急工单应插队派工，演示仓库仅作标识，未实现抢占逻辑。
 */
@Getter
@AllArgsConstructor
public enum EamRepairPriority {

    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高"),
    URGENT(4, "紧急");

    private final Integer code;
    private final String name;
}
