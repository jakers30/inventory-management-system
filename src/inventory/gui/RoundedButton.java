package inventory.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * A flat, rounded, colored button. The color is the only style it owns; the
 * action listener is added by the caller, so the button behaves like any other
 * {@link JButton}.
 */
class RoundedButton extends JButton {

    private static final long serialVersionUID = 1L;

    private static final Color DISABLED_FILL = new Color(0xC7, 0xCD, 0xD6);

    private final Color base;
    private Color current;

    RoundedButton(String text, Color base) {
        super(text);
        this.base = base;
        this.current = base;
        setForeground(Color.WHITE);
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                current = base.darker();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                current = base;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isEnabled() ? current : DISABLED_FILL);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
        g2.dispose();
        super.paintComponent(g);
    }
}
