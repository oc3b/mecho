// SymbolGrid Component - Step 2
const SymbolGrid = {
  props: ['symbols', 'loading', 'error', 'onSymbolClick'],
  template: `
    <div>
      <!-- Loading Skeleton -->
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="n in 6" :key="n" class="bg-gray-800 rounded-lg shadow-lg p-6 animate-pulse">
          <div class="h-6 bg-gray-700 rounded w-1/2 mb-4"></div>
          <div class="h-4 bg-gray-700 rounded w-1/4 mb-2"></div>
          <div class="h-4 bg-gray-700 rounded w-3/4 mb-4"></div>
          <div class="h-8 bg-gray-700 rounded w-full"></div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="text-center py-12">
        <p class="text-red-400 text-lg mb-4">{{ error }}</p>
        <button @click="$emit('retry')" class="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors">
          Retry
        </button>
      </div>

      <!-- Empty State -->
      <div v-else-if="symbols && symbols.length === 0" class="text-center py-12">
        <p class="text-gray-400 text-xl">No symbols available</p>
      </div>

      <!-- Symbol Cards -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div 
          v-for="symbol in symbols" 
          :key="symbol.ticker"
          @click="$emit('symbol-click', symbol)"
          class="bg-gray-800 rounded-lg shadow-lg p-6 cursor-pointer hover:bg-gray-750 transition-colors border-2 border-transparent hover:border-blue-500">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-xl font-bold text-blue-400">{{ symbol.ticker }}</h3>
            <span 
              :class="symbol.success ? 'text-green-400' : 'text-red-400'"
              class="text-sm font-semibold">
              {{ symbol.success ? '✓' : '✗' }}
            </span>
          </div>
          
          <div class="space-y-2 text-sm text-gray-300">
            <div class="flex justify-between">
              <span>Asset Class:</span>
              <span class="text-gray-100">{{ symbol.assetClass || 'N/A' }}</span>
            </div>
            <div class="flex justify-between">
              <span>Data Points:</span>
              <span class="text-gray-100">{{ symbol.dataPointsCount || 0 }}</span>
            </div>
            <div class="flex justify-between">
              <span>Last fetch:</span>
              <span class="text-gray-100">{{ formatTime(symbol.lastFetchTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  methods: {
    formatTime(timestamp) {
      if (!timestamp) return 'Never';
      return new Date(timestamp).toLocaleString();
    }
  }
};

window.SymbolGrid = SymbolGrid;
