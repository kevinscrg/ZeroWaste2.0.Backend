package zerowaste.backend.recipe;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import zerowaste.backend.recipe.models.Recipe;
import zerowaste.backend.recipe.models.RecipeDto;
import zerowaste.backend.security.AppUserDetails;

import java.util.List;

@Controller
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;


    public record RateRecipeRequest(long recipe_id, Boolean rating){}

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @MessageMapping("/python-response")
    public void handlePythonResponse(@Payload RecipeService.PythonMessage request) {
        recipeService.handlePythonResponse(request);
    }


    @GetMapping("/")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getRecipes(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal AppUserDetails me){

        RecipeService.PageResponse<RecipeDto> result = recipeService.getRecipesPaged(me, limit, offset);

        if(result == null){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/refresh-recipes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> refreshRecipes(@AuthenticationPrincipal AppUserDetails me){
        recipeService.refreshCachedRecipes(me);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rate-recipe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rateRecipe(@AuthenticationPrincipal AppUserDetails me, @RequestBody RateRecipeRequest request){
        recipeService.rateRecipe(me, request.recipe_id, request.rating);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search-recipes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchRecipes(@AuthenticationPrincipal AppUserDetails me,
                                           @RequestParam(defaultValue = "10") int limit,
                                           @RequestParam(defaultValue = "0") int offset,
                                           @RequestParam String search){

        return ResponseEntity.ok(recipeService.searchRecipes(me, limit, offset, search));
    }

    @PostMapping("/filter-recipes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> filterRecipes(@AuthenticationPrincipal AppUserDetails me,
                                           @RequestParam(defaultValue = "10") int limit,
                                           @RequestParam(defaultValue = "0") int offset,
                                           @RequestBody RecipeService.RecipeFilter  recipeFilter){

        System.out.println(recipeFilter);

        return ResponseEntity.ok(recipeService.filterRecipes(me, limit, offset, recipeFilter));
    }

}
