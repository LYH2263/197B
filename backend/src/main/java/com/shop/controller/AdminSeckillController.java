package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.SeckillSessionCreateRequest;
import com.shop.entity.SeckillSession;
import com.shop.service.SeckillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/seckill")
@RequiredArgsConstructor
public class AdminSeckillController {

    private final SeckillService seckillService;

    @GetMapping("/sessions")
    public Result<List<SeckillSession>> list() {
        return Result.ok(seckillService.listAll());
    }

    @GetMapping("/sessions/{id}")
    public Result<SeckillSession> getById(@PathVariable Long id) {
        SeckillSession s = seckillService.getDetailById(id, null);
        if (s == null) {
            return Result.fail(404, "场次不存在");
        }
        return Result.ok(s);
    }

    @PostMapping("/sessions")
    public Result<SeckillSession> create(@Valid @RequestBody SeckillSessionCreateRequest req) {
        return Result.ok(seckillService.create(req));
    }

    @PutMapping("/sessions/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SeckillSessionCreateRequest req) {
        seckillService.update(id, req);
        return Result.ok();
    }

    @PatchMapping("/sessions/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        seckillService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/sessions/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        seckillService.delete(id);
        return Result.ok();
    }
}
