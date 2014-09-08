package gov.usgs.swarm;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;


/**
 * Panel that displays all calculations(Coda, Azimuth, Duration magnitude) required in an attempt to find an event
 * 
 * @author Chirag Patel
 *
 */
public class EventCalculationPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private JLabel pToSTimeValue;
    private JLabel pToSdistanceValue;
    private JLabel codaValue;
    private JLabel durationMagnitudeValue;
    private JLabel azimuthValue;
    private JLabel stationValue;


    public EventCalculationPanel() {

        this.setLayout(new BorderLayout());
        FormLayout layout =
                new FormLayout("pref, 3dlu, pref:grow",
                        "pref, 3dlu, pref, 3dlu,pref,3dlu,pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("Calculations", "1,1,3,1,FILL,FILL");

        stationValue = new JLabel();

        builder.addLabel("Station :", "1,3,1,1,FILL,FILL");
        builder.add(stationValue, "3,3,1,1,FILL,FILL");


        pToSTimeValue = new JLabel();
        builder.addLabel("S - P Time :", "1,5,1,1,FILL,FILL");
        builder.add(pToSTimeValue, "3,5,1,1,FILL,FILL");

        pToSdistanceValue = new JLabel();
        builder.addLabel("S - P Distance :", "1,7,1,1,FILL,FILL");
        builder.add(pToSdistanceValue, "3,7,1,1,FILL,FILL");


        codaValue = new JLabel();
        builder.addLabel("Coda :", "1,9,1,1,FILL,FILL");
        builder.add(codaValue, "3,9,1,1,FILL,FILL");

        durationMagnitudeValue = new JLabel();
        builder.addLabel("Duration Magnitude :", "1,11,1,1,FILL,FILL");
        builder.add(durationMagnitudeValue, "3,11,1,1,FILL,FILL");

        azimuthValue = new JLabel();
        builder.addLabel("Azimuth :", "1,13,1,1,FILL,FILL");
        builder.add(azimuthValue, "3,13,1,1,FILL,FILL");


        this.add(builder.getPanel(), BorderLayout.CENTER);
    }


    /**
     * Clears all the UI fields on this Panel
     * 
     */
    public void clearUIFields() {
        stationValue.setText("");
        pToSTimeValue.setText("");
        pToSdistanceValue.setText("");
        codaValue.setText("");
        durationMagnitudeValue.setText("");
        azimuthValue.setText("");
    }


    /**
     * Sets the time difference between the P and S marker
     * 
     * @param value
     *            : Time value of p to s markers
     */
    public void setPToSTimeValue(String value) {
        pToSTimeValue.setText(value);
    }


    /**
     * Sets the distance between the P and S marker
     * 
     * @param value
     *            : Distance value of p to s markers
     */
    public void setPToSDistanceValue(String value) {
        pToSdistanceValue.setText(value);
    }


    /**
     * Sets the Coda
     * 
     * @param value
     *            : Coda
     */
    public void setCodaValue(String value) {
        codaValue.setText(value);
    }


    /**
     * Sets the Duration magnitude that was calculated
     * 
     * @param value
     *            : Duration magnitude
     */
    public void setDurationMagnitudeValue(String value) {
        durationMagnitudeValue.setText(value);
    }


    /**
     * Sets the Azimuth value
     * 
     * @param value
     *            : Azimuth
     */
    public void setAzimuthValue(String value) {
        azimuthValue.setText(value);
    }


    /**
     * Sets the associated station
     * 
     * @param value
     *            : station code
     */
    public void setStationValue(String value) {
        stationValue.setText(value);
    }

}
