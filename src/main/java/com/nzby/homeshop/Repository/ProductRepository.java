package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Category;
import com.nzby.homeshop.POJO.Enum.ProductStatus;
import com.nzby.homeshop.POJO.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category")
    List<Product> findAll();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("search") String search);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.status = :status")
    List<Product> findByStatus(@Param("status") ProductStatus status);

    @Query(value = "SELECT p.* FROM products p LEFT JOIN categories c ON p.category_id = c.id ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Product> findRandomProducts(@Param("limit") int limit);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.images WHERE p.id = :id")
    Product findByIdWithCategoryAndImages(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.images WHERE p.id = :id")
    Product refreshWithCategoryAndImages(@Param("id") Long id);

    @Query(value = "SELECT COALESCE(AVG(rating), 0.0) as avg_rating, COUNT(*) as rating_count " +
            "FROM reviews WHERE product_id = :productId",
            nativeQuery = true)
    Object[] getAverageRatingAndCount(@Param("productId") Long productId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) AND p.category = :category")
    Page<Product> findByNameContainingIgnoreCaseAndCategory(@Param("search") String search, @Param("category") Category category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> findByNameContainingIgnoreCase(@Param("search") String search, Pageable pageable);

    Page<Product> findByCategory(Category category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
            "(:search IS NULL OR LOWER(p.name) LIKE %:search%) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchProducts(@Param("search") String search, @Param("categoryId") Long categoryId, Pageable pageable);
}