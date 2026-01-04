package zerowaste.backend.product.controller.requests;

import jakarta.validation.constraints.NotNull;

public record DeleteProductRequest(
        @NotNull Long id
) {}