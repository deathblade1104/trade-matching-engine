export enum OrderSide {
  BUY = 'BUY',
  SELL = 'SELL',
}

export enum OrderStatus {
  OPEN = 'OPEN',
  PARTIAL = 'PARTIAL',
  FILLED = 'FILLED',
  EXPIRED = 'EXPIRED',
}

export enum OrderTaskQueueEnum {
  PROCESS_ORDER = 'PROCESS_ORDER',
  EXPIRE_ORDER = 'EXPIRE_ORDER',
}

export enum OrderJobNameEnum {
  PROCESS_ORDER = 'PROCESS_ORDER',
  EXPIRE_ORDER = 'EXPIRE_ORDER',
}

export enum OrderStatusActor {
  USER = 'USER',
  SYSTEM = 'SYSTEM',
}
