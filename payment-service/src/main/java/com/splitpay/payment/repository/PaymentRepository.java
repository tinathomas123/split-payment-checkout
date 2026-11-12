package com.splitpay.payment.repository;

import com.splitpay.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderBySplitPart(Long orderId);
    boolean existsByOrderId(Long orderId);
}
