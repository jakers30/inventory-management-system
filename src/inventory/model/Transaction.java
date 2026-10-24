package inventory.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * One row of the transaction history (the stack in {@code inventory.structures.TransactionStack}).
 *
 * <p>The history is an append-only audit log: an action is never deleted from it.
 * When an action is undone, the original row is only flagged with
 * {@link #isUndone()} and a brand new {@code UNDO} row is added on top, so the
 * log keeps both the event and its reversal.</p>
 *
 * <p>File format used by {@code data/transactions.txt} (12 pipe-delimited fields):</p>
 * <pre>type|productId|productName|quantityChange|timestamp|prevName|prevCategory|
 * prevQuantity|prevPrice|prevReorderLevel|undone|undoTargetType</pre>
 */
public class Transaction {

    public static final String ADD = "ADD";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String STOCK_IN = "STOCK_IN";
    public static final String STOCK_OUT = "STOCK_OUT";
    public static final String UNDO = "UNDO";

    private String type;
    private String productId;
    private String productName;
    private int quantityChange;
    private String timestamp;

    // Snapshot of the product BEFORE the action, so Undo can restore it.
    private String prevName = "";
    private String prevCategory = "";
    private int prevQuantity;
    private double prevPrice;
    private int prevReorderLevel;

    private boolean undone;
    private String undoTargetType = "";

    /** Full constructor, used by the file loader and the factory methods below. */
    public Transaction(String type, String productId, String productName, int quantityChange,
                       String timestamp, String prevName, String prevCategory, int prevQuantity,
                       double prevPrice, int prevReorderLevel, boolean undone, String undoTargetType) {
        this.type = type;
        this.productId = productId;
        this.productName = productName;
        this.quantityChange = quantityChange;
        this.timestamp = timestamp;
        setSnapshot(prevName, prevCategory, prevQuantity, prevPrice, prevReorderLevel);
        this.undone = undone;
        this.undoTargetType = (undoTargetType == null) ? "" : undoTargetType;
    }

    /**
     * Logs one action.
     *
     * @param type           one of the {@code ADD/UPDATE/DELETE/STOCK_IN/STOCK_OUT} constants
     * @param product        the product after the action (only its ID and name are copied)
     * @param quantityChange +n for Stock In, -n for Stock Out, 0 otherwise
     * @param previous       snapshot of the product before the action, or {@code null}
     */
    public Transaction(String type, Product product, int quantityChange, Product previous) {
        this(type,
             product.getId(),
             product.getName(),
             quantityChange,
             now(),
             previous == null ? "" : previous.getName(),
             previous == null ? "" : previous.getCategory(),
             previous == null ? 0 : previous.getQuantity(),
             previous == null ? 0.0 : previous.getPrice(),
             previous == null ? 0 : previous.getReorderLevel(),
             false,
             "");
    }

    /**
     * Creates the {@code UNDO} row that is written on top of the action it reversed.
     *
     * @param reversed          the action that was just undone
     * @param netQuantityChange the quantity change actually applied by the undo
     *                          (e.g. -10 when reversing a Stock In of +10)
     */
    public static Transaction createUndo(Transaction reversed, int netQuantityChange) {
        return new Transaction(UNDO,
                reversed.getProductId(),
                reversed.getProductName(),
                netQuantityChange,
                now(),
                "", "", 0, 0.0, 0,
                false,
                reversed.getType());
    }

    /** True for a row that only records an undo (never undoable itself). */
    public boolean isUndoRecord() {
        return UNDO.equals(type);
    }

    /** True when this row can still be reversed by Undo Last Action. */
    public boolean isUndoable() {
        return !undone && !isUndoRecord();
    }

    /** True when the snapshot contains the values needed to restore the product. */
    public boolean hasSnapshot() {
        return prevName != null && !prevName.isEmpty();
    }

    private void setSnapshot(String name, String category, int quantity, double price, int reorder) {
        this.prevName = (name == null) ? "" : name;
        this.prevCategory = (category == null) ? "" : category;
        this.prevQuantity = quantity;
        this.prevPrice = price;
        this.prevReorderLevel = reorder;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public String getType() {
        return type;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPrevName() {
        return prevName;
    }

    public String getPrevCategory() {
        return prevCategory;
    }

    public int getPrevQuantity() {
        return prevQuantity;
    }

    public double getPrevPrice() {
        return prevPrice;
    }

    public int getPrevReorderLevel() {
        return prevReorderLevel;
    }

    public boolean isUndone() {
        return undone;
    }

    public void setUndone(boolean undone) {
        this.undone = undone;
    }

    public String getUndoTargetType() {
        return undoTargetType;
    }

    /** One line for data/transactions.txt. */
    public String toFileString() {
        return type + "|" + productId + "|" + productName + "|" + quantityChange + "|" + timestamp
                + "|" + prevName + "|" + prevCategory + "|" + prevQuantity + "|" + prevPrice
                + "|" + prevReorderLevel + "|" + (undone ? "1" : "0") + "|" + undoTargetType;
    }

    /**
     * Reads one line of data/transactions.txt.
     *
     * @return the transaction, or {@code null} when the line is corrupted
     *         (wrong field count, unknown type, bad number).
     */
    public static Transaction fromFileString(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length != 12) {
            return null;
        }
        String type = parts[0].trim();
        if (!isKnownType(type)) {
            return null;
        }
        try {
            int quantityChange = Integer.parseInt(parts[3].trim());
            int prevQuantity = parseIntOrZero(parts[7]);
            double prevPrice = parseDoubleOrZero(parts[8]);
            int prevReorderLevel = parseIntOrZero(parts[9]);
            boolean undone = "1".equals(parts[10].trim()) || "true".equalsIgnoreCase(parts[10].trim());
            return new Transaction(type,
                    parts[1].trim(),
                    parts[2].trim(),
                    quantityChange,
                    parts[4].trim(),
                    parts[5].trim(),
                    parts[6].trim(),
                    prevQuantity,
                    prevPrice,
                    prevReorderLevel,
                    undone,
                    parts[11].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isKnownType(String type) {
        return ADD.equals(type) || UPDATE.equals(type) || DELETE.equals(type)
                || STOCK_IN.equals(type) || STOCK_OUT.equals(type) || UNDO.equals(type);
    }

    private static int parseIntOrZero(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(text);
    }

    private static double parseDoubleOrZero(String raw) {
        String text = raw.trim();
        if (text.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(text);
    }

    @Override
    public String toString() {
        return timestamp + " " + type + " " + productId
                + (isUndoRecord() ? " (reverses " + undoTargetType + ")" : "")
                + (undone ? " [undone]" : "");
    }
}
