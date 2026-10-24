package inventory.gui;

import inventory.algorithm.SortAlgorithm;
import inventory.model.Product;
import inventory.model.Transaction;
import inventory.service.InventoryException;
import inventory.service.InventoryService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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

    private static final String[] COLUMNS = {"ID", "Name", "Category", "Quantity", "Price", "Reorder Level"};
    private static final String[] SEARCH_TYPES = {"Name", "Category", "Product ID"};
    private static final String[] SORT_ORDERS = {"Ascending", "Descending"};

    private static final Color LOW_STOCK_BACKGROUND = new Color(255, 228, 225);
    private static final Color LOW_STOCK_FOREGROUND = new Color(150, 20, 20);

    private final InventoryService service;
    private final DefaultTableModel tableModel;
    private final JTable table;

    /** Products in the same order as the table rows, used to map a row to an object. */
    private Product[] displayedProducts = new Product[0];

    private JComboBox<String> searchTypeCombo;
    private JTextField searchField;
    private JComboBox<String> sortFieldCombo;
    private JComboBox<String> sortOrderCombo;
    private JLabel itemsLabel;
    private JLabel valueLabel;
    private JLabel lowStockLabel;

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

        setMinimumSize(new java.awt.Dimension(820, 460));
        setSize(980, 560);
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
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(buildSearchAndSortPanel(), BorderLayout.NORTH);
        content.add(buildTablePanel(), BorderLayout.CENTER);
        content.add(buildActionsAndStatusPanel(), BorderLayout.SOUTH);
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
        fileMenu.add(createMenuItem("Save Data", KeyEvent.VK_S, () -> onSave()));
        fileMenu.add(createMenuItem("Reload Data", KeyEvent.VK_R, () -> onLoad()));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", KeyEvent.VK_Q, () -> onExit()));

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(createMenuItem("Add Product", KeyEvent.VK_N, () -> onAdd()));
        editMenu.add(createMenuItem("Update Product", KeyEvent.VK_U, () -> onUpdate()));
        editMenu.add(createMenuItem("Delete Product", KeyEvent.VK_D, () -> onDelete()));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("Undo Last Action", KeyEvent.VK_Z, () -> onUndo()));

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(createMenuItem("Show All Products", KeyEvent.VK_A, () -> onShowAll()));
        viewMenu.add(createMenuItem("Low-Stock Only", KeyEvent.VK_L, () -> onLowStockOnly()));

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic(KeyEvent.VK_T);
        toolsMenu.add(createMenuItem("Stock In", KeyEvent.VK_I, () -> onStockIn()));
        toolsMenu.add(createMenuItem("Stock Out", KeyEvent.VK_O, () -> onStockOut()));
        toolsMenu.addSeparator();
        toolsMenu.add(createMenuItem("Transaction History", KeyEvent.VK_H, () -> onHistory()));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        return menuBar;
    }

    private JPanel buildSearchAndSortPanel() {
        JPanel top = new JPanel(new GridLayout(2, 1));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        searchTypeCombo = new JComboBox<>(SEARCH_TYPES);
        searchField = new JTextField(20);
        searchField.addActionListener(event -> onSearch());
        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchTypeCombo);
        searchRow.add(searchField);
        searchRow.add(createButton("Search", () -> onSearch()));
        searchRow.add(createButton("Show All", () -> onShowAll()));

        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        sortFieldCombo = new JComboBox<>(SortAlgorithm.FIELDS);
        sortOrderCombo = new JComboBox<>(SORT_ORDERS);
        sortRow.add(new JLabel("Sort by:"));
        sortRow.add(sortFieldCombo);
        sortRow.add(sortOrderCombo);
        sortRow.add(createButton("Sort", () -> onSort()));
        sortRow.add(new JLabel("   "));
        sortRow.add(createButton("Low-Stock Only", () -> onLowStockOnly()));

        top.add(searchRow);
        top.add(sortRow);
        return top;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);

        LowStockRenderer renderer = new LowStockRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildActionsAndStatusPanel() {
        JPanel bottom = new JPanel(new BorderLayout());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        actions.add(createButton("Add Product", () -> onAdd()));
        actions.add(createButton("Update Product", () -> onUpdate()));
        actions.add(createButton("Delete Product", () -> onDelete()));
        actions.add(createButton("Stock In", () -> onStockIn()));
        actions.add(createButton("Stock Out", () -> onStockOut()));
        actions.add(createButton("Undo Last Action", () -> onUndo()));

        JPanel status = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        itemsLabel = new JLabel();
        valueLabel = new JLabel();
        lowStockLabel = new JLabel();
        status.add(itemsLabel);
        status.add(valueLabel);
        status.add(lowStockLabel);

        bottom.add(actions, BorderLayout.WEST);
        bottom.add(status, BorderLayout.EAST);
        return bottom;
    }

    private JButton createButton(String text, Runnable action) {
        JButton button = new JButton(text);
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
            showError("Please select a product in the table first.");
            return;
        }
        ProductDialog dialog = new ProductDialog(this, service, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshTable();
            showInfo("Product updated.");
        }
    }

    private void onDelete() {
        Product selected = getSelectedProduct();
        if (selected == null) {
            showError("Please select a product in the table first.");
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
            showError("Please select a product in the table first.");
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

    private void onLowStockOnly() {
        Product[] lowStock = service.getLowStockProducts();
        if (lowStock.length == 0) {
            showInfo("No low-stock products.");
            return;
        }
        showProducts(lowStock);
    }

    private void onShowAll() {
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

    /** Reloads the full product list from the service and repaints the table. */
    private void refreshTable() {
        displayedProducts = service.getAllProducts();
        fillTable();
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
                Integer.valueOf(product.getQuantity()),
                String.format("%.2f", product.getPrice()),
                Integer.valueOf(product.getReorderLevel())
            });
        }
        table.clearSelection();
        updateStatus();
    }

    private void updateStatus() {
        itemsLabel.setText("Products: " + service.getProductCount());
        valueLabel.setText("Total value: " + String.format("%.2f", service.getInventoryValue()));
        lowStockLabel.setText("Low stock: " + service.getLowStockCount());
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
            if (value instanceof Number) {
                setHorizontalAlignment(SwingConstants.RIGHT);
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
