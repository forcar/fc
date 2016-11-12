package org.clas.fcmon.ec;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.clas.fcmon.tools.*;

//clas12
import org.jlab.clas12.detector.FADCConfigLoader;
import org.jlab.clasrec.utils.DatabaseConstantProvider;

//clas12rec
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.base.DataEvent;

import org.jlab.service.ec.*;
import org.jlab.rec.ecn.*;
 
import java.util.TreeMap;

public class ECMon extends DetectorMonitor {
	
    static MonitorApp           app = new MonitorApp("ECMon",1800,950);	
    
    ECPixels                ecPix[] = new ECPixels[3];
    FADCConfigLoader          fadc  = new FADCConfigLoader();
 
    ECDetector                ecDet = null;
    
    ECReconstructionApp     ecRecon = null;
    ECMode1App              ecMode1 = null;
    ECSingleEventApp  ecSingleEvent = null;
    ECAdcApp                  ecAdc = null;
    ECTdcApp                  ecTdc = null;
    ECCalibrationApp        ecCalib = null;
    ECPedestalApp        ecPedestal = null;
    ECPixelsApp            ecPixels = null;
    ECScalersApp          ecScalers = null;
    ECHvApp                    ecHv = null;   
    
    ECEngine                  ecEng = null;
    ECDetectorReconstruction  ecRec = null;
    DatabaseConstantProvider   ccdb = null;
   
    public boolean             inMC = false;  //true=MC false=DATA
    public boolean            inCRT = false;  //true=CRT pre-installation CRT data
    public boolean            doRec = false;  //true=2.4 EC processor
    public boolean            doEng = false;  //true=3.0 EC processor
    public String            config = "muon"; //configs: pizero,phot,muon,elec
    public int               calRun = 2;
    public int            inProcess = 0;      //0=init 1=processing 2=end-of-run 3=post-run
    int                       detID = 0;
    int                         is1 = 2 ;
    int                         is2 = 3 ;  
    int    nsa,nsb,tet,p1,p2,pedref = 0;
    double               PCMon_zmin = 0;
    double               PCMon_zmax = 0;
   
    String                   mondet = "EC";
    String                 detnam[] = {"PCAL","ECin","ECout"};
        
    TreeMap<String,Object> glob = new TreeMap<String,Object>();
   
    public ECMon(String det) {
        super("ECMON","1.0","lcsmith");
        mondet = det;
        ecPix[0] = new ECPixels("PCAL");
        ecPix[1] = new ECPixels("ECin");
        ecPix[2] = new ECPixels("ECout");
        ccdb = new DatabaseConstantProvider(calRun,"default");
        ccdb.loadTable("/calibration/ec/attenuation");
        ccdb.disconnect();
    }
	
    public static void main(String[] args){
        String det = "PCAL";
        ECMon monitor = new ECMon(det);		
        app.setPluginClass(monitor);
        app.getEnv();
        app.makeGUI();
        app.mode7Emulation.init("/daq/fadc/ec",3, 3, 1);
        monitor.initConstants();
        monitor.initGlob();
        monitor.makeApps();
        monitor.addCanvas();
        monitor.init();
        monitor.initDetector();
        app.init();
        monitor.ecDet.initButtons();
    }
    
    public void initConstants() {
        ECConstants.setSectors(is1,is2);
    }
	
    public void initDetector() {
        System.out.println("monitor.initDetector()"); 
        ecDet = new ECDetector("ECDet",ecPix);
        ecDet.setMonitoringClass(this);
        ecDet.setApplicationClass(app);
        ecDet.init();
    }
    
    public void makeApps() {
        System.out.println("monitor.makeApps()");   
        
        if (doEng) ecEng   = new ECEngine();
        if (doRec) ecRec   = new ECDetectorReconstruction();
        
        ecRecon = new ECReconstructionApp("ECREC",ecPix);        
        ecRecon.setMonitoringClass(this);
        ecRecon.setApplicationClass(app);
        
        ecMode1 = new ECMode1App("Mode1",ecPix);
        ecMode1.setMonitoringClass(this);
        ecMode1.setApplicationClass(app);
        
        ecSingleEvent = new ECSingleEventApp("SingleEvent",ecPix);
        ecSingleEvent.setMonitoringClass(this);
        ecSingleEvent.setApplicationClass(app);
        
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
        ecCalib.init(is1,is2);    
                
        ecHv = new ECHvApp("HV","EC");
        ecHv.setMonitoringClass(this);
        ecHv.setApplicationClass(app);  
        
        ecScalers = new ECScalersApp("Scalers","EC");
        ecScalers.setMonitoringClass(this);
        ecScalers.setApplicationClass(app);     
        
    }
    
    public void addCanvas() {
        System.out.println("monitor.addCanvas()"); 
        app.addFrame(ecMode1.getName(),             ecMode1.getPanel());
        app.addFrame(ecSingleEvent.getName(), ecSingleEvent.getPanel());
        app.addCanvas(ecAdc.getName(),                ecAdc.getCanvas());          
        app.addCanvas(ecTdc.getName(),                ecTdc.getCanvas());          
        app.addCanvas(ecPedestal.getName(),      ecPedestal.getCanvas());         
        app.addCanvas(ecPixels.getName(),          ecPixels.getCanvas());         
        app.addFrame(ecCalib.getName(),             ecCalib.getCalibPane());
        app.addFrame(ecHv.getName(),                   ecHv.getScalerPane());
        app.addFrame(ecScalers.getName(),         ecScalers.getScalerPane());        
    }
	
    public void init( ) {	    
        System.out.println("monitor.init()");	
        inProcess = 0; putGlob("inProcess", inProcess);
        initApps();
        for (int i=0; i<ecPix.length; i++) ecPix[i].initHistograms(" ");
    }

    public void initApps() {
        System.out.println("monitor.initApps()");
        System.out.println("Configuration: "+config);
        for (int i=0; i<ecPix.length; i++)   ecPix[i].init();
        ecRecon.init(); 
        if (doEng) {
        ecEng.init();
        ecEng.setStripThresholds(ecPix[0].getStripThr(config, 1),
                                 ecPix[1].getStripThr(config, 1),
                                 ecPix[2].getStripThr(config, 1));  
        ecEng.setPeakThresholds(ecPix[0].getPeakThr(config, 1),
                                ecPix[1].getPeakThr(config, 1),
                                ecPix[2].getPeakThr(config, 1));  
        ecEng.setClusterCuts(ecPix[0].getClusterErr(config),
                             ecPix[1].getClusterErr(config),
                             ecPix[2].getClusterErr(config));
        putGlob("ecEng",ecEng.getHist());
        }
        if (doRec) {
        ecRec.init();
        ecRec.setStripThresholds(ecPix[0].getStripThr(config, 1),
                                 ecPix[1].getStripThr(config, 1),
                                 ecPix[2].getStripThr(config, 1));  
        ecRec.setPeakThresholds(ecPix[0].getPeakThr(config, 1),
                                ecPix[1].getPeakThr(config, 1),
                                ecPix[2].getPeakThr(config, 1));  
        }
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_a.add(0,0,0, ecRecon.toTreeMap(ecPix[i].ec_cmap));
        for (int i=0; i<ecPix.length; i++)   ecPix[i].Lmap_a.add(0,0,1, ecRecon.toTreeMap(ecPix[i].ec_zmap));
        if (app.doEpics) {
            System.out.println("monitor.initApps():Intializing scalers");
            ecHv.init();        
            ecScalers.init(); 
        }          
    }
	
    public void initGlob() {
        System.out.println("monitor.initGlob()");
        putGlob("detID", detID);
        putGlob("inMC", inMC);
        putGlob("inCRT",inCRT);
        putGlob("doRec",doRec);
        putGlob("doEng",doEng);
        putGlob("nsa", nsa);
        putGlob("nsb", nsb);
        putGlob("tet", tet);		
        putGlob("ccdb", ccdb);
        putGlob("PCMon_zmin", PCMon_zmin);
        putGlob("PCMon_zmax", PCMon_zmax);
        putGlob("fadc",fadc);
        putGlob("mondet",mondet);
        putGlob("is1",ECConstants.IS1);
        putGlob("config",config);
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
		
    }
	
    @Override
    public void close() {
	    
    } 

    @Override
    public void dataEventAction(DataEvent de) {        
      if(doEng) {ecEng.singleEvent=app.isSingleEvent() ; ecEng.debug = app.debug; ecEng.processDataEvent(de);} 
      if(doRec) ecRec.processEvent((EvioDataEvent)de);      
      ecRecon.addEvent(de);
    }
    
    @Override    
    public void update(DetectorShape2D shape) {
        putGlob("inProcess", inProcess);
        ecDet.update(shape);
//      ecCalib.updateDetectorView(shape);
    }
    
	@Override
	public void analyze(int process) {		
		this.inProcess = process; glob.put("inProcess", process);
		switch (inProcess) {
			case 1: 
			    for (int idet=0; idet<ecPix.length; idet++) ecRecon.makeMaps(idet); 
			    break;
			case 2: 
			    for (int idet=0; idet<ecPix.length; idet++) ecRecon.makeMaps(idet);
				// Final analysis of full detector at end of run
				for (int idet=0; idet<ecPix.length; idet++) ecCalib.analyze(idet,is1,is2,1,4);
		        inProcess=3; glob.put("inProcess", process);
		}
	}
	
    @Override
    public void processShape(DetectorShape2D shape) {		
        DetectorDescriptor dd = shape.getDescriptor();
        this.analyze(inProcess);	
        switch (app.getSelectedTabName()) {
        case "Mode1":                       ecMode1.updateCanvas(dd); break;
        case "SingleEvent":           ecSingleEvent.updateCanvas(dd); break;
        case "ADC":                           ecAdc.updateCanvas(dd); break;
        case "TDC":                           ecTdc.updateCanvas(dd); break;
        case "Pedestal":                 ecPedestal.updateCanvas(dd); break;
        case "Pixels":                     ecPixels.updateCanvas(dd); break;
        case "Calibration":                 ecCalib.updateCanvas(dd); break;
        case "HV":        if(app.doEpics)      ecHv.updateCanvas(dd); break;
        case "Scalers":   if(app.doEpics) ecScalers.updateCanvas(dd);
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
            ecPix[idet].initHistograms(hipoFileName);
            ecRecon.makeMaps(idet);
          }
          inProcess = 2;          
    }
    
    @Override
    public void saveToFile() {
        for (int idet=0; idet<3; idet++) {
            String hipoFileName = app.hipoPath+"/"+mondet+idet+"_"+app.calibRun+".hipo";
            System.out.println("Saving Histograms to "+hipoFileName);
            HipoFile histofile = new HipoFile(hipoFileName);
            histofile.addToMap("H2_a_Hist", ecPix[idet].strips.hmap2.get("H2_a_Hist")); 
            histofile.addToMap("H1_a_Hist", ecPix[idet].strips.hmap1.get("H1_a_Hist")); 
            histofile.addToMap("H1_a_Maps", ecPix[idet].pixels.hmap1.get("H1_a_Maps"));
            histofile.addToMap("H2_t_Hist", ecPix[idet].strips.hmap2.get("H2_t_Hist"));
            histofile.addToMap("H1_t_Maps", ecPix[idet].pixels.hmap1.get("H1_t_Maps"));
            histofile.writeHipoFile(hipoFileName);
        }
    }	
}
