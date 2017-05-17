package gov.usgs.volcanoes.swarm.event;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.Version;
import gov.usgs.volcanoes.core.quakeml.Event;
import gov.usgs.volcanoes.core.quakeml.EventType;
import gov.usgs.volcanoes.core.quakeml.EventTypeCertainty;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.quakeml.QuakeMlUtils;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Event information dialog.
 * 
 * @author Diana Norgaard
 */
public class EventDialog extends SwarmModalDialog {

  private static final long serialVersionUID = 4870724789310886277L;
  private static EventDialog dialog;

  public static final String QUAKEML_RESOURCE_ID = "quakeml:volcanoes.usgs.gov/Swarm/v"
      + Version.POM_VERSION + "/" + SwarmConfig.getInstance().getUser();
  private JComboBox<EventType> eventType;
  private JComboBox<EventTypeCertainty> eventTypeCertainty;
  private JTextField description;
  private JTextField exportFile;
  private String user;

  /**
   * Default constructor.
   */
  private EventDialog() {
    super(Swarm.getApplicationFrame(), "Save Event");
    setSizeAndLocation();
    user = SwarmConfig.getInstance().getUser();
  }

  /**
   * Get instance of pick settings dialog.
   * @return pick settings dialog
   */
  public static EventDialog getInstance() {
    if (dialog == null) {
      dialog = new EventDialog();
    }
    return dialog;
  }

  /**
   * Create UI.
   */
  protected void createUi() {
    
    super.createUi();
    
    FormLayout layout = new FormLayout("right:50dlu, 10dlu, 120dlu, 10dlu, 20dlu");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);
        
    eventType = new JComboBox<EventType>(EventType.values());
    eventType.setSelectedItem(EventType.VOLCANIC_ERUPTION);
    builder.append("Event Type", eventType);
    builder.nextLine();
    
    eventTypeCertainty = new JComboBox<EventTypeCertainty>(EventTypeCertainty.values());
    eventTypeCertainty.setSelectedItem(EventTypeCertainty.SUSPECTED);
    builder.append("Event Type Certainty", eventTypeCertainty);
    builder.nextLine();
    
    description = new JTextField("TEST TEST");
    builder.append("Description", description);
    builder.nextLine();
    
    exportFile = new JTextField(SwarmConfig.getInstance().lastPath);
    builder.append("Save Directory", exportFile);
    JButton browseButton = new JButton("...");
    browseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openFileChooser();
      }      
    });
    builder.append(browseButton);
    builder.nextLine();

    mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
  }

  /**
   * Open file chooser dialog for export directory selection.
   */
  private void openFileChooser() {
    JFileChooser chooser = new JFileChooser();
    //chooser.setFileFilter(new FileNameExtensionFilter("QuakeML (.xml)", "xml"));
    chooser.setCurrentDirectory(new File(SwarmConfig.getInstance().lastPath));
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle("Select Save Directory...");
    int result = chooser.showOpenDialog(Swarm.getApplicationFrame());
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      exportFile.setText(file.getAbsolutePath());
    }
  }
  
  /**
   * Create event.
   * @return event
   */
  private Event createEvent() {
    String eventId = Long.toString(System.currentTimeMillis());
    String publicId = QUAKEML_RESOURCE_ID + "/Event/" + eventId;
    Event event = new Event(publicId);
    event.setEventId(eventId);
    event.setEventSource(QUAKEML_RESOURCE_ID);
    event.setType((EventType) eventType.getSelectedItem());
    event.setTypeCertainty((EventTypeCertainty) eventTypeCertainty.getSelectedItem());
    event.setDescription(description.getText());

    HashMap<String, Pick> picks = new HashMap<String, Pick>();
    WaveClipboardFrame clipboard = WaveClipboardFrame.getInstance();
    for (WaveViewPanel wvp : clipboard.getWaves()) {
      PickMenu pickMenu = wvp.getPickMenu();
      if (pickMenu.getP() != null && pickMenu.isPickChannelP()) {
        Pick p = pickMenu.getP();
        picks.put(p.publicId, p);
      }
      if (pickMenu.getS() != null && pickMenu.isPickChannelS()) {
        Pick s = pickMenu.getS();
        picks.put(s.publicId, s);
      }
    }
    event.setPicks(picks);
    return event;
  }
  
  /**
   * Validate input values.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#allowOk()
   */
  protected boolean allowOk() {
    return true;
  }
  
  /**
   * Create event and save to QuakeML file.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasOk()
   */
  protected void wasOk() {    
    try {
      // build XML document
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      Document doc = docBuilder.newDocument();
      Element quakeml = doc.createElement("q:quakeml");
      quakeml.setAttribute("xmlns:catalog", "http://xmlns/catalog/0.1");
      doc.appendChild(quakeml);
      
      Element eventParameters = doc.createElement("eventParameters");
      eventParameters.setAttribute("publicID", QUAKEML_RESOURCE_ID);
      Event event = createEvent();
      eventParameters.appendChild(event.toElement(doc));
      quakeml.appendChild(eventParameters);
      
      Element creationInfo = doc.createElement("creationInfo");
      Element author = doc.createElement("author");
      author.appendChild(doc.createTextNode(user));
      creationInfo.appendChild(author);
      Element creationTime = doc.createElement("creationTime");
      creationTime
          .appendChild(doc.createTextNode(QuakeMlUtils.formatDate(System.currentTimeMillis())));
      creationInfo.appendChild(creationTime);

      // write to file     
      String filename = exportFile.getText() + "/Swarm" + Version.POM_VERSION + "_QuakeML_" + user
          + "_" + System.currentTimeMillis() + ".xml";
      FileOutputStream fos = new FileOutputStream(filename);
      
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(fos);
      transformer.transform(source, result);
      
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TransformerConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }  
  }


  
  /**
   * Revert settings.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasCancelled()
   */
  protected void wasCancelled() {

  }

 
}
