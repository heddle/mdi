package edu.cnu.mdi.view;

import java.util.List;

public class DrawingViewInfo extends AbstractViewInfo {

	@Override
	public String getTitle() {
		return "Drawing View";
	}

	@Override
	public String getPurpose() {
		return "A simple drawing view for testing and demonstration. "
				+ "It is not intended to be a full-featured drawing application, "
				+ "but rather a sandbox for experimenting with MDI capabilities. It demonstrates how tools and features can be added to any view, including:"
				+ "<ul>"
				+ "<li>Rubberbanding (drag-to-stretch) for rectangles, ovals, and lines.</li>"
				+ "<li>Navigation and zooming</li>"
				+ "<li>Interactive editing including dragging and style changes</li>"
				+ "</ul>";
		
	}
	
	@Override
	public List<String> getUsageBullets() {
		return List.of(
				"Use the toolbar buttons to select a drawing tool, then click and drag in the view to draw. ",
				"Use the navigation tools to pan and zoom. ",
				"Click on shapes to select them, then use the style editor to change their color and stroke. ",
				"To insert an image into the drawing, drag a PNG or JPEG file from the file system and drop it into the view."
				);
	}
	
	@Override
	protected boolean isPurposeHtml() {
	    return true;
	}

}
