// Inputs and outputs of the three Lambdas. Step Functions passes these as JSON.

export interface CardPaymentEvent {
  orderId: number;
  cardLast4: string;
  amount: number;
}

export interface ChargeCardResult {
  orderId: number;
  cardLast4: string;
  amount: number;
  approved: boolean;
}

export interface RefundCardResult {
  orderId: number;
  cardLast4: string;
  amount: number;
  refunded: true;
}

export type OrderStatus = 'PAID' | 'PAYMENT_FAILED';

export interface UpdateOrderStatusEvent {
  orderId: number;
  status: OrderStatus;
  reason?: string;
}

export interface UpdateOrderStatusResult {
  orderId: number;
  status: OrderStatus;
  reason?: string;
  updatedAt: string;
}
