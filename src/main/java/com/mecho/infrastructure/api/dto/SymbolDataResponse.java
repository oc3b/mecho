package com.mecho.infrastructure.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolDataResponse {
    
    private String ticker;
    
    private List<MarketDataDTO> data;
}
