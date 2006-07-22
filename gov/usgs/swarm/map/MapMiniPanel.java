package gov.usgs.swarm.map;

import gov.usgs.plot.AxisRenderer;
import gov.usgs.plot.FrameDecorator;
import gov.usgs.plot.FrameRenderer;
import gov.usgs.plot.SmartTick;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.WaveViewPanel;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.Color;
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
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * 
 * @author Dan Cervelli
 */
public class MapMiniPanel extends JComponent implements MouseListener, MouseMotionListener, MouseWheelListener
{
	private static final long serialVersionUID = 1L;
	public static final Font FONT = Font.decode("dialog-PLAIN-10");
	public static final int LABEL_HEIGHT = 13;
	private static final int SIZES[] = new int[] { 100, 150, 200, 250, 300, 350, 400, 450, 500};
	private int sizeIndex = 3;
	private Metadata activeMetadata;
	private List<Metadata> metadataList;
	private WaveViewPanel wavePanel;
	private boolean waveVisible = false;
	private Line2D.Double line;
	private int labelWidth = 1;
	
	private JPopupMenu popup;
	
	private final static Color NORMAL_BACKGROUND = new Color(255, 255, 255, 128);
	private final static Color MOUSEOVER_BACKGROUND = new Color(128, 255, 128, 128);
	private Color titleBackground = NORMAL_BACKGROUND;
	
	public MapMiniPanel()
	{
		metadataList = new ArrayList<Metadata>();
		setSize(labelWidth, LABEL_HEIGHT);
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		setLayout(null);
	}

	public void addMetadata(Metadata md)
	{
		metadataList.add(md);
		Collections.sort(metadataList);
		activeMetadata = metadataList.get(metadataList.size() - 1);
		popup = null;
	}
	
	public void setLine(Line2D.Double l)
	{
		line = l;
	}

	public boolean isWaveVisible()
	{
		return waveVisible;
	}
	
	public WaveViewPanel getWaveViewPanel()
	{
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
	
	private void resetWave()
	{
		if (wavePanel == null)
			wavePanel = new WaveViewPanel();
		removeAll();
		int w = SIZES[sizeIndex];
		int h = (int)Math.round(w * 80 / 300);
		setSize(w, h);

		//		SeismicDataSource sds = Swarm.config.getSource("pubavo1");
//		double now = CurrentTime.getInstance().nowJ2K();
//		Wave w = sds.getWave(channel, now - 600, now);
//		wavePanel.setWave(wave, wave.getEndTime(), wave.getStartTime());
		wavePanel.setOffsets(0, 0, 0, 0);
		wavePanel.setSize(w - 1, h - 13);
		wavePanel.setBackgroundColor(new Color(255, 255, 255, 128));
		wavePanel.setFrameDecorator(new MapWaveDecorator(wavePanel));
		wavePanel.setLocation(0, LABEL_HEIGHT - 1);
		add(wavePanel);
		
		JLabel close = new JLabel(Images.getIcon("close_view"));
		close.setSize(16, 16);
		close.setLocation(w - 16, -2);
		close.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						if (waveVisible)
							toggleWave();
					}
					
					public void mouseEntered(MouseEvent e)
					{
						setTitleBackground(MOUSEOVER_BACKGROUND);
					}

					public void mouseExited(MouseEvent e)
					{
						setTitleBackground(NORMAL_BACKGROUND);
					}
				});
		add(close);
		adjustLine();
	}
	
	public void changeChannel(Metadata md)
	{
		activeMetadata = md;
		updateWave(wavePanel.getStartTime(), wavePanel.getEndTime());
	}
	
	public void updateWave(double st, double et)
	{			
		if (!waveVisible || activeMetadata.source == null)
			return;
		
		Wave sw = activeMetadata.source.getWave(activeMetadata.channel, st, et);
		wavePanel.setWorking(true);
		wavePanel.setWave(sw, st, et);
		wavePanel.setWorking(false);
		repaint();
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
			JRadioButtonMenuItem rmi = new JRadioButtonMenuItem(md.channel);
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
		popup.addSeparator();
		JMenuItem mi = new JMenuItem("Open Helicorder");
		popup.add(mi);
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
			label = activeMetadata.scnl.toString();
			if (metadataList.size() > 1)
				label += "+";
		}
		else
			label = activeMetadata.scnl.station;
		
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

	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			if (activeMetadata.source != null)
			{
				Swarm.getApplication().openHelicorder(activeMetadata.source, activeMetadata.channel);
			}
		}
		else if (!waveVisible)
			toggleWave();
	}

	public void mouseEntered(MouseEvent e)
	{
		setTitleBackground(MOUSEOVER_BACKGROUND);
	}

	public void mouseExited(MouseEvent e)
	{
		setTitleBackground(NORMAL_BACKGROUND);
	}

	private int startX;
	private int startY;
	private int deltaX;
	private int deltaY;
	
	public void mousePressed(MouseEvent e)
	{
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
		setLocation(startX + e.getX() - deltaX, startY + e.getY() - deltaY);
		Point p = getLocation();
		startX = p.x;
		startY = p.y;
		adjustLine();
		getParent().repaint();
	}

	public void mouseMoved(MouseEvent e)
	{
		
	}
	
	private class MapWaveDecorator implements FrameDecorator
	{
		private WaveViewPanel panel;
		
		public MapWaveDecorator(WaveViewPanel wvp)
		{
			panel = wvp;
		}
		
		public void decorate(FrameRenderer fr)
		{
			fr.createEmptyAxis();
			AxisRenderer ar = fr.getAxis();
			ar.createDefault();
			ar.setBackgroundColor(new Color(255, 255, 255, 64));
			
			TextRenderer label = new TextRenderer(fr.getGraphX() + 4, fr.getGraphY() + 14, panel.getChannel(), Color.BLACK);
			label.backgroundColor = Color.WHITE;
			
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
	        
	        ar.addPostRenderer(label);
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int cnt = -e.getWheelRotation();
		changeSize(cnt);
	}
	
}