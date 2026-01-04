package zerowaste.backend.controller.requests;

import jakarta.validation.constraints.NotNull;

public record DeleteProductRequest(
        @NotNull Long id
) {}