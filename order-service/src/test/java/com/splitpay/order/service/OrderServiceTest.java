package com.splitpay.order.service;

import com.splitpay.order.dto.CreateOrderRequest;
import com.splitpay.order.dto.OrderItemRequest;
import com.splitpay.order.events.PaymentResultEvent;
import com.splitpay.order.events.PaymentResultEvent.PaymentOutcome;
import com.splitpay.order.messaging.OrderEventPublisher;
import com.splitpay.order.model.Order;
import com.splitpay.order.model.OrderStatus;
import com.splitpay.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    // Cart: 2 x $25.00 + 1 x $50.00 = $100.00
    private List<OrderItemRequest> cart() {
        return List.of(
                new OrderItemRequest("Headphones", 2, new BigDecimal("25.00")),
                new OrderItemRequest("Backpack", 1, new BigDecimal("50.00")));
    }

    private CreateOrderRequest request(String cardA, String cardB) {
        return new CreateOrderRequest(cart(), "4242", new BigDecimal(cardA), "1881", new BigDecimal(cardB));
    }

    private Order pendingOrder() {
        return new Order(List.of(), new BigDecimal("100.00"),
                "4242", new BigDecimal("60.00"), "1881", new BigDecimal("40.00"));
    }

    @Test
    void createsOrderAndPublishesEventWhenSplitAddsUpToTotal() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(request("60.00", "40.00"));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(eventPublisher).publishOrderPlaced(order);
    }

    @Test
    void rejectsSplitThatDoesNotAddUpAndPublishesNothing() {
        assertThatThrownBy(() -> orderService.createOrder(request("60.00", "30.00")))
                .isInstanceOf(InvalidSplitPaymentException.class)
                .hasMessageContaining("must add up");
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void rejectsZeroAmountOnEitherCard() {
        assertThatThrownBy(() -> orderService.createOrder(request("100.00", "0")))
                .isInstanceOf(InvalidSplitPaymentException.class)
                .hasMessageContaining("greater than zero");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void acceptsAmountsWithDifferentDecimalScales() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(request("60.0", "40"));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("100");
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void marksOrderPaidWhenPaymentCompleted() {
        Order order = pendingOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent(1L, PaymentOutcome.COMPLETED, null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void marksOrderFailedWhenPaymentFailed() {
        Order order = pendingOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent(1L, PaymentOutcome.FAILED, "Card 1881 declined"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void ignoresDuplicateResultForAnOrderThatIsAlreadyFinal() {
        Order order = pendingOrder();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent(1L, PaymentOutcome.FAILED, "late duplicate"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
