package zerowaste.backend.user.properties;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreferenceRepository extends JpaRepository<Preference,Long> {
    Optional<Preference> findByName(String name);
    List<Preference> findByNameInIgnoreCase(List<String> names);
}
