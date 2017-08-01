package gov.usgs.volcanoes.swarm;

import gov.usgs.plot.data.file.FileType;
import gov.usgs.volcanoes.core.quakeml.EventSet;
import gov.usgs.volcanoes.core.ui.ExtensionFileFilter;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.FileDataSource;
import gov.usgs.volcanoes.swarm.internalFrame.InternalFrameListener;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Swarm Menu.
 * @author Dan Cervelli
 */
public class SwarmMenu extends JMenuBar implements InternalFrameListener {
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
  private JCheckBoxMenuItem chooser;
  private JCheckBoxMenuItem map;
  private JMenuItem mapToFront;
  private JMenuItem closeAll;

  private JMenu helpMenu;
  private JMenuItem about;

  private AboutDialog aboutDialog;

  private Map<JInternalFrame, InternalFrameMenuItem> windows;
  private Map<SwarmLayout, JMenuItem> layouts;

  /**
   * Constructor.
   */
  public SwarmMenu() {
    super();
    windows = new HashMap<JInternalFrame, InternalFrameMenuItem>();
    layouts = new HashMap<SwarmLayout, JMenuItem>();
    createFileMenu();
    createLayoutMenu();
    createWindowMenu();
    createHelpMenu();
    SwarmInternalFrames.addInternalFrameListener(this);
  }

  private void createFileMenu() {
    fileMenu = new JMenu("File");
    fileMenu.setMnemonic('F');

    openFile = new JMenuItem("Open File...");
    openFile.setMnemonic('O');
    openFile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.resetChoosableFileFilters();
        for (FileType ft : FileType.getKnownTypes()) {
          ExtensionFileFilter f = new ExtensionFileFilter(ft.extension, ft.description);
          chooser.addChoosableFileFilter(f);
        }

        chooser.setFileFilter(chooser.getAcceptAllFileFilter());
        File lastPath = new File(SwarmConfig.getInstance().lastPath);
        chooser.setCurrentDirectory(lastPath);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Open Wave as Data Source");
        int result = chooser.showOpenDialog(Swarm.getApplicationFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
          File[] fs = chooser.getSelectedFiles();
          SwarmConfig.getInstance().lastPath = fs[0].getParent();
          FileDataSource.getInstance().openFiles(fs);
        }
      }
    });
    openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    fileMenu.add(openFile);

    closeFiles = new JMenuItem("Close Files");
    closeFiles.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        FileDataSource.getInstance().flush();
      }
    });
    closeFiles.setMnemonic('l');
    fileMenu.add(closeFiles);

    clearCache = new JMenuItem("Clear Cache");
    clearCache.setMnemonic('C');
    clearCache.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CachedDataSource cache = CachedDataSource.getInstance();
        if (cache != null) {
          cache.flush();
        }
      }
    });
    clearCache.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, KeyEvent.CTRL_DOWN_MASK));
    fileMenu.add(clearCache);
    
    fileMenu.addSeparator();
    
    JMenuItem importEvent = new JMenuItem("Import Events...");
    importEvent.setMnemonic('I');
    importEvent.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("QuakeML (.xml)", "xml"));
        chooser.setCurrentDirectory(new File(SwarmConfig.getInstance().lastPath));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Open QuakeML Event");
        int result = chooser.showOpenDialog(Swarm.getApplicationFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
          File[] fs = chooser.getSelectedFiles();
          for (File f : fs) {
            try {
              FileInputStream fis = new FileInputStream(f);
              SwarmConfig.getInstance().lastPath = f.getParent();
              EventSet events = EventSet.parseQuakeml(fis);
              MapFrame.getInstance().getHypocenterLayer().add(events);
            } catch (FileNotFoundException e1) {
              JOptionPane.showMessageDialog(null, e1.getMessage());
            } catch (IOException e1) {
              // TODO Auto-generated catch block
              e1.printStackTrace();
            } catch (ParserConfigurationException e1) {
              // TODO Auto-generated catch block
              e1.printStackTrace();
            } catch (SAXException e1) {
              // TODO Auto-generated catch block
              e1.printStackTrace();
            }
            
          }
        }
      }
    });
    importEvent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
    fileMenu.add(importEvent);

    fileMenu.addSeparator();
    
    options = new JMenuItem("Options...");
    options.setMnemonic('O');
    options.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        OptionsDialog od = new OptionsDialog();
        od.setVisible(true);
      }
    });
    fileMenu.add(options);

    fileMenu.addSeparator();

    exit = new JMenuItem("Exit");
    exit.setMnemonic('x');
    exit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Swarm.getApplication().closeApp();
      }
    });
    fileMenu.add(exit);
    add(fileMenu);

    fileMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        CachedDataSource cache = CachedDataSource.getInstance();
        clearCache.setEnabled(!cache.isEmpty());
      }

      public void menuDeselected(MenuEvent e) {}

      public void menuCanceled(MenuEvent e) {}
    });
  }

  private void createLayoutMenu() {
    layoutMenu = new JMenu("Layout");
    layoutMenu.setMnemonic('L');

    saveLayout = new JMenuItem("Save Layout...");
    saveLayout.setMnemonic('S');
    saveLayout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Swarm.getApplication().saveLayout(null);
      }
    });
    saveLayout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
    layoutMenu.add(saveLayout);

    saveLastLayout = new JMenuItem("Overwrite Last Layout...");
    saveLastLayout.setMnemonic('L');
    saveLastLayout.setEnabled(false);
    saveLastLayout.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (lastLayoutName != null) {
          Swarm.getApplication().saveLayout(lastLayoutName);
        }
      }
    });
    saveLastLayout.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
    layoutMenu.add(saveLastLayout);

    removeLayouts = new JMenuItem("Remove Layout...");
    removeLayouts.setMnemonic('R');
    removeLayouts.addActionListener(new ActionListener() {
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

  /**
   * Set last layout name.
   * @param ln layout name
   */
  public void setLastLayoutName(String ln) {
    lastLayoutName = ln;
    saveLastLayout.setEnabled(true);
    saveLastLayout.setText("Overwrite Last Layout (" + ln + ")");
  }

  /**
   * All layout.
   * @param sl swarm layout
   */
  public void addLayout(final SwarmLayout sl) {
    JMenuItem mi = new JMenuItem(sl.getName());
    mi.addActionListener(new ActionListener() {
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
    if (i == layoutMenu.getItemCount()) {
      layoutMenu.add(mi, i);
    }
    layouts.put(sl, mi);
  }

  /**
   * Remove layout.
   * @param sl swarm layout
   */
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
      public void actionPerformed(ActionEvent e) {
        Swarm.getApplication().setChooserVisible(!Swarm.getApplication().isChooserVisible());
      }
    });
    chooser.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
    windowMenu.add(chooser);

    clipboard = new JCheckBoxMenuItem("Wave Clipboard");
    clipboard.setMnemonic('W');
    clipboard.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        WaveClipboardFrame waveClipboard = WaveClipboardFrame.getInstance();
        waveClipboard.setVisible(!waveClipboard.isVisible());
      }
    });
    clipboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
    windowMenu.add(clipboard);

    final MapFrame mapFrame = MapFrame.getInstance();
    map = new JCheckBoxMenuItem("Map");
    map.setMnemonic('M');
    map.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mapFrame.setVisible(!mapFrame.isVisible());
      }
    });
    map.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK));
    windowMenu.add(map);

    mapToFront = new JMenuItem("Bring Map to Front");
    mapToFront.setMnemonic('F');
    mapToFront.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mapFrame.setVisible(true);
      }
    });
    mapToFront.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0));
    windowMenu.add(mapToFront);
    windowMenu.addSeparator();

    tileHelicorders = new JMenuItem("Tile Helicorders");
    tileHelicorders.setMnemonic('H');
    tileHelicorders.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Swarm.getApplication().tileHelicorders();
      }
    });
    windowMenu.add(tileHelicorders);

    tileWaves = new JMenuItem("Tile Waves");
    tileWaves.setMnemonic('v');
    tileWaves.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Swarm.getApplication().tileWaves();
      }
    });
    windowMenu.add(tileWaves);

    windowMenu.addSeparator();

    fullScreen = new JMenuItem("Kiosk Mode");
    fullScreen.setMnemonic('K');
    fullScreen.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Swarm.getApplication().toggleFullScreenMode();
      }
    });
    fullScreen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
    windowMenu.add(fullScreen);

    windowMenu.addMenuListener(new MenuListener() {
      public void menuSelected(MenuEvent e) {
        clipboard.setSelected(WaveClipboardFrame.getInstance().isVisible());
        chooser.setSelected(Swarm.getApplication().isChooserVisible());
        map.setSelected(MapFrame.getInstance().isVisible());
      }

      public void menuDeselected(MenuEvent e) {}

      public void menuCanceled(MenuEvent e) {}
    });

    windowMenu.addSeparator();

    closeAll = new JMenuItem("Close All");
    closeAll.setMnemonic('C');
    closeAll.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwarmInternalFrames.removeAllFrames();
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
        public void actionPerformed(ActionEvent e) {
          try {
            if (frame.isIcon()) {
              frame.setIcon(false);
            }
            frame.toFront();
            frame.setSelected(true);
          } catch (Exception ex) {
            // do nothing
          }
        }
      });
    }
  }

  private class RemoveLayoutDialog extends SwarmModalDialog {
    private static final long serialVersionUID = 1L;
    private JList<String> layoutList;
    private DefaultListModel<String> model;

    protected RemoveLayoutDialog() {
      super(Swarm.getApplicationFrame(), "Remove Layouts");
      setSizeAndLocation();
    }

    protected void createUi() {
      super.createUi();
      Set<String> keys = swarmConfig.layouts.keySet();
      List<String> sls = new ArrayList<String>();
      sls.addAll(keys);
      Collections.sort(sls, StringUtils.getCaseInsensitiveStringComparator());
      model = new DefaultListModel<String>();
      for (String sl : sls) {
        model.addElement(sl);
      }
      layoutList = new JList<String>(model);
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
      int h = Math.max(200, Math.min(350, sls.size() * 19));
      panel.setPreferredSize(new Dimension(200, h));
      panel.add(new JLabel("Select layouts to remove:"), BorderLayout.NORTH);
      panel.add(new JScrollPane(layoutList), BorderLayout.CENTER);
      mainPanel.add(panel, BorderLayout.CENTER);
    }

    public void wasOk() {
      Object[] toRemove = layoutList.getSelectedValuesList().toArray();

      for (Object key : toRemove) {
        SwarmLayout layout = swarmConfig.layouts.get((String) key);
        if (layout != null) {
          JMenuItem mi = layouts.get(layout);
          layoutMenu.remove(mi);
          swarmConfig.removeLayout(layout);
        }
      }
    }
  }

  /**
   * @see gov.usgs.volcanoes.swarm.internalFrame.InternalFrameListener
   * #internalFrameAdded(javax.swing.JInternalFrame)
   */
  public void internalFrameAdded(JInternalFrame f) {
    InternalFrameMenuItem mi = new InternalFrameMenuItem(f);
    windows.put(f, mi);
    windowMenu.add(mi);
  }

  /**
   * @see gov.usgs.volcanoes.swarm.internalFrame.InternalFrameListener
   * #internalFrameRemoved(javax.swing.JInternalFrame)
   */
  public void internalFrameRemoved(JInternalFrame f) {
    InternalFrameMenuItem mi = windows.get(f);
    windows.remove(f);
    windowMenu.remove(mi);
  }
}
