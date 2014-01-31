package gov.usgs.swarm;

import gov.usgs.swarm.data.CachedDataSource;
//import gov.usgs.swarm.database.model.Attempt;
//import gov.usgs.swarm.database.model.Event;
//import gov.usgs.swarm.database.view.DataSearchDialog;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.swing.filechooser.FileFilter;

/**
 * 
 * @author Dan Cervelli
 */
public class SwarmMenu extends JMenuBar {
	private static final long serialVersionUID = 1L;
	private JMenu fileMenu;
	private JMenuItem openFile;
	private JMenuItem closeFiles;
	private JMenuItem clearCache;
	private JMenuItem exit;

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
	private JMenuItem dataQuery;
	private JMenuItem eventProperties;
	private JMenuItem newEvent;
	private JCheckBoxMenuItem chooser;
	private JCheckBoxMenuItem map;
	private JMenuItem mapToFront;
	private JMenuItem closeAll;
//	public static DataRecord eventPropertiesDialog = null;
	public static boolean eventPropertiesDialogOpened = false;
	public static boolean dataQueryOpened = false;
	public static File[] file1;
	public static boolean empltyDataChooser = true;

	private JMenu helpMenu;
	private JMenuItem about;

	private AboutDialog aboutDialog;

	private Map<JInternalFrame, InternalFrameMenuItem> windows;
	private Map<SwarmLayout, JMenuItem> layouts;
	public int filePointer = 0;

	public enum FileType1 {
		TEXT, SAC, SEED, WIN, SEISAN, UNKNOWN;

		public static FileType1 fromFile(String f) {

			if (f == "SAC")
				return SAC;
			else if (f.equalsIgnoreCase("TEXT"))
				return TEXT;
			else if (f.equalsIgnoreCase("SEISAN"))
				return SEISAN;
			else if (f.equalsIgnoreCase("SEED"))
				return SEED;
			else if (f.equalsIgnoreCase("WIN"))
				return WIN;
			else
				return UNKNOWN;
		}
		
		
		public static FileType1 fromFileExtension(File f) {
			if (f.getPath().endsWith(".sac"))
				return SAC;
			else if (f.getPath().endsWith(".txt"))
				return TEXT;
			else if (f.getPath().endsWith(".seed"))
				return SEED;
			else if (f.getPath().endsWith(".win"))
				return WIN;
			else if (f.getPath().endsWith(".seisan"))
				return SEISAN;
			else
				return UNKNOWN;
		}
	}

	public static boolean DataRecordState() {
		return eventPropertiesDialogOpened;
	}

	public static boolean DataChooserState() {
		return empltyDataChooser;
	}

	public static File[] getFile() {
		return file1;
	}

	public static void setDataQueryState(boolean dataQueryState) {
		dataQueryOpened = dataQueryState;
	}

	public static void setDataRecordState(boolean dataRecordState) {
		eventPropertiesDialogOpened = dataRecordState;
	}

//	public static DataRecord getDataRecord() {
//		return eventPropertiesDialog;
//	}

	public SwarmMenu() {
		super();
		windows = new HashMap<JInternalFrame, InternalFrameMenuItem>();
		layouts = new HashMap<SwarmLayout, JMenuItem>();
		file1 = new File[50];
		createFileMenu();
		createLayoutMenu();
		createWindowMenu();
		createHelpMenu();
	}

	private void createFileMenu() {
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		openFile = new JMenuItem("Open File...");
		openFile.setMnemonic('O');
		openFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = Swarm.getApplication().getFileChooser();
				chooser.resetChoosableFileFilters();
				chooser.setFileFilter(chooser.getAcceptAllFileFilter());
				File lastPath = new File(Swarm.config.lastPath);
				chooser.setCurrentDirectory(lastPath);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(true);
				chooser.setDialogTitle("Open Wave as Data Source");
				FileFilter win = new FileFilter() {

					@Override
					public String getDescription() {
						// TODO Auto-generated method stub
						return null;
					}

					@Override
					public boolean accept(File f) {

						return false;
					}
				};
				chooser.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						String fName = f.getName().toUpperCase();

						return true;
					}

					@Override
					public String getDescription() {
						return "WIN";
					}

				});
				chooser.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						String fName = f.getName().toUpperCase();

						return true;
					}

					@Override
					public String getDescription() {
						return "SAC";
					}

				});
				chooser.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						String fName = f.getName().toUpperCase();

						return true;
					}

					@Override
					public String getDescription() {
						return "SEED";
					}

				});

				chooser.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						String fName = f.getName().toUpperCase();

						return true;
					}

					@Override
					public String getDescription() {
						return "SEISAN";
					}

				});

				chooser.setAcceptAllFileFilterUsed(false);
				int result = chooser.showOpenDialog(Swarm.getApplication());
				if (result == JFileChooser.APPROVE_OPTION) {

					empltyDataChooser = false;
					FileType1 ft = FileType1.fromFile(chooser.getFileFilter()
							.getDescription());
					File[] fs = chooser.getSelectedFiles();
					System.out.println(fs.length);
					file1[filePointer] = fs[0];
					System.out.println(file1.toString());
					Swarm.getFileSource().openFiles(fs, ft);
					filePointer++;

				}
			}
		});
		openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				InputEvent.CTRL_DOWN_MASK));
		fileMenu.add(openFile);

		closeFiles = new JMenuItem("Close Files");
		closeFiles.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getFileSource().flush();
			}
		});
		closeFiles.setMnemonic('l');
		fileMenu.add(closeFiles);

		clearCache = new JMenuItem("Clear Cache");
		clearCache.setMnemonic('C');
		clearCache.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CachedDataSource cache = CachedDataSource.getInstance();
				if (cache != null)
					cache.flush();
			}
		});
		clearCache.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12,
				InputEvent.CTRL_DOWN_MASK));
		fileMenu.add(clearCache);
		fileMenu.addSeparator();

		options = new JMenuItem("Options...");
		options.setMnemonic('O');
		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				OptionsDialog od = new OptionsDialog();
				od.setVisible(true);
			}
		});
		fileMenu.add(options);
		
		
//		newEvent = new JMenuItem("New Event");
//		newEvent.setMnemonic('E');
//		newEvent.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				
//				Event event = new Event();
//				event.setEventLabel("event");
//				event.setEventType("event type");
//				event.persist();
//				
//				Swarm.setSelectedEvent(event);
//				
//				Attempt attempt = new Attempt();
//				attempt.setEvent(event.getId());
//				attempt.persist();
//				Swarm.setSelectedAttempt(attempt);
//				eventProperties.setEnabled(true);Swarm.getApplication().getWaveClipboard().enableMarkerGeneration();
//				
//				if (!eventPropertiesDialogOpened) {
//					eventPropertiesDialogOpened = true;
//					
//					if(eventPropertiesDialog == null){
//						eventPropertiesDialog = new DataRecord();
//					}
//					eventPropertiesDialog.setVisible(true);
//				}else{
//					eventPropertiesDialog.doInitialise();
//				}
//					
//			}
//		});
//		newEvent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
//				InputEvent.CTRL_DOWN_MASK));
//		fileMenu.add(newEvent);

		fileMenu.addSeparator();

		exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().closeApp();
			}
		});
		fileMenu.add(exit);
		add(fileMenu);

		fileMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				CachedDataSource cache = CachedDataSource.getInstance();
				clearCache.setEnabled(!cache.isEmpty());
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});
	}

	private void createLayoutMenu() {
		layoutMenu = new JMenu("Layout");
		layoutMenu.setMnemonic('L');

		saveLayout = new JMenuItem("Save Layout...");
		saveLayout.setMnemonic('S');
		saveLayout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().saveLayout(null);
			}
		});
		saveLayout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
				InputEvent.CTRL_DOWN_MASK));
		layoutMenu.add(saveLayout);

		saveLastLayout = new JMenuItem("Overwrite Last Layout...");
		saveLastLayout.setMnemonic('L');
		saveLastLayout.setEnabled(false);
		saveLastLayout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (lastLayoutName != null)
					Swarm.getApplication().saveLayout(lastLayoutName);
			}
		});
		saveLastLayout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L,
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		layoutMenu.add(saveLastLayout);

		removeLayouts = new JMenuItem("Remove Layout...");
		removeLayouts.setMnemonic('R');
		removeLayouts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RemoveLayoutDialog d = new RemoveLayoutDialog();
				d.setVisible(true);
			}
		});
		layoutMenu.add(removeLayouts);
		layoutMenu.addSeparator();
		add(layoutMenu);
	}

	public String getLastLayoutName() {
		return lastLayoutName;
	}

	public void setLastLayoutName(String ln) {
		lastLayoutName = ln;
		saveLastLayout.setEnabled(true);
		saveLastLayout.setText("Overwrite Last Layout (" + ln + ")");
	}

	public void addLayout(final SwarmLayout sl) {
		JMenuItem mi = new JMenuItem(sl.getName());
		mi.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sl.process();
				setLastLayoutName(sl.getName());
			}
		});
		int i;
		for (i = 4; i < layoutMenu.getItemCount(); i++) {
			JMenuItem m = layoutMenu.getItem(i);
			if (m.getText().compareToIgnoreCase(sl.getName()) >= 0) {
				layoutMenu.add(mi, i);
				break;
			}
		}
		if (i == layoutMenu.getItemCount())
			layoutMenu.add(mi, i);
		layouts.put(sl, mi);
	}

	public void removeLayout(SwarmLayout sl) {
		JMenuItem mi = layouts.get(sl);
		layoutMenu.remove(mi);
		layouts.remove(sl);
	}

	private void createWindowMenu() {
		windowMenu = new JMenu("Window");
		windowMenu.setMnemonic('W');

		chooser = new JCheckBoxMenuItem("Data Chooser");
		chooser.setMnemonic('D');
		chooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().setChooserVisible(
						!Swarm.getApplication().isChooserVisible());
			}
		});
		chooser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D,
				InputEvent.CTRL_DOWN_MASK));
		windowMenu.add(chooser);

		// code added
//		dataQuery = new JMenuItem("Event Search");
//		dataQuery.setMnemonic('S');
//		dataQuery.addActionListener(new ActionListener() {
//			private DataSearchDialog dataSearchDialog;
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (!dataQueryOpened) {
//					dataQueryOpened = true;
//					dataSearchDialog = dataSearchDialog != null ? dataSearchDialog
//							: new DataSearchDialog();
//					dataSearchDialog.setAlwaysOnTop(true);
//					dataSearchDialog.centerOnScreen();
//					// dataQuery.setLocationByPlatform(true);
//					// dataQuery.setBounds(40, 40, 350, 200);
//					// dataQuery.setSize(350, 200);
//					// dataQuery.setLocationRelativeTo(null);
//					dataSearchDialog.setVisible(true);
//				}
//
//			}
//		});
//		dataQuery.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
//				InputEvent.CTRL_DOWN_MASK));
//
//		windowMenu.add(dataQuery);

		// code added data Record

//		eventProperties = new JMenuItem("Event Properties");
//		eventProperties.setMnemonic('R');
//		eventProperties.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
//				InputEvent.CTRL_DOWN_MASK));
//		eventProperties.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if (!eventPropertiesDialogOpened) {
//					if(eventPropertiesDialog == null){
//						eventPropertiesDialog = new DataRecord();
//					}
//					eventPropertiesDialogOpened = true;
//					eventPropertiesDialog.setVisible(true);
//				}
//
//			}
//		});
//		eventProperties.setEnabled(false);
//		windowMenu.add(eventProperties);

		clipboard = new JCheckBoxMenuItem("Wave Clipboard");
		clipboard.setMnemonic('W');
		clipboard.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().setClipboardVisible(
						!Swarm.getApplication().isClipboardVisible());

			}
		});
		clipboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
				InputEvent.CTRL_DOWN_MASK));
		windowMenu.add(clipboard);

		map = new JCheckBoxMenuItem("Map");
		map.setMnemonic('M');
		map.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().setMapVisible(
						!Swarm.getApplication().isMapVisible());
			}
		});
		map.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M,
				InputEvent.CTRL_DOWN_MASK));
		windowMenu.add(map);

		mapToFront = new JMenuItem("Bring Map to Front");
		mapToFront.setMnemonic('F');
		mapToFront.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().setMapVisible(true);
			}
		});
		mapToFront.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
		windowMenu.add(mapToFront);
		windowMenu.addSeparator();

		tileHelicorders = new JMenuItem("Tile Helicorders");
		tileHelicorders.setMnemonic('H');
		tileHelicorders.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().tileHelicorders();
			}
		});
		windowMenu.add(tileHelicorders);

		tileWaves = new JMenuItem("Tile Waves");
		tileWaves.setMnemonic('v');
		tileWaves.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().tileWaves();
			}
		});
		windowMenu.add(tileWaves);

		windowMenu.addSeparator();

		fullScreen = new JMenuItem("Kiosk Mode");
		fullScreen.setMnemonic('K');
		fullScreen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().toggleFullScreenMode();
			}
		});
		fullScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		windowMenu.add(fullScreen);

		windowMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				clipboard.setSelected(Swarm.getApplication()
						.isClipboardVisible());
				chooser.setSelected(Swarm.getApplication().isChooserVisible());
				map.setSelected(Swarm.getApplication().isMapVisible());
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});

		windowMenu.addSeparator();

		closeAll = new JMenuItem("Close All");
		closeAll.setMnemonic('C');
		closeAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Swarm.getApplication().removeAllFrames();
			}
		});
		windowMenu.add(closeAll);

		add(windowMenu);
	}

	private void createHelpMenu() {
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		about = new JMenuItem("About...");
		about.setMnemonic('A');
		aboutDialog = new AboutDialog();
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aboutDialog.update();
				aboutDialog.setVisible(true);
			}
		});

		helpMenu.add(about);
		add(helpMenu);
	}

	private class InternalFrameMenuItem extends JMenuItem {
		private static final long serialVersionUID = 1L;
		private JInternalFrame frame;

		public InternalFrameMenuItem(JInternalFrame f) {
			frame = f;
			setText(f.getTitle());
			setIcon(f.getFrameIcon());
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						if (frame.isIcon())
							frame.setIcon(false);
						frame.toFront();
						frame.setSelected(true);
					} catch (Exception ex) {
					}
				}
			});
		}
	}

	public void addInternalFrame(JInternalFrame f) {
		InternalFrameMenuItem mi = new InternalFrameMenuItem(f);
		windows.put(f, mi);
		windowMenu.add(mi);
	}

	public void removeInternalFrame(JInternalFrame f) {
		InternalFrameMenuItem mi = windows.get(f);
		windows.remove(f);
		windowMenu.remove(mi);
	}

	private class RemoveLayoutDialog extends SwarmDialog {
		private static final long serialVersionUID = 1L;
		private JList layoutList;
		private DefaultListModel model;

		protected RemoveLayoutDialog() {
			super(Swarm.getApplication(), "Remove Layouts", true);
			setSizeAndLocation();
		}

		@Override
		protected void createUI() {
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
			panel.add(new JLabel("Select layouts to remove:"),
					BorderLayout.NORTH);
			panel.add(new JScrollPane(layoutList), BorderLayout.CENTER);
			mainPanel.add(panel, BorderLayout.CENTER);
		}

		@Override
		public void wasOK() {
			List<String> toRemove = Arrays.asList((String[]) layoutList
					.getSelectedValues());

			for (String key : toRemove) {
				SwarmLayout layout = Swarm.config.layouts.get(key);
				if (layout != null) {
					JMenuItem mi = layouts.get(layout);
					layoutMenu.remove(mi);
					Swarm.config.removeLayout(layout);
				}
			}
		}
	}
	
	public void enableEventProperties(){
		eventProperties.setEnabled(true);
	}
	
	public void disableEventProperties(){
		eventProperties.setEnabled(false);
	}
}
