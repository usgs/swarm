package gov.usgs.swarm;
 
import gov.usgs.swarm.chooser.DataChooser;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.heli.HelicorderViewerFrame;
import gov.usgs.swarm.map.MapFrame;
import gov.usgs.swarm.wave.MultiMonitor;
import gov.usgs.swarm.wave.WaveClipboardFrame;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.swarm.wave.WaveViewerFrame;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Log;
import gov.usgs.util.ui.GlobalKeyManager;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

/**
 * The main UI and application class for Swarm.  Only functions directly 
 * pertaining to the UI and overall application operation belong here.
 *
 * TODO: resize listener
 * TODO: chooser visibility
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.26  2006/08/01 23:41:32  cervelli
 * Many 2.0 changes.
 *
 * Revision 1.25  2006/07/30 22:43:03  cervelli
 * Changes for layouts.
 *
 * Revision 1.24  2006/07/22 20:30:05  cervelli
 * Map, time zones, time listeners.
 *
 * Revision 1.23  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.22  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.21  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.20  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
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
	private static Swarm application;
	private JDesktopPane desktop;
	private JSplitPane split;
	private DataChooser chooser;
	private SwarmMenu swarmMenu;
	private CachedDataSource cache;
	private int frameCount = 0;
	
	private WaveClipboardFrame waveClipboard;
	private MapFrame mapFrame;
	
	private static final String TITLE = "Swarm";
	private static final String VERSION = "2.0.0.20060804-alpha-1";
	
	private List<JInternalFrame> frames;
	private boolean fullScreen = false;
	private int oldState = 0;
	private Dimension oldSize;
	private Point oldLocation;
	private JFileChooser fileChooser;

	private Map<String, MultiMonitor> monitors;
	
	private AbstractAction toggleFullScreenAction;
	
	private long lastUITime;
	
	private EventListenerList timeListeners = new EventListenerList();
	
	public static Config config;
	
	public static Logger logger;
	
	public Swarm(String[] args)
	{
		super(TITLE + " [" + VERSION + "]");
		logger = Log.getLogger("gov.usgs.swarm");
		setIconImage(Images.getIcon("swarm").getImage());

		monitors = new HashMap<String, MultiMonitor>();
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
		logger.fine("java.version: " + version);
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
				
//		m.getInputMap().put(KeyStroke.getKeyStroke("control F12"), "flushcache");
//		m.getActionMap().put("flushcache", new AbstractAction()
//				{
//					private static final long serialVersionUID = -1;
//					public void actionPerformed(ActionEvent e)
//					{
//						if (cache != null)
//							cache.flush();
//					}
//				});

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
	
	public MapFrame getMapFrame()
	{
		return mapFrame;
	}
	
	public DataChooser getDataChooser()
	{
		return chooser;
	}
	
	public static Swarm getApplication()
	{
		return application;	
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
								JInternalFrame f = frames.get(i);
								if (f instanceof HelicorderViewerFrame)
								{
									jf = f;
									break;
								}
							}
							if (jf == null)
								jf = frames.get(0);
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
		split = SwarmUtil.createStrippedSplitPane(JSplitPane.HORIZONTAL_SPLIT, chooser, desktop);
		split.setDividerLocation(config.chooserDividerLocation);
		split.setDividerSize(4);
		setChooserVisible(config.chooserVisible);
		chooser.setDividerLocation(config.nearestDividerLocation);
		
		waveClipboard = new WaveClipboardFrame();
		desktop.add(waveClipboard);
		waveClipboard.setVisible(config.clipboardVisible);
		
		mapFrame = new MapFrame();
		desktop.add(mapFrame);
		mapFrame.setVisible(config.mapVisible);
		
		swarmMenu = new SwarmMenu();
		this.setJMenuBar(swarmMenu);
		
		for (SwarmLayout sl : config.layouts)
			swarmMenu.addLayout(sl);
		
		this.setVisible(true);
		
		long offset = CurrentTime.getInstance().getOffset();
		if (Math.abs(offset) > 10 * 60 * 1000)
			JOptionPane.showMessageDialog(this, "You're system clock is off by more than 10 minutes.\n" + 
					"This is just for your information, Swarm will not be affected by this.", "System Clock", JOptionPane.INFORMATION_MESSAGE);
	}

	public void addTimeListener(TimeListener tl)
	{
		timeListeners.add(TimeListener.class, tl);
	}
	
	public void removeTimeListener(TimeListener tl)
	{
		timeListeners.remove(TimeListener.class, tl);
	}
	
	public void fireTimeChanged(double j2k)
	{
		Object[] ls = timeListeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
		    if (ls[i] == TimeListener.class)
		        ((TimeListener)ls[i + 1]).timeChanged(j2k);
	}
	
	public void setChooserVisible(boolean vis)
	{
		if (vis)
		{
			split.setRightComponent(desktop);
			split.setDividerLocation(config.chooserDividerLocation);
			setContentPane(split);
		}
		else
		{
			if (isChooserVisible())
				config.chooserDividerLocation = split.getDividerLocation();
			setContentPane(desktop);
		}
		if (SwingUtilities.isEventDispatchThread())
			validate();
	}
	
	public boolean isChooserVisible()
	{
		return getContentPane() == split;
	}
	
	public boolean isMapVisible()
	{
		return mapFrame.isVisible();
	}
	
	public void setMapVisible(boolean vis)
	{
		mapFrame.setVisible(vis);
		
		if (vis)
			mapFrame.toFront();
	}
	
	public boolean isClipboardVisible()
	{
		return waveClipboard.isVisible();
	}
	
	public void setClipboardVisible(boolean vis)
	{
		waveClipboard.setVisible(vis);
		
		if (vis)
			waveClipboard.toFront();
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
			this.setJMenuBar(swarmMenu);
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

		if (waveClipboard.isMaximum())
			config.clipboardMaximized = true;
		else
		{
			config.clipboardVisible = isClipboardVisible();
			config.clipboardX = waveClipboard.getX();
			config.clipboardY = waveClipboard.getY();
			config.clipboardWidth = waveClipboard.getWidth();
			config.clipboardHeight = waveClipboard.getHeight();
			config.clipboardMaximized = false;
		}
		
		if (mapFrame.isMaximum())
			config.mapMaximized = true;
		else
		{
			config.mapVisible = mapFrame.isVisible();
			config.mapX = mapFrame.getX();
			config.mapY = mapFrame.getY();
			config.mapWidth = mapFrame.getWidth();
			config.mapHeight = mapFrame.getHeight();
			config.mapMaximized = false;
		}
		
		config.chooserDividerLocation = split.getDividerLocation();
		config.chooserVisible = isChooserVisible();
		
		config.nearestDividerLocation = chooser.getDividerLocation();
		
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

	public void loadClipboardWave(final SeismicDataSource source, final String channel)
	{
		final WaveViewPanel wvp = new WaveViewPanel();
		wvp.setChannel(channel);
		wvp.setDataSource(source);
		WaveViewPanel cwvp = waveClipboard.getSelected();
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
			st = cwvp.getStartTime();	
			et = cwvp.getEndTime();
		}
		final double fst = st;
		final double fet = et;
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
//						double now = CurrentTime.nowJ2K();
						waveClipboard.getThrobber().increment();
						Wave sw = source.getWave(channel, fst, fet);
						wvp.setWave(sw, fst, fet);
						return null;
					}
					
					public void finished()
					{
						waveClipboard.getThrobber().decrement();
						waveClipboard.setVisible(true);
						waveClipboard.toFront();
						try
						{
							waveClipboard.setSelected(true);
						}
						catch (Exception e) {}
						waveClipboard.addWave(wvp);
					}
				};
		worker.start();
	}

	public void removeMonitor(MultiMonitor mm)
	{
		removeInternalFrame(mm);
		monitors.remove(mm.getDataSource().getName());
	}
	
	public void monitorChannelSelected(SeismicDataSource source, String channel)
	{
		MultiMonitor monitor = monitors.get(source.getName());
		if (monitor == null)
		{
			monitor = new MultiMonitor(source);
			monitors.put(source.getName(), monitor);
			addInternalFrame(monitor);
			monitor.setVisible(true);
		}
		
		monitor.addChannel(channel);
	}

	public WaveViewerFrame openRealtimeWave(SeismicDataSource source, String channel)
	{
		WaveViewerFrame frame = new WaveViewerFrame(source, channel);
		addInternalFrame(frame);
		return frame;
	}
	
	public HelicorderViewerFrame openHelicorder(SeismicDataSource source, String channel)
	{
		source.establish();
		HelicorderViewerFrame frame = new HelicorderViewerFrame(source, channel);
		frame.addLinkListeners();
		addInternalFrame(frame);
		return frame;
	}
	
	public SwarmLayout getCurrentLayout()
	{
		ConfigFile cf = new ConfigFile();
		cf.put("name", "Current Layout");
		
		chooser.saveLayout(cf, "chooser");
		
		SwarmLayout sl = new SwarmLayout(cf);
		int i = 0;
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof HelicorderViewerFrame)
			{
				HelicorderViewerFrame hvf = (HelicorderViewerFrame)frame;
				hvf.saveLayout(cf, "helicorder-" + i++);
			}
		}
		
		mapFrame.saveLayout(cf, "map");
		
		return sl;
	}
	
	public void removeAllFrames()
	{
		Runnable r = new Runnable() 
				{
					public void run()
					{
						Iterator<JInternalFrame> it = frames.iterator();
						while (it.hasNext())
						{
							JInternalFrame frame = it.next();
							if (frame instanceof HelicorderViewerFrame ||
									frame instanceof WaveViewerFrame ||
									frame instanceof MultiMonitor)
							{
								try { frame.setClosed(true); } catch (Exception e) {}
							}
						}
					}
				};
		
		if (SwingUtilities.isEventDispatchThread())
			r.run();
		else
		{
			try	{ SwingUtilities.invokeAndWait(r); } catch (Exception e) {}
		}
	}
	
	public void removeInternalFrame(final JInternalFrame f)
	{
		SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						swarmMenu.removeInternalFrame(f);
						frames.remove(f);
						if (frameCount > 0)
							frameCount--;
					}
				});			
	}
	
	public void addInternalFrame(final JInternalFrame f)
	{
		addInternalFrame(f, true);
	}
	
	public void addInternalFrame(final JInternalFrame f, boolean setLoc)
	{
		frames.add(f);
		frameCount++;			
		frameCount = frameCount % 10;
		if (setLoc)
			f.setLocation(frameCount * 24, frameCount * 24);
		SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						swarmMenu.addInternalFrame(f);
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
		    HelicorderViewerFrame hvf0 = hcs.get(0);
		    HelicorderViewerFrame hvf1 = hcs.get(1);
		    HelicorderViewerFrame hvf2 = hcs.get(2);
		    HelicorderViewerFrame hvf3 = hcs.get(3);
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
				HelicorderViewerFrame hvf = hcs.get(i);
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

	public void setFrameLayer(JInternalFrame c, int layer)
	{
		desktop.setLayer(c, layer, 0);
	}
	
	public void parseKiosk()
	{
		String[] kiosks = config.kiosk.split(",");
		for (int i = 0; i < kiosks.length; i++)
		{ 
			String[] ch = kiosks[i].split(";");
			SeismicDataSource sds = config.getSource(ch[0]);
			if (sds == null)
				continue;
			openHelicorder(sds, ch[1]);
		}
		toggleFullScreenMode();
	}
	
	// TODO: make listener based
	public void optionsChanged()
	{
		for (JInternalFrame frame : frames)
		{
			if (frame instanceof HelicorderViewerFrame)
			{
				HelicorderViewerFrame hvf = (HelicorderViewerFrame)frame;
				hvf.getHelicorderViewPanel().cursorChanged();
				hvf.getHelicorderViewPanel().invalidateImage();
			}
		}
	}
	
	public static void main(String[] args)
	{
		try 
		{
			// JDK 1.5 by default has an ugly theme, this line uses the one from 1.4
//			PlasticLookAndFeel.setTabStyle(PlasticLookAndFeel.TAB_STYLE_METAL_VALUE);
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
//			UIManager.setLookAndFeel("net.java.plaf.windows.WindowsLookAndFeel");
//			MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
		}
		catch (Exception e) { }
		
		Swarm swarm = new Swarm(args);

		if (Swarm.config.isKiosk())
			swarm.parseKiosk();
	}
}