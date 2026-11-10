package cn.iocoder.yudao.module.eam.service.maintainplan;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainOrderDO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainPlanDO;
import cn.iocoder.yudao.module.eam.dal.mysql.EamMaintainOrderMapper;
import cn.iocoder.yudao.module.eam.dal.mysql.EamMaintainPlanMapper;
import cn.iocoder.yudao.module.eam.enums.EamPlanStatus;
import cn.iocoder.yudao.module.eam.framework.eam.maintain.MaintainPlanEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EAM 保养计划 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class EamMaintainPlanServiceImpl implements EamMaintainPlanService {

    private static final ErrorCode ERROR_CODE_NOT_EXISTS = new ErrorCode(1_003_500, "保养计划不存在: id={}");

    private final EamMaintainPlanMapper maintainPlanMapper;
    private final EamMaintainOrderMapper maintainOrderMapper;

    @Override
    public Long createMaintainPlan(EamMaintainPlanSaveReqVO createReqVO) {
        EamMaintainPlanDO plan = new EamMaintainPlanDO();
        plan.setPlanNo(createReqVO.planNo());
        plan.setAssetId(createReqVO.assetId());
        plan.setPlanName(createReqVO.planName());
        plan.setCycleType(createReqVO.cycleType());
        plan.setCycleValue(createReqVO.cycleValue());
        plan.setAdvanceDays(createReqVO.advanceDays() != null ? createReqVO.advanceDays() : 0);
        plan.setLastMaintainTime(createReqVO.lastMaintainTime());
        plan.setNextMaintainTime(createReqVO.nextMaintainTime());
        plan.setStatus(createReqVO.status() != null ? createReqVO.status() : EamPlanStatus.ENABLED.getCode());
        plan.setRemark(createReqVO.remark());
        maintainPlanMapper.insert(plan);
        return plan.getId();
    }

    @Override
    public void updateMaintainPlan(EamMaintainPlanSaveReqVO updateReqVO) {
        validatePlanExists(updateReqVO.id());
        EamMaintainPlanDO updateObj = new EamMaintainPlanDO();
        updateObj.setId(updateReqVO.id());
        updateObj.setPlanNo(updateReqVO.planNo());
        updateObj.setAssetId(updateReqVO.assetId());
        updateObj.setPlanName(updateReqVO.planName());
        updateObj.setCycleType(updateReqVO.cycleType());
        updateObj.setCycleValue(updateReqVO.cycleValue());
        updateObj.setAdvanceDays(updateReqVO.advanceDays());
        updateObj.setLastMaintainTime(updateReqVO.lastMaintainTime());
        updateObj.setNextMaintainTime(updateReqVO.nextMaintainTime());
        updateObj.setStatus(updateReqVO.status());
        updateObj.setRemark(updateReqVO.remark());
        maintainPlanMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaintainPlan(Long id) {
        validatePlanExists(id);
        maintainPlanMapper.deleteById(id);
    }

    @Override
    public EamMaintainPlanDO getMaintainPlan(Long id) {
        EamMaintainPlanDO plan = maintainPlanMapper.selectById(id);
        if (plan == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
        return plan;
    }

    @Override
    public PageResult<EamMaintainPlanDO> getMaintainPlanPage(EamMaintainPlanPageReqVO pageReqVO) {
        return maintainPlanMapper.selectPage(pageReqVO);
    }

    /**
     * 扫描启用计划并生成到期保养工单。
     *
     * 职责切分：引擎（MaintainPlanEngine）只做纯函数计算——要生成哪些工单、
     * 计划时间推进到哪儿；本方法负责把结果落库。演示仓库由接口手动触发，
     * 商用应挂 XXL-Job/Quartz 每日凌晨调度本方法。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateOrders() {
        // 1. 捞启用中的计划（引擎内部会再过滤一次，双保险）
        List<EamMaintainPlanDO> plans = maintainPlanMapper.selectListByStatus(EamPlanStatus.ENABLED.getCode());
        if (plans.isEmpty()) {
            return 0;
        }
        // 2. 引擎纯函数计算：返回工单列表，并就地推进命中计划的 last/next 时间
        List<EamMaintainOrderDO> orders = MaintainPlanEngine.scanAndGenerate(plans, LocalDate.now());
        // 3. 落库：批量插工单 + 只更新被推进过的计划（未命中的计划不动，避免无效 UPDATE）
        Set<Long> advancedPlanIds = new HashSet<>();
        for (EamMaintainOrderDO order : orders) {
            maintainOrderMapper.insert(order);
            advancedPlanIds.add(order.getPlanId());
        }
        for (EamMaintainPlanDO plan : plans) {
            if (advancedPlanIds.contains(plan.getId())) {
                maintainPlanMapper.updateById(plan);
            }
        }
        return orders.size();
    }

    private void validatePlanExists(Long id) {
        if (maintainPlanMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
    }
}
