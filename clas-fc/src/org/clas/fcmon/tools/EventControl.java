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
import org.jlab.io.base.DataEvent;
import org.jlab.io.base.DataSource;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioETSource;
import org.jlab.io.evio.EvioRingSource;
import org.jlab.io.evio.EvioSource;
import org.jlab.io.hipo3.Hipo3DataSource;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.io.hipo.HipoRingSource;
import org.jlab.utils.groups.IndexedList;

public class EventControl extends JPanel implements ActionListener, ChangeListener {
	
    JPanel    eventSource = new JPanel();
    JPanel   eventControl = new JPanel();
    JLabel      fileLabel = new JLabel("");
    JLabel    statusLabel = new JLabel("No Opened File");       
    JButton    buttonPrev = new JButton("<");
    JButton    buttonNext = new JButton(">");
    JButton buttonNextFFW = new JButton(">>");
    JButton    buttonStop = new JButton("||");
    
    SpinnerModel    model = new SpinnerNumberModel(0,0,10,0.1);
    JSpinner spinnerDelay = new JSpinner(model);
	
    File                    file = null;
    String              filename = null;
    
    DataSource          evReader = null;      
    EvioETSource        etReader = null;
    EvioRingSource   XEvioReader = null;
    HipoRingSource   XHipoReader = null;
    String                ethost = null;
    String                etfile = null;
    String              xMsgHost = null;
    Boolean       isEvioFileOpen = false;
    Boolean       isHipoFileOpen = false;
    public Boolean  isEtFileOpen = false;
    public Boolean   isXEvioOpen = false;
    public Boolean   isXHipoOpen = false;
    public Boolean      isRemote = false;
    public Boolean isSingleEvent = false;
    public Boolean     isRunning = false;
    public int         inProcess = 0;
    public int          startFPS = 2;
    public int           stopFPS = 0;
    int                nEtEvents = 0;
    int              threadDelay = 0;
    public int      currentEvent = 0;
    
    private java.util.Timer    processTimer  = null;	
	
    DetectorMonitor monitoringClass;
    DetectorPane2D detectorView;
    IndexedList<DataEvent> eventList = new IndexedList<DataEvent>(1);
	
    public void setPluginClass(DetectorMonitor monitoringClass, DetectorPane2D detectorView) {
      this.monitoringClass = monitoringClass;
      this.detectorView = detectorView;
    }
    
    public void setXmsgHost(String host) {
      this.xMsgHost = host;        
    }
    
    public String getDataSource() {
        if    (isEvioFileOpen) return "EVIO";
        if    (isHipoFileOpen) return "HIPO";
        if      (isEtFileOpen) return "ET";
        if       (isXEvioOpen) return "XEVIO";
        if       (isXHipoOpen) return "XHIPO";
        return "NONE";
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

    public void openEtFile(String ethost, String etfile, int port) { 
      this.etfile=etfile;
      this.ethost=ethost;
      if(isEtFileOpen) etReader.close();
      if(etfile!=null){
    		try {
    			etReader = new EvioETSource(ethost,port);
    			etReader.open(etfile);
    			this.fileLabel.setText("FILE: "+ethost+"::"+etfile);
    			nEtEvents=0;
    		} catch(Exception e){
    			System.out.println("Error opening ET file : " + etfile);
    			this.fileLabel.setText(" ");
    			etReader = null;
    		} finally {
    			isRemote	   = true;
    			isSingleEvent  = false;
    			isEtFileOpen   = true;
                isEvioFileOpen = false;
                isHipoFileOpen = false;
    			etReader.close();
    			etReader.loadEvents();
    			buttonNext.setEnabled(true);
    			buttonPrev.setEnabled(false);
    			buttonNextFFW.setEnabled(true);
    			buttonStop.setEnabled(false);
    		}    
    	}
    }
 
    public void openFile(File file, String tag) {
        if(isEvioFileOpen||isHipoFileOpen) evReader.close();
        this.monitoringClass.init();
        if(file.getName().contains("hipo")==true){
            isHipoFileOpen = true; isEvioFileOpen = false;
            if(tag=="HIPO3") evReader = new Hipo3DataSource();
            if(tag=="HIPO4") evReader = new HipoDataSource();
        } 
        if(file.getName().contains("evio")==true) {
            isEvioFileOpen = true; isHipoFileOpen = false;
            if(tag=="EVIO")  evReader = new EvioSource();
        }        
        filename = file.getAbsolutePath();
        evReader.open(filename);
        isRemote          = false;
        isSingleEvent     = false;
        isEtFileOpen      = false;
        isXEvioOpen       = false;
        isXHipoOpen       = false;
        buttonNext.setEnabled(true);
        buttonNextFFW.setEnabled(true);
        currentEvent = 1;
//        currentEvent = evReader.getCurrentIndex();
        Integer nevents = evReader.getSize();  
        this.fileLabel.setText("FILE: "+ file.getName());
        this.statusLabel.setText("   EVENTS IN FILE : " + nevents.toString() + "  CURRENT : " + currentEvent);
    }
    
    public void openXEvioRing(){
        if(isXEvioOpen) XEvioReader.close();
              try {
                  XEvioReader = new EvioRingSource();
                  XEvioReader.open(xMsgHost);
                  this.fileLabel.setText("HOST::"+xMsgHost);
                  nEtEvents=0;
              } catch(Exception e){
                  System.out.println("Error opening xMsg host : " + xMsgHost);
                  this.fileLabel.setText(" ");
                  XEvioReader = null;
              } finally {
                  isRemote       = true;
                  isSingleEvent  = false;
                  isXEvioOpen    = true;
                  isXHipoOpen    = false;
                  isEvioFileOpen = false;
                  isHipoFileOpen = false;
                  isEtFileOpen   = false;
                  buttonNext.setEnabled(true);
                  buttonPrev.setEnabled(false);
                  buttonNextFFW.setEnabled(true);
                  buttonStop.setEnabled(false);
              }           
    }
    public void openXHipoRing(){
        if(isXHipoOpen) XHipoReader.close();
              try {
                  XHipoReader = new HipoRingSource();
                  XHipoReader.open(xMsgHost);
                  this.fileLabel.setText("HOST::"+xMsgHost);
                  nEtEvents=0;
              } catch(Exception e){
                  System.out.println("Error opening xMsg host : " + xMsgHost);
                  this.fileLabel.setText(" ");
                  XHipoReader = null;
              } finally {
                  isRemote       = true;
                  isSingleEvent  = false;
                  isXHipoOpen    = true;
                  isXEvioOpen    = false;
                  isEvioFileOpen = false;
                  isHipoFileOpen = false;
                  isEtFileOpen   = false;
                  buttonNext.setEnabled(true);
                  buttonPrev.setEnabled(false);
                  buttonNextFFW.setEnabled(true);
                  buttonStop.setEnabled(false);
              }           
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
//                currentEvent = evReader.getCurrentIndex();
                DataEvent event = null;
                if(currentEvent>=2){                    
                	    currentEvent--; currentEvent--;
                    DataEvent   dum = evReader.getPreviousEvent();
                    currentEvent++;
//                    currentEvent = evReader.getCurrentIndex();
                    if(eventList.hasItem(currentEvent)) {
                        event = eventList.getItem(currentEvent);
                    }else{
                        System.out.println("WARNING:EVENT "+currentEvent+" NOT FOUND IN eventList");
                        event = dum;
                    }
                    Integer nevents = evReader.getSize();
                    this.statusLabel.setText("   EVENTS IN FILE : " + nevents.toString() + "  CURRENT : " + currentEvent);

                    if(currentEvent==2){
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
            if (!isRunning) isRunning = true;
         	isSingleEvent = true;
         	inProcess = 1;
         	this.processNextEvent();
         	buttonPrev.setEnabled(true);        	
            this.detectorView.update();
            this.detectorView.getView().updateGUI();
        }
        
        if(e.getActionCommand().compareTo(">>")==0){
            eventList.clear();
            monitoringClass.go();
            isSingleEvent = false;
            isRunning     = true;
            inProcess     = 1;
         	
            class CrunchifyReminder extends TimerTask {
            	    public void run() {
            	      	for (int i=1 ; i<2000 ; i++) {
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
            isRunning = false;
            inProcess = 2;
            monitoringClass.pause();
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
    	if (!isRunning) return;
    	
        if(isEtFileOpen == true){
            if(etReader.hasEvent()==false){
                int maxTries = 20;
                int trycount = 0;
    	        currentEvent++;
                etReader.clearEvents();
                while(trycount<maxTries&&etReader.getSize()<=0){                    
//                    System.out.println("[Et-Ring::Thread] ---> reloading the data....");
                    try {
                        Thread.sleep(threadDelay);
                    } 
                    
                    catch (InterruptedException ex) {
                        Logger.getLogger(MonitorApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    etReader.loadEvents();
//                    System.out.println("[Et-Ring::Thread] ---> reloaded events try = " + trycount
//                    + "  event buffer size = " + etReader.getSize());
                    trycount++;
                }
                if(trycount==maxTries){
                    System.out.println("[Et-Ring::Thread] Tried reloading events unsuccesfully");
                    buttonStop.setEnabled(true); buttonStop.setSelected(true);
                }
            }
            
            if(etReader.hasEvent()==true){
                EvioDataEvent event = (EvioDataEvent) etReader.getNextEvent();
                int current = etReader.getCurrentIndex();
                int nevents = etReader.getSize();
                nEtEvents++;
                if(nEtEvents>10&&nEtEvents%50==0) monitoringClass.analyze();                
                this.statusLabel.setText("   EVENTS IN ET : " + nevents + "  CURRENT : " + nEtEvents);
                                  
                    try {
                        monitoringClass.dataEventAction(event);
                    } 
                    
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                
                    try {
                      	Thread.sleep(threadDelay);
                    } 
                    
                    catch (InterruptedException ex) {
                        Logger.getLogger(MonitorApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
            return;
        }
        
        if(isXEvioOpen && XEvioReader.hasEvent()==true){
            DataEvent event = XEvioReader.getNextEvent();
            event.show();
        }
        
        if(isXHipoOpen) {
            if(XHipoReader.hasEvent()){
                
                DataEvent event = XHipoReader.getNextEvent();
                int current = XHipoReader.getCurrentIndex();
                int nevents = XHipoReader.getSize();  
                if(isSingleEvent) monitoringClass.analyze();
                if(current>100&&current%5000==0) monitoringClass.analyze();
                this.statusLabel.setText("   EVENTS IN FILE : " + nevents + "  CURRENT : " + current);
                
                try {
                    Thread.sleep(threadDelay);
                    monitoringClass.dataEventAction(event);
                } 
                catch (Exception e) {
                    e.printStackTrace();                     
                }
            } else {             
                try {
                    Thread.sleep(20);
                } 
                catch (InterruptedException ex) {
                        Logger.getLogger(HipoRingSource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    	
    	if(isEvioFileOpen||isHipoFileOpen) {
    		
    	    if(evReader.hasEvent()) {
    	        
    	        DataEvent event = evReader.getNextEvent();
    	        currentEvent++;
    	        
//    		    currentEvent    = evReader.getCurrentIndex();
                if(isSingleEvent) {
                    eventList.add(event,currentEvent);
                    monitoringClass.analyze();    
                }
            
    		    int nevents = evReader.getSize();  
                if(currentEvent>100&&currentEvent%5000==0) monitoringClass.analyze();
            
    		    this.statusLabel.setText("   EVENTS IN FILE : " + nevents + "  CURRENT : " + currentEvent);
        
    		    try {
                    Thread.sleep(threadDelay);
                    monitoringClass.dataEventAction(event);
    		    } 
    		    
    		    catch (Exception ex) {
    			    ex.printStackTrace();
    		    }
     
            } else {
        	
                isRunning = false;
                inProcess = 2;
                killTimer();
                evReader.close();
                isEvioFileOpen = false;
                isHipoFileOpen = false;
                buttonNextFFW.setEnabled(false);
                buttonStop.setEnabled(false);
                buttonNext.setEnabled(false);
                buttonPrev.setEnabled(false);        
                monitoringClass.analyze();
                monitoringClass.close();
                System.out.println("DONE PROCESSING FILE");
            
            }
    	}

    }  

}

