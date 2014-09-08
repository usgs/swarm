package gov.usgs.swarm;

import gov.usgs.swarm.database.model.Attempt;
import gov.usgs.swarm.database.model.Event;
import gov.usgs.swarm.database.model.Marker;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel that shows  Event label and type
 * 
 * @author Chirag Patel
 *
 */
public class EventPanel extends JPanel{

	private JTextField event;
	private JTextField eventType;
	private JButton saveEventButton;
	private JButton duplicateAttemptButton;
	
	public EventPanel(){
		this.setLayout(new BorderLayout());
		
		FormLayout layout = new FormLayout(
				"pref, 3dlu, right:80dlu, 3dlu, pref:grow", 
				"pref, 3dlu, pref, 3dlu,pref,3dlu,pref,3dlu,pref");
		
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		builder.addSeparator("Event", "1,1,5,1,FILL,FILL");
		
		eventType = new JTextField();
		builder.addLabel("Event Type :","1,3,1,1,FILL,FILL");
		builder.add(eventType,"3,3,1,1,FILL,FILL");

		
		
		event = new JTextField();
		builder.addLabel("Event :","1,5,1,1,FILL,FILL");
		builder.add(event,"3,5,1,1,FILL,FILL");
		
		
		
		saveEventButton = new JButton("Update Event Label & Type");
		builder.add(saveEventButton,"1,7,3,1");
		
		duplicateAttemptButton = new JButton("Duplicate Attempt");
		builder.add(duplicateAttemptButton,"1,9,3,1");
		
		
		this.add(builder.getPanel(), BorderLayout.CENTER);
		saveEventButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if(Swarm.getSelectedEvent() == null){
					Event e = new Event();
					Swarm.setSelectedEvent(e);
				}
				
				Swarm.getSelectedEvent().setEventType(eventType.getText());
				Swarm.getSelectedEvent().setEventLabel(event.getText());
				Swarm.getSelectedEvent().persist();
				
				if(Swarm.getSelectedAttempt() == null){
					Attempt a = new Attempt();
					Swarm.setSelectedAttempt(a);
					Swarm.getSelectedAttempt().setEvent(Swarm.getSelectedEvent().getId());
				}
				Swarm.getSelectedAttempt().persist();
				SwarmMenu.eventPropertiesDialog.enableHypoCalculation();
				Swarm.getApplication().getWaveClipboard().enableMarkerGeneration();
				saveEventButton.setEnabled(false);
			}
		});
		

		duplicateAttemptButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				Attempt newAttempt = new Attempt();
				newAttempt.setEvent(Swarm.getSelectedEvent().getId());
				newAttempt.persist();
				
				List<Marker> markers = Marker.listByAttempt(Swarm.getSelectedAttempt().getId());
				for(Marker m : markers){
					Marker marker = new Marker();
					marker.setFileIndex(m.getFileIndex());
					marker.setFileName(m.getFileName());
					marker.setFilePath(m.getFilePath());
					marker.setIp_ep(m.getIp_ep());
					marker.setIs_es(m.getIs_es());
					marker.setMarkerTime(m.getMarkerTime());
					marker.setMarkerType(m.getMarkerType());
					marker.setUpDownUnknown(m.getUpDownUnknown());
					marker.setAttempt(newAttempt.getId());
					marker.persist();
					Swarm.getApplication().getWaveClipboard().clearAndLoadMarkerOnWave(marker);
				}
				
				Swarm.setSelectedAttempt(newAttempt);
				
				
			}
			
		});
		
		
		event.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(Swarm.getSelectedEvent() != null){
					if((eventType.getText().equalsIgnoreCase(Swarm.getSelectedEvent().getEventType()))
							&& (event.getText().equalsIgnoreCase(Swarm.getSelectedEvent().getEventLabel()))){
						saveEventButton.setEnabled(false);
					}else{
						saveEventButton.setEnabled(true);
					}
				}
			}

		});
		
		
		
		eventType.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(Swarm.getSelectedEvent() != null){
					if((eventType.getText().equalsIgnoreCase(Swarm.getSelectedEvent().getEventType()))
							&& (event.getText().equalsIgnoreCase(Swarm.getSelectedEvent().getEventLabel()))){
						saveEventButton.setEnabled(false);
					}else{
						saveEventButton.setEnabled(true);
					}
				}
			}

		});
		
	}
	
	/**
	 * Set the selected Event properties on the UI fields if an event as been set in session
	 * else event UI fields would be cleared, depicting that no event is in session.
	 * 
	 */
	public void intialiseEventFields(){
		if(Swarm.getSelectedEvent() != null){
			eventType.setText(Swarm.getSelectedEvent().getEventType());
			event.setText(Swarm.getSelectedEvent().getEventLabel());
			saveEventButton.setEnabled(false);
		}else{
			eventType.setText("");
			event.setText("");
			saveEventButton.setEnabled(true);
		}
		
		if(Swarm.getSelectedAttempt() == null){
			duplicateAttemptButton.setEnabled(false);
		}else{
			duplicateAttemptButton.setEnabled(true);
		}
	}
	
	
		
}
