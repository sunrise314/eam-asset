package cn.iocoder.yudao.module.eam.controller.admin.maintainorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderRespVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainOrderDO;
import cn.iocoder.yudao.module.eam.service.maintainorder.EamMaintainOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * EAM 保养工单 Controller。
 */
@Tag(name = "管理后台 - EAM 保养工单")
@RestController
@RequestMapping("/api/admin/eam/maintain-order")
@RequiredArgsConstructor
@Validated
public class EamMaintainOrderController {

    private final EamMaintainOrderService maintainOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建保养工单（手工补录）")
    public CommonResult<Long> createMaintainOrder(@Valid @RequestBody EamMaintainOrderSaveReqVO createReqVO) {
        return success(maintainOrderService.createMaintainOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新保养工单")
    public CommonResult<Boolean> updateMaintainOrder(@Valid @RequestBody EamMaintainOrderSaveReqVO updateReqVO) {
        maintainOrderService.updateMaintainOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除保养工单")
    @Parameter(name = "id", description = "工单 ID", required = true)
    public CommonResult<Boolean> deleteMaintainOrder(@RequestParam("id") Long id) {
        maintainOrderService.deleteMaintainOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取保养工单详情")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    public CommonResult<EamMaintainOrderRespVO> getMaintainOrder(@RequestParam("id") Long id) {
        return success(toRespVO(maintainOrderService.getMaintainOrder(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询保养工单")
    public CommonResult<PageResult<EamMaintainOrderRespVO>> getMaintainOrderPage(@Valid EamMaintainOrderPageReqVO pageReqVO) {
        PageResult<EamMaintainOrderDO> page = maintainOrderService.getMaintainOrderPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(EamMaintainOrderController::toRespVO).toList(), page.getTotal()));
    }

    @PutMapping("/start")
    @Operation(summary = "开始执行（待执行 → 执行中）")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    public CommonResult<Boolean> start(@RequestParam("id") Long id) {
        maintainOrderService.start(id);
        return success(true);
    }

    @PutMapping("/finish")
    @Operation(summary = "完成执行（执行中 → 已完成）")
    public CommonResult<Boolean> finish(@RequestParam("id") Long id,
                                        @RequestParam(value = "resultNote", required = false) String resultNote) {
        maintainOrderService.finish(id, resultNote);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消保养工单（待执行/执行中 → 已取消）")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        maintainOrderService.cancel(id);
        return success(true);
    }

    // ---------- VO 转换 ----------

    private static EamMaintainOrderRespVO toRespVO(EamMaintainOrderDO o) {
        return new EamMaintainOrderRespVO(o.getId(), o.getOrderNo(), o.getPlanId(), o.getAssetId(),
                o.getPlanDate(), o.getStatus(), o.getAssigneeId(), o.getStartTime(), o.getFinishTime(),
                o.getContent(), o.getResultNote(), o.getVersion(), o.getCreateTime(), o.getUpdateTime());
    }
}
