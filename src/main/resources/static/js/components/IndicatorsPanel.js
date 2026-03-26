// IndicatorsPanel Component - Step 5
const IndicatorsPanel = {
  props: ['symbol', 'visible', 'onClose'],
  template: `
    <div v-if="visible" 
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="$emit('close')">
      <div class="bg-gray-800 rounded-lg shadow-xl min-w-[700px] max-w-6xl max-h-[90vh] flex flex-col overflow-hidden">
        <div class="p-4 border-b border-gray-700 flex items-center justify-between flex-shrink-0">
          <h2 class="text-xl font-bold text-blue-400">{{ symbol?.ticker }} <span class="text-gray-400 text-base">- Technical Indicators</span></h2>
          <button @click="$emit('close')" class="text-gray-400 hover:text-white text-2xl">&times;</button>
        </div>

        <div class="flex-1 overflow-y-auto p-6">
          <!-- Loading State -->
          <div v-if="loading" class="flex items-center justify-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-400"></div>
            <span class="ml-4 text-gray-400">Loading indicators...</span>
          </div>

          <!-- Error State -->
          <div v-else-if="error" class="text-center py-12">
            <p class="text-red-400 mb-4">{{ error }}</p>
            <button @click="fetchIndicators" class="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded transition-colors">Retry</button>
          </div>

          <!-- Indicators Grid -->
          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <!-- RSI Gauge -->
            <div class="bg-gray-750 rounded-lg p-6">
              <h3 class="text-lg font-semibold text-gray-200 mb-4">RSI (14)</h3>
              <div ref="rsiContainer" class="flex items-center justify-center h-48"></div>
              <p class="text-center mt-2" :class="rsiSignal.class">{{ rsiSignal.text }}</p>
            </div>

            <!-- MACD Chart -->
            <div class="bg-gray-750 rounded-lg p-6">
              <h3 class="text-lg font-semibold text-gray-200 mb-2">MACD</h3>
              <div ref="macdContainer" class="h-48"></div>
              <p class="text-center mt-2" :class="macdSignal.class">{{ macdSignal.text }}</p>
            </div>

            <!-- Bollinger Bands -->
            <div class="bg-gray-750 rounded-lg p-6">
              <h3 class="text-lg font-semibold text-gray-200 mb-4">Bollinger Bands (20, 2)</h3>
              <div class="space-y-3">
                <div class="flex justify-between items-center">
                  <span class="text-gray-400">Upper Band:</span>
                  <span class="text-gray-200 font-semibold">{{ formatPrice(bollinger.upper) }}</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-gray-400">Middle (MA20):</span>
                  <span class="text-gray-200 font-semibold">{{ formatPrice(bollinger.middle) }}</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-gray-400">Lower Band:</span>
                  <span class="text-gray-200 font-semibold">{{ formatPrice(bollinger.lower) }}</span>
                </div>
                <div class="border-t border-gray-600 pt-2 mt-2">
                  <div class="flex justify-between items-center">
                    <span class="text-gray-400">Current Price:</span>
                    <span class="text-green-400 font-bold">{{ formatPrice(bollinger.current) }}</span>
                  </div>
                </div>
              </div>
              <p class="text-center mt-2" :class="bollingerSignal.class">{{ bollingerSignal.text }}</p>
            </div>

            <!-- MA Crossover -->
            <div class="bg-gray-750 rounded-lg p-6">
              <h3 class="text-lg font-semibold text-gray-200 mb-4">Moving Averages</h3>
              <div class="space-y-3">
                <div class="flex justify-between items-center">
                  <span class="text-gray-400">MA50:</span>
                  <span class="text-blue-400 font-semibold">{{ formatPrice(ma.ma50) }}</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-gray-400">MA200:</span>
                  <span class="text-yellow-400 font-semibold">{{ formatPrice(ma.ma200) }}</span>
                </div>
              </div>
              <p class="text-center mt-4" :class="maSignal.class">{{ maSignal.text }}</p>
              <div class="mt-4 p-3 bg-gray-700 rounded text-sm">
                MA50/MA200 crossover: {{ maSignal.crossover ? 'Detected' : 'No crossover' }}
              </div>
            </div>

            <!-- Stochastic %K -->
            <div class="bg-gray-750 rounded-lg p-6 md:col-span-2">
              <h3 class="text-lg font-semibold text-gray-200 mb-4">Stochastic Oscillator (%K)</h3>
              <div ref="stochContainer" class="h-32"></div>
              <div class="flex justify-between mt-2 text-sm">
                <span>Overbought (>80)</span>
                <span>Oversold (<20)</span>
              </div>
              <p class="text-center mt-2" :class="stochSignal.class">{{ stochSignal.text }} ({{ stochastic.k.toFixed(1) }})</p>
            </div>

            <!-- Overall Signal Summary -->
            <div class="bg-gray-750 rounded-lg p-6 md:col-span-2">
              <h3 class="text-lg font-semibold text-gray-200 mb-4">Overall Signal Summary</h3>
              <div class="flex items-center justify-center gap-8">
                <div class="text-center">
                  <dive class="text-3xl">{{ indicatorsSummary.buy }}</div>
                  <span class="text-gray-400">Buy Signals</span>
                </div>
                <div class="text-center">
                  <dive class="text-3xl">{{ indicatorsSummary.neutral }}</div>
                  <span class="text-gray-400">Neutral</span>
                </div>
                <div class="text-center">
                  <dive class="text-3xl">{{ indicatorsSummary.sell }}</div>
                  <span class="text-gray-400">Sell Signals</span>
                </div>
              </div>
              <p class="text-center mt-4 text-xl font-bold" :class="overallSignal.class">{{ overallSignal.text }}</p>
            </div>
          </div>
        </div>

        <div class="p-4 border-t border-gray-700 flex justify-end flex-shrink-0">
          <button @click="$emit('close')" class="px-4 py-2 bg-gray-600 hover:bg-gray-500 rounded-lg transition-colors">Close</button>
        </div>
      </div>
    </div>
  `,
  data() {
    return {
      loading: false,
      error: null,
      indicators: {},
      rsiChart: null,
      macdChart: null,
      stochChart: null
    };
  },
  computed: {
    rsi() {
      return this.indicators.RSI?.value || 50;
    },
    rsiSignal() {
      const rsi = this.rsi;
      if (rsi < 30) return { text: 'OVERSOLD - BUY SIGNAL', class: 'text-green-400', signal: 'buy' };
      if (rsi > 70) return { text: 'OVERBOUGHT - SELL SIGNAL', class: 'text-red-400', signal: 'sell' };
      return { text: 'NEUTRAL', class: 'text-gray-400', signal: 'neutral' };
    },
    macd() {
      return this.indicators.MACD || { macd: 0, signal: 0 };        return { macd: this.indicators.MACD?.macd || 0, signal: this.indicators.MACD?.signal || 0 };
    },
    macdSignal() {
      const diff = this.macd.macd - this.macd.signal;
      if (diff > 0) return { text: 'MACD ABOVE SIGNAL - BUY', class: 'text-green-400', signal: 'buy' };
      if (diff < 0) return { text: 'MACD BELOW SIGNAL - SELL', class: 'text-red-400', signal: 'sell' };
      return { text: 'MACD NEUTRAL', class: 'text-gray-400', signal: 'neutral' };
    },
    bollinger() {
      const bb = this.indicators.BollingerBands || { upper: 0, middle: 0, lower: 0, current: 0 };
      return bb;
    },
    bollingerSignal() {
      const bb = this.bollinger;
      if (bb.current > bb.upper) return { text: 'Price above upper band - OVERBOUGHT', class: 'text-red-400', signal: 'sell' };
      if (bb.current < bb.lower) return { text: 'Price below lower band - OVERSOLD', class: 'text-green-400', signal: 'buy' };
      return { text: 'Price within bands - NEUTRAL', class: 'text-gray-400', signal: 'neutral' };
    },
    ma() {
      return this.indicators.MA || { ma50: 0, ma200: 0 };
    },
    maSignal() {
      const ma50 = this.ma.ma50;
      const ma200 = this.ma.ma200;
      const crossover = ma50 && ma200 ? (ma50 > ma200) : null;
      if (crossover === true) return { text: 'MA50 ABOVE MA200 - BULLISH GOLDEN CROSS', class: 'text-green-400', signal: 'buy', crossover: true };
      if (crossover === false) return { text: 'MA50 BELOW MA200 - BEARISH DEATH CROSS', class: 'text-red-400', signal: 'sell', crossover: true };
      return { text: 'MA50/MA200 - AWAITING DATA', class: 'text-gray-400', signal: 'neutral', crossover: false };
    },
    stochastic() {
      return this.indicators.Stochastic || { k: 50, d: 50 };
    },
    stochSignal() {
      const k = this.stochastic.k;
      if (k < 20) return { text: 'OVERSOLD - BUY SIGNAL', class: 'text-green-400', signal: 'buy' };
      if (k > 80) return { text: 'OVERBOUGHT - SELL SIGNAL', class: 'text-red-400', signal: 'sell' };
      return { text: 'NEUTRAL', class: 'text-gray-400', signal: 'neutral' };
    },
    indicatorsSummary() {
      const signals = [this.rsiSignal, this.macdSignal, this.bollingerSignal, this.maSignal, this.stochSignal];
      return {
        buy: signals.filter(s => s.signal === 'buy').length,
        neutral: signals.filter(s => s.signal === 'neutral').length,
        sell: signals.filter(s => s.signal === 'sell').length
      };
    },
    overallSignal() {
      const { buy, sell, neutral } = this.indicatorsSummary;
      if (buy > sell + neutral) return { text: 'STRONG BUY', class: 'text-green-400' };
      if (sell > buy + neutral) return { text: 'STRONG SELL', class: 'text-red-400' };
      if (buy > sell) return { text: 'BUY', class: 'text-green-400' };
      if (sell > buy) return { text: 'SELL', class: 'text-red-400' };
      return { text: 'NEUTRAL', class: 'text-gray-400' };
    }
  },
  watch: {
    visible(newVal) {
      if (newVal && this.symbol) {
        this.fetchIndicators();
      }
    }
  },
  beforeUnmount() {
    this.destroyCharts();
  },
  methods: {
    async fetchIndicators() {
      this.loading = true;
      this.error = null;
      try {
        const response = await fetch(`/api/symbols/${this.symbol.ticker}/indicators`);
        if (!response.ok) throw new Error('Failed to fetch indicators');
        const data = await response.json();
        this.indicators = data;
        this.$nextTick(() => {
          this.renderCharts();
        });
      } catch (err) {
        this.error = err.message;
      } finally {
        this.loading = false;
      }
    },
    renderCharts() {
      this.destroyCharts();
      this.renderRSIGauge();
      this.renderMACDChart();
      this.renderStochChart();
    },
    destroyCharts() {
      [this.rsiChart, this.macdChart, this.stochChart].forEach(chart => {
        if (chart) chart.destroy();
      });
    },
    renderRSIGauge() {
      if (!this.$refs.rsiContainer) return;
      const rsi = this.rsi;
      let color = '#9ca3af';
      if (rsi < 30) color = '#22c55e';
      if (rsi > 70) color = '#ef4444';

      const options = {
        series: [rsi],
        chart: {
          type: 'radialBar',
          height: 180,
          sparkline: { enabled: true }
        },
        plotOptions: {
          radialBar: {
            hollow: { size: '60%' },
            track: {
              background: '#374151'
            },
            dataLabels: {
              show: true,
              name: { show: false },
              value: {
                show: true,
                fontSize: '24px',
                fontWeight: 'bold',
                color: color
              }
            }
          }
        },
        colors: [color],
        labels: ['RSI'],
        stroke: { dashArray: 5 }
      };

      this.rsiChart = new ApexCharts(this.$refs.rsiContainer, options);
      this.rsiChart.render();
    },
    renderMACDChart() {
      if (!this.$refs.macdContainer) return;
      const macd = this.macd;
      const diff = macd.macd - macd.signal;
      const isPositive = diff >= 0;

      const options = {
        series: [{
          name: 'MACD',
          data: [macd.macd]
        }, {
          name: 'Signal',
          data: [macd.signal]
        }],
        chart: {
          type: 'bar',
          height: '100%',
          toolbar: { show: false },
          sparkline: { enabled: true }
        },
        plotOptions: {
          bar: {
            borderRadius: 4,
            columnWidth: '50%'
          }
        },
        colors: [isPositive ? '#22c55e' : '#ef4444', '#93c5fd'],
        xaxis: { categories: ['Current'] },
        legend: { show: false },
        grid: { show: false }
      };

      this.macdChart = new ApexCharts(this.$refs.macdContainer, options);
      this.macdChart.render();
    },
    renderStochChart() {
      if (!this.$refs.stochContainer) return;
      const k = this.stochastic.k;

      const options = {
        series: [{
          name: '%K',
          data: [k]
        }],
        chart: {
          type: 'bar',
          height: '100%',
          toolbar: { show: false },
          sparkline: { enabled: true }
        },
        plotOptions: {
          bar: {
            borderRadius: 4,
            columnWidth: '50%'
          }
        },
        colors: [k < 20 ? '#22c55e' : (k > 80 ? '#ef4444' : '#93c5fd')],
        xaxis: { min: 0, max: 100, labels: { show: false } },
        grid: { show: false },
        legend: { show: false },
        annotations: {
          yaxis: [{
            y: 20,
            strokeDashArray: 2,
            borderColor: '#ef4444',
            label: { text: 'Oversold', style: { color: '#ef4444', fontSize: '10px' } }
          }, {
            y: 80,
            strokeDashArray: 2,
            borderColor: '#22c55e',
            label: { text: 'Overbought', style: { color: '#22c55e', fontSize: '10px' } }
          }]
        }
      };

      this.stochChart = new ApexCharts(this.$refs.stochContainer, options);
      this.stochChart.render();
    },
    formatPrice(price) {
      if (!price) return '0.00';
      return price.toFixed(price < 1 ? 6 : 2);
    }
  }
};

window.IndicatorsPanel = IndicatorsPanel;
