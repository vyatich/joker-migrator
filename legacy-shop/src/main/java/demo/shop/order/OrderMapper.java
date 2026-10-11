package demo.shop.order;

import demo.shop.api.model.OrderDto;
import demo.shop.api.model.OrderLineDto;
import demo.shop.api.model.OrderStatusDto;

import java.time.ZoneOffset;
import java.util.stream.Collectors;

final class OrderMapper {
    private OrderMapper() {
    }

    static OrderDto toDto(Order order) {
        return new OrderDto()
            .id(order.getId())
            .customerId(order.getCustomerId())
            .status(OrderStatusDto.fromValue(order.getStatus().name()))
            .lines(order.getLines().stream().map(OrderMapper::toDto).collect(Collectors.toList()))
            .itemsTotal(order.getItemsTotal())
            .delivery(order.getDelivery())
            .total(order.getTotal())
            .placedAt(order.getPlacedAt().atOffset(ZoneOffset.UTC));
    }

    private static OrderLineDto toDto(OrderLine line) {
        return new OrderLineDto()
            .productId(line.getProductId())
            .productName(line.getProductName())
            .quantity(line.getQuantity())
            .amount(line.getAmount());
    }
}
