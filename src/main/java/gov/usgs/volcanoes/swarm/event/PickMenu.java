package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.quakeml.Pick;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * Right click menu for picks.
 * 
 * @author Diana Norgaard
 */
public class PickMenu extends JPopupMenu {

  private static final long serialVersionUID = 8681764007165352268L;

  private final WaveViewPanel wvp;
  private double j2k;

  protected HashMap<String, JRadioButtonMenuItem[]> weightButtons =
      new HashMap<String, JRadioButtonMenuItem[]>();

  private JCheckBoxMenuItem hidePhasesMenu;
  private JCheckBoxMenuItem hideCodaMenu;
  private JCheckBoxMenuItem plotMenu;

  private PickData pickData;

  /**
   * Constructor.
   */
  public PickMenu(PickData pickData, WaveViewPanel wvp) {
    super("Pick Menu");
    this.pickData = pickData;
    this.wvp = wvp;
    createMenu();
  }

  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createPickMenu();
    createClearMenu();
    createHideMenu();

    // S-P plot
    plotMenu = new JCheckBoxMenuItem("Plot");
    plotMenu.setSelected(pickData.plot);
    plotMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pickData.plot = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        pickData.propagatePlot(pickData.plot, wvp);
      }
    });
    this.add(plotMenu);

  }

  /**
   * Pick menu.
   */
  private void createPickMenu() {
    JMenu pickMenu = new JMenu("Pick");
    this.add(pickMenu);

    for (final String phase : new String[] { PickData.P, PickData.S }) {
      JMenu phaseMenu = new JMenu(phase);
      pickMenu.add(phaseMenu);
      Pick pick = pickData.getPick(phase);
      Integer pickWeight = pickData.weight.get(phase);
      if (pickWeight == null) {
        pickWeight = 0;
      }
      for (final Pick.Onset onset : Pick.Onset.values()) {
        if (onset == Pick.Onset.QUESTIONABLE) {
          continue;
        }
        JMenu onsetMenu = new JMenu(onset.toString());
        phaseMenu.add(onsetMenu);

        for (final Pick.Polarity polarity : Pick.Polarity.values()) {
          String label = "";
          switch (polarity) {
            case POSITIVE:
              label = "+";
              break;
            case NEGATIVE:
              label = "-";
              break;
            case UNDECIDABLE:
              label = "?";
              break;
            default:
              break;
          }
          JMenu polarityMenu = new JMenu(label);
          onsetMenu.add(polarityMenu);

          // create weight menu
          ButtonGroup bg = new ButtonGroup();
          JRadioButtonMenuItem[] mi = new JRadioButtonMenuItem[PickSettings.numWeight];
          for (int i = 0; i < mi.length; i++) {
            final int weight = i;
            mi[i] = new JRadioButtonMenuItem(Integer.toString(i));
            bg.add(mi[i]);
            if (pick != null && onset.equals(pick.getOnset()) && polarity.equals(pick.getPolarity())
                && i == pickWeight.intValue()) {
              mi[i].setSelected(true);
            } else {
              mi[i].setSelected(false);
            }
            mi[i].addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                Pick pick = pickData.createPick(phase, onset, polarity, j2k, wvp, weight);
                pickData.propagatePick(phase, pick, wvp);
                pickData.setWeight(phase, weight);
                pickData.pickChannels.put(phase, true);
                pickData.propagateUncertainty(phase, weight, wvp);
                wvp.repaint();
              }
            });
            polarityMenu.add(mi[i]);
          }

          String key =
              phase + onset.toString().substring(0, 1) + polarity.toString().substring(0, 1);
          weightButtons.put(key, mi);
        }
      }

    }
    for (final String coda : new String[] { PickData.CODA1, PickData.CODA2 }) {
      JMenuItem codaMenuItem = new JMenuItem(coda);
      codaMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          pickData.createPick(coda, null, null, j2k, wvp, 0);
          wvp.repaint();
        }
      });
      pickMenu.add(codaMenuItem);
    }
  }

  /**
   * Clear menu.
   */
  private void createClearMenu() {
    JMenu clearMenu = new JMenu("Clear");
    this.add(clearMenu);

    for (final String pickType : new String[] { PickData.P, PickData.S, PickData.CODA1,
        PickData.CODA2 }) {
      JMenuItem clearPick = new JMenuItem(pickType);
      clearPick.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          pickData.clearPick(pickType, wvp);
          wvp.repaint();
        }
      });
      clearMenu.add(clearPick);
    }

    JMenuItem clearAll = new JMenuItem("All");
    clearAll.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        for (final String pickType : new String[] { PickData.P, PickData.S, PickData.CODA1,
            PickData.CODA2 }) {
          pickData.clearPick(pickType, wvp);
          wvp.repaint();
        }
      }
    });
    clearMenu.add(clearAll);

  }


  /**
   * Hide menu.
   */
  private void createHideMenu() {
    JMenu hideMenu = new JMenu("Hide");
    this.add(hideMenu);

    // hide P & S
    hidePhasesMenu = new JCheckBoxMenuItem("P & S");
    hidePhasesMenu.setSelected(pickData.isHidePhases());
    hidePhasesMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pickData.setHidePhases(hidePhasesMenu.isSelected());
        wvp.repaint();
      }
    });
    hideMenu.add(hidePhasesMenu);


    hideCodaMenu = new JCheckBoxMenuItem("Coda");
    hideCodaMenu.setSelected(pickData.isHideCoda());
    hideCodaMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pickData.setHideCoda(hideCodaMenu.isSelected());
        wvp.repaint();
      }
    });
    hideMenu.add(hideCodaMenu);
  }

  /**
   * Clear coda picks.
   */
  public void clearCoda() {
    pickData.picks.remove(PickData.CODA1);
    pickData.picks.remove(PickData.CODA2);
    wvp.repaint();
  }


  /**
   * Get currently set time as J2K.
   * 
   * @return j2k time
   */
  public double getJ2k() {
    return j2k;
  }

  /**
   * Set currently selected time as J2K.
   * 
   * @param j2k time
   */
  public void setJ2k(double j2k) {
    this.j2k = j2k;
  }

  /**
   * Get enabled/disabled option for hiding phases.
   * 
   * @return true if hide phases option is enabled
   */
  public boolean isHidePhases() {
    return hidePhasesMenu.isSelected();
  }

  /**
   * Get enabled/disabled option for hiding coda.
   * 
   * @return true if hide coda option is enabled
   */
  public boolean isHideCoda() {
    return hideCodaMenu.isSelected();
  }



}
