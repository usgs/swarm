package gov.usgs.volcanoes.swarm.wave;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.math.Butterworth.FilterType;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;
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


/**
 * Wave view settings dialog.
 * 
 * @author Dan Cervelli
 */
public class WaveViewSettingsDialog extends SwarmModalDialog {
  private static final long serialVersionUID = 1L;

  private JPanel dialogPanel;

  private static WaveViewSettingsDialog dialog;
  private WaveViewSettings settings;

  private JLabel warningLabel;

  // view options
  private ButtonGroup viewGroup;
  private JRadioButton waveButton;
  private JCheckBox removeBias;
  private JCheckBox useUnits;
  private JRadioButton spectraButton;
  private JRadioButton spectrogramButton;
  private JRadioButton particleMotionButton;

  // wave options
  private ButtonGroup waveScaleGroup;
  private JRadioButton waveAutoScale;
  private JRadioButton waveManualScale;
  private JCheckBox waveAutoScaleMemory;
  private JTextField minAmp;
  private JTextField maxAmp;

  // spectra options
  private JCheckBox spectraLogPower;
  private JCheckBox spectraLogFreq;
  private ButtonGroup spectraScaleGroup;
  private JRadioButton spectraAutoScale;
  private JRadioButton spectraManualScale;
  private JTextField spectraMinFreq;
  private JTextField spectraMaxFreq;
  private JTextField spectraPowerRange;
  
  // spectrogram options
  private ButtonGroup spectrogramScaleGroup;
  private JRadioButton spectrogramAutoScale;
  private JRadioButton spectrogramManualScale;  
  private JCheckBox spectrogramLogPower;
  private JTextField spectrogramPowerRange;
  private JTextField spectrogramMinFreq;
  private JTextField spectrogramMaxFreq;
  private JTextField binSize;
  private JTextField nfft;
  private JTextField spectrogramOverlap;

  // filter options
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

  private JCheckBox useAlternateOrientationCode;
  private JTextField alternateOrientationCode;

  private WaveViewSettingsDialog() {
    super(applicationFrame, "Wave Settings");
    createUi();
    setSizeAndLocation();
  }
  
  public static WaveViewSettingsDialog getInstance(WaveViewSettings s) {
    return getInstance(s, 1);
  }
  
  /**
   * Get instance of wave view settings dialog.
   * @param s wave view settings
   * @param count settings count
   * @return wave view settings dialog
   */
  public static WaveViewSettingsDialog getInstance(WaveViewSettings s, int count) {
    if (dialog == null) {
      dialog = new WaveViewSettingsDialog();
    }

    dialog.setSettings(s);
    dialog.setToCurrent();
    dialog.setSettingsCount(count);
    return dialog;
  }

  /**
   * Set settings count.
   * @param i number of different waves
   */
  public void setSettingsCount(int i) {
    settingsCount = i;
    if (settingsCount > 1) {
      warningLabel.setText(
          "You are currently configuring the settings for " + settingsCount + " different waves.");
    } else {
      warningLabel.setText(" ");
    }
  }

  public void setSettings(WaveViewSettings s) {
    settings = s;
  }

  /**
   * Set to current view type.
   */
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
      case PARTICLE_MOTION:
        particleMotionButton.setSelected(true);
        break;
      default:
        break;
    }
    
    // wave settings
    removeBias.setSelected(settings.removeBias);
    useUnits.setSelected(settings.useUnits);
    if (settings.autoScaleAmp) {
      waveAutoScale.setSelected(true);
    } else {
      waveManualScale.setSelected(true);
    }
    waveAutoScaleMemory.setSelected(settings.autoScaleAmpMemory);
    minAmp.setText(String.format("%.1f", settings.waveMinAmp));
    maxAmp.setText(String.format("%.1f", settings.waveMaxAmp));

    // spectra options
    spectraLogPower.setSelected(settings.spectraLogPower);
    spectraLogFreq.setSelected(settings.spectraLogFreq);
    spectraMinFreq.setText(String.format("%.1f", settings.spectraMinFreq));
    spectraMaxFreq.setText(String.format("%.1f", settings.spectraMaxFreq));
    if (settings.autoScaleSpectraPower) {
      spectraAutoScale.setSelected(true);
    } else {
      spectraManualScale.setSelected(true);
    }
    spectraPowerRange
        .setText(String.format("%.1f, %.1f", settings.spectraMinPower, settings.spectraMaxPower));
    
    // spectrogram options
    if (settings.autoScaleSpectrogramPower) {
      spectrogramAutoScale.setSelected(true);
    } else {
      spectrogramManualScale.setSelected(true);
    }
    spectrogramMinFreq.setText(String.format("%.1f", settings.spectrogramMinFreq));
    spectrogramMaxFreq.setText(String.format("%.1f", settings.spectrogramMaxFreq));
    spectrogramLogPower.setSelected(settings.spectrogramLogPower);
    binSize.setText(String.format("%.1f", settings.binSize));
    nfft.setText(String.format("%d", settings.nfft));
    spectrogramPowerRange.setText(
        String.format("%.1f, %.1f", settings.spectrogramMinPower, settings.spectrogramMaxPower));
    spectrogramOverlap.setText(String.format("%3.0f", settings.spectrogramOverlap * 100));

    // particle motion options
    useAlternateOrientationCode.setSelected(settings.useAlternateOrientationCode);
    alternateOrientationCode.setText(settings.alternateOrientationCode);
    
    // filter options
    filterEnabled.setSelected(settings.filterOn);
    switch (settings.filter.getType()) {
      case LOWPASS:
        lowPass.setSelected(true);
        corner1.setText("0.00");
        corner2.setText(String.format("%.2f", settings.filter.getCorner2()));
        break;
      case HIGHPASS:
        highPass.setSelected(true);
        corner1.setText(String.format("%.2f", settings.filter.getCorner1()));
        corner2.setText("0.00");
        break;
      case BANDPASS:
        bandPass.setSelected(true);
        corner1.setText(String.format("%.2f", settings.filter.getCorner1()));
        corner2.setText(String.format("%.2f", settings.filter.getCorner2()));
        break;
      default:
        break;
    }
    order.setValue(settings.filter.getOrder());
  }

  private void createComponents() {
    warningLabel = new JLabel(" ");

    // View options
    viewGroup = new ButtonGroup();
    waveButton = new JRadioButton("Wave");
    spectraButton = new JRadioButton("Spectra");
    spectrogramButton = new JRadioButton("Spectrogram");
    particleMotionButton = new JRadioButton("Particle Motion");
    viewGroup.add(waveButton);
    viewGroup.add(spectraButton);
    viewGroup.add(spectrogramButton);
    viewGroup.add(particleMotionButton);

    // Wave options
    waveScaleGroup = new ButtonGroup();
    removeBias = new JCheckBox("Remove bias");
    useUnits = new JCheckBox("Use calibrations");
    waveManualScale = new JRadioButton("Manual scale amp.");
    waveAutoScale = new JRadioButton("Auto scale amp.");
    waveScaleGroup.add(waveAutoScale);
    waveScaleGroup.add(waveManualScale);
    waveAutoScaleMemory = new JCheckBox("Persistent rescale");
    minAmp = new JTextField(7);
    maxAmp = new JTextField(7);

    // spectra options
    spectraLogPower = new JCheckBox("Log power");
    spectraLogFreq = new JCheckBox("Log frequency");
    spectraMinFreq = new JTextField(4);
    spectraMaxFreq = new JTextField(4);
    spectraScaleGroup = new ButtonGroup();
    spectraAutoScale = new JRadioButton("Auto scale power");
    spectraManualScale = new JRadioButton("Manual scale power");
    spectraScaleGroup.add(spectraAutoScale);
    spectraScaleGroup.add(spectraManualScale);
    spectraPowerRange = new JTextField(6);
    
    // spectrogram options
    spectrogramLogPower = new JCheckBox("Log power");
    spectrogramPowerRange = new JTextField(6);
    spectrogramMinFreq = new JTextField(4);
    spectrogramMaxFreq = new JTextField(4);
    spectrogramScaleGroup = new ButtonGroup();
    spectrogramAutoScale = new JRadioButton("Auto scale power");
    spectrogramManualScale = new JRadioButton("Manual scale power");
    spectrogramScaleGroup.add(spectrogramAutoScale);
    spectrogramScaleGroup.add(spectrogramManualScale);
    binSize = new JTextField(4);
    nfft = new JTextField(4);
    spectrogramOverlap = new JTextField(4);

    // particle motion options
    useAlternateOrientationCode = new JCheckBox("Use alternate orientation code");
    alternateOrientationCode = new JTextField("Z12");
    
    // filter options
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

  protected void createUi() {
    super.createUi();
    createComponents();
    FormLayout layout =
        new FormLayout("left:70dlu, 3dlu, left:60dlu, 3dlu, left:60dlu, 3dlu, left:70dlu", "");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);

    CellConstraints cc = new CellConstraints();

    // view options
    builder.appendSeparator("View");
    builder.nextLine();
    builder.append(waveButton);
    builder.append(spectraButton);
    builder.append(spectrogramButton);
    builder.append(particleMotionButton);
    builder.nextLine();

    // wave options
    builder.appendSeparator("Wave Options");
    builder.nextLine();
    builder.append(removeBias);
    builder.add(new JLabel("Min. Amplitude:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(minAmp);
    builder.append(waveAutoScale);
    builder.nextLine();
    builder.append(useUnits);
    builder.add(new JLabel("Max. Amplitude:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(maxAmp);
    builder.append(waveManualScale);

    builder.nextLine();
    builder.append(new JLabel(""), 5);
    builder.append(waveAutoScaleMemory);
    builder.nextLine();

    // spectra optins
    builder.appendSeparator("Spectra Options");
    builder.nextLine(); // row 1
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("Min. frequency:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectraMinFreq);
    builder.append(spectraAutoScale, 3);
    builder.nextLine(); // row 2
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("Max. frequency:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectraMaxFreq);
    builder.append(spectraManualScale, 3);
    builder.nextLine(); // row 3
    builder.append(spectraLogPower);
    builder.append(spectraLogFreq);
    builder.add(new JLabel("Power range (dB):"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectraPowerRange);
    builder.nextLine();

    // spectrogram
    builder.appendSeparator("Spectrogram Options");
    builder.nextLine(); // row 1
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("Min. frequency:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectrogramMinFreq);
    builder.append(spectrogramAutoScale, 3);
    builder.nextLine(); // row 2
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("Max. frequency:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectrogramMaxFreq);
    builder.append(spectrogramManualScale, 3);
    builder.nextLine(); // row 3
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("Window size (s):"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(binSize);
    builder.add(new JLabel("Power range (dB):"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectrogramPowerRange);
    builder.nextLine(); // row 4
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("# of FFT points:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(nfft);
    builder.append(spectrogramLogPower,3);
    builder.nextLine(); // row 5
    builder.appendRow("center:18dlu");
    builder.add(new JLabel("Overlap (%)"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(spectrogramOverlap);
    builder.nextLine();
    
    // particle motion
    builder.appendSeparator("Particle Motion Options");
    builder.nextLine();
    builder.append(useAlternateOrientationCode, 3);
    builder.append(new JLabel("ZNE alternative:"));
    builder.append(alternateOrientationCode);
    builder.nextLine();

    // filter
    builder.appendSeparator("Butterworth Filter");
    builder.append(filterEnabled, 3);
    builder.append(zeroPhaseShift, 3);
    builder.nextLine();
    builder.append(lowPass, 3);
    builder.add(new JLabel("Min. frequency:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(corner1);
    builder.nextLine();
    builder.append(highPass, 3);
    builder.add(new JLabel("Max. frequency:"),
        cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
    builder.nextColumn(2);
    builder.append(corner2);
    builder.nextLine();
    builder.append(bandPass, 3);
    builder.add(new JLabel("Order"),
        cc.xyw(builder.getColumn(), builder.getRow(), 3, "center, center"));
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

  /**
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#allowOk()
   */
  public boolean allowOk() {
    String message = null;
    try {
      message = "Error in minimum ampitude format.";
      double min = Double.parseDouble(minAmp.getText());
      message = "Error in maximum ampitude format.";
      double max = Double.parseDouble(maxAmp.getText());
      message = "Minimum amplitude must be less than maximum amplitude.";
      if (min >= max) {
        throw new NumberFormatException();
      }

      message = "Error in minimum frequency format.";
      double minf = Double.parseDouble(spectrogramMinFreq.getText());
      message = "Error in maximum frequency format.";
      double maxf = Double.parseDouble(spectrogramMaxFreq.getText());
      message = "Minimum frequency must be 0 or above and less than maximum frequency.";
      if (minf < 0 || minf >= maxf) {
        throw new NumberFormatException();
      }

      message = "Error in spectrogram overlap format.";
      double so = Double.parseDouble(spectrogramOverlap.getText());
      message = "Spectrogram overlap must be between 0 and 95%.";
      if (so < 0 || so > 95) {
        throw new NumberFormatException();
      }

      message = "Error in minimum Hz format.";
      double c1 = Double.parseDouble(corner1.getText());
      message = "Minimum Hz must be 0 or above.";
      if (c1 < 0) {
        throw new NumberFormatException();
      }

      message = "Error in maximum Hz format.";
      double c2 = Double.parseDouble(corner2.getText());
      message = "Maximum Hz must be 0 or above.";
      if (c2 < 0) {
        throw new NumberFormatException();
      }

      message = "Minimum Hz must be less than maximum Hz.";
      if (bandPass.isSelected()) {
        if (c1 >= c2) {
          throw new NumberFormatException();
        }
      }

      return true;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

  /**
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasOk()
   */
  public void wasOk() {
    try {
      // view option
      if (waveButton.isSelected()) {
        settings.viewType = ViewType.WAVE;
      } else if (spectraButton.isSelected()) {
        settings.viewType = ViewType.SPECTRA;
      } else if (spectrogramButton.isSelected()) {
        settings.viewType = ViewType.SPECTROGRAM;
      } else if (particleMotionButton.isSelected()) {
        settings.viewType = ViewType.PARTICLE_MOTION;
      } 
      
      // wave options
      settings.removeBias = removeBias.isSelected();
      settings.useUnits = useUnits.isSelected();
      settings.autoScaleAmp = waveAutoScale.isSelected();
      settings.autoScaleAmpMemory = waveAutoScaleMemory.isSelected();
      settings.waveMinAmp = Double.parseDouble(minAmp.getText());
      settings.waveMaxAmp = Double.parseDouble(maxAmp.getText());
      
      // spectra options
      settings.spectraLogFreq = spectraLogFreq.isSelected();
      settings.spectraLogPower = spectraLogPower.isSelected();
      settings.spectraMaxFreq = Double.parseDouble(spectraMaxFreq.getText());
      settings.spectraMinFreq = Double.parseDouble(spectraMinFreq.getText());
      if (settings.spectraMinFreq < 0) {
        settings.spectraMinFreq = 0;
      }
      settings.autoScaleSpectraPower = spectraAutoScale.isSelected();
      double newMinPower = Double.parseDouble(spectraPowerRange.getText().split(",")[0]);
      double newMaxPower = Double.parseDouble(spectraPowerRange.getText().split(",")[1]);
      if (newMinPower != settings.spectraMinPower | newMaxPower != settings.spectraMaxPower) {
        settings.spectraMinPower = Double.parseDouble(spectraPowerRange.getText().split(",")[0]);
        settings.spectraMaxPower = Double.parseDouble(spectraPowerRange.getText().split(",")[1]);
        settings.autoScaleSpectraPower = false;
      }
      
      // spectrogram options
      settings.spectrogramMaxFreq = Double.parseDouble(spectrogramMaxFreq.getText());
      settings.spectrogramMinFreq = Double.parseDouble(spectrogramMinFreq.getText());
      if (settings.spectrogramMinFreq < 0) {
        settings.spectrogramMinFreq = 0;
      }
      settings.autoScaleSpectrogramPower = spectrogramAutoScale.isSelected();
      newMinPower = Double.parseDouble(spectrogramPowerRange.getText().split(",")[0]);
      newMaxPower = Double.parseDouble(spectrogramPowerRange.getText().split(",")[1]);
      if (newMinPower != settings.spectrogramMinPower
          | newMaxPower != settings.spectrogramMaxPower) {
        settings.spectrogramMinPower =
            Double.parseDouble(spectrogramPowerRange.getText().split(",")[0]);
        settings.spectrogramMaxPower =
            Double.parseDouble(spectrogramPowerRange.getText().split(",")[1]);
        settings.autoScaleSpectrogramPower = false;
      }
      settings.binSize = Double.parseDouble(binSize.getText());
      settings.nfft = Integer.parseInt(nfft.getText());
      settings.spectrogramOverlap = Double.parseDouble(spectrogramOverlap.getText()) / 100;
      if (settings.spectrogramOverlap > 0.95 || settings.spectrogramOverlap < 0) {
        settings.spectrogramOverlap = 0;
      }
      settings.spectrogramLogPower = spectrogramLogPower.isSelected();
      settings.notifyView();
      
      // particle motion options
      settings.useAlternateOrientationCode = useAlternateOrientationCode.isSelected();
      settings.alternateOrientationCode = alternateOrientationCode.getText();

      // filter options
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
      JOptionPane.showMessageDialog(this, "Illegal values.", "Options Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}
