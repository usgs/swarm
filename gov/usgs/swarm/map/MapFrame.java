package gov.usgs.swarm.map;

import gov.usgs.proj.GeoRange;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Kioskable;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmFrame;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.UiTime;
import gov.usgs.swarm.heli.HelicorderViewPanelListener;
import gov.usgs.swarm.map.MapPanel.DragMode;
import gov.usgs.swarm.map.MapPanel.LabelSetting;
import gov.usgs.swarm.wave.MultiMonitor;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.swarm.wave.WaveViewSettingsToolbar;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.png.PngEncoder;
import gov.usgs.util.png.PngEncoderB;

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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * 
 * @author Dan Cervelli
 */
public class MapFrame extends SwarmFrame implements Runnable, Kioskable {
    private static final long serialVersionUID = 1L;
    private static final MapFrame INSTANCE = new MapFrame();

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

    private Thread updateThread;

    private MapPanel mapPanel;

    private Throbber throbber;

    private int spanIndex = 3;
    private boolean realtime = true;

    private long refreshInterval = 1000;

    private HelicorderViewPanelListener linkListener;

    private boolean heliLinked = true;

    private Border border;

    private MapFrame() {
        super("Map", true, true, true, false);
        this.setFocusable(true);
        UiTime.touchTime();

        createUI();

        updateThread = new Thread(this, "Map Update");
        updateThread.start();
    }

    public static MapFrame getInstance() {
        return INSTANCE;
    }

    public void saveLayout(ConfigFile cf, String prefix) {
        super.saveLayout(cf, prefix);
        mapPanel.saveLayout(cf, prefix + ".panel");
    }

    public void processLayout(ConfigFile cf) {
        processStandardLayout(cf);
        mapPanel.processLayout(cf.getSubConfig("panel"));
        LabelSetting ls = mapPanel.getLabelSetting();
        labelButton.setIcon(ls.getIcon());
    }

    private void createUI() {
        setFrameIcon(Icons.earth);
        setSize(swarmConfig.mapWidth, swarmConfig.mapHeight);
        setLocation(swarmConfig.mapX, swarmConfig.mapY);
        setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);

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
            public void componentShown(ComponentEvent e) {
                if (!mapPanel.imageValid())
                    mapPanel.resetImage();
            }
        });

        this.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent e) {
                setVisible(false);
            }
        });

        linkListener = new HelicorderViewPanelListener() {
            public void insetCreated(double st, double et) {
                if (heliLinked) {
                    if (!realtime)
                        mapPanel.timePush();

                    setRealtime(false);
                    mapPanel.setTimes(st, et, true);
                }
            }
        };

        setVisible(true);
    }

    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (isVisible)
            toFront();
    }

    public void setMaximum(boolean max) throws PropertyVetoException {
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
        optionsButton = SwarmUtil.createToolBarButton(Icons.settings, "Map options", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MapSettingsDialog msd = MapSettingsDialog.getInstance(MapFrame.this);
                msd.setVisible(true);
            }
        });
        toolbar.add(optionsButton);

        labelButton = SwarmUtil.createToolBarButton(Icons.label_some, "Change label settings", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LabelSetting ls = mapPanel.getLabelSetting().next();
                labelButton.setIcon(ls.getIcon());
                mapPanel.setLabelSetting(ls);
            }
        });
        toolbar.add(labelButton);

        toolbar.addSeparator();

        realtimeButton = SwarmUtil.createToolBarToggleButton(Icons.clock, "Realtime mode (N)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setRealtime(realtimeButton.isSelected());
            }
        });
        Util.mapKeyStrokeToButton(this, "N", "realtime", realtimeButton);
        realtimeButton.setSelected(realtime);
        toolbar.add(realtimeButton);

        linkButton = SwarmUtil.createToolBarToggleButton(Icons.helilink, "Synchronize times with helicorder wave (H)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        heliLinked = linkButton.isSelected();
                    }
                });
        Util.mapKeyStrokeToButton(this, "H", "helilink", linkButton);
        linkButton.setSelected(heliLinked);
        toolbar.add(linkButton);

        toolbar.addSeparator();

        JButton earthButton = SwarmUtil.createToolBarButton(Icons.earth, "Zoom out to full scale (Home)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Point2D.Double c = new Point2D.Double(mapPanel.getCenter().x, 0);
                        mapPanel.setCenterAndScale(c, 100000);
                    }
                });
        Util.mapKeyStrokeToButton(this, "HOME", "home", earthButton);
        toolbar.add(earthButton);

        dragButton = SwarmUtil.createToolBarToggleButton(Icons.drag, "Drag map (D)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapPanel.setDragMode(DragMode.DRAG_MAP);
            }
        });
        Util.mapKeyStrokeToButton(this, "D", "drag", dragButton);
        dragButton.setSelected(true);
        toolbar.add(dragButton);

        dragZoomButton = SwarmUtil.createToolBarToggleButton(Icons.dragbox, "Zoom into box (B)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapPanel.setDragMode(DragMode.BOX);
            }
        });
        Util.mapKeyStrokeToButton(this, "B", "box", dragZoomButton);
        dragZoomButton.setSelected(false);
        toolbar.add(dragZoomButton);

        rulerButton = SwarmUtil.createToolBarToggleButton(Icons.ruler, "Measure distances (M)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapPanel.setDragMode(DragMode.RULER);
            }
        });
        Util.mapKeyStrokeToButton(this, "M", "measure", rulerButton);
        toolbar.add(rulerButton);
        toolbar.addSeparator();

        ButtonGroup group = new ButtonGroup();
        group.add(dragButton);
        group.add(dragZoomButton);
        group.add(rulerButton);

        JButton zoomIn = SwarmUtil.createToolBarButton(Icons.zoomplus, "Zoom in (+)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapPanel.zoom(0.5);
            }
        });
        Util.mapKeyStrokeToButton(this, "EQUALS", "zoomin1", zoomIn);
        Util.mapKeyStrokeToButton(this, "shift EQUALS", "zoomin2", zoomIn);
        toolbar.add(zoomIn);

        JButton zoomOut = SwarmUtil.createToolBarButton(Icons.zoomminus, "Zoom out (-)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapPanel.zoom(2);
            }
        });
        Util.mapKeyStrokeToButton(this, "MINUS", "zoomout1", zoomOut);
        toolbar.add(zoomOut);

        JButton backButton = SwarmUtil.createToolBarButton(Icons.geoback, "Last map view (Ctrl-backspace)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        mapPanel.mapPop();
                    }
                });
        Util.mapKeyStrokeToButton(this, "ctrl BACK_SPACE", "mapback1", backButton);
        toolbar.add(backButton);

        toolbar.addSeparator();

        backTimeButton = SwarmUtil.createToolBarButton(Icons.left, "Scroll back time 20% (Left arrow)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setRealtime(false);
                        mapPanel.shiftTime(-0.20);
                    }
                });
        Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backTimeButton);
        toolbar.add(backTimeButton);

        forwardTimeButton = SwarmUtil.createToolBarButton(Icons.right, "Scroll forward time 20% (Right arrow)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setRealtime(false);
                        mapPanel.shiftTime(0.20);
                    }
                });
        toolbar.add(forwardTimeButton);
        Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardTimeButton);

        gotoButton = SwarmUtil.createToolBarButton(Icons.gototime, "Go to time (Ctrl-G)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String t = JOptionPane.showInputDialog(applicationFrame, "Input time in 'YYYYMMDDhhmm[ss]' format:",
                        "Go to Time", JOptionPane.PLAIN_MESSAGE);
                if (t != null) {
                    if (t.length() == 12)
                        t = t + "30";

                    double j2k = Time.parse("yyyyMMddHHmmss", t);
                    setRealtime(false);
                    mapPanel.gotoTime(j2k);

                }
            }
        });
        toolbar.add(gotoButton);
        Util.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);

        compXButton = SwarmUtil.createToolBarButton(Icons.xminus, "Shrink time axis (Alt-left arrow",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (realtime) {
                            if (spanIndex != 0)
                                spanIndex--;
                        } else {
                            mapPanel.scaleTime(0.20);
                        }
                    }
                });
        toolbar.add(compXButton);
        Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);

        expXButton = SwarmUtil.createToolBarButton(Icons.xplus, "Expand time axis (Alt-right arrow)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (realtime) {
                            if (spanIndex < MultiMonitor.SPANS.length - 1)
                                spanIndex++;
                        } else {
                            mapPanel.scaleTime(-0.20);
                        }
                    }
                });
        toolbar.add(expXButton);
        Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);

        timeHistoryButton = SwarmUtil.createToolBarButton(Icons.timeback, "Last time settings (Backspace)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (mapPanel.timePop())
                            setRealtime(false);
                    }
                });
        Util.mapKeyStrokeToButton(this, "BACK_SPACE", "back", timeHistoryButton);
        toolbar.add(timeHistoryButton);
        toolbar.addSeparator();

        waveToolbar = new WaveViewSettingsToolbar(null, toolbar, this);

        clipboardButton = SwarmUtil.createToolBarButton(Icons.clipboard, "Copy inset to clipboard (C or Ctrl-C)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        mapPanel.wavesToClipboard();
                    }
                });
        Util.mapKeyStrokeToButton(this, "control C", "clipboard1", clipboardButton);
        Util.mapKeyStrokeToButton(this, "C", "clipboard2", clipboardButton);
        toolbar.add(clipboardButton);

        toolbar.addSeparator();

        captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save map image (P)", new CaptureActionListener());
        Util.mapKeyStrokeToButton(this, "P", "capture", captureButton);
        toolbar.add(captureButton);

        toolbar.addSeparator();

        toolbar.add(Box.createHorizontalGlue());
        throbber = new Throbber();
        toolbar.add(throbber);
    }

    class CaptureActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = Swarm.getApplication().getFileChooser();
            File lastPath = new File(swarmConfig.lastPath);
            chooser.setCurrentDirectory(lastPath);
            chooser.setSelectedFile(new File("map.png"));
            chooser.setDialogTitle("Save Map Screen Capture");
            int result = chooser.showSaveDialog(applicationFrame);
            File f = null;
            if (result == JFileChooser.APPROVE_OPTION) {
                f = chooser.getSelectedFile();

                if (f.exists()) {
                    int choice = JOptionPane.showConfirmDialog(applicationFrame, "File exists, overwrite?", "Confirm",
                            JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION)
                        return;
                }
                swarmConfig.lastPath = f.getParent();
            }
            if (f == null)
                return;

            Insets i = mapPanel.getInsets();
            BufferedImage image = new BufferedImage(mapPanel.getWidth() - i.left - i.right, mapPanel.getHeight()
                    - i.top - i.bottom, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = image.getGraphics();
            g.translate(-i.left, -i.top);
            mapPanel.paint(g);
            try {
                PngEncoderB png = new PngEncoderB(image, false, PngEncoder.FILTER_NONE, 7);
                FileOutputStream out = new FileOutputStream(f);
                byte[] bytes = png.pngEncode();
                out.write(bytes);
                out.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setRealtime(boolean b) {
        realtime = b;
        realtimeButton.setSelected(realtime);
    }

    public Throbber getThrobber() {
        return throbber;
    }

    public void setRefreshInterval(long r) {
        refreshInterval = r;
    }

    public long getRefreshInterval() {
        return refreshInterval;
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public void setSelectedWave(WaveViewPanel wvp) {
        selected = wvp;
        waveToolbar.setSettings(selected.getSettings());
    }

    public void setView(GeoRange gr) {
        double lr1 = gr.getLonRange();
        double lr2 = GeoRange.getLonRange(gr.getEast(), gr.getWest());
        if (lr2 < lr1)
            gr.flipEastWest();

        mapPanel.setCenterAndScale(gr);
    }

    public void setStatusText(final String t) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String missing = "";
                if (mapPanel.getMissing() == 1)
                    missing = "(" + mapPanel.getMissing() + " channel hidden) ";
                else if (mapPanel.getMissing() > 1)
                    missing = "(" + mapPanel.getMissing() + " channels hidden) ";
                statusLabel.setText(missing + t);
                statusLabel.repaint();
            }
        });
    }

    public void reloadImages() {
        mapPanel.loadMaps(true);
    }

    public void reset(boolean doMap) {
        mapPanel.resetImage(doMap);
    }

    public void mapSettingsChanged() {
        mapPanel.loadMaps(true);
    }

    public HelicorderViewPanelListener getLinkListener() {
        return linkListener;
    }

    public void setKioskMode(boolean b) {
        setDefaultKioskMode(b);
        if (fullScreen) {
            mainPanel.remove(toolbar);
            mapPanel.setBorder(null);
        } else {
            mainPanel.add(toolbar, BorderLayout.NORTH);
            mapPanel.setBorder(border);
        }
    }

    public void run() {
        while (true) {
            try {
                if (this.isVisible() && realtime) {
                    double end = CurrentTime.getInstance().nowJ2K();
                    double start = end - MultiMonitor.SPANS[spanIndex];
                    mapPanel.setTimes(start, end, false);
                }

                Thread.sleep(refreshInterval);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}