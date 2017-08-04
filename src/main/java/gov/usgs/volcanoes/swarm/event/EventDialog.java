package gov.usgs.volcanoes.swarm.event;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.contrib.hypo71.Hypo71;
import gov.usgs.volcanoes.core.contrib.hypo71.Hypo71.Results;
import gov.usgs.volcanoes.core.contrib.hypo71.Hypocenter;
import gov.usgs.volcanoes.core.quakeml.Arrival;
import gov.usgs.volcanoes.core.quakeml.EvaluationMode;
import gov.usgs.volcanoes.core.quakeml.Event;
import gov.usgs.volcanoes.core.quakeml.EventSet;
import gov.usgs.volcanoes.core.quakeml.EventType;
import gov.usgs.volcanoes.core.quakeml.EventTypeCertainty;
import gov.usgs.volcanoes.core.quakeml.Magnitude;
import gov.usgs.volcanoes.core.quakeml.Origin;
import gov.usgs.volcanoes.core.quakeml.OriginQuality;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.quakeml.QuakeMlUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.Version;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Event information dialog.
 * 
 * @author Diana Norgaard
 */
public class EventDialog extends JFrame {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventDialog.class);
  private static final long serialVersionUID = 4870724789310886277L;
  private static EventDialog dialog;
  private JPanel mainPanel;

  // event details
  private Event event;
  public static String QUAKEML_RESOURCE_ID = "quakeml:volcanoes.usgs.gov/Swarm/v"
      + Version.POM_VERSION + "/" + SwarmConfig.getInstance().getUser();
  private JComboBox<EventType> eventType;
  private JComboBox<EventTypeCertainty> eventTypeCertainty;
  private JTextField description;
  private JTextArea comment;
  
  // hypo71 info
  private Hypo71Manager hypo71Mgr; 
  protected JRadioButton usePicks;
  protected JTextField crustalModelFile;
  protected JRadioButton useInputFile;
  protected JTextField hypo71InputFile;
  protected JTextArea hypo71Output;
  
  private String user;

  /**
   * Default constructor.
   */
  private EventDialog() {
    super("Create Event");
    setIconImage(Icons.pick.getImage());
    setResizable(false);
    createUi();
    setSizeAndLocation();
    user = SwarmConfig.getInstance().getUser();
  }
  
  protected void setSizeAndLocation() {
    Dimension d = mainPanel.getPreferredSize();
    setSize(d.width + 10, d.height + 30);
    setMinimumSize(getSize());
    setMaximumSize(getSize());
    Dimension parentSize = Swarm.getApplicationFrame().getSize();
    Point parentLoc = Swarm.getApplicationFrame().getLocation();
    this.setLocation(parentLoc.x + (parentSize.width / 2 - d.width / 2),
        parentLoc.y + (parentSize.height / 2 - d.height / 2));
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

    mainPanel = new JPanel(new BorderLayout());
    this.add(mainPanel);
    //super.createUi();
    
    FormLayout layout = new FormLayout("left:65dlu, 5dlu, 120dlu, 3dlu, 10dlu");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);
    
    builder.appendSeparator("Event Details");
    
    eventType = new JComboBox<EventType>(EventType.values());
    eventType.setSelectedItem(EventType.EARTHQUAKE);
    builder.append("Event Type", eventType);
    builder.nextLine();
    
    eventTypeCertainty = new JComboBox<EventTypeCertainty>(EventTypeCertainty.values());
    eventTypeCertainty.setSelectedItem(EventTypeCertainty.SUSPECTED);
    builder.append("Event Type Certainty", eventTypeCertainty);
    builder.nextLine();
    
    description = new JTextField("");
    builder.append("Description", description);
    builder.nextLine();
    
    comment = new JTextArea(4,1);
    JScrollPane scrollPane = new JScrollPane(comment);
    builder.append("Comment", scrollPane);
    builder.nextLine();
    
    builder.appendSeparator("Hypo71 Input");
    hypo71Mgr = new Hypo71Manager();
 
    usePicks = new JRadioButton("Use Clipboard Picks");
    usePicks.setSelected(true);
    usePicks.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hypo71InputFile.setEnabled(false);
      }
    });
    builder.append(usePicks);
    builder.nextLine();

    crustalModelFile = new JTextField(hypo71Mgr.crustalModelFileName);
    builder.append("Crustal Model File", crustalModelFile);
    JButton openCrustalModelButton = new JButton("...");
    openCrustalModelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String filename = openFileChooser(JFileChooser.FILES_ONLY, null, null);
        if (filename != null) {
          crustalModelFile.setText(filename);
        }
      }
    });
    builder.append(openCrustalModelButton); 
    builder.nextLine();
    
    useInputFile = new JRadioButton("Use Input File");
    useInputFile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hypo71InputFile.setEnabled(true);
      }
    });
    builder.append(useInputFile);
    builder.nextLine();
    
    ButtonGroup hypoGroup = new ButtonGroup();
    hypoGroup.add(usePicks);
    hypoGroup.add(useInputFile);

    hypo71InputFile = new JTextField();
    builder.append("Hypo71 Input File", hypo71InputFile);
    JButton openInputFileButton = new JButton("...");
    openInputFileButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        FileFilter filter = new FileNameExtensionFilter("Hypo71 Input (.INP)", "INP");
        String filename = openFileChooser(JFileChooser.FILES_ONLY, null, filter);
        if (filename != null) {
          hypo71InputFile.setText(filename);
          hypo71Output.setText("");
          hypo71Output.setToolTipText("");
        }
      }
    });
    builder.append(openInputFileButton);
    builder.nextLine();
    
    JButton locateButton = new JButton("Run");
    locateButton.setToolTipText("Locate hypocenter using Hypo71");
    locateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          runHypo71();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        } catch (ParseException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }   
    });


    ButtonBarBuilder bbBuilder = new ButtonBarBuilder();
    bbBuilder.addGlue();
    bbBuilder.addButton(locateButton);
    JPanel buttonPanel = bbBuilder.getPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
    builder.append(buttonPanel, 5);
    
    builder.appendSeparator("Hypo71 Output");
    
    hypo71Output = new JTextArea(5,1);
    hypo71Output.setEditable(false);
    JScrollPane hypo71OutScroll = new JScrollPane(hypo71Output);
    builder.append(hypo71OutScroll, 5);
    builder.nextLine();

    JButton plotHypo71Button = new JButton("Plot");
    plotHypo71Button.setToolTipText("Plot located hypocenters on map.");
    plotHypo71Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        plotHypo71Output();
      }   
    });
    
    JButton saveHypo71Button = new JButton("Save");
    saveHypo71Button.setToolTipText("Save Hypo71 output to file.");
    saveHypo71Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveHypo71Output();
      }   
    });

    JButton clearHypo71Button = new JButton("Clear");
    clearHypo71Button.setToolTipText("Clear all hypo71 inputs.");
    clearHypo71Button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearHypo71();
      }   
    });

    bbBuilder = new ButtonBarBuilder();
    bbBuilder.addGlue();
    bbBuilder.addButton(plotHypo71Button, saveHypo71Button, clearHypo71Button);
    buttonPanel = bbBuilder.getPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
    builder.append(buttonPanel, 5);
    
    builder.appendSeparator("QuakeML");

    final FileFilter xmlFilter = new FileNameExtensionFilter("QuakeML (.xml)", "XML");
    JButton importQuakemlButton = new JButton("Import");
    importQuakemlButton.setToolTipText("Import event from QuakeML file.");
    importQuakemlButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String filename = openFileChooser(JFileChooser.FILES_ONLY, null, xmlFilter);
        importFile(filename);
        checkForPicks();
      }   
    }); 
    
    JButton exportQuakemlButton = new JButton("Export");
    exportQuakemlButton.setToolTipText("Export event to QuakeML file.");
    exportQuakemlButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String filename = SwarmConfig.getInstance().lastPath + "/Swarm" + Version.POM_VERSION
            + "_QuakeML_" + user + "_" + System.currentTimeMillis() + ".xml";
        filename = openFileChooser(JFileChooser.FILES_ONLY, filename, xmlFilter);
        saveQuakeMl(filename);
      }   
    });
    
    bbBuilder = new ButtonBarBuilder();
    bbBuilder.addGlue();
    bbBuilder.addButton(importQuakemlButton, exportQuakemlButton);
    buttonPanel = bbBuilder.getPanel();
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
    builder.append(buttonPanel, 5);
    
    mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
  }
  
  /**
   * Clear event information and hypo71 run data.
   */
  private void clearHypo71() {
    event = null;
    hypo71Mgr.clear();
    hypo71Output.setText("");
    hypo71Output.setToolTipText("");
  }
  
  /**
   * Plot Hypo71 output on map.
   */
  private void plotHypo71Output() {
    if (event == null) {
      String message = "You must first run Hypo71 to plot an event.";
      JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), message);
    } else if (event.getPreferredOrigin() == null) {
      String message = "No origin associated with event.";
      JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), message);
    } else {
      EventSet eventSet = new EventSet();
      eventSet.put(event.publicId, event);
      MapFrame.getInstance().getHypocenterLayer().add(eventSet);
    }
    
  }
  
  /**
   * Export Hypo71 output text.
   */
  private void saveHypo71Output() {
    String outputFile = SwarmConfig.getInstance().lastPath + "/Swarm" + Version.POM_VERSION
        + "_Hypo71_" + user + "_" + System.currentTimeMillis() + ".OUT";
    outputFile = openFileChooser(JFileChooser.FILES_ONLY, outputFile, null);
    if (outputFile == null) {
      return;
    }

    try {
      FileWriter fileWriter = new FileWriter(outputFile);
      String output = hypo71Mgr.hypo71.getResults().getOutput();
      fileWriter.write(output);
      fileWriter.close();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    JOptionPane.showMessageDialog(Swarm.getApplicationFrame(),
        "Hypo71 output saved.");
  }

  /**
   * Open file chooser dialog.
   * @param selectionMode file or directory
   * @return filename
   */
  private String openFileChooser(int selectionMode, String filename, FileFilter filter) {
    JFileChooser chooser = new JFileChooser();
    chooser.setCurrentDirectory(new File(SwarmConfig.getInstance().lastPath));
    if (filename != null) {
      chooser.setSelectedFile(new File(filename));
    }
    if (filter != null) {
      chooser.setFileFilter(filter);
    }
    chooser.setFileSelectionMode(selectionMode);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle("Open File...");
    int result = chooser.showOpenDialog(Swarm.getApplicationFrame());
    if (result == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (selectionMode == JFileChooser.DIRECTORIES_ONLY) {
        SwarmConfig.getInstance().lastPath = file.getAbsolutePath();
      } else {
        SwarmConfig.getInstance().lastPath = file.getParent();
      }
      return file.getAbsolutePath();
    }
    return null;
  }
  
  /**
   * Locate earthquake using hypo71.
   * @throws ParseException parse exception
   * @throws IOException IO exception
   */
  private void runHypo71() throws IOException, ParseException {
    hypo71Mgr.clear();
    boolean success;
    if (useInputFile.isSelected()) {
      String filename = hypo71InputFile.getText();
      if (filename == null || filename.equals("")) {
        JOptionPane.showMessageDialog(this, "Please select a Hypo71 input file to use.");
        return;
      }
      success = hypo71Mgr.calculate(hypo71InputFile.getText());
    } else {
      hypo71Mgr.loadCrustalModelFromFile();
      hypo71Mgr.description = description.getText();
      for (WaveViewPanel wvp : WaveClipboardFrame.getInstance().getWaves()) {
        PickMenu pickMenu = wvp.getPickMenu();
        Pick p = pickMenu.getPick(PickMenu.P);
        Pick s = pickMenu.getPick(PickMenu.S);
        if (p == null || !pickMenu.isPickChannel(PickMenu.P)) {
          continue;
        }
        
        // add station
        String channel = wvp.getChannel();
        Metadata md = SwarmConfig.getInstance().getMetadata(channel);
        String station = md.getSCNL().station;
        Double delay = Double.isNaN(md.getDelay()) ? 0 : md.getDelay();
        Double fmag = Double.isNaN(md.getFmagCorrection()) ? 0 : md.getFmagCorrection();
        Double xmag = Double.isNaN(md.getXmagCorrection()) ? 0 : md.getXmagCorrection();
        double latitude = md.getLatitude();
        double longitude = md.getLongitude();
        double height = md.getHeight();
        try {
          hypo71Mgr.addStation(station, latitude, longitude, height, delay, fmag, xmag, 0);
        } catch (IllegalArgumentException e) {
          JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), e.getMessage());
          return;
        }
        
        // add phase record
        hypo71Mgr.addPhaseRecord(station, p, s);
      }
      success = hypo71Mgr.calculate(null);
    }
    if (success) {
      String output = hypo71Mgr.hypo71.getResults().getOutput();;
      hypo71Output.setText(output);
      hypo71Output.setToolTipText("<html><pre>" + output + "</pre></html>");
      createEvent();
    }
  }
  
  /**
   * Create event.
   */
  private void createEvent() {
    String eventId = Long.toString(System.currentTimeMillis());
    String publicId = QUAKEML_RESOURCE_ID + "/Event/" + eventId;
    event = new Event(publicId);
    event.setEventId(eventId);
    event.setEventSource(QUAKEML_RESOURCE_ID);
    event.setType((EventType) eventType.getSelectedItem());
    event.setTypeCertainty((EventTypeCertainty) eventTypeCertainty.getSelectedItem());
    event.setDescription(description.getText());
    event.setComment(comment.getText());

    // get origins and magnitudes from hypo71 output
    Hypo71 hypo71 = hypo71Mgr.hypo71;
    Results result =  hypo71.getResults();
    char ins = hypo71.getINS();
    char iew = hypo71.getIEW();
    List<Hypocenter> hypocenters = result.getHypocenterOutput();
    HashMap<String, Origin> origins = new HashMap<String, Origin>();
    HashMap<String, Magnitude> magnitudes = new HashMap<String, Magnitude>();
    int magCount = 0;
    int originCount = 0;
    for (Hypocenter hypocenter : hypocenters) {
      // Magnitude
      double mag = Double.parseDouble(hypocenter.getMAGOUT());
      int no = hypocenter.getNO();
      publicId = QUAKEML_RESOURCE_ID + "/Magnitude/" + magCount;
      Magnitude magnitude = new Magnitude(publicId, mag);
      magnitude.setStationCount(no);
      magnitudes.put(publicId, magnitude);
      magCount++;
/*      double avxm = hypocenter.getAVXM(); // average of XMAG of available stations
      double sdxm = hypocenter.getSDXM(); // std dev of XMAG of available stations
      if (avxm > 0.00 || sdxm > 0.00) {
        int nm = hypocenter.getNM();        // number of station readings for xmag
        publicId = QUAKEML_RESOURCE_ID + "/Magnitude/" + magCount;
        Magnitude xmag = new Magnitude(publicId, avxm);
        xmag.getMagnitude().setUncertainty(sdxm);
        xmag.setStationCount(nm);
        xmag.setType("Mx");
        magnitudes.put(publicId, xmag);
        System.out.println(xmag);
        magCount++;
      }
      
      double avfm = hypocenter.getAVFM(); // average of FMAG of available stations
      double sdfm = hypocenter.getSDFM(); // std dev of FMAG of available stations
      if (avfm > 0.00 && sdfm > 0.00) {
        int nf = hypocenter.getNF();        // number of station readings for fmag
        publicId = QUAKEML_RESOURCE_ID + "/Magnitude/" + magCount;
        Magnitude fmag = new Magnitude(publicId, avfm);
        fmag.getMagnitude().setUncertainty(sdfm);
        fmag.setStationCount(nf);
        fmag.setType("Md");
        magnitudes.put(publicId, fmag);
        System.out.println(fmag);
        event.setPreferredMagnitude(fmag);
        magCount++;
      }
      if (avxm == 0.00 && avfm == 0.00) {
        continue;
      }*/
      
      try {
        // Origin
        // time
        int kdate = hypocenter.getKDATE();
        int khr = hypocenter.getKHR();
        int kmin = hypocenter.getKMIN();
        int sec = (int) hypocenter.getSEC();
        long time;
        time = getDate(kdate, khr, kmin, sec).getTime();
        
        // hypocenter
        double latitude = hypocenter.getLatitude();
        if (ins == 'S') {
          latitude *= -1;
        }
        double longitude = hypocenter.getLongitude();
        if (iew != 'E') {
          longitude *= -1;
        }
        double depth = hypocenter.getZ() * 1000;

        publicId = QUAKEML_RESOURCE_ID + "/Origin/" + originCount;
        Origin origin = new Origin(publicId, time, longitude, latitude);
        origin.setDepth(depth);
        
        // quality
        OriginQuality quality = new OriginQuality();
        int gap = hypocenter.getIGAP();
        quality.setAzimuthalGap(gap);
        double rms = hypocenter.getRMS();
        quality.setStandardError(rms);
        double dm = hypocenter.getDMIN();
        dm = Math.toDegrees(dm / 6371); // convert km to degrees
        quality.setMinimumDistance(dm);
        int nr = hypocenter.getNR();
        quality.setAssociatedStationCount(nr);
        origin.setQuality(quality);
        
        origin.setEvaluationMode(EvaluationMode.AUTOMATIC);       
        origins.put(publicId, origin);
        event.setPreferredOrigin(origin);
        System.out.println(origin);
        originCount++;
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    if (magnitudes.size() > 0) {
      event.setMagnitudes(magnitudes);
    }
    if (origins.size() > 0) {
      event.setOrigins(origins);
    }
    
    // add picks
    HashMap<String, Pick> picks = new HashMap<String, Pick>();
    if (usePicks.isSelected()) {
      for (WaveViewPanel wvp : WaveClipboardFrame.getInstance().getWaves()) {
        PickMenu pickMenu = wvp.getPickMenu();
        for (String phase : new String[] {PickMenu.P, PickMenu.S}) {
          if (pickMenu.getPick(phase) != null && pickMenu.isPickChannel(phase)) {
            Pick pick = pickMenu.getPick(phase);
            picks.put(pick.publicId, pick);
          }
        }
      }
    } /*else {
      // Create basic pick object from hypo71 phase record found in 
      int pickNum = 1;
      for (PhaseRecord phaseRecord : //TODO: Where to get phase data from in Hypo71 code?) {
        publicId = QUAKEML_RESOURCE_ID + "/Pick/" + pickNum;
        String channel = phaseRecord.getMSTA();
        int jtime = phaseRecord.getJTIME();
        int jmin = phaseRecord.getJMIN();
        float psec = phaseRecord.getP();
        float ssec = phaseRecord.getS();
        try {
          long pTime = getDate(jtime, jmin, (int)psec).getTime();
          long sTime = (long) (pTime - ((int)psec)*1000) + ssec*1000);
          for (String remark : new String[] {phaseRecord.getPRMK(), phaseRecord.getSRMK()}) {
            if (remark != null && !remark.equals("")) {
              String phase = remark.substring(1, 2);
              long time = 0;
              if (phase.equals(PickMenu.P)) {
                time = pTime;
              }
              if (phase.equals(PickMenu.S)) {
                time = sTime;
              }
              Pick pick = new Pick(publicId, time, channel);
              pick.setPhaseHint(phase);
              String onset = remark.substring(0, 1);
              if (onset.equals("I")) {
                pick.setOnset(Onset.IMPULSIVE);
              }
              if (onset.equals("E")) {
                pick.setOnset(Onset.EMERGENT);
              }
              String motion = remark.substring(2, 3);
              if (motion.equals("U") || motion.equals("+")) {
                pick.setPolarity(Polarity.POSITIVE);
              }
              if (motion.equals("D") || motion.equals("-")) {
                pick.setPolarity(Polarity.NEGATIVE);
              }

              picks.put(publicId, pick);
            }
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }      
      }
    }*/
    event.setPicks(picks);
    
    // add arrivals
    Origin origin = event.getPreferredOrigin();
    if (origin != null) {
      HashMap<String, Arrival> arrivalMap = new HashMap<String, Arrival>();
      int arrivalNum = 1;
      for (Pick pick : picks.values()) {
        publicId = QUAKEML_RESOURCE_ID + "/Arrival/" + arrivalNum;
        Arrival arrival = new Arrival(publicId, pick, pick.getPhaseHint());
        arrivalMap.put(publicId, arrival);
        arrivalNum++;
      }
      origin.setArrivals(arrivalMap);
    }
  }
  
    
  private SimpleDateFormat df = new SimpleDateFormat("yyMMddHHmmss");
  
  /**
   * Get date object from hypo71 hypocenter date format.
   * 
   * @param kdate year, month, and day (YYMMDD)
   * @param khr hour
   * @param kmin minute
   * @param ksec seconds
   * @return date
   * @throws ParseException parse exception
   */
  private Date getDate(int kdate, int khr, int kmin, int ksec) throws ParseException {
    String date = String.format("%06d", kdate);
    String hour = String.format("%02d", khr);
    String min = String.format("%02d", kmin);
    String sec = String.format("%02d", ksec);
    String ds = date + hour + min + sec;
    Date d = df.parse(ds);
    return d;
  }

  /**
   * Import event from file.
   * @param f file
   */
  private void importFile(String filename) {
    try {
      if (filename == null) {
        return;
      }
      WaveClipboardFrame clipboard = WaveClipboardFrame.getInstance();
      EventSet eventSet = EventSet.parseQuakeml(new FileInputStream(new File(filename)));
      if (eventSet.size() == 0) {
        JOptionPane.showMessageDialog(clipboard, "No events found in file.");
        return;
      }
      Event event;
      if (eventSet.size() > 1) { // Get user to decide which event to import
        HashMap<String, Event> eventMap = new HashMap<String, Event>();
        for (Event e : eventSet.values()) {
          String description = e.getDescription();
          if (description == null || description.equals("")) {
            description = e.publicId;
          }
          eventMap.put(description, e);
        }
        event = openEventChooser(eventMap);
      } else {
        event = eventSet.values().iterator().next();
      }
      //clipboard.setEvent(event);
      clipboard.importEvent(event);
    } catch (FileNotFoundException e) {
      LOGGER.warn(e.getMessage());
    } catch (IOException e) {
      LOGGER.warn(e.getMessage());
    } catch (ParserConfigurationException e) {
      LOGGER.warn(e.getMessage());
    } catch (SAXException e) {
      LOGGER.warn(e.getMessage());
    }
  }
  
  /**
   * Open chooser with list of event descriptions.
   * @param eventMap map of description to event
   * @return user selected event
   */
  private Event openEventChooser(HashMap<String, Event> eventMap) {
    String s = (String) JOptionPane.showInputDialog(WaveClipboardFrame.getInstance(),
        "Select event to import", "Import Event", JOptionPane.PLAIN_MESSAGE, null,
        eventMap.keySet().toArray(), eventMap.keySet().iterator().next());
    return eventMap.get(s);
  }
  
  /**
   * Save event to QuakeML file.
   */
  public void saveQuakeMl(String filename) {
    if (filename == null) {
      return;
    }
    try {
      createEvent();
      
      // build XML document
      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

      Document doc = docBuilder.newDocument();
      Element quakeml = doc.createElement("q:quakeml");
      quakeml.setAttribute("xmlns:catalog", "http://xmlns/catalog/0.1");
      doc.appendChild(quakeml);
      
      Element eventParameters = doc.createElement("eventParameters");
      eventParameters.setAttribute("publicID", QUAKEML_RESOURCE_ID);
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
      FileOutputStream fos = new FileOutputStream(filename);
      
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(fos);
      transformer.transform(source, result);
      
      fos.close();

      String message = "QuakeML event saved.";
      JOptionPane.showMessageDialog(Swarm.getApplicationFrame(), message);
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
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }  
  }
  
  /**
   * Check to see if there are picks on clipboard.  If so, enable
   * 'Use Picks' option. Otherwise, disable.
   */
  public void checkForPicks() {
    boolean hasPicks = false;
    for (WaveViewPanel wvp : WaveClipboardFrame.getInstance().getWaves()) {
      if (wvp.getPickMenu().getPickCount() > 0) {
        hasPicks = true;
        break;
      }
    }
    if (hasPicks) {
      usePicks.setEnabled(true);
      crustalModelFile.setEnabled(true);
      usePicks.setSelected(true);
    } else {
      usePicks.setEnabled(false);
      crustalModelFile.setEnabled(false); 
      useInputFile.setSelected(true);     
    }
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
   
  }

  /**
   * Revert settings.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasCancelled()
   */
  protected void wasCancelled() {

  }
  
  /**
   * Set event details from existing event.
   * @param event existing event
   */
  public void setEventDetails(Event event) {
    if (event == null) {
      return;
    }
    setComment(event.getComment());
    setDescription(event.getDescription());
    setEventType(event.getType());
    setEventTypeCertainty(event.getTypeCertainty());
  }
  
  /**
   * Set comment.
   * @param comment comment text
   */
  public void setComment(String comment) {
    this.comment.setText(comment);
  }
  
  /**
   * Set description.
   * @param description event description
   */
  public void setDescription(String description) {
    this.description.setText(description);
  }

  /**
   * Set event type.
   * @param eventType event type
   */
  public void setEventType(EventType eventType) {
    this.eventType.setSelectedItem(eventType);
  }
  
  /**
   * Set event type certainty.
   * @param eventTypeCertainty event type certainty
   */
  public void setEventTypeCertainty(EventTypeCertainty eventTypeCertainty) {
    this.eventTypeCertainty.setSelectedItem(eventTypeCertainty);
  }
}
