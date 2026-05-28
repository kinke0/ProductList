package com.superpower.modules.image.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImageDirectoryNode {
    private String label;
    private List<ImageDirectoryNode> children;
    private int count;
}