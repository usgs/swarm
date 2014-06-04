package gov.usgs.swarm.heli;

import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.decorate.FrameDecorator;
import gov.usgs.plot.decorate.SmartTick;
import gov.usgs.plot.render.AxisRenderer;
import gov.usgs.plot.render.FrameRenderer;
import gov.usgs.plot.render.HelicorderRenderer;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.wave.WaveClipboardFrame;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.swarm.wave.WaveViewPanelAdapter;
import gov.usgs.util.Time;
import gov.usgs.util.Util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * A <code>JComponent</code> for displaying and interacting with a helicorder.
 * 
 * @author Dan Cervelli
 */
public class HelicorderViewPanel extends JComponent {
	private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);
	public static final long serialVersionUID = -1;

	public static final int X_OFFSET = 70;
	public static final int Y_OFFSET = 10;
	public static final int RIGHT_WIDTH = 70;
	public static final int BOTTOM_HEIGHT = 35;
	private int insetHeight = 200;

	private Plot plot;
	private HelicorderRenderer heliRenderer;
	private HelicorderViewerSettings settings;
	private HelicorderData heliData;
	private double startTime;
	private double endTime;
	private double mean;
	private double bias;
	private HelicorderViewerFrame parent;
	private double[] translation;

	private WaveViewPanel insetWavePanel;

	private BufferedImage bufferImage, displayImage;
	private DateFormat dateFormat;

	private boolean working;
	private boolean resized;

	private int insetY;

	private boolean fullScreen;

	private boolean minimal;

	private double startMark = Double.NaN;
	private double endMark = Double.NaN;
	private double lastMark = Double.NaN;

	private EventListenerList listeners = new EventListenerList();

	public HelicorderViewPanel(HelicorderViewerFrame hvf) {
		// setBackground(new Color(255, 255, 255, 128));
		parent = hvf;
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		plot = new Plot();
		plot.setBackgroundColor(BACKGROUND_COLOR);
		// plot.setBackgroundColor(null);
		settings = hvf.getHelicorderViewerSettings();
		heliRenderer = new HelicorderRenderer();
		if (Swarm.config.heliColors != null)
			heliRenderer.setDefaultColors(Swarm.config.heliColors);// DCK: add
																	// configured
																	// colors
		heliRenderer.setExtents(0, 1, Double.MAX_VALUE, -Double.MAX_VALUE);
		plot.addRenderer(heliRenderer);

		this.setRequestFocusEnabled(true);
		this.addMouseListener(new HelicorderMouseListener());
		this.addMouseMotionListener(new HelicorderMouseMotionListener());
		this.addMouseWheelListener(new HelicorderMouseWheelListener());

		cursorChanged();
	}

	public void addListener(HelicorderViewPanelListener listener) {
		listeners.add(HelicorderViewPanelListener.class, listener);
	}

	public void removeListener(HelicorderViewPanelListener listener) {
		listeners.remove(HelicorderViewPanelListener.class, listener);
	}

	public void fireInsetCreated(double st, double et) {
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
			if (ls[i] == HelicorderViewPanelListener.class)
				((HelicorderViewPanelListener) ls[i + 1]).insetCreated(st, et);
	}

	public void cursorChanged() {
		Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
		if (Swarm.config.useLargeCursor) {
			Image cursorImg = Icons.crosshair.getImage();
			crosshair = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(16, 16), "Large crosshair");
		}

		this.setCursor(crosshair);
	}

	public void settingsChanged() {
		if (insetWavePanel != null) {
			double zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
			double j2k = insetWavePanel.getStartTime() + (insetWavePanel.getEndTime() - insetWavePanel.getStartTime())
					/ 2;
			loadInsetWave(j2k - zoomOffset, j2k + zoomOffset);
		}
		parent.settingsChanged();

		repaint();
	}

	public void setStartMark(double t) {
		startMark = t;
	}

	public void setEndMark(double t) {
		endMark = t;
	}

	public void clearMarks() {
		startMark = endMark = Double.NaN;
	}

	public void setCursorMark(double t) {
		if (insetWavePanel != null)
			insetWavePanel.setCursorMark(t);
	}

	public void markTime(double t) {
		if (Double.isNaN(startMark) && Double.isNaN(endMark)) {
			startMark = t;
		} else if (!Double.isNaN(startMark) && Double.isNaN(endMark)) {
			endMark = t;
			if (endMark < startMark) {
				double tm = startMark;
				startMark = endMark;
				endMark = tm;
			}
		} else {
			startMark = Math.min(lastMark, t);
			endMark = Math.max(lastMark, t);
		}
		lastMark = t;
		if (insetWavePanel != null) {
			insetWavePanel.setMarks(startMark, endMark);
		}
		repaint();
	}

	class HelicorderMouseMotionListener implements MouseMotionListener {
		public void mouseDragged(MouseEvent e) {
			Swarm.getApplication().touchUITime();
			HelicorderViewPanel.this.requestFocus();
			int mx = e.getX();
			int my = e.getY();

			if (mx < heliRenderer.getGraphX() || my < heliRenderer.getGraphY()
					|| mx > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
					|| my > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1) {
				/*
				 * // removed because it wasn't helpful! if (insetWavePanel !=
				 * null) { removeWaveInset(); repaint(); } return;
				 */
			} else {
				double j2k = getMouseJ2K(mx, my);
				processMousePosition(mx, my);

				if (SwingUtilities.isLeftMouseButton(e))
					createWaveInset(j2k, mx, my);
			}
		}

		public void mouseMoved(MouseEvent e) {
			Swarm.getApplication().touchUITime();
			processMousePosition(e.getX(), e.getY());
		}
	}

	class HelicorderMouseWheelListener implements MouseWheelListener {
		int totalScroll = 0;
		Delay delay;

		public void mouseWheelMoved(MouseWheelEvent e) {
			Swarm.getApplication().touchUITime();
			totalScroll += e.getWheelRotation();
			if (delay == null)
				delay = new Delay(250);
			else
				delay.restart();
		}

		public void delayOver() {
			removeWaveInset();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					parent.scroll(totalScroll);
					delay = null;
					totalScroll = 0;
				}
			});
		}

		class Delay extends Thread {
			long delayLeft;

			public Delay(long ms) {
				delayLeft = ms;
				start();
			}

			public void restart() {
				interrupt();
			}

			public void run() {
				boolean done = false;
				while (!done) {
					try {
						Thread.sleep(delayLeft);
						done = true;
					} catch (Exception e) {
					}
				}
				delayOver();
			}
		}
	}

	class HelicorderMouseListener implements MouseListener {
		public void mouseClicked(MouseEvent e) {
			Swarm.getApplication().touchUITime();
			HelicorderViewPanel.this.requestFocus();
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			Swarm.getApplication().touchUITime();
			if (e.getButton() == MouseEvent.BUTTON1) {
				int mx = e.getX();
				int my = e.getY();
				if (mx < heliRenderer.getGraphX() || my < heliRenderer.getGraphY()
						|| mx > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
						|| my > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1)
					return;

				double j2k = getMouseJ2K(mx, my);
				if (j2k != -1E300) {
					if (insetWavePanel != null)
						insetWavePanel.setWave(null, 0, 1);
					createWaveInset(j2k, mx, my);
				}
			}
			/*
			 * else if (e.getButton() == MouseEvent.BUTTON3) { if
			 * (insetWavePanel != null) { removeWaveInset(); repaint(); } }
			 */
		}

		public void mouseReleased(MouseEvent e) {
		}
	}

	public boolean hasInset() {
		return insetWavePanel != null;
	}

	public HelicorderData getData() {
		return heliData;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void insetToClipboard() {
		if (insetWavePanel != null) {
			WaveViewPanel p = new WaveViewPanel(insetWavePanel);
			p.setDataSource(insetWavePanel.getDataSource().getCopy());
			WaveClipboardFrame cb = Swarm.getApplication().getWaveClipboard();
			cb.setVisible(true);
			cb.addWave(p);
			requestFocus();
		}
	}

	private void processMousePosition(int x, int y) {
		if (heliData == null)
			return;

		String status = null;

		boolean wp = false;
		if (insetWavePanel != null) {
			Point loc = insetWavePanel.getLocation();
			wp = insetWavePanel.processMousePosition(x - loc.x, y - loc.y);
		}

		if (!wp) {
			if (status == null) {
				if (!(x < heliRenderer.getGraphX() || y < heliRenderer.getGraphY()
						|| x > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1 || y > heliRenderer
						.getGraphY() + heliRenderer.getGraphHeight() - 1)) {
					double j2k = getMouseJ2K(x, y);
					status = dateFormat.format(Util.j2KToDate(j2k));
					TimeZone tz = Swarm.config.getTimeZone(settings.channel);
					double tzo = Time.getTimeZoneOffset(tz, j2k);
					if (tzo != 0) {
						String tza = tz.getDisplayName(tz.inDaylightTime(Util.j2KToDate(j2k)), TimeZone.SHORT);
						status = dateFormat.format(Util.j2KToDate(j2k + tzo)) + " (" + tza + "), " + status + " (UTC)";
					}
				}
			}

			if (status == null)
				status = " ";

			if (!Double.isNaN(startMark) && !Double.isNaN(endMark)) {
				double dur = Math.abs(startMark - endMark);
				String pre = String.format("Duration: %.2fs (Md: %.2f)", dur, Swarm.config.getDurationMagnitude(dur));
				if (status.length() > 2)
					status = pre + ", " + status;
				else
					status = pre;
			}

			if (status != null)
				parent.setStatus(status);
		}
	}

	public double getMouseJ2K(int mx, int my) {
		double j2k = 0;
		if (translation != null) {
			j2k = translation[4];
			j2k += (mx - translation[0]) * translation[7];
			j2k += getHelicorderRow(my) * translation[6];
		}
		return j2k;
	}

	public int getHelicorderRow(int my) {
		return (int) Math.floor((my - translation[3]) / translation[2]);
	}

	public void removeWaveInset() {
		if (insetWavePanel != null) {
			parent.setInsetButtonsEnabled(false);
			this.remove(insetWavePanel);
			insetWavePanel = null;
			repaint();
		}
	}

	public void moveInset(int offset) {
		if (insetWavePanel != null) {
			double st = insetWavePanel.getStartTime();
			double et = insetWavePanel.getEndTime();
			double dt = et - st;
			double nst = st + dt * offset;
			double net = et + dt * offset;

			int row = heliRenderer.getRow(st + dt * offset + (dt / 2));
			if (row < 0 || row >= heliRenderer.getNumRows())
				return;

			loadInsetWave(nst, net);
			int height = insetHeight;

			if (row * translation[2] + translation[3] > height + translation[3]) {
				int y = (int) Math.ceil((row - 1) * translation[2] + translation[3]);
				insetWavePanel.setLocation(0, y - height);
			} else {
				int y = (int) Math.ceil((row + 2) * translation[2] + translation[3]);
				insetWavePanel.setLocation(0, y);
			}
		}
	}

	public void createWaveInset(final double j2k, final int mx, final int my) {
		if (working)
			return;

		insetY = my;

		if (insetWavePanel == null) {
			insetWavePanel = new WaveViewPanel(parent.getWaveViewSettings());
			insetWavePanel.addListener(new WaveViewPanelAdapter() {
				public void waveClosed(WaveViewPanel src) {
					removeWaveInset();
				}

				public void waveTimePressed(WaveViewPanel src, MouseEvent e, double j2k) {
					if (Swarm.config.durationEnabled && SwingUtilities.isLeftMouseButton(e))
						markTime(j2k);
					insetWavePanel.processMousePosition(e.getX(), e.getY());
				}
			});
		}

		// insetWavePanel.setHelicorderPanel(this);
		insetWavePanel.setMarks(startMark, endMark);
		insetWavePanel.setChannel(settings.channel);
		insetWavePanel.setDataSource(parent.getDataSource());
		insetWavePanel.setStatusLabel(parent.getStatusLabel());

		Dimension d = getSize();
		insetHeight = getHeight() / 4;
		int height = insetHeight;
		int row = heliRenderer.getRow(j2k);

		insetWavePanel.setSize(d.width + 2, height);
		if (insetY - heliRenderer.getRowHeight() > height + translation[3]) {
			int y = (int) Math.ceil((row - 1) * translation[2] + translation[3]);
			insetWavePanel.setLocation(-1, y - height);
		} else {
			int y = (int) Math.ceil((row + 2) * translation[2] + translation[3]);
			insetWavePanel.setLocation(-1, y);
		}

		insetWavePanel.setAllowClose(true);
		insetWavePanel.setWorking(true);

		double zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
		loadInsetWave(j2k - zoomOffset, j2k + zoomOffset);
		parent.setInsetButtonsEnabled(true);
		this.add(insetWavePanel);
		repaint();
	}

	private void loadInsetWave(final double st, final double et) {
		fireInsetCreated(st, et);
		final SwingWorker worker = new SwingWorker() {
			private Wave sw;

			public Object construct() {
				parent.getThrobber().increment();
				working = true;
				sw = parent.getWave(st, et);
				return null;
			}

			public void finished() {
				parent.getThrobber().decrement();
				working = false;
				if (insetWavePanel != null) {
					insetWavePanel.setWave(sw, st, et);
					insetWavePanel.setWorking(false);
				}
				repaint();
			}
		};
		worker.start();
	}

	public void setHelicorder(HelicorderData d, double time1, double time2) {
		heliData = d;
		if (heliData != null) {
			startTime = time1;
			endTime = time2;
			heliRenderer.setData(heliData);
			heliRenderer.setTimeChunk(settings.timeChunk);
			heliRenderer.setTimeZone(Swarm.config.getTimeZone(settings.channel));
			heliRenderer.setForceCenter(settings.forceCenter);
			heliRenderer.setClipBars(settings.clipBars);
			heliRenderer.setShowClip(settings.showClip);
			heliRenderer.setAlertClip(settings.alertClip);
			heliRenderer.setClipWav("clip.wav");
			heliRenderer.setClipAlertTimeout(settings.alertClipTimeout);
			mean = heliData.getMeanMax();
			bias = heliData.getBias();
			if (bias != mean)
				mean = Math.abs(bias - mean);
			heliRenderer.setClipValue(settings.clipValue);

		}
	}

	public void invalidateImage() {
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				createImage();
				return null;
			}

			public void finished() {
				displayImage = bufferImage;
				repaint();
			}
		};
		worker.start();
	}

	public void setResized(boolean b) {
		resized = b;
	}

	private void createImage() {
		if (heliData == null)
			return;

		Dimension d = this.getSize();
		if (d.width <= 0 || d.height <= 0)
			return;

		bufferImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_4BYTE_ABGR);

		Graphics2D ig = (Graphics2D) bufferImage.getGraphics();
		plot.setSize(d);

		double offset = 0;
		double multiplier = 1;
		Metadata md = Swarm.config.getMetadata(settings.channel);
		if (md != null) {
			offset = md.getOffset();
			multiplier = md.getMultiplier();
		}

		if (minimal)
			heliRenderer.setLocation(X_OFFSET / 2, Y_OFFSET, d.width - X_OFFSET - 5, d.height - Y_OFFSET
					- BOTTOM_HEIGHT / 2);
		else
			heliRenderer.setLocation(X_OFFSET, Y_OFFSET, d.width - X_OFFSET - RIGHT_WIDTH, d.height - Y_OFFSET
					- BOTTOM_HEIGHT);

		if (settings.autoScale) {
			settings.barRange = (int) (mean * settings.barMult);
			settings.clipValue = (int) (mean * settings.clipBars);
			heliRenderer.setHelicorderExtents(startTime, endTime, -1 * Math.abs(settings.barRange),
					Math.abs(settings.barRange));
		} else {
			heliRenderer.setHelicorderExtents(startTime, endTime,
					-1 * Math.abs((settings.barRange - offset) / multiplier),
					Math.abs((settings.barRange - offset) / multiplier));
		}

		heliRenderer.setTimeZone(Swarm.config.getTimeZone(settings.channel));
		heliRenderer.setClipValue(settings.clipValue);
		if (minimal) {
			// System.out.println("minimal");
			// heliRenderer.createMinimumAxis();
			heliRenderer.setFrameDecorator(new SmallDecorator());
		} else
			heliRenderer.createDefaultAxis();

		if (md == null || md.getAlias() == null)
			heliRenderer.setChannel(settings.channel);
		else
			heliRenderer.setChannel(md.getAlias());

		translation = heliRenderer.getTranslationInfo(false);
		heliRenderer.setLargeChannelDisplay(fullScreen);

		try {
			plot.render(ig);
		} catch (PlotException e) {
			e.printStackTrace();
		}
	}

	class SmallDecorator extends FrameDecorator {
		public void decorate(FrameRenderer fr) {
			HelicorderRenderer hr = (HelicorderRenderer) fr;
			AxisRenderer axis = new AxisRenderer(fr);
			axis.createDefault();
			fr.setAxis(axis);

			int minutes = (int) Math.round(settings.timeChunk / 60.0);
			int majorTicks = minutes;
			if (minutes > 30 && minutes < 180)
				majorTicks = minutes / 5;
			else if (minutes >= 180 && minutes < 360)
				majorTicks = minutes / 10;
			else if (minutes >= 360)
				majorTicks = minutes / 20;
			double[] mjt = SmartTick.intervalTick(0, settings.timeChunk, majorTicks);

			axis.createBottomTicks(null, mjt);
			axis.createTopTicks(null, mjt);
			axis.createVerticalGridLines(mjt);

			int bc = Math.round(settings.timeChunk / 5) + 2;
			String[] btl = new String[bc];
			double[] btlv = new double[bc];
			btl[0] = "+";
			btlv[0] = 30;
			for (int i = 0, j = 1; i < mjt.length; i++) {
				long m = Math.round(mjt[i] / 60.0);
				if (m % 5 == 0) {
					btl[j] = Long.toString(m);
					btlv[j++] = mjt[i];
				}
			}
			axis.createBottomTickLabels(btlv, btl);

			int numRows = hr.getNumRows();
			double[] labelPosLR = new double[numRows];
			String[] leftLabelText = new String[numRows];
			String[] rightLabelText = new String[numRows];
			TimeZone timeZone = Swarm.config.getTimeZone(settings.channel);

			DateFormat localTimeFormat = new SimpleDateFormat("HH:mm");
			localTimeFormat.setTimeZone(timeZone);

			DateFormat localDayFormat = new SimpleDateFormat("MM-dd");
			localDayFormat.setTimeZone(timeZone);

			double pixelsPast = 0;
			double pixelsPerRow = fr.getGraphHeight() / numRows;
			String lastLocalDay = "";
			for (int i = numRows - 1; i >= 0; i--) {
				pixelsPast += pixelsPerRow;
				labelPosLR[i] = i + 0.5;

				String localTime = localTimeFormat.format(Util.j2KToDate(hr.getHelicorderMaxX() - (i + 1)
						* settings.timeChunk));
				leftLabelText[i] = null;
				if (pixelsPast > 20) {
					leftLabelText[i] = localTime;
					pixelsPast = 0;
				}

				Date dtz = Util.j2KToDate(hr.getHelicorderMaxX() - (i + 1) * settings.timeChunk);
				String localDay = localDayFormat.format(new Date(dtz.getTime() + settings.timeChunk * 1000));
				if (!localDay.equals(lastLocalDay)) {
					rightLabelText[i] = localDay;
					lastLocalDay = localDay;
				}
			}

			axis.createLeftTickLabels(labelPosLR, leftLabelText);
			axis.createRightTickLabels(labelPosLR, rightLabelText);

			boolean dst = timeZone.inDaylightTime(Util.j2KToDate(hr.getViewEndTime()));
			String timeZoneName = timeZone.getDisplayName(dst, TimeZone.SHORT);
			TextRenderer tr = new TextRenderer(3, fr.getGraphY() + fr.getGraphHeight() + 14, timeZoneName);
			tr.font = Font.decode("dialog-PLAIN-9");
			tr.antiAlias = false;
			axis.addPostRenderer(tr);

			double[] hg = new double[numRows - 1];
			for (int i = 0; i < numRows - 1; i++)
				hg[i] = i + 1.0;

			axis.createHorizontalGridLines(hg);

			axis.setBackgroundColor(Color.white);
		}
	}

	public void setFullScreen(boolean b) {
		fullScreen = b;
	}

	public void setMinimal(boolean b) {
		minimal = b;
	}

	private void drawMark(Graphics2D g2, double t, Color color) {
		if (Double.isNaN(t))
			return;

		int x = (int) (heliRenderer.helicorderGetXPixel(t));
		int row = heliRenderer.getRow(t);
		int y = (int) Math.ceil(row * translation[2] + translation[3]);
		g2.setColor(color);
		g2.draw(new Line2D.Double(x, y, x, y + translation[2]));

		GeneralPath gp = new GeneralPath();
		gp.moveTo(x, y);
		gp.lineTo((float) x - 4, (float) y - 6);
		gp.lineTo((float) x + 4, (float) y - 6);
		gp.closePath();
		g2.setColor(Color.GREEN);
		g2.fill(gp);
		g2.setColor(DARK_GREEN);
		g2.draw(gp);

		gp.reset();
		gp.moveTo(x, (float) (y + translation[2]));
		gp.lineTo((float) x - 4, (float) (y + 6 + translation[2]));
		gp.lineTo((float) x + 4, (float) (y + 6 + translation[2]));
		gp.closePath();
		gp.closePath();
		g2.setColor(Color.GREEN);
		g2.fill(gp);
		g2.setColor(DARK_GREEN);
		g2.draw(gp);
	}

	private static final Color DARK_GREEN = new Color(0, 168, 0);

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Dimension d = this.getSize();
		if (heliData == null) {
			// if (parent.isWorking())
			// g2.drawString("Attempting to retrieve data...", d.width / 2 - 75,
			// d.height / 2);
			// else
			if (!parent.isWorking()) {
				// g2.drawString("The server returned no helicorder data.",
				// d.width / 2 - 150, d.height / 2);
				parent.setStatus("The server returned no helicorder data.");
				// double start = settings.getBottomTime() - settings.span * 60;
				// double end = settings.getBottomTime();
				// g2.drawString(String.format("Time range: %s to %s",
				// Time.format(Time.STANDARD_TIME_FORMAT, start),
				// Time.format(Time.STANDARD_TIME_FORMAT, end)),
				// d.width / 2 - 100, d.height / 2 + 16);
			}
		} else if (displayImage != null)
			g2.drawImage(displayImage, 0, 0, null);

		drawMark(g2, startMark, DARK_GREEN);
		drawMark(g2, endMark, DARK_GREEN);

		if (insetWavePanel != null) {
			// find out where time highlight will be, possibly reposition the
			// insetWavePanel
			double t1 = insetWavePanel.getStartTime();
			double t2 = insetWavePanel.getEndTime();
			double t = (t2 - t1) / 2 + t1;
			int row = heliRenderer.getRow(t);
			if (resized) {
				insetWavePanel.setSize(d.width, insetHeight);
				insetWavePanel.createImage();
				if (row * translation[2] > insetHeight + translation[3]) {
					int y = (int) Math.ceil((row - 1) * translation[2] + translation[3]);
					insetWavePanel.setLocation(0, y - insetHeight);
				} else {
					int y = (int) Math.ceil((row + 2) * translation[2] + translation[3]);
					insetWavePanel.setLocation(0, y);
				}
			}

			// now it's safe to draw the waveInsetPanel
			Point p = insetWavePanel.getLocation();
			g2.translate(p.x, p.y);
			insetWavePanel.paint(g2);
			Dimension wvd = insetWavePanel.getSize();
			g.setColor(Color.gray);
			g.drawRect(0, 0, wvd.width - 1, wvd.height);
			g2.translate(-p.x, -p.y);

			// fixes bug where highlight was being drawn before wave loaded
			if (row < 0)
				return;

			// finally, draw the highlight. support for spanning one row
			// above and one row below the center point.
			// TODO: support multi-row span
			Paint pnt = g2.getPaint();
			g2.setPaint(new Color(255, 255, 0, 128));
			Rectangle2D.Double rect = new Rectangle2D.Double();
			int zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
			double xo = 1.0 / translation[7] * zoomOffset;
			int bx = (int) (heliRenderer.helicorderGetXPixel(t) - xo);
			int width = (int) (xo * 2);
			int right = heliRenderer.getGraphX() + heliRenderer.getGraphWidth();
			if (bx < heliRenderer.getGraphX()) {
				int width2 = heliRenderer.getGraphX() - bx;
				rect.setRect(right - width2, (int) Math.ceil((row - 1) * translation[2] + translation[3]), width2,
						(int) Math.ceil(translation[2]));
				if (row - 1 >= 0)
					g2.fill(rect);
				bx = heliRenderer.getGraphX();
				width = width - width2;
			}
			if (bx + width > right) {
				int width2 = bx + width - right;
				rect.setRect(heliRenderer.getGraphX() + 1,
						(int) Math.ceil((row + 1) * translation[2] + translation[3]), width2,
						(int) Math.ceil(translation[2]));
				if (row + 1 < heliRenderer.getNumRows())
					g2.fill(rect);
				width = width - width2;
			}
			rect.setRect(bx, (int) Math.ceil(row * translation[2] + translation[3]), width,
					(int) Math.floor(translation[2]));

			g2.fill(rect);
			g2.setPaint(pnt);
		}

		resized = false;
	}
}