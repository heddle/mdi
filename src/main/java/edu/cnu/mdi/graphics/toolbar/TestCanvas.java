package edu.cnu.mdi.graphics.toolbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.FlatIntelliJLaf;

import edu.cnu.mdi.ui.fonts.Fonts;

@SuppressWarnings("serial")
public class TestCanvas extends JFrame {

	private final JComponent canvas;

	public TestCanvas() {
		setTitle("Test Canvas");
		setSize(900, 600);
		UIInit();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		canvas = new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Rectangle r = getBounds();

				int del = 2;
				for (int x = 0; x < 1+r.width/del  ; x++) {
					for (int y = 0; y < 1+r.height/del ; y++) {
						Color c = new Color	(x % 256,
											 y % 256,
											 (x*y) % 256);
						g.setColor(c);
						g.fillRect(x*del, y*del, del, del);
					}
				}
			}
		};
		canvas.setBackground(Color.pink);
		canvas.setOpaque(true);
		add(canvas, BorderLayout.CENTER);

		// add a toolbar
		BaseToolBar toolBar = new BaseToolBar(canvas, new DefaultToolHandler(), ToolBits.EVERYTHING);
		add(toolBar, BorderLayout.NORTH);

		canvas.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				toolBar.updateStatusText(String.format("Test Canvas - Mouse at (%d, %d)", e.getX(), e.getY()));
			}
		});
	}

    //initialize the FlatLaf UI
	private void UIInit() {
		FlatIntelliJLaf.setup();
		UIManager.put("Component.focusWidth", 1);
		UIManager.put("Component.arc", 6);
		UIManager.put("Button.arc", 6);
		UIManager.put("TabbedPane.showTabSeparators", true);
		Fonts.refresh();
	}

	/**
	 * Launch the test frame.
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			TestCanvas f = new TestCanvas();
			f.setVisible(true);
		});
	}

}
