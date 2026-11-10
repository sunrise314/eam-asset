package cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "管理后台 - EAM 维修工单新增/修改 Request VO")
public record EamRepairOrderSaveReqVO(

        @Schema(description = "工单 ID", example = "1")
        Long id,

        @Schema(description = "工单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "RO-20261108-001")
        @NotBlank(message = "工单号不能为空")
        String orderNo,

        @Schema(description = "设备 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
        @NotNull(message = "设备不能为空")
        Long assetId,

        @Schema(description = "故障描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "液压系统压力不足，合模无力")
        @NotBlank(message = "故障描述不能为空")
        String faultDesc,

        @Schema(description = "优先级: 1=低 2=中 3=高 4=紧急", example = "3")
        Integer priority,

        @Schema(description = "报修人", example = "张三")
        String reporter
) {
}
