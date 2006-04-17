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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
 * TODO: slider tooltip
 * TODO: change slider checkbox
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.14  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.13  2006/03/04 23:03:45  cervelli
 * Added alias feature. More thoroughly incorporated calibrations.  Got rid of 'waves' tab and combined all functionality under a 'channels' tab.
 *
 * Revision 1.12  2006/01/21 11:04:11  tparker
 * Apply alertClip settings
 *
 * Revision 1.11  2005/11/29 00:45:34  tparker
 * Add PS option to "save image"
 *
 * Revision 1.10  2005/09/22 20:57:16  dcervelli
 * Helicorder screenshot cleaned up a bit, autoscale slider size fixed.
 *
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
	private JToolBar toolBar;
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
	private JFileChooser chooser;
	protected JToggleButton autoScaleSliderButton;
	protected int autoScaleSliderButtonState;
	protected JSlider autoScaleSlider;

	private HelicorderViewPanel helicorderViewPanel;
	
	private WaveViewSettings waveViewSettings;
	private HelicorderViewerSettings settings;
	
	private boolean working;
	private JLabel statusLabel;
	
	private boolean fullScreen;
	
	private JComponent northPane;
	private Dimension oldNorthPaneSize;
	
	private JPanel heliPanel;
	private WigglerPanel wigglerPanel;
	
	protected long lastRefreshTime;
	
	private Border border;
	
	protected Throbber throbber;
	
	public HelicorderViewerFrame(Swarm sw, SeismicDataSource sds, String ch)
	{
		super(ch + ", [" + sds + "]", true, true, true, true);
		Swarm.getApplication().touchUITime();
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
		mainPanel = new JPanel(new BorderLayout());
		createHeliPanel();
		createToolBar();
		createStatusLabel();
		createListeners();
		
		setFrameIcon(Images.getIcon("heli"));
		setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		setSize(800, 750);
		setContentPane(mainPanel);
		setVisible(true);
	}

	private void createStatusLabel()
	{
		statusLabel = new JLabel(" ");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
		mainPanel.add(statusLabel, BorderLayout.SOUTH);
	}
	
	private void createHeliPanel()
	{
		helicorderViewPanel = new HelicorderViewPanel(this);
		settings.view = helicorderViewPanel;
		heliPanel = new JPanel(new BorderLayout());
		border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 3), 
				LineBorder.createGrayLineBorder());
		heliPanel.setBorder(border);
		heliPanel.add(helicorderViewPanel, BorderLayout.CENTER);
		mainPanel.add(heliPanel, BorderLayout.CENTER);
	}
	
	private void createToolBar()
	{
		toolBar = SwarmUtil.createToolBar();
		
		settingsButton = SwarmUtil.createToolBarButton(
				Images.getIcon("settings"),
				"Helicorder view settings",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						HelicorderViewerSettingsDialog hvsd = HelicorderViewerSettingsDialog.getInstance(settings, waveViewSettings);
						hvsd.setVisible(true);
						getHelicorder();
					}
				});
		toolBar.add(settingsButton);
		
		toolBar.addSeparator();
		
		backButton = SwarmUtil.createToolBarButton(
				Images.getIcon("left"),
				"Scroll back time (A or Left arrow)",						
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (helicorderViewPanel.hasInset())
							helicorderViewPanel.moveInset(-1);
						else
							scroll(-1);
					}
				});
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
		Util.mapKeyStrokeToButton(this, "A", "backward2", backButton);
		toolBar.add(backButton);
		
		forwardButton = SwarmUtil.createToolBarButton(
				Images.getIcon("right"),
				"Scroll forward time (Z or Right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (helicorderViewPanel.hasInset())
							helicorderViewPanel.moveInset(1);
						else
							scroll(1);
					}
				});
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);				
		Util.mapKeyStrokeToButton(this, "Z", "forward2", forwardButton);
		toolBar.add(forwardButton);
		
		compX = SwarmUtil.createToolBarButton(
				Images.getIcon("xminus"),
				"Compress X-axis (Alt-left arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						decXAxis();
						getHelicorder();
					}
				});
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compX);
		toolBar.add(compX);
		
		expX = SwarmUtil.createToolBarButton(
				Images.getIcon("xplus"),
				"Expand X-axis (Alt-right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						incXAxis();
						getHelicorder();
					}
				});
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expX);
		toolBar.add(expX);
		
		compY = SwarmUtil.createToolBarButton(
				Images.getIcon("yminus"),
				"Compress Y-axis (Alt-down arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						decYAxis();
						getHelicorder();
					}
				});
		Util.mapKeyStrokeToButton(this, "alt DOWN", "compy", compY);
		toolBar.add(compY);
		
		expY = SwarmUtil.createToolBarButton(
				Images.getIcon("yplus"),
				"Expand Y-axis (Alt-up arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						incYAxis();
						getHelicorder();
					}
				});
		Util.mapKeyStrokeToButton(this, "alt UP", "expy", expY);
		toolBar.add(expY);
		
		toolBar.addSeparator();
		
		JButton addZoom = SwarmUtil.createToolBarButton(
				Images.getIcon("zoomplus"),
				"Decrease zoom time window (+)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						decZoom();
						settings.notifyView();
					}
				});
		Util.mapKeyStrokeToButton(this, "EQUALS", "addzoom1", addZoom);
		Util.mapKeyStrokeToButton(this, "shift EQUALS", "addzoom2", addZoom);
		toolBar.add(addZoom);
		
		JButton subZoom = SwarmUtil.createToolBarButton(
				Images.getIcon("zoomminus"),
				"Increase zoom time window (-)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						incZoom();
						settings.notifyView();
					}
				});
		Util.mapKeyStrokeToButton(this, "MINUS", "subzoom", subZoom);
		toolBar.add(subZoom);

		new WaveViewSettingsToolbar(waveViewSettings, toolBar, this);
		
		clipboard = SwarmUtil.createToolBarButton(
				Images.getIcon("clipboard"),
				"Copy inset to clipboard (C or Ctrl-C)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						helicorderViewPanel.insetToClipboard();
					}
				});
		clipboard.setEnabled(false);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard1", clipboard);
		Util.mapKeyStrokeToButton(this, "C", "clipboard2", clipboard);
		toolBar.add(clipboard);
		
		removeWave = SwarmUtil.createToolBarButton(
				Images.getIcon("delete"),
				"Remove inset wave (Delete or Escape)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						helicorderViewPanel.removeWaveInset();
					}
				});
		removeWave.setEnabled(false);
		Util.mapKeyStrokeToButton(this, "ESCAPE", "removewave", removeWave);
		Util.mapKeyStrokeToButton(this, "DELETE", "removewave", removeWave);
		toolBar.add(removeWave);

		toolBar.addSeparator();
		
		saveWave = SwarmUtil.createToolBarButton(
				Images.getIcon("camera"),
				"Save helicorder image",
				new SaveButtonActionListener());
		toolBar.add(saveWave);

		toolBar.addSeparator();
		
		autoScaleSliderButton = new JCheckBox(Images.getIcon("wavezoom"));
		autoScaleSliderButton.setFocusable(false);
		autoScaleSliderButton.setSelectedIcon(Images.getIcon("waveclip"));
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
		toolBar.add(autoScaleSliderButton);

		autoScaleSlider = new JSlider(1, 39, (int) (10 - settings.barMult) * 4);
		autoScaleSlider.setFocusable(false);
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
		toolBar.add(autoScaleSlider);
		
		toolBar.add(Box.createHorizontalGlue());
		throbber = new Throbber();
		toolBar.add(throbber);
		mainPanel.add(toolBar, BorderLayout.NORTH);
	}
	
	private void createListeners()
	{
		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameClosing(InternalFrameEvent e)
					{
						dispose();
						throbber.close();
						refreshThread.kill();
						Swarm.getApplication().removeInternalFrame(HelicorderViewerFrame.this);
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
		int index = SwarmUtil.linearSearch(chunkValues, settings.timeChunk);
		if (index == -1 || index == chunkValues.length - 1)
			return;
		settings.timeChunk = chunkValues[index + 1];
	}
	
	public void decXAxis()
	{
		int index = SwarmUtil.linearSearch(chunkValues, settings.timeChunk);
		if (index == -1 || index == 0)
			return;
		settings.timeChunk = chunkValues[index - 1];
	}
	
	public void incYAxis()
	{
		int index = SwarmUtil.linearSearch(spanValues, settings.span);
		if (index == -1 || index == spanValues.length - 1)
			return;
		settings.span = spanValues[index + 1];
	}
	
	public void decYAxis()
	{
		int index = SwarmUtil.linearSearch(spanValues, settings.span);
		if (index == -1 || index == 0)
			return;
		settings.span = spanValues[index - 1];
	}
	
	public void incZoom()
	{
		int index = SwarmUtil.linearSearch(zoomValues, settings.waveZoomOffset);
		if (index == -1 || index == zoomValues.length - 1)
			return;
		settings.waveZoomOffset = zoomValues[index + 1];
	}
	
	public void decZoom()
	{ 
		int index = SwarmUtil.linearSearch(zoomValues, settings.waveZoomOffset);
		if (index == -1 || index == 0)
			return;
		settings.waveZoomOffset = zoomValues[index - 1];
	}
	
	public void setInsetButtonsEnabled(final boolean b)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						clipboard.setEnabled(b);
						removeWave.setEnabled(b);				
					}
				});
	}
	
	public void setNavigationButtonsEnabled(final boolean b)
	{
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
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
				});
	}

	public void setFullScreen(boolean full)
	{
		System.out.println("evd: " + SwingUtilities.isEventDispatchThread());
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
			mainPanel.remove(toolBar);
			heliPanel.setBorder(null);
		}
		else
		{
			northPane.setVisible(true);
			northPane.setPreferredSize(oldNorthPaneSize);
			mainPanel.add(toolBar, BorderLayout.NORTH);
			heliPanel.setBorder(border);
		}	
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
	
	public Throbber getThrobber()
	{
		return throbber;
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
	    					throbber.increment();
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
						throbber.decrement();
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

	private class RefreshThread extends Thread
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
			Swarm.getApplication().incThreadCount();
			while (!kill)
			{
				long lastUI = System.currentTimeMillis() - Swarm.getApplication().getLastUITime();
				boolean reset = Swarm.config.isKiosk() && lastUI > 10 * 60 * 1000;
				// TODO: extract magic number
				if (reset || !Double.isNaN(settings.getBottomTime()) && settings.getLastBottomTimeSet() > 10 * 60 * 1000)
				{
					helicorderViewPanel.removeWaveInset();
					helicorderViewPanel.clearMarks();
					settings.setBottomTime(Double.NaN);
					if (Swarm.config.isKiosk() && !Swarm.getApplication().isFullScreenMode())
						Swarm.getApplication().toggleFullScreenMode();
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
			Swarm.getApplication().decThreadCount();
		}	
	}
	
	private class SaveButtonActionListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e)
		{
			chooser = Swarm.getApplication().getFileChooser();
			File lastPath = new File(Swarm.config.lastPath);
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
			
			JLabel fileFormatLabel = new JLabel("File format:");
			JComboBox fileFormatCB = new JComboBox();
			fileFormatCB.addItem("PNG");
			fileFormatCB.addItem("PS");
			
			fileFormatCB.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							JComboBox source = (JComboBox) e.getSource();
							if (source.getSelectedItem().equals("PS")) {
								String fn = chooser.getSelectedFile().getName().replaceAll("\\..*$", ".ps");
								chooser.setSelectedFile(new File (chooser.getCurrentDirectory().getAbsoluteFile(), fn));			
							} else {
								String fn = chooser.getSelectedFile().getName().replaceAll("\\..*$", ".png");
								chooser.setSelectedFile(new File (chooser.getCurrentDirectory().getAbsoluteFile(), fn));			
							}
						}
					});

			imagePanel.add(heightLabel, GridBagHelper.set(c, "x=0;y=0;w=1;h=1;wx=1;wy=0;ix=12;iy=2;a=nw;f=n;i=0,4,0,4"));
			imagePanel.add(heightTextField, GridBagHelper.set(c, "x=1;y=0;w=1;h=1;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(widthLabel, GridBagHelper.set(c, "x=0;y=1;w=1;h=1;wx=1;wy=0;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(widthTextField, GridBagHelper.set(c, "x=1;y=1;w=1;h=1;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(fileFormatLabel, GridBagHelper.set(c, "x=0;y=2;w=1;h=1;wx=1;wy=0;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));
			imagePanel.add(fileFormatCB, GridBagHelper.set(c, "x=1;y=2;w=1;h=1;wx=1;wy=1;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));
			imagePanel.add(includeChannel, GridBagHelper.set(c, "x=0;y=3;w=2;h=1;wx=1;wy=1;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));

			chooser.setAccessory(imagePanel);
			
			String fn = channel.replace(' ', '_') + ".png";
			chooser.setSelectedFile(new File (chooser.getCurrentDirectory().getAbsoluteFile(), fn));
			
			int result = chooser.showSaveDialog(Swarm.getApplication());
			if (result == JFileChooser.APPROVE_OPTION) 
			{						 
				File f = chooser.getSelectedFile();

				if (f.exists()) 
				{
					int choice = JOptionPane.showConfirmDialog(Swarm.getApplication(), "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
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
				heliRenderer.setTimeZoneOffset(Swarm.config.timeZoneOffset);
				heliRenderer.setTimeZoneAbbr(Swarm.config.timeZoneAbbr);
				heliRenderer.setForceCenter(settings.forceCenter);
				heliRenderer.setClipBars(settings.clipBars);
				heliRenderer.setShowClip(settings.showClip);
				heliRenderer.setClipValue(settings.clipValue);
				heliRenderer.setChannel(channel);
				heliRenderer.setLargeChannelDisplay(includeChannel.isSelected());
				heliRenderer.createDefaultAxis();
				plot.addRenderer(heliRenderer);
				
				if (fileFormatCB.getSelectedItem().equals("PS"))
					plot.writePS(f.getAbsolutePath());					
				else
					plot.writePNG(f.getAbsolutePath());
			
				Swarm.config.lastPath = f.getParent();
			}
			
			chooser.setAccessory(null);
		}
	}
	

}