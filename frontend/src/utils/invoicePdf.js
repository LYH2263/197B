import { jsPDF } from 'jspdf'

export function generateInvoicePdf(invoice) {
  const doc = new jsPDF()
  const pageWidth = doc.internal.pageSize.getWidth()
  const margin = 20
  let y = margin

  doc.setFontSize(20)
  doc.setFont('helvetica', 'bold')
  doc.text('电子发票', pageWidth / 2, y, { align: 'center' })
  y += 15

  doc.setFontSize(12)
  doc.setFont('helvetica', 'normal')
  doc.text(`发票号码: ${invoice.invoiceNumber || '-'}`, margin, y)
  y += 10
  doc.text(`申请日期: ${formatDate(invoice.createdAt)}`, margin, y)
  y += 10
  doc.text(`订单号: ${invoice.orderNo || '-'}`, margin, y)
  y += 15

  doc.setFontSize(14)
  doc.setFont('helvetica', 'bold')
  doc.text('发票信息', margin, y)
  y += 10

  doc.setFontSize(11)
  doc.setFont('helvetica', 'normal')
  const typeText = invoice.invoiceType === 'enterprise' ? '企业' : '个人'
  doc.text(`发票类型: ${typeText}`, margin, y)
  y += 8
  doc.text(`发票抬头: ${invoice.title}`, margin, y)
  y += 8
  if (invoice.taxNumber) {
    doc.text(`税号: ${invoice.taxNumber}`, margin, y)
    y += 8
  }
  if (invoice.bankName) {
    doc.text(`开户行: ${invoice.bankName}`, margin, y)
    y += 8
  }
  if (invoice.bankAccount) {
    doc.text(`银行账号: ${invoice.bankAccount}`, margin, y)
    y += 8
  }
  if (invoice.address) {
    doc.text(`企业地址: ${invoice.address}`, margin, y)
    y += 8
  }
  if (invoice.phone) {
    doc.text(`企业电话: ${invoice.phone}`, margin, y)
    y += 8
  }
  doc.text(`接收邮箱: ${invoice.receiveEmail}`, margin, y)
  y += 15

  doc.setFontSize(14)
  doc.setFont('helvetica', 'bold')
  doc.text('订单明细', margin, y)
  y += 10

  if (invoice.orderItems && invoice.orderItems.length > 0) {
    const colWidths = [80, 30, 30, 30]
    const startX = margin

    doc.setFontSize(10)
    doc.setFont('helvetica', 'bold')
    doc.text('商品名称', startX, y)
    doc.text('单价', startX + colWidths[0], y)
    doc.text('数量', startX + colWidths[0] + colWidths[1], y)
    doc.text('小计', startX + colWidths[0] + colWidths[1] + colWidths[2], y)
    y += 6

    doc.line(margin, y, pageWidth - margin, y)
    y += 4

    doc.setFont('helvetica', 'normal')
    invoice.orderItems.forEach((item) => {
      if (y > 270) {
        doc.addPage()
        y = margin
      }
      const name = item.productName || ''
      const lines = doc.splitTextToSize(name, colWidths[0] - 2)
      doc.text(lines, startX, y)
      doc.text(`¥${item.price}`, startX + colWidths[0], y)
      doc.text(`${item.quantity}`, startX + colWidths[0] + colWidths[1], y)
      doc.text(`¥${item.totalAmount}`, startX + colWidths[0] + colWidths[1] + colWidths[2], y)
      y += lines.length * 5 + 3
    })

    doc.line(margin, y, pageWidth - margin, y)
    y += 8

    doc.setFont('helvetica', 'bold')
    doc.text(`订单总金额: ¥${invoice.orderAmount}`, pageWidth - margin - 50, y, { align: 'right' })
    y += 15
  }

  if (invoice.rejectReason) {
    doc.setFontSize(12)
    doc.setFont('helvetica', 'bold')
    doc.setTextColor(255, 0, 0)
    doc.text(`驳回原因: ${invoice.rejectReason}`, margin, y)
    doc.setTextColor(0, 0, 0)
    y += 10
  }

  if (invoice.adminName) {
    doc.setFontSize(10)
    doc.setFont('helvetica', 'normal')
    doc.text(`审核人: ${invoice.adminName}`, margin, y)
    y += 6
  }
  if (invoice.reviewedAt) {
    doc.text(`审核时间: ${formatDate(invoice.reviewedAt)}`, margin, y)
  }

  const statusText = { 0: '待开票', 1: '已开票', 2: '已驳回' }[invoice.status] || '未知'
  doc.setFontSize(10)
  doc.text(`状态: ${statusText}`, pageWidth - margin, 287, { align: 'right' })

  const fileName = `发票_${invoice.orderNo || invoice.id}_${formatDate(invoice.createdAt).replace(/\//g, '')}.pdf`
  doc.save(fileName)
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
