package edu.cnu.mdi.mapping;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;

import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.view.BaseView;


public class MapView2D extends BaseView implements MouseMotionListener {
	
	
	//the map projection
	private IMapProjection _projection; 
	private GraticuleRenderer _gratRenderer;
	
	public MapView2D(Object... keyVals) {
		super(keyVals);
		
		setProjection(EProjection.LAMBERT_EQUAL_AREA);
		
		getContainer().setWorldSystem(getWorldSystem(_projection.getProjection()));
		getContainer().getComponent().addMouseMotionListener(this);
		getContainer().setAfterDraw(new DrawableAdapter() {
		    @Override
		    public void draw(Graphics2D g, IContainer container) {
		        _gratRenderer.render(g, container);
		    }
		});
	}

	/**
	 * Get the map projection
	 * @return the map projection
	 */
	public IMapProjection getProjection() {
		return _projection;
	}
	
	public void setProjection(EProjection projection) {
		_projection = ProjectionFactory.create(projection);
		_gratRenderer = new GraticuleRenderer(_projection);
	}


	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	/**
     * Get the default world system
     * @param eprojection the map projection
     * @return the world system
     */
	protected Rectangle2D.Double getWorldSystem(EProjection eprojection) {

		double xlim;
		double ylim;

		switch (eprojection) {
		case MOLLWEIDE:
			xlim = 2.1;
			ylim = 1.4;
			break;
		case MERCATOR:
			xlim = 1.1*Math.PI;
			ylim = 1.1*Math.PI;
			break;
		case ORTHOGRAPHIC:
			xlim = 1.1;
			ylim = 1.1;
			break;
		case LAMBERT_EQUAL_AREA:
			xlim = Math.PI/2;
			ylim = Math.PI/2;
           break;
		default:
			xlim = 2.1;
			ylim = 1.4;
		}
		return new Rectangle2D.Double(-xlim, -ylim, 2 * xlim, 2 * ylim);
	}



}
