package gov.usgs.volcanoes.swarm.picker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

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
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.volcanoes.swarm.time.TimeListener;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelAdapter;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelListener;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsToolbar;

/**
 * The picker internal frame. Adapted from the WaveClipboardFrame.
 *
 * @author Tom Parker
 */
public class PickerFrame extends SwarmFrame {
  private static final Logger LOGGER = LoggerFactory.getLogger(PickerFrame.class);

  public static final long serialVersionUID = -1;
  private static final Color SELECT_COLOR = new Color(200, 220, 241);
  private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);

  private JScrollPane scrollPane;
  private Box waveBox;
  private PickListPanel pickList;
  private JSplitPane mainPanel;
  private final List<PickerWavePanel> waves;
  private final Set<PickerWavePanel> selectedSet;
  private JToolBar toolbar;
  private JLabel statusLabel;
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

  private JPopupMenu popup;

  private final Map<AbstractWavePanel, Stack<double[]>> histories;

  private boolean heliLinked = true;

  private Throbber throbber;

  private int waveHeight = -1;

  private int lastClickedIndex = -1;

  private Event event;

  public PickerFrame() {
    super("Picker", true, true, true, false);
    event = new Event();

    this.setFocusable(true);
    this.setVisible(true);
    selectedSet = new HashSet<PickerWavePanel>();
    saveAllDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    saveAllDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    waves = new ArrayList<PickerWavePanel>();
    histories = new HashMap<AbstractWavePanel, Stack<double[]>>();
    createUI();
    LOGGER.debug("Finished creating picker frame.");
   }


  private void createUI() {
    this.setFrameIcon(Icons.clipboard);
    this.setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    this.setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    LOGGER.debug("picker frame: {} @ {}", this.getSize(), this.getLocation());

    toolbar = SwarmUtil.createToolBar();
    
    JPanel wavePanel = new JPanel();
    wavePanel.setLayout(new BoxLayout(wavePanel, BoxLayout.PAGE_AXIS));
    
   JPanel tablePanel = new JPanel();
   tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.PAGE_AXIS));
//   tablePanel.setMinimumSize(new Dimension(tablePanel.getMinimumSize().width, 50));

     mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wavePanel, tablePanel);
    mainPanel.setOneTouchExpandable(true);

    createMainButtons();
    createWaveButtons();

    waveBox = new Box(BoxLayout.Y_AXIS);
    scrollPane = new JScrollPane(waveBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    wavePanel.add(scrollPane);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BorderLayout());
    statusLabel = new JLabel(" ");
//    statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 1));
    statusLabel.setBorder(BorderFactory.createEtchedBorder());
    statusPanel.add(statusLabel);
    wavePanel.add(statusPanel);
    tablePanel.add(toolbar, BorderLayout.NORTH);

    pickList = new PickListPanel(event);
    pickList.setPreferredSize(new Dimension(pickList.getPreferredSize().width,25));
    pickList.setParent(mainPanel);
    scrollPane = new JScrollPane(pickList);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    tablePanel.add(scrollPane);

    mainPanel.setResizeWeight(.75);
//    mainPanel.setDividerLocation(550);
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

    copyButton = SwarmUtil.createToolBarButton(Icons.clipboard,
        "Place another copy of wave on clipboard (C or Ctrl-C)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            // TODO: implement
            // if (selected != null)
            // {
            // WaveViewPanel wvp = new WaveViewPanel(selected);
            // wvp.setBackgroundColor(BACKGROUND_COLOR);
            // addWave(wvp);
            // }
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "C", "clipboard1", copyButton);
    UiUtils.mapKeyStrokeToButton(this, "control C", "clipboard2", copyButton);
    toolbar.add(copyButton);

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
        event.notifyObservers();
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
          if (waveHeight == size)
            mi.setSelected(true);
          group.add(mi);
          popup.add(mi);
        } else
          popup.addSeparator();
      }
    }
    popup.show(this, x, y);
  }

  private class OpenActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      final JFileChooser chooser = FileChooser.getFileChooser();
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
        final File[] fs = chooser.getSelectedFiles();

        for (int i = 0; i < fs.length; i++) {
          if (fs[i].isDirectory()) {
            final File[] dfs = fs[i].listFiles();
            if (dfs == null)
              continue;
            for (int j = 0; j < dfs.length; j++)
              openFile(dfs[j]);
            swarmConfig.lastPath = fs[i].getParent();
          } else {
            openFile(fs[i]);
            swarmConfig.lastPath = fs[i].getParent();
          }
        }
      }
    }
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
            // String channel = selected.getChannel().replace(' ', '_');
            final Wave wave = selected.getWave();
            // file.putWave(channel, wave);
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
      if (waves.size() <= 0)
        return;

      final FileTypeDialog dialog = new FileTypeDialog();
      FileType fileType = FileType.UNKNOWN;
      if (!dialog.isOpen() || (dialog.isOpen() && !dialog.isAssumeSame())) {
        dialog.setVisible(true);

        if (dialog.isCancelled())
          fileType = FileType.UNKNOWN;
        else
          fileType = dialog.getFileType();

      }

      final JFileChooser chooser = FileChooser.getFileChooser();
      chooser.resetChoosableFileFilters();
      chooser.setMultiSelectionEnabled(false);
      chooser.setDialogTitle("Save All Files");
      final File lastPath = new File(swarmConfig.lastPath);
      chooser.setCurrentDirectory(lastPath);

      if (!fileType.isCollective)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      else
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

      final int result = chooser.showSaveDialog(applicationFrame);
      if (result == JFileChooser.CANCEL_OPTION)
        return;

      final File f = chooser.getSelectedFile();

      if (f == null) {
        JOptionPane.showMessageDialog(applicationFrame, "Save location not understood.", "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (result == JFileChooser.APPROVE_OPTION) {
        try {
          if (f.exists() && !f.isDirectory())
            return;
          if (!f.exists())
            f.mkdir();
          for (final AbstractWavePanel wvp : waves) {
            Wave sw = wvp.getWave();

            if (sw != null) {
              sw = sw.subset(wvp.getStartTime(), wvp.getEndTime());
              final String date = saveAllDateFormat.format(J2kSec.asDate(sw.getStartTime()));
              final File dir = new File(f.getPath() + File.separatorChar + date);
              if (!dir.exists())
                dir.mkdir();

              swarmConfig.lastPath = f.getParent();
              final String fn =
                  dir + File.separator + wvp.getChannel().replace(' ', '_') + fileType.extension;
              final SeismicDataFile file = SeismicDataFile.getFile(fn);
              file.putWave(wvp.getChannel(), sw);
              file.write();
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

  public void openFile(final File f) {
    SeismicDataFile file = SeismicDataFile.getFile(f);
    if (file == null) {
      final FileTypeDialog dialog = new FileTypeDialog();
      FileType fileType = FileType.UNKNOWN;
      if (!dialog.isOpen() || (dialog.isOpen() && !dialog.isAssumeSame())) {
        dialog.setVisible(true);

        if (dialog.isCancelled())
          fileType = FileType.UNKNOWN;
        else
          fileType = dialog.getFileType();

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
      wvp.setChannel(channel);
      final CachedDataSource cache = CachedDataSource.getInstance();

      final Wave wave = file.getWave(channel);
      cache.putWave(channel, wave);
      wvp.setDataSource(cache);
      wvp.setWave(wave, wave.getStartTime(), wave.getEndTime());
      addWave(new PickerWavePanel(wvp));
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
    upButton.setEnabled(allowSingle);
    downButton.setEnabled(allowSingle);
    sortButton.setEnabled(allowSingle);
    syncButton.setEnabled(allowSingle);
    saveButton.setEnabled(allowSingle);

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

  public synchronized void sortChannelsByNearest() {
    final PickerWavePanel p = getSingleSelected();
    if (p == null)
      return;

    final ArrayList<PickerWavePanel> sorted = new ArrayList<PickerWavePanel>(waves.size());
    for (final PickerWavePanel wave : waves)
      sorted.add(wave);

    final Metadata smd = swarmConfig.getMetadata(p.getChannel());
    if (smd == null || Double.isNaN(smd.getLongitude()) || Double.isNaN(smd.getLatitude()))
      return;

    Collections.sort(sorted, new Comparator<AbstractWavePanel>() {
      public int compare(final AbstractWavePanel wvp1, final AbstractWavePanel wvp2) {
        Metadata md = swarmConfig.getMetadata(wvp1.getChannel());
        final double d1 = smd.distanceTo(md);
        md = swarmConfig.getMetadata(wvp2.getChannel());
        final double d2 = smd.distanceTo(md);
        return Double.compare(d1, d2);
      }
    });

    removeWaves();
    for (final PickerWavePanel wave : sorted)
      addWave(wave);
    select(p);
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

  public AbstractWavePanel getSelected() {
    return null;
  }

  public synchronized void setBaseWave(final PickerWavePanel p) {
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

  public synchronized void addWave(final PickerWavePanel p) {
    LOGGER.debug("Adding listener: {}", selectListener);
    p.addListener(selectListener);
    p.setOffsets(54, 8, 21, 19);
    p.setAllowClose(true);
    p.setStatusLabel(statusLabel);
    p.setAllowDragging(true);
    p.setDisplayTitle(true);
    p.setEvent(event);
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

  private synchronized void remove(final WaveViewPanel p) {
    event.remove(p.getChannel());
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
    final WaveViewPanel[] panels = selectedSet.toArray(new WaveViewPanel[0]);
    for (final WaveViewPanel p : panels)
      remove(p);
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
      g.drawString("Clipboard empty.", dim.width / 2 - 40, dim.height / 2);
    }
  }

  @Override
  public void setVisible(final boolean isVisible) {
    super.setVisible(isVisible);
    if (isVisible)
      toFront();
  }
}
