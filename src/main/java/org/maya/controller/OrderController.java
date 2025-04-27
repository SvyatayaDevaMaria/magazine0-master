package org.maya.controller;

import io.quarkus.panache.common.Sort;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid; // Импорт для валидации DTO
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.dto.OrderItemRequest;
import org.maya.dto.OrderRequest;
import org.maya.dto.OrderResponseDTO;
import org.maya.dto.OrderItemResponseDTO;
import org.maya.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderController {


    @GET
    public List<OrderResponseDTO> getAllOrders() {
        List<Order> orders = Order.listAll(Sort.by("id"));
        return orders.stream().map(this::mapOrderToDto).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Заказ с ID " + id + " не найден\"}")
                    .build();
        }
        return Response.ok(mapOrderToDto(order)).build();
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid OrderRequest request) {

        Order order = new Order();
        order.address = request.address;

        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Товар с ID " + itemReq.itemId + " не найден\"}")
                        .build();
            }
            if (itemReq.quantity > item.quantity) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Недостаточно товара '" + item.name + "' на складе (запрошено: " + itemReq.quantity + ", в наличии: " + item.quantity + ")\"}")
                        .build();
            }
        }

        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);

            OrderItem orderItem = new OrderItem();
            orderItem.item = item;
            orderItem.quantity = itemReq.quantity;
            order.addOrderItem(orderItem);

            item.quantity -= itemReq.quantity;
        }

        try {
            order.persist();
            return Response.status(Response.Status.CREATED).entity(mapOrderToDto(order)).build();
        } catch (Exception e) {
            System.err.println("Ошибка при создании заказа: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при создании заказа\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, @Valid OrderRequest request) {

        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Заказ с ID " + id + " не найден\"}")
                    .build();
        }

        for (OrderItem oldOrderItem : order.items) {
            if (oldOrderItem.item != null) {
                oldOrderItem.item.quantity += oldOrderItem.quantity;
            }
        }
        order.items.clear();
        Order.flush();


        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Товар с ID " + itemReq.itemId + " не найден для обновления заказа\"}")
                        .build();
            }
            if (itemReq.quantity > item.quantity) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Недостаточно товара '" + item.name + "' на складе для обновления (запрошено: " + itemReq.quantity + ", в наличии: " + item.quantity + ")\"}")
                        .build();
            }
        }

        order.address = request.address;
        for (OrderItemRequest itemReq : request.items) {
            Item item = Item.findById(itemReq.itemId);

            OrderItem newOrderItem = new OrderItem();
            newOrderItem.item = item;
            newOrderItem.quantity = itemReq.quantity;
            order.addOrderItem(newOrderItem);

            item.quantity -= itemReq.quantity;
        }

        try {
            return Response.ok(mapOrderToDto(order)).build();
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении заказа: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при обновлении заказа\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Заказ с ID " + id + " не найден\"}")
                    .build();
        }

        try {
            for (OrderItem orderItem : order.items) {
                if (orderItem.item != null) {
                    orderItem.item.quantity += orderItem.quantity;
                }
            }
            order.delete();
            return Response.noContent().build();
        } catch (Exception e) {
            System.err.println("Ошибка при удалении заказа: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при удалении заказа\"}")
                    .build();
        }
    }

    private OrderResponseDTO mapOrderToDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.id = order.id;
        dto.orderDate = order.orderDate;
        dto.address = order.address;
        dto.items = order.items.stream()
                .map(oi -> {
                    OrderItemResponseDTO itemDto = new OrderItemResponseDTO();
                    itemDto.quantity = oi.quantity;
                    if (oi.item != null) {
                        itemDto.itemId = oi.item.id;
                        itemDto.itemName = oi.item.name;
                        itemDto.price = oi.item.price;
                    } else {
                        itemDto.itemName = "[Товар удален]";
                    }
                    return itemDto;
                })
                .collect(Collectors.toList());
        return dto;
    }
}