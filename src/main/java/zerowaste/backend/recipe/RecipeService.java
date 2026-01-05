package zerowaste.backend.recipe;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.Preference;

import java.util.List;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    private final UserRecipeRepository userRecipeRepository;

    private final SimpMessagingTemplate template;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository, UserRecipeRepository userRecipeRepository, SimpMessagingTemplate template) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.template = template;
    }

    public record NotificationPayload(String email, List<String> Allergens, List<String> Preferences,
                                      int Difficulty, int Time, String Type, List<Long> LikedRecipes,
                                      List<Long> DislikedRecipes, List<String> ExpiringProducts){}
    public record NotificationRequest(NotificationPayload payload){}
    public List<Recipe> getRecipes(AppUserDetails me, int difficulty, int time, String type){
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        NotificationPayload n = new NotificationPayload(user.getEmail(), user.getAllergies().stream().map(Allergy::getName).toList(),
                user.getPreferences().stream().map(Preference::getName).toList(),
                difficulty, time, type,
                userRecipeRepository.getUserRecipeByUserAndRating(user, true).stream()
                        .map(UserRecipe::getRecipe).map(Recipe::getId).toList(),
                userRecipeRepository.getUserRecipeByUserAndRating(user, false).stream()
                        .map(UserRecipe::getRecipe).map(Recipe::getId).toList(),
                user.getUserProductList().getProducts().stream().filter(Product::isExpiringSoon).map(Product::getName).toList()
                );

        NotificationRequest not = new NotificationRequest(n);

        template.convertAndSend("/topic/python-requests", not);

        return recipeRepository.findAll();
    }

}
