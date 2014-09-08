package gov.usgs.swarm;

import gov.usgs.swarm.database.model.Marker;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.util.Time;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel that shows all information about a selected marker on a selected WaveView Panel
 * 
 * @author Chirag Patel
 *
 */
public class MarkerPanel extends JPanel{
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final long serialVersionUID = 1L;


	private JLabel waveFileName;
	private JLabel pointCoordinate;
	private JLabel markerType;
	private JButton deleteMarkerButton;
	private JButton particleMotionButton;
	JComboBox combo1;
	JComboBox combo3;
	
	JComboBox weightCombo;
	
	private WaveViewPanel viewPanel;
	
	
	public MarkerPanel(){
		
		this.setLayout(new BorderLayout());
		FormLayout layout = new FormLayout(
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref:grow", 
				"pref, 3dlu, pref, 3dlu,pref,3dlu,pref, 3dlu, pref, 3dlu, pref");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		
		builder.addSeparator("Marker", "1,1,7,1,FILL,FILL");
		
		waveFileName = new JLabel("  ", JLabel.LEFT);
		builder.addLabel("File Name :","1,3,1,1,FILL,FILL");
		builder.add(waveFileName,"3,3,5,1,FILL,FILL");
		
		

		pointCoordinate = new JLabel("  ", JLabel.LEFT);
		builder.addLabel("Time :","1,5,1,1,FILL,FILL");
		builder.add(pointCoordinate,"3,5,5,1,FILL,FILL");
		

		markerType = new JLabel();
		builder.addLabel("Marker Type:","1,7,1,1,FILL,FILL");
		builder.add(markerType,"3,7,1,1,FILL,FILL");
		
		
		String[] wieghtSpecifications = {"0", "1", "2","3","4"};
		weightCombo  = new JComboBox(wieghtSpecifications);
		JPanel weightPanel = new JPanel();
		weightPanel.setLayout(new BorderLayout());
		weightPanel.add(new JLabel("Weight : "),BorderLayout.WEST);
		weightPanel.add(weightCombo,BorderLayout.CENTER);
		
		
		builder.add(weightPanel,"4,7,2,1,FILL,FILL");
		
		
		
		String[] specifications = { "Up", "Down", "Unknown" };
		String[] specifications2 = { "I", "E" };
		combo1 = new JComboBox(specifications);
		combo3 = new JComboBox(specifications2);
		combo1.setSelectedIndex(0);
		combo3.setSelectedIndex(1);

		
		
		
		
		
		
		builder.addLabel("Specifications:","1,9,1,1,FILL,FILL");
		builder.add(combo1,"3,9,1,1,FILL,FILL");
		builder.add(combo3,"5,9,1,1,FILL,FILL");
		
		
		deleteMarkerButton = new JButton("Delete");
		
		particleMotionButton = new JButton("Particle Motion");
				
		builder.add(deleteMarkerButton,"3,11,1,1,FILL,FILL");
		builder.add(particleMotionButton,"5,11,1,1,FILL,FILL");
		
		this.add(builder.getPanel(), BorderLayout.CENTER);
		
		resetMarkerUIFields();
		
		deleteMarkerButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Marker marker = Marker.find(viewPanel.getSelectedMarker().getId());
				if(marker != null){
					marker.delete();
				}
				resetMarkerUIFields();
				viewPanel.createImage();
				viewPanel.setEventCalculations();

			}
		});
		
		particleMotionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Swarm
				.getApplication()
				.getWaveClipboard().plotParticleMotion(viewPanel.getChannel().stationCode);

			}
		});
		
		combo1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)){
					viewPanel.getSelectedMarker().setUpDownUnknown(combo1.getSelectedItem().toString());
				}else if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)){
					viewPanel.getSelectedMarker().setUpDownUnknown(combo1.getSelectedItem().toString());
				} 
				
				viewPanel.createImage();
				viewPanel.getSelectedMarker().persist();
			}
		});
		
		combo3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)){
					viewPanel.getSelectedMarker().setIp_ep(combo3.getSelectedItem().toString()+"P");
				}else if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)){
					viewPanel.getSelectedMarker().setIs_es(combo3.getSelectedItem().toString()+"S");
				} 
				viewPanel.createImage();
				viewPanel.getSelectedMarker().persist();
			}
		});
		
		
		weightCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				viewPanel.getSelectedMarker().setWeight(Integer.parseInt(weightCombo.getSelectedItem().toString()));
				viewPanel.getSelectedMarker().persist();
			}
		});
	}
	
	public WaveViewPanel getViewPanel(){
		return viewPanel;
	}
	
	/**
	 * Links a WaveView Panel and its selected marker to this Marker panel.
	 * This would also enable tight coupling between functions to alter UI changes of the selected marker to this class. Thus by altering
	 * properties of a selected marker on this panel, the WaveView Panel will redraw it's selected marker to reflect its changes.
	 * 
	 * @param vp : {@link WaveViewPanel} object 
	 */
	public void setViewPanel(WaveViewPanel vp) {
		viewPanel = vp;
		if (viewPanel != null){
			if(viewPanel.getSelectedMarker() != null){
				this.waveFileName.setText(viewPanel.getChannel().toString);
				this.pointCoordinate.setText(Time.format(DATE_FORMAT, viewPanel.getSelectedMarker().getMarkerTime()));
				
				if (viewPanel.getSelectedMarker().getAttempt()!= null) {
					
					if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)){
						
						if(viewPanel.getSelectedMarker().getIp_ep().equalsIgnoreCase("IP")){
							this.combo3.setSelectedItem("I");
						}else if(viewPanel.getSelectedMarker().getIp_ep().equalsIgnoreCase("EP")){
							this.combo3.setSelectedItem("E");
						}
						this.combo1.setSelectedItem(viewPanel.getSelectedMarker().getUpDownUnknown());
						if(viewPanel.getSelectedMarker().getWeight()!= null){
							weightCombo.setSelectedItem(viewPanel.getSelectedMarker().getWeight().toString());
						}else{
							viewPanel.getSelectedMarker().setWeight(Integer.parseInt(weightCombo.getSelectedItem().toString()));
							viewPanel.getSelectedMarker().persist();
						}
						combo1.setEnabled(true);
						combo3.setEnabled(true);
						weightCombo.setEnabled(true);
					} else if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)){
						
						if(viewPanel.getSelectedMarker().getIs_es().equalsIgnoreCase("IS") ){
							this.combo3.setSelectedItem("I");
						}else if(viewPanel.getSelectedMarker().getIs_es().equalsIgnoreCase("ES") ){
							this.combo3.setSelectedItem("E");
						}
						this.combo1.setSelectedItem(viewPanel.getSelectedMarker().getUpDownUnknown());
						combo1.setEnabled(true);
						combo3.setEnabled(true);
						weightCombo.setEnabled(false);
					} else{
						combo1.setEnabled(false);
						combo3.setEnabled(false);
						weightCombo.setEnabled(false);
					}
					this.markerType.setText(viewPanel.getSelectedMarker().getMarkerType());
					deleteMarkerButton.setEnabled(true);
					particleMotionButton.setEnabled(true);
				
				} 
			}
		}
		else {
			this.waveFileName.setText("");
			this.pointCoordinate.setText("");
			this.markerType.setText("");
			combo1.setEnabled(false);
			combo3.setEnabled(false);
			weightCombo.setEnabled(false);
			deleteMarkerButton.setEnabled(false);
		}
		
		
		
}
	
	/**
	 * Sets selected marker properties on this panel
	 * 
	 * 
	 */
	public void setSelectedMarkerProperties() {
		if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)){
			viewPanel.getSelectedMarker().setIp_ep(this.combo3.getSelectedItem().toString()+"P");
			viewPanel.getSelectedMarker().setUpDownUnknown(combo1.getSelectedItem().toString());
			viewPanel.getSelectedMarker().setWeight(Integer.parseInt(weightCombo.getSelectedItem().toString()));
			
		}else if(viewPanel.getSelectedMarker().getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)){
			viewPanel.getSelectedMarker().setIs_es(this.combo3.getSelectedItem().toString()+"S");
			viewPanel.getSelectedMarker().setUpDownUnknown(combo1.getSelectedItem().toString());
		} 
		viewPanel.getSelectedMarker().persist();
		viewPanel.createImage();
	}
	
	
	/**
	 * 
	 * Resets the Marker properties UI fields . this is done mainly when no marker is set for this panel.
	 */
	private void resetMarkerUIFields() {
		pointCoordinate.setText("");
		deleteMarkerButton.setEnabled(false);
		particleMotionButton.setEnabled(false);
		combo1.setEnabled(false);
		combo3.setEnabled(false);
		weightCombo.setEnabled(false);
		if(viewPanel != null){
			viewPanel.removeMarker(viewPanel.getSelectedMarker());
		}
		
		this.markerType.setText("");
	}
}
