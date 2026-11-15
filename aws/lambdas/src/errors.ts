// Step Functions sees a thrown error's `name` as the error type, so the state machine can
// retry one kind of error and not another.

/** A technical failure (the payment provider is down). The state machine retries this. */
export class ProviderUnavailableError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'ProviderUnavailableError';
  }
}

/** Bad input (for example, a negative amount). Retrying won't help, so it isn't retried. */
export class InvalidPaymentError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'InvalidPaymentError';
  }
}
