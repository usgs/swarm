package gov.usgs.swarm.wave;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.decorate.FrameDecorator;
import gov.usgs.plot.decorate.SmartTick;
import gov.usgs.plot.render.AxisRenderer;
import gov.usgs.plot.render.FrameRenderer;
import gov.usgs.plot.render.RectangleRenderer;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Kioskable;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmFrame;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.wave.WaveViewSettings.ViewType;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Time;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
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
 *
 * @author Dan Cervelli
 */
public class MultiMonitor extends SwarmFrame implements Kioskable
{
	public static final long serialVersionUID = -1;
	
	public final static int[] SPANS = new int[] {15, 30, 60, 120, 180, 240, 300, 600, 15*60, 20*60, 30*60, 60*60};
	private int spanIndex = 1;
	private List<WaveViewPanel> panels;
	private SeismicDataSource dataSource;

	private JToolBar toolbar;
	private JButton settingsButton;
	private JButton removeButton;
	private JPanel wavePanel;
	private JPanel mainPanel;
	private JButton compXButton;
	private JButton expXButton;
	private JButton optionsButton;
	private JButton copyButton;
	private JToggleButton pauseButton;
	
	private int bottomLabelHeight = 20;
	
	private int selectedIndex = -1;
	
	private static final Color SELECT_COLOR = new Color(204, 204, 255);
	
	private Throbber throbber;
	
	private Map<String, Wave> waveMap;
	
	private Timer timer;
	private long slideInterval = 500;
	private long refreshInterval = 1000;
	private SlideTask slideTask;
	private RefreshTask refreshTask;

	private int labelFontSize;
	private Font font;
	private FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
	private Border border;
	
	private double pauseStartTime = Double.NaN;
	
	public MultiMonitor(SeismicDataSource sds)
	{
		super("Monitor, [" + sds.getName() + "]", true, true, true, true);
		waveMap = Collections.synchronizedMap(new HashMap<String, Wave>());
		this.setFrameIcon(Icons.monitor);
		dataSource = sds;
		dataSource.setUseCache(false);
		panels = new ArrayList<WaveViewPanel>();
		createUI();
		timer = new Timer("Monitor Timer [" + sds.getName() + "]");
		setIntervals();
	}

	public void saveLayout(ConfigFile cf, String prefix)
	{
		cf.put("monitor", prefix);
		super.saveLayout(cf, prefix);
		cf.put(prefix + ".source", dataSource.getName());
		for (int i = 0; i < panels.size(); i++)
		{
			String p = prefix + ".wave-" + i;
			WaveViewPanel wvp = panels.get(i);
			cf.put(p + ".channel", wvp.getChannel());
			wvp.getSettings().save(cf, p);
		}
		cf.put(prefix + ".waves", Integer.toString(panels.size()));
		cf.put(prefix + ".spanIndex", Integer.toString(spanIndex));
		cf.put(prefix + ".slideInterval", Long.toString(slideInterval));
		cf.put(prefix + ".refreshInterval", Long.toString(refreshInterval));
	}
	
	public void processLayout(ConfigFile cf)
	{
		processStandardLayout(cf);
		spanIndex = Integer.parseInt(cf.getString("spanIndex"));
		slideInterval = Long.parseLong(cf.getString("slideInterval"));
		refreshInterval = Long.parseLong(cf.getString("refreshInterval"));
		int waves = Integer.parseInt(cf.getString("waves"));
		for (int i = 0; i < waves; i++)
		{
			String w = "wave-" + i;
			String channel = cf.getString(w + ".channel");
			ConfigFile scf = cf.getSubConfig(w);
			WaveViewPanel wvp = addChannel(channel);
			wvp.getSettings().set(scf);
		}
	}
	
	private void setIntervals()
	{
		if (slideTask != null)
			slideTask.cancel();
		if (refreshTask != null)
			refreshTask.cancel();
		timer.purge();
		slideTask = new SlideTask();
		refreshTask = new RefreshTask();
		timer.schedule(slideTask, 0, slideInterval);
		timer.schedule(refreshTask, 0, refreshInterval);
	}
	
	public void setDataSource(SeismicDataSource sds)
	{
		dataSource = sds;
	}
	
	public SeismicDataSource getDataSource()
	{
		return dataSource;
	}
	
	public long getSlideInterval()
	{
		return slideInterval;
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
		setIntervals();
	}
	
	public void setSlideInterval(long ms)
	{
		slideInterval = ms;
		setIntervals();
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
		
		toolbar = SwarmUtil.createToolBar();

		optionsButton = SwarmUtil.createToolBarButton(
				Icons.settings,
				"Monitor options",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						MultiMonitorSettingsDialog mmsd = MultiMonitorSettingsDialog.getInstance(MultiMonitor.this);
						mmsd.setVisible(true);
					}
				});
		toolbar.add(optionsButton);
		
		toolbar.addSeparator();
		
		compXButton = SwarmUtil.createToolBarButton(
				Icons.xminus,
				"Shrink time axis (Alt-left arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						if (spanIndex != 0)
							spanIndex--;
					}
				});
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		toolbar.add(compXButton);
		
		expXButton = SwarmUtil.createToolBarButton(
				Icons.xplus,
				"Expand time axis (Alt-right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						if (spanIndex < SPANS.length - 1)
							spanIndex++;
					}
				});
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
		toolbar.add(expXButton);
		
		pauseButton = SwarmUtil.createToolBarToggleButton(
				Icons.pause,
				"Pause the monitor (P)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						if (!pauseButton.isSelected())
							setPauseStartTime(Double.NaN);
						else
							setPauseStartTime(getTimeWindow()[0]);
					}
				});
		Util.mapKeyStrokeToButton(this, "P", "pause", pauseButton);
		toolbar.add(pauseButton);
		
		toolbar.addSeparator();
		
		settingsButton = SwarmUtil.createToolBarButton(
				Icons.wavesettings,
				"Settings for selected wave",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						if (selectedIndex >= 0)
						{
							WaveViewPanel panel = panels.get(selectedIndex);
							WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(panel.getSettings());
							wvsd.setVisible(true);
						}
					}
				});
		toolbar.add(settingsButton);
	
		copyButton = SwarmUtil.createToolBarButton(
				Icons.clipboard,
				"Copy waves to clipboard (C or Ctrl-C)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						for (WaveViewPanel panel : panels)
						{
							if (panel != null)
							{
								WaveViewPanel p =  new WaveViewPanel(panel);
								p.setDataSource(dataSource.getCopy());
								WaveClipboardFrame cb = Swarm.getApplication().getWaveClipboard();
								cb.setVisible(true);
								cb.addWave(p);
							}
						}
					}
				});
		Util.mapKeyStrokeToButton(this, "C", "clipboard1", copyButton);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard2", copyButton);
		toolbar.add(copyButton);
		
		toolbar.addSeparator();
		
		removeButton = SwarmUtil.createToolBarButton(
				Icons.delete,
				"Remove selected wave from monitor",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						requestFocus();
						if (selectedIndex >= 0)
						{
							removeWaveAtIndex(selectedIndex);
							if (panels.size() == 0)
								selectedIndex = -1;
							else if (panels.size() == selectedIndex)
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
		wavePanel = new WavePanel();
		wavePanel.setLayout(null);
		
		border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 3, 3), 
				BorderFactory.createLineBorder(Color.GRAY, 1));
				
		wavePanel.setBorder(border);
		
		mainPanel.add(wavePanel, BorderLayout.CENTER);

		createListeners();
		this.setContentPane(mainPanel);
		this.setVisible(true);
	}
	
	private class WavePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;
		private BufferedImage image;
		
		public WavePanel()
		{
			addComponentListener(new ComponentAdapter()
					{
						public void componentMoved(ComponentEvent e)
						{
							resizeWaves();
						}
						
						public void componentResized(ComponentEvent e)
						{
							createImage();
							resizeWaves();
						}
					});
		}
				
		private synchronized void createImage()
		{
			if (getWidth() > 0 && getHeight() > 0)
				image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		}
		
		public void paint(Graphics g)
		{
			if (getWidth() <= 0 || getHeight() <= 0)
				return;
			
			if (image == null || panels.size() == 0)
			{
				super.paint(g);
				Dimension dim = this.getSize();
				g.setColor(Color.black);
				g.drawString("Monitor empty.", dim.width / 2 - 40, dim.height / 2);	
			}
			else
			{
				super.paint(image.getGraphics());
				g.drawImage(image, 0, 0, null);
				g.setColor(Color.WHITE);
				Insets insets = wavePanel.getInsets();
				int y = getHeight() - bottomLabelHeight - insets.top - insets.bottom;
				g.fillRect(insets.left, y, getWidth() - insets.left - insets.right, bottomLabelHeight + 1);
				g.setColor(Color.GRAY);
				g.drawLine(insets.left, y, getWidth() - insets.left - 1, y);
				g.setColor(Color.BLACK);
//				double now = CurrentTime.getInstance().nowJ2K();
				Graphics2D g2 = (Graphics2D)g;
				Object aa = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				double[] times = getTimeWindow();
				double now = times[1];
				double start = times[0];
				
				String tf = Time.format("HH:mm:ss", now);
				Font font = Font.decode("dialog-BOLD-" + (bottomLabelHeight - 2));
				g.setFont(font);
				FontMetrics fm = g.getFontMetrics();
//				System.out.println(insets.bottom);
				g.drawString(tf, getWidth() - fm.stringWidth(tf) - 4, getHeight() - (insets.bottom + 3));
				tf = Time.format("HH:mm:ss", start);
				g.drawString(tf, 4, getHeight() - (insets.bottom + 3));
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, aa);
			}
		}
	}
	
	private void createListeners()
	{
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
						throbber.close();
						selectedIndex = -1;
						timer.cancel();
						dataSource.close();
						panels.clear();
						wavePanel.removeAll();
						waveMap.clear();
						dispose();
						Swarm.getApplication().removeMonitor(MultiMonitor.this);
					}
			  });

		this.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		
		this.addKeyListener(new KeyAdapter()
				{
					public void keyPressed(KeyEvent e)
					{
						if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_R)
						{
							for (WaveViewPanel panel : panels)
								panel.resetAutoScaleMemory();
						}
					}
				});
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
			RectangleRenderer rr = ar.getFrame();
			rr.color = Color.GRAY;
				
			TextRenderer label = new TextRenderer(fr.getGraphX() + 5, fr.getGraphY() + labelFontSize + 3, panel.getChannel(), Color.BLACK);
			label.font = font;
			label.color = Color.BLACK;
			
			rr = new RectangleRenderer();
			rr.rect = new Rectangle2D.Double();
			rr.rect.setFrame(font.getStringBounds(panel.getChannel(), frc));
			rr.rect.x = 2;
			rr.rect.width += 5;
			rr.rect.y = 3;
	        rr.color = Color.GRAY;
	        rr.backgroundColor = new Color(255, 255, 255, 210);
			
			int hTicks = fr.getGraphWidth() / 108;
			Object[] stt = SmartTick.autoTimeTick(fr.getMinXAxis(), fr.getMaxXAxis(), hTicks);
	        if (stt != null)
	        	ar.createVerticalGridLines((double[])stt[0]);
	        
	        if (panel.getSettings().viewType == ViewType.WAVE && panel.getHeight() > 36)
	        {
	        	double m = 1;
	        	double b = 0;
	        	String Units = "Counts";
	        	if (panel.getSettings().useUnits)
	        	{
	        		Metadata md = Swarm.config.getMetadata(panel.getChannel(), true);
	        		m = md.getMultiplier();
	        		b = md.getOffset();
	        		Units = md.getUnit();
	        	}
	        	
	        	if (Units == null)
	        		Units = "Counts";

		        double min = fr.getMinY() * m + b;
		        double max = fr.getMaxY() * m + b;
	        	String range = String.format("%.0f / %.0f %s", min, max, Units);
	        	TextRenderer tr = new TextRenderer(fr.getGraphX() + 2, fr.getGraphY() + fr.getGraphHeight() - 2, range);
	        	int fs = Math.min(10, labelFontSize);
	        	tr.font = Font.decode("dialog-PLAIN-" + fs);
	        	tr.color = Color.BLACK;
	        	ar.addPostRenderer(tr);
	        }
	        
	        ar.addPostRenderer(rr);
	        ar.addPostRenderer(label);
		}
	}

	// TODO: add sorted
	public synchronized WaveViewPanel addChannel(String ch)
	{
		final WaveViewPanel panel = new WaveViewPanel();
		panel.setChannel(ch);
		panel.setOffsets(-1, 0, 0, 0);
		panel.setWorking(true);
		panel.setDisplayTitle(false);
		panel.setFrameDecorator(new MonitorWaveDecorator(panel));
		panels.add(panel);
		wavePanel.add(panel);
		panel.addListener(new WaveViewPanelAdapter()
				{
					public void mousePressed(WaveViewPanel src, MouseEvent e, boolean dragging)
					{
						requestFocusInWindow();
						select(panel);
					}
				});
		resizeWaves();
		return panel;
	}
	
	public void deselect()
	{
		if (selectedIndex >= 0)
		{
			WaveViewPanel panel = panels.get(selectedIndex);
			setBackgroundColor(panel, selectedIndex);
			panel.createImage();
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
				panel.createImage();
				panel.repaint();
				break;
			}
		}
		repaint();
	}
	
	public void removeWaveAtIndex(int i)
	{
		WaveViewPanel wvp = panels.get(i);
		waveMap.remove(wvp.getChannel());
		panels.remove(i);
		wavePanel.remove(wvp);
		resizeWaves();
	}
	
	public void removeAllWaves()
	{
		waveMap.clear();
		panels.clear();
		wavePanel.removeAll();
		resizeWaves();
	}
	
	private void setBackgroundColor(WaveViewPanel wvp, int i)
	{
		if (i % 2 == 1)
			wvp.setBackgroundColor(Color.WHITE);
		else
			wvp.setBackgroundColor(new Color(230, 230, 230));
	}
	
	private void resizeWaves()
	{
		if (panels.size() == 0 || wavePanel == null || wavePanel.getWidth() <= 0 || wavePanel.getHeight() <= 0)
		{
			repaint();
			return;
		}
		
		int area = wavePanel.getHeight() * wavePanel.getWidth();
		bottomLabelHeight = (int)(area / 18000.0);
		bottomLabelHeight = Math.max(bottomLabelHeight, 12);
		bottomLabelHeight = Math.min(bottomLabelHeight, 26);
		
		Insets insets = wavePanel.getInsets();
		int ah = wavePanel.getHeight() - insets.top - insets.bottom - bottomLabelHeight;
		int ww = wavePanel.getWidth() - insets.left - insets.right;
		double dy = ((double)ah / (double)panels.size());
		int wh = (int)Math.round(dy);
		int th = wh * panels.size();
		int dh = th - ah;
		int rh = insets.top;
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
			wvp.setSize(ww, awh);
			wvp.setLocation(insets.left, rh);
			setBackgroundColor(wvp, i);
			rh += awh;
		}
		labelFontSize = Math.min(36, wh / 2);
		boolean done = false;
		while (!done)
		{
			font = Font.decode("dialog-BOLD-" + labelFontSize);
			Rectangle2D r = font.getStringBounds("XXXX XX XXX", frc);
			if (r.getWidth() / ww < 0.25 || labelFontSize <= 8)
				done = true;
			else
				labelFontSize--;
		}
		repaint();
	}
	
	private double[] getTimeWindow()
	{
		double[] times = new double[2];
		if (Double.isNaN(pauseStartTime))
		{
			times[1] = CurrentTime.getInstance().nowJ2K();
			times[0] = times[1] - SPANS[spanIndex];
		}
		else
		{
			times[0] = pauseStartTime;
			times[1] = times[0] + SPANS[spanIndex];
		}
		return times;
	}
	
	private boolean sliding = false;
	private synchronized void slide()
	{
		if (sliding)
			return;
		
		Runnable r = new Runnable()
				{
					public void run()
					{
						sliding = true;
						double[] times = getTimeWindow();
						double now = times[1];
						double start = times[0];
						for (int i = 0; i < panels.size(); i++)
						{
							WaveViewPanel waveViewPanel = panels.get(i);
							Wave wave = waveMap.get(waveViewPanel.getChannel());
							waveViewPanel.setWave(wave, start, now);
						}
						wavePanel.repaint();
						sliding = false;
					}
				};
				
		Thread worker = new Thread(r);
		worker.start();
	}
	
	/**
	 * Sets the start time of the monitor if it's paused.  Set to Double.NaN
	 * to resume.
	 * @param start
	 */
	public void setPauseStartTime(double start)
	{
		pauseStartTime = start;
		pauseButton.setSelected(!Double.isNaN(pauseStartTime));
	}
	
	public double getPauseStartTime()
	{
		return pauseStartTime;
	}
	
	private void refresh()
	{
		if (throbber.getCount() >= 1)
			return;
		
		Runnable r = new Runnable()
				{
					public void run()
					{
						throbber.increment();
						String channel = null;
						
						double[] times = getTimeWindow();
						double now = times[1];
						double start = times[0];
						for (int i = 0; i < panels.size(); i++)
						{
							WaveViewPanel wvp = panels.get(i);
							wvp.setWorking(true);
							channel = wvp.getChannel();
							try
							{
								Wave sw = waveMap.get(channel);
								if (sw != null) 
								{
									if (sw.overlaps(start, now)) // runaway monitor bug fix
									{
										if (sw.getEndTime() < now)
										{
											Wave w2 = dataSource.getWave(channel, sw.getEndTime() - 10, now);
											if (w2 != null && sw.overlaps(w2) || sw.adjacent(w2))
												sw = sw.combine(w2);
										}
										if (sw.getStartTime() > start)
										{
											Wave w2 = dataSource.getWave(channel, start, sw.getStartTime() + 10);
											if (w2 != null && sw.overlaps(w2) || sw.adjacent(w2))
												sw = sw.combine(w2);
										}
										sw = sw.subset(start, sw.getEndTime());
									}
									else
										sw = null;
								}
								
								// something bad happened above, just get the whole wave
								if (sw == null)
									sw = dataSource.getWave(channel, start, now);
								if (sw != null)
								{
									waveMap.put(channel, sw);
								}
							}
							catch (Throwable t)
							{
								System.out.println(channel);
								t.printStackTrace();
							}
							wvp.setWorking(false);
						}
						throbber.decrement();
					}
				};
	
		Thread worker = new Thread(r);
		worker.start();
	}
	
	public void setKioskMode(boolean b)
	{
		setDefaultKioskMode(b);
		if (fullScreen)
		{
			mainPanel.remove(toolbar);
			wavePanel.setBorder(null);
		}
		else
		{
			mainPanel.add(toolbar, BorderLayout.NORTH);
			wavePanel.setBorder(border);
		}
	}
	
	private class SlideTask extends TimerTask
	{
		public void run()
		{
			if (panels.size() > 0)
				slide();
		}
	}
	
	private class RefreshTask extends TimerTask
	{
		public void run()
		{
			if (panels.size() > 0)
				refresh();
		}
	}
}
