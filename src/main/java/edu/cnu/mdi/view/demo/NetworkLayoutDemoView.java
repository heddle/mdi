package edu.cnu.mdi.view.demo;

import java.awt.Point;
import java.awt.geom.Point2D.Double;
import java.util.List;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.toolbar.IContainerToolBar;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.view.BaseView;

@SuppressWarnings("serial")
public class NetworkLayoutDemoView extends BaseView implements IFeedbackProvider {


	public NetworkLayoutDemoView(Object... keyVals) {
		super(PropertySupport.fromKeyValues(keyVals));
		getContainer().getFeedbackControl().addFeedbackProvider(this);
		addToToolBar();
	}
	
	private void addToToolBar() {
	    IContainerToolBar tb = getContainer().getToolBar();
	    if (tb == null) return;
	    
	    // this is a useless button that just beeps when pressed,
	    //but it shows hot to add "one-shot" buttons to the toolbar
        tb.addOneShot(new BeepButton());
        
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.CAMERA),      EDeviceSymbol.CAMERA.iconPath,      "Camera");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.COMPUTER),    EDeviceSymbol.COMPUTER.iconPath,    "Computer");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.LAPTOP),      EDeviceSymbol.LAPTOP.iconPath,      "Laptop");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.MONITOR),     EDeviceSymbol.MONITOR.iconPath,     "Monitor");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.ROUTER),      EDeviceSymbol.ROUTER.iconPath,      "Router");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.TABLET),      EDeviceSymbol.TABLET.iconPath,      "Tablet");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.VIDEOCAMERA), EDeviceSymbol.VIDEOCAMERA.iconPath, "Video camera");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.WEBCAM),      EDeviceSymbol.WEBCAM.iconPath,      "Webcam");
        tb.addToolToggle(new PlaceDeviceTool(EDeviceSymbol.WORKSTATION), EDeviceSymbol.WORKSTATION.iconPath, "Workstation");
        
	}


	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Double wp, List<String> feedbackStrings) {
	}

}
