package util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Design system tokens and styling helpers for GreenBasketGUI v2.0.
 * Organic supermarket aesthetic with crystal-clear high-contrast buttons and typography.
 */
public class UITheme {

    // Brand Palette
    public static final Color PRIMARY_DARK  = new Color(0x1B, 0x5E, 0x20); // Deep Forest Green (#1B5E20)
    public static final Color PRIMARY       = new Color(0x2E, 0x7D, 0x32); // Vibrant Supermarket Green (#2E7D32)
    public static final Color PRIMARY_HOVER = new Color(0x1B, 0x5E, 0x20); // Darker Green for hover
    public static final Color PRIMARY_LIGHT = new Color(0x4C, 0xAF, 0x50); // Leaf Green
    public static final Color BG_LIGHT      = new Color(0xF4, 0xF7, 0xF5); // Soft Neutral Background
    public static final Color CARD_BG       = Color.WHITE;
    public static final Color TEXT_MAIN     = new Color(0x21, 0x25, 0x29); // Dark Charcoal Text
    public static final Color TEXT_MUTED    = new Color(0x6C, 0x75, 0x7D); // Cool Gray
    public static final Color BORDER        = new Color(0xDE, 0xE2, 0xE6); // Crisp Border
    public static final Color DANGER        = new Color(0xC8, 0x23, 0x33); // Strong Red for Delete / Danger
    public static final Color WARNING       = new Color(0xD3, 0x54, 0x00); // Amber/Orange Warning
    public static final Color SUCCESS       = new Color(0x28, 0xA7, 0x45); // Success Green
    public static final Color INFO          = new Color(0x0D, 0x6E, 0xFD); // Royal Blue

    // Standard Non-Emoji Fonts
    public static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_HEADER   = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR  = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD     = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_CARD_NUM = new Font("Segoe UI", Font.BOLD, 22);

    /**
     * Creates a high-contrast custom-painted button that bypasses native Look & Feel rendering bugs.
     * Guarantees 100% crystal-clear readable text on all platforms.
     */
    public static JButton createCustomButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color currentBg = bgColor;
                if (!isEnabled()) {
                    currentBg = new Color(0xE0, 0xE0, 0xE0);
                } else if (getModel().isPressed()) {
                    currentBg = bgColor.darker().darker();
                } else if (getModel().isRollover()) {
                    currentBg = bgColor.darker();
                }

                // Fill smooth rounded rectangle
                g2.setColor(currentBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                // Draw border for light buttons
                if (bgColor.equals(Color.WHITE) || fgColor.equals(TEXT_MAIN)) {
                    g2.setColor(new Color(0xBD, 0xC3, 0xC7));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                }

                // Draw Text with high contrast
                Color currentFg = isEnabled() ? fgColor : new Color(0x9E, 0x9E, 0x9E);
                g2.setFont(getFont());
                g2.setColor(currentFg);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        btn.setFont(FONT_BOLD);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Ensure sufficient padding
        FontMetrics fm = btn.getFontMetrics(FONT_BOLD);
        int textWidth = fm.stringWidth(text);
        btn.setPreferredSize(new Dimension(Math.max(textWidth + 28, 90), 34));
        return btn;
    }

    public static JButton createPrimaryButton(String text) {
        return createCustomButton(text, PRIMARY, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return createCustomButton(text, Color.WHITE, TEXT_MAIN);
    }

    public static JButton createDangerButton(String text) {
        return createCustomButton(text, DANGER, Color.WHITE);
    }

    public static JButton createWarningButton(String text) {
        return createCustomButton(text, WARNING, Color.WHITE);
    }

    public static JButton createInfoButton(String text) {
        return createCustomButton(text, INFO, Color.WHITE);
    }

    /**
     * Styles text input fields with clean borders and padding.
     */
    public static void styleTextField(JTextField tf) {
        tf.setFont(FONT_REGULAR);
        tf.setForeground(TEXT_MAIN);
        tf.setBackground(Color.WHITE);
        tf.setCaretColor(PRIMARY);
        Border line = new LineBorder(BORDER, 1, true);
        Border pad = new EmptyBorder(6, 10, 6, 10);
        tf.setBorder(new CompoundBorder(line, pad));
    }

    /**
     * Creates an interactive dashboard metric card with clean typography (no broken emoji boxes).
     */
    public static JPanel createMetricCard(String title, String value, Color accentColor, String badgeText) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        // Left Text area
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(FONT_SMALL);
        lblTitle.setForeground(TEXT_MUTED);

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(FONT_CARD_NUM);
        lblVal.setForeground(accentColor);

        textPanel.add(lblTitle);
        textPanel.add(lblVal);

        // Right Indicator badge (clean text/code badge instead of broken emoji)
        JLabel lblBadge = new JLabel(badgeText, SwingConstants.CENTER);
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBadge.setForeground(accentColor);
        lblBadge.setPreferredSize(new Dimension(54, 32));
        lblBadge.setOpaque(true);
        lblBadge.setBackground(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25));
        lblBadge.setBorder(new LineBorder(accentColor, 1, true));

        card.add(textPanel, BorderLayout.CENTER);
        card.add(lblBadge, BorderLayout.EAST);
        return card;
    }

    /**
     * Styles JTable with header styling, alternating row colors, row height, and selection highlights.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEE, 0xF0, 0xF2));
        table.setSelectionBackground(new Color(0xC8, 0xE6, 0xC9));
        table.setSelectionForeground(TEXT_MAIN);
        table.setFillsViewportHeight(true);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADER);
        header.setBackground(new Color(0xFA, 0xFB, 0xFC));
        header.setForeground(TEXT_MAIN);
        header.setBorder(new LineBorder(BORDER, 1));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, isSel, hasFocus, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!isSel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9, 0xFB, 0xF9));
                }
                return c;
            }
        });
    }
}
