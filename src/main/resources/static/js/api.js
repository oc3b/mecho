// API Service for Mecho Backend
// Dynamically detect host and use port 8080 for backend API
const getApiBase = () => {
  const protocol = window.location.protocol;
  const host = window.location.hostname;
  return `${protocol}//${host}:8080/api`;
};

const api = {
  async getStatus() {
    const response = await fetch(`${getApiBase()}/status`);
    return response.json();
  },

  async getSymbols() {
    const response = await fetch(`${getApiBase()}/symbols`);
    return response.json();
  },

  async getSymbol(ticker) {
    const response = await fetch(`${getApiBase()}/symbols/${ticker}`);
    return response.json();
  },

  async getSymbolData(ticker) {
    const response = await fetch(`${getApiBase()}/symbols/${ticker}/data`);
    return response.json();
  },

  async getIndicator(ticker, type) {
    const response = await fetch(`${getApiBase()}/symbols/${ticker}/indicators`);
    const data = await response.json();
    return data[type] || null;
  },

  async getPredictions() {
    const response = await fetch(`${getApiBase()}/predictions`);
    return response.json();
  }
};

window.api = api;

  async getSymbols() {
    const response = await fetch(`${API_BASE}/symbols`);
    return response.json();
  },

  async getSymbol(ticker) {
    const response = await fetch(`${API_BASE}/symbols/${ticker}`);
    return response.json();
  },

  async getSymbolData(ticker) {
    const response = await fetch(`${API_BASE}/symbols/${ticker}/data`);
    return response.json();
  },

  async getIndicator(ticker, type) {
    const response = await fetch(`${API_BASE}/symbols/${ticker}/indicators`);
    const data = await response.json();
    return data[type] || null;
  },

  async getPredictions() {
    const response = await fetch(`${API_BASE}/predictions`);
    return response.json();
  }
};

window.api = api;
