package zerowaste.backend.recipe.repos;
import org.springframework.data.jpa.repository.JpaRepository;
import zerowaste.backend.recipe.models.Recipe;

import java.util.List;

import java.util.Optional;

public interface RecipeRepository  extends JpaRepository<Recipe,Long> {

    Optional<List<Recipe>> findAllByDifficulty(int d);
    Optional<List<Recipe>> findAllByTimeBefore(int t);
    Optional<List<Recipe>> findAllByRecipeType(String r);
    Optional<List<Recipe>> findAllByNameContainingIgnoreCase(String n);
}
