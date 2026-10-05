package ui;

import model.User;
import service.ProductService;
import service.UserService;
import util.UITheme;
import util.ValidationException;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Login screen. On first run (no accounts yet) it opens the setup dialog
 * so the owner can create the first Store Manager account.
 */
public class LoginFrame extends JFrame {

    private final UserService userService;
    private final ProductService productService;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private JButton btnExit;

    public LoginFrame(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
        initUI();

        if (userService.hasNoUsers()) {
            SwingUtilities.invokeLater(this::runFirstTimeSetup);
        }
    }

    private void runFirstTimeSetup() {
        UserDialog setup = new UserDialog(this, userService, null);
        setup.setVisible(true);
        if (setup.isUserCreated()) {
            txtUser.setText(setup.getCreatedUsername());
            txtPass.requestFocusInWindow();
        } else if (userService.hasNoUsers()) {
            JOptionPane.showMessageDialog(this,
                    "A Store Manager account is needed before anyone can log in.",
                    "Setup Required", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    private void initUI() {
        setTitle("Green Basket — Inventory Management System v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 570);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(UITheme.BG_LIGHT);

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBackground(UITheme.PRIMARY_DARK);
        headerPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblLogo = new JLabel("THE GREEN BASKET", SwingConstants.CENTER);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblLogo.setForeground(Color.WHITE);

        JLabel lblTagline = new JLabel("Organic Supermarket Inventory System v2.0", SwingConstants.CENTER);
        lblTagline.setFont(UITheme.FONT_REGULAR);
        lblTagline.setForeground(new Color(0xD4, 0xED, 0xDA));

        headerPanel.add(lblLogo, BorderLayout.NORTH);
        headerPanel.add(lblTagline, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Card Panel
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weightx = 1.0;

        JLabel lblSignIn = new JLabel("Sign In to Your Workspace");
        lblSignIn.setFont(UITheme.FONT_SUBTITLE);
        lblSignIn.setForeground(UITheme.PRIMARY_DARK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        card.add(lblSignIn, gbc);

        // Username
        gbc.gridwidth = 2;
        gbc.gridy = 1;
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(UITheme.FONT_HEADER);
        lblUser.setForeground(UITheme.TEXT_MAIN);
        card.add(lblUser, gbc);

        gbc.gridy = 2;
        txtUser = new JTextField();
        UITheme.styleTextField(txtUser);
        txtUser.setPreferredSize(new Dimension(320, 36));
        card.add(txtUser, gbc);

        // Password with Show/Hide Eye Toggle
        gbc.gridy = 3;
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(UITheme.FONT_HEADER);
        lblPass.setForeground(UITheme.TEXT_MAIN);
        card.add(lblPass, gbc);

        gbc.gridy = 4;
        JPanel passContainer = new JPanel(new BorderLayout(6, 0));
        passContainer.setOpaque(false);

        txtPass = new JPasswordField();
        UITheme.styleTextField(txtPass);
        txtPass.setPreferredSize(new Dimension(265, 36));

        char defaultEcho = txtPass.getEchoChar();
        JToggleButton btnTogglePass = new JToggleButton("👁");
        btnTogglePass.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        btnTogglePass.setPreferredSize(new Dimension(48, 36));
        btnTogglePass.setFocusPainted(false);
        btnTogglePass.setBackground(Color.WHITE);
        btnTogglePass.setForeground(UITheme.PRIMARY_DARK);
        btnTogglePass.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnTogglePass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePass.setToolTipText("Show Password");

        btnTogglePass.addActionListener(e -> {
            if (btnTogglePass.isSelected()) {
                txtPass.setEchoChar((char) 0);
                btnTogglePass.setText("🔒");
                btnTogglePass.setToolTipText("Hide Password");
                btnTogglePass.setBackground(new Color(0xE8, 0xF5, 0xE9));
            } else {
                txtPass.setEchoChar(defaultEcho);
                btnTogglePass.setText("👁");
                btnTogglePass.setToolTipText("Show Password");
                btnTogglePass.setBackground(Color.WHITE);
            }
        });

        passContainer.add(txtPass, BorderLayout.CENTER);
        passContainer.add(btnTogglePass, BorderLayout.EAST);
        card.add(passContainer, gbc);

        // Buttons
        gbc.gridy = 5;
        gbc.insets = new Insets(18, 6, 6, 6);
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonRow.setOpaque(false);

        btnExit = UITheme.createSecondaryButton("Exit");
        btnLogin = UITheme.createPrimaryButton("Login");

        buttonRow.add(btnExit);
        buttonRow.add(btnLogin);
        card.add(buttonRow, gbc);

        centerWrapper.add(card);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        // Footer
        JLabel lblFooter = new JLabel("The Green Basket Supermarket • Inventory System v2.0", SwingConstants.CENTER);
        lblFooter.setFont(UITheme.FONT_SMALL);
        lblFooter.setForeground(UITheme.TEXT_MUTED);
        lblFooter.setBorder(new EmptyBorder(0, 0, 16, 0));
        mainPanel.add(lblFooter, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Event Listeners
        btnLogin.addActionListener(e -> performLogin());
        btnExit.addActionListener(e -> System.exit(0));

        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        txtUser.addKeyListener(enterKeyListener);
        txtPass.addKeyListener(enterKeyListener);
    }

    private void performLogin() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword());

        try {
            User user = userService.login(username, password);
            dispose();
            SwingUtilities.invokeLater(() -> new DashboardFrame(user, userService, productService).setVisible(true));
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
            txtPass.setText("");
            txtPass.requestFocus();
        }
    }
}
