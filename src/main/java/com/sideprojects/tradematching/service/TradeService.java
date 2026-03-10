package com.sideprojects.tradematching.service;

import com.sideprojects.tradematching.dto.PagedResponse;
import com.sideprojects.tradematching.dto.order.TradeResponseDto;
import com.sideprojects.tradematching.entity.Trade;
import com.sideprojects.tradematching.mapper.TradeMapper;
import com.sideprojects.tradematching.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final TradeMapper tradeMapper;

    public PagedResponse<TradeResponseDto> getTradesByUserId(Long userId, int page, int limit) {
        PageRequest pr = PageRequest.of(Math.max(0, page - 1), limit);
        var pageResult = tradeRepository.findByUserIdOrderByCreatedAtDesc(userId, pr);
        List<TradeResponseDto> items = pageResult.getContent().stream()
                .map(tradeMapper::toDto)
                .collect(Collectors.toList());
        return PagedResponse.<TradeResponseDto>builder()
                .total(pageResult.getTotalElements())
                .page(page)
                .limit(limit)
                .items(items)
                .build();
    }
}
