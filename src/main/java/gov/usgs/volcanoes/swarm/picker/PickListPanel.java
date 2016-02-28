package gov.usgs.volcanoes.swarm.picker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.Time;

public class PickListPanel extends JPanel implements EventObserver {
  private static final Logger LOGGER = LoggerFactory.getLogger(PickListPanel.class);
  private static final Color BACKGROUND_COLOR = new Color(255, 255, 255);
  private static final Color SELECTED_BACKGROUND_COLOR = new Color(255, 248, 220);
  // private static final Color SELECTED_BACKGROUND_COLOR = new Color(0, 0, 220);
  private static final Font TABLE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
  private final Event event;
  private JPanel pickList;
  private Set<String> selected;
  private Component parent;

  public PickListPanel(Event event) {
    super();
    this.event = event;
    LOGGER.debug("GOT EVENT: {}", event);
    selected = new HashSet<String>();

    setLayout(new BorderLayout());
    LOGGER.debug("Event: " + event);
    event.addObserver(this);
    pickList = new JPanel();
    pickList.setLayout(new GridBagLayout());

    writeList(pickList);
    add(pickList, BorderLayout.PAGE_START);
  }

  private void writeList(JPanel pickList) {
    // pickList.setBorder(BorderFactory.createLineBorder(Color.black));
    pickList.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
    pickList.setBackground(BACKGROUND_COLOR);
    JLabel label;
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = .5;
    c.anchor = GridBagConstraints.CENTER;
    c.gridy = 0;
    c.gridx = GridBagConstraints.RELATIVE;
    c.ipadx = 3;
    c.ipady = 2;

    Border border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK);

    label = new JLabel("Time", SwingConstants.CENTER);
    label.setBorder(border);
    pickList.add(label, c);

    label = new JLabel("Type", SwingConstants.CENTER);
    label.setBorder(border);
    pickList.add(label, c);

    label = new JLabel("Channel", SwingConstants.CENTER);
    label.setBorder(border);
    pickList.add(label, c);

    label = new JLabel("Weight", SwingConstants.CENTER);
    label.setBorder(border);
    pickList.add(label, c);

    label = new JLabel("Onset", SwingConstants.CENTER);
    label.setBorder(border);
    pickList.add(label, c);

    label = new JLabel("First Motion", SwingConstants.CENTER);
    label.setBorder(border);
    pickList.add(label, c);

    LOGGER.debug("event: {}", event);
    Map<String, EventChannel> channels = event.getChannels();
    String[] keys = channels.keySet().toArray(new String[0]);
    int idx = keys.length;
    while (idx-- > 0) {
      c.gridy++;
      String chan = keys[idx];

      EventChannel eventChannel = channels.get(chan);

      Phase phase;

      phase = eventChannel.getPhase(Phase.PhaseType.P);
      if (phase != null) {
        writePhase(pickList, chan, phase, c);
        long coda = eventChannel.getCodaTime();
        if (coda > 0) {
          writeCoda(pickList, chan, coda, c);

        }
      }

      phase = eventChannel.getPhase(Phase.PhaseType.S);
      if (phase != null) {
        writePhase(pickList, chan, phase, c);
      }
  }

    c.weighty = 1;
    c.gridy++;
    JPanel filler = new JPanel();
    filler.setBackground(BACKGROUND_COLOR);
    pickList.add(filler, c);
  }

  private void writeCoda(JPanel pickList, String channel, long coda, GridBagConstraints c) {
    boolean isSelected = selected.contains(channel);
    c.gridy++;

    String time = Time.toDateString(coda);
    pickList.add(getLabel(time, isSelected), c);

    String phaseT = "C";
    pickList.add(getLabel(phaseT, isSelected), c);

    pickList.add(getLabel(channel, isSelected), c);

  }

  public void writePhase(final JPanel pickList, final String channel, final Phase phase,
      final GridBagConstraints c) {

    boolean isSelected = selected.contains(channel);

    c.gridy++;

    String time = Time.toDateString(phase.time);
    pickList.add(getLabel(time, isSelected), c);

    String phaseT = phase.phaseType.toString();
    pickList.add(getLabel(phaseT, isSelected), c);

    pickList.add(getLabel(channel, isSelected), c);

    c.fill = GridBagConstraints.NONE;
    final JComboBox<Integer> weight = new JComboBox<Integer>(new Integer[] {0, 1, 2, 3, 4});
    weight.setFont(TABLE_FONT);
    weight.setSelectedIndex(phase.weight);
    weight.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Phase newPhase = new Phase.Builder(phase).weight(weight.getSelectedIndex()).build();
        event.setPhase(channel, newPhase);
        parent.validate();
        parent.repaint();
      }
    });
    pickList.add(weight, c);
    c.fill = GridBagConstraints.HORIZONTAL;

    c.fill = GridBagConstraints.NONE;
    Phase.Onset[] onsets = Phase.Onset.values();
    final JComboBox<Phase.Onset> onset = new JComboBox<Phase.Onset>(onsets);
    onset.setFont(TABLE_FONT);
    int i = 0;
    while (i < onsets.length && onsets[i] != phase.onset) {
      i++;
    }
    onset.setSelectedIndex(i);
    onset.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Phase newPhase =
            new Phase.Builder(phase).onset((Phase.Onset) onset.getSelectedItem()).build();
        event.setPhase(channel, newPhase);
        parent.validate();
        parent.repaint();
      }
    });
    pickList.add(onset, c);
    c.fill = GridBagConstraints.HORIZONTAL;

    if (phase.phaseType == Phase.PhaseType.P) {
      c.fill = GridBagConstraints.NONE;
      Phase.FirstMotion[] firstMotions = Phase.FirstMotion.values();
      final JComboBox<Phase.FirstMotion> firstMotion =
          new JComboBox<Phase.FirstMotion>(firstMotions);
      firstMotion.setFont(TABLE_FONT);
      int idx = 0;
      while (idx < firstMotions.length && firstMotions[idx] != phase.firstMotion) {
        idx++;
      }
      firstMotion.setSelectedIndex(idx);
      firstMotion.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          Phase newPhase = new Phase.Builder(phase)
              .firstMotion((Phase.FirstMotion) firstMotion.getSelectedItem()).build();
          event.setPhase(channel, newPhase);
          parent.validate();
          parent.repaint();
        }
      });
      pickList.add(firstMotion, c);
      c.fill = GridBagConstraints.HORIZONTAL;
    } else {
      pickList.add(new JLabel(""), c);
    }
  }

  public void updateEvent() {
    if (event == null) {
      return;
    }

    LOGGER.debug("Updating pick list");
    JPanel newList = new JPanel();
    newList.setLayout(new GridBagLayout());
    writeList(newList);
    remove(pickList);
    pickList = newList;
    add(pickList, BorderLayout.CENTER);
    // parent.repaint();
  }

  public void deselect(String channel) {
    LOGGER.debug("deselecting {}", channel);
    selected.remove(channel);
  }

  public void deselectAll() {
    LOGGER.debug("deselecting all");
    selected = new HashSet<String>();

  }

  public void select(String channel) {
    LOGGER.debug("selecting {}", channel);
    selected.add(channel);
  }

  public void remove(String channel) {
    LOGGER.debug("removing {}", channel);

    selected.remove(channel);
    event.remove(channel);
  }

  private JLabel getLabel(String string, boolean selected) {
    JLabel label = new JLabel(string, SwingConstants.CENTER);
    label.setFont(TABLE_FONT);
    label.setOpaque(true);
    if (selected) {
      // label.setBackground(SELECTED_BACKGROUND_COLOR);
      label.setBackground(BACKGROUND_COLOR);
      Font font = label.getFont();
      font = font
          .deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
      // TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE));

      label.setFont(font);
    } else {
      label.setBackground(BACKGROUND_COLOR);
    }

    return label;
  }

  public void setParent(Component parent) {
    this.parent = parent;
  }

  public void repaint() {
    updateEvent();
    super.repaint();
  }
}
