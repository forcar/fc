package org.clas.fcmon.htcc;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.io.base.DataEvent;

public class HTCCMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("HTCCMon",1800,950);	
    
    HTCCPixels              htccPix[] = new HTCCPixels[1];
    ConstantsManager             ccdb = new ConstantsManager();
    FTHashCollection              rtt = null;      
    HTCCDet                   htccDet = null;  
    
    HTCCReconstructionApp   htccRecon = null;
    HTCCMode1App            htccMode1 = null;
    HTCCAdcApp                htccAdc = null;
    HTCCTdcApp                htccTdc = null;
    HTCCPedestalApp      htccPedestal = null;
    HTCCSPEApp                htccSpe = null;    
    HTCCCalibrationApp      htccCalib = null;
    HTCCScalersApp        htccScalers = null;
    HTCCHvApp                  htccHv = null;
       
    public int                 calRun = 12;
    int                         detID = 0;
    int                           is1 = 1;    //All sectors: is1=1 is2=7  Single sector: is1=s is2=s+1
    int                           is2 = 7; 
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
    boolean                firstevent = true;
    
    String mondet                     = "HTCC";
    static String             appname = "HTCCMON";
	
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public HTCCMon(String det) {
        super(appname, "1.0", "lcsmith");
        mondet = det;
        htccPix[0] = new HTCCPixels("HTCC");
    }

    public static void main(String[] args){		
        String det = "HTCC";
        HTCCMon monitor = new HTCCMon(det);	
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
        monitor.htccDet.initButtons();
    }
    
    public void initConstants() {
        HTCCConstants.setSectorRange(is1,is2);
    }   
    
    public void initCCDB(int runno) {
        System.out.println(appname+".initCCDB()"); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/htcc",
                "/daq/tt/htcc",
                "/calibration/ctof/attenuation",
                "/calibration/ctof/gain_balance",
                "/calibration/ctof/status"}));
        app.getReverseTT(ccdb,runno,"/daq/tt/htcc"); 
        app.mode7Emulation.init(ccdb,runno,"/daq/fadc/htcc", 59,13,1);        
    } 
    
    public void initDetector() {
        System.out.println(appname+".initDetector()"); 
        htccDet = new HTCCDet("HTCCDet",htccPix);
        htccDet.setMonitoringClass(this);
        htccDet.setApplicationClass(app);
        htccDet.init();
    }
	
    public void makeApps() {
        System.out.println(appname+".makeApps()"); 
        htccRecon = new HTCCReconstructionApp("HTCCREC",htccPix);        
        htccRecon.setMonitoringClass(this);
        htccRecon.setApplicationClass(app);	
        
        htccMode1 = new HTCCMode1App("Mode1",htccPix);        
        htccMode1.setMonitoringClass(this);
        htccMode1.setApplicationClass(app);   
        
        htccAdc = new HTCCAdcApp("ADC",htccPix);        
        htccAdc.setMonitoringClass(this);
        htccAdc.setApplicationClass(app);     
        
        htccTdc = new HTCCTdcApp("TDC",htccPix);        
        htccTdc.setMonitoringClass(this);
        htccTdc.setApplicationClass(app);           
        
        htccPedestal = new HTCCPedestalApp("Pedestal",htccPix);        
        htccPedestal.setMonitoringClass(this);
        htccPedestal.setApplicationClass(app);       
        
        htccSpe = new HTCCSPEApp("SPE",htccPix);        
        htccSpe.setMonitoringClass(this);
        htccSpe.setApplicationClass(app);  
        
        htccCalib = new HTCCCalibrationApp("Calibration", htccPix);
        htccCalib.setMonitoringClass(this);
        htccCalib.setApplicationClass(app);  
        htccCalib.init(is1,is2);
        
        htccHv = new HTCCHvApp("HV",mondet);
        htccHv.setMonitoringClass(this);
        htccHv.setApplicationClass(app);  
        htccHv.init();
        
        htccScalers = new HTCCScalersApp("Scalers",mondet);
        htccScalers.setMonitoringClass(this);
        htccScalers.setApplicationClass(app); 
        htccScalers.init();
        
        if(app.xMsgHost=="localhost") app.startEpics();
    }
	
    public void addCanvas() {
        System.out.println(appname+".addCanvas()"); 
        app.addFrame(htccMode1.getName(),          htccMode1.getPanel());
        app.addCanvas(htccAdc.getName(),             htccAdc.getCanvas());          
        app.addCanvas(htccTdc.getName(),             htccTdc.getCanvas());          
        app.addCanvas(htccPedestal.getName(),   htccPedestal.getCanvas());
        app.addCanvas(htccSpe.getName(),             htccSpe.getCanvas()); 
        app.addFrame(htccCalib.getName(),          htccCalib.getCalibPane());
        app.addFrame(htccHv.getName(),                htccHv.getPanel());
        app.addFrame(htccScalers.getName(),      htccScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println(appname+".init()");  
        firstevent = true;
        app.setInProcess(0);
        initApps();
        for (int i=0; i<htccPix.length; i++) htccPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println(appname+".initApps()");
        for (int i=0; i<htccPix.length; i++) htccPix[i].init();
        htccRecon.init();
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
        putGlob("is1",HTCCConstants.IS1);
        putGlob("is2",HTCCConstants.IS2);
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
        htccRecon.clearHistograms();
    }
	
    @Override
    public void dataEventAction(DataEvent de) {
        if (firstevent&&app.getEventNumber()>2) {
	        System.out.println(appname+".dataEventAction: First Event");
   	        initCCDB(app.run);
   	        firstevent=false;
        }       
        htccRecon.addEvent(de);	
    }

    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: 
            for (int idet=0; idet<htccPix.length; idet++) htccRecon.makeMaps(idet); 
            break;
        case 2:
            for (int idet=0; idet<htccPix.length; idet++) htccRecon.makeMaps(idet); 
            System.out.println("End of run");                 
//            htccCalib.engines[0].analyze();
            app.setInProcess(3);
        }
    }
    
    @Override
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        htccDet.update(shape);
        if (app.getSelectedTabName().equals("Scalers")) htccScalers.updateDetectorView(shape);
        if (app.getSelectedTabName().equals("HV"))           htccHv.updateDetectorView(shape);
    }
        
    @Override
    public void processShape(DetectorShape2D shape) { 
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze();  // Refresh color maps      
        switch (app.getSelectedTabName()) {
        case "Mode1":                        htccMode1.updateCanvas(dd); break;
        case "ADC":                            htccAdc.updateCanvas(dd); break;
        case "TDC":                            htccTdc.updateCanvas(dd); break;
        case "Pedestal":                  htccPedestal.updateCanvas(dd); break;
        case "SPE":                            htccSpe.updateCanvas(dd); break; 
        case "HV":                              htccHv.updateCanvas(dd); break;
        case "Scalers":                    htccScalers.updateCanvas(dd);
       } 
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        htccHv.loadHV(is1,is2,il1,il2);
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
        for (int idet=0; idet<htccPix.length; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Reading Histograms from "+hipoFileName);
          htccPix[idet].initHistograms(hipoFileName);
        }
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println(appname+".writeHipoFile()");
        for (int idet=0; idet<htccPix.length; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Writing Histograms to "+hipoFileName);
          HipoFile histofile = new HipoFile(hipoFileName);
          histofile.addToMap("H2_a_Hist",htccPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",htccPix[idet].strips.hmap2.get("H2_t_Hist"));
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
        if (app.xMsgHost=="localhost") {htccHv.online=false ; htccScalers.online=false;}
        if ( doEpics) {htccHv.startEPICS(); htccScalers.startEPICS();}
        if (!doEpics) {htccHv.stopEPICS();  htccScalers.stopEPICS();}
    }
    
}
