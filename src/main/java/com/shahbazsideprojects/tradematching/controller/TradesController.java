package com.shahbazsideprojects.tradematching.controller;

import com.shahbazsideprojects.tradematching.dto.MessageData;
import com.shahbazsideprojects.tradematching.dto.PagedResponse;
import com.shahbazsideprojects.tradematching.dto.order.TradeResponseDto;
import com.shahbazsideprojects.tradematching.security.AuthContext;
import com.shahbazsideprojects.tradematching.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradesController {

    private final TradeService tradeService;
    private final AuthContext authContext;

    @GetMapping
    public ResponseEntity<MessageData<PagedResponse<TradeResponseDto>>> getTrades(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        PagedResponse<TradeResponseDto> data = tradeService.getTradesByUserId(authContext.getCurrentUserId(), page, limit);
        return ResponseEntity.ok(
                MessageData.<PagedResponse<TradeResponseDto>>builder()
                        .message("Trades fetched successfully")
                        .data(data)
                        .build());
    }
}
