package edu.cnu.mdi.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TransferPackageTest {

    @TempDir
    Path tempDir;

    @Test
    void fileDropRejectsNullHandlerAndEmptyFilteredDrop() throws Exception {
        assertThrows(NullPointerException.class, () -> new FileDropHandler(null));

        File file = Files.createFile(tempDir.resolve("file.txt")).toFile();
        RecordingDropHandler receiver = new RecordingDropHandler(f -> false);
        FileDropHandler handler = new FileDropHandler(receiver);

        assertFalse(handler.importData(support(List.of(file))));
        assertTrue(receiver.files.isEmpty());
    }

    @Test
    void fileDropDeliversAcceptedFiles() throws Exception {
        File file = Files.createFile(tempDir.resolve("file.txt")).toFile();
        RecordingDropHandler receiver = new RecordingDropHandler(f -> true);

        assertTrue(new FileDropHandler(receiver).importData(support(List.of(file))));
        assertEquals(List.of(file), receiver.files);
    }

    @Test
    void imagePredicatesRecognizeContentAndCaseInsensitiveExtension() throws Exception {
        File image = tempDir.resolve("IMAGE.PNG").toFile();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", image);
        File fake = Files.writeString(tempDir.resolve("fake.png"), "not an image").toFile();

        assertTrue(ImageFilters.isReadableImage.test(image));
        assertTrue(ImageFilters.isActualImage.test(image));
        assertTrue(ImageFilters.isReadableImage.test(fake));
        assertFalse(ImageFilters.isActualImage.test(fake));
    }

    @Test
    void scaleToDragImageProducesASquareImageOfTheRequestedSize() {
        BufferedImage source = new BufferedImage(40, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage scaled = PaletteDragSupport.scaleToDragImage(source, 24);

        assertEquals(24, scaled.getWidth());
        assertEquals(24, scaled.getHeight());
    }

    @Test
    void scaleToDragImageRejectsInvalidArguments() {
        BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        assertThrows(IllegalArgumentException.class,
                () -> PaletteDragSupport.scaleToDragImage(null, 24));
        assertThrows(IllegalArgumentException.class,
                () -> PaletteDragSupport.scaleToDragImage(source, 0));
        assertThrows(IllegalArgumentException.class,
                () -> PaletteDragSupport.scaleToDragImage(source, -1));
    }

    private static TransferHandler.TransferSupport support(List<File> files) {
        Transferable transferable = new Transferable() {
            @Override public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] { DataFlavor.javaFileListFlavor };
            }
            @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.javaFileListFlavor.equals(flavor);
            }
            @Override public Object getTransferData(DataFlavor flavor) {
                return files;
            }
        };
        return new TransferHandler.TransferSupport(new JPanel(), transferable);
    }

    private static final class RecordingDropHandler implements IFileDropHandler {
		private Predicate<File> filter;
        private List<File> files = List.of();

        RecordingDropHandler(Predicate<File> filter) {
            this.filter = filter;
        }

        @Override public void filesDropped(List<File> files) {
            this.files = List.copyOf(files);
        }

		@Override public Predicate<File> getFileFilter() {
			return filter;
		}

		@Override public void setFileFilter(Predicate<File> filter) {
			this.filter = filter;
		}
    }
}
