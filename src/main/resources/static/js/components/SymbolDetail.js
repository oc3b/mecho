// SymbolDetail Modal Component - Step 3
const SymbolDetail = {
  props: ['symbol', 'visible', 'onClose', '.onViewChart', 'onViewIndicators'],
  template: `
    <div v-if="visible" 
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="$emit('close')">
      <div v-if="loading" class="bg-gray-800 rounded-lg shadow-xl p-12 min-w-[400px] text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-400 mx-auto mb-4"></div>
        <p class="text-gray-400">Loading symbol details...</p>
      </div>

      <div v-else-if="error" class="bg-gray-800 rounded-lg shadow-xl p-8 min-w-[400px] text-center">
        <p class="text-red-400 mb-4">{{ error }}</p>
        <button @click="$emit('close')" class="px-4 py-2 bg-gray-600 hover:bg-gray-500 rounded-lg transition-colors">
          Close
        </button>
      </div>

      <div v-else-if="symbol" class="bg-gray-800 rounded-lg shadow-xl min-w-[500px] max-w-2xl max-h-[80vh] overflow-y-auto">
        <div class="p-6 border-b border-gray-700 sticky top-0 bg-gray-800 z-10">
          <div class="flex items-center justify-between">
            <h2 class="text-2xl font-bold text-blue-400">{{ symbol.ticker }}</h2>
            <button @click="$emit('close')" class="text-gray-400 hover:text-white text-2xl">&times;</button>
          </div>
          <p class="text-gray-400 mt-1">{{ symbol.assetClass }}</p>
        </div>

        <div class="p-6 space-y-6">
          <!-- Key Price Info -->
          <div v-if="symbol.details" class="grid grid-cols-2 gap-4 mb-6">
            <div class="bg-gray-750 rounded-lg p-4">
              <p class="text-gray-400 text-sm mb-1">Current Price</p>
              <p class="text-2xl font-bold text-green-400">{{ formatPrice(symbol.details.price) }}</p>
            </div>
            <div class="bg-gray-750 rounded-lg p-4">
              <p class="text-gray-400 text-sm mb-1">Volume</p>
              <p class="text-xl font-semibold text-gray-200">{{ formatNumber(symbol.details.volume) }}</p>
            </div>
          </div>

          <!-- Timestamp Info -->
          <div v-if="symbol.details" class="text-sm text-gray-400">
            <p>Last updated: {{ new Date(symbol.details.timestamp).toLocaleString() }}</p>
          </div>

          <!-- Recent Data Table -->
          <div v-if="symbol.data && symbol.data.length > 0" class="mt-6">
            <h3 class="text-lg font-semibold text-gray-200 mb-3">Recent Data</h3>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-gray-700">
                    <th class="text-left py-2 text-gray-400">Date</th>
                    <th class="text-right py-2 text-gray-400">Open</th>
                    <th class="text-right py-2 text-gray-400">High</th>
                    <th class="text-right py-2 text-gray-400">Low</th>
                    <th class="text-right py-2 text-gray-400">Close</th>
                  </tr>
                </tbody>
                  <tru v-for="(row, idx) in symbol.data.slice(0, 10)" :key="idx" class="border-b border-gray-750">
                    <td class="py-2">{{ formatDate(row.date) }}</td>
                    <td class="text-right py-2">{{ formatPrice(row.open) }}</td>
                    <td class="text-right py-2 text-green-400">{{ formatPrice(row.high) }}</td>
                    <td class="text-right py-2 text-red-400">{{ formatPrice(row.low) }}</td>
                    <td class="text-right py-2" :class="row.close >= row.open ? 'text-green-400' : 'text-red-400'">{{ formatPrice(row.close) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="flex flex-wrap gap-3 pt-4 border-t border-gray-700">
            <button 
              @click="$emit('view-chart', symbol)"
              class="flex-1 px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors text-center">
              View Chart
            </button>
            <button 
              @click="$emit('view-indicators', symbol)"
              class="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg transition-colors text-center">
              View Indicators
            </button>
            <button 
              @click="$emit('close')"
              class="flex-1 px-4 py-2 bg-gray-600 hover:bg-gray-500 rounded-lg transition-colors text-center">
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  data() {
    return {
      loading: false,
      error: null,
      symbolDetails: null
    };
  },
  watch: {
    visible(newVal) {
      if (newVal && this.symbol) {
        this.fetchSymbolDetails();
      }
    }
  },
  methods: {
    async fetchSymbolDetails() {
      this.loading = true;
      this.error = null;
      try {
        const response = await fetch(`/api/symbols/${this.symbol.ticker}`);
        if (!response.ok) throw new Error('Failed to fetch symbol details');
        const data = await response.json();
        this.symbolDetails = data;
      } catch (err) {
        this.error = err.message;
      } finally {
        this.loading = false;
      }
    },
    formatPrice(price) {
      if (!price) return 'N/A';
      return price.toFixed(price < 1 ? 6 : 2);
    },
    formatNumber(num) {
      if (!num) return 'N/A';
      if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
      if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
      return num.toString();
    },
    formatDate(dateStr) {
      return new Date(dateStr).toLocaleDateString();
    }
  }
};

window.SymbolDetail = SymbolDetail;
