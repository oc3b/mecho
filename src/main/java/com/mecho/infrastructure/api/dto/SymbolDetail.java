package com.mecho.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolDetail {
    
    private String ticker;
    
    private String name;
    
    private String assetClass;
    
    private BigDecimal latestPrice;
    
    private BigDecimal latestVolume;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime latestTimestamp;
    
    private List<MarketDataDTO> marketData;
}
