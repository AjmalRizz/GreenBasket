package ui;

import model.SalesAssistant;
import model.StoreManager;
import model.User;
import service.UserService;
import util.UITheme;
import util.ValidationException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Dialog for creating a user account. Used in two ways:
 *  - operator == null : first-run setup, creates the first Store Manager
 *  - operator != null : a logged-in Store Manager adding a staff member
 */
public class UserDialog extends JDialog {

    private final UserService userService;
    private final User operator;
    private final boolean setupMode;
    private String createdUsername;

    private JTextField txtFullName;
    private JTextField txtUsername;
    private JComboBox<String> cmbRole;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;

    public UserDialog(Frame parent, UserService userService, User operator) {
        super(parent, operator == null ? "First-Time Setup" : "Create New User Account", true);
        this.userService = userService;
        this.operator = operator;
        this.setupMode = (operator == null);

        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(UITheme.BG_LIGHT);
        content.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel lblTitle = new JLabel(setupMode
                ? "<html>Welcome! Create the first <b>Store Manager</b> account.</html>"
                : "Create GreenBasket User Account");
        lblTitle.setFont(UITheme.FONT_SUBTITLE);
        lblTitle.setForeground(UITheme.PRIMARY_DARK);
        content.add(lblTitle, BorderLayout.NORTH);

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

        txtFullName = new JTextField(15);
        txtUsername = new JTextField(15);
        cmbRole = new JComboBox<>(new String[]{"Sales Assistant", "Store Manager"});
        cmbRole.setFont(UITheme.FONT_REGULAR);
        cmbRole.setBackground(Color.WHITE);
        if (setupMode) {
            cmbRole.setSelectedItem("Store Manager");
            cmbRole.setEnabled(false);
        }

        txtPassword = new JPasswordField(15);
        txtConfirmPassword = new JPasswordField(15);

        UITheme.styleTextField(txtFullName);
        UITheme.styleTextField(txtUsername);
        UITheme.styleTextField(txtPassword);
        UITheme.styleTextField(txtConfirmPassword);

        int row = 0;
        addFormField(form, gbc, "Full Name:", txtFullName, row++);
        addFormField(form, gbc, "Username:", txtUsername, row++);
        addFormField(form, gbc, "Role / Designation:", cmbRole, row++);
        addFormField(form, gbc, "Password:", createPasswordToggleWrapper(txtPassword), row++);
        addFormField(form, gbc, "Confirm Password:", createPasswordToggleWrapper(txtConfirmPassword), row++);

        content.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = UITheme.createSecondaryButton(setupMode ? "Exit" : "Cancel");
        btnCancel.addActionListener(e -> dispose());

        JButton btnCreate = UITheme.createPrimaryButton("Create Account");
        btnCreate.addActionListener(e -> handleCreate());

        btnPanel.add(btnCancel);
        btnPanel.add(btnCreate);
        content.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel createPasswordToggleWrapper(JPasswordField field) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setOpaque(false);
        panel.add(field, BorderLayout.CENTER);

        char defaultEcho = field.getEchoChar();
        JToggleButton btnEye = new JToggleButton("👁");
        btnEye.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        btnEye.setPreferredSize(new Dimension(38, 30));
        btnEye.setFocusPainted(false);
        btnEye.setBackground(Color.WHITE);
        btnEye.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        btnEye.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEye.setToolTipText("Show Password");

        btnEye.addActionListener(e -> {
            if (btnEye.isSelected()) {
                field.setEchoChar((char) 0);
                btnEye.setText("🔒");
                btnEye.setToolTipText("Hide Password");
                btnEye.setBackground(new Color(0xE8, 0xF5, 0xE9));
            } else {
                field.setEchoChar(defaultEcho);
                btnEye.setText("👁");
                btnEye.setToolTipText("Show Password");
                btnEye.setBackground(Color.WHITE);
            }
        });

        panel.add(btnEye, BorderLayout.EAST);
        return panel;
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

    private void handleCreate() {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String confirm = new String(txtConfirmPassword.getPassword());
        boolean isManager = "Store Manager".equals(cmbRole.getSelectedItem());

        try {
            if (!password.equals(confirm)) {
                throw new ValidationException("Passwords do not match.");
            }

            // The service validates the input, checks permissions and hashes the password.
            User created = setupMode
                    ? userService.createFirstManager(username, fullName, password)
                    : userService.addUser(operator, username, fullName, password, isManager);

            JOptionPane.showMessageDialog(this,
                    "Account created for " + created.getFullName() + " (" + created.getRoleName() + ")",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            createdUsername = created.getUsername();
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean isUserCreated() {
        return createdUsername != null;
    }

    public String getCreatedUsername() {
        return createdUsername;
    }
}
