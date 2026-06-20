package com.shop.service;

import com.shop.dto.InvoiceCreateRequest;
import com.shop.dto.InvoiceUpdateRequest;
import com.shop.dto.InvoiceVO;
import com.shop.entity.InvoiceRequest;
import com.shop.entity.OrderItem;
import com.shop.entity.OrderMain;
import com.shop.mapper.InvoiceRequestMapper;
import com.shop.mapper.OrderItemMapper;
import com.shop.mapper.OrderMainMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private static final Pattern TAX_NUMBER_PATTERN = Pattern.compile("^[A-HJ-NP-RT-UW-Y0-9]{15,20}$");

    private final InvoiceRequestMapper invoiceRequestMapper;
    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;

    @Transactional(rollbackFor = Exception.class)
    public InvoiceRequest create(Long userId, InvoiceCreateRequest req) {
        OrderMain order = orderMainMapper.selectById(req.getOrderId());
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该订单");
        }
        if (order.getStatus() != 3) {
            throw new IllegalArgumentException("仅已完成订单可申请发票");
        }

        InvoiceRequest existing = invoiceRequestMapper.selectByOrderId(req.getOrderId());
        if (existing != null) {
            throw new IllegalArgumentException("该订单已申请过发票");
        }

        validateInvoiceRequest(req.getInvoiceType(), req.getTitle(), req.getTaxNumber());

        InvoiceRequest invoice = new InvoiceRequest();
        invoice.setOrderId(req.getOrderId());
        invoice.setUserId(userId);
        invoice.setInvoiceType(req.getInvoiceType());
        invoice.setTitle(req.getTitle());
        invoice.setTaxNumber(req.getTaxNumber());
        invoice.setBankName(req.getBankName());
        invoice.setBankAccount(req.getBankAccount());
        invoice.setAddress(req.getAddress());
        invoice.setPhone(req.getPhone());
        invoice.setReceiveEmail(req.getReceiveEmail());
        invoice.setStatus(0);

        invoiceRequestMapper.insert(invoice);
        log.info("Invoice request created: id={}, orderId={}, userId={}", invoice.getId(), req.getOrderId(), userId);
        return invoiceRequestMapper.selectById(invoice.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public InvoiceRequest update(Long id, Long userId, InvoiceUpdateRequest req) {
        InvoiceRequest invoice = invoiceRequestMapper.selectById(id);
        if (invoice == null) {
            throw new IllegalArgumentException("发票申请不存在");
        }
        if (!invoice.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该发票申请");
        }
        if (invoice.getStatus() != 2) {
            throw new IllegalArgumentException("仅已驳回的发票申请可修改");
        }

        validateInvoiceRequest(req.getInvoiceType(), req.getTitle(), req.getTaxNumber());

        invoice.setInvoiceType(req.getInvoiceType());
        invoice.setTitle(req.getTitle());
        invoice.setTaxNumber(req.getTaxNumber());
        invoice.setBankName(req.getBankName());
        invoice.setBankAccount(req.getBankAccount());
        invoice.setAddress(req.getAddress());
        invoice.setPhone(req.getPhone());
        invoice.setReceiveEmail(req.getReceiveEmail());
        invoice.setStatus(0);
        invoice.setRejectReason(null);
        invoice.setAdminId(null);
        invoice.setAdminName(null);
        invoice.setReviewedAt(null);

        invoiceRequestMapper.update(invoice);
        log.info("Invoice request updated: id={}, userId={}", id, userId);
        return invoiceRequestMapper.selectById(id);
    }

    public List<InvoiceRequest> listByUserId(Long userId) {
        return invoiceRequestMapper.selectByUserId(userId);
    }

    public InvoiceVO getDetail(Long id, Long userId) {
        InvoiceRequest invoice = invoiceRequestMapper.selectById(id);
        if (invoice == null) {
            return null;
        }
        if (userId != null && !invoice.getUserId().equals(userId)) {
            return null;
        }
        return convertToVO(invoice);
    }

    public InvoiceRequest getByOrderId(Long orderId, Long userId) {
        InvoiceRequest invoice = invoiceRequestMapper.selectByOrderId(orderId);
        if (invoice == null) {
            return null;
        }
        if (!invoice.getUserId().equals(userId)) {
            return null;
        }
        return invoice;
    }

    public List<InvoiceRequest> listAll(Integer status) {
        return invoiceRequestMapper.selectAll(status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, String invoiceNumber, Long adminId, String adminName) {
        InvoiceRequest invoice = invoiceRequestMapper.selectById(id);
        if (invoice == null) {
            throw new IllegalArgumentException("发票申请不存在");
        }
        if (invoice.getStatus() != 0) {
            throw new IllegalArgumentException("仅待开票的申请可审核");
        }
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("发票号码不能为空");
        }

        invoiceRequestMapper.approve(id, invoiceNumber.trim(), adminId, adminName, LocalDateTime.now());
        log.info("Invoice request approved: id={}, invoiceNumber={}, adminId={}", id, invoiceNumber, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String rejectReason, Long adminId, String adminName) {
        InvoiceRequest invoice = invoiceRequestMapper.selectById(id);
        if (invoice == null) {
            throw new IllegalArgumentException("发票申请不存在");
        }
        if (invoice.getStatus() != 0) {
            throw new IllegalArgumentException("仅待开票的申请可审核");
        }
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }

        invoiceRequestMapper.reject(id, rejectReason.trim(), adminId, adminName, LocalDateTime.now());
        log.info("Invoice request rejected: id={}, adminId={}", id, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateInvoiceNumber(Long id, String invoiceNumber) {
        InvoiceRequest invoice = invoiceRequestMapper.selectById(id);
        if (invoice == null) {
            throw new IllegalArgumentException("发票申请不存在");
        }
        if (invoice.getStatus() != 1) {
            throw new IllegalArgumentException("仅已开票的申请可修改发票号码");
        }
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("发票号码不能为空");
        }

        invoiceRequestMapper.updateInvoiceNumber(id, invoiceNumber.trim());
        log.info("Invoice number updated: id={}, invoiceNumber={}", id, invoiceNumber);
    }

    public String exportCsv(List<Long> ids) {
        List<InvoiceRequest> invoices;
        if (ids == null || ids.isEmpty()) {
            invoices = invoiceRequestMapper.selectAll(null);
        } else {
            invoices = invoiceRequestMapper.selectByIds(ids);
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("ID,订单号,发票类型,抬头,税号,开户行,银行账号,企业地址,企业电话,接收邮箱,发票号码,状态,申请时间,审核时间,审核人");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (InvoiceRequest inv : invoices) {
            OrderMain order = orderMainMapper.selectById(inv.getOrderId());
            String orderNo = order != null ? order.getOrderNo() : "";
            String type = "enterprise".equals(inv.getInvoiceType()) ? "企业" : "个人";
            String status = switch (inv.getStatus()) {
                case 0 -> "待开票";
                case 1 -> "已开票";
                case 2 -> "已驳回";
                default -> "未知";
            };
            String createdAt = inv.getCreatedAt() != null ? inv.getCreatedAt().format(formatter) : "";
            String reviewedAt = inv.getReviewedAt() != null ? inv.getReviewedAt().format(formatter) : "";

            pw.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    inv.getId(),
                    escapeCsv(orderNo),
                    escapeCsv(type),
                    escapeCsv(inv.getTitle()),
                    escapeCsv(inv.getTaxNumber()),
                    escapeCsv(inv.getBankName()),
                    escapeCsv(inv.getBankAccount()),
                    escapeCsv(inv.getAddress()),
                    escapeCsv(inv.getPhone()),
                    escapeCsv(inv.getReceiveEmail()),
                    escapeCsv(inv.getInvoiceNumber()),
                    status,
                    createdAt,
                    reviewedAt,
                    escapeCsv(inv.getAdminName())
            );
        }

        pw.flush();
        return sw.toString();
    }

    private void validateInvoiceRequest(String invoiceType, String title, String taxNumber) {
        if (!"personal".equals(invoiceType) && !"enterprise".equals(invoiceType)) {
            throw new IllegalArgumentException("发票类型不正确");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("发票抬头不能为空");
        }

        if ("enterprise".equals(invoiceType)) {
            if (taxNumber == null || taxNumber.trim().isEmpty()) {
                throw new IllegalArgumentException("企业发票税号不能为空");
            }
            if (!isValidTaxNumber(taxNumber.trim())) {
                throw new IllegalArgumentException("企业税号格式不正确，应为15-20位大写字母和数字");
            }
        }
    }

    private boolean isValidTaxNumber(String taxNumber) {
        return TAX_NUMBER_PATTERN.matcher(taxNumber).matches();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private InvoiceVO convertToVO(InvoiceRequest invoice) {
        InvoiceVO vo = new InvoiceVO();
        vo.setId(invoice.getId());
        vo.setOrderId(invoice.getOrderId());
        vo.setUserId(invoice.getUserId());
        vo.setInvoiceType(invoice.getInvoiceType());
        vo.setTitle(invoice.getTitle());
        vo.setTaxNumber(invoice.getTaxNumber());
        vo.setBankName(invoice.getBankName());
        vo.setBankAccount(invoice.getBankAccount());
        vo.setAddress(invoice.getAddress());
        vo.setPhone(invoice.getPhone());
        vo.setReceiveEmail(invoice.getReceiveEmail());
        vo.setInvoiceNumber(invoice.getInvoiceNumber());
        vo.setStatus(invoice.getStatus());
        vo.setRejectReason(invoice.getRejectReason());
        vo.setAdminId(invoice.getAdminId());
        vo.setAdminName(invoice.getAdminName());
        vo.setReviewedAt(invoice.getReviewedAt());
        vo.setCreatedAt(invoice.getCreatedAt());
        vo.setUpdatedAt(invoice.getUpdatedAt());

        OrderMain order = orderMainMapper.selectById(invoice.getOrderId());
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setOrderAmount(order.getTotalAmount());
            List<OrderItem> items = orderItemMapper.selectByOrderId(invoice.getOrderId());
            vo.setOrderItems(items);
        }

        return vo;
    }

    public List<InvoiceVO> convertToVOList(List<InvoiceRequest> invoices) {
        List<InvoiceVO> result = new ArrayList<>();
        for (InvoiceRequest inv : invoices) {
            result.add(convertToVO(inv));
        }
        return result;
    }
}
