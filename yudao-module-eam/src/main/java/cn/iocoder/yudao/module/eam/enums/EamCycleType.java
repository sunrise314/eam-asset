package cn.iocoder.yudao.module.eam.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 保养周期类型。与 cycleValue 组成二元组表达"距上次完成 N 天/周/月"的滑动窗口，
 * 由 NextDateCalculator 换算成下一次保养时间。
 */
@Getter
@AllArgsConstructor
public enum EamCycleType {

    DAYS(1, "按天数"),
    WEEKS(2, "按周数"),
    MONTHS(3, "按月数");

    private final Integer code;
    private final String name;
}
