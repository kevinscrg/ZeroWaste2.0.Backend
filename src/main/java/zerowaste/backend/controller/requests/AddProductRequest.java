package zerowaste.backend.controller.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record AddProductRequest(
        String name,
        @JsonProperty("best_before") LocalDate bestBefore,
        @JsonProperty("consumption_days") Integer consumptionDays,
        LocalDate opened
) {}