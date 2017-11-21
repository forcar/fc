package org.clas.fcmon.cc;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12
import org.jlab.detector.base.DetectorCollection;

//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.io.base.DataEvent;

public class CCMon extends DetectorMonitor {
	
    static MonitorApp           app = new MonitorApp("LTCCMon",1800,950);
    
    CCPixels                  ccPix = null;
    ConstantsManager           ccdb = new ConstantsManager();  
    FTHashCollection            rtt = null;    
    CCDet                     ccDet = null;  
    
    CCReconstructionApp     ccRecon = null;
    CCMode1App              ccMode1 = null;
    CCOccupancyApp      ccOccupancy = null;
    CCPedestalApp        ccPedestal = null;
    CCSpeApp                  ccSpe = null;    
    CCCalibrationApp        ccCalib = null;
    CCScalersApp          ccScalers = null;
    CCHvApp                    ccHv = null;
    
    public int               calRun = 2;
    int                       detID = 0;
    int                         is1 = 1 ;
    int                         is2 = 7 ; 
    int    nsa,nsb,tet,p1,p2,pedref = 0;
    double               PCMon_zmin = 0;
    double               PCMon_zmax = 0;
    
    String                   mondet = "LTCC";
    static String           appname = "LTCCMON";
    
    DetectorCollection<H1F> H1_CCa_Sevd = new DetectorCollection<H1F>();
    DetectorCollection<H1F> H1_CCt_Sevd = new DetectorCollection<H1F>();
    DetectorCollection<H2F> H2_CCa_Hist = new DetectorCollection<H2F>();
    DetectorCollection<H2F> H2_CCt_Hist = new DetectorCollection<H2F>();
    DetectorCollection<H2F> H2_CCa_Sevd = new DetectorCollection<H2F>();
		
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public CCMon(String det) {
        super("CCMON", "1.0", "lcsmith");
        mondet = det;
        ccPix = new CCPixels("LTCC");  
    }

    public static void main(String[] args){		
        String det = "LTCC";
        CCMon monitor = new CCMon(det);	
        if (args.length != 0) {
            monitor.is1=Integer.parseInt(args[0]); 
            monitor.is2=Integer.parseInt(args[1]);    
         }
        app.setPluginClass(monitor);
        app.setAppName(appname);
        app.makeGUI();
        app.getEnv();
        monitor.initConstants();
        monitor.initCCDB();
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.init();
        monitor.initDetector();
        app.init();
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(1); 
        monitor.ccDet.initButtons();
    }
    
    public void initConstants() {
        CCConstants.setSectors(is1,is2);
    }

    public void initCCDB() {
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ltcc",
                "/daq/tt/ltcc",
                "/calibration/ltcc/gain",
                "/calibration/ltcc/timing_offset",
                "/calibration/ltcc/status"}));
        app.getReverseTT(ccdb,"/daq/tt/ltcc");
        app.mode7Emulation.init(ccdb, calRun, "/daq/fadc/ltcc", 1,18,12);        
    }
    
    public void initDetector() {
        System.out.println("monitor.initDetector()"); 
        ccDet = new CCDet("CCDet",ccPix);
        ccDet.setMonitoringClass(this);
        ccDet.setApplicationClass(app);
        ccDet.init();
    }
	
    public void makeApps() {
        
        System.out.println("monitor.makeApps()");  
        
        ccRecon = new CCReconstructionApp("LTCCREC",ccPix);        
        ccRecon.setMonitoringClass(this);
        ccRecon.setApplicationClass(app);	
        
        ccMode1 = new CCMode1App("Mode1",ccPix);        
        ccMode1.setMonitoringClass(this);
        ccMode1.setApplicationClass(app);   
        
        ccOccupancy = new CCOccupancyApp("Occupancy",ccPix);        
        ccOccupancy.setMonitoringClass(this);
        ccOccupancy.setApplicationClass(app);           
        
        ccPedestal = new CCPedestalApp("Pedestal",ccPix);        
        ccPedestal.setMonitoringClass(this);
        ccPedestal.setApplicationClass(app);       
        
        ccSpe = new CCSpeApp("SPE",ccPix);        
        ccSpe.setMonitoringClass(this);
        ccSpe.setApplicationClass(app);  
        
        ccCalib = new CCCalibrationApp("Calibration", ccPix);
        ccCalib.setMonitoringClass(this);
        ccCalib.setApplicationClass(app);  
        ccCalib.setConstantsManager(ccdb,calRun);
        ccCalib.init();
        
        ccHv = new CCHvApp("HV","LTCC");
        ccHv.setMonitoringClass(this);
        ccHv.setApplicationClass(app);  
        
        ccScalers = new CCScalersApp("Scalers","LTCC");
        ccScalers.setMonitoringClass(this);
        ccScalers.setApplicationClass(app);  
        
        if(app.xMsgHost=="localhost") app.startEpics();
        
    }
	
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addFrame(ccMode1.getName(),         ccMode1.getPanel());
        app.addCanvas(ccOccupancy.getName(), ccOccupancy.getCanvas());          
        app.addCanvas(ccPedestal.getName(),   ccPedestal.getCanvas());
        app.addCanvas(ccSpe.getName(),             ccSpe.getCanvas()); 
        app.addFrame(ccCalib.getName(),          ccCalib.getPanel());
        app.addFrame(ccHv.getName(),                ccHv.getPanel());
        app.addFrame(ccScalers.getName(),      ccScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println("monitor.init()");   
        app.setInProcess(0); 
        initApps();
        ccPix.initHistograms(" ");
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        ccPix.init();
        ccRecon.init();
    }
    
    public void initGlob() {
        System.out.println("monitor.initGlob()");
        putGlob("detID", detID);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("PCMon_zmin", PCMon_zmin);
        putGlob("PCMon_zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",CCConstants.IS1);
        putGlob("is2",CCConstants.IS2);
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
        ccRecon.clearHistograms();
    }	
	
    @Override
    public void dataEventAction(DataEvent de) {
        ccRecon.addEvent(de);	
    }
	
    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: ccRecon.makeMaps();  break;
        case 2: ccRecon.makeMaps();
                ccCalib.analyzeAllEngines(is1,is2,1,3);   
                app.setInProcess(3); 
        }
    }
    
    @Override
    public void update(DetectorShape2D shape) {
        ccDet.update(shape);
        if (app.getSelectedTabName().equals("Scalers"))   ccScalers.updateDetectorView(shape);
        if (app.getSelectedTabName().equals("HV"))             ccHv.updateDetectorView(shape);
    }


    @Override
    public void processShape(DetectorShape2D shape) {       
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd);
        this.analyze();        
        switch (app.getSelectedTabName()) {
        case "Mode1":                ccMode1.updateCanvas(dd); break;
        case "Occupancy":        ccOccupancy.updateCanvas(dd); break;
        case "Pedestal":          ccPedestal.updateCanvas(dd); break;
        case "SPE":                    ccSpe.updateCanvas(dd); break; 
        case "Calibration":          ccCalib.updateCanvas(dd); break; 
        case "HV":                      ccHv.updateCanvas(dd); break;
        case "Scalers":            ccScalers.updateCanvas(dd);
        }                       
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ccHv.loadHV(is1,is2,il1,il2);
    }
    
    @Override
    public void resetEventListener() {
    }

    @Override
    public void timerUpdate() {
    }
    
    @Override
    public void readHipoFile() {        
        System.out.println("monitor.readHipoFile()");
        String hipoFileName = app.hipoPath+mondet+"_"+app.runNumber+".hipo";
        System.out.println("Reading Histograms from "+hipoFileName);
        ccPix.initHistograms(hipoFileName);
        ccOccupancy.analyze();
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println("monitor.writeHipoFile()");
        String hipoFileName = app.hipoPath+mondet+"_"+app.runNumber+".hipo";
        System.out.println("Writing Histograms to "+hipoFileName);
        HipoFile histofile = new HipoFile(hipoFileName);
        histofile.addToMap("H2_CCa_Hist", this.H2_CCa_Hist);
        histofile.addToMap("H2_CCt_Hist", this.H2_CCt_Hist);
        histofile.writeHipoFile(hipoFileName);
    }
    
    @Override
    public void close() {
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
        if (app.xMsgHost=="localhost") {ccHv.online=false ; ccScalers.online=false;}
        if ( doEpics) {ccHv.startEPICS(); ccScalers.startEPICS();}
        if (!doEpics) {ccHv.stopEPICS();  ccScalers.stopEPICS();}		
	}
    
}
