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
	 * @param icon         the cached icon, or {@code null} if not yet loaded
	 */
	public MilSymbolDescriptor(String id, String displayName, String category, String resourcePath, ImageIcon icon) {
		this.id = id;
		this.displayName = displayName;
		this.category = category;
		this.resourcePath = resourcePath;
		this.icon = icon;
	}

	/** @return unique internal symbol identifier */
	public String getId() {
		return id;
	}

	/** @return user-facing symbol name */
	public String getDisplayName() {
		return displayName;
	}

	/** @return palette category containing the symbol */
	public String getCategory() {
		return category;
	}

	/** @return classpath path from which the symbol icon was loaded */
	public String getResourcePath() {
		return resourcePath;
	}

	/** @return cached symbol icon, which may be {@code null} */
	public ImageIcon getIcon() {
		return icon;
	}

	/** @return the user-facing symbol name */
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
	 * @param icon         the icon to associate with the descriptor, or
	 *                     {@code null} if not yet loaded
	 * @return a symbol descriptor for the given resource path
	 */
	public static MilSymbolDescriptor fromResourcePath(String resourcePath, ImageIcon icon) {
	    if (resourcePath == null || resourcePath.isEmpty()) {
	        return new MilSymbolDescriptor("unknown", "Unknown", "Unknown", "unknown", icon);
	    }

	    if (resourcePath.contains("/nato_icons/")) {
	        String[] parts = resourcePath.split("/nato_icons/")[1].split("/");
	        if (parts.length == 2) {
	            String category = parts[0];
	            String affiliation = parts[1].replace(".png", "");

	            String prettyCategory = category.replace("_", " ");
	            String prettyAffiliation =
	                    affiliation.substring(0, 1).toUpperCase() + affiliation.substring(1).toLowerCase();

	            String id = category + "_" + affiliation;
	            String displayName = prettyCategory + " (" + prettyAffiliation + ")";

	            return new MilSymbolDescriptor(id, displayName, prettyCategory, resourcePath, icon);
	        }
	    }

	    return new MilSymbolDescriptor(resourcePath, resourcePath, "Unknown", resourcePath, icon);
	}
}
