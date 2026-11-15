import { ProviderUnavailableError } from './errors';
import type { CardPaymentEvent, ChargeCardResult } from './types';
import { validateCardPayment } from './validation';

// Same test cards as payment-service's SimulatedPaymentGateway.
export const DECLINED_CARD = '0002';
export const PROVIDER_DOWN_CARD = '9999';

/**
 * Simulates charging a card.
 * - A decline is a normal business outcome, so it is returned (approved: false), not thrown.
 * - The provider being down is a technical error, so it is thrown and the state machine retries it.
 */
export const handler = async (event: CardPaymentEvent): Promise<ChargeCardResult> => {
  validateCardPayment(event);

  if (event.cardLast4 === PROVIDER_DOWN_CARD) {
    console.warn(`Order ${event.orderId}: payment provider unavailable for card ending ${event.cardLast4}`);
    throw new ProviderUnavailableError('Payment provider unavailable');
  }

  const approved = event.cardLast4 !== DECLINED_CARD;
  console.log(
    `Order ${event.orderId}: charge ${event.amount.toFixed(2)} on card ending ${event.cardLast4}: ` +
      (approved ? 'APPROVED' : 'DECLINED'),
  );

  return { orderId: event.orderId, cardLast4: event.cardLast4, amount: event.amount, approved };
};
