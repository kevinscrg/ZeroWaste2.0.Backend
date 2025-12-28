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
    @Column(unique = true, length = 6)
    private String shareCode;

    @OneToMany
    List<Product> products =  new ArrayList<>();

    @OneToMany(mappedBy = "userProductList")
    private List<User> collaborators = new ArrayList<>();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
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
