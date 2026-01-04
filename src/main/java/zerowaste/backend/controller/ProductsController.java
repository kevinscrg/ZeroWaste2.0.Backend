package zerowaste.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zerowaste.backend.controller.requests.AddProductRequest;
import zerowaste.backend.controller.requests.DeleteProductRequest;
import zerowaste.backend.controller.requests.UpdateProductRequest;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.product.models.ProductDto;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.models.UserProductListDto;
import zerowaste.backend.service.ProductService;
import zerowaste.backend.webSocket.ProductWsNotifier;

import java.util.List;

@RestController
@RequestMapping("/user-product-list")
public class ProductsController {

    private final ProductService service;


    public ProductsController(ProductService service) {
        this.service = service;

    }

    @GetMapping("/")
    public ResponseEntity<?> getProductList() {
        System.out.println("Fetching productList");
        UserProductList list = service.getMyProductList();
        UserProductListDto dto = mapToDto(list);
        System.out.println("returning"+dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/")
    public ResponseEntity<Product> add(@RequestBody AddProductRequest req) {
        System.out.println("saving product "+req);
        Product saved = service.addProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/")
    public ResponseEntity<Product> update(@RequestBody UpdateProductRequest req) {
        Product updated = service.updateProduct(req);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/")
    public ResponseEntity<Void> delete(@RequestBody DeleteProductRequest req) {
        service.deleteProduct(req.id());

        return ResponseEntity.noContent().build(); // 204
    }

    private UserProductListDto mapToDto(UserProductList list) {
        List<ProductDto> productDtos = list.getProducts()
                .stream()
                .map(this::mapProductToDto)
                .toList();

        return new UserProductListDto(
                list.getId(),
                list.getShare_code(),
                productDtos
        );
    }

    private ProductDto mapProductToDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getBestBefore(),
                p.getOpened(),
                p.getConsumptionDays()
        );
    }
}
