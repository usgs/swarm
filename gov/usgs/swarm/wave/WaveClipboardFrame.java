package gov.usgs.swarm.wave;

import gov.usgs.swarm.Images;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmFrame;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.ui.ExtensionFileFilter;
import gov.usgs.vdx.data.wave.SAC;
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
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * The wave clipboard internal frame.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2006/08/09 21:49:46  cervelli
 * Many changes including resizable waves and permanent scrollbar.
 *
 * Revision 1.2  2006/08/09 05:08:54  cervelli
 * Override setMaximum to eliminate quirk when saving maximized clipboard.
 *
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.15  2006/07/30 22:44:03  cervelli
 * Icon change.
 *
 * Revision 1.14  2006/07/25 05:16:13  cervelli
 * Change for FrameDecorator being class instead of interface.
 *
 * Revision 1.13  2006/07/22 20:31:43  cervelli
 * Added throbber and status bar.
 *
 * Revision 1.12  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.11  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.10  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.9  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.8  2006/04/08 01:31:04  dcervelli
 * Error messages on failed saves, bug #34.
 *
 * Revision 1.7  2006/04/02 17:15:23  cervelli
 * Got rid of automatic '.sac' extension at request of John Power
 *
 * Revision 1.6  2005/09/23 21:57:09  dcervelli
 * Uses subset() on save all SAC.
 *
 * Revision 1.5  2005/09/22 20:59:50  dcervelli
 * Multiple SAC save/load fixes.
 *
 * Revision 1.4  2005/08/30 00:34:26  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:31:20  tparker
 * Tidied code, no functional changes.
 *
 * Revision 1.2  2005/08/26 23:49:04  tparker
 * Create image path constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.3  2005/03/27 22:03:46  cervelli
 * Added Save All button.  Moved the Save/Save All/Open ActionListeners into separate
 * internal non-anonymous classes.  Open now opens directories.
 *
 * Revision 1.2  2005/03/24 20:48:31  cervelli
 * Support for opening and saving SAC files.
 *
 * @author Dan Cervelli
 */
public class WaveClipboardFrame extends SwarmFrame
{
	public static final long serialVersionUID = -1;
	private static final Color SELECT_COLOR = new Color(204, 204, 255);
	private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);
		
	private JScrollPane scrollPane;
	private Box waveBox;
	private List<WaveViewPanel> waves;
	private WaveViewPanel selected;
	private JToolBar toolbar;
	private JPanel mainPanel;
	private JLabel statusLabel;
	private JToggleButton linkButton;
	private JButton sizeButton;
	private JButton syncButton;
	private JButton sortButton;
	private JButton removeAllButton;
	private JButton saveButton;
	private JButton saveAllButton;
	private JButton openButton;
	private DateFormat saveAllDateFormat;
	
	private WaveViewSettingsToolbar waveToolbar;
	
	private JButton upButton;
	private JButton downButton;
	private JButton removeButton;
	private JButton compXButton;
	private JButton expXButton;
	private JButton copyButton;
	private JButton forwardButton;
	private JButton backButton;
	private JButton gotoButton;
	
	private JPopupMenu popup;
	
	private Map<WaveViewPanel, Stack<double[]>> histories;
	
	private HelicorderViewPanelListener linkListener;

	private boolean heliLinked = true;
	
	private Throbber throbber;
	
	private int waveHeight = -1;
	
	public WaveClipboardFrame()
	{
		super("Wave Clipboard", true, true, true, true);
		this.setFocusable(true);
		saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		waves = new ArrayList<WaveViewPanel>();
		histories = new HashMap<WaveViewPanel, Stack<double[]>>();
		createUI();
		linkListener = new HelicorderViewPanelListener() 
				{
					public void insetCreated(double st, double et)
					{
						if (heliLinked)
							repositionWaves(st, et);
					}
				};
	}

	public HelicorderViewPanelListener getLinkListener()
	{
		return linkListener;
	}
	
	public void createUI()
	{
		this.setFrameIcon(Images.getIcon("clipboard"));
		this.setSize(Swarm.config.clipboardWidth, Swarm.config.clipboardHeight);
		this.setLocation(Swarm.config.clipboardX, Swarm.config.clipboardY);
		this.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
		
		toolbar = SwarmUtil.createToolBar();
		mainPanel = new JPanel(new BorderLayout());
		
		createMainButtons();
		createWaveButtons();

		mainPanel.add(toolbar, BorderLayout.NORTH);
		
		waveBox = new Box(BoxLayout.Y_AXIS);
		scrollPane = new JScrollPane(waveBox);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getVerticalScrollBar().setUnitIncrement(40);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		
		statusLabel = new JLabel(" ");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 1));
		mainPanel.add(statusLabel, BorderLayout.SOUTH);
		
		mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 1, 2));
		this.setContentPane(mainPanel);
		
		createListeners();
	}

	private void createMainButtons()
	{
		openButton = SwarmUtil.createToolBarButton(
				Images.getIcon("open"),
				"Open a saved wave",
				new OpenActionListener());
		toolbar.add(openButton);

		saveButton = SwarmUtil.createToolBarButton(
				Images.getIcon("save"),
				"Save selected wave",
				new SaveActionListener());
		saveButton.setEnabled(false);
		toolbar.add(saveButton);

		saveAllButton = SwarmUtil.createToolBarButton(
				Images.getIcon("saveall"),
				"Save all waves",
				new SaveAllActionListener());
		saveAllButton.setEnabled(false);
		toolbar.add(saveAllButton);
		
		toolbar.addSeparator();
		
		linkButton = SwarmUtil.createToolBarToggleButton(
				Images.getIcon("helilink"),
				"Synchronize times with helicorder wave",
				new ActionListener()
				{
						public void actionPerformed(ActionEvent e)
						{
							heliLinked = linkButton.isSelected();
						}
				});
		linkButton.setSelected(heliLinked);
		toolbar.add(linkButton);
		
		syncButton = SwarmUtil.createToolBarButton(
				Images.getIcon("clock"),
				"Synchronize times with selected wave",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected == null)
							return;
						
						syncChannels();
					}
				}); 
		syncButton.setEnabled(false);
		toolbar.add(syncButton);
		
		sortButton = SwarmUtil.createToolBarButton(
				Images.getIcon("geosort"),
				"Sort waves by nearest to selected wave",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected == null)
							return;
						
						sortChannelsByNearest();
					}
				});
		sortButton.setEnabled(false);
		toolbar.add(sortButton);
		
		toolbar.addSeparator();
		
		sizeButton = SwarmUtil.createToolBarButton(
				Images.getIcon("resize"),
				"Set clipboard wave size",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						doSizePopup(sizeButton.getX(), sizeButton.getY() + 2 * sizeButton.getHeight());
					}
				}); 
		toolbar.add(sizeButton);
		
		removeAllButton = SwarmUtil.createToolBarButton(
				Images.getIcon("deleteall"),
				"Remove all waves from clipboard",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						removeWaves();
					}
				});
		removeAllButton.setEnabled(false);
		toolbar.add(removeAllButton);
	}
	
	private void createWaveButtons()
	{
		toolbar.addSeparator();
		
		backButton = SwarmUtil.createToolBarButton(
				Images.getIcon("left"),
				"Scroll back time 20% (Left arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							shiftTime(selected, -0.20);						
					}
				});
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
		toolbar.add(backButton);
		
		forwardButton = SwarmUtil.createToolBarButton(
				Images.getIcon("right"),
				"Scroll forward time 20% (Right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							shiftTime(selected, 0.20);
					}
				});
		toolbar.add(forwardButton);
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);
		
		gotoButton = SwarmUtil.createToolBarButton(
				Images.getIcon("gototime"),
				"Go to time (Ctrl-G)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						String t = JOptionPane.showInputDialog(Swarm.getApplication(), "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time", JOptionPane.PLAIN_MESSAGE);
						if (selected != null && t != null)
							gotoTime(selected, t);
					}
				});
		toolbar.add(gotoButton);
		Util.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);
		
		compXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xminus"),
				"Shrink sample time 20% (Alt-left arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							scaleTime(selected, 0.20);
					}
				});
		toolbar.add(compXButton);
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
		
		expXButton = SwarmUtil.createToolBarButton(
				Images.getIcon("xplus"),
				"Expand sample time 20% (Alt-right arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							scaleTime(selected, -0.20);
					}
				});
		toolbar.add(expXButton);
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);

		JButton histButton = SwarmUtil.createToolBarButton(
				Images.getIcon("back"),
				"Last time settings (Backspace)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							back(selected);
					}
				});		
		Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", histButton);
		toolbar.add(histButton);
		toolbar.addSeparator();

		waveToolbar = new WaveViewSettingsToolbar(null, toolbar, this);
		
		copyButton = SwarmUtil.createToolBarButton(
				Images.getIcon("clipboard"),
				"Place another copy of wave on clipboard (C or Ctrl-C)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
						{
							WaveViewPanel wvp = new WaveViewPanel(selected); 
							wvp.setBackgroundColor(BACKGROUND_COLOR);
							addWave(wvp);
						}
					}
				});
		Util.mapKeyStrokeToButton(this, "C", "clipboard1", copyButton);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard2", copyButton);
		toolbar.add(copyButton);
		
		toolbar.addSeparator();
		
		upButton = SwarmUtil.createToolBarButton(
				Images.getIcon("up"),
				"Move wave up in clipboard (Up arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							moveUp(selected);
					}
				});
		Util.mapKeyStrokeToButton(this, "UP", "up", upButton);
		toolbar.add(upButton);
		
		downButton = SwarmUtil.createToolBarButton(
				Images.getIcon("down"),
				"Move wave down in clipboard (Down arrow)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							moveDown(selected);
					}
				});
		Util.mapKeyStrokeToButton(this, "DOWN", "down", downButton);
		toolbar.add(downButton);
		
		removeButton = SwarmUtil.createToolBarButton(
				Images.getIcon("delete"),
				"Remove wave from clipboard (Delete)",
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (selected != null)
							remove(selected);
					}
				});
		Util.mapKeyStrokeToButton(this, "DELETE", "remove", removeButton);
		toolbar.add(removeButton);
		
		toolbar.add(Box.createHorizontalGlue());
		
		throbber = new Throbber();
		toolbar.add(throbber);
	}
	
	private void createListeners()
	{
		this.addInternalFrameListener(new InternalFrameAdapter()
				{
					public void internalFrameActivated(InternalFrameEvent e)
					{
						if (selected != null)
						{
							selected.setBackgroundColor(BACKGROUND_COLOR);
							Swarm.getApplication().getDataChooser().setNearest(selected.getChannel());
						}
					}
					
					public void internalFrameDeiconified(InternalFrameEvent e)
					{
						resizeWaves();	
					}
					
					public void internalFrameClosing(InternalFrameEvent e)
					{
						setVisible(false);
					}
					
					public void internalFrameClosed(InternalFrameEvent e)
					{}
				});
		
		this.addComponentListener(new ComponentAdapter()
				{
					public void componentResized(ComponentEvent e)
					{
						resizeWaves();
					}
				});
	}
	
	private int calculateWaveHeight()
	{
		if (waveHeight > 0)
			return waveHeight;
		
		int w = scrollPane.getViewport().getSize().width;
		int h = (int)Math.round((double)w * 60.0 / 300.0);
		h = Math.min(200, h);
		h = Math.max(h, 80);
		return h;
	}
	
	private void setWaveHeight(int s)
	{
		waveHeight = s;
		resizeWaves();
	}
	
	private void doSizePopup(int x, int y)
	{
		if (popup == null)
		{
			final String[] labels = new String[] { "Auto", null, "Tiny", "Small", "Medium", "Large" };
			final int[] sizes = new int[] { -1, -1, 50, 100, 160, 230 };
			popup = new JPopupMenu();
			ButtonGroup group = new ButtonGroup();
			for (int i = 0; i < labels.length; i++)
			{
				if (labels[i] != null)
				{
					final int size = sizes[i];
					JRadioButtonMenuItem mi = new JRadioButtonMenuItem(labels[i]);
					mi.addActionListener(new ActionListener()
							{
								public void actionPerformed(ActionEvent e)
								{
									setWaveHeight(size);
								}
							});
					if (waveHeight == size)
						mi.setSelected(true);
					group.add(mi);
					popup.add(mi);
				}
				else
					popup.addSeparator();
			}
		}
		popup.show(this, x, y);
	}
	
	private class OpenActionListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e)
		{
			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			chooser.resetChoosableFileFilters();
			ExtensionFileFilter txtExt = new ExtensionFileFilter(".txt", "Matlab-readable text files");
			ExtensionFileFilter sacExt = new ExtensionFileFilter(".sac", "SAC files");
			chooser.addChoosableFileFilter(txtExt);
			chooser.addChoosableFileFilter(sacExt);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(true);
			int result = chooser.showOpenDialog(Swarm.getApplication());
			if (result == JFileChooser.APPROVE_OPTION) 
			{						            
				File[] fs = chooser.getSelectedFiles();

				for (int i = 0; i < fs.length; i++)
				{
				    if (fs[i].isDirectory())
				    {
				        File[] dfs = fs[i].listFiles();
				        for (int j = 0; j < dfs.length; j++)
					        openFile(dfs[j]);
					    Swarm.config.lastPath = fs[i].getParent();
				    }
				    else
				    {
				        openFile(fs[i]);
				        Swarm.config.lastPath = fs[i].getParent();
				    }
				}
			}
		}
	}
	
	private class SaveActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if (selected == null)
				return;
			
//			WaveViewPanel wvp = selected.getWaveViewPanel();
			
			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			chooser.resetChoosableFileFilters();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(false);
			
			ExtensionFileFilter txtExt = new ExtensionFileFilter(".txt", "Matlab-readable text files");
			ExtensionFileFilter sacExt = new ExtensionFileFilter(".sac", "SAC files");
			chooser.addChoosableFileFilter(txtExt);
			chooser.addChoosableFileFilter(sacExt);
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
			
			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			chooser.setSelectedFile(new File(selected.getChannel() + ".txt"));
			int result = chooser.showSaveDialog(Swarm.getApplication());
			if (result == JFileChooser.APPROVE_OPTION) 
			{						            
				File f = chooser.getSelectedFile();
				String path = f.getPath();
				if (!(path.endsWith(".txt") || path.endsWith(".sac")))
				{
				    if (chooser.getFileFilter() == sacExt)
				        f = new File(path + ".sac");
				    else
				        f = new File(path + ".txt");
				}
				boolean confirm = true;
				if (f.exists())
				{
				    if (f.isDirectory())
				    {
				        JOptionPane.showMessageDialog(Swarm.getApplication(), "You can not select an existing directory.", "Error", JOptionPane.ERROR_MESSAGE);
					    return;
				    }
					confirm = false;
					int choice = JOptionPane.showConfirmDialog(Swarm.getApplication(), "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.YES_OPTION)
						confirm = true;
				}
				
				if (confirm)
				{
					try
				    {
						Swarm.config.lastPath = f.getParent();
						String fn = f.getPath().toLowerCase();
						if (fn.endsWith(".sac"))
						{
						    SAC sac = selected.getWave().toSAC();
						    String[] scn = selected.getChannel().split(" ");
						    sac.kstnm = scn[0];
						    sac.kcmpnm = scn[1];
						    sac.knetwk = scn[2];
						    sac.write(f);
						}
						else
						    selected.getWave().exportToText(f.getPath());
				    }
					catch (FileNotFoundException ex)
				    {
				    	JOptionPane.showMessageDialog(Swarm.getApplication(), 
				    			"Directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
				    }
				    catch (IOException ex)
				    {
				    	JOptionPane.showMessageDialog(Swarm.getApplication(), 
				    			"Error writing file.", "Error", JOptionPane.ERROR_MESSAGE);
				    }
				}
			}
		}
	}
	
	private class SaveAllActionListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e)
		{
			if (waves.size() <= 0)
				return;
			
			JFileChooser chooser = Swarm.getApplication().getFileChooser();
			chooser.resetChoosableFileFilters();
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			File lastPath = new File(Swarm.config.lastPath);
			chooser.setCurrentDirectory(lastPath);
			int result = chooser.showSaveDialog(Swarm.getApplication());
			File f = chooser.getSelectedFile();
			if (f == null)
			{
			    JOptionPane.showMessageDialog(Swarm.getApplication(), "You must select a directory.", "Error", JOptionPane.ERROR_MESSAGE);
			    return;
			}
			if (result == JFileChooser.APPROVE_OPTION) 
			{	
				try
			    {
				    if (f.exists() && !f.isDirectory())
				        return;
				    if (!f.exists())
				        f.mkdir();
				    for (WaveViewPanel wvp : waves)
				    {
				        Wave sw = wvp.getWave();
				        
				        if (sw != null)
				        {
				        	sw = sw.subset(wvp.getStartTime(), wvp.getEndTime());
				            String date = saveAllDateFormat.format(Util.j2KToDate(sw.getStartTime()));
				            File dir = new File(f.getPath() + File.separatorChar + date);
				            if (!dir.exists())
				                dir.mkdir();
				            
				            SAC sac = sw.toSAC();
						    String[] scn = wvp.getChannel().split(" ");
						    sac.kstnm = scn[0];
						    sac.kcmpnm = scn[1];
						    sac.knetwk = scn[2];
					        sac.write(new File(dir.getPath() + File.separatorChar + wvp.getChannel().replace(' ', '.')));
				        }
				    }
				    Swarm.config.lastPath = f.getPath();
			    }
				catch (FileNotFoundException ex)
			    {
			    	JOptionPane.showMessageDialog(Swarm.getApplication(), 
			    			"Directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
			    }
			    catch (IOException ex)
			    {
			    	JOptionPane.showMessageDialog(Swarm.getApplication(), 
			    			"Error writing file.", "Error", JOptionPane.ERROR_MESSAGE);
			    }
			}
		}
	}
	
	private enum FileType 
	{ 
		TEXT, SAC, UNKNOWN;
		
		public static FileType fromFile(File f)
		{
			if (f.getPath().endsWith(".sac"))
				return SAC;
			else if( f.getPath().endsWith(".txt"))
				return TEXT;
			else 
				return UNKNOWN;
		}
	}
	
	private SAC readSAC(File f)
	{
		SAC sac = new SAC();
	    try
	    {
	        sac.read(f.getPath());
	    }
	    catch (Exception ex)
	    {
	    	sac = null;
	    }
	    return sac;
	}
	
	public void openFile(File f)
	{
		SAC sac = null;
	    Wave sw = null;
	    String channel = f.getName();
	    FileType ft = FileType.fromFile(f);
	    switch (ft)
	    {
		    case SAC:
		    	sac = readSAC(f);
		    	break;
		    case TEXT:
		    	sw = Wave.importFromText(f.getPath());
		    	break;
		    case UNKNOWN:
		    	// try SAC
		    	sac = readSAC(f);
		    	// try text
		    	if (sac == null)
		    		sw = Wave.importFromText(f.getPath());
		    	break;
	    }
	    
	    if (sac != null)
    	{
    		sw = sac.toWave();
    		channel = sac.getWinstonChannel().replace('$', ' ');
    	}
	    
		if (sw != null)
		{
			WaveViewPanel wvp = new WaveViewPanel();
			wvp.setChannel(channel);
			Swarm.getCache().putWave(channel, sw);
			wvp.setDataSource(Swarm.getCache());
			wvp.setWave(sw, sw.getStartTime(), sw.getEndTime());
			WaveClipboardFrame.this.addWave(new WaveViewPanel(wvp));
		}
		else
			JOptionPane.showMessageDialog(Swarm.getApplication(), "There was an error opening the file, '" + f.getName() + "'.", "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	private void doButtonEnables()
	{
		boolean enable = (waves == null || waves.size() == 0);
		saveButton.setEnabled(!enable);
		sortButton.setEnabled(!enable);
		saveAllButton.setEnabled(!enable);
		syncButton.setEnabled(!enable);
		removeAllButton.setEnabled(!enable);
	}
	
	public synchronized void sortChannelsByNearest()
	{
		if (selected == null)
			return;
		
		ArrayList<WaveViewPanel> sorted = new ArrayList<WaveViewPanel>(waves.size());
		for (WaveViewPanel wave : waves)
			sorted.add(wave);
		
		final Metadata smd = Swarm.config.getMetadata(selected.getChannel());
		if (smd == null || Double.isNaN(smd.getLongitude()) || Double.isNaN(smd.getLatitude()))
			return;
		
		Collections.sort(sorted, new Comparator<WaveViewPanel>()
				{
					public int compare(WaveViewPanel wvp1, WaveViewPanel wvp2)
					{
						Metadata md = Swarm.config.getMetadata(wvp1.getChannel());
						double d1 = smd.distanceTo(md);
						md = Swarm.config.getMetadata(wvp2.getChannel());
						double d2 = smd.distanceTo(md);
						return Double.compare(d1, d2);
					}
				});
		
		WaveViewPanel s = selected;
		removeWaves();
		for (WaveViewPanel wave : sorted)
			addWave(wave);
		select(s);
	}
	
	public synchronized void syncChannels()
	{
		final double st = selected.getStartTime();
		final double et = selected.getEndTime();
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						for (WaveViewPanel wvp : waves)
						{
							if (wvp != selected)
							{
								if (wvp.getDataSource() != null)
								{
									Wave sw = wvp.getDataSource().getWave(wvp.getChannel(), st, et);
									wvp.setWave(sw, st, et);
								}
							}
						}
						return null;
					}
					
					public void finished()
					{
						repaint();	
					}
				};
		worker.start();	
	}
	
	public void setStatusText(final String s)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				statusLabel.setText(s);
			}
		});
	}
	
	public WaveViewPanel getSelected()
	{
		return selected;
	}
	
	/*
	private class ClipboardWaveDecorator extends FrameDecorator
	{
		private WaveViewPanel panel;
		
		public ClipboardWaveDecorator(WaveViewPanel p)
		{
			panel = p;
		}
		
		public void decorate(FrameRenderer fr)
		{
			fr.createEmptyAxis();
			AxisRenderer ar = fr.getAxis();
			ar.createDefault();
			ar.setBackgroundColor(Color.WHITE);
			if (selected == panel)
				ar.setBackgroundColor(SELECT_COLOR);
				
			TextRenderer label = new TextRenderer(fr.getGraphX() + 4, fr.getGraphY() + 14, panel.getChannel(), Color.BLACK);
			label.backgroundColor = Color.WHITE;
			
			int hTicks = fr.getGraphWidth() / 108;
			Object[] stt = SmartTick.autoTimeTick(fr.getMinXAxis(), fr.getMaxXAxis(), hTicks);
	        if (stt != null)
	        	ar.createVerticalGridLines((double[])stt[0]);
	        
	        ar.createBottomTickLabels((double[])stt[0], (String[])stt[1]);
	        int vTicks = fr.getGraphHeight() / 24;
	        fr.createDefaultYAxis(vTicks, false);
	        
	        ar.addPostRenderer(label);
		}
	}
	*/
	
	public synchronized void addWave(final WaveViewPanel p)
	{
		p.addListener(new WaveViewPanelAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						requestFocusInWindow();
						select(p);
					}
					
					public void waveZoomed(double st, double et)
					{
						double[] t = new double[] {st, et};
						addHistory(p, t);
					}
				});
		p.setOffsets(54, 7, 7, 18);
		p.setStatusLabel(statusLabel);
		p.setAllowDragging(true);
//		p.setFrameDecorator(new ClipboardWaveDecorator(p));
		int w = scrollPane.getViewport().getSize().width;
		p.setSize(w, calculateWaveHeight());
		p.createImage();
		waveBox.add(p);	
		waves.add(p);
		doButtonEnables();
		waveBox.validate();
	}
	
	public synchronized void select(final WaveViewPanel p)
	{
		if (selected == p)
			return;
		
		if (selected != null)
		{
			selected.setBackgroundColor(BACKGROUND_COLOR);
			selected.createImage();
		}
		selected = p;
		Swarm.getApplication().getDataChooser().setNearest(selected.getChannel());
		selected.setBackgroundColor(SELECT_COLOR);
		selected.createImage();
		waveToolbar.setSettings(selected.getSettings());
	}
	
	public synchronized void remove(WaveViewPanel p)
	{
		if (selected == p)
		{
			selected = null;
			waveToolbar.setSettings(null);
		}
		int i = 0;
		for (i = 0; i < waveBox.getComponentCount(); i++)
		{
			if (p == waveBox.getComponent(i))
				break;
		}
		
		p.getDataSource().close();
		setStatusText(" ");
		waveBox.remove(i);
		waves.remove(p);
		histories.remove(p);
		if (selected == null && waves.size() > 0)
		{
			if (i >= waves.size())
				i = waves.size() - 1;
			select(waves.get(i));
		}
		doButtonEnables();
		waveBox.validate();
	}
	
	public synchronized void moveDown(WaveViewPanel p)
	{
		int i = waves.indexOf(p);
		if (i == waves.size() - 1)
			return;
			
		waves.remove(i);
		waves.add(i + 1, p);
		waveBox.remove(p);
		waveBox.add(p, i + 1);
		waveBox.validate();
		if (selected != null)
			selected.requestFocus();
		repaint();
	}
	
	public synchronized void moveUp(WaveViewPanel p)
	{
		int i = waves.indexOf(p);
		if (i == 0)
			return;
			
		waves.remove(i);
		waves.add(i - 1, p);
		waveBox.remove(p);
		waveBox.add(p, i - 1);
		waveBox.validate();
		if (selected != null)
			selected.requestFocus();
		repaint();
	}
	
	public void resizeWaves()
	{
		SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						int w = scrollPane.getViewport().getSize().width;
						for (WaveViewPanel wave : waves)
						{
							wave.setSize(w, calculateWaveHeight());
							wave.createImage();
						}
						return null;
					}
					
					public void finished()
					{
						waveBox.validate();
						validate();
						repaint();
					}
				};
		worker.start();
	}
	 
	public void removeWaves()
	{
		for (WaveViewPanel wave : waves)
		{
			wave.getDataSource().close();
			waveBox.remove(wave);
		}
		selected = null;
		waveToolbar.setSettings(null);
		waves.clear();
		waveBox.validate();
		scrollPane.validate();
		doButtonEnables();
		repaint();
	}
	
	private void addHistory(WaveViewPanel wvp, double[] t)
	{
		Stack<double[]> history = histories.get(wvp);
		if (history == null)
		{
			history = new Stack<double[]>();
			histories.put(wvp, history);
		}
		history.push(t);
	}

	public void gotoTime(WaveViewPanel wvp, String t)
	{
		double j2k = Double.NaN;
		try
		{
			if (t.length() == 12)
				t = t + "30";
				
			j2k = Time.parse("yyyyMMddHHmmss", t);
		}	
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(Swarm.getApplication(), "Illegal time value.", "Error", JOptionPane.ERROR_MESSAGE);
		}	
		
		if (!Double.isNaN(j2k))
		{
			double dt = 60;
			if (wvp.getWave() != null)
			{
				double st = wvp.getStartTime();	
				double et = wvp.getEndTime();
				double[] ts = new double[] {st, et};
				addHistory(wvp, ts);
				dt = (et - st);	
			}
			
			double tzo = Time.getTimeZoneOffset(Swarm.config.getTimeZone(wvp.getChannel()));
			double nst = j2k - tzo - dt / 2;
			double net = nst + dt;

			fetchNewWave(wvp, nst, net);
		}	
	}
	
	public void scaleTime(WaveViewPanel wvp, double pct)
	{
		double st = wvp.getStartTime();	
		double et = wvp.getEndTime();
		double[] t = new double[] {st, et};
		addHistory(wvp, t);
		double dt = (et - st) * (1 - pct);
		double mt = (et - st) / 2 + st;
		double nst = mt - dt / 2;
		double net = mt + dt / 2;
		fetchNewWave(wvp, nst, net);
	}

	public void back(WaveViewPanel wvp)
	{
		Stack<double[]> history = histories.get(wvp);
		if (history == null || history.empty())
			return;
			
		final double[] t = history.pop();
		fetchNewWave(wvp, t[0], t[1]);
	}

	public void shiftTime(WaveViewPanel wvp, double pct)
	{
		double st = wvp.getStartTime();	
		double et = wvp.getEndTime();
		double[] t = new double[] {st, et};
		addHistory(wvp, t);
		double dt = (et - st) * pct;
		double nst = st + dt;
		double net = et + dt;
		fetchNewWave(wvp, nst, net);
	}
	
	public void repositionWaves(double st, double et)
	{
		for (WaveViewPanel wave : waves)
		{
			fetchNewWave(wave, st, et);
		}
	}
	
	public Throbber getThrobber()
	{
		return throbber;
	}
	
	// This isn't right, this should be a method of waveviewpanel 
	private void fetchNewWave(final WaveViewPanel wvp, final double nst, final double net)
	{
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						throbber.increment();
//						disableNavigationButtons();
//						System.out.println(waveViewPanel.getDataSource().getClass());
						SeismicDataSource sds = wvp.getDataSource();
						// Hacky fix for bug #84
						Wave sw = null;
						if (sds instanceof CachedDataSource)
							sw = ((CachedDataSource)sds).getBestWave(wvp.getChannel(), nst, net);
						else
							sw = sds.getWave(wvp.getChannel(), nst, net);
						wvp.setWave(sw, nst, net);
						wvp.repaint();
						return null;
					}
					
					public void finished()
					{
						throbber.decrement();
						repaint();	
					}
				};
		worker.start();	
	}

	public void setMaximum(boolean max) throws PropertyVetoException
	{
		if (max)
		{
			Swarm.config.clipboardX = getX();
			Swarm.config.clipboardY = getY();
		}
		super.setMaximum(max);
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		if (waves.size() == 0)
		{
			Dimension dim = this.getSize();
			g.setColor(Color.black);
			g.drawString("Clipboard empty.", dim.width / 2 - 40, dim.height / 2);	
		}	
	}
}