package com.mecho.application.scheduler;

import com.mecho.application.config.ConfigLoader;
import com.mecho.application.service.AlertNotificationService;
import com.mecho.application.service.AnalyzeIndicatorsService;
import com.mecho.application.service.FetchMarketDataService;
import com.mecho.domain.model.MarketData;
import com.mecho.domain.prediction.Prediction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "mecho.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketDataScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(MarketDataScheduler.class);
    
    private final FetchMarketDataService fetchMarketDataService;
    private final AnalyzeIndicatorsService analyzeIndicatorsService;
    private final AlertNotificationService alertNotificationService;
    private final ConfigLoader configLoader;
    
    public MarketDataScheduler(
            FetchMarketDataService fetchMarketDataService,
            AnalyzeIndicatorsService analyzeIndicatorsService,
            AlertNotificationService alertNotificationService,
            ConfigLoader configLoader) {
        this.fetchMarketDataService = fetchMarketDataService;
        this.analyzeIndicatorsService = analyzeIndicatorsService;
        this.alertNotificationService = alertNotificationService;
        this.configLoader = configLoader;
    }
    
    @Scheduled(cron = "${mecho.scheduler.cron:0 */15 * * * *}")
    public void runMarketAnalysisPipeline() {
        log.info("Starting market analysis pipeline");
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> symbols = configLoader.getSymbols();
            
            if (symbols == null || symbols.isEmpty()) {
                log.warn("No symbols configured for analysis");
                return;
            }
            
            for (String ticker : symbols) {
                processSymbol(ticker);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Market analysis pipeline completed in {} ms", duration);
            
        } catch (Exception e) {
            log.error("Error in market analysis pipeline: {}", e.getMessage(), e);
        }
    }
    
    public void processSymbol(String ticker) {
        try {
            log.debug("Processing symbol: {}", ticker);
            
            fetchMarketDataService.fetchDataForSymbol(ticker);
            
            Prediction prediction = analyzeIndicatorsService.analyzeSymbol(ticker);
            
            if (alertNotificationService.shouldSendAlert(prediction)) {
                List<MarketData> recentData = fetchMarketDataService.getRecentMarketData(ticker, 1);
                
                if (!recentData.isEmpty()) {
                    MarketData latest = recentData.get(0);
                    alertNotificationService.createAndSendAlert(
                            prediction,
                            latest.closePrice(),
                            latest.volume().longValue()
                    );
                } else {
                    alertNotificationService.createAndSendAlert(prediction, null, null);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing symbol {}: {}", ticker, e.getMessage());
        }
    }
    
    public void runFullAnalysis() {
        log.info("Starting full analysis for all configured symbols");
        
        List<String> symbols = configLoader.getSymbols();
        if (symbols == null || symbols.isEmpty()) {
            log.warn("No symbols configured");
            return;
        }
        
        symbols.forEach(this::processSymbol);
        
        log.info("Full analysis completed for {} symbols", symbols.size());
    }
    
    public void runAnalysisForSymbol(String ticker) {
        log.info("Running analysis for symbol: {}", ticker);
        processSymbol(ticker);
    }
}
