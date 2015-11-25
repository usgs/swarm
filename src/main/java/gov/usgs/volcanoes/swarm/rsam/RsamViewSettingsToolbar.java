package gov.usgs.volcanoes.swarm.rsam;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings.ViewType;

/**
 *
 *
 * @author Dan Cervelli
 */
public class RsamViewSettingsToolbar implements SettingsListener {
  private JButton waveSet;
  private JToggleButton valuesToggle;
  private JToggleButton countsToggle;
  private ButtonGroup rsamTypes;

  private final RsamViewSettings settings;

  public RsamViewSettingsToolbar(final RsamViewSettings s, final JToolBar dest,
      final JComponent keyComp) {
    settings = s;
    createUI(dest, keyComp);
    settings.addListener(this);
  }

  public void createUI(final JToolBar dest, final JComponent keyComp) {
    waveSet = SwarmUtil.createToolBarButton(Icons.wavesettings, "RSAM view settings (?)",
        new ActionListener() {
          @Override
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
          @Override
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
          @Override
          public void actionPerformed(final ActionEvent e) {
            settings.setType(ViewType.COUNTS);
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", countsToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "S", "spectra2", countsToggle);
    dest.add(countsToggle);
    rsamTypes.add(countsToggle);
  }

  @Override
  public void settingsChanged() {
    if (settings.getType() == ViewType.VALUES)
      rsamTypes.setSelected(valuesToggle.getModel(), true);
    else
      rsamTypes.setSelected(countsToggle.getModel(), true);

  }
}
