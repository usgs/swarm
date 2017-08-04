package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.plot.data.file.WinDataFile;
import gov.usgs.volcanoes.core.contrib.PngEncoder;
import gov.usgs.volcanoes.core.contrib.PngEncoderB;
import gov.usgs.volcanoes.core.quakeml.Event;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.ui.ExtensionFileFilter;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.FileTypeDialog;
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
import gov.usgs.volcanoes.swarm.data.FileDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesSource;
import gov.usgs.volcanoes.swarm.event.EventDialog;
import gov.usgs.volcanoes.swarm.event.PickMenu;
import gov.usgs.volcanoes.swarm.event.PickMenuBar;
import gov.usgs.volcanoes.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.volcanoes.swarm.time.TimeListener;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * The wave clipboard internal frame.
 *
 * @author Dan Cervelli
 */
public class WaveClipboardFrame extends SwarmFrame {
  public static final long serialVersionUID = -1;
  private static final Color SELECT_COLOR = new Color(200, 220, 241);
  private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);

  private JScrollPane scrollPane;
  private Box waveBox;
  private final List<WaveViewPanel> waves;
  private final Set<WaveViewPanel> selectedSet;
  private JToolBar toolbar;
  private JPanel mainPanel;
  private StatusTextArea statusText;
  private JToggleButton linkButton;
  private JButton sizeButton;
  private JButton syncButton;
  private JButton sortButton;
  private JButton removeAllButton;
  private JButton saveButton;
  private JButton saveAllButton;
  private JButton openButton;
  private JButton captureButton;
  private JButton histButton;
  private final DateFormat saveAllDateFormat;

  private WaveViewPanelListener selectListener;
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

  private PickMenuBar pickMenuBar;
  private JToggleButton pickButton;
  
  private JPopupMenu popup;

  private final Map<WaveViewPanel, Stack<double[]>> histories;

  private final HelicorderViewPanelListener linkListener;

  private boolean heliLinked = true;

  private Throbber throbber;

  private int waveHeight = -1;

  private int lastClickedIndex = -1;

  private WaveClipboardFrame() {
    super("Wave Clipboard", true, true, true, false);
    this.setFocusable(true);
    selectedSet = new HashSet<WaveViewPanel>();
    saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    waves = new ArrayList<WaveViewPanel>();
    histories = new HashMap<WaveViewPanel, Stack<double[]>>();
    createUi();
    linkListener = new HelicorderViewPanelListener() {
      public void insetCreated(final double st, final double et) {
        if (heliLinked) {
          repositionWaves(st, et);
        }
      }
    };
  }

  public static WaveClipboardFrame getInstance() {
    return WaveClipboardFrameHolder.waveClipiboardFrame;
  }

  public HelicorderViewPanelListener getLinkListener() {
    return linkListener;
  }

  private void createUi() {
    this.setFrameIcon(Icons.clipboard);
    this.setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    this.setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    toolbar = SwarmUtil.createToolBar();
    mainPanel = new JPanel(new BorderLayout());

    createMainButtons();
    createWaveButtons();

    mainPanel.add(toolbar, BorderLayout.NORTH);

    waveBox = new Box(BoxLayout.Y_AXIS);
    scrollPane = new JScrollPane(waveBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    statusText = new StatusTextArea(" ");
    statusText.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 1));
    mainPanel.add(statusText, BorderLayout.SOUTH);

    mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 2, 1, 2));
    this.setContentPane(mainPanel);

    createListeners();
    doButtonEnables();
  }
  
  private void createMainButtons() {
    openButton =
        SwarmUtil.createToolBarButton(Icons.open, "Open a saved wave", new OpenActionListener());
    toolbar.add(openButton);

    saveButton =
        SwarmUtil.createToolBarButton(Icons.save, "Save selected wave", new SaveActionListener());
    saveButton.setEnabled(false);
    toolbar.add(saveButton);

    saveAllButton =
        SwarmUtil.createToolBarButton(Icons.saveall, "Save all waves", new SaveAllActionListener());
    saveAllButton.setEnabled(false);
    toolbar.add(saveAllButton);

    toolbar.addSeparator();

    linkButton = SwarmUtil.createToolBarToggleButton(Icons.helilink,
        "Synchronize times with helicorder wave", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            heliLinked = linkButton.isSelected();
          }
        });
    linkButton.setSelected(heliLinked);
    toolbar.add(linkButton);

    syncButton = SwarmUtil.createToolBarButton(Icons.clock, "Synchronize times with selected wave",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            syncChannels();
          }
        });
    syncButton.setEnabled(false);
    toolbar.add(syncButton);

    sortButton = SwarmUtil.createToolBarButton(Icons.geosort,
        "Sort waves by nearest to selected wave", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            sortChannelsByNearest();
          }
        });
    sortButton.setEnabled(false);
    toolbar.add(sortButton);

    toolbar.addSeparator();

    sizeButton = SwarmUtil.createToolBarButton(Icons.resize, "Set clipboard wave size",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            doSizePopup(sizeButton.getX(), sizeButton.getY() + 2 * sizeButton.getHeight());
          }
        });
    toolbar.add(sizeButton);

    removeAllButton = SwarmUtil.createToolBarButton(Icons.deleteall,
        "Remove all waves from clipboard", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            removeWaves();
          }
        });
    removeAllButton.setEnabled(false);
    toolbar.add(removeAllButton);

    toolbar.addSeparator();
    captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save clipboard image (P)",
        new CaptureActionListener());
    UiUtils.mapKeyStrokeToButton(this, "P", "capture", captureButton);
    toolbar.add(captureButton);
    

    toolbar.addSeparator();
    pickMenuBar = new PickMenuBar(this);
    pickButton = SwarmUtil.createToolBarToggleButton(Icons.pick,
        "Pick Mode", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (pickButton.isSelected()) {
              pickMenuBar.setVisible(true);
            } else {
              pickMenuBar.setVisible(false);
            }
            for (WaveViewPanel awp : waves) {
              if (awp instanceof WaveViewPanel) {
                WaveViewPanel wvp = (WaveViewPanel) awp;
                wvp.getSettings().pickEnabled = pickButton.isSelected();
              }
            }
            repaint();
          }
        });
    pickButton.setEnabled(true);
    toolbar.add(pickButton);
    toolbar.add(pickMenuBar);
    pickMenuBar.setVisible(false);
  }

  // TODO: don't write image on event thread
  // TODO: unify with MapFrame.CaptureActionListener
  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      if (waves == null || waves.size() == 0) {
        return;
      }

      final JFileChooser chooser = new JFileChooser();
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
          if (choice != JOptionPane.YES_OPTION) {
            return;
          }
        }
        swarmConfig.lastPath = f.getParent();
      }
      if (f == null) {
        return;
      }

      int height = 0;
      final int width = waves.get(0).getWidth();
      for (final WaveViewPanel panel : waves) {
        height += panel.getHeight();
      }
      
      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics g = image.getGraphics();
      for (final WaveViewPanel panel : waves) {
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
            if (t != null) {
              gotoTime(t);
            }
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

    copyButton = SwarmUtil.createToolBarButton(Icons.clipboard,
        "Place another copy of wave on clipboard (C or Ctrl-C)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            WaveViewPanel wvp = new WaveViewPanel(getSingleSelected());
            addWave(wvp);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "C", "clipboard1", copyButton);
    UiUtils.mapKeyStrokeToButton(this, "control C", "clipboard2", copyButton);
    toolbar.add(copyButton);

    toolbar.addSeparator();

    upButton = SwarmUtil.createToolBarButton(Icons.up, "Move wave(s) up in clipboard (Up arrow)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            moveWaves(-1);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "UP", "up", upButton);
    toolbar.add(upButton);

    downButton = SwarmUtil.createToolBarButton(Icons.down,
        "Move wave down in clipboard (Down arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            moveWaves(1);
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

    toolbar.add(Box.createHorizontalGlue());

    throbber = new Throbber();
    toolbar.add(throbber);

    UiUtils.mapKeyStrokeToAction(this, "control A", "selectAll", new AbstractAction() {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent e) {
        for (final WaveViewPanel wave : waves) {
          select(wave);
        }
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
        for (final WaveViewPanel panel : waves) {
          if (panel != null) {
            panel.setCursorMark(j2k);
          }
        }
      }
    });

    selectListener = new WaveViewPanelAdapter() {
      @Override
      public void mousePressed(final WaveViewPanel src, final MouseEvent e,
          final boolean dragging) {
        requestFocusInWindow();
        final int thisIndex = getWaveIndex(src);
        if (!e.isControlDown() && !e.isShiftDown() && !e.isAltDown()) {
          deselectAll();
          select(src);
        } else if (e.isControlDown()) {
          if (selectedSet.contains(src)) {
            deselect(src);
          } else {
            select(src);
          }
        } else if (e.isShiftDown()) {
          if (lastClickedIndex == -1) {
            select(src);
          } else {
            deselectAll();
            final int min = Math.min(lastClickedIndex, thisIndex);
            final int max = Math.max(lastClickedIndex, thisIndex);
            for (int i = min; i <= max; i++) {
              final WaveViewPanel ps = (WaveViewPanel) waveBox.getComponent(i);
              select(ps);
            }
          }
        }
        lastClickedIndex = thisIndex;
      }

      @Override
      public void waveZoomed(final WaveViewPanel src, final double st, final double et,
          final double nst, final double net) {
        final double[] t = new double[] {st, et};
        addHistory(src, t);
        for (final WaveViewPanel wvp : selectedSet) {
          if (wvp != src) {
            addHistory(wvp, t);
            wvp.zoom(nst, net);
          }
        }
      }

      @Override
      public void waveClosed(final WaveViewPanel src) {
        remove(src);
      }
    };
  }

  private int calculateWaveHeight() {
    if (waveHeight > 0) {
      return waveHeight;
    }

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

  private void doSizePopup(final int x, final int y) {
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
          if (waveHeight == size) {
            mi.setSelected(true);
          }
          group.add(mi);
          popup.add(mi);
        } else {
          popup.addSeparator();
        }
      }
    }
    popup.show(this, x, y);
  }

  private class OpenActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      final JFileChooser chooser = new JFileChooser();
      chooser.resetChoosableFileFilters();
      for (final FileType ft : FileType.getKnownTypes()) {
        final ExtensionFileFilter f = new ExtensionFileFilter(ft.extension, ft.description);
        chooser.addChoosableFileFilter(f);
      }
      chooser.setDialogTitle("Open Wave");
      chooser.setFileFilter(chooser.getAcceptAllFileFilter());
      final File lastPath = new File(swarmConfig.lastPath);
      chooser.setCurrentDirectory(lastPath);
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setMultiSelectionEnabled(true);
      final int result = chooser.showOpenDialog(applicationFrame);
      if (result == JFileChooser.APPROVE_OPTION) {
        FileDataSource.useWinBatch = false;
        FileTypeDialog dialog = new FileTypeDialog(false);
        final File[] fs = chooser.getSelectedFiles();
        swarmConfig.lastPath = fs[0].getParent();
        for (int i = 0; i < fs.length; i++) {
          if (fs[i].isDirectory()) {
            final File[] dfs = fs[i].listFiles();
            if (dfs == null) {
              continue;
            }
            for (int j = 0; j < dfs.length; j++) {
              openFile(dfs[j], dialog);
            }
          } else {
            openFile(fs[i], dialog);
          }
        }
      }
    }
  }

  private class SaveActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      final WaveViewPanel selected = getSingleSelected();
      if (selected == null) {
        return;
      }

      final JFileChooser chooser = new JFileChooser();
      chooser.resetChoosableFileFilters();
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setMultiSelectionEnabled(false);
      chooser.setDialogTitle("Save Wave");

      for (final FileType ft : FileType.values()) {
        if (ft == FileType.UNKNOWN) {
          continue;
        }

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
          if (choice == JOptionPane.YES_OPTION) {
            confirm = true;
          }
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

  private class SaveAllActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      if (waves.size() <= 0) {
        return;
      }

      final FileTypeDialog dialog = new FileTypeDialog(true);
      FileType fileType = FileType.UNKNOWN;
      if (!dialog.isOpen() || (dialog.isOpen() && !dialog.isAssumeSame())) {
        dialog.setVisible(true);

        if (dialog.isCancelled()) {
          fileType = FileType.UNKNOWN;
        } else {
          fileType = dialog.getFileType();
        }
      }

      final JFileChooser chooser = new JFileChooser();
      chooser.resetChoosableFileFilters();
      chooser.setMultiSelectionEnabled(false);
      chooser.setDialogTitle("Save All Files");
      final File lastPath = new File(swarmConfig.lastPath);
      chooser.setCurrentDirectory(lastPath);

      if (fileType.equals(FileType.SEISAN)) {
        // Seisan files are multiplexed, i.e. can contain multiple stations.
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      } else {
        if (!fileType.isCollective) {
          chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
          chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }
      }

      final int result = chooser.showSaveDialog(applicationFrame);
      if (result == JFileChooser.CANCEL_OPTION) {
        return;
      }

      final File f = chooser.getSelectedFile();

      if (f == null) {
        JOptionPane.showMessageDialog(applicationFrame, "Save location not understood.", "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (result == JFileChooser.APPROVE_OPTION) {
        try {
          if (fileType.equals(FileType.SEISAN)) {
            final SeismicDataFile file =
                SeismicDataFile.getFile(f.getAbsolutePath(), FileType.SEISAN);
            for (final WaveViewPanel wvp : waves) {
              Wave sw = wvp.getWave();
              if (sw != null) {
                sw = sw.subset(wvp.getStartTime(), wvp.getEndTime());
                file.putWave(wvp.getChannel(), sw);
                file.write();
              }
            }
          } else {
            if (f.exists() && !f.isDirectory()) {
              return;
            }
            if (!f.exists()) {
              f.mkdir();
            }
            for (final WaveViewPanel wvp : waves) {
              Wave sw = wvp.getWave();

              if (sw != null) {
                sw = sw.subset(wvp.getStartTime(), wvp.getEndTime());
                final String date = saveAllDateFormat.format(J2kSec.asDate(sw.getStartTime()));
                final File dir = new File(f.getPath() + File.separatorChar + date);
                if (!dir.exists()) {
                  dir.mkdir();
                }
                swarmConfig.lastPath = f.getParent();
                final String fn =
                    dir + File.separator + wvp.getChannel().replace(' ', '_') + fileType.extension;
                final SeismicDataFile file = SeismicDataFile.getFile(fn);
                file.putWave(wvp.getChannel(), sw);
                file.write();
              }
            }
          }
          swarmConfig.lastPath = f.getPath();
        } catch (final FileNotFoundException ex) {
          JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), "Directory does not exist.",
              "Error", JOptionPane.ERROR_MESSAGE);
        } catch (final IOException ex) {
          JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), "Error writing file.", "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  /**
   * Open seismic data file.
   * @param f file object
   */
  public void openFile(final File f, FileTypeDialog dialog) {
    SeismicDataFile file = SeismicDataFile.getFile(f);
    if (file == null) {
      FileType fileType = dialog.getFileType();
      if (!dialog.isOpen() || (dialog.isOpen() && !dialog.isAssumeSame())) {
        dialog.setFilename(f.getName());
        dialog.setVisible(true);

        if (dialog.isCancelled() || dialog.getFileType() == null) {
          fileType = FileType.UNKNOWN;
        } else {
          fileType = dialog.getFileType();
        }
        if (fileType == FileType.WIN) { // Open WIN config file
          if (!FileDataSource.useWinBatch) {
            FileDataSource.openWinConfigFileDialog();
          }
          FileDataSource.useWinBatch = dialog.isAssumeSame();
          if (WinDataFile.configFile == null) {
            JOptionPane.showMessageDialog(applicationFrame, "No WIN configuration file set.", "WIN",
                JOptionPane.ERROR_MESSAGE);
            return;
          }
        }
      }
      file = SeismicDataFile.getFile(f, fileType);
    }

    if (file == null) {
      JOptionPane.showMessageDialog(applicationFrame,
          "There was an error opening the file, '" + f.getName() + "'.", "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    try {
      file.read();
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(applicationFrame,
          "There was an error opening the file, '" + f.getName() + "'.\n" + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
    }

    for (final String channel : file.getChannels()) {
      final WaveViewPanel wvp = new WaveViewPanel();
      wvp.setChannel(channel.replaceAll("\\$", " "));
      final CachedDataSource cache = CachedDataSource.getInstance();

      final Wave wave = file.getWave(channel);
      cache.putWave(channel, wave);
      wvp.setDataSource(cache);
      wvp.setWave(wave, wave.getStartTime(), wave.getEndTime());
      WaveClipboardFrame.this.addWave(new WaveViewPanel(wvp));
    }
  }

  private void doButtonEnables() {
    final boolean enable = (waves == null || waves.size() == 0);
    saveButton.setEnabled(!enable);
    sortButton.setEnabled(!enable);
    saveAllButton.setEnabled(!enable);
    syncButton.setEnabled(!enable);
    removeAllButton.setEnabled(!enable);
    saveAllButton.setEnabled(!enable);

    final boolean allowSingle = (selectedSet.size() == 1);
    sortButton.setEnabled(allowSingle);
    syncButton.setEnabled(allowSingle);
    saveButton.setEnabled(allowSingle);

    final boolean allowMulti = (selectedSet.size() > 0);
    upButton.setEnabled(allowMulti);
    downButton.setEnabled(allowMulti);
    backButton.setEnabled(allowMulti);
    expXButton.setEnabled(allowMulti);
    compXButton.setEnabled(allowMulti);
    backButton.setEnabled(allowMulti);
    forwardButton.setEnabled(allowMulti);
    histButton.setEnabled(allowMulti);
    removeButton.setEnabled(allowMulti);
    gotoButton.setEnabled(allowMulti);
  }

  /**
   * Sort wave panels in clipboard.
   */
  public synchronized void sortChannelsByNearest() {
    final WaveViewPanel p = getSingleSelected();
    if (p == null) {
      return;
    }

    final ArrayList<WaveViewPanel> sorted = new ArrayList<WaveViewPanel>(waves.size());
    for (final WaveViewPanel wave : waves) {
      sorted.add(wave);
    }
    
    final Metadata smd = swarmConfig.getMetadata(p.getChannel());
    if (smd == null || Double.isNaN(smd.getLongitude()) || Double.isNaN(smd.getLatitude())) {
      return;
    }

    Collections.sort(sorted, new Comparator<WaveViewPanel>() {
      public int compare(final WaveViewPanel wvp1, final WaveViewPanel wvp2) {
        Metadata md = swarmConfig.getMetadata(wvp1.getChannel());
        final double d1 = smd.distanceTo(md);
        md = swarmConfig.getMetadata(wvp2.getChannel());
        final double d2 = smd.distanceTo(md);
        return Double.compare(d1, d2);
      }
    });

    removeWaves();
    for (final WaveViewPanel wave : sorted) {
      addWave(wave);
    }
    select(p);
  }

  /**
   * Get selected wave panel.
   * @return wave panel
   */
  public synchronized WaveViewPanel getSingleSelected() {
    if (selectedSet.size() != 1) {
      return null;
    }

    WaveViewPanel p = null;
    for (final WaveViewPanel panel : selectedSet) {
      p = panel;
    }
    
    return p;
  }

  /**
   * Synchronize start and end times of wave panels to selected wave panel.
   */
  public synchronized void syncChannels() {
    final WaveViewPanel p = getSingleSelected();
    if (p == null) {
      return;
    }

    final double st = p.getStartTime();
    final double et = p.getEndTime();

    // TODO: thread bug here. must synch iterator below
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        List<WaveViewPanel> copy = null;
        synchronized (WaveClipboardFrame.this) {
          copy = new ArrayList<WaveViewPanel>(waves);
        }
        for (final WaveViewPanel wvp : copy) {
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

  /**
   * Set status bar text.
   * @param s status text
   */
  public void setStatusText(final String s) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        statusText.setText(s);
      }
    });
  }

  /**
   * Add wave panel to clipboard.
   * @param p wave panel
   */
  public synchronized void addWave(final WaveViewPanel p) {
    p.addListener(selectListener);
    p.setOffsets(54, 8, 21, 19);
    p.setAllowClose(true);
    p.setStatusText(statusText);
    p.setAllowDragging(true);
    p.setDisplayTitle(true);
    final int w = scrollPane.getViewport().getSize().width;
    p.setSize(w, calculateWaveHeight());
    p.setBottomBorderColor(Color.GRAY);
    p.createImage();
    p.getSettings().pickEnabled = pickButton.isSelected();
    if (p.wave != null) {
      p.getPickMenu().marksToCoda();
    }
    waveBox.add(p);
    waves.add(p);
    doButtonEnables();
    waveBox.validate();
  }

  private synchronized void deselect(final WaveViewPanel p) {
    selectedSet.remove(p);
    waveToolbar.removeSettings(p.getSettings());
    p.setBackgroundColor(BACKGROUND_COLOR);
    p.createImage();
    doButtonEnables();
  }

  private synchronized void deselectAll() {
    final WaveViewPanel[] panels = selectedSet.toArray(new WaveViewPanel[0]);
    for (final WaveViewPanel p : panels) {
      deselect(p);
    }
  }

  private synchronized void select(final WaveViewPanel p) {
    if (p == null || selectedSet.contains(p)) {
      return;
    }

    selectedSet.add(p);
    doButtonEnables();
    p.setBackgroundColor(SELECT_COLOR);
    DataChooser.getInstance().setNearest(p.getChannel());
    p.createImage();
    waveToolbar.addSettings(p.getSettings());
  }

  private synchronized void remove(final WaveViewPanel p) {
    int i = 0;
    for (i = 0; i < waveBox.getComponentCount(); i++) {
      if (p == waveBox.getComponent(i)) {
        break;
      }
    }

    p.removeListener(selectListener);
    //p.getDataSource().close();
    setStatusText(" ");
    waveBox.remove(i);
    waves.remove(p);
    histories.remove(p);
    doButtonEnables();
    waveBox.validate();
    selectedSet.remove(p);
    lastClickedIndex = Math.min(lastClickedIndex, waveBox.getComponentCount() - 1);
    waveToolbar.removeSettings(p.getSettings());
    repaint();
  }

  /**
   * Remove selected wave view panels.
   */
  public synchronized void remove() {
    final WaveViewPanel[] panels = selectedSet.toArray(new WaveViewPanel[0]);
    for (final WaveViewPanel p : panels) {
      remove(p);
    }
  }

  protected int getWaveIndex(final WaveViewPanel p) {
    int i = 0;
    for (i = 0; i < waveBox.getComponentCount(); i++) {
      if (p == waveBox.getComponent(i)) {
        break;
      }
    }
    return i;
  }
  
  /**
   * Move selected wave panel down one.
   */
  public synchronized void moveDown() {
    final WaveViewPanel p = getSingleSelected();
    if (p == null) {
      return;
    }

    final int i = waves.indexOf(p);
    if (i == waves.size() - 1) {
      return;
    }

    waves.remove(i);
    waves.add(i + 1, p);
    waveBox.remove(p);
    waveBox.add(p, i + 1);
    waveBox.validate();
    repaint();
  }

  /**
   * Move waves. 
   * @param i index
   */
  public synchronized void moveWaves(int i) {
    final WaveViewPanel[] panels = selectedSet.toArray(new WaveViewPanel[0]);

    List<Integer> panelIdx = new ArrayList<Integer>();
    for (int idx = 0; idx < panels.length; idx++) {
      panelIdx.add(waves.indexOf(panels[idx]));
    }

    Collections.sort(panelIdx);
    if (i > 0) {
      Collections.reverse(panelIdx);
      if (panelIdx.get(0) + i > waves.size() - 1) {
        return;
      }
    } else {
      if (panelIdx.get(0) + i < 0) {
        return;
      }
    }

    for (int idx : panelIdx) {
      WaveViewPanel p = waves.get(idx);
      waves.remove(idx);
      waves.add(idx + i, p);
      waveBox.remove(p);
      waveBox.add(p, idx + i);
    }

    waveBox.validate();
    repaint();
  }

  /**
   * Resize wave panel.
   */
  public void resizeWaves() {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        final int w = scrollPane.getViewport().getSize().width;
        for (final WaveViewPanel wave : waves) {
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

  /**
   * Remove all waves.
   */
  public void removeWaves() {
    while (waves.size() > 0) {
      remove(waves.get(0));
    }

    waveBox.validate();
    scrollPane.validate();
    doButtonEnables();
    repaint();
  }

  private void addHistory(final WaveViewPanel wvp, final double[] t) {
    Stack<double[]> history = histories.get(wvp);
    if (history == null) {
      history = new Stack<double[]>();
      histories.put(wvp, history);
    }
    history.push(t);
  }

  private void gotoTime(final WaveViewPanel wvp, double j2k) {
    double dt = 60;
    if (wvp.getWave() != null) {
      final double st = wvp.getStartTime();
      final double et = wvp.getEndTime();
      final double[] ts = new double[] {st, et};
      addHistory(wvp, ts);
      dt = (et - st);
    }

    final double nst = j2k;
    final double net = nst + dt;

    fetchNewWave(wvp, nst, net);
  }

  private void gotoTime(String t) {
    if (t.length() == 12) {
      t += "30";
    }

    try {
      Double j2k = J2kSec.parse("yyyyMMddHHmmss", t);
      if (j2k.isNaN()) {
        throw new ParseException(t, 0);
      }

      for (final WaveViewPanel p : selectedSet) {
        gotoTime(p, j2k);
      }
    } catch (final ParseException e) {
      JOptionPane.showMessageDialog(applicationFrame, "Illegal time value.", "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void scaleTime(final WaveViewPanel wvp, final double pct) {
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

  /**
   * Scale time for selected sets.
   * @param pct percent to scale by
   */
  public void scaleTime(final double pct) {
    for (final WaveViewPanel p : selectedSet) {
      scaleTime(p, pct);
    }
  }

  /**
   * Revert wave panel time to previous time window.
   * @param wvp wave view panel
   */
  public void back(final WaveViewPanel wvp) {
    final Stack<double[]> history = histories.get(wvp);
    if (history == null || history.empty()) {
      return;
    }

    final double[] t = history.pop();
    fetchNewWave(wvp, t[0], t[1]);
  }

  /**
   * Revert selected wave panels' times to previous time window.
   */
  public void back() {
    for (final WaveViewPanel p : selectedSet) {
      back(p);
    }
  }

  private void shiftTime(final WaveViewPanel wvp, final double pct) {
    final double st = wvp.getStartTime();
    final double et = wvp.getEndTime();
    final double[] t = new double[] {st, et};
    addHistory(wvp, t);
    final double dt = (et - st) * pct;
    final double nst = st + dt;
    final double net = et + dt;
    fetchNewWave(wvp, nst, net);
  }

  /**
   * Shift time of selected wave panels.
   * @param pct percent to shift time by.
   */
  public void shiftTime(final double pct) {
    for (final WaveViewPanel p : selectedSet) {
      shiftTime(p, pct);
    }
  }

  /**
   * Reposition wave to given times.
   * @param st start time
   * @param et end time
   */
  public void repositionWaves(final double st, final double et) {
    for (final WaveViewPanel wave : waves) {
      fetchNewWave(wave, st, et);
    }
  }

  public Throbber getThrobber() {
    return throbber;
  }

  // TODO: This isn't right, this should be a method of waveviewpanel
  private void fetchNewWave(final WaveViewPanel wvp, final double nst, final double net) {
    System.err.println(
        "Fetching new wave " + J2kSec.toDateString(nst) + " -> " + J2kSec.toDateString(net));
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        throbber.increment();
        final SeismicDataSource sds = wvp.getDataSource();
        // Hacky fix for bug #84
        Wave sw = null;
        if (sds instanceof CachedDataSource) {
          sw = ((CachedDataSource) sds).getBestWave(wvp.getChannel(), nst, net);
        } else {
          sw = sds.getWave(wvp.getChannel(), nst, net);
        }
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
      g.drawString("Clipboard empty.", dim.width / 2 - 40, dim.height / 2);
    }
  }

  @Override
  public void setVisible(final boolean isVisible) {
    super.setVisible(isVisible);
    if (isVisible) {
      toFront();
    }
  }

  private static class WaveClipboardFrameHolder {
    public static WaveClipboardFrame waveClipiboardFrame = new WaveClipboardFrame();
  }
  
  /**
   * Get wave view panels on clipboard.
   * @return list of wave view panel
   */
  public List<WaveViewPanel> getWaves() {
    return waves;
  }
    
  /**
   * Import event into clipboard. Event must be set first.
   */
  public void importEvent(final Event event) {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        throbber.increment();

        // update event dialog 
        EventDialog.getInstance().setEventDetails(event);
        
        // get wave start and end times
        long firstPick = Long.MAX_VALUE;
        long lastPick = Long.MIN_VALUE;

        for (Pick pick : event.getPicks().values()) {
          firstPick = Math.min(pick.getTime(), firstPick);
          lastPick = Math.max(pick.getTime(), lastPick);
        }
        double waveStart = J2kSec.fromEpoch(firstPick) - 2;
        double waveEnd = J2kSec.fromEpoch(lastPick) + 2;

        // create wave view panels 
        HashMap<String, WaveViewPanel> panels = new HashMap<String, WaveViewPanel>();
        for (Pick pick : event.getPicks().values()) {
          String channel = pick.getChannel().replaceAll("\\$", " ").trim();
          WaveViewPanel wvp = panels.get(channel);
          if (wvp == null) {
            wvp = new WaveViewPanel();
            wvp.setChannel(channel);
            wvp.setStartTime(waveStart);
            wvp.setEndTime(waveEnd);
            boolean foundSource = false;
            for (SeismicDataSource source : SwarmConfig.getInstance().getSources().values()) {
              for (String ch : source.getChannels()) {
                if (ch.equals(channel)) {
                  wvp.setDataSource(source);
                  Wave wave = source.getWave(channel, waveStart, waveEnd);
                  if (wave != null) {
                    wvp.setWave(wave, waveStart, waveEnd);
                    foundSource = true;
                    break;
                  }
                }
              }
              if (foundSource) {
                break;
              }
            }
            // If no data source already available go to IRIS
            if (wvp.getDataSource() == null) {
              WebServicesSource source = new WebServicesSource(pick.getChannel());
              wvp.setDataSource(source);
              Wave wave = source.getWave(channel, waveStart, waveEnd);
              if (wave != null) {
                wvp.setWave(wave, waveStart, waveEnd);
              }
            }
            panels.put(channel, wvp);
          }
          String phaseHint = pick.getPhaseHint();
          PickMenu pickMenu = wvp.getPickMenu();
          pickMenu.setPick(phaseHint, pick, true);
          
        }
        
        // add wave view panels to clipboard
        for (WaveViewPanel wvp : panels.values()) {
          addWave(wvp);
        }
        
        // propagate picks
        for (WaveViewPanel wvp : waves) {
          PickMenu pickMenu = wvp.getPickMenu();
          for (String phase : new String[] {PickMenu.P, PickMenu.S}) {
            Pick pick = pickMenu.getPick(phase);
            if (pick != null && pickMenu.isPickChannel(phase)) {
              pickMenu.propagatePick(phase, pick);
            }
          }
        }
        return null;
      }

      @Override
      public void finished() {
        throbber.decrement();
        repaint();
        EventDialog.getInstance().checkForPicks();
      }
    };
    worker.start();
    
  }

  public JToggleButton getPickButton() {
    return pickButton;
  }

  public PickMenuBar getPickMenuBar() {
    return pickMenuBar;
  }
}
