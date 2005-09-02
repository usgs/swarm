package gov.usgs.swarm;

import gov.usgs.math.Butterworth.FilterType;
import gov.usgs.util.GridBagHelper;
import gov.usgs.util.ui.BaseDialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class WaveViewSettingsDialog extends BaseDialog
{
	public static final long serialVersionUID = -1;
	private static final int WIDTH = 520;
	private static final int HEIGHT = 560;	
	private WaveViewSettings settings;
	private NumberFormat numberFormat; 
	
	private JPanel dialogPanel;
	
	private JPanel wavePanel;
	private JTextField minAmp;
	private JTextField maxAmp;
	private JCheckBox autoScaleAmp;
	private JCheckBox autoScaleAmpMemory;
	private JCheckBox removeBias;

	private JCheckBox autoScalePower;
	private JCheckBox autoScalePowerMemory;
	private JTextField maxPower;

	private JPanel spectraPanel;
	private JComboBox fftSize;
	private JTextField minFreq;
	private JTextField maxFreq;
	private JCheckBox logFreq;
	private JCheckBox logPower;
	private JTextField sgramOverlap;

	private JPanel typePanel;	
	private JRadioButton waveButton;
	private JRadioButton spectraButton;
	private JRadioButton spectrogramButton;
	
	private JCheckBox filterOn;
	private JCheckBox zeroPhaseShift;
	private JRadioButton lowPass;
	private JRadioButton highPass;
	private JRadioButton bandPass;
	private JSlider order;
	private JTextField corner1;
	private JTextField corner2;
	
	private static WaveViewSettingsDialog dialog;
	
	private WaveViewSettingsDialog()
	{
		super(Swarm.getParentFrame(), "Wave View Settings", true, WIDTH, HEIGHT);
//		settings = s;
		numberFormat = new DecimalFormat("#.##");
		createSettingsUI();
	}
	
	public static WaveViewSettingsDialog getInstance(WaveViewSettings s)
	{
		if (dialog == null)
			dialog = new WaveViewSettingsDialog();

		dialog.setSettings(s);
		dialog.setToCurrent();
		return dialog;
	}

	public void setSettings(WaveViewSettings s)
	{
	    settings = s;
	}
	
	public void createSettingsUI()
	{
		dialogPanel = new JPanel(new GridBagLayout());
		
		AbstractAction enableAction = new AbstractAction()
		{
			public static final long serialVersionUID = -1;
			public void actionPerformed(ActionEvent e)
			{
				doEnables();
			}
		};
		
		typePanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		typePanel.setBorder(new TitledBorder(new EtchedBorder(), "General View Options"));
		waveButton = new JRadioButton("Wave");
		spectraButton = new JRadioButton("Spectra");
		spectrogramButton = new JRadioButton("Spectrogram");
		ButtonGroup bg = new ButtonGroup();
		removeBias = new JCheckBox("Remove bias");

		typePanel.add(waveButton, GridBagHelper.set(c, "x=0;y=0;a=w;wx=0.5;ix=2;iy=2"));
		typePanel.add(spectraButton, GridBagHelper.set(c, "x=0;y=1;a=w;wx=0.5"));
		typePanel.add(spectrogramButton, GridBagHelper.set(c, "x=0;y=2;a=w;wx=0.5"));
		typePanel.add(removeBias, GridBagHelper.set(c, "x=1;y=0;a=e;f=n;wx=0.5"));
		
		bg.add(waveButton);
		bg.add(spectraButton);
		bg.add(spectrogramButton);
		
		wavePanel = new JPanel(new GridBagLayout());
		
		wavePanel.setBorder(new TitledBorder(new EtchedBorder(), "Wave Options")); 
		autoScaleAmp = new JCheckBox("Auto-scale amplitude");
		autoScaleAmp.addActionListener(enableAction);
		autoScaleAmpMemory = new JCheckBox("Auto-scale memory");
		
		minAmp = new JTextField(5);
		maxAmp = new JTextField(5);
		JLabel minAmpLabel = new JLabel("Minimum amplitude: ");
		JLabel maxAmpLabel = new JLabel("Maximum amplitude: ");
		minAmpLabel.setLabelFor(minAmp);
		maxAmpLabel.setLabelFor(maxAmp);
		JPanel p1 = new JPanel();
		p1.add(minAmpLabel);
		p1.add(minAmp);
		JPanel p2 = new JPanel();
		p2.add(maxAmpLabel);
		p2.add(maxAmp);
		
		wavePanel.add(autoScaleAmp, GridBagHelper.set(c, "x=0;y=0;w=3;a=w;wx=1"));
		wavePanel.add(autoScaleAmpMemory, GridBagHelper.set(c, "x=0;y=1;w=3;a=w;wx=1"));
		wavePanel.add(minAmpLabel, GridBagHelper.set(c, "x=0;y=2;a=w;w=2;wx=0"));
		wavePanel.add(minAmp, GridBagHelper.set(c, "x=3;y=2;a=w;f=h;w=1;wx=1"));
		wavePanel.add(maxAmpLabel, GridBagHelper.set(c, "x=0;y=3;a=nw;w=2;wx=0;wy=1"));
		wavePanel.add(maxAmp, GridBagHelper.set(c, "x=3;y=3;a=nw;f=h;w=1;wx=1;wy=1"));
		
		spectraPanel = new JPanel(new GridBagLayout());
		spectraPanel.setBorder(new TitledBorder(new EtchedBorder(), "Spectra/Spectrogram Options"));
		fftSize = new JComboBox(new String[] {"Auto", "64", "128", "256", "512", "1024", "2048"});
		
		JLabel fftLabel = new JLabel("FFT bin (samples): ");
		fftLabel.setLabelFor(fftSize);
		JPanel p3 = new JPanel();
		p3.add(fftLabel);
		p3.add(fftSize);
		
		minFreq = new JTextField(5);
		JLabel minFreqLabel = new JLabel("Minimum frequency: ");
		maxFreq = new JTextField(5);
		JLabel maxFreqLabel = new JLabel("Maximum frequency: ");
		minFreqLabel.setLabelFor(minFreq);
		maxFreqLabel.setLabelFor(maxFreq);
		minFreqLabel.setLabelFor(minFreq);
		maxFreqLabel.setLabelFor(maxFreq);
		JPanel p4 = new JPanel();
		p4.add(minFreqLabel);
		p4.add(minFreq);
		JPanel p5 = new JPanel();
		p5.add(maxFreqLabel);
		p5.add(maxFreq);
		logPower = new JCheckBox("Log of power");
		logFreq = new JCheckBox("Log of frequency");
		JLabel sgramOverlapLabel = new JLabel("Spectrogram Overlap (%): ");
		sgramOverlap = new JTextField(5);
		sgramOverlapLabel.setLabelFor(sgramOverlap);
		
		autoScalePower = new JCheckBox("Auto-scale power");
		autoScalePower.addActionListener(enableAction);
		autoScalePowerMemory = new JCheckBox("Auto-scale power memory");
		
		JLabel maxPowerLabel = new JLabel("Maximum power: ");
		maxPower = new JTextField(5);
		
		spectraPanel.add(autoScalePower, GridBagHelper.set(c, "x=0;y=0;w=3;a=w;wx=1"));
		spectraPanel.add(autoScalePowerMemory, GridBagHelper.set(c, "x=0;y=1;w=3;a=w;wx=1"));
		spectraPanel.add(maxPowerLabel, GridBagHelper.set(c, "x=0;y=2;w=2;a=w;wx=1;f=n"));
		spectraPanel.add(maxPower, GridBagHelper.set(c, "x=2;y=2;w=1;a=e;wx=0"));
		spectraPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=3;w=4;a=c;wx=1;f=h"));
		spectraPanel.add(minFreqLabel, GridBagHelper.set(c, "x=0;y=4;w=2;a=w;wx=1;f=n"));
		spectraPanel.add(minFreq, GridBagHelper.set(c, "x=2;y=4;w=1;a=e;wx=0"));
		spectraPanel.add(maxFreqLabel, GridBagHelper.set(c, "x=0;y=5;w=2;a=w;wx=1"));
		spectraPanel.add(maxFreq, GridBagHelper.set(c, "x=2;y=5;w=1;a=e;wx=0"));
		spectraPanel.add(logPower, GridBagHelper.set(c, "x=0;y=6;w=3;a=w;wx=1"));
		spectraPanel.add(logFreq, GridBagHelper.set(c, "x=0;y=7;w=3;a=w;wx=1"));
		spectraPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=8;w=4;a=c;wx=1;f=h"));
		spectraPanel.add(fftLabel, GridBagHelper.set(c, "x=0;y=9;w=2;a=w;wx=1"));
		spectraPanel.add(fftSize, GridBagHelper.set(c, "x=2;y=9;w=1;a=e;wx=0;f=h"));
		spectraPanel.add(sgramOverlapLabel, GridBagHelper.set(c, "x=0;y=10;w=2;a=nw;wx=1;f=n;wy=1"));
		spectraPanel.add(sgramOverlap, GridBagHelper.set(c, "x=2;y=10;w=1;a=ne;wx=0;wy=1"));
		
		JPanel filterPanel = new JPanel(new GridBagLayout());
		filterPanel.setBorder(new TitledBorder(new EtchedBorder(), "Butterworth Filter")); 
		order = new JSlider(2, 8, 4);
		order.setMajorTickSpacing(2);
		order.setSnapToTicks(true);
		order.createStandardLabels(2);
		order.setPaintLabels(true);
		order.setPaintTicks(true);
		JLabel orderLabel = new JLabel("Order");
		
		zeroPhaseShift = new JCheckBox("Zero phase shift");
		
		corner1 = new JTextField(5);
		corner2 = new JTextField(5);
		JLabel c1Label = new JLabel("Minimum hz:");
		JLabel c2Label = new JLabel("Maximum hz:");
		c1Label.setLabelFor(corner1);
		c2Label.setLabelFor(corner2);
		
//		JLabel filterType = new JLabel("Type");
		lowPass = new JRadioButton("Low pass");
		highPass = new JRadioButton("High pass");
		bandPass = new JRadioButton("Band pass");
		ButtonGroup filterTypeGroup = new ButtonGroup();
		filterTypeGroup.add(lowPass);
		filterTypeGroup.add(highPass);
		filterTypeGroup.add(bandPass);
		lowPass.addActionListener(enableAction);
		highPass.addActionListener(enableAction);
		bandPass.addActionListener(enableAction);
		
		JPanel ftPanel = new JPanel(new GridLayout(3, 1));
		ftPanel.setBorder(new TitledBorder(new EtchedBorder(), "Type")); 
		ftPanel.add(lowPass);
		ftPanel.add(highPass);
		ftPanel.add(bandPass);
		filterOn = new JCheckBox("Enabled");
		
		JLabel zeroLabel = new JLabel("<html>Runs filter forward<br>and backward.<br>This effectively doubles<br>filter order.</html>");
		
		filterPanel.add(filterOn, GridBagHelper.set(c, "x=0;y=0;w=4;h=1;a=nw;wx=0.2;ix=2;i=2,0,0,0"));
		filterPanel.add(ftPanel, GridBagHelper.set(c, "x=0;y=1;w=1;h=4;a=nw;wx=0;f=v"));
		filterPanel.add(zeroPhaseShift, GridBagHelper.set(c, "x=1;y=0;w=1;h=1;a=w;wx=0.3;f=n"));
		filterPanel.add(zeroLabel, GridBagHelper.set(c, "x=1;y=1;w=1;h=4;a=nw;wx=0.3;f=n;i=0,5,0,0"));
		
		filterPanel.add(orderLabel, GridBagHelper.set(c, "x=2;y=0;w=3;h=1;a=c;wx=5;f=n;i=2,0,0,0"));
		filterPanel.add(order, GridBagHelper.set(c, "x=2;y=1;w=3;a=w;wx=0"));
		filterPanel.add(c1Label, GridBagHelper.set(c, "x=2;y=2;w=2;a=w;wx=0"));
		filterPanel.add(corner1, GridBagHelper.set(c, "x=4;y=2;w=1;a=e;wx=0"));
		filterPanel.add(c2Label, GridBagHelper.set(c, "x=2;y=3;w=2;a=w;wx=0"));
		filterPanel.add(corner2, GridBagHelper.set(c, "x=4;y=3;w=1;a=e;wx=0"));
		
		dialogPanel.add(typePanel, GridBagHelper.set(c, "x=0;y=0;w=1;h=1;f=b;a=w;wx=0"));
		dialogPanel.add(wavePanel, GridBagHelper.set(c, "x=0;y=1;w=1;h=1;f=b;a=w;wx=0"));
		dialogPanel.add(spectraPanel, GridBagHelper.set(c, "x=1;y=0;w=1;h=2;f=b;a=e;wx=1"));
		dialogPanel.add(filterPanel, GridBagHelper.set(c, "x=0;y=2;w=2;h=1;f=b;a=w;wx=0"));
		
//		setToCurrent();
		doEnables();
		mainPanel.add(dialogPanel, BorderLayout.CENTER);
	}
	
	private void doEnables()
	{
		if (lowPass.isSelected())
		{
			corner1.setEnabled(false);
			corner2.setEnabled(true);
		}
		else if (highPass.isSelected())
		{
			corner1.setEnabled(true);
			corner2.setEnabled(false);
		}
		else if (bandPass.isSelected())
		{
			corner1.setEnabled(true);
			corner2.setEnabled(true);
		}
		
		minAmp.setEnabled(!autoScaleAmp.isSelected());
		maxAmp.setEnabled(!autoScaleAmp.isSelected());
		
		maxPower.setEnabled(!autoScalePower.isSelected());
	}
	
	public void setToCurrent()
	{
		if (settings.type == WaveViewSettings.WAVE)
			waveButton.setSelected(true);
		else if (settings.type == WaveViewSettings.SPECTRA)
			spectraButton.setSelected(true);
		else if (settings.type == WaveViewSettings.SPECTROGRAM)
			spectrogramButton.setSelected(true);
		
		removeBias.setSelected(settings.removeBias);
		autoScaleAmp.setSelected(settings.autoScaleAmp);
		autoScaleAmpMemory.setSelected(settings.autoScaleAmpMemory);
		
		autoScalePower.setSelected(settings.autoScalePower);
		autoScalePowerMemory.setSelected(settings.autoScalePowerMemory);
		
		minAmp.setText(numberFormat.format(settings.minAmp));
		maxAmp.setText(numberFormat.format(settings.maxAmp));
		maxPower.setText(numberFormat.format(settings.maxPower));

		fftSize.setSelectedItem(settings.fftSize);
		minFreq.setText(numberFormat.format(settings.minFreq));
		maxFreq.setText(numberFormat.format(settings.maxFreq));
		logFreq.setSelected(settings.logFreq);
		logPower.setSelected(settings.logPower);
		sgramOverlap.setText(numberFormat.format(settings.spectrogramOverlap * 100));
		filterOn.setSelected(settings.filterOn);
		
		switch (settings.filter.getType())
		{
			case LOWPASS:
				lowPass.setSelected(true);
				corner1.setText("0.0");
				corner2.setText(numberFormat.format(settings.filter.getCorner1()));
				break;
			case HIGHPASS:
				highPass.setSelected(true);
				corner1.setText(numberFormat.format(settings.filter.getCorner1()));
				corner2.setText("0.0");
				break;
			case BANDPASS:
				bandPass.setSelected(true);
				corner1.setText(numberFormat.format(settings.filter.getCorner1()));
				corner2.setText(numberFormat.format(settings.filter.getCorner2()));
				break;
		}
		order.setValue(settings.filter.getOrder());
		zeroPhaseShift.setSelected(settings.zeroPhaseShift);
	}

	public boolean allowOK()
	{
		String message = null;
		try
		{
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
			
			message = "Error in maximum power.";
			double maxp = Double.parseDouble(maxPower.getText());
			message = "Maximum power must be above 0.";
			if (maxp < 0)
				throw new NumberFormatException();
			
			message = "Error in spectrogram overlap format.";
			double so = Double.parseDouble(sgramOverlap.getText());
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
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
	
	public void wasOK()
	{
		try
		{
			if (waveButton.isSelected())
				settings.type = WaveViewSettings.WAVE;
			else if (spectraButton.isSelected())
				settings.type = WaveViewSettings.SPECTRA;
			else if (spectrogramButton.isSelected())
				settings.type = WaveViewSettings.SPECTROGRAM;
				
			settings.removeBias = removeBias.isSelected();
			settings.autoScaleAmp = autoScaleAmp.isSelected();
			settings.autoScaleAmpMemory = autoScaleAmpMemory.isSelected();
			
			settings.autoScalePower = autoScalePower.isSelected();
			settings.autoScalePowerMemory = autoScalePowerMemory.isSelected();
			settings.maxPower = Double.parseDouble(maxPower.getText());
			
			settings.minAmp = Double.parseDouble(minAmp.getText());
			settings.maxAmp = Double.parseDouble(maxAmp.getText());
			
			settings.maxFreq = Double.parseDouble(maxFreq.getText());
			settings.minFreq = Double.parseDouble(minFreq.getText());
			if (settings.minFreq < 0)
				settings.minFreq = 0;
			
			settings.fftSize = (String)fftSize.getSelectedItem();
			settings.logFreq = logFreq.isSelected();
			settings.logPower = logPower.isSelected();
			settings.spectrogramOverlap = Double.parseDouble(sgramOverlap.getText()) / 100;
			if (settings.spectrogramOverlap > 0.95 || settings.spectrogramOverlap < 0)
				settings.spectrogramOverlap = 0;
			settings.notifyView();

			settings.filterOn = filterOn.isSelected();
			settings.zeroPhaseShift = zeroPhaseShift.isSelected();
			
			FilterType ft = null;
			double c1 = 0;
			double c2 = 0;
			if (lowPass.isSelected())
			{
				ft = FilterType.LOWPASS;
				c1 = Double.parseDouble(corner2.getText());
				c2 = 0;
			}
			else if (highPass.isSelected())
			{
				ft = FilterType.HIGHPASS;
				c1 = Double.parseDouble(corner1.getText());
				c2 = 0;
			}
			else if (bandPass.isSelected())
			{
				ft = FilterType.BANDPASS;
				c1 = Double.parseDouble(corner1.getText());
				c2 = Double.parseDouble(corner2.getText());
			}
			settings.filter.set(ft, order.getValue(), 100, c1, c2);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Illegal values.", "Options Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
}