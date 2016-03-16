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
import java.io.IOException;
import java.text.SimpleDateFormat;

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

import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmFrame;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesClient;
import gov.usgs.volcanoes.swarm.data.fdsnWs.WebServicesSource;
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
  private Event event;
  private SeismicDataSource seismicDataSource;

  public EventFrame(Event event) {
    super("Event - " + event.getEvid(), true, true, true, false);
    this.event = event;

    event.addObserver(this);
    this.setFocusable(true);
    createUI();
    createListeners();
    this.setVisible(true);
    fetchDetailedEvent(event);
  }

  private void fetchDetailedEvent(Event event) {
    final String neicEvid = event.getEventSource() + event.getEvid();
    final Event workingEvent = event;

    new Thread() {
      public void run() {
        String url = "http://earthquake.usgs.gov/fdsnws/event/1/query?eventid=" + neicEvid;

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;
        try {

          dBuilder = dbFactory.newDocumentBuilder();
          doc = dBuilder.parse(url);
          doc.getDocumentElement().normalize();
          NodeList eventElements = doc.getElementsByTagName("event");
          LOGGER.debug("Got {} events.", eventElements.getLength());
          workingEvent.updateEvent((Element) eventElements.item(0));

          WebServicesClient.getWave(wsDataSelectUrl, channelInfo, t1, t2);
          
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
    }.start();
  }

  private void createUI() {
    this.setFrameIcon(Icons.ruler);
    this.setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    this.setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    LOGGER.debug("event frame: {} @ {}", this.getSize(), this.getLocation());

    // JPanel pickPanel = new JPanel();
    // pickPanel.setLayout(new BoxLayout(pickPanel, BoxLayout.PAGE_AXIS));

    mainPanel =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, createParameterPanel(), createPickPanel());
    mainPanel.setOneTouchExpandable(true);


    // scrollPane = new JScrollPane(createParameterPanel());
    // parameterPanel.add(scrollPane, BorderLayout.NORTH);

    // Box pickBox = new Box(BoxLayout.Y_AXIS);
    // scrollPane = new JScrollPane(pickBox);
    // scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    // scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    // scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    // pickPanel.add(scrollPane, BorderLayout.NORTH);

    // mainPanel.setResizeWeight(.25);
    this.setContentPane(mainPanel);
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

  private Component createPickPanel() {
    Box pickBox = new Box(BoxLayout.Y_AXIS);

    Origin origin = event.getPreferredOrigin();
    Magnitude magnitude = event.getPerferredMagnitude();

    long firstPick = Long.MAX_VALUE;
    long lastPick = Long.MIN_VALUE;
    for (Arrival arrival : origin.getArrivals()) {
      WaveViewPanel wavePanel = new WaveViewPanel();

      Pick pick = arrival.getPick();
      firstPick = Math.min(pick.getTime(), firstPick);
      lastPick = Math.max(pick.getTime(), lastPick);
      String channel = pick.getChannel();
      wavePanel.setChannel(channel);

      pickBox.add(wavePanel);
    }

    JScrollPane scrollPane = new JScrollPane(pickBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);

    return scrollPane;
  }

  // public synchronized void addWave(final String chan) {
  // SeismicDataSource sds = p2.getDataSource();
  // double st = p2.getStartTime();
  // double et = p2.getEndTime();
  // Wave wave = sds.getWave(chan, st, et);
  // if (wave != null && wave.isData()) {
  // p2.setWave(wave, st, et);
  // p2.getWaveViewSettings().autoScaleAmpMemory = false;
  // addWave(p2);
  // }
  // }
  //
  // public synchronized void addWave(final PickerWavePanel p) {
  // p.addListener(selectListener);
  // p.setOffsets(54, 8, 21, 19);
  // p.setAllowClose(true);
  // p.setStatusLabel(statusLabel);
  // p.setAllowDragging(true);
  // p.setDisplayTitle(true);
  //// p.setEvent(event);
  // p.setParent(mainPanel);
  // final int w = scrollPane.getViewport().getSize().width;
  // p.setSize(w, calculateWaveHeight());
  // p.setBottomBorderColor(Color.GRAY);
  // p.createImage();
  // waveBox.add(p);
  // waves.add(p);
  // LOGGER.debug("{} panels; {} waves", waveBox.getComponentCount(), waves.size());
  // }

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

}
