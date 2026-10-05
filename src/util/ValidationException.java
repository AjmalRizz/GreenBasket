package util;

/**
 * Custom exception representing business validation errors in GreenBasket.
 */
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
