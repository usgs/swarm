package gov.usgs.swarm;
 
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.ui.GlobalKeyManager;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * The main UI and application class for Swarm.  Only functions directly 
 * pertaining to the UI and overall application operation belong here.
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.19  2006/04/11 17:55:14  dcervelli
 * Duration magnitude option.
 *
 * Revision 1.18  2006/04/08 18:15:16  cervelli
 * Made audible alerts off by default.
 *
 * Revision 1.17  2006/04/02 17:18:18  cervelli
 * Green lines banished, '.sac' extension no longer is automatically appended.
 *
 * Revision 1.16  2006/03/04 23:03:45  cervelli
 * Added alias feature. More thoroughly incorporated calibrations.  Got rid of 'waves' tab and combined all functionality under a 'channels' tab.
 *
 * Revision 1.15  2006/03/02 00:55:02  dcervelli
 * Added calibrations.
 *
 * Revision 1.14  2006/02/05 14:56:50  cervelli
 * Bumped version. Added info about NTP.config to the manual.
 *
 * Revision 1.13  2006/01/26 22:02:55  tparker
 * Add new config file defaults.
 *
 * Revision 1.12  2006/01/25 21:50:10  tparker
 * Cleanup imports
 *
 * Revision 1.11  2006/01/25 00:39:28  tparker
 * Move clipping alert into the heli renderer. In progress...
 *
 * Revision 1.10  2006/01/21 11:04:11  tparker
 * Apply alertClip settings
 *
 * Revision 1.9  2006/01/21 01:29:20  tparker
 * First swipe at adding voice alerting of clipping. A work in progress...
 *
 * Revision 1.8  2005/10/27 16:01:56  cervelli
 * Added release date to Swarm.java
 *
 * Revision 1.7  2005/10/27 15:39:27  dcervelli
 * Fixed showclip typo.
 *
 * Revision 1.6  2005/10/26 16:47:38  cervelli
 * Made showClip variable configurable.  Changed manually slightly.
 *
 * Revision 1.5  2005/10/01 16:16:30  dcervelli
 * Version bump.
 *
 * Revision 1.4  2005/09/23 21:58:02  dcervelli
 * Version bump.
 *
 * Revision 1.3  2005/09/22 21:00:50  dcervelli
 * Many changes (lastUITime, duration magnitudes, version bump, etc.).
 *
 * Revision 1.2  2005/09/02 16:40:17  dcervelli
 * CurrentTime changes.
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.12  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.11  2005/04/27 03:52:10  cervelli
 * Peter's configuration changes.
 *
 * Revision 1.10  2005/04/25 22:45:32  cervelli
 * 1.1.12 version bump.
 *
 * Revision 1.9  2005/04/11 00:26:11  cervelli
 * Don't use the stupid JDK 1.5 Swing theme.
 *
 * Revision 1.8  2005/03/28 17:11:20  cervelli
 * Final 1.1.10 version bump.
 *
 * Revision 1.7  2005/03/26 17:29:57  cervelli
 * "--sleep" option.
 *
 * Revision 1.6  2005/03/25 00:49:23  cervelli
 * Initial version to support WWS.
 *
 * Revision 1.5  2005/03/24 20:50:08  cervelli
 * User specified group config file; tile 4 helicorders to quadrants.
 *
 * Revision 1.4  2004/10/28 20:16:51  cvs
 * Big red mouse cursor support and version bump.
 *
 * Revision 1.3  2004/10/23 19:35:30  cvs
 * Version bump.
 *
 * Revision 1.2  2004/10/12 23:45:11  cvs
 * Bumped version, added log.
 *
 * @author Dan Cervelli
 */
public class Swarm extends JFrame
{
	private static final long serialVersionUID = -1;
	private static String CALIBRATION_CONFIG_FILE = "Calibration.config";
	private static Swarm application;
	private ConfigFile calibrations;
	private JDesktopPane desktop;
	private JSplitPane split;
	private DataChooser chooser;
	private JMenuBar menuBar;
	private CachedDataSource cache;
	private AboutDialog aboutDialog;
	private JLabel threadLabel;
	private int frameCount = 0;
	private int threadCount = 0;
	
	private WaveClipboardFrame waveClipboard;
	
	private static final String TITLE = "Swarm";
	private static final String VERSION = "1.3.0.20060411";
	
	private List<JInternalFrame> frames;
	private boolean fullScreen = false;
	private int oldState = 0;
	private Dimension oldSize;
	private Point oldLocation;
	private JFileChooser fileChooser;

	private Map<String, MultiMonitor> monitors;
	
	private AbstractAction toggleFullScreenAction;
	
	private long lastUITime;
	
	public static Config config;
	
	public Swarm(String[] args)
	{
		super(TITLE + " [" + VERSION + "]");

		monitors = new HashMap<String, MultiMonitor>();
		calibrations = new ConfigFile(CALIBRATION_CONFIG_FILE);
		cache = new CachedDataSource();
		frames = new ArrayList<JInternalFrame>();
		application = this;
		
		checkJavaVersion();
		loadFileChooser();
		setupGlobalKeys();
		config = Config.createConfig(args);
		createUI();
	}

	private void checkJavaVersion()
	{
		String version = System.getProperty("java.version");
		if (version.startsWith("1.1") || version.startsWith("1.2") || version.startsWith("1.3") || version.startsWith("1.4"))
		{
			JOptionPane.showMessageDialog(this, TITLE + " " + VERSION + " requires at least Java version 1.5 or above.", "Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
	}
	
	private void setupGlobalKeys()
	{
		// clean this up a bit and decide if I really want to use this ghkm thingy
		GlobalKeyManager m = GlobalKeyManager.getInstance();
		m.getInputMap().put(KeyStroke.getKeyStroke("F12"), "focus");
		m.getActionMap().put("focus", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
						System.out.println("Focus check: \n" + 
								"Current window: " + kfm.getFocusedWindow() + "\n\n" +
								"Current component: " + kfm.getFocusOwner() + "\n");	
					}
				});
				
		m.getInputMap().put(KeyStroke.getKeyStroke("alt F12"), "outputcache");
		m.getActionMap().put("outputcache", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						if (cache != null)
							cache.output();
					}
				});
				
		m.getInputMap().put(KeyStroke.getKeyStroke("control F12"), "flushcache");
		m.getActionMap().put("flushcache", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						if (cache != null)
							cache.flush();
					}
				});

		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullScreenToggle");
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, InputEvent.CTRL_DOWN_MASK), "fullScreenToggle");
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, InputEvent.CTRL_DOWN_MASK), "fullScreenToggle");
		toggleFullScreenAction = new AbstractAction()
		{
			private static final long serialVersionUID = -1;
		
			public void actionPerformed(ActionEvent e)
			{
				toggleFullScreenMode();					
				Swarm.this.requestFocus();
			}	
		};
		m.getActionMap().put("fullScreenToggle", toggleFullScreenAction);	
	}
	
	public void touchUITime()
	{
		lastUITime = System.currentTimeMillis();
	}
	
	public long getLastUITime()
	{
		return lastUITime;
	}
	
	private void loadFileChooser()
	{
		Thread t = new Thread(new Runnable() 
				{
					public void run()
					{
						fileChooser = new JFileChooser();
					}
				});
				
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	public JFileChooser getFileChooser()
	{
		int timeout = 10000;
		while (fileChooser == null && timeout > 0)
		{
			try { Thread.sleep(100); } catch (Exception e) {}
			timeout -= 100;
		}
		return fileChooser;
	}
	
	public static String getVersion()
	{
		return VERSION;
	}
	
	public static CachedDataSource getCache()
	{
		return application.cache;
	}
	
	public WaveClipboardFrame getWaveClipboard()
	{
		return waveClipboard;	
	}
	
	public Calibration getCalibration(String scn)
	{
		String c = calibrations.getString(scn);
		if (c == null)
			 return null;
		
		return Calibration.fromString(c);
	}
	
	public static Swarm getApplication()
	{
		return application;	
	}
	
	public static JSplitPane createStrippedSplitPane(int orient, JComponent comp1, JComponent comp2)
	{
		JSplitPane split = new JSplitPane(orient, comp1, comp2);
		split.setBorder(BorderFactory.createEmptyBorder());
		SplitPaneUI splitPaneUI = split.getUI();
	    if (splitPaneUI instanceof BasicSplitPaneUI)
	    {
	        BasicSplitPaneUI basicUI = (BasicSplitPaneUI)splitPaneUI;
	        basicUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
	    }
	    return split;
	}
	
	public void createUI()
	{
		this.addWindowListener(new WindowAdapter()
				{
					public void windowClosing(WindowEvent e)
					{
						closeApp();
					}
				});
		this.addFocusListener(new FocusListener()
				{
					public void focusGained(FocusEvent e)
					{
						// The main Swarm window has no need for the focus.  If it gets it 
						// then it attempts to pass it on to the first helicorder, failing
						// that it gives it to the first wave.
						if (frames != null && frames.size() > 0)
						{
							JInternalFrame jf = null;
							for (int i = 0; i < frames.size(); i++)
							{
								JInternalFrame f = (JInternalFrame)frames.get(i);
								if (f instanceof HelicorderViewerFrame)
								{
									jf = f;
									break;
								}
							}
							if (jf == null)
								jf = (JInternalFrame)frames.get(0);
							jf.requestFocus();
						}
					}
					
					public void focusLost(FocusEvent e)
					{}
				});
		
		desktop = new JDesktopPane();
		desktop.setBorder(BorderFactory.createLineBorder(DataChooser.LINE_COLOR));
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
		// disable dragging in fullscreen mode
		desktop.setDesktopManager(new DefaultDesktopManager()
				{
					private static final long serialVersionUID = -1;
					public void beginDraggingFrame(JComponent f)
					{
						if (fullScreen)
							return;
						else
							super.beginDraggingFrame(f);
					}
					
					public void dragFrame(JComponent f, int x, int y)
					{
						if (fullScreen)
							return;
						else
							super.dragFrame(f, x, y);
					}
				});
		
		this.setSize(config.windowWidth, config.windowHeight);
		this.setLocation(config.windowX, config.windowY);
		if (config.windowMaximized)
			this.setExtendedState(Frame.MAXIMIZED_BOTH);
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		chooser = new DataChooser();
		split = createStrippedSplitPane(JSplitPane.HORIZONTAL_SPLIT, chooser, desktop);
		split.setDividerLocation(config.chooserDividerLocation);
		split.setDividerSize(4);
		this.setContentPane(split);	
		
		menuBar = new JMenuBar();
//		menuBar.setBorder(BorderFactory.createEmptyBorder());
		JMenu fileMenu = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						closeApp();
					}
				});
		fileMenu.add(exit);
		JMenu editMenu = new JMenu("Edit");
		JMenuItem options = new JMenuItem("Options...");
		options.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						OptionsDialog od = new OptionsDialog();
						od.setVisible(true);
					}
				});
		editMenu.add(options);
		JMenu windowMenu = new JMenu("Window");
		JMenuItem tileHelis = new JMenuItem("Tile Helicorders");
		tileHelis.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						tileHelicorders();
					}
				});
		windowMenu.add(tileHelis);
		JMenuItem tileWaves = new JMenuItem("Tile Waves");
		tileWaves.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						tileWaves();
					}
				});
		windowMenu.add(tileWaves);
		windowMenu.addSeparator();
		JMenuItem fullScreenItem = new JMenuItem("Kiosk Mode");
		fullScreenItem.addActionListener(toggleFullScreenAction);
		fullScreenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		windowMenu.add(fullScreenItem);
		JMenu helpMenu = new JMenu("Help");
		JMenuItem about = new JMenuItem("About...");
		aboutDialog = new AboutDialog();
		about.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						aboutDialog.update();
						aboutDialog.setVisible(true);
					}
				});
				
		helpMenu.add(about);
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(windowMenu);
		menuBar.add(helpMenu);
		menuBar.add(Box.createHorizontalGlue());
		threadLabel = new JLabel(" ");
		menuBar.add(threadLabel);
		this.setJMenuBar(menuBar);
		
		waveClipboard = new WaveClipboardFrame();
		desktop.add(waveClipboard);
		
		this.setVisible(true);
		long offset = CurrentTime.getInstance().getOffset();
		if (Math.abs(offset) > 10 * 60 * 1000)
			JOptionPane.showMessageDialog(this, "You're system clock is off by more than 10 minutes.\n" + 
					"This is just for your information, Swarm will not be affected by this.", "System Clock", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public boolean isFullScreenMode()
	{
		return fullScreen;	
	}
	
	public void toggleFullScreenMode()
	{
		fullScreen = !fullScreen;
		setFullScreenMode(fullScreen);
	}
	
	private void setFullScreenMode(boolean full)
	{
		this.dispose();
		this.setUndecorated(full);
		this.setResizable(!full);
		waveClipboard.setVisible(!full);
		waveClipboard.toBack();
		
		if (full)
		{
			this.setJMenuBar(null);
			config.chooserDividerLocation = split.getDividerLocation();
			oldState = this.getExtendedState();
			oldSize = this.getSize();
			oldLocation = this.getLocation();
			this.setContentPane(desktop);
			this.setVisible(true);
			this.setExtendedState(Frame.MAXIMIZED_BOTH);
			desktop.setSize(this.getSize());
			desktop.setPreferredSize(this.getSize());
		}
		else
		{
			this.setJMenuBar(menuBar);
			this.setExtendedState(oldState);
			this.setSize(oldSize);
			this.setLocation(oldLocation);
			split.setRightComponent(desktop);
			split.setDividerLocation(config.chooserDividerLocation);
			this.setContentPane(split);
		}
		validate();
		this.setVisible(true);
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof HelicorderViewerFrame)
			{
				HelicorderViewerFrame f = (HelicorderViewerFrame)frame; 
				f.setFullScreen(full);
			}
		}
		tileHelicorders();
	}
	
	public void closeApp()
	{
		Point p = this.getLocation();

		if (this.getExtendedState() == Frame.MAXIMIZED_BOTH)
			config.windowMaximized = true;
		else
		{
			Dimension d = this.getSize();
			config.windowX = p.x;
			config.windowY = p.y;
			config.windowWidth = d.width;
			config.windowHeight = d.height;
			config.windowMaximized = false;
		}

		config.chooserDividerLocation = split.getDividerLocation();
		
		if (config.saveConfig)
		{
			ConfigFile configFile = config.toConfigFile();
			configFile.remove("configFile");
			configFile.writeToFile(config.configFilename);
		}
  
		waveClipboard.removeWaves();
		try
		{
			for (JInternalFrame frame : frames)
				frame.setClosed(true);
		}
		catch (Exception e) {} // doesn't matter at this point
		System.exit(0);
	}

	// TODO: move these functions
	public SeismicDataSource parseDataSource(String abbrSource)
	{
		String source = config.getServer(abbrSource);
		return SeismicDataSource.getDataSource(source);
	}
	
//	public void dataSourceSelected(final String source)
//	{
//		final SwingWorker worker = new SwingWorker()
//				{
//					private List hs;
//					private boolean failed;
//					
//					public Object construct()
//					{
//						incThreadCount();
//						try
//						{
//							SeismicDataSource sds = parseDataSource(source);
//							if (sds != null)
//							{
////								ws = sds.getWaveStations();
//								hs = sds.getHelicorderStations();
//								sds.close();
//							}
//							else 
//								failed = true;
//						} 
//						catch (Exception e)
//						{
//							failed = true;
//						}
//						return null;	
//					}			
//					
//					public void finished()
//					{
//						if (!failed)
//						{
//	//						channelPanel.populateWaveChannels(source, ws);
//							channelPanel.populateHelicorderChannels(source, hs);
//						}
//						chooser.enableGoButton();
//						decThreadCount();
//					}
//				};
//		worker.start();
//	}

	public void clipboardWaveChannelSelected(final String source, final String[] channels)
	{
		final SeismicDataSource sds = parseDataSource(source);
		for (int i = 0; i < channels.length; i++)
			loadClipboardWave(sds, channels[i]);
	}
	
	public void clipboardWaveChannelSelected(final String source, final String channel)
	{
		final SeismicDataSource sds = parseDataSource(source);
		loadClipboardWave(sds, channel);
	}
	
	private void loadClipboardWave(final SeismicDataSource source, final String channel)
	{
		final WaveViewPanel wvp = new WaveViewPanel();
		wvp.setChannel(channel);
		wvp.setDataSource(source);
		ClipboardWaveViewPanel cwvp = waveClipboard.getSelected();
		double st = 0;
		double et = 0;
		if (cwvp == null)
		{
			double now = CurrentTime.getInstance().nowJ2K();
			st = now - 180;
			et = now;
		}
		else
		{
			st = cwvp.getWaveViewPanel().getStartTime();	
			et = cwvp.getWaveViewPanel().getEndTime();
		}
		final double fst = st;
		final double fet = et;
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						incThreadCount();
//						double now = CurrentTime.nowJ2K();
						Wave sw = source.getWave(channel, fst, fet);
						wvp.setWave(sw, fst, fet);
						return null;
					}
					
					public void finished()
					{
						waveClipboard.toFront();
						try
						{
							waveClipboard.setSelected(true);
						}
						catch (Exception e) {}
						waveClipboard.addWave(new ClipboardWaveViewPanel(wvp));
						decThreadCount();
					}
				};
		worker.start();
	}
	
	public void monitorChannelSelected(String source, String channel)
	{
		MultiMonitor monitor = (MultiMonitor)monitors.get(source);
		if (monitor == null)
		{
			SeismicDataSource sds = parseDataSource(source);
			monitor = new MultiMonitor(sds);
			monitors.put(source, monitor);
			addInternalFrame(monitor);
		}
	
		if (!monitor.isVisible())
			monitor.setVisible(true);
		
		monitor.addChannel(channel);
	}
	
	public WaveViewerFrame waveChannelSelected(String source, String channel)
	{
		SeismicDataSource sds = parseDataSource(source);
		WaveViewerFrame frame = new WaveViewerFrame(this, sds, channel);
		addInternalFrame(frame);
		return frame;
	}
	
	public HelicorderViewerFrame helicorderChannelSelected(String source, String channel)
	{
		SeismicDataSource sds = parseDataSource(source);
		HelicorderViewerFrame frame = new HelicorderViewerFrame(this, sds, channel);
		addInternalFrame(frame);
		return frame;
	}
	
	public void removeInternalFrame(final JInternalFrame f)
	{
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						frames.remove(f);
						if (frameCount > 0)
							frameCount--;
					}
				});			
	}
	
	public void addInternalFrame(final JInternalFrame f)
	{
		frames.add(f);
		frameCount++;			
		frameCount = frameCount % 10;
		f.setLocation(frameCount * 30, frameCount * 30);
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						desktop.add(f);
						f.toFront();
						try
						{
							f.setSelected(true);
						}
						catch (Exception e) {}
					}
				});
	}
	
	public void tileHelicorders()
	{
		Dimension ds = desktop.getSize();

		ArrayList<HelicorderViewerFrame> hcs = new ArrayList<HelicorderViewerFrame>(10);
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof HelicorderViewerFrame)
			    hcs.add((HelicorderViewerFrame)frame);
		}
		
		if (hcs.size() == 0)
			return;
		
		if (hcs.size() == 4)
		{
		    int w = ds.width / 2;
		    int h = ds.height / 2;
		    HelicorderViewerFrame hvf0 = (HelicorderViewerFrame)hcs.get(0);
		    HelicorderViewerFrame hvf1 = (HelicorderViewerFrame)hcs.get(1);
		    HelicorderViewerFrame hvf2 = (HelicorderViewerFrame)hcs.get(2);
		    HelicorderViewerFrame hvf3 = (HelicorderViewerFrame)hcs.get(3);
		    hvf0.setSize(w, h);
		    hvf0.setLocation(0, 0);
		    hvf1.setSize(w, h);
		    hvf1.setLocation(w, 0);
		    hvf2.setSize(w, h);
		    hvf2.setLocation(0, h);
		    hvf3.setSize(w, h);
		    hvf3.setLocation(w, h);
		}
		else
		{
		    int w = ds.width / hcs.size();
			int cx = 0;
			for (int i = 0; i < hcs.size(); i++)
			{
				HelicorderViewerFrame hvf = (HelicorderViewerFrame)hcs.get(i);
				try 
				{ 
					hvf.setIcon(false);
					hvf.setMaximum(false);
				}
				catch (Exception e) {}
				hvf.setSize(w, ds.height);
				hvf.setLocation(cx, 0);
				cx += w;
			}
		}
	}
	
	public void tileWaves()
	{
		Dimension ds = desktop.getSize();
		
		int wc = 0;
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof WaveViewerFrame)
				wc++;	
		}
		
		if (wc == 0)
			return; 
			
		int h = ds.height / wc;
		int cy = 0;
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof WaveViewerFrame)
			{
				WaveViewerFrame wvf = (WaveViewerFrame)frame;
				try 
				{ 
					wvf.setIcon(false);
					wvf.setMaximum(false);
				}
				catch (Exception e) {}
				wvf.setSize(ds.width, h);
				wvf.setLocation(0, cy);
				cy += h;
			}
		}
	}

	public void updateThreadLabel()
	{
		if (threadCount == 0)
			threadLabel.setText(" ");
		else if (threadCount == 1)
			threadLabel.setText("1 thread ");
		else
			threadLabel.setText(threadCount + " threads ");
	}

	public void incThreadCount()
	{
		threadCount++;	
		updateThreadLabel();
	}
	
	public void decThreadCount()
	{
		threadCount--;	
		updateThreadLabel();
	}
	
	public void parseKiosk()
	{
		String[] kiosks = config.kiosk.split(",");
		for (int i = 0; i < kiosks.length; i++)
		{ 
			String[] ch = kiosks[i].split(";");
			helicorderChannelSelected(ch[0], ch[1]);
		}
		toggleFullScreenMode();
	}
	
	public void optionsChanged()
	{
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof HelicorderViewerFrame)
			{
				HelicorderViewerFrame hvf = (HelicorderViewerFrame)frame;
				hvf.getHelicorderViewPanel().cursorChanged();
			}
		}
	}
	
	public static void main(String[] args)
	{
		try 
		{
			// JDK 1.5 by default has an ugly theme, this line uses the one from 1.4
//			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
//			UIManager.setLookAndFeel("net.java.plaf.windows.WindowsLookAndFeel");
			MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
		}
		catch (Exception e) { }
		
		Swarm swarm = new Swarm(args);

		if (Swarm.config.isKiosk())
			swarm.parseKiosk();
	}
}