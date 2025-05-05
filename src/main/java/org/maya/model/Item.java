package org.maya.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
public class Item extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String name;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public double price;

    @OneToMany(mappedBy = "item")
    @JsonManagedReference("item-orderItems")
    public List<OrderItem> orderItems = new ArrayList<>();
}