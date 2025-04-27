package org.maya.dto;

import jakarta.validation.constraints.*;

public class OrderItemRequest {
    @NotNull
    public Long itemId;

    @Min(1)
    public int quantity;
}