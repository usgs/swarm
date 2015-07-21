package gov.usgs.swarm.rsam;

import gov.usgs.swarm.Icons;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.rsam.RsamViewSettings.ViewType;
import gov.usgs.swarm.wave.WaveViewSettings;
import gov.usgs.util.Util;

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
public class RsamViewSettingsToolbar {
    private JButton waveSet;
    private JToggleButton valuesToggle;
    private JToggleButton countsToggle;
    private ButtonGroup rsamTypes;

    private Set<RsamViewSettings> settingsSet;

    public RsamViewSettingsToolbar(RsamViewSettings s, JToolBar dest, JComponent keyComp) {
        settingsSet = new HashSet<RsamViewSettings>();
        createUI(dest, keyComp);
        setSettings(s);
    }

    public void createUI(JToolBar dest, JComponent keyComp) {
        waveSet = SwarmUtil.createToolBarButton(Icons.wavesettings, "RSAM view settings (?)", new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (settingsSet.size() == 0)
                    return;
                RsamViewSettings s = settingsSet.iterator().next();
                RsamViewSettingsDialog wvsd = RsamViewSettingsDialog.getInstance(s, settingsSet.size());
                wvsd.setVisible(true);
                for (RsamViewSettings settings : settingsSet) {
                    settings.copy(s);
                    settings.notifyView();
                }
            }
        });
        Util.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
        dest.add(waveSet);

        valuesToggle = SwarmUtil.createToolBarToggleButton(Icons.rsam_values, "Values view (V or ,)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        for (RsamViewSettings settings : settingsSet)
                            settings.setType(ViewType.VALUES);
                    }
                });
        Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", valuesToggle);
        Util.mapKeyStrokeToButton(keyComp, "W", "wave2", valuesToggle);
        dest.add(valuesToggle);

        countsToggle = SwarmUtil.createToolBarToggleButton(Icons.rsam_counts, "Counts view (C or .)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        for (RsamViewSettings settings : settingsSet)
                            settings.setType(ViewType.COUNTS);
                    }
                });
        Util.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", countsToggle);
        Util.mapKeyStrokeToButton(keyComp, "S", "spectra2", countsToggle);
        dest.add(countsToggle);

        rsamTypes = new ButtonGroup();
    }

    public void clearSettingsSet() {
        settingsSet.clear();
    }

    public void addSettings(RsamViewSettings s) {
        if (s != null) {
            settingsSet.add(s);
            s.toolbar = this;
            settingsChanged();
        }
    }

    public void removeSettings(RsamViewSettings s) {
        settingsSet.remove(s);
        if (s != null) {
            s.toolbar = null;
            settingsChanged();
        }
    }

    public void setSettings(RsamViewSettings s) {
        clearSettingsSet();
        addSettings(s);
    }

    public void settingsChanged() {
        boolean v = false;
        boolean c = false;
        for (RsamViewSettings set : settingsSet) {
            if (set.viewType == ViewType.VALUES)
                v = true;
            if (set.viewType == ViewType.COUNTS)
                c = true;
        }

        valuesToggle.setSelected(v && !c);
        countsToggle.setSelected(!v && c);
    }
}