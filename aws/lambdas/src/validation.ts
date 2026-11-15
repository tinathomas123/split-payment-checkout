import { InvalidPaymentError } from './errors';
import type { CardPaymentEvent } from './types';

const LAST4_PATTERN = /^\d{4}$/;

/** Throws InvalidPaymentError if the event can't be a real card payment. */
export function validateCardPayment(event: CardPaymentEvent): void {
  if (!Number.isInteger(event.orderId) || event.orderId <= 0) {
    throw new InvalidPaymentError(`Invalid order ID: ${event.orderId}`);
  }
  if (typeof event.cardLast4 !== 'string' || !LAST4_PATTERN.test(event.cardLast4)) {
    throw new InvalidPaymentError('cardLast4 must be exactly 4 digits');
  }
  if (typeof event.amount !== 'number' || !(event.amount > 0)) {
    throw new InvalidPaymentError(`Amount must be greater than 0, got ${event.amount}`);
  }
}
