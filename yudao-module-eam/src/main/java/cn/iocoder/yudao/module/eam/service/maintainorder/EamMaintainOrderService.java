package cn.iocoder.yudao.module.eam.service.maintainorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainorder.vo.EamMaintainOrderSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainOrderDO;

/**
 * EAM 保养工单 Service 接口。
 */
public interface EamMaintainOrderService {

    /** 创建保养工单（手工补录；自动生成的走 MaintainPlanService.generateOrders） */
    Long createMaintainOrder(EamMaintainOrderSaveReqVO createReqVO);

    /** 更新保养工单 */
    void updateMaintainOrder(EamMaintainOrderSaveReqVO updateReqVO);

    /** 删除保养工单 */
    void deleteMaintainOrder(Long id);

    /** 根据 ID 查询 */
    EamMaintainOrderDO getMaintainOrder(Long id);

    /** 分页查询 */
    PageResult<EamMaintainOrderDO> getMaintainOrderPage(EamMaintainOrderPageReqVO pageReqVO);

    /** 开始执行（待执行 → 执行中） */
    void start(Long id);

    /** 完成执行（执行中 → 已完成，回填执行结果） */
    void finish(Long id, String resultNote);

    /** 取消（待执行/执行中 → 已取消） */
    void cancel(Long id);
}
