package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.math.Butterworth.FilterType;
import gov.usgs.volcanoes.swarm.SwarmDialog;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;

import java.awt.BorderLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * 
 * 
 * @author Dan Cervelli
 */
public class WaveViewSettingsDialog extends SwarmDialog {
    private static final long serialVersionUID = 1L;

    private JPanel dialogPanel;

    private static WaveViewSettingsDialog dialog;
    private WaveViewSettings settings;

    private JLabel warningLabel;

    private ButtonGroup viewGroup;
    private JRadioButton waveButton;
    private JCheckBox removeBias;
    private JCheckBox useUnits;
    private JRadioButton spectraButton;
    private JRadioButton spectrogramButton;

    private ButtonGroup waveScaleGroup;
    private JRadioButton waveAutoScale;
    private JRadioButton waveManualScale;
    private JCheckBox waveAutoScaleMemory;
    private JTextField minAmp;
    private JTextField maxAmp;

    private JCheckBox logPower;
    private JCheckBox logFreq;
    private JTextField powerRange;
    private JTextField minFreq;
    private JTextField maxFreq;
    private ButtonGroup spectraScaleGroup;
    private JRadioButton spectraAutoScale;
    private JRadioButton spectraManualScale;
    private JTextField binSize;
    private JTextField nfft;
    private JTextField spectrogramOverlap;

    private ButtonGroup filterGroup;
    private JCheckBox filterEnabled;
    private JRadioButton lowPass;
    private JRadioButton highPass;
    private JRadioButton bandPass;
    private JCheckBox zeroPhaseShift;
    private JTextField corner1;
    private JTextField corner2;
    private JSlider order;
    private int settingsCount;

    private WaveViewSettingsDialog() {
        super(applicationFrame, "Wave Settings", true);
        createUI();
        setSizeAndLocation();
    }

    public static WaveViewSettingsDialog getInstance(WaveViewSettings s) {
        return getInstance(s, 1);
    }

    public static WaveViewSettingsDialog getInstance(WaveViewSettings s, int count) {
        if (dialog == null)
            dialog = new WaveViewSettingsDialog();

        dialog.setSettings(s);
        dialog.setToCurrent();
        dialog.setSettingsCount(count);
        return dialog;
    }

    public void setSettingsCount(int i) {
        settingsCount = i;
        if (settingsCount > 1)
            warningLabel.setText("You are currently configuring the settings for " + settingsCount
                    + " different waves.");
        else
            warningLabel.setText(" ");
    }

    public void setSettings(WaveViewSettings s) {
        settings = s;
    }

    public void setToCurrent() {
        switch (settings.viewType) {
        case WAVE:
            waveButton.setSelected(true);
            break;
        case SPECTRA:
            spectraButton.setSelected(true);
            break;
        case SPECTROGRAM:
            spectrogramButton.setSelected(true);
            break;
        }
        removeBias.setSelected(settings.removeBias);
        useUnits.setSelected(settings.useUnits);

        if (settings.autoScaleAmp)
            waveAutoScale.setSelected(true);
        else
            waveManualScale.setSelected(true);
        waveAutoScaleMemory.setSelected(settings.autoScaleAmpMemory);

        if (settings.autoScalePower)
            spectraAutoScale.setSelected(true);
        else
            spectraManualScale.setSelected(true);

        minAmp.setText(String.format("%.1f", settings.minAmp));
        maxAmp.setText(String.format("%.1f", settings.maxAmp));
        powerRange.setText(String.format("%.1f, %.1f", settings.minPower, settings.maxPower));

        binSize.setText(String.format("%.1f", settings.binSize));
        nfft.setText(String.format("%d", settings.nfft));
        minFreq.setText(String.format("%.1f", settings.minFreq));
        maxFreq.setText(String.format("%.1f", settings.maxFreq));
        logFreq.setSelected(settings.logFreq);
        logPower.setSelected(settings.logPower);
        spectrogramOverlap.setText(String.format("%3.0f", settings.spectrogramOverlap * 100));

        filterEnabled.setSelected(settings.filterOn);

        switch (settings.filter.getType()) {
        case LOWPASS:
            lowPass.setSelected(true);
            corner1.setText("0.0");
            corner2.setText(String.format("%.1f", settings.filter.getCorner1()));
            break;
        case HIGHPASS:
            highPass.setSelected(true);
            corner1.setText(String.format("%.1f", settings.filter.getCorner1()));
            corner2.setText("0.0");
            break;
        case BANDPASS:
            bandPass.setSelected(true);
            corner1.setText(String.format("%.1f", settings.filter.getCorner1()));
            corner2.setText(String.format("%.1f", settings.filter.getCorner2()));
            break;
        }
        order.setValue(settings.filter.getOrder());
    }

    private void createComponents() {
        warningLabel = new JLabel(" ");

        viewGroup = new ButtonGroup();
        waveButton = new JRadioButton("Wave");
        spectraButton = new JRadioButton("Spectra");
        spectrogramButton = new JRadioButton("Spectrogram");
        viewGroup.add(waveButton);
        viewGroup.add(spectraButton);
        viewGroup.add(spectrogramButton);

        waveScaleGroup = new ButtonGroup();
        removeBias = new JCheckBox("Remove bias");
        useUnits = new JCheckBox("Use calibrations");
        waveManualScale = new JRadioButton("Manual scale");
        waveAutoScale = new JRadioButton("Autoscale");
        waveScaleGroup.add(waveAutoScale);
        waveScaleGroup.add(waveManualScale);
        waveAutoScaleMemory = new JCheckBox("Persistent rescale");
        minAmp = new JTextField(7);
        maxAmp = new JTextField(7);

        logPower = new JCheckBox("Log power");
        logFreq = new JCheckBox("Log frequency");
        powerRange = new JTextField(6);
        minFreq = new JTextField(4);
        maxFreq = new JTextField(4);
        spectraScaleGroup = new ButtonGroup();
        spectraAutoScale = new JRadioButton("Auto scale");
        spectraManualScale = new JRadioButton("Manual scale");
        spectraScaleGroup.add(spectraAutoScale);
        spectraScaleGroup.add(spectraManualScale);
        binSize = new JTextField(4);
        nfft = new JTextField(4);
        spectrogramOverlap = new JTextField(4);

        filterGroup = new ButtonGroup();
        filterEnabled = new JCheckBox("Enabled");
        lowPass = new JRadioButton("Low pass");
        highPass = new JRadioButton("High pass");
        bandPass = new JRadioButton("Band pass");
        filterGroup.add(lowPass);
        filterGroup.add(highPass);
        filterGroup.add(bandPass);
        zeroPhaseShift = new JCheckBox("Zero phase shift (doubles order)");
        corner1 = new JTextField(7);
        corner2 = new JTextField(7);
        order = new JSlider(2, 8, 4);
        order.setMajorTickSpacing(2);
        order.setSnapToTicks(true);
        order.createStandardLabels(2);
        order.setPaintLabels(true);
    }

    protected void createUI() {
        super.createUI();
        createComponents();
        FormLayout layout = new FormLayout("left:60dlu, 3dlu, left:60dlu, 3dlu, left:60dlu, 3dlu, left:60dlu", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.appendSeparator("View");
        builder.nextLine();
        builder.append(waveButton);
        builder.append(spectraButton);
        builder.append(spectrogramButton);
        builder.nextLine();

        builder.appendSeparator("Wave Options");
        builder.nextLine();
        builder.append(removeBias);
        builder.add(new JLabel("Min. Amplitude:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(minAmp);
        builder.append(waveAutoScale);
        builder.nextLine();
        builder.append(useUnits);
        builder.add(new JLabel("Max. Amplitude:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(maxAmp);
        builder.append(waveManualScale);

        builder.nextLine();
        builder.append(new JLabel(""), 5);
        builder.append(waveAutoScaleMemory);
        builder.nextLine();

        builder.appendSeparator("Spectra Options");
        builder.nextLine();
        builder.append(new JLabel(""), 1);
        builder.append(logPower);
        builder.append(logFreq);
        builder.nextLine();

        builder.appendSeparator("Spectrogram Options");
        builder.nextLine();
        builder.append(new JLabel(""), 1);
        builder.append(spectraAutoScale);
        builder.append(spectraManualScale);
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Min. frequency:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(minFreq);
        builder.add(new JLabel("Window size (s):"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(binSize);
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Max. frequency:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(maxFreq);
        builder.add(new JLabel("# of FFT points:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(nfft);
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Overlap (%)"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(spectrogramOverlap);
        builder.add(new JLabel("Power range (dB):"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(powerRange);
        builder.nextLine();

        builder.appendSeparator("Butterworth Filter");
        builder.append(filterEnabled, 3);
        builder.append(zeroPhaseShift, 3);
        builder.nextLine();
        builder.append(lowPass, 3);
        builder.add(new JLabel("Min. frequency:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(corner1);
        builder.nextLine();
        builder.append(highPass, 3);
        builder.add(new JLabel("Max. frequency:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(corner2);
        builder.nextLine();
        builder.append(bandPass, 3);
        builder.add(new JLabel("Order"), cc.xyw(builder.getColumn(), builder.getRow(), 3, "center, center"));
        builder.nextLine();
        builder.appendRow("center:20dlu");
        builder.nextColumn(3);
        builder.append(order, 4);
        builder.nextLine();
        builder.append(warningLabel, 7);
        builder.nextLine();

        dialogPanel = builder.getPanel();
        mainPanel.add(dialogPanel, BorderLayout.CENTER);

    }

    public boolean allowOK() {
        String message = null;
        try {
            message = "Error in minimum ampitude format.";
            double min = Double.parseDouble(minAmp.getText());
            message = "Error in maximum ampitude format.";
            double max = Double.parseDouble(maxAmp.getText());
            message = "Minimum amplitude must be less than maximum amplitude.";
            if (min >= max)
                throw new NumberFormatException();

            message = "Error in minimum frequency format.";
            double minf = Double.parseDouble(minFreq.getText());
            message = "Error in maximum frequency format.";
            double maxf = Double.parseDouble(maxFreq.getText());
            message = "Minimum frequency must be 0 or above and less than maximum frequency.";
            if (minf < 0 || minf >= maxf)
                throw new NumberFormatException();

            message = "Error in spectrogram overlap format.";
            double so = Double.parseDouble(spectrogramOverlap.getText());
            message = "Spectrogram overlap must be between 0 and 95%.";
            if (so < 0 || so > 95)
                throw new NumberFormatException();

            message = "Error in minimum Hz format.";
            double c1 = Double.parseDouble(corner1.getText());
            message = "Minimum Hz must be 0 or above.";
            if (c1 < 0)
                throw new NumberFormatException();

            message = "Error in maximum Hz format.";
            double c2 = Double.parseDouble(corner2.getText());
            message = "Maximum Hz must be 0 or above.";
            if (c2 < 0)
                throw new NumberFormatException();

            message = "Minimum Hz must be less than maximum Hz.";
            if (bandPass.isSelected())
                if (c1 >= c2)
                    throw new NumberFormatException();

            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    public void wasOK() {
        try {
            if (waveButton.isSelected())
                settings.viewType = ViewType.WAVE;
            else if (spectraButton.isSelected())
                settings.viewType = ViewType.SPECTRA;
            else if (spectrogramButton.isSelected())
                settings.viewType = ViewType.SPECTROGRAM;

            settings.removeBias = removeBias.isSelected();
            settings.useUnits = useUnits.isSelected();
            settings.autoScaleAmp = waveAutoScale.isSelected();
            settings.autoScaleAmpMemory = waveAutoScaleMemory.isSelected();

            settings.autoScalePower = spectraAutoScale.isSelected();

            double newMinPower = Double.parseDouble(powerRange.getText().split(",")[0]);
            double newMaxPower = Double.parseDouble(powerRange.getText().split(",")[1]);

            if (newMinPower != settings.minPower | newMaxPower != settings.maxPower) {
                settings.minPower = Double.parseDouble(powerRange.getText().split(",")[0]);
                settings.maxPower = Double.parseDouble(powerRange.getText().split(",")[1]);
                settings.autoScalePower = false;
            }

            settings.minAmp = Double.parseDouble(minAmp.getText());
            settings.maxAmp = Double.parseDouble(maxAmp.getText());

            settings.maxFreq = Double.parseDouble(maxFreq.getText());
            settings.minFreq = Double.parseDouble(minFreq.getText());
            if (settings.minFreq < 0)
                settings.minFreq = 0;

            settings.binSize = Double.parseDouble(binSize.getText());
            settings.nfft = Integer.parseInt(nfft.getText());
            settings.logFreq = logFreq.isSelected();
            settings.logPower = logPower.isSelected();
            settings.spectrogramOverlap = Double.parseDouble(spectrogramOverlap.getText()) / 100;
            if (settings.spectrogramOverlap > 0.95 || settings.spectrogramOverlap < 0)
                settings.spectrogramOverlap = 0;
            settings.notifyView();

            settings.filterOn = filterEnabled.isSelected();
            settings.zeroPhaseShift = zeroPhaseShift.isSelected();

            FilterType ft = null;
            double c1 = 0;
            double c2 = 0;
            if (lowPass.isSelected()) {
                ft = FilterType.LOWPASS;
                c1 = Double.parseDouble(corner2.getText());
                c2 = 0;
            } else if (highPass.isSelected()) {
                ft = FilterType.HIGHPASS;
                c1 = Double.parseDouble(corner1.getText());
                c2 = 0;
            } else if (bandPass.isSelected()) {
                ft = FilterType.BANDPASS;
                c1 = Double.parseDouble(corner1.getText());
                c2 = Double.parseDouble(corner2.getText());
            }
            settings.filter.set(ft, order.getValue(), 100, c1, c2);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Illegal values.", "Options Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
