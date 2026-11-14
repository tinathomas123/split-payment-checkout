import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, createOrder } from '../api';
import { useCart } from '../CartContext';
import { centsToDollars, formatCents, parseDollarsToCents } from '../money';
import type { CreateOrderRequest } from '../types';

const LAST4_PATTERN = /^\d{4}$/;

export default function CheckoutPage() {
  const { items, totalCents, clearCart } = useCart();
  const navigate = useNavigate();

  const [cardALast4, setCardALast4] = useState('');
  const [cardBLast4, setCardBLast4] = useState('');
  // Start with an even split; card B always gets whatever card A doesn't cover.
  const [cardAAmountText, setCardAAmountText] = useState((Math.ceil(totalCents / 2) / 100).toFixed(2));
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (items.length === 0) {
    return (
      <section>
        <h1>Checkout</h1>
        <p>Your cart is empty. <Link to="/">Browse products</Link></p>
      </section>
    );
  }

  const cardACents = parseDollarsToCents(cardAAmountText);
  const cardBCents = cardACents === null ? null : totalCents - cardACents;

  /** Returns a message describing the first problem, or null if the form is valid. */
  function validate(): string | null {
    if (!LAST4_PATTERN.test(cardALast4) || !LAST4_PATTERN.test(cardBLast4)) {
      return 'Enter the last 4 digits of both cards.';
    }
    if (cardACents === null) {
      return 'Enter a valid amount for card A, for example 25.50.';
    }
    if (cardACents <= 0 || cardBCents === null || cardBCents <= 0) {
      return `Each card must pay more than $0.00, and card A must pay less than ${formatCents(totalCents)}.`;
    }
    return null;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }

    const request: CreateOrderRequest = {
      items: items.map(({ product, quantity }) => ({
        productName: product.name,
        quantity,
        unitPrice: centsToDollars(product.priceCents),
      })),
      cardALast4,
      cardAAmount: centsToDollars(cardACents!),
      cardBLast4,
      cardBAmount: centsToDollars(cardBCents!),
    };

    setSubmitting(true);
    setError(null);
    try {
      const order = await createOrder(request);
      clearCart();
      navigate(`/orders/${order.id}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not reach the order service. Is it running?');
      setSubmitting(false);
    }
  }

  const splitEvenly = () => setCardAAmountText((Math.ceil(totalCents / 2) / 100).toFixed(2));

  return (
    <section>
      <h1>Checkout</h1>
      <p className="total">Order total: {formatCents(totalCents)}</p>

      <form onSubmit={handleSubmit} className="checkout-form" noValidate>
        <fieldset>
          <legend>Card A</legend>
          <label>
            Last 4 digits
            <input
              value={cardALast4}
              onChange={(e) => setCardALast4(e.target.value)}
              inputMode="numeric"
              maxLength={4}
              placeholder="4242"
            />
          </label>
          <label>
            Amount ($)
            <input
              value={cardAAmountText}
              onChange={(e) => setCardAAmountText(e.target.value)}
              inputMode="decimal"
            />
          </label>
        </fieldset>

        <fieldset>
          <legend>Card B</legend>
          <label>
            Last 4 digits
            <input
              value={cardBLast4}
              onChange={(e) => setCardBLast4(e.target.value)}
              inputMode="numeric"
              maxLength={4}
              placeholder="1881"
            />
          </label>
          <p>
            Amount: <strong>{cardBCents === null ? '—' : formatCents(cardBCents)}</strong>{' '}
            <span className="muted">(the rest of the total)</span>
          </p>
        </fieldset>

        <button type="button" className="secondary" onClick={splitEvenly}>Split evenly</button>

        <p className="muted small">
          Test cards (last 4 digits): <code>0002</code> is declined; <code>9999</code> simulates the
          payment provider being down; anything else is approved. Only the last 4 digits are sent.
        </p>

        {error && <p className="error" role="alert">{error}</p>}

        <button type="submit" disabled={submitting}>
          {submitting ? 'Placing order…' : 'Place order'}
        </button>
      </form>
    </section>
  );
}
