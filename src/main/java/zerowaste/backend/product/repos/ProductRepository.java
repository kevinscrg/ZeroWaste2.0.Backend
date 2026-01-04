package zerowaste.backend.product.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import zerowaste.backend.product.models.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
}
