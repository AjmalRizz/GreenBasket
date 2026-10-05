package app;

import service.ProductService;
import service.TransactionService;
import service.UserService;
import ui.LoginFrame;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts the Green Basket inventory app.
 *
 * The services are created once here and passed into each window
 * (constructor injection), so every screen works with the same data.
 */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOG.log(Level.INFO, "System look and feel not available, using default", e);
        }

        // Last line of defence: if something unexpected goes wrong (for example the
        // data file cannot be saved), tell the user instead of failing silently.
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            LOG.log(Level.SEVERE, "Unexpected error", error);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Something went wrong: " + error.getMessage(),
                    "Unexpected Error", JOptionPane.ERROR_MESSAGE));
        });

        UserService userService = new UserService(UserService.DEFAULT_FILE);
        ProductService productService = new ProductService(ProductService.DEFAULT_FILE,
                new TransactionService(TransactionService.DEFAULT_FILE));

        SwingUtilities.invokeLater(() -> new LoginFrame(userService, productService).setVisible(true));
    }
}
