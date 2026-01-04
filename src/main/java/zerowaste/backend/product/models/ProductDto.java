package zerowaste.backend.product.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record ProductDto(
        long id,
        String name,
        @JsonProperty("best_before") LocalDate bestBefore,
        LocalDate opened,
        @JsonProperty("consumption_days") Integer consumptionDays
) {}
