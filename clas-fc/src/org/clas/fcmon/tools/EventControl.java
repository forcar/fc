package org.clas.fcmon.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.detector.view.DetectorPane2D;
import org.jlab.io.base.DataSource;
//import org.jlab.evio.clas12.EvioDataEvent;
//import org.jlab.evio.clas12.EvioETSource;
//import org.jlab.evio.clas12.EvioSource;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioETSource;
import org.jlab.io.evio.EvioSource;
import org.jlab.io.hipo.HipoDataSource;

public class EventControl extends JPanel implements ActionListener, ChangeListener {
	
    JPanel    eventSource = new JPanel();
    JPanel   eventControl = new JPanel();
    JLabel      fileLabel = new JLabel("");
    JLabel    statusLabel = new JLabel("No Opened File");       
	
    DataSource                       evReader;      
    boolean  isRegularFileOpen = false;
    File                  file = null;
    String            filename = null;
    
    EvioETSource                     etReader;
    String         ethost=null,etfile = null;
    public Boolean  isEtFileOpen = false;
    public Boolean      isRemote = false;
    public Boolean isSingleEvent = false;
    Boolean              running = false;
    
    int               nEtEvents = 0;
    
    File          eviofile = null;
    String    eviofilename = null;	 
    
    JButton    buttonPrev = new JButton("<");
    JButton    buttonNext = new JButton(">");
    JButton buttonNextFFW = new JButton(">>");
    JButton    buttonStop = new JButton("||");
	
    SpinnerModel    model = new SpinnerNumberModel(0,0,10,0.1);
    JSpinner spinnerDelay = new JSpinner(model);
    int       threadDelay = 0;
    private java.util.Timer    processTimer  = null;	
	
    DetectorMonitor monitoringClass;
    DetectorPane2D detectorView;
	
    public void setPluginClass(DetectorMonitor monitoringClass, DetectorPane2D detectorView) {
      this.monitoringClass = monitoringClass;
      this.detectorView = detectorView;
    }
	
    public EventControl(){
		
      this.setBackground(Color.LIGHT_GRAY);
      this.eventSource.setBackground(Color.LIGHT_GRAY);
      this.eventControl.setBackground(Color.LIGHT_GRAY);
      this.fileLabel.setBackground(Color.LIGHT_GRAY);
      this.statusLabel.setBackground(Color.LIGHT_GRAY);
      this.eventSource.add(fileLabel);	
      this.eventSource.add(statusLabel);	

      this.eventControl.add(buttonPrev);
      this.eventControl.add(buttonNext);
      this.eventControl.add(buttonNextFFW);
      this.eventControl.add(buttonStop);        
      this.eventControl.add(new JLabel("Delay (sec)"));
      this.eventControl.add(this.spinnerDelay);
        	  
      this.setLayout(new BorderLayout());
      this.add(eventSource,BorderLayout.CENTER);
      this.add(eventControl,BorderLayout.PAGE_END);

      buttonPrev.addActionListener(this);
      buttonNext.addActionListener(this);        
      buttonNextFFW.addActionListener(this);        
      buttonStop.addActionListener(this);   
      spinnerDelay.addChangeListener(this);			

      buttonNext.setEnabled(false);
      buttonPrev.setEnabled(false);
      buttonStop.setEnabled(false);
      buttonNextFFW.setEnabled(false); 	
      
    }

    public void openEtFile(String ethost, String etfile) { 
      this.etfile=etfile;
      this.ethost=ethost;
      if(etfile!=null){
    		try {
    			etReader = new EvioETSource(ethost);
    			etReader.open(etfile);
    			this.fileLabel.setText("FILE: "+ethost+"::"+etfile);
    			nEtEvents=0;
    		} catch(Exception e){
    			System.out.println("Error opening ET file : " + etfile);
    			this.fileLabel.setText(" ");
    			etReader = null;
    		} finally {
    			isRemote	  = true;
    			isSingleEvent = false;
    			isEtFileOpen  = true;
    			isRegularFileOpen = false;
    			etReader.close();
    			etReader.loadEvents();
    			buttonNext.setEnabled(true);
    			buttonPrev.setEnabled(false);
    			buttonNextFFW.setEnabled(true);
    			buttonStop.setEnabled(false);
    		}    
    	}
    }
 
    public void openEvioFile(File eviofile) {
    	isRemote = false;
        isSingleEvent = false;
        this.monitoringClass.init();
        if(eviofile.getName().contains("hipo")==true){
            evReader = new HipoDataSource();
        } else {
            evReader = new EvioSource();
        }        
        eviofilename = eviofile.getAbsolutePath();
        evReader.open(eviofilename);
        isRegularFileOpen = true;
        buttonNext.setEnabled(true);
        buttonNextFFW.setEnabled(true);
        Integer current = evReader.getCurrentIndex();
        Integer nevents = evReader.getSize();  
        this.fileLabel.setText("FILE: "+eviofile.getName());
        this.statusLabel.setText("   EVENTS IN FILE : " + nevents.toString() + "  CURRENT : " + current.toString());
    }
    
    @Override
	public void stateChanged(ChangeEvent e) {
        double delay = (double) spinnerDelay.getValue();
        threadDelay = (int) (delay*1000);
        isSingleEvent = false;
        if (delay!=0) isSingleEvent=true;     		
	}

	   
	@Override
    public void actionPerformed(ActionEvent e) {
    	    	
        if(e.getActionCommand().compareTo("<")==0){
            isSingleEvent = true;
            if(evReader.hasEvent()){
                if(evReader.getCurrentIndex()>=2){
                    
                    EvioDataEvent event = (EvioDataEvent) evReader.getPreviousEvent();
                    Integer current = evReader.getCurrentIndex();
                    Integer nevents = evReader.getSize();
                    this.statusLabel.setText("   EVENTS IN FILE : " + nevents.toString() + "  CURRENT : " + current.toString());

                    if(evReader.getCurrentIndex()==2){
                        buttonPrev.setEnabled(false);
                    }
                    if(event!=null){
           
                            try {
                                monitoringClass.dataEventAction(event);
                                this.detectorView.update();
                                this.detectorView.getView().updateGUI();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        
                    }
                }
            }
        }
        
        if(e.getActionCommand().compareTo(">")==0){
            if (!running) running = true;
        	isSingleEvent = true;
        	this.processNextEvent();
        	buttonPrev.setEnabled(true);
            this.detectorView.update();
            this.detectorView.getView().updateGUI();
        }
        
        if(e.getActionCommand().compareTo(">>")==0){
        	isSingleEvent = false;
        	running = true;
            class CrunchifyReminder extends TimerTask {
            	public void run() {
            		for (int i=1 ; i<200 ; i++) {
                      processNextEvent();
            		}
            	}
            }
            processTimer = new java.util.Timer();
            processTimer.schedule(new CrunchifyReminder(),1,1);
            buttonStop.setEnabled(true);
            buttonNext.setEnabled(false);
            buttonPrev.setEnabled(false);
            buttonNextFFW.setEnabled(false);
        }
        
        if(e.getActionCommand().compareTo("||")==0){
        	killTimer();
            buttonNextFFW.setEnabled(true);
            buttonStop.setEnabled(false);
            buttonNext.setEnabled(true);
            buttonPrev.setEnabled(true);
            //monitoringClass.analyze(2);
        }
        
        
    }

    private void killTimer(){
        if(processTimer!=null){
           processTimer.cancel();
           processTimer = null;
        }   	
    }
    
    private void processNextEvent() {	
    	if (!running) return;
    	
        if(isEtFileOpen == true){
            if(etReader.hasEvent()==false){
                int maxTries = 20;
                int trycount = 0;
                etReader.clearEvents();
                while(trycount<maxTries&&etReader.getSize()<=0){
                    
//                    System.out.println("[Et-Ring::Thread] ---> reloading the data....");
                    try {
                        Thread.sleep(threadDelay);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MonitorApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    etReader.loadEvents();
//                    System.out.println("[Et-Ring::Thread] ---> reloaded events try = " + trycount
//                    + "  event buffer size = " + etReader.getSize());
                    trycount++;
                }
                if(trycount==maxTries){
                    System.out.println("[Et-Ring::Thread] Tried reloading events unsuccesfully");
                }
            }
            
            if(etReader.hasEvent()==true){
                EvioDataEvent event = (EvioDataEvent) etReader.getNextEvent();
                int current = etReader.getCurrentIndex();
                int nevents = etReader.getSize();
                nEtEvents++;
                if(nEtEvents>100&&nEtEvents%500==0) monitoringClass.analyze(1);                
                this.statusLabel.setText("   EVENTS IN ET : " + nevents + "  CURRENT : " + nEtEvents);
                                  
                    try {
                        monitoringClass.dataEventAction(event);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                
                    try {
                    	Thread.sleep(threadDelay);
                    } catch (InterruptedException ex) {
                    	Logger.getLogger(MonitorApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
            return;
        }
    	   	
    	if(evReader.hasEvent()){	 
    		EvioDataEvent event = (EvioDataEvent) evReader.getNextEvent();
    		int current = evReader.getCurrentIndex();
    		int nevents = evReader.getSize();  
    		if(isSingleEvent) monitoringClass.analyze(1);
    		if( current>100&&current%500==0) monitoringClass.analyze(1);
    		this.statusLabel.setText("   EVENTS IN FILE : " + nevents + "  CURRENT : " + current);
        
    		try {
                Thread.sleep(threadDelay);
               monitoringClass.dataEventAction(event);
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}
     
        } else {
          running = false;
    	  killTimer();
    	  evReader.close();
          buttonNextFFW.setEnabled(false);
          buttonStop.setEnabled(false);
          buttonNext.setEnabled(false);
          buttonPrev.setEnabled(false);        
    	  monitoringClass.analyze(2);
    	  monitoringClass.close();
          System.out.println("DONE PROCESSING FILE");
        }

    }  

}

