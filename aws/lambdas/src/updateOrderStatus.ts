import { InvalidPaymentError } from './errors';
import type { UpdateOrderStatusEvent, UpdateOrderStatusResult } from './types';

const VALID_STATUSES = ['PAID', 'PAYMENT_FAILED'];

/**
 * Simulates updating the order's status. In a full deployment this would call order-service
 * (or publish a payment-result event); here it logs the outcome so it shows in CloudWatch
 * and returns it as the final output of the workflow.
 */
export const handler = async (event: UpdateOrderStatusEvent): Promise<UpdateOrderStatusResult> => {
  if (!Number.isInteger(event.orderId) || event.orderId <= 0) {
    throw new InvalidPaymentError(`Invalid order ID: ${event.orderId}`);
  }
  if (!VALID_STATUSES.includes(event.status)) {
    throw new InvalidPaymentError(`Invalid status: ${event.status}`);
  }

  console.log(`Order ${event.orderId}: status set to ${event.status}${event.reason ? ` (${event.reason})` : ''}`);
  return { ...event, updatedAt: new Date().toISOString() };
};
