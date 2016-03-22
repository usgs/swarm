package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.SimpleDateFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

public class ParameterPanel {
  private static final Logger LOGGER = LoggerFactory.getLogger(ParameterPanel.class);
  private static final Font KEY_FONT = Font.decode("dialog-BOLD-12");
  private static final Font VALUE_FONT = Font.decode("dialog-12");

  private ParameterPanel() {}
  
  public static Component create(Event event) {
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

    label = new JLabel("Event source: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    String source = Networks.getInstance().getName(event.getEventSource().toUpperCase());
    if (source == null) {
      source = event.getEventSource();
    }
    label = new JLabel(Networks.getInstance().getName(event.getEventSource().toUpperCase()),
        SwingConstants.LEFT);
    label.setFont(VALUE_FONT);
    parameterPanel.add(label, c);

    c.gridy++;

    label = new JLabel("Description: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    // wrap description to support multi-line descriptions
    String description = event.getDescription();
    description = description.replace("\n", "<BR>");
    description = "<HTML>" + description + "</HTML>";
    label = new JLabel(description, SwingConstants.LEFT);
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


    label = new JLabel("Hypocenter: ", SwingConstants.LEFT);
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

    label = new JLabel("Error (RMS): ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    double error = origin.getStandardError();
    if (!Double.isNaN(error)) {
      label = new JLabel("" + error, SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    label = new JLabel("Azimuthal gap: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    double gap = origin.getAzimuthalGap();
    if (!Double.isNaN(gap)) {
      label = new JLabel("" + gap + "\u00B0", SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    label = new JLabel("Nearest station: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    double distance = origin.getMinimumDistance();
    if (!Double.isNaN(distance)) {
      label = new JLabel("" + distance + "\u00B0", SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    label = new JLabel("Phase count: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    int phaseCount = origin.getPhaseCount();
    if (phaseCount > 0) {
      label = new JLabel("" + phaseCount, SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

   label = new JLabel("Magnitude: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    if (magnitude != null) {
      String mag = String.format("%s %s", magnitude.getMag(), magnitude.getType());
      String uncertaintly = magnitude.getUncertainty();
      if (uncertaintly != null) {
        mag += " (" + uncertaintly + ")";
      }
      label = new JLabel(mag, SwingConstants.LEFT);
      label.setFont(VALUE_FONT);
      parameterPanel.add(label, c);
    }
    c.gridy++;

    label = new JLabel("Evalutation: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    String evaluationTag = "";

    Origin.EvaluationMode evaluationMode = origin.getEvaluationMode();
    if (evaluationMode != null) {
      evaluationTag += evaluationMode.toString().toLowerCase();
    }

    Origin.EvaluationStatus evaluationStatus = origin.getEvaluationStatus();
    if (evaluationStatus != null) {
      if (evaluationTag.length() > 0) {
        evaluationTag += " / ";
      }
      evaluationTag += evaluationStatus.toString().toLowerCase();
    }

    label = new JLabel(evaluationTag, SwingConstants.LEFT);
    label.setFont(VALUE_FONT);
    parameterPanel.add(label, c);

    c.gridy++;

    label = new JLabel("Event id: ", SwingConstants.LEFT);
    label.setFont(KEY_FONT);
    parameterPanel.add(label, c);

    label = new JLabel(event.getEvid(), SwingConstants.LEFT);
    label.setFont(VALUE_FONT);
    parameterPanel.add(label, c);
    c.gridy++;

    c.weighty = 1;
    c.weightx = 1;
    c.gridy++;
    c.gridx = 10;
    JPanel filler = new JPanel();
    parameterPanel.add(filler, c);

    final JScrollPane scrollPane = new JScrollPane(parameterPanel);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getVerticalScrollBar().setUnitIncrement(40);

    return scrollPane;
  }

}
