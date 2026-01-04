package zerowaste.backend.product.controller.requests;


import java.time.LocalDate;

public record AddProductRequest(
        String name,
        LocalDate bestBefore,
        Integer consumptionDays,
        LocalDate opened
) {}