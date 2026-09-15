package edu.cnu.mdi.splot.plot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.awt.event.MouseEvent;
import org.junit.jupiter.api.Test;
import edu.cnu.mdi.splot.pdata.PlotData;

class PlotMouseHandlerTest {
    @Test void mouseMoveAllowsPanelWithoutFeedbackPane() {
        PlotCanvas canvas=new PlotCanvas(PlotData.emptyData(),"Test","x","y");
        new PlotPanel(canvas,PlotPanel.BARE);
        MouseEvent event=new MouseEvent(canvas,MouseEvent.MOUSE_MOVED,System.currentTimeMillis(),0,10,10,0,false);
        assertDoesNotThrow(() -> new PlotMouseHandler(canvas).mouseMoved(event));
    }
}
