package com.superpower.modules.data.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "data_entry")
public class DataEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_leaf")
    private Boolean isLeaf = true;

    @Column(name = "col_产品系统", length = 500)
    private String colProductSystem;

    @Column(name = "col_应用角色", length = 500)
    private String colAppRole;

    @Column(name = "col_招标参数说明", columnDefinition = "TEXT")
    private String colBidParamDesc;

    @Column(name = "col_功能说明", columnDefinition = "TEXT")
    private String colFeatureDesc;

    @Column(name = "col_状态", length = 100)
    private String colStatus;

    @Column(name = "col_业务分类", length = 200)
    private String colBizCategory;

    @Column(name = "col_业务域", length = 200)
    private String colBizDomain;

    @Column(name = "col_版本划分", length = 200)
    private String colVersionDivision;

    @Column(name = "col_远", length = 50)
    private String colYuan;

    @Column(name = "col_交付工作量人月", length = 100)
    private String colDeliveryWorkload;

    @Column(name = "col_控标点", length = 50)
    private String colControlPoint;

    @Column(name = "col_控标点截图1", columnDefinition = "TEXT")
    private String colControlPointImg1;

    @Column(name = "col_控标点截图2", columnDefinition = "TEXT")
    private String colControlPointImg2;

    @Column(name = "col_控标点截图3", columnDefinition = "TEXT")
    private String colControlPointImg3;

    @Column(name = "col_控标点文档说明", columnDefinition = "TEXT")
    private String colControlPointDoc;

    @Column(name = "col_软著", length = 500)
    private String colCopyright;

    @Column(name = "col_备注", columnDefinition = "TEXT")
    private String colRemark;

    @Column(name = "col_智慧医疗", length = 100)
    private String colSmartMedical;

    @Column(name = "col_智慧服务", length = 100)
    private String colSmartService;

    @Column(name = "col_智慧管理", length = 100)
    private String colSmartManagement;

    @Column(name = "col_互联互通", length = 100)
    private String colInterconnection;

    @Column(name = "col_产品系统标识", length = 100)
    private String colProductSysId;

    @Column(name = "col_模块标识", length = 100)
    private String colModuleId;

    @Column(name = "col_其他解决方案标记", length = 200)
    private String colOtherSolutionTag;

    @Column(name = "col_文档维护人员", length = 100)
    private String colDocMaintainer;

    @Column(name = "col_产品经理", length = 100)
    private String colProductManager;

    @Column(name = "col_父记录", length = 500)
    private String colParentRecord;

    @Column(name = "col_内部版本", length = 100)
    private String colInternalVersion;

    @Column(name = "col_智能化", length = 50)
    private String colIntelligent;

    @Column(name = "col_曜", length = 50)
    private String colYao;

    @Column(name = "col_驰", length = 50)
    private String colChi;

    @Column(name = "col_FY23")
    private BigDecimal colFY23;

    @Column(name = "col_FY24")
    private BigDecimal colFY24;

    @Column(name = "col_FY25")
    private BigDecimal colFY25;

    @Column(name = "col_FY26")
    private BigDecimal colFY26;

    @Column(name = "col_FY27")
    private BigDecimal colFY27;

    @Column(name = "col_FY28")
    private BigDecimal colFY28;

    @Column(name = "col_FY29")
    private BigDecimal colFY29;

    @Column(name = "col_研发成本合计")
    private BigDecimal colRDCostTotal;

    @Column(name = "col_销量曜")
    private Integer colSalesYao;

    @Column(name = "col_销量远")
    private Integer colSalesYuan;

    @Column(name = "col_销量驰")
    private Integer colSalesChi;

    @Column(name = "col_出厂套价保本")
    private BigDecimal colFactoryPrice;

    @Column(name = "col_负责人", length = 200)
    private String colPrincipal;

    @Column(name = "col_产品线", length = 200)
    private String colProductLine;

    @Column(name = "col_资产类型", length = 100)
    private String colAssetType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;

    public DataEntry cloneWithoutId() {
        DataEntry copy = new DataEntry();
        copy.parentId = this.parentId;
        copy.level = this.level;
        copy.sortOrder = this.sortOrder;
        copy.isLeaf = this.isLeaf;
        copy.colProductSystem = this.colProductSystem;
        copy.colAppRole = this.colAppRole;
        copy.colBidParamDesc = this.colBidParamDesc;
        copy.colFeatureDesc = this.colFeatureDesc;
        copy.colStatus = this.colStatus;
        copy.colBizCategory = this.colBizCategory;
        copy.colBizDomain = this.colBizDomain;
        copy.colVersionDivision = this.colVersionDivision;
        copy.colYuan = this.colYuan;
        copy.colDeliveryWorkload = this.colDeliveryWorkload;
        copy.colControlPoint = this.colControlPoint;
        copy.colControlPointImg1 = this.colControlPointImg1;
        copy.colControlPointImg2 = this.colControlPointImg2;
        copy.colControlPointImg3 = this.colControlPointImg3;
        copy.colControlPointDoc = this.colControlPointDoc;
        copy.colCopyright = this.colCopyright;
        copy.colRemark = this.colRemark;
        copy.colSmartMedical = this.colSmartMedical;
        copy.colSmartService = this.colSmartService;
        copy.colSmartManagement = this.colSmartManagement;
        copy.colInterconnection = this.colInterconnection;
        copy.colProductSysId = this.colProductSysId;
        copy.colModuleId = this.colModuleId;
        copy.colOtherSolutionTag = this.colOtherSolutionTag;
        copy.colDocMaintainer = this.colDocMaintainer;
        copy.colProductManager = this.colProductManager;
        copy.colParentRecord = this.colParentRecord;
        copy.colInternalVersion = this.colInternalVersion;
        copy.colIntelligent = this.colIntelligent;
        copy.colYao = this.colYao;
        copy.colChi = this.colChi;
        copy.colFY23 = this.colFY23;
        copy.colFY24 = this.colFY24;
        copy.colFY25 = this.colFY25;
        copy.colFY26 = this.colFY26;
        copy.colFY27 = this.colFY27;
        copy.colFY28 = this.colFY28;
        copy.colFY29 = this.colFY29;
        copy.colRDCostTotal = this.colRDCostTotal;
        copy.colSalesYao = this.colSalesYao;
        copy.colSalesYuan = this.colSalesYuan;
        copy.colSalesChi = this.colSalesChi;
        copy.colFactoryPrice = this.colFactoryPrice;
        copy.colPrincipal = this.colPrincipal;
        copy.colProductLine = this.colProductLine;
        copy.colAssetType = this.colAssetType;
        return copy;
    }
}
