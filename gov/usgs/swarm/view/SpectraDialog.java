package gov.usgs.swarm.view;


/**
 * @author cervelli
 */
public class SpectraDialog
{}
/*
extends BaseDialog 
{
	private static final int WIDTH = 250;
	private static final int HEIGHT = 270;	
	private JPanel spectraPanel;
	private JCheckBox autoScalePower;
	private JCheckBox autoScalePowerMemory;
	private JTextField maxPower;
	private JTextField minFreq;
	private JTextField maxFreq;
	private JCheckBox logFreq;
	private JCheckBox logPower;
	private DecimalFormat numberFormat = new DecimalFormat("#.##");
	private Spectra spectra;

	public SpectraDialog(Spectra s)
	{
		super(Swarm.getParentFrame(), "Spectra View Options", true, WIDTH, HEIGHT);
		spectra = s;
		createOptionsUI();
	}
	
	public void createOptionsUI()
	{
		spectraPanel = new JPanel(new GridBagLayout());
		spectraPanel.setBorder(new TitledBorder(new EtchedBorder(), "Spectra/Spectrogram Options"));
		
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
		
		autoScalePower = new JCheckBox("Auto-scale power");
//		autoScalePower.addActionListener(enableAction);
		autoScalePowerMemory = new JCheckBox("Auto-scale power memory");
		
		JLabel maxPowerLabel = new JLabel("Maximum power: ");
		maxPower = new JTextField(5);
		
		GridBagConstraints c = new GridBagConstraints();
		
		spectraPanel.add(logPower, GridBagHelper.set(c, "x=0;y=0;w=3;a=w;wx=1"));
		spectraPanel.add(logFreq, GridBagHelper.set(c, "x=0;y=1;w=3;a=w;wx=1"));
		spectraPanel.add(autoScalePower, GridBagHelper.set(c, "x=0;y=2;w=3;a=w;wx=1"));
		spectraPanel.add(autoScalePowerMemory, GridBagHelper.set(c, "x=0;y=3;w=3;a=w;wx=1"));
		spectraPanel.add(maxPowerLabel, GridBagHelper.set(c, "x=0;y=4;w=2;a=w;wx=1;f=n"));
		spectraPanel.add(maxPower, GridBagHelper.set(c, "x=2;y=4;w=1;a=e;wx=0"));
		spectraPanel.add(minFreqLabel, GridBagHelper.set(c, "x=0;y=5;w=2;a=w;wx=1;f=n"));
		spectraPanel.add(minFreq, GridBagHelper.set(c, "x=2;y=5;w=1;a=e;wx=0"));
		spectraPanel.add(maxFreqLabel, GridBagHelper.set(c, "x=0;y=6;w=2;a=w;wx=1"));
		spectraPanel.add(maxFreq, GridBagHelper.set(c, "x=2;y=6;w=1;a=e;wx=0"));
		
		setToCurrent();
		mainPanel.add(spectraPanel, BorderLayout.CENTER);
	}

	public void setToCurrent()
	{
		autoScalePower.setSelected(spectra.isAutoScale());
		autoScalePowerMemory.setSelected(spectra.isAutoScaleMemory());
		
		maxPower.setText(numberFormat.format(spectra.getMaxPower()));
		minFreq.setText(numberFormat.format(spectra.getMinFreq()));
		maxFreq.setText(numberFormat.format(spectra.getMaxFreq()));
		 
		logPower.setSelected(spectra.isLogPower());
		logFreq.setSelected(spectra.isLogFreq());
	}
	
	public boolean allowOK()
	{
	    String message = null;
		try
		{
				
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
	    spectra.setAutoScale(autoScalePower.isSelected());
	    spectra.setAutoScaleMemory(autoScalePowerMemory.isSelected());
	    spectra.setMaxFreq(Double.parseDouble(maxFreq.getText()));
		spectra.setMinFreq(Double.parseDouble(minFreq.getText()));
		spectra.setMaxPower(Double.parseDouble(maxPower.getText()));
		
		spectra.setLogFreq(logFreq.isSelected());
		spectra.setLogPower(logPower.isSelected());
	}
}

*/