package zerowaste.backend.notification;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import zerowaste.backend.email.EmailTemplateService;
import zerowaste.backend.email.MailService;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DailyPlanifierService {

    private final ThreadPoolTaskScheduler taskScheduler;
    private final UserRepository userRepository;
//    private final UserProductListRepository userProductListRepository;
    private final MailService mailService;
    private final EmailTemplateService emailTemplateService;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Value("${frontend.url}")
    private String frontendUrl;

    public DailyPlanifierService(ThreadPoolTaskScheduler taskScheduler,
                                 UserRepository userRepository, MailService mailService,
                                 EmailTemplateService emailTemplateService) {
        this.taskScheduler = taskScheduler;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.emailTemplateService = emailTemplateService;
    }

    @PostConstruct
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledTasks() {
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();

        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (user.getPreferred_notification_hour() != null) {
                scheduleUserTask(user);
            }
        }
    }

    public void updateUserNotification(User user) {
        ScheduledFuture<?> existing = scheduledTasks.get(user.getId());
        if (existing != null) {
            existing.cancel(false);
        }
        scheduleUserTask(user);
    }

    private void scheduleUserTask(User user) {
        String cronExp = String.format("0 %d %d * * *",
                user.getPreferred_notification_hour().getMinute(),
                user.getPreferred_notification_hour().getHour());

        ScheduledFuture<?> future = taskScheduler.schedule(() ->
                verifyAndSend(user.getId()), new CronTrigger(cronExp));

        if (future != null) {
            scheduledTasks.put(user.getId(), future);
            System.out.println("task scheduled for " + user.getEmail());
        }
    }

    protected void verifyAndSend(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            LocalDate expiringDay = LocalDate.now().plusDays(user.getNotification_day());

            List<Product> expiringProducts = user.getUserProductList().getProducts().stream()
                    .filter(p -> p.getBestBefore() != null && p.getBestBefore().equals(expiringDay))
                    .toList();

            if (!expiringProducts.isEmpty()) {
                sendExpiringProductsEmail(user.getEmail(), expiringProducts, user.getNotification_day());
            }
        });
    }

    private void sendExpiringProductsEmail(String email, List<Product> expiringProducts, int days) {
        String html = emailTemplateService.render("mail/expiringProducts-email",
                Map.of(
                        "products", expiringProducts,
                        "days", days,
                        "frontendUrl", frontendUrl
                )
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yy", Locale.ENGLISH);
        String formattedDate = LocalDate.now().format(formatter);

        mailService.sendHtmlEmail(email, "Expiring Products Alert " + formattedDate, html);
    }
}