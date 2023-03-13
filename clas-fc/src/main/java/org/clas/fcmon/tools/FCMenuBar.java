package org.clas.fcmon.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import javax.swing.JOptionPane;

import org.clas.fcmon.tools.EventControl;

@SuppressWarnings("serial")

public class FCMenuBar extends JMenuBar  {
	
    public MonitorApp      app = null; 
    
    MenuFile        menu1 = new MenuFile();
    MenuSettings    menu2 = new MenuSettings();
    MenuTriggerBits menu3 = new MenuTriggerBits();
    
	public FCMenuBar() {		
		this.add(menu1.menu);
		this.add(menu2.menu);
		this.add(menu3.menu);
	}
	
    public void setApplicationClass(MonitorApp app) {
        this.app = app;
    }
    
    public void initMenu() {
    	menu2.initButtons();
    }
    
	public class MenuFile extends JMenu implements ActionListener {
		
	    public MenuFile() {
	    	    createMenu();
	    }
	    			
        JMenu             menu = new JMenu("File");
        JMenu          ET_open = new JMenu("Attach to ET");
        JMenu          XM_open = new JMenu("xMsg Ring");
        JMenuItem ev_file_open = new JMenuItem("Load EVIO File");    
        JMenuItem h3_file_open = new JMenuItem("Load HIPO3 File");    
        JMenuItem h4_file_open = new JMenuItem("Load HIPO4 File");    
        JMenuItem           s1 = new JMenuItem("Sector 1");
        JMenuItem           s2 = new JMenuItem("Sector 2");
        JMenuItem           s3 = new JMenuItem("Sector 3");
        JMenuItem           s4 = new JMenuItem("Sector 4");
        JMenuItem           s5 = new JMenuItem("Sector 5");
        JMenuItem           s6 = new JMenuItem("Sector 6");
        JMenuItem         ctof = new JMenuItem("CTOF");
        JMenuItem          cnd = new JMenuItem("CND");
        JMenuItem         band = new JMenuItem("BAND");
        JMenuItem         htcc = new JMenuItem("HTCC");
        JMenuItem          svt = new JMenuItem("SVT");
        JMenuItem          sd3 = new JMenuItem("clondaq3");
        JMenuItem          sd4 = new JMenuItem("clondaq4");
        JMenuItem          sd5 = new JMenuItem("clondaq5");
        JMenuItem          sd6 = new JMenuItem("clondaq6");
        JMenuItem           x0 = new JMenuItem("EVIO");
        JMenuItem           x1 = new JMenuItem("HIPO");
    
        JMenuItem menuItem1, menuItem2, menuItem3, menuItem4;
        
        String          ethost = null;
        String          etfile = null;
    
        String      fileformat = null;
        File              file = null;
  
        public JMenu getMenu() {
        	    return menu;
        }
    
        public void createMenu() {
    	
            menu.add(ev_file_open);
            menu.add(h3_file_open);
            menu.add(h4_file_open);
            menu.add(ET_open);
            menu.add(XM_open);
           
            ET_open.add(s1);
            ET_open.add(s2);
            ET_open.add(s3);
            ET_open.add(s4);
            ET_open.add(s5);
            ET_open.add(s6);
            ET_open.add(ctof);
            ET_open.add(cnd);
            ET_open.add(band);
            ET_open.add(htcc);
            ET_open.add(svt);
            ET_open.add(sd3);
            ET_open.add(sd4);
            ET_open.add(sd5);
            ET_open.add(sd6);
     
            XM_open.add(x0);
            XM_open.add(x1);
   
            ev_file_open.addActionListener(this);   
            h3_file_open.addActionListener(this);   
            h4_file_open.addActionListener(this);   
        
            s1.addActionListener(this);
            s2.addActionListener(this);
            s3.addActionListener(this);
            s4.addActionListener(this);
            s5.addActionListener(this);
            s6.addActionListener(this);   
          ctof.addActionListener(this);
           cnd.addActionListener(this);
          band.addActionListener(this);
          htcc.addActionListener(this);
           svt.addActionListener(this);
           sd3.addActionListener(this);     
           sd4.addActionListener(this);     
           sd5.addActionListener(this);     
           sd6.addActionListener(this);     
            x0.addActionListener(this);      
            x1.addActionListener(this);   
          
        }
    
	@Override
	    public void actionPerformed(ActionEvent e) {
		    int port=app.tcpPort;
		    
    	    if(e.getActionCommand().compareTo("Sector 1")==0) {ethost="adcecal1";etfile="/tmp/et_sys_clasprod";}
    	    if(e.getActionCommand().compareTo("Sector 2")==0) {ethost="adcecal2";etfile="/tmp/et_sys_clasprod";}
    	    if(e.getActionCommand().compareTo("Sector 3")==0) {ethost="adcecal3";etfile="/tmp/et_sys_clasprod";}
    	    if(e.getActionCommand().compareTo("Sector 4")==0) {ethost="adcecal4";etfile="/tmp/et_sys_clasprod";}
    	    if(e.getActionCommand().compareTo("Sector 5")==0) {ethost="adcecal5";etfile="/tmp/et_sys_clasprod";}
            if(e.getActionCommand().compareTo("Sector 6")==0) {ethost="adcecal6";etfile="/tmp/et_sys_clasprod";}      
            if(e.getActionCommand().compareTo("HTCC")==0)     {ethost="adcctof1";etfile="/et/clasprod";port=11111;}      
            if(e.getActionCommand().compareTo("CTOF")==0)     {ethost="adcctof1";etfile="/et/clasprod";port=11111;}      
            if(e.getActionCommand().compareTo("CND")==0)      {ethost="adcctof1";etfile="/et/clasprod";port=11111;}      
            if(e.getActionCommand().compareTo("BAND")==0)     {ethost="adcband1";etfile="/et/bandtest";port=app.tcpPort;}      
            if(e.getActionCommand().compareTo("SVT")==0)      {ethost="svt1";    etfile="/et/clasprod";port=11111;}      
            if(e.getActionCommand().compareTo("clondaq3")==0) {ethost="clondaq3";etfile="/tmp/et_sys_clasprod";}       
            if(e.getActionCommand().compareTo("clondaq4")==0) {ethost="clondaq4";etfile="/tmp/et_sys_clasprod";}       
            if(e.getActionCommand().compareTo("clondaq5")==0) {ethost="clondaq5";etfile="/et/clasprod";}       
            if(e.getActionCommand().compareTo("clondaq6")==0) {ethost="clondaq6";etfile="/et/clasprod";} 
    	    if(ethost!=null) app.eventControl.openEtFile(ethost,etfile,port);    
    	    
            if(e.getActionCommand().compareTo("Load EVIO File")==0)  this.chooseFile("EVIO");
            if(e.getActionCommand().compareTo("Load HIPO3 File")==0) this.chooseFile("HIPO3");
            if(e.getActionCommand().compareTo("Load HIPO4 File")==0) this.chooseFile("HIPO4");
            if(e.getActionCommand().compareTo("EVIO")==0) app.eventControl.openXEvioRing();
            if(e.getActionCommand().compareTo("HIPO")==0) app.eventControl.openXHipoRing();
	    }
	
        public void chooseFile(String tag) {
        	
            final JFileChooser fc = new JFileChooser();
            
            fc.setFileFilter(new javax.swing.filechooser.FileFilter(){
    		
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".evio") || f.getName().toLowerCase().matches(".*\\.evio.*") ||
                       f.getName().toLowerCase().endsWith(".hipo") || f.getName().toLowerCase().matches(".*\\.hipo.*") ||
                       f.isDirectory();
    		    }
    	    	
            public String getDescription() {
            	return tag+" CLAS data format";
            }
        
            });
    	
            String currentDir = System.getenv("PWD");
            if(currentDir!=null) fc.setCurrentDirectory(new File(currentDir));
            int      returnVal = fc.showOpenDialog(this);
        
            if (returnVal == JFileChooser.APPROVE_OPTION) {
          	    file = fc.getSelectedFile();
                app.eventControl.openFile(file,tag);
            }
        }
        
	}
	
	public class MenuSettings extends JMenu implements ActionListener {
		
		JMenuItem item0,item1,item2,item3,item4;
		
        public MenuSettings() {
            createMenu();
		}
        
        public JMenu getMenu() {
    	        return menu;
        } 
        
        JMenu menu = new JMenu("Settings");
        
        public void initButtons() {
        }
        
        public void createMenu() {
        	JCheckBoxMenuItem item0 = new JCheckBoxMenuItem("DEBUG");
            item0.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {        	
                    if(e.getStateChange() == ItemEvent.SELECTED) {
             	          app.debug = true;
                    } else {
             	          app.debug = false;
                    };
                }
                }); 
            menu.add(item0);
        	
            item1 = new JMenuItem("HISTO reset interval");
            item1.addActionListener(this);
            menu.add(item1);
            
            item2 = new JMenuItem("TDC Offset");
            item2.addActionListener(this);
            menu.add(item2);
            
            item3 = new JMenuItem("Phase Offset");
            item3.addActionListener(this);
            menu.add(item3);
            
            JCheckBoxMenuItem item4 = new JCheckBoxMenuItem("Phase Correction");  
            item4.setSelected(true);
            item4.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {        	
                    if(e.getStateChange() == ItemEvent.SELECTED) {
             	          app.correctPhase = true;
                    } else {
             	          app.correctPhase = false;
                    };
                }
                }); 
            menu.add(item4);

        }
        
        public void chooseUpdateInterval() {
	        String s = (String)JOptionPane.showInputDialog(
	                    null,
	                    "HISTO reset interval (events)",
	                    " ",
	                    JOptionPane.PLAIN_MESSAGE,
	                    null,
	                    null,
	                    "100000000");
	        if(s!=null){
	            int events = 100000000;
	            try { 
	                events= Integer.parseInt(s);
	            } catch(NumberFormatException e) { 
	                JOptionPane.showMessageDialog(null, "Value must be a positive integer!");
	            }
	            if(events>0) {
	                app.setMaxEvents(events);
	            }
	            else {
	                JOptionPane.showMessageDialog(null, "Value must be a positive integer!");
	            }
	        }
	    }
        
        public void chooseTDCOffset() {
	        String s = (String)JOptionPane.showInputDialog(
	                    null,
	                    "TDC Offset",
	                    " ",
	                    JOptionPane.PLAIN_MESSAGE,
	                    null,
	                    null,
	                    Integer.toString(app.tdcOffset));
	        if(s!=null) app.setTDCOffset(Integer.parseInt(s));
	        
	    }        

        public void choosePhaseOffset() {
	        String s = (String)JOptionPane.showInputDialog(
	                    null,
	                    "Phase Offset",
	                    " ",
	                    JOptionPane.PLAIN_MESSAGE,
	                    null,
	                    null,
	                    Integer.toString(app.phaseOffset));
	        if(s!=null) app.setPhaseOffset(Integer.parseInt(s));
	        
	    }        

		@Override
		public void actionPerformed(ActionEvent e) {
	        if(e.getActionCommand() == "HISTO reset interval") {
	            this.chooseUpdateInterval();
	        }	        if(e.getActionCommand() == "HISTO reset interval") {
	            this.chooseUpdateInterval();
	        } 			
	        if(e.getActionCommand() == "TDC Offset") {
	            this.chooseTDCOffset();
	        } 			
	        if(e.getActionCommand() == "Phase Offset") {
	            this.choosePhaseOffset();
	        } 			
			
			
		}	    
	}
	
	public class MenuTriggerBits extends JMenu implements ActionListener {
        
        public MenuTriggerBits() {
            createMenu();
		}
		
        JMenu menu = new JMenu("TriggerBits");
        
//RGA		
        String TriggerDefRGA[] = { "Electron",
		        "Electron S1","Electron S2","Electron S3","Electron S4","Electron S5","Electron S6",
		        "ElectronOR noDC>300","PCALxECAL>10","","","","","","","","","","",
		        "FTOFxPCALxECAL(1-4)","FTOFxPCALxECAL(2-5)","FTOFxPCALxECAL(3-6)","","",
		        "FTxHDxFTOFxPCALxCTOF",
		        "FTxHDx(FTOFxPCAL)^2","FTxHD>100","FT>100","","","",
		        "1K Pulser"};
        
//RGB        
        String TriggerDef[] = { "Electron OR",
		        "e Sector 1","e Sector 2","e Sector 3","e Sector 4","e Sector 5","e Sector 6",
		        "Muons S1+ S4-","Muons S2+ S5-","Muons S3+ S6-",
		        "Muons S4+ S1-","Muons S5+ S2-","Muons S6+ S3-","","","","","","","","","","","","","","","","","","",
		        "1K Pulser"};          
        
        public JMenu getMenu() {
        	    return menu;
        }	
        
        public void createMenu() {
        	
            for (int i=0; i<32; i++) {
	
               JCheckBoxMenuItem bb = new JCheckBoxMenuItem(TriggerDef[i]);  
               final Integer bit = new Integer(i);
               bb.addItemListener(new ItemListener() {
               public void itemStateChanged(ItemEvent e) {        	
                   if(e.getStateChange() == ItemEvent.SELECTED) {
            	          app.setTriggerMask(bit);
                   } else {
            	          app.clearTriggerMask(bit);
                   };
               }
               });    
               menu.add(bb); 
            }
        }

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}
	}
             
}

