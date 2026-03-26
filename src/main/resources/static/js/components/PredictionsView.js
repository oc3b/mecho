// PredictionsView Component - Step 6
const PredictionsView = {
  props: ['predictions', 'loading', 'error'],
  template: `
    <div>
      <!-- Header with controls -->
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-200">Predictions</h2>
        <div class="flex items-center gap-4">
          <label class="flex items-center gap-2 cursor-pointer">
            <input 
              type="checkbox" 
              v-model="showAll" 
              class="w-4 h-4 accent-blue-500">
            <span class="text-sm text-gray-400">Show all (&lt;60% too)</span>
          </lab>
          <button 
            @click="$emit('refresh')"
            :disabled="refreshing"
            class="px-3 py-1 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 rounded transition-colors text-sm flex items-center gap-2">
            <span v-if="refreshing" class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
            Refresh
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="n in 6" :key="n" class="bg-gray-800 rounded-lg shadow-lg p-6 animate-pulse">
          <div class="h-6 bg-gray-700 rounded w-1/2 mb-4"></div>
          <div class="h-4 bg-gray-700 rounded w-full mb-2"></div>
          <div class="h-4 bg-gray-700 rounded w-3/4 mb-4"></div>
          <div class="h-2 bg-gray-700 rounded w-full"></div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="text-center py-12">
        <p class="text-red-400 text-lg mb-4">{{ error }}</p>
        <button @click="$emit('retry')" class="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors">Retry</button>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredPredictions.length === 0" class="text-center py-12">
        <p class="text-gray-400 text-xl">No predictions available</p>
        <p class="text-gray-500 text-sm mt-2" v-if="!showAll">Try enabling "Show all" to see lower probability predictions</p>
      </div>

      <!-- Predictions Grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div 
          v-for="pred in filteredPredictions" 
          :key="pred.ticker"
          class="bg-gray-800 rounded-lg shadow-lg p-6 hover:bg-gray-750 transition-colors border-2 border-transparent"
          :class="pred.direction === 'UP' ? 'hover:border-green-500' : 'hover:border-red-500'">
          <!-- Header -->
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-xl font-bold text-blue-400">{{ pred.ticker }}</h3>
            <div class="flex items-center gap-2">
              <span 
                class="text-2xl" 
                :class="pred.direction === 'UP' ? 'text-green-400' : 'text-red-400'">
                {{ pred.direction === 'UP' ? '&#8595;' : '&#8593;' }}
              </span>
            </div>
          </div>

          <!-- Direction -->
          <div class="mb-4">
            <span 
              class="px-3 py-1 rounded-full text-sm font-semibold"
              :class="pred.direction === 'UP' ? 'bg-green-600 text-white' : 'bg-red-600 text-white'">
              {{ pred.direction === 'UP' ? '&#8595; BULLISH' : '&#8593; BEARISH' }}
            </span>
          </div>

          <!-- Probability Bar -->
          <div class="mb-4">
            <div class="flex items-center justify-between mb-2">
              <span class="text-gray-400 text-sm">Probability</span>
              <span 
                class="font-bold" 
                :class="probColorClass(pred.probability)">
                {{ formatProbability(pred.probability) }}
              </span>
            </div>
            <div class="h-3 bg-gray-700 rounded-full overflow-hidden">
              <div 
                class="h-full rounded-full transition-all duration-500"
                :class="probBarClass(pred.probability)"
                :style="{ width: pred.probability + '%' }">
              </div>
            </div>
          </div>

          <!-- Target Price (if available) -->
          <div v-if="pred.targetPrice" class="mb-4 p-3 bg-gray-750 rounded">
            <div class="flex justify-between text-sm">
              <span class="text-gray-400">Target Price:</span>
              <span class="text-gray-200 font-semibold">{{ formatPrice(pred.targetPrice) }}</span>
            </div>
            <div class="flex justify-between text-sm mt-1">
              <span class="text-gray-400">Potential:</span>
              <span 
                class="font-semibold" 
                :class="pred.direction === 'UP' ? 'text-green-400' : 'text-red-400'">
                {{ formatPotential(pred) }}
              </span>
            </div>
          </div>

          <!-- Timeframe -->
          <div class="mb-4 text-sm text-gray-400">
            <span v-if="pred.timeframe">Timeframe: {{ pred.timeframe }}</span>
            <span v-if="pred.confidence"> | Confidence: {{ formatProbability(pred.confidence) }}</span>
          </div>

          <!-- Model/Algorithm Info -->
          <div v-if="pred.model" class="mb-4 text-xs text-gray-500">
            Model: {{ pred.model }}
          </div>

          <!-- Timestamp -->
          <div class="pt-3 border-t border-gray-700 text-xs text-gray-500">
            Last updated: {{ formatTime(pred.timestamp) }}
          </div>
        </div>
      </div>
    </div>
  `,
  data() {
    return {
      showAll: false,
      refreshing: false
    };
  },
  computed: {
    filteredPredictions() {
      let predictions = this.predictions || [];
      
      if (!this.showAll) {
        predictions = predictions.filter(p => p.probability >= 60);
      }

      return predictions.sort((a, b) => b.probability - a.probability);
    }
  },
  methods: {
    formatProbability(prob) {
      return prob.toFixed(1) + '%';
    },
    formatPrice(price) {
      if (!price) return 'N/A';
      return price.toFixed(price < 1 ? 6 : 2);
    },
    formatPotential(pred) {
      if (!pred.targetPrice || !pred.currentPrice) return 'N/A';
      const potential = ((pred.targetPrice - pred.currentPrice) / pred.currentPrice) * 100;
      const sign = potential > 0 ? '+' : '';
      return sign + potential.toFixed(2) + '%';
    },
    probColorClass(prob) {
      if (prob >= 80) return 'text-green-400';
      if (prob >= 70) return 'text-yellow-400';
      if (prob >= 60) return 'text-orange-400';
      return 'text-red-400';
    },
    probBarClass(prob) {
      if (prob >= 80) return 'bg-green-500';
      if (prob >= 70) return 'bg-yellow-500';
      if (prob >= 60) return 'bg-orange-500';
      return 'bg-red-500';
    },
    formatTime(timestamp) {
      if (!timestamp) return 'Never';
      const date = new Date(timestamp);
      const now = new Date();
      const diffMs = now - date;
      const diffMins = Math.floor(diffMs / 60000);
      
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return diffMins + ' min ago';
      
      const diffHrs = Math.floor(diffMins / 60);
      if (diffHrs < 24) return diffHrs + ' hour' + (diffHrs > 1 ? 's' : '') + ' ago';
      
      return date.toLocaleString();
    }
  }
};

window.PredictionsView = PredictionsView;
