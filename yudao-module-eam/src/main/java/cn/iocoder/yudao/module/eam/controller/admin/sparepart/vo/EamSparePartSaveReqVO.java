package cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "管理后台 - EAM 备件新增/修改 Request VO")
public record EamSparePartSaveReqVO(

        @Schema(description = "备件 ID", example = "1")
        Long id,

        @Schema(description = "备件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SP-BEAR-001")
        @NotBlank(message = "备件编号不能为空")
        String partNo,

        @Schema(description = "备件名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "主轴轴承")
        @NotBlank(message = "备件名称不能为空")
        String name,

        @Schema(description = "规格型号", example = "SKF 7010C")
        String spec,

        @Schema(description = "计量单位", example = "套")
        String unit,

        @Schema(description = "初始库存（仅创建时生效，后续变动请走出入库）", example = "10")
        Integer stock,

        @Schema(description = "安全库存", example = "3")
        Integer safetyStock,

        @Schema(description = "适用设备 ID 列表", example = "[1,2]")
        List<Long> assetIds,

        @Schema(description = "货架位置", example = "A库-01-03")
        String location,

        @Schema(description = "备注")
        String remark
) {
}
