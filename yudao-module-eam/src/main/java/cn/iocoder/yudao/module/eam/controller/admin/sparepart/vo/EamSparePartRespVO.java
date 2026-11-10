package cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - EAM 备件 Response VO")
public record EamSparePartRespVO(

        @Schema(description = "备件 ID", example = "1")
        Long id,

        @Schema(description = "备件编号", example = "SP-BEAR-001")
        String partNo,

        @Schema(description = "备件名称", example = "主轴轴承")
        String name,

        @Schema(description = "规格型号", example = "SKF 7010C")
        String spec,

        @Schema(description = "计量单位", example = "套")
        String unit,

        @Schema(description = "当前库存", example = "8")
        Integer stock,

        @Schema(description = "安全库存", example = "3")
        Integer safetyStock,

        @Schema(description = "适用设备 ID 列表", example = "[1,2]")
        List<Long> assetIds,

        @Schema(description = "货架位置", example = "A库-01-03")
        String location,

        @Schema(description = "备注")
        String remark,

        @Schema(description = "创建时间")
        LocalDateTime createTime,

        @Schema(description = "更新时间")
        LocalDateTime updateTime
) {
}
