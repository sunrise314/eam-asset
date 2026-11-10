package cn.iocoder.yudao.module.eam.framework.eam.maintain;

import cn.iocoder.yudao.module.eam.enums.EamCycleType;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

/**
 * 保养周期下次日期计算器。
 *
 * 为什么不用 Cron 表达式：Cron 表达的是"日历定点"（每月 1 号、每周一），
 * 而设备保养周期是"相对上次完成时间"的滑动窗口——设备 1 月 5 号做完保养、周期 30 天，
 * 下次就是 2 月 4 号；若延期到 1 月 10 号才做完，下次也跟着滑到 2 月 9 号。
 * 用 Cron 会把全厂设备挤到固定日期扎堆保养，且无法表达"完成时间滑动"语义，
 * 所以这里用 (cycleType, cycleValue) 二元组 + 基准时间做 plus 运算。
 */
@UtilityClass
public class NextDateCalculator {

    /**
     * 计算下次保养时间。
     *
     * @param type       周期类型：按天/按周/按月
     * @param cycleValue 周期数值，必须为正整数
     * @param base       基准时间（通常是上次保养完成时间；首次为计划录入时间）
     * @return 下次保养时间
     */
    public static LocalDateTime next(EamCycleType type, int cycleValue, LocalDateTime base) {
        if (cycleValue <= 0) {
            throw new IllegalArgumentException("保养周期数值必须为正整数: " + cycleValue);
        }
        if (type == null || base == null) {
            throw new IllegalArgumentException("周期类型与基准时间不能为空");
        }
        switch (type) {
            case DAYS:
                return base.plusDays(cycleValue);
            case WEEKS:
                return base.plusWeeks(cycleValue);
            case MONTHS:
                return base.plusMonths(cycleValue);
            default:
                throw new IllegalArgumentException("未知保养周期类型: " + type);
        }
    }
}
