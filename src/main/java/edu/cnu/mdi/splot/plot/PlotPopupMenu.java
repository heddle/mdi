package edu.cnu.mdi.splot.plot;

import javax.swing.JPopupMenu;

public class PlotPopupMenu extends JPopupMenu {

    // the owner canvas
    protected PlotCanvas _plotCanvas;

    // all the menus and items
    protected SplotEditMenu _menus;

    public PlotPopupMenu(PlotCanvas plotCanvas) {
	_plotCanvas = plotCanvas;
	_menus = new SplotEditMenu(_plotCanvas, this, false);
    }

}
