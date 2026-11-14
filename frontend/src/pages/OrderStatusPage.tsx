import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getOrder, getPayments } from '../api';
import { formatDollars } from '../money';
import type { Order, OrderStatus, Payment } from '../types';

const POLL_INTERVAL_MS = 2000;
// After this many checks (about 30 seconds) the page stops checking and explains why.
const MAX_POLLS = 15;

const STATUS_TEXT: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Processing payment…',
  PAID: 'Paid',
  PAYMENT_FAILED: 'Payment failed',
};

export default function OrderStatusPage() {
  const { orderId } = useParams();
  const id = Number(orderId);

  const [order, setOrder] = useState<Order | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [gaveUp, setGaveUp] = useState(false);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setError('That is not a valid order number.');
      return;
    }

    // `cancelled` stops updates after the user leaves the page (and stops the loop).
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let polls = 0;

    async function check() {
      try {
        const latest = await getOrder(id);
        const latestPayments = await getPayments(id);
        if (cancelled) return;
        setOrder(latest);
        setPayments(latestPayments);
        setError(null);

        polls += 1;
        if (latest.status !== 'PENDING_PAYMENT') {
          return; // final status reached: stop checking
        }
        if (polls >= MAX_POLLS) {
          setGaveUp(true);
          return;
        }
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 404) {
          setError(`Order ${id} was not found.`);
          return;
        }
        setError('Could not reach the services. Trying again…');
      }
      // Schedule the next check only after this one finishes, so requests never overlap.
      timer = setTimeout(check, POLL_INTERVAL_MS);
    }

    check();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [id]);

  return (
    <section>
      <h1>Order {Number.isInteger(id) ? `#${id}` : ''}</h1>

      {error && <p className="error" role="alert">{error}</p>}

      {order && (
        <>
          <p className={`status status-${order.status.toLowerCase()}`}>{STATUS_TEXT[order.status]}</p>
          <p>Total: {formatDollars(order.totalAmount)}</p>

          {order.status === 'PENDING_PAYMENT' && !gaveUp && (
            <p className="muted">Checking every {POLL_INTERVAL_MS / 1000} seconds…</p>
          )}
          {gaveUp && (
            <p className="notice">
              Still waiting after 30 seconds. If the payment provider is down, payment-service
              retries 3 times and then moves the message to the dead-letter topic, so the order
              stays pending. <button className="link-button" onClick={() => window.location.reload()}>Check again</button>
            </p>
          )}

          <h2>Payments</h2>
          {payments.length === 0 ? (
            <p className="muted">No payments recorded yet.</p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Card</th>
                  <th>Last 4</th>
                  <th>Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {payments.map((payment) => (
                  <tr key={payment.id}>
                    <td>{payment.splitPart === 1 ? 'A' : 'B'}</td>
                    <td>{payment.cardLast4}</td>
                    <td>{formatDollars(payment.amount)}</td>
                    <td>{payment.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      <p><Link to="/">Continue shopping</Link></p>
    </section>
  );
}
