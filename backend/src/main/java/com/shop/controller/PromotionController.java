package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.PromotionCalculateRequest;
import com.shop.dto.PromotionCalculateResult;
import com.shop.dto.PromotionVO;
import com.shop.entity.Promotion;
import com.shop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/active")
    public Result<List<PromotionVO>> listActive() {
        List<Promotion> promotions = promotionService.listActive();
        List<PromotionVO> result = promotions.stream().map(p -> {
            PromotionVO vo = new PromotionVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setStartTime(p.getStartTime());
            vo.setEndTime(p.getEndTime());
            vo.setScopeType(p.getScopeType());
            vo.setScopeTypeDesc(p.getScopeType() == 1 ? "全场通用" : "指定分类");
            vo.setApplicableCategory(p.getApplicableCategory());
            vo.setStatus(p.getStatus());
            vo.setStatusDesc(p.getStatus() == 1 ? "进行中" : "已结束");
            List<PromotionVO.TierVO> tierVOs = new ArrayList<>();
            if (p.getTiers() != null) {
                for (var t : p.getTiers()) {
                    PromotionVO.TierVO tvo = new PromotionVO.TierVO();
                    tvo.setId(t.getId());
                    tvo.setThreshold(t.getThreshold());
                    tvo.setDiscount(t.getDiscount());
                    tierVOs.add(tvo);
                }
            }
            vo.setTiers(tierVOs);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tierVOs.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append("满").append(tierVOs.get(i).getThreshold())
                        .append("减").append(tierVOs.get(i).getDiscount());
            }
            vo.setTierDesc(sb.toString());
            return vo;
        }).collect(Collectors.toList());
        return Result.ok(result);
    }

    @PostMapping("/calculate")
    public Result<PromotionCalculateResult> calculate(@RequestBody PromotionCalculateRequest req) {
        return Result.ok(promotionService.calculate(req));
    }

    @PostMapping("/calculate-no-recommend")
    public Result<PromotionCalculateResult> calculateNoRecommend(@RequestBody PromotionCalculateRequest req) {
        return Result.ok(promotionService.calculate(req, false));
    }
}
