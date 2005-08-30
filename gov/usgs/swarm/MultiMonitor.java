package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * MultiMonitor is a window that is used to display multiple seismic
 * channels in real-time.
 *
 * $Log: not supported by cvs2svn $
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
//	private JPanel wavePanel;
	private JButton compXButton;
	private JButton expXButton;
	private JButton optionsButton;
	
	private Box waveBox;
	
	private long refreshInterval = 1000;
	
	private Thread refreshThread;
	
	private int selectedIndex = -1;
	
	private static final Color selectColor = new Color(204, 204, 255);
	private static final Color backgroundColor = new Color(0xf7, 0xf7, 0xf7);	
	
	public MultiMonitor(SeismicDataSource sds)
	{
		super("Monitor", true, true, true, true);
		dataSource = sds;
		channels = new ArrayList<String>();
		panels = new ArrayList<WaveViewPanel>();
		waveBox = new Box(BoxLayout.Y_AXIS);
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						createUI();
					}
				});
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
					public void internalFrameOpened(InternalFrameEvent e)
					{
						resizeWaves();
					}
					
					public void internalFrameClosing(InternalFrameEvent e)
					{
						dataSource.close();
						panels.clear();
						channels.clear();
						waveBox.removeAll();
//						wavePanel.removeAll();
						MultiMonitor.this.setVisible(true);
					}
			  });

		this.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
		refreshThread = new Thread(this);
		refreshThread.start();
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
		
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.setRollover(true);

		optionsButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("settings"))));
		optionsButton.setToolTipText("Monitor options");
		optionsButton.setMargin(new Insets(0,0,0,0));
		optionsButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						MultiMonitorSettingsDialog mmsd = MultiMonitorSettingsDialog.getInstance(MultiMonitor.this);
						mmsd.setVisible(true);
					}
				});
		toolbar.add(optionsButton);
		toolbar.addSeparator();
		
		compXButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("xminus"))));
		compXButton.setMargin(new Insets(0,0,0,0));
		compXButton.setToolTipText("Shrink time axis");
		compXButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex != 0)
							spanIndex--;
					}
				});
		toolbar.add(compXButton);
		
		expXButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("xplus"))));
		expXButton.setMargin(new Insets(0,0,0,0));
		expXButton.setToolTipText("Expand time axis");
		expXButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (spanIndex < SPANS.length - 1)
							spanIndex++;
					}
				});
		toolbar.add(expXButton);
		toolbar.addSeparator();
		
		settingsButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("wavesettings"))));
		settingsButton.setMargin(new Insets(0,0,0,0));
		settingsButton.setToolTipText("Settings for selected wave");
		settingsButton.addActionListener(new ActionListener()
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
		
		removeButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("delete"))));
		removeButton.setMargin(new Insets(0,0,0,0));
		removeButton.setToolTipText("Remove selected wave from monitor");
		removeButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selectedIndex >= 0)
						{
							removeWaveAtIndex(selectedIndex);
							selectedIndex = -1;
						}
						
					}
				});
		toolbar.add(removeButton);
		mainPanel.add(toolbar, BorderLayout.NORTH);
		mainPanel.add(waveBox, BorderLayout.CENTER);
		
		waveBox.setBorder(LineBorder.createGrayLineBorder());
		
		this.setContentPane(mainPanel);
		this.setVisible(true);
	}
	
	public synchronized void addChannel(String ch)
	{
		channels.add(ch);
		WaveViewPanel panel = new WaveViewPanel();
		panel.setWorking(true);
		panel.setDisplayTitle(true);
		panels.add(panel);
		waveBox.add(panel);
		panel.setMonitor(this);
		resizeWaves();
	}
	
	public void select(WaveViewPanel p)
	{
		if (selectedIndex >= 0)
		{
			WaveViewPanel panel = panels.get(selectedIndex);
			panel.setBackgroundColor(backgroundColor);
			panel.invalidateImage();
			panel.repaint();
		}
		for (int i = 0; i < panels.size(); i++)
		{
			WaveViewPanel panel = panels.get(i);
			if (panel == p)
			{
				selectedIndex = i;
				panel.setBackgroundColor(selectColor);
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
			return;
		int dy = waveBox.getHeight() / panels.size();
		for (int i = 0; i < panels.size(); i++)
		{
			WaveViewPanel wvp = panels.get(i);
			wvp.setLocation(0, dy * i);
			wvp.setSize(this.getWidth(), dy);
		}
		waveBox.validate();
		repaint();
	}
	
	public synchronized void refresh()
	{
		double now = CurrentTime.nowJ2K();
		for (int i = 0; i < channels.size(); i++)
		{
			WaveViewPanel waveViewPanel = panels.get(i);
			String channel = channels.get(i);
			Wave sw = dataSource.getWave(channel, now - SPANS[spanIndex], now);
			waveViewPanel.setWorking(true);
			waveViewPanel.setWave(sw, now - SPANS[spanIndex], now);
			waveViewPanel.setChannel(channel);
			waveViewPanel.setDataSource(dataSource);
			waveViewPanel.setWorking(false);
		}
		if (!this.isVisible())
			dataSource.close();
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
					refresh();
				
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
