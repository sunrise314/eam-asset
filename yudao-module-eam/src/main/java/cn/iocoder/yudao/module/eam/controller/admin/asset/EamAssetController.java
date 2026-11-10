package cn.iocoder.yudao.module.eam.controller.admin.asset;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetRespVO;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamAssetDO;
import cn.iocoder.yudao.module.eam.service.asset.EamAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * EAM 设备台账 Controller。
 */
@Tag(name = "管理后台 - EAM 设备台账")
@RestController
@RequestMapping("/api/admin/eam/asset")
@RequiredArgsConstructor
@Validated
public class EamAssetController {

    private final EamAssetService assetService;

    @PostMapping("/create")
    @Operation(summary = "创建设备台账")
    public CommonResult<Long> createAsset(@Valid @RequestBody EamAssetSaveReqVO createReqVO) {
        return success(assetService.createAsset(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备台账")
    public CommonResult<Boolean> updateAsset(@Valid @RequestBody EamAssetSaveReqVO updateReqVO) {
        assetService.updateAsset(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备台账")
    @Parameter(name = "id", description = "设备 ID", required = true)
    public CommonResult<Boolean> deleteAsset(@RequestParam("id") Long id) {
        assetService.deleteAsset(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取设备台账详情")
    @Parameter(name = "id", description = "设备 ID", required = true, example = "1")
    public CommonResult<EamAssetRespVO> getAsset(@RequestParam("id") Long id) {
        return success(toRespVO(assetService.getAsset(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询设备台账")
    public CommonResult<PageResult<EamAssetRespVO>> getAssetPage(@Valid EamAssetPageReqVO pageReqVO) {
        PageResult<EamAssetDO> page = assetService.getAssetPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(EamAssetController::toRespVO).toList(), page.getTotal()));
    }

    // ---------- VO 转换 ----------

    private static EamAssetRespVO toRespVO(EamAssetDO a) {
        return new EamAssetRespVO(a.getId(), a.getAssetNo(), a.getName(), a.getModel(),
                a.getCategory(), a.getLocation(), a.getStatus(), a.getPurchaseDate(),
                a.getWarrantyUntil(), a.getResponsibleUserId(), a.getAttributes(),
                a.getRemark(), a.getCreateTime(), a.getUpdateTime());
    }
}
