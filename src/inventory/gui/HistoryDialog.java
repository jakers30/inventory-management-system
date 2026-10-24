package inventory.gui;

import inventory.model.Transaction;
import inventory.service.InventoryException;
import inventory.service.InventoryService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Read-only view of the transaction stack, newest row on top.
 *
 * <p>The "Note" column shows the undo design: a row that was reversed says
 * "undone", and the {@code UNDO} row itself says which action it reversed. The
 * original row is never removed, so the log is a complete audit trail.</p>
 */
public class HistoryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Time", "Type", "Product ID", "Product", "Qty change", "Note"};

    private final InventoryService service;
    private final Runnable onDataChanged;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel countLabel = new JLabel();

    /**
     * @param onDataChanged called after an undo, so the main window can repaint
     */
    public HistoryDialog(JFrame parent, InventoryService service, Runnable onDataChanged) {
        super(parent, "Transaction History", true);
        this.service = service;
        this.onDataChanged = onDataChanged;
        this.tableModel = new DefaultTableModel(COLUMNS, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.table = new JTable(tableModel);
        buildUi();
        fillTable();
    }

    private void buildUi() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton undoButton = new JButton("Undo Last Action");
        undoButton.addActionListener(event -> onUndo());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> dispose());
        buttons.add(undoButton);
        buttons.add(closeButton);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        info.add(countLabel);

        bottom.add(buttons, BorderLayout.WEST);
        bottom.add(info, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(800, 420);
        setLocationRelativeTo(getParent());
    }

    private void fillTable() {
        Transaction[] history = service.getHistory();
        tableModel.setRowCount(0);
        for (int i = 0; i < history.length; i++) {
            Transaction transaction = history[i];
            tableModel.addRow(new Object[]{
                transaction.getTimestamp(),
                transaction.getType(),
                transaction.getProductId(),
                transaction.getProductName(),
                Integer.valueOf(transaction.getQuantityChange()),
                buildNote(transaction)
            });
        }
        countLabel.setText("Rows: " + history.length);
    }

    private String buildNote(Transaction transaction) {
        if (transaction.isUndoRecord()) {
            return "reverses " + transaction.getUndoTargetType();
        }
        if (transaction.isUndone()) {
            return "undone";
        }
        return "";
    }

    private void onUndo() {
        try {
            Transaction undo = service.undoLast();
            fillTable();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            JOptionPane.showMessageDialog(this, "Undone: " + undo.getUndoTargetType()
                    + " of product " + undo.getProductId() + ".", "Undo",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (InventoryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Undo Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
