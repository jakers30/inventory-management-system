package inventory.service;

/**
 * One checked exception for every business rule the user can break:
 * duplicate ID, blank name, letters in a quantity, not enough stock, and so on.
 *
 * <p>The GUI catches this exception and shows {@link #getMessage()} in a
 * {@code JOptionPane} with an error icon, so the user never sees a stack trace.</p>
 */
public class InventoryException extends Exception {

    private static final long serialVersionUID = 1L;

    public InventoryException(String message) {
        super(message);
    }
}
