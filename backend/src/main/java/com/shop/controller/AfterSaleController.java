package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.AfterSaleApplyRequest;
import com.shop.dto.AfterSaleDetailVO;
import com.shop.dto.AfterSaleShipRequest;
import com.shop.entity.AfterSale;
import com.shop.entity.AfterSaleSupplement;
import com.shop.service.AfterSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端售后接口
 */
@RestController
@RequestMapping("/after-sale")
@RequiredArgsConstructor
public class AfterSaleController {

    private final AfterSaleService afterSaleService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping
    public Result<AfterSale> apply(Authentication auth, @RequestBody AfterSaleApplyRequest req) {
        Long userId = requireUserId(auth);
        return Result.ok(afterSaleService.apply(userId, req));
    }

    @GetMapping
    public Result<List<AfterSale>> list(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(afterSaleService.listByUser(userId));
    }

    @GetMapping("/{id}")
    public Result<AfterSaleDetailVO> detail(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        AfterSaleDetailVO vo = afterSaleService.getDetail(id, userId);
        if (vo == null) return Result.fail(404, "售后不存在");
        return Result.ok(vo);
    }

    @PostMapping("/{id}/ship-return")
    public Result<Void> shipReturn(Authentication auth, @PathVariable Long id, @RequestBody AfterSaleShipRequest req) {
        Long userId = requireUserId(auth);
        afterSaleService.userShipReturn(id, req.getCompany(), req.getTrackingNo(), userId);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        afterSaleService.cancel(id, userId);
        return Result.ok();
    }

    @PostMapping("/supplement/{id}/pay")
    public Result<Void> paySupplement(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        afterSaleService.paySupplement(id, userId);
        return Result.ok();
    }

    @GetMapping("/supplements")
    public Result<List<AfterSaleSupplement>> listSupplements(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(afterSaleService.listSupplementsByUser(userId));
    }
}
