package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.FavoriteBatchRequest;
import com.shop.dto.FavoriteCountVO;
import com.shop.dto.FavoriteGroupVO;
import com.shop.dto.FavoriteVO;
import com.shop.dto.PriceAlertVO;
import com.shop.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/count")
    public Result<FavoriteCountVO> getCount(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(favoriteService.getCount(userId));
    }

    @GetMapping("/check/{productId}")
    public Result<Boolean> isFavorited(Authentication auth, @PathVariable Long productId) {
        Long userId = auth != null && auth.getPrincipal() instanceof Long ? (Long) auth.getPrincipal() : null;
        return Result.ok(favoriteService.isFavorited(userId, productId));
    }

    @GetMapping
    public Result<List<FavoriteVO>> list(
            Authentication auth,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Boolean ungrouped
    ) {
        Long userId = requireUserId(auth);
        return Result.ok(favoriteService.listFavorites(userId, groupId, ungrouped));
    }

    @PostMapping("/add")
    public Result<Void> add(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        Long groupId = body.get("groupId") != null ? ((Number) body.get("groupId")).longValue() : null;
        favoriteService.addFavorite(userId, productId, groupId);
        return Result.ok();
    }

    @DeleteMapping("/{productId}")
    public Result<Void> remove(Authentication auth, @PathVariable Long productId) {
        Long userId = requireUserId(auth);
        favoriteService.removeFavorite(userId, productId);
        return Result.ok();
    }

    @PostMapping("/batch-remove")
    public Result<Void> batchRemove(Authentication auth, @RequestBody FavoriteBatchRequest req) {
        Long userId = requireUserId(auth);
        favoriteService.batchRemoveFavorite(userId, req.getProductIds());
        return Result.ok();
    }

    @PutMapping("/move")
    public Result<Void> moveToGroup(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        Long groupId = body.get("groupId") != null ? ((Number) body.get("groupId")).longValue() : null;
        favoriteService.moveToGroup(userId, productId, groupId);
        return Result.ok();
    }

    @PostMapping("/batch-move")
    public Result<Void> batchMoveToGroup(Authentication auth, @RequestBody FavoriteBatchRequest req) {
        Long userId = requireUserId(auth);
        favoriteService.batchMoveToGroup(userId, req);
        return Result.ok();
    }

    @GetMapping("/groups")
    public Result<List<FavoriteGroupVO>> listGroups(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(favoriteService.listGroups(userId));
    }

    @PostMapping("/groups")
    public Result<FavoriteGroupVO> createGroup(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        String name = (String) body.get("name");
        return Result.ok(favoriteService.createGroup(userId, name));
    }

    @PutMapping("/groups/{groupId}")
    public Result<Void> updateGroup(
            Authentication auth,
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> body
    ) {
        Long userId = requireUserId(auth);
        String name = (String) body.get("name");
        favoriteService.updateGroup(userId, groupId, name);
        return Result.ok();
    }

    @DeleteMapping("/groups/{groupId}")
    public Result<Void> deleteGroup(Authentication auth, @PathVariable Long groupId) {
        Long userId = requireUserId(auth);
        favoriteService.deleteGroup(userId, groupId);
        return Result.ok();
    }

    @PostMapping("/price-alert")
    public Result<Void> setPriceAlert(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        BigDecimal targetPrice = new BigDecimal(body.get("targetPrice").toString());
        favoriteService.setPriceAlert(userId, productId, targetPrice);
        return Result.ok();
    }

    @DeleteMapping("/price-alert/{productId}")
    public Result<Void> cancelPriceAlert(Authentication auth, @PathVariable Long productId) {
        Long userId = requireUserId(auth);
        favoriteService.cancelPriceAlert(userId, productId);
        return Result.ok();
    }

    @GetMapping("/price-alert/{productId}")
    public Result<PriceAlertVO> getActivePriceAlert(Authentication auth, @PathVariable Long productId) {
        Long userId = requireUserId(auth);
        return Result.ok(favoriteService.getActivePriceAlert(userId, productId));
    }

    @GetMapping("/price-alerts")
    public Result<List<PriceAlertVO>> listPriceAlerts(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(favoriteService.listPriceAlerts(userId));
    }

    @PostMapping("/scan-price-alerts")
    public Result<Void> manualScan() {
        favoriteService.scanPriceAlerts();
        return Result.ok();
    }

    @PostMapping("/trigger-alerts/{productId}")
    public Result<Integer> manualTriggerForProduct(@PathVariable Long productId) {
        return Result.ok(favoriteService.manualTriggerAlertsForProduct(productId));
    }
}
