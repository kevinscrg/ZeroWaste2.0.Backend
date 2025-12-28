package zerowaste.backend.product.models;


import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Entity
@Table(name="products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private LocalDate best_before;

    @Positive
    private int consumption_days;

    private boolean opened;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBest_before() {
        return best_before;
    }

    public void setBest_before(LocalDate best_before) {
        this.best_before = best_before;
    }

    public int getConsumption_days() {
        return consumption_days;
    }

    public void setConsumption_days(int consumption_days) {
        this.consumption_days = consumption_days;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }
}
