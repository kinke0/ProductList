package com.superpower.modules.data.dto;

import lombok.Data;
import java.util.List;

@Data
public class TreeNodeDTO {
    private Long id;
    private Long parentId;
    private Integer level;
    private String label;
    private Integer sortOrder;
    private Boolean isLeaf;
    private List<TreeNodeDTO> children;
}
