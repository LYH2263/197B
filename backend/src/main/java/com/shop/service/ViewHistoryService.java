package com.shop.service;

import com.shop.dto.ViewHistoryMergeRequest;
import com.shop.dto.ViewHistoryTop10VO;
import com.shop.dto.ViewHistoryVO;
import com.shop.entity.Product;
import com.shop.entity.ViewHistory;
import com.shop.mapper.ProductMapper;
import com.shop.mapper.ViewHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ViewHistoryService.class);
    private static final int DEDUP_MINUTES = 30;

    private final ViewHistoryMapper viewHistoryMapper;
    private final ProductMapper productMapper;

    private ViewHistoryVO buildVO(ViewHistory vh, Product p) {
        ViewHistoryVO vo = new ViewHistoryVO();
        vo.setId(vh.getId());
        vo.setProductId(p.getId());
        vo.setProductName(p.getName());
        vo.setProductImage(p.getMainImage());
        vo.setCategoryId(p.getCategoryId());
        vo.setViewedPrice(vh.getViewedPrice());
        vo.setCurrentPrice(p.getPrice());
        vo.setStock(p.getStock());
        vo.setStatus(p.getStatus());
        vo.setOffShelf(p.getStatus() != null && p.getStatus() == 0);
        vo.setViewedAt(vh.getViewedAt());

        BigDecimal diff = p.getPrice().subtract(vh.getViewedPrice());
        vo.setPriceChange(diff);
        if (vh.getViewedPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percent = diff.divide(vh.getViewedPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setPriceChangePercent(percent);
        } else {
            vo.setPriceChangePercent(BigDecimal.ZERO);
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addViewHistory(Long userId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        ViewHistory exist = viewHistoryMapper.selectByUserIdAndProductId(userId, productId);
        LocalDateTime now = LocalDateTime.now();

        if (exist != null && exist.getViewedAt() != null) {
            LocalDateTime threshold = exist.getViewedAt().plusMinutes(DEDUP_MINUTES);
            if (now.isBefore(threshold)) {
                exist.setViewedAt(now);
                exist.setViewedPrice(product.getPrice());
                viewHistoryMapper.updateById(exist);
                log.debug("ViewHistory updated (dedup): userId={}, productId={}", userId, productId);
                return;
            }
        }

        ViewHistory vh = new ViewHistory();
        vh.setUserId(userId);
        vh.setProductId(productId);
        vh.setViewedPrice(product.getPrice());
        vh.setViewedAt(now);
        viewHistoryMapper.insert(vh);
        log.debug("ViewHistory inserted: userId={}, productId={}", userId, productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int mergeLocalHistory(Long userId, List<ViewHistoryMergeRequest.LocalHistoryItem> items) {
        if (items == null || items.isEmpty()) return 0;

        int merged = 0;
        Map<Long, ViewHistoryMergeRequest.LocalHistoryItem> dedupMap = new HashMap<>();

        for (ViewHistoryMergeRequest.LocalHistoryItem item : items) {
            if (item == null || item.getProductId() == null) continue;
            Long pid = item.getProductId();
            if (!dedupMap.containsKey(pid)
                    || (item.getViewedAt() != null
                    && dedupMap.get(pid).getViewedAt() != null
                    && item.getViewedAt().isAfter(dedupMap.get(pid).getViewedAt()))) {
                dedupMap.put(pid, item);
            }
        }

        for (ViewHistoryMergeRequest.LocalHistoryItem item : dedupMap.values()) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) continue;

            ViewHistory dbRecord = viewHistoryMapper.selectByUserIdAndProductId(userId, item.getProductId());
            if (dbRecord != null) {
                if (item.getViewedAt() != null && dbRecord.getViewedAt() != null
                        && item.getViewedAt().isAfter(dbRecord.getViewedAt())) {
                    dbRecord.setViewedAt(item.getViewedAt());
                    if (item.getViewedPrice() != null) {
                        dbRecord.setViewedPrice(item.getViewedPrice());
                    }
                    viewHistoryMapper.updateById(dbRecord);
                    merged++;
                }
                continue;
            }

            ViewHistory vh = new ViewHistory();
            vh.setUserId(userId);
            vh.setProductId(item.getProductId());
            vh.setViewedPrice(item.getViewedPrice() != null ? item.getViewedPrice() : product.getPrice());
            vh.setViewedAt(item.getViewedAt() != null ? item.getViewedAt() : LocalDateTime.now());
            viewHistoryMapper.insert(vh);
            merged++;
        }

        log.info("ViewHistory merged: userId={}, count={}", userId, merged);
        return merged;
    }

    public int countByUserId(Long userId) {
        return viewHistoryMapper.countByUserId(userId);
    }

    public List<ViewHistoryVO> listByPage(Long userId, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<ViewHistory> list = viewHistoryMapper.selectByUserIdWithPage(userId, offset, size);
        List<ViewHistoryVO> result = new ArrayList<>();
        for (ViewHistory vh : list) {
            Product p = productMapper.selectById(vh.getProductId());
            if (p == null) continue;
            result.add(buildVO(vh, p));
        }
        return result;
    }

    public List<ViewHistoryVO> listRecent(Long userId, int limit) {
        List<ViewHistory> list = viewHistoryMapper.selectRecentByUserId(userId, limit);
        List<ViewHistoryVO> result = new ArrayList<>();
        for (ViewHistory vh : list) {
            Product p = productMapper.selectById(vh.getProductId());
            if (p == null) continue;
            result.add(buildVO(vh, p));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long userId, Long id) {
        viewHistoryMapper.deleteById(userId, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeBatchByIds(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        viewHistoryMapper.deleteBatchByIds(userId, ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        viewHistoryMapper.deleteAllByUserId(userId);
    }

    public List<ViewHistoryTop10VO> getTop10ProductsLast7Days() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        return viewHistoryMapper.selectTop10Products(startTime, 10);
    }
}
