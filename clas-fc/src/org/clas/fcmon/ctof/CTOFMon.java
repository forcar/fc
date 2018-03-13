package org.clas.fcmon.ctof;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.io.base.DataEvent;

public class CTOFMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("CTOFMon",1800,950);	
   
    CTOFPixels              ctofPix[] = new CTOFPixels[1];
    ConstantsManager             ccdb = new ConstantsManager();
    FTHashCollection              rtt = null;      
    CTOFDet                   ctofDet = null;  
    
    CTOFReconstructionApp   ctofRecon = null;
    CTOFMode1App            ctofMode1 = null;
    CTOFAdcApp                ctofAdc = null;
    CTOFTdcApp                ctofTdc = null;
    CTOFPedestalApp      ctofPedestal = null;
    CTOFMipApp                ctofMip = null;    
    CTOFCalibrationApp      ctofCalib = null;
    CTOFScalersApp        ctofScalers = null;
    CTOFHvApp                  ctofHv = null;
       
    public int                 calRun = 12;
    int                         detID = 0;
    int                           is1 = 1;    //All sectors: is1=1 is2=7  Single sector: is1=s is2=s+1
    int                           is2 = 2; 
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
   
    String mondet                     = "CTOF";
    static String             appname = "CTOFMON";
	
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public CTOFMon(String det) {
        super(appname, "1.0", "lcsmith");
        mondet = det;
        ctofPix[0] = new CTOFPixels("CTOF");
    }

    public static void main(String[] args){		
        String det = "CTOF";
        CTOFMon monitor = new CTOFMon(det);	
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
        app.setSelectedTab(2); 
        monitor.ctofDet.initButtons();
    }
    
    public void initConstants() {
        CTOFConstants.setSectorRange(is1,is2);
    }   
    
    public void initCCDB() {
        System.out.println("monitor.initCCDB()"); 
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ctof",
                "/daq/tt/ctof",
                "/calibration/ctof/attenuation",
                "/calibration/ctof/gain_balance",
                "/calibration/ctof/status"}));
        app.getReverseTT(ccdb,"/daq/tt/ctof"); 
        app.mode7Emulation.init(ccdb,calRun,"/daq/fadc/ctof", 59,3,1);        
    } 
    
    public void initDetector() {
        System.out.println("monitor.initDetector()"); 
        ctofDet = new CTOFDet("CTOFDet",ctofPix);
        ctofDet.setMonitoringClass(this);
        ctofDet.setApplicationClass(app);
        ctofDet.init();
    }
	
    public void makeApps() {
        System.out.println("monitor.makeApps()"); 
        ctofRecon = new CTOFReconstructionApp("CTOFREC",ctofPix);        
        ctofRecon.setMonitoringClass(this);
        ctofRecon.setApplicationClass(app);	
        
        ctofMode1 = new CTOFMode1App("Mode1",ctofPix);        
        ctofMode1.setMonitoringClass(this);
        ctofMode1.setApplicationClass(app);   
        
        ctofAdc = new CTOFAdcApp("ADC",ctofPix);        
        ctofAdc.setMonitoringClass(this);
        ctofAdc.setApplicationClass(app);     
        
        ctofTdc = new CTOFTdcApp("TDC",ctofPix);        
        ctofTdc.setMonitoringClass(this);
        ctofTdc.setApplicationClass(app);           
        
        ctofPedestal = new CTOFPedestalApp("Pedestal",ctofPix);        
        ctofPedestal.setMonitoringClass(this);
        ctofPedestal.setApplicationClass(app);       
        
        ctofMip = new CTOFMipApp("MIP",ctofPix);        
        ctofMip.setMonitoringClass(this);
        ctofMip.setApplicationClass(app);  
        
        ctofCalib = new CTOFCalibrationApp("Calibration", ctofPix);
        ctofCalib.setMonitoringClass(this);
        ctofCalib.setApplicationClass(app);  
        ctofCalib.init(is1,is2);
        
        ctofHv = new CTOFHvApp("HV",mondet);
        ctofHv.setMonitoringClass(this);
        ctofHv.setApplicationClass(app);  
        ctofHv.init();
        
        ctofScalers = new CTOFScalersApp("Scalers",mondet);
        ctofScalers.setMonitoringClass(this);
        ctofScalers.setApplicationClass(app); 
        ctofScalers.init();
        
        if(app.xMsgHost=="localhost") app.startEpics();
    }
	
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addFrame(ctofMode1.getName(),          ctofMode1.getPanel());
        app.addCanvas(ctofAdc.getName(),             ctofAdc.getCanvas());          
        app.addCanvas(ctofTdc.getName(),             ctofTdc.getCanvas());          
        app.addCanvas(ctofPedestal.getName(),   ctofPedestal.getCanvas());
        app.addCanvas(ctofMip.getName(),             ctofMip.getCanvas()); 
        app.addFrame(ctofCalib.getName(),          ctofCalib.getCalibPane());
        app.addFrame(ctofHv.getName(),                ctofHv.getPanel());
        app.addFrame(ctofScalers.getName(),      ctofScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println("monitor.init()");   
        app.setInProcess(0);
        initApps();
        for (int i=0; i<ctofPix.length; i++) ctofPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        for (int i=0; i<ctofPix.length; i++) ctofPix[i].init();
        ctofRecon.init();
    }
    
    public void initGlob() {
        System.out.println("monitor.initGlob()");
        putGlob("detID", detID);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("ccdb", ccdb);
        putGlob("zmin", PCMon_zmin);
        putGlob("zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",CTOFConstants.IS1);
        putGlob("is2",CTOFConstants.IS2);
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
        ctofRecon.clearHistograms();
    }
	
    @Override
    public void dataEventAction(DataEvent de) {
        ctofRecon.addEvent(de);	
    }

    @Override
    public void analyze() {
        
        switch (app.getInProcess()) {
        case 1: 
            for (int idet=0; idet<ctofPix.length; idet++) ctofRecon.makeMaps(idet); 
            break;
        case 2:
            for (int idet=0; idet<ctofPix.length; idet++) ctofRecon.makeMaps(idet); 
            System.out.println("End of run");                 
            ctofCalib.engines[0].analyze();
            app.setInProcess(3);
        }
    }
    
    @Override
    public void update(DetectorShape2D shape) {
        //From DetectorView2D.DetectorViewLayer2D.drawLayer: Update color map of shape
        ctofDet.update(shape);
        if (app.getSelectedTabName().equals("Scalers")) ctofScalers.updateDetectorView(shape);
        if (app.getSelectedTabName().equals("HV"))           ctofHv.updateDetectorView(shape);
    }
        
    @Override
    public void processShape(DetectorShape2D shape) { 
        // From updateGUI timer or mouseover : process entering a new detector shape and repaint
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatusString(dd); // For strip/pixel ID and reverse translation table
        this.analyze();  // Refresh color maps      
        switch (app.getSelectedTabName()) {
        case "Mode1":                        ctofMode1.updateCanvas(dd); break;
        case "ADC":                            ctofAdc.updateCanvas(dd); break;
        case "TDC":                            ctofTdc.updateCanvas(dd); break;
        case "Pedestal":                  ctofPedestal.updateCanvas(dd); break;
        case "MIP":                            ctofMip.updateCanvas(dd); break; 
        case "HV":                              ctofHv.updateCanvas(dd); break;
        case "Scalers":                    ctofScalers.updateCanvas(dd);
       } 
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ctofHv.loadHV(is1,is2,il1,il2);
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
        for (int idet=0; idet<ctofPix.length; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Reading Histograms from "+hipoFileName);
          ctofPix[idet].initHistograms(hipoFileName);
        }
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println("monitor.writeHipoFile()");
        for (int idet=0; idet<ctofPix.length; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Writing Histograms to "+hipoFileName);
          HipoFile histofile = new HipoFile(hipoFileName);
          histofile.addToMap("H2_a_Hist",ctofPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",ctofPix[idet].strips.hmap2.get("H2_t_Hist"));
          histofile.writeHipoFile(hipoFileName);
        }
    }
    
    @Override
    public void close() {
        System.out.println("monitor.close()");
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
        if (app.xMsgHost=="localhost") {ctofHv.online=false ; ctofScalers.online=false;}
        if ( doEpics) {ctofHv.startEPICS(); ctofScalers.startEPICS();}
        if (!doEpics) {ctofHv.stopEPICS();  ctofScalers.stopEPICS();}
    }
    
}
