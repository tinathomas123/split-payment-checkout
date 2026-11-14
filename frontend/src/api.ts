import type { CreateOrderRequest, Order, Payment } from './types';

// Relative URLs: Vite (development) or nginx (Docker/Kubernetes) forwards /api/... to the services.
const ORDERS_URL = '/api/orders';
const PAYMENTS_URL = '/api/payments';

/** Error with the message the service sent back (Spring's ProblemDetail "detail" field). */
export class ApiError extends Error {
  constructor(message: string, public readonly status: number) {
    super(message);
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body && typeof body.detail === 'string') {
        message = body.detail;
      }
    } catch {
      // The body wasn't JSON; keep the generic message.
    }
    throw new ApiError(message, response.status);
  }
  return response.json() as Promise<T>;
}

export async function createOrder(request: CreateOrderRequest): Promise<Order> {
  const response = await fetch(ORDERS_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  return handleResponse<Order>(response);
}

export async function getOrder(id: number): Promise<Order> {
  return handleResponse<Order>(await fetch(`${ORDERS_URL}/${id}`));
}

export async function getPayments(orderId: number): Promise<Payment[]> {
  return handleResponse<Payment[]>(await fetch(`${PAYMENTS_URL}?orderId=${orderId}`));
}
