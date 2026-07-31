package edu.cnu.mdi.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.graphics.GraphicsUtils;

/** Prints Swing components either directly or from an EDT-created snapshot. */
public final class PrintUtils implements Printable {

	private static final Logger LOGGER = Logger.getLogger(PrintUtils.class.getName());

    private final Component component;

    // Behavior knobs (keep defaults sane)
    private final boolean fitToPage;
    private final boolean centerOnPage;
    private final boolean allowUpscale;

    /**
     * Convenience method to print a component with default settings:
     * @param c the component to print (must have non-zero size)
     */
    public static void printComponent(Component c) {
		Objects.requireNonNull(c, "component");
        new PrintUtils(c, true, true, false).print();
    }

    /**
     * Convenience constructor with default settings:
     * fitToPage=true, centerOnPage=true, allowUpscale=false
     *
     * @param componentToBePrinted component to print
     */
    public PrintUtils(Component componentToBePrinted) {
        this(componentToBePrinted, true, true, false);
    }

    /**
     * Full constructor with behavior knobs:
     *
	 * @param componentToBePrinted component to print
     * @param fitToPage    if true, scale to fit imageable area
     * @param centerOnPage if true, center within imageable area (after scaling)
     * @param allowUpscale if false, scale will never exceed 1.0 (no enlarging)
     */
    public PrintUtils(Component componentToBePrinted, boolean fitToPage, boolean centerOnPage, boolean allowUpscale) {
		this.component = Objects.requireNonNull(componentToBePrinted, "componentToBePrinted");
        this.fitToPage = fitToPage;
        this.centerOnPage = centerOnPage;
        this.allowUpscale = allowUpscale;
    }

    /**
	 * Show print dialog and print if user confirms.
	 */
    public void print() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(component.getName() != null ? component.getName() : component.getClass().getSimpleName());
        job.setPrintable(this);

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException pe) {
				LOGGER.log(Level.WARNING, "Unable to print component.", pe);
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
            return NO_SUCH_PAGE;
        }

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			Runnable render = () -> renderComponent(g2, pf);
			if (SwingUtilities.isEventDispatchThread()) {
				render.run();
			} else {
				SwingUtilities.invokeAndWait(render);
			}
			return PAGE_EXISTS;
		} catch (Exception e) {
			PrinterException failure = new PrinterException("Unable to render component for printing");
			failure.initCause(e);
			throw failure;
		} finally {
			g2.dispose();
		}
	}

	private void renderComponent(Graphics2D g2, PageFormat pf) {
		g2.translate(pf.getImageableX(), pf.getImageableY());
		int cw = Math.max(1, component.getWidth());
		int ch = Math.max(1, component.getHeight());
		double scale = 1.0;
		if (fitToPage) {
			scale = Math.min(pf.getImageableWidth() / cw, pf.getImageableHeight() / ch);
			if (!allowUpscale) {
				scale = Math.min(1.0, scale);
			}
		}
		if (centerOnPage) {
			double dx = (pf.getImageableWidth() - cw * scale) / 2.0;
			double dy = (pf.getImageableHeight() - ch * scale) / 2.0;
			if (dx > 0) {
				g2.translate(dx, 0);
			}
			if (dy > 0) {
				g2.translate(0, dy);
			}
		}
		g2.scale(scale, scale);
		RepaintManager manager = RepaintManager.currentManager(component);
		boolean doubleBuffered = manager.isDoubleBufferingEnabled();
		manager.setDoubleBufferingEnabled(false);
		try {
			component.printAll(g2);
		} finally {
			manager.setDoubleBufferingEnabled(doubleBuffered);
		}
	}
    
	/**
	 * Capture a component on the EDT and print the resulting image.
	 *
	 * @param c component to capture and print
	 */
    public static void printComponentAsImage(Component c) {
		Objects.requireNonNull(c, "component");
        final BufferedImage[] snap = new BufferedImage[1];

        try {
            Runnable r = () -> snap[0] = GraphicsUtils.getComponentImage(c);

            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeAndWait(r);
            }
        } catch (Exception e) {
			LOGGER.log(Level.WARNING, "Unable to capture component for printing.", e);
            return;
        }

        if (snap[0] == null) return;

        printImage(snap[0], c.getName());
    }

    private static void printImage(BufferedImage img, String jobName) {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (jobName != null) job.setJobName(jobName);

        job.setPrintable((graphics, pf, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.translate(pf.getImageableX(), pf.getImageableY());

                double iw = img.getWidth();
                double ih = img.getHeight();
                double sx = pf.getImageableWidth() / iw;
                double sy = pf.getImageableHeight() / ih;
                double scale = Math.min(sx, sy);        // fit-to-page, preserve aspect
                scale = Math.min(1.0, scale);           // no upscale

                // center
                double dx = (pf.getImageableWidth() - iw * scale) / 2.0;
                double dy = (pf.getImageableHeight() - ih * scale) / 2.0;
                if (dx > 0) g2.translate(dx, 0);
                if (dy > 0) g2.translate(0, dy);

                g2.scale(scale, scale);
                g2.drawImage(img, 0, 0, null);
                return Printable.PAGE_EXISTS;
            } finally {
                g2.dispose();
            }
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException pe) {
				LOGGER.log(Level.WARNING, "Unable to print component image.", pe);
            }
        }   
    }
}
