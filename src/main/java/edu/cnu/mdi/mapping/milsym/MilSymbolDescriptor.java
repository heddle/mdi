package edu.cnu.mdi.mapping.milsym;

import javax.swing.ImageIcon;

/**
 * Immutable descriptor for a palette military symbol.
 *
 * <p>
 * This class intentionally stays simple for the proof-of-concept phase. It
 * identifies a symbol by an internal id, a user-facing display name, a category
 * used by the palette UI, and a classpath image resource.
 * </p>
 */
public class MilSymbolDescriptor {

	private final String id;
	private final String displayName;
	private final String category;
	private final String resourcePath;
	
	//optional cached icon for efficiency; in a real implementation, you might want to
	// load the icon lazily and cache it here for reuse, but for this demo we just load 
	// it on demand in the palette
	private final ImageIcon icon; // cache the loaded icon for efficiency

	/**
	 * Creates a new symbol descriptor.
	 *
	 * @param id           unique internal id
	 * @param displayName  label shown in the palette and tooltips
	 * @param category     category label such as "Ground", "Air", or "Support"
	 * @param resourcePath classpath resource path to the icon image
	 */
	public MilSymbolDescriptor(String id, String displayName, String category, String resourcePath, ImageIcon icon) {
		this.id = id;
		this.displayName = displayName;
		this.category = category;
		this.resourcePath = resourcePath;
		this.icon = icon;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getCategory() {
		return category;
	}

	public String getResourcePath() {
		return resourcePath;
	}
	
	public ImageIcon getIcon() {
		return icon;
	}

	@Override
	public String toString() {
		return displayName;
	}

	/**
	 * Factory method to create a symbol descriptor from a resource path.
	 * <p>
	 * In a real implementation, this would look up the symbol by resource path and
	 * return a fully populated descriptor. For this demo, we just create a dummy
	 * descriptor with the resource path as the id and display name.
	 * </p>
	 *
	 * @param resourcePath the classpath resource path to the icon image
	 * @return a symbol descriptor for the given resource path
	 */
	public static MilSymbolDescriptor fromResourcePath(String resourcePath, ImageIcon icon) {
		// In a real implementation, this would look up the symbol by resource path.
		// For this demo, we just create a dummy descriptor with the resource path as
		// the id and display name.
		// In the demo the resource path looks like:
		// "/edu/cnu/mdi/images/nato_icons/Air_Defence/friendly.png"
		// try to build a reasonable descriptor from that path, e.g.
		// id = "Air_Defence_friendly", displayName = "Air Defence (Friendly)",
		// category = "Air Defence"

		if (resourcePath == null || resourcePath.isEmpty()) {
			return new MilSymbolDescriptor("unknown", "Unknown", "Unknown", "unknown", icon);
		}

		if (resourcePath.contains("/nato_icons/")) {
			String[] parts = resourcePath.split("/nato_icons/")[1].split("/");
			if (parts.length == 2) {
				String category = parts[0];
				String name = parts[1].replace(".png", "");
				String displayName = name.replace("_", " ") + " (" + category + ")";
				return new MilSymbolDescriptor(category + "_" + name, displayName, category, resourcePath, icon);
			}
		}

		//fallback for unexpected resource paths
		return new MilSymbolDescriptor(resourcePath, resourcePath, "Unknown", resourcePath, icon);
	}
}