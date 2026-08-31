package edu.cnu.mdi.component;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JPanel;

/**
 * Centers one component in the available area while preserving a requested
 * width-to-height ratio. Extra space remains around the hosted component.
 */
@SuppressWarnings("serial")
public final class AspectRatioPanel extends JPanel {

	private final Component content;
	private final double aspectRatio;

	public AspectRatioPanel(Component content, double aspectRatio) {
		if (content == null) throw new IllegalArgumentException("content must not be null");
		if (!Double.isFinite(aspectRatio) || aspectRatio <= 0) {
			throw new IllegalArgumentException("aspectRatio must be positive and finite");
		}
		this.content = content;
		this.aspectRatio = aspectRatio;
		setLayout(null);
		setOpaque(false);
		add(content);
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension preferred = content.getPreferredSize();
		if (preferred == null) return new Dimension(400, (int) Math.round(400 / aspectRatio));
		int height = Math.max(1, preferred.height);
		int width = Math.max(1, (int) Math.round(height * aspectRatio));
		return new Dimension(width, height);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(100, Math.max(1, (int) Math.round(100 / aspectRatio)));
	}

	@Override
	public void doLayout() {
		int availableWidth = getWidth();
		int availableHeight = getHeight();
		if (availableWidth <= 0 || availableHeight <= 0) return;

		int width = availableWidth;
		int height = (int) Math.round(width / aspectRatio);
		if (height > availableHeight) {
			height = availableHeight;
			width = (int) Math.round(height * aspectRatio);
		}
		content.setBounds((availableWidth - width) / 2, (availableHeight - height) / 2,
				Math.max(1, width), Math.max(1, height));
	}
}
