package cn.iocoder.yudao.module.eam.service.maintainorder;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainOrderDO;
import cn.iocoder.yudao.module.eam.dal.mysql.EamMaintainOrderMapper;
import cn.iocoder.yudao.module.eam.enums.EamMaintainOrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * EAM 保养工单 Service 实现。
 *
 * 保养工单流转是线性的（待执行→执行中→已完成，非终态可取消），只有 3 条迁移边，
 * 直接 if 校验当前状态即可，没有再抽状态机——维修工单 6 状态 5 边才有 RepairOrderStateMachine。
 */
@Service
@RequiredArgsConstructor
public class EamMaintainOrderServiceImpl implements EamMaintainOrderService {

    private static final ErrorCode ERROR_CODE_NOT_EXISTS = new ErrorCode(1_003_501, "保养工单不存在: id={}");
    private static final ErrorCode ERROR_CODE_STATUS_ILLEGAL = new ErrorCode(1_003_502, "保养工单状态非法: 当前={}, 期望={}");

    private final EamMaintainOrderMapper maintainOrderMapper;

    @Override
    public Long createMaintainOrder(EamMaintainOrderSaveReqVO createReqVO) {
        EamMaintainOrderDO order = new EamMaintainOrderDO();
        order.setOrderNo(createReqVO.orderNo());
        order.setPlanId(createReqVO.planId());
        order.setAssetId(createReqVO.assetId());
        order.setPlanDate(createReqVO.planDate());
        order.setStatus(EamMaintainOrderStatus.PENDING.getCode());
        order.setAssigneeId(createReqVO.assigneeId());
        order.setContent(createReqVO.content());
        maintainOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    public void updateMaintainOrder(EamMaintainOrderSaveReqVO updateReqVO) {
        validateOrderExists(updateReqVO.id());
        EamMaintainOrderDO updateObj = new EamMaintainOrderDO();
        updateObj.setId(updateReqVO.id());
        updateObj.setOrderNo(updateReqVO.orderNo());
        updateObj.setPlanId(updateReqVO.planId());
        updateObj.setAssetId(updateReqVO.assetId());
        updateObj.setPlanDate(updateReqVO.planDate());
        updateObj.setAssigneeId(updateReqVO.assigneeId());
        updateObj.setContent(updateReqVO.content());
        maintainOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaintainOrder(Long id) {
        validateOrderExists(id);
        maintainOrderMapper.deleteById(id);
    }

    @Override
    public EamMaintainOrderDO getMaintainOrder(Long id) {
        EamMaintainOrderDO order = maintainOrderMapper.selectById(id);
        if (order == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
        return order;
    }

    @Override
    public PageResult<EamMaintainOrderDO> getMaintainOrderPage(EamMaintainOrderPageReqVO pageReqVO) {
        return maintainOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public void start(Long id) {
        EamMaintainOrderDO order = getMaintainOrder(id);
        if (!EamMaintainOrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_STATUS_ILLEGAL,
                    order.getStatus(), EamMaintainOrderStatus.PENDING.getCode());
        }
        // 在查出的整单上修改再 updateById：version 非空，乐观锁生效，并发开始只有一个成功
        order.setStatus(EamMaintainOrderStatus.IN_PROGRESS.getCode());
        order.setStartTime(LocalDateTime.now());
        maintainOrderMapper.updateById(order);
    }

    @Override
    public void finish(Long id, String resultNote) {
        EamMaintainOrderDO order = getMaintainOrder(id);
        if (!EamMaintainOrderStatus.IN_PROGRESS.getCode().equals(order.getStatus())) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_STATUS_ILLEGAL,
                    order.getStatus(), EamMaintainOrderStatus.IN_PROGRESS.getCode());
        }
        order.setStatus(EamMaintainOrderStatus.DONE.getCode());
        order.setFinishTime(LocalDateTime.now());
        order.setResultNote(resultNote);
        maintainOrderMapper.updateById(order);
    }

    @Override
    public void cancel(Long id) {
        EamMaintainOrderDO order = getMaintainOrder(id);
        // 终态（已完成/已取消）不允许再取消
        if (EamMaintainOrderStatus.DONE.getCode().equals(order.getStatus())
                || EamMaintainOrderStatus.CANCELLED.getCode().equals(order.getStatus())) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_STATUS_ILLEGAL,
                    order.getStatus(), "待执行/执行中");
        }
        order.setStatus(EamMaintainOrderStatus.CANCELLED.getCode());
        maintainOrderMapper.updateById(order);
    }

    private void validateOrderExists(Long id) {
        if (maintainOrderMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
    }
}
