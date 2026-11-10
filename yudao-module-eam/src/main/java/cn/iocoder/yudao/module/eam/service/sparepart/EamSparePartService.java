package cn.iocoder.yudao.module.eam.service.sparepart;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.sparepart.vo.EamSparePartSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamSparePartDO;

/**
 * EAM 备件 Service 接口。
 */
public interface EamSparePartService {

    /** 创建备件 */
    Long createSparePart(EamSparePartSaveReqVO createReqVO);

    /** 更新备件 */
    void updateSparePart(EamSparePartSaveReqVO updateReqVO);

    /** 删除备件 */
    void deleteSparePart(Long id);

    /** 根据 ID 查询 */
    EamSparePartDO getSparePart(Long id);

    /** 分页查询 */
    PageResult<EamSparePartDO> getSparePartPage(EamSparePartPageReqVO pageReqVO);

    /** 入库：增加库存 + 写流水，返回更新后的备件（含最新 stock/safetyStock） */
    EamSparePartDO inbound(Long partId, Integer quantity, String bizOrderNo, String operator);

    /** 出库：校验库存充足后扣减 + 写流水，返回更新后的备件（含最新 stock/safetyStock） */
    EamSparePartDO outbound(Long partId, Integer quantity, String bizOrderNo, String operator);
}
