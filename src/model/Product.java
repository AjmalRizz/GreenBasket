package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A product sold in the supermarket.
 *
 * Price uses BigDecimal instead of double because double cannot store
 * values like 0.10 exactly, which causes rounding errors with money.
 *
 * This class only holds data. Validation (no negative prices, etc.)
 * is done in ProductService so the rules live in one place.
 */
public class Product implements Serializable {

    private static final long serialVersionUID = 3L;

    private String productId;
    private String name;
    private String category;
    private String supplier;
    private BigDecimal price;
    private int quantity;
    private int reorderLevel;

    public Product(String productId, String name, String category, String supplier,
                   BigDecimal price, int quantity, int reorderLevel) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.supplier = supplier;
        this.price = (price == null) ? null : price.setScale(2, RoundingMode.HALF_UP);
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getPrice() {
        return price;
    }

    /** Stores the price rounded to 2 decimal places (cents). */
    public void setPrice(BigDecimal price) {
        this.price = (price == null) ? null : price.setScale(2, RoundingMode.HALF_UP);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }

    public boolean isOutOfStock() {
        return quantity <= 0;
    }

    /** Total value of this product's stock = price x quantity. */
    public BigDecimal calculateStockValue() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /** Text label used in tables and CSV exports. */
    public String getStockStatus() {
        if (isOutOfStock()) return "OUT OF STOCK";
        if (isLowStock()) return "LOW STOCK";
        return "IN STOCK";
    }

    @Override
    public String toString() {
        return "[" + productId + "] " + name + " (" + category + ") - Qty: " + quantity + " @ LKR " + price;
    }
}