package gov.usgs.swarm.chooser.node;

import gov.usgs.swarm.Icons;

import javax.swing.Icon;

public class MessageNode extends AbstractChooserNode {
    private static final long serialVersionUID = 1L;
    private String message;

    public MessageNode(String m) {
        message = m;
        label = message;
        icon = Icons.warning;
    }

    public String toString() {
        return message;
    }
    
}
