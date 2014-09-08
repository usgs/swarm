package gov.usgs.swarm;



import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
/**
 * 
 * This Window holds all the Panels({@link HypoPanel}, {@link EventPanel}, {@link MarkerPanel} and {@link EventCalculationPanel} related to showing the selected event properties
 * 
 * @author Chirag Patel
 *
 */
@SuppressWarnings("serial")
public class DataRecord extends JFrame
{
	
	private MarkerPanel markerPanel;
	
	
	private EventPanel eventPanel;
	
	
	private EventCalculationPanel eventCalculationPanel;
	
	private HypoPanel hypoPanel;
	
	
	public DataRecord() {
		super();
		setAlwaysOnTop(false);

		setTitle("Event Properties");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel mainPanel = new JPanel(new BorderLayout());
		this.setContentPane(mainPanel);
		FormLayout layout = new FormLayout(
				"pref:grow", "pref, 2dlu, pref, 2dlu,pref, 2dlu, pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		eventPanel = new EventPanel();
		markerPanel = new MarkerPanel();
		eventCalculationPanel = new EventCalculationPanel();
		hypoPanel = new HypoPanel();
		
		builder.add(eventPanel,"1,1,1,1,FILL,FILL");
		builder.add(markerPanel,"1,3,1,1,FILL,FILL");
		builder.add(eventCalculationPanel,"1,5,1,1,FILL,FILL");
		builder.add(hypoPanel,"1,7,1,1,FILL,FILL");
		
		mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
		
		/*Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth();
		double height = screenSize.getHeight();*/
		
		//setSize((int)width, (int)height);
		pack();


		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				SwarmMenu.setDataRecordState(false);
				Swarm.getApplication().getWaveClipboard().disableMarkerGeneration();
			}
		});
		
		//setSize(360, 690);
	}
	
	public MarkerPanel getMarkerPanel() {
		return markerPanel;
	}

	public EventPanel getEventPanel() {
		return eventPanel;
	}
	
	public EventCalculationPanel getEventCalculationPanel(){
		return eventCalculationPanel;
	}

	
	public void enableHypoCalculation(){
		hypoPanel.enableFields();
	}
	
	public void disableHypoCalculation(){
		hypoPanel.enableFields();
	}

	@SuppressWarnings("deprecation")
	public void show(){
		super.show();
		doInitialise();
	}
	
	/**
	 * This initializes all event fields and hypo calculation fields for the selected Event in session
	 */
	public void doInitialise(){
		eventPanel.intialiseEventFields();
		if(Swarm.getSelectedAttempt()!= null){
			SwarmMenu.eventPropertiesDialog.enableHypoCalculation();
		}
	}
	
	
	/**
	 * This sets the EventPanel UI fields with the selected Event in session
	 */
	public void setEventFields(){
		eventPanel.intialiseEventFields();
	}
}
