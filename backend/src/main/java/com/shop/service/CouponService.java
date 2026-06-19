package com.shop.service;

import com.shop.dto.*;
import com.shop.entity.Category;
import com.shop.entity.Coupon;
import com.shop.entity.UserCoupon;
import com.shop.mapper.CategoryMapper;
import com.shop.mapper.CouponMapper;
import com.shop.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    private static final int MAX_RETRY = 3;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CategoryMapper categoryMapper;

    public Coupon create(CouponCreateRequest req) {
        if (req.getType() == 1 || req.getType() == 3) {
            if (req.getFaceValue() == null || req.getFaceValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("满减/立减券必须设置面额");
            }
        } else if (req.getType() == 2) {
            if (req.getDiscountRate() == null || req.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0 || req.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
                throw new IllegalArgumentException("折扣券折扣率必须在0-1之间");
            }
        } else {
            throw new IllegalArgumentException("无效的券类型");
        }
        if (req.getValidEnd().isBefore(req.getValidStart())) {
            throw new IllegalArgumentException("有效期结束时间不能早于开始时间");
        }
        if (req.getThreshold().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("使用门槛不能为负数");
        }
        if (req.getApplicableCategory() != null) {
            Category category = categoryMapper.selectById(req.getApplicableCategory());
            if (category == null) {
                throw new IllegalArgumentException("分类不存在");
            }
        }

        Coupon coupon = new Coupon();
        coupon.setName(req.getName());
        coupon.setType(req.getType());
        coupon.setThreshold(req.getThreshold());
        coupon.setFaceValue(req.getFaceValue());
        coupon.setDiscountRate(req.getDiscountRate());
        coupon.setValidStart(req.getValidStart());
        coupon.setValidEnd(req.getValidEnd());
        coupon.setTotalQuantity(req.getTotalQuantity());
        coupon.setClaimedQuantity(0);
        coupon.setUsedQuantity(0);
        coupon.setPerUserLimit(req.getPerUserLimit());
        coupon.setApplicableCategory(req.getApplicableCategory());
        coupon.setStatus(1);
        coupon.setVersion(0);
        couponMapper.insert(coupon);
        log.info("Coupon created: id={}, name={}", coupon.getId(), coupon.getName());
        return coupon;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserCoupon claim(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        if (coupon.getStatus() != 1) {
            throw new IllegalArgumentException("优惠券已作废");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidStart()) || now.isAfter(coupon.getValidEnd())) {
            throw new IllegalArgumentException("优惠券不在有效期内");
        }
        if (coupon.getClaimedQuantity() >= coupon.getTotalQuantity()) {
            throw new IllegalArgumentException("优惠券已领完");
        }

        int count = userCouponMapper.countByUserIdAndCouponId(userId, couponId);
        if (count >= coupon.getPerUserLimit()) {
            throw new IllegalArgumentException("已达到领取上限");
        }

        int updated = 0;
        for (int i = 0; i < MAX_RETRY; i++) {
            coupon = couponMapper.selectById(couponId);
            if (coupon.getClaimedQuantity() >= coupon.getTotalQuantity()) {
                throw new IllegalArgumentException("优惠券已领完");
            }
            updated = couponMapper.increaseClaimedQuantity(couponId, coupon.getVersion());
            if (updated > 0) {
                break;
            }
        }
        if (updated <= 0) {
            throw new IllegalStateException("领取失败，请稍后重试");
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setCode(generateCouponCode());
        userCoupon.setStatus(0);
        userCoupon.setExpiredAt(coupon.getValidEnd());
        userCouponMapper.insert(userCoupon);

        log.info("Coupon claimed: userId={}, couponId={}, userCouponId={}", userId, couponId, userCoupon.getId());
        return userCoupon;
    }

    public CouponCalculateResult calculate(Long userId, CouponCalculateRequest req) {
        BigDecimal originalAmount = BigDecimal.ZERO;
        Set<Long> categoryIds = new HashSet<>();
        for (CouponCalculateRequest.OrderItemDTO item : req.getItems()) {
            originalAmount = originalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            if (item.getCategoryId() != null) {
                categoryIds.add(item.getCategoryId());
            }
        }

        CouponCalculateResult result = new CouponCalculateResult();
        result.setOriginalAmount(originalAmount);
        result.setFinalAmount(originalAmount);
        result.setDiscountAmount(BigDecimal.ZERO);

        if (userId == null) {
            return result;
        }

        userCouponMapper.expireOverdue(LocalDateTime.now());

        List<UserCoupon> availableUserCoupons = userCouponMapper.selectAvailableByUserId(userId, LocalDateTime.now());
        List<UserCouponVO> availableVOs = new ArrayList<>();
        List<UserCouponVO> unavailableVOs = new ArrayList<>();

        UserCouponVO bestCoupon = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;

        for (UserCoupon uc : availableUserCoupons) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null) continue;

            UserCouponVO vo = convertToUserCouponVO(uc, coupon);
            BigDecimal applicableAmount = calculateApplicableAmount(coupon, categoryIds, req.getItems());

            BigDecimal discount = calculateDiscount(coupon, applicableAmount);
            vo.setDiscountAmount(discount);

            boolean isAvailable = applicableAmount.compareTo(coupon.getThreshold()) >= 0 && discount.compareTo(BigDecimal.ZERO) > 0;
            vo.setAvailable(isAvailable);

            if (isAvailable) {
                availableVOs.add(vo);
                if (discount.compareTo(bestDiscount) > 0) {
                    bestDiscount = discount;
                    bestCoupon = vo;
                }
            } else {
                unavailableVOs.add(vo);
            }
        }

        result.setAvailableCoupons(availableVOs);
        result.setUnavailableCoupons(unavailableVOs);

        if (bestCoupon != null) {
            result.setBestCoupon(bestCoupon);
            result.setBestUserCouponId(bestCoupon.getId());
        }

        UserCouponVO selectedCoupon = null;
        if (req.getUserCouponId() != null) {
            for (UserCouponVO vo : availableVOs) {
                if (vo.getId().equals(req.getUserCouponId())) {
                    selectedCoupon = vo;
                    break;
                }
            }
        }

        if (selectedCoupon == null && bestCoupon != null) {
            selectedCoupon = bestCoupon;
        }

        if (selectedCoupon != null) {
            result.setSelectedCoupon(selectedCoupon);
            result.setSelectedUserCouponId(selectedCoupon.getId());
            result.setDiscountAmount(selectedCoupon.getDiscountAmount());
            result.setFinalAmount(originalAmount.subtract(selectedCoupon.getDiscountAmount()));
        }

        return result;
    }

    private BigDecimal calculateApplicableAmount(Coupon coupon, Set<Long> categoryIds, List<CouponCalculateRequest.OrderItemDTO> items) {
        if (coupon.getApplicableCategory() == null) {
            return items.stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return items.stream()
                .filter(i -> coupon.getApplicableCategory().equals(i.getCategoryId()))
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal applicableAmount) {
        if (applicableAmount.compareTo(coupon.getThreshold()) < 0) {
            return BigDecimal.ZERO;
        }
        if (coupon.getType() == 1) {
            return coupon.getFaceValue().min(applicableAmount);
        } else if (coupon.getType() == 2) {
            BigDecimal discount = applicableAmount.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
            return discount.setScale(2, RoundingMode.HALF_UP);
        } else if (coupon.getType() == 3) {
            return coupon.getFaceValue().min(applicableAmount);
        }
        return BigDecimal.ZERO;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserCoupon useCoupon(Long userCouponId, Long userId, Long orderId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        if (uc.getStatus() != 0) {
            throw new IllegalArgumentException("优惠券状态不可用");
        }
        if (uc.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("优惠券已过期");
        }

        int updated = userCouponMapper.markUsed(userCouponId, orderId, LocalDateTime.now());
        if (updated <= 0) {
            throw new IllegalStateException("优惠券使用失败");
        }

        couponMapper.increaseUsedQuantity(uc.getCouponId());

        log.info("Coupon used: userCouponId={}, orderId={}", userCouponId, orderId);
        return userCouponMapper.selectById(userCouponId);
    }

    public List<CouponVO> listAvailableForClaim(Long userId) {
        List<Coupon> coupons = couponMapper.selectAvailable(LocalDateTime.now());
        Map<Long, Integer> userClaimCountMap = new HashMap<>();
        if (userId != null) {
            List<UserCoupon> userCoupons = userCouponMapper.selectByUserId(userId);
            for (UserCoupon uc : userCoupons) {
                userClaimCountMap.merge(uc.getCouponId(), 1, Integer::sum);
            }
        }

        List<CouponVO> result = new ArrayList<>();
        for (Coupon coupon : coupons) {
            CouponVO vo = convertToCouponVO(coupon);
            Integer userClaimed = userClaimCountMap.getOrDefault(coupon.getId(), 0);
            vo.setUserClaimedCount(userClaimed);
            vo.setCanClaim(userClaimed < coupon.getPerUserLimit() && coupon.getClaimedQuantity() < coupon.getTotalQuantity());
            result.add(vo);
        }
        return result;
    }

    public List<UserCouponVO> listMyCoupons(Long userId, Integer status) {
        userCouponMapper.expireOverdue(LocalDateTime.now());
        List<UserCoupon> userCoupons;
        if (status != null) {
            userCoupons = userCouponMapper.selectByUserIdAndStatus(userId, status);
        } else {
            userCoupons = userCouponMapper.selectByUserId(userId);
        }

        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null) continue;
            result.add(convertToUserCouponVO(uc, coupon));
        }
        return result;
    }

    public List<Coupon> listAll(Integer status) {
        return couponMapper.selectList(status);
    }

    public List<UserCouponVO> listClaimedRecords(Long couponId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectAll(couponId);

        List<UserCouponVO> result = new ArrayList<>();
        for (UserCoupon uc : userCoupons) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null) continue;
            result.add(convertToUserCouponVO(uc, coupon));
        }
        return result;
    }

    public void invalidate(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new IllegalArgumentException("优惠券不存在");
        }
        couponMapper.updateStatus(couponId, 0);
        log.info("Coupon invalidated: id={}", couponId);
    }

    public Coupon getById(Long id) {
        return couponMapper.selectById(id);
    }

    public UserCoupon getUserCouponById(Long id) {
        return userCouponMapper.selectById(id);
    }

    private String generateCouponCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("CPN");
        Random random = new Random();
        for (int i = 0; i < 9; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private CouponVO convertToCouponVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setTypeDesc(getTypeDesc(coupon.getType()));
        vo.setThreshold(coupon.getThreshold());
        vo.setFaceValue(coupon.getFaceValue());
        vo.setDiscountRate(coupon.getDiscountRate());
        vo.setValidStart(coupon.getValidStart());
        vo.setValidEnd(coupon.getValidEnd());
        vo.setTotalQuantity(coupon.getTotalQuantity());
        vo.setClaimedQuantity(coupon.getClaimedQuantity());
        vo.setPerUserLimit(coupon.getPerUserLimit());
        vo.setApplicableCategory(coupon.getApplicableCategory());
        vo.setDesc(getCouponDesc(coupon));
        if (coupon.getApplicableCategory() != null) {
            Category category = categoryMapper.selectById(coupon.getApplicableCategory());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return vo;
    }

    private UserCouponVO convertToUserCouponVO(UserCoupon uc, Coupon coupon) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(uc.getId());
        vo.setCouponId(uc.getCouponId());
        vo.setCode(uc.getCode());
        vo.setStatus(uc.getStatus());
        vo.setStatusDesc(getStatusDesc(uc.getStatus()));
        vo.setUsedOrderId(uc.getUsedOrderId());
        vo.setUsedAt(uc.getUsedAt());
        vo.setClaimedAt(uc.getClaimedAt());
        vo.setExpiredAt(uc.getExpiredAt());
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setTypeDesc(getTypeDesc(coupon.getType()));
        vo.setThreshold(coupon.getThreshold());
        vo.setFaceValue(coupon.getFaceValue());
        vo.setDiscountRate(coupon.getDiscountRate());
        vo.setValidStart(coupon.getValidStart());
        vo.setValidEnd(coupon.getValidEnd());
        vo.setApplicableCategory(coupon.getApplicableCategory());
        vo.setDesc(getCouponDesc(coupon));
        if (coupon.getApplicableCategory() != null) {
            Category category = categoryMapper.selectById(coupon.getApplicableCategory());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return vo;
    }

    private String getTypeDesc(Integer type) {
        switch (type) {
            case 1: return "满减券";
            case 2: return "折扣券";
            case 3: return "立减券";
            default: return "未知";
        }
    }

    private String getStatusDesc(Integer status) {
        switch (status) {
            case 0: return "未使用";
            case 1: return "已使用";
            case 2: return "已过期";
            case 3: return "已作废";
            default: return "未知";
        }
    }

    private String getCouponDesc(Coupon coupon) {
        StringBuilder sb = new StringBuilder();
        if (coupon.getType() == 1) {
            sb.append("满").append(coupon.getThreshold()).append("减").append(coupon.getFaceValue());
        } else if (coupon.getType() == 2) {
            sb.append(coupon.getDiscountRate().multiply(BigDecimal.TEN).setScale(1, RoundingMode.HALF_UP)).append("折");
            if (coupon.getThreshold().compareTo(BigDecimal.ZERO) > 0) {
                sb.append(" 满").append(coupon.getThreshold()).append("可用");
            }
        } else if (coupon.getType() == 3) {
            sb.append("立减").append(coupon.getFaceValue());
        }
        if (coupon.getApplicableCategory() != null) {
            Category category = categoryMapper.selectById(coupon.getApplicableCategory());
            if (category != null) {
                sb.append(" 限").append(category.getName());
            }
        } else {
            sb.append(" 全场通用");
        }
        return sb.toString();
    }
}
