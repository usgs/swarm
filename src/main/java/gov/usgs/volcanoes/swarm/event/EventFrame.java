package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmFrame;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.picker.PickerWavePanel;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsToolbar;

/**
 * The picker internal frame. Adapted from the WaveClipboardFrame.
 *
 * @author Tom Parker
 */
public class EventFrame extends SwarmFrame implements EventObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(EventFrame.class);
  private static final Font KEY_FONT = Font.decode("dialog-BOLD-12");
  private static final Font VALUE_FONT = Font.decode("dialog-12");

  public static final long serialVersionUID = -1;

  private final PickPanel pickBox;
  private final JLabel statusLabel;
  private final Map<AbstractWavePanel, Stack<double[]>> histories;
  private final Set<PickerWavePanel> selectedSet;

  private JSplitPane mainPanel;
  private Event event;
  private JToolBar toolbar;
  private JButton sizeButton;
  private JButton saveButton;
  private JButton captureButton;
  private JButton histButton;

  private WaveViewSettingsToolbar waveToolbar;

  private JButton compXButton;
  private JButton expXButton;
  private JButton forwardButton;
  private JButton backButton;
  private JButton gotoButton;
  private Throbber throbber;
  private JPopupMenu popup;
  private int waveHeight;

  private boolean closing = false;

  public EventFrame(Event event) {
    super("Event - " + event.getEvid(), true, true, true, false);
    this.event = event;

    statusLabel = new JLabel(" ");
    pickBox = new PickPanel(statusLabel);
    pickBox.setLayout(new BoxLayout(pickBox, BoxLayout.PAGE_AXIS));

    histories = new HashMap<AbstractWavePanel, Stack<double[]>>();
    selectedSet = new HashSet<PickerWavePanel>();

    event.addObserver(this);
    this.setFocusable(true);
    createListeners();

    setFrameIcon(Icons.ruler);
    setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    LOGGER.debug("event frame: {} @ {}", this.getSize(), this.getLocation());

    mainPanel =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, ParameterPanel.create(event), createPickPanel());
    mainPanel.setOneTouchExpandable(true);

    setContentPane(mainPanel);
    this.setVisible(true);

    new SwingWorker() {
      @Override
      public Object construct() {
        fetchDetailedEvent();
        populatePicks();
        return null;
      }

      @Override
      public void finished() {
        repaint();
      }
    }.start();
  }


  private JPanel createPickPanel() {

    toolbar = SwarmUtil.createToolBar();
    createMainButtons();
    createWaveButtons();
    final JScrollPane scrollPane = new JScrollPane(pickBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);

    JPanel pickPanel = new JPanel();
    pickPanel.setLayout(new BoxLayout(pickPanel, BoxLayout.PAGE_AXIS));
    LOGGER.info("Adding toolbar");
    pickPanel.add(toolbar);
    pickPanel.add(scrollPane);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BorderLayout());
    statusLabel.setBorder(BorderFactory.createEtchedBorder());
    statusPanel.add(statusLabel);
    statusPanel
        .setMaximumSize(new Dimension(Integer.MAX_VALUE, statusPanel.getPreferredSize().height));
    pickPanel.add(statusPanel);

    return pickPanel;
  }

  private void createMainButtons() {
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

    toolbar.add(Box.createHorizontalGlue());

    throbber = new Throbber();
    toolbar.add(throbber);

    // UiUtils.mapKeyStrokeToAction(this, "control A", "selectAll", new AbstractAction() {
    // private static final long serialVersionUID = 1L;
    //
    // public void actionPerformed(final ActionEvent e) {
    // for (final PickerWavePanel wave : waves)
    // select(wave);
    // }
    // });
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

  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      pickBox.writeImage();
    }
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
              pickBox.setWaveHeight(size);
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

  public void fetchDetailedEvent() {
    final String neicEvid = event.getEventSource() + event.getEvid();
    final Event workingEvent = event;

    String url = "http://earthquake.usgs.gov/fdsnws/event/1/query?eventid=" + neicEvid;

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = null;
    Document doc = null;

    try {
      dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(url);
      doc.getDocumentElement().normalize();

      NodeList eventElements = doc.getElementsByTagName("event");
      Element eventElement = (Element) eventElements.item(0);
      NodeList descriptionNodes = eventElement.getElementsByTagName("description");
      for (int idx = 0; idx < descriptionNodes.getLength(); idx++) {
        eventElement.removeChild(descriptionNodes.item(idx));
      }
      workingEvent.updateEvent(eventElement);
    } catch (SAXException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void populatePicks() {
    Origin origin = event.getPreferredOrigin();

    long firstPick = Long.MAX_VALUE;
    long lastPick = Long.MIN_VALUE;
    for (Arrival arrival : origin.getArrivals()) {
      Pick pick = arrival.getPick();
      firstPick = Math.min(pick.getTime(), firstPick);
      lastPick = Math.max(pick.getTime(), lastPick);
    }

    double waveStart = J2kSec.fromEpoch(firstPick) - 1;
    double waveEnd = J2kSec.fromEpoch(lastPick) + 1;
    LOGGER.debug("wave span {} - {}", waveStart, waveEnd);

    pickBox.setStart(waveStart);
    pickBox.setEnd(waveEnd);

    throbber.increment();
    TreeSet<Arrival> arrivals = new TreeSet<Arrival>(Arrival.distanceComparator());
    arrivals.addAll(origin.getArrivals());
    for (Arrival arrival : arrivals) {
      if (closing) {
        break;
      }
      pickBox.addPick(arrival);
      mainPanel.validate();
      LOGGER.debug("pickBox {}", pickBox.countComponents());
    }
    throbber.decrement();
  }

  @Override
  public void paint(final Graphics g) {
    super.paint(g);
  }

  @Override
  public void setVisible(final boolean isVisible) {
    LOGGER.debug("Visible = {}", isVisible);
    super.setVisible(isVisible);
    if (isVisible)
      toFront();
  }

  public void eventUpdated() {
    mainPanel.setTopComponent(ParameterPanel.create(event));
    mainPanel.setBottomComponent(createPickPanel());
  }

  private void createListeners() {
    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameActivated(final InternalFrameEvent e) {}

      @Override
      public void internalFrameDeiconified(final InternalFrameEvent e) {}

      @Override
      public void internalFrameClosing(final InternalFrameEvent e) {
        closing = true;
        dispose();
        SwarmInternalFrames.remove(EventFrame.this);
      }

      @Override
      public void internalFrameClosed(final InternalFrameEvent e) {}
    });

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {}
    });
  }
}
