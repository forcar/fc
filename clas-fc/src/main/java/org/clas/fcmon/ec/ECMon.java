package org.clas.fcmon.ec;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.groot.base.GStyle;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioDataSync;
import org.jlab.service.ec.ECEngine;


import java.util.Arrays;
import java.util.TreeMap;

public class ECMon extends DetectorMonitor {
	
    static MonitorApp           app = new MonitorApp("ECMon",1800,950);	
    
    ECPixels                ecPix[] = new ECPixels[3];
    ConstantsManager           ccdb = new ConstantsManager();
    FTHashCollection            rtt = null;
    ECDet                     ecDet = null;
    
    ECReconstructionApp     ecRecon = null;
    ECMode1App              ecMode1 = null;
    ECEngineApp               ecEng = null;
    ECAdcApp                  ecAdc = null;
    ECTdcApp                  ecTdc = null;
    ECCalibrationApp        ecCalib = null;
    ECPedestalApp        ecPedestal = null;
    ECPixelsApp            ecPixels = null;
    ECGainsApp              ecGains = null;
    ECScalersApp          ecScalers = null;
    ECHvApp                    ecHv = null;  
    ECTriggerApp             ecTrig = null;
    
    EvioDataSync             writer = null;
    Boolean                saveFile = false;
   
    public static int        calRun = 4483;
    public static String  variation = "default";
    int                       detID = 0;
    int                         is1 = 1;
    int                         is2 = 7;  
    int    nsa,nsb,tet,p1,p2,pedref = 0;
    double               PCMon_zmin = 0;
    double               PCMon_zmax = 0;
    boolean              firstevent = true;
  
    String                   mondet = "EC";
    static String           appname = "ECMON";
    String                 detnam[] = {"PCAL","ECin","ECout"};
        
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
   
    public ECMon(String det) {
        super(appname,"1.0","lcsmith");
        mondet = det;
        ECDetector ecdet  = new ECFactory().createDetectorTilted(GeometryFactory.getConstants(DetectorType.ECAL, 10, "default"));
        ecPix[0] = new ECPixels("PCAL",ecdet);
        ecPix[1] = new ECPixels("ECin",ecdet);
        ecPix[2] = new ECPixels("ECout",ecdet);
    }
	
    public static void main(String[] args){
        String det = "PCAL";
        ECMon monitor = new ECMon(det);
        if (args.length != 0) {
           monitor.is1=Integer.parseInt(args[0]); 
           monitor.is2=Integer.parseInt(args[1]);    
        }
        app.setPluginClass(monitor);
        app.setAppName(appname);
        app.setVariation(variation);
        app.makeGUI();
        app.getEnv();
        monitor.initConstants();
        monitor.initCCDB(10);
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.init();
        monitor.initDetector();
        app.init();
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(2); 
        app.setTDCOffset(420);
        monitor.ecDet.initButtons();
        app.initFCMenu();
    }
    
    public void initConstants() {
        ECConstants.setSectorRange(is1,is2);
    }
    
    public void initCCDB(int runno) {
        System.out.println(appname+".initCCDB() for run "+runno); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ec",
                "/daq/tt/ec",
                "/calibration/ec/attenuation",
                "/calibration/ec/gain",
                "/calibration/ec/status"}));
        app.getReverseTT(ccdb,runno,"/daq/tt/ec");
        app.mode7Emulation.init(ccdb,runno,"/daq/fadc/ec", 3,3,1);        
    }	
    
    public void initDetector() {
        System.out.println(appname+".initDetector()"); 
        ecDet = new ECDet("ECDet",ecPix);
        ecDet.setMonitoringClass(this);
        ecDet.setApplicationClass(app);
        ecDet.init();
    }
    
    public void makeApps()  {
        System.out.println(appname+".makeApps()");   
        
        ecRecon = new ECReconstructionApp("ECREC",ecPix);        
        ecRecon.setMonitoringClass(this);
        ecRecon.setApplicationClass(app);
        
        ecMode1 = new ECMode1App("Mode1",ecPix);
        ecMode1.setMonitoringClass(this);
        ecMode1.setApplicationClass(app);
        
        ecAdc = new ECAdcApp("ADC",ecPix);        
        ecAdc.setMonitoringClass(this);
        ecAdc.setApplicationClass(app);     
               
        ecTdc = new ECTdcApp("TDC",ecPix);        
        ecTdc.setMonitoringClass(this);
        ecTdc.setApplicationClass(app); 
        
        ecPixels = new ECPixelsApp("Pixels",ecPix);       
        ecPixels.setMonitoringClass(this);
        ecPixels.setApplicationClass(app); 
        
        ecPedestal = new ECPedestalApp("Pedestal",ecPix);       
        ecPedestal.setMonitoringClass(this);
        ecPedestal.setApplicationClass(app);  

        ecCalib = new ECCalibrationApp("Calibration", ecPix);
        ecCalib.setMonitoringClass(this);
        ecCalib.setApplicationClass(app);
        ecCalib.setConstantsManager(ccdb,calRun);
        ecCalib.init(); 
        
        ecEng = new ECEngineApp("ECEngine",ecPix);
        ecEng.setMonitoringClass(this);
        ecEng.setApplicationClass(app);
        
        ecGains = new ECGainsApp("Gains");
        ecGains.setMonitoringClass(this);
        ecGains.setApplicationClass(app);
                
        ecHv = new ECHvApp("HV","EC");
        ecHv.setMonitoringClass(this);
        ecHv.setApplicationClass(app);  
        ecHv.init();
        
        ecScalers = new ECScalersApp("Scalers","EC");
        ecScalers.setMonitoringClass(this);
        ecScalers.setApplicationClass(app);    
        ecScalers.init();
        
        ecTrig = new ECTriggerApp("Triggers",ecPix);
        ecTrig.setMonitoringClass(this);
        ecTrig.setApplicationClass(app);    
        
        if(app.xMsgHost=="localhost") app.startEpics();
    }
    
    public void addCanvas() {
        System.out.println(appname+".addCanvas()"); 
        app.addFrame(ecMode1.getName(),             ecMode1.getPanel());
        app.addFrame(ecEng.getName(),                 ecEng.getPanel());
        app.addCanvas(ecAdc.getName(),                ecAdc.getCanvas());          
        app.addCanvas(ecTdc.getName(),                ecTdc.getCanvas());          
        app.addCanvas(ecPedestal.getName(),      ecPedestal.getCanvas());         
        app.addCanvas(ecPixels.getName(),          ecPixels.getCanvas());         
        app.addFrame(ecCalib.getName(),             ecCalib.getPanel());
        app.addFrame(ecGains.getName(),             ecGains.getPanel());
        app.addFrame(ecHv.getName(),                   ecHv.getPanel());
        app.addFrame(ecScalers.getName(),         ecScalers.getPanel());     
        app.addFrame(ecTrig.getName(),               ecTrig.getPanel());
    }
	
    public void init( ) {	    
        System.out.println(appname+".init()");	
        firstevent = true;
        app.setInProcess(0);  
        initApps();
        for (int i=0; i<ecPix.length; i++) ecPix[i].initHistograms(" ");
    }

    public void initApps() {
        System.out.println(appname+".initApps()");
        for (int i=0; i<ecPix.length; i++)   ecPix[i].init();
        ecRecon.init();
        ecGains.init();
        ecTrig.init();
        initEngine();
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_a.add(0,0,0, ecRecon.toTreeMap(ecPix[i].ec_cmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_t.add(0,0,0, ecRecon.toTreeMap(ecPix[i].ec_cmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_a.add(0,0,1, ecRecon.toTreeMap(ecPix[i].ec_zmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].getLmapMinMax(0,1,0,0); 
    }
    
    public void initEpics(Boolean doEpics) {
        System.out.println(appname+".initScalers():Initializing EPICS Channel Access");
        if (app.xMsgHost=="localhost") {ecHv.online=false ; ecScalers.online=false;}
        if ( doEpics) {ecHv.startEPICS(); ecScalers.startEPICS();}
        if (!doEpics) {ecHv.stopEPICS();  ecScalers.stopEPICS();}
    }
    
    public void initEngine() {
    	
    }
	
    public void initGlob() {
        System.out.println(appname+".initGlob()");
        putGlob("detID", detID);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);		
        putGlob("PCMon_zmin", PCMon_zmin);
        putGlob("PCMon_zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",ECConstants.IS1);
        putGlob("calRun",calRun);
    }
    
    @Override
    public TreeMap<String,Object> getGlob(){
        return this.glob;
    }
	
    @Override
    public void putGlob(String name, Object obj){
        glob.put(name,obj);
    }


    @Override
    public void reset() {
		ecRecon.clearHistograms();
		ecGains.clearHistograms();
		ecTrig.clearHistograms();
    } 
    
    public void dropBanks(DataEvent event) {    	
        if(event.hasBank("ECAL::clusters")) event.removeBanks("ECAL::hits","ECAL::peaks","ECAL::clusters","ECAL::calib","ECAL::moments");
        if(event.hasBank("ECAL::clusters")) event.removeBank("ECAL::clusters");
        if(event.hasBank("ECAL::hits"))     event.removeBank("ECAL::hits");
        if(event.hasBank("ECAL::peaks"))    event.removeBank("ECAL::peaks");
        if(event.hasBank("ECAL::calib"))    event.removeBank("ECAL::calib");
        if(event.hasBank("ECAL::moments"))  event.removeBank("ECAL::moments");  	
    } 

    @Override
    public void dataEventAction(DataEvent de) {
    	
        if (firstevent && app.getEventNumber()>2) {
        	System.out.println(appname+".dataEventAction: First Event");
   	        initCCDB(calRun);
   	        firstevent=false;
        }  
        
        ecRecon.addEvent(de);
        ecTrig.addEvent(de);
      
        if(app.doEng) {
          ecEng.ecEngine.singleEvent = app.isSingleEvent() ; 
          ecEng.ecEngine.debug       = app.debug; 
          ecEng.ecEngine.setDebugSplit(app.debug);
          ecEng.ecEngine.isMC        = app.isMC;       
          if(de.hasBank("ECAL::hits")) {
        	     de.removeBank("ECAL::hits");
        	     de.removeBank("ECAL::peaks");
        	     de.removeBank("ECAL::clusters");
        	     de.removeBank("ECAL::calib");  
          }
          dropBanks(de);
          ecEng.ecEngine.processDataEvent(de);     
          ecEng.addEvent(de);
          if(app.doGain) ecGains.addEvent(de);
          if(de instanceof EvioDataEvent && saveFile) writer.writeEvent(de);
        }
    }

	@Override
	public void analyze() {		
	
		switch (app.getInProcess()) {
			case 1: 
			    for (int idet=0; idet<ecPix.length; idet++) ecRecon.makeMaps(idet); 
			    break;
			case 2: 
                // Final analysis of full detector at end of run
			    for (int idet=0; idet<ecPix.length; idet++) ecRecon.makeMaps(idet);
		        System.out.println("End of run");
				ecCalib.analyzeAllEngines(is1,is2,1,4);			
				if (app.doEng && app.doGain) ecGains.analyze();
		        app.setInProcess(3); 
		}
	}
    
    @Override    
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        ecDet.update(shape); 
//        ecCalib.updateDetectorView(shape); //For status maps
    }	
    
    @Override
    public void processShape(DetectorShape2D shape) {	
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze();             // Refresh color maps 
        switch (app.getSelectedTabName()) {
        case "Mode1":                       ecMode1.updateCanvas(dd); break;
        case "ADC":                           ecAdc.updateCanvas(dd); break;
        case "TDC":                           ecTdc.updateCanvas(dd); break;
        case "Pedestal":                 ecPedestal.updateCanvas(dd); break;
        case "Pixels":                     ecPixels.updateCanvas(dd); break;
        case "Calibration":                 ecCalib.updateCanvas(dd); break;
        case "Gains":                       ecGains.updateCanvas(dd); break;
        case "ECEngine":                      ecEng.updateCanvas(dd); break;
        case "HV":                             ecHv.updateCanvas(dd); break;
        case "Scalers":                   ecScalers.updateCanvas(dd); break; 
        case "Triggers":                     ecTrig.updateCanvas(dd);
        }				
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ecHv.loadHV(is1,is2,il1,il2);
    }
    
    @Override
    public void resetEventListener() {

    }

    @Override
    public void timerUpdate() {

    }
    
    @Override
    public void readHipoFile() {        
        System.out.println(appname+".readHipoFile()");
        for (int idet=0; idet<3; idet++) {
            String hipoFileName = app.hipoFilePath+mondet+idet+"_"+app.runNumber+".hipo";
            System.out.println("Reading Histograms from "+hipoFileName);
            ecPix[idet].initHistograms(hipoFileName);
          }
          app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println(appname+".writeHipoFile()");
        for (int idet=0; idet<3; idet++) {
            String hipoFileName = app.hipoFilePath+mondet+idet+"_"+app.runNumber+".hipo";
            System.out.println("Writing Histograms to "+hipoFileName);
            HipoFile histofile = new HipoFile(hipoFileName);
            histofile.addToMap("H2_a_Hist",    ecPix[idet].strips.hmap2.get("H2_a_Hist")); 
            histofile.addToMap("H1_a_Hist",    ecPix[idet].strips.hmap1.get("H1_a_Hist")); 
            histofile.addToMap("H2_PC_Stat",   ecPix[idet].strips.hmap2.get("H2_PC_Stat"));
            histofile.addToMap("H2_PCa_Stat",  ecPix[idet].strips.hmap2.get("H2_PCa_Stat"));
            histofile.addToMap("H2_PCt_Stat",  ecPix[idet].strips.hmap2.get("H2_PCt_Stat"));
            histofile.addToMap("H2_Peds_Hist", ecPix[idet].strips.hmap2.get("H2_Peds_Hist"));
            histofile.addToMap("H2_Mode1_Hist",ecPix[idet].strips.hmap2.get("H2_Mode1_Hist"));
            histofile.addToMap("H2_t_Hist",    ecPix[idet].strips.hmap2.get("H2_t_Hist"));
            histofile.addToMap("H1_a_Maps",    ecPix[idet].pixels.hmap1.get("H1_a_Maps"));
            histofile.addToMap("H1_t_Maps",    ecPix[idet].pixels.hmap1.get("H1_t_Maps"));
            if (idet==0) {
            histofile.addToMap("H1_SCA", ecScalers.H1_SCA);
            histofile.addToMap("H2_SCA", ecScalers.H2_SCA);
            }
            histofile.writeHipoFile(hipoFileName);
        }
    }
    
    @Override
    public void close() {
        System.out.println(appname+".close()");
        app.displayControl.setFPS(1);
        if(saveFile) writer.close();
    }
    
    @Override
    public void pause() {
        app.displayControl.setFPS(1);
        
    }

    @Override
    public void go() {
        app.displayControl.setFPS(1);
        
    }	

}
