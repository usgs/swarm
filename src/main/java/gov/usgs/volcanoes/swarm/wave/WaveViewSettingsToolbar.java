package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

/**
 * Wave view settings toolbar.
 *
 * @author Dan Cervelli
 */
public class WaveViewSettingsToolbar {
  private JButton waveSet;
  private JToggleButton waveToggle;
  private JToggleButton spectraToggle;
  private JToggleButton spectrogramToggle;
  private JToggleButton particleMotionToggle;      // particle motion analysis
  private ButtonGroup waveTypes;

  private Set<WaveViewSettings> settingsSet;

  /**
   * Constructor.
   * @param s  Wave view settings
   * @param dest Tool bar destination
   * @param keyComp key component
   */
  public WaveViewSettingsToolbar(WaveViewSettings s, JToolBar dest, JComponent keyComp) {
    settingsSet = new HashSet<WaveViewSettings>();
    createUi(dest, keyComp);
    setSettings(s);
  }

  /**
   * Create wave view settings tool bar.
   * @param dest tool bar
   * @param keyComp key component
   */
  public void createUi(JToolBar dest, JComponent keyComp) {
    waveSet = SwarmUtil.createToolBarButton(Icons.wavesettings, "Wave view settings (?)",
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (settingsSet.size() == 0) {
              return;
            }
            WaveViewSettings s = settingsSet.iterator().next();
            WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(s, settingsSet.size());
            wvsd.setVisible(true);
            for (WaveViewSettings settings : settingsSet) {
              settings.copy(s);
              settings.notifyView();
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
    dest.add(waveSet);

    waveToggle =
        SwarmUtil.createToolBarToggleButton(Icons.wave, "Wave view (W or ,)", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            for (WaveViewSettings settings : settingsSet) {
              settings.setType(ViewType.WAVE);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", waveToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "W", "wave2", waveToggle);
    dest.add(waveToggle);

    spectraToggle = SwarmUtil.createToolBarToggleButton(Icons.spectra, "Spectra view (S or .)",
        new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            for (WaveViewSettings settings : settingsSet) {
              settings.setType(ViewType.SPECTRA);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", spectraToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "S", "spectra2", spectraToggle);
    dest.add(spectraToggle);

    spectrogramToggle = SwarmUtil.createToolBarToggleButton(Icons.spectrogram,
        "Spectrogram view (G or /)", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            for (WaveViewSettings settings : settingsSet) {
              settings.setType(ViewType.SPECTROGRAM);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "SLASH", "spectrogram1", spectrogramToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "G", "spectrogram2", spectrogramToggle);
    dest.add(spectrogramToggle);
    
    particleMotionToggle = SwarmUtil.createToolBarToggleButton(Icons.particle_motion,
        "Particle Motion Analysis (R or ')", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            for (WaveViewSettings settings : settingsSet) {
              settings.setType(ViewType.PARTICLE_MOTION);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(keyComp, "APOSTROPHE", "pma1", particleMotionToggle);
    UiUtils.mapKeyStrokeToButton(keyComp, "O", "pma2", particleMotionToggle);
    dest.add(particleMotionToggle);

    keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke("L"), "cycleLogSettings");
    keyComp.getActionMap().put("cycleLogSettings", new AbstractAction() {
      public static final long serialVersionUID = -1;

      public void actionPerformed(ActionEvent e) {
        for (WaveViewSettings settings : settingsSet) {
          if (settings.viewType == ViewType.SPECTRA) {
            settings.cycleLogSettings();
          }
          if (settings.viewType == ViewType.SPECTROGRAM) {
            settings.toggleLogPower();
          }
        }
      }
    });

    keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke("F"), "toggleFilter");
    keyComp.getActionMap().put("toggleFilter", new AbstractAction() {
      public static final long serialVersionUID = -1;

      public void actionPerformed(ActionEvent e) {
        for (WaveViewSettings settings : settingsSet) {
          settings.toggleFilter();
        }
      }
    });

    keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke("R"), "resetAutoScale");
    keyComp.getActionMap().put("resetAutoScale", new AbstractAction() {
      public static final long serialVersionUID = -1;

      public void actionPerformed(ActionEvent e) {
        for (WaveViewSettings settings : settingsSet) {
          settings.resetAutoScaleMemory();
        }
      }
    });

    keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke("OPEN_BRACKET"), "yScaleIn");
    keyComp.getActionMap().put("yScaleIn", new AbstractAction() {
      public static final long serialVersionUID = -1;

      public void actionPerformed(ActionEvent e) {
        for (WaveViewSettings settings : settingsSet) {
          settings.adjustScale(1.0 / 1.25);
        }
      }
    });

    keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke("CLOSE_BRACKET"), "yScaleOut");
    keyComp.getActionMap().put("yScaleOut", new AbstractAction() {
      public static final long serialVersionUID = -1;

      public void actionPerformed(ActionEvent e) {
        for (WaveViewSettings settings : settingsSet) {
          settings.adjustScale(1.25);
        }
      }
    });

    waveTypes = new ButtonGroup();
    waveTypes.add(waveToggle);
    waveTypes.add(spectraToggle);
    waveTypes.add(spectrogramToggle);
    waveTypes.add(particleMotionToggle);
 
  }

  public void clearSettingsSet() {
    settingsSet.clear();
  }

  /**
   * Add wave view settings.
   * @param s settings
   */
  public void addSettings(WaveViewSettings s) {
    if (s != null) {
      settingsSet.add(s);
      s.toolbar = this;
      settingsChanged();
    }
  }

  /**
   * Remove wave view settings.
   * @param s settings
   */
  public void removeSettings(WaveViewSettings s) {
    settingsSet.remove(s);
    if (s != null) {
      s.toolbar = null;
      settingsChanged();
    }
  }

  public void setSettings(WaveViewSettings s) {
    clearSettingsSet();
    addSettings(s);
  }

  /**
   * Process settings change.
   */
  public void settingsChanged() {

    boolean p = false;
    boolean s = false;
    boolean sg = false;
    boolean w = false;
    for (WaveViewSettings set : settingsSet) {
      p = false;
      s = false;
      sg = false;
      w = false;
      switch (set.viewType) {
        case PARTICLE_MOTION:
          p = true;
          break;
        case SPECTRA:
          s = true;
          break;
        case SPECTROGRAM:
          sg = true;
          break;
        case WAVE:
          w = true;
          break;
        default:
          break;
      }
    }

    particleMotionToggle.setSelected(p);
    spectraToggle.setSelected(s);
    spectrogramToggle.setSelected(sg);
    waveToggle.setSelected(w);
  }

}
