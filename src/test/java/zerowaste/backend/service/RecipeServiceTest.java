package zerowaste.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.recipe.RecipeService;
import zerowaste.backend.recipe.models.Recipe;
import zerowaste.backend.recipe.models.RecipeDto;
import zerowaste.backend.recipe.models.UserRecipe;
import zerowaste.backend.recipe.repos.RecipeRepository;
import zerowaste.backend.recipe.repos.UserRecipeRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(value = MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRecipeRepository userRecipeRepository;

    @Mock
    private SimpMessagingTemplate template;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private RecipeService recipeService;

    private User testUser;
    private Recipe testRecipe;
    private AppUserDetails appUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setAllergies(new ArrayList<>());
        testUser.setPreferences(new ArrayList<>());
        UserProductList productList = new UserProductList();
        productList.setProducts(new ArrayList<>());
        testUser.setUserProductList(productList);
        testRecipe = new Recipe();
        testRecipe.setId(1L);
        testRecipe.setName("Test Recipe");
        testRecipe.setTime(30);
        testRecipe.setDifficulty(2);
        testRecipe.setRecipeType("Dessert");
        appUserDetails = new AppUserDetails(testUser);
    }

    @Test
    void testGetRecipesPagedWithCachedRecipes() {
        // Arrange
        List<Long> cachedIds = List.of(1L, 2L, 3L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cacheManager.getCache("userRecipes")).thenReturn(cache);

        when(cache.get(testUser.getEmail())).thenReturn(new SimpleValueWrapper(cachedIds));
        when(cache.get(testUser.getEmail(), List.class)).thenReturn(cachedIds);

        when(recipeRepository.findAllById(anyList())).thenReturn(List.of(testRecipe));
        when(recipeRepository.findAllById(cachedIds))
                .thenReturn(new ArrayList<>(List.of(testRecipe, testRecipe, testRecipe)));

        // Act
        RecipeService.PageResponse<RecipeDto> result = recipeService.getRecipesPaged(appUserDetails, 10, 0);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.count()); // Should now match the cachedIds size
    }


    @Test
    void testGetRecipesPagedWithoutCache() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cacheManager.getCache("userRecipes")).thenReturn(cache);
        when(cache.get(testUser.getEmail())).thenReturn(null);

        // Act
        RecipeService.PageResponse<RecipeDto> result = recipeService.getRecipesPaged(appUserDetails, 10, 0);

        // Assert
        assertEquals(0, result.count());
        assertTrue(result.results().isEmpty());
        verify(template).convertAndSend(eq("/topic/python-requests"), any(RecipeService.NotificationRequest.class));
    }


    @Test
    void testRateRecipeNewRating() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(userRecipeRepository.findByUserAndRecipe(testUser, testRecipe)).thenReturn(Optional.empty());

        // Act
        recipeService.rateRecipe(appUserDetails, 1L, true);

        // Assert
        verify(userRecipeRepository).save(any(UserRecipe.class));
    }

    @Test
    void testRateRecipeUpdateExisting() {
        // Arrange
        UserRecipe existing = new UserRecipe();
        existing.setUser(testUser);
        existing.setRecipe(testRecipe);
        existing.setRating(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(userRecipeRepository.findByUserAndRecipe(testUser, testRecipe)).thenReturn(Optional.of(existing));

        // Act
        recipeService.rateRecipe(appUserDetails, 1L, true);

        // Assert
        verify(userRecipeRepository).save(argThat(ur -> ur.isRating()));
    }


    @Test
    void testRateRecipeRemoveRating() {
        // Arrange
        UserRecipe existing = new UserRecipe();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(userRecipeRepository.findByUserAndRecipe(testUser, testRecipe)).thenReturn(Optional.of(existing));

        // Act
        recipeService.rateRecipe(appUserDetails, 1L, null);

        // Assert
        verify(userRecipeRepository).delete(existing);
        verify(userRecipeRepository, never()).save(any());
    }


    @Test
    void testSearchRecipesWithValidQuery() {
        // Arrange
        List<Long> cachedIds = List.of(1L);
        testRecipe.setName("Chocolate Cake");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cacheManager.getCache("userRecipes")).thenReturn(cache);

        // 1. Mock wrapper existence
        when(cache.get(testUser.getEmail())).thenReturn(new SimpleValueWrapper(cachedIds));

        // 2. MISSING LINE: Mock the specific typed retrieval
        when(cache.get(eq(testUser.getEmail()), eq(List.class))).thenReturn(cachedIds);

        when(recipeRepository.findAllById(cachedIds)).thenReturn(List.of(testRecipe));
        when(userRecipeRepository.findByUserAndRecipeIn(any(), anyList())).thenReturn(List.of());

        // Act
        RecipeService.PageResponse<RecipeDto> result = recipeService.searchRecipes(appUserDetails, 10, 0, "chocolate");

        // Assert
        assertEquals(1, result.count());
        assertEquals(1, result.results().size());
    }

    @Test
    void testSearchRecipesWithNullQuery() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> recipeService.searchRecipes(appUserDetails, 10, 0, null));
    }

    @Test
    void testFilterRecipesByFavourites() {
        // Arrange
        UserRecipe userRecipe = new UserRecipe();
        userRecipe.setRecipe(testRecipe);
        userRecipe.setRating(true);

        RecipeService.RecipeFilter filter = new RecipeService.RecipeFilter(null, null, null, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRecipeRepository.findByUserAndRating(testUser, true)).thenReturn(List.of(userRecipe));
        when(userRecipeRepository.findByUserAndRecipeIn(any(), anyList())).thenReturn(List.of(userRecipe));

        // Act
        RecipeService.PageResponse<RecipeDto> result = recipeService.filterRecipes(appUserDetails, 10, 0, filter);

        // Assert
        assertEquals(1, result.count());
    }


    @Test
    void testFilterRecipesByTimeAndDifficulty() {
        // Arrange
        List<Long> cachedIds = List.of(1L, 2L);
        testRecipe.setTime(30);
        testRecipe.setDifficulty(2);

        RecipeService.RecipeFilter filter = new RecipeService.RecipeFilter(45, List.of(2), null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cacheManager.getCache("userRecipes")).thenReturn(cache);

        when(cache.get(testUser.getEmail())).thenReturn(new SimpleValueWrapper(cachedIds));

        when(cache.get(eq(testUser.getEmail()), eq(List.class))).thenReturn(cachedIds);

        // Ensure findAllById returns a MUTABLE list (just in case)
        when(recipeRepository.findAllById(cachedIds))
                .thenReturn(new ArrayList<>(List.of(testRecipe)));

        when(userRecipeRepository.findByUserAndRecipeIn(any(), anyList())).thenReturn(List.of());

        // Act
        RecipeService.PageResponse<RecipeDto> result = recipeService.filterRecipes(appUserDetails, 10, 0, filter);

        // Assert
        assertEquals(1, result.count());
    }

    @Test
    void testHandlePythonResponseSuccess() {
        // Arrange
        List<Long> recipeIds = List.of(1L, 2L, 3L);
        RecipeService.PythonPayload payload = new RecipeService.PythonPayload(recipeIds, "test@example.com");
        RecipeService.PythonMessage message = new RecipeService.PythonMessage("run", payload);

        when(cacheManager.getCache("userRecipes")).thenReturn(cache);

        // Act
        recipeService.handlePythonResponse(message);

        // Assert
        verify(cache).put("test@example.com", recipeIds);
        verify(template).convertAndSend(eq("/topic/notifications/testexamplecom"), any(RecipeService.WsMessage.class));
    }


    @Test
    void testGetRecipesPagedOffsetBeyondTotal() {

        List<Long> cachedIds = List.of(1L, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cacheManager.getCache("userRecipes")).thenReturn(cache);

        when(cache.get(testUser.getEmail())).thenReturn(new SimpleValueWrapper(cachedIds));

        when(cache.get(eq(testUser.getEmail()), eq(List.class))).thenReturn(cachedIds);

        RecipeService.PageResponse<RecipeDto> result = recipeService.getRecipesPaged(appUserDetails, 10, 100);

        assertEquals(2, result.count()); // Now correctly returns total count (2)
        assertTrue(result.results().isEmpty()); // But empty results because of offset
        assertNull(result.next());
    }
}
