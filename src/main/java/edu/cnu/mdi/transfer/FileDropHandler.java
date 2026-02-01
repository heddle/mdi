package edu.cnu.mdi.transfer;

import javax.swing.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.util.List;

public class FileDropHandler extends TransferHandler {

    @Override
    public boolean canImport(TransferSupport support) {
        // Only accept "Copy" or "Move" actions and ensure it's a file list
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        // Get the transferable data
        Transferable t = support.getTransferable();

        try {
            // Cast the data to a List of Files
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

            // Process the files (print to console or open them)
            for (File file : files) {
                System.out.println("Dropped file: " + file.getAbsolutePath());
                // YOUR LOGIC HERE: e.g., openFile(file);
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}