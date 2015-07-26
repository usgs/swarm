package gov.usgs.swarm.chooser.node;

import gov.usgs.swarm.Icons;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JProgressBar;

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