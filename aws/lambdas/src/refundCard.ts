import type { CardPaymentEvent, RefundCardResult } from './types';
import { validateCardPayment } from './validation';

/**
 * Simulates refunding a card: the compensating action that undoes a successful charge
 * when a later step of the saga fails.
 */
export const handler = async (event: CardPaymentEvent): Promise<RefundCardResult> => {
  validateCardPayment(event);
  console.log(`Order ${event.orderId}: refunded ${event.amount.toFixed(2)} to card ending ${event.cardLast4}`);
  return { orderId: event.orderId, cardLast4: event.cardLast4, amount: event.amount, refunded: true };
};
