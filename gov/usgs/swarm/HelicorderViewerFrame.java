package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicInternalFrameUI;
 
/**
 * <code>JInternalFrame</code> that holds a helicorder.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/08/26 23:27:03  uid889
 * Create image path constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.5  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.4  2005/04/13 00:50:02  cervelli
 * Fixed show() call.
 *
 * Revision 1.3  2005/03/27 22:02:16  cervelli
 * Did some cleanup.
 *
 * Revision 1.2  2004/10/28 20:14:41  cvs
 * Support for big red mouse cursor.
 *
 * @author Dan Cervelli
 */
public class HelicorderViewerFrame extends JInternalFrame 
{
	public static final long serialVersionUID = -1;
		
	// minutes * 60 = seconds
	public static final int[] chunkValues = new int[] {10 * 60, 15 * 60, 20 * 60, 30 * 60, 60 * 60, 120 * 60, 180 * 60, 360 * 60};
	
	// hours * 60 = minutes
	public static final int[] spanValues = new int[] {2 * 60, 4 * 60, 6 * 60, 12 * 60, 24 * 60, 48 * 60, 72 * 60, 96 * 60, 120 * 60, 144 * 60, 168 * 60, 192 * 60, 216 * 60,
			240 * 60, 264 * 60, 288 * 60, 312 * 60, 336 * 60};
	
	// seconds
	public static final int[] zoomValues = new int[] {1, 2, 5, 10, 20, 30, 60, 120, 300};
	
	private RefreshThread refreshThread;
	private SeismicDataSource dataSource;
	private String channel;
	private JPanel mainPanel;
	private JToolBar toolbar;
	private JButton showToolbar;
	private JButton settingsButton;
	private JButton backButton;
	private JButton forwardButton;
	private JButton compX;
	private JButton expX;
	private JButton compY;
	private JButton expY;
	private JButton clipboard;
	private JButton removeWave; 
	
//	private JButton waveSettingsButton; 
	private HelicorderViewPanel helicorderViewPanel;
	
	private WaveViewSettings waveViewSettings;
	private HelicorderViewerSettings settings;
	
	private boolean working;
	private JPanel statusPanel;
	private JLabel statusLabel;
	
	private boolean fullScreen;
	private boolean toolbarWasVisible;
	
	private JComponent northPane;
	private Dimension oldNorthPaneSize;
	
	private JPanel heliPanel;
	private WigglerPanel wigglerPanel;
	
	private long lastRefreshTime;
	
	public HelicorderViewerFrame(Swarm sw, SeismicDataSource sds, String ch)
	{
		super("[" + sds + "]: " + ch, true, true, true, true);
		settings = new HelicorderViewerSettings();
		waveViewSettings = new WaveViewSettings();
		dataSource = sds.getCopy();
		channel = ch;
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						createUI();
						getHelicorder();
					}
				});
		
		//settingsDialog = new HelicorderViewerSettingsDialog(settings, waveViewSettings);
		
		refreshThread = new RefreshThread();
	}
	
	public void createUI()
	{
		helicorderViewPanel = new HelicorderViewPanel(this);
		settings.view = helicorderViewPanel;

		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameClosing(InternalFrameEvent e)
					{
						dispose();
						refreshThread.kill();
						Swarm.getParentFrame().removeInternalFrame(HelicorderViewerFrame.this);
						dataSource.notifyDataNotNeeded(channel, helicorderViewPanel.getStartTime(), helicorderViewPanel.getEndTime());
						dataSource.close();
						if (wigglerPanel != null)
							wigglerPanel.kill();
					}
					
					public void internalFrameDeiconified(InternalFrameEvent e)
					{
						helicorderViewPanel.setResized(true);
						repaintHelicorder();
						repaint();
					}
				});

		this.addComponentListener(new ComponentAdapter()
				{
					public void componentResized(ComponentEvent e)
					{
						helicorderViewPanel.setResized(true);
						repaintHelicorder();
						repaint();
					}
				});
		this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		
		mainPanel = new JPanel(new BorderLayout());
		heliPanel = new JPanel(new BorderLayout());
		heliPanel.setBorder(LineBorder.createGrayLineBorder());
		heliPanel.add(helicorderViewPanel, BorderLayout.CENTER);
		mainPanel.add(heliPanel, BorderLayout.CENTER);
			
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		JButton hideTB = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("minimize"))));
		hideTB.setToolTipText("Hide toolbar");
		hideTB.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						showToolbar.setVisible(true);
						mainPanel.remove(toolbar);
						mainPanel.validate();
						repaintHelicorder();
						helicorderViewPanel.requestFocus();
					}
				});
		hideTB.setMargin(new Insets(0,0,0,0));
		toolbar.add(hideTB);
		toolbar.addSeparator();
		
		settingsButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("settings"))));
		settingsButton.setToolTipText("Helicorder View Settings");
		settingsButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						HelicorderViewerSettingsDialog hvsd = HelicorderViewerSettingsDialog.getInstance(settings, waveViewSettings);
						hvsd.setVisible(true);
						getHelicorder();
					}
				});
		settingsButton.setMargin(new Insets(0,0,0,0));
		toolbar.add(settingsButton);
		
		toolbar.addSeparator();
		backButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("left"))));
		
		backButton.setToolTipText("Scroll back time (A or Left arrow)");
		backButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (helicorderViewPanel.hasInset())
							helicorderViewPanel.moveInset(-1);
						else
							scroll(-1);
					}
				});
		backButton.setMargin(new Insets(0,0,0,0));
		toolbar.add(backButton);
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
		Util.mapKeyStrokeToButton(this, "A", "backward2", backButton);
		
		forwardButton = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("right"))));
		forwardButton.setToolTipText("Scroll forward time (Z or Right arrow)");
		forwardButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (helicorderViewPanel.hasInset())
							helicorderViewPanel.moveInset(1);
						else
							scroll(1);
					}
				});
		forwardButton.setMargin(new Insets(0,0,0,0));
		toolbar.add(forwardButton);
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);				
		Util.mapKeyStrokeToButton(this, "Z", "forward2", forwardButton);				
		
		compX = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("xminus"))));
		compX.setToolTipText("Compress X-axis (Alt-left arrow)");
		toolbar.add(compX);
		compX.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						decXAxis();
						getHelicorder();
					}
				});
		compX.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compX);
		
		expX = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("xplus"))));
		toolbar.add(expX);
		expX.setToolTipText("Expand X-axis (Alt-right arrow)");
		expX.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						incXAxis();
						getHelicorder();
					}
				});
		expX.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expX);
		
		compY = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("yminus"))));
		compY.setToolTipText("Compress Y-axis (Alt-down arrow)");
		toolbar.add(compY);
		compY.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
//						settingsDialog.decYAxis();
						decYAxis();
						getHelicorder();
					}
				});
		compY.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "alt DOWN", "compy", compY);				
		
		expY = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("yplus"))));
		toolbar.add(expY);
		expY.setToolTipText("Expand Y-axis (Alt-up arrow)");
		expY.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						incYAxis();
						getHelicorder();
					}
				});
		expY.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "alt UP", "expy", expY);
		toolbar.addSeparator();
		
		JButton addZoom = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("zoomplus"))));
		addZoom.setToolTipText("Decrease zoom time window (+)");
		toolbar.add(addZoom);
		addZoom.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						decZoom();
						settings.notifyView();
					}
				});
		addZoom.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "EQUALS", "addzoom1", addZoom);
		Util.mapKeyStrokeToButton(this, "shift EQUALS", "addzoom2", addZoom);
		
		JButton subZoom = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("zoomminus"))));
		toolbar.add(subZoom);
		subZoom.setToolTipText("Increase zoom time window (-)");
		subZoom.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						incZoom();
						settings.notifyView();
					}
				});
		subZoom.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "MINUS", "subzoom", subZoom);

		new WaveViewSettingsToolbar(waveViewSettings, toolbar, this);
		clipboard = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("clipboard"))));
		clipboard.setEnabled(false);
		toolbar.add(clipboard);
		clipboard.setToolTipText("Copy inset to clipboard (C or Ctrl-C)");
		clipboard.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						helicorderViewPanel.insetToClipboard();
					}
				});
		clipboard.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "control C", "clipboard1", clipboard);
		Util.mapKeyStrokeToButton(this, "C", "clipboard2", clipboard);
		
		removeWave = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("delete"))));
		removeWave.setEnabled(false);
		toolbar.add(removeWave);
		removeWave.setToolTipText("Remove inset wave (Delete or Escape)");
		removeWave.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						helicorderViewPanel.removeWaveInset();
					}
				});		
		removeWave.setMargin(new Insets(0,0,0,0));
		Util.mapKeyStrokeToButton(this, "ESCAPE", "removewave", removeWave);
		Util.mapKeyStrokeToButton(this, "DELETE", "removewave", removeWave);
		
		mainPanel.add(toolbar, BorderLayout.NORTH);
		
		statusPanel = new JPanel(new BorderLayout());
		statusLabel = new JLabel(" ");
		statusLabel.setHorizontalAlignment(JLabel.LEFT);
		statusPanel.add(statusLabel, BorderLayout.CENTER);
		mainPanel.add(statusPanel, BorderLayout.SOUTH);
		
		showToolbar = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("maximize"))));
		showToolbar.setMargin(new Insets(0, 0, 0, 0));
		showToolbar.setSize(24, 24);
		showToolbar.setLocation(0, 0);
		showToolbar.setVisible(false);
		showToolbar.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						showToolbar();
					}
				});
		this.getLayeredPane().setLayer(showToolbar, JLayeredPane.PALETTE_LAYER.intValue());
		this.getLayeredPane().add(showToolbar);
		
		toolbar.setRollover(true);
		this.setContentPane(mainPanel);
		this.setSize(800, 750);
		this.setVisible(true);
	}

	public HelicorderViewPanel getHelicorderViewPanel()
	{
		return helicorderViewPanel;
	}
	
	public void settingsChanged()
	{
		if (!settings.showWiggler && wigglerPanel != null)
			removeWiggler();
		if (settings.showWiggler && wigglerPanel == null)
			createWiggler();
	}
	
	public void createWiggler()
	{
		wigglerPanel = new WigglerPanel(dataSource, channel);
		heliPanel.add(wigglerPanel, BorderLayout.SOUTH);
		wigglerPanel.setPreferredSize(new Dimension(this.getSize().width, 75));
	}
	
	public void removeWiggler()
	{
		if (wigglerPanel != null)
		{
			wigglerPanel.kill();
			heliPanel.remove(wigglerPanel);
			wigglerPanel = null;
		}
	}

	public void incXAxis()
	{
		int index = -1;
		for (int i = 0; i < chunkValues.length; i++)
			if (settings.timeChunk == chunkValues[i])
			{
				index = i;
				break;
			}
		
		if (index == -1 || index == chunkValues.length - 1)
			return;
		
		settings.timeChunk = chunkValues[index + 1];
	}
	
	public void decXAxis()
	{
		int index = -1;
		for (int i = 0; i < chunkValues.length; i++)
			if (settings.timeChunk == chunkValues[i])
			{
				index = i;
				break;
			}
		
		if (index == -1 || index == 0)
			return;
		
		settings.timeChunk = chunkValues[index - 1];
	}
	
	public void incYAxis()
	{
		int index = -1;
		for (int i = 0; i < spanValues.length; i++)
			if (settings.span == spanValues[i])
			{
				index = i;
				break;
			}
		
		if (index == -1 || index == spanValues.length - 1)
			return;
		
		settings.span = spanValues[index + 1];
	}
	
	public void decYAxis()
	{
		int index = -1;
		for (int i = 0; i < spanValues.length; i++)
			if (settings.span == spanValues[i])
			{
				index = i;
				break;
			}
		
		if (index == -1 || index == 0)
			return;
		
		settings.span = spanValues[index - 1];
	}
	
	public void incZoom()
	{
		int index = -1;
		for (int i = 0; i < zoomValues.length; i++)
			if (settings.waveZoomOffset == zoomValues[i])
			{
				index = i;
				break;
			}
		
		if (index == -1 || index == zoomValues.length - 1)
			return;
		
		settings.waveZoomOffset = zoomValues[index + 1];
	}
	
	public void decZoom()
	{ 
		int index = -1;
		for (int i = 0; i < zoomValues.length; i++)
			if (settings.waveZoomOffset == zoomValues[i])
			{
				index = i;
				break;
			}
		
		if (index == -1 || index == 0)
			return;
		
		settings.waveZoomOffset = zoomValues[index - 1];
	}
	
	// TODO: change these to setXXX(boolean)
	public void disableInsetButtons()
	{
		clipboard.setEnabled(false);
		removeWave.setEnabled(false);
	}
	
	public void enableInsetButtons()
	{
		clipboard.setEnabled(true);
		removeWave.setEnabled(true);
	}
	
	public void disableNavigationButtons()
	{
		compX.setEnabled(false);
		expX.setEnabled(false);
		forwardButton.setEnabled(false);
		backButton.setEnabled(false);
		compY.setEnabled(false);
		expY.setEnabled(false);
	}
	
	public void enableNavigationButtons()
	{
		compX.setEnabled(true);
		expX.setEnabled(true);
		forwardButton.setEnabled(true);
		backButton.setEnabled(true);
		compY.setEnabled(true);
		expY.setEnabled(true);
	}
	//

	public void setFullScreen(boolean full)
	{
		fullScreen = full;
		
		this.setResizable(!fullScreen);
		this.setIconifiable(!fullScreen);
		this.setMaximizable(!fullScreen);
		this.setClosable(!fullScreen);
		this.putClientProperty("JInternalFrame.isPalette", new Boolean(fullScreen)); 
		helicorderViewPanel.setFullScreen(fullScreen);
		BasicInternalFrameUI ui = (BasicInternalFrameUI)this.getUI();
		if (fullScreen)
		{
			northPane = ui.getNorthPane();
			oldNorthPaneSize = northPane.getSize();
			northPane.setVisible(false);
			northPane.setPreferredSize(new Dimension(0,0));
			
			toolbarWasVisible = !showToolbar.isVisible();
			mainPanel.remove(toolbar);
			showToolbar.setVisible(false);
		}
		else
		{
			northPane.setVisible(true);
			northPane.setPreferredSize(oldNorthPaneSize);
			if (toolbarWasVisible)
				mainPanel.add(toolbar, BorderLayout.NORTH);
			else
				showToolbar.setVisible(true);
		}	
	}
	{
		if (helicorderViewPanel != null)
			helicorderViewPanel.requestFocus();
	}
	public void setStatus(final String status)
	{
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						statusLabel.setText(status);
					}
				});	
	}
	
	public JLabel getStatusLabel()
	{
		return statusLabel;	
	}

	public WaveViewSettings getWaveViewSettings()
	{
		return waveViewSettings;	
	}

	public HelicorderViewerSettings getHelicorderViewerSettings()
	{
		return settings;	
	}

	public void showToolbar()
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						showToolbar.setVisible(false);
						mainPanel.add(toolbar, BorderLayout.PAGE_START);	
						mainPanel.doLayout();
						repaintHelicorder();
						helicorderViewPanel.requestFocus();
					}
				});
	}

	public void repaintHelicorder()
	{
		helicorderViewPanel.invalidateImage();
		//helicorderViewPanel.repaint();	
	}
	
	public void scroll(int units)
	{
		double bt = settings.getBottomTime();
		if (Double.isNaN(bt))
			bt = CurrentTime.nowJ2K();
			
		settings.setBottomTime(bt + units * settings.scrollSize * settings.timeChunk);
		getHelicorder();
	}

	public boolean isWorking()
	{
		return working;	
	}

	public void getHelicorder()
	{
    	final SwingWorker worker = new SwingWorker() 
    			{
    				private double end;
    				private double before;
    				private HelicorderData hd;
    				private boolean success = false;
    				
    				public Object construct()
    				{
    					try
						{
	    					disableNavigationButtons();
	    					Swarm.getParentFrame().incThreadCount();
	    					working = true;
							end = settings.getBottomTime();
							if (Double.isNaN(end))
								end = CurrentTime.nowJ2K();
							
							before = end - settings.span * 60;
							int tc = 30;
							if (helicorderViewPanel != null)
								tc = settings.timeChunk;
							
							hd = dataSource.getHelicorder(channel, before - tc, end + tc);
							success = true;
						}
    					catch (RuntimeException e)
						{
    						e.printStackTrace();
    						System.err.println("Error: " + e.getMessage());
						}
						return null;
					}
					
					public void finished()
					{
						lastRefreshTime = System.currentTimeMillis();
						Swarm.getParentFrame().decThreadCount();
						enableNavigationButtons();
						working = false;
						if (success)
						{
							if (hd != null && hd.getEndTime() < before && !dataSource.isActiveSource())
							{
								// this would get executed if the data source
								// forcably returned a different time than asked
								// for -- like in the case of a miniSEED.
								double dt = end - before;
								before = hd.getEndTime() - dt / 2;
								end = hd.getEndTime() + dt / 2;
								settings.setBottomTime(end);
							}
							helicorderViewPanel.setHelicorder(hd, before, end);
							repaintHelicorder();
						}
					}
				};
		worker.start();
	}

	public Wave getWave(double t1, double t2)
	{
		return dataSource.getWave(channel, t1, t2);	
	}

	public SeismicDataSource getDataSource()
	{
		return dataSource;	
	}
	
	public String getChannel()
	{
		return channel;	
	}

	class RefreshThread extends Thread
	{
		private boolean kill = false;
		
		public RefreshThread()
		{
			super("HeliRefresh-" + channel);
			this.setPriority(Thread.MIN_PRIORITY);
			start();
		}
		
		public void kill()
		{
			kill = true;	
			this.interrupt();
		}
		
		public void run()
		{
			Swarm.getParentFrame().incThreadCount();
			while (!kill)
			{
				if (!Double.isNaN(settings.getBottomTime()) && settings.getLastBottomTimeSet() > 10 * 60 * 1000)
				{
					helicorderViewPanel.removeWaveInset();
					settings.setBottomTime(Double.NaN);
				}
				
				try 
				{ 
					long now = System.currentTimeMillis();
					long sleepTime = Math.min(now - lastRefreshTime, settings.refreshInterval * 1000);
					
					if (settings.refreshInterval > 0)
						Thread.sleep(sleepTime); 
					else
						Thread.sleep(30 * 1000);
				} catch (Exception e) {}
				
				long now = System.currentTimeMillis();
				
				if (!kill && settings.refreshInterval > 0 && (now - lastRefreshTime) > settings.refreshInterval * 1000)
				{
					try
					{
						double bt = settings.getBottomTime();
						if (dataSource.isActiveSource() || Double.isNaN(bt) || CurrentTime.nowJ2K() < bt)
						{
							if (!working)
								getHelicorder();
						}
					}
					catch (Exception e)
					{
						System.err.println("Exception during refresh:");
						e.printStackTrace();	
					}
				}
			}			
			Swarm.getParentFrame().decThreadCount();
		}	
	}
}