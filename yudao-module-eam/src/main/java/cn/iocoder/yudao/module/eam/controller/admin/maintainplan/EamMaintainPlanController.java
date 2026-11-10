package cn.iocoder.yudao.module.eam.controller.admin.maintainplan;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanRespVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainPlanDO;
import cn.iocoder.yudao.module.eam.service.maintainplan.EamMaintainPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * EAM 保养计划 Controller。
 */
@Tag(name = "管理后台 - EAM 保养计划")
@RestController
@RequestMapping("/api/admin/eam/maintain-plan")
@RequiredArgsConstructor
@Validated
public class EamMaintainPlanController {

    private final EamMaintainPlanService maintainPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建保养计划")
    public CommonResult<Long> createMaintainPlan(@Valid @RequestBody EamMaintainPlanSaveReqVO createReqVO) {
        return success(maintainPlanService.createMaintainPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新保养计划")
    public CommonResult<Boolean> updateMaintainPlan(@Valid @RequestBody EamMaintainPlanSaveReqVO updateReqVO) {
        maintainPlanService.updateMaintainPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除保养计划")
    @Parameter(name = "id", description = "计划 ID", required = true)
    public CommonResult<Boolean> deleteMaintainPlan(@RequestParam("id") Long id) {
        maintainPlanService.deleteMaintainPlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取保养计划详情")
    @Parameter(name = "id", description = "计划 ID", required = true, example = "1")
    public CommonResult<EamMaintainPlanRespVO> getMaintainPlan(@RequestParam("id") Long id) {
        return success(toRespVO(maintainPlanService.getMaintainPlan(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询保养计划")
    public CommonResult<PageResult<EamMaintainPlanRespVO>> getMaintainPlanPage(@Valid EamMaintainPlanPageReqVO pageReqVO) {
        PageResult<EamMaintainPlanDO> page = maintainPlanService.getMaintainPlanPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(EamMaintainPlanController::toRespVO).toList(), page.getTotal()));
    }

    @PostMapping("/generate-orders")
    @Operation(summary = "扫描启用计划，生成到期保养工单")
    public CommonResult<Integer> generateOrders() {
        return success(maintainPlanService.generateOrders());
    }

    // ---------- VO 转换 ----------

    private static EamMaintainPlanRespVO toRespVO(EamMaintainPlanDO p) {
        return new EamMaintainPlanRespVO(p.getId(), p.getPlanNo(), p.getAssetId(), p.getPlanName(),
                p.getCycleType(), p.getCycleValue(), p.getAdvanceDays(), p.getLastMaintainTime(),
                p.getNextMaintainTime(), p.getStatus(), p.getRemark(), p.getCreateTime(), p.getUpdateTime());
    }
}
