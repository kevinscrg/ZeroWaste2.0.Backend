package zerowaste.backend.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;
import zerowaste.backend.email.EmailTemplateService;
import zerowaste.backend.email.MailService;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @InjectMocks
    private DailyPlanifierService dailyPlanifierService;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    private ArgumentCaptor<CronTrigger> cronCaptor;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dailyPlanifierService, "frontendUrl", "http://localhost:3000");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNotification_day(2);
        testUser.setPreferred_notification_hour(LocalTime.of(10, 30));

        testProduct = new Product();
        testProduct.setName("Milk");
        testProduct.setBestBefore(LocalDate.now().plusDays(2));

        UserProductList list = new UserProductList();
        list.setProducts(new ArrayList<>(List.of(testProduct)));
        testUser.setUserProductList(list);
    }

    @Test
    void testScheduledTasks_Init_SchedulesForValidUsers() {
        // Arrange
        User userNoTime = new User();
        userNoTime.setId(2L);
        userNoTime.setPreferred_notification_hour(null);

        when(userRepository.findAll()).thenReturn(List.of(testUser, userNoTime));
        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        // Act
        dailyPlanifierService.scheduledTasks();

        // Assert
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), cronCaptor.capture());

        // Verify Cron 0 30 10 * * * (10:30 AM)
        CronTrigger trigger = cronCaptor.getValue();
        assertEquals("0 30 10 * * *", trigger.getExpression());
    }

    @Test
    void testUpdateUserNotification_CancelsOldAndSchedulesNew() {
        // Arrange - existing task
        Map<Long, ScheduledFuture<?>> internalMap =
                (Map<Long, ScheduledFuture<?>>) ReflectionTestUtils.getField(dailyPlanifierService, "scheduledTasks");
        internalMap.put(testUser.getId(), scheduledFuture);

        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));

        // Act
        dailyPlanifierService.updateUserNotification(testUser);

        // Assert
        verify(scheduledFuture).cancel(false); //Old task canceled
        verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class)); //New task
    }

    @Test
    void testVerifyAndSend_SendsEmailWhenProductExpires() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(emailTemplateService.render(anyString(), anyMap())).thenReturn("<html>Body</html>");

        // Act
        dailyPlanifierService.verifyAndSend(1L);

        // Assert
        verify(emailTemplateService).render(eq("mail/expiringProducts-email"), anyMap());
        verify(mailService).sendHtmlEmail(eq("test@example.com"), contains("Expiring Products Alert"), eq("<html>Body</html>"));
    }

    @Test
    void testVerifyAndSend_NoEmailWhenNoProductExpires() {
        // Arrange
        testProduct.setBestBefore(LocalDate.now().plusDays(5)); // Expires in 5 days (Mismatch)
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        dailyPlanifierService.verifyAndSend(1L);

        // Assert
        verifyNoInteractions(mailService, emailTemplateService);
    }

    @Test
    void testScheduleUserTask_RunnableExecution() {
        // Arrange
        doReturn(scheduledFuture).when(taskScheduler).schedule(runnableCaptor.capture(), any(CronTrigger.class));


        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(emailTemplateService.render(anyString(), anyMap())).thenReturn("html");

        // Act
        dailyPlanifierService.updateUserNotification(testUser);
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Assert
        // If 'verifyAndSend(1L)' then trigger the repository and mail service
        verify(userRepository).findById(1L);
        verify(mailService).sendHtmlEmail(anyString(), anyString(), anyString());
    }
}
