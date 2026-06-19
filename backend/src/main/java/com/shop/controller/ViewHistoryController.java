package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.ViewHistoryMergeRequest;
import com.shop.dto.ViewHistoryVO;
import com.shop.service.ViewHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/view-history")
@RequiredArgsConstructor
public class ViewHistoryController {

    private final ViewHistoryService viewHistoryService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @PostMapping("/add")
    public Result<Void> add(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        Long productId = ((Number) body.get("productId")).longValue();
        viewHistoryService.addViewHistory(userId, productId);
        return Result.ok();
    }

    @PostMapping("/merge")
    public Result<Map<String, Object>> merge(Authentication auth, @RequestBody ViewHistoryMergeRequest req) {
        Long userId = requireUserId(auth);
        int count = viewHistoryService.mergeLocalHistory(userId, req.getItems());
        Map<String, Object> data = new HashMap<>();
        data.put("mergedCount", count);
        return Result.ok(data);
    }

    @GetMapping("/count")
    public Result<Map<String, Object>> getCount(Authentication auth) {
        Long userId = requireUserId(auth);
        int count = viewHistoryService.countByUserId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.ok(data);
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = requireUserId(auth);
        List<ViewHistoryVO> list = viewHistoryService.listByPage(userId, page, size);
        int total = viewHistoryService.countByUserId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    @GetMapping("/recent")
    public Result<List<ViewHistoryVO>> recent(
            Authentication auth,
            @RequestParam(defaultValue = "6") int limit
    ) {
        Long userId = requireUserId(auth);
        return Result.ok(viewHistoryService.listRecent(userId, limit));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(Authentication auth, @PathVariable Long id) {
        Long userId = requireUserId(auth);
        viewHistoryService.removeById(userId, id);
        return Result.ok();
    }

    @PostMapping("/batch-remove")
    public Result<Void> batchRemove(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = requireUserId(auth);
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) body.get("ids");
        viewHistoryService.removeBatchByIds(userId, ids);
        return Result.ok();
    }

    @DeleteMapping("/clear")
    public Result<Void> clear(Authentication auth) {
        Long userId = requireUserId(auth);
        viewHistoryService.clearAll(userId);
        return Result.ok();
    }
}
