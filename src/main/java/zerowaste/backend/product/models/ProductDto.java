package zerowaste.backend.product.models;


import java.time.LocalDate;

public record ProductDto(
        long id,
        String name,
        LocalDate bestBefore,
        LocalDate opened,
        Integer consumptionDays
) {}
