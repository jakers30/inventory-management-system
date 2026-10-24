package inventory.model;

/**
 * One product kept in the inventory.
 *
 * <p>File format used by {@code data/products.txt} (6 pipe-delimited fields):</p>
 * <pre>id|name|category|quantity|price|reorderLevel</pre>
 *
 * <p>The ID is the unique key of the product. It is set by the constructor and
 * never changes, because the hash table indexes products by it.</p>
 */
public class Product {

    private String id;
    private String name;
    private String category;
    private int quantity;
    private double price;
    private int reorderLevel;

    public Product(String id, String name, String category,
                   int quantity, double price, int reorderLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.reorderLevel = reorderLevel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    /** True when the product reached (or passed) its low-stock threshold. */
    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }

    /** quantity x price, used for the total inventory value. */
    public double getStockValue() {
        return quantity * price;
    }

    /**
     * Deep copy of this product. Used to take the "before" snapshot that
     * Undo needs, so later changes to the live object do not touch the snapshot.
     */
    public Product copy() {
        return new Product(id, name, category, quantity, price, reorderLevel);
    }

    /** One line for data/products.txt. */
    public String toFileString() {
        return id + "|" + name + "|" + category + "|" + quantity + "|" + price + "|" + reorderLevel;
    }

    /**
     * Reads one line of data/products.txt.
     *
     * @return the product, or {@code null} when the line is corrupted
     *         (wrong field count, bad number, empty ID/name). The caller counts
     *         the {@code null}s and reports how many lines were skipped.
     */
    public static Product fromFileString(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length != 6) {
            return null;
        }
        try {
            String id = parts[0].trim();
            String name = parts[1].trim();
            String category = parts[2].trim();
            int quantity = Integer.parseInt(parts[3].trim());
            double price = Double.parseDouble(parts[4].trim());
            int reorderLevel = Integer.parseInt(parts[5].trim());

            if (id.isEmpty() || name.isEmpty() || category.isEmpty()) {
                return null;
            }
            if (quantity < 0 || price <= 0 || reorderLevel < 0) {
                return null;
            }
            return new Product(id, name, category, quantity, price, reorderLevel);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
