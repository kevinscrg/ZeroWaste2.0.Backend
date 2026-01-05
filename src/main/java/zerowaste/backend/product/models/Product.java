package zerowaste.backend.product.models;


import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

@Entity
@Table(name="products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private LocalDate best_before;

    @PositiveOrZero
    private Integer consumption_days;

    private LocalDate opened;


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

    public LocalDate getBestBefore() {
        return best_before;
    }

    public void setBestBefore(LocalDate best_before) {
        this.best_before = best_before;
    }

    public int getConsumptionDays() {
        return consumption_days;
    }

    public void setConsumptionDays(int consumption_days) {
        this.consumption_days = consumption_days;
    }

    public LocalDate getOpened() {
        return opened;
    }

    public void setOpened(LocalDate opened) {
        this.opened = opened;
    }

    public boolean isExpiringSoon(){

        LocalDate expDay = null;

        if(this.getBestBefore() != null) expDay = this.getBestBefore();

        if(this.getOpened() != null && this.getConsumptionDays() != 0){
            LocalDate openedExp = this.getOpened().plusDays(this.getConsumptionDays());

            expDay = (expDay == null || expDay.isAfter(openedExp))
                    ? openedExp
                    : expDay;


        }

        if(expDay == null){
            return false;
        }

        return expDay.isBefore(LocalDate.now().plusDays(4)) &&
                expDay.isAfter(LocalDate.now().minusDays(1));
    }
}
