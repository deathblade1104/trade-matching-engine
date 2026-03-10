package com.shahbazsideprojects.tradematching.mapper;

import com.shahbazsideprojects.tradematching.dto.order.OrderStatusHistoryResponseDto;
import com.shahbazsideprojects.tradematching.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderStatusHistoryMapper {

    @Mapping(target = "createdAt", expression = "java(history.getCreatedAt() != null ? history.getCreatedAt().toString() : null)")
    OrderStatusHistoryResponseDto toDto(OrderStatusHistory history);
}
