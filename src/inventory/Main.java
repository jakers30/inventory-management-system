package inventory;

import inventory.gui.MainFrame;
import inventory.service.InventoryService;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point of the Inventory Management System.
 *
 * <p>Swing is not thread safe, so the window is created on the Event Dispatch
 * Thread with {@link SwingUtilities#invokeLater(Runnable)}.</p>
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                    // The default look and feel is fine, but the reason is reported.
                    System.err.println("Could not apply the system look and feel: " + ex.getMessage());
                }
                InventoryService service = new InventoryService();
                MainFrame frame = new MainFrame(service);
                frame.setVisible(true);
            }
        });
    }
}
