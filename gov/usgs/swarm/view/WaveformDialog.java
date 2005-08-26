package gov.usgs.swarm.view;


/**
 * @author Dan Cervelli
 */
public class WaveformDialog 
{}
/*
extends BaseDialog 
{
	private static final int WIDTH = 250;
	private static final int HEIGHT = 220;	

	private JPanel wavePanel;
	private JTextField minAmp;
	private JTextField maxAmp;
	private JCheckBox autoScale;
	private JCheckBox autoScaleMemory;
	private JCheckBox removeBias;
	
	private Waveform waveform;
	
	private DecimalFormat numberFormat = new DecimalFormat("#.##");
	
	public WaveformDialog(Waveform w)
	{
		super(Swarm.getParentFrame(), "Waveform View Options", true, WIDTH, HEIGHT);
		waveform = w;
		createOptionsUI();
	}
	
	protected void createOptionsUI()
	{
		GridBagConstraints c = new GridBagConstraints();
		wavePanel = new JPanel(new GridBagLayout());
		
		wavePanel.setBorder(new TitledBorder(new EtchedBorder(), "Wave Options")); 
		autoScale = new JCheckBox("Auto-scale amplitude");
		//autoScaleAmp.addActionListener(enableAction);
		autoScaleMemory = new JCheckBox("Auto-scale memory");
		removeBias = new JCheckBox("Remove bias");
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
		
		wavePanel.add(removeBias, GridBagHelper.set(c, "x=0;y=0;w=3;a=w;wx=1"));
		wavePanel.add(autoScale, GridBagHelper.set(c, "x=0;y=1;w=3;a=w;wx=1"));
		wavePanel.add(autoScaleMemory, GridBagHelper.set(c, "x=0;y=2;w=3;a=w;wx=1"));
		wavePanel.add(minAmpLabel, GridBagHelper.set(c, "x=0;y=3;a=w;w=2;wx=1"));
		wavePanel.add(minAmp, GridBagHelper.set(c, "x=3;y=3;a=e;w=1;wx=0"));
		wavePanel.add(maxAmpLabel, GridBagHelper.set(c, "x=0;y=4;a=nw;w=2;wx=1;wy=1"));
		wavePanel.add(maxAmp, GridBagHelper.set(c, "x=3;y=4;a=ne;w=1;wx=0;wy=1"));
		
		setToCurrent();
		mainPanel.add(wavePanel,BorderLayout.CENTER);
	}
	
	protected void setToCurrent()
	{
		removeBias.setSelected(waveform.isRemoveBias());
		autoScale.setSelected(waveform.isAutoScale());
		autoScaleMemory.setSelected(waveform.isAutoScaleMemory());
		
		minAmp.setText(numberFormat.format(waveform.getMinY()));
		maxAmp.setText(numberFormat.format(waveform.getMaxY()));
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
				
			return true;
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, message, "Options Error",
					JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
	
	public void wasOK()
	{
		try
		{
			waveform.setRemoveBias(removeBias.isSelected());
			waveform.setAutoScale(autoScale.isSelected());
			waveform.setAutoScaleMemory(autoScaleMemory.isSelected());
			
			waveform.setMinY(Double.parseDouble(minAmp.getText()));
			waveform.setMaxY(Double.parseDouble(maxAmp.getText()));
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Illegal values.", "Options Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
*/