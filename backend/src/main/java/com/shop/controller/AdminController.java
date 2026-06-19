package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.AfterSaleDetailVO;
import com.shop.dto.AfterSaleReviewRequest;
import com.shop.dto.AfterSaleShipRequest;
import com.shop.dto.ReviewVO;
import com.shop.dto.UserVO;
import com.shop.entity.AfterSale;
import com.shop.entity.User;
import com.shop.mapper.UserMapper;
import com.shop.service.AdminService;
import com.shop.service.AfterSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 管理员接口（需 ROLE_ADMIN）
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AfterSaleService afterSaleService;
    private final UserMapper userMapper;

    private String getAdminName(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return "管理员";
        Long id = (Long) auth.getPrincipal();
        User u = userMapper.selectById(id);
        if (u == null) return "管理员";
        return u.getNickname() != null ? u.getNickname() : u.getUsername();
    }

    private Long getAdminId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return null;
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/users")
    public Result<List<UserVO>> listUsers() {
        return Result.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        String role = body.get("role") != null ? String.valueOf(body.get("role")) : null;
        adminService.updateUser(id, status, role);
        return Result.ok();
    }

    @GetMapping("/reviews")
    public Result<List<ReviewVO>> listReviews() {
        return Result.ok(adminService.listAllReviews());
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return Result.ok();
    }

    @GetMapping("/after-sale")
    public Result<List<AfterSale>> listAfterSale(@RequestParam(required = false) Integer status) {
        return Result.ok(afterSaleService.listAll(status));
    }

    @GetMapping("/after-sale/{id}")
    public Result<AfterSaleDetailVO> afterSaleDetail(@PathVariable Long id) {
        AfterSaleDetailVO vo = afterSaleService.getAdminDetail(id);
        if (vo == null) return Result.fail(404, "售后不存在");
        return Result.ok(vo);
    }

    @PostMapping("/after-sale/{id}/approve")
    public Result<Void> approveAfterSale(Authentication auth, @PathVariable Long id, @RequestBody(required = false) AfterSaleReviewRequest req) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        BigDecimal refundAmount = req != null ? req.getRefundAmount() : null;
        afterSaleService.approve(id, refundAmount, adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/after-sale/{id}/reject")
    public Result<Void> rejectAfterSale(Authentication auth, @PathVariable Long id, @RequestBody AfterSaleReviewRequest req) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        afterSaleService.reject(id, req.getRejectReason(), adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/after-sale/{id}/warehouse-confirm")
    public Result<Void> warehouseConfirm(Authentication auth, @PathVariable Long id) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        afterSaleService.warehouseConfirm(id, adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/after-sale/{id}/ship-exchange")
    public Result<Void> shipExchange(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        String company = body.get("company") != null ? String.valueOf(body.get("company")) : null;
        String trackingNo = body.get("trackingNo") != null ? String.valueOf(body.get("trackingNo")) : null;
        BigDecimal priceDiff = null;
        if (body.get("priceDiff") != null) {
            priceDiff = new BigDecimal(String.valueOf(body.get("priceDiff")));
        }
        afterSaleService.adminShipExchange(id, company, trackingNo, priceDiff, adminId, adminName);
        return Result.ok();
    }
}

