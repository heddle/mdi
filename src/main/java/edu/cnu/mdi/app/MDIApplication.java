package edu.cnu.mdi.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class MDIApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = new JFrame("MDI Application with Reliable Dragging");
            mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            mainFrame.setSize(800, 600);

            // Create a JDesktopPane with a custom DesktopManager
            CustomDesktopPane desktopPane = new CustomDesktopPane();
            desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE); // faster
            RobustDesktopManager manager = new RobustDesktopManager(desktopPane);
            desktopPane.setDesktopManager(manager);


            // Add several JInternalFrames to the JDesktopPane
            for (int i = 1; i <= 3; i++) {
                JInternalFrame internalFrame = createInternalFrame("Internal Frame " + i, i * 50, i * 50);
                desktopPane.add(internalFrame);
                internalFrame.setVisible(true);

                // Attach drag listener
                manager.attachDragListener(internalFrame);
            }

            // Set up the GlassPane for capturing mouse events
            DragGlassPane glassPane = new DragGlassPane();
            mainFrame.setGlassPane(glassPane);
            glassPane.setVisible(false); // Initially invisible

            manager.setGlassPane(glassPane);

            // Add the JDesktopPane to the JFrame
            mainFrame.add(desktopPane);
            mainFrame.setVisible(true);
        });
    }

    private static JInternalFrame createInternalFrame(String title, int x, int y) {
        JInternalFrame internalFrame = new JInternalFrame(title, true, true, true, true);
        internalFrame.setBounds(x, y, 300, 200);
        internalFrame.setLayout(new BorderLayout());

        JLabel label = new JLabel("Content of " + title, SwingConstants.CENTER);
        internalFrame.add(label, BorderLayout.CENTER);

        return internalFrame;
    }

    static class CustomDesktopPane extends JDesktopPane {
        private Rectangle dragOutline = null;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw the drag outline if it exists
            if (dragOutline != null) {
                g.setColor(Color.RED); // Outline color
                ((Graphics2D) g).draw(dragOutline);
            }
        }

        public void setDragOutline(Rectangle outline) {
            this.dragOutline = outline;
            repaint();
        }
    }

    static class DragGlassPane extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Optional: custom paint for debugging
        }
    }

    static class RobustDesktopManager extends DefaultDesktopManager {
        private final CustomDesktopPane desktopPane;
        private DragGlassPane glassPane;
        private Rectangle dragOutline = null;
        private Point dragStart = null;

        public RobustDesktopManager(CustomDesktopPane desktopPane) {
            this.desktopPane = desktopPane;
        }

        public void setGlassPane(DragGlassPane glassPane) {
            this.glassPane = glassPane;
        }

        public void attachDragListener(JInternalFrame frame) {
            frame.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragStart = SwingUtilities.convertPoint(frame, e.getPoint(), desktopPane);
                    dragOutline = frame.getBounds();
                    glassPane.setVisible(true); // Activate glass pane for tracking
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                	System.err.println("mouseReleased");
                    if (dragOutline != null) {
                        finalizeDragging(frame);
                    }
                }
            });

            frame.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragStart != null) {
                        Point dragCurrent = SwingUtilities.convertPoint(frame, e.getPoint(), desktopPane);
                        int dx = dragCurrent.x - dragStart.x;
                        int dy = dragCurrent.y - dragStart.y;

                        // Update the drag outline
                        dragOutline.x += dx;
                        dragOutline.y += dy;

                        // Update the drag start position
                        dragStart = dragCurrent;

                        // Pass the drag outline to the desktop pane for rendering
                        desktopPane.setDragOutline(dragOutline);
                    }
                }
            });
        }

        @Override
        public void endDraggingFrame(JComponent f) {
            finalizeDragging(f);
        }

        public void finalizeDragging(JComponent f) {
            if (dragOutline != null && f instanceof JInternalFrame) {
                f.setBounds(dragOutline);
                dragOutline = null;
                desktopPane.setDragOutline(null); // Clear outline
                desktopPane.repaint();
                glassPane.setVisible(false); // Deactivate glass pane
            }
        }
    }
}



