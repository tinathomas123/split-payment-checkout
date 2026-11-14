/** 2550 -> "$25.50" */
export function formatCents(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}

/** 25.5 (from the API) -> "$25.50" */
export function formatDollars(amount: number): string {
  return `$${amount.toFixed(2)}`;
}

/** Cents -> a dollar value for the API, e.g. 2550 -> 25.5 (sent as JSON, read as BigDecimal). */
export function centsToDollars(cents: number): number {
  return Number((cents / 100).toFixed(2));
}

/**
 * Text typed into an amount box -> cents, or null if it isn't a valid amount.
 * Accepts "25", "25.5" and "25.50"; rejects negatives, letters and more than 2 decimals.
 */
export function parseDollarsToCents(text: string): number | null {
  const trimmed = text.trim();
  if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) {
    return null;
  }
  return Math.round(Number(trimmed) * 100);
}
