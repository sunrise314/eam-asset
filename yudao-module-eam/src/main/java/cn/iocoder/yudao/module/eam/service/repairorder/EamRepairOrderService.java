package cn.iocoder.yudao.module.eam.service.repairorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.repairorder.vo.EamRepairOrderSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamRepairOrderDO;
import cn.iocoder.yudao.module.eam.framework.eam.metrics.MetricsResult;

import java.time.LocalDateTime;

/**
 * EAM 维修工单 Service 接口。
 */
public interface EamRepairOrderService {

    /** 创建维修工单（报修） */
    Long createRepairOrder(EamRepairOrderSaveReqVO createReqVO);

    /** 更新维修工单 */
    void updateRepairOrder(EamRepairOrderSaveReqVO updateReqVO);

    /** 删除维修工单 */
    void deleteRepairOrder(Long id);

    /** 根据 ID 查询 */
    EamRepairOrderDO getRepairOrder(Long id);

    /** 分页查询 */
    PageResult<EamRepairOrderDO> getRepairOrderPage(EamRepairOrderPageReqVO pageReqVO);

    /** 派工（待派工 → 已派工，指定维修人） */
    void dispatch(Long id, Long assigneeId);

    /** 开始维修（已派工 → 维修中） */
    void start(Long id);

    /** 完工（维修中 → 待验收，回填维修记录） */
    void finish(Long id, String repairNote);

    /** 验收通过（待验收 → 已完成，回填验收记录） */
    void accept(Long id, String acceptNote);

    /** 取消（待派工/已派工/维修中 → 已取消） */
    void cancel(Long id);

    /** 计算设备在统计窗口内的可靠性指标（MTBF/MTTR，单位小时） */
    MetricsResult calcMetrics(Long assetId, LocalDateTime windowStart, LocalDateTime windowEnd);
}
