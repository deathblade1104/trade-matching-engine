package com.shahbazsideprojects.tradematching.service;

import com.shahbazsideprojects.tradematching.dto.PagedResponse;
import com.shahbazsideprojects.tradematching.dto.order.CreateOrderDto;
import com.shahbazsideprojects.tradematching.dto.order.OrderResponseDto;
import com.shahbazsideprojects.tradematching.dto.order.OrderbookLevelDto;
import com.shahbazsideprojects.tradematching.entity.Order;
import com.shahbazsideprojects.tradematching.entity.OrderStatusHistory;
import com.shahbazsideprojects.tradematching.entity.enums.OrderSide;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatus;
import com.shahbazsideprojects.tradematching.entity.enums.OrderStatusActor;
import com.shahbazsideprojects.tradematching.exception.BadRequestException;
import com.shahbazsideprojects.tradematching.exception.ForbiddenException;
import com.shahbazsideprojects.tradematching.exception.ResourceNotFoundException;
import com.shahbazsideprojects.tradematching.kafka.OrderEventProducer;
import com.shahbazsideprojects.tradematching.mapper.OrderMapper;
import com.shahbazsideprojects.tradematching.repository.OrderRepository;
import com.shahbazsideprojects.tradematching.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponseDto createOrder(CreateOrderDto dto, Long userId) {
        Order order = orderMapper.toOrder(dto, userId);
        order = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .actor(OrderStatusActor.USER)
                .build();
        orderStatusHistoryRepository.save(history);

        orderEventProducer.sendProcessOrder(order.getId(), 0);
        orderEventProducer.sendExpireOrder(order.getId());

        return orderMapper.toOrderResponseDto(order, List.of(history));
    }

    public OrderResponseDto getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to access this order");
        }
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return orderMapper.toOrderResponseDto(order, history);
    }

    public PagedResponse<OrderbookLevelDto> getOrderbook(String side, int page, int limit) {
        OrderSide orderSide;
        try {
            orderSide = OrderSide.valueOf(side.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("side param should be in [BUY,SELL].");
        }
        List<OrderStatus> active = List.of(OrderStatus.OPEN, OrderStatus.PARTIAL);
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), limit);
        List<Order> orders = orderSide == OrderSide.BUY
                ? orderRepository.findActiveBySideWithUserOrderByPriceDesc(orderSide, active, pr)
                : orderRepository.findActiveBySideWithUserOrderByPriceAsc(orderSide, active, pr);

        List<OrderbookLevelDto> items = orders.stream()
                .map(orderMapper::toOrderbookLevelDto)
                .collect(Collectors.toList());

        return PagedResponse.<OrderbookLevelDto>builder()
                .total((long) items.size())
                .page(page)
                .limit(limit)
                .items(items)
                .build();
    }

    public PagedResponse<OrderResponseDto> getOrdersByUserId(Long userId, int page, int limit) {
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), limit);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        long total = orderRepository.countByUserId(userId);
        List<OrderResponseDto> items = orders.stream().map(o -> {
            List<OrderStatusHistory> h = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(o.getId());
            return orderMapper.toOrderResponseDto(o, h);
        }).collect(Collectors.toList());
        return PagedResponse.<OrderResponseDto>builder()
                .total(total)
                .page(page)
                .limit(limit)
                .items(items)
                .build();
    }

}
