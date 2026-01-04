package zerowaste.backend.controller.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record UpdateProductRequest(
        @NotNull Long id,
        String name,
        @JsonProperty("best_before") LocalDate bestBefore,
        LocalDate opened,
        @JsonProperty("consumption_days") @Positive Integer consumptionDays
) {}