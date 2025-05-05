package org.maya.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.maya.dto.*;
import org.maya.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrderService {

    @Inject
    Logger logger;

    public Response getAll() {
        try {
            List<Order> orders = Order.listAll();
            List<OrderResponseDTO> dtos = orders.stream()
                    .map(this::mapToDto) // Используем mapToDto
                    .collect(Collectors.toList());
            logger.info("Found " + dtos.size() + " orders");
            return Response.ok(dtos).build(); // Возвращаем DTO
        } catch (Exception e) {
            logger.error("Failed to fetch orders", e);
            return Response.serverError().entity("{\"error\": \"Failed to fetch orders\"}").build();
        }
    }

    public Response getById(Long id) {
        try {
            Order order = Order.findById(id);
            if (order == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok(mapToDto(order)).build();
        } catch (Exception e) {
            logger.error("Failed to fetch order: " + id, e);
            return Response.serverError().build();
        }
    }

    @Transactional
    public Response create(OrderRequest request) {
        try {
            Order order = new Order();
            order.address = request.address;

            for (OrderItemRequest itemReq : request.items) {
                Item item = Item.findById(itemReq.itemId);
                if (item == null) {
                    logger.error("Item not found: " + itemReq.itemId);
                    return Response.status(404)
                            .entity("{\"error\":\"Item not found: " + itemReq.itemId + "\"}")
                            .build();
                }

                // Проверка наличия достаточного количества товара
                if (itemReq.quantity > item.quantity) {
                    return Response.status(400)
                            .entity("{\"error\":\"Not enough stock for item: " + item.name + "\"}")
                            .build();
                }

                OrderItem orderItem = new OrderItem();
                orderItem.item = item;
                orderItem.quantity = itemReq.quantity;
                order.addOrderItem(orderItem);

                item.quantity -= itemReq.quantity;
            }

            order.persist();
            logger.info("Order created: " + order.id);
            return Response.status(Response.Status.CREATED).entity(mapToDto(order)).build();

        } catch (Exception e) {
            logger.error("Order creation failed", e);
            return Response.serverError().build();
        }
    }

    @Transactional
    public Response update(Long id, OrderRequest request) {
        try {
            Order order = Order.findById(id);
            if (order == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"Order not found\"}").build();
            }

            // 1. Собираем старые количества по ID товара
            Map<Long, Integer> oldItemQuantities = order.items.stream()
                    .filter(oi -> oi.item != null) // Пропускаем, если товар был удален
                    .collect(Collectors.toMap(oi -> oi.item.id, oi -> oi.quantity));

            // 2. Собираем новые по ID товара
            Map<Long, Integer> newItemQuantities = request.items.stream()
                    .collect(Collectors.toMap(req -> req.itemId, req -> req.quantity, Integer::sum)); // Суммируем, если один товар добавлен несколько раз

            // 3. Проверяем доступность и рассчитываем дельту для склада
            Map<Long, Integer> stockAdjustments = new HashMap<>(); // itemId -> delta (положительная - вернуть на склад, отрицательная - списать)

            for (OrderItem orderItem : order.items) {
                if (orderItem.item != null) {
                    long itemId = orderItem.item.id;
                    if (!newItemQuantities.containsKey(itemId)) {
                        stockAdjustments.put(itemId, oldItemQuantities.getOrDefault(itemId, 0));
                    }
                }
            }

            // Теперь проходим по новым товарам для проверки и расчета списания/корректировки
            for (OrderItemRequest itemReq : request.items) {
                Item item = Item.findById(itemReq.itemId);
                if (item == null) {
                    return Response.status(Response.Status.NOT_FOUND).entity("{\"error\":\"Item not found: " + itemReq.itemId + "\"}").build();
                }

                int oldQty = oldItemQuantities.getOrDefault(itemReq.itemId, 0);
                int newQty = itemReq.quantity;
                int delta = newQty - oldQty; // > 0: нужно больше, < 0: нужно меньше, = 0: без изменений

                // Сколько нужно *дополнительно* взять со склада
                int neededFromStock = Math.max(0, delta);

                // Проверяем, достаточно ли *текущего* остатка + того, что вернется от других позиций этого же товара (если он был)
                int alreadyReturned = stockAdjustments.getOrDefault(item.id, 0);
                if (item.quantity + alreadyReturned < neededFromStock) {
                    // Немедленно возвращаем транзакцию, если не хватает
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"Not enough stock for item: " + item.name + ". Required additional: " + neededFromStock
                                    + ", Available (current + returning): " + (item.quantity + alreadyReturned) + "\"}")
                            .build();
                }
                // Записываем итоговое изменение для этого товара
                // Если delta > 0, списываем (отрицательное значение). Если delta < 0, возвращаем (положительное).
                stockAdjustments.put(item.id, stockAdjustments.getOrDefault(item.id, 0) - delta);
            }


            // 4. Обновляем склад
            for (Map.Entry<Long, Integer> entry : stockAdjustments.entrySet()) {
                Item item = Item.findById(entry.getKey());
                if (item != null) {
                    item.quantity += entry.getValue();
                    if(item.quantity < 0) {
                        logger.errorf("Stock calculation error for item %d resulted in negative quantity: %d", item.id, item.quantity);
                        throw new IllegalStateException("Stock calculation error resulted in negative quantity for item " + item.id);
                    }
                } else {
                    logger.warnf("Item with id %d not found during stock adjustment for order %d update.", entry.getKey(), id);
                }
            }

            // 5. Очищаем старые элементы и добавляем новые
            order.items.clear(); // orphanRemoval=true позаботится об удалении старых OrderItem
            order.address = request.address; // Обновляем адрес
            for (OrderItemRequest itemReq : request.items) {
                Item item = Item.findById(itemReq.itemId); // Уже проверили, что он не null
                OrderItem orderItem = new OrderItem();
                orderItem.item = item;
                orderItem.quantity = itemReq.quantity;
                order.addOrderItem(orderItem); // Добавляем новый OrderItem
            }


            logger.info("Order updated: " + order.id);
            return Response.ok(mapToDto(order)).build(); // Возвращаем DTO

        } catch (Exception e) {
            logger.error("Failed to update order: " + id, e);
            return Response.serverError().entity("{\"error\": \"Internal server error during order update\", \"details\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @Transactional
    public Response delete(Long id) {
        try {
            Order order = Order.findById(id);
            if (order == null) {
                return Response.status(404).build();
            }

            // Возвращаем товары на склад
            for (OrderItem orderItem : order.items) {
                if (orderItem.item != null) {
                    orderItem.item.quantity += orderItem.quantity;
                }
            }

            order.delete();
            logger.info("Order deleted: " + id);
            return Response.noContent().build();

        } catch (Exception e) {
            logger.error("Failed to delete order: " + id, e);
            return Response.serverError().build();
        }
    }

    private OrderResponseDTO mapToDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.id = order.id;
        dto.orderDate = order.orderDate;
        dto.address = order.address;
        dto.items = order.items.stream()
                .map(oi -> {
                    OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
                    if (oi.item != null) {  // Проверка на null!
                        itemDto.itemId = oi.item.id;
                        itemDto.itemName = oi.item.name;
                        itemDto.price = oi.item.price;
                    } else {
                        itemDto.itemName = "[Товар удален]";
                        itemDto.price = 0.0;
                    }
                    itemDto.quantity = oi.quantity;
                    return itemDto;
                })
                .collect(Collectors.toList());
        return dto;
    }
}