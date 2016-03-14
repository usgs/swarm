package gov.usgs.volcanoes.swarm.picker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.util.Pair;
import gov.usgs.volcanoes.core.contrib.PngEncoder;
import gov.usgs.volcanoes.core.contrib.PngEncoderB;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.ui.ExtensionFileFilter;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.FileChooser;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwarmFrame;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.chooser.DataChooser;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.event.Event;
import gov.usgs.volcanoes.swarm.picker.hypo71.Hypo71Locator;
import gov.usgs.volcanoes.swarm.time.TimeListener;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelAdapter;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelListener;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsToolbar;

/**
 * The picker internal frame. Adapted from the WaveClipboardFrame.
 *
 * @author Tom Parker
 */
public class PickerFrame extends SwarmFrame implements EventObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(PickerFrame.class);

  public static final long serialVersionUID = -1;
  private static final Color SELECT_COLOR = new Color(200, 220, 241);
  private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);

  private JScrollPane scrollPane;
  private Box waveBox;
  private PickListPanel pickList;
  private JSplitPane mainPanel;
  private PickerWavePanel baseWave;
  private final List<PickerWavePanel> waves;
  private final Set<PickerWavePanel> selectedSet;
  private JToolBar toolbar;
  private JLabel statusLabel;
  private JButton sizeButton;
  private JButton saveButton;
  private JButton captureButton;
  private JButton histButton;
  private JButton locateButton;

  private final DateFormat saveAllDateFormat;

  private WaveViewPanelListener selectListener;
  private WaveViewSettingsToolbar waveToolbar;

  private JButton upButton;
  private JButton downButton;
  private JButton removeButton;
  private JButton compXButton;
  private JButton expXButton;
  private JButton forwardButton;
  private JButton backButton;
  private JButton gotoButton;

  private JPopupMenu popup;

  private final Map<AbstractWavePanel, Stack<double[]>> histories;

  private Throbber throbber;

  private int waveHeight = -1;

  private int lastClickedIndex = -1;

  private Event event;

  public PickerFrame() {
    super("Picker", true, true, true, false);
    event = new Event("testIId");
    event.addObserver(this);

    this.setFocusable(true);
    this.setVisible(true);
    selectedSet = new HashSet<PickerWavePanel>();
    saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    waves = new ArrayList<PickerWavePanel>();
    histories = new HashMap<AbstractWavePanel, Stack<double[]>>();
    createUI();
    LOGGER.debug("Finished creating picker frame.");
    this.setVisible(true);

    // getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("typed q"),
    // "pPick");
    getInputMap().put(KeyStroke.getKeyStroke("typed q"), "pPick");
    getActionMap().put("pPick", new AbstractAction() {
      private static final long serialVersionUID = -1;

      public void actionPerformed(final ActionEvent e) {
        findWavePanel();
      }
    });

  }

  public PickerFrame(Event event) {
    super("Picker", true, true, true, false);
    this.event = event;
    event.addObserver(this);

    this.setFocusable(true);
    this.setVisible(true);
    selectedSet = new HashSet<PickerWavePanel>();
    saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    waves = new ArrayList<PickerWavePanel>();
    histories = new HashMap<AbstractWavePanel, Stack<double[]>>();
    createUI();
    LOGGER.debug("Finished creating picker frame.");
    this.setVisible(true);

    // getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("typed q"),
    // "pPick");
    getInputMap().put(KeyStroke.getKeyStroke("typed q"), "pPick");
    getActionMap().put("pPick", new AbstractAction() {
      private static final long serialVersionUID = -1;

      public void actionPerformed(final ActionEvent e) {
        findWavePanel();
      }
    });
  }
  
  private void findWavePanel() {
    Point p = MouseInfo.getPointerInfo().getLocation();
    SwingUtilities.convertPointFromScreen(p, waveBox);
    int idx = p.y / calculateWaveHeight();
    PickerWavePanel panel = waves.get(idx);
    panel.instantPick(Phase.PhaseType.P);
  }

  private void createUI() {
    this.setFrameIcon(Icons.ruler);
    this.setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    this.setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    LOGGER.debug("picker frame: {} @ {}", this.getSize(), this.getLocation());

    toolbar = SwarmUtil.createToolBar();

    JPanel wavePanel = new JPanel();
    wavePanel.setLayout(new BoxLayout(wavePanel, BoxLayout.PAGE_AXIS));

    JPanel tablePanel = new JPanel();
    tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.PAGE_AXIS));

    mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wavePanel, tablePanel);
    mainPanel.setOneTouchExpandable(true);

    createMainButtons();
    createWaveButtons();
    wavePanel.add(toolbar);

    waveBox = new Box(BoxLayout.Y_AXIS);
    waveBox.setTransferHandler(new TransferHandler("") {
      public boolean canImport(TransferSupport supp) {
        return supp.isDataFlavorSupported(DataFlavor.stringFlavor);
      }

      public boolean importData(TransferSupport supp) {
        if (!canImport(supp)) {
          return false;
        }

        Transferable t = supp.getTransferable();
        String data = null;
        try {
          data = (String) t.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }

        // Fetch the drop location
        DropLocation loc = supp.getDropLocation();

        // Insert the data at this location
        LOGGER.debug("DROP {} @ {}", data, loc);
        String[] chans = data.split("\n");
        for (String chan : chans) {
          addWave(chan);
        }
        return true;
      }

    });
    scrollPane = new JScrollPane(waveBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    wavePanel.add(scrollPane);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BorderLayout());
    statusLabel = new JLabel(" ");
    statusLabel.setBorder(BorderFactory.createEtchedBorder());
    statusPanel.add(statusLabel);
    statusPanel
        .setMaximumSize(new Dimension(Integer.MAX_VALUE, statusPanel.getPreferredSize().height));
    wavePanel.add(statusPanel);

    pickList = new PickListPanel(event);
    pickList.setParent(mainPanel);

    scrollPane = new JScrollPane(pickList);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    tablePanel.add(scrollPane);

    mainPanel.setResizeWeight(.75);
    this.setContentPane(mainPanel);

    createListeners();
    doButtonEnables();
  }

  private void createMainButtons() {
    saveButton =
        SwarmUtil.createToolBarButton(Icons.save, "Save selected wave", new SaveActionListener());
    saveButton.setEnabled(false);
    toolbar.add(saveButton);

    toolbar.addSeparator();

    sizeButton =
        SwarmUtil.createToolBarButton(Icons.resize, "Set wave height", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            doSizePopup();
          }
        });
    toolbar.add(sizeButton);

    toolbar.addSeparator();
    captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save pick image (P)",
        new CaptureActionListener());
    UiUtils.mapKeyStrokeToButton(this, "P", "capture", captureButton);
    toolbar.add(captureButton);
  }

  // TODO: don't write image on event thread
  // TODO: unify with MapFrame.CaptureActionListener
  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      if (waves == null || waves.size() == 0)
        return;

      final JFileChooser chooser = FileChooser.getFileChooser();
      final File lastPath = new File(swarmConfig.lastPath);
      chooser.setCurrentDirectory(lastPath);
      chooser.setSelectedFile(new File("clipboard.png"));
      chooser.setDialogTitle("Save Clipboard Screen Capture");
      final int result = chooser.showSaveDialog(applicationFrame);
      File f = null;
      if (result == JFileChooser.APPROVE_OPTION) {
        f = chooser.getSelectedFile();

        if (f.exists()) {
          final int choice = JOptionPane.showConfirmDialog(applicationFrame,
              "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
          if (choice != JOptionPane.YES_OPTION)
            return;
        }
        swarmConfig.lastPath = f.getParent();
      }
      if (f == null)
        return;

      int height = 0;
      final int width = waves.get(0).getWidth();
      for (final AbstractWavePanel panel : waves)
        height += panel.getHeight();

      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics g = image.getGraphics();
      for (final AbstractWavePanel panel : waves) {
        panel.paint(g);
        g.translate(0, panel.getHeight());
      }
      try {
        final PngEncoderB png = new PngEncoderB(image, false, PngEncoder.FILTER_NONE, 7);
        final FileOutputStream out = new FileOutputStream(f);
        final byte[] bytes = png.pngEncode();
        out.write(bytes);
        out.close();
      } catch (final Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private void createWaveButtons() {
    toolbar.addSeparator();

    backButton = SwarmUtil.createToolBarButton(Icons.left, "Scroll back time 20% (Left arrow)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            shiftTime(-0.20);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
    toolbar.add(backButton);

    forwardButton = SwarmUtil.createToolBarButton(Icons.right,
        "Scroll forward time 20% (Right arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            shiftTime(0.20);
          }
        });
    toolbar.add(forwardButton);
    UiUtils.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);

    gotoButton =
        SwarmUtil.createToolBarButton(Icons.gototime, "Go to time (Ctrl-G)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            final String t = JOptionPane.showInputDialog(applicationFrame,
                "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time",
                JOptionPane.PLAIN_MESSAGE);
            if (t != null)
              gotoTime(t);
          }
        });
    toolbar.add(gotoButton);
    UiUtils.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);

    compXButton = SwarmUtil.createToolBarButton(Icons.xminus,
        "Shrink sample time 20% (Alt-left arrow, +)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            scaleTime(0.20);
          }
        });
    toolbar.add(compXButton);
    UiUtils.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
    UiUtils.mapKeyStrokeToButton(this, "EQUALS", "compx2", compXButton);
    UiUtils.mapKeyStrokeToButton(this, "shift EQUALS", "compx2", compXButton);

    expXButton = SwarmUtil.createToolBarButton(Icons.xplus,
        "Expand sample time 20% (Alt-right arrow, -)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            scaleTime(-0.20);
          }
        });
    toolbar.add(expXButton);
    UiUtils.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
    UiUtils.mapKeyStrokeToButton(this, "MINUS", "expx", expXButton);

    histButton = SwarmUtil.createToolBarButton(Icons.timeback, "Last time settings (Backspace)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            back();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "BACK_SPACE", "back", histButton);
    toolbar.add(histButton);
    toolbar.addSeparator();

    waveToolbar = new WaveViewSettingsToolbar(null, toolbar, this);

    toolbar.addSeparator();

    upButton = SwarmUtil.createToolBarButton(Icons.up, "Move wave up in clipboard (Up arrow)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            moveUp();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "UP", "up", upButton);
    toolbar.add(upButton);

    downButton = SwarmUtil.createToolBarButton(Icons.down,
        "Move wave down in clipboard (Down arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            moveDown();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "DOWN", "down", downButton);
    toolbar.add(downButton);

    removeButton = SwarmUtil.createToolBarButton(Icons.delete,
        "Remove wave from clipboard (Delete)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            remove();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "DELETE", "remove", removeButton);
    toolbar.add(removeButton);

    locateButton = SwarmUtil.createToolBarButton(Icons.locate, "Locate", new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        EventLocator locator = new Hypo71Locator();
//        try {
//          locator.locate(event);
//        } catch (IOException e1) {
//          e1.printStackTrace();
//        };
      }

      private void particleMotionPlot() {
        // TODO Auto-generated method stub

      }
    });
    toolbar.add(locateButton);

    toolbar.add(Box.createHorizontalGlue());

    throbber = new Throbber();
    toolbar.add(throbber);

    UiUtils.mapKeyStrokeToAction(this, "control A", "selectAll", new AbstractAction() {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent e) {
        for (final PickerWavePanel wave : waves)
          select(wave);
      }
    });
  }

  private void createListeners() {
    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameActivated(final InternalFrameEvent e) {}

      @Override
      public void internalFrameDeiconified(final InternalFrameEvent e) {
        resizeWaves();
      }

      @Override
      public void internalFrameClosing(final InternalFrameEvent e) {
        setVisible(false);
      }

      @Override
      public void internalFrameClosed(final InternalFrameEvent e) {}
    });

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        resizeWaves();
      }
    });

    WaveViewTime.addTimeListener(new TimeListener() {
      public void timeChanged(final double j2k) {
        for (final AbstractWavePanel panel : waves) {
          if (panel != null)
            panel.setCursorMark(j2k);
        }
      }
    });

    selectListener = new WaveViewPanelAdapter() {
      public void mousePressed(final AbstractWavePanel src, final MouseEvent e,
          final boolean dragging) {
        PickerWavePanel panel = (PickerWavePanel) src;
        LOGGER.debug("wave selected.");
        requestFocusInWindow();
        final int thisIndex = getWaveIndex(src);
        if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
          deselectAll();
          select(panel);
        } else if (e.isControlDown()) {
          if (selectedSet.contains(src))
            deselect(src);
          else
            select(panel);
        } else if (e.isShiftDown()) {
          if (lastClickedIndex == -1)
            select(panel);
          else {
            deselectAll();
            final int min = Math.min(lastClickedIndex, thisIndex);
            final int max = Math.max(lastClickedIndex, thisIndex);
            for (int i = min; i <= max; i++) {
              final PickerWavePanel ps = (PickerWavePanel) waveBox.getComponent(i);
              select(ps);
            }
          }
        }
        lastClickedIndex = thisIndex;
//        event.notifyObservers();
        mainPanel.validate();
        mainPanel.repaint();
      }

      @Override
      public void waveZoomed(final AbstractWavePanel src, final double st, final double et,
          final double nst, final double net) {
        final double[] t = new double[] {st, et};
        addHistory(src, t);
        for (final AbstractWavePanel wvp : selectedSet) {
          if (wvp != src) {
            addHistory(wvp, t);
            wvp.zoom(nst, net);
          }
        }
      }

      @Override
      public void waveClosed(final AbstractWavePanel src) {
        LOGGER.debug("Removing wave: {}", src.getChannel());
        remove(src);
      }
    };
  }

  private int calculateWaveHeight() {
    if (waveHeight > 0)
      return waveHeight;

    final int w = scrollPane.getViewport().getSize().width;
    int h = (int) Math.round(w * 60.0 / 300.0);
    h = Math.min(200, h);
    h = Math.max(h, 80);
    return h;
  }

  private void setWaveHeight(final int s) {
    waveHeight = s;
    resizeWaves();
  }

  private void doSizePopup() {
    if (popup == null) {
      final String[] labels = new String[] {"Auto", null, "Tiny", "Small", "Medium", "Large"};
      final int[] sizes = new int[] {-1, -1, 50, 100, 160, 230};
      popup = new JPopupMenu();
      final ButtonGroup group = new ButtonGroup();
      for (int i = 0; i < labels.length; i++) {
        if (labels[i] != null) {
          final int size = sizes[i];
          final JRadioButtonMenuItem mi = new JRadioButtonMenuItem(labels[i]);
          mi.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
              setWaveHeight(size);
            }
          });
          if (waveHeight == size)
            mi.setSelected(true);
          group.add(mi);
          popup.add(mi);
        } else
          popup.addSeparator();
      }
    }
    popup.show(sizeButton.getParent(), sizeButton.getX(), sizeButton.getY());
  }

  private class SaveActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      final AbstractWavePanel selected = getSingleSelected();
      if (selected == null)
        return;

      final JFileChooser chooser = FileChooser.getFileChooser();
      chooser.resetChoosableFileFilters();
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setMultiSelectionEnabled(false);
      chooser.setDialogTitle("Save Wave");

      for (final FileType ft : FileType.values()) {
        if (ft == FileType.UNKNOWN)
          continue;

        final ExtensionFileFilter f = new ExtensionFileFilter(ft.extension, ft.description);
        chooser.addChoosableFileFilter(f);
      }

      chooser.setFileFilter(chooser.getAcceptAllFileFilter());

      final File lastPath = new File(swarmConfig.lastPath);
      chooser.setCurrentDirectory(lastPath);
      final String fileName = selected.getChannel().replace(' ', '_') + ".sac";
      chooser.setSelectedFile(new File(fileName));
      final int result = chooser.showSaveDialog(applicationFrame);
      if (result == JFileChooser.APPROVE_OPTION) {
        final File f = chooser.getSelectedFile();
        boolean confirm = true;
        if (f.exists()) {
          if (f.isDirectory()) {
            JOptionPane.showMessageDialog(applicationFrame,
                "You can not select an existing directory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
          }
          confirm = false;
          final int choice = JOptionPane.showConfirmDialog(applicationFrame,
              "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
          if (choice == JOptionPane.YES_OPTION)
            confirm = true;
        }

        if (confirm) {
          try {
            swarmConfig.lastPath = f.getParent();
            final String fn = f.getPath();
            final SeismicDataFile file = SeismicDataFile.getFile(fn);
            final Wave wave = selected.getWave();
            file.putWave(selected.getChannel(), wave);
            file.write();
          } catch (final FileNotFoundException ex) {
            JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), "Directory does not exist.",
                "Error", JOptionPane.ERROR_MESSAGE);
          } catch (final IOException ex) {
            JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), "Error writing file.",
                "Error", JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    }
  }



  private void doButtonEnables() {
    final boolean enable = (waves == null || waves.size() == 0);

    final boolean allowSingle = (selectedSet.size() == 1);
    upButton.setEnabled(allowSingle);
    downButton.setEnabled(allowSingle);

    final boolean allowMulti = (selectedSet.size() > 0);
    backButton.setEnabled(allowMulti);
    expXButton.setEnabled(allowMulti);
    compXButton.setEnabled(allowMulti);
    backButton.setEnabled(allowMulti);
    forwardButton.setEnabled(allowMulti);
    histButton.setEnabled(allowMulti);
    removeButton.setEnabled(allowMulti);
    gotoButton.setEnabled(allowMulti);
  }

  public synchronized PickerWavePanel getSingleSelected() {
    if (selectedSet.size() != 1)
      return null;

    PickerWavePanel p = null;
    for (final PickerWavePanel panel : selectedSet)
      p = panel;

    return p;
  }

  public synchronized void syncChannels() {
    final AbstractWavePanel p = getSingleSelected();
    if (p == null)
      return;

    final double st = p.getStartTime();
    final double et = p.getEndTime();

    // TODO: thread bug here. must synch iterator below
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        List<AbstractWavePanel> copy = null;
        synchronized (PickerFrame.this) {
          copy = new ArrayList<AbstractWavePanel>(waves);
        }
        for (final AbstractWavePanel wvp : copy) {
          if (wvp != p) {
            if (wvp.getDataSource() != null) {
              addHistory(wvp, new double[] {wvp.getStartTime(), wvp.getEndTime()});
              final Wave sw = wvp.getDataSource().getWave(wvp.getChannel(), st, et);
              wvp.setWave(sw, st, et);
            }
          }
        }
        return null;
      }

      @Override
      public void finished() {
        repaint();
      }
    };
    worker.start();
  }

  public void setStatusText(final String s) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        statusLabel.setText(s);
      }
    });
  }

  public synchronized void setBaseWave(final PickerWavePanel p) {
    baseWave = p;
    addWave(p);
    doButtonEnables();
    waveBox.validate();

    String channel = p.getChannel();

    final List<Pair<Double, String>> nrst =
        Metadata.findNearest(SwarmConfig.getInstance().getMetadata(), channel);

    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        for (Pair<Double, String> item : nrst) {
          if (item.item1 > 20000) {
            break;
          } else if (item.item2.matches(".*(B|E|H)H(Z|E|N).*")) {
            PickerWavePanel p2 = new PickerWavePanel(p);
            p2.setChannel(item.item2);
            SeismicDataSource sds = p2.getDataSource();
            double st = p2.getStartTime();
            double et = p2.getEndTime();
            Wave wave = sds.getWave(item.item2, st, et);
            if (wave != null && wave.isData()) {
              p2.setWave(wave, st, et);
              p2.getWaveViewSettings().autoScaleAmpMemory = false;
              addWave(p2);
              System.out.println(String.format("%s (%.1f km)", item.item2, item.item1 / 1000));
            } else {
              LOGGER.debug("Skipping {}, no data.", item.item2);
            }
          }
        }
        return null;
      }

      @Override
      public void finished() {
        waveBox.validate();
        validate();
        repaint();
      }
    };
    worker.start();
  }

  public synchronized void addWave(final String chan) {
    PickerWavePanel p2 = new PickerWavePanel(baseWave);
    p2.setChannel(chan);
    SeismicDataSource sds = p2.getDataSource();
    double st = p2.getStartTime();
    double et = p2.getEndTime();
    Wave wave = sds.getWave(chan, st, et);
    if (wave != null && wave.isData()) {
      p2.setWave(wave, st, et);
      p2.getWaveViewSettings().autoScaleAmpMemory = false;
      addWave(p2);
    }
  }

  public synchronized void addWave(final PickerWavePanel p) {
    p.addListener(selectListener);
    p.setOffsets(54, 8, 21, 19);
    p.setAllowClose(true);
    p.setStatusLabel(statusLabel);
    p.setAllowDragging(true);
    p.setDisplayTitle(true);
//    p.setEvent(event);
    p.setParent(mainPanel);
    final int w = scrollPane.getViewport().getSize().width;
    p.setSize(w, calculateWaveHeight());
    p.setBottomBorderColor(Color.GRAY);
    p.createImage();
    waveBox.add(p);
    waves.add(p);
    LOGGER.debug("{} panels; {} waves", waveBox.getComponentCount(), waves.size());
  }

  private synchronized void deselect(final AbstractWavePanel p) {
    selectedSet.remove(p);
    pickList.deselect(p.getChannel());
    waveToolbar.removeSettings(p.getSettings());
    p.setBackgroundColor(BACKGROUND_COLOR);
    p.createImage();
    doButtonEnables();
  }

  private synchronized void deselectAll() {
    final AbstractWavePanel[] panels = selectedSet.toArray(new AbstractWavePanel[0]);
    pickList.deselectAll();
    for (final AbstractWavePanel p : panels) {
      deselect(p);
    }
  }

  private synchronized void select(final PickerWavePanel p) {
    if (p == null || selectedSet.contains(p))
      return;

    pickList.select(p.getChannel());
    selectedSet.add(p);
    doButtonEnables();
    p.setBackgroundColor(SELECT_COLOR);
    DataChooser.getInstance().setNearest(p.getChannel());
    p.createImage();
    waveToolbar.addSettings(p.getSettings());
  }

  private synchronized void remove(final AbstractWavePanel p) {
//    event.remove(p.getChannel());
    int i = 0;
    for (i = 0; i < waveBox.getComponentCount(); i++) {
      if (p == waveBox.getComponent(i))
        break;
    }

    p.removeListener(selectListener);
    p.getDataSource().close();
    setStatusText(" ");
    waveBox.remove(i);
    waves.remove(p);
    histories.remove(p);
    doButtonEnables();
    waveBox.validate();
    selectedSet.remove(p);
    pickList.remove(p.getChannel());
    lastClickedIndex = Math.min(lastClickedIndex, waveBox.getComponentCount() - 1);
    waveToolbar.removeSettings(p.getSettings());
//    event.notifyObservers();
    validate();
    repaint();
  }

  protected int getWaveIndex(final AbstractWavePanel p) {
    int i = 0;
    for (i = 0; i < waveBox.getComponentCount(); i++) {
      if (p == waveBox.getComponent(i))
        break;
    }
    return i;
  }

  public synchronized void remove() {
    final PickerWavePanel[] panels = selectedSet.toArray(new PickerWavePanel[0]);
    for (final PickerWavePanel p : panels) {
      LOGGER.debug("removing panel {}", p);
      waveBox.remove(p);
      waves.remove(p);
//      event.remove(p.getChannel());
      // pickList.remove(p);
      validate();
      repaint();
    }
  }

  public synchronized void moveDown() {
    final PickerWavePanel p = getSingleSelected();
    if (p == null)
      return;

    final int i = waves.indexOf(p);
    if (i == waves.size() - 1)
      return;

    waves.remove(i);
    waves.add(i + 1, p);
    waveBox.remove(p);
    waveBox.add(p, i + 1);
    waveBox.validate();
    repaint();
  }

  public synchronized void moveUp() {
    final PickerWavePanel p = getSingleSelected();
    if (p == null)
      return;

    final int i = waves.indexOf(p);
    if (i == 0)
      return;

    waves.remove(i);
    waves.add(i - 1, p);
    waveBox.remove(p);
    waveBox.add(p, i - 1);
    waveBox.validate();
    repaint();
  }

  public void resizeWaves() {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        final int w = scrollPane.getViewport().getSize().width;
        for (final AbstractWavePanel wave : waves) {
          wave.setSize(w, calculateWaveHeight());
          wave.createImage();
        }
        return null;
      }

      @Override
      public void finished() {
        waveBox.validate();
        validate();
        repaint();
      }
    };
    worker.start();
  }

  public void removeWaves() {
    while (waves.size() > 0)
      remove(waves.get(0));

    waveBox.validate();
    scrollPane.validate();
    doButtonEnables();
    repaint();
  }

  private void addHistory(final AbstractWavePanel wvp, final double[] t) {
    Stack<double[]> history = histories.get(wvp);
    if (history == null) {
      history = new Stack<double[]>();
      histories.put(wvp, history);
    }
    history.push(t);
  }

  public void gotoTime(final AbstractWavePanel wvp, String t) {
    double j2k = Double.NaN;
    try {
      if (t.length() == 12)
        t = t + "30";

      j2k = J2kSec.parse("yyyyMMddHHmmss", t);
    } catch (final Exception e) {
      JOptionPane.showMessageDialog(applicationFrame, "Illegal time value.", "Error",
          JOptionPane.ERROR_MESSAGE);
    }

    if (!Double.isNaN(j2k)) {
      double dt = 60;
      if (wvp.getWave() != null) {
        final double st = wvp.getStartTime();
        final double et = wvp.getEndTime();
        final double[] ts = new double[] {st, et};
        addHistory(wvp, ts);
        dt = (et - st);
      }

      final double tzo =
          swarmConfig.getTimeZone(wvp.getChannel()).getOffset(System.currentTimeMillis()) / 1000;

      final double nst = j2k - tzo - dt / 2;
      final double net = nst + dt;

      fetchNewWave(wvp, nst, net);
    }
  }

  public void gotoTime(final String t) {
    for (final AbstractWavePanel p : selectedSet)
      gotoTime(p, t);
  }

  public void scaleTime(final AbstractWavePanel wvp, final double pct) {
    final double st = wvp.getStartTime();
    final double et = wvp.getEndTime();
    final double[] t = new double[] {st, et};
    addHistory(wvp, t);
    final double dt = (et - st) * (1 - pct);
    final double mt = (et - st) / 2 + st;
    final double nst = mt - dt / 2;
    final double net = mt + dt / 2;
    fetchNewWave(wvp, nst, net);
  }

  public void scaleTime(final double pct) {
    for (final AbstractWavePanel p : selectedSet)
      scaleTime(p, pct);
  }

  public void back(final AbstractWavePanel wvp) {
    final Stack<double[]> history = histories.get(wvp);
    if (history == null || history.empty())
      return;

    final double[] t = history.pop();
    fetchNewWave(wvp, t[0], t[1]);
  }

  public void back() {
    for (final AbstractWavePanel p : selectedSet)
      back(p);
  }

  private void shiftTime(final AbstractWavePanel wvp, final double pct) {
    final double st = wvp.getStartTime();
    final double et = wvp.getEndTime();
    final double[] t = new double[] {st, et};
    addHistory(wvp, t);
    final double dt = (et - st) * pct;
    final double nst = st + dt;
    final double net = et + dt;
    fetchNewWave(wvp, nst, net);
  }

  public void shiftTime(final double pct) {
    for (final AbstractWavePanel p : selectedSet)
      shiftTime(p, pct);
  }

  public void repositionWaves(final double st, final double et) {
    for (final AbstractWavePanel wave : waves) {
      fetchNewWave(wave, st, et);
    }
  }

  public Throbber getThrobber() {
    return throbber;
  }

  // TODO: This isn't right, this should be a method of waveviewpanel
  private void fetchNewWave(final AbstractWavePanel wvp, final double nst, final double net) {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        throbber.increment();
        final SeismicDataSource sds = wvp.getDataSource();
        // Hacky fix for bug #84
        Wave sw = null;
        if (sds instanceof CachedDataSource)
          sw = ((CachedDataSource) sds).getBestWave(wvp.getChannel(), nst, net);
        else
          sw = sds.getWave(wvp.getChannel(), nst, net);
        wvp.setWave(sw, nst, net);
        wvp.repaint();
        return null;
      }

      @Override
      public void finished() {
        throbber.decrement();
        repaint();
      }
    };
    worker.start();
  }

  @Override
  public void setMaximum(final boolean max) throws PropertyVetoException {
    if (max) {
      swarmConfig.clipboardX = getX();
      swarmConfig.clipboardY = getY();
    }
    super.setMaximum(max);
  }

  @Override
  public void paint(final Graphics g) {
    super.paint(g);
    if (waves.size() == 0) {
      final Dimension dim = this.getSize();
      g.setColor(Color.black);
      g.drawString("Picker empty.", dim.width / 2 - 40, dim.height / 2);
    }
  }

  @Override
  public void setVisible(final boolean isVisible) {
    LOGGER.debug("Visible = {}", isVisible);
    super.setVisible(isVisible);
    if (isVisible)
      toFront();
  }


  public void updateEvent() {
//    LOGGER.debug("event is empty? {}", event.getChannels().isEmpty());
//    saveButton.setEnabled(!event.getChannels().isEmpty());
  }
}
