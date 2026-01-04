package zerowaste.backend.product.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import zerowaste.backend.user.User;

import java.util.List;

public record UserProductListDto(
        long id,
        @JsonProperty("share_code") String shareCode,
        List<ProductDto> products
) {}
