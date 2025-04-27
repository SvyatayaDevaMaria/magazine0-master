package org.maya.dto;

import jakarta.validation.Valid; // Для валидации вложенных объектов
import jakarta.validation.constraints.*;
import java.util.List;

public class OrderRequest {

    @Size(min = 1, message = "Заказ должен содержать хотя бы один товар")
    @Valid // Включаем валидацию для элементов списка (OrderItemRequest)
    public List<OrderItemRequest> items;

    @NotBlank(message = "Адрес доставки не может быть пустым") // Добавляем валидацию адреса
    @Size(max = 500, message = "Адрес слишком длинный")
    public String address; // Добавляем поле адреса в запрос
}
