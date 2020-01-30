/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.quakeml.EvaluationMode;
import gov.usgs.volcanoes.quakeml.EvaluationStatus;
import gov.usgs.volcanoes.quakeml.Event;
import gov.usgs.volcanoes.quakeml.Magnitude;
import gov.usgs.volcanoes.quakeml.Origin;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterPanel {
  private static final Logger LOGGER = LoggerFactory.getLogger(ParameterPanel.class);
  private static final Font KEY_FONT = Font.decode("dialog-BOLD-12");
  private static final Font VALUE_FONT = Font.decode("dialog-12");

  private ParameterPanel() {}

  /**
   * Create event component.
   * @param event seismic event
   * @return component
   */
  public static Component create(Event event) {
    Origin origin = event.getPreferredOrigin();

    JPanel parameterPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    if (origin == null) {
      LOGGER.error("Cannot find perferred origin.");
      return parameterPanel;
    }


    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.gridy = 0;
    c.gridx = GridBagConstraints.RELATIVE;
    c.ipadx = 3;
    c.ipady = 2;

    addKey("Event source: ", parameterPanel, c);

    String source = Networks.getInstance().getName(event.getEventSource().toUpperCase());
    if (source == null) {
      source = event.getEventSource();
    }
    addValue(source, parameterPanel, c);

    c.gridy++;
    addKey("Description: ", parameterPanel, c);
    
    // wrap description to support multi-line descriptions
    String description = event.getDescription();
    if (description != null && !description.equals("")) {
      description = description.replace("\n", "<BR>");
      description = "<HTML>" + description + "</HTML>";
      addValue(description, parameterPanel, c);
    }

    c.gridy++;
    addKey("Origin date: ", parameterPanel, c);

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String date = dateFormat.format(event.getPreferredOrigin().getTime());
    addValue(date, parameterPanel, c);

    c.gridy++;
    addKey("Type: ", parameterPanel, c);

    if (origin != null) {
      addValue(event.getType().toString(), parameterPanel, c);
    }

    c.gridy++;
    addKey("Hypocenter: ", parameterPanel, c);

    String loc = origin.getLatitude() + ", " + origin.getLongitude();
    loc += " at " + (origin.getDepth() / 1000) + " km depth";
    addValue(loc, parameterPanel, c);

    c.gridy++;
    addKey("Error (RMS): ", parameterPanel, c);

    double error = origin.getQuality().getStandardError();
    if (!Double.isNaN(error)) {
      addValue(Double.toString(error), parameterPanel, c);
    }

    c.gridy++;
    addKey("Azimuthal gap: ", parameterPanel, c);

    double gap = origin.getQuality().getAzimuthalGap();
    if (!Double.isNaN(gap)) {
      addValue(gap + "\u00B0", parameterPanel, c);
    }

    c.gridy++;
    addKey("Nearest station: ", parameterPanel, c);

    double distance = origin.getQuality().getMinimumDistance();
    if (!Double.isNaN(distance)) {
      String tag = distance + "\u00B0" + " \u2248" + String.format("%.2f", Math.toRadians(distance * 6371)) + " km";
      addValue(tag, parameterPanel, c);
    }

    c.gridy++;
    addKey("Phase count: ", parameterPanel, c);

    int phaseCount = origin.getQuality().getUsedPhaseCount();
    if (phaseCount > 0) {
      addValue(Integer.toString(phaseCount), parameterPanel, c);
    }
    
    c.gridy++;
    addKey("Magnitude: ", parameterPanel, c);

    Magnitude magnitude = event.getPreferredMagnitude();
    if (magnitude != null) {
      String mag = String.format("%s %s", magnitude.getMagnitude().getValue(), magnitude.getType());
      double magUncertainty = magnitude.getMagnitude().getUncertainty();
      if (!Double.isNaN(magUncertainty)) {
        mag += " (" + String.format("%.1f", magUncertainty) + ")";
      }
      addValue(mag, parameterPanel, c);
    }

    c.gridy++;
    addKey("Evalutation: ", parameterPanel, c);

    String evaluationTag = "";
    EvaluationMode evaluationMode = origin.getEvaluationMode();
    if (evaluationMode != null) {
      evaluationTag += evaluationMode.toString().toLowerCase();
    }

    EvaluationStatus evaluationStatus = origin.getEvaluationStatus();
    if (evaluationStatus != null) {
      if (evaluationTag.length() > 0) {
        evaluationTag += " / ";
      }
      evaluationTag += evaluationStatus.toString().toLowerCase();
    }
    addValue(evaluationTag, parameterPanel, c);


    c.gridy++;
    addKey("Event id: ", parameterPanel, c);
    addValue(event.getEventId(), parameterPanel, c);

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

  private static void addKey(String labelS, JPanel parameterPanel, GridBagConstraints c) {
    addLabel(labelS, KEY_FONT, parameterPanel, c);
  }

  private static void addValue(String labelS, JPanel parameterPanel, GridBagConstraints c) {
    addLabel(labelS, VALUE_FONT, parameterPanel, c);
  }

  private static void addLabel(String labelS, Font font, JPanel parameterPanel,
      GridBagConstraints c) {
    JLabel label = new JLabel(labelS, SwingConstants.LEFT);
    label.setFont(font);
    parameterPanel.add(label, c);

  }

}
