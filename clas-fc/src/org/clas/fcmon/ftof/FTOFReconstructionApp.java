package org.clas.fcmon.ftof;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

//clas12
import org.clas.fcmon.tools.FADCFitter;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas12.detector.FADCConfigLoader;
//import org.root.histogram.H1D;
//import org.root.histogram.H2D;

//groot
//import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

//clas12rec
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioDataBank;

//import org.clas.fcmon.jroot.*;

public class FTOFReconstructionApp extends FCApplication {
    
   FADCConfigLoader fadc  = new FADCConfigLoader();
   FADCFitter     fitter  = new FADCFitter(1,15);
   String          mondet = null;
   Boolean           inMC = null;
   String        BankType = null;
   int              detID = 0;

   CodaEventDecoder            newdecoder = new CodaEventDecoder();
   DetectorEventDecoder   detectorDecoder = new DetectorEventDecoder();
   List<DetectorDataDgtz>  detectorData   = new ArrayList<DetectorDataDgtz>();
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nstr = ftofPix[1].nstr;
       
   int nsa,nsb,tet,pedref;     
   int     thrcc = 20;
   short[] pulse = new short[100]; 
    
   public FTOFReconstructionApp(String name, FTOFPixels[] ftofPix) {
       super(name,ftofPix);
   }
   
   public void init() {
       System.out.println("FTOFReconstruction.init()");
       fadc.load("/daq/fadc/ftof",10,"default");
       mondet =           (String) mon.getGlob().get("mondet");
       inMC   =          (Boolean) mon.getGlob().get("inMC");
//       detID  =              (int) mon.getGlob().get("detID"); 
   } 
   
   public void clearHistograms() {
       
       for (int idet=0; idet<ftofPix.length; idet++) {
           for (int is=1 ; is<7 ; is++) {
               ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,0,0).reset();
               ftofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,0,0).reset();
               for (int il=1 ; il<3 ; il++) {
                   ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).reset();
                   ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,3).reset();
                   ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,5).reset();
                   ftofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).reset();
               }
           }       
       } 
   }
   
   public void getMode7(int cr, int sl, int ch) {    
      app.mode7Emulation.configMode7(cr,sl,ch);
      this.nsa    = app.mode7Emulation.nsa;
      this.nsb    = app.mode7Emulation.nsb;
      this.tet    = app.mode7Emulation.tet;
      this.pedref = app.mode7Emulation.pedref;
   }
   
   public void addEvent(EvioDataEvent event) {
      
      if((event.hasBank("FTOF1A::dgtz"))||(event.hasBank("FTOF1B::dgtz"))==true) {
          this.updateSimulatedData(event);
      } else {
          this.updateRealData(event);         
      }
          
      if (app.isSingleEvent()) {
         findPixels();     // Process all pixels for SED
         processSED();
      } else {
         processPixels();  // Process only single pixels 
         processCalib();   // Quantities for display and calibration engine
      }
   }
   
   public String detID(int layer) {
       return "FTOF";
   }
   
   public void updateRealData(EvioDataEvent event){

      int adc,ped,npk,il=0;
      double tdc=0,tdcf=0;
      String AdcType ;
      
      List<DetectorDataDgtz>  dataSet = newdecoder.getDataEntries((EvioDataEvent) event);
      
      detectorDecoder.translate(dataSet);   
      detectorDecoder.fitPulses(dataSet);
      this.detectorData.clear();
      this.detectorData.addAll(dataSet);
      
      clear(0); clear(1); clear(2);
      int nsum=0;
     
      for (DetectorDataDgtz strip : detectorData) {
         if(strip.getDescriptor().getType().getName()=="FTOF") {
            adc=ped=pedref=npk=0 ; tdc=tdcf=0;
            int icr = strip.getDescriptor().getCrate(); 
            int isl = strip.getDescriptor().getSlot(); 
            int ich = strip.getDescriptor().getChannel(); 
            int is  = strip.getDescriptor().getSector();
            int iil = strip.getDescriptor().getLayer();  
            int ip  = strip.getDescriptor().getComponent();
            int iord= strip.getDescriptor().getOrder(); 

            app.currentCrate = icr;
            app.currentSlot  = isl;
            app.currentChan  = ich;
            
            if (iil>0) {
                
            if (strip.getTDCSize()>0) {
                il=iord-1;
                tdc = strip.getTDCData(0).getTime()*24./1000.;
            }
            
            if (strip.getADCSize()>0) {     
               il=iord+1;
               
               AdcType = strip.getADCData(0).getPulseSize()>0 ? "ADCPULSE":"ADCFPGA";
               
               if(AdcType=="ADCFPGA") { // FADC MODE 7
                  adc = strip.getADCData(0).getIntegral();
                  ped = strip.getADCData(0).getPedestal();
                  npk = strip.getADCData(0).getHeight();
                 tdcf = strip.getADCData(0).getTime();            
                  getMode7(icr,isl,ich);
                  if (app.mode7Emulation.User_pedref==0) adc = (adc-ped*(this.nsa+this.nsb));
                  if (app.mode7Emulation.User_pedref==1) adc = (adc-this.pedref*(this.nsa+this.nsb));
               }   
               
               if (AdcType=="ADCPULSE") { // FADC MODE 1
                  for (int i=0 ; i<strip.getADCData(0).getPulseSize();i++) {               
                     pulse[i] = (short) strip.getADCData(0).getPulseValue(i);
                  }              
                  getMode7(icr,isl,ich);
                  if (app.mode7Emulation.User_pedref==0) fitter.fit(this.nsa,this.nsb,this.tet,0,pulse);                  
                  if (app.mode7Emulation.User_pedref==1) fitter.fit(this.nsa,this.nsb,this.tet,pedref,pulse);                    
                  adc = fitter.adc;
                  ped = fitter.pedsum;
                  for (int i=0 ; i< pulse.length ; i++) {
                     ftofPix[iil-1].strips.hmap2.get("H2_a_Hist").get(is,il,5).fill(i,ip,pulse[i]-this.pedref);
                     if (app.isSingleEvent()) {
                        ftofPix[iil-1].strips.hmap2.get("H2_a_Sevd").get(is,il,0).fill(i,ip,pulse[i]-this.pedref);
                        int w1 = fitter.t0-this.nsb ; int w2 = fitter.t0+this.nsa;
                        if (fitter.adc>0&&i>=w1&&i<=w2) ftofPix[iil-1].strips.hmap2.get("H2_a_Sevd").get(is,il,1).fill(i,ip,pulse[i]-this.pedref);                     
                     }
                  }
               }               
               if (ped>0) ftofPix[iil-1].strips.hmap2.get("H2_a_Hist").get(is,il,3).fill(this.pedref-ped, ip);
             } 
//               System.out.println(icr+" "+isl+" "+ich+" "+is+" "+il+" "+ip+" "+iord+" "+tdc+" "+adc);
            
             fill(is, il, ip, adc, tdc, tdcf, iil-1);    
            }
         }
      }
   }
   
   public void updateSimulatedData(EvioDataEvent event) {
       
      float tdcmax=100000;
      int nrows, adc, tdcc, fac;
      double mc_t=0.,tdc=0,tdcf=0;

      String det[] = {"FTOF1A","FTOF1B","FTOF2B"}; // FTOF.xml banknames
      
      clear(0); clear(1); clear(2);
      
      for (int idet=0; idet<det.length ; idet++) {
          
          if(event.hasBank(det[idet]+"::true")==true) {
              EvioDataBank bank  = (EvioDataBank) event.getBank(det[idet]+"::true"); 
              for(int i=0; i < bank.rows(); i++) mc_t = bank.getDouble("avgT",i);          
          }
         
          inMC = true; mon.putGlob("inMC",true); 
      
          if(event.hasBank(det[idet]+"::dgtz")==true) {            
              EvioDataBank bank = (EvioDataBank) event.getBank(det[idet]+"::dgtz");
              
              for(int i = 0; i < bank.rows(); i++){
                  float dum = (float)bank.getInt("TDC",i)-(float)mc_t*1000;
                  if (dum<tdcmax) tdcmax=dum; //Find latest hit time
              }
      
              for(int i = 0; i < bank.rows(); i++){
                  int is  = bank.getInt("sector",i);
                  int ip  = bank.getInt("paddle",i);
                      adc = bank.getInt("ADCL",i);
                     tdcc = bank.getInt("TDCL",i);
                     tdcf = tdcc;
                      tdc = (((float)tdcc-(float)mc_t*1000)-tdcmax+1340000)/1000; 
                 fill(is, 1, ip, adc, tdc, tdcf, idet); 
                      adc = bank.getInt("ADCR",i);
                     tdcc = bank.getInt("TDCR",i);
                     tdcf = tdcc;
                      tdc = (((float)tdcc-(float)mc_t*1000)-tdcmax+1340000)/1000; 
                 fill(is, 2, ip, adc, tdc, tdcf, idet); 
              }                     
          }         
       }         
   }
   
   public void clear(int idet) {
       
       for (int is=0 ; is<6 ; is++) {
           for (int il=0 ; il<2 ; il++) {
               ftofPix[idet].nha[is][il] = 0;
               ftofPix[idet].nht[is][il] = 0;
               for (int ip=0 ; ip<nstr ; ip++) {
                   ftofPix[idet].strra[is][il][ip] = 0;
                   ftofPix[idet].strrt[is][il][ip] = 0;
                   ftofPix[idet].adcr[is][il][ip]  = 0;
                   ftofPix[idet].tdcr[is][il][ip]  = 0;
               }
           }
       }
       
       if (app.isSingleEvent()) {
           for (int is=0 ; is<6 ; is++) {
               for (int il=0 ; il<2 ; il++) {
                    ftofPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).reset();
                    ftofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,0).reset();
                    ftofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,1).reset();
               }
           }
       }   
   }
   
   public void fill(int is, int il, int ip, int adc, double tdc, double tdcf, int idet) {
           
       if(tdc>1200&&tdc<1500){
             ftofPix[idet].nht[is-1][il-1]++; int inh = ftofPix[idet].nht[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ftofPix[idet].tdcr[is-1][il-1][inh-1] = (float) tdc;
             ftofPix[idet].strrt[is-1][il-1][inh-1] = ip;
             ftofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc,ip,1.0);
             }
       if(adc>thrcc){
             ftofPix[idet].nha[is-1][il-1]++; int inh = ftofPix[idet].nha[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ftofPix[idet].adcr[is-1][il-1][inh-1] = adc;
             ftofPix[idet].strra[is-1][il-1][inh-1] = ip;
             ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.0);
             } 
   }
   
   public void processCalib() {
       
       int iL,iR,ipL,ipR;
       
       for (int is=1 ; is<7 ; is++) {
           for (int idet=0; idet<3; idet++) {
                iL = ftofPix[idet].nha[is-1][0];
                iR = ftofPix[idet].nha[is-1][1];
               ipL = ftofPix[idet].strra[is-1][0][0];
               ipR = ftofPix[idet].strra[is-1][1][0];
               if ((iL==1&&iR==1)&&(ipL==ipR)) {
                   float gm = (float) Math.sqrt(ftofPix[idet].adcr[is-1][0][0]*ftofPix[idet].adcr[is-1][1][0]);
                   ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is, 0, 0).fill(gm, ipL,1.0);
               }
               iL = ftofPix[idet].nht[is-1][0];
               iR = ftofPix[idet].nht[is-1][1];
              ipL = ftofPix[idet].strrt[is-1][0][0];
              ipR = ftofPix[idet].strrt[is-1][1][0];
              if ((iL==1&&iR==1)&&(ipL==ipR)) {
                  float td = ftofPix[idet].tdcr[is-1][0][0]-ftofPix[idet].tdcr[is-1][1][0];
                  ftofPix[idet].strips.hmap2.get("H2_t_Hist").get(is, 0, 0).fill(td, ipL,1.0);
              }
           }
       }       
   }
   
   public void processSED() {
       
       for (int idet=0; idet<ftofPix.length; idet++) {
       for (int is=0; is<6; is++) {
          for (int il=0; il<2; il++ ){;
              for (int n=0 ; n<ftofPix[idet].nha[is][il] ; n++) {
                  int ip=ftofPix[idet].strra[is][il][n]; int ad=ftofPix[idet].adcr[is][il][n];
                  ftofPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).fill(ip,ad);
              }
          }
       } 
       }
   } 
   
   public void findPixels() {
       
   }
   
   public void processPixels() {
       
   }

   public TreeMap<Integer, Object> toTreeMap(float dat[]) {
       TreeMap<Integer, Object> hcontainer = new TreeMap<Integer, Object>();
       hcontainer.put(1, dat);
       float[] b = Arrays.copyOf(dat, dat.length);
       double min=100000,max=0;
       for (int i =0 ; i < b.length; i++){
           if (b[i] !=0 && b[i] < min) min=b[i];
           if (b[i] !=0 && b[i] > max) max=b[i];
       }
       // Arrays.sort(b);
       // double min = b[0]; double max=b[b.length-1];
       if (min<=0) min=0.01;
       hcontainer.put(2, min);
       hcontainer.put(3, max);
       return hcontainer;        
   }

   public void makeMaps() {
       
       for (int idet=0; idet<ftofPix.length ; idet++) {
           DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
           DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
           H2_a_Hist = ftofPix[idet].strips.hmap2.get("H2_a_Hist");
           H1_a_Sevd = ftofPix[idet].strips.hmap1.get("H1_a_Sevd");
               for (int is=1;is<7;is++) {
                   for (int il=1 ; il<3 ; il++) {
                       if (!app.isSingleEvent()) ftofPix[idet].Lmap_a.add(is,il,0, toTreeMap(H2_a_Hist.get(is,il,0).projectionY().getData())); //Strip View ADC 
                       if  (app.isSingleEvent()) ftofPix[idet].Lmap_a.add(is,il,0, toTreeMap(H1_a_Sevd.get(is,il,0).getData()));           
                   }
               } 
           }
       }    
}
    
    


