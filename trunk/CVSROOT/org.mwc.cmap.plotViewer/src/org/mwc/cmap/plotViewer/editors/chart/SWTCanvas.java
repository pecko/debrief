// Copyright MWC 1999, Debrief 3 Project
// $RCSfile$
// @author $Author$
// @version $Revision$
// $Log$
// Revision 1.26  2006-05-31 13:40:17  Ian.Mayo
// Minor tidying
//
// Revision 1.25  2006/05/17 08:34:30  Ian.Mayo
// Reduce number of instances where we set canvas-color
//
// Revision 1.24  2006/05/16 08:45:16  Ian.Mayo
// Include another separator
//
// Revision 1.23  2006/04/06 13:31:13  Ian.Mayo
// Output time-to-plot to log for performance tracking
//
// Revision 1.22  2006/04/06 13:03:13  Ian.Mayo
// Ditch performance indicators
//
// Revision 1.21  2006/04/05 08:33:59  Ian.Mayo
// Minor tidying
//
// Revision 1.20  2006/02/23 11:49:07  Ian.Mayo
// Tidying
//
// Revision 1.19  2005/12/09 14:54:37  Ian.Mayo
// Add right-click property editing
//
// Revision 1.18  2005/11/14 10:28:24  Ian.Mayo
// Double-check everything is there before we start plotting
//
// Revision 1.17  2005/09/08 11:01:41  Ian.Mayo
// Makeing more robust when plotting fails through disposed GC
//
// Revision 1.16  2005/08/31 15:02:23  Ian.Mayo
// Do display updates in UI thread
//
// Revision 1.15  2005/06/22 13:22:00  Ian.Mayo
// Part way through implementation of copy location from plot
//
// Revision 1.14  2005/06/22 10:27:43  Ian.Mayo
// Insert tests, tidy export of location to clipboard
//
// Revision 1.13  2005/06/22 09:18:32  Ian.Mayo
// Tidy implementation of actions which receive location data
//
// Revision 1.12  2005/06/20 08:06:10  Ian.Mayo
// Experiment with right-click support (copy location)
//
// Revision 1.11  2005/06/15 14:30:11  Ian.Mayo
// Refactor, so that we can call it more easily from WMF painter
//
// Revision 1.10  2005/06/15 11:03:42  Ian.Mayo
// Overcome tidying error
//
// Revision 1.9  2005/06/14 09:49:28  Ian.Mayo
// Eclipse-triggered tidying (unused variables)
//
// Revision 1.8  2005/05/26 14:04:50  Ian.Mayo
// Tidy up double-buffering
//
// Revision 1.7  2005/05/26 07:53:56  Ian.Mayo
// Minor tidying
//
// Revision 1.6  2005/05/25 15:31:54  Ian.Mayo
// Get double-buffering going
//
// Revision 1.5  2005/05/25 14:18:17  Ian.Mayo
// Refactor to provide more useful SWT GC wrapper (hopefully suitable for buffered images)
//
// Revision 1.4  2005/05/24 13:26:42  Ian.Mayo
// Start including double-click support.
//
// Revision 1.3  2005/05/24 07:35:57  Ian.Mayo
// Ignore anti-alias bits, sort out text-writing in filling areas
//
// Revision 1.2  2005/05/20 15:34:44  Ian.Mayo
// Hey, practically working!
//
// Revision 1.1  2005/05/20 13:45:03  Ian.Mayo
// Start doing chart
//
//

package org.mwc.cmap.plotViewer.editors.chart;

import java.awt.Dimension;
import java.util.Enumeration;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.mwc.cmap.core.CorePlugin;
import MWC.GUI.CanvasType;
import MWC.GenericData.WorldLocation;

/**
 * Swing implementation of a canvas.
 */
public class SWTCanvas extends SWTCanvasAdapter
{

	// ///////////////////////////////////////////////////////////
	// member variables
	// //////////////////////////////////////////////////////////

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	org.eclipse.swt.widgets.Canvas _myCanvas = null;

	/**
	 * our double-buffering safe copy.
	 */
	private transient Image _dblBuff;

	private LocationSelectedAction _copyLocation;

	// ///////////////////////////////////////////////////////////
	// constructor
	// //////////////////////////////////////////////////////////

	/**
	 * default constructor.
	 */
	public SWTCanvas(Composite parent)
	{
		super(null);

		_myCanvas = new Canvas(parent, SWT.NO_BACKGROUND);

		// add handler to catch canvas resizes
		_myCanvas.addControlListener(new ControlAdapter()
		{

			public void controlResized(final ControlEvent e)
			{
				Point pt = _myCanvas.getSize();
				Dimension dim = new Dimension(pt.x, pt.y);
				setScreenSize(dim);
			}
		});

		// switch on tooltips for this panel
		_myCanvas.setToolTipText("blank");

		// setup our own painter
		_myCanvas.addPaintListener(new org.eclipse.swt.events.PaintListener()
		{

			public void paintControl(PaintEvent e)
			{
				repaintMe(e);
			}
		});

//		_myCanvas.setBackground(ColorHelper.getColor(java.awt.Color.BLUE));

		_myCanvas.addMouseListener(new MouseAdapter()
		{

			/**
			 * @param e
			 */
			public void mouseUp(MouseEvent e)
			{
				if (e.button == 3)
				{
					// cool, right-had button. process it
					MenuManager mmgr = new MenuManager();
					Point display = Display.getCurrent().getCursorLocation();
					Point scrPoint = _myCanvas.toControl(display);
					WorldLocation targetLoc = getProjection().toWorld(
							new java.awt.Point(scrPoint.x, scrPoint.y));
					fillContextMenu(mmgr, scrPoint, targetLoc);
					Menu thisM = mmgr.createContextMenu(_myCanvas);
					thisM.setVisible(true);
				}
			}

		});
	}

	/**
	 * ok - insert the right-hand button related items
	 * 
	 * @param mmgr
	 * @param scrPoint 
	 */
	protected void fillContextMenu(MenuManager mmgr, Point scrPoint, WorldLocation loc)
	{
		// right, we create the actions afresh each time here. We can't
		// automatically calculate it.
		_copyLocation = new LocationSelectedAction("Copy cursor location",
				SWT.PUSH, loc)
		{
			/**
			 * @param loc
			 *          the converted world location for the mouse-click
			 * @param pt
			 *          the screen coordinate of the click
			 */
			public void run(WorldLocation theLoc)
			{
				// represent the location as a text-string
				String locText = CorePlugin.toClipboard(theLoc);

				// right, copy the location to the clipboard
				Clipboard clip = CorePlugin.getDefault().getClipboard();
				Object[] data = new Object[] { locText };
				Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
				clip.setContents(data, types);

			}
		};

		mmgr.add(_copyLocation);
		mmgr.add(new Separator());

		
	}

	// ////////////////////////////////////////////////////
	// screen redraw related
	// ////////////////////////////////////////////////////

	protected void repaintMe(PaintEvent pe)
	{

		// paintPlot(pe.gc);
		// get the graphics destination
		GC gc = pe.gc;

		// put double-buffering code in here.
		if (_dblBuff == null)
		{
			// ok, create the new image
			Point theSize = _myCanvas.getSize();

			if ((theSize.x == 0) || (theSize.y == 0))
				return;

			_dblBuff = new Image(Display.getCurrent(), theSize.x, theSize.y);
			GC theDest = new GC(_dblBuff);

			// prepare the ground (remember the graphics dest for a start)
			startDraw(theDest);

			// and paint into it
			paintPlot(this);

			// all finished, close it now
			endDraw(null);

		}

		// finally put the required bits of the target image onto the screen
		if(_dblBuff != null)
			gc.drawImage(_dblBuff, pe.x, pe.y, pe.width, pe.height, pe.x, pe.y,
				pe.width, pe.height);
		else
		{
			CorePlugin.logError(Status.INFO, "Double-buffering failed, no image produced", null);
		}
	}

	/**
	 * the real paint function, called when it's not satisfactory to just paint in
	 * our safe double-buffered image.
	 * 
	 * @param g1
	 */
	public void paintPlot(CanvasType dest)
	{
		// what's the time Mr Wolf?
		long tThen = System.currentTimeMillis();
		
		// go through our painters
		final Enumeration enumer = _thePainters.elements();
		while (enumer.hasMoreElements())
		{
			final CanvasType.PaintListener thisPainter = (CanvasType.PaintListener) enumer
					.nextElement();

			// check the screen has been defined
			final Dimension area = this.getProjection().getScreenArea();
			if ((area == null) || (area.getWidth() <= 0) || (area.getHeight() <= 0))
			{
				return;
			}

			// it must be ok
			thisPainter.paintMe(dest);
		}
		
		// how long was it?
		long tNow = System.currentTimeMillis();
		long tDelta = tNow - tThen;
		CorePlugin.logError(Status.INFO, "Canvas update took:" + tDelta + " millis", null);

	}

	// ///////////////////////////////////////////////////////////
	// member functions
	// //////////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////////
	// projection related
	// //////////////////////////////////////////////////////////
	/**
	 * handler for a screen resize - inform our projection of the resize then
	 * inform the painters.
	 */
	public void setScreenSize(final java.awt.Dimension p1)
	{
		super.setScreenSize(p1);

		// check if this is a real resize
		if ((_theSize == null) || (!_theSize.equals(p1)))
		{
			// inform our parent
			_myCanvas.setSize(p1.width, p1.height);

			// erase the double buffer, (if we have one)
			// since it is now invalid
			if (_dblBuff != null)
			{
				_dblBuff.dispose();
				_dblBuff = null;
			}

			// inform the listeners that we have resized
			final Enumeration enumer = _thePainters.elements();
			while (enumer.hasMoreElements())
			{
				final CanvasType.PaintListener thisPainter = (CanvasType.PaintListener) enumer
						.nextElement();
				thisPainter.resizedEvent(_theProjection, p1);
			}

		}
	}

	// ///////////////////////////////////////////////////////////
	// graphics plotting related
	// //////////////////////////////////////////////////////////

	/**
	 * first repaint the plot, then trigger a screen update
	 */
public final void updateMe()
	{
		if (_dblBuff != null)
		{
			_dblBuff.dispose();
			_dblBuff = null;
		}

		if(!_myCanvas.isDisposed())
		{
			// called deferred redraw method
			Display.getDefault().asyncExec(new Runnable(){
				public void run()
				{
					if(!_myCanvas.isDisposed())
					{
						_myCanvas.redraw();
					}
				}
				
			});
		}
	}
	/**
	 * provide close method, clear elements.
	 */
	public final void close()
	{
		_dblBuff = null;
	}

	public String getName()
	{
		// TODO Auto-generated method stub
		return "SWT Canvas";
	}

	public void redraw(int x, int y, int width, int height, boolean b)
	{
		_myCanvas.redraw(x, y, width, height, b);
	}

	public void addControlListener(ControlAdapter adapter)
	{
		_myCanvas.addControlListener(adapter);
	}

	public void addMouseMoveListener(MouseMoveListener listener)
	{
		_myCanvas.addMouseMoveListener(listener);
	}

	public void addMouseListener(MouseListener listener)
	{
		_myCanvas.addMouseListener(listener);
	}

	public Control getCanvas()
	{
		return _myCanvas;
	}

	public abstract static class LocationSelectedAction extends Action
	{

		WorldLocation _theLoc;

		/**
		 * pass some parameters back to the main parent
		 * 
		 * @param text
		 * @param style
		 * @param theCanvas -
		 *          used to get the screen coords
		 * @param theProjection -
		 *          our screen/world converter
		 */
		public LocationSelectedAction(String text, int style, WorldLocation theLoc)
		{
			super(text, style);
			_theLoc = theLoc;
		}

		/**
		 * 
		 */
		public void run()
		{
			// ok - trigger our geospatial operation
			run(_theLoc);
		}

		/**
		 * so, the user has selected a chart location, process the selection
		 * 
		 * @param loc
		 */
		abstract public void run(WorldLocation loc);
	}

	// ////////////////////////////////////////////////
	// testing code...
	// ////////////////////////////////////////////////
	static public class testImport extends junit.framework.TestCase
	{
		static public final String TEST_ALL_TEST_TYPE = "CONV";

		public testImport(String val)
		{
			super(val);
		}

		public void testClipboardTextManagement()
		{
			WorldLocation theLoc = new WorldLocation(12.3, 12.555555, 1.2);
			String txt = CorePlugin.toClipboard(theLoc);
			assertEquals("correct string not produced", "LOC:12.3,12.555555,1.2", txt);

			// check for valid location
			boolean validStr;
			validStr = CorePlugin.isLocation(txt);
			assertTrue("is a location string", validStr);

			// and check for duff location
			validStr = CorePlugin.isLocation("aasdfasdfasdfadf");
			assertFalse("is a location string", validStr);

			// and back to the location
			WorldLocation loc2 = CorePlugin.fromClipboard(txt);
			assertEquals("correct location parsed back in", theLoc, loc2);

			// try southern/western location
			theLoc = new WorldLocation(-12.3, -12.555555, -1.2);
			txt = CorePlugin.toClipboard(theLoc);
			assertEquals("correct string not produced", "LOC:-12.3,-12.555555,-1.2",
					txt);
		}
	}
}
