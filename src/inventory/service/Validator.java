package inventory.service;

/**
 * All input validation in one place. Every method trims the text first, then
 * either returns a clean value or throws {@link InventoryException} with a message
 * that can be shown to the user as it is.
 */
public class Validator {

    public static final int MAX_ID_LENGTH = 10;
    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_CATEGORY_LENGTH = 50;

    private Validator() {
        // utility class - not meant to be instantiated
    }

    /**
     * Product ID: required, no spaces, no '|', at most 10 characters.
     * Uniqueness is checked by {@link InventoryService} with the hash table.
     */
    public static String validateId(String raw) throws InventoryException {
        String id = trim(raw);
        if (id.isEmpty()) {
            throw new InventoryException("Product ID is required.");
        }
        if (id.length() > MAX_ID_LENGTH) {
            throw new InventoryException("Product ID must be at most " + MAX_ID_LENGTH + " characters.");
        }
        if (id.indexOf(' ') >= 0) {
            throw new InventoryException("Product ID must not contain spaces.");
        }
        if (id.indexOf('|') >= 0) {
            throw new InventoryException("Product ID must not contain the '|' character.");
        }
        return id;
    }

    /** Product name: required, at most 50 characters, no '|'. */
    public static String validateName(String raw) throws InventoryException {
        String name = trim(raw);
        if (name.isEmpty()) {
            throw new InventoryException("Product name is required.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InventoryException("Product name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (name.indexOf('|') >= 0) {
            throw new InventoryException("Product name must not contain the '|' character.");
        }
        return name;
    }

    /** Category: required, at most 50 characters, no '|'. */
    public static String validateCategory(String raw) throws InventoryException {
        String category = trim(raw);
        if (category.isEmpty()) {
            throw new InventoryException("Category is required.");
        }
        if (category.length() > MAX_CATEGORY_LENGTH) {
            throw new InventoryException("Category must be at most " + MAX_CATEGORY_LENGTH + " characters.");
        }
        if (category.indexOf('|') >= 0) {
            throw new InventoryException("Category must not contain the '|' character.");
        }
        return category;
    }

    /** Quantity on the Add form: a whole number, 0 or more. */
    public static int parseQuantity(String raw) throws InventoryException {
        int quantity = parseWholeNumber(raw, "Quantity");
        if (quantity < 0) {
            throw new InventoryException("Quantity cannot be negative.");
        }
        return quantity;
    }

    /** Price: a number greater than 0. */
    public static double parsePrice(String raw) throws InventoryException {
        String text = trim(raw);
        if (text.isEmpty()) {
            throw new InventoryException("Price is required.");
        }
        double price;
        try {
            price = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new InventoryException("Price must be a number, for example 12.50.");
        }
        if (price <= 0) {
            throw new InventoryException("Price must be greater than 0.");
        }
        return price;
    }

    /** Reorder level: a whole number, 0 or more. */
    public static int parseReorderLevel(String raw) throws InventoryException {
        int level = parseWholeNumber(raw, "Reorder level");
        if (level < 0) {
            throw new InventoryException("Reorder level cannot be negative.");
        }
        return level;
    }

    /** Amount typed in the Stock In / Stock Out dialog: a whole number greater than 0. */
    public static int parseStockAmount(String raw) throws InventoryException {
        int amount = parseWholeNumber(raw, "Amount");
        if (amount <= 0) {
            throw new InventoryException("Amount must be greater than 0.");
        }
        return amount;
    }

    /** The search box must not be empty. */
    public static String validateSearchKeyword(String raw) throws InventoryException {
        String keyword = trim(raw);
        if (keyword.isEmpty()) {
            throw new InventoryException("Please type something to search for.");
        }
        return keyword;
    }

    /** Trims the text, treating null as an empty string. */
    public static String trim(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    /** Shared whole-number parsing with a friendly message. */
    private static int parseWholeNumber(String raw, String label) throws InventoryException {
        String text = trim(raw);
        if (text.isEmpty()) {
            throw new InventoryException(label + " is required.");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InventoryException(label + " must be a whole number, for example 25.");
        }
    }
}
