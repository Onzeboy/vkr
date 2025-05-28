package com.nzby.homeshop.POJO.Enum;

public enum ProductCategory {
    DAIRY("Молочные продукты"),
    MEAT("Мясо и птица"),
    FRUITS("Фрукты"),
    VEGETABLES("Овощи"),
    BEVERAGES("Напитки"),
    SNACKS("Закуски"),
    FROZEN("Мороженое и замороженные продукты"),
    BREAD("Хлеб и выпечка"),
    CONFECTIONERY("Кондитерские изделия"),
    SEAFOOD("Морепродукты"),
    SPICES("Приправы и специи"),
    CANNED("Консервация"),
    NON_FOOD("Не продовольственные товары"),
    DIET("Диетические продукты"),
    ORGANIC("Органические продукты"),
    GLUTEN_FREE("Безглютеновые продукты"),
    VEGAN("Веганские продукты"),
    ECO_FRIENDLY("Экологически чистые продукты"),
    BABY_FOOD("Детское питание"),
    HERBS("Травы и растения"),
    HEALTH_SUPPLEMENTS("БАДы и витамины"),
    PET_FOOD("Корма для домашних животных"),
    GOURMET("Гурманские продукты"),
    DELI("Деликатесы"),
    LOCAL("Продукты местного производства"),
    FAST_FOOD("Фаст-фуд");

    private final String displayName;

    ProductCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}