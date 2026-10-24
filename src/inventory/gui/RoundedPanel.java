package inventory.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;

/**
 * A panel painted as a rounded rectangle, used for the stat cards.
 *
 * <p>Styling only: it holds the same labels any {@link JPanel} would hold and
 * does not know anything about the inventory.</p>
 */
class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Color fill;
    private final int radius;

    RoundedPanel(Color fill, int radius) {
        this.fill = fill;
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
        g2.dispose();
        super.paintComponent(g);
    }
}
