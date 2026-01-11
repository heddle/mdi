package edu.cnu.mdi.experimental;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JFrame;

@SuppressWarnings("serial")
public class TestCanvas extends JFrame {
	
	private final JComponent canvas;
	
	public TestCanvas() {
		setTitle("Test Canvas");
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
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
