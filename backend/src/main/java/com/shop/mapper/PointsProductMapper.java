package com.shop.mapper;

import com.shop.entity.PointsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PointsProductMapper {

    PointsProduct selectById(@Param("id") Long id);

    List<PointsProduct> selectOnShelf();

    List<PointsProduct> selectAll();

    int insert(PointsProduct pointsProduct);

    int updateById(PointsProduct pointsProduct);

    int updateStock(@Param("id") Long id, @Param("quantity") int quantity);
}
