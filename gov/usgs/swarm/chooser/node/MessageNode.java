package gov.usgs.swarm.chooser.node;

import gov.usgs.swarm.Icons;

import javax.swing.Icon;

public class MessageNode extends ChooserNode {
    private static final long serialVersionUID = 1L;
    private String message;

    public MessageNode(String m) {
        message = m;
    }

    public Icon getIcon() {
        return Icons.warning;
    }

    public String getLabel() {
        return message;
    }

    public String getToolTip() {
        return null;
    }

    public String toString() {
        return message;
    }
    
}
