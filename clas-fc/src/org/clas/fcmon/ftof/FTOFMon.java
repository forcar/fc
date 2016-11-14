package org.clas.fcmon.ftof;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.base.DataEvent;

public class FTOFMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("FTOFMon",1800,950);	
    
    FTOFPixels              ftofPix[] = new FTOFPixels[3];
    ConstantsManager             ccdb = new ConstantsManager();
      
    FTOFDetector              ftofDet = null;  
    
    FTOFReconstructionApp   ftofRecon = null;
    FTOFMode1App            ftofMode1 = null;
    FTOFAdcApp                ftofAdc = null;
    FTOFTdcApp                ftofTdc = null;
    FTOFPedestalApp      ftofPedestal = null;
    FTOFMipApp                ftofMip = null;    
    FTOFCalibrationApp      ftofCalib = null;
    FTOFScalersApp        ftofScalers = null;
    FTOFHvApp                  ftofHv = null;
        
    public boolean               inMC = false; //true=MC false=DATA
    public int              inProcess = 0;    //0=init 1=processing 2=end-of-run 3=post-run
    int                         detID = 0;
    int                           is1 = 2;    //All sectors: is1=1 is2=7  Single sector: is1=s is2=s+1
    int                           is2 = 3; 
    int                        calrun = 2;
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
    
    String mondet                     = "FTOF";
	
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
	   
    public FTOFMon(String det) {
        super("FTOFMON", "1.0", "lcsmith");
        mondet = det;
        ftofPix[0] = new FTOFPixels("PANEL1A");
        ftofPix[1] = new FTOFPixels("PANEL1B");
        ftofPix[2] = new FTOFPixels("PANEL2");
    }

    public static void main(String[] args){		
        String det = "FTOF";
        FTOFMon monitor = new FTOFMon(det);	
        app.setPluginClass(monitor);
        app.getEnv();
        app.makeGUI();
        monitor.initCCDB();
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.init();
        monitor.initDetector();
        app.init();
        monitor.ftofDet.initButtons();
    }
    
    public void initCCDB() {
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ftof",
                "/calibration/ftof/attenuation","/calibration/ftof/gain_balance","/calibration/ftof/status"}));
        app.mode7Emulation.init(ccdb, calrun, "/daq/fadc/ftof", 5,3,1);        
    }
    
    public void initDetector() {
        System.out.println("monitor.initDetector()"); 
        ftofDet = new FTOFDetector("FTOFDet",ftofPix);
        ftofDet.setMonitoringClass(this);
        ftofDet.setApplicationClass(app);
        ftofDet.init(is1,is2);
    }
	
    public void makeApps() {
        System.out.println("monitor.makeApps()"); 
        ftofRecon = new FTOFReconstructionApp("FTOFREC",ftofPix);        
        ftofRecon.setMonitoringClass(this);
        ftofRecon.setApplicationClass(app);	
        
        ftofMode1 = new FTOFMode1App("Mode1",ftofPix);        
        ftofMode1.setMonitoringClass(this);
        ftofMode1.setApplicationClass(app);   
        
        ftofAdc = new FTOFAdcApp("ADC",ftofPix);        
        ftofAdc.setMonitoringClass(this);
        ftofAdc.setApplicationClass(app);     
        
        ftofTdc = new FTOFTdcApp("TDC",ftofPix);        
        ftofTdc.setMonitoringClass(this);
        ftofTdc.setApplicationClass(app);           
        
        ftofPedestal = new FTOFPedestalApp("Pedestal",ftofPix);        
        ftofPedestal.setMonitoringClass(this);
        ftofPedestal.setApplicationClass(app);       
        
        ftofMip = new FTOFMipApp("MIP",ftofPix);        
        ftofMip.setMonitoringClass(this);
        ftofMip.setApplicationClass(app);  
        
        ftofCalib = new FTOFCalibrationApp("Calibration", ftofPix);
        ftofCalib.setMonitoringClass(this);
        ftofCalib.setApplicationClass(app);  
        ftofCalib.init(is1,is2);
        
        ftofHv = new FTOFHvApp("HV","FTOF");
        ftofHv.setMonitoringClass(this);
        ftofHv.setApplicationClass(app);  
        
        ftofScalers = new FTOFScalersApp("Scalers","FTOF");
        ftofScalers.setMonitoringClass(this);
        ftofScalers.setApplicationClass(app);  
    }
	
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addFrame(ftofMode1.getName(),          ftofMode1.getPanel());
        app.addCanvas(ftofAdc.getName(),             ftofAdc.getCanvas());          
        app.addCanvas(ftofTdc.getName(),             ftofTdc.getCanvas());          
        app.addCanvas(ftofPedestal.getName(),   ftofPedestal.getCanvas());
        app.addCanvas(ftofMip.getName(),             ftofMip.getCanvas()); 
        app.addFrame(ftofCalib.getName(),          ftofCalib.getCalibPane());
        app.addFrame(ftofHv.getName(),                ftofHv.getScalerPane());
        app.addFrame(ftofScalers.getName(),      ftofScalers.getScalerPane());
    }
    
    public void init( ) {       
        System.out.println("monitor.init()");   
        inProcess = 0; putGlob("inProcess", inProcess);
        initApps();
        for (int i=0; i<ftofPix.length; i++) ftofPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        for (int i=0; i<ftofPix.length; i++)   ftofPix[i].init();
        ftofRecon.init();
        if (app.doEpics) {
          ftofHv.init(is1,is2);        
          ftofScalers.init(is1,is2); 
        }
    }
    
    public void initGlob() {
        System.out.println("monitor.initGlob()");
        putGlob("inProcess", inProcess);
        putGlob("detID", detID);
        putGlob("inMC", inMC);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);        
        putGlob("ccdb", ccdb);
        putGlob("zmin", PCMon_zmin);
        putGlob("zmax", PCMon_zmax);
        putGlob("mondet",mondet);
        putGlob("is1",is1);
        putGlob("is2",is2);
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
        ftofRecon.clearHistograms();
    }
	
    @Override
    public void close() {	
    }	
	
    @Override
    public void dataEventAction(DataEvent de) {
        ftofRecon.addEvent((EvioDataEvent) de);	
    }

    @Override
    public void update(DetectorShape2D shape) {
        putGlob("inProcess", inProcess);
        ftofDet.update(shape);
//        ftofCalib.updateDetectorView(shape);
    }
		
    @Override
    public void analyze(int process) {
        this.inProcess = process; glob.put("inProcess", process);
        if (process==1||process==2) {
            ftofRecon.makeMaps();	
            ftofCalib.engines[0].analyze();
        }
    }

    @Override
    public void processShape(DetectorShape2D shape) { 
        DetectorDescriptor dd = shape.getDescriptor();
        this.analyze(inProcess);       
        switch (app.getSelectedTabName()) {
        case "Mode1":                        ftofMode1.updateCanvas(dd); break;
        case "ADC":                            ftofAdc.updateCanvas(dd); break;
        case "TDC":                            ftofTdc.updateCanvas(dd); break;
        case "Pedestal":                  ftofPedestal.updateCanvas(dd); break;
        case "MIP":                            ftofMip.updateCanvas(dd); break; 
        case "HV":         if(app.doEpics)      ftofHv.updateCanvas(dd); break;
        case "Scalers":    if(app.doEpics) ftofScalers.updateCanvas(dd);
       } 
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
        for (int idet=0; idet<3; idet++) {
          String hipoFileName = app.hipoPath+"/"+mondet+idet+"_"+app.calibRun+".hipo";
          System.out.println("Loading Histograms from "+hipoFileName);
          ftofPix[idet].initHistograms(hipoFileName);
          ftofRecon.makeMaps();
        }
        inProcess = 2;          
    }
    
    @Override
    public void saveToFile() {
        System.out.println("monitor.saveToFile()");
        for (int idet=0; idet<3; idet++) {
          String hipoFileName = app.hipoPath+"/"+mondet+idet+"_"+app.calibRun+".hipo";
          System.out.println("Saving Histograms to "+hipoFileName);
          HipoFile histofile = new HipoFile(hipoFileName);
          histofile.addToMap("H2_a_Hist",ftofPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",ftofPix[idet].strips.hmap2.get("H2_t_Hist"));
          histofile.writeHipoFile(hipoFileName);
        }
    }
    
}
