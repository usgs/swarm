package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmFrame;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesClient;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

/**
 * The picker internal frame. Adapted from the WaveClipboardFrame.
 *
 * @author Tom Parker
 */
public class EventFrame extends SwarmFrame implements EventObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(EventFrame.class);
  private static final Color BACKGROUND_COLOR = new Color(255, 255, 255);
  private static final Font KEY_FONT = Font.decode("dialog-BOLD-12");
  private static final Font VALUE_FONT = Font.decode("dialog-12");

  public static final long serialVersionUID = -1;

  private JSplitPane mainPanel;
  private JScrollPane pickScrollPane;
  private Event event;
  private SeismicDataSource seismicDataSource;
  private final PickPanel pickBox;

  public EventFrame(Event event) {
    super("Event - " + event.getEvid(), true, true, true, false);
    this.event = event;
    event.addObserver(this);
    this.setFocusable(true);
    createListeners();

    pickBox = new PickPanel();
    pickBox.setLayout(new BoxLayout(pickBox, BoxLayout.PAGE_AXIS));

    setFrameIcon(Icons.ruler);
    setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    LOGGER.debug("event frame: {} @ {}", this.getSize(), this.getLocation());


    pickScrollPane = createPickPanel();
    mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createParameterPanel(), pickScrollPane);
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

  private Component createParameterPanel() {
    Origin origin = event.getPreferredOrigin();
    Magnitude magnitude = event.getPerferredMagnitude();

    JPanel parameterPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();


    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridy = 0;
    c.gridx = GridBagConstraints.RELATIVE;
    c.ipadx = 3;
    c.ipady = 2;

    JLabel label;

    label = new JLabel("Description: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    label = new JLabel(event.getDescription(), SwingConstants.LEFT);
    label.setFont(VALUE_FONT);
    parameterPanel.add(label, c);

    c.gridy++;

    label = new JLabel("Origin date: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    if (origin != null) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      String date = dateFormat.format(event.getPreferredOrigin().getTime());
      label = new JLabel(date, SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    label = new JLabel("Magnitude: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    if (magnitude != null) {
      String mag = String.format("%s (%s)", magnitude.getMag(), magnitude.getType());
      label = new JLabel(mag, SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    label = new JLabel("Location: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    if (origin != null) {
      String loc = origin.getLatitude() + ", " + origin.getLongitude();
      loc += " at " + (origin.getDepth() / 1000) + " km depth";
      label = new JLabel(loc, SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    c.weighty = 1;
    c.weightx = 1;
    c.gridy++;
    c.gridx = 10;
    JPanel filler = new JPanel();
    parameterPanel.add(filler, c);

    return parameterPanel;
  }

  private JScrollPane createPickPanel() {

    final JScrollPane scrollPane = new JScrollPane(pickBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    scrollPane.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        scrollPane.getViewport().setSize(scrollPane.getWidth(), scrollPane.getViewport().getHeight());
      }
    });

    return scrollPane;
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
      workingEvent.updateEvent((Element) eventElements.item(0));
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

    for (Arrival arrival : origin.getArrivals()) {
      pickBox.addPick(arrival);
      mainPanel.validate();
      LOGGER.debug("pickBox {}", pickBox.countComponents());
    }
  }

  @Override
  public void paint(final Graphics g) {
    super.paint(g);
    // if (waves.size() == 0) {
    // final Dimension dim = this.getSize();
    // g.setColor(Color.black);
    // g.drawString("Picker empty.", dim.width / 2 - 40, dim.height / 2);
    // }
  }

  @Override
  public void setVisible(final boolean isVisible) {
    LOGGER.debug("Visible = {}", isVisible);
    super.setVisible(isVisible);
    if (isVisible)
      toFront();
  }

  public void eventUpdated() {
    mainPanel.setTopComponent(createParameterPanel());
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

  // private synchronized void createImage() {
  // if (getWidth() > 0 && getHeight() > 0)
  // image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
  // }

}
