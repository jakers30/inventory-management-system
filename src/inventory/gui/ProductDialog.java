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

/**
 * The Add / Update form.
 *
 * <p>The dialog only collects text. The values are sent to
 * {@link InventoryService}, which validates them; if a rule is broken the message
 * is shown in an error box and the dialog stays open so the user can fix the
 * field. In Update mode the ID and the quantity are disabled: the ID is the key
 * of the product and the quantity is only changed by Stock In / Stock Out.</p>
 */
public class ProductDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final InventoryService service;
    private final Product existing;

    private final JTextField idField = new JTextField(16);
    private final JTextField nameField = new JTextField(16);
    private final JTextField categoryField = new JTextField(16);
    private final JTextField quantityField = new JTextField(16);
    private final JTextField priceField = new JTextField(16);
    private final JTextField reorderField = new JTextField(16);

    private boolean saved;

    /**
     * @param existing the product to edit, or {@code null} to add a new one
     */
    public ProductDialog(JFrame parent, InventoryService service, Product existing) {
        super(parent, existing == null ? "Add Product" : "Update Product", true);
        this.service = service;
        this.existing = existing;
        buildUi();
    }

    /** True when the service accepted the input and the dialog was closed by Save. */
    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridLayout(0, 2, 10, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        form.add(new JLabel("Product ID:"));
        form.add(idField);
        form.add(new JLabel("Name:"));
        form.add(nameField);
        form.add(new JLabel("Category:"));
        form.add(categoryField);
        form.add(new JLabel("Quantity:"));
        form.add(quantityField);
        form.add(new JLabel("Price:"));
        form.add(priceField);
        form.add(new JLabel("Reorder level:"));
        form.add(reorderField);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(event -> onSave());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());

        JPanel buttons = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        buttons.add(saveButton);
        buttons.add(cancelButton);

        if (existing != null) {
            fillFields(existing);
        }

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);
        pack();
        setResizable(false);
        setLocationRelativeTo(getParent());
    }

    private void fillFields(Product product) {
        idField.setText(product.getId());
        idField.setEnabled(false);
        idField.setToolTipText("The product ID cannot be changed.");
        nameField.setText(product.getName());
        categoryField.setText(product.getCategory());
        quantityField.setText(String.valueOf(product.getQuantity()));
        quantityField.setEnabled(false);
        quantityField.setToolTipText("Quantity only changes through Stock In / Stock Out.");
        priceField.setText(String.format("%.2f", product.getPrice()));
        reorderField.setText(String.valueOf(product.getReorderLevel()));
    }

    private void onSave() {
        try {
            if (existing == null) {
                service.addProduct(idField.getText(), nameField.getText(), categoryField.getText(),
                        quantityField.getText(), priceField.getText(), reorderField.getText());
            } else {
                service.updateProduct(idField.getText(), nameField.getText(), categoryField.getText(),
                        priceField.getText(), reorderField.getText());
            }
            saved = true;
            dispose();
        } catch (InventoryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
