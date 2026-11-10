package cn.iocoder.yudao.module.eam.service.asset;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamAssetDO;

/**
 * EAM 设备台账 Service 接口。
 */
public interface EamAssetService {

    /** 创建设备台账 */
    Long createAsset(EamAssetSaveReqVO createReqVO);

    /** 更新设备台账 */
    void updateAsset(EamAssetSaveReqVO updateReqVO);

    /** 删除设备台账 */
    void deleteAsset(Long id);

    /** 根据 ID 查询 */
    EamAssetDO getAsset(Long id);

    /** 分页查询 */
    PageResult<EamAssetDO> getAssetPage(EamAssetPageReqVO pageReqVO);

    /** 根据设备编号查询 */
    EamAssetDO getByAssetNo(String assetNo);
}
