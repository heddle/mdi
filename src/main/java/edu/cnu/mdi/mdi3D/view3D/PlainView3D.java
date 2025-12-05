package edu.cnu.mdi.mdi3D.view3D;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;

import javax.swing.Box;
import javax.swing.JMenuBar;

import edu.cnu.mdi.mdi3D.panel.Panel3D;
import edu.cnu.mdi.properties.PropertySupport;
import edu.cnu.mdi.view.BaseView;


@SuppressWarnings("serial")
public abstract class PlainView3D extends BaseView implements ActionListener {

	// the menu bar
	private final JMenuBar _menuBar;


	// the 3D panel
	protected final Panel3D _panel3D;

	public PlainView3D(Object... keyVals) {
		super(PropertySupport.fromKeyValues(keyVals)); 
		_menuBar = new JMenuBar();
		setJMenuBar(_menuBar);
		addMenus();

		float angleX = PropertySupport.getFloat(properties, PropertySupport.ANGLE_X);
		float angleY = PropertySupport.getFloat(properties, PropertySupport.ANGLE_Y);
		float angleZ = PropertySupport.getFloat(properties, PropertySupport.ANGLE_Z);
		float xDist = PropertySupport.getFloat(properties, PropertySupport.DIST_X);
		float yDist = PropertySupport.getFloat(properties, PropertySupport.DIST_Y);
		float zDist = PropertySupport.getFloat(properties, PropertySupport.DIST_Z);
		setLayout(new BorderLayout(1, 1));
		_panel3D = make3DPanel(angleX, angleY, angleZ, xDist, yDist, zDist);

		add(_panel3D, BorderLayout.CENTER);
		add(Box.createHorizontalStrut(1), BorderLayout.WEST);
	//	pack();
		
	}
	

	// make the 3d panel
	protected abstract Panel3D make3DPanel(float angleX, float angleY, float angleZ, float xDist, float yDist,
			float zDist);


	// add the menus
	protected void addMenus() {
	}


	@Override
	public void actionPerformed(ActionEvent e) {
	}

	@Override
	public void focusGained(FocusEvent e) {
		if (_panel3D != null) {
			_panel3D.requestFocus();
		}
	}

	@Override
	public void refresh() {
		if (_panel3D != null) {
			_panel3D.refresh();
		}
	}

}
