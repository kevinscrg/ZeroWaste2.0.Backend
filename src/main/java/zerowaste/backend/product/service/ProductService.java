// src/main/java/com/example/products/ProductService.java
package zerowaste.backend.product.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zerowaste.backend.exception.classes.ConstraintException;
import zerowaste.backend.product.controller.requests.AddProductRequest;
import zerowaste.backend.product.controller.requests.UpdateProductRequest;
import zerowaste.backend.product.models.Product;
import zerowaste.backend.product.models.UserProductList;
import zerowaste.backend.product.repos.ProductRepository;
import zerowaste.backend.product.repos.UserProductListRepository;
import zerowaste.backend.security.AppUserDetails;
import zerowaste.backend.user.User;
import zerowaste.backend.user.UserRepository;
import zerowaste.backend.webSocket.ProductListWsEvent;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserProductListRepository userProductListRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ProductService(UserRepository userRepository, ProductRepository productRepository, UserProductListRepository userProductListRepository, ApplicationEventPublisher applicationEventPublisher ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.userProductListRepository = userProductListRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Product addProduct(AddProductRequest req, AppUserDetails me) {

        UserProductList list = getMyProductList(me);

        if (list == null) {
            throw new EntityNotFoundException("User has no product list");
        }

        if (req.opened() != null && req.opened().isAfter(LocalDate.now())){
            throw new ConstraintException("Opened date can`t be in the future!");
        }

        // 2) Create + save product
        Product p = new Product();
        p.setName(req.name());
        p.setBestBefore(req.bestBefore());
        p.setConsumptionDays(req.consumptionDays() == null ? 0 : req.consumptionDays());
        p.setOpened(req.opened());

        Product saved = productRepository.save(p);

        // 3) Attach to list (join table will be updated)
        list.getProducts().add(saved);


        userProductListRepository.save(list);

        applicationEventPublisher.publishEvent(new ProductListWsEvent( list.getShare_code(), "add_product", saved));

        return saved;
    }

    @Transactional
    public Product updateProduct(UpdateProductRequest req, AppUserDetails me) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        UserProductList list = user.getUserProductList();

        Product p = productRepository.findById(req.id())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + req.id()));

        if (req.opened() != null && req.opened().isAfter(LocalDate.now())){
            throw new ConstraintException("Opened date can`t be in the future!");
        }

        if (req.name() != null) p.setName(req.name());
        if (req.bestBefore() != null) p.setBestBefore(req.bestBefore());
        p.setOpened(req.opened());
        p.setConsumptionDays(req.consumptionDays() == null ? 0 : req.consumptionDays());

        Product updated = productRepository.save(p);

        applicationEventPublisher.publishEvent(new ProductListWsEvent( list.getShare_code(), "update_product", updated));

        return updated;
    }

    @Transactional
    public void deleteProduct(Long id, AppUserDetails me) {
        UserProductList list = getMyProductList(me);
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        //DELETE din user_product_lists_products
        list.getProducts().removeIf(prod -> prod.getId() == id);

        //sterge produsul complet din tabela products
        productRepository.delete(p);

        applicationEventPublisher.publishEvent(new ProductListWsEvent( list.getShare_code(), "delete_product", id));

    }

    @Transactional(readOnly = true)
    public UserProductList getMyProductList(AppUserDetails me) {
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();

        UserProductList list = user.getUserProductList();
        if (list == null) {
            throw new EntityNotFoundException("User has no product list");
        }

        // Force initialization while the persistence context is open
        list.getProducts().size();
        list.getCollaborators().size();

        return list;
    }

    public List<String> getCollaborators(AppUserDetails me){
        User user = userRepository.findById(me.getDomainUser().getId()).orElseThrow();
        return user.getUserProductList().getCollaborators().stream().map(User::getEmail).filter(e -> !e.equals(user.getEmail())).toList();

    }
}
