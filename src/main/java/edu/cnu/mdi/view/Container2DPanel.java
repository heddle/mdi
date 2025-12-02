package edu.cnu.mdi.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;

import edu.cnu.mdi.graphics.drawable.Drawable;

/**
 * Default Swing implementation of {@link Container2D}.
 * <p>
 * This component maintains a world-to-screen transform based on its
 * current size and configured world bounds, and renders a set of
 * {@link Drawable} instances.
 */
@SuppressWarnings("serial")
public class Container2DPanel extends JComponent implements Container2D {

    private Rectangle2D worldBounds =
            new Rectangle2D.Double(0.0, 0.0, 100.0, 100.0);

    private final List<Drawable> drawables = new ArrayList<>();

    private AffineTransform worldToScreen = new AffineTransform();
    private AffineTransform screenToWorld = new AffineTransform();

    private boolean dirty = true;
    private int lastWidth = -1;
    private int lastHeight = -1;

    public Container2DPanel() {
        setOpaque(true);
    }

    @Override
    public Rectangle2D getWorldBounds() {
        return (Rectangle2D) worldBounds.clone();
    }

    @Override
    public void setWorldBounds(Rectangle2D world) {
        if (world == null || world.isEmpty()) {
            throw new IllegalArgumentException("World bounds must be non-empty.");
        }
        this.worldBounds = (Rectangle2D) world.clone();
        setDirty(true);
        repaint();
    }

    @Override
    public AffineTransform getWorldToScreenTransform() {
        ensureTransforms();
        return new AffineTransform(worldToScreen);
    }

    @Override
    public AffineTransform getScreenToWorldTransform() {
        ensureTransforms();
        return new AffineTransform(screenToWorld);
    }

    @Override
    public Point worldToScreen(Point2D world) {
        ensureTransforms();
        Point2D dest = worldToScreen.transform(world, null);
        return new Point((int) Math.round(dest.getX()),
                         (int) Math.round(dest.getY()));
    }

    @Override
    public Point2D screenToWorld(Point screen) {
        ensureTransforms();
        return screenToWorld.transform(screen, null);
    }

    @Override
    public void addDrawable(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        synchronized (drawables) {
            drawables.add(drawable);
        }
        repaint();
    }

    @Override
    public void removeDrawable(Drawable drawable) {
        if (drawable == null) {
            return;
        }
        synchronized (drawables) {
            if (drawables.remove(drawable)) {
                drawable.prepareForRemoval();
            }
        }
        repaint();
    }

    @Override
    public List<Drawable> getDrawables() {
        synchronized (drawables) {
            return Collections.unmodifiableList(new ArrayList<>(drawables));
        }
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            // Propagate to drawables so they can clear any cached pixel geometry.
            synchronized (drawables) {
                for (Drawable d : drawables) {
                    d.setDirty(true);
                }
            }
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    private void ensureTransforms() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        if (!dirty && w == lastWidth && h == lastHeight) {
            return;
        }

        lastWidth = w;
        lastHeight = h;
        dirty = false;

        // Non-uniform scaling: world width maps to component width,
        // world height maps to component height.
        double sx = w / worldBounds.getWidth();
        double sy = h / worldBounds.getHeight();

        // Build transform: world -> screen.
        // World y increases up; screen y increases down.
        AffineTransform at = new AffineTransform();

        // Translate world origin to (0, worldMinY).
        at.translate(-worldBounds.getX(), -worldBounds.getY());
        // Scale to component size, flipping y.
        at.scale(sx, -sy);
        // Shift so world min y maps to bottom of component.
        at.translate(0, -worldBounds.getHeight());

        // Now adjust so that worldBounds maps exactly into [0,w]x[0,h].
        // (The above already does that given sx, sy.)
        worldToScreen = at;

        try {
            screenToWorld = worldToScreen.createInverse();
        } catch (Exception e) {
            // Fallback to identity in pathological cases.
            screenToWorld = new AffineTransform();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        ensureTransforms();

        Graphics2D g2 = (Graphics2D) g;
        // Basic anti-aliasing; callers can adjust if needed.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        List<Drawable> snapshot;
        synchronized (drawables) {
            snapshot = new ArrayList<>(drawables);
        }

        for (Drawable d : snapshot) {
            if (d.isVisible() && d.isEnabled()) {
                d.draw(g2, this);
            }
        }
    }
}
