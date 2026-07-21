package edu.cnu.mdi.component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Shared animation manager for drawn buttons.
 * <p>
 * The manager owns one Swing {@link Timer}. Animated buttons register when they become
 * displayable and unregister when removed from the Swing hierarchy. The timer stops when
 * no animated buttons remain.
 * </p>
 */
public final class DrawnComponentManager {

    /** Milliseconds between animation frames, roughly 30 frames per second. */
    public static final int DEFAULT_DELAY = 33;

    private static DrawnComponentManager instance;

    private final Timer timer;
    private final Set<AbstractButton> registeredButtons = new LinkedHashSet<>();

    private long frameCount;

    private DrawnComponentManager() {
        timer = new Timer(DEFAULT_DELAY, event -> advanceFrame());
    }

    /**
     * Returns the shared animation manager.
     *
     * @return the singleton manager
     */
    public static synchronized DrawnComponentManager getInstance() {
        if (instance == null) {
            instance = new DrawnComponentManager();
        }
        return instance;
    }

    /**
     * Registers a button for animation repainting.
     *
     * @param button the button to repaint on animation frames
     */
    public void register(AbstractButton button) {
        if (button == null) {
            return;
        }
        runOnEdt(() -> {
            registeredButtons.add(button);
            if (!timer.isRunning()) {
                timer.start();
            }
        });
    }

    /**
     * Unregisters a button from animation repainting.
     *
     * @param button the button to unregister
     */
    public void unregister(AbstractButton button) {
        if (button == null) {
            return;
        }
        runOnEdt(() -> {
            registeredButtons.remove(button);
            if (registeredButtons.isEmpty()) {
                timer.stop();
            }
        });
    }

    /**
     * Returns the number of elapsed animation frames.
     *
     * @return the current frame count
     */
    public long getFrameCount() {
        return frameCount;
    }

    private void advanceFrame() {
        frameCount++;
        for (AbstractButton button : new ArrayList<>(registeredButtons)) {
            if (button.isDisplayable() && button.isShowing()) {
                button.repaint();
            }
        }
    }

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}