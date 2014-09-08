package gov.usgs.swarm.wave;

import gov.usgs.swarm.wave.WaveClipboardFrame.WavePlotInfo;
import gov.usgs.util.Pair;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JFrame;


/**
 * A window that holds the three particle motion plots
 * 
 * @author Jamil Shehzad
 * @author Joel Shellman
 */
@SuppressWarnings("serial")
public class ParticleMotionFrame extends JFrame {

    private ParticleMotionViewPanel component1;
    private ParticleMotionViewPanel component2;
    private ParticleMotionViewPanel component3;

    /**
     * @param plots data ordered N, E, Z
     */
    public ParticleMotionFrame(WavePlotInfo[] plots) {
        super();

        // Plots should always have Z on y-axis if Z is involved and
        // Should have N on y-axis when plotting N vs E
        component1 = new ParticleMotionViewPanel(plots[1], plots[0]);
        component2 = new ParticleMotionViewPanel(plots[0], plots[2]);
        component3 = new ParticleMotionViewPanel(plots[1], plots[2]);

        this.setTitle("Particle Motion Plot");
        GridLayout gr = new GridLayout(1, 3);
        gr.setHgap(2);
        gr.setHgap(2);
        this.getContentPane().setLayout(gr);
        this.add(component1);
        this.add(component2);
        this.add(component3);
        this.setSize(756, 306);
        this.setResizable(false);
    }

    public static Pair<Double, Double> extent(double[] dataN) {
        double min = dataN[0];
        double max = dataN[0];
        for (int i = 0; i < dataN.length; i++) {
            double d = dataN[i];
            if (d > max) {
                max = d;
            } else if (d < min) {
                min = d;
            }
        }
        return new Pair<Double, Double>(min, max);
    }

    public static Pair<Double, Double> extent(Pair<Double, Double> extent1, Pair<Double, Double> extent2) {
        return new Pair<Double, Double>(Math.min(extent1.item1, extent2.item1), Math.max(extent1.item2, extent2.item2));
    }
}
