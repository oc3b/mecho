// Auto-Refresh Utility - Step 7
const AutoRefresh = {
  intervals: {},
  lastUpdated: {
    status: null,
    symbols: null,
    predictions: null
  },
  refreshCallbacks: {},
  debounceTimers: {},

  init(config = {}) {
    this.config = {
      status: config.statusInterval || 30000,
      symbols: config.symbolsInterval || 60000,
      predictions: config.predictionsInterval || 30000,
      ...config
    };

    this.startAll();
  },

  start(type) {
    const intervals = { status: this.config.status, symbols: this.config.symbols, predictions: this.config.predictions };
    const interval = intervals[type];

    if (!interval) return;

    this.stop(type);

    this.intervals[type] = setInterval(() => {
      this.refresh(type);
    }, interval);
  },

  stop(type) {
    if (this.intervals[type]) {
      clearInterval(this.intervals[type]);
      delete this.intervals[type];
    }
  },

  stopAll() {
    Object.keys(this.intervals).forEach(type => this.stop(type));
  },

  startAll() {
    this.start('status');
    this.start('symbols');
    this.start('predictions');
  },

  refresh(type, force = false) {
    if (!this.config[type]) return;

    if (!force && this.debounce(type)) return;

    this.refreshCallbacks[type]?.();
    this.lastUpdated[type] = Date.now();
  },

  debounce(type) {
    const NOW = Date.now();
    if (this.debounceTimers[type] && NOW - this.debounceTimers[type] < 5000) {
      return true
    }
    this.debounceTimers[type] = NOW;
    return false;
  },

  register(type, callback) {
    this.refreshCallbacks[type] = callback;
  },

  unregister(type) {
    delete this.refreshCallbacks[type];
  },

  getLastUpdated(type) {
    return this.lastUpdated[type];
  },

  formatLastUpdated(timestamp) {
    if (!timestamp) return 'Never';
    const diff = Date.now() - timestamp;
    const mins = Math.floor(diff / 60000);
    
    if (mins < 1) return 'Just now';
    if (mins < 60) return mins + 'm ago';
    
    const hrs = Math.floor(mins / 60);
    return new Date(timestamp).toLocaleTimeString();
  },

  toggleInterval(type, enabled) {
    if (enabled) {
      this.start(type);
    } else {
      this.stop(type);
    }
  },

  destroy() {
    this.stopAll();
    this.refreshCallbacks = {};
    this.lastUpdated = { status: null, symbols: null, predictions: null };
  }
};

window.AutoRefresh = AutoRefresh;
