package zerowaste.backend.product.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.user.User;

import java.util.Optional;

public interface UserProductListRepository extends JpaRepository<UserProductList, Long> {
    Optional<UserProductList> findByShareCode(String shareCode);
    boolean existsByShareCode(String shareCode);
}
