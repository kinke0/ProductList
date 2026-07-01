package com.superpower.modules.data.dto;

import lombok.Data;
import java.util.List;

@Data
public class RenumberRequest {
    private List<RenumberItem> items;
}
