package edu.cnu.mdi.transfer;

import java.io.File;
import java.util.List;
import java.util.function.Predicate;

public interface IFileDropHandler {

	/**
     * Set a file filter, e.g., f -> f.getName().endsWith(".png").
     * Used for drag and drop.
     * @param filter the file filter can be null
     */
 	public abstract void setFileFilter(Predicate<File> filter);

 	/**
	 * Get the current file filter.
	 * @return the file filter can be null
	 */
 	public abstract Predicate<File> getFileFilter();

 	/**
	 * Handle files dropped onto the component.
	 * @param files the dropped files.
	 */
 	public abstract void filesDropped(List<File> files);
}
