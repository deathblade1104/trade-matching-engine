export enum OrderType {
  LIMIT = 'LIMIT', // Standard limit order
  MARKET = 'MARKET', // Market order (immediate execution)
  STOP_LOSS = 'STOP_LOSS', // Stop loss order
  TAKE_PROFIT = 'TAKE_PROFIT', // Take profit order
  ICEBERG = 'ICEBERG', // Large order split into smaller chunks
}

export enum TimeInForce {
  GTC = 'GTC', // Good Till Cancelled
  IOC = 'IOC', // Immediate or Cancel
  FOK = 'FOK', // Fill or Kill
  DAY = 'DAY', // Good for Day
}
