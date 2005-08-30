package gov.usgs.swarm;

import gov.usgs.util.Util;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2005/08/27 00:34:46  tparker
 * Tidy code, no functional changes.
 *
 * Revision 1.2  2005/08/26 23:26:33  tparker
 * Create image path constants
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * @author Dan Cervelli
 */
public class WaveViewSettingsToolbar
{
	private JToggleButton waveToggle;
	private JToggleButton spectraToggle;
	private JToggleButton spectrogramToggle;
	
	private WaveViewSettings settings;
	
	
	public WaveViewSettingsToolbar(WaveViewSettings s, JToolBar dest, JComponent keyComp)
	{
		settings = s;
		createUI(dest, keyComp);
		settings.toolbar = this;
	}
	
	public void createUI(JToolBar dest, JComponent keyComp)
	{
		JButton waveSet = new JButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("wavesettings"))));
		dest.add(waveSet);
		waveSet.setToolTipText("Wave view settings (?)");
		waveSet.setMargin(new Insets(0,0,0,0));
		waveSet.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
					    WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(settings);
//						WaveViewSettingsDialog wvsd = new WaveViewSettingsDialog(settings);
						wvsd.setVisible(true);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "shift SLASH", "settings", waveSet);
		
		waveToggle = new JToggleButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("wave"))));
		waveToggle.setMargin(new Insets(0,0,0,0));
		dest.add(waveToggle);
		waveToggle.setSelected(true);
		waveToggle.setToolTipText("Wave view (W or ,)");
		waveToggle.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						settings.setType(WaveViewSettings.WAVE);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "COMMA", "wave1", waveToggle);
		Util.mapKeyStrokeToButton(keyComp, "W", "wave2", waveToggle);
		
		spectraToggle = new JToggleButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("spectra"))));
		spectraToggle.setMargin(new Insets(0,0,0,0));
		dest.add(spectraToggle);
		spectraToggle.setToolTipText("Spectra view (S or .)");
		spectraToggle.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						settings.setType(WaveViewSettings.SPECTRA);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "PERIOD", "spectra1", spectraToggle);
		Util.mapKeyStrokeToButton(keyComp, "S", "spectra2", spectraToggle);
		
		spectrogramToggle = new JToggleButton(new ImageIcon(getClass().getClassLoader().getResource(Images.get("spectrogram"))));
		spectrogramToggle.setMargin(new Insets(0,0,0,0));
		dest.add(spectrogramToggle);
		spectrogramToggle.setToolTipText("Spectrogram view (G or /)");
		spectrogramToggle.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						settings.setType(WaveViewSettings.SPECTROGRAM);
					}
				});
		Util.mapKeyStrokeToButton(keyComp, "SLASH", "spectrogram1", spectrogramToggle);
		Util.mapKeyStrokeToButton(keyComp, "G", "spectrogram2", spectrogramToggle);
		
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("L"), "cycleLogSettings");
		keyComp.getActionMap().put("cycleLogSettings", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						settings.cycleLogSettings();
					}	
				});
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F"), "toggleFilter");
		keyComp.getActionMap().put("toggleFilter", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						settings.toggleFilter();
					}	
				});				
				
		keyComp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("R"), "resetAutoScale");
		keyComp.getActionMap().put("resetAutoScale", new AbstractAction()
				{
					public static final long serialVersionUID = -1;
					public void actionPerformed(ActionEvent e)
					{
						settings.resetAutoScaleMemory();
					}	
				});
				
		ButtonGroup waveTypes = new ButtonGroup();
		waveTypes.add(waveToggle);
		waveTypes.add(spectraToggle);
		waveTypes.add(spectrogramToggle);
	}
	
	public void settingsChanged()
	{
		waveToggle.setSelected(settings.type == WaveViewSettings.WAVE);	
		spectraToggle.setSelected(settings.type == WaveViewSettings.SPECTRA);
		spectrogramToggle.setSelected(settings.type == WaveViewSettings.SPECTROGRAM);
	}
}