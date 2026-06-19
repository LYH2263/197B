package com.shop.service;

import com.shop.dto.SeckillSessionCreateRequest;
import com.shop.dto.SeckillSessionVO;
import com.shop.entity.Product;
import com.shop.entity.SeckillSession;
import com.shop.entity.SeckillToken;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.SeckillOrderMapper;
import com.shop.mapper.SeckillSessionMapper;
import com.shop.mapper.SeckillTokenMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    private static final int TOKEN_EXPIRE_MINUTES = 10;

    private static final int ACTIVITY_STATUS_NOT_START = 0;
    private static final int ACTIVITY_STATUS_ONGOING = 1;
    private static final int ACTIVITY_STATUS_ENDED = 2;
    private static final int ACTIVITY_STATUS_SOLD_OUT = 3;
    private static final int ACTIVITY_STATUS_DISABLED = 4;

    private final SeckillSessionMapper seckillSessionMapper;
    private final SeckillTokenMapper seckillTokenMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final ProductMapper productMapper;

    public SeckillSessionVO getDetailById(Long id, Long userId) {
        SeckillSession session = seckillSessionMapper.selectById(id);
        if (session == null) {
            return null;
        }
        return toVO(session, userId);
    }

    public List<SeckillSessionVO> listActiveSessions(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillSession> sessions = seckillSessionMapper.selectUpcomingOrActive(now);
        List<SeckillSessionVO> result = new ArrayList<>(sessions.size());
        for (SeckillSession s : sessions) {
            result.add(toVO(s, userId));
        }
        return result;
    }

    public List<SeckillSession> listAll() {
        return seckillSessionMapper.selectAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public SeckillSession create(SeckillSessionCreateRequest req) {
        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        Product p = productMapper.selectById(req.getProductId());
        if (p == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (req.getSeckillPrice().compareTo(p.getPrice()) >= 0) {
            throw new IllegalArgumentException("秒杀价必须低于原价");
        }
        SeckillSession s = new SeckillSession();
        s.setProductId(req.getProductId());
        s.setSeckillPrice(req.getSeckillPrice());
        s.setTotalStock(req.getTotalStock());
        s.setSoldStock(0);
        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setPerUserLimit(req.getPerUserLimit());
        s.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        seckillSessionMapper.insert(s);
        log.info("Seckill session created: id={}, productId={}", s.getId(), s.getProductId());
        return s;
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SeckillSessionCreateRequest req) {
        SeckillSession s = seckillSessionMapper.selectById(id);
        if (s == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        if (req.getStartTime().isAfter(req.getEndTime())) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }
        Product p = productMapper.selectById(req.getProductId());
        if (p == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (req.getTotalStock() < s.getSoldStock()) {
            throw new IllegalArgumentException("总库存不能小于已售数量");
        }
        s.setProductId(req.getProductId());
        s.setSeckillPrice(req.getSeckillPrice());
        s.setTotalStock(req.getTotalStock());
        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setPerUserLimit(req.getPerUserLimit());
        if (req.getStatus() != null) {
            s.setStatus(req.getStatus());
        }
        seckillSessionMapper.update(s);
        log.info("Seckill session updated: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        SeckillSession s = seckillSessionMapper.selectById(id);
        if (s == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        seckillSessionMapper.updateStatus(id, status);
        log.info("Seckill session status updated: id={}, status={}", id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SeckillSession s = seckillSessionMapper.selectById(id);
        if (s == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        seckillSessionMapper.deleteById(id);
        log.info("Seckill session deleted: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public String acquireToken(Long sessionId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        SeckillSession session = seckillSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("秒杀场次不存在");
        }
        if (session.getStatus() != 1) {
            throw new IllegalStateException("秒杀活动未启用");
        }
        if (now.isBefore(session.getStartTime())) {
            long seconds = Duration.between(now, session.getStartTime()).getSeconds();
            throw new IllegalStateException("活动未开始，请等待 " + seconds + " 秒后再试");
        }
        if (now.isAfter(session.getEndTime())) {
            throw new IllegalStateException("活动已结束");
        }
        if (session.getTotalStock() - session.getSoldStock() <= 0) {
            throw new IllegalStateException("很抱歉，商品已售罄");
        }
        int userBought = seckillOrderMapper.sumQuantityBySessionIdAndUserId(sessionId, userId);
        if (userBought >= session.getPerUserLimit()) {
            throw new IllegalStateException("您已达到本场限购数量（" + session.getPerUserLimit() + " 件）");
        }
        int validTokenCount = seckillTokenMapper.countValidBySessionIdAndUserId(sessionId, userId, now);
        if (validTokenCount > 0) {
            throw new IllegalStateException("您已有待使用的秒杀资格，请先下单");
        }
        try {
            int simulateMs = ThreadLocalRandom.current().nextInt(200, 800);
            Thread.sleep(simulateMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        SeckillToken st = new SeckillToken();
        st.setToken(token);
        st.setSessionId(sessionId);
        st.setUserId(userId);
        st.setExpireAt(now.plusMinutes(TOKEN_EXPIRE_MINUTES));
        st.setUsed(0);
        seckillTokenMapper.insert(st);
        log.info("Seckill token issued: sessionId={}, userId={}, token={}", sessionId, userId, token);
        return token;
    }

    @Transactional(rollbackFor = Exception.class)
    public SeckillToken validateAndMarkToken(String token, Long sessionId, Long userId) {
        SeckillToken st = seckillTokenMapper.selectByToken(token);
        if (st == null) {
            throw new IllegalArgumentException("秒杀资格无效，请重新获取");
        }
        if (!st.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("秒杀资格与场次不匹配");
        }
        if (!st.getUserId().equals(userId)) {
            throw new IllegalArgumentException("秒杀资格不属于当前用户");
        }
        if (st.getUsed() == 1) {
            throw new IllegalStateException("秒杀资格已使用，请重新获取");
        }
        if (LocalDateTime.now().isAfter(st.getExpireAt())) {
            throw new IllegalStateException("秒杀资格已过期，请重新获取");
        }
        int updated = seckillTokenMapper.markUsed(token);
        if (updated == 0) {
            throw new IllegalStateException("秒杀资格已被占用，请重新获取");
        }
        return st;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long sessionId, int quantity) {
        int rows = seckillSessionMapper.deductStock(sessionId, quantity);
        return rows > 0;
    }

    public int getUserBoughtCount(Long sessionId, Long userId) {
        return seckillOrderMapper.sumQuantityBySessionIdAndUserId(sessionId, userId);
    }

    private SeckillSessionVO toVO(SeckillSession s, Long userId) {
        SeckillSessionVO vo = new SeckillSessionVO();
        vo.setId(s.getId());
        vo.setProductId(s.getProductId());
        vo.setSeckillPrice(s.getSeckillPrice());
        vo.setTotalStock(s.getTotalStock());
        vo.setSoldStock(s.getSoldStock());
        vo.setStartTime(s.getStartTime());
        vo.setEndTime(s.getEndTime());
        vo.setPerUserLimit(s.getPerUserLimit());
        vo.setStatus(s.getStatus());
        vo.setCreatedAt(s.getCreatedAt());
        vo.setUpdatedAt(s.getUpdatedAt());

        Product p = productMapper.selectById(s.getProductId());
        if (p != null) {
            vo.setProductName(p.getName());
            vo.setProductImage(p.getMainImage());
            vo.setOriginalPrice(p.getPrice());
        }

        LocalDateTime now = LocalDateTime.now();
        int remainStock = s.getTotalStock() - s.getSoldStock();
        int activityStatus;
        long remainSeconds;

        if (s.getStatus() != 1) {
            activityStatus = ACTIVITY_STATUS_DISABLED;
            remainSeconds = 0L;
        } else if (now.isBefore(s.getStartTime())) {
            activityStatus = ACTIVITY_STATUS_NOT_START;
            remainSeconds = Duration.between(now, s.getStartTime()).getSeconds();
        } else if (remainStock <= 0) {
            activityStatus = ACTIVITY_STATUS_SOLD_OUT;
            remainSeconds = 0L;
        } else if (now.isAfter(s.getEndTime())) {
            activityStatus = ACTIVITY_STATUS_ENDED;
            remainSeconds = 0L;
        } else {
            activityStatus = ACTIVITY_STATUS_ONGOING;
            remainSeconds = Duration.between(now, s.getEndTime()).getSeconds();
        }

        vo.setActivityStatus(activityStatus);
        vo.setRemainSeconds(remainSeconds);

        int soldPercent = s.getTotalStock() == 0 ? 100 : (int) (s.getSoldStock() * 100.0 / s.getTotalStock());
        vo.setSoldPercent(soldPercent);

        if (userId != null) {
            vo.setUserBoughtCount(seckillOrderMapper.sumQuantityBySessionIdAndUserId(s.getId(), userId));
        } else {
            vo.setUserBoughtCount(0);
        }

        return vo;
    }
}
