package com.sideprojects.tradematching.controller;

import com.sideprojects.tradematching.dto.MessageData;
import com.sideprojects.tradematching.dto.PagedResponse;
import com.sideprojects.tradematching.dto.order.CreateOrderDto;
import com.sideprojects.tradematching.dto.order.OrderResponseDto;
import com.sideprojects.tradematching.security.AuthContext;
import com.sideprojects.tradematching.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuthContext authContext;

    @PostMapping
    public ResponseEntity<MessageData<OrderResponseDto>> createOrder(
            @Valid @RequestBody CreateOrderDto dto) {
        OrderResponseDto data = orderService.createOrder(dto, authContext.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                MessageData.<OrderResponseDto>builder()
                        .message("Order has been received, and has been enqueued for processing.")
                        .data(data)
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageData<OrderResponseDto>> getOrderById(
            @PathVariable Long id) {
        OrderResponseDto data = orderService.getOrderById(id, authContext.getCurrentUserId());
        return ResponseEntity.ok(
                MessageData.<OrderResponseDto>builder().message("Fetched order successfully.").data(data).build());
    }

    @GetMapping
    public ResponseEntity<MessageData<PagedResponse<OrderResponseDto>>> getOrders(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        PagedResponse<OrderResponseDto> data = orderService.getOrdersByUserId(authContext.getCurrentUserId(), page, limit);
        return ResponseEntity.ok(
                MessageData.<PagedResponse<OrderResponseDto>>builder()
                        .message("Orders fetched successfully")
                        .data(data)
                        .build());
    }
}
