package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.SeckillOrderCreateRequest;
import com.shop.dto.SeckillSessionVO;
import com.shop.entity.OrderMain;
import com.shop.service.OrderService;
import com.shop.service.SeckillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final OrderService orderService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/sessions")
    public Result<List<SeckillSessionVO>> listSessions(Authentication auth) {
        Long userId = null;
        if (auth != null && auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        }
        return Result.ok(seckillService.listActiveSessions(userId));
    }

    @GetMapping("/sessions/{id}")
    public Result<SeckillSessionVO> getSessionDetail(@PathVariable Long id, Authentication auth) {
        Long userId = null;
        if (auth != null && auth.getPrincipal() instanceof Long) {
            userId = (Long) auth.getPrincipal();
        }
        SeckillSessionVO vo = seckillService.getDetailById(id, userId);
        if (vo == null) {
            return Result.fail(404, "秒杀场次不存在");
        }
        return Result.ok(vo);
    }

    @PostMapping("/sessions/{id}/token")
    public Result<Map<String, Object>> acquireToken(@PathVariable Long id, Authentication auth) {
        Long userId = requireUserId(auth);
        String token = seckillService.acquireToken(id, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("sessionId", id);
        data.put("expireMinutes", 10);
        return Result.ok(data);
    }

    @PostMapping("/order")
    public Result<OrderMain> createSeckillOrder(Authentication auth, @Valid @RequestBody SeckillOrderCreateRequest req) {
        Long userId = requireUserId(auth);
        OrderMain order = orderService.createSeckillOrder(userId, req);
        return Result.ok(order);
    }
}
