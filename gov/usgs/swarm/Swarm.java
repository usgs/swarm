package gov.usgs.swarm;
 
import gov.usgs.plot.data.Wave;
import gov.usgs.swarm.chooser.DataChooser;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.FileDataSource;
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
import gov.usgs.util.Util;
import gov.usgs.util.ui.GlobalKeyManager;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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
 * TODO: name worker thread for better debugging
 *
 * @author Dan Cervelli, Peter Cervelli, and Thomas Parker.
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
	private static final String VERSION;
	private static final String BUILD_DATE;
	
	private static final int LEFT = 1;
	private static final int RIGHT = 2;
	private static final int TOP = 3;
	private static final int BOTTOM = 4;
	private static final int BOTTOM_LEFT = 5;
	private static final int BOTTOM_RIGHT = 6;
	private static final int TOP_LEFT = 7;
	private static final int TOP_RIGHT = 8;
	
	static
	{
		String[] ss = Util.getVersion("gov.usgs.swarm");
		if (ss != null && ss.length >= 2)
		{
			VERSION = ss[0];
			BUILD_DATE = ss[1];
		}
		else
		{
			VERSION = "Development";
			BUILD_DATE = null;
		}
	}
	
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
	
	public FileDataSource fileSource;
	
	public Swarm(String[] args)
	{
		super(TITLE + " [" + VERSION + "]");
		logger = Log.getLogger("gov.usgs.swarm");
		logger.setLevel(Level.ALL);
		if (BUILD_DATE == null)
		{
			logger.fine("no build version information available");
		}
		else
		{
			logger.fine("Swarm version/date: " + VERSION  + "/" + BUILD_DATE);
		}
		logger.fine("JNLP: " + isJNLP());
		setIconImage(Icons.swarm.getImage());

		monitors = new HashMap<String, MultiMonitor>();
		cache = CachedDataSource.getInstance();
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
		
		Runtime rt = Runtime.getRuntime();
		logger.fine("maximum heap size: " + Util.numBytesToString(rt.maxMemory()));
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
		
		m.getInputMap().put(KeyStroke.getKeyStroke("ctrl L"), "savelayout");
		m.getActionMap().put("savelayout", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						saveLayout(null);
					}
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke("ctrl shift L"), "savelastlayout");
		m.getActionMap().put("savelastlayout", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						String ll = swarmMenu.getLastLayoutName();
						if (ll != null)
							saveLayout(ll);
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
					}	
				};
		m.getActionMap().put("fullScreenToggle", toggleFullScreenAction);
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK), "flushRight");
		m.getActionMap().put("flushRight", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushRight();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK), "flushLeft");
		m.getActionMap().put("flushLeft", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushLeft();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK), "flushTop");
		m.getActionMap().put("flushTop", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushTop();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK), "flushBottom");
		m.getActionMap().put("flushBottom", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushBottom();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.CTRL_DOWN_MASK), "flushBottomRight");
		m.getActionMap().put("flushBottomRight", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushBottomRight();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_END, InputEvent.CTRL_DOWN_MASK), "flushBottomLeft");
		m.getActionMap().put("flushBottomLeft", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushBottomLeft();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.CTRL_DOWN_MASK), "flushTopRight");
		m.getActionMap().put("flushTopRight", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushTopRight();					
					}	
				});
		
		m.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, InputEvent.CTRL_DOWN_MASK), "flushTopLeft");
		m.getActionMap().put("flushTopLeft", new AbstractAction()
				{
					private static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						flushTopLeft();					
					}	
				});
		
	}
	
	public boolean isJNLP()
	{
		try
		{
			BasicService bs = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
			return bs != null;
		}
		catch (Throwable e)
		{}
		return false;
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
	
	public static FileDataSource getFileSource()
	{
		return application.fileSource;
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
		/*
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
		*/
		this.setSize(config.windowWidth, config.windowHeight);
		this.setLocation(config.windowX, config.windowY);
		if (config.windowMaximized)
			this.setExtendedState(Frame.MAXIMIZED_BOTH);
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		fileSource = new FileDataSource();
		
		chooser = new DataChooser();
		split = SwarmUtil.createStrippedSplitPane(JSplitPane.HORIZONTAL_SPLIT, chooser, desktop);
		split.setDividerLocation(config.chooserDividerLocation);
		split.setDividerSize(4);
		setChooserVisible(config.chooserVisible);
		chooser.setDividerLocation(config.nearestDividerLocation);
		
		waveClipboard = new WaveClipboardFrame();
		desktop.add(waveClipboard);
		waveClipboard.setVisible(config.clipboardVisible);
		if (Swarm.config.clipboardMaximized)
		{
			try { waveClipboard.setMaximum(true); } catch (Exception e) {}
		}
		
		mapFrame = new MapFrame();
		desktop.add(mapFrame);
		frames.add(mapFrame);
		mapFrame.setVisible(config.mapVisible);
		if (Swarm.config.mapMaximized)
		{
			try { mapFrame.setMaximum(true); } catch (Exception e) {}
		}
		
		mapFrame.toFront();
		
		swarmMenu = new SwarmMenu();
		this.setJMenuBar(swarmMenu);
		
		for (SwarmLayout sl : config.layouts.values())
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
		setFullScreenMode(!fullScreen);
	}
	
	public void setFullScreenMode(boolean full)
	{
		if (fullScreen == full)
			return;
		
		requestFocus();
		fullScreen = full;
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
			if (frame.isVisible() && frame instanceof Kioskable)
			{
				Kioskable f = (Kioskable)frame; 
				f.setKioskMode(full);
			}
		}
		tileKioskFrames();
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
			config.clipboardX = waveClipboard.getX();
			config.clipboardY = waveClipboard.getY();
			config.clipboardWidth = waveClipboard.getWidth();
			config.clipboardHeight = waveClipboard.getHeight();
			config.clipboardMaximized = false;
		}
		config.clipboardVisible = isClipboardVisible();
		
		if (mapFrame.isMaximum())
			config.mapMaximized = true;
		else
		{
			config.mapX = mapFrame.getX();
			config.mapY = mapFrame.getY();
			config.mapWidth = mapFrame.getWidth();
			config.mapHeight = mapFrame.getHeight();
			config.mapMaximized = false;
		}
		config.mapVisible = mapFrame.isVisible();
		
		config.chooserDividerLocation = split.getDividerLocation();
		config.chooserVisible = isChooserVisible();
		
		config.nearestDividerLocation = chooser.getDividerLocation();
		config.kiosk = Boolean.toString(fullScreen);

		config.userTimes = chooser.getUserTimes();
		
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
		monitors.remove(mm.getDataSource().getName());
		removeInternalFrame(mm);
		mm = null;
	}
	
	public MultiMonitor getMonitor(SeismicDataSource source)
	{
		MultiMonitor monitor = monitors.get(source.getName());
		if (monitor == null)
		{
			monitor = new MultiMonitor(source);
			monitors.put(source.getName(), monitor);
			addInternalFrame(monitor);
		}
		return monitor;
	}
	
	public void monitorChannelSelected(SeismicDataSource source, String channel)
	{
		MultiMonitor monitor = getMonitor(source);
		monitor.setVisible(true);
		monitor.addChannel(channel);
	}

	public WaveViewerFrame openRealtimeWave(SeismicDataSource source, String channel)
	{
		WaveViewerFrame frame = new WaveViewerFrame(source, channel);
		addInternalFrame(frame);
		return frame;
	}
	
	public HelicorderViewerFrame openHelicorder(SeismicDataSource source, String channel, double time)
	{
		source.establish();
		HelicorderViewerFrame frame = new HelicorderViewerFrame(source, channel, time);
		frame.addLinkListeners();
		addInternalFrame(frame);
		return frame;
	}
	
	private String lastLayout = "";
	
	public void saveLayout(String name)
	{
		boolean fixedName = (name != null);
		SwarmLayout sl = getCurrentLayout();
		boolean done = false;
		while (!done)
		{
			if (name == null)
			{
				name = (String)JOptionPane.showInputDialog(
					Swarm.getApplication(), "Enter a name for this layout:", 
					"Save Layout", JOptionPane.INFORMATION_MESSAGE, null, null, lastLayout);
			}
			if (name != null)
			{
				if (Swarm.config.layouts.containsKey(name))
				{
					boolean overwrite = false;
					if (!fixedName)
					{
						int opt = JOptionPane.showConfirmDialog(
								Swarm.getApplication(), "A layout by that name already exists.  Overwrite?", 
								"Warning", JOptionPane.YES_NO_OPTION);
						overwrite = (opt == JOptionPane.YES_OPTION);
					}
					else
						overwrite = true;
					
					if (overwrite)
					{
						if (fixedName)
						{
							JOptionPane.showMessageDialog(Swarm.getApplication(), "Layout overwritten.");
						}
						swarmMenu.removeLayout(Swarm.config.layouts.get(name));
						Swarm.config.removeLayout(Swarm.config.layouts.get(name));
					}
				}
				
				if (!Swarm.config.layouts.containsKey(name))
				{
					swarmMenu.setLastLayoutName(name);
					sl.setName(name);
					sl.save();
					swarmMenu.addLayout(sl);
					Swarm.config.addLayout(sl);
					done = true;
					lastLayout = name;
				}
			}
			else 
				done = true; // cancelled
		}
	}
	
	public SwarmLayout getCurrentLayout()
	{
		ConfigFile cf = new ConfigFile();
		cf.put("name", "Current Layout");
		cf.put("kiosk", Boolean.toString(isFullScreenMode()));
		cf.put("kioskX", Integer.toString(getX()));
		cf.put("kioskY", Integer.toString(getY()));
		
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
			else if (frame instanceof MultiMonitor)
			{
				MultiMonitor mm = (MultiMonitor)frame;
				mm.saveLayout(cf, "monitor-" + i++);
			}
		}
		
		if (mapFrame.isVisible())
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
	
	public void flush(int position) {
		
		Dimension ds = desktop.getSize();
		JInternalFrame bingo = null;
		
		if (waveClipboard.isSelected()) {
			bingo = waveClipboard;
		}
		else
			for (JInternalFrame frame : frames)
				if (frame.isSelected())
					bingo = frame;
		
		if (bingo != null)
			switch (position) {
				case LEFT:
					bingo.setSize(ds.width / 2, ds.height);
					bingo.setLocation(0, 0);
					break;
					
				case RIGHT:
					bingo.setSize(ds.width / 2, ds.height);
					bingo.setLocation(ds.width / 2, 0);
					break;
					
				case TOP:
					bingo.setSize(ds.width, ds.height / 2);
					bingo.setLocation(0, 0);
					break;
					
				case BOTTOM:
					bingo.setSize(ds.width, ds.height / 2);
					bingo.setLocation(0, ds.height / 2);
					break;
					
				case BOTTOM_LEFT:
					bingo.setSize(ds.width / 2, ds.height / 2);
					bingo.setLocation(0, ds.height / 2);
					break;
					
				case BOTTOM_RIGHT:
					bingo.setSize(ds.width / 2, ds.height / 2);
					bingo.setLocation(ds.width / 2, ds.height / 2);
					break;
					
				case TOP_LEFT:
					bingo.setSize(ds.width / 2, ds.height / 2);
					bingo.setLocation(0, 0);
					break;
					
				case TOP_RIGHT:
					bingo.setSize(ds.width / 2, ds.height / 2);
					bingo.setLocation(ds.width / 2, 0);
					break;
			}
	}
	
	public void flushLeft() {		
		flush(LEFT);
	}
	
	public void flushRight() {		
		flush(RIGHT);
	}
	
	public void flushTop() {		
		flush(TOP);
	}
	
	public void flushBottom() {		
		flush(BOTTOM);
	}
	
	public void flushBottomRight() {		
		flush(BOTTOM_RIGHT);
	}
	
	public void flushBottomLeft() {		
		flush(BOTTOM_LEFT);
	}
	
	public void flushTopRight() {		
		flush(TOP_RIGHT);
	}
	
	public void flushTopLeft() {		
		flush(TOP_LEFT);
	}
	
	public void tileKioskFrames()
	{
		Dimension ds = desktop.getSize();

		ArrayList<JInternalFrame> ks = new ArrayList<JInternalFrame>();
		for (JInternalFrame frame : frames)
		{
			if (frame.isVisible() && frame instanceof Kioskable)
				ks.add(frame);
		}
		
		if (ks.size() == 0)
			return;
		
		int mapCount = 0;
		int heliCount = 0;
		int monitorCount = 0;
		for (JInternalFrame frame : ks)
		{
			if (frame instanceof MapFrame)
				mapCount++;
			else if (frame instanceof HelicorderViewerFrame)
				heliCount++;
			else if (frame instanceof MultiMonitor)
				monitorCount++;
		}
		
		if (ks.size() == 4)
		{
		    int w = ds.width / 2;
		    int h = ds.height / 2;
		    JInternalFrame hvf0 = ks.get(0);
		    JInternalFrame hvf1 = ks.get(1);
		    JInternalFrame hvf2 = ks.get(2);
		    JInternalFrame hvf3 = ks.get(3);
		    hvf0.setSize(w, h);
		    hvf0.setLocation(0, 0);
		    hvf1.setSize(w, h);
		    hvf1.setLocation(w, 0);
		    hvf2.setSize(w, h);
		    hvf2.setLocation(0, h);
		    hvf3.setSize(w, h);
		    hvf3.setLocation(w, h);
		}
		else if (ks.size() == 3 && mapCount == 1 && heliCount == 1 && monitorCount == 1)
		{
			int w = ds.width / 2;
		    int h = ds.height / 2;
		    for (JInternalFrame frame : ks)
			{
				if (frame instanceof MapFrame)
				{
					frame.setLocation(w, h);
					frame.setSize(w, h);
				}
				else if (frame instanceof HelicorderViewerFrame)
				{
					frame.setLocation(0, 0);
					frame.setSize(w, h * 2);
				}
				else if (frame instanceof MultiMonitor)
				{
					frame.setLocation(w, 0);
					frame.setSize(w, h);
				}
			}
		}
		else
		{
		    int w = ds.width / ks.size();
			int cx = 0;
			for (int i = 0; i < ks.size(); i++)
			{
				JInternalFrame hvf = ks.get(i);
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
	
	private void parseKiosk()
	{
		String[] kiosks = config.kiosk.split(",");
		if (config.kiosk.startsWith("layout:"))
		{
			String layout = config.kiosk.substring(7);
			SwarmLayout sl = config.layouts.get(layout);
			if (sl != null)
			{
				lastLayout = layout;
				sl.process();
			}
			else
				Swarm.logger.warning("could not start with layout: " + layout);
		}
		boolean set = false;
		for (int i = 0; i < kiosks.length; i++)
		{ 
			String[] ch = kiosks[i].split(";");
			SeismicDataSource sds = config.getSource(ch[0]);
			if (sds == null)
				continue;
			openHelicorder(sds, ch[1], Double.NaN);
			set = true;
		}
		if (config.kiosk.equals("true"))
			set = true;
		
		if (set)
			toggleFullScreenMode();
		else
			Swarm.logger.warning("no helicorders, skipping kiosk mode.");
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
				if (!config.durationEnabled)
					hvf.getHelicorderViewPanel().clearMarks();
			}
			else if (frame instanceof MapFrame)
			{
				MapFrame mf = (MapFrame)frame;
				mf.reloadImages();
			}
		}
	}
	
	public static void main(String[] args)
	{
		try 
		{
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
			UIManager.put("InternalFrame.border", SwarmUtil.getInternalFrameBorder());
		}
		catch (Exception e) { }
		
		Swarm swarm = new Swarm(args);

		if (Swarm.config.isKiosk())
			swarm.parseKiosk();
	}
}