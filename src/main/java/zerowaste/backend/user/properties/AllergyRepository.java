package zerowaste.backend.user.properties;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllergyRepository extends JpaRepository<Allergy,Long> {
    Optional<Allergy> findByName(String name);
    List<Allergy> findByNameInIgnoreCase(List<String> names);
}
