package gov.usgs.volcanoes.swarm.picker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.Time;

public class PickListPanel extends JPanel implements EventObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(PickListPanel.class);
  private static final Color BACKGROUND_COLOR = new Color(255, 255, 255);
  private Event event;
  private JPanel pickList;

  public PickListPanel(Event event) {
    super();
    this.event = event;
    setLayout(new BorderLayout());
    this.setPreferredSize(new Dimension(this.getPreferredSize().width, 200));
    LOGGER.debug("Event: " + event);
    event.addObserver(this);
    pickList = new JPanel();
    pickList.setLayout(new GridBagLayout());

    writeList(pickList);
    add(pickList, BorderLayout.PAGE_START);
  }

  private void writeList(JPanel pickList) {
    pickList.setBorder(BorderFactory.createLineBorder(Color.black));
    pickList.setBackground(BACKGROUND_COLOR);
    JLabel label;
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = .5;
    c.anchor = GridBagConstraints.PAGE_START;
    c.gridy = 0;
    c.gridx = GridBagConstraints.RELATIVE;
    c.ipadx = 3;
    c.ipady = 2;

    label = new JLabel("Time");
    pickList.add(label, c);

    label = new JLabel("Type");
    pickList.add(label, c);

    label = new JLabel("Channel");
    pickList.add(label, c);

    label = new JLabel("Weight");
    pickList.add(label, c);

    label = new JLabel("Onset");
    pickList.add(label, c);

    label = new JLabel("First Motion");
    pickList.add(label, c);

    Map<String, EventChannel> channels = event.getChannels();
    for (String channel : channels.keySet()) {
      c.gridy++;

      Phase phase;

      phase = channels.get(channel).getPhase(Phase.PhaseType.P);
      if (phase != null) {
        writePhase(pickList, channel, phase, c);
      }

      phase = channels.get(channel).getPhase(Phase.PhaseType.S);
      if (phase != null) {
        writePhase(pickList, channel, phase, c);
      }

    }
    

    c.weighty = 1;
    c.gridy++;
    JPanel filler = new JPanel();
    filler.setBackground(BACKGROUND_COLOR);
    pickList.add(filler, c);
  }

  public void writePhase(JPanel pickList, String channel, Phase phase, GridBagConstraints c) {
    c.gridy++;

    JLabel label;
    label = new JLabel(Time.toDateString(phase.time));
    pickList.add(label, c);

    label = new JLabel(phase.phaseType.toString());
    pickList.add(label, c);

    label = new JLabel(channel);
    pickList.add(label, c);

    label = new JLabel("" + phase.weight);
    pickList.add(label, c);

    label = new JLabel(phase.onset.toString());
    pickList.add(label, c);

    label = new JLabel(phase.firstMotion.toString());
    pickList. add(label, c);
  }

  public void updateEvent() {
    LOGGER.debug("Updating pick list");
    JPanel newList = new JPanel();
    newList.setLayout(new GridBagLayout());
    writeList(newList);
    
    remove(pickList);
    pickList = newList;
    add(pickList, BorderLayout.CENTER);
  }

}
