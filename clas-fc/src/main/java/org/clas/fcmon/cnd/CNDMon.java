 package org.clas.fcmon.cnd;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.io.base.DataEvent;

public class CNDMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("CNDMon",1800,950);	
    
    CNDPixels              cndPix[] = new CNDPixels[1];
    ConstantsManager           ccdb = new ConstantsManager();
    FTHashCollection            rtt = null;      
    CNDDet                   cndDet = null;  
    
    CNDReconstructionApp   cndRecon = null;
    CNDMode1App            cndMode1 = null;
    CNDAdcApp                cndAdc = null;
    CNDTdcApp                cndTdc = null;
    CNDPedestalApp      cndPedestal = null;
    CNDMipApp                cndMip = null;    
    CNDCalibrationApp      cndCalib = null;
    CNDScalersApp        cndScalers = null;
    CNDHvApp                  cndHv = null;
       
    public int                 calRun = 10;
    int                         detID = 0;
    int                           is1 = 1;    
    int                           is2 = 25; 
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
    boolean                firstevent = true;
    
    String mondet                     = "CND";
    static String             appname = "CNDMON";
	
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public CNDMon(String det) {
        super(appname, "1.0", "lcsmith");
        mondet = det;
        cndPix[0] = new CNDPixels("CND");
    }

    public static void main(String[] args){		
        String det = "CND";
        CNDMon monitor = new CNDMon(det);	
        if (args.length != 0) {
            monitor.is1=Integer.parseInt(args[0]); 
            monitor.is2=Integer.parseInt(args[1]);    
         }
        app.setPluginClass(monitor);
        app.setAppName(appname);
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
        monitor.cndDet.initButtons();
    }
    
    public void initConstants() {
        CNDConstants.setSectorRange(is1,is2);
    }   
    
    public void initCCDB(int runno) {
        System.out.println(appname+".initCCDB(): Run "+runno); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/cnd",
                "/daq/tt/cnd"}));
        app.getReverseTT(ccdb,runno,"/daq/tt/cnd"); 
        app.mode7Emulation.init(ccdb,runno,"/daq/fadc/cnd", 73,3,1);        
    } 
    
    public void initDetector() {
        System.out.println(appname+".initDetector()"); 
        cndDet = new CNDDet("CNDDet",cndPix);
        cndDet.setMonitoringClass(this);
        cndDet.setApplicationClass(app);
        cndDet.init();
    }
	
    public void makeApps() {
        System.out.println(appname+".makeApps()"); 
        cndRecon = new CNDReconstructionApp("CNDREC",cndPix);        
        cndRecon.setMonitoringClass(this);
        cndRecon.setApplicationClass(app);	
        
        cndMode1 = new CNDMode1App("Mode1",cndPix);        
        cndMode1.setMonitoringClass(this);
        cndMode1.setApplicationClass(app);   
        
        cndAdc = new CNDAdcApp("ADC",cndPix);        
        cndAdc.setMonitoringClass(this);
        cndAdc.setApplicationClass(app);     
        
        cndTdc = new CNDTdcApp("TDC",cndPix);        
        cndTdc.setMonitoringClass(this);
        cndTdc.setApplicationClass(app);           
        
        cndPedestal = new CNDPedestalApp("Pedestal",cndPix);        
        cndPedestal.setMonitoringClass(this);
        cndPedestal.setApplicationClass(app);       
        
        cndMip = new CNDMipApp("MIP",cndPix);        
        cndMip.setMonitoringClass(this);
        cndMip.setApplicationClass(app);  
        
        cndCalib = new CNDCalibrationApp("Calibration", cndPix);
        cndCalib.setMonitoringClass(this);
        cndCalib.setApplicationClass(app);  
        cndCalib.init(is1,is2);
        
        cndHv = new CNDHvApp("HV","CND");
        cndHv.setMonitoringClass(this);
        cndHv.setApplicationClass(app);  
        cndHv.init();
        
        cndScalers = new CNDScalersApp("Scalers","CND");
        cndScalers.setMonitoringClass(this);
        cndScalers.setApplicationClass(app); 
        cndScalers.init();
        
        if(app.xMsgHost=="localhost") app.startEpics();
    }
	
    public void addCanvas() {
        System.out.println(appname+".addCanvas()"); 
        app.addFrame(cndMode1.getName(),          cndMode1.getPanel());
        app.addCanvas(cndAdc.getName(),             cndAdc.getCanvas());          
        app.addCanvas(cndTdc.getName(),             cndTdc.getCanvas());          
        app.addCanvas(cndPedestal.getName(),   cndPedestal.getCanvas());
        app.addCanvas(cndMip.getName(),             cndMip.getCanvas()); 
        app.addFrame(cndCalib.getName(),          cndCalib.getCalibPane());
        app.addFrame(cndHv.getName(),                cndHv.getPanel());
        app.addFrame(cndScalers.getName(),      cndScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println(appname+".init()");  
        firstevent = true;
        app.setInProcess(0);
        initApps();
        for (int i=0; i<cndPix.length; i++) cndPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println(appname+".initApps()");
        for (int i=0; i<cndPix.length; i++) cndPix[i].init();
        cndRecon.init();
    }
    
    public void initGlob() {
        System.out.println(appname+".initGlob()");
        putGlob("detID", detID);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("ccdb", ccdb);
        putGlob("zmin", PCMon_zmin);
        putGlob("zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",CNDConstants.IS1);
        putGlob("is2",CNDConstants.IS2);
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
        cndRecon.clearHistograms();
    }
	
    @Override
    public void dataEventAction(DataEvent de) {    	  
          if (firstevent&&app.getEventNumber()>2) {
  	         System.out.println(appname+".dataEventAction: First Event");
        	 initCCDB(app.getEventNumber());
        	 firstevent=false;
          }
    	  cndRecon.addEvent(de);	
    }

    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: 
            for (int idet=0; idet<cndPix.length; idet++) cndRecon.makeMaps(idet); 
            break;
        case 2:
            for (int idet=0; idet<cndPix.length; idet++) cndRecon.makeMaps(idet); 
            System.out.println("End of run");                 
//            cndCalib.engines[0].analyze();
            app.setInProcess(3);
        }
    }
    
    @Override
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        cndDet.update(shape);
        if (app.getSelectedTabName().equals("Scalers")) cndScalers.updateDetectorView(shape);
        if (app.getSelectedTabName().equals("HV"))           cndHv.updateDetectorView(shape);
    }
        
    @Override
    public void processShape(DetectorShape2D shape) { 
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze();  // Refresh color maps      
        switch (app.getSelectedTabName()) {
        case "Mode1":                        cndMode1.updateCanvas(dd); break;
        case "ADC":                            cndAdc.updateCanvas(dd); break;
        case "TDC":                            cndTdc.updateCanvas(dd); break;
        case "Pedestal":                  cndPedestal.updateCanvas(dd); break;
        case "MIP":                            cndMip.updateCanvas(dd); break; 
        case "HV":                              cndHv.updateCanvas(dd); break;
        case "Scalers":                    cndScalers.updateCanvas(dd);
       } 
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        cndHv.loadHV(is1,is2,il1,il2);
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
        for (int idet=0; idet<cndPix.length; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Reading Histograms from "+hipoFileName);
          cndPix[idet].initHistograms(hipoFileName);
        }
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println(appname+".writeHipoFile()");
        for (int idet=0; idet<cndPix.length; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Writing Histograms to "+hipoFileName);
          HipoFile histofile = new HipoFile(hipoFileName);
          histofile.addToMap("H2_a_Hist",cndPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",cndPix[idet].strips.hmap2.get("H2_t_Hist"));
          histofile.writeHipoFile(hipoFileName);
        }
    }
    
    @Override
    public void close() {
        System.out.println(appname+".close()");
        app.displayControl.setFPS(1);
    }
    
    @Override
    public void pause() {
        app.displayControl.setFPS(1);
        
    }

    @Override
    public void go() {
        app.displayControl.setFPS(10);
        
    }

    @Override
    public void initEngine() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initEpics(Boolean doEpics) {
        // TODO Auto-generated method stub
        System.out.println("monitor.initEpics():Initializing EPICS Channel Access");
        if (app.xMsgHost=="localhost") {cndHv.online=false ; cndScalers.online=false;}
        if ( doEpics) {cndHv.startEPICS(); cndScalers.startEPICS();}
        if (!doEpics) {cndHv.stopEPICS();  cndScalers.stopEPICS();}
    }
    
}
