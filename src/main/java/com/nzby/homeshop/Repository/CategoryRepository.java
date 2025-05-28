package com.nzby.homeshop.Repository;

import com.nzby.homeshop.POJO.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    List<Category> findByParentIsNull();

    @Query("SELECT c FROM Category c WHERE c.parent.id = :parentId")
    List<Category> findByParentCategoryId(@Param("parentId") Long parentId);

    List<Category> findByParent(Category parent);
    boolean existsByParentId(Long parentId);
}