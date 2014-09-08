package gov.usgs.swarm.wave;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.WindowConstants;
@SuppressWarnings("serial")
public class WaveTable extends JFrame {
	JRadioButton  first;
    public WaveTable() {
    	
        super();
        setTitle("Location Mode");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Object[][] data = {
                {"Wave1", "P","up", "Impulsive"},
                {"Wave2", "s", "down", "emergent"},
                {"Wave3", "p", "up","Impulsive"},    
        };
        String[] entetes = {"      ","Wave", "Trajectory", "Impulsive/emergent"};
       final  JTable tableau = new JTable(data, entetes);
      
       
  
    	
    	
    
        
        
        
        JPanel boutons = new JPanel();
        
       // JPanel panel = new JPanel();
        first = new JRadioButton("choose it");
      /*  panel.add(first,BorderLayout.CENTER);
        panel.revalidate();*/
        
        
        
        JButton locate = new JButton("Locate");
        locate.addActionListener(new ActionListener() { 
        	  @Override
			public void actionPerformed(ActionEvent e) { 
        	   // execute the GAD algo
        	  } 
        	} );
        
      
        JButton saveToFile = new JButton("Save To File");
        
        saveToFile.addActionListener(new ActionListener() { 
        	  @Override
			public void actionPerformed(ActionEvent e) { 
        	  //Save Values
        	  } 
        	} );
        JButton editRow = new JButton("Edit Row");
        editRow.addActionListener(new ActionListener() { 
      	  @Override
		public void actionPerformed(ActionEvent e) { 
      	  tableau.setEnabled(true);
      	  } 
      	} );
        
        
        JButton deleteRow = new JButton("Delete Row");
        
        deleteRow.addActionListener(new ActionListener() { 
        	  @Override
			public void actionPerformed(ActionEvent e) { 
        		  int index = tableau.getSelectedRow(); // it throws the exception here
        		  tableau.clearSelection();             
        	      System.out.println(index);
        	      if(index != -1){
        	    	 tableau.removeRowSelectionInterval(index, index);
        	      }
        	  } 
        	} );
        
        
        boutons.add(editRow);
        boutons.add(deleteRow);
        
        boutons.add(locate);
        boutons.add(saveToFile);
       // boutons.add(first,BorderLayout.CENTER);
        boutons.revalidate();
        getContentPane().add(tableau.getTableHeader(), BorderLayout.NORTH);
        getContentPane().add(tableau, BorderLayout.CENTER);
        
        getContentPane().add(boutons, BorderLayout.SOUTH);
       // getContentPane().add(panel, BorderLayout.SOUTH);
        pack();
    }

}