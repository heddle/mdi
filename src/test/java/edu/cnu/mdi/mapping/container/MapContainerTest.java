package edu.cnu.mdi.mapping.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cnu.mdi.transfer.FileDropHandler;
import edu.cnu.mdi.transfer.IFileDropHandler;

/**
 * Tests {@link MapContainer#delegateToInstalledTransferHandler}, the fallback
 * used when the raw AWT milsym {@link java.awt.dnd.DropTarget} doesn't recognize
 * a drop's flavor. Without this delegation, that DropTarget — installed on the
 * same component {@link MapView2D#enableFileDrop} installs its file-drop
 * {@link javax.swing.TransferHandler} on — would shadow file drops (e.g.
 * shapefiles) entirely, rejecting them before Swing's TransferHandler ever saw
 * them.
 */
class MapContainerTest {

	private static MapContainer newContainer() {
		return new MapContainer(new Rectangle2D.Double(-180, -90, 360, 180));
	}

	private static Transferable fileListTransferable(File... files) {
		List<File> fileList = new ArrayList<>(List.of(files));
		return new Transferable() {
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] { DataFlavor.javaFileListFlavor };
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.javaFileListFlavor.equals(flavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
				if (!isDataFlavorSupported(flavor)) {
					throw new UnsupportedFlavorException(flavor);
				}
				return fileList;
			}
		};
	}

	private static class RecordingFileDropHandler implements IFileDropHandler {
		private java.util.function.Predicate<File> filter;
		List<File> received;

		@Override
		public void setFileFilter(java.util.function.Predicate<File> filter) {
			this.filter = filter;
		}

		@Override
		public java.util.function.Predicate<File> getFileFilter() {
			return filter;
		}

		@Override
		public void filesDropped(List<File> files) {
			received = files;
		}
	}

	@Test
	void delegatesAFileListDropToTheInstalledFileDropHandler() {
		MapContainer container = newContainer();
		RecordingFileDropHandler recorder = new RecordingFileDropHandler();
		recorder.setFileFilter(f -> f.getName().endsWith(".shp"));
		container.setTransferHandler(new FileDropHandler(recorder));

		File shpFile = new File("countries.shp");
		boolean imported = container.delegateToInstalledTransferHandler(fileListTransferable(shpFile));

		assertTrue(imported);
		assertEquals(List.of(shpFile), recorder.received);
	}

	@Test
	void returnsFalseWhenNoTransferHandlerIsInstalled() {
		MapContainer container = newContainer();
		assertFalse(container.delegateToInstalledTransferHandler(fileListTransferable(new File("countries.shp"))));
	}

	@Test
	void returnsFalseWithoutThrowingWhenTheInstalledHandlerRejectsTheFlavor() {
		MapContainer container = newContainer();
		RecordingFileDropHandler recorder = new RecordingFileDropHandler();
		container.setTransferHandler(new FileDropHandler(recorder));

		// A plain string, not a file list - FileDropHandler.canImport() requires
		// DataFlavor.javaFileListFlavor, so this should be rejected, not thrown.
		boolean imported = container.delegateToInstalledTransferHandler(new StringSelection("not a file"));

		assertFalse(imported);
	}

	@Test
	void canDelegateHandleReflectsWhetherTheInstalledHandlerAcceptsTheFlavor() {
		MapContainer container = newContainer();
		container.setTransferHandler(new FileDropHandler(new RecordingFileDropHandler()));

		assertTrue(container.canDelegateHandle(fileListTransferable(new File("countries.shp"))),
				"used by dragEnter/dragOver to decide the accept/reject cursor before drop() is ever called");
		assertFalse(container.canDelegateHandle(new StringSelection("not a file")));
	}

	@Test
	void canDelegateHandleIsFalseWithNoTransferHandlerInstalled() {
		MapContainer container = newContainer();
		assertFalse(container.canDelegateHandle(fileListTransferable(new File("countries.shp"))));
	}

	@Test
	void filterStillAppliesSoAllRejectedFilesMeansNoImport() throws IOException {
		MapContainer container = newContainer();
		RecordingFileDropHandler recorder = new RecordingFileDropHandler();
		recorder.setFileFilter(f -> f.getName().endsWith(".shp"));
		container.setTransferHandler(new FileDropHandler(recorder));

		boolean imported = container.delegateToInstalledTransferHandler(fileListTransferable(new File("notes.txt")));

		assertFalse(imported, "a drop where every file is rejected by the filter should not be imported");
	}
}
