package gov.usgs.swarm;

import gov.usgs.plot.Plot;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.GridBagHelper;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.heli.plot.HelicorderRenderer;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicInternalFrameUI;
 
/**
 * <code>JInternalFrame</code> that holds a helicorder.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.9  2005/09/13 18:20:59  tparker
 * Display confirm dialog before overwritting png file.
 *
 * Revision 1.8  2005/09/13 17:56:46  dcervelli
 * Uses helicorder rendering constants to position helicorder when saving pngs.
 *
 * Revision 1.7  2005/09/08 20:27:57  tparker
 * Disable save and autoscale controls during rendering
 *
 * Revision 1.6  2005/09/08 18:54:54  tparker
 * Add save image button
 *
 * Revision 1.5  2005/09/02 16:40:05  dcervelli
 * CurrentTime changes and changed enable/disable[xxx] to set[xxx].
 *
 * Revision 1.4  2005/08/30 18:01:39  tparker
 * Add Autoscale Slider to Helicorder Viewer Frame
 *
 * Revision 1.3  2005/08/30 00:33:30  tparker
 * Update to use Images class
 *
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
	
	//autoScaleSliderButton state
	protected static final int zoomSelected = ItemEvent.DESELECTED;
	protected static final int clippingSelected = ItemEvent.SELECTED;
	
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
	private JButton saveWave;
	protected JCheckBox autoScaleSliderButton;
	protected int autoScaleSliderButtonState;
	protected JSlider autoScaleSlider;

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
		Swarm.getParentFrame().touchUITime();
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

		toolbar.addSeparator();
		
		saveWave = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("camera"))));
		toolbar.add(saveWave);
		saveWave.addActionListener(new SaveButtonActionListener());
		saveWave.setMargin(new Insets(0,0,0,0));

		toolbar.addSeparator();
		
		autoScaleSliderButton = new JCheckBox(new ImageIcon(getClass().getClassLoader().getResource(Images.get("wavezoom"))));
		autoScaleSliderButton.setSelectedIcon(new ImageIcon(getClass().getClassLoader().getResource(Images.get("waveclip"))));
		autoScaleSliderButton.setMargin(new Insets(0,0,0,0));
		autoScaleSliderButton.addItemListener(new ItemListener()
				{
					public void itemStateChanged(ItemEvent e)
					{

						autoScaleSliderButtonState = e.getStateChange();
						if (autoScaleSliderButtonState == zoomSelected) 
						{
							autoScaleSlider.setValue(40 - (new Double(settings.barMult * 4).intValue()));
						} 
						else if (autoScaleSliderButtonState == clippingSelected)
						{
							autoScaleSlider.setValue(settings.clipBars / 3);
						}
						
						settings.notifyView();
					}
				});
		
		autoScaleSliderButtonState = zoomSelected;
		toolbar.add(autoScaleSliderButton);

		autoScaleSlider = new JSlider(1, 39, (int) (10 - settings.barMult) * 4);
		autoScaleSlider.setPreferredSize(new Dimension(100, 20));
		autoScaleSlider.setMaximumSize(new Dimension(100, 20));
		autoScaleSlider.setMinimumSize(new Dimension(100, 20));
		autoScaleSlider.addChangeListener(new ChangeListener()
				{
					public void stateChanged(ChangeEvent e)
					{
						settings.autoScale = true;
						if (!autoScaleSlider.getValueIsAdjusting()) 
						{
							if (autoScaleSliderButtonState == zoomSelected)
								settings.barMult = 10 - ( new Integer(autoScaleSlider.getValue()).doubleValue() / 4);
							else if (autoScaleSliderButtonState == clippingSelected)
								settings.clipBars = autoScaleSlider.getValue() * 3;
							repaintHelicorder();
						}
					}
				});
		
		toolbar.add(autoScaleSlider);
		
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
	
	public void setInsetButtonsEnabled(boolean b)
	{
		clipboard.setEnabled(b);
		removeWave.setEnabled(b);
	}
	
	public void setNavigationButtonsEnabled(boolean b)
	{
		compX.setEnabled(b);
		expX.setEnabled(b);
		forwardButton.setEnabled(b);
		backButton.setEnabled(b);
		compY.setEnabled(b);
		expY.setEnabled(b);
		autoScaleSliderButton.setEnabled(b);
		autoScaleSlider.setEnabled(b);
		saveWave.setEnabled(b);
	}

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
	}
	
	public void scroll(int units)
	{
		double bt = settings.getBottomTime();
		if (Double.isNaN(bt))
			bt = CurrentTime.getInstance().nowJ2K();
			
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
	    					setNavigationButtonsEnabled(false);
	    					Swarm.getParentFrame().incThreadCount();
	    					working = true;
							end = settings.getBottomTime();
							if (Double.isNaN(end))
								end = CurrentTime.getInstance().nowJ2K();
							
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
						setNavigationButtonsEnabled(true);
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
				boolean kiosk = Swarm.getParentFrame().isKiosk();
				long lastUI = System.currentTimeMillis() - Swarm.getParentFrame().getLastUITime();
				boolean reset = kiosk && lastUI > 10 * 60 * 1000;
				// TODO: extract magic number
				if (reset || !Double.isNaN(settings.getBottomTime()) && settings.getLastBottomTimeSet() > 10 * 60 * 1000)
				{
					helicorderViewPanel.removeWaveInset();
					helicorderViewPanel.clearMarks();
					settings.setBottomTime(Double.NaN);
					if (kiosk && !Swarm.getParentFrame().isFullScreenMode())
						Swarm.getParentFrame().toggleFullScreenMode();
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
						if (dataSource.isActiveSource() || Double.isNaN(bt) || CurrentTime.getInstance().nowJ2K() < bt)
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
	
	private class SaveButtonActionListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e)
		{
			JFileChooser chooser = Swarm.getParentFrame().getFileChooser();
			File lastPath = new File(Swarm.getParentFrame().getConfig().getString("lastPath"));
			chooser.setCurrentDirectory(lastPath);
			
			JPanel imagePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			imagePanel.setBorder(new TitledBorder(new EtchedBorder(), "Image Properties"));
			
			JLabel heightLabel = new JLabel("Height:");
			JTextField heightTextField = new JTextField(4);
			heightLabel.setLabelFor(heightTextField);
			heightTextField.setText("700");
			
			JLabel widthLabel = new JLabel("Width:");
			JTextField widthTextField = new JTextField(4);
			widthLabel.setLabelFor(widthTextField);
			widthTextField.setText("900");
			
			JCheckBox includeChannel = new JCheckBox("Include channel");
			includeChannel.setSelected(true);

			imagePanel.add(heightLabel, GridBagHelper.set(c, "x=0;y=0;w=1;h=1;wx=1;wy=0;ix=12;iy=2;a=nw;f=n;i=0,4,0,4"));
			imagePanel.add(heightTextField, GridBagHelper.set(c, "x=1;y=0;w=1;h=1;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(widthLabel, GridBagHelper.set(c, "x=0;y=1;w=1;h=1;wx=1;wy=0;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(widthTextField, GridBagHelper.set(c, "x=1;y=1;w=1;h=1;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(includeChannel, GridBagHelper.set(c, "x=0;y=2;w=2;h=1;wx=1;wy=1;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));
			
			chooser.setAccessory(imagePanel);
			
			String fn = channel.replace(' ', '_') + ".png";
			chooser.setSelectedFile(new File (chooser.getCurrentDirectory().getAbsoluteFile(), fn));
			
			int result = chooser.showSaveDialog(Swarm.getParentFrame());
			if (result == JFileChooser.APPROVE_OPTION) 
			{						 
				File f = chooser.getSelectedFile();

				if (f.exists()) 
				{
					int choice = JOptionPane.showConfirmDialog(Swarm.getParentFrame(), "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
					if (choice != JOptionPane.YES_OPTION) 
						return;
			    }
			
				int width = -1;
				int height = -1;
				try
				{
					width = Integer.parseInt(widthTextField.getText());
					height = Integer.parseInt(heightTextField.getText());
				}
				catch (Exception ex) {}
				if (width <= 0 || height <= 0)
				{
					JOptionPane.showMessageDialog(HelicorderViewerFrame.this, "Illegal width or height.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				Plot plot = new Plot(width, height);

				Double end = settings.getBottomTime();
				if (Double.isNaN(end))
					end = CurrentTime.getInstance().nowJ2K();
				
				Double before = end - settings.span * 60;
				int tc = 30;
				
				HelicorderData heliData = dataSource.getHelicorder(channel, before - tc, end + tc);
				HelicorderRenderer heliRenderer = new HelicorderRenderer(heliData, settings.timeChunk);
								
				heliRenderer.setChannel(channel);
				heliRenderer.setLocation(HelicorderViewPanel.X_OFFSET, HelicorderViewPanel.Y_OFFSET, 
						width - HelicorderViewPanel.X_OFFSET - HelicorderViewPanel.RIGHT_WIDTH,
						height - HelicorderViewPanel.Y_OFFSET - HelicorderViewPanel.BOTTOM_HEIGHT);
				heliRenderer.setHelicorderExtents(before,end , -1 * Math.abs(settings.barRange), Math.abs(settings.barRange));
				heliRenderer.setTimeZoneOffset(Double.parseDouble(Swarm.getParentFrame().getConfig().getString("timeZoneOffset")));
				heliRenderer.setTimeZoneAbbr(Swarm.getParentFrame().getConfig().getString("timeZoneAbbr"));
				heliRenderer.setForceCenter(settings.forceCenter);
				heliRenderer.setClipBars(settings.clipBars);
				heliRenderer.setShowClip(settings.showClip);
				heliRenderer.setClipValue(settings.clipValue);
				heliRenderer.setChannel(channel);
				heliRenderer.setLargeChannelDisplay(includeChannel.isSelected());
				heliRenderer.createDefaultAxis();
				plot.addRenderer(heliRenderer);
				
				plot.writePNG(f.getAbsolutePath());
				Swarm.getParentFrame().getConfig().put("lastPath", f.getParent(), false);
			}
			
			chooser.setAccessory(null);
		}
	}
	

}