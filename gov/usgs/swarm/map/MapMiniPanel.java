package gov.usgs.swarm.map;

import gov.usgs.plot.AxisRenderer;
import gov.usgs.plot.FrameDecorator;
import gov.usgs.plot.FrameRenderer;
import gov.usgs.plot.SmartTick;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.util.Util;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2006/07/26 22:41:00  cervelli
 * Bunch more development for 2.0.
 *
 * Revision 1.3  2006/07/26 00:39:36  cervelli
 * New resetImage() behavior.
 *
 * @author Dan Cervelli
 */
public class MapMiniPanel extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{
	private static final long serialVersionUID = 1L;
	public static final Font FONT = Font.decode("dialog-PLAIN-10");
	public static final int LABEL_HEIGHT = 13;
	private static final int SIZES[] = new int[] { 100, 150, 200, 250, 300, 350, 400, 450, 500, 550};
	private int sizeIndex = 4;
	private Metadata activeMetadata;
	private SortedSet<Metadata> metadataList;
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
	
	public MapMiniPanel(MapPanel p)
	{
		parent = p;
		metadataList = new TreeSet<Metadata>();
		setSize(labelWidth, LABEL_HEIGHT);
		setCursor(Cursor.getDefaultCursor());
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		addKeyListener(new KeyListener()
		{
			public void keyPressed(KeyEvent e)
			{
				System.out.println("keypressed MMP");
				if (Character.toLowerCase(e.getKeyChar()) == 'r')
				{
					getWaveViewPanel().getSettings().resetAutoScaleMemory();
				}
			}

			public void keyReleased(KeyEvent e)
			{
			}

			public void keyTyped(KeyEvent e)
			{
			}
		});
		setLayout(null);
	}

	public Metadata getActiveMetadata()
	{
		return activeMetadata;
	}
	
	public void addMetadata(Metadata md)
	{
		metadataList.add(md);
//		Collections.sort(metadataList);
//		activeMetadata = metadataList.get(metadataList.size() - 1);
		// TODO: should be intelligently chosen
		if (activeMetadata == null)
			activeMetadata = md;
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
		wavePanel.setDisplayTitle(false);
		wavePanel.setDataSource(activeMetadata.source);
		wavePanel.setChannel(activeMetadata.getChannel());
	}
	
	private void createCloseLabel()
	{
		close = new JLabel(Images.getIcon("close_view"));
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
		int h = (int)Math.round(w * 80 / 300);
		setSize(w, h);

		wavePanel.setOffsets(0, 0, 0, 0);
		wavePanel.setSize(w - 1, h - 13);
		wavePanel.setBackgroundColor(WAVE_BACKGROUND);
//		wavePanel.setFrameDecorator(new MapWaveDecorator(wavePanel));
		wavePanel.setFrameDecorator(new MapWaveDecorator());
		wavePanel.setLocation(0, LABEL_HEIGHT - 1);
		add(wavePanel);
		
		if (close == null)
			createCloseLabel();
		close.setLocation(SIZES[sizeIndex] - 16, -2);
		
		add(close);
		adjustLine();
		updateWave(parent.getStartTime(), parent.getEndTime());
	}
	
	public void changeChannel(Metadata md)
	{
		activeMetadata = md;
		if (wavePanel != null)
			updateWave(wavePanel.getStartTime(), wavePanel.getEndTime());
	}
	
	public void updateWave(final double st, final double et)
	{			
		if (!waveVisible || activeMetadata.source == null)
			return;
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						parent.getThrobber().increment();
						wavePanel.setWorking(true);
						wavePanel.setDataSource(activeMetadata.source);
						wavePanel.setChannel(activeMetadata.getChannel());
						Wave sw = activeMetadata.source.getWave(activeMetadata.getChannel(), st, et);
						wavePanel.setWave(sw, st, et);
						return null;
					}
					
					public void finished()
					{
						parent.getThrobber().decrement();
						wavePanel.setWorking(false);
						repaint();	
					}
				};
		
		worker.start();
	}
	
	public void toggleWave()
	{
		waveVisible = !waveVisible;
		if (waveVisible)
		{
			resetWave();
//			setSize(320, 420);
//			
//			SeismicDataSource sds = Swarm.config.getSource("pubavo1");
//			double now = CurrentTime.getInstance().nowJ2K();
//			double then = now - 6 * 60 * 60;
//			HelicorderData hd = sds.getHelicorder(channel, then, now);
//			System.out.println("rows: " + hd.rows());
//			heliRenderer = new HelicorderRenderer(hd, 15 * 60);
//			heliRenderer.setLocation(30, 15, 280, 380);
//			double mean = hd.getMeanMax();
//			double bias = hd.getBias();
//			mean = Math.abs(bias - mean);
//
//			double clipValue = (int)(21 * mean);
//			double barRange = (int)(3 * mean);
//			heliRenderer.setChannel(channel);
//			heliRenderer.setHelicorderExtents(then, now, -1 * Math.abs(barRange), Math.abs(barRange));
//			heliRenderer.setClipValue((int)clipValue);
//			heliRenderer.setShowClip(true);
//			heliRenderer.createMinimumAxis();
		}
		else
		{
			parent.deselectPanel(this);
			setSize(labelWidth, LABEL_HEIGHT);
		}
		adjustLine();
		getParent().repaint();
//		repaint();
	}
	
	protected void createPopup()
	{
		popup = new JPopupMenu();
		ButtonGroup group = new ButtonGroup();
		for (final Metadata md : metadataList)
		{
			JRadioButtonMenuItem rmi = new JRadioButtonMenuItem(md.getChannel());
			rmi.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							changeChannel(md);
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
		g2.fillRect(0, 0, getWidth() - 1, LABEL_HEIGHT - 1);
		
		super.paint(g);

		g2.setFont(FONT);
		g2.setColor(Color.BLACK);
		g2.drawString(label, 2, 10);
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
				Swarm.getApplication().openHelicorder(activeMetadata.source, activeMetadata.getChannel());
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
//		private WaveViewPanel panel;
		
		public MapWaveDecorator()
		{}
		
//		public MapWaveDecorator(WaveViewPanel wvp)
//		{
//			panel = wvp;
//		}
		
		public void decorate(FrameRenderer fr)
		{
			fr.createEmptyAxis();
			AxisRenderer ar = fr.getAxis();
			ar.createDefault();
			ar.setBackgroundColor(new Color(255, 255, 255, 64));
			
//			TextRenderer label = new TextRenderer(fr.getGraphX() + 4, fr.getGraphY() + 14, panel.getChannel(), Color.BLACK);
//			label.backgroundColor = Color.WHITE;
			
			int hTicks = fr.getGraphWidth() / 54;
			Object[] stt = SmartTick.autoTimeTick(fr.getMinXAxis(), fr.getMaxXAxis(), hTicks);
	        if (stt != null)
	        	ar.createVerticalGridLines((double[])stt[0]);
	        
	        double[] bt = (double[])stt[0];
	        String[] labels = (String[])stt[1];
	        for (int i = 0; i < bt.length; i++)
	        {
	            TextRenderer tr = new TextRenderer();
                tr.text = labels[i];
	            tr.x = (float)fr.getXPixel(bt[i]);
	            tr.y = fr.getGraphY() + fr.getGraphHeight() - 10 ;
	            tr.color = Color.BLACK;
	            tr.horizJustification = TextRenderer.CENTER;
	            tr.vertJustification = TextRenderer.TOP;
	            tr.font = TextRenderer.SMALL_FONT;
	            ar.addPostRenderer(tr);
	        }
	        
//	        ar.addPostRenderer(label);
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int cnt = -e.getWheelRotation();
		changeSize(cnt);
	}
	
}