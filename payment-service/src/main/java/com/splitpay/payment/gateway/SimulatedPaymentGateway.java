package com.splitpay.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Simulated provider. Test cards:
 *  - last 4 "0002": declined (a business outcome, not retried)
 *  - last 4 "9999": provider unavailable (a technical error, retried then sent to the dead-letter topic)
 *  - anything else: approved
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);

    @Override
    public boolean charge(String cardLast4, BigDecimal amount) {
        if ("9999".equals(cardLast4)) {
            throw new ProviderUnavailableException("Payment provider unavailable");
        }
        boolean approved = !"0002".equals(cardLast4);
        log.info("Charge {} on card ending {}: {}", amount, cardLast4, approved ? "APPROVED" : "DECLINED");
        return approved;
    }

    @Override
    public void refund(String cardLast4, BigDecimal amount) {
        log.info("Refunded {} to card ending {}", amount, cardLast4);
    }
}
