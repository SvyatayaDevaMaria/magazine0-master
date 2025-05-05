package org.maya.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem extends PanacheEntity {
    @ManyToOne
    @JsonBackReference("item-orderItems")
    @JoinColumn(name = "item_id")
    public Item item;

    @ManyToOne
    @JsonBackReference("order-orderItems")
    @JoinColumn(name = "order_id")
    public Order order;

    public int quantity;
}