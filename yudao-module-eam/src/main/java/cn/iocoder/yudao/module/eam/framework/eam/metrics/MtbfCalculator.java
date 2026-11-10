package cn.iocoder.yudao.module.eam.framework.eam.metrics;

import cn.iocoder.yudao.module.eam.dal.dataobject.EamRepairOrderDO;
import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MTBF/MTTR 可靠性指标计算器。
 *
 * MTBF（Mean Time Between Failures，平均故障间隔）= (窗口总时长 - 总维修时长) / 故障次数
 *   ——分子只算"设备本可以正常跑的时间"，维修占用的时间不属于"无故障运行"。
 * MTTR（Mean Time To Repair，平均修复时间）= 总维修时长 / 故障次数
 * 单位统一为小时；只统计"已完成"的维修工单（进行中的工单时长未定，纳入会虚增维修时长）。
 *
 * 纯函数、随查随算、不落库：统计窗口（月/季/年）由调用方传入，口径灵活。
 */
@UtilityClass
public class MtbfCalculator {

    /**
     * 计算单台设备在统计窗口内的可靠性指标。
     *
     * @param assetId     设备 ID
     * @param doneOrders  窗口内已完成的维修工单（status=DONE 且 finishTime 落在窗口内）
     * @param windowStart 统计窗口起点
     * @param windowEnd   统计窗口终点
     * @return 指标结果；故障 0 次时 mtbfHours/mttrHours 均为 null
     */
    public static MetricsResult calc(Long assetId, List<EamRepairOrderDO> doneOrders,
                                     LocalDateTime windowStart, LocalDateTime windowEnd) {
        int faultCount = doneOrders != null ? doneOrders.size() : 0;
        if (faultCount == 0) {
            // 除零无意义：MTBF 是"平均多久坏一次"，0 次故障时它趋近无穷大而不是 0。
            // 返回 null 让前端显示 "—"，避免误导成"这台设备每 0 小时就坏一次"。
            return new MetricsResult(assetId, 0, null, null);
        }
        // 总维修时长：只累加起止时间齐全的工单；缺时间的脏数据跳过，不污染整体口径
        double repairHours = 0.0;
        for (EamRepairOrderDO order : doneOrders) {
            if (order.getStartTime() == null || order.getFinishTime() == null) {
                continue;
            }
            repairHours += Duration.between(order.getStartTime(), order.getFinishTime()).toMinutes() / 60.0;
        }
        double windowHours = Duration.between(windowStart, windowEnd).toMinutes() / 60.0;
        // 窗口总时长 - 维修时长 = 无故障运行时长；防御负数（脏数据维修时长超过窗口）
        double mtbf = Math.max(windowHours - repairHours, 0.0) / faultCount;
        double mttr = repairHours / faultCount;
        return new MetricsResult(assetId, faultCount, mtbf, mttr);
    }
}
