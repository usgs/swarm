package gov.usgs.swarm.rsam;

import gov.usgs.math.BinSize;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.SwarmDialog;
import gov.usgs.swarm.rsam.RsamViewSettings.ViewType;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * 
 * 
 * @author Tom Parker
 */
public class RsamViewSettingsDialog extends SwarmDialog {
    private static final long serialVersionUID = 1L;

    private JPanel dialogPanel;

    private static RsamViewSettingsDialog dialog;
    private RsamViewSettings settings;

    private ButtonGroup viewGroup;
    private JRadioButton valuesButton;
    private JRadioButton countsButton;

    private JCheckBox detrend;
    private JComboBox valuesPeriod;
    private JComboBox countsPeriod;

    private JCheckBox bandpassButton;
    private JTextField bandpassMin;
    private JTextField bandpassMax;
    private JCheckBox runningMedianButton;
    private JTextField runningMedianPeriod;
    private JCheckBox runningMeanButton;
    private JTextField runningMeanPeriod;

    private JTextField eventThreshold;
    private JTextField eventRatio;
    private JTextField eventMaxLength;
    private JComboBox binSize;

    private RsamViewSettingsDialog() {
        super(applicationFrame, "RSAM Settings", true);
        this.setIconImage(Icons.rsam_values.getImage());
        createUI();
        setSizeAndLocation();
    }

    public static RsamViewSettingsDialog getInstance(RsamViewSettings s) {
        return getInstance(s, 1);
    }

    public static RsamViewSettingsDialog getInstance(RsamViewSettings s, int count) {
        if (dialog == null)
            dialog = new RsamViewSettingsDialog();

        dialog.settings = s;
        dialog.setToCurrent();
        return dialog;
    }

    public void setToCurrent() {
        switch (settings.viewType) {
        case VALUES:
            valuesButton.setSelected(true);
            break;
        case COUNTS:
            countsButton.setSelected(true);
            break;
        }

        detrend.setSelected(settings.detrend);

        bandpassButton.setSelected(settings.bandpass);
        bandpassMin.setText(String.format("%.1f", settings.bandpassMin));
        bandpassMin.setEnabled(bandpassButton.isSelected());
        bandpassMax.setText(String.format("%.1f", settings.bandpassMax));
        bandpassMax.setEnabled(bandpassButton.isSelected());
        runningMedianButton.setSelected(settings.runningMedian);
        runningMedianPeriod.setText(String.format("%.1f", settings.runningMedianPeriod));
        runningMedianPeriod.setEnabled(settings.runningMedian);
        runningMeanButton.setSelected(settings.runningMean);
        runningMeanPeriod.setText(String.format("%.1f", settings.runningMeanPeriod));
        runningMeanPeriod.setEnabled(settings.runningMean);
        eventThreshold.setText(String.format("%d", settings.eventThreshold));
        eventRatio.setText(String.format("%.1f", settings.eventRatio));
        eventMaxLength.setText(String.format("%.1f", settings.eventMaxLength));
        binSize.setSelectedItem(settings.binSize);
    }

    private void createComponents() {
        viewGroup = new ButtonGroup();
        valuesButton = new JRadioButton("RSAM values");
        countsButton = new JRadioButton("Event counts");
        viewGroup.add(valuesButton);
        viewGroup.add(countsButton);

        valuesPeriod = new JComboBox(ValuesPeriods.values());
        countsPeriod = new JComboBox(countsPeriods.values());
        
        detrend = new JCheckBox("Detrend (linear)");
 
        bandpassButton = new JCheckBox("Bandpass");
        bandpassMin = new JTextField(4);
        bandpassMax = new JTextField(4);
        bandpassButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                bandpassMin.setEnabled(bandpassButton.isSelected());
                bandpassMax.setEnabled(bandpassButton.isSelected());
            }
        });
        runningMedianButton = new JCheckBox("Running median");
        runningMedianPeriod = new JTextField(4);
        runningMedianButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runningMedianPeriod.setEnabled(runningMedianButton.isSelected());
            }
        });
        runningMeanButton = new JCheckBox("Running mean");
        runningMeanPeriod = new JTextField(4);
        runningMeanButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runningMeanPeriod.setEnabled(runningMeanButton.isSelected());
            }
        });
        
        eventThreshold = new JTextField(4);
        eventRatio = new JTextField(4);
        eventMaxLength = new JTextField(4);
        binSize = new JComboBox(BinSize.values());
    }

    protected void createUI() {
        super.createUI();
        createComponents();
        FormLayout layout = new FormLayout(
                "left:65dlu, 1dlu, left:60dlu, 3dlu, left:30dlu, 3dlu, left:30dlu, 3dlu, left:30dlu", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.appendSeparator("View");
        builder.nextLine();
        builder.append(valuesButton);
        builder.append(countsButton);
        builder.nextLine();

        builder.appendSeparator("RSAM Options");
        builder.nextLine();
        builder.append(detrend);
        builder.add(new JLabel("Period:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(valuesPeriod, cc.xyw(builder.getColumn(), builder.getRow(), 3));
        builder.nextLine();

        builder.appendSeparator("Filter Options");
        builder.nextLine();
        builder.append(bandpassButton);
        builder.add(new JLabel("Min:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(bandpassMin);
        builder.add(new JLabel("Max:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(bandpassMax);
        builder.nextLine();
        builder.append(runningMedianButton);
        builder.add(new JLabel("Period:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(runningMedianPeriod);
        builder.nextLine();
        builder.append(runningMeanButton);
        builder.add(new JLabel("Period:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(runningMeanPeriod);
        builder.nextLine();

        builder.appendSeparator("Event Options");
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Event threshold:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(eventThreshold);
        builder.add(new JLabel("Bin size:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(binSize, cc.xyw(builder.getColumn(), builder.getRow(), 3));
        builder.nextColumn();
        
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Event ratio:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(eventRatio);
        builder.add(new JLabel("Period:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(countsPeriod, cc.xyw(builder.getColumn(), builder.getRow(), 3));
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Event max length:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(eventMaxLength);

        dialogPanel = builder.getPanel();
        mainPanel.add(dialogPanel, BorderLayout.CENTER);

    }

    public boolean allowOK() {
        String message = null;
        try {
            message = "Error in bandpass minimum frequency.";
            double bmin = Double.parseDouble(bandpassMin.getText());
            message = "Error in bandpass maximum frequency.";
            double bmax = Double.parseDouble(bandpassMax.getText());
            message = "Bandpass minimum frequency must be less than bandpass maximum frequency.";
            if (bmin >= bmax)
                throw new NumberFormatException();
            message = "Error in running median period.";
            double rm = Double.parseDouble(runningMedianPeriod.getText());
            message = "Error in running mean period.";
            double rmp = Double.parseDouble(runningMeanPeriod.getText());

            message = "Error in event threshold.";
            double et = Double.parseDouble(eventThreshold.getText());
            message = "Error in event ratio.";
            double er = Double.parseDouble(eventRatio.getText());
            message = "Error in event maximum length.";
            double eml = Double.parseDouble(eventMaxLength.getText());

            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }

    public void wasOK() {
        
        try {
            if (valuesButton.isSelected())
                settings.viewType = ViewType.VALUES;
            else
                settings.viewType = ViewType.COUNTS;

            settings.detrend = detrend.isSelected();

            settings.bandpass = bandpassButton.isSelected();
            settings.bandpassMin = Double.parseDouble(bandpassMin.getText());
            settings.bandpassMax = Double.parseDouble(bandpassMax.getText());
            settings.runningMean = runningMeanButton.isSelected();
            settings.runningMeanPeriod = Double.parseDouble(runningMeanPeriod.getText());
            settings.runningMedian = runningMedianButton.isSelected();
            settings.runningMedianPeriod = Double.parseDouble(runningMedianPeriod.getText());
            
            settings.eventThreshold = Integer.parseInt(eventThreshold.getText());
            settings.eventRatio = Double.parseDouble(eventRatio.getText());
            settings.eventMaxLength = Double.parseDouble(eventMaxLength.getText());
            settings.binSize = (BinSize) binSize.getSelectedItem();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Illegal values.", "Options Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
