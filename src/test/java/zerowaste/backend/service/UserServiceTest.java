package zerowaste.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import zerowaste.backend.notification.DailyPlanifierService;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.user.UserService;
import zerowaste.backend.user.properties.Allergy;
import zerowaste.backend.user.properties.AllergyRepository;
import zerowaste.backend.user.properties.Preference;
import zerowaste.backend.user.properties.PreferenceRepository;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProductListRepository userProductListRepository;

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private PreferenceRepository preferenceRepository;

    @Mock
    private DailyPlanifierService dailyPlanifierService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private AppUserDetails appUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setDark_mode(false);
        testUser.setNotification_day(1); // Default

        testUser.setPreferences(new ArrayList<>());
        testUser.setAllergies(new ArrayList<>());

        appUserDetails = new AppUserDetails(testUser);
    }

    @Test
    void testGetAuthUser_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getAuthUser(appUserDetails);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetAuthUser_NotFound_ThrowsAccessDenied() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> userService.getAuthUser(appUserDetails));
    }

    @Test
    void testUpdatePreferences() {
        // Arrange
        List<String> prefNames = List.of("Vegan", "Keto");
        UserService.PreferenceRequest request = new UserService.PreferenceRequest(prefNames);

        Preference p1 = new Preference(); p1.setName("Vegan");
        Preference p2 = new Preference(); p2.setName("Keto");
        List<Preference> foundPreferences = List.of(p1, p2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(preferenceRepository.findByNameInIgnoreCase(prefNames)).thenReturn(foundPreferences);

        // Act
        List<Preference> result = userService.updatePreferences(appUserDetails, request);

        // Assert
        assertEquals(2, result.size());
        assertEquals(2, testUser.getPreferences().size());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateAllergies() {
        // Arrange
        List<String> allergyNames = List.of("Peanuts");
        UserService.AllergyRequest request = new UserService.AllergyRequest(allergyNames);

        Allergy a1 = new Allergy(); a1.setName("Peanuts");
        List<Allergy> foundAllergies = List.of(a1);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(allergyRepository.findByNameInIgnoreCase(allergyNames)).thenReturn(foundAllergies);

        // Act
        List<Allergy> result = userService.updateAllergies(appUserDetails, request);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Peanuts", testUser.getAllergies().getFirst().getName());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateDarkMode() {
        // Arrange
        testUser.setDark_mode(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.updateDarkMode(appUserDetails);

        // Assert
        assertTrue(testUser.isDark_mode());
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateNotificationDay() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.updateNotificationDay(appUserDetails, 5);

        // Assert
        assertEquals(5, testUser.getNotification_day());
        verify(userRepository).save(testUser);
        verify(dailyPlanifierService).updateUserNotification(testUser);
    }

    @Test
    void testUpdateNotificationHour_Success() {
        // Arrange
        String validTime = "14:30";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        userService.updateNotificationHour(appUserDetails, validTime);

        // Assert
        assertEquals(LocalTime.of(14, 30), testUser.getPreferred_notification_hour());
        verify(userRepository).save(testUser);
        verify(dailyPlanifierService).updateUserNotification(testUser);
    }

    @Test
    void testUpdateNotificationHour_InvalidFormat_ThrowsException() {
        // Arrange
        String invalidTime = "25:99"; // Invalid hour/minute
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        DateTimeException exception = assertThrows(DateTimeException.class, () ->
                userService.updateNotificationHour(appUserDetails, invalidTime)
        );
        assertEquals("Invalid Hour Format, should be: HH:MM", exception.getMessage());

        verify(userRepository, never()).save(any());
        verify(dailyPlanifierService, never()).updateUserNotification(any());
    }

    @Test
    void testChangeList_Success_LeaveOldJoinNew() {
        // Arrange
        String oldCode = "OLD123";
        String newCode = "NEW456";

        UserProductList oldList = new UserProductList();
        oldList.setShare_code(oldCode);
        oldList.setCollaborators(new ArrayList<>(List.of(testUser)));

        UserProductList newList = new UserProductList();
        newList.setShare_code(newCode);
        newList.setCollaborators(new ArrayList<>());

        testUser.setUserProductList(oldList);

        UserService.changeListRequest request = new UserService.changeListRequest(newCode);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProductListRepository.findByShareCode(newCode)).thenReturn(Optional.of(newList));

        // Act
        userService.changeList(appUserDetails, request);

        // Assert
        // 1. Verify user removed from old list
        assertFalse(oldList.getCollaborators().contains(testUser));
        assertTrue(oldList.getCollaborators().isEmpty());
        verify(userProductListRepository).delete(oldList); // Deleted because empty

        // 2. Verify user added to new list
        assertEquals(newList, testUser.getUserProductList());
        assertTrue(newList.getCollaborators().contains(testUser));

        verify(userRepository).save(testUser);
    }

    @Test
    void testChangeList_Success_NoOldList() {
        // Arrange
        String newCode = "NEW456";
        testUser.setUserProductList(null); // No previous list

        UserProductList newList = new UserProductList();
        newList.setShare_code(newCode);
        newList.setCollaborators(new ArrayList<>());

        UserService.changeListRequest request = new UserService.changeListRequest(newCode);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProductListRepository.findByShareCode(newCode)).thenReturn(Optional.of(newList));

        // Act
        userService.changeList(appUserDetails, request);

        // Assert
        assertEquals(newList, testUser.getUserProductList());
        assertTrue(newList.getCollaborators().contains(testUser));
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangeList_AlreadyMember_ThrowsException() {
        // Arrange
        String code = "SAME123";
        UserProductList currentList = new UserProductList();
        currentList.setShare_code(code);
        testUser.setUserProductList(currentList);

        UserService.changeListRequest request = new UserService.changeListRequest(code);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changeList(appUserDetails, request)
        );
        assertEquals("You are already member of this list", ex.getMessage());
    }

    @Test
    void testChangeList_NewListNotFound_ThrowsException() {
        // Arrange
        String newCode = "MISSING";
        testUser.setUserProductList(null);
        UserService.changeListRequest request = new UserService.changeListRequest(newCode);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userProductListRepository.findByShareCode(newCode)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changeList(appUserDetails, request)
        );
        assertEquals("Product list with provided share code does not exist.", ex.getMessage());
    }
}
