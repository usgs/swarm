package gov.usgs.volcanoes.swarm.event;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
  private PickToolBar toolbar;

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

    toolbar = new PickToolBar();
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

    toolbar.incrementThrobber();
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
    toolbar.decrementThrobber();
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
