package gov.usgs.swarm.wave;

import gov.usgs.plot.AxisRenderer;
import gov.usgs.plot.FrameDecorator;
import gov.usgs.plot.FrameRenderer;
import gov.usgs.plot.SmartTick;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * MultiMonitor is a window that is used to display multiple seismic
 * channels in real-time.
 * 
 * TODO: save monitor size
 * TODO: clipboard
 * TODO: up/down arrows
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.12  2006/07/25 05:15:36  cervelli
 * Change for FrameDecorator being class instead of interface.
 *
 * Revision 1.11  2006/07/22 20:28:39  cervelli
 * Added up/down to move selection, delete to remove selection.
 *
 * Revision 1.10  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.9  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.8  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.7  2006/04/13 16:41:33  dcervelli
 * Fixed some bugs with the low bandwidth monitor.
 *
 * Revision 1.6  2006/04/03 05:15:53  dcervelli
 * Reduced bandwidth monitor mode.
 *
 * Revision 1.5  2005/09/02 16:40:17  dcervelli
 * CurrentTime changes.
 *
 * Revision 1.4  2005/08/30 00:34:13  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:30:15  tparker
 * Tidy code, no functional changes.
 *
 * Revision 1.2  2005/08/27 00:15:08  tparker
 * Create image constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.6  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.5  2005/04/15 04:36:11  cervelli
 * More JDK 1.5 deprecation stuff.
 *
 * Revision 1.4  2005/04/11 00:22:58  cervelli
 * Removed JDK 1.5 deprecated Dialog.show().
 *
 * Revision 1.3  2004/10/28 20:24:12  cvs
 * Settings button, waves in Box.
 *
 * Revision 1.2  2004/10/12 23:43:15  cvs
 * Added log info and some comments.
 *
 * @author Dan Cervelli
 */
public class MultiMonitor extends JInternalFrame implements Runnable
{
	public static final long serialVersionUID = -1;
	
	public final static int[] SPANS = new int[] {15, 30, 60, 120, 180, 240, 300, 600, 15*60, 20*60, 30*60, 60*60};
	private int spanIndex = 1;
	private List<String> channels;
	private List<WaveViewPanel> panels;
	private SeismicDataSource dataSource;

	private JToolBar toolbar;
	private JButton settingsButton;
	private JButton removeButton;
	private JPanel mainPanel;
	private JButton compXButton;
	private JButton expXButton;
	private JButton optionsButton;
	
	private Box waveBox;
	
	private long refreshInterval = 1000;
	
	private Thread refreshThread;
	
	private int selectedIndex = -1;
	
	private static final Color SELECT_COLOR = new Color(204, 204, 255);
	private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);	
	
	private Throbber throbber;
	
	public MultiMonitor(SeismicDataSource sds)
	{
		super("Monitor, [" + sds.getName() + "]", true, true, true, true);
		this.setFrameIcon(new ImageIcon(getClass().getClassLoader().getResource(Images.get("monitor"))));
		dataSource = sds;
		channels = new ArrayList<String>();
		panels = new ArrayList<WaveViewPanel>();
		waveBox = new Box(BoxLayout.Y_AXIS);
//		waveBox.setBackground(Color.RED);
		createUI();
		refreshThread = new Thread(this);
		refreshThread.start();
	}
	
	public SeismicDataSource getDataSource()
	{
		return dataSource;
	}
	
	public void setDataSource(SeismicDataSource sds)
	{
		dataSource = sds;
	}
	
	public long getRefreshInterval()
	{
		return refreshInterval;
	}
	
	public int getSpan()
	{
		return SPANS[spanIndex];
	}
	
	public void setRefreshInterval(long ms)
	{
		refreshInterval = ms;
	}
	
	public void setSpan(int span)
	{
		for (int i = 0; i < SPANS.length; i++)
		{
			if (SPANS[i] == span)
			{
				spanIndex = i;
				break;
			}
		}
	}
	
	protected void createUI()
	{
		this.setSize(600, 700);
		this.setLocation(100, 0);
		mainPanel = new JPanel(new BorderLayout());
//		mainPanel.setBackground(Color.RED);
		Border border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 3), 
				LineBorder.createGrayLineBorder());
//		mainPanel.setBorder(border);
		
		toolbar = SwarmUtil.createToolBar();

		optionsButton = SwarmUtil.createToolBarButton(
				Images.getIcon("settings"),
				"Monitor options",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						MultiMonitorSettingsDialog mmsd = MultiMonitorSettingsDialog.getInstance(MultiMonitor.this);
						mmsd.setVisible(true);
					}
				});
		toolbar.add(optionsButton);
		
		toolbar.addSeparator();
		
		compXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xminus"),
				"Shrink time axis",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex != 0)
							spanIndex--;
					}
				});
		toolbar.add(compXButton);
		
		expXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xplus"),
				"Expand time axis",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex < SPANS.length - 1)
							spanIndex++;
					}
				});
		toolbar.add(expXButton);
		
		toolbar.addSeparator();
		
		settingsButton = SwarmUtil.createToolBarButton(
				Images.getIcon("wavesettings"),
				"Settings for selected wave",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selectedIndex >= 0)
						{
							WaveViewPanel panel = panels.get(selectedIndex);
							WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(panel.getSettings());
							wvsd.setVisible(true);
						}
					}
				});
		toolbar.add(settingsButton);
		
		toolbar.addSeparator();
		
		removeButton = SwarmUtil.createToolBarButton(
				Images.getIcon("delete"),
				"Remove selected wave from monitor",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selectedIndex >= 0)
						{
							removeWaveAtIndex(selectedIndex);
							if (channels.size() == 0)
								selectedIndex = -1;
							else if (channels.size() == selectedIndex)
								selectedIndex--;
							if (selectedIndex != -1)
								select(panels.get(selectedIndex));
						}
					}
				});
		Util.mapKeyStrokeToButton(this, "DELETE", "delete", removeButton);
		toolbar.add(removeButton);
		
		Util.mapKeyStrokeToAction(this, "UP", "up", new AbstractAction()
				{
					private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent e)
					{
						if (selectedIndex > 0)
						{
							deselect();
							selectedIndex--;
							select(panels.get(selectedIndex));
						}
					}
				});
		
		Util.mapKeyStrokeToAction(this, "DOWN", "down", new AbstractAction()
				{
					private static final long serialVersionUID = 1L;
					public void actionPerformed(ActionEvent e)
					{
						if (selectedIndex < panels.size() - 1)
						{
							deselect();
							selectedIndex++;
							select(panels.get(selectedIndex));
						}
					}
				});
		
		toolbar.add(Box.createHorizontalGlue());
		
		throbber = new Throbber();
		toolbar.add(throbber);
		
		mainPanel.add(toolbar, BorderLayout.NORTH);
		mainPanel.add(waveBox, BorderLayout.CENTER);
		
//		waveBox.setBorder(LineBorder.createGrayLineBorder());
		border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 3, 3), 
				LineBorder.createGrayLineBorder());
		waveBox.setBorder(border);

		createListeners();
		this.setContentPane(mainPanel);
//		this.setVisible(true);
	}
	
	private void createListeners()
	{
		this.addComponentListener(new ComponentAdapter()
				{
					public void componentMoved(ComponentEvent e)
					{
						resizeWaves();
					}
					
					public void componentResized(ComponentEvent e)
					{
						resizeWaves();
					}
				});
		
		this.addInternalFrameListener(new InternalFrameAdapter()
			  {
					public void internalFrameActivated(InternalFrameEvent e)
					{
						if (selectedIndex != -1)
						{
							String ch = panels.get(selectedIndex).getChannel();
							Swarm.getApplication().getDataChooser().setNearest(ch);
						}
					}
					
					public void internalFrameOpened(InternalFrameEvent e)
					{
						resizeWaves();
					}
					
					public void internalFrameClosing(InternalFrameEvent e)
					{
						selectedIndex = -1;
						dataSource.close();
						panels.clear();
						channels.clear();
						waveBox.removeAll();
//						Swarm.getApplication().removeInternalFrame(MultiMonitor.this);
						Swarm.getApplication().removeMonitor(MultiMonitor.this);
//						MultiMonitor.this.setVisible(true);
					}
			  });

		this.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
	}
	
	private class MonitorWaveDecorator extends FrameDecorator
	{
		private WaveViewPanel panel;
		
		public MonitorWaveDecorator(WaveViewPanel wvp)
		{
			panel = wvp;
		}
		
		public void decorate(FrameRenderer fr)
		{
			fr.createEmptyAxis();
			AxisRenderer ar = fr.getAxis();
			ar.createDefault();
			ar.setBackgroundColor(Color.WHITE);
			if (selectedIndex != -1 && panels.get(selectedIndex) == panel)
				ar.setBackgroundColor(SELECT_COLOR);
				
			TextRenderer label = new TextRenderer(fr.getGraphX() + 2, fr.getGraphY() + 12, panel.getChannel(), Color.BLACK);
			label.backgroundColor = new Color(255, 255, 255, 210);
			
			int hTicks = fr.getGraphWidth() / 108;
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
	            tr.y = fr.getGraphY() + fr.getGraphHeight() - 10;
	            tr.color = Color.BLACK;
	            tr.horizJustification = TextRenderer.CENTER;
	            tr.vertJustification = TextRenderer.TOP;
	            tr.font = TextRenderer.SMALL_FONT;
	            ar.addPostRenderer(tr);
	        }
	        
	        ar.addPostRenderer(label);
		}
	}
	
	public synchronized void addChannel(String ch)
	{
		channels.add(ch);
		final WaveViewPanel panel = new WaveViewPanel();
		panel.setOffsets(-1, 0, 0, 0);
		panel.setWorking(true);
		panel.setDisplayTitle(true);
		panel.setFrameDecorator(new MonitorWaveDecorator(panel));
		panels.add(panel);
		waveBox.add(panel);
		panel.addListener(new WaveViewPanelAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						requestFocusInWindow();
						select(panel);
					}
				});
		resizeWaves();
	}
	
	public void deselect()
	{
		if (selectedIndex >= 0)
		{
			WaveViewPanel panel = panels.get(selectedIndex);
			panel.setBackgroundColor(BACKGROUND_COLOR);
			panel.invalidateImage();
			panel.repaint();
		}
	}
	
	public void select(WaveViewPanel p)
	{
		deselect();
		for (int i = 0; i < panels.size(); i++)
		{
			WaveViewPanel panel = panels.get(i);
			if (panel == p)
			{
				selectedIndex = i;
				Swarm.getApplication().getDataChooser().setNearest(panel.getChannel());
				panel.setBackgroundColor(SELECT_COLOR);
				panel.invalidateImage();
				panel.repaint();
				break;
			}
		}
		repaint();
	}
	
	public void removeWaveAtIndex(int i)
	{
		WaveViewPanel wvp = panels.get(i);
		channels.remove(i);
		panels.remove(i);
		waveBox.remove(wvp);
		resizeWaves();
	}
	
	private void resizeWaves()
	{
		if (panels.size () == 0 || waveBox == null)
		{
			repaint();
			return;
		}
		int ah = waveBox.getHeight() - 5;
		double dy = ((double)ah / (double)panels.size());
		int wh = (int)Math.round(dy);
		int th = wh * panels.size();
		int dh = th - ah;
		for (int i = 0; i < panels.size(); i++)
		{
			WaveViewPanel wvp = panels.get(i);
			int awh = wh;
			if (dh < 0)
			{
				awh++;
				dh++;
			}
			else if (dh > 0)
			{
				awh--;
				dh--;
			}
			wvp.setSize(this.getWidth(), awh);
		}
		waveBox.validate();
		repaint();
	}
	
	public synchronized void refresh()
	{
		System.out.println("refresh start");
		throbber.increment();
		CachedDataSource cache = Swarm.getCache();
		double now = CurrentTime.getInstance().nowJ2K();
		double start = now - SPANS[spanIndex];
		for (int i = 0; i < channels.size(); i++)
		{
			WaveViewPanel waveViewPanel = panels.get(i);
			String channel = channels.get(i);
			CodeTimer ct = new CodeTimer("getWave");
			Wave sw = cache.getBestWave(channel, start, now);
			if (sw != null) 
			{
				if (sw.getEndTime() < now)
				{
					Wave w2 = dataSource.getWave(channel, sw.getEndTime() - 10, now);
					if (w2 != null)
						sw = sw.combine(w2);
				}
				if (sw.getStartTime() > start)
				{
					Wave w2 = dataSource.getWave(channel, start, sw.getStartTime() + 10);
					if (w2 != null)
						sw = sw.combine(w2);
				}
			}
//			Wave sw = null;
			// something bad happened above, just get the whole wave
			if (sw == null)
				sw = dataSource.getWave(channel, start, now);
			ct.stop();
			waveViewPanel.setWorking(true);
			waveViewPanel.setWave(sw, start, now);
			waveViewPanel.setChannel(channel);
			waveViewPanel.setDataSource(dataSource);
			waveViewPanel.setWorking(false);
		}
		if (!this.isVisible())
			dataSource.close();
		throbber.decrement();
		System.out.println("refresh stop");
	}
	
	public void run()
	{
		boolean firstResize = false;
		while (true)
		{
			try
			{
				if (!firstResize)
				{
					if (waveBox.getHeight() != 0)
					{
						resizeWaves();
						firstResize = true;
					}
				}
				if (this.isVisible())
				{
					refresh();
					for (int i = 0; i < channels.size(); i++)
					{
						WaveViewPanel waveViewPanel = panels.get(i);
						waveViewPanel.repaint();
					}
				}
				
				Thread.sleep(refreshInterval);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		if (channels.size() == 0)
		{
			Dimension dim = this.getSize();
			g.setColor(Color.black);
			g.drawString("Monitor empty.", dim.width / 2 - 40, dim.height / 2);	
		}
	}
}
