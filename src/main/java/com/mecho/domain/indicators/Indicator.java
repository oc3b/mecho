package com.mecho.domain.indicators;

import com.mecho.domain.model.OHLCV;
import java.util.List;

public interface Indicator {
    String getCode();
    double calculate(List<OHLCV> prices);
    Signal calculateSignal(double value);
}
