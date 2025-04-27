package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem extends PanacheEntity {
    @ManyToOne
    public Item item;

    @ManyToOne
    public Order order;

    public int quantity;
}