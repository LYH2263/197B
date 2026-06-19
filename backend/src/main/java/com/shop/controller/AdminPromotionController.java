package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.PromotionCreateRequest;
import com.shop.dto.PromotionVO;
import com.shop.entity.Promotion;
import com.shop.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public Result<Promotion> create(@Valid @RequestBody PromotionCreateRequest req) {
        Promotion promotion = promotionService.create(req);
        return Result.ok(promotion);
    }

    @GetMapping
    public Result<List<PromotionVO>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(promotionService.listAll(status));
    }

    @GetMapping("/{id}")
    public Result<PromotionVO> getById(@PathVariable Long id) {
        Promotion p = promotionService.getById(id);
        if (p == null) {
            return Result.fail(404, "满减活动不存在");
        }
        PromotionVO vo = promotionService.listAll(null).stream()
                .filter(v -> v.getId().equals(id))
                .findFirst()
                .orElse(null);
        return Result.ok(vo);
    }

    @PutMapping("/{id}")
    public Result<Promotion> update(@PathVariable Long id, @Valid @RequestBody PromotionCreateRequest req) {
        Promotion promotion = promotionService.update(id, req);
        return Result.ok(promotion);
    }

    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        promotionService.updateStatus(id, status);
        return Result.ok();
    }

    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        promotionService.updateStatus(id, 1);
        return Result.ok();
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        promotionService.updateStatus(id, 0);
        return Result.ok();
    }
}
