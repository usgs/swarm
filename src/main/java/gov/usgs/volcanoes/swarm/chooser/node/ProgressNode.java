package gov.usgs.volcanoes.swarm.chooser.node;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JProgressBar;

import gov.usgs.volcanoes.swarm.Icons;

public class ProgressNode extends AbstractChooserNode {
    private static final long serialVersionUID = 1L;
    private JProgressBar progressBar;

    public ProgressNode() {
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(80, 10));
        icon = Icons.warning;
        label = "progress";
    }

    public void setProgress(double p) {
        progressBar.setValue((int) Math.round(p * 100));
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }
}