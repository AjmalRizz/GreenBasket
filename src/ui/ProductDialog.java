package ui;

import model.Product;
import model.User;
import service.ProductService;
import util.UITheme;
import util.ValidationException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Modal dialog for Adding and Editing Products with complete validation.
 */
public class ProductDialog extends JDialog {

    private final ProductService productService;
    private final Product existingProduct;
    private final User operator;
    private boolean saved = false;

    private JTextField txtId;
    private JTextField txtName;
    private JComboBox<String> cmbCategory;
    private JTextField txtSupplier;
    private JTextField txtPrice;
    private JTextField txtQuantity;
    private JTextField txtReorder;

    public ProductDialog(Frame parent, ProductService productService, Product existingProduct, User operator) {
        super(parent, existingProduct == null ? "Add New Product" : "Edit Product", true);
        this.productService = productService;
        this.existingProduct = existingProduct;
        this.operator = operator;

        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(UITheme.BG_LIGHT);
        content.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        JLabel lblTitle = new JLabel(existingProduct == null ? "Register New Supermarket Product" : "Update Product Details");
        lblTitle.setFont(UITheme.FONT_SUBTITLE);
        lblTitle.setForeground(UITheme.PRIMARY_DARK);
        content.add(lblTitle, BorderLayout.NORTH);

        // Form Fields
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        txtId = new JTextField(15);
        txtName = new JTextField(15);
        cmbCategory = new JComboBox<>(new String[]{
                "Fresh Produce", "Dairy", "Beverages", "Snacks",
                "Bakery", "Grains & Rice", "Cleaning Supplies", "Personal Care"
        });
        cmbCategory.setFont(UITheme.FONT_REGULAR);
        cmbCategory.setBackground(Color.WHITE);

        txtSupplier = new JTextField(15);
        txtPrice = new JTextField(15);
        txtQuantity = new JTextField(15);
        txtReorder = new JTextField(15);

        UITheme.styleTextField(txtId);
        UITheme.styleTextField(txtName);
        UITheme.styleTextField(txtSupplier);
        UITheme.styleTextField(txtPrice);
        UITheme.styleTextField(txtQuantity);
        UITheme.styleTextField(txtReorder);

        if (existingProduct == null) {
            txtId.setText(productService.generateNextProductId());
            txtQuantity.setText("0");
            txtReorder.setText("10");
        } else {
            txtId.setText(existingProduct.getProductId());
            txtId.setEditable(false);
            txtId.setBackground(new Color(0xFA, 0xFA, 0xFA));
            txtName.setText(existingProduct.getName());
            cmbCategory.setSelectedItem(existingProduct.getCategory());
            txtSupplier.setText(existingProduct.getSupplier());
            txtPrice.setText(existingProduct.getPrice().toPlainString());
            txtQuantity.setText(String.valueOf(existingProduct.getQuantity()));
            txtReorder.setText(String.valueOf(existingProduct.getReorderLevel()));
        }

        int row = 0;
        addFormField(form, gbc, "Product ID (SKU):", txtId, row++);
        addFormField(form, gbc, "Product Name:", txtName, row++);
        addFormField(form, gbc, "Category:", cmbCategory, row++);
        addFormField(form, gbc, "Supplier / Farm:", txtSupplier, row++);
        addFormField(form, gbc, "Unit Price (LKR):", txtPrice, row++);
        addFormField(form, gbc, "Initial Stock Quantity:", txtQuantity, row++);
        addFormField(form, gbc, "Reorder Alert Level:", txtReorder, row++);

        content.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UITheme.createPrimaryButton(existingProduct == null ? "Save Product" : "Update Changes");
        btnSave.addActionListener(e -> handleSave());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.FONT_HEADER);
        lbl.setForeground(UITheme.TEXT_MAIN);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(field, gbc);
    }

    private void handleSave() {
        try {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String category = (String) cmbCategory.getSelectedItem();
            String supplier = txtSupplier.getText().trim();

            BigDecimal price;
            try {
                price = new BigDecimal(txtPrice.getText().trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Please enter a valid numeric Price.");
            }

            int qty;
            try {
                qty = Integer.parseInt(txtQuantity.getText().trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Please enter a valid whole number for Quantity.");
            }

            int reorder;
            try {
                reorder = Integer.parseInt(txtReorder.getText().trim());
            } catch (NumberFormatException e) {
                throw new ValidationException("Please enter a valid whole number for Reorder Level.");
            }

            Product product = new Product(id, name, category, supplier, price, qty, reorder);

            if (existingProduct == null) {
                productService.addProduct(product, operator);
                JOptionPane.showMessageDialog(this, "Product registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                productService.updateProduct(product, operator);
                JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            saved = true;
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
