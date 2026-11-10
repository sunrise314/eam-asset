package cn.iocoder.yudao.module.eam.controller.admin.sparepart;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartRespVO;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSparePartDO;
import cn.iocoder.yudao.module.eam.service.sparepart.EamSparePartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * EAM 备件 Controller。
 */
@Tag(name = "管理后台 - EAM 备件")
@RestController
@RequestMapping("/api/admin/eam/spare-part")
@RequiredArgsConstructor
@Validated
public class EamSparePartController {

    private final EamSparePartService sparePartService;

    @PostMapping("/create")
    @Operation(summary = "创建备件")
    public CommonResult<Long> createSparePart(@Valid @RequestBody EamSparePartSaveReqVO createReqVO) {
        return success(sparePartService.createSparePart(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新备件（不动库存，库存变动走出入库）")
    public CommonResult<Boolean> updateSparePart(@Valid @RequestBody EamSparePartSaveReqVO updateReqVO) {
        sparePartService.updateSparePart(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除备件")
    @Parameter(name = "id", description = "备件 ID", required = true)
    public CommonResult<Boolean> deleteSparePart(@RequestParam("id") Long id) {
        sparePartService.deleteSparePart(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取备件详情")
    @Parameter(name = "id", description = "备件 ID", required = true, example = "1")
    public CommonResult<EamSparePartRespVO> getSparePart(@RequestParam("id") Long id) {
        return success(toRespVO(sparePartService.getSparePart(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询备件")
    public CommonResult<PageResult<EamSparePartRespVO>> getSparePartPage(@Valid EamSparePartPageReqVO pageReqVO) {
        PageResult<EamSparePartDO> page = sparePartService.getSparePartPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(EamSparePartController::toRespVO).toList(), page.getTotal()));
    }

    @PostMapping("/inbound")
    @Operation(summary = "备件入库（返回最新备件，前端比对 stock < safetyStock 亮预警）")
    public CommonResult<EamSparePartRespVO> inbound(@RequestParam("partId") Long partId,
                                                    @RequestParam("quantity") Integer quantity,
                                                    @RequestParam(value = "bizOrderNo", required = false) String bizOrderNo,
                                                    @RequestParam(value = "operator", required = false) String operator) {
        return success(toRespVO(sparePartService.inbound(partId, quantity, bizOrderNo, operator)));
    }

    @PostMapping("/outbound")
    @Operation(summary = "备件出库（库存不足报错；返回最新备件，前端比对 stock < safetyStock 亮预警）")
    public CommonResult<EamSparePartRespVO> outbound(@RequestParam("partId") Long partId,
                                                     @RequestParam("quantity") Integer quantity,
                                                     @RequestParam(value = "bizOrderNo", required = false) String bizOrderNo,
                                                     @RequestParam(value = "operator", required = false) String operator) {
        return success(toRespVO(sparePartService.outbound(partId, quantity, bizOrderNo, operator)));
    }

    // ---------- VO 转换 ----------

    private static EamSparePartRespVO toRespVO(EamSparePartDO p) {
        return new EamSparePartRespVO(p.getId(), p.getPartNo(), p.getName(), p.getSpec(), p.getUnit(),
                p.getStock(), p.getSafetyStock(), p.getAssetIds(), p.getLocation(),
                p.getRemark(), p.getCreateTime(), p.getUpdateTime());
    }
}
