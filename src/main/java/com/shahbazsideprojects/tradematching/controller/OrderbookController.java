package com.shahbazsideprojects.tradematching.controller;

import com.shahbazsideprojects.tradematching.dto.MessageData;
import com.shahbazsideprojects.tradematching.dto.PagedResponse;
import com.shahbazsideprojects.tradematching.dto.order.OrderbookLevelDto;
import com.shahbazsideprojects.tradematching.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orderbook")
@RequiredArgsConstructor
public class OrderbookController {

    private final OrderService orderService;

    @GetMapping("/{side}")
    public ResponseEntity<MessageData<PagedResponse<OrderbookLevelDto>>> getOrderbook(
            @PathVariable String side,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        PagedResponse<OrderbookLevelDto> data = orderService.getOrderbook(side, page, limit);
        return ResponseEntity.ok(
                MessageData.<PagedResponse<OrderbookLevelDto>>builder()
                        .message("Orderbook fetched successfully")
                        .data(data)
                        .build());
    }
}
