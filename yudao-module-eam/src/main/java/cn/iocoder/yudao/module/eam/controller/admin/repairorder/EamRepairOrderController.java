package cn.iocoder.yudao.module.eam.controller.admin.repairorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderRespVO;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamRepairOrderDO;
import cn.iocoder.yudao.module.eam.framework.eam.metrics.MetricsResult;
import cn.iocoder.yudao.module.eam.service.repairorder.EamRepairOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * EAM 维修工单 Controller。
 */
@Tag(name = "管理后台 - EAM 维修工单")
@RestController
@RequestMapping("/api/admin/eam/repair-order")
@RequiredArgsConstructor
@Validated
public class EamRepairOrderController {

    private final EamRepairOrderService repairOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建维修工单（报修）")
    public CommonResult<Long> createRepairOrder(@Valid @RequestBody EamRepairOrderSaveReqVO createReqVO) {
        return success(repairOrderService.createRepairOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新维修工单")
    public CommonResult<Boolean> updateRepairOrder(@Valid @RequestBody EamRepairOrderSaveReqVO updateReqVO) {
        repairOrderService.updateRepairOrder(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除维修工单")
    @Parameter(name = "id", description = "工单 ID", required = true)
    public CommonResult<Boolean> deleteRepairOrder(@RequestParam("id") Long id) {
        repairOrderService.deleteRepairOrder(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取维修工单详情")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    public CommonResult<EamRepairOrderRespVO> getRepairOrder(@RequestParam("id") Long id) {
        return success(toRespVO(repairOrderService.getRepairOrder(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询维修工单")
    public CommonResult<PageResult<EamRepairOrderRespVO>> getRepairOrderPage(@Valid EamRepairOrderPageReqVO pageReqVO) {
        PageResult<EamRepairOrderDO> page = repairOrderService.getRepairOrderPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(EamRepairOrderController::toRespVO).toList(), page.getTotal()));
    }

    @PutMapping("/dispatch")
    @Operation(summary = "派工（待派工 → 已派工）")
    public CommonResult<Boolean> dispatch(@RequestParam("id") Long id,
                                          @RequestParam("assigneeId") Long assigneeId) {
        repairOrderService.dispatch(id, assigneeId);
        return success(true);
    }

    @PutMapping("/start")
    @Operation(summary = "开始维修（已派工 → 维修中）")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    public CommonResult<Boolean> start(@RequestParam("id") Long id) {
        repairOrderService.start(id);
        return success(true);
    }

    @PutMapping("/finish")
    @Operation(summary = "完工（维修中 → 待验收）")
    public CommonResult<Boolean> finish(@RequestParam("id") Long id,
                                        @RequestParam(value = "repairNote", required = false) String repairNote) {
        repairOrderService.finish(id, repairNote);
        return success(true);
    }

    @PutMapping("/accept")
    @Operation(summary = "验收通过（待验收 → 已完成）")
    public CommonResult<Boolean> accept(@RequestParam("id") Long id,
                                        @RequestParam(value = "acceptNote", required = false) String acceptNote) {
        repairOrderService.accept(id, acceptNote);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消维修工单（待派工/已派工/维修中 → 已取消）")
    @Parameter(name = "id", description = "工单 ID", required = true, example = "1")
    public CommonResult<Boolean> cancel(@RequestParam("id") Long id) {
        repairOrderService.cancel(id);
        return success(true);
    }

    @GetMapping("/metrics")
    @Operation(summary = "设备可靠性指标（MTBF/MTTR，单位小时；0 故障返回 null）")
    @Parameter(name = "assetId", description = "设备 ID", required = true, example = "3")
    @Parameter(name = "windowStart", description = "统计窗口起点", required = true, example = "2026-10-01T00:00:00")
    @Parameter(name = "windowEnd", description = "统计窗口终点", required = true, example = "2026-12-01T00:00:00")
    public CommonResult<MetricsResult> metrics(
            @RequestParam("assetId") Long assetId,
            @RequestParam("windowStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime windowStart,
            @RequestParam("windowEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime windowEnd) {
        return success(repairOrderService.calcMetrics(assetId, windowStart, windowEnd));
    }

    // ---------- VO 转换 ----------

    private static EamRepairOrderRespVO toRespVO(EamRepairOrderDO o) {
        return new EamRepairOrderRespVO(o.getId(), o.getOrderNo(), o.getAssetId(), o.getFaultDesc(),
                o.getPriority(), o.getStatus(), o.getReporter(), o.getAssigneeId(), o.getDispatchTime(),
                o.getStartTime(), o.getFinishTime(), o.getRepairNote(), o.getAcceptNote(),
                o.getVersion(), o.getCreateTime(), o.getUpdateTime());
    }
}
