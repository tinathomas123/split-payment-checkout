import { test } from 'node:test';
import assert from 'node:assert/strict';
import { handler as chargeCard } from '../chargeCard';
import { handler as refundCard } from '../refundCard';
import { handler as updateOrderStatus } from '../updateOrderStatus';
import type { OrderStatus } from '../types';

const payment = { orderId: 1, cardLast4: '4242', amount: 60 };

test('chargeCard approves a normal card', async () => {
  const result = await chargeCard(payment);
  assert.equal(result.approved, true);
  assert.equal(result.amount, 60);
});

test('chargeCard declines card 0002 without throwing', async () => {
  const result = await chargeCard({ ...payment, cardLast4: '0002' });
  assert.equal(result.approved, false);
});

test('chargeCard throws ProviderUnavailableError for card 9999, so Step Functions can retry', async () => {
  await assert.rejects(chargeCard({ ...payment, cardLast4: '9999' }), { name: 'ProviderUnavailableError' });
});

test('chargeCard rejects invalid input with InvalidPaymentError', async () => {
  await assert.rejects(chargeCard({ ...payment, amount: 0 }), { name: 'InvalidPaymentError' });
  await assert.rejects(chargeCard({ ...payment, cardLast4: '12' }), { name: 'InvalidPaymentError' });
});

test('refundCard returns refunded: true', async () => {
  const result = await refundCard(payment);
  assert.equal(result.refunded, true);
  assert.equal(result.cardLast4, '4242');
});

test('updateOrderStatus returns the status and a timestamp', async () => {
  const result = await updateOrderStatus({ orderId: 1, status: 'PAYMENT_FAILED', reason: 'Card B declined' });
  assert.equal(result.status, 'PAYMENT_FAILED');
  assert.ok(!Number.isNaN(Date.parse(result.updatedAt)));
});

test('updateOrderStatus rejects an unknown status', async () => {
  await assert.rejects(
    updateOrderStatus({ orderId: 1, status: 'SHIPPED' as OrderStatus }),
    { name: 'InvalidPaymentError' },
  );
});
