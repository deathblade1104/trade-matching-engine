package com.sideprojects.tradematching.mapper;

import com.sideprojects.tradematching.dto.order.OrderStatusHistoryResponseDto;
import com.sideprojects.tradematching.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderStatusHistoryMapper {

    @Mapping(target = "createdAt", expression = "java(history.getCreatedAt() != null ? history.getCreatedAt().toString() : null)")
    OrderStatusHistoryResponseDto toDto(OrderStatusHistory history);
}
