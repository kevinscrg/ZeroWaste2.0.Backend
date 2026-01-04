package zerowaste.backend.product.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zerowaste.backend.product.controller.requests.AddProductRequest;
import zerowaste.backend.product.controller.requests.DeleteProductRequest;
import zerowaste.backend.product.controller.requests.UpdateProductRequest;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.product.models.ProductDto;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.models.UserProductListDto;
import zerowaste.backend.product.service.ProductService;
import zerowaste.backend.security.AppUserDetails;

import java.util.List;

@RestController
@RequestMapping("/user-product-list")
public class ProductsController {

    private final ProductService service;


    public ProductsController(ProductService service) {
        this.service = service;

    }

    @GetMapping("/")
    public ResponseEntity<?> getProductList(@AuthenticationPrincipal AppUserDetails me) {
        System.out.println("Fetching productList");
        UserProductList list = service.getMyProductList(me);
        UserProductListDto dto = mapToDto(list);
        System.out.println("returning"+dto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/")
    public ResponseEntity<Product> add(@RequestBody AddProductRequest req, @AuthenticationPrincipal AppUserDetails me) {
        System.out.println("saving product "+req);
        Product saved = service.addProduct(req, me);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/")
    public ResponseEntity<Product> update(@RequestBody UpdateProductRequest req, @AuthenticationPrincipal AppUserDetails me) {
        Product updated = service.updateProduct(req, me);
        return ResponseEntity.ok(updated);
    }


    @DeleteMapping("/")
    public ResponseEntity<Void> delete(@RequestBody DeleteProductRequest req, @AuthenticationPrincipal AppUserDetails me) {
        service.deleteProduct(req.id(), me);

        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("/collaborators")
    public ResponseEntity<?> getCollaborators(@AuthenticationPrincipal AppUserDetails me) {
        return ResponseEntity.ok(service.getCollaborators(me));
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
