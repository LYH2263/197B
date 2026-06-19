package com.shop.service;

import com.shop.dto.PointsCalculateResult;
import com.shop.dto.PointsLogPage;
import com.shop.dto.UserLevelVO;
import com.shop.entity.PointsLog;
import com.shop.entity.User;
import com.shop.mapper.PointsLogMapper;
import com.shop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PointsLevelService {

    private static final Logger log = LoggerFactory.getLogger(PointsLevelService.class);

    private static final Map<Integer, LevelConfig> LEVEL_CONFIG = new LinkedHashMap<>();
    static {
        LEVEL_CONFIG.put(1, new LevelConfig(1, "Lv1 新手", BigDecimal.ZERO, new BigDecimal("1.0"), "基础权益：积分1倍累计"));
        LEVEL_CONFIG.put(2, new LevelConfig(2, "Lv2 铜牌", new BigDecimal("500.00"), new BigDecimal("1.2"), "积分1.2倍累计"));
        LEVEL_CONFIG.put(3, new LevelConfig(3, "Lv3 银牌", new BigDecimal("2000.00"), new BigDecimal("1.5"), "积分1.5倍累计"));
        LEVEL_CONFIG.put(4, new LevelConfig(4, "Lv4 金牌", new BigDecimal("5000.00"), new BigDecimal("2.0"), "积分2倍累计"));
        LEVEL_CONFIG.put(5, new LevelConfig(5, "Lv5 钻石", new BigDecimal("20000.00"), new BigDecimal("3.0"), "积分3倍累计，专属客服"));
    }

    public static final int POINTS_PER_YUAN = 100;
    public static final BigDecimal MAX_DEDUCT_RATIO = new BigDecimal("0.30");

    private final UserMapper userMapper;
    private final PointsLogMapper pointsLogMapper;

    public static class LevelConfig {
        public final int level;
        public final String name;
        public final BigDecimal threshold;
        public final BigDecimal rate;
        public final String benefits;
        public LevelConfig(int level, String name, BigDecimal threshold, BigDecimal rate, String benefits) {
            this.level = level;
            this.name = name;
            this.threshold = threshold;
            this.rate = rate;
            this.benefits = benefits;
        }
    }

    public static LevelConfig getLevelConfig(int level) {
        return LEVEL_CONFIG.getOrDefault(level, LEVEL_CONFIG.get(1));
    }

    public static LevelConfig calcLevelByConsume(BigDecimal totalConsume) {
        LevelConfig result = LEVEL_CONFIG.get(1);
        for (LevelConfig cfg : LEVEL_CONFIG.values()) {
            if (totalConsume.compareTo(cfg.threshold) >= 0) {
                result = cfg;
            } else {
                break;
            }
        }
        return result;
    }

    public static LevelConfig getNextLevel(int currentLevel) {
        return LEVEL_CONFIG.get(currentLevel + 1);
    }

    public UserLevelVO getUserLevelInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        BigDecimal consume = user.getTotalConsume() != null ? user.getTotalConsume() : BigDecimal.ZERO;
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;
        LevelConfig currentCfg = getLevelConfig(currentLevel);
        LevelConfig nextCfg = getNextLevel(currentLevel);

        UserLevelVO vo = new UserLevelVO();
        vo.setLevel(currentLevel);
        vo.setLevelName(currentCfg.name);
        vo.setTotalConsume(consume);
        vo.setPointRate(currentCfg.rate);
        vo.setBenefits(currentCfg.benefits);

        if (nextCfg != null) {
            vo.setNextLevelThreshold(nextCfg.threshold);
            vo.setNextLevelName(nextCfg.name);
            BigDecimal diff = nextCfg.threshold.subtract(currentCfg.threshold);
            BigDecimal progress = diff.compareTo(BigDecimal.ZERO) > 0
                    ? consume.subtract(currentCfg.threshold).divide(diff, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ONE;
            if (progress.compareTo(BigDecimal.ONE) > 0) progress = BigDecimal.ONE;
            if (progress.compareTo(BigDecimal.ZERO) < 0) progress = BigDecimal.ZERO;
            vo.setProgress(progress);
        } else {
            vo.setNextLevelThreshold(null);
            vo.setNextLevelName("已达最高等级");
            vo.setProgress(BigDecimal.ONE);
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public int awardPointsByOrder(Long userId, BigDecimal paidAmount, Long orderId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        BigDecimal consume = user.getTotalConsume() != null ? user.getTotalConsume() : BigDecimal.ZERO;
        consume = consume.add(paidAmount);
        user.setTotalConsume(consume);

        LevelConfig cfg = calcLevelByConsume(consume);
        int newLevel = cfg.level;
        user.setLevel(newLevel);

        BigDecimal basePoints = paidAmount.multiply(new BigDecimal("0.01"));
        BigDecimal finalPoints = basePoints.multiply(cfg.rate);
        int awarded = finalPoints.setScale(0, RoundingMode.DOWN).intValue();
        if (awarded < 0) awarded = 0;

        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        int newBalance = currentPoints + awarded;
        user.setPoints(newBalance);
        userMapper.updateById(user);

        PointsLog pl = new PointsLog();
        pl.setUserId(userId);
        pl.setType(1);
        pl.setChangePoints(awarded);
        pl.setBalance(newBalance);
        pl.setRelatedType("order");
        pl.setRelatedId(orderId);
        pl.setRemark("订单收货奖励积分 x" + cfg.rate + "倍");
        pointsLogMapper.insert(pl);

        log.info("Points awarded: userId={}, orderId={}, base={}, final={}, rate={}, newLevel={}",
                userId, orderId, basePoints, awarded, cfg.rate, newLevel);
        return awarded;
    }

    public PointsCalculateResult calculateForOrder(Long userId, BigDecimal orderPayAmount) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        int available = user.getPoints() != null ? user.getPoints() : 0;
        BigDecimal maxDiscount = orderPayAmount.multiply(MAX_DEDUCT_RATIO);
        int maxPointsByRatio = maxDiscount.multiply(new BigDecimal(POINTS_PER_YUAN)).setScale(0, RoundingMode.DOWN).intValue();
        int maxPoints = Math.min(available, maxPointsByRatio);

        LevelConfig cfg = getLevelConfig(user.getLevel() != null ? user.getLevel() : 1);

        PointsCalculateResult r = new PointsCalculateResult();
        r.setAvailablePoints(available);
        r.setMaxPointsUsable(maxPoints);
        r.setMaxDiscountAmount(new BigDecimal(maxPoints).divide(new BigDecimal(POINTS_PER_YUAN), 2, RoundingMode.DOWN));
        r.setLevel(cfg.level);
        r.setPointRate(cfg.rate);
        return r;
    }

    @Transactional(rollbackFor = Exception.class)
    public int deductPointsForOrder(Long userId, int points, Long orderId) {
        if (points <= 0) return 0;
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (currentPoints < points) {
            throw new IllegalStateException("积分余额不足");
        }
        int newBalance = currentPoints - points;
        user.setPoints(newBalance);
        userMapper.updateById(user);

        PointsLog pl = new PointsLog();
        pl.setUserId(userId);
        pl.setType(2);
        pl.setChangePoints(-points);
        pl.setBalance(newBalance);
        pl.setRelatedType("order");
        pl.setRelatedId(orderId);
        pl.setRemark("下单抵扣使用" + points + "积分");
        pointsLogMapper.insert(pl);

        log.info("Points deducted: userId={}, orderId={}, points={}", userId, orderId, points);
        return points;
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long userId, int changePoints, String remark, Long adminId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        int newBalance = currentPoints + changePoints;
        if (newBalance < 0) {
            throw new IllegalStateException("调整后积分不能为负");
        }
        user.setPoints(newBalance);
        userMapper.updateById(user);

        PointsLog pl = new PointsLog();
        pl.setUserId(userId);
        pl.setType(4);
        pl.setChangePoints(changePoints);
        pl.setBalance(newBalance);
        pl.setRelatedType("admin");
        pl.setRelatedId(adminId);
        pl.setRemark(remark != null ? remark : "管理员手工调整");
        pointsLogMapper.insert(pl);
        log.info("Points adjusted: userId={}, change={}, by admin={}", userId, changePoints, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustLevel(Long userId, int level) {
        if (level < 1 || level > 5) {
            throw new IllegalArgumentException("等级范围 1-5");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setLevel(level);
        userMapper.updateById(user);
        log.info("Level adjusted: userId={}, newLevel={}", userId, level);
    }

    public PointsLogPage listLogs(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<PointsLog> list = pointsLogMapper.selectByUserId(userId, offset, size);
        int total = pointsLogMapper.countByUserId(userId);
        PointsLogPage p = new PointsLogPage();
        p.setList(list);
        p.setTotal(total);
        p.setPage(page);
        p.setSize(size);
        return p;
    }

    public int getUserPoints(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null && u.getPoints() != null ? u.getPoints() : 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public int deductPointsForExchange(Long userId, int points, Long exchangeOrderId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        if (currentPoints < points) {
            throw new IllegalStateException("积分余额不足");
        }
        int newBalance = currentPoints - points;
        user.setPoints(newBalance);
        userMapper.updateById(user);

        PointsLog pl = new PointsLog();
        pl.setUserId(userId);
        pl.setType(3);
        pl.setChangePoints(-points);
        pl.setBalance(newBalance);
        pl.setRelatedType("redeem");
        pl.setRelatedId(exchangeOrderId);
        pl.setRemark("积分商城兑换扣减" + points + "积分");
        pointsLogMapper.insert(pl);
        log.info("Points exchanged: userId={}, exchangeOrderId={}, points={}", userId, exchangeOrderId, points);
        return points;
    }
}
