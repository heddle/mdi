package edu.cnu.mdi.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import edu.cnu.mdi.graphics.GraphicsUtils;

public class PrintUtils implements Printable {

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
        new PrintUtils(c, true, true, false).print();
    }

    /**
     * Convenience constructor with default settings:
     * fitToPage=true, centerOnPage=true, allowUpscale=false
     * @param componentToBePrinted
     */
    public PrintUtils(Component componentToBePrinted) {
        this(componentToBePrinted, true, true, false);
    }

    /**
     * Full constructor with behavior knobs:
     * @param fitToPage    if true, scale to fit imageable area
     * @param centerOnPage if true, center within imageable area (after scaling)
     * @param allowUpscale if false, scale will never exceed 1.0 (no enlarging)
     */
    public PrintUtils(Component componentToBePrinted, boolean fitToPage, boolean centerOnPage, boolean allowUpscale) {
        this.component = componentToBePrinted;
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
                System.out.println("Error printing: " + pe);
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
            // Move origin to imageable area
            g2.translate(pf.getImageableX(), pf.getImageableY());

            double scale = 1.0;

            int cw = Math.max(1, component.getWidth());
            int ch = Math.max(1, component.getHeight());

            if (fitToPage) {
                double sx = pf.getImageableWidth() / cw;
                double sy = pf.getImageableHeight() / ch;
                scale = Math.min(sx, sy);
                if (!allowUpscale) {
                    scale = Math.min(1.0, scale);
                }
            }

            // Optional centering within imageable area
            if (centerOnPage) {
                double printedW = cw * scale;
                double printedH = ch * scale;
                double dx = (pf.getImageableWidth() - printedW) / 2.0;
                double dy = (pf.getImageableHeight() - printedH) / 2.0;
                if (dx > 0) g2.translate(dx, 0);
                if (dy > 0) g2.translate(0, dy);
            }

            g2.scale(scale, scale);

            // Disable double buffering for cleaner printing
            RepaintManager mgr = RepaintManager.currentManager(component);
            boolean wasDB = mgr.isDoubleBufferingEnabled();
            mgr.setDoubleBufferingEnabled(false);

           try {
                // For Swing components, printAll is usually fine.
                // component.print(g2) is also valid; printAll is slightly more "include everything".
                component.printAll(g2);
            } finally {
                mgr.setDoubleBufferingEnabled(wasDB);
            }

            return PAGE_EXISTS;
        } finally {
            g2.dispose();
        }
    }
    
    public static void printComponentAsImage(Component c) {
        final BufferedImage[] snap = new BufferedImage[1];

        try {
            Runnable r = () -> snap[0] = GraphicsUtils.getComponentImage(c);

            if (SwingUtilities.isEventDispatchThread()) {
                r.run();
            } else {
                SwingUtilities.invokeAndWait(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                pe.printStackTrace();
            }
        }   
    }
}