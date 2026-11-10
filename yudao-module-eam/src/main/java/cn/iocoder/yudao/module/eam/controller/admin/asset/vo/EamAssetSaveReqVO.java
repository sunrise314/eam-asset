package cn.iocoder.yudao.module.eam.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Map;

@Schema(description = "管理后台 - EAM 设备台账新增/修改 Request VO")
public record EamAssetSaveReqVO(

        @Schema(description = "设备 ID", example = "1")
        Long id,

        @Schema(description = "设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "EQ-CNC-001")
        @NotBlank(message = "设备编号不能为空")
        String assetNo,

        @Schema(description = "设备名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "数控车床")
        @NotBlank(message = "设备名称不能为空")
        String name,

        @Schema(description = "规格型号", example = "CKA6150")
        String model,

        @Schema(description = "设备分类", example = "机加")
        String category,

        @Schema(description = "安装位置", example = "一车间-A线")
        String location,

        @Schema(description = "状态: 1=运行 2=停机 3=维修中 4=报废", example = "1")
        Integer status,

        @Schema(description = "采购日期")
        LocalDate purchaseDate,

        @Schema(description = "质保到期日")
        LocalDate warrantyUntil,

        @Schema(description = "责任人用户 ID", example = "1001")
        Long responsibleUserId,

        @Schema(description = "设备扩展属性")
        Map<String, Object> attributes,

        @Schema(description = "备注")
        String remark
) {
}
