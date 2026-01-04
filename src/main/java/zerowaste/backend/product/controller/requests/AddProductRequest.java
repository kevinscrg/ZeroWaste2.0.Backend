package zerowaste.backend.product.controller.requests;


import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record AddProductRequest(
        String name,
        LocalDate bestBefore,
        @PositiveOrZero Integer consumptionDays,
        LocalDate opened
) {}