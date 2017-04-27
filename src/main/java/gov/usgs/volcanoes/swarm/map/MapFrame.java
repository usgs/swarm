package gov.usgs.volcanoes.swarm.map;

import gov.usgs.proj.GeoRange;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.contrib.PngEncoder;
import gov.usgs.volcanoes.core.contrib.PngEncoderB;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.FileChooser;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Kioskable;
import gov.usgs.volcanoes.swarm.SwarmFrame;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.volcanoes.swarm.map.MapPanel.DragMode;
import gov.usgs.volcanoes.swarm.map.MapPanel.LabelSetting;
import gov.usgs.volcanoes.swarm.map.hypocenters.HypocenterLayer;
import gov.usgs.volcanoes.swarm.options.SwarmOptions;
import gov.usgs.volcanoes.swarm.options.SwarmOptionsListener;
import gov.usgs.volcanoes.swarm.time.UiTime;
import gov.usgs.volcanoes.swarm.wave.MultiMonitor;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsToolbar;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map frame.
 * @author Dan Cervelli
 */
public class MapFrame extends SwarmFrame implements Runnable, Kioskable, SwarmOptionsListener {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MapFrame.class);

  private JToolBar toolbar;
  private JPanel mainPanel;
  private JButton optionsButton;
  private JButton labelButton;
  private JLabel statusLabel;
  private JToggleButton linkButton;
  private JToggleButton realtimeButton;
  private JButton compXButton;
  private JButton expXButton;
  private JButton forwardTimeButton;
  private JButton backTimeButton;
  private JButton gotoButton;
  private JButton timeHistoryButton;
  private JButton captureButton;
  private JButton clipboardButton;

  private JToggleButton dragButton;
  private JToggleButton dragZoomButton;
  private JToggleButton rulerButton;

  private WaveViewSettingsToolbar waveToolbar;
  private WaveViewPanel selected;

  private final Thread updateThread;

  private MapPanel mapPanel;

  private Throbber throbber;

  private int spanIndex = 3;
  private boolean realtime = true;

  private long refreshInterval = 1000;

  private HelicorderViewPanelListener linkListener;

  private boolean heliLinked = true;

  private Border border;

  private MapSettingsDialog mapSettingsDialog;

  private HypocenterLayer hypocenterLayer;

  private MapFrame() {
    super("Map", true, true, true, false);
    this.setFocusable(true);
    UiTime.touchTime();

    mapSettingsDialog = new MapSettingsDialog(this);

    createUi();
    addLayers();

    updateThread = new Thread(this, "Map Update");
    updateThread.start();
    SwarmOptions.addOptionsListener(this);
  }

  private void addLayers() {
    try {
      hypocenterLayer = new HypocenterLayer();
      hypocenterLayer.setMapPanel(mapPanel);
      addLayer(hypocenterLayer);
    } catch (MalformedURLException ex) {
      LOGGER.error("Unable to load layer. {}", ex);
    }
  }

  public static MapFrame getInstance() {
    return MapFrameHolder.mapFrame;
  }

  @Override
  public void saveLayout(final ConfigFile cf, final String prefix) {
    super.saveLayout(cf, prefix);
    mapPanel.saveLayout(cf, prefix + ".panel");
  }

  /**
   * Process layout.
   * @param cf config file
   */
  public void processLayout(final ConfigFile cf) {
    processStandardLayout(cf);
    mapPanel.processLayout(cf.getSubConfig("panel"));
    final LabelSetting ls = mapPanel.getLabelSetting();
    labelButton.setIcon(ls.getIcon());
  }

  /**
   * Create UI.
   */
  private void createUi() {
    setFrameIcon(Icons.earth);
    setSize(swarmConfig.mapWidth, swarmConfig.mapHeight);
    setLocation(swarmConfig.mapX, swarmConfig.mapY);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    mainPanel = new JPanel(new BorderLayout());

    createToolbar();

    mainPanel.add(toolbar, BorderLayout.NORTH);

    mapPanel = new MapPanel();

    border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3),
        LineBorder.createGrayLineBorder());
    mapPanel.setBorder(border);
    mainPanel.add(mapPanel, BorderLayout.CENTER);
    statusLabel = new JLabel(" ");
    statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));
    mainPanel.add(statusLabel, BorderLayout.SOUTH);
    setContentPane(mainPanel);

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(final ComponentEvent e) {
        if (!mapPanel.imageValid()) {
          mapPanel.resetImage();
        }
      }
    });

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosing(final InternalFrameEvent e) {
        setVisible(false);
      }
    });

    linkListener = new HelicorderViewPanelListener() {
      public void insetCreated(final double st, final double et) {
        if (heliLinked) {
          if (!realtime) {
            mapPanel.timePush();
          }

          setRealtime(false);
          mapPanel.setTimes(st, et, true);
        }
      }
    };

    setVisible(true);
  }

  @Override
  public void setVisible(final boolean isVisible) {
    LOGGER.debug("Frame closing");
    super.setVisible(isVisible);
    if (mapPanel != null) {
      mapPanel.setVisible(isVisible);
    }
    if (isVisible) {
      toFront();
    }
  }

  @Override
  public void setMaximum(final boolean max) throws PropertyVetoException {
    if (max) {
      swarmConfig.mapX = getX();
      swarmConfig.mapY = getY();
    }
    super.setMaximum(max);
  }

  public GeoRange getRange() {
    return mapPanel.getRange();
  }

  private void createToolbar() {
    toolbar = SwarmUtil.createToolBar();
    optionsButton =
        SwarmUtil.createToolBarButton(Icons.settings, "Map options", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapSettingsDialog.setToCurrent();
            mapSettingsDialog.setVisible(true);
          }
        });
    toolbar.add(optionsButton);

    labelButton = SwarmUtil.createToolBarButton(Icons.label_some, "Change label settings",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            final LabelSetting ls = mapPanel.getLabelSetting().next();
            labelButton.setIcon(ls.getIcon());
            mapPanel.setLabelSetting(ls);
          }
        });
    toolbar.add(labelButton);

    toolbar.addSeparator();

    realtimeButton =
        SwarmUtil.createToolBarToggleButton(Icons.clock, "Realtime mode (N)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            setRealtime(realtimeButton.isSelected());
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "N", "realtime", realtimeButton);
    realtimeButton.setSelected(realtime);
    toolbar.add(realtimeButton);

    linkButton = SwarmUtil.createToolBarToggleButton(Icons.helilink,
        "Synchronize times with helicorder wave (H)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            heliLinked = linkButton.isSelected();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "H", "helilink", linkButton);
    linkButton.setSelected(heliLinked);
    toolbar.add(linkButton);

    toolbar.addSeparator();

    final JButton earthButton = SwarmUtil.createToolBarButton(Icons.earth,
        "Zoom out to full scale (Home)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            final Point2D.Double c = new Point2D.Double(mapPanel.getCenter().x, 0);
            mapPanel.setCenterAndScale(c, 100000);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "HOME", "home", earthButton);
    toolbar.add(earthButton);

    dragButton =
        SwarmUtil.createToolBarToggleButton(Icons.drag, "Drag map (D)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.setDragMode(DragMode.DRAG_MAP);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "D", "drag", dragButton);
    dragButton.setSelected(true);
    toolbar.add(dragButton);

    dragZoomButton = SwarmUtil.createToolBarToggleButton(Icons.dragbox, "Zoom into box (B)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.setDragMode(DragMode.BOX);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "B", "box", dragZoomButton);
    dragZoomButton.setSelected(false);
    toolbar.add(dragZoomButton);

    rulerButton = SwarmUtil.createToolBarToggleButton(Icons.ruler, "Measure distances (M)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.setDragMode(DragMode.RULER);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "M", "measure", rulerButton);
    toolbar.add(rulerButton);
    toolbar.addSeparator();

    final ButtonGroup group = new ButtonGroup();
    group.add(dragButton);
    group.add(dragZoomButton);
    group.add(rulerButton);

    final JButton zoomIn =
        SwarmUtil.createToolBarButton(Icons.zoomplus, "Zoom in (+)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.zoom(0.5);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "EQUALS", "zoomin1", zoomIn);
    UiUtils.mapKeyStrokeToButton(this, "shift EQUALS", "zoomin2", zoomIn);
    toolbar.add(zoomIn);

    final JButton zoomOut =
        SwarmUtil.createToolBarButton(Icons.zoomminus, "Zoom out (-)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.zoom(2);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "MINUS", "zoomout1", zoomOut);
    toolbar.add(zoomOut);

    final JButton backButton = SwarmUtil.createToolBarButton(Icons.geoback,
        "Last map view (Ctrl-backspace)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.mapPop();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "ctrl BACK_SPACE", "mapback1", backButton);
    toolbar.add(backButton);

    toolbar.addSeparator();

    backTimeButton = SwarmUtil.createToolBarButton(Icons.left, "Scroll back time 20% (Left arrow)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            setRealtime(false);
            mapPanel.shiftTime(-0.20);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "LEFT", "backward1", backTimeButton);
    toolbar.add(backTimeButton);

    forwardTimeButton = SwarmUtil.createToolBarButton(Icons.right,
        "Scroll forward time 20% (Right arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            setRealtime(false);
            mapPanel.shiftTime(0.20);
          }
        });
    toolbar.add(forwardTimeButton);
    UiUtils.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardTimeButton);

    gotoButton =
        SwarmUtil.createToolBarButton(Icons.gototime, "Go to time (Ctrl-G)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            String t = JOptionPane.showInputDialog(applicationFrame,
                "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time",
                JOptionPane.PLAIN_MESSAGE);
            if (t != null) {
              if (t.length() == 12) {
                t = t + "30";
              }

              double j2k;

              try {
                j2k = J2kSec.parse("yyyyMMddHHmmss", t);
                setRealtime(false);
                mapPanel.gotoTime(j2k);
              } catch (final ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              }
            }
          }
        });
    toolbar.add(gotoButton);
    UiUtils.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);

    compXButton = SwarmUtil.createToolBarButton(Icons.xminus, "Shrink time axis (Alt-left arrow",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (realtime) {
              if (spanIndex != 0) {
                spanIndex--;
              }
            } else {
              mapPanel.scaleTime(0.20);
            }
          }
        });
    toolbar.add(compXButton);
    UiUtils.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);

    expXButton = SwarmUtil.createToolBarButton(Icons.xplus, "Expand time axis (Alt-right arrow)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (realtime) {
              if (spanIndex < MultiMonitor.SPANS.length - 1) {
                spanIndex++;
              }
            } else {
              mapPanel.scaleTime(-0.20);
            }
          }
        });
    toolbar.add(expXButton);
    UiUtils.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);

    timeHistoryButton = SwarmUtil.createToolBarButton(Icons.timeback,
        "Last time settings (Backspace)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (mapPanel.timePop()) {
              setRealtime(false);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "BACK_SPACE", "back", timeHistoryButton);
    toolbar.add(timeHistoryButton);
    toolbar.addSeparator();

    waveToolbar = new WaveViewSettingsToolbar(null, toolbar, this);

    clipboardButton = SwarmUtil.createToolBarButton(Icons.clipboard,
        "Copy inset to clipboard (C or Ctrl-C)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            mapPanel.wavesToClipboard();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "control C", "clipboard1", clipboardButton);
    UiUtils.mapKeyStrokeToButton(this, "C", "clipboard2", clipboardButton);
    toolbar.add(clipboardButton);

    toolbar.addSeparator();

    captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save map image (P)",
        new CaptureActionListener());
    UiUtils.mapKeyStrokeToButton(this, "P", "capture", captureButton);
    toolbar.add(captureButton);

    toolbar.addSeparator();

    toolbar.add(Box.createHorizontalGlue());
    throbber = new Throbber();
    toolbar.add(throbber);
  }

  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      final JFileChooser chooser = FileChooser.getFileChooser();
      final File lastPath = new File(swarmConfig.lastPath);
      chooser.setCurrentDirectory(lastPath);
      chooser.setSelectedFile(new File("map.png"));
      chooser.setDialogTitle("Save Map Screen Capture");
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

      final Insets i = mapPanel.getInsets();
      final BufferedImage image = new BufferedImage(mapPanel.getWidth() - i.left - i.right,
          mapPanel.getHeight() - i.top - i.bottom, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics g = image.getGraphics();
      g.translate(-i.left, -i.top);
      mapPanel.paint(g);
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

  public void setRealtime(final boolean b) {
    realtime = b;
    realtimeButton.setSelected(realtime);
  }

  public Throbber getThrobber() {
    return throbber;
  }

  public void setRefreshInterval(final long r) {
    refreshInterval = r;
  }

  public long getRefreshInterval() {
    return refreshInterval;
  }

  public MapPanel getMapPanel() {
    return mapPanel;
  }

  public void setSelectedWave(final WaveViewPanel wvp) {
    selected = wvp;
    waveToolbar.setSettings(selected.getSettings());
  }

  /**
   * Set view.
   * @param gr geo range
   */
  public void setView(final GeoRange gr) {
    final double lr1 = gr.getLonRange();
    final double lr2 = GeoRange.getLonRange(gr.getEast(), gr.getWest());
    if (lr2 < lr1) {
      gr.flipEastWest();
    }

    mapPanel.setCenterAndScale(gr);
  }

  /**
   * Set status text.
   * @param t text
   */
  public void setStatusText(final String t) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        String missing = "";
        if (mapPanel.getMissing() == 1) {
          missing = "(" + mapPanel.getMissing() + " channel hidden) ";
        } else if (mapPanel.getMissing() > 1) {
          missing = "(" + mapPanel.getMissing() + " channels hidden) ";
        }
        statusLabel.setText(missing + t);
        statusLabel.repaint();
      }
    });
  }

  public void reloadImages() {
    mapPanel.loadMaps(true);
  }

  public void reset(final boolean doMap) {
    mapPanel.resetImage(doMap);
  }

  public void mapSettingsChanged() {
    mapPanel.loadMaps(true);
  }

  public HelicorderViewPanelListener getLinkListener() {
    return linkListener;
  }

  /**
   * @see gov.usgs.volcanoes.swarm.Kioskable#setKioskMode(boolean)
   */
  public void setKioskMode(final boolean b) {
    setDefaultKioskMode(b);
    if (fullScreen) {
      mainPanel.remove(toolbar);
      mapPanel.setBorder(null);
    } else {
      mainPanel.add(toolbar, BorderLayout.NORTH);
      mapPanel.setBorder(border);
    }
  }

  /**
   * Add map layer.
   * @param layer map layer
   * @return map panel
   */
  public MapPanel addLayer(MapLayer layer) {
    mapPanel.addLayer(layer);

    return mapPanel;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while (true) {
      try {
        if (this.isVisible() && realtime) {
          final double end = J2kSec.now();
          final double start = end - MultiMonitor.SPANS[spanIndex];
          mapPanel.setTimes(start, end, false);
        }

        Thread.sleep(refreshInterval);
      } catch (final Throwable e) {
        e.printStackTrace();
      }
    }
  }

  public void optionsChanged() {
    reloadImages();
  }

  private static class MapFrameHolder {
    public static MapFrame mapFrame = new MapFrame();
  }

  public HypocenterLayer getHypocenterLayer() {
    return hypocenterLayer;
  }
}
