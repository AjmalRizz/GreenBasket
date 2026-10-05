package ui;

import model.Product;
import model.StockTransaction;
import model.User;
import service.ProductService;
import service.TransactionService;
import service.UserService;
import util.UITheme;
import util.ValidationException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Modern, unified single-window Dashboard for GreenBasketGUI v2.0.
 * Features 100% crystal-clear readable buttons and clean, professional typography.
 */
public class DashboardFrame extends JFrame {

    private final User loggedUser;
    private final ProductService productService;
    private final UserService userService;
    private final TransactionService transactionService;

    // Metric Labels
    private JLabel lblMetricProducts;
    private JLabel lblMetricValue;
    private JLabel lblMetricLowStock;
    private JLabel lblMetricTransactions;

    // Inventory Tab Components
    private JTable tblProducts;
    private DefaultTableModel modelProducts;
    private JTextField txtSearch;
    private JComboBox<String> cmbCategoryFilter;
    private JComboBox<String> cmbStockFilter;
    private JButton btnAddProduct;
    private JButton btnEditProduct;
    private JButton btnDeleteProduct;
    private JButton btnExportProducts;

    // Stock Movement Tab Components
    private JComboBox<ProductComboItem> cmbRestockProduct;
    private JLabel lblRestockCurrent;
    private JTextField txtRestockQty;
    private JTextField txtRestockNotes;

    private JComboBox<ProductComboItem> cmbSaleProduct;
    private JLabel lblSaleCurrent;
    private JLabel lblSalePrice;
    private JTextField txtSaleQty;
    private JLabel lblSaleTotal;

    // Low Stock Tab Components
    private JTable tblLowStock;
    private DefaultTableModel modelLowStock;

    // User Admin Tab Components
    private JTable tblUsers;
    private DefaultTableModel modelUsers;
    private JButton btnAddUser;
    private JButton btnDeleteUser;

    // Audit Tab Components
    private JTable tblAudit;
    private DefaultTableModel modelAudit;

    public DashboardFrame(User user, UserService userService, ProductService productService) {
        this.loggedUser = user;
        this.userService = userService;
        this.productService = productService;
        this.transactionService = productService.getTransactionService();

        initUI();
        refreshAllViews();
        applyRoleAccess();
    }

    private void initUI() {
        setTitle("Green Basket Supermarket — Stock Management System v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(1020, 680));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(UITheme.BG_LIGHT);

        // 1. Top Header Banner
        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Content: Metrics + Tabs
        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(0, 16, 14, 16));

        centerPanel.add(createMetricsPanel(), BorderLayout.NORTH);
        centerPanel.add(createTabbedPane(), BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
    }

    // ==================== HEADER PANEL ====================
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(UITheme.PRIMARY_DARK);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        // Brand
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        JLabel lblLogo = new JLabel("GREEN BASKET");
        lblLogo.setFont(UITheme.FONT_TITLE);
        lblLogo.setForeground(Color.WHITE);

        JLabel lblTag = new JLabel("| Stock Management System v2.0");
        lblTag.setFont(UITheme.FONT_REGULAR);
        lblTag.setForeground(new Color(0xD4, 0xED, 0xDA));
        brand.add(lblLogo);
        brand.add(lblTag);

        // User Info & Logout
        JPanel userArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userArea.setOpaque(false);

        String roleBadge = loggedUser != null ? loggedUser.getRoleName() : "Guest";
        String userName = loggedUser != null ? loggedUser.getFullName() : "User";

        JLabel lblUser = new JLabel("Logged in: " + userName + " (" + roleBadge + ")");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setForeground(Color.WHITE);

        JButton btnLogout = UITheme.createDangerButton("Logout");
        btnLogout.setPreferredSize(new Dimension(95, 32));
        btnLogout.addActionListener(e -> handleLogout());

        userArea.add(lblUser);
        userArea.add(btnLogout);

        header.add(brand, BorderLayout.WEST);
        header.add(userArea, BorderLayout.EAST);
        return header;
    }

    // ==================== METRICS PANEL ====================
    private JPanel createMetricsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);

        panel.add(UITheme.createMetricCard("Total Catalog Items", "0", UITheme.PRIMARY, "ITEMS"));
        panel.add(UITheme.createMetricCard("Total Inventory Value", "LKR 0.00", UITheme.INFO, "VALUE"));
        panel.add(UITheme.createMetricCard("Low Stock Alerts", "0", UITheme.WARNING, "ALERT"));
        panel.add(UITheme.createMetricCard("Stock Transactions", "0", UITheme.PRIMARY_DARK, "LOGS"));

        // Store references to dynamic value labels
        lblMetricProducts = (JLabel) ((JPanel) ((JPanel) panel.getComponent(0)).getComponent(0)).getComponent(1);
        lblMetricValue = (JLabel) ((JPanel) ((JPanel) panel.getComponent(1)).getComponent(0)).getComponent(1);
        lblMetricLowStock = (JLabel) ((JPanel) ((JPanel) panel.getComponent(2)).getComponent(0)).getComponent(1);
        lblMetricTransactions = (JLabel) ((JPanel) ((JPanel) panel.getComponent(3)).getComponent(0)).getComponent(1);

        return panel;
    }

    // ==================== TABBED CONTENT ====================
    private JTabbedPane createTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_HEADER);
        tabs.setBackground(Color.WHITE);

        tabs.addTab("  Inventory Catalog  ", createInventoryTab());
        tabs.addTab("  Stock Operations  ", createStockMovementTab());
        tabs.addTab("  Low Stock Alerts  ", createLowStockTab());

        if (loggedUser != null && loggedUser.canManageUsers()) {
            tabs.addTab("  Staff & Users  ", createUserAdminTab());
        }

        tabs.addTab("  Audit History  ", createAuditTab());
        return tabs;
    }

    // ----------------- TAB 1: INVENTORY CATALOG -----------------
    private JPanel createInventoryTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Two-Row Toolbar (Row 1: Search & Filter, Row 2: Actions)
        JPanel toolbar = new JPanel(new GridLayout(2, 1, 0, 8));
        toolbar.setOpaque(false);

        // Row 1: Search & Category Filters
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        filterRow.setOpaque(false);

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(UITheme.FONT_HEADER);
        filterRow.add(lblSearch);

        txtSearch = new JTextField(15);
        UITheme.styleTextField(txtSearch);
        txtSearch.setToolTipText("Search by product code, name, or supplier");
        filterRow.add(txtSearch);

        JLabel lblCat = new JLabel("Category:");
        lblCat.setFont(UITheme.FONT_HEADER);
        filterRow.add(lblCat);

        cmbCategoryFilter = new JComboBox<>();
        cmbCategoryFilter.setFont(UITheme.FONT_REGULAR);
        cmbCategoryFilter.setBackground(Color.WHITE);
        filterRow.add(cmbCategoryFilter);

        JLabel lblStock = new JLabel("Stock Status:");
        lblStock.setFont(UITheme.FONT_HEADER);
        filterRow.add(lblStock);

        cmbStockFilter = new JComboBox<>(new String[]{"All", "In Stock", "Low Stock", "Out of Stock"});
        cmbStockFilter.setFont(UITheme.FONT_REGULAR);
        cmbStockFilter.setBackground(Color.WHITE);
        filterRow.add(cmbStockFilter);

        JButton btnSearch = UITheme.createPrimaryButton("Filter");
        btnSearch.addActionListener(e -> loadProductTable());
        filterRow.add(btnSearch);

        JButton btnReset = UITheme.createSecondaryButton("Reset Filters");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            cmbCategoryFilter.setSelectedIndex(0);
            cmbStockFilter.setSelectedIndex(0);
            loadProductTable();
        });
        filterRow.add(btnReset);

        // Row 2: CRUD Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        actionRow.setOpaque(false);

        btnAddProduct = UITheme.createPrimaryButton("+ Add New Product");
        btnAddProduct.addActionListener(e -> handleAddProduct());

        btnEditProduct = UITheme.createSecondaryButton("Edit Product");
        btnEditProduct.addActionListener(e -> handleEditProduct());

        btnDeleteProduct = UITheme.createDangerButton("Delete Product");
        btnDeleteProduct.addActionListener(e -> handleDeleteProduct());

        btnExportProducts = UITheme.createSecondaryButton("Export to CSV");
        btnExportProducts.addActionListener(e -> handleExportInventoryCSV());

        JButton btnRefresh = UITheme.createSecondaryButton("Refresh Catalog");
        btnRefresh.addActionListener(e -> refreshAllViews());

        actionRow.add(btnAddProduct);
        actionRow.add(btnEditProduct);
        actionRow.add(btnDeleteProduct);
        actionRow.add(btnExportProducts);
        actionRow.add(btnRefresh);

        toolbar.add(filterRow);
        toolbar.add(actionRow);
        tab.add(toolbar, BorderLayout.NORTH);

        // Products Table
        String[] cols = {"Code", "Product Name", "Category", "Supplier", "Price (LKR)", "Stock Qty", "Reorder Level", "Value (LKR)", "Status"};
        modelProducts = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblProducts = new JTable(modelProducts);
        UITheme.styleTable(tblProducts);
        setupProductTableRenderers();

        tab.add(new JScrollPane(tblProducts), BorderLayout.CENTER);
        return tab;
    }

    private void setupProductTableRenderers() {
        // Status Column Renderer (Index 8)
        tblProducts.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(UITheme.FONT_BOLD);
                String val = String.valueOf(value);
                if ("OUT OF STOCK".equalsIgnoreCase(val)) {
                    lbl.setForeground(UITheme.DANGER);
                } else if ("LOW STOCK".equalsIgnoreCase(val)) {
                    lbl.setForeground(UITheme.WARNING);
                } else {
                    lbl.setForeground(UITheme.SUCCESS);
                }
                return lbl;
            }
        });

        // Right-align Price, Qty, Value
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblProducts.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        tblProducts.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tblProducts.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        tblProducts.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
    }

    // ----------------- TAB 2: STOCK OPERATIONS -----------------
    private JPanel createStockMovementTab() {
        JPanel tab = new JPanel(new GridLayout(1, 2, 24, 0));
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        // LEFT: Restock (Stock In)
        JPanel restockCard = new JPanel(new GridBagLayout());
        restockCard.setBackground(Color.WHITE);
        restockCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.PRIMARY, 1, true),
                new EmptyBorder(18, 22, 18, 22)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.weightx = 1.0;

        JLabel lblRTitle = new JLabel("Stock In (Restock Products)");
        lblRTitle.setFont(UITheme.FONT_SUBTITLE);
        lblRTitle.setForeground(UITheme.PRIMARY_DARK);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        restockCard.add(lblRTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; restockCard.add(new JLabel("Select Product:"), gbc);
        gbc.gridx = 1;
        cmbRestockProduct = new JComboBox<>();
        cmbRestockProduct.setFont(UITheme.FONT_REGULAR);
        cmbRestockProduct.setBackground(Color.WHITE);
        restockCard.add(cmbRestockProduct, gbc);

        gbc.gridx = 0; gbc.gridy = 2; restockCard.add(new JLabel("Current Stock:"), gbc);
        gbc.gridx = 1;
        lblRestockCurrent = new JLabel("0 units");
        lblRestockCurrent.setFont(UITheme.FONT_BOLD);
        lblRestockCurrent.setForeground(UITheme.PRIMARY_DARK);
        restockCard.add(lblRestockCurrent, gbc);

        gbc.gridx = 0; gbc.gridy = 3; restockCard.add(new JLabel("Units to Add (+):"), gbc);
        gbc.gridx = 1;
        txtRestockQty = new JTextField("10");
        UITheme.styleTextField(txtRestockQty);
        restockCard.add(txtRestockQty, gbc);

        gbc.gridx = 0; gbc.gridy = 4; restockCard.add(new JLabel("Supplier / Notes:"), gbc);
        gbc.gridx = 1;
        txtRestockNotes = new JTextField("Weekly Delivery");
        UITheme.styleTextField(txtRestockNotes);
        restockCard.add(txtRestockNotes, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        JButton btnApplyRestock = UITheme.createPrimaryButton("Confirm Restock");
        btnApplyRestock.addActionListener(e -> handleProcessRestock());
        restockCard.add(btnApplyRestock, gbc);

        // RIGHT: Sale (Stock Out)
        JPanel saleCard = new JPanel(new GridBagLayout());
        saleCard.setBackground(Color.WHITE);
        saleCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.INFO, 1, true),
                new EmptyBorder(18, 22, 18, 22)
        ));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.weightx = 1.0;

        JLabel lblSTitle = new JLabel("Point of Sale (Stock Deduction)");
        lblSTitle.setFont(UITheme.FONT_SUBTITLE);
        lblSTitle.setForeground(UITheme.INFO);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        saleCard.add(lblSTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; saleCard.add(new JLabel("Select Product:"), gbc);
        gbc.gridx = 1;
        cmbSaleProduct = new JComboBox<>();
        cmbSaleProduct.setFont(UITheme.FONT_REGULAR);
        cmbSaleProduct.setBackground(Color.WHITE);
        saleCard.add(cmbSaleProduct, gbc);

        gbc.gridx = 0; gbc.gridy = 2; saleCard.add(new JLabel("Available Stock:"), gbc);
        gbc.gridx = 1;
        lblSaleCurrent = new JLabel("0 units");
        lblSaleCurrent.setFont(UITheme.FONT_BOLD);
        saleCard.add(lblSaleCurrent, gbc);

        gbc.gridx = 0; gbc.gridy = 3; saleCard.add(new JLabel("Unit Price:"), gbc);
        gbc.gridx = 1;
        lblSalePrice = new JLabel("LKR 0.00");
        lblSalePrice.setFont(UITheme.FONT_BOLD);
        saleCard.add(lblSalePrice, gbc);

        gbc.gridx = 0; gbc.gridy = 4; saleCard.add(new JLabel("Quantity to Sell (-):"), gbc);
        gbc.gridx = 1;
        txtSaleQty = new JTextField("1");
        UITheme.styleTextField(txtSaleQty);
        saleCard.add(txtSaleQty, gbc);

        gbc.gridx = 0; gbc.gridy = 5; saleCard.add(new JLabel("Total Amount:"), gbc);
        gbc.gridx = 1;
        lblSaleTotal = new JLabel("LKR 0.00");
        lblSaleTotal.setFont(UITheme.FONT_SUBTITLE);
        lblSaleTotal.setForeground(UITheme.PRIMARY_DARK);
        saleCard.add(lblSaleTotal, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JButton btnApplySale = UITheme.createInfoButton("Complete Sale & Print Bill");
        btnApplySale.addActionListener(e -> handleProcessSale());
        saleCard.add(btnApplySale, gbc);

        // Listeners for ComboBox selection updates
        cmbRestockProduct.addActionListener(e -> updateRestockCardInfo());
        cmbSaleProduct.addActionListener(e -> updateSaleCardInfo());
        txtSaleQty.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                calculateSaleTotal();
            }
        });

        tab.add(restockCard);
        tab.add(saleCard);
        return tab;
    }

    // ----------------- TAB 3: LOW STOCK ALERTS -----------------
    private JPanel createLowStockTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel lbl = new JLabel("Inventory Items Requiring Immediate Reordering");
        lbl.setFont(UITheme.FONT_SUBTITLE);
        lbl.setForeground(UITheme.WARNING);

        JButton btnQuickRestock = UITheme.createPrimaryButton("Quick Restock");
        btnQuickRestock.addActionListener(e -> handleQuickRestockFromAlert());

        topBar.add(lbl, BorderLayout.WEST);
        topBar.add(btnQuickRestock, BorderLayout.EAST);
        tab.add(topBar, BorderLayout.NORTH);

        String[] cols = {"Product Code", "Product Name", "Category", "Supplier", "Price (LKR)", "Current Stock", "Reorder Level", "Urgency Status"};
        modelLowStock = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblLowStock = new JTable(modelLowStock);
        UITheme.styleTable(tblLowStock);

        tblLowStock.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(UITheme.FONT_BOLD);
                if ("CRITICAL: OUT OF STOCK".equalsIgnoreCase(String.valueOf(value))) {
                    l.setForeground(UITheme.DANGER);
                } else {
                    l.setForeground(UITheme.WARNING);
                }
                return l;
            }
        });

        tab.add(new JScrollPane(tblLowStock), BorderLayout.CENTER);
        return tab;
    }

    // ----------------- TAB 4: USER ADMIN (MANAGER ONLY) -----------------
    private JPanel createUserAdminTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lbl = new JLabel("Staff & User Account Management");
        lbl.setFont(UITheme.FONT_SUBTITLE);
        lbl.setForeground(UITheme.PRIMARY_DARK);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        btnAddUser = UITheme.createPrimaryButton("Register User");
        btnAddUser.addActionListener(e -> handleAddUser());

        btnDeleteUser = UITheme.createDangerButton("Delete User");
        btnDeleteUser.addActionListener(e -> handleDeleteUser());

        actions.add(btnAddUser);
        actions.add(btnDeleteUser);

        top.add(lbl, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);
        tab.add(top, BorderLayout.NORTH);

        String[] cols = {"Username", "Full Name", "Role / Designation", "Registration Date"};
        modelUsers = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblUsers = new JTable(modelUsers);
        UITheme.styleTable(tblUsers);
        tab.add(new JScrollPane(tblUsers), BorderLayout.CENTER);
        return tab;
    }

    // ----------------- TAB 5: AUDIT LOG -----------------
    private JPanel createAuditTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 10));
        tab.setBackground(Color.WHITE);
        tab.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lbl = new JLabel("Stock Movement Audit Trail");
        lbl.setFont(UITheme.FONT_SUBTITLE);
        lbl.setForeground(UITheme.PRIMARY_DARK);

        JButton btnExport = UITheme.createSecondaryButton("Export Audit Log");
        btnExport.addActionListener(e -> handleExportAuditCSV());

        top.add(lbl, BorderLayout.WEST);
        top.add(btnExport, BorderLayout.EAST);
        tab.add(top, BorderLayout.NORTH);

        String[] cols = {"Transaction ID", "Timestamp", "Product Code", "Product Name", "Movement Type", "Change", "New Balance", "Unit Price", "Total (LKR)", "Staff Member", "Notes"};
        modelAudit = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblAudit = new JTable(modelAudit);
        UITheme.styleTable(tblAudit);

        tab.add(new JScrollPane(tblAudit), BorderLayout.CENTER);
        return tab;
    }

    // ==================== DATA LOADING & REFRESH ====================
    public void refreshAllViews() {
        refreshDashboardMetrics();
        refreshCategoryFilter();
        loadProductTable();
        refreshStockOperationCombos();
        loadLowStockTable();
        if (loggedUser != null && loggedUser.canManageUsers()) {
            loadUserTable();
        }
        loadAuditTable();
    }

    private void refreshDashboardMetrics() {
        int totalItems = productService.getAllProducts().size();
        BigDecimal totalVal = productService.getTotalInventoryValue();
        int lowStock = productService.getLowStockCount();
        int transactions = transactionService.getTransactionCount();

        lblMetricProducts.setText(totalItems + " Products");
        lblMetricValue.setText(String.format("LKR %,.2f", totalVal));
        lblMetricLowStock.setText(lowStock + " Items");
        lblMetricTransactions.setText(transactions + " Logs");
    }

    private void refreshCategoryFilter() {
        String current = (String) cmbCategoryFilter.getSelectedItem();
        cmbCategoryFilter.removeAllItems();
        cmbCategoryFilter.addItem("All Categories");
        for (String cat : productService.getAllCategories()) {
            cmbCategoryFilter.addItem(cat);
        }
        if (current != null) {
            cmbCategoryFilter.setSelectedItem(current);
        }
    }

    private void loadProductTable() {
        modelProducts.setRowCount(0);
        String q = txtSearch.getText().trim();
        String cat = (String) cmbCategoryFilter.getSelectedItem();
        String stock = (String) cmbStockFilter.getSelectedItem();

        List<Product> products = productService.searchProducts(q, cat, stock);
        for (Product p : products) {
            String status = p.isOutOfStock() ? "OUT OF STOCK" : (p.isLowStock() ? "LOW STOCK" : "IN STOCK");
            modelProducts.addRow(new Object[]{
                    p.getProductId(),
                    p.getName(),
                    p.getCategory(),
                    p.getSupplier(),
                    String.format("%.2f", p.getPrice()),
                    p.getQuantity(),
                    p.getReorderLevel(),
                    String.format("%,.2f", p.calculateStockValue()),
                    status
            });
        }
    }

    private void loadLowStockTable() {
        modelLowStock.setRowCount(0);
        List<Product> list = productService.getLowStockProducts();
        for (Product p : list) {
            String status = p.isOutOfStock() ? "CRITICAL: OUT OF STOCK" : "LOW STOCK WARNING";
            modelLowStock.addRow(new Object[]{
                    p.getProductId(),
                    p.getName(),
                    p.getCategory(),
                    p.getSupplier(),
                    String.format("%.2f", p.getPrice()),
                    p.getQuantity(),
                    p.getReorderLevel(),
                    status
            });
        }
    }

    private void loadUserTable() {
        if (modelUsers == null) return;
        modelUsers.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (User u : userService.getAllUsers()) {
            modelUsers.addRow(new Object[]{
                    u.getUsername(),
                    u.getFullName(),
                    u.getRoleName(),
                    u.getCreatedAt() != null ? sdf.format(u.getCreatedAt()) : "N/A"
            });
        }
    }

    private void loadAuditTable() {
        modelAudit.setRowCount(0);
        for (StockTransaction tx : transactionService.getAllTransactions()) {
            modelAudit.addRow(new Object[]{
                    tx.getTransactionId(),
                    tx.getFormattedDate(),
                    tx.getProductId(),
                    tx.getProductName(),
                    tx.getType(),
                    (tx.getQuantityChanged() > 0 ? "+" : "") + tx.getQuantityChanged(),
                    tx.getResultingQuantity(),
                    String.format("%.2f", tx.getUnitPrice()),
                    String.format("%.2f", tx.getTotalAmount()),
                    tx.getPerformedBy(),
                    tx.getNotes()
            });
        }
    }

    private void refreshStockOperationCombos() {
        cmbRestockProduct.removeAllItems();
        cmbSaleProduct.removeAllItems();

        List<Product> products = productService.getAllProducts();
        for (Product p : products) {
            ProductComboItem item = new ProductComboItem(p);
            cmbRestockProduct.addItem(item);
            cmbSaleProduct.addItem(item);
        }

        updateRestockCardInfo();
        updateSaleCardInfo();
    }

    private void updateRestockCardInfo() {
        ProductComboItem item = (ProductComboItem) cmbRestockProduct.getSelectedItem();
        if (item != null) {
            Product p = productService.getProductById(item.getId());
            lblRestockCurrent.setText(p != null ? p.getQuantity() + " units in stock" : "0 units");
        } else {
            lblRestockCurrent.setText("No product selected");
        }
    }

    private void updateSaleCardInfo() {
        ProductComboItem item = (ProductComboItem) cmbSaleProduct.getSelectedItem();
        if (item != null) {
            Product p = productService.getProductById(item.getId());
            if (p != null) {
                lblSaleCurrent.setText(p.getQuantity() + " units");
                lblSalePrice.setText(String.format("LKR %.2f", p.getPrice()));
                calculateSaleTotal();
                return;
            }
        }
        lblSaleCurrent.setText("0 units");
        lblSalePrice.setText("LKR 0.00");
        lblSaleTotal.setText("LKR 0.00");
    }

    private void calculateSaleTotal() {
        try {
            int qty = Integer.parseInt(txtSaleQty.getText().trim());
            ProductComboItem item = (ProductComboItem) cmbSaleProduct.getSelectedItem();
            if (item != null && qty > 0) {
                Product p = productService.getProductById(item.getId());
                if (p != null) {
                    BigDecimal total = p.getPrice().multiply(BigDecimal.valueOf(qty));
                    lblSaleTotal.setText(String.format("LKR %,.2f", total));
                    return;
                }
            }
        } catch (NumberFormatException ignored) {}
        lblSaleTotal.setText("LKR 0.00");
    }

    // ==================== ROLE PERMISSIONS ====================
    private void applyRoleAccess() {
        if (loggedUser == null) return;

        // Polymorphic check on User model
        btnDeleteProduct.setEnabled(loggedUser.canDeleteProducts());
        btnDeleteProduct.setToolTipText(loggedUser.canDeleteProducts() ? "Delete selected product" : "Store Manager privilege required");

        if (btnAddUser != null) {
            btnAddUser.setEnabled(loggedUser.canManageUsers());
        }
        if (btnDeleteUser != null) {
            btnDeleteUser.setEnabled(loggedUser.canManageUsers());
        }
    }

    // ==================== ACTION HANDLERS ====================
    private void handleAddProduct() {
        ProductDialog dialog = new ProductDialog(this, productService, null, loggedUser);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshAllViews();
        }
    }

    private void handleEditProduct() {
        int selectedRow = tblProducts.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String productId = (String) tblProducts.getValueAt(selectedRow, 0);
        Product p = productService.getProductById(productId);
        if (p != null) {
            ProductDialog dialog = new ProductDialog(this, productService, p, loggedUser);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshAllViews();
            }
        }
    }

    private void handleDeleteProduct() {
        int selectedRow = tblProducts.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String productId = (String) tblProducts.getValueAt(selectedRow, 0);
        String productName = (String) tblProducts.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to permanently delete '" + productName + "' (" + productId + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                productService.deleteProduct(productId, loggedUser);
                JOptionPane.showMessageDialog(this, "Product deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAllViews();
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleProcessRestock() {
        ProductComboItem item = (ProductComboItem) cmbRestockProduct.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a product to restock.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(txtRestockQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for Restock Quantity.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String notes = txtRestockNotes.getText().trim();
        try {
            productService.processStockMovement(item.getId(), qty, StockTransaction.Type.RESTOCK, loggedUser, notes);
            JOptionPane.showMessageDialog(this, "Restocked " + qty + " units of '" + item.getName() + "' successfully!",
                    "Stock In Completed", JOptionPane.INFORMATION_MESSAGE);
            refreshAllViews();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Operation Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleProcessSale() {
        ProductComboItem item = (ProductComboItem) cmbSaleProduct.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select a product for sale.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(txtSaleQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive number for Sale Quantity.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Product p = productService.getProductById(item.getId());
        if (p == null) return;

        BigDecimal billAmount = p.getPrice().multiply(BigDecimal.valueOf(qty));

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Confirm POS Sale:\n\nProduct: %s (%s)\nQuantity: %d units\nUnit Price: LKR %.2f\nTotal Bill: LKR %,.2f",
                        p.getName(), p.getProductId(), qty, p.getPrice(), billAmount),
                "Confirm Checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            productService.processStockMovement(item.getId(), qty, StockTransaction.Type.SALE, loggedUser, "Customer Retail Sale");

            // Display Receipt
            String receipt = String.format(
                    "========================================\n" +
                    "       THE GREEN BASKET SUPERMARKET     \n" +
                    "           SALES RECEIPT & BILL         \n" +
                    "========================================\n" +
                    "Date: %s\nCashier: %s\n" +
                    "----------------------------------------\n" +
                    "Item: %s [%s]\n" +
                    "Qty:  %d  x  LKR %.2f\n" +
                    "TOTAL AMOUNT PAID: LKR %.2f\n" +
                    "Remaining Stock:   %d units\n" +
                    "========================================\n" +
                    "     Thank you for shopping organic!    \n" +
                    "========================================",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                    loggedUser.getFullName(),
                    p.getName(), p.getProductId(),
                    qty, p.getPrice(),
                    billAmount,
                    p.getQuantity()
            );

            JTextArea ta = new JTextArea(receipt);
            ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
            ta.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(ta), "Sale Successful — Printed Receipt", JOptionPane.INFORMATION_MESSAGE);

            refreshAllViews();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Sale Rejected", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleQuickRestockFromAlert() {
        int selectedRow = tblLowStock.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item from the alert table to restock.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String productId = (String) tblLowStock.getValueAt(selectedRow, 0);
        String name = (String) tblLowStock.getValueAt(selectedRow, 1);

        String input = JOptionPane.showInputDialog(this, "Enter number of units to restock for:\n" + name + " (" + productId + "):", "Quick Restock", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();

            productService.processStockMovement(productId, qty, StockTransaction.Type.RESTOCK, loggedUser, "Quick Alert Restock");
            JOptionPane.showMessageDialog(this, "Successfully restocked " + qty + " units of " + name + "!", "Restock Complete", JOptionPane.INFORMATION_MESSAGE);
            refreshAllViews();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid positive integer.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
        } catch (ValidationException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAddUser() {
        UserDialog dialog = new UserDialog(this, userService, loggedUser);
        dialog.setVisible(true);
        if (dialog.isUserCreated()) {
            loadUserTable();
        }
    }

    private void handleDeleteUser() {
        int selectedRow = tblUsers.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String username = (String) tblUsers.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete user account: " + username + "?",
                "Confirm Account Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                userService.deleteUser(username, loggedUser);
                JOptionPane.showMessageDialog(this, "User '" + username + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadUserTable();
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Prohibited", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleExportInventoryCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("GreenBasket_Inventory_" + System.currentTimeMillis() + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            boolean ok = productService.exportToCSV(chooser.getSelectedFile());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Inventory exported successfully to CSV!", "Export Completed", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to write CSV file.", "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleExportAuditCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("GreenBasket_AuditLog_" + System.currentTimeMillis() + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            boolean ok = transactionService.exportToCSV(chooser.getSelectedFile());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Audit log exported successfully to CSV!", "Export Completed", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to write CSV file.", "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout of the system?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame(userService, productService).setVisible(true));
        }
    }

    // Helper class for ComboBox items
    private static class ProductComboItem {
        private final String id;
        private final String name;

        public ProductComboItem(Product p) {
            this.id = p.getProductId();
            this.name = p.getName();
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "[" + id + "] " + name;
        }
    }
}
