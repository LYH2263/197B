package com.shop.controller;

import com.shop.common.Result;
import com.shop.dto.*;
import com.shop.entity.AfterSale;
import com.shop.entity.DeliveryIssue;
import com.shop.entity.DeliveryUrge;
import com.shop.entity.InvoiceRequest;
import com.shop.entity.OrderMain;
import com.shop.entity.PointsExchangeOrder;
import com.shop.entity.PointsProduct;
import com.shop.entity.Shipment;
import com.shop.entity.User;
import com.shop.mapper.UserMapper;
import com.shop.dto.ViewHistoryTop10VO;
import com.shop.service.AdminService;
import com.shop.service.AfterSaleService;
import com.shop.service.InvoiceService;
import com.shop.service.OrderService;
import com.shop.service.PointsExchangeService;
import com.shop.service.PointsLevelService;
import com.shop.service.ShipmentService;
import com.shop.service.ViewHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AfterSaleService afterSaleService;
    private final UserMapper userMapper;
    private final OrderService orderService;
    private final PointsLevelService pointsLevelService;
    private final PointsExchangeService pointsExchangeService;
    private final ShipmentService shipmentService;
    private final ViewHistoryService viewHistoryService;
    private final InvoiceService invoiceService;

    private String getAdminName(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return "管理员";
        Long id = (Long) auth.getPrincipal();
        User u = userMapper.selectById(id);
        if (u == null) return "管理员";
        return u.getNickname() != null ? u.getNickname() : u.getUsername();
    }

    private Long getAdminId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return null;
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/users")
    public Result<List<UserVO>> listUsers() {
        return Result.ok(adminService.listUsers());
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = body.get("status") != null ? ((Number) body.get("status")).intValue() : null;
        String role = body.get("role") != null ? String.valueOf(body.get("role")) : null;
        adminService.updateUser(id, status, role);
        return Result.ok();
    }

    @PostMapping("/users/{id}/points")
    public Result<Void> adjustPoints(Authentication auth, @PathVariable Long id, @RequestBody AdminPointsAdjustRequest req) {
        if (req.getChangePoints() == null) {
            return Result.fail(400, "变动积分不能为空");
        }
        Long adminId = getAdminId(auth);
        pointsLevelService.adjustPoints(id, req.getChangePoints(), req.getRemark(), adminId);
        return Result.ok();
    }

    @PostMapping("/users/{id}/level")
    public Result<Void> adjustLevel(@PathVariable Long id, @RequestBody AdminLevelAdjustRequest req) {
        if (req.getLevel() == null) {
            return Result.fail(400, "等级不能为空");
        }
        pointsLevelService.adjustLevel(id, req.getLevel());
        return Result.ok();
    }

    @GetMapping("/reviews")
    public Result<List<ReviewVO>> listReviews() {
        return Result.ok(adminService.listAllReviews());
    }

    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return Result.ok();
    }

    @GetMapping("/orders")
    public Result<List<OrderMain>> listOrders() {
        return Result.ok(orderService.listAll());
    }

    @GetMapping("/express-companies")
    public Result<List<ExpressCompanyVO>> listExpressCompanies() {
        return Result.ok(shipmentService.listExpressCompanies());
    }

    @PostMapping("/orders/{id}/ship")
    public Result<Shipment> shipOrder(@PathVariable Long id, @Valid @RequestBody ShipmentCreateRequest req) {
        req.setOrderId(id);
        Shipment shipment = shipmentService.createShipment(req);
        return Result.ok(shipment);
    }

    @GetMapping("/shipments")
    public Result<List<Shipment>> listShipments() {
        return Result.ok(shipmentService.listAllShipments());
    }

    @GetMapping("/shipments/{id}")
    public Result<ShipmentDetailVO> getShipmentDetail(@PathVariable Long id) {
        ShipmentDetailVO vo = shipmentService.getAdminShipmentDetail(id);
        if (vo == null) return Result.fail(404, "物流不存在");
        return Result.ok(vo);
    }

    @GetMapping("/delivery-urges")
    public Result<List<DeliveryUrge>> listDeliveryUrges(@RequestParam(required = false) Integer handled) {
        return Result.ok(shipmentService.listAllUrges(handled));
    }

    @PostMapping("/delivery-urges/{id}/handle")
    public Result<Void> handleUrge(Authentication auth, @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long adminId = getAdminId(auth);
        String remark = body != null && body.get("remark") != null ? String.valueOf(body.get("remark")) : null;
        shipmentService.handleUrge(id, adminId, remark);
        return Result.ok();
    }

    @GetMapping("/delivery-issues")
    public Result<List<DeliveryIssue>> listDeliveryIssues(@RequestParam(required = false) Integer status) {
        return Result.ok(shipmentService.listAllDeliveryIssues(status));
    }

    @GetMapping("/delivery-issues/{id}")
    public Result<DeliveryIssue> getDeliveryIssue(@PathVariable Long id) {
        DeliveryIssue issue = shipmentService.getDeliveryIssueById(id);
        if (issue == null) return Result.fail(404, "异常记录不存在");
        return Result.ok(issue);
    }

    @PostMapping("/delivery-issues/{id}/handle")
    public Result<Void> handleDeliveryIssue(Authentication auth, @PathVariable Long id, @RequestBody DeliveryIssueHandleRequest req) {
        Long adminId = getAdminId(auth);
        shipmentService.handleDeliveryIssue(id, req, adminId);
        return Result.ok();
    }

    @GetMapping("/after-sale")
    public Result<List<AfterSale>> listAfterSale(@RequestParam(required = false) Integer status) {
        return Result.ok(afterSaleService.listAll(status));
    }

    @GetMapping("/after-sale/{id}")
    public Result<AfterSaleDetailVO> afterSaleDetail(@PathVariable Long id) {
        AfterSaleDetailVO vo = afterSaleService.getAdminDetail(id);
        if (vo == null) return Result.fail(404, "售后不存在");
        return Result.ok(vo);
    }

    @PostMapping("/after-sale/{id}/approve")
    public Result<Void> approveAfterSale(Authentication auth, @PathVariable Long id, @RequestBody(required = false) AfterSaleReviewRequest req) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        BigDecimal refundAmount = req != null ? req.getRefundAmount() : null;
        afterSaleService.approve(id, refundAmount, adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/after-sale/{id}/reject")
    public Result<Void> rejectAfterSale(Authentication auth, @PathVariable Long id, @RequestBody AfterSaleReviewRequest req) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        afterSaleService.reject(id, req.getRejectReason(), adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/after-sale/{id}/warehouse-confirm")
    public Result<Void> warehouseConfirm(Authentication auth, @PathVariable Long id) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        afterSaleService.warehouseConfirm(id, adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/after-sale/{id}/ship-exchange")
    public Result<Void> shipExchange(Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        String company = body.get("company") != null ? String.valueOf(body.get("company")) : null;
        String trackingNo = body.get("trackingNo") != null ? String.valueOf(body.get("trackingNo")) : null;
        BigDecimal priceDiff = null;
        if (body.get("priceDiff") != null) {
            priceDiff = new BigDecimal(String.valueOf(body.get("priceDiff"));
        }
        afterSaleService.adminShipExchange(id, company, trackingNo, priceDiff, adminId, adminName);
        return Result.ok();
    }

    @GetMapping("/points-products")
    public Result<List<PointsProduct>> listPointsProducts() {
        return Result.ok(pointsExchangeService.listAllProducts());
    }

    @PostMapping("/points-products")
    public Result<PointsProduct> createPointsProduct(@RequestBody PointsProduct product) {
        return Result.ok(pointsExchangeService.createProduct(product));
    }

    @PutMapping("/points-products/{id}")
    public Result<PointsProduct> updatePointsProduct(@PathVariable Long id, @RequestBody PointsProduct product) {
        product.setId(id);
        return Result.ok(pointsExchangeService.updateProduct(product));
    }

    @GetMapping("/exchange-orders")
    public Result<List<PointsExchangeOrder>> listExchangeOrders() {
        return Result.ok(pointsExchangeService.listAllOrders());
    }

    @GetMapping("/exchange-orders/{id}")
    public Result<PointsExchangeOrder> getExchangeOrder(@PathVariable Long id) {
        PointsExchangeOrder order = pointsExchangeService.getAdminOrder(id);
        if (order == null) return Result.fail(404, "兑换单不存在");
        return Result.ok(order);
    }

    @PostMapping("/exchange-orders/{id}/ship")
    public Result<Void> shipExchangeOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String company = body.get("company") != null ? String.valueOf(body.get("company")) : null;
        String no = body.get("expressNo") != null ? String.valueOf(body.get("expressNo")) : null;
        pointsExchangeService.shipOrder(id, company, no);
        return Result.ok();
    }

    @GetMapping("/view-history/top10")
    public Result<List<ViewHistoryTop10VO>> getViewHistoryTop10() {
        return Result.ok(viewHistoryService.getTop10ProductsLast7Days());
    }

    @GetMapping("/invoices")
    public Result<List<InvoiceVO>> listInvoices(@RequestParam(required = false) Integer status) {
        return Result.ok(invoiceService.convertToVOList(invoiceService.listAll(status)));
    }

    @GetMapping("/invoices/{id}")
    public Result<InvoiceVO> getInvoiceDetail(@PathVariable Long id) {
        InvoiceVO vo = invoiceService.getDetail(id, null);
        if (vo == null) return Result.fail(404, "发票申请不存在");
        return Result.ok(vo);
    }

    @PostMapping("/invoices/{id}/approve")
    public Result<Void> approveInvoice(Authentication auth, @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        String invoiceNumber = body != null && body.get("invoiceNumber") != null ? String.valueOf(body.get("invoiceNumber")) : null;
        invoiceService.approve(id, invoiceNumber, adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/invoices/{id}/reject")
    public Result<Void> rejectInvoice(Authentication auth, @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        Long adminId = getAdminId(auth);
        String adminName = getAdminName(auth);
        String rejectReason = body != null && body.get("rejectReason") != null ? String.valueOf(body.get("rejectReason")) : null;
        invoiceService.reject(id, rejectReason, adminId, adminName);
        return Result.ok();
    }

    @PostMapping("/invoices/{id}/invoice-number")
    public Result<Void> updateInvoiceNumber(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String invoiceNumber = body.get("invoiceNumber") != null ? String.valueOf(body.get("invoiceNumber")) : null;
        invoiceService.updateInvoiceNumber(id, invoiceNumber);
        return Result.ok();
    }

    @GetMapping("/invoices/export")
    public ResponseEntity<byte[]> exportInvoices(@RequestParam(required = false) List<Long> ids) {
        String csv = invoiceService.exportCsv(ids);
        String filename = "invoices_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=utf-8"));
        headers.setContentDispositionFormData("attachment", filename);
        byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] content = csv.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);
        return ResponseEntity.ok().headers(headers).body(result);
    }
}
