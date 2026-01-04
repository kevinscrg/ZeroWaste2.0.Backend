package zerowaste.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.repos.ProductRepository;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
class SeedService{
    @Transactional
    void seedInternal(UserRepository userRepo,
                      ProductRepository productRepo,
                      UserProductListRepository listRepo,
                      PasswordEncoder passwordEncoder) {

        final String email = "demo@zerowaste.local";
        final String shareCode = "ABC123";

        // 1) Ensure list exists
        UserProductList list = listRepo.findByShareCode(shareCode)
                .orElseGet(() -> {
                    UserProductList l = new UserProductList();
                    l.setShare_code(shareCode);
                    return listRepo.save(l);
                });

        // 2) Ensure products exist (by name)
        Product milk = productRepo.findByName("Milk")
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setName("Milk");
                    p.setConsumptionDays(3);
                    p.setOpened(LocalDate.of(2026, 1, 5));
                    p.setBestBefore(LocalDate.of(2026, 1, 10));
                    return productRepo.save(p);
                });

        Product yogurt = productRepo.findByName("Yogurt")
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setName("Yogurt");
                    p.setConsumptionDays(5);
                    p.setOpened(LocalDate.of(2026, 1, 5));
                    p.setBestBefore(LocalDate.of(2026, 1, 8));
                    return productRepo.save(p);
                });

        // 3) Ensure join-table links exist (no duplicate add)
        if (!list.getProducts().contains(milk)) list.getProducts().add(milk);
        if (!list.getProducts().contains(yogurt)) list.getProducts().add(yogurt);
        listRepo.save(list);

        // 4) Ensure user exists
        User user = userRepo.findByEmail(email)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setDark_mode(false);
                    u.setVerified(true);
                    u.setNotification_day(1);
                    u.setPreferred_notification_hour(LocalTime.of(9, 0));
                    u.setPassword(passwordEncoder.encode("demo1234"));
                    return u;
                });

        // Ensure user is linked to list (idempotent)
        if (user.getUserProductList() == null) {
            user.setUserProductList(list);
            userRepo.save(user);
        }
    }
}


@Configuration
public class SeedData {

    @Bean
    CommandLineRunner seed(UserRepository userRepo,
                           ProductRepository productRepo,
                           UserProductListRepository listRepo,
                           PasswordEncoder passwordEncoder,
                           SeedService seedService) {
        return args -> seedService.seedInternal(userRepo, productRepo, listRepo, passwordEncoder);
    }

}
