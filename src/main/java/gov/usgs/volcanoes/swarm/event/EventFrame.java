/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.quakeml.Arrival;
import gov.usgs.volcanoes.core.quakeml.Event;
import gov.usgs.volcanoes.core.quakeml.EventObserver;
import gov.usgs.volcanoes.core.quakeml.Origin;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmFrame;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.wave.StatusTextArea;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
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

/**
 * The picker internal frame. Adapted from the WaveClipboardFrame.
 *
 * @author Tom Parker
 */
public class EventFrame extends SwarmFrame implements EventObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(EventFrame.class);
  private static final String EVENT_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query?eventid=";
      
  public static final long serialVersionUID = -1;

  
  private final PickBox pickBox;
  private final StatusTextArea statusText;
  private final Event event;
  private final JSplitPane mainPanel;
  private final PickToolBar toolbar;
  
  private boolean closing = false;

  /**
   * Constructor.
   * 
   * @param event Event to display
   */
  public EventFrame(Event event) {
    super("Event - " + event.getEventSource() + event.getEventId(), true, true, true, false);
    this.event = event;
    event.addObserver(this);

    statusText = new StatusTextArea(" ");
    pickBox = new PickBox(statusText);
    pickBox.setLayout(new BoxLayout(pickBox, BoxLayout.PAGE_AXIS));
    
    toolbar = new PickToolBar(pickBox, event);

    mainPanel =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, ParameterPanel.create(event), createPickPanel());
    mainPanel.setOneTouchExpandable(true);
    mainPanel.setDividerLocation(0.25);

    setContentPane(mainPanel);
    setFrameIcon(Icons.ruler);
    setSize(swarmConfig.clipboardWidth, swarmConfig.clipboardHeight);
    setLocation(swarmConfig.clipboardX, swarmConfig.clipboardY);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    createListeners();

    this.setVisible(true);
    this.setFocusable(true);
    updateEvent();
  }

  private JPanel createPickPanel() {
    JPanel pickPanel = new JPanel();
    pickPanel.setLayout(new BoxLayout(pickPanel, BoxLayout.PAGE_AXIS));

    pickPanel.add(toolbar);
    pickBox.addListener(toolbar);

    final JScrollPane scrollPane = new JScrollPane(pickBox);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);
    
    JViewport viewPort = scrollPane.getViewport();
    viewPort.setScrollMode(JViewport.BLIT_SCROLL_MODE);
    
    pickPanel.add(scrollPane);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BorderLayout());
    statusText.setBorder(BorderFactory.createEtchedBorder());
    statusPanel.add(statusText);
    statusPanel
        .setMaximumSize(new Dimension(Integer.MAX_VALUE, statusPanel.getPreferredSize().height));
    pickPanel.add(statusPanel);

    return pickPanel;
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
  }
  
  private void updateEvent() {
    new SwingWorker() {
      @Override
      public Object construct() {
        toolbar.incrementThrobber();
        CodeTimer timer = new CodeTimer("Detailed event");
        fetchDetailedEvent();
        timer.stopAndReport();
        populatePicks();
        return null;
      }

      @Override
      public void finished() {
        toolbar.decrementThrobber();
        repaint();
      }
    }.start();
  }

 
  /**
   * Fetch detailed event.
   */
  public void fetchDetailedEvent() {
    String neicEvid = event.getEventSource() + event.getEventId();
    Event workingEvent = event;

    String url = EVENT_URL + neicEvid;
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;
    Document doc = null;

    try {
      docBuilder = dbFactory.newDocumentBuilder();
      doc = docBuilder.parse(url);
      doc.getDocumentElement().normalize();

      NodeList eventElements = doc.getElementsByTagName("event");
      Element eventElement = (Element) eventElements.item(0);
      
      // NEIC has better descriptions
      NodeList descriptionNodes = eventElement.getElementsByTagName("description");
      for (int idx = 0; idx < descriptionNodes.getLength(); idx++) {
        eventElement.removeChild(descriptionNodes.item(idx));
      }
      
      workingEvent.parseEvent(eventElement);
    } catch (SAXException e) {
      LOGGER.warn("Unable to redtieve detailed event description. ({})", e.getLocalizedMessage());
    } catch (IOException e) {
      LOGGER.warn("Unable to redtieve detailed event description. ({})", e.getLocalizedMessage());
    } catch (ParserConfigurationException e) {
      LOGGER.warn("Unable to redtieve detailed event description. ({})", e.getLocalizedMessage());
    }
  }

  private void populatePicks() {
    toolbar.incrementThrobber();

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

    pickBox.setStart(waveStart);
    pickBox.setEnd(waveEnd);

    TreeSet<Arrival> arrivals = new TreeSet<Arrival>(Arrival.distanceComparator());
    arrivals.addAll(origin.getArrivals());
    for (Arrival arrival : arrivals) {
      if (closing) {
        break;
      }
      pickBox.addPick(arrival);
      mainPanel.validate();
    }
    
    toolbar.decrementThrobber();
  }

  @Override
  public void setVisible(final boolean isVisible) {
    super.setVisible(isVisible);
    if (isVisible) {
      toFront();
    }
  }

  /**
   * @see gov.usgs.volcanoes.core.quakeml.EventObserver#eventUpdated()
   */
  public void eventUpdated() {
    int loc = mainPanel.getDividerLocation();
    mainPanel.setTopComponent(ParameterPanel.create(event));
    mainPanel.setBottomComponent(createPickPanel());
    mainPanel.setDividerLocation(loc);
  }
}
