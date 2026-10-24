package inventory.gui;

import inventory.algorithm.SortAlgorithm;
import inventory.model.Product;
import inventory.model.Transaction;
import inventory.service.InventoryException;
import inventory.service.InventoryService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * The single window of the application.
 *
 * <p>The frame only reads the text fields, calls {@link InventoryService} and
 * shows the result. Every rule (validation, duplicate IDs, stock limits, undo)
 * lives in the service layer, so nothing here has to be explained twice.</p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** Table order of the design; Reorder Level is shown in the dialogs only. */
    private static final String[] COLUMNS = {"ID", "Product", "Category", "Price", "Quantity", "Status"};
    private static final String[] SEARCH_TYPES = {"Name", "Category", "Product ID"};
    private static final String[] SORT_ORDERS = {"Ascending", "Descending"};

    private static final int PRICE_COLUMN = 3;
    private static final int QUANTITY_COLUMN = 4;
    private static final int STATUS_COLUMN = 5;

    private static final Color LOW_STOCK_BACKGROUND = new Color(255, 228, 225);
    private static final Color LOW_STOCK_FOREGROUND = new Color(150, 20, 20);

    // Restyle palette - change these to re-theme the window (see INVENTORY_UI_RESTYLE.md).
    private static final Color READ_COLOR = new Color(0xE8772E);
    private static final Color DELETE_COLOR = new Color(0xE41E4D);
    private static final Color EDIT_COLOR = new Color(0x2D5FE0);
    private static final Color ADD_COLOR = new Color(0x10B981);
    private static final Color NEUTRAL_COLOR = new Color(0x475569);
    private static final Color CARD_GREEN = new Color(0xE6F7EF);
    private static final Color CARD_BLUE = new Color(0xE8F0FE);
    private static final Color CARD_RED = new Color(0xFDE8EC);
    private static final Color CARD_AMBER = new Color(0xFFF4E0);
    private static final Color CARD_GREEN_TEXT = new Color(0x059669);
    private static final Color CARD_BLUE_TEXT = new Color(0x1A47C0);
    private static final Color CARD_RED_TEXT = new Color(0xB31241);
    private static final Color CARD_AMBER_TEXT = new Color(0xB45309);
    private static final Color CARD_TITLE_TEXT = new Color(0x555F6D);

    private final InventoryService service;
    private final DefaultTableModel tableModel;
    private final JTable table;

    /** Products in the same order as the table rows, used to map a row to an object. */
    private Product[] displayedProducts = new Product[0];

    private JComboBox<String> searchTypeCombo;
    private JTextField searchField;
    private JComboBox<String> sortFieldCombo;
    private JComboBox<String> sortOrderCombo;
    private JCheckBox lowStockOnlyCheck;

    /** The four stat cards, refreshed with the table. */
    private JLabel productCountLabel;
    private JLabel valueLabel;
    private JLabel lowStockLabel;
    private JLabel outOfStockLabel;

    private String startupNotice;

    public MainFrame(InventoryService service) {
        super("Inventory Management System");
        this.service = service;
        this.tableModel = createTableModel();
        this.table = new JTable(tableModel);

        loadDataOnStartup();
        buildUi();
        refreshTable();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onExit();
            }
        });

        setMinimumSize(new Dimension(900, 600));
        setSize(1000, 660);
        setLocationRelativeTo(null);

        if (startupNotice != null) {
            final String message = startupNotice;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    showWarning(message);
                }
            });
        }
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(buildStatCardsPanel(), BorderLayout.NORTH);
        top.add(buildControlBandPanel(), BorderLayout.CENTER);

        content.add(top, BorderLayout.NORTH);
        content.add(buildTablePanel(), BorderLayout.CENTER);
        setContentPane(content);
        setJMenuBar(buildMenuBar());
    }

    // ------------------------------------------------------------------
    // Building the window
    // ------------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(createMenuItem("Save", KeyEvent.VK_S, () -> onSave()));
        fileMenu.add(createMenuItem("Load", KeyEvent.VK_R, () -> onLoad()));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", KeyEvent.VK_Q, () -> onExit()));

        JMenu inventoryMenu = new JMenu("Inventory");
        inventoryMenu.setMnemonic(KeyEvent.VK_I);
        inventoryMenu.add(createMenuItem("Add Product", KeyEvent.VK_N, () -> onAdd()));
        inventoryMenu.add(createMenuItem("Update Product", KeyEvent.VK_U, () -> onUpdate()));
        inventoryMenu.add(createMenuItem("Delete Product", KeyEvent.VK_D, () -> onDelete()));

        JMenu stockMenu = new JMenu("Stock");
        stockMenu.setMnemonic(KeyEvent.VK_S);
        stockMenu.add(createMenuItem("Stock In", KeyEvent.VK_I, () -> onStockIn()));
        stockMenu.add(createMenuItem("Stock Out", KeyEvent.VK_O, () -> onStockOut()));
        stockMenu.addSeparator();
        stockMenu.add(createMenuItem("Undo Last Action", KeyEvent.VK_Z, () -> onUndo()));

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(createMenuItem("Transaction History", KeyEvent.VK_H, () -> onHistory()));
        viewMenu.add(createMenuItem("Show All", KeyEvent.VK_A, () -> onShowAll()));
        viewMenu.add(createMenuItem("Low-Stock Only", KeyEvent.VK_L, () -> onLowStockOnly()));

        menuBar.add(fileMenu);
        menuBar.add(inventoryMenu);
        menuBar.add(stockMenu);
        menuBar.add(viewMenu);
        return menuBar;
    }

    /**
     * One band directly under the stat cards: the search/sort rows on the left,
     * the action buttons right-aligned on the right.
     */
    private JPanel buildControlBandPanel() {
        JPanel band = new JPanel(new BorderLayout(12, 0));
        band.add(buildControlPanel(), BorderLayout.CENTER);
        band.add(buildActionButtonsPanel(), BorderLayout.EAST);
        return band;
    }

    private JPanel buildControlPanel() {
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 4));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        searchTypeCombo = new JComboBox<>(SEARCH_TYPES);
        searchField = new JTextField(20);
        searchField.addActionListener(event -> onSearch());
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchTypeCombo);
        searchRow.add(searchField);
        searchRow.add(createButton("Search", NEUTRAL_COLOR, () -> onSearch()));
        searchRow.add(createButton("Show All", NEUTRAL_COLOR, () -> onShowAll()));

        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        sortOrderCombo = new JComboBox<>(SORT_ORDERS);
        sortFieldCombo = new JComboBox<>(SortAlgorithm.FIELDS);
        lowStockOnlyCheck = new JCheckBox("Low-Stock Only");
        lowStockOnlyCheck.setOpaque(false);
        lowStockOnlyCheck.addActionListener(event -> onLowStockFilterChanged());
        sortRow.add(new JLabel("Sort:"));
        sortRow.add(sortOrderCombo);
        sortRow.add(sortFieldCombo);
        sortRow.add(createButton("Sort", NEUTRAL_COLOR, () -> onSort()));
        sortRow.add(lowStockOnlyCheck);

        controls.add(searchRow);
        controls.add(sortRow);
        return controls;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(0xE2E8F0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        int[] widths = {70, 220, 130, 90, 90, 90};
        LowStockRenderer renderer = new LowStockRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatCardsPanel() {
        productCountLabel = new JLabel("0");
        valueLabel = new JLabel("P0.00");
        lowStockLabel = new JLabel("0");
        outOfStockLabel = new JLabel("0");

        JPanel cards = new JPanel(new GridLayout(1, 4, 10, 10));
        cards.add(createStatCard("Total Products", productCountLabel, CARD_GREEN, CARD_GREEN_TEXT));
        cards.add(createStatCard("Total Value", valueLabel, CARD_BLUE, CARD_BLUE_TEXT));
        cards.add(createStatCard("Low Stock", lowStockLabel, CARD_RED, CARD_RED_TEXT));
        cards.add(createStatCard("Out of Stock", outOfStockLabel, CARD_AMBER, CARD_AMBER_TEXT));
        return cards;
    }

    /** One rounded card: small title on top, big colored value underneath. */
    private RoundedPanel createStatCard(String title, JLabel valueLabel, Color background, Color valueColor) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        titleLabel.setForeground(CARD_TITLE_TEXT);
        valueLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        valueLabel.setForeground(valueColor);

        RoundedPanel card = new RoundedPanel(background, 16);
        card.setLayout(new GridLayout(2, 1, 0, 2));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.add(titleLabel);
        card.add(valueLabel);
        return card;
    }

    /** Action buttons for the right side of the band, aligned to its east edge. */
    private JPanel buildActionButtonsPanel() {
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        firstRow.add(createButton("Stock In", NEUTRAL_COLOR, () -> onStockIn()));
        firstRow.add(createButton("Stock Out", NEUTRAL_COLOR, () -> onStockOut()));
        firstRow.add(createButton("Undo", NEUTRAL_COLOR, () -> onUndo()));

        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        secondRow.add(createButton("Read", READ_COLOR, () -> onRead()));
        secondRow.add(createButton("Delete", DELETE_COLOR, () -> onDelete()));
        secondRow.add(createButton("Edit", EDIT_COLOR, () -> onUpdate()));
        secondRow.add(createButton("Add", ADD_COLOR, () -> onAdd()));

        JPanel rows = new JPanel(new GridLayout(2, 1));
        rows.add(firstRow);
        rows.add(secondRow);
        return rows;
    }

    /** Every button on the window is built here; only the color changes. */
    private JButton createButton(String text, Color baseColor, Runnable action) {
        JButton button = new RoundedButton(text, baseColor);
        button.addActionListener(event -> action.run());
        return button;
    }

    private JMenuItem createMenuItem(String text, int acceleratorKey, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.setAccelerator(KeyStroke.getKeyStroke(acceleratorKey, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(event -> action.run());
        return item;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void onAdd() {
        ProductDialog dialog = new ProductDialog(this, service, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
            showInfo("Product added.");
        }
    }

    private void onUpdate() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Select a product first.");
            return;
        }
        ProductDialog dialog = new ProductDialog(this, service, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
            showInfo("Product updated.");
        }
    }

    /** Read: shows every field of the selected product without opening a form. */
    private void onRead() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Select a product first.");
            return;
        }
        String message = "ID: " + selected.getId() + System.lineSeparator()
                + "Product: " + selected.getName() + System.lineSeparator()
                + "Category: " + selected.getCategory() + System.lineSeparator()
                + "Price: " + String.format("%.2f", selected.getPrice()) + System.lineSeparator()
                + "Quantity: " + selected.getQuantity() + System.lineSeparator()
                + "Reorder Level: " + selected.getReorderLevel() + System.lineSeparator()
                + "Status: " + statusOf(selected);
        JOptionPane.showMessageDialog(this, message, "Product Details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void onDelete() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Select a product first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete product " + selected.getId() + " - " + selected.getName() + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.deleteProduct(selected.getId());
            refreshTable();
            showInfo("Product deleted. Use Undo Last Action to restore it.");
        } catch (InventoryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onStockIn() {
        stockDialog(true);
    }

    private void onStockOut() {
        stockDialog(false);
    }

    private void stockDialog(boolean stockIn) {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Select a product first.");
            return;
        }
        StockDialog dialog = new StockDialog(this, service, selected, stockIn);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
            showInfo(stockIn ? "Stock In recorded." : "Stock Out recorded.");
        }
    }

    private void onUndo() {
        try {
            Transaction undo = service.undoLast();
            refreshTable();
            showInfo("Undone: " + undo.getUndoTargetType() + " of product "
                    + undo.getProductId() + " (" + undo.getProductName() + ").");
        } catch (InventoryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onHistory() {
        HistoryDialog dialog = new HistoryDialog(this, service, () -> refreshTable());
        dialog.setVisible(true);
    }

    private void onSave() {
        try {
            service.save();
            showInfo("Saved " + service.getProductCount() + " product(s) and "
                    + service.getHistorySize() + " transaction(s) to the data folder.");
        } catch (InventoryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onLoad() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Replace the products in memory with the content of the data files?",
                "Confirm Reload", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            InventoryService.LoadReport report = service.load();
            refreshTable();
            String message = "Loaded " + report.getProductCount() + " product(s) and "
                    + report.getTransactionCount() + " transaction(s).";
            if (report.hasSkippedLines()) {
                message = message + System.lineSeparator() + "Skipped "
                        + report.getSkippedProducts() + " corrupted product line(s) and "
                        + report.getSkippedTransactions() + " corrupted transaction line(s).";
            }
            showInfo(message);
        } catch (InventoryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onSearch() {
        String keyword = searchField.getText();
        String type = (String) searchTypeCombo.getSelectedItem();
        try {
            Product[] results;
            if (SEARCH_TYPES[2].equals(type)) {
                results = service.searchById(keyword);
            } else if (SEARCH_TYPES[1].equals(type)) {
                results = service.searchByCategory(keyword);
            } else {
                results = service.searchByName(keyword);
            }
            if (results.length == 0) {
                showInfo("No matching product found.");
                return;
            }
            showProducts(results);
        } catch (InventoryException ex) {
            showError(ex.getMessage());
        }
    }

    private void onSort() {
        String field = (String) sortFieldCombo.getSelectedItem();
        boolean ascending = SORT_ORDERS[0].equals(sortOrderCombo.getSelectedItem());
        showProducts(service.sort(field, ascending));
    }

    /** View menu entry: switches the filter on and shows the low-stock rows. */
    private void onLowStockOnly() {
        Product[] lowStock = service.getLowStockProducts();
        if (lowStock.length == 0) {
            showInfo("No low-stock products.");
            return;
        }
        lowStockOnlyCheck.setSelected(true);
        showProducts(lowStock);
    }

    /** The Low-Stock Only check box: filter while selected, full list when cleared. */
    private void onLowStockFilterChanged() {
        if (lowStockOnlyCheck.isSelected()) {
            Product[] lowStock = service.getLowStockProducts();
            showProducts(lowStock);
            if (lowStock.length == 0) {
                showInfo("No low-stock products.");
            }
        } else {
            refreshTable();
        }
    }

    private void onShowAll() {
        lowStockOnlyCheck.setSelected(false);
        searchField.setText("");
        refreshTable();
    }

    private void onExit() {
        try {
            service.save();
        } catch (InventoryException ex) {
            showWarning("The data could not be saved: " + ex.getMessage()
                    + System.lineSeparator() + "The application will close anyway.");
        }
        dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------
    // Table and status helpers
    // ------------------------------------------------------------------

    /** Loads the data files on start-up and remembers a problem for later. */
    private void loadDataOnStartup() {
        try {
            InventoryService.LoadReport report = service.load();
            if (report.hasSkippedLines()) {
                startupNotice = "Skipped " + report.getSkippedProducts()
                        + " corrupted product line(s) and " + report.getSkippedTransactions()
                        + " corrupted transaction line(s) while loading.";
            }
        } catch (InventoryException ex) {
            startupNotice = ex.getMessage();
        }
    }

    /** Reloads the product list from the service and repaints the table. */
    private void refreshTable() {
        displayedProducts = lowStockOnly() ? service.getLowStockProducts() : service.getAllProducts();
        fillTable();
    }

    /** True while the Low-Stock Only filter is switched on. */
    private boolean lowStockOnly() {
        return lowStockOnlyCheck != null && lowStockOnlyCheck.isSelected();
    }

    /** Shows a search/sort/low-stock result instead of the full list. */
    private void showProducts(Product[] products) {
        displayedProducts = products;
        fillTable();
    }

    private void fillTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < displayedProducts.length; i++) {
            Product product = displayedProducts[i];
            tableModel.addRow(new Object[]{
                product.getId(),
                product.getName(),
                product.getCategory(),
                String.format("%.2f", product.getPrice()),
                Integer.valueOf(product.getQuantity()),
                statusOf(product)
            });
        }
        table.clearSelection();
        updateStatus();
    }

    /** Display-only status text; it is never typed in or written to the files. */
    private static String statusOf(Product product) {
        if (product.getQuantity() == 0) {
            return "Out";
        }
        return product.isLowStock() ? "Low" : "OK";
    }

    /** Refreshes the four stat cards; called from {@link #fillTable()}. */
    private void updateStatus() {
        productCountLabel.setText(String.valueOf(service.getProductCount()));
        valueLabel.setText("P" + String.format("%.2f", service.getInventoryValue()));
        lowStockLabel.setText(String.valueOf(service.getLowStockCount()));
        outOfStockLabel.setText(String.valueOf(countOutOfStock()));
    }

    /** No getter exists for this, so it is counted here from the returned array. */
    private int countOutOfStock() {
        Product[] all = service.getAllProducts();
        int count = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i].getQuantity() == 0) {
                count++;
            }
        }
        return count;
    }

    /** Maps the selected table row to the product it was built from. */
    private Product getSelectedProduct() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedProducts.length) {
            return null;
        }
        return displayedProducts[row];
    }

    // ------------------------------------------------------------------
    // Messages
    // ------------------------------------------------------------------

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Inventory Management",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Inventory Management",
                JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Problem", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Row colours
    // ------------------------------------------------------------------

    /** Paints low-stock rows so they stand out even without the filter. */
    private class LowStockRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable source, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(source, value, isSelected,
                    hasFocus, row, column);
            int modelColumn = source.convertColumnIndexToModel(column);
            if (modelColumn == PRICE_COLUMN || modelColumn == QUANTITY_COLUMN) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (modelColumn == STATUS_COLUMN) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            boolean lowStock = row >= 0 && row < displayedProducts.length
                    && displayedProducts[row].isLowStock();
            if (isSelected) {
                component.setBackground(lowStock ? new Color(205, 145, 145) : source.getSelectionBackground());
                component.setForeground(lowStock ? Color.WHITE : source.getSelectionForeground());
            } else {
                component.setBackground(lowStock ? LOW_STOCK_BACKGROUND : source.getBackground());
                component.setForeground(lowStock ? LOW_STOCK_FOREGROUND : source.getForeground());
            }
            return component;
        }
    }
}
