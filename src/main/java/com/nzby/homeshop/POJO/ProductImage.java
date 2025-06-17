package com.nzby.homeshop.POJO;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_images_product_id", columnList = "product_id")
})
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "position")
    private Integer position;

    public Boolean isPrimary() {
        return isPrimary;
    }

    public Boolean getPrimary() {
        return isPrimary;
    }

    public void setPrimary(Boolean primary) {
        isPrimary = primary;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

}