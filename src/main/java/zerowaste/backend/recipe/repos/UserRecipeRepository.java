package zerowaste.backend.recipe.repos;

import org.springframework.data.jpa.repository.JpaRepository;
import zerowaste.backend.recipe.models.Recipe;
import zerowaste.backend.recipe.models.UserRecipe;
import zerowaste.backend.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRecipeRepository extends JpaRepository<UserRecipe,Long> {

    List<UserRecipe> getUserRecipeByUserAndRating(User user, boolean rating);
    Optional<UserRecipe> findByUserAndRecipe(User user, Recipe recipe);
    List<UserRecipe> findByUserAndRecipeIn(User user, List<Recipe> recipes);
    List<UserRecipe> findByUserAndRating(User user, boolean rating);
}
