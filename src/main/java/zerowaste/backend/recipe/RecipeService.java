package zerowaste.backend.recipe;

import jakarta.transaction.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.recipe.models.Recipe;
import zerowaste.backend.recipe.models.RecipeDto;
import zerowaste.backend.recipe.models.UserRecipe;
import zerowaste.backend.recipe.repos.RecipeRepository;
import zerowaste.backend.recipe.repos.UserRecipeRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.Preference;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final UserRecipeRepository userRecipeRepository;
    private final SimpMessagingTemplate template;
    private final CacheManager cacheManager;


    public record PythonPayload(List<Long> recipe_ids, String email){}
    public record PythonMessage(String type, PythonPayload payload){}
    public record WsMessage(String type){}

    public record NotificationPayload(String email, List<String> Allergens, List<String> Preferences,
                                      List<Integer> Difficulty, int Time, List<String> Type, List<Long> LikedRecipes,
                                      List<Long> DislikedRecipes, List<String> ExpiringProducts){}

    public record NotificationRequest(NotificationPayload payload){}

    public record RecipeFilter(Integer time, List<Integer> difficulty, String recipeType, Boolean favourites){}

    public record PageResponse<T>(
            long count,
            String next,
            List<T> results
    ) {}

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository,
                         UserRecipeRepository userRecipeRepository, SimpMessagingTemplate template,
                         CacheManager cacheManager) {

        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.userRecipeRepository = userRecipeRepository;
        this.template = template;
        this.cacheManager = cacheManager;
    }



    private List<Long> getRecipesFromCache(String email) {
        Cache cache = cacheManager.getCache("userRecipes");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(email);
            if (wrapper != null && wrapper.get() != null) {
                return cache.get(email, List.class);
            }
        }
        return null;
    }


    private void askAiModule(User user, List<Integer> difficulty, Integer time, List<String> type){

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
    }


    public PageResponse<RecipeDto> getRecipesPaged(AppUserDetails me, int limit, int offset) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();
        List<Long> cachedIds = getRecipesFromCache(user.getEmail());

        if (cachedIds == null || cachedIds.isEmpty()) {
            askAiModule(user, null, 0, null);
            return new PageResponse<>(0, null, List.of());
        }

        int total = cachedIds.size();
        int start = Math.min(offset, total);
        int end = Math.min(offset + limit, total);

        if (start >= total) {
            return new PageResponse<>(total, null, List.of());
        }

        List<Long> pageIds = cachedIds.subList(start, end);
        List<Recipe> recipes = recipeRepository.findAllById(pageIds);

        recipes.sort(Comparator.comparingInt(r -> pageIds.indexOf(r.getId())));

        Map<Long, UserRecipe> ratingsMap = userRecipeRepository.findByUserAndRecipeIn(user, recipes)
                .stream()
                .collect(Collectors.toMap(ur -> ur.getRecipe().getId(), ur -> ur));

        List<RecipeDto> responseList = recipes.stream()
                .map(recipe -> mapToRecipeDto(recipe, ratingsMap.get(recipe.getId())))
                .toList();

        String next = (end < total) ? String.format("?limit=%d&offset=%d", limit, end) : null;

        return new PageResponse<>(total, next, responseList);
    }


    public void refreshCachedRecipes(AppUserDetails me) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        Cache cache = cacheManager.getCache("userRecipes");
        if(cache != null) {
            cache.evict(user.getEmail());
            askAiModule(user, null, 0, null);
        }
    }


    @Transactional
    public void rateRecipe(AppUserDetails me, long recipeId, Boolean rate) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow();

        Optional<UserRecipe> existingRating = userRecipeRepository.findByUserAndRecipe(user, recipe);

        if (rate == null) {
            existingRating.ifPresent(userRecipeRepository::delete);
            return;
        }
        UserRecipe recipeRating = existingRating.orElse(new UserRecipe());

        recipeRating.setUser(user);
        recipeRating.setRecipe(recipe);
        recipeRating.setRating(rate);

        userRecipeRepository.save(recipeRating);
    }


    public PageResponse<RecipeDto> searchRecipes(AppUserDetails me, int limit, int offset, String search) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        if (search == null || search.isEmpty()) {
            throw new IllegalArgumentException("No query provided.");
        }

        final String searchLower = search.toLowerCase();
        List<Long> cachedIds = getRecipesFromCache(user.getEmail());

        if (cachedIds == null || cachedIds.isEmpty()) {
            return new PageResponse<>(0, null, List.of());
        }

        List<Recipe> filteredRecipes = recipeRepository.findAllById(cachedIds).stream()
                .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(searchLower))
                .sorted(Comparator.comparingInt(r -> cachedIds.indexOf(r.getId())))
                .toList();

        int total = filteredRecipes.size();

        if (offset >= total) {
            return new PageResponse<>(total, null, List.of());
        }

        int end = Math.min(offset + limit, total);
        List<Recipe> pageRecipes = filteredRecipes.subList(offset, end);

        Map<Long, UserRecipe> ratingsMap = userRecipeRepository.findByUserAndRecipeIn(user, pageRecipes)
                .stream()
                .collect(Collectors.toMap(ur -> ur.getRecipe().getId(), ur -> ur));

        List<RecipeDto> responseList = pageRecipes.stream()
                .map(recipe -> mapToRecipeDto(recipe, ratingsMap.get(recipe.getId())))
                .toList();

        String next = (end < total) ? String.format("?limit=%d&offset=%d&search=%s", limit, end, search) : null;

        return new PageResponse<>(total, next, responseList);
    }


    public PageResponse<RecipeDto> filterRecipes(AppUserDetails me, int limit, int offset, RecipeFilter recipeFilter) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        if (recipeFilter == null) {
            return new PageResponse<>(0, null, List.of());
        }

        List<Recipe> baseRecipes;

        if (recipeFilter.favourites() != null) {
            baseRecipes = userRecipeRepository.findByUserAndRating(user, recipeFilter.favourites())
                    .stream()
                    .map(UserRecipe::getRecipe)
                    .toList();
        } else {
            List<Long> cachedIds = getRecipesFromCache(user.getEmail());
            if (cachedIds == null || cachedIds.isEmpty()) {
//                askAiModule(user, recipeFilter.difficulty(), recipeFilter.time(), List.of(recipeFilter.recipeType()));
                return new PageResponse<>(0, null, List.of());
            }
            baseRecipes = recipeRepository.findAllById(cachedIds)
                    .stream()
                    .sorted(Comparator.comparingInt(r -> cachedIds.indexOf(r.getId()))).toList();

        }

        System.out.println(recipeFilter);

        List<Recipe> filteredRecipes = baseRecipes.stream()
                .filter(r -> recipeFilter.time() == null || r.getTime() <= recipeFilter.time())
                .filter(r -> recipeFilter.difficulty() == null || recipeFilter.difficulty().getFirst() == null
                        || recipeFilter.difficulty().isEmpty()
                        || recipeFilter.difficulty().contains(r.getDifficulty()))
                .filter(r -> recipeFilter.recipeType() == null || recipeFilter.recipeType().isEmpty()
                        || r.getRecipeType().equalsIgnoreCase(recipeFilter.recipeType()))
                .toList();

        int total = filteredRecipes.size();
        if (offset >= total) {
            return new PageResponse<>(total, null, List.of());
        }

        int end = Math.min(offset + limit, total);
        List<Recipe> pageRecipes = filteredRecipes.subList(offset, end);

        Map<Long, UserRecipe> ratingsMap = userRecipeRepository.findByUserAndRecipeIn(user, pageRecipes)
                .stream()
                .collect(Collectors.toMap(ur -> ur.getRecipe().getId(), ur -> ur));

        List<RecipeDto> responseList = pageRecipes.stream()
                .map(recipe -> mapToRecipeDto(recipe, ratingsMap.get(recipe.getId())))
                .toList();

        String next = (end < total) ? String.format("?limit=%d&offset=%d", limit, end) : null;

        return new PageResponse<>(total, next, responseList);
    }


    private RecipeDto mapToRecipeDto(Recipe recipe, UserRecipe recipeRating) {
        RecipeDto recipeDto = new RecipeDto();
        recipeDto.setId(recipe.getId());
        recipeDto.setDifficulty(recipe.getDifficulty());
        recipeDto.setImage(recipe.getImage());
        recipeDto.setLink(recipe.getLink());
        recipeDto.setName(recipe.getName());
        recipeDto.setTime(recipe.getTime());
        recipeDto.setRecipeType(recipe.getRecipeType());
        if(recipeRating != null) {
        recipeDto.setRating(recipeRating.isRating());
        }
        else{
            recipeDto.setRating(null);
        }


        return recipeDto;
    }

    public void handlePythonResponse(PythonMessage request) {

        if(!Objects.equals(request.type, "run")){
            System.out.println("something went wrong");
            System.out.println(request);
            return;
        }

        System.out.println(request.payload());

        Cache cache = cacheManager.getCache("userRecipes");
        if (cache != null && request.payload() != null) {
            cache.put(request.payload().email(), request.payload().recipe_ids());

            String cleanEmail = request.payload().email().replace("@","").replace(".","");
            template.convertAndSend("/topic/notifications/" + cleanEmail, new WsMessage("recipe"));

            System.out.println(cleanEmail);
        }

    }
}
