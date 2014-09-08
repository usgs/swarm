package gov.usgs.swarm.map;

import gov.usgs.plot.AxisRenderer;
import gov.usgs.plot.FrameDecorator;
import gov.usgs.plot.FrameRenderer;
import gov.usgs.plot.RectangleRenderer;
import gov.usgs.plot.SmartTick;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.SCNL;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.heli.HelicorderViewerFrame;
import gov.usgs.swarm.map.MapPanel.LabelSetting;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.SeisanChannel.SimpleChannel;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 *
 * @author Dan Cervelli
 * @author Chirag Patel
 */
public class MapMiniPanel extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{
	private static final Color BACKGROUND_COLOR = new Color(255, 255, 255, 64);
	private static final Color LABEL_BACKGROUND_COLOR = new Color(255, 255, 255, 192);
	private static final long serialVersionUID = 1L;
	public static final Font FONT = Font.decode("dialog-PLAIN-10");
	private int labelHeight = 13;
	private int labelFontSize = 10;
	private int timeFontSize = 10;
	private static final int SIZES[] = new int[] { 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700};
	private int sizeIndex = 3;
	private Metadata activeMetadata;
	private boolean activeMetadataChosen = false;
	private SortedMap<String, Metadata> metadataList;
	private WaveViewPanel wavePanel;
	private boolean waveVisible = false;
	private Line2D.Double line;
	private int labelWidth = 1;
	private JLabel close;
	
	private JPopupMenu popup;
	
	private final static Color NORMAL_BACKGROUND = new Color(255, 255, 255, 128);
	private final static Color MOUSEOVER_BACKGROUND = new Color(128, 255, 128, 128);
	private final static Color WAVE_BACKGROUND = new Color(255, 255, 255, 128);
	private Color titleBackground = NORMAL_BACKGROUND;
	
	private MapPanel parent;
	
	private boolean selected;
	
	public enum Position
	{
		UNSET, AUTOMATIC, MANUAL_SET, MANUAL_UNSET, HIDDEN;
	}
	
	// TODO: choose XY or LL positioning
	private Position position = Position.UNSET;
//	private Point2D.Double manualPosition;
	private Point2D.Double manualPositionXY;
	
	private double[] pendingRequest;
	private boolean working;
	
	public MapMiniPanel(MapPanel p)
	{
		parent = p;
		metadataList = new TreeMap<String, Metadata>();
		setSize(labelWidth, labelHeight);
		setCursor(Cursor.getDefaultCursor());
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		setLayout(null);
	}

	public void saveLayout(ConfigFile cf, String prefix)
	{
		cf.put(prefix + ".sizeIndex", Integer.toString(sizeIndex));
		cf.put(prefix + ".activeChannel", activeMetadata.getChannel());
		int x = -1;
		int y = -1;
		if (position == Position.MANUAL_SET)
		{
			x = (int)manualPositionXY.x;
			y = (int)manualPositionXY.y;
		}
		cf.put(prefix + ".x", Integer.toString(x));
		cf.put(prefix + ".y", Integer.toString(y));
		wavePanel.getSettings().save(cf, prefix + ".settings");
	}
	
	public void processLayout(ConfigFile cf)
	{
		int x = Integer.parseInt(cf.getString("x"));
		int y = Integer.parseInt(cf.getString("y"));
		setManualPosition(new Point2D.Double(x, y));
		position = Position.MANUAL_SET;
		sizeIndex = Integer.parseInt(cf.getString("sizeIndex"));
		setLocation(x, y);
		if (wavePanel == null)
			createWaveViewPanel();
		wavePanel.getSettings().set(cf.getSubConfig("settings"));
		activeMetadata = metadataList.get(cf.getString("activeChannel"));
		if (!waveVisible)
			toggleWave();
		else
			resetWave();
	}
	
	public Metadata getActiveMetadata()
	{
		return activeMetadata;
	}
	
	public void addMetadata(Metadata md)
	{
		metadataList.put(md.getChannel(), md);
//		Collections.sort(metadataList);
//		activeMetadata = metadataList.get(metadataList.size() - 1);
		// TODO: should be intelligently chosen
		if (activeMetadata == null)
			activeMetadata = md;
		if (!activeMetadataChosen)
		{
			SCNL as = activeMetadata.getSCNL();
			SCNL ms = md.getSCNL();
			if (ms.channel.endsWith("Z"))
			{
				if (as.channel.endsWith("Z"))
				{
					if (ms.channel.charAt(0) < as.channel.charAt(0))
					{
						activeMetadata = md;
					}
					else if (ms.channel.charAt(0) == as.channel.charAt(0))
					{
						if (as.location != null && ms.location == null)
							activeMetadata = md;
					}
				}
				else
					activeMetadata = md;
			}
		}
		popup = null;
	}
	
	public Position getPosition()
	{
		return position;
	}

	public void setPosition(Position p)
	{
		position = p;
	}
	
	public void setLine(Line2D.Double l)
	{
		line = l;
	}

	public void setManualPosition(Point2D.Double p)
	{
		manualPositionXY = p;
	}
	
	public Point2D.Double getManualPosition()
	{
		return manualPositionXY;
	}
	
	public boolean isWaveVisible()
	{
		return waveVisible;
	}
	
	public WaveViewPanel getWaveViewPanel()
	{
		if (wavePanel == null)
			createWaveViewPanel();
		
		return wavePanel;
	}
	
	public void changeSize(int ds)
	{
		if (waveVisible)
		{
			sizeIndex += ds;
			if (sizeIndex < 0)
				sizeIndex = 0;
			if (sizeIndex >= SIZES.length)
				sizeIndex = SIZES.length - 1;
			resetWave();
			getParent().repaint();
		}
	}
	
	public void setTitleBackground(Color color)
	{
		titleBackground = color;
		repaint();
	}
	
	private void createWaveViewPanel()
	{
		wavePanel = new WaveViewPanel();
		wavePanel.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						determineSelection(e);
					}
					
					public void mouseEntered(MouseEvent e)
					{
						MapMiniPanel.this.mouseEntered(e);
					}
		
					public void mouseExited(MouseEvent e)
					{
						MapMiniPanel.this.mouseExited(e);
					}
				});
		wavePanel.setUseFilterLabel(false);
		wavePanel.setDisplayTitle(false);
		wavePanel.setDataSource(activeMetadata.source);
		wavePanel.setChannel(SimpleChannel.parse(activeMetadata.getChannel()));
	}
	
	private void createCloseLabel()
	{
		close = new JLabel(Icons.close_view);
		close.setSize(16, 16);
		close.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						if (waveVisible)
							toggleWave();
					}
					
					public void mouseEntered(MouseEvent e)
					{
						MapMiniPanel.this.mouseEntered(e);
					}

					public void mouseExited(MouseEvent e)
					{
						MapMiniPanel.this.mouseExited(e);
					}
				});
	}
	
	private void resetWave()
	{
		if (wavePanel == null)
			createWaveViewPanel();
		
		removeAll();
		int w = SIZES[sizeIndex];
		int h = (int)Math.round((double)w * 80.0 / 300.0);
		setSize(w, h);
		timeFontSize = Math.min(11, Math.max(9, (int)Math.round((double)h / 10.0)));
		labelFontSize = Math.max(10, (int)Math.round((double)h / 8.0));
		labelHeight = labelFontSize + 4;

		wavePanel.setOffsets(13, 0, 0, timeFontSize + 4);
		wavePanel.setSize(w - 1, h - labelHeight);
		wavePanel.setBackgroundColor(WAVE_BACKGROUND);
		wavePanel.setFrameDecorator(new MapWaveDecorator());
		wavePanel.setLocation(0, labelHeight - 1);
		add(wavePanel);
		
		if (close == null)
			createCloseLabel();
		close.setLocation(SIZES[sizeIndex] - 16, -2);
		
		add(close);
		adjustLine();
		updateWave(parent.getStartTime(), parent.getEndTime(), true);
	}
	
	public void changeChannel(Metadata md)
	{
		activeMetadata = md;
		if (wavePanel != null)
		{
			wavePanel.setWave(null, 0, 0);
			updateWave(wavePanel.getStartTime(), wavePanel.getEndTime());
		}
	}
	
	private synchronized boolean isWorking()
	{
		return working;
	}
	
	private synchronized void setWorking(boolean b)
	{
		working = b;
	}
	
	private synchronized void setPendingRequest(double st, double et)
	{
		if (Double.isNaN(st))
			pendingRequest = null;
		else
			pendingRequest = new double[] { st, et };
	}
	
	private synchronized double[] getPendingRequest()
	{
		return pendingRequest;
	}
	
	public boolean updateWave(final double st, final double et)
	{
		return updateWave(st, et, false, false);
	}
	
	public boolean updateWave(final double st, final double et, boolean repaint)
	{
		return updateWave(st, et, false, repaint);
	}
	
	/**
	 * @param st
	 * @param et
	 * @param reenter
	 * @return success
	 */
	public boolean updateWave(final double st, final double et, boolean reenter, final boolean repaint)
	{	
		if (!waveVisible || activeMetadata.source == null)
			return false;
		
		if (isWorking() && !reenter)
		{
			setPendingRequest(st, et);
			return false;
		}
		else
			setWorking(true);
		
		if (reenter)
		{
			setPendingRequest(Double.NaN, Double.NaN);
		}
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						parent.getThrobber().increment();
						wavePanel.setWorking(true);
						wavePanel.setDataSource(activeMetadata.source);
						wavePanel.setChannel(SimpleChannel.parse(activeMetadata.getChannel()));
						Wave cw = wavePanel.getWave();
						// TODO: unify this and the monitor code
						if (cw != null && cw.overlaps(st, et))
						{
							boolean before = activeMetadata.source.isUseCache();
							activeMetadata.source.setUseCache(false);
							if (cw.getEndTime() < et)
							{
								Wave w2 = activeMetadata.source.getWave(activeMetadata.getChannel(), cw.getEndTime() - 10, et);
								if (w2 != null)
									cw = cw.combine(w2);
							}
							if (cw.getStartTime() > st)
							{
								Wave w2 = activeMetadata.source.getWave(activeMetadata.getChannel(), st, cw.getStartTime() + 10);
								if (w2 != null)
									cw = cw.combine(w2);
							}
							cw = cw.subset(st, Math.min(et, cw.getEndTime()));
							activeMetadata.source.setUseCache(before);
						}
						else
							cw = null;
						
						if (cw == null)
							cw = activeMetadata.source.getWave(activeMetadata.getChannel(), st, et);
						
						wavePanel.setWave(cw, st, et);
						return null;
					}
					
					public void finished()
					{
						double[] pr = getPendingRequest();
						if (pr != null)
						{
							repaint();
							updateWave(pr[0], pr[1], true, repaint);
						}
						else
							setWorking(false);
						parent.getThrobber().decrement();
						wavePanel.setWorking(false);
						if (repaint)
							wavePanel.repaint();	
					}
				};
		
		worker.start();
		return true;
	}
	
	public void toggleWave()
	{
		waveVisible = !waveVisible;
		if (waveVisible)
		{
			resetWave();
		}
		else
		{
			parent.deselectPanel(this);
			labelFontSize = 10;
			labelHeight = 13;
			setSize(labelWidth, labelHeight);
			if (parent.getLabelSetting() == LabelSetting.NONE)
				parent.resetImage(false);
		}
		adjustLine();
		getParent().repaint();
//		repaint();
	}
	
	protected void createPopup()
	{
		popup = new JPopupMenu();
		ButtonGroup group = new ButtonGroup();
		for (final Metadata md : metadataList.values())
		{
			JRadioButtonMenuItem rmi = new JRadioButtonMenuItem(md.getChannel());
			rmi.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							changeChannel(md);
							activeMetadataChosen = true;
						}
					});
			group.add(rmi);
			popup.add(rmi);
			if (md == activeMetadata)
				rmi.setSelected(true);
		}
	}
	
	protected void doPopup(MouseEvent e)
	{
		if (popup == null)
			createPopup();
		
		popup.show(e.getComponent(), e.getX(), e.getY());
	}
	
	public String getLabel()
	{
		String label = null;
		if (waveVisible)
		{
			label = activeMetadata.getSCNL().toString();
			if (metadataList.size() > 1)
				label += "+";
		}
		else
			label = activeMetadata.getSCNL().station;
		
		return label;
	}
	
	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.setFont(FONT);
		
		String label = getLabel();
		boolean wave = (waveVisible && wavePanel != null);
		if (!wave)
		{
			if (labelWidth == 1)
			{
				FontMetrics fm = g.getFontMetrics(FONT);
				labelWidth = fm.stringWidth(label) + 5;
			}
			setSize(labelWidth, getHeight());
			adjustLine();
		}
		
		g2.setColor(titleBackground);
		g2.fillRect(0, 0, getWidth() - 1, labelHeight - 1);
		
		super.paint(g);

		Font font = Font.decode("dialog-BOLD-" + labelFontSize);
		g2.setFont(font);
		g2.setColor(Color.BLACK);
		g2.drawString(label, 2, labelFontSize);
		g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
	}

	protected void determineSelection(MouseEvent e)
	{
		if (e.isControlDown())
		{
			if (selected)
				parent.deselectPanel(this);
			else
				parent.addSelectedPanel(this);
		}
		else
			parent.setSelectedPanel(this);
	}
	
	public void mouseClicked(MouseEvent e)
	{
		determineSelection(e);
		setPosition(Position.MANUAL_SET);
		Point pt = getLocation();
		manualPositionXY = new Point2D.Double(pt.x, pt.y);
		if (e.getClickCount() == 2)
		{
			if (activeMetadata.source != null)
			{
				HelicorderViewerFrame hvf = Swarm.getApplication().openHelicorder(activeMetadata.source, activeMetadata.getChannel(), Double.NaN);
				if (Swarm.getApplication().isFullScreenMode())
					hvf.setPinned(true);
			}
		}
		else if (!waveVisible)
			toggleWave();
	}

	public void setSelected(boolean b)
	{
		selected = b;
		if (selected)
			setTitleBackground(MOUSEOVER_BACKGROUND);
		else
			setTitleBackground(NORMAL_BACKGROUND);
	}
	
	public void mouseEntered(MouseEvent e)
	{
		parent.setStatusText(activeMetadata.getSCNL().station + ": " +
				Util.lonLatToString(activeMetadata.getLonLat()));
//		setTitleBackground(MOUSEOVER_BACKGROUND);
//		parent.setSelectedPanel(this);
	}

	public void mouseExited(MouseEvent e)
	{
//		setTitleBackground(NORMAL_BACKGROUND);
//		parent.setSelectedPanel(null);
	}

	private int startX;
	private int startY;
	private int deltaX;
	private int deltaY;
	
	public void mousePressed(MouseEvent e)
	{
		parent.requestFocusInWindow();
		if (e.isPopupTrigger())
			doPopup(e);
		Point p = getLocation();
		startX = p.x;
		startY = p.y;
		deltaX = e.getX();
		deltaY = e.getY();
	}

	public void mouseReleased(MouseEvent e)
	{
		if (e.isPopupTrigger())
			doPopup(e);
	}

	public void adjustLine()
	{
		Point p = getLocation();
		Dimension d = getSize();
		Line2D.Double[] lines = new Line2D.Double[8];
		lines[0] = new Line2D.Double(p.x, p.y, line.x2, line.y2);
		lines[1] = new Line2D.Double(p.x + d.width - 1, p.y, line.x2, line.y2);
		lines[2] = new Line2D.Double(p.x + d.width - 1, p.y + d.height, line.x2, line.y2);
		lines[3] = new Line2D.Double(p.x, p.y + d.height, line.x2, line.y2);
		lines[4] = new Line2D.Double(p.x + d.width / 2, p.y + d.height, line.x2, line.y2);
		lines[5] = new Line2D.Double(p.x + d.width / 2, p.y, line.x2, line.y2);
		lines[6] = new Line2D.Double(p.x + d.width - 1, p.y + d.height / 2, line.x2, line.y2);
		lines[7] = new Line2D.Double(p.x, p.y + d.height / 2, line.x2, line.y2);
		double min = Double.MAX_VALUE;
		Line2D.Double shortest = null;
		for (int i = 0; i < lines.length; i++)
		{
			Line2D.Double l = lines[i];
			double len = (l.x1 - l.x2) * (l.x1 - l.x2) + (l.y1 - l.y2) * (l.y1 - l.y2);
			if (len < min)
			{
				min = len;
				shortest = l;
			}
		}
		line.setLine(shortest);
	}
	
	public void mouseDragged(MouseEvent e)
	{
		setPosition(Position.MANUAL_SET);
		setLocation(startX + e.getX() - deltaX, startY + e.getY() - deltaY);
		Point p = getLocation();
		startX = p.x;
		startY = p.y;
//		manualPosition = parent.getLonLat(p.x, p.y);
		manualPositionXY = new Point2D.Double(p.x, p.y);
		adjustLine();
		getParent().repaint();
	}

	public void mouseMoved(MouseEvent e)
	{
		
	}
	
	// TODO: could be singleton to reduce garbage
	private class MapWaveDecorator extends FrameDecorator
	{
		private Font font;
		private FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
		
		public MapWaveDecorator()
		{}

		private void createAxis(FrameRenderer fr)
		{
			fr.createEmptyAxis();
			AxisRenderer ar = fr.getAxis();
			ar.createDefault();
			ar.setBackgroundColor(BACKGROUND_COLOR);
			if (wavePanel.getSettings().filterOn)
			{
				TextRenderer tr = wavePanel.getFilterLabel();
				tr.horizJustification = TextRenderer.RIGHT;
				tr.x = fr.getGraphWidth() + fr.getGraphX();
				tr.y = fr.getGraphY() + 11;
				tr.color = Color.RED;
				ar.addPostRenderer(tr);
			}
		}
		
		private void setTimeAxis(FrameRenderer fr)
		{
			int hTicks = 6;
			Object[] stt = SmartTick.autoTimeTick(fr.getMinXAxis(), fr.getMaxXAxis(), hTicks);
	        if (stt != null)
	        	fr.getAxis().createVerticalGridLines((double[])stt[0]);
	        
	        double[] bt = (double[])stt[0];
	        String[] labels = (String[])stt[1];
	        for (int i = 0; i < bt.length; i++)
	        {
	            TextRenderer tr = new TextRenderer();
                tr.text = labels[i];
	            tr.x = (float)fr.getXPixel(bt[i]);
	            tr.y = fr.getGraphY() + fr.getGraphHeight() + timeFontSize + 2;
	            tr.color = Color.BLACK;
	            tr.horizJustification = TextRenderer.CENTER;
	            tr.font = font;
	            fr.getAxis().addPostRenderer(tr);
	        }
		}
		
		/*
		private void setLinearAxis(FrameRenderer fr, boolean log)
		{
			int hTicks = 6;
			double[] stt = SmartTick.autoTick(fr.getMinXAxis(), fr.getMaxXAxis(), hTicks, false, false);
	        if (stt != null)
	        	fr.getAxis().createVerticalGridLines(stt);
	        
	        for (int i = 0; i < stt.length; i++)
	        {
	            TextRenderer tr = new TextRenderer();
	            double val = stt[i];
	            if (log)
	            	val = Math.pow(10, val);
                tr.text = String.format("%.0f", val);
	            tr.x = (float)fr.getXPixel(stt[i]);
	            if (tr.x >= getWidth() - 3)
	            	tr.x -= 5;
	            tr.y = fr.getGraphY() + fr.getGraphHeight() + timeFontSize + 2;
	            tr.color = Color.BLACK;
	            tr.horizJustification = TextRenderer.CENTER;
	            tr.font = font;
	            fr.getAxis().addPostRenderer(tr);
	        }
		}
		*/
		
		private void setLeftLabel(FrameRenderer fr, String label)
		{
			TextRenderer tr = new TextRenderer();
	        tr.color = Color.BLACK;
	        tr.text = label;
	        tr.x = 4;
	        tr.y = fr.getGraphY() + fr.getGraphHeight() / 2 + 8;
	        tr.horizJustification = TextRenderer.CENTER;
	        tr.orientation = -90.0f;
	        fr.getAxis().addPostRenderer(tr);
		}

		private void setMinMaxBoxes(FrameRenderer fr, double min, double max)
		{
			AxisRenderer ar = fr.getAxis();
			TextRenderer ultr = new TextRenderer();
	        ultr.color = Color.BLACK;
	        
	        ultr.text = String.format("%.0f", max);
	        ultr.horizJustification = TextRenderer.RIGHT;
	        ultr.y = fr.getGraphY() + timeFontSize;
	        ultr.font = font;
	        
	        TextRenderer lltr = new TextRenderer();
	        lltr.color = Color.BLACK;
	        lltr.text = String.format("%.0f", min);
	        lltr.horizJustification = TextRenderer.RIGHT;
	        lltr.y = fr.getGraphY() + fr.getGraphHeight() - 1;
	        lltr.font = font;

	        int w = (int)Math.round(Math.max(font.getStringBounds(ultr.text, frc).getWidth(), font.getStringBounds(lltr.text, frc).getWidth())) + 3;
	        ultr.backgroundWidth = w;
	        lltr.backgroundWidth = w;
	        ultr.x = fr.getGraphX() + w;
	        lltr.x = fr.getGraphX() + w;
	        
	        RectangleRenderer ulrr = new RectangleRenderer();
	        ulrr.rect = new Rectangle2D.Double(fr.getGraphX(), fr.getGraphY(), w + 1, timeFontSize + 2);
	        ulrr.color = Color.BLACK;
	        ulrr.backgroundColor = LABEL_BACKGROUND_COLOR;
	        RectangleRenderer llrr = new RectangleRenderer();
	        llrr.rect = new Rectangle2D.Double(fr.getGraphX(), fr.getGraphY() + fr.getGraphHeight() - timeFontSize - 2, w + 1, timeFontSize + 2);
	        llrr.color = Color.BLACK;
	        llrr.backgroundColor = LABEL_BACKGROUND_COLOR;
	        ar.addPostRenderer(ulrr);
	        ar.addPostRenderer(llrr);
	        ar.addPostRenderer(ultr);
	        ar.addPostRenderer(lltr);
		}
		
		public void decorateWave(FrameRenderer fr)
		{
			createAxis(fr);
			setTimeAxis(fr);
	        String label = "Counts";
	        if (activeMetadata.getUnit() != null)
				label = activeMetadata.getUnit();
	        setLeftLabel(fr, label);
	        double m = activeMetadata.getMultiplier();
	        double b = activeMetadata.getOffset();
	        setMinMaxBoxes(fr, fr.getMinY() * m + b, fr.getMaxY() * m + b);
		}
		
		public void decorateSpectra(FrameRenderer fr)
		{
			createAxis(fr);
//			setLinearAxis(fr, wavePanel.getSettings().logFreq);
			
			if (wavePanel.getSettings().logPower)
				setLeftLabel(fr, "log(P)");
			else
				setLeftLabel(fr, "Power");
	        setMinMaxBoxes(fr, fr.getMinY(), fr.getMaxY());
		}
		
		public void decorateSpectrogram(FrameRenderer fr)
		{
			createAxis(fr);
			setTimeAxis(fr);
	        String label = "Freq";
	        setLeftLabel(fr, label);
	        setMinMaxBoxes(fr, fr.getMinY(), fr.getMaxY());
		}
		
		public void decorate(FrameRenderer fr)
		{
			font = Font.decode("dialog-PLAIN-" + timeFontSize);
			switch (wavePanel.getSettings().viewType)
			{
				case WAVE:
					decorateWave(fr);
					break;
				case SPECTRA:
					decorateSpectra(fr);
					break;
				case SPECTROGRAM:
					decorateSpectrogram(fr);
					break;
			}
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int cnt = -e.getWheelRotation();
		changeSize(cnt);
	}
	
}