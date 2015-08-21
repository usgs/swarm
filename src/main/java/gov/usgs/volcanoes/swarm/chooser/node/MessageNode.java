package gov.usgs.volcanoes.swarm.chooser.node;

import javax.swing.Icon;

import gov.usgs.volcanoes.swarm.Icons;

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
