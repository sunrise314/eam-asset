package cn.iocoder.yudao.module.eam.service.repairorder;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamRepairOrderDO;
import cn.iocoder.yudao.module.eam.dal.mysql.EamRepairOrderMapper;
import cn.iocoder.yudao.module.eam.enums.EamRepairPriority;
import cn.iocoder.yudao.module.eam.enums.EamRepairStatus;
import cn.iocoder.yudao.module.eam.framework.eam.metrics.MetricsResult;
import cn.iocoder.yudao.module.eam.framework.eam.metrics.MtbfCalculator;
import cn.iocoder.yudao.module.eam.framework.eam.repair.RepairOrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EAM 维修工单 Service 实现。
 *
 * 所有流转方法（dispatch/start/finish/accept/cancel）第一行都过
 * RepairOrderStateMachine.assertTransit——状态迁移合法性只维护在状态机一处，
 * Service 不自己判断"当前状态允不允许"，避免两处规则漂移。
 */
@Service
@RequiredArgsConstructor
public class EamRepairOrderServiceImpl implements EamRepairOrderService {

    private static final ErrorCode ERROR_CODE_NOT_EXISTS = new ErrorCode(1_003_400, "维修工单不存在: id={}");

    private final EamRepairOrderMapper repairOrderMapper;

    @Override
    public Long createRepairOrder(EamRepairOrderSaveReqVO createReqVO) {
        EamRepairOrderDO order = new EamRepairOrderDO();
        order.setOrderNo(createReqVO.orderNo());
        order.setAssetId(createReqVO.assetId());
        order.setFaultDesc(createReqVO.faultDesc());
        order.setPriority(createReqVO.priority() != null ? createReqVO.priority() : EamRepairPriority.MEDIUM.getCode());
        order.setStatus(EamRepairStatus.PENDING_DISPATCH.getCode());
        order.setReporter(createReqVO.reporter());
        repairOrderMapper.insert(order);
        return order.getId();
    }

    @Override
    public void updateRepairOrder(EamRepairOrderSaveReqVO updateReqVO) {
        validateOrderExists(updateReqVO.id());
        EamRepairOrderDO updateObj = new EamRepairOrderDO();
        updateObj.setId(updateReqVO.id());
        updateObj.setOrderNo(updateReqVO.orderNo());
        updateObj.setAssetId(updateReqVO.assetId());
        updateObj.setFaultDesc(updateReqVO.faultDesc());
        updateObj.setPriority(updateReqVO.priority());
        updateObj.setReporter(updateReqVO.reporter());
        repairOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteRepairOrder(Long id) {
        validateOrderExists(id);
        repairOrderMapper.deleteById(id);
    }

    @Override
    public EamRepairOrderDO getRepairOrder(Long id) {
        EamRepairOrderDO order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
        return order;
    }

    @Override
    public PageResult<EamRepairOrderDO> getRepairOrderPage(EamRepairOrderPageReqVO pageReqVO) {
        return repairOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public void dispatch(Long id, Long assigneeId) {
        EamRepairOrderDO order = getRepairOrder(id);
        RepairOrderStateMachine.assertTransit(order.getStatus(), EamRepairStatus.DISPATCHED.getCode());
        // 在查出的整单上修改再 updateById：version 非空，乐观锁生效，并发派工只有一个成功
        order.setStatus(EamRepairStatus.DISPATCHED.getCode());
        order.setAssigneeId(assigneeId);
        order.setDispatchTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
    }

    @Override
    public void start(Long id) {
        EamRepairOrderDO order = getRepairOrder(id);
        RepairOrderStateMachine.assertTransit(order.getStatus(), EamRepairStatus.REPAIRING.getCode());
        order.setStatus(EamRepairStatus.REPAIRING.getCode());
        order.setStartTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
    }

    @Override
    public void finish(Long id, String repairNote) {
        EamRepairOrderDO order = getRepairOrder(id);
        RepairOrderStateMachine.assertTransit(order.getStatus(), EamRepairStatus.PENDING_ACCEPT.getCode());
        order.setStatus(EamRepairStatus.PENDING_ACCEPT.getCode());
        order.setFinishTime(LocalDateTime.now());
        order.setRepairNote(repairNote);
        repairOrderMapper.updateById(order);
    }

    /**
     * 验收通过。
     *
     * 联动取舍：验收后设备理应从 REPAIRING 回 RUNNING，但此处故意不注入 EamAssetService——
     * repair→asset 调用会变成双向依赖（asset 报修时又想置 REPAIRING），演示仓库保持单向；
     * 商用建议完工/验收后发领域事件，由 asset 侧订阅改状态，两个 Service 彻底解耦。
     */
    @Override
    public void accept(Long id, String acceptNote) {
        EamRepairOrderDO order = getRepairOrder(id);
        RepairOrderStateMachine.assertTransit(order.getStatus(), EamRepairStatus.DONE.getCode());
        order.setStatus(EamRepairStatus.DONE.getCode());
        order.setAcceptNote(acceptNote);
        repairOrderMapper.updateById(order);
    }

    @Override
    public void cancel(Long id) {
        EamRepairOrderDO order = getRepairOrder(id);
        // 能否取消由状态机决定：1/2/3 → 6 合法，待验收与终态抛 1_003_401
        RepairOrderStateMachine.assertTransit(order.getStatus(), EamRepairStatus.CANCELLED.getCode());
        order.setStatus(EamRepairStatus.CANCELLED.getCode());
        repairOrderMapper.updateById(order);
    }

    @Override
    public MetricsResult calcMetrics(Long assetId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        // 只取窗口内已完成的工单：进行中的工单维修时长未定，纳入会虚增维修时长
        List<EamRepairOrderDO> doneOrders = repairOrderMapper.selectDoneList(assetId, windowStart, windowEnd);
        return MtbfCalculator.calc(assetId, doneOrders, windowStart, windowEnd);
    }

    private void validateOrderExists(Long id) {
        if (repairOrderMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
    }
}
