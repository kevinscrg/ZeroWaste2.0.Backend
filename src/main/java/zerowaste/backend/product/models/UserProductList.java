package zerowaste.backend.product.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import zerowaste.backend.user.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="userProductLists")
public class UserProductList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "share_code", unique = true, length = 6)
    private String shareCode;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_product_lists_products",
            joinColumns = @JoinColumn(name = "user_product_list_id"),
            inverseJoinColumns = @JoinColumn(name = "products_id")
    )
    List<Product> products =  new ArrayList<>();

    @OneToMany(mappedBy = "userProductList")
    private List<User> collaborators = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getShare_code() {
        return shareCode;
    }

    public void setShare_code(String shareCode) {
        this.shareCode = shareCode;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<User> getCollaborators() {
        return collaborators;
    }

    public void setCollaborators(List<User> collaborators) {
        this.collaborators = collaborators;
    }
}
