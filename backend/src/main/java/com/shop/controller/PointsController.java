package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.PointsCalculateResult;
import com.shop.dto.PointsLogPage;
import com.shop.dto.UserLevelVO;
import com.shop.service.PointsLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsLevelService pointsLevelService;

    private Long requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new IllegalStateException("未登录");
        }
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(Authentication auth) {
        Long userId = requireUserId(auth);
        Map<String, Object> data = new HashMap<>();
        data.put("points", pointsLevelService.getUserPoints(userId));
        data.put("level", pointsLevelService.getUserLevelInfo(userId));
        return Result.ok(data);
    }

    @GetMapping("/level")
    public Result<UserLevelVO> level(Authentication auth) {
        Long userId = requireUserId(auth);
        return Result.ok(pointsLevelService.getUserLevelInfo(userId));
    }

    @GetMapping("/logs")
    public Result<PointsLogPage> logs(Authentication auth,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        Long userId = requireUserId(auth);
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 20;
        return Result.ok(pointsLevelService.listLogs(userId, page, size));
    }

    @GetMapping("/calculate")
    public Result<PointsCalculateResult> calculate(Authentication auth,
                                                   @RequestParam BigDecimal amount) {
        Long userId = requireUserId(auth);
        if (amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        return Result.ok(pointsLevelService.calculateForOrder(userId, amount));
    }
}
