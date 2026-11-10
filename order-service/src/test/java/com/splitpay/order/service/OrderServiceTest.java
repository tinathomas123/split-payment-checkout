package com.splitpay.order.service;

import com.splitpay.order.dto.CreateOrderRequest;
import com.splitpay.order.dto.OrderItemRequest;
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

    @InjectMocks
    private OrderService orderService;

    // Cart: 2 x $25.00 + 1 x $50.00 = $100.00
    private List<OrderItemRequest> cart() {
        return List.of(
                new OrderItemRequest("Headphones", 2, new BigDecimal("25.00")),
                new OrderItemRequest("Backpack", 1, new BigDecimal("50.00")));
    }

    @Test
    void createsOrderWhenSplitAddsUpToTotal() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                new CreateOrderRequest(cart(), new BigDecimal("60.00"), new BigDecimal("40.00")));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void rejectsSplitThatDoesNotAddUpToTotal() {
        CreateOrderRequest request =
                new CreateOrderRequest(cart(), new BigDecimal("60.00"), new BigDecimal("30.00"));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidSplitPaymentException.class)
                .hasMessageContaining("must add up");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsZeroAmountOnEitherCard() {
        CreateOrderRequest request =
                new CreateOrderRequest(cart(), new BigDecimal("100.00"), BigDecimal.ZERO);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidSplitPaymentException.class)
                .hasMessageContaining("greater than zero");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void acceptsAmountsWithDifferentDecimalScales() {
        // 60.0 + 40 = 100.00 should be valid; BigDecimal.equals would wrongly reject it
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createOrder(
                new CreateOrderRequest(cart(), new BigDecimal("60.0"), new BigDecimal("40")));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("100");
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
