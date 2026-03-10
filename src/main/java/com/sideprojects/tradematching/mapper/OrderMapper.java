package com.sideprojects.tradematching.mapper;

import com.sideprojects.tradematching.dto.order.CreateOrderDto;
import com.sideprojects.tradematching.dto.order.OrderResponseDto;
import com.sideprojects.tradematching.dto.order.OrderbookLevelDto;
import com.sideprojects.tradematching.entity.Order;
import com.sideprojects.tradematching.entity.OrderStatusHistory;
import com.sideprojects.tradematching.entity.enums.OrderStatus;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = OrderStatusHistoryMapper.class, builder = @Builder(disableBuilder = true))
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "remaining", source = "dto.quantity")
    @Mapping(target = "status", expression = "java(OrderStatus.OPEN)")
    @Mapping(target = "validityDays", expression = "java(dto.getValidityDays() != null ? dto.getValidityDays() : 60)")
    Order toOrder(CreateOrderDto dto, Long userId);

    @Mapping(target = "price", expression = "java(order.getPrice() != null ? order.getPrice().toPlainString() : null)")
    @Mapping(target = "quantity", expression = "java(order.getQuantity() != null ? order.getQuantity().toPlainString() : null)")
    @Mapping(target = "remaining", expression = "java(order.getRemaining() != null ? order.getRemaining().toPlainString() : null)")
    @Mapping(target = "createdAt", expression = "java(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)")
    @Mapping(target = "updatedAt", expression = "java(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)")
    @Mapping(target = "statusHistory", source = "historyList")
    OrderResponseDto toOrderResponseDto(Order order, List<OrderStatusHistory> historyList);

    @Mapping(target = "price", expression = "java(order.getPrice() != null ? order.getPrice().toPlainString() : null)")
    @Mapping(target = "userName", expression = "java(order.getUser() != null ? order.getUser().getName() : null)")
    @Mapping(target = "createdAt", expression = "java(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)")
    OrderbookLevelDto toOrderbookLevelDto(Order order);
}
