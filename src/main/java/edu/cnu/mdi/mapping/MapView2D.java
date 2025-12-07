package edu.cnu.mdi.mapping;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.cnu.mdi.component.EnumComboBox;
import edu.cnu.mdi.container.IContainer;
import edu.cnu.mdi.feedback.FeedbackControl;
import edu.cnu.mdi.feedback.FeedbackPane;
import edu.cnu.mdi.feedback.IFeedbackProvider;
import edu.cnu.mdi.graphics.drawable.DrawableAdapter;
import edu.cnu.mdi.graphics.drawable.IDrawable;
import edu.cnu.mdi.graphics.text.UnicodeSupport;
import edu.cnu.mdi.graphics.toolbar.BaseToolBar;
import edu.cnu.mdi.mapping.GeoJsonCountryLoader.CountryFeature;
import edu.cnu.mdi.view.BaseView;


public class MapView2D extends BaseView implements IFeedbackProvider, MouseMotionListener {
	
	//share country boundaries across all map views
	
	private static List<CountryFeature> _countries;
	
	//the map projection
	private IMapProjection _projection; 
	private GraticuleRenderer _gratRenderer;
	
	//feedback pane
	private FeedbackPane _feedbackPane;
	
	//country renderer
	private CountryFeatureRenderer _countryRenderer;
	
	//workspace and strings for feedback
	private LatLon _latLon = new LatLon();
	private static String _latPrefix = "$yellow$Lat (" + UnicodeSupport.SMALL_PHI + ")";
	private static String _lonPrefix = "$yellow$Lon (" + UnicodeSupport.SMALL_LAMBDA + ")";
	private static String _deg = UnicodeSupport.DEGREE;
	
	/**
	 * Create a map view with the given key-value pairs
	 * 
	 * @param keyVals variable set of arguments.
	 */
	public MapView2D(Object... keyVals) {
		super(keyVals);
		
		//view serves as a feedback provider
		getContainer().getFeedbackControl().addFeedbackProvider(this);
		
		//default to mercator projection
		setProjection(EProjection.MERCATOR);
		
		//add the projection combobox to the toolbar
		addProjectionComboBox();
		
		//set the feedback
		setFeedback();
		
		//set the before and after draws
		setBeforeDraw();
		setAfterDraw();

		getContainer().getComponent().addMouseMotionListener(this);
	}
	
	/**
	 * Set the view's before draw
	 */
	private void setBeforeDraw() {
		IDrawable beforeDraw = new DrawableAdapter() {
			@Override
			public void draw(Graphics g, IContainer container) {
			}
		};

		getContainer().setBeforeDraw(beforeDraw);
	}

	/**
	 * Set the view's after draw
	 */
	private void setAfterDraw() {
		IDrawable afterDraw = new DrawableAdapter() {
			@Override
			public void draw(Graphics2D g, IContainer container) {
				// 1. Clear background
				g.setColor(_projection.getTheme().getBackgroundColor());
				g.fillRect(0, 0, getWidth(), getHeight());

				// 2. Fill ocean inside projection clip
				_projection.fillOcean(g, container);

				// 3. Draw graticule and outline on top
				_gratRenderer.render(g, container);

				// 4. Draw land polygons, labels, etc...
				_countryRenderer.render(g, container);
			}
		};

		getContainer().setAfterDraw(afterDraw);
	}

	// Add the projection combo box to the toolbar
	private void addProjectionComboBox() {
		BaseToolBar toolBar = getToolBar();
		EnumComboBox<EProjection> combo = EProjection.createComboBox();
		toolBar.add(combo);

		combo.addActionListener(e -> {
			EProjection selected = combo.getSelectedEnum();
			setProjection(selected);
		});

	}
	
	//set up the feedback
	private void setFeedback() {
		FeedbackControl fbc = getContainer().getFeedbackControl();
		fbc.addFeedbackProvider(this);
		_feedbackPane = new FeedbackPane();
		getContainer().setFeedbackPane(_feedbackPane);

		Dimension dim = _feedbackPane.getPreferredSize();
		_feedbackPane.setPreferredSize(new Dimension(200, dim.height));
		add(_feedbackPane, BorderLayout.EAST);
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
		getContainer().resetWorldSystem(getWorldSystem(_projection.getProjection()));
		_countryRenderer = new CountryFeatureRenderer(_countries, _projection);
		refresh();
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
			xlim = 2.9;
			ylim = 2.9;
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

	@Override
	public void getFeedbackStrings(IContainer container, Point pp, Double wp, List<String> feedbackStrings) {
		
		boolean onMap = _projection.getLatLon(wp.x, wp.y, _latLon);
		
		String numCountryStr = String.format("Countries loaded: %d", 
				(_countries != null) ? _countries.size() : 0);
		String projStr = String.format("projection %s", _projection.name());
		String screenStr = String.format("screen [%d, %d] ", pp.x, pp.y);
		String worldStr = String.format("world [%6.2f, %6.2f] ", wp.x, wp.y);
		feedbackStrings.add(numCountryStr);
		feedbackStrings.add(projStr);
		feedbackStrings.add(screenStr);
		feedbackStrings.add(worldStr);
		
		if (onMap) {
			String latStr = String.format("%s %.2f%s", _latPrefix, _latLon.phiDeg(), _deg);
			String lonStr = String.format("%s %.2f%s", _lonPrefix, _latLon.lambdaDeg(), _deg);
			feedbackStrings.add(latStr);
			feedbackStrings.add(lonStr);
		}

	}

	/**
	 * Set the countries
	 * @param countries loaded from geojson
	 */
	public static void setCountries(List<CountryFeature> countries) {
		_countries = countries;
	}

}
