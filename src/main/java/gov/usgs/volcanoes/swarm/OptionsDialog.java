package gov.usgs.volcanoes.swarm;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.swarm.map.NationalMapLayer;
import gov.usgs.volcanoes.swarm.options.SwarmOptions;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.TimeZone;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * 
 * @author Dan Cervelli
 * @version $Id: OptionsDialog.java,v 1.7 2007-05-21 02:38:41 dcervelli Exp $
 */
public class OptionsDialog extends SwarmModalDialog {
  private static final long serialVersionUID = 1L;
  private static final JFrame applicationFrame = Swarm.getApplicationFrame();

  private JPanel dialogPanel;

  private JCheckBox durationEnabled;
  private JTextField durationA;
  private JTextField durationB;
  private JTextField pVelocity;
  private JTextField velocityRatio;
  private JCheckBox useLargeCursor;

  private JCheckBox tzInstrument;
  private JRadioButton tzLocal;
  private JRadioButton tzSpecific;
  private JComboBox<String> timeZones;
  private JComboBox<NationalMapLayer> natMapList;
  private JRadioButton useMapPacks;
  private JRadioButton useWms;
  private JTextField wmsServer;
  private JTextField wmsLayer;
  private JTextField wmsStyles;
  private JLabel wmsServerLabel;
  private JLabel wmsLayerLabel;
  private JLabel wmsStylesLabel;

  /**
   * Constructor.
   */
  public OptionsDialog() {
    super(applicationFrame, "Options");
    createUi();
    setCurrentValues();
    setSizeAndLocation();
  }

  private void createFields() {
    durationEnabled = new JCheckBox("Enabled");
    durationA = new JTextField();
    durationB = new JTextField();
    pVelocity = new JTextField();
    velocityRatio = new JTextField();
    useLargeCursor = new JCheckBox("Large Helicorder Cursor");
    tzInstrument = new JCheckBox("Use instrument time zone if available");
    tzLocal = new JRadioButton("Use local machine time zone:");
    tzSpecific = new JRadioButton("Use specific time zone:");
    ButtonGroup tzGroup = new ButtonGroup();
    tzGroup.add(tzLocal);
    tzGroup.add(tzSpecific);
    String[] tzs = TimeZone.getAvailableIDs();
    Arrays.sort(tzs);
    timeZones = new JComboBox<String>(tzs);

    useMapPacks = new JRadioButton("Use local MapPacks");
    useWms = new JRadioButton("Use WMS");
    ButtonGroup mapGroup = new ButtonGroup();
    mapGroup.add(useMapPacks);
    mapGroup.add(useWms);


    natMapList = new JComboBox<NationalMapLayer>(NationalMapLayer.values());
    wmsLayer = new JTextField();
    wmsServer = new JTextField();
    wmsStyles = new JTextField();
  }

  protected void createUi() {
    super.createUi();
    createFields();

    FormLayout layout = new FormLayout(
        "right:max(30dlu;pref), 3dlu, 40dlu, 3dlu, right:max(40dlu;pref), 3dlu, 40dlu", "");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);

    builder.appendSeparator("Time Zone");

    builder.append(tzInstrument, 7);
    builder.nextLine();
    TimeZone local = TimeZone.getDefault();

    builder.append(tzLocal, 7);
    builder.append("   ");
    builder.append(new JLabel(local.getID()), 5);
    builder.nextLine();

    builder.append(tzSpecific, 7);
    builder.nextLine();

    builder.append("   ");
    builder.append(timeZones, 5);
    builder.nextLine();

    builder.appendSeparator("Duration Magnitude");
    builder.append(durationEnabled, 7);
    builder.nextLine();
    builder.append("Md=", durationA);
    builder.append("* Log(t) +", durationB);

    builder.appendSeparator("S-P Distance");
    builder.append("P-velocity (km/s)=", pVelocity);
    builder.nextLine();
    builder.append("Vp/Vs Ratio =", velocityRatio);
    builder.nextLine();
    
    builder.appendSeparator("Maps");
    builder.append(useMapPacks, 7);
    builder.nextLine();
    builder.append(useWms, 7);
    builder.nextLine();
    builder.append("USGS National Map:");
    builder.append(natMapList, 5);
    builder.nextLine();
    wmsServerLabel = new JLabel("Server:");
    wmsServerLabel.setLabelFor(wmsServer);
    builder.append(wmsServerLabel);
    builder.append(wmsServer, 5);
    builder.nextLine();
    wmsLayerLabel = new JLabel("Layer:");
    wmsLayerLabel.setLabelFor(wmsLayer);
    builder.append(wmsLayerLabel);
    builder.append(wmsLayer, 5);
    builder.nextLine();
    wmsStylesLabel = new JLabel("Styles:");
    wmsStylesLabel.setLabelFor(wmsStyles);
    builder.append(wmsStylesLabel);
    builder.append(wmsStyles, 5);
    builder.nextLine();

    useMapPacks.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        doEnables();
      }
    });

    natMapList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        NationalMapLayer layer = (NationalMapLayer)((JComboBox<?>) e.getSource()).getSelectedItem();
        wmsServer.setText(layer.server);
        wmsLayer.setText(layer.layer);
        wmsStyles.setText(layer.style);
      }
    });
    builder.appendSeparator("Other");
    builder.append(useLargeCursor, 7);
    builder.nextLine();

    dialogPanel = builder.getPanel();
    mainPanel.add(dialogPanel, BorderLayout.CENTER);
  }

  /**
   * Set enabled flags.
   */
  public void doEnables() {
    boolean state = useMapPacks.isSelected();
    wmsServer.setEnabled(!state);
    wmsLayer.setEnabled(!state);
    wmsStyles.setEnabled(!state);
    wmsServerLabel.setEnabled(!state);
    wmsLayerLabel.setEnabled(!state);
    wmsStylesLabel.setEnabled(!state);
    natMapList.setEnabled(!state);
  }

  /**
   * Set current values.
   */
  public void setCurrentValues() {
    useLargeCursor.setSelected(swarmConfig.useLargeCursor);
    durationA.setText(Double.toString(swarmConfig.durationA));
    durationB.setText(Double.toString(swarmConfig.durationB));
    durationEnabled.setSelected(swarmConfig.durationEnabled);
    pVelocity.setText(Double.toString(swarmConfig.pVelocity));
    velocityRatio.setText(Double.toString(swarmConfig.velocityRatio));
    tzInstrument.setSelected(swarmConfig.useInstrumentTimeZone);
    if (swarmConfig.useLocalTimeZone) {
      tzLocal.setSelected(true);
    } else {
      tzSpecific.setSelected(true);
    }
    timeZones.setSelectedItem(swarmConfig.specificTimeZone.getID());

    useMapPacks.setSelected(!swarmConfig.useWMS);
    useWms.setSelected(swarmConfig.useWMS);
    NationalMapLayer basemap = NationalMapLayer.getFromServer(swarmConfig.wmsServer);
    if (basemap != null) {
      natMapList.setSelectedItem(basemap);
    } else {
      natMapList.setSelectedItem(NationalMapLayer.OTHER);
    }
    wmsServer.setText(swarmConfig.wmsServer);
    wmsLayer.setText(swarmConfig.wmsLayer);
    wmsStyles.setText(swarmConfig.wmsStyles);
    doEnables();
  }

  /**
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#allowOk()
   */
  public boolean allowOk() {
    boolean ok = true;
    try {
      Double.parseDouble(durationA.getText().trim());
      Double.parseDouble(durationB.getText().trim());
    } catch (Exception e) {
      String message = "The duration magnitude constants must be numbers.";
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
      ok = false;
    }
    try {
      Double.parseDouble(pVelocity.getText().trim());
    } catch (Exception e) {
      String message = "The P-velocity must be a number.";
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
      ok = false;
    }
    try {
      Double.parseDouble(velocityRatio.getText().trim());
    } catch (Exception e) {
      String message = "The Vp/Vs ratio must be a number.";
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
      ok = false;
    }
    return ok;
  }

  /**
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasOk()
   */
  public void wasOk() {
    swarmConfig.useLargeCursor = useLargeCursor.isSelected();
    swarmConfig.durationEnabled = durationEnabled.isSelected();
    swarmConfig.durationA = Double.parseDouble(durationA.getText().trim());
    swarmConfig.durationB = Double.parseDouble(durationB.getText().trim());
    swarmConfig.pVelocity = Double.parseDouble(pVelocity.getText().trim());
    swarmConfig.velocityRatio = Double.parseDouble(velocityRatio.getText().trim());
    swarmConfig.useInstrumentTimeZone = tzInstrument.isSelected();
    swarmConfig.useLocalTimeZone = tzLocal.isSelected();
    swarmConfig.specificTimeZone = TimeZone.getTimeZone((String) timeZones.getSelectedItem());
    swarmConfig.useWMS = useWms.isSelected();
    swarmConfig.wmsServer = wmsServer.getText();
    swarmConfig.wmsLayer = wmsLayer.getText();
    swarmConfig.wmsStyles = wmsStyles.getText();

    SwarmOptions.optionsChanged();
  }
}
