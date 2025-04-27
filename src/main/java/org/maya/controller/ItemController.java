package org.maya.controller;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.maya.model.Item;
import org.maya.model.Order; // Импортируем Order
import org.maya.model.OrderItem; // Импортируем OrderItem

import java.util.List;
import java.util.Set; // Используем Set для уникальных ID заказов
import java.util.stream.Collectors; // Для сбора ID

@Path("/api/items")
@Produces(MediaType.APPLICATION_JSON)
public class ItemController {

    @GET
    public List<Item> getAll() {
        List<Item> items = Item.listAll();
        System.out.println("Отправляемые данные (товары):" + items);
        return items;
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON) // Добавляем к методу
    public Response create(Item item) {
        // Валидация входных данных
        if (item == null || item.name == null || item.name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Название товара обязательно\"}")
                    .build();
        }
        if (item.quantity < 0) { // Количество не может быть отрицательным
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Количество товара не может быть отрицательным\"}")
                    .build();
        }
        if (item.price < 0) { // Цена не может быть отрицательной
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Цена товара не может быть отрицательной\"}")
                    .build();
        }


        // Проверка уникальности имени (без учета регистра для надежности)
        if (Item.count("LOWER(name) = LOWER(?1)", item.name.trim()) > 0) {
            return Response.status(Response.Status.CONFLICT) // 409 Conflict - более подходящий статус
                    .entity("{\"error\":\"Товар с таким названием уже существует\"}")
                    .build();
        }

        item.name = item.name.trim();
        item.id = null;

        try {
            item.persist();
            return Response.status(Response.Status.CREATED).entity(item).build();
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении товара: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при сохранении товара\"}")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, Item updatedItem) {
        if (updatedItem == null || updatedItem.name == null || updatedItem.name.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Название товара обязательно\"}")
                    .build();
        }
        if (updatedItem.quantity < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Количество товара не может быть отрицательным\"}")
                    .build();
        }
        if (updatedItem.price < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Цена товара не может быть отрицательной\"}")
                    .build();
        }

        Item item = Item.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Товар с ID " + id + " не найден\"}")
                    .build();
        }

        // Проверка уникальности имени при изменении (если имя отличается от старого)
        String newNameTrimmed = updatedItem.name.trim();
        if (!item.name.equalsIgnoreCase(newNameTrimmed)) { // Сравниваем без учета регистра
            if (Item.count("LOWER(name) = LOWER(?1) AND id != ?2", newNameTrimmed, id) > 0) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("{\"error\":\"Другой товар с названием '" + newNameTrimmed + "' уже существует\"}")
                        .build();
            }
            item.name = newNameTrimmed;
        }


        // Обновляем остальные поля
        item.quantity = updatedItem.quantity;
        item.price = updatedItem.price;

        try {
            return Response.ok(item).build();
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении товара: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при обновлении товара\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        Item item = Item.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Товар с ID " + id + " не найден\"}")
                    .build();
        }

        try {
            List<OrderItem> relatedOrderItems = OrderItem.list("item", item);

            Set<Long> affectedOrderIds = relatedOrderItems.stream()
                    .map(orderItem -> orderItem.order.id)
                    .collect(Collectors.toSet());

            OrderItem.delete("item", item);
            OrderItem.flush();


            item.delete();

            for (Long orderId : affectedOrderIds) {
                Order order = Order.findById(orderId);
                if (order != null) {
                    long remainingItemsCount = OrderItem.count("order", order);
                    if (remainingItemsCount == 0) {
                        System.out.println("Удаляем пустой заказ ID: " + orderId);
                        order.delete();
                    }
                }
            }

            return Response.noContent().build();

        } catch (Exception e) {
            System.err.println("Ошибка при удалении товара и связанных данных: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Внутренняя ошибка сервера при удалении товара\"}")
                    .build();
        }
    }
}