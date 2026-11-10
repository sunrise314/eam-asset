package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 保养计划状态。停用的计划不参与 MaintainPlanEngine 扫描。
 */
@Getter
@AllArgsConstructor
public enum EamPlanStatus {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String name;
}
