package gov.usgs.volcanoes.swarm.map;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;
import gov.usgs.volcanoes.swarm.map.MapPanel.LabelSetting;
import gov.usgs.volcanoes.swarm.map.hypocenters.HypocenterSource;

/**
 * 
 * @author Dan Cervelli
 */
public class MapSettingsDialog extends SwarmModalDialog {
  public static final long serialVersionUID = -1;

  private static final Logger LOGGER = LoggerFactory.getLogger(MapSettingsDialog.class);

  private JPanel dialogPanel;

  private JTextField scale;
  private JTextField longitude;
  private JTextField latitude;

  private JTextField lineWidth;
  private JTextField refreshInterval;
  private ButtonGroup labelGroup;
  private JRadioButton someLabels;
  private JRadioButton allLabels;
  private JRadioButton noLabels;
  private JButton mapLine;
  private JColorChooser lineChooser;
  private JComboBox hypocenterSource;

  private MapFrame mapFrame;

  public MapSettingsDialog(MapFrame mapFrame) {
    super(applicationFrame, "Map Settings", "mapSettings.md");
    this.mapFrame = mapFrame;
    createUI();
    setToCurrent();
    setSizeAndLocation();
  }

  private void createFields() {
    latitude = new JTextField();
    longitude = new JTextField();
    scale = new JTextField();
    lineWidth = new JTextField();
    refreshInterval = new JTextField();
    labelGroup = new ButtonGroup();
    someLabels = new JRadioButton("Some");
    allLabels = new JRadioButton("All");
    noLabels = new JRadioButton("None");
    labelGroup.add(someLabels);
    labelGroup.add(allLabels);
    labelGroup.add(noLabels);
    mapLine = new JButton();
    hypocenterSource = new JComboBox(HypocenterSource.values());
    lineChooser = new JColorChooser();
    lineChooser.setPreviewPanel(new MapLinePreview());

    final ActionListener okActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        // swarmConfig.mapLineColor = lineChooser.getC
      }
    };

    mapLine.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JDialog dialog = JColorChooser.createDialog(applicationFrame, "Map Line Settings", true,
            lineChooser, okActionListener, null);
        dialog.setVisible(true);
      }
    });

  }

  protected void createUI() {
    super.createUI();
    createFields();

    FormLayout layout = new FormLayout("right:max(30dlu;pref), 3dlu, 50dlu, 3dlu, 30dlu", "");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();

    builder.appendSeparator("Location");
    builder.append("Longitude:");
    builder.append(longitude);
    builder.append("degrees");
    builder.nextLine();
    builder.append("Latitude:");
    builder.append(latitude);
    builder.append("degrees");
    builder.nextLine();
    builder.append("Scale:");
    builder.append(scale);
    builder.append("m/pixel");
    builder.nextLine();

    builder.appendSeparator("Options");
    builder.append("Line:");
    builder.append(mapLine);
    builder.nextLine();
    builder.append("Refresh Interval:");
    builder.append(refreshInterval);
    builder.append(" seconds");
    builder.nextLine();
    builder.append("Channel Labels:");
    builder.append(noLabels);
    builder.nextLine();
    builder.append(" ");
    builder.append(someLabels);
    builder.nextLine();
    builder.append(" ");
    builder.append(allLabels);

    builder.nextLine();
    builder.append("NEIC Event Summary");
    builder.append(hypocenterSource, 3);

    dialogPanel = builder.getPanel();
    mainPanel.add(dialogPanel, BorderLayout.CENTER);

  }

  public void setVisible(boolean b) {
    if (b)
      this.getRootPane().setDefaultButton(okButton);
    super.setVisible(b);
  }

  public void setToCurrent() {
    MapPanel panel = mapFrame.getMapPanel();
    if (panel == null) 
      return;
    
    longitude.setText(String.format("%.4f", panel.getCenter().x));
    latitude.setText(String.format("%.4f", panel.getCenter().y));
    scale.setText(String.format("%.1f", panel.getScale()));
    lineWidth.setText(Integer.toString(swarmConfig.mapLineWidth));
    refreshInterval.setText(String.format("%.2f", mapFrame.getRefreshInterval() / 1000.0));
    LabelSetting ls = panel.getLabelSetting();
    switch (ls) {
      case ALL:
        allLabels.setSelected(true);
        break;
      case SOME:
        someLabels.setSelected(true);
        break;
      case NONE:
        noLabels.setSelected(true);
        break;
    }
    hypocenterSource.setSelectedItem(swarmConfig.getHypocenterSource());
  }

  protected void wasOK() {
    try {
      MapPanel panel = mapFrame.getMapPanel();
      LabelSetting ls = LabelSetting.ALL;
      if (someLabels.isSelected())
        ls = LabelSetting.SOME;
      else if (noLabels.isSelected())
        ls = LabelSetting.NONE;
      panel.setLabelSetting(ls);
      Point2D.Double center = new Point2D.Double();
      center.x = Double.parseDouble(longitude.getText());
      center.y = Double.parseDouble(latitude.getText());
      double sc = Double.parseDouble(scale.getText());
      panel.setCenterAndScale(center, sc);
      swarmConfig.mapLineWidth = Integer.parseInt(lineWidth.getText());
      mapFrame.setRefreshInterval(Math.round(Double.parseDouble(refreshInterval.getText()) * 1000));
      
      swarmConfig.setHypocenterSource((HypocenterSource) hypocenterSource.getSelectedItem());
    } catch (Exception e) {
      LOGGER.debug("Exception caught while accepting map options.");
      e.printStackTrace();
    }
  }

  protected boolean allowOK() {
    String message = null;
    try {
      message =
          "Invalid refresh interval; legal values are between 0 and 3600, 0 to refresh continuously.";
      double ri = Double.parseDouble(refreshInterval.getText());
      if (ri < 0 || ri > 3600)
        throw new NumberFormatException();

      message = "Invalid longitude; legal values are between -180 and 180.";
      double lon = Double.parseDouble(longitude.getText());
      if (lon < -180 || lon > 180)
        throw new NumberFormatException();

      message = "Invalid latitude; legal values are between -90 and 90.";
      double lat = Double.parseDouble(latitude.getText());
      if (lat < -90 || lat > 90)
        throw new NumberFormatException();

      message = "Invalid scale; legal values are greater than 0.";
      double sc = Double.parseDouble(scale.getText());
      if (sc <= 0)
        throw new NumberFormatException();

      message = "Invalid line width; legal values are integers greater than 0.";
      int i = Integer.parseInt(lineWidth.getText());
      if (i <= 0)
        throw new NumberFormatException();

      return true;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

}
