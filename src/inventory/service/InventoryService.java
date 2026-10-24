package inventory.service;

import inventory.algorithm.BinarySearch;
import inventory.algorithm.LinearSearch;
import inventory.algorithm.SortAlgorithm;
import inventory.model.Product;
import inventory.model.Transaction;
import inventory.storage.FileHandler;
import inventory.structures.ProductHashTable;
import inventory.structures.ProductLinkedList;
import inventory.structures.TransactionStack;
import java.io.File;

/**
 * The only class the GUI talks to. It owns the three data structures, applies
 * every business rule, writes the transaction log and calls the algorithms.
 *
 * <ul>
 *   <li>{@link ProductLinkedList} - master list of products</li>
 *   <li>{@link ProductHashTable} - fast lookup / duplicate check by ID</li>
 *   <li>{@link TransactionStack} - undo history</li>
 * </ul>
 */
public class InventoryService {

    private final ProductLinkedList products = new ProductLinkedList();
    private final ProductHashTable index = new ProductHashTable();
    private final TransactionStack history = new TransactionStack();
    private final FileHandler storage;

    public InventoryService() {
        this(new FileHandler());
    }

    public InventoryService(FileHandler storage) {
        this.storage = storage;
    }

    // ------------------------------------------------------------------
    // Add / Update / Delete
    // ------------------------------------------------------------------

    /**
     * Validates the form text, checks the ID against the hash table, stores the
     * product and logs an {@code ADD} transaction.
     */
    public Product addProduct(String idText, String nameText, String categoryText,
                              String quantityText, String priceText, String reorderText)
            throws InventoryException {
        String id = Validator.validateId(idText);
        String name = Validator.validateName(nameText);
        String category = Validator.validateCategory(categoryText);
        int quantity = Validator.parseQuantity(quantityText);
        double price = Validator.parsePrice(priceText);
        int reorderLevel = Validator.parseReorderLevel(reorderText);

        if (index.containsKey(id)) {
            throw new InventoryException("Product ID already exists: " + id);
        }

        Product product = new Product(id, name, category, quantity, price, reorderLevel);
        addInternal(product);
        history.push(new Transaction(Transaction.ADD, product, 0, null));
        return product;
    }

    /**
     * Changes name, category, price and reorder level of an existing product.
     * The ID can never change and the quantity is only changed by Stock In/Out,
     * so an {@code UPDATE} transaction always changes those four fields.
     */
    public Product updateProduct(String idText, String nameText, String categoryText,
                                 String priceText, String reorderText) throws InventoryException {
        String id = Validator.validateId(idText);
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("No product found with ID: " + id);
        }
        String name = Validator.validateName(nameText);
        String category = Validator.validateCategory(categoryText);
        double price = Validator.parsePrice(priceText);
        int reorderLevel = Validator.parseReorderLevel(reorderText);

        Product snapshot = product.copy();
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setReorderLevel(reorderLevel);

        history.push(new Transaction(Transaction.UPDATE, product, 0, snapshot));
        return product;
    }

    /**
     * Removes a product from the list and the index, keeping a full snapshot in
     * the {@code DELETE} transaction so Undo can re-create it.
     */
    public Product deleteProduct(String idText) throws InventoryException {
        String id = Validator.validateId(idText);
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("No product found with ID: " + id);
        }
        Product snapshot = product.copy();
        removeInternal(id);
        history.push(new Transaction(Transaction.DELETE, snapshot, 0, snapshot));
        return snapshot;
    }

    // ------------------------------------------------------------------
    // Kept private helpers so the list and the hash table never drift apart
    // ------------------------------------------------------------------

    private void addInternal(Product product) {
        products.addLast(product);
        index.put(product);
    }

    private void removeInternal(String id) {
        products.remove(id);
        index.remove(id);
    }

    private void clearAll() {
        products.clear();
        index.clear();
        while (!history.isEmpty()) {
            history.pop();
        }
    }

    // ------------------------------------------------------------------
    // Stock In / Stock Out
    // ------------------------------------------------------------------

    /** Adds stock and logs a {@code STOCK_IN} transaction with the old quantity. */
    public Product stockIn(String idText, String amountText) throws InventoryException {
        String id = Validator.validateId(idText);
        int amount = Validator.parseStockAmount(amountText);
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("No product found with ID: " + id);
        }
        Product snapshot = product.copy();
        product.setQuantity(product.getQuantity() + amount);
        history.push(new Transaction(Transaction.STOCK_IN, product, amount, snapshot));
        return product;
    }

    /**
     * Removes stock and logs a {@code STOCK_OUT} transaction. Taking out more than
     * the quantity on hand is rejected, so a quantity never becomes negative.
     */
    public Product stockOut(String idText, String amountText) throws InventoryException {
        String id = Validator.validateId(idText);
        int amount = Validator.parseStockAmount(amountText);
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("No product found with ID: " + id);
        }
        if (amount > product.getQuantity()) {
            throw new InventoryException("Not enough stock: only " + product.getQuantity()
                    + " unit(s) of " + product.getId() + " available.");
        }
        Product snapshot = product.copy();
        product.setQuantity(product.getQuantity() - amount);
        history.push(new Transaction(Transaction.STOCK_OUT, product, -amount, snapshot));
        return product;
    }

    // ------------------------------------------------------------------
    // Undo
    // ------------------------------------------------------------------

    /**
     * Undoes the newest action that is still undoable and returns the new
     * {@code UNDO} transaction that was written to the log.
     *
     * <p>How the stack is walked: rows are popped until an undoable one is found.
     * Every skipped row (already undone, or an {@code UNDO} row itself) is held in
     * an array and pushed back in its original order, so the stack ends up exactly
     * as it was apart from the two rows this method adds: the reversed action gets
     * {@code undone = true} and a new {@code UNDO} row goes on top of it.</p>
     */
    public Transaction undoLast() throws InventoryException {
        Transaction[] held = new Transaction[history.size()];
        int heldCount = 0;
        Transaction target = null;

        while (!history.isEmpty()) {
            Transaction top = history.pop();
            if (top.isUndoable()) {
                target = top;
                break;
            }
            held[heldCount++] = top;
        }

        if (target == null) {
            pushBack(held, heldCount);
            throw new InventoryException("Nothing to undo.");
        }

        pushBack(held, heldCount);

        int netChange = reverse(target);

        target.setUndone(true);
        history.push(target);

        Transaction undoRecord = Transaction.createUndo(target, netChange);
        history.push(undoRecord);
        return undoRecord;
    }

    /**
     * Applies the opposite of the action.
     *
     * @return the net quantity change that was applied to the inventory
     */
    private int reverse(Transaction target) throws InventoryException {
        String type = target.getType();
        String id = target.getProductId();

        if (Transaction.ADD.equals(type)) {
            if (findById(id) == null) {
                throw new InventoryException("Cannot undo: product " + id + " no longer exists.");
            }
            removeInternal(id);
            return 0;
        }

        if (Transaction.DELETE.equals(type)) {
            if (findById(id) != null) {
                throw new InventoryException("Cannot undo the delete: product ID " + id
                        + " is already used again.");
            }
            addInternal(new Product(id, target.getPrevName(), target.getPrevCategory(),
                    target.getPrevQuantity(), target.getPrevPrice(), target.getPrevReorderLevel()));
            return 0;
        }

        // UPDATE, STOCK_IN and STOCK_OUT all restore the snapshot.
        Product product = findById(id);
        if (product == null) {
            throw new InventoryException("Cannot undo: product " + id + " no longer exists.");
        }
        int before = product.getQuantity();
        product.setName(target.getPrevName());
        product.setCategory(target.getPrevCategory());
        product.setQuantity(target.getPrevQuantity());
        product.setPrice(target.getPrevPrice());
        product.setReorderLevel(target.getPrevReorderLevel());
        return product.getQuantity() - before;
    }

    private void pushBack(Transaction[] held, int heldCount) {
        for (int i = heldCount - 1; i >= 0; i--) {
            history.push(held[i]);
        }
    }

    // ------------------------------------------------------------------
    // Lookups and totals
    // ------------------------------------------------------------------

    /**
     * Exact lookup by ID through the hash table (O(1) average).
     *
     * @return the product, or null when the ID is unknown
     */
    public Product findById(String idText) {
        String id = Validator.trim(idText);
        if (id.isEmpty()) {
            return null;
        }
        return index.get(id);
    }

    /** Every product, in the order it was added (the linked list order). */
    public Product[] getAllProducts() {
        return products.toArray();
    }

    public int getProductCount() {
        return products.size();
    }

    /** Sum of quantity x price over every product. */
    public double getInventoryValue() {
        Product[] all = products.toArray();
        double total = 0;
        for (int i = 0; i < all.length; i++) {
            total += all[i].getStockValue();
        }
        return total;
    }

    public int getLowStockCount() {
        Product[] all = products.toArray();
        int count = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i].isLowStock()) {
                count++;
            }
        }
        return count;
    }

    /** Products whose quantity is at or below the reorder level. */
    public Product[] getLowStockProducts() {
        Product[] all = products.toArray();
        int count = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i].isLowStock()) {
                count++;
            }
        }
        Product[] result = new Product[count];
        int position = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i].isLowStock()) {
                result[position++] = all[i];
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Searching and sorting (the three algorithms)
    // ------------------------------------------------------------------

    /**
     * ALGORITHM 2 - exact ID search with binary search. The products are copied,
     * sorted ascending by ID, and then searched in O(log n).
     */
    public Product[] searchById(String idText) throws InventoryException {
        String id = Validator.validateSearchKeyword(idText);
        Product[] sortedById = products.toArray();
        SortAlgorithm.sort(sortedById, SortAlgorithm.BY_ID, true);
        Product found = BinarySearch.findById(sortedById, id);
        if (found == null) {
            return new Product[0];
        }
        Product[] result = new Product[1];
        result[0] = found;
        return result;
    }

    /** ALGORITHM 1 - partial, case-insensitive name search with linear search. */
    public Product[] searchByName(String keyword) throws InventoryException {
        String value = Validator.validateSearchKeyword(keyword);
        return LinearSearch.searchByName(products.toArray(), value);
    }

    /** ALGORITHM 1 - partial, case-insensitive category search with linear search. */
    public Product[] searchByCategory(String keyword) throws InventoryException {
        String value = Validator.validateSearchKeyword(keyword);
        return LinearSearch.searchByCategory(products.toArray(), value);
    }

    /**
     * ALGORITHM 3 - returns a sorted copy of the products with insertion sort.
     * The linked list itself keeps its insertion order.
     */
    public Product[] sort(String field, boolean ascending) {
        Product[] copy = products.toArray();
        SortAlgorithm.sort(copy, field, ascending);
        return copy;
    }

    // ------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------

    /** All transactions, newest first (top of the stack first). */
    public Transaction[] getHistory() {
        return history.toArray();
    }

    public int getHistorySize() {
        return history.size();
    }

    // ------------------------------------------------------------------
    // Saving and loading
    // ------------------------------------------------------------------

    /** Writes data/products.txt and data/transactions.txt. */
    public void save() throws InventoryException {
        storage.saveProducts(products.toArray());
        storage.saveTransactionsNewestFirst(history.toArray());
    }

    /**
     * Writes the products that are on screen to a CSV report at the chosen file.
     *
     * <p>The rows are written in the order they arrive, so whatever search, filter
     * or sort the table is showing is exactly what the report contains. Nothing in
     * memory changes: this only reads the array it is given.</p>
     */
    public void exportReport(Product[] products, File file) throws InventoryException {
        storage.writeReport(file, products);
    }

    /**
     * Replaces everything in memory with the content of the data files.
     *
     * <p>Products that appear twice in the file are only kept once (the first
     * line wins) and counted as skipped. Transactions are pushed oldest-first so
     * the newest row ends up on top of the stack, exactly like before saving.</p>
     */
    public LoadReport load() throws InventoryException {
        FileHandler.LoadResult result = storage.loadAll();
        clearAll();

        int skippedProducts = result.getSkippedProducts();
        Product[] loadedProducts = result.getProducts();
        for (int i = 0; i < loadedProducts.length; i++) {
            Product product = loadedProducts[i];
            if (index.containsKey(product.getId())) {
                skippedProducts++;
                continue;
            }
            addInternal(product);
        }

        Transaction[] loadedTransactions = result.getTransactions();
        for (int i = 0; i < loadedTransactions.length; i++) {
            history.push(loadedTransactions[i]);
        }

        return new LoadReport(products.size(), history.size(),
                skippedProducts, result.getSkippedTransactions());
    }

    /** What a {@link #load()} produced, so the GUI can report it. */
    public static class LoadReport {

        private final int productCount;
        private final int transactionCount;
        private final int skippedProducts;
        private final int skippedTransactions;

        public LoadReport(int productCount, int transactionCount,
                          int skippedProducts, int skippedTransactions) {
            this.productCount = productCount;
            this.transactionCount = transactionCount;
            this.skippedProducts = skippedProducts;
            this.skippedTransactions = skippedTransactions;
        }

        public int getProductCount() {
            return productCount;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public int getSkippedProducts() {
            return skippedProducts;
        }

        public int getSkippedTransactions() {
            return skippedTransactions;
        }

        /** True when at least one corrupted line was ignored. */
        public boolean hasSkippedLines() {
            return skippedProducts > 0 || skippedTransactions > 0;
        }
    }
}
