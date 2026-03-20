package com.mecho.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Symbol {
    private Long id;
    private String ticker;
    private String name;
    private AssetClass assetClass;
    private String exchange;
    private Boolean enabled;
}
