package cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - EAM 保养计划 Response VO")
public record EamMaintainPlanRespVO(

        @Schema(description = "计划 ID", example = "1")
        Long id,

        @Schema(description = "计划编号", example = "MP-2026-001")
        String planNo,

        @Schema(description = "设备 ID", example = "1")
        Long assetId,

        @Schema(description = "计划名称", example = "数控车床月度保养")
        String planName,

        @Schema(description = "周期类型: 1=按天数 2=按周数 3=按月数", example = "3")
        Integer cycleType,

        @Schema(description = "周期数值", example = "1")
        Integer cycleValue,

        @Schema(description = "提前几天生成工单", example = "3")
        Integer advanceDays,

        @Schema(description = "上次保养时间")
        LocalDateTime lastMaintainTime,

        @Schema(description = "下次保养时间")
        LocalDateTime nextMaintainTime,

        @Schema(description = "状态: 0=停用 1=启用", example = "1")
        Integer status,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
