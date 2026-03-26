// ChartView Component - Step 4
const ChartView = {
  props: ['symbol', 'visible', 'onClose'],
  template: `
    <div v-if="visible" 
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
      @click.self="$emit('close')">
      <div class="bg-gray-800 rounded-lg shadow-xl min-w-[600px] max-w-5xl max-h-[90vh] flex flex-col">
        <div class="p-4 border-b border-gray-700 flex items-center justify-between">
          <h2 class="text-xl font-bold text-blue-400">{{ symbol?.ticker }} <span class="text-gray-400 text-base">- Chart View</span></h2>
          <button @click="$emit('close')" class="text-gray-400 hover:text-white text-2xl">&times;</button>
        </div>

        <div class="flex-1 overflow-hidden p-4 flex flex-col">
          <!-- Time Range Selector -->
          <div class="flex flex-wrap gap-2 mb-4">
            <button 
              v-for="range in timeRanges" 
              :key="range.value"
              @click="changeRange(range.value)"
              :class="selectedRange === range.value ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'"
              class="px-3 py-1 rounded transition-colors text-sm">
              {{ range.label }}
            </button>
            <button 
              v-if="selectedRange === 'CUSTOM'"
              @click="showCustomRange = true"
              class="px-3 py-1 bg-gray-700 text-gray-300 hover:bg-gray-600 rounded transition-colors text-sm">
              Custom
            </button>
          </div>

          <!-- Chart Container -->
          <div v-if="loading" class="flex-1 flex items-center justify-center">
            <div class="text-center">
              <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-400 mx-auto mb-4"></div>
              <p class="text-gray-400">Loading chart data...</p>
            </div>
          </div>
          <div v-else-if="error" class="flex-1 flex items-center justify-center">
            <div class="text-center">
              <p class="text-red-400 mb-4">{{ error }}</p>
              <button @click="fetchChartData" class="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded transition-colors">Retry</button>
            </div>
          </div>
          <div v-else ref="chartContainer" class="flex-1 min-h-[400px]"></div>

          <!-- Chart Controls -->
          <div class="flex items-center justify-between mt-4 pt-4 border-t border-gray-700">
            <div class="text-sm text-gray-400">
              <span v-if="latestPrice">Latest: <span class="text-green-400 font-semibold">{{ formatPrice(latestPrice) }}</span></span>
            </div>
            <div class="flex gap-2">
              <button 
                @click="zoomChart('out')"
                class="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded transition-colors text-sm">
                </svg>
              <button 
                @click="resetView()"
                class="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded transition-colors text-sm">
                Reset View
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  data() {
    return {
      chartData: [],
      loading: false,
      error: null,
      chartInstance: null,
      selectedRange: '1M',
      timeRanges: [
        { value: '1D', label: '1 Day' },
        { value: '1W', label: '1 Week' },
        { value: '1M', label: '1 Month' },
        { value: '3M', label: '3 Months' },
        { value: 'ALL', label: 'All' }
      ],
      latestPrice: null,
      showCustomRange: false
    };
  },
  watch: {
    visible(newVal) {
      if (newVal && this.symbol) {
        this.selectedRange = '1M';
        this.fetchChartData();
      }
    }
  },
  beforeUnmount() {
    if (this.chartInstance) {
      this.chartInstance.destroy();
    }
  },
  methods: {
    async fetchChartData() {
      this.loading = true;
      this.error = null;
      try {
        const response = await fetch(`/api/symbols/${this.symbol.ticker}/data`);
        if (!response.ok) throw new Error('Failed to fetch chart data');
        const data = await response.json();
        this.processChartData(data);
      } catch (err) {
        this.error = err.message;
      } finally {
        this.loading = false;
      }
    },
    processChartData(data) {
      if (!data || data.length === 0) {
        this.chartData = [];
        return;
      }

      this.chartData = data.map(d => ({
        x: new Date(d.date).getTime(),
        y: [d.open, d.high, d.low, d.close]
      })).sort((a, b) => a.x - b.x);

      this.latestPrice = data[data.length - 1]?.close;

      this.$nextTick(() => {
        this.renderChart();
      });
    },
    renderChart() {
      if (this.chartInstance) {
        this.chartInstance.destroy();
      }

      const filteredData = this.filterDataByRange();

      const options = {
        series: [{
          data: filteredData
        }],
        chart: {
          type: 'candlestick',
          height: '100%',
          toolbar: {
            show: true,
            tools: {
              download: true,
              selection: true,
              zoom: true,
              zoomin: true,
              zoomout: true,
              pan: true,
              reset: true
            }
          }
        },
        title: {
          text: this.symbol.ticker,
          align: 'left',
          style: { color: '#93c5fd' }
        },
        theme: {
          mode: 'dark'
        },
        plotOptions: {
          candlestick: {
            colors: {
              upward: '#22c55e',
              downward: '#ef4444'
            }
          }
        },
        xaxis: {
          type: 'datetime',
          labels: {
            style: { colors: '#9ca3af' }
          },
          axisBorder: {
            color: '#374151'
          },
          axisTicks: {
            color: '#374151'
          }
        },
        yaxis: {
          labels: {
            style: { colors: '#9ca3af' },
            formatter: (val) => this.formatPrice(val)
          }
        },
        grid: {
          borderColor: '#374151'
        },
        tooltip: {
          theme: 'dark',
          y: {
            formatter: (val) => this.formatPrice(val)
          }
        },
        dataLabels: {
          enabled: false
        }
      };

      this.chartInstance = new ApexCharts(this.$refs.chartContainer, options);
      this.chartInstance.render();
    },
    filterDataByRange() {
      if (this.selectedRange === 'ALL') return this.chartData;

      const now = Date.now();
      let rangeMs;

      switch (this.selectedRange) {
        case '1D': rangeMs = 24 * 60 * 60 * 1000; break;
        case '1W': rangeMs = 7 * 24 * 60 * 60 * 1000; break;
        case '1M': rangeMs = 30 * 24 * 60 * 60 * 1000; break;
        case '3M': rangeMs = 90 * 24 * 60 * 60 * 1000; break;
        default: return this.chartData;
      }

      const cutoff = now - rangeMs;
      return this.chartData.filter(d => d.x >= cutoff);
    },
    changeRange(range) {
      this.selectedRange = range;
      this.renderChart();
    },
    resetView() {
      if (this.chartInstance) {
        this.chartInstance.zoomX(0, this.chartData.length);
      }
    },
    zoomChart(direction) {
      if (this.chartInstance && direction === 'out') {
        this.chartInstance.zoomOut();
      }
    },
    formatPrice(price) {
      if (!price) return '0.00';
      return price.toFixed(price < 1 ? 6 : 2);
    }
  }
};

window.ChartView = ChartView;
