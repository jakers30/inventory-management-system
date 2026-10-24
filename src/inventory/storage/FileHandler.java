package inventory.storage;

import inventory.model.Product;
import inventory.model.Transaction;
import inventory.service.InventoryException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Reads and writes the plain text files in the {@code data/} folder.
 *
 * <p>Only file work happens here: no business rules and no GUI calls. Reading
 * uses {@code BufferedReader} and writing uses {@code BufferedWriter}, both with
 * try-with-resources. Lines that cannot be parsed are skipped and counted so the
 * user can be told how many were ignored.</p>
 */
public class FileHandler {

    public static final String DEFAULT_DIRECTORY = "data";
    public static final String PRODUCTS_FILE = "products.txt";
    public static final String TRANSACTIONS_FILE = "transactions.txt";

    private final File directory;

    public FileHandler() {
        this(DEFAULT_DIRECTORY);
    }

    public FileHandler(String directoryPath) {
        this.directory = new File(directoryPath);
    }

    public File getDirectory() {
        return directory;
    }

    /** Overwrites data/products.txt with the given products, one per line. */
    public void saveProducts(Product[] products) throws InventoryException {
        try {
            ensureDirectory();
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(new File(directory, PRODUCTS_FILE)))) {
                for (int i = 0; i < products.length; i++) {
                    writer.write(products[i].toFileString());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new InventoryException("Could not save the data file: " + e.getMessage());
        }
    }

    /**
     * Overwrites data/transactions.txt. The array arrives newest-first (the stack
     * order) and is written from the last element down, so the file ends up
     * oldest-first. Reloading pushes the lines in that order, which puts the
     * newest row back on top of the stack.
     */
    public void saveTransactionsNewestFirst(Transaction[] newestFirst) throws InventoryException {
        try {
            ensureDirectory();
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(new File(directory, TRANSACTIONS_FILE)))) {
                for (int i = newestFirst.length - 1; i >= 0; i--) {
                    writer.write(newestFirst[i].toFileString());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new InventoryException("Could not save the data file: " + e.getMessage());
        }
    }

    /**
     * Writes the products to the given file as a CSV report.
     *
     * <p>The file starts with the header {@code ID,Product,Category,Price,Quantity,Status},
     * then one line per product in the order the array arrives (so the report keeps the
     * table's current filter and sort), and finally a summary block with the total
     * number of products, the total value, the low stock count, the out of stock count
     * and the date and time the report was written. The summary describes exactly the
     * products that were written, so a filtered export adds up to the filtered rows.</p>
     */
    public void writeReport(File file, Product[] products) throws InventoryException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("ID,Product,Category,Price,Quantity,Status");
            writer.newLine();

            double totalValue = 0;
            int lowStockCount = 0;
            int outOfStockCount = 0;
            for (int i = 0; i < products.length; i++) {
                Product product = products[i];
                writer.write(csvField(product.getId()));
                writer.write(',');
                writer.write(csvField(product.getName()));
                writer.write(',');
                writer.write(csvField(product.getCategory()));
                writer.write(',');
                writer.write(String.format(Locale.US, "%.2f", product.getPrice()));
                writer.write(',');
                writer.write(String.valueOf(product.getQuantity()));
                writer.write(',');
                writer.write(statusOf(product));
                writer.newLine();

                totalValue += product.getStockValue();
                if (product.isLowStock()) {
                    lowStockCount++;
                }
                if (product.getQuantity() == 0) {
                    outOfStockCount++;
                }
            }

            writer.newLine();
            writer.write("Summary");
            writer.newLine();
            writer.write("Total products," + products.length);
            writer.newLine();
            writer.write("Total value," + String.format(Locale.US, "%.2f", totalValue));
            writer.newLine();
            writer.write("Low stock count," + lowStockCount);
            writer.newLine();
            writer.write("Out of stock count," + outOfStockCount);
            writer.newLine();
            writer.write("Generated," + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.newLine();
        } catch (IOException e) {
            throw new InventoryException("Could not write the report file: " + e.getMessage());
        }
    }

    /**
     * Reads both files.
     *
     * <p>Blank lines are ignored, corrupted lines are skipped and counted. When a
     * file does not exist yet it is created empty and an empty array is returned,
     * so the first run never crashes.</p>
     */
    public LoadResult loadAll() throws InventoryException {
        String[] productLines;
        String[] transactionLines;
        try {
            ensureDirectory();
            productLines = readLines(new File(directory, PRODUCTS_FILE));
            transactionLines = readLines(new File(directory, TRANSACTIONS_FILE));
        } catch (IOException e) {
            throw new InventoryException("Could not load the data file: " + e.getMessage());
        }

        Product[] products = new Product[productLines.length];
        int productCount = 0;
        int skippedProducts = 0;
        for (int i = 0; i < productLines.length; i++) {
            Product product = Product.fromFileString(productLines[i]);
            if (product == null) {
                skippedProducts++;
            } else {
                products[productCount++] = product;
            }
        }

        Transaction[] transactions = new Transaction[transactionLines.length];
        int transactionCount = 0;
        int skippedTransactions = 0;
        for (int i = 0; i < transactionLines.length; i++) {
            Transaction transaction = Transaction.fromFileString(transactionLines[i]);
            if (transaction == null) {
                skippedTransactions++;
            } else {
                transactions[transactionCount++] = transaction;
            }
        }

        return new LoadResult(compactProducts(products, productCount),
                compactTransactions(transactions, transactionCount),
                skippedProducts, skippedTransactions);
    }

    /** Reads every non-blank line, growing a plain array as needed. */
    private String[] readLines(File file) throws IOException {
        String[] lines = new String[16];
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty()) {
                    if (count == lines.length) {
                        String[] bigger = new String[lines.length * 2];
                        for (int i = 0; i < count; i++) {
                            bigger[i] = lines[i];
                        }
                        lines = bigger;
                    }
                    lines[count++] = line;
                }
                line = reader.readLine();
            }
        }
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = lines[i];
        }
        return result;
    }

    private Product[] compactProducts(Product[] products, int count) {
        Product[] result = new Product[count];
        for (int i = 0; i < count; i++) {
            result[i] = products[i];
        }
        return result;
    }

    private Transaction[] compactTransactions(Transaction[] transactions, int count) {
        Transaction[] result = new Transaction[count];
        for (int i = 0; i < count; i++) {
            result[i] = transactions[i];
        }
        return result;
    }

    /**
     * Wraps a CSV field in quotes when it holds a comma, a quote or a line break,
     * doubling any quotes inside it so the file stays readable by spreadsheets.
     */
    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Same three words the table shows in its Status column: Out, Low, OK. */
    private String statusOf(Product product) {
        if (product.getQuantity() == 0) {
            return "Out";
        }
        return product.isLowStock() ? "Low" : "OK";
    }

    /** Creates data/ and the two files when they are missing. */
    private void ensureDirectory() throws IOException {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("cannot create the folder " + directory.getPath());
        }
        createEmptyFileIfMissing(PRODUCTS_FILE);
        createEmptyFileIfMissing(TRANSACTIONS_FILE);
    }

    private void createEmptyFileIfMissing(String fileName) throws IOException {
        File file = new File(directory, fileName);
        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("cannot create the file " + file.getPath());
        }
    }

    /** What a load produced: the data that could be read plus the skipped counts. */
    public static class LoadResult {

        private final Product[] products;
        private final Transaction[] transactions;
        private final int skippedProducts;
        private final int skippedTransactions;

        public LoadResult(Product[] products, Transaction[] transactions,
                          int skippedProducts, int skippedTransactions) {
            this.products = products;
            this.transactions = transactions;
            this.skippedProducts = skippedProducts;
            this.skippedTransactions = skippedTransactions;
        }

        public Product[] getProducts() {
            return products;
        }

        /** Oldest first, the order they were written to the file. */
        public Transaction[] getTransactions() {
            return transactions;
        }

        public int getSkippedProducts() {
            return skippedProducts;
        }

        public int getSkippedTransactions() {
            return skippedTransactions;
        }
    }
}
