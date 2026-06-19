package com.shop.service;

import com.shop.dto.*;
import com.shop.entity.Category;
import com.shop.entity.OrderPromotionSnapshot;
import com.shop.entity.Product;
import com.shop.entity.Promotion;
import com.shop.entity.PromotionTier;
import com.shop.mapper.CategoryMapper;
import com.shop.mapper.OrderPromotionSnapshotMapper;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.PromotionMapper;
import com.shop.mapper.PromotionTierMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 满减促销服务
 *
 * 叠加规则说明：
 * 满减与优惠券可并存，计算顺序为【先满减、再优惠券】。
 * 即：最终金额 = (商品原价 - 满减减免) - 优惠券减免 - 积分抵扣
 * 原因：满减是平台级的商品促销，属于"降价"性质的基础优惠；
 * 优惠券是用户领取的专属权益，应在满减后的基础上进一步抵扣，
 * 这样可以避免优惠券门槛因满减而"被降低"的不公平情况。
 * 代码中对应流程见 OrderService.create()。
 */
@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);

    private static final int RECOMMEND_LIMIT = 3;

    private final PromotionMapper promotionMapper;
    private final PromotionTierMapper promotionTierMapper;
    private final OrderPromotionSnapshotMapper orderPromotionSnapshotMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public Promotion create(PromotionCreateRequest req) {
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("活动名称不能为空");
        }
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new IllegalArgumentException("活动时间不能为空");
        }
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("活动结束时间不能早于开始时间");
        }
        if (req.getScopeType() == null || (req.getScopeType() != 1 && req.getScopeType() != 2)) {
            throw new IllegalArgumentException("适用范围类型无效");
        }
        if (req.getScopeType() == 2) {
            if (req.getApplicableCategory() == null) {
                throw new IllegalArgumentException("指定分类时分类ID不能为空");
            }
            Category cat = categoryMapper.selectById(req.getApplicableCategory());
            if (cat == null) {
                throw new IllegalArgumentException("指定分类不存在");
            }
        }
        if (req.getTiers() == null || req.getTiers().isEmpty()) {
            throw new IllegalArgumentException("至少需要配置一档满减");
        }
        for (PromotionCreateRequest.TierDTO t : req.getTiers()) {
            if (t.getThreshold() == null || t.getThreshold().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("满减门槛必须大于0");
            }
            if (t.getDiscount() == null || t.getDiscount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("减免金额必须大于0");
            }
            if (t.getDiscount().compareTo(t.getThreshold()) >= 0) {
                throw new IllegalArgumentException("减免金额不能大于等于门槛金额");
            }
        }

        List<PromotionCreateRequest.TierDTO> sorted = new ArrayList<>(req.getTiers());
        sorted.sort(Comparator.comparing(PromotionCreateRequest.TierDTO::getThreshold));
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).getDiscount().compareTo(sorted.get(i - 1).getDiscount()) <= 0) {
                throw new IllegalArgumentException("高门槛档位的减免金额必须大于低门槛档位");
            }
        }

        Promotion promotion = new Promotion();
        promotion.setName(req.getName().trim());
        promotion.setStartTime(req.getStartTime());
        promotion.setEndTime(req.getEndTime());
        promotion.setScopeType(req.getScopeType());
        promotion.setApplicableCategory(req.getScopeType() == 2 ? req.getApplicableCategory() : null);
        promotion.setStatus(1);
        promotionMapper.insert(promotion);

        int sortOrder = 0;
        for (PromotionCreateRequest.TierDTO t : sorted) {
            PromotionTier tier = new PromotionTier();
            tier.setPromotionId(promotion.getId());
            tier.setThreshold(t.getThreshold());
            tier.setDiscount(t.getDiscount());
            tier.setSortOrder(sortOrder++);
            promotionTierMapper.insert(tier);
        }

        promotion.setTiers(promotionTierMapper.selectByPromotionId(promotion.getId()));
        log.info("Promotion created: id={}, name={}, tiers={}", promotion.getId(), promotion.getName(), promotion.getTiers().size());
        return promotion;
    }

    public Promotion getById(Long id) {
        Promotion p = promotionMapper.selectById(id);
        if (p != null) {
            p.setTiers(promotionTierMapper.selectByPromotionId(id));
        }
        return p;
    }

    public List<PromotionVO> listAll(Integer status) {
        List<Promotion> list = promotionMapper.selectList(status);
        List<PromotionVO> result = new ArrayList<>();
        for (Promotion p : list) {
            result.add(convertToVO(p, true));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Promotion p = promotionMapper.selectById(id);
        if (p == null) {
            throw new IllegalArgumentException("满减活动不存在");
        }
        promotionMapper.updateStatus(id, status);
        log.info("Promotion status updated: id={}, status={}", id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public Promotion update(Long id, PromotionCreateRequest req) {
        Promotion existing = promotionMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("满减活动不存在");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("活动名称不能为空");
        }
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new IllegalArgumentException("活动时间不能为空");
        }
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new IllegalArgumentException("活动结束时间不能早于开始时间");
        }
        if (req.getScopeType() == null || (req.getScopeType() != 1 && req.getScopeType() != 2)) {
            throw new IllegalArgumentException("适用范围类型无效");
        }
        if (req.getScopeType() == 2) {
            if (req.getApplicableCategory() == null) {
                throw new IllegalArgumentException("指定分类时分类ID不能为空");
            }
            Category cat = categoryMapper.selectById(req.getApplicableCategory());
            if (cat == null) {
                throw new IllegalArgumentException("指定分类不存在");
            }
        }
        if (req.getTiers() != null && !req.getTiers().isEmpty()) {
            for (PromotionCreateRequest.TierDTO t : req.getTiers()) {
                if (t.getThreshold() == null || t.getThreshold().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("满减门槛必须大于0");
                }
                if (t.getDiscount() == null || t.getDiscount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("减免金额必须大于0");
                }
                if (t.getDiscount().compareTo(t.getThreshold()) >= 0) {
                    throw new IllegalArgumentException("减免金额不能大于等于门槛金额");
                }
            }
            List<PromotionCreateRequest.TierDTO> sorted = new ArrayList<>(req.getTiers());
            sorted.sort(Comparator.comparing(PromotionCreateRequest.TierDTO::getThreshold));
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).getDiscount().compareTo(sorted.get(i - 1).getDiscount()) <= 0) {
                    throw new IllegalArgumentException("高门槛档位的减免金额必须大于低门槛档位");
                }
            }
        }

        existing.setName(req.getName().trim());
        existing.setStartTime(req.getStartTime());
        existing.setEndTime(req.getEndTime());
        existing.setScopeType(req.getScopeType());
        existing.setApplicableCategory(req.getScopeType() == 2 ? req.getApplicableCategory() : null);
        promotionMapper.updateById(existing);

        if (req.getTiers() != null && !req.getTiers().isEmpty()) {
            promotionTierMapper.deleteByPromotionId(id);
            int sortOrder = 0;
            List<PromotionCreateRequest.TierDTO> sorted = new ArrayList<>(req.getTiers());
            sorted.sort(Comparator.comparing(PromotionCreateRequest.TierDTO::getThreshold));
            for (PromotionCreateRequest.TierDTO t : sorted) {
                PromotionTier tier = new PromotionTier();
                tier.setPromotionId(id);
                tier.setThreshold(t.getThreshold());
                tier.setDiscount(t.getDiscount());
                tier.setSortOrder(sortOrder++);
                promotionTierMapper.insert(tier);
            }
        }

        existing.setTiers(promotionTierMapper.selectByPromotionId(id));
        log.info("Promotion updated: id={}", id);
        return existing;
    }

    /**
     * 计算购物车商品的满减结果（取最优档位）
     *
     * 叠加规则：满减与优惠券可并存，但顺序必须是【先满减 → 再优惠券】
     * 即计算优惠券时，应传入的是满减后的金额，而非原始金额。
     */
    public PromotionCalculateResult calculate(PromotionCalculateRequest req) {
        return calculate(req, true);
    }

    public PromotionCalculateResult calculate(PromotionCalculateRequest req, boolean withRecommend) {
        PromotionCalculateResult result = new PromotionCalculateResult();

        if (req.getItems() == null || req.getItems().isEmpty()) {
            result.setOriginalAmount(BigDecimal.ZERO);
            result.setPromotionDiscount(BigDecimal.ZERO);
            result.setApplicableAmount(BigDecimal.ZERO);
            return result;
        }

        BigDecimal originalAmount = BigDecimal.ZERO;
        Set<Long> categoryIds = new HashSet<>();
        List<Long> productIds = new ArrayList<>();
        for (PromotionCalculateRequest.OrderItemDTO item : req.getItems()) {
            originalAmount = originalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            if (item.getCategoryId() != null) {
                categoryIds.add(item.getCategoryId());
            }
            if (item.getProductId() != null) {
                productIds.add(item.getProductId());
            }
        }
        result.setOriginalAmount(originalAmount);
        result.setPromotionDiscount(BigDecimal.ZERO);
        result.setApplicableAmount(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        Promotion bestPromotion = null;
        PromotionTier bestTier = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        BigDecimal bestApplicable = BigDecimal.ZERO;

        List<Promotion> activePromotions = promotionMapper.selectActive(now);
        for (Promotion promotion : activePromotions) {
            List<PromotionTier> tiers = promotionTierMapper.selectByPromotionId(promotion.getId());
            if (tiers == null || tiers.isEmpty()) continue;

            BigDecimal applicable = calculateApplicableAmount(promotion, categoryIds, req.getItems());
            if (applicable.compareTo(BigDecimal.ZERO) <= 0) continue;

            PromotionTier tier = findBestTier(tiers, applicable);
            if (tier == null) continue;

            if (tier.getDiscount().compareTo(bestDiscount) > 0) {
                bestPromotion = promotion;
                bestTier = tier;
                bestDiscount = tier.getDiscount();
                bestApplicable = applicable;
            }
        }

        if (bestPromotion != null && bestTier != null) {
            result.setPromotionId(bestPromotion.getId());
            result.setPromotionName(bestPromotion.getName());
            result.setTierId(bestTier.getId());
            result.setTierThreshold(bestTier.getThreshold());
            result.setTierDiscount(bestTier.getDiscount());
            result.setPromotionDiscount(bestDiscount.min(bestApplicable));
            result.setApplicableAmount(bestApplicable);
            result.setScopeType(bestPromotion.getScopeType());
            result.setApplicableCategory(bestPromotion.getApplicableCategory());
            result.setDesc(buildDesc(bestPromotion, bestTier));

            if (bestPromotion.getApplicableCategory() != null) {
                Category cat = categoryMapper.selectById(bestPromotion.getApplicableCategory());
                if (cat != null) {
                    result.setCategoryName(cat.getName());
                }
            }

            List<PromotionTier> tiers = promotionTierMapper.selectByPromotionId(bestPromotion.getId());
            PromotionTier nextTier = findNextTier(tiers, bestTier);
            if (nextTier != null) {
                result.setNextTierThreshold(nextTier.getThreshold());
                result.setNextTierDiscount(nextTier.getDiscount());
                result.setGapToNextTier(nextTier.getThreshold().subtract(bestApplicable));
                result.setNextTierDesc(buildNextTierDesc(nextTier, result.getGapToNextTier()));
            }

            if (withRecommend && result.getGapToNextTier() != null
                    && result.getGapToNextTier().compareTo(BigDecimal.ZERO) > 0) {
                Long recommendCategoryId = bestPromotion.getScopeType() == 2
                        ? bestPromotion.getApplicableCategory() : null;
                List<Product> products = productMapper.selectRecommendForPromotion(
                        recommendCategoryId,
                        result.getGapToNextTier(),
                        productIds,
                        1,
                        RECOMMEND_LIMIT
                );
                if (products.isEmpty() && recommendCategoryId != null) {
                    products = productMapper.selectRecommendForPromotion(
                            null,
                            result.getGapToNextTier(),
                            productIds,
                            1,
                            RECOMMEND_LIMIT
                    );
                }
                List<PromotionRecommendProductVO> recommendVOs = new ArrayList<>();
                for (Product p : products) {
                    PromotionRecommendProductVO vo = new PromotionRecommendProductVO();
                    vo.setProductId(p.getId());
                    vo.setProductName(p.getName());
                    vo.setProductImage(p.getMainImage());
                    vo.setPrice(p.getPrice());
                    vo.setStock(p.getStock());
                    vo.setCategoryId(p.getCategoryId());
                    recommendVOs.add(vo);
                }
                result.setRecommendProducts(recommendVOs);
            }
        }

        return result;
    }

    private BigDecimal calculateApplicableAmount(Promotion promotion,
                                                 Set<Long> categoryIds,
                                                 List<PromotionCalculateRequest.OrderItemDTO> items) {
        if (promotion.getScopeType() == 1) {
            return items.stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        Long targetCategoryId = promotion.getApplicableCategory();
        return items.stream()
                .filter(i -> targetCategoryId.equals(i.getCategoryId()))
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PromotionTier findBestTier(List<PromotionTier> tiers, BigDecimal amount) {
        PromotionTier best = null;
        for (PromotionTier tier : tiers) {
            if (amount.compareTo(tier.getThreshold()) >= 0) {
                best = tier;
            }
        }
        return best;
    }

    private PromotionTier findNextTier(List<PromotionTier> tiers, PromotionTier current) {
        for (PromotionTier tier : tiers) {
            if (tier.getThreshold().compareTo(current.getThreshold()) > 0) {
                return tier;
            }
        }
        return null;
    }

    private String buildDesc(Promotion promotion, PromotionTier tier) {
        StringBuilder sb = new StringBuilder();
        sb.append("满").append(tier.getThreshold()).append("减").append(tier.getDiscount());
        if (promotion.getScopeType() == 2 && promotion.getApplicableCategory() != null) {
            Category cat = categoryMapper.selectById(promotion.getApplicableCategory());
            if (cat != null) {
                sb.append("（限").append(cat.getName()).append("）");
            }
        } else {
            sb.append("（全场）");
        }
        return sb.toString();
    }

    private String buildNextTierDesc(PromotionTier nextTier, BigDecimal gap) {
        return "再买 ¥" + gap + " 可享满" + nextTier.getThreshold() + "减" + nextTier.getDiscount();
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderPromotionSnapshot lockSnapshotForOrder(Long orderId,
                                                       PromotionCalculateResult calcResult) {
        if (calcResult == null || calcResult.getPromotionId() == null
                || calcResult.getPromotionDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        OrderPromotionSnapshot snapshot = new OrderPromotionSnapshot();
        snapshot.setOrderId(orderId);
        snapshot.setPromotionId(calcResult.getPromotionId());
        snapshot.setPromotionName(calcResult.getPromotionName());
        snapshot.setTierId(calcResult.getTierId());
        snapshot.setTierThreshold(calcResult.getTierThreshold());
        snapshot.setTierDiscount(calcResult.getTierDiscount());
        snapshot.setPromotionDiscount(calcResult.getPromotionDiscount());
        snapshot.setScopeType(calcResult.getScopeType());
        snapshot.setApplicableCategory(calcResult.getApplicableCategory());
        orderPromotionSnapshotMapper.insert(snapshot);
        log.info("Order promotion snapshot locked: orderId={}, promotionId={}, discount={}",
                orderId, calcResult.getPromotionId(), calcResult.getPromotionDiscount());
        return snapshot;
    }

    public OrderPromotionSnapshot getSnapshotByOrderId(Long orderId) {
        return orderPromotionSnapshotMapper.selectByOrderId(orderId);
    }

    private PromotionVO convertToVO(Promotion p, boolean withTiers) {
        PromotionVO vo = new PromotionVO();
        vo.setId(p.getId());
        vo.setName(p.getName());
        vo.setStartTime(p.getStartTime());
        vo.setEndTime(p.getEndTime());
        vo.setScopeType(p.getScopeType());
        vo.setScopeTypeDesc(p.getScopeType() == 1 ? "全场通用" : "指定分类");
        vo.setApplicableCategory(p.getApplicableCategory());
        if (p.getApplicableCategory() != null) {
            Category cat = categoryMapper.selectById(p.getApplicableCategory());
            if (cat != null) vo.setCategoryName(cat.getName());
        }
        vo.setStatus(p.getStatus());
        vo.setStatusDesc(p.getStatus() == 1 ? "启用" : "关闭");
        vo.setCreatedAt(p.getCreatedAt());

        List<PromotionTier> tiers;
        if (withTiers) {
            tiers = promotionTierMapper.selectByPromotionId(p.getId());
        } else {
            tiers = p.getTiers() != null ? p.getTiers() : promotionTierMapper.selectByPromotionId(p.getId());
        }
        List<PromotionVO.TierVO> tierVOs = tiers.stream()
                .sorted(Comparator.comparing(PromotionTier::getSortOrder))
                .map(t -> {
                    PromotionVO.TierVO tvo = new PromotionVO.TierVO();
                    tvo.setId(t.getId());
                    tvo.setThreshold(t.getThreshold());
                    tvo.setDiscount(t.getDiscount());
                    return tvo;
                })
                .collect(Collectors.toList());
        vo.setTiers(tierVOs);
        vo.setTierDesc(buildTierDesc(tierVOs));

        return vo;
    }

    private String buildTierDesc(List<PromotionVO.TierVO> tiers) {
        if (tiers == null || tiers.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (PromotionVO.TierVO t : tiers) {
            parts.add("满" + t.getThreshold() + "减" + t.getDiscount());
        }
        return String.join(" / ", parts);
    }

    public List<Promotion> listActive() {
        List<Promotion> promotions = promotionMapper.selectActive(LocalDateTime.now());
        for (Promotion p : promotions) {
            p.setTiers(promotionTierMapper.selectByPromotionId(p.getId()));
        }
        return promotions;
    }
}
