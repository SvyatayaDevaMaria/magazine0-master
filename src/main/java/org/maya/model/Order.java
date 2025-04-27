package org.maya.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {

    @Column(name = "order_date", nullable = false)
    public LocalDateTime orderDate = LocalDateTime.now();

    @Column(length = 500)
    public String address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<OrderItem> items = new ArrayList<>();

    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.order = this;
    }
}