package inventory.gui;

import inventory.model.Product;
import inventory.service.InventoryException;
import inventory.service.InventoryService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The Stock In / Stock Out form.
 *
 * <p>Both actions use the same dialog with a flag, because the only difference is
 * the title and whether the amount is added or subtracted. The service checks
 * that the amount is a positive whole number and that a Stock Out never takes
 * more than the quantity on hand.</p>
 */
public class StockDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final InventoryService service;
    private final Product product;
    private final boolean stockIn;

    private final JTextField amountField = new JTextField(10);

    private boolean saved;

    public StockDialog(JFrame parent, InventoryService service, Product product, boolean stockIn) {
        super(parent, stockIn ? "Stock In" : "Stock Out", true);
        this.service = service;
        this.product = product;
        this.stockIn = stockIn;
        buildUi();
    }

    /** True when the stock change was accepted. */
    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        form.add(new JLabel("Product ID:"));
        form.add(new JLabel(product.getId()));
        form.add(new JLabel("Name:"));
        form.add(new JLabel(product.getName()));
        form.add(new JLabel("Current quantity:"));
        form.add(new JLabel(String.valueOf(product.getQuantity())));
        form.add(new JLabel(stockIn ? "Quantity to add:" : "Quantity to remove:"));
        form.add(amountField);

        JButton confirmButton = new JButton(stockIn ? "Stock In" : "Stock Out");
        confirmButton.addActionListener(event -> onConfirm());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());

        JPanel buttons = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        buttons.add(confirmButton);
        buttons.add(cancelButton);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(confirmButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                amountField.requestFocusInWindow();
                amountField.selectAll();
            }
        });
    }

    private void onConfirm() {
        try {
            if (stockIn) {
                service.stockIn(product.getId(), amountField.getText());
            } else {
                service.stockOut(product.getId(), amountField.getText());
            }
            saved = true;
            dispose();
        } catch (InventoryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Stock Update Failed",
                    JOptionPane.ERROR_MESSAGE);
            amountField.selectAll();
            amountField.requestFocusInWindow();
        }
    }
}
