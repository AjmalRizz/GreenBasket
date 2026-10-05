package service;

import model.Product;
import model.StockTransaction;
import model.User;
import util.FileHandler;
import util.SecurityUtil;
import util.ValidationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Business logic for products: add / edit / delete, stock in and out,
 * search, and totals.
 *
 * Every method that changes data takes the logged-in User and checks their
 * permission here, not only in the UI. Hiding a button is not security -
 * the rule must be enforced where the data is changed.
 */
public class ProductService {

    public static final String DEFAULT_FILE = "products.dat";

    private final String fileName;
    private final TransactionService transactionService;
    private final List<Product> productList;

    public ProductService() {
        this(DEFAULT_FILE, new TransactionService());
    }

    /** @param fileName where products are stored (tests pass a temp file) */
    public ProductService(String fileName, TransactionService transactionService) {
        this.fileName = fileName;
        this.transactionService = transactionService;
        this.productList = FileHandler.loadFromFile(fileName);
        if (productList.isEmpty()) {
            addSampleProducts();
        }
    }

    /** Sample catalogue so the app has something to show on first run. */
    private void addSampleProducts() {
        productList.add(new Product("GB-101", "Organic Hass Avocado", "Fresh Produce", "Green Valley Farms", new BigDecimal("250.00"), 45, 10));
        productList.add(new Product("GB-102", "Farm Fresh Free-Range Eggs 10pk", "Dairy", "Sunnyside Organic Farm", new BigDecimal("680.00"), 30, 8));
        productList.add(new Product("GB-103", "Ceylon Organic Green Tea 100g", "Beverages", "Highland Tea Estate", new BigDecimal("420.00"), 18, 5));
        productList.add(new Product("GB-104", "Roasted Salted Almonds 200g", "Snacks", "NutriHarvest Naturals", new BigDecimal("950.00"), 4, 6));
        productList.add(new Product("GB-105", "Eco-Friendly Dish Wash 500ml", "Cleaning Supplies", "PureBio Solutions", new BigDecimal("580.00"), 12, 5));
        productList.add(new Product("GB-106", "Organic Full Cream Milk 1L", "Dairy", "Happy Cow Meadows", new BigDecimal("490.00"), 3, 10));
        productList.add(new Product("GB-107", "Organic Crimson Apples 1kg", "Fresh Produce", "Orchard Bloom", new BigDecimal("850.00"), 22, 10));
        productList.add(new Product("GB-108", "Sparkling Spring Water 750ml", "Beverages", "Crystal Springs", new BigDecimal("220.00"), 0, 5));
        save();
    }

    // ---------------------------------------------------------------- changes

    public synchronized void addProduct(Product product, User operator) throws ValidationException {
        requirePermission(operator != null && operator.canAddProducts(), "add products");
        validateProduct(product, true);

        productList.add(product);
        save();
        transactionService.recordTransaction(product.getProductId(), product.getName(),
                StockTransaction.Type.RESTOCK, product.getQuantity(), product.getQuantity(),
                product.getPrice(), operator.getFullName(), "New product added");
    }

    public synchronized void updateProduct(Product updated, User operator) throws ValidationException {
        requirePermission(operator != null && operator.canEditProducts(), "edit products");
        validateProduct(updated, false);

        Product existing = findOrThrow(updated.getProductId());
        int oldQty = existing.getQuantity();

        existing.setName(updated.getName().trim());
        existing.setCategory(updated.getCategory());
        existing.setSupplier(updated.getSupplier().trim());
        existing.setPrice(updated.getPrice());
        existing.setReorderLevel(updated.getReorderLevel());
        existing.setQuantity(updated.getQuantity());
        save();

        if (oldQty != updated.getQuantity()) {
            transactionService.recordTransaction(existing.getProductId(), existing.getName(),
                    StockTransaction.Type.ADJUSTMENT, updated.getQuantity() - oldQty, existing.getQuantity(),
                    existing.getPrice(), operator.getFullName(), "Manual stock adjustment");
        }
    }

    public synchronized void deleteProduct(String productId, User operator) throws ValidationException {
        requirePermission(operator != null && operator.canDeleteProducts(), "delete products");
        Product target = findOrThrow(productId);

        productList.remove(target);
        save();
        transactionService.recordTransaction(target.getProductId(), target.getName(),
                StockTransaction.Type.ADJUSTMENT, -target.getQuantity(), 0,
                target.getPrice(), operator.getFullName(), "Product deleted");
    }

    /** Stock in (RESTOCK) or stock out (SALE). A sale can never take stock below zero. */
    public synchronized void processStockMovement(String productId, int quantity, StockTransaction.Type type,
                                                  User operator, String notes) throws ValidationException {
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }
        Product p = findOrThrow(productId);
        int change;

        if (type == StockTransaction.Type.SALE) {
            requirePermission(operator != null && operator.canPerformSales(), "make sales");
            if (p.getQuantity() < quantity) {
                throw new ValidationException("Insufficient stock! Available: " + p.getQuantity() + ", Requested: " + quantity);
            }
            change = -quantity;
        } else if (type == StockTransaction.Type.RESTOCK) {
            requirePermission(operator != null && operator.canRestock(), "restock products");
            change = quantity;
        } else {
            throw new ValidationException("Use updateProduct() for manual adjustments.");
        }

        p.setQuantity(p.getQuantity() + change);
        save();

        String defaultNote = (type == StockTransaction.Type.SALE) ? "Customer sale" : "Supplier restock";
        transactionService.recordTransaction(p.getProductId(), p.getName(), type, change, p.getQuantity(),
                p.getPrice(), operator.getFullName(), isBlank(notes) ? defaultNote : notes.trim());
    }

    // ---------------------------------------------------------------- queries

    public synchronized Product getProductById(String id) {
        if (id == null) return null;
        for (Product p : productList) {
            if (p.getProductId().equalsIgnoreCase(id.trim())) {
                return p;
            }
        }
        return null;
    }

    /** Returns a copy so callers cannot change the internal list. */
    public synchronized List<Product> getAllProducts() {
        return new ArrayList<>(productList);
    }

    public synchronized List<Product> searchProducts(String query, String category, String stockFilter) {
        String q = (query == null) ? "" : query.trim().toLowerCase();
        List<Product> results = new ArrayList<>();

        for (Product p : productList) {
            boolean matchesText = q.isEmpty()
                    || p.getProductId().toLowerCase().contains(q)
                    || p.getName().toLowerCase().contains(q)
                    || p.getSupplier().toLowerCase().contains(q);

            boolean matchesCategory = category == null
                    || category.equalsIgnoreCase("All Categories")
                    || p.getCategory().equalsIgnoreCase(category);

            boolean matchesStock = switch (stockFilter == null ? "All" : stockFilter) {
                case "In Stock" -> !p.isLowStock();
                case "Low Stock" -> p.isLowStock() && !p.isOutOfStock();
                case "Out of Stock" -> p.isOutOfStock();
                default -> true;
            };

            if (matchesText && matchesCategory && matchesStock) {
                results.add(p);
            }
        }
        return results;
    }

    public synchronized List<Product> getLowStockProducts() {
        List<Product> lowStock = new ArrayList<>();
        for (Product p : productList) {
            if (p.isLowStock()) {
                lowStock.add(p);
            }
        }
        return lowStock;
    }

    public synchronized int getLowStockCount() {
        return getLowStockProducts().size();
    }

    public synchronized BigDecimal getTotalInventoryValue() {
        BigDecimal total = BigDecimal.ZERO;
        for (Product p : productList) {
            total = total.add(p.calculateStockValue());
        }
        return total;
    }

    public synchronized Set<String> getAllCategories() {
        Set<String> categories = new TreeSet<>();
        for (Product p : productList) {
            categories.add(p.getCategory());
        }
        return categories;
    }

    /** Next free ID in the GB-xxx format, e.g. GB-109. */
    public synchronized String generateNextProductId() {
        int max = 100;
        for (Product p : productList) {
            String id = p.getProductId();
            if (id.startsWith("GB-")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(3)));
                } catch (NumberFormatException ignored) {
                    // IDs typed by hand may not be numbers - skip them
                }
            }
        }
        return "GB-" + (max + 1);
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public synchronized boolean exportToCSV(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("ProductID,Name,Category,Supplier,Price,Quantity,ReorderLevel,TotalValue,Status\n");
            for (Product p : productList) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%s,%d,%d,%s,\"%s\"%n",
                        SecurityUtil.sanitizeForCSV(p.getProductId()),
                        SecurityUtil.sanitizeForCSV(p.getName()),
                        SecurityUtil.sanitizeForCSV(p.getCategory()),
                        SecurityUtil.sanitizeForCSV(p.getSupplier()),
                        p.getPrice().toPlainString(),
                        p.getQuantity(),
                        p.getReorderLevel(),
                        p.calculateStockValue().toPlainString(),
                        p.getStockStatus()));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- helpers

    private void validateProduct(Product p, boolean isNew) throws ValidationException {
        if (p == null) throw new ValidationException("Product cannot be empty.");
        if (isBlank(p.getProductId())) throw new ValidationException("Product ID is required.");
        if (isBlank(p.getName())) throw new ValidationException("Product name is required.");
        if (isBlank(p.getCategory())) throw new ValidationException("Category is required.");
        if (isBlank(p.getSupplier())) throw new ValidationException("Supplier is required.");
        if (p.getPrice() == null || p.getPrice().signum() < 0) throw new ValidationException("Price cannot be negative.");
        if (p.getQuantity() < 0) throw new ValidationException("Quantity cannot be negative.");
        if (p.getReorderLevel() < 1) throw new ValidationException("Reorder level must be at least 1.");

        String id = p.getProductId().trim();
        String name = p.getName().trim();
        for (Product other : productList) {
            boolean sameId = other.getProductId().equalsIgnoreCase(id);
            if (isNew && sameId) {
                throw new ValidationException("A product with ID '" + id + "' already exists.");
            }
            if (!sameId && other.getName().equalsIgnoreCase(name)) {
                throw new ValidationException("A product named '" + name + "' already exists.");
            }
        }
    }

    private Product findOrThrow(String productId) throws ValidationException {
        Product p = getProductById(productId);
        if (p == null) {
            throw new ValidationException("Product not found: " + productId);
        }
        return p;
    }

    private static void requirePermission(boolean allowed, String action) throws ValidationException {
        if (!allowed) {
            throw new ValidationException("Access denied: your role cannot " + action + ".");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void save() {
        FileHandler.saveToFile(productList, fileName);
    }
}
