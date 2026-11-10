package cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - EAM 保养计划新增/修改 Request VO")
public record EamMaintainPlanSaveReqVO(

        @Schema(description = "计划 ID", example = "1")
        Long id,

        @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MP-2026-001")
        @NotBlank(message = "计划编号不能为空")
        String planNo,

        @Schema(description = "设备 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "设备不能为空")
        Long assetId,

        @Schema(description = "计划名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "数控车床月度保养")
        @NotBlank(message = "计划名称不能为空")
        String planName,

        @Schema(description = "周期类型: 1=按天数 2=按周数 3=按月数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
        @NotNull(message = "周期类型不能为空")
        Integer cycleType,

        @Schema(description = "周期数值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "周期数值不能为空")
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
        String remark
) {
}
