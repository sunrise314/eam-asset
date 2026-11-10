package cn.iocoder.yudao.module.eam.service.maintainplan;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.maintainplan.vo.EamMaintainPlanSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamMaintainPlanDO;

/**
 * EAM 保养计划 Service 接口。
 */
public interface EamMaintainPlanService {

    /** 创建保养计划 */
    Long createMaintainPlan(EamMaintainPlanSaveReqVO createReqVO);

    /** 更新保养计划 */
    void updateMaintainPlan(EamMaintainPlanSaveReqVO updateReqVO);

    /** 删除保养计划 */
    void deleteMaintainPlan(Long id);

    /** 根据 ID 查询 */
    EamMaintainPlanDO getMaintainPlan(Long id);

    /** 分页查询 */
    PageResult<EamMaintainPlanDO> getMaintainPlanPage(EamMaintainPlanPageReqVO pageReqVO);

    /** 扫描启用计划，生成到期保养工单，返回生成数量 */
    int generateOrders();
}
