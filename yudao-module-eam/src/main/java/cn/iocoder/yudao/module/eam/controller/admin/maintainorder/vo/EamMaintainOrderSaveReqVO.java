package cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "管理后台 - EAM 保养工单新增/修改 Request VO")
public record EamMaintainOrderSaveReqVO(

        @Schema(description = "工单 ID", example = "1")
        Long id,

        @Schema(description = "工单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MO-20261110-1")
        @NotBlank(message = "工单号不能为空")
        String orderNo,

        @Schema(description = "保养计划 ID", example = "1")
        Long planId,

        @Schema(description = "设备 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "设备不能为空")
        Long assetId,

        @Schema(description = "计划保养日期")
        LocalDate planDate,

        @Schema(description = "执行人用户 ID", example = "1002")
        Long assigneeId,

        @Schema(description = "保养内容")
        String content
) {
}
