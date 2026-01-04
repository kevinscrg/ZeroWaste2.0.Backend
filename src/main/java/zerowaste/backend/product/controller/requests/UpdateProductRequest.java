package zerowaste.backend.product.controller.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record UpdateProductRequest(
        @NotNull Long id,
        String name,
        LocalDate bestBefore,
        LocalDate opened,
        @Positive Integer consumptionDays
) {}