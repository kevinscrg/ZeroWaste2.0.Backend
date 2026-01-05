package zerowaste.backend.recipe;

import org.springframework.data.jpa.repository.JpaRepository;
import zerowaste.backend.user.User;

import java.util.List;

public interface UserRecipeRepository extends JpaRepository<UserRecipe,Long> {

    public List<UserRecipe> getUserRecipeByUserAndRating(User user, boolean rating);
}
