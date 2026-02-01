package edu.cnu.mdi.transfer;

import javax.swing.*;

import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class FileDropHandler extends TransferHandler {
	
	private final IFileDropHandler dropHandler;
	public FileDropHandler(IFileDropHandler dropHandler) {
		super();
		this.dropHandler = dropHandler;
	}

	@Override
	public boolean canImport(TransferSupport support) {
		// Validate that we are dropping files
		return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
	}

	@Override
	public boolean importData(TransferSupport support) {
		if (!canImport(support))
			return false;

		try {
			Transferable t = support.getTransferable();
			@SuppressWarnings("unchecked")
			List<File> allFiles = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

			Predicate<File> fileFilter = dropHandler != null ? dropHandler.getFileFilter() : null;
			// Apply the filter if it exists
            List<File> acceptedFiles = (fileFilter == null) ? allFiles : 
                allFiles.stream().filter(fileFilter).collect(Collectors.toList());

            if (!acceptedFiles.isEmpty()) {
            	dropHandler.filesDropped(acceptedFiles);
                return true;
            }
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}