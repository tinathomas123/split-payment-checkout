package com.splitpay.payment.service;

import com.splitpay.payment.events.OrderPlacedEvent;
import com.splitpay.payment.events.PaymentResultEvent;
import com.splitpay.payment.events.PaymentResultEvent.PaymentOutcome;
import com.splitpay.payment.gateway.PaymentGateway;
import com.splitpay.payment.gateway.ProviderUnavailableException;
import com.splitpay.payment.model.Payment;
import com.splitpay.payment.model.PaymentStatus;
import com.splitpay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<List<Payment>> savedPayments;

    private static final BigDecimal SIXTY = new BigDecimal("60.00");
    private static final BigDecimal FORTY = new BigDecimal("40.00");

    private OrderPlacedEvent event() {
        return new OrderPlacedEvent(1L, new BigDecimal("100.00"), "4242", SIXTY, "1881", FORTY);
    }

    @Test
    void completesWhenBothCardsAreApproved() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentGateway.charge(anyString(), any())).thenReturn(true);

        Optional<PaymentResultEvent> result = paymentService.processSplitPayment(event());

        assertThat(result).get().extracting(PaymentResultEvent::outcome).isEqualTo(PaymentOutcome.COMPLETED);
        verify(paymentRepository).saveAll(savedPayments.capture());
        assertThat(savedPayments.getValue()).allMatch(p -> p.getStatus() == PaymentStatus.SUCCEEDED);
    }

    @Test
    void refundsCardAWhenCardBIsDeclined() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentGateway.charge("4242", SIXTY)).thenReturn(true);
        when(paymentGateway.charge("1881", FORTY)).thenReturn(false);

        Optional<PaymentResultEvent> result = paymentService.processSplitPayment(event());

        assertThat(result).get().extracting(PaymentResultEvent::outcome).isEqualTo(PaymentOutcome.FAILED);
        verify(paymentGateway).refund("4242", SIXTY);
        verify(paymentRepository).saveAll(savedPayments.capture());
        assertThat(savedPayments.getValue()).extracting(Payment::getStatus)
                .containsExactly(PaymentStatus.REFUNDED, PaymentStatus.FAILED);
    }

    @Test
    void doesNotChargeCardBWhenCardAIsDeclined() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentGateway.charge("4242", SIXTY)).thenReturn(false);

        Optional<PaymentResultEvent> result = paymentService.processSplitPayment(event());

        assertThat(result).get().extracting(PaymentResultEvent::outcome).isEqualTo(PaymentOutcome.FAILED);
        verify(paymentGateway, never()).charge(eq("1881"), any());
        verify(paymentGateway, never()).refund(anyString(), any());
    }

    @Test
    void skipsDuplicateEventForAnOrderAlreadyProcessed() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        Optional<PaymentResultEvent> result = paymentService.processSplitPayment(event());

        assertThat(result).isEmpty();
        verifyNoInteractions(paymentGateway);
        verify(paymentRepository, never()).saveAll(anyList());
    }

    @Test
    void failsOrderWhenAmountsDoNotAddUp() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        OrderPlacedEvent bad = new OrderPlacedEvent(1L, new BigDecimal("100.00"), "4242", SIXTY, "1881", SIXTY);

        Optional<PaymentResultEvent> result = paymentService.processSplitPayment(bad);

        assertThat(result).get().extracting(PaymentResultEvent::outcome).isEqualTo(PaymentOutcome.FAILED);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void letsProviderOutagePropagateSoTheMessageIsRetried() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(paymentGateway.charge(anyString(), any())).thenThrow(new ProviderUnavailableException("timeout"));

        assertThatThrownBy(() -> paymentService.processSplitPayment(event()))
                .isInstanceOf(ProviderUnavailableException.class);
        verify(paymentRepository, never()).saveAll(anyList());
    }
}
