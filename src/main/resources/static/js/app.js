// Main Vue.js 3 Application - Updated with all components
const {
  createApp,
  ref,
  onMounted,
  onUnmounted,
  reactive
} = Vue;

const App = {
  components: {
    StatusCard,
    SymbolGrid,
    SymbolDetail,
    ChartView,
    IndicatorsPanel,
    PredictionsView
  },
  setup() {
    const currentPage = ref('dashboard');
    const backendConnected = ref(true);
    
    // Status
    const status = reactive({
      status: 'LOADING',
      uptime: 0,
      lastSync: null,
      symbolsCount: 0
    });

    // Symbols
    const symbols = ref([]);
    
    // Predictions
    const predictions = ref([]);

    // Loading states
    const loading = reactive({
      status: false,
      symbols: false,
      predictions: false
    });

    // Errors
    const errors = reactive({
      status: null,
      symbols: null,
      predictions: null
    });

    // Refresh states
    const refreshing = reactive({
      status: false,
      symbols: false,
      predictions: false
    });

    // Last updated timestamps
    const lastUpdated = reactive({
      status: null,
      symbols: null,
      predictions: null
    });

    // Selection state
    const selectedSymbol = ref(null);
    const chartSymbol = ref(null);
    const indicatorsSymbol = ref(null);

    // Error handling wrapper
    async function safeFetch(fetchFn, type) {
      loading[type] = true;
      errors[type] = null;
      try {
        const data = await fetchFn();
        return data;
      } catch (err) {
        console.error(`Failed to fetch ${type}:`, err);
        errors[type] = err.message || 'Failed to fetch data';
        backendConnected.value = false;
        return null;
      } finally {
        loading[type] = false;
      }
    }

    // Fetch status
    async function fetchStatus() {
      refreshing.status = true;
      const data = await safeFetch(() => api.getStatus(), 'status');
      if (data) {
        Object.assign(status, data);
        lastUpdated.status = Date.now();
        backendConnected.value = true;
      }
      refreshing.status = false;
    }

    // Fetch symbols
    async function fetchSymbols() {
      refreshing.symbols = true;
      const data = await safeFetch(() => api.getSymbols(), 'symbols');
      if (data) {
        symbols.value = data;
        lastUpdated.symbols = Date.now();
        backendConnected.value = true;
      }
      refreshing.symbols = false;
    }

    // Fetch predictions
    async function fetchPredictions() {
      refreshing.predictions = true;
      const data = await safeFetch(() => api.getPredictions(), 'predictions');
      if (data) {
        predictions.value = data;
        lastUpdated.predictions = Date.now();
        backendConnected.value = true;
      }
      refreshing.predictions = false;
    }

    // Manual refresh handler
    function refresh(type) {
      switch (type) {
        case 'status':
          fetchStatus();
          break;
        case 'symbols':
          fetchSymbols();
          break;
        case 'predictions':
          fetchPredictions();
          break;
      }
    }

    // Check backend connectivity
    async function checkBackend() {
      try {
        await api.getStatus();
        backendConnected.value = true;
        refresh('status');
      } catch (err) {
        console.error('Backend check failed:', err);
      }
    }

    // Open symbol detail modal
    function openSymbolDetail(symbol) {
      selectedSymbol.value = symbol;
    }

    // Open chart view from symbol detail
    function openChartView(symbol) {
      selectedSymbol.value = null;
      chartSymbol.value = symbol;
    }

    // Open indicators view from symbol detail
    function openIndicatorsView(symbol) {
      selectedSymbol.value = null;
      indicatorsSymbol.value = symbol;
    }

    // Format utilities
    function formatLastUpdated(timestamp) {
      if (!timestamp) return 'Never';
      const diff = Date.now() - timestamp;
      const mins = Math.floor(diff / 60000);
      
      if (mins < 1) return 'Just now';
      if (mins < 60) return mins + 'm ago';
      
      const hrs = Math.floor(mins / 60);
      return new Date(timestamp).toLocaleTimeString();
    }

    // Lifecycle hooks
    onMounted(() => {
      // Initial fetch
      fetchStatus();
      fetchSymbols();
      fetchPredictions();

      // Check backend connectivity
      checkBackend().catch(() => {});

      // Set up auto-refresh
      AutoRefresh.init({
        status: 30000,
        symbols: 60000,
        predictions: 30000
      });

      // Register refresh callbacks
      AutoRefresh.register('status', fetchStatus);
      AutoRefresh.register('symbols', fetchSymbols);
      AutoRefresh.register('predictions', fetchPredictions);
    });

    onUnmounted(() => {
      AutoRefresh.destroy();
    });

    return {
      currentPage,
      backendConnected,
      status,
      symbols,
      predictions,
      loading,
      errors,
      refreshing,
      lastUpdated,
      selectedSymbol,
      chartSymbol,
      indicatorsSymbol,
      fetchStatus,
      fetchSymbols,
      fetchPredictions,
      refresh,
      checkBackend,
      openSymbolDetail,
      openChartView,
      openIndicatorsView,
      formatLastUpdated
    };
  }
};

const app = createApp(App);
app.mount('#app');
