package com.shop.service;

import com.shop.dto.FavoriteBatchRequest;
import com.shop.dto.FavoriteCountVO;
import com.shop.dto.FavoriteGroupVO;
import com.shop.dto.FavoriteVO;
import com.shop.dto.PriceAlertVO;
import com.shop.entity.Favorite;
import com.shop.entity.FavoriteGroup;
import com.shop.entity.PriceAlert;
import com.shop.entity.Product;
import com.shop.mapper.FavoriteGroupMapper;
import com.shop.mapper.FavoriteMapper;
import com.shop.mapper.PriceAlertMapper;
import com.shop.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
public class FavoriteService {

    private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);
    private static final BigDecimal PRICE_DOWN_THRESHOLD = new BigDecimal("0.05");

    private final FavoriteMapper favoriteMapper;
    private final FavoriteGroupMapper favoriteGroupMapper;
    private final PriceAlertMapper priceAlertMapper;
    private final ProductMapper productMapper;

    private FavoriteVO buildFavoriteVO(Favorite fav, Product p, PriceAlert alert) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(fav.getId());
        vo.setFavoriteId(fav.getId());
        vo.setProductId(p.getId());
        vo.setGroupId(fav.getGroupId());
        vo.setProductName(p.getName());
        vo.setProductImage(p.getMainImage());
        vo.setCategoryId(p.getCategoryId());
        vo.setFavorPrice(fav.getFavorPrice());
        vo.setCurrentPrice(p.getPrice());
        vo.setStock(p.getStock());
        vo.setStatus(p.getStatus());
        vo.setOffShelf(p.getStatus() != null && p.getStatus() == 0);
        vo.setCreatedAt(fav.getCreatedAt());

        BigDecimal diff = p.getPrice().subtract(fav.getFavorPrice());
        vo.setPriceChange(diff);
        if (fav.getFavorPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percent = diff.divide(fav.getFavorPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            vo.setPriceChangePercent(percent);
            vo.setPriceDownFlag(percent.negate().compareTo(PRICE_DOWN_THRESHOLD.multiply(new BigDecimal("100"))) >= 0);
        } else {
            vo.setPriceChangePercent(BigDecimal.ZERO);
            vo.setPriceDownFlag(false);
        }

        if (alert != null) {
            vo.setHasPriceAlert(true);
            vo.setAlertTargetPrice(alert.getTargetPrice());
        } else {
            vo.setHasPriceAlert(false);
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, Long productId, Long groupId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        Favorite exist = favoriteMapper.selectByUserIdAndProductId(userId, productId);
        if (exist != null) {
            throw new IllegalArgumentException("商品已在收藏夹中");
        }
        if (groupId != null) {
            FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new IllegalArgumentException("分组不存在");
            }
        }
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setProductId(productId);
        fav.setGroupId(groupId);
        fav.setFavorPrice(product.getPrice());
        favoriteMapper.insert(fav);
        log.debug("Favorite added: userId={}, productId={}", userId, productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, Long productId) {
        favoriteMapper.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveFavorite(Long userId, List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        favoriteMapper.deleteBatchByUserIdAndProductIds(userId, productIds);
    }

    public FavoriteCountVO getCount(Long userId) {
        return new FavoriteCountVO(favoriteMapper.countByUserId(userId));
    }

    public Boolean isFavorited(Long userId, Long productId) {
        if (userId == null) return false;
        Favorite fav = favoriteMapper.selectByUserIdAndProductId(userId, productId);
        return fav != null;
    }

    public List<FavoriteVO> listFavorites(Long userId, Long groupId, Boolean ungrouped) {
        List<Favorite> favs;
        if (ungrouped != null && ungrouped) {
            favs = favoriteMapper.selectByUserIdAndGroupId(userId, null);
        } else if (groupId != null) {
            FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new IllegalArgumentException("分组不存在");
            }
            favs = favoriteMapper.selectByUserIdAndGroupId(userId, groupId);
        } else {
            favs = favoriteMapper.selectByUserId(userId);
        }

        Map<Long, PriceAlert> alertMap = new HashMap<>();
        List<PriceAlert> activeAlerts = priceAlertMapper.selectByUserId(userId);
        for (PriceAlert a : activeAlerts) {
            if (a.getStatus() != null && a.getStatus() == 0) {
                alertMap.put(a.getProductId(), a);
            }
        }

        List<FavoriteVO> result = new ArrayList<>();
        for (Favorite fav : favs) {
            Product p = productMapper.selectById(fav.getProductId());
            if (p == null) continue;
            result.add(buildFavoriteVO(fav, p, alertMap.get(fav.getProductId())));
        }
        return result;
    }

    public List<FavoriteGroupVO> listGroups(Long userId) {
        List<FavoriteGroup> groups = favoriteGroupMapper.selectByUserId(userId);
        List<FavoriteGroupVO> result = new ArrayList<>();
        for (FavoriteGroup g : groups) {
            FavoriteGroupVO vo = new FavoriteGroupVO();
            vo.setId(g.getId());
            vo.setName(g.getName());
            vo.setSortOrder(g.getSortOrder());
            vo.setCreatedAt(g.getCreatedAt());
            vo.setCount(favoriteMapper.countByUserIdAndGroupId(userId, g.getId()));
            result.add(vo);
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public FavoriteGroupVO createGroup(Long userId, String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        String trimmed = name.trim();
        FavoriteGroup exist = favoriteGroupMapper.selectByUserIdAndName(userId, trimmed);
        if (exist != null) {
            throw new IllegalArgumentException("分组名称已存在");
        }
        FavoriteGroup group = new FavoriteGroup();
        group.setUserId(userId);
        group.setName(trimmed);
        group.setSortOrder(0);
        favoriteGroupMapper.insert(group);

        FavoriteGroupVO vo = new FavoriteGroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setSortOrder(group.getSortOrder());
        vo.setCreatedAt(group.getCreatedAt());
        vo.setCount(0);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long userId, Long groupId, String name) {
        FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分组不存在");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        String trimmed = name.trim();
        FavoriteGroup exist = favoriteGroupMapper.selectByUserIdAndName(userId, trimmed);
        if (exist != null && !exist.getId().equals(groupId)) {
            throw new IllegalArgumentException("分组名称已存在");
        }
        group.setName(trimmed);
        favoriteGroupMapper.updateById(group);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long userId, Long groupId) {
        FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
        if (group == null || !group.getUserId().equals(userId)) {
            throw new IllegalArgumentException("分组不存在");
        }
        favoriteMapper.clearGroupIdByGroupId(groupId);
        favoriteGroupMapper.deleteById(groupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void moveToGroup(Long userId, Long productId, Long groupId) {
        Favorite fav = favoriteMapper.selectByUserIdAndProductId(userId, productId);
        if (fav == null) {
            throw new IllegalArgumentException("未收藏该商品");
        }
        if (groupId != null) {
            FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new IllegalArgumentException("分组不存在");
            }
        }
        favoriteMapper.updateGroupId(userId, productId, groupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchMoveToGroup(Long userId, FavoriteBatchRequest req) {
        if (req == null || req.getProductIds() == null || req.getProductIds().isEmpty()) return;
        Long groupId = req.getGroupId();
        if (groupId != null) {
            FavoriteGroup group = favoriteGroupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new IllegalArgumentException("分组不存在");
            }
        }
        favoriteMapper.updateBatchGroupId(userId, req.getProductIds(), groupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setPriceAlert(Long userId, Long productId, BigDecimal targetPrice) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (targetPrice == null || targetPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("目标价格必须大于0");
        }
        PriceAlert exist = priceAlertMapper.selectActiveByUserIdAndProductId(userId, productId);
        if (exist != null) {
            exist.setTargetPrice(targetPrice);
            priceAlertMapper.updateById(exist);
        } else {
            PriceAlert alert = new PriceAlert();
            alert.setUserId(userId);
            alert.setProductId(productId);
            alert.setTargetPrice(targetPrice);
            alert.setStatus(0);
            priceAlertMapper.insert(alert);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelPriceAlert(Long userId, Long productId) {
        priceAlertMapper.cancelByUserIdAndProductId(userId, productId);
    }

    public List<PriceAlertVO> listPriceAlerts(Long userId) {
        List<PriceAlert> alerts = priceAlertMapper.selectByUserId(userId);
        List<PriceAlertVO> result = new ArrayList<>();
        for (PriceAlert a : alerts) {
            Product p = productMapper.selectById(a.getProductId());
            PriceAlertVO vo = new PriceAlertVO();
            vo.setId(a.getId());
            vo.setProductId(a.getProductId());
            vo.setCurrentPrice(p != null ? p.getPrice() : null);
            vo.setTargetPrice(a.getTargetPrice());
            vo.setStatus(a.getStatus());
            vo.setTriggeredAt(a.getTriggeredAt());
            vo.setCreatedAt(a.getCreatedAt());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setProductImage(p.getMainImage());
            }
            result.add(vo);
        }
        return result;
    }

    public PriceAlertVO getActivePriceAlert(Long userId, Long productId) {
        PriceAlert alert = priceAlertMapper.selectActiveByUserIdAndProductId(userId, productId);
        if (alert == null) return null;
        Product p = productMapper.selectById(productId);
        PriceAlertVO vo = new PriceAlertVO();
        vo.setId(alert.getId());
        vo.setProductId(productId);
        vo.setCurrentPrice(p != null ? p.getPrice() : null);
        vo.setTargetPrice(alert.getTargetPrice());
        vo.setStatus(alert.getStatus());
        vo.setTriggeredAt(alert.getTriggeredAt());
        vo.setCreatedAt(alert.getCreatedAt());
        if (p != null) {
            vo.setProductName(p.getName());
            vo.setProductImage(p.getMainImage());
        }
        return vo;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void scanPriceAlerts() {
        List<PriceAlert> alerts = priceAlertMapper.selectAllActive();
        int triggered = 0;
        for (PriceAlert alert : alerts) {
            Product p = productMapper.selectById(alert.getProductId());
            if (p != null && p.getPrice() != null && alert.getTargetPrice() != null) {
                if (p.getPrice().compareTo(alert.getTargetPrice()) <= 0) {
                    priceAlertMapper.triggerById(alert.getId());
                    triggered++;
                    log.info("Price alert triggered: id={}, userId={}, productId={}, target={}, current={}",
                            alert.getId(), alert.getUserId(), alert.getProductId(),
                            alert.getTargetPrice(), p.getPrice());
                }
            }
        }
        if (triggered > 0) {
            log.info("Price alert scan completed, triggered={}", triggered);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int manualTriggerAlertsForProduct(Long productId) {
        List<PriceAlert> alerts = priceAlertMapper.selectAllActive();
        int count = 0;
        Product p = productMapper.selectById(productId);
        if (p == null) return 0;
        for (PriceAlert alert : alerts) {
            if (!alert.getProductId().equals(productId)) continue;
            if (p.getPrice() != null && alert.getTargetPrice() != null
                    && p.getPrice().compareTo(alert.getTargetPrice()) <= 0) {
                priceAlertMapper.triggerById(alert.getId());
                count++;
            }
        }
        return count;
    }
}
