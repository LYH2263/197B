package com.shop.mapper;

import com.shop.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper {

    List<Product> selectByCondition(@Param("categoryId") Long categoryId,
                                    @Param("keyword") String keyword,
                                    @Param("status") Integer status);

    Product selectById(@Param("id") Long id);

    int updateStock(@Param("id") Long id, @Param("quantity") int quantity);

    List<Product> selectRecommendForPromotion(@Param("categoryId") Long categoryId,
                                              @Param("maxPrice") BigDecimal maxPrice,
                                              @Param("excludeProductIds") List<Long> excludeProductIds,
                                              @Param("status") Integer status,
                                              @Param("limit") int limit);
}
