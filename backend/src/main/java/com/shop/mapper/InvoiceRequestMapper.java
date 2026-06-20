package com.shop.mapper;

import com.shop.entity.InvoiceRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InvoiceRequestMapper {
    int insert(InvoiceRequest invoice);
    InvoiceRequest selectById(@Param("id") Long id);
    InvoiceRequest selectByOrderId(@Param("orderId") Long orderId);
    List<InvoiceRequest> selectByUserId(@Param("userId") Long userId);
    List<InvoiceRequest> selectAll(@Param("status") Integer status);
    List<InvoiceRequest> selectByIds(@Param("ids") List<Long> ids);
    int update(InvoiceRequest invoice);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int approve(@Param("id") Long id, @Param("invoiceNumber") String invoiceNumber,
                @Param("adminId") Long adminId, @Param("adminName") String adminName,
                @Param("reviewedAt") LocalDateTime reviewedAt);
    int reject(@Param("id") Long id, @Param("rejectReason") String rejectReason,
               @Param("adminId") Long adminId, @Param("adminName") String adminName,
               @Param("reviewedAt") LocalDateTime reviewedAt);
    int updateInvoiceNumber(@Param("id") Long id, @Param("invoiceNumber") String invoiceNumber);
}
