package gov.usgs.volcanoes.swarm.rsam;

import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings.ViewType;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**
 * RSAM view settings toolbar.
 *
 * @author Dan Cervelli
 */
public class RsamViewSettingsToolbar implements SettingsListener {
  private JButton waveSet;
  private JToggleButton valuesToggle;
  private JToggleButton countsToggle;
  private ButtonGroup rsamTypes;

  private final RsamViewSettings settings;

  /**
   * Default constructor.
   * @param s RSAM view settings
   * @param dest tool bar destination
   * @param keyComp component
   */
  public RsamViewSettingsToolbar(final RsamViewSettings s, final JToolBar dest,
      final JComponent keyComp) {
    settings = s;
    createUi(dest, keyComp);
    settings.addListener(this);
  }

  private void createUi(final JToolBar dest, final JComponent keyComp) {
    waveSet = SwarmUtil.createToolBarButton(Icons.wavesettings, "RSAM view settings (?)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            final RsamViewSettingsDialog wvsd = RsamViewSettingsDialog.getInstance(settings);
            wvsd.setVisible(true);
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
    dest.add(waveSet);

    rsamTypes = new ButtonGroup();
    valuesToggle = SwarmUtil.createToolBarToggleButton(Icons.rsam_values, "Values view (V or ,)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            settings.setType(ViewType.VALUES);
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", valuesToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "W", "wave2", valuesToggle);
    dest.add(valuesToggle);
    rsamTypes.add(valuesToggle);
    rsamTypes.setSelected(valuesToggle.getModel(), true);

    countsToggle = SwarmUtil.createToolBarToggleButton(Icons.rsam_counts, "Counts view (C or .)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            settings.setType(ViewType.COUNTS);
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", countsToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "S", "spectra2", countsToggle);
    dest.add(countsToggle);
    rsamTypes.add(countsToggle);
  }

  public void settingsChanged() {
    if (settings.getType() == ViewType.VALUES) {
      rsamTypes.setSelected(valuesToggle.getModel(), true);
    } else {
      rsamTypes.setSelected(countsToggle.getModel(), true);
    }

  }
}
