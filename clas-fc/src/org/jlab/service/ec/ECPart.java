package org.jlab.service.ec;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.jlab.clas.detector.DetectorParticle;
import org.jlab.clas.detector.DetectorResponse;
import org.jlab.clas.physics.GenericKinematicFitter;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.clas.physics.Vector3;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Line3D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioSource;
import org.jlab.service.eb.EBConstants;
import org.jlab.service.eb.EventBuilder;


public class ECPart {
	
    EventBuilder builder = new EventBuilder();
    public static double distance11,distance12,distance21,distance22;
    public static double e1,e2,e1c,e2c,cth,cth1,cth2,X,tpi2,cpi0,refE,refP,refTH;
    static double mpi0 = 0.1349764;
    public static String geom = "2.4";
    public static String config = null;
    public static double SF1 = 0.27;
    public static double SF2 = 0.27;
    
    public static void readMC(DataEvent event) {
        int pid=0;
        double ppx=0,ppy=0,ppz=0;
        Boolean isEvio = event instanceof EvioDataEvent;        
        if(isEvio&&event.hasBank("GenPart::true")) {
            EvioDataBank bank = (EvioDataBank) event.getBank("GenPart::true");
            ppx = bank.getDouble("px",0);
            ppy = bank.getDouble("py",0);
            ppz = bank.getDouble("pz",0);
            pid = bank.getInt("pid",0);
        }
        if(!isEvio&&event.hasBank("MC::Particle")) {
            DataBank bank = event.getBank("MC::Particle");
            ppx = bank.getFloat("px",0);
            ppy = bank.getFloat("py",0);
            ppz = bank.getFloat("pz",0);
            pid = bank.getInt("pid",0);                
        }
        double  rm = 0.;
        if (pid==111) rm=mpi0;                   
        refP  = Math.sqrt(ppx*ppx+ppy*ppy+ppz*ppz);  
        refE  = Math.sqrt(refP*refP+rm*rm);            
        refTH = Math.acos(ppz/refP)*180/3.14159;        
    }
    
    public static List<DetectorResponse> readEvioEvent(DataEvent event, String bankName, DetectorType type) {
        List<DetectorResponse> responseList = new ArrayList<DetectorResponse>();
        if(event.hasBank(bankName)==true){
            EvioDataBank bank = (EvioDataBank) event.getBank(bankName);
            int nrows = bank.rows();
            for(int row = 0; row < nrows; row++){
                int sector = bank.getInt("sector", row);
                int  layer = bank.getInt("layer",  row);
                DetectorResponse  response = new DetectorResponse(sector,layer,0);
                response.getDescriptor().setType(type);
                double x = bank.getDouble("X", row);
                double y = bank.getDouble("Y", row);
                double z = bank.getDouble("Z", row);
                response.setPosition(x, y, z);
                response.setEnergy(bank.getDouble("energy", row));
                response.setTime(bank.getDouble("time", row));
                responseList.add(response);
            }
        }
        return responseList;                      
    }
    
    public List<DetectorResponse>  readEC(DataEvent event){
        List<DetectorResponse>  ecResponse = new ArrayList<DetectorResponse>();
        Boolean isEvio = event instanceof EvioDataEvent;                  
        if (isEvio) ecResponse =                  readEvioEvent(event, "ECDetector::clusters", DetectorType.EC); 
        if(!isEvio) ecResponse = DetectorResponse.readHipoEvent(event, "ECAL::clusters", DetectorType.EC);

        return ecResponse;
    } 
    
    public double getTwoPhoton(List<DetectorResponse> response, int sector){
        
        List<DetectorResponse> rPCAL = builder.getUnmatchedResponses(response, DetectorType.EC, 1);
        List<DetectorResponse> rSectorPCAL = DetectorResponse.getListBySector(rPCAL, DetectorType.EC, sector);
        if (rSectorPCAL.size()!=2) return -1;
        return processNeutralTracks(doHitMatching(response,sector));
    }
     
    public List<DetectorParticle> doHitMatching(List<DetectorResponse> response, int sector) {
        
        List<DetectorParticle>  particles = new ArrayList<DetectorParticle>();
        
        List<DetectorResponse>    rPC = builder.getUnmatchedResponses(response, DetectorType.EC,1);
        List<DetectorResponse>   rECi = builder.getUnmatchedResponses(response, DetectorType.EC,4);
        List<DetectorResponse>   rECo = builder.getUnmatchedResponses(response, DetectorType.EC,7);
        List<DetectorResponse>  rPCAL = DetectorResponse.getListBySector(rPC,  DetectorType.EC, sector);
        List<DetectorResponse>  rECIN = DetectorResponse.getListBySector(rECi, DetectorType.EC, sector);
        List<DetectorResponse> rECOUT = DetectorResponse.getListBySector(rECo, DetectorType.EC, sector);
                
        distance11=distance12=distance21=distance22=-10;
                        
        for(int i = 0; i < rPCAL.size(); i++){
            particles.add(DetectorParticle.createNeutral(rPCAL.get(i)));
        }
                        
        DetectorParticle p1 = particles.get(0);  //PCAL Photon 1
        DetectorParticle p2 = particles.get(1);  //PCAL Photon 2
        
        int index=0;
        
        index  = p1.getDetectorHit(rECIN,DetectorType.EC,4,EBConstants.ECIN_MATCHING);
        if(index>=0){p1.addResponse(rECIN.get(index),true); rECIN.get(index).setAssociation(0);
        distance11 = p1.getDistance(rECIN.get(index)).length();}
        
        index  = p1.getDetectorHit(rECOUT,DetectorType.EC,7,EBConstants.ECOUT_MATCHING);
        if(index>=0){p1.addResponse(rECOUT.get(index),true); rECOUT.get(index).setAssociation(0);
        distance12 = p1.getDistance(rECOUT.get(index)).length();}
        
        index  = p2.getDetectorHit(rECIN,DetectorType.EC,4,EBConstants.ECIN_MATCHING);
        if(index>=0){p2.addResponse(rECIN.get(index),true); rECIN.get(index).setAssociation(1);
        distance21 = p2.getDistance(rECIN.get(index)).length();}
        
        index  = p2.getDetectorHit(rECOUT,DetectorType.EC,7,EBConstants.ECOUT_MATCHING);
        if(index>=0){p2.addResponse(rECOUT.get(index),true); rECOUT.get(index).setAssociation(1);
        distance22 = p2.getDistance(rECOUT.get(index)).length();}
        
        return particles;
    }
       
    public double processNeutralTracks(List<DetectorParticle> particles) {
        
        DetectorParticle p1 = particles.get(0);  //Photon 1
        DetectorParticle p2 = particles.get(1);  //Photon 2
        
        Vector3 n1 = p1.vector(); n1.unit();
        Vector3 n2 = p2.vector(); n2.unit();
                
        e1 = p1.getEnergy(DetectorType.EC);
        e2 = p2.getEnergy(DetectorType.EC);

        SF1 = getSF(geom,e1); e1c=e1/SF1;
        Particle g1 = new Particle(22,
                n1.x()*e1c,
                n1.y()*e1c,
                n1.z()*e1c
        );
        
        SF2 = getSF(geom,e2); e2c = e2/SF2;
        Particle g2 = new Particle(22,
                n2.x()*e2c,
                n2.y()*e2c,
                n2.z()*e2c
        );
        
        X    =  1e3;
        tpi2 =  1e9;
        cpi0 =  -1;
        cth  =  -1;
        cth1 = Math.cos(g1.theta());
        cth2 = Math.cos(g2.theta());
         cth = g1.cosTheta(g2);
         
        // Require 2 photons in PCAL and ECinner
         
        if(p1.getResponse(DetectorType.EC, 1)!=null&&
           p1.getResponse(DetectorType.EC, 4)!=null&&
           p2.getResponse(DetectorType.EC, 1)!=null&& 
           p2.getResponse(DetectorType.EC, 4)!=null) {                
              X = (e1c-e2c)/(e1c+e2c);
           tpi2 = 2*mpi0*mpi0/(1-cth)/(1-X*X);
           cpi0 = (e1c*cth1+e2c*cth2)/Math.sqrt(e1c*e1c+e2c*e2c+2*e1c*e2c*cth);
           g1.combine(g2, +1);
           return g1.mass2();
        }
        
        return  0.0;

        //System.out.println("  ENERGIES = " + (e1/0.27) + "  " + (e2/0.27));
        //System.out.println(particles.get(0));
        //System.out.println(particles.get(1));
        //System.out.println(gen);
    }
    
    public void setGeom(String geom) {
        this.geom = geom;
    }
    
    public void setConfig(String config) {
        this.config = config;
    }
    
    public static double getSF(String geom, double e) {
        switch (geom) {
        case "2.4": return 0.268*(1.0510 - 0.0104/e - 0.00008/e/e); 
        case "2.5": return 0.250*(1.0286 - 0.0150/e + 0.00012/e/e);
        }
        return Double.parseDouble(geom);
    }    
    
    public static void main(String[] args){
        
        ECEngine   engine = new ECEngine();
        EvioSource reader = new EvioSource();
        ECPart       part = new ECPart();
        
        String evioPath = "/Users/colesmith/coatjava/data/pizero/";
        // GEMC file: 10k 2.0 GeV pizeros thrown at 25 deg into Sector 2 using GEMC 2.4 geometry
        // JLAB: evioPath = "/lustre/expphy/work/hallb/clas12/lcsmith/clas12/forcar/gemc/evio/";
        
        if (args.length == 0) { 
//            reader.open(evioPath+"fc-pizero-10k-s2-25deg-oldgeom.evio");
            reader.open(evioPath+"fc-pizero-10k-s2-newgeom.evio");
        } else {
            String inputFile = args[0];
            reader.open(inputFile);
        }
                
        engine.init();
        engine.isMC = true;
        engine.setVariation("clas6");
        engine.setCalRun(2);
        engine.setStripThresholds(10,9,8);
        engine.setPeakThresholds(18,20,15);
        engine.setClusterCuts(7,15,20);
        part.setGeom("2.5");
        
        H1F h1 = new H1F("Invariant Mass",50,10.,200);         
        h1.setOptStat(Integer.parseInt("1100")); h1.setTitleX("Pizero Invariant Mass (MeV)");
        H1F h2 = new H1F("Energy Asymmetry",50,-1.0,1.0);      
        h2.setOptStat(Integer.parseInt("1100")); h2.setTitleX("X:(E1-E2)/(E1+E2)");
        H1F h3 = new H1F("Pizero Energy Error",50,-500.,500.); 
        h3.setOptStat(Integer.parseInt("1100")); h3.setTitleX("Pizero Energy Error (MeV)");
        H1F h4 = new H1F("Pizero Theta Error",50,-1.,1.);      
        h4.setOptStat(Integer.parseInt("1100")); h4.setTitleX("Pizero Theta Error (deg)");
        
        while(reader.hasEvent()){
            DataEvent event = reader.getNextEvent();
            part.readMC(event);
            engine.processDataEvent(event);      
            double invmass = 1e3*Math.sqrt(part.getTwoPhoton(part.readEC(event),2));
            
            h1.fill((float)invmass,1.);                          //Two-photon invariant mass
            
            if (invmass>60 && invmass<200) {
                h2.fill((float)part.X);                          //Pizero energy asymmetry
                h3.fill((float)(1e3*Math.sqrt(part.tpi2)-refE)); //Pizero total energy error
                h4.fill(Math.acos(part.cpi0)*180/3.14159-refTH); //Pizero theta angle error
            }
        }
        
        JFrame frame = new JFrame("Pizero Reconstruction");
        frame.setSize(800,800);
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        canvas.divide(2,2);
        canvas.cd(0); canvas.draw(h1);
        canvas.cd(1); canvas.draw(h2);
        canvas.cd(2); canvas.draw(h3);
        canvas.cd(3); canvas.draw(h4);
        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);     
    }
}
