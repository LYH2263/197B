package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.InvoiceCreateRequest;
import com.shop.dto.InvoiceUpdateRequest;
import com.shop.dto.InvoiceVO;
import com.shop.entity.InvoiceRequest;
import com.shop.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public Result<InvoiceRequest> create(Authentication auth, @Valid @RequestBody InvoiceCreateRequest req) {
        Long userId = requireUserId(auth);
        InvoiceRequest invoice = invoiceService.create(userId, req);
        return Result.ok(invoice);
    }

    @PutMapping("/{id}")
    public Result<InvoiceRequest> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody InvoiceUpdateRequest req) {
        Long userId = requireUserId(auth);
        InvoiceRequest invoice = invoiceService.update(id, userId, req);
        return Result.ok(invoice);
    }

    @GetMapping
    public Result<List<InvoiceVO>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(invoiceService.convertToVOList(invoiceService.listByUserId(userId)));
    }

    @GetMapping("/{id}")
    public Result<InvoiceVO> getDetail(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        InvoiceVO vo = invoiceService.getDetail(id, userId);
        if (vo == null) {
            return Result.fail(404, "发票申请不存在");
        }
        return Result.ok(vo);
    }

    @GetMapping("/order/{orderId}")
    public Result<InvoiceRequest> getByOrderId(Authentication auth, @PathVariable Long orderId) {
        Long userId = requireUserId(auth);
        InvoiceRequest invoice = invoiceService.getByOrderId(orderId, userId);
        return Result.ok(invoice);
    }
}
