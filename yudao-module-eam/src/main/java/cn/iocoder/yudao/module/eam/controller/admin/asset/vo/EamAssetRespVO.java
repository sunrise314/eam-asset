package cn.iocoder.yudao.module.eam.controller.admin.asset.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "管理后台 - EAM 设备台账 Response VO")
public record EamAssetRespVO(

        @Schema(description = "设备 ID", example = "1")
        Long id,

        @Schema(description = "设备编号", example = "EQ-CNC-001")
        String assetNo,

        @Schema(description = "设备名称", example = "数控车床")
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
        String remark,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
