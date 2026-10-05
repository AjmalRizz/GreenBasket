package test;

import model.Product;
import model.SalesAssistant;
import model.StockTransaction;
import model.StoreManager;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.ProductService;
import service.TransactionService;
import service.UserService;
import util.FileHandler;
import util.SecurityUtil;
import util.ValidationException;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated test suite verifying GreenBasket business logic,
 * role-based security, cryptographic integrity, and data persistence.
 *
 * All tests run against isolated temporary files (@TempDir),
 * ensuring zero side-effects on production data files.
 */
public class ProjectVerificationTest {

    @TempDir
    Path tempDir;

    private String userFile;
    private String productFile;
    private String transactionFile;

    private UserService userService;
    private TransactionService transactionService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        userFile = tempDir.resolve("test_users.dat").toString();
        productFile = tempDir.resolve("test_products.dat").toString();
        transactionFile = tempDir.resolve("test_transactions.dat").toString();

        userService = new UserService(userFile);
        transactionService = new TransactionService(transactionFile);
        productService = new ProductService(productFile, transactionService);
    }

    // =========================================================================
    // 1. CRYPTOGRAPHIC & INPUT SECURITY TESTS
    // =========================================================================
    @Nested
    @DisplayName("Security & Cryptography")
    class SecurityTests {

        @Test
        @DisplayName("PBKDF2 password hashing produces valid unique salted hashes")
        void testPasswordHashingAndVerification() {
            String password = "SecretPassword123";
            String hash1 = SecurityUtil.hashPassword(password);
            String hash2 = SecurityUtil.hashPassword(password);

            assertNotNull(hash1);
            assertTrue(hash1.startsWith("PBKDF2$65536$"), "Hash should follow PBKDF2 format");
            assertNotEquals(hash1, hash2, "Per-user random salt must produce different hashes for identical passwords");

            assertTrue(SecurityUtil.verifyPassword(password, hash1), "Verification should succeed with correct password");
            assertFalse(SecurityUtil.verifyPassword("WrongPassword123", hash1), "Verification must fail with incorrect password");
            assertFalse(SecurityUtil.verifyPassword(null, hash1));
            assertFalse(SecurityUtil.verifyPassword(password, null));
        }

        @Test
        @DisplayName("Password complexity policy requires >= 8 chars, letter, and digit")
        void testPasswordPolicy() {
            assertDoesNotThrow(() -> SecurityUtil.validatePasswordStrength("Passw0rd"));
            assertDoesNotThrow(() -> SecurityUtil.validatePasswordStrength("GreenBasket2026"));

            assertThrows(ValidationException.class, () -> SecurityUtil.validatePasswordStrength("short1"));
            assertThrows(ValidationException.class, () -> SecurityUtil.validatePasswordStrength("onlyletters"));
            assertThrows(ValidationException.class, () -> SecurityUtil.validatePasswordStrength("1234567890"));
            assertThrows(ValidationException.class, () -> SecurityUtil.validatePasswordStrength(""));
            assertThrows(ValidationException.class, () -> SecurityUtil.validatePasswordStrength(null));
        }

        @Test
        @DisplayName("CSV Formula Injection sanitization neutralizes formula triggers")
        void testCsvSanitization() {
            assertEquals("'=SUM(A1:A10)", SecurityUtil.sanitizeForCSV("=SUM(A1:A10)"));
            assertEquals("'+cmd|' /C calc'!A0", SecurityUtil.sanitizeForCSV("+cmd|' /C calc'!A0"));
            assertEquals("'-1234", SecurityUtil.sanitizeForCSV("-1234"));
            assertEquals("'@evil", SecurityUtil.sanitizeForCSV("@evil"));
            assertEquals("Fresh Apples", SecurityUtil.sanitizeForCSV("Fresh Apples"));
            assertEquals("\"\"Quoted\"\"", SecurityUtil.sanitizeForCSV("\"Quoted\""));
            assertEquals("", SecurityUtil.sanitizeForCSV(null));
        }
    }

    // =========================================================================
    // 2. USER AUTHENTICATION & ACCESS CONTROL TESTS
    // =========================================================================
    @Nested
    @DisplayName("User Service & Access Control")
    class UserServiceTests {

        @Test
        @DisplayName("First-time setup allows creating the initial Store Manager")
        void testFirstTimeSetup() throws ValidationException {
            assertTrue(userService.hasNoUsers(), "New system must report no users");

            User manager = userService.createFirstManager("admin", "Admin User", "AdminPass123");
            assertNotNull(manager);
            assertTrue(manager instanceof StoreManager);
            assertEquals("Store Manager", manager.getRoleName());
            assertFalse(userService.hasNoUsers(), "System must report users exist after setup");

            // Subsequent attempts to call createFirstManager must be blocked
            assertThrows(ValidationException.class, () ->
                    userService.createFirstManager("hacker", "Hacker", "Hacker123"));
        }

        @Test
        @DisplayName("Role-based account creation enforces Store Manager authorization")
        void testRoleBasedAccountCreation() throws ValidationException {
            User manager = userService.createFirstManager("admin", "Head Manager", "AdminPass123");

            // Store Manager can create a Sales Assistant
            User sales = userService.addUser(manager, "sarah", "Sarah Cashier", "SarahPass123", false);
            assertNotNull(sales);
            assertTrue(sales instanceof SalesAssistant);
            assertEquals("Sales Assistant", sales.getRoleName());

            // Sales Assistant CANNOT create another user (Access Denied)
            assertThrows(ValidationException.class, () ->
                    userService.addUser(sales, "rogue", "Rogue Account", "RoguePass123", true));

            // Null operator is also blocked
            assertThrows(ValidationException.class, () ->
                    userService.addUser(null, "rogue2", "Rogue Account", "RoguePass123", false));
        }

        @Test
        @DisplayName("Duplicate username registration is rejected")
        void testDuplicateUsernameRejected() throws ValidationException {
            User manager = userService.createFirstManager("admin", "Head Manager", "AdminPass123");

            assertThrows(ValidationException.class, () ->
                    userService.addUser(manager, "admin", "Another Admin", "AdminPass456", true));
            assertThrows(ValidationException.class, () ->
                    userService.addUser(manager, "ADMIN", "Another Admin", "AdminPass456", true));
        }

        @Test
        @DisplayName("Authentication succeeds with valid credentials and locks out after 5 failures")
        void testLoginAndLockout() throws ValidationException {
            userService.createFirstManager("admin", "Head Manager", "AdminPass123");

            // Successful login
            User loggedIn = userService.login("admin", "AdminPass123");
            assertNotNull(loggedIn);
            assertEquals("admin", loggedIn.getUsername());

            // 4 Failed attempts - must still allow tries
            for (int i = 1; i <= 4; i++) {
                ValidationException ex = assertThrows(ValidationException.class, () ->
                        userService.login("admin", "WrongPassword123"));
                assertTrue(ex.getMessage().contains("attempt(s) left"));
            }

            // 5th Failed attempt triggers lockout
            ValidationException lockEx = assertThrows(ValidationException.class, () ->
                    userService.login("admin", "WrongPassword123"));
            assertTrue(lockEx.getMessage().contains("locked"), "5th failure must trigger account lockout");

            // Subsequent attempts while locked are rejected even with correct password
            ValidationException lockedTry = assertThrows(ValidationException.class, () ->
                    userService.login("admin", "AdminPass123"));
            assertTrue(lockedTry.getMessage().contains("locked"));
        }

        @Test
        @DisplayName("User deletion safeguards: cannot delete self and cannot delete last manager")
        void testUserDeletionSafeguards() throws ValidationException {
            User manager = userService.createFirstManager("admin", "Head Manager", "AdminPass123");
            User sales = userService.addUser(manager, "sales1", "Sales Rep", "SalesPass123", false);

            // Sales assistant cannot delete accounts
            assertThrows(ValidationException.class, () ->
                    userService.deleteUser("admin", sales));

            // Manager cannot delete themselves
            assertThrows(ValidationException.class, () ->
                    userService.deleteUser("admin", manager));

            // Manager cannot delete the only Store Manager
            assertThrows(ValidationException.class, () ->
                    userService.deleteUser("admin", manager));

            // Manager CAN delete a sales assistant
            assertDoesNotThrow(() -> userService.deleteUser("sales1", manager));
            assertEquals(1, userService.getAllUsers().size());
        }
    }

    // =========================================================================
    // 3. PRODUCT INVENTORY & STOCK OPERATIONS TESTS
    // =========================================================================
    @Nested
    @DisplayName("Product Service & Stock Operations")
    class ProductServiceTests {

        private User manager;
        private User sales;

        @BeforeEach
        void initStaff() throws ValidationException {
            manager = userService.createFirstManager("admin", "Manager", "AdminPass123");
            sales = userService.addUser(manager, "sales", "Assistant", "SalesPass123", false);
        }

        @Test
        @DisplayName("Default catalog seeds on first run and calculates total inventory value accurately")
        void testDefaultCatalogAndValueCalculation() {
            List<Product> products = productService.getAllProducts();
            assertFalse(products.isEmpty(), "Catalog should contain initial sample products");

            BigDecimal totalVal = productService.getTotalInventoryValue();
            assertTrue(totalVal.compareTo(BigDecimal.ZERO) > 0, "Total value should be positive");

            // Verify calculation matches sum of individual products
            BigDecimal manualSum = BigDecimal.ZERO;
            for (Product p : products) {
                manualSum = manualSum.add(p.calculateStockValue());
            }
            assertEquals(manualSum, totalVal);
        }

        @Test
        @DisplayName("Adding a product enforces permissions and validates input")
        void testAddProductValidation() throws ValidationException {
            Product valid = new Product("GB-999", "Organic Honey 500g", "Snacks",
                    "Pure Nature", new BigDecimal("1200.00"), 20, 5);

            assertDoesNotThrow(() -> productService.addProduct(valid, manager));

            Product retrieved = productService.getProductById("GB-999");
            assertNotNull(retrieved);
            assertEquals("Organic Honey 500g", retrieved.getName());

            // Duplicate ID rejected
            Product duplicateId = new Product("GB-999", "Different Honey", "Snacks",
                    "Pure Nature", new BigDecimal("1200.00"), 10, 5);
            assertThrows(ValidationException.class, () -> productService.addProduct(duplicateId, manager));

            // Negative price rejected
            Product negativePrice = new Product("GB-998", "Bad Price", "Snacks",
                    "Supplier", new BigDecimal("-10.00"), 10, 5);
            assertThrows(ValidationException.class, () -> productService.addProduct(negativePrice, manager));
        }

        @Test
        @DisplayName("Atomic stock movement: RESTOCK increases balance, SALE decreases balance")
        void testStockMovements() throws ValidationException {
            Product p = productService.getAllProducts().get(0);
            int initialQty = p.getQuantity();

            // 1. Restock +10
            productService.processStockMovement(p.getProductId(), 10,
                    StockTransaction.Type.RESTOCK, manager, "Test Restock");
            Product afterRestock = productService.getProductById(p.getProductId());
            assertEquals(initialQty + 10, afterRestock.getQuantity());

            // 2. Sale -5
            productService.processStockMovement(p.getProductId(), 5,
                    StockTransaction.Type.SALE, sales, "Test Customer Sale");
            Product afterSale = productService.getProductById(p.getProductId());
            assertEquals(initialQty + 5, afterSale.getQuantity());

            // 3. Sale exceeding stock is rejected
            int available = afterSale.getQuantity();
            assertThrows(ValidationException.class, () ->
                    productService.processStockMovement(p.getProductId(), available + 100,
                            StockTransaction.Type.SALE, sales, "Exceeding Sale"));
        }

        @Test
        @DisplayName("Role permission: Sales Assistant cannot delete products, Store Manager can")
        void testProductDeletionPermission() {
            Product p = productService.getAllProducts().get(0);

            // Sales Assistant blocked
            assertThrows(ValidationException.class, () ->
                    productService.deleteProduct(p.getProductId(), sales));

            // Store Manager allowed
            assertDoesNotThrow(() ->
                    productService.deleteProduct(p.getProductId(), manager));

            assertNull(productService.getProductById(p.getProductId()));
        }

        @Test
        @DisplayName("Search and filtering works across keywords, categories, and stock status")
        void testSearchAndFilters() {
            List<Product> avocados = productService.searchProducts("Avocado", "All Categories", "All");
            assertFalse(avocados.isEmpty());
            assertEquals("GB-101", avocados.get(0).getProductId());

            List<Product> lowStock = productService.searchProducts("", "All Categories", "Low Stock");
            for (Product p : lowStock) {
                assertTrue(p.isLowStock());
            }
        }

        @Test
        @DisplayName("CSV export generates valid file without errors")
        void testCsvExport() throws Exception {
            File exportFile = tempDir.resolve("export_test.csv").toFile();
            boolean ok = productService.exportToCSV(exportFile);

            assertTrue(ok);
            assertTrue(exportFile.exists());
            List<String> lines = Files.readAllLines(exportFile.toPath());
            assertTrue(lines.size() > 1, "CSV must have header + data lines");
            assertTrue(lines.get(0).startsWith("ProductID,Name,Category"));
        }
    }

    // =========================================================================
    // 4. ATOMIC DATA PERSISTENCE TESTS
    // =========================================================================
    @Nested
    @DisplayName("FileHandler Persistence & Safety")
    class PersistenceTests {

        @Test
        @DisplayName("Atomic save writes cleanly and reloads identical data")
        void testSaveAndLoad() {
            String testFile = tempDir.resolve("persistence_test.dat").toString();
            List<String> sampleData = List.of("Item1", "Item2", "Item3");

            FileHandler.saveToFile(sampleData, testFile);
            List<String> loaded = FileHandler.loadFromFile(testFile);

            assertEquals(sampleData, loaded);
        }

        @Test
        @DisplayName("Corrupted file is safely renamed to .corrupt backup without crashing")
        void testCorruptedFileSafeguard() throws Exception {
            File corruptFile = tempDir.resolve("corrupt_test.dat").toFile();
            Files.writeString(corruptFile.toPath(), "NOT_VALID_SERIALIZED_DATA");

            List<Object> loaded = FileHandler.loadFromFile(corruptFile.getAbsolutePath());
            assertNotNull(loaded);
            assertTrue(loaded.isEmpty(), "Corrupt file should safely yield empty list");

            // Verify a backup file was generated
            File[] backups = tempDir.toFile().listFiles((dir, name) -> name.contains("corrupt-"));
            assertNotNull(backups);
            assertTrue(backups.length > 0, "A .corrupt backup file should have been created");
        }
    }
}
