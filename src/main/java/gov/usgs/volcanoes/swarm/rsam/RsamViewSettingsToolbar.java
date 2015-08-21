package gov.usgs.volcanoes.swarm.rsam;

import gov.usgs.util.Util;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings.ViewType;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

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

    private RsamViewSettings settings;

    public RsamViewSettingsToolbar(RsamViewSettings s, JToolBar dest, JComponent keyComp) {
        settings = s;
        createUI(dest, keyComp);
        settings.addListener(this);
    }

    public void createUI(JToolBar dest, JComponent keyComp) {
        waveSet = SwarmUtil.createToolBarButton(Icons.wavesettings, "RSAM view settings (?)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RsamViewSettingsDialog wvsd = RsamViewSettingsDialog.getInstance(settings);
                wvsd.setVisible(true);
            }
        });
        Util.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
        dest.add(waveSet);

        rsamTypes = new ButtonGroup();
        valuesToggle = SwarmUtil.createToolBarToggleButton(Icons.rsam_values, "Values view (V or ,)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                            settings.setType(ViewType.VALUES);
                    }
                });
        Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", valuesToggle);
        Util.mapKeyStrokeToButton(keyComp, "W", "wave2", valuesToggle);
        dest.add(valuesToggle);
        rsamTypes.add(valuesToggle);
        rsamTypes.setSelected(valuesToggle.getModel(), true);
        
        countsToggle = SwarmUtil.createToolBarToggleButton(Icons.rsam_counts, "Counts view (C or .)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                            settings.setType(ViewType.COUNTS);
                    }
                });
        Util.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", countsToggle);
        Util.mapKeyStrokeToButton(keyComp, "S", "spectra2", countsToggle);
        dest.add(countsToggle);
        rsamTypes.add(countsToggle);
    }

    public void settingsChanged() {
        if (settings.getType()== ViewType.VALUES)
            rsamTypes.setSelected(valuesToggle.getModel(), true);
        else
            rsamTypes.setSelected(countsToggle.getModel(), true);

    }
}