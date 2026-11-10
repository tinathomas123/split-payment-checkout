package com.splitpay.payment.service;

import com.splitpay.payment.dto.SplitPaymentRequest;
import com.splitpay.payment.model.Payment;
import com.splitpay.payment.model.PaymentStatus;
import com.splitpay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private SplitPaymentRequest request(String a, String b) {
        return new SplitPaymentRequest(1L, new BigDecimal("100.00"),
                "4242", new BigDecimal(a), "1881", new BigDecimal(b));
    }

    @Test
    void recordsTwoPendingPaymentsForValidSplit() {
        when(paymentRepository.findByOrderIdOrderBySplitPart(1L)).thenReturn(List.of());
        when(paymentRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Payment> payments = paymentService.recordSplitPayment(request("70.00", "30.00"));

        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getSplitPart).containsExactly(1, 2);
        assertThat(payments).allMatch(p -> p.getStatus() == PaymentStatus.PENDING);
    }

    @Test
    void rejectsSplitThatDoesNotMatchOrderTotal() {
        assertThatThrownBy(() -> paymentService.recordSplitPayment(request("70.00", "20.00")))
                .isInstanceOf(InvalidSplitPaymentException.class);
        verify(paymentRepository, never()).saveAll(anyList());
    }

    @Test
    void doesNotRecordPaymentsTwiceForSameOrder() {
        when(paymentRepository.findByOrderIdOrderBySplitPart(1L))
                .thenReturn(List.of(new Payment(1L, 1, "4242", new BigDecimal("70.00"))));

        assertThatThrownBy(() -> paymentService.recordSplitPayment(request("70.00", "30.00")))
                .isInstanceOf(InvalidSplitPaymentException.class)
                .hasMessageContaining("already recorded");
        verify(paymentRepository, never()).saveAll(anyList());
    }
}
