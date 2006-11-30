package gov.usgs.swarm;

import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.10  2006/10/26 00:48:18  dcervelli
 * Makes sure windows aren't iconified before bringing them to the front.
 *
 * Revision 1.9  2006/08/14 22:43:52  dcervelli
 * Remove layout dialog box.
 *
 * Revision 1.8  2006/08/09 21:55:20  cervelli
 * Moved layout saving code to Swarm.java.
 *
 * Revision 1.7  2006/08/07 22:33:10  cervelli
 * Open file items.
 *
 * Revision 1.6  2006/08/04 21:18:02  cervelli
 * Map to front item.
 *
 * Revision 1.5  2006/08/01 23:41:14  cervelli
 * Clear cache menu item.
 *
 * Revision 1.4  2006/07/31 15:56:18  cervelli
 * Added Layout menu.
 *
 * Revision 1.3  2006/07/22 20:31:00  cervelli
 * Added Window>Map menu item.
 *
 * Revision 1.2  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.1  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * @author Dan Cervelli
 */
public class SwarmMenu extends JMenuBar
{
	private static final long serialVersionUID = 1L;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem closeFiles;
	private JMenuItem clearCache;
	private JMenuItem exit;
	
	private JMenu editMenu;
	private JMenuItem options;
	
	private String lastLayoutName;
	private JMenu layoutMenu;
	private JMenuItem saveLayout;
	private JMenuItem saveLastLayout;
	private JMenuItem removeLayouts;
	
	private JMenu windowMenu;
	private JMenuItem tileWaves;
	private JMenuItem tileHelicorders;
	private JMenuItem fullScreen;
	private JCheckBoxMenuItem clipboard;
	private JCheckBoxMenuItem chooser;
	private JCheckBoxMenuItem map;
	private JMenuItem mapToFront;
	private JMenuItem closeAll;
	
	private JMenu helpMenu;
	private JMenuItem about;
	
	private AboutDialog aboutDialog;
	
	private Map<JInternalFrame, InternalFrameMenuItem> windows;
	private Map<SwarmLayout, JMenuItem> layouts;
	
	public SwarmMenu()
	{
		super();
		windows = new HashMap<JInternalFrame, InternalFrameMenuItem>();
		layouts = new HashMap<SwarmLayout, JMenuItem>();
		createFileMenu();
		createEditMenu();
		createLayoutMenu();
		createWindowMenu();
		createHelpMenu();
	}
	
	private void createFileMenu()
	{
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		
		openFile = new JMenuItem("Open File...");
		openFile.setMnemonic('O');
		openFile.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						JFileChooser chooser = Swarm.getApplication().getFileChooser();
						chooser.resetChoosableFileFilters();
						chooser.setFileFilter(chooser.getAcceptAllFileFilter());
						File lastPath = new File(Swarm.config.lastPath);
						chooser.setCurrentDirectory(lastPath);
						chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						chooser.setMultiSelectionEnabled(true);
						int result = chooser.showOpenDialog(Swarm.getApplication());
						if (result == JFileChooser.APPROVE_OPTION)
						{
							File[] fs = chooser.getSelectedFiles();
							Swarm.getFileSource().openFiles(fs);
						}
					}
				});
		openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		fileMenu.add(openFile);
		
		closeFiles = new JMenuItem("Close Files");
		closeFiles.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getFileSource().flush();
					}
				});
		closeFiles.setMnemonic('l');
		fileMenu.add(closeFiles);
		
		clearCache = new JMenuItem("Clear Cache");
		clearCache.setMnemonic('C');
		clearCache.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						CachedDataSource cache = Swarm.getCache();
						if (cache != null)
							cache.flush();
					}
				});
		clearCache.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, KeyEvent.CTRL_DOWN_MASK));
		fileMenu.add(clearCache);
		fileMenu.addSeparator();
		
		exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().closeApp();
					}
				});
		fileMenu.add(exit);
		add(fileMenu);
		
		fileMenu.addMenuListener(new MenuListener()
				{
					public void menuSelected(MenuEvent e)
					{
						CachedDataSource cache = Swarm.getCache();
						clearCache.setEnabled(!cache.isEmpty());
					}
		
					public void menuDeselected(MenuEvent e)
					{}
					
					public void menuCanceled(MenuEvent e)
					{}
				});
	}

	private void createEditMenu()
	{
		editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		options = new JMenuItem("Options...");
		options.setMnemonic('O');
		options.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						OptionsDialog od = new OptionsDialog();
						od.setVisible(true);
					}
				});
		editMenu.add(options);
		add(editMenu);
	}
	
	private void createLayoutMenu()
	{
		layoutMenu = new JMenu("Layout");
		layoutMenu.setMnemonic('L');
		
		saveLayout = new JMenuItem("Save Layout...");
		saveLayout.setMnemonic('S');
		saveLayout.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().saveLayout(null);
					}
				});
		saveLayout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
		layoutMenu.add(saveLayout);
		
		saveLastLayout = new JMenuItem("Overwrite Last Layout...");
		saveLastLayout.setMnemonic('L');
		saveLastLayout.setEnabled(false);
		saveLastLayout.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (lastLayoutName != null)
							Swarm.getApplication().saveLayout(lastLayoutName);
					}
				});
		saveLastLayout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		layoutMenu.add(saveLastLayout);
		
		removeLayouts = new JMenuItem("Remove Layout...");
		removeLayouts.setMnemonic('R');
		removeLayouts.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						RemoveLayoutDialog d = new RemoveLayoutDialog();
						d.setVisible(true);
					}
				});
		layoutMenu.add(removeLayouts);
		layoutMenu.addSeparator();
		add(layoutMenu);
	}
	
	public String getLastLayoutName()
	{
		return lastLayoutName;
	}
	
	public void setLastLayoutName(String ln)
	{
		lastLayoutName = ln;
		saveLastLayout.setEnabled(true);
		saveLastLayout.setText("Overwrite Last Layout (" + ln + ")");
	}
	
	public void addLayout(final SwarmLayout sl)
	{
		JMenuItem mi = new JMenuItem(sl.getName());
		mi.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						sl.process();
						setLastLayoutName(sl.getName());
					}
				});
		int i;
		for (i = 4; i < layoutMenu.getItemCount(); i++)
		{
			JMenuItem m = layoutMenu.getItem(i);
			if (m.getText().compareToIgnoreCase(sl.getName()) >= 0)
			{
				layoutMenu.add(mi, i);
				break;
			}
		}
		if (i == layoutMenu.getItemCount())
			layoutMenu.add(mi, i);
		layouts.put(sl, mi);
	}
	
	public void removeLayout(SwarmLayout sl)
	{
		JMenuItem mi = layouts.get(sl);
		layoutMenu.remove(mi);
		layouts.remove(sl);
	}
	
	private void createWindowMenu()
	{
		windowMenu = new JMenu("Window");
		windowMenu.setMnemonic('W');
		
		chooser = new JCheckBoxMenuItem("Data Chooser");
		chooser.setMnemonic('D');
		chooser.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setChooserVisible(!Swarm.getApplication().isChooserVisible());
					}
				});
		chooser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
		windowMenu.add(chooser);
		
		clipboard = new JCheckBoxMenuItem("Wave Clipboard");
		clipboard.setMnemonic('W');
		clipboard.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setClipboardVisible(!Swarm.getApplication().isClipboardVisible());
					}
				});
		clipboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
		windowMenu.add(clipboard);
		
		map = new JCheckBoxMenuItem("Map");
		map.setMnemonic('M');
		map.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setMapVisible(!Swarm.getApplication().isMapVisible());
					}
				});
		map.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK));
		windowMenu.add(map);
		
		mapToFront = new JMenuItem("Bring Map to Front");
		mapToFront.setMnemonic('F');
		mapToFront.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().setMapVisible(true);
					}
				});
		mapToFront.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
		windowMenu.add(mapToFront);
		windowMenu.addSeparator();
		
		tileHelicorders = new JMenuItem("Tile Helicorders");
		tileHelicorders.setMnemonic('H');
		tileHelicorders.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().tileHelicorders();
					}
				});
		windowMenu.add(tileHelicorders);
		
		tileWaves = new JMenuItem("Tile Waves");
		tileWaves.setMnemonic('v');
		tileWaves.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().tileWaves();
					}
				});
		windowMenu.add(tileWaves);
		
		windowMenu.addSeparator();
		
		fullScreen = new JMenuItem("Kiosk Mode");
		fullScreen.setMnemonic('K');
		fullScreen.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().toggleFullScreenMode();
					}
				});
		fullScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		windowMenu.add(fullScreen);
		
		windowMenu.addMenuListener(new MenuListener()
				{
					public void menuSelected(MenuEvent e)
					{
						clipboard.setSelected(Swarm.getApplication().isClipboardVisible());
						chooser.setSelected(Swarm.getApplication().isChooserVisible());
						map.setSelected(Swarm.getApplication().isMapVisible());
					}

					public void menuDeselected(MenuEvent e)
					{}
					
					public void menuCanceled(MenuEvent e)
					{}
				});

		windowMenu.addSeparator();
		
		closeAll = new JMenuItem("Close All");
		closeAll.setMnemonic('C');
		closeAll.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						Swarm.getApplication().removeAllFrames();
					}
				});
		windowMenu.add(closeAll);
		
		add(windowMenu);
	}
	
	private void createHelpMenu()
	{
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		about = new JMenuItem("About...");
		about.setMnemonic('A');
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
		add(helpMenu);
	}
	
	private class InternalFrameMenuItem extends JMenuItem
	{
		private static final long serialVersionUID = 1L;
		private JInternalFrame frame;
		
		public InternalFrameMenuItem(JInternalFrame f)
		{
			frame = f;
			setText(f.getTitle());
			setIcon(f.getFrameIcon());
			addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							try
							{
								if (frame.isIcon())
									frame.setIcon(false);
								frame.toFront();
								frame.setSelected(true);
							}
							catch (Exception ex) {}
						}
					});
		}
	}
	
	public void addInternalFrame(JInternalFrame f)
	{
		InternalFrameMenuItem mi = new InternalFrameMenuItem(f);
		windows.put(f, mi);
		windowMenu.add(mi);
	}
	
	public void removeInternalFrame(JInternalFrame f)
	{
		InternalFrameMenuItem mi = windows.get(f);
		windows.remove(f);
		windowMenu.remove(mi);
	}
	
	private class RemoveLayoutDialog extends SwarmDialog
	{
		private static final long serialVersionUID = 1L;
		private JList layoutList;
		private DefaultListModel model;
		
		protected RemoveLayoutDialog()
		{
			super(Swarm.getApplication(), "Remove Layouts", true);
			setSizeAndLocation();
		}
		
		protected void createUI()
		{
			super.createUI();
			Set<String> keys = Swarm.config.layouts.keySet();
			List<String> sls = new ArrayList<String>();
			sls.addAll(keys);
			Collections.sort(sls, Util.getIgnoreCaseStringComparator());
			model = new DefaultListModel();
			for (String sl : sls)
				model.addElement(sl);
			layoutList = new JList(model);
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
			int h = Math.max(200, Math.min(350, sls.size() * 19));
			panel.setPreferredSize(new Dimension(200, h));
			panel.add(new JLabel("Select layouts to remove:"), BorderLayout.NORTH);
			panel.add(new JScrollPane(layoutList), BorderLayout.CENTER);
			mainPanel.add(panel, BorderLayout.CENTER);
		}
		
		public void wasOK()
		{
			Object[] toRemove = layoutList.getSelectedValues();
			for (int i = 0; i < toRemove.length; i++)
			{
				String key = (String)toRemove[i];
				SwarmLayout layout = Swarm.config.layouts.get(key);
				if (layout != null)
				{
					JMenuItem mi = layouts.get(layout);
					layoutMenu.remove(mi);
					Swarm.config.removeLayout(layout);
				}
			}
		}
	}
}
