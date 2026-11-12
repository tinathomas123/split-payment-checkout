package com.splitpay.payment.gateway;

import java.math.BigDecimal;

/** Abstraction over the card payment provider, so it can be simulated now and mocked in tests. */
public interface PaymentGateway {

    /** @return true if the charge succeeded, false if the card was declined */
    boolean charge(String cardLast4, BigDecimal amount);

    void refund(String cardLast4, BigDecimal amount);
}
