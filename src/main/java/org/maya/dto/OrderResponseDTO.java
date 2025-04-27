package org.maya.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {
    public Long id;
    public LocalDateTime orderDate;
    public String address;
    public List<OrderItemResponseDTO> items;
}