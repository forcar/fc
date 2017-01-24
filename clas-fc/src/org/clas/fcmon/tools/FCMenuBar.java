package org.clas.fcmon.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.clas.fcmon.tools.EventControl;

@SuppressWarnings("serial")
public class FCMenuBar extends JMenuBar implements ActionListener {
    
    JMenu             file = new JMenu("File");
    JMenu          ET_open = new JMenu("Attach to ET");
    JMenu          XM_open = new JMenu("xMsg Ring");
    
    JMenuItem    file_open = new JMenuItem("Load EVIO or HIPO File");    
    JMenuItem           s1 = new JMenuItem("Sector 1");
    JMenuItem           s2 = new JMenuItem("Sector 2");
    JMenuItem           s3 = new JMenuItem("Sector 3");
    JMenuItem           s4 = new JMenuItem("Sector 4");
    JMenuItem           s5 = new JMenuItem("Sector 5");
    JMenuItem           s6 = new JMenuItem("Sector 6");
    JMenuItem           s0 = new JMenuItem("Sector 0");
    JMenuItem           x0 = new JMenuItem("EVIO");
    JMenuItem           x1 = new JMenuItem("HIPO");
       
    JMenu          plugins = new JMenu("Plugins");
    JMenuItem  load_plugin = new JMenuItem("Load Plugin");
    
    String          ethost = null;
    String          etfile = null;
    
    String      fileformat = null;
    File          eviofile = null;
    
    EventControl eventControl;
    
    public FCMenuBar() {
    	
    }
    
    public FCMenuBar(EventControl eventControl) {
    	
    	this.add(file);
    	this.add(plugins);
        file.add(file_open);
        file.add(ET_open);
        file.add(XM_open);
     plugins.add(load_plugin);
     
     this.eventControl = eventControl;
     
     ET_open.add(s1);
     ET_open.add(s2);
     ET_open.add(s3);
     ET_open.add(s4);
     ET_open.add(s5);
     ET_open.add(s6);
     ET_open.add(s0);
     
     XM_open.add(x0);
     XM_open.add(x1);
   
    file_open.addActionListener(this);              
           s1.addActionListener(this);
           s2.addActionListener(this);
           s3.addActionListener(this);
           s4.addActionListener(this);
           s5.addActionListener(this);
           s6.addActionListener(this);        
           s0.addActionListener(this);     
           x0.addActionListener(this);      
           x1.addActionListener(this);      
  load_plugin.addActionListener(this);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
    	if(e.getActionCommand().compareTo("Sector 1")==0) {ethost="adcecal1";etfile="/tmp/et_sys_clasprod1";}
    	if(e.getActionCommand().compareTo("Sector 2")==0) {ethost="adcecal2";etfile="/tmp/et_sys_clasprod2";}
    	if(e.getActionCommand().compareTo("Sector 3")==0) {ethost="adcecal3";etfile="/tmp/et_sys_clasprod3";}
    	if(e.getActionCommand().compareTo("Sector 4")==0) {ethost="adcecal4";etfile="/tmp/et_sys_clasprod4";}
    	if(e.getActionCommand().compareTo("Sector 5")==0) {ethost="adcecal5";etfile="/tmp/et_sys_clasprod5";}
        if(e.getActionCommand().compareTo("Sector 6")==0) {ethost="adcecal6";etfile="/tmp/et_sys_clasprod6";}      
        if(e.getActionCommand().compareTo("Sector 0")==0) {ethost="clondaq6";etfile="/tmp/et_sys_clastest0";}       
    	if(ethost!=null) this.eventControl.openEtFile(ethost,etfile);    	
        if(e.getActionCommand().compareTo("Load EVIO or HIPO File")==0) this.chooseEvioFile();
        if(e.getActionCommand().compareTo("EVIO")==0) this.eventControl.openXEvioRing();
        if(e.getActionCommand().compareTo("HIPO")==0) this.eventControl.openXHipoRing();
	}
	
    public void chooseEvioFile() {
    	final JFileChooser fc = new JFileChooser();
    	
    	fc.setFileFilter(new javax.swing.filechooser.FileFilter(){
    		public boolean accept(File f) {
    			return f.getName().toLowerCase().endsWith(".evio") || 
    			       f.getName().toLowerCase().endsWith(".hipo") || 
    			       f.isDirectory();
    		}
            public String getDescription() {
                    return "EVIO CLAS data format";
            }
        });
    	
    	String currentDir = System.getenv("PWD");
        if(currentDir!=null) fc.setCurrentDirectory(new File(currentDir));
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
        	eviofile     =       fc.getSelectedFile();
            this.eventControl.openEvioFile(eviofile);
        }
    }
             
}

