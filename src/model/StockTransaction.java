package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * One line in the audit trail: a record of a stock movement
 * (RESTOCK, SALE or ADJUSTMENT). Once created it never changes.
 */
public class StockTransaction implements Serializable {

    private static final long serialVersionUID = 3L;

    public enum Type {
        RESTOCK, SALE, ADJUSTMENT
    }

    private final String transactionId;
    private final String productId;
    private final String productName;
    private final Type type;
    private final int quantityChanged;
    private final int resultingQuantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final Date timestamp;
    private final String performedBy;
    private final String notes;

    public StockTransaction(String transactionId, String productId, String productName,
                            Type type, int quantityChanged, int resultingQuantity,
                            BigDecimal unitPrice, String performedBy, String notes) {
        this.transactionId = transactionId;
        this.productId = productId;
        this.productName = productName;
        this.type = type;
        this.quantityChanged = quantityChanged;
        this.resultingQuantity = resultingQuantity;
        this.unitPrice = unitPrice;
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(Math.abs(quantityChanged)));
        this.timestamp = new Date();
        this.performedBy = performedBy;
        this.notes = notes;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Type getType() {
        return type;
    }

    public int getQuantityChanged() {
        return quantityChanged;
    }

    public int getResultingQuantity() {
        return resultingQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public String getNotes() {
        return notes;
    }
}
