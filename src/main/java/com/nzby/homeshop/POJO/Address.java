package com.nzby.homeshop.POJO;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "addresses",
indexes = {@Index( name = "idx_address_id", columnList = "id"),
       @Index(name = "idx_address_user", columnList = "user_id")
})
@Data
public class Address {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Связь с пользователем

    @Column(name = "street_address")
    private String streetAddress;

    @Column(name = "city")
    private String city;


    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    public void setId(Long id) {
        this.id = id;
    }

}
