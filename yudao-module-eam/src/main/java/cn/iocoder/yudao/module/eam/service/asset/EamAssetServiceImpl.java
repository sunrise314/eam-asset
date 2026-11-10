package cn.iocoder.yudao.module.eam.service.asset;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetPageReqVO;
import cn.iocoder.yudao.module.eam.controller.admin.asset.vo.EamAssetSaveReqVO;
import cn.iocoder.yudao.module.eam.dal.dataobject.EamAssetDO;
import cn.iocoder.yudao.module.eam.dal.mysql.EamAssetMapper;
import cn.iocoder.yudao.module.eam.enums.EamAssetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * EAM 设备台账 Service 实现。
 */
@Service
@RequiredArgsConstructor
public class EamAssetServiceImpl implements EamAssetService {

    private static final ErrorCode ERROR_CODE_NOT_EXISTS = new ErrorCode(1_003_001, "设备不存在: id={}");
    private static final ErrorCode ERROR_CODE_ASSET_NO_NOT_EXISTS = new ErrorCode(1_003_002, "设备编号不存在: {}");

    private final EamAssetMapper assetMapper;

    @Override
    public Long createAsset(EamAssetSaveReqVO createReqVO) {
        EamAssetDO asset = new EamAssetDO();
        asset.setAssetNo(createReqVO.assetNo());
        asset.setName(createReqVO.name());
        asset.setModel(createReqVO.model());
        asset.setCategory(createReqVO.category());
        asset.setLocation(createReqVO.location());
        asset.setStatus(createReqVO.status() != null ? createReqVO.status() : EamAssetStatus.RUNNING.getCode());
        asset.setPurchaseDate(createReqVO.purchaseDate());
        asset.setWarrantyUntil(createReqVO.warrantyUntil());
        asset.setResponsibleUserId(createReqVO.responsibleUserId());
        asset.setAttributes(createReqVO.attributes());
        asset.setRemark(createReqVO.remark());
        assetMapper.insert(asset);
        return asset.getId();
    }

    @Override
    public void updateAsset(EamAssetSaveReqVO updateReqVO) {
        validateAssetExists(updateReqVO.id());
        EamAssetDO updateObj = new EamAssetDO();
        updateObj.setId(updateReqVO.id());
        updateObj.setAssetNo(updateReqVO.assetNo());
        updateObj.setName(updateReqVO.name());
        updateObj.setModel(updateReqVO.model());
        updateObj.setCategory(updateReqVO.category());
        updateObj.setLocation(updateReqVO.location());
        updateObj.setStatus(updateReqVO.status());
        updateObj.setPurchaseDate(updateReqVO.purchaseDate());
        updateObj.setWarrantyUntil(updateReqVO.warrantyUntil());
        updateObj.setResponsibleUserId(updateReqVO.responsibleUserId());
        updateObj.setAttributes(updateReqVO.attributes());
        updateObj.setRemark(updateReqVO.remark());
        assetMapper.updateById(updateObj);
    }

    @Override
    public void deleteAsset(Long id) {
        validateAssetExists(id);
        assetMapper.deleteById(id);
    }

    @Override
    public EamAssetDO getAsset(Long id) {
        EamAssetDO asset = assetMapper.selectById(id);
        if (asset == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
        return asset;
    }

    @Override
    public PageResult<EamAssetDO> getAssetPage(EamAssetPageReqVO pageReqVO) {
        return assetMapper.selectPage(pageReqVO);
    }

    @Override
    public EamAssetDO getByAssetNo(String assetNo) {
        EamAssetDO asset = assetMapper.selectByAssetNo(assetNo);
        if (asset == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_ASSET_NO_NOT_EXISTS, assetNo);
        }
        return asset;
    }

    private void validateAssetExists(Long id) {
        if (assetMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ERROR_CODE_NOT_EXISTS, id);
        }
    }
}
