package org.maya.dto;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponseDTO {
    public Long id;
    public LocalDateTime orderDate;
    public String address;
    public List<OrderItemResponseDTO> items;
}