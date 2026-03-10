package com.shahbazsideprojects.tradematching.mapper;

import com.shahbazsideprojects.tradematching.dto.order.TradeResponseDto;
import com.shahbazsideprojects.tradematching.entity.Trade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TradeMapper {

    @Mapping(target = "price", expression = "java(trade.getPrice() != null ? trade.getPrice().toPlainString() : null)")
    @Mapping(target = "quantity", expression = "java(trade.getQuantity() != null ? trade.getQuantity().toPlainString() : null)")
    @Mapping(target = "createdAt", expression = "java(trade.getCreatedAt() != null ? trade.getCreatedAt().toString() : null)")
    TradeResponseDto toDto(Trade trade);
}
