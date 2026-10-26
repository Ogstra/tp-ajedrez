package adaptadores;

import skins.Skin;
import skins.SkinService;
import usuarios.User;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

// Pantalla de skins, con el mismo estilo que el ranking: todas las skins en un recuadro, cada
// una con una vista previa (cuatro casillas y el rey). La que está en uso lo dice, las
// desbloqueadas tienen Usar y las bloqueadas muestran cuántos puntos piden.
public class SkinsPanel extends JPanel {

    private final SkinService skins;
    private final JPanel rows = new JPanel(new GridBagLayout());
    private final JLabel points = MenuPanel.label(" ");
    private User user;

    public SkinsPanel(SkinService skins, Runnable onBack) {
        this.skins = skins;
        JLabel title = new JLabel("Skins", SwingConstants.CENTER);
        title.setFont(Palette.title(32));
        title.setForeground(Palette.PRIMARY);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        strip.setOpaque(false);
        strip.add(new BoardStrip());
        points.setHorizontalAlignment(SwingConstants.CENTER);

        rows.setOpaque(false);
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        card.add(rows, BorderLayout.CENTER);

        FlatButton back = new FlatButton("Volver", FlatButton.Style.SECONDARY);
        back.addActionListener(e -> onBack.run());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.add(back);

        JPanel content = MenuPanel.column();
        MenuPanel.put(content, title, 0);
        MenuPanel.put(content, strip, 8);
        MenuPanel.put(content, points, 14);
        MenuPanel.put(content, card, 10);
        MenuPanel.put(content, bottom, 20);

        setBackground(Palette.BACKGROUND);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(12, 0, 12, 0);
        add(content, c);
    }

    // Se llena cada vez que se abre, porque cambian los puntos y la skin elegida.
    public void show(User user) {
        this.user = user;
        points.setText("Tus puntos: " + skins.pointsOf(user));
        rows.removeAll();
        Skin current = skins.selected(user);
        boolean first = true;
        for (Skin skin : skins.all()) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            rows.add(row(skin, skin.equals(current), skins.isUnlocked(skin, user), !first), c);
            first = false;
        }
        revalidate();
        repaint();
    }

    // Una fila: vista previa, nombre y, a la derecha, "En uso", Usar o los puntos que pide.
    private JPanel row(Skin skin, boolean inUse, boolean unlocked, boolean divider) {
        JLabel name = new JLabel(skin.getName());
        name.setForeground(unlocked ? Palette.INK : Palette.MUTED);
        name.setFont(name.getFont().deriveFont(14f));

        JComponent right;
        if (inUse) {
            JLabel label = new JLabel("En uso");
            label.setForeground(Palette.PRIMARY);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
            right = label;
        } else if (unlocked) {
            FlatButton use = new FlatButton("Usar", FlatButton.Style.SECONDARY);
            use.setName("use-" + skin.getId());
            use.addActionListener(e -> {
                skins.select(user, skin);
                show(user);
            });
            right = use;
        } else {
            right = MenuPanel.small(skin.getRequiredPoints() + " puntos");
        }

        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setName("skin-" + skin.getId());
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(divider ? 1 : 0, 0, 0, 0, Palette.LINE),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        row.add(new Preview(skin, unlocked), BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // Cuatro casillas con los colores de la skin y su rey blanco encima.
    private static class Preview extends JComponent {
        private static final int SIZE = 40;
        private final Skin skin;
        private final boolean unlocked;
        private final java.awt.Image king; // el rey ya preparado para la pantalla escalada

        Preview(Skin skin, boolean unlocked) {
            this.skin = skin;
            this.unlocked = unlocked;
            java.awt.Image image = SkinImages.piece(skin, "wK");
            this.king = image == null ? null : SmoothImage.of(image, SIZE - 8);
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            int half = SIZE / 2;
            java.awt.Color light = SkinImages.color(skin.getLightSquare());
            java.awt.Color dark = SkinImages.color(skin.getDarkSquare());
            g2.setColor(light);
            g2.fillRect(0, 0, SIZE, SIZE);
            g2.setColor(dark);
            g2.fillRect(half, 0, half, half);
            g2.fillRect(0, half, half, half);
            if (king != null) g2.drawImage(king, 4, 4, null);
            if (!unlocked) { // las bloqueadas se ven apagadas
                g2.setColor(new java.awt.Color(Palette.BACKGROUND.getRed(), Palette.BACKGROUND.getGreen(),
                    Palette.BACKGROUND.getBlue(), 140));
                g2.fillRect(0, 0, SIZE, SIZE);
            }
            g2.dispose();
        }
    }
}
