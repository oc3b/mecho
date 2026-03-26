package com.mecho.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorInfo {
    
    private String ticker;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private BigDecimal rsi;
    
    private BigDecimal macd;
    
    private BigDecimal upperBand;
    
    private BigDecimal lowerBand;
    
    private BigDecimal ma50;
    
    private BigDecimal ma200;
    
    private BigDecimal stoch;
}
