package cn.iocoder.yudao.module.eam.framework.eam.metrics;

/**
 * 设备可靠性指标结果。
 *
 * @param assetId    设备 ID
 * @param faultCount 统计窗口内故障次数（已完成维修工单数）
 * @param mtbfHours  平均故障间隔（小时）；0 次故障时为 null，前端显示 "—"
 * @param mttrHours  平均修复时间（小时）；0 次故障时为 null，前端显示 "—"
 */
public record MetricsResult(
        Long assetId,
        Integer faultCount,
        Double mtbfHours,
        Double mttrHours
) {
}
