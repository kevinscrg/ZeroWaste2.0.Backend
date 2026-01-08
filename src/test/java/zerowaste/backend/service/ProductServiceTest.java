package zerowaste.backend.product.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserProductListRepository userProductListRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<ProductListWsEvent> eventCaptor;

    private User testUser;
    private AppUserDetails appUserDetails;
    private UserProductList testList;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testList = new UserProductList();
        testList.setShare_code("SHARE123");
        testList.setProducts(new ArrayList<>());
        testList.setCollaborators(new ArrayList<>());
        testUser.setUserProductList(testList);

        appUserDetails = new AppUserDetails(testUser);

        testProduct = new Product();
        testProduct.setId(100L);
        testProduct.setName("Milk");
        testProduct.setBestBefore(LocalDate.now().plusDays(10));
        // Both consumptionDays and opened are Dates
        testProduct.setConsumptionDays(5);
        testProduct.setOpened(LocalDate.now());
    }

    @Test
    void testAddProduct_Success() {
        // Arrange
        LocalDate bestBefore = LocalDate.now().plusDays(10);
        LocalDate consumptionDate = LocalDate.now().plusDays(5);
        LocalDate openedDate = LocalDate.now();

        AddProductRequest request = new AddProductRequest("Milk", bestBefore, 5, openedDate);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        // Act
        Product result = productService.addProduct(request, appUserDetails);

        // Assert
        assertNotNull(result);
        assertEquals("Milk", result.getName());
        assertEquals(consumptionDate, result.getConsumptionDays());
        assertEquals(openedDate, result.getOpened());

        verify(userProductListRepository).save(testList);
        verify(applicationEventPublisher).publishEvent(any(ProductListWsEvent.class));
    }

    @Test
    void testAddProduct_FutureOpenedDate_ThrowsException() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(1);
        AddProductRequest request = new AddProductRequest("Milk", null, null, futureDate);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        ConstraintException exception = assertThrows(ConstraintException.class, () ->
                productService.addProduct(request, appUserDetails)
        );
        assertEquals("Opened date can`t be in the future!", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void testUpdateProduct_Success() {
        // Arrange
        int newConsumptionDays = 7; // MATCH THIS VALUE


        UpdateProductRequest request = new UpdateProductRequest(100L, "Soy Milk", null,  null,newConsumptionDays);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Product result = productService.updateProduct(request, appUserDetails);

        // Assert
        assertEquals("Soy Milk", result.getName());
        assertEquals(newConsumptionDays, result.getConsumptionDays()); // 7 == 7
    }


    @Test
    void testDeleteProduct_Success() {
        // Arrange
        testList.getProducts().add(testProduct);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct(100L, appUserDetails);

        // Assert
        assertTrue(testList.getProducts().isEmpty());
        verify(productRepository).delete(testProduct);
        verify(applicationEventPublisher).publishEvent(any(ProductListWsEvent.class));
    }

    @Test
    void testGetMyProductList_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        UserProductList result = productService.getMyProductList(appUserDetails);
        assertNotNull(result);
        assertEquals("SHARE123", result.getShare_code());
    }

    @Test
    void testGetCollaborators() {
        User collaborator = new User();
        collaborator.setEmail("friend@example.com");
        testList.getCollaborators().add(testUser);
        testList.getCollaborators().add(collaborator);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        List<String> emails = productService.getCollaborators(appUserDetails);

        assertEquals(1, emails.size());
        assertEquals("friend@example.com", emails.getFirst());
    }
}
