package com.nzby.homeshop.Utils;

import com.nzby.homeshop.POJO.ProductImage;
import java.util.Comparator;

public class ProductImageComparator implements Comparator<ProductImage> {
    public static final ProductImageComparator INSTANCE = new ProductImageComparator();

    @Override
    public int compare(ProductImage a, ProductImage b) {
        // Сначала сортируем по primary (true идет первым)
        int primaryCompare = Boolean.compare(b.isPrimary(), a.isPrimary());
        if (primaryCompare != 0) {
            return primaryCompare;
        }
        // Затем по id или другому полю, если нужно
        return Long.compare(a.getId(), b.getId());
    }
}