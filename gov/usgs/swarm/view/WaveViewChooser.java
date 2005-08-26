package gov.usgs.swarm.view;


/**
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/04/11 00:26:56  cervelli
 * Removed JDK 1.5 deprecated Dialog.show().
 *
 * @author Dan Cervelli
 */
public class WaveViewChooser
{}
/*
{
	private WaveComponent waveComponent;
	private WaveModel model;
	
	private JToggleButton waveToggle;
	private JToggleButton spectraToggle;
	private JToggleButton spectrogramToggle;
	
	private ActionListener viewCallback;
	
	private Waveform waveform;
	private Spectra spectra;
	private Spectrogram spectrogram;
	
	public WaveViewChooser()
	{
	    waveform = new Waveform();
		waveComponent = waveform;
	}
	
	public void setModel(WaveModel m)
	{
		model = m;
		waveComponent.setModel(model);
	}

	public WaveComponent getWaveComponent()
	{
		return waveComponent;
	}
	
	public void setViewCallback(ActionListener al)
	{
		viewCallback = al;
	}
	
	public void addButtonsToToolbar(JComponent keyComp, JToolBar toolbar)
	{
		JButton waveSet = new JButton(new ImageIcon("images/wavesettings.png"));
		toolbar.add(waveSet);
		waveSet.setToolTipText("Wave view settings (?)");
		waveSet.setMargin(new Insets(0,0,0,0));
		waveSet.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (waveComponent instanceof Configurable)
							((Configurable)waveComponent).getConfigurationDialog().setVisible(true);
					}
				});
		
		waveToggle = new JToggleButton(new ImageIcon("images/wave.png"));
		waveToggle.setMargin(new Insets(0,0,0,0));
		toolbar.add(waveToggle);
		waveToggle.setSelected(true);
		waveToggle.setToolTipText("Wave view (W or ,)");
		waveToggle.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (!(waveComponent instanceof Waveform))
						{
						    if (waveform == null)
						        waveform = new Waveform();
							waveComponent.dispose();
							waveComponent = waveform;
							waveComponent.setModel(model);
							viewCallback.actionPerformed(new ActionEvent
									(WaveViewChooser.this, ActionEvent.ACTION_PERFORMED,
									"viewChanged"));
						}
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", waveToggle);
		Util.mapKeyStrokeToButton(keyComp, "W", "wave2", waveToggle);
		
		spectraToggle = new JToggleButton(new ImageIcon("images/spectra.png"));
		spectraToggle.setMargin(new Insets(0,0,0,0));
		toolbar.add(spectraToggle);
		spectraToggle.setToolTipText("Spectra view (S or .)");
		spectraToggle.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (!(waveComponent instanceof Spectra))
						{
						    if (spectra == null)
						        spectra = new Spectra();
							waveComponent.dispose();
							waveComponent = spectra;
							waveComponent.setModel(model);
							viewCallback.actionPerformed(new ActionEvent
									(WaveViewChooser.this, ActionEvent.ACTION_PERFORMED,
									"viewChanged"));
						}
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", spectraToggle);
		Util.mapKeyStrokeToButton(keyComp, "S", "spectra2", spectraToggle);
		
		spectrogramToggle = new JToggleButton(new ImageIcon("images/spectrogram.png"));
		spectrogramToggle.setMargin(new Insets(0,0,0,0));
		toolbar.add(spectrogramToggle);
		spectrogramToggle.setToolTipText("Spectrogram view (G or /)");
		spectrogramToggle.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if (!(waveComponent instanceof Spectrogram))
						{
						    if (spectrogram == null)
						        spectrogram = new Spectrogram();
							waveComponent.dispose();
							waveComponent = spectrogram;
							waveComponent.setModel(model);
							viewCallback.actionPerformed(new ActionEvent
									(WaveViewChooser.this, ActionEvent.ACTION_PERFORMED,
									"viewChanged"));
						}
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "SLASH", "spectrogram1", spectrogramToggle);
		Util.mapKeyStrokeToButton(keyComp, "G", "spectrogram2", spectrogramToggle);
		
		ButtonGroup waveTypes = new ButtonGroup();
		waveTypes.add(waveToggle);
		waveTypes.add(spectraToggle);
		waveTypes.add(spectrogramToggle);
	}
}
*/