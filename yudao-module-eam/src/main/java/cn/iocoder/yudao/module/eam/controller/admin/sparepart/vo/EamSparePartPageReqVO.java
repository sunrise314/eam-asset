package cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 备件分页 Request VO。
 */
@Schema(description = "管理后台 - EAM 备件分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EamSparePartPageReqVO extends PageParam {

    @Schema(description = "备件编号", example = "SP-BEAR-001")
    private String partNo;

    @Schema(description = "备件名称（模糊）", example = "主轴轴承")
    private String name;
}
