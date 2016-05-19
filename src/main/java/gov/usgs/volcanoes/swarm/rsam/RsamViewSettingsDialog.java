package gov.usgs.volcanoes.swarm.rsam;

import gov.usgs.math.BinSize;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings.ViewType;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

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
public class RsamViewSettingsDialog extends SwarmModalDialog {
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

    private JCheckBox runningMedianButton;
    private JTextField runningMedianPeriod;
    private JCheckBox runningMeanButton;
    private JTextField runningMeanPeriod;

    private JTextField eventThreshold;
    private JTextField eventRatio;
    private JTextField eventMaxLength;
    private JComboBox binSize;
    
    private JCheckBox autoScale;
    private JTextField scaleMax;
    private JTextField scaleMin;

    private RsamViewSettingsDialog() {
        super(applicationFrame, "RSAM Settings");
        this.setIconImage(Icons.rsam_values.getImage());
        createUI();
        setSizeAndLocation();
    }


    public static RsamViewSettingsDialog getInstance(RsamViewSettings s) {
        if (dialog == null)
            dialog = new RsamViewSettingsDialog();

        dialog.settings = s;
        dialog.setToCurrent();
        return dialog;
    }

    public void setToCurrent() {
        switch (settings.getType()) {
        case VALUES:
            valuesButton.setSelected(true);
            break;
        case COUNTS:
            countsButton.setSelected(true);
            break;
        }

        detrend.setSelected(settings.detrend);

        valuesPeriod.setSelectedItem(ValuesPeriods.fromS(settings.valuesPeriodS));
        countsPeriod.setSelectedItem(CountsPeriods.fromS(settings.countsPeriodS));
        runningMedianButton.setSelected(settings.runningMedian);
        runningMedianPeriod.setText(String.format("%.1f", settings.runningMedianPeriodS));
        runningMedianPeriod.setEnabled(settings.runningMedian);
        runningMeanButton.setSelected(settings.runningMean);
        runningMeanPeriod.setText(String.format("%.1f", settings.runningMeanPeriodS));
        runningMeanPeriod.setEnabled(settings.runningMean);
        eventThreshold.setText(String.format("%d", settings.eventThreshold));
        eventRatio.setText(String.format("%.1f", settings.eventRatio));
        eventMaxLength.setText(String.format("%.1f", settings.eventMaxLengthS));
        binSize.setSelectedItem(settings.binSize);
        autoScale.setSelected(settings.getAutoScale());
        scaleMax.setText("" + settings.scaleMax);
        scaleMax.setEnabled(!autoScale.isSelected());
        scaleMin.setText("" + settings.scaleMin);
        scaleMin.setEnabled(!autoScale.isSelected());
    }

    private void createComponents() {
        viewGroup = new ButtonGroup();
        valuesButton = new JRadioButton("RSAM values");
        countsButton = new JRadioButton("Event counts");
        viewGroup.add(valuesButton);
        viewGroup.add(countsButton);

        valuesPeriod = new JComboBox(ValuesPeriods.values());
        countsPeriod = new JComboBox(CountsPeriods.values());
        
        detrend = new JCheckBox("Detrend (linear)");
 
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
        
        autoScale = new JCheckBox("Auto scale");
        autoScale.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                scaleMax.setEnabled(!autoScale.isSelected());
                scaleMin.setEnabled(!autoScale.isSelected());
            }
        });

        scaleMax = new JTextField(4);
        scaleMin = new JTextField(4);
        
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
        builder.nextColumn(2);
        builder.add(new JLabel("Period:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(valuesPeriod, cc.xyw(builder.getColumn(), builder.getRow(), 3));

        builder.nextLine();
        builder.append(autoScale);
        builder.nextColumn(2);
        builder.add(new JLabel("Min:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(scaleMin, cc.xyw(builder.getColumn(), builder.getRow(), 1));
        
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.nextColumn(4);
        builder.add(new JLabel("Max:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(scaleMax, cc.xyw(builder.getColumn(), builder.getRow(), 1));

        builder.nextLine();
        builder.appendSeparator("Event Options");
        builder.nextLine();
        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Event threshold:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(eventThreshold);
        builder.add(new JLabel("Period:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(countsPeriod, cc.xyw(builder.getColumn(), builder.getRow(), 3));
        builder.nextLine();

        builder.appendRow("center:18dlu");
        builder.add(new JLabel("Event ratio:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.append(eventRatio);
        builder.add(new JLabel("Bin size:"), cc.xy(builder.getColumn(), builder.getRow(), "right, center"));
        builder.nextColumn(2);
        builder.add(binSize, cc.xyw(builder.getColumn(), builder.getRow(), 3));
        
        
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

            if (!autoScale.isSelected()) {
                message = "Error in Scale min";
                int sm = Integer.parseInt(scaleMin.getText());
                message = "Error in Scale max";
                int sn = Integer.parseInt(scaleMax.getText());
            }
            
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }

    public void wasOK() {
        
        try {
            settings.valuesPeriodS = ((ValuesPeriods)valuesPeriod.getSelectedItem()).getPeriodS();
            settings.countsPeriodS = ((CountsPeriods)countsPeriod.getSelectedItem()).getPeriodS();
            settings.detrend = detrend.isSelected();

            settings.runningMean = runningMeanButton.isSelected();
            settings.runningMeanPeriodS = Double.parseDouble(runningMeanPeriod.getText());
            settings.runningMedian = runningMedianButton.isSelected();
            settings.runningMedianPeriodS = Double.parseDouble(runningMedianPeriod.getText());
            
            settings.eventThreshold = Integer.parseInt(eventThreshold.getText());
            settings.eventRatio = Double.parseDouble(eventRatio.getText());
            settings.eventMaxLengthS = Double.parseDouble(eventMaxLength.getText());
            settings.binSize = (BinSize) binSize.getSelectedItem();

            settings.setAutoScale(autoScale.isSelected());
            if (!autoScale.isSelected()) {
                settings.scaleMax = Integer.parseInt(scaleMax.getText());
                settings.scaleMin = Integer.parseInt(scaleMin.getText());
            }
            
            if (valuesButton.isSelected())
                settings.setType(ViewType.VALUES);
            else
                settings.setType(ViewType.COUNTS);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Illegal values.", "Options Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
}
