package gov.usgs.swarm.database.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.h2.jdbcx.JdbcDataSource;

public class DatabaseConnection{
	
	public static Map<String,Object> properties = new HashMap<String,Object>();
	
	//Read properties file.
	public static File readFile(){
		JFileChooser chooser = new JFileChooser();
	     chooser.setMultiSelectionEnabled(false);
	     int option = chooser.showOpenDialog(null);
	     if (option == JFileChooser.APPROVE_OPTION) {
		    File sf = chooser.getSelectedFile();
		    String fileName = sf.getAbsolutePath().toString();
			if(fileName.endsWith(".prop")){
		    	 return sf;
		    }else{
		    	 JOptionPane.showMessageDialog(null, "You must select file with extension .prop");
		    	 return null;
		    }
		 }else {
		       JOptionPane.showMessageDialog(null, "You must select properties file.");
		       return null;
		 }
	}
	
	public static Properties setProperties(File file){
		Properties prop = new Properties();
		FileInputStream input = null;
		try {
			input = new FileInputStream(file.toString());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		if (input == null) {
			System.out.println("Sorry, unable to find " + file.toString());
			return null;
		}	 
		try {
			prop.load(input);
		} catch (IOException e1) {
			e1.printStackTrace();
		}	 
		return prop;
	}
	
	public static Object getValue(Object key){
		return properties.get(key);
	}
	
	public static void setDatabaseProperties(Properties prop){
		Enumeration<?> e = prop.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = prop.getProperty(key);
			properties.put(key, value);
			//System.out.println("Key : " + key + ", Value : " + value);
		}
	}
	
	public static void loadProperties(){
		setDatabaseProperties(setProperties(readFile()));
	}
	
	public static DataSource getDataSource() {	
		loadProperties();
		JdbcDataSource dataSource = new JdbcDataSource();	
		dataSource.setURL(properties.get("dbUrl").toString());
		dataSource.setUser(properties.get("user").toString());
		dataSource.setPassword(properties.get("password").toString());
		return dataSource;
	}
	
	
	public static void main(String[] args){
		loadProperties();
	}
	
	
	
}