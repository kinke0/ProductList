package com.superpower.modules.data.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DataEntryDTO {
    private Long id;
    private Long versionId;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;
    private Boolean isLeaf;

    private String colProductSystem;
    private String colAppRole;
    private String colBidParamDesc;
    private String colFeatureDesc;
    private String colStatus;
    private String colBizCategory;
    private String colBizDomain;
    private String colVersionDivision;
    private String colYuan;
    private String colDeliveryWorkload;
    private String colControlPoint;
    private String colControlPointImg1;
    private String colControlPointImg2;
    private String colControlPointImg3;
    private String colControlPointDoc;
    private String colCopyright;
    private String colRemark;
    private String colSmartMedical;
    private String colSmartService;
    private String colSmartManagement;
    private String colInterconnection;
    private String colProductSysId;
    private String colModuleId;
    private String colOtherSolutionTag;
    private String colDocMaintainer;
    private String colProductManager;
    private String colParentRecord;
    private String colInternalVersion;
    private String colIntelligent;
    private String colYao;
    private String colChi;
    private BigDecimal colFY23;
    private BigDecimal colFY24;
    private BigDecimal colFY25;
    private BigDecimal colFY26;
    private BigDecimal colFY27;
    private BigDecimal colFY28;
    private BigDecimal colFY29;
    private BigDecimal colRDCostTotal;
    private Integer colSalesYao;
    private Integer colSalesYuan;
    private Integer colSalesChi;
    private BigDecimal colFactoryPrice;
    private String colPrincipal;
    private String colProductLine;
    private String colAssetType;
}
