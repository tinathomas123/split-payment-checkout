// Types that mirror the JSON returned by the Spring Boot services.
// Money arrives as JSON numbers (BigDecimal on the server); the UI works in whole cents
// to avoid floating-point rounding errors when splitting a total.

export interface Product {
  id: string;
  name: string;
  priceCents: number;
  description: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PAYMENT_FAILED';

/** GET /orders/{id} and POST /orders response (order-service OrderResponse). */
export interface Order {
  id: number;
  totalAmount: number;
  cardALast4: string;
  cardAAmount: number;
  cardBLast4: string;
  cardBAmount: number;
  status: OrderStatus;
}

export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED';

/** GET /payments?orderId= response item (payment-service PaymentResponse). */
export interface Payment {
  id: number;
  orderId: number;
  splitPart: number; // 1 = card A, 2 = card B
  cardLast4: string;
  amount: number;
  status: PaymentStatus;
}

/** POST /orders request body (order-service CreateOrderRequest). */
export interface CreateOrderRequest {
  items: { productName: string; quantity: number; unitPrice: number }[];
  cardALast4: string;
  cardAAmount: number;
  cardBLast4: string;
  cardBAmount: number;
}
