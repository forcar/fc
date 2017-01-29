package org.clas.fcmon.ftof;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.containers.FTHashCollection;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedTable;
import org.jlab.io.base.DataEvent;

public class FTOFMon extends DetectorMonitor {
	
    static MonitorApp             app = new MonitorApp("FTOFMon",1800,950);	
    
    FTOFPixels              ftofPix[] = new FTOFPixels[3];
    ConstantsManager             ccdb = new ConstantsManager();
    FTHashCollection              rtt = null;      
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
       
    public int                 calRun = 12;
    int                         detID = 0;
    int                           is1 = 2;    //All sectors: is1=1 is2=7  Single sector: is1=s is2=s+1
    int                           is2 = 3; 
    int      nsa,nsb,tet,p1,p2,pedref = 0;
    double                 PCMon_zmin = 0;
    double                 PCMon_zmax = 0;
    
    String mondet                     = "FTOF";
    static String             appname = "FTOFMON";
	
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
        monitor.initEPICS();
        monitor.init();
        monitor.initDetector();
        app.init();
        app.getDetectorView().setFPS(10);
        app.setSelectedTab(2); 
        monitor.ftofDet.initButtons();
    }
    
    public FTHashCollection getReverseTT(ConstantsManager ccdb) {
        System.out.println("monitor.getReverseTT()"); 
        IndexedTable tt = ccdb.getConstants(10,  "/daq/tt/ftof");
        FTHashCollection rtt = new FTHashCollection<int[]>(4);
        for(int ic=1; ic<35; ic++) {
            for (int sl=3; sl<19; sl++) {
                int chmax=16;
                if (sl==6||sl==16) chmax=128;
                for (int ch=0; ch<chmax; ch++){
                    if (tt.hasEntry(ic,sl,ch)) {
                        int[] dum = {ic,sl,ch}; rtt.add(dum,tt.getIntValue("sector",    ic,sl,ch),
                                                            tt.getIntValue("layer",     ic,sl,ch),
                                                            tt.getIntValue("component", ic,sl,ch),
                                                            tt.getIntValue("order",     ic,sl,ch));
                    };
                }
            }
        }
        return rtt;
    }
    
    public void initConstants() {
        FTOFConstants.setSectors(is1,is2);
    }   
    
    public void initCCDB() {
        ccdb.init(Arrays.asList(new String[]{
                "/daq/fadc/ftof",
                "/daq/tt/ftof",
                "/calibration/ftof/attenuation",
                "/calibration/ftof/gain_balance",
                "/calibration/ftof/status"}));
        rtt = getReverseTT(ccdb); 
        app.mode7Emulation.init(ccdb,calRun,"/daq/fadc/ftof", 3,3,1);        
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
        app.addFrame(ftofHv.getName(),                ftofHv.getPanel());
        app.addFrame(ftofScalers.getName(),      ftofScalers.getPanel());
    }
    
    public void init( ) {       
        System.out.println("monitor.init()");   
        app.setInProcess(0);
        initApps();
        for (int i=0; i<ftofPix.length; i++) ftofPix[i].initHistograms(" "); 
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        for (int i=0; i<ftofPix.length; i++)   ftofPix[i].init();
        ftofRecon.init();
    }
    
    public void initEPICS() {
        System.out.println("monitor.initScalers():Initializing EPICS Channel Access");
        ftofHv.init(app.doEpics);        
        ftofScalers.init(app.doEpics);         
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
    public void dataEventAction(DataEvent de) {
        ftofRecon.addEvent((EvioDataEvent) de);	
    }

    @Override
    public void update(DetectorShape2D shape) {
        ftofDet.update(shape);
//        ftofCalib.updateDetectorView(shape);
    }
		
    @Override
    public void analyze() {
        if (app.getInProcess()==1||app.getInProcess()==2) {
            ftofRecon.makeMaps();	
            ftofCalib.engines[0].analyze();
        }
    }

    @Override
    public void processShape(DetectorShape2D shape) { 
        DetectorDescriptor dd = shape.getDescriptor();
        app.updateStatus(getStatusString(dd));
        this.analyze();       
        switch (app.getSelectedTabName()) {
        case "Mode1":                        ftofMode1.updateCanvas(dd); break;
        case "ADC":                            ftofAdc.updateCanvas(dd); break;
        case "TDC":                            ftofTdc.updateCanvas(dd); break;
        case "Pedestal":                  ftofPedestal.updateCanvas(dd); break;
        case "MIP":                            ftofMip.updateCanvas(dd); break; 
        case "HV":                              ftofHv.updateCanvas(dd); break;
        case "Scalers":                    ftofScalers.updateCanvas(dd);
       } 
    }
    
    public void loadHV(int is1, int is2, int il1, int il2) {
        ftofHv.loadHV(is1,is2,il1,il2);
    }  
    
    public String getStatusString(DetectorDescriptor dd) {
        
        String comp=(dd.getLayer()==4) ? "  Pixel:":"  PMT:";  
      
        int is = dd.getSector();
        int sp = app.viewIndex+2*app.detectorIndex;
        int ic = dd.getComponent()+1;
        int or = 0;
        int cr = 0;
        int sl = 0;
        int ch = 0;
        if (app.getSelectedTabName()=="TDC") or=2;
        if (rtt.hasItem(is,sp,ic,or)) {
            int[] dum = (int[]) rtt.getItem(is,sp,ic,or);
            cr = dum[0];
            sl = dum[1];
            ch = dum[2];
        }   
        return " Sector:"+is+"  Layer:"+sp+comp+ic+" "+" Crate:"+cr+" Slot:"+sl+" Chan:"+ch;
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
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Reading Histograms from "+hipoFileName);
          ftofPix[idet].initHistograms(hipoFileName);
          ftofRecon.makeMaps();
        }
        app.setInProcess(2);          
    }
    
    @Override
    public void writeHipoFile() {
        System.out.println("monitor.writeHipoFile()");
        for (int idet=0; idet<3; idet++) {
          String hipoFileName = app.hipoPath+mondet+idet+"_"+app.runNumber+".hipo";
          System.out.println("Writing Histograms to "+hipoFileName);
          HipoFile histofile = new HipoFile(hipoFileName);
          histofile.addToMap("H2_a_Hist",ftofPix[idet].strips.hmap2.get("H2_a_Hist"));
          histofile.addToMap("H2_t_Hist",ftofPix[idet].strips.hmap2.get("H2_t_Hist"));
          histofile.writeHipoFile(hipoFileName);
        }
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
    
}
