 package org.clas.fcmon.ec;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

//clas12
import org.clas.fcmon.tools.FADCFitter;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.clas.physics.GenericKinematicFitter;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.clas.detector.DetectorResponse;

//groot
//import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.math.StatNumber;
//clas12rec
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.detector.base.DetectorType;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.service.eb.EventBuilder;
import org.jlab.service.ec.ECPart;
import org.jlab.utils.groups.IndexedList;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.clas.fcmon.detector.view.DetectorShape2D;
//import org.clas.fcmon.jroot.*;

public class ECReconstructionApp extends FCApplication {
    
   FADCFitter     fitter  = new FADCFitter(1,15);
   String          mondet = null;

   String BankType        = null;
   int              detID = 0;
   
   int is1,is2,iis1,iis2;
   
   double pcx,pcy,pcz;
   double refE=0,refP=0,refTH=25;
   Boolean printit = false;
   
   CodaEventDecoder           codaDecoder = new CodaEventDecoder();
   DetectorEventDecoder   detectorDecoder = new DetectorEventDecoder();
   List<DetectorDataDgtz>   detectorData  = new ArrayList<DetectorDataDgtz>();
   
   ECConstants                        ecc = new ECConstants();
   DataBank                mcData,genData = null;
   ECPart                            part = new ECPart(); 
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nstr = ecPix[0].ec_nstr[0];
   int npix = ecPix[0].pixels.getNumPixels();  
   
   int nsa,nsb,tet,pedref;     
   short[] pulse = new short[100]; 
   
   public ECReconstructionApp(String name, ECPixels[] ecPix) {
       super(name,ecPix);
   }
   
   public void init() {
       System.out.println("ECReconstruction.init()");
       mondet =           (String) mon.getGlob().get("mondet");
       DetectorCollection<H1F> ecEngHist = (DetectorCollection<H1F>) mon.getGlob().get("ecEng");
       System.out.println(ECConstants.IS1+" "+ECConstants.IS2);
        is1 = ECConstants.IS1;
        is2 = ECConstants.IS2;
       iis1 = ECConstants.IS1-1;
       iis2 = ECConstants.IS2-1;
   }
   
   public void clearHistograms() {
     
       for (int idet=0; idet<ecPix.length; idet++) {
           for (int is=is1 ; is<is2 ; is++) {
               for (int il=1 ; il<4 ; il++) {
                   ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).reset();
                   ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,1).reset();
                   ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,2).reset();
                   ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).reset();
                   ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,1).reset();
                   ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,2).reset();
                   ecPix[idet].strips.hmap2.get("H2_Peds_Hist").get(is,il,0).reset();
                   ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,il,0).reset();
               }
           }       
       } 
   }      
   
   public void addEvent(DataEvent event) {
       
      if(app.getDataSource()=="ET") this.updateRawData(event);
      
      if(app.getDataSource()=="EVIO") {
          if(app.isMC==true)  this.updateSimulatedData(event);
          if(app.isMC==false) this.updateRawData(event); 
      }
      
      if(app.getDataSource()=="XHIPO"||app.getDataSource()=="HIPO") this.updateHipoData(event);
      
      if (app.isSingleEvent()) {
//         for (int idet=0; idet<ecPix.length; idet++) findPixels(idet);  // Process all pixels for SED
         for (int idet=0; idet<ecPix.length; idet++) processSED(idet);
      } else {
         for (int idet=0; idet<ecPix.length; idet++) processPixels(idet); // Process only single pixels 
 //         processCalib();  // Process only single pixels 
      }
   }
   
   public int getDet(int layer) {
       int[] il = {0,0,0,1,1,1,2,2,2}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
       return il[layer-1];
    }
   
   public int getLay(int layer) {
       int[] il = {1,2,3,1,2,3,1,2,3}; // layer 1-3: PCAL 4-6: ECinner 7-9: ECouter  
       return il[layer-1];
    }
   
   public void updateHipoData(DataEvent event) {
       IndexedList<Integer> tdcs = new IndexedList<Integer>(3);
       int ilay=0, idet=-1;
       double sca=1;
       float tdcf=0;
       
       clear(0); clear(1); clear(2);
       
       sca = (app.isCRT==true) ? 6.6:1; // For pre-installation PCAL CRT runs
       
       tdcs.clear();
       
       if(event.hasBank("ECAL::tdc")==true){
           DataBank  bank = event.getBank("ECAL::tdc");
           int rows = bank.rows();
           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  ip = bank.getShort("component",i);
               int tdc = bank.getInt("TDC",i);    
               tdcs.add(tdc,is,il,ip);
           }
       }
       
       if(event.hasBank("ECAL::adc")==true){
           DataBank  bank = event.getBank("ECAL::adc");
           int rows = bank.rows();
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  ip = bank.getShort("component",i);
               int adc = bank.getInt("ADC",i);
               int ped = bank.getShort("ped", i); 
               int tdc = (tdcs.hasItem(is,il,ip)) ? tdcs.getItem(is,il,ip):0;
               idet = getDet(il);
               ilay = getLay(il);
               if (app.rtt.hasItem(is,il,ip,0)) {
                   int[] dum = (int[]) app.rtt.getItem(is,il,ip,0);
                   getMode7(dum[0],dum[1],dum[2]);
               }
               if (ped>0) ecPix[idet].strips.hmap2.get("H2_Peds_Hist").get(is,ilay,0).fill(this.pedref-ped, ip);
               if(!app.isMC) sca = (is==5)?ecc.SCALE5[il-1]:ecc.SCALE[il-1];
               if(isGoodSector(is)) fill(idet, is, ilay, ip, adc/(int)sca, tdc*24/1000, tdcf);    
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
     
   public void updateRawData(DataEvent event){

      int adc,npk,ped;
      double tdc=0,tdcf=0;
      String AdcType ;
      
      List<DetectorDataDgtz>  dataSet = codaDecoder.getDataEntries((EvioDataEvent) event);
      
      detectorDecoder.translate(dataSet);   
      detectorDecoder.fitPulses(dataSet);
      this.detectorData.clear();
      this.detectorData.addAll(dataSet);
      
      clear(0); clear(1); clear(2);

      int ilay=0;
      int idet=-1;
//    System.out.println("tbits="+codaDecoder.getTriggerBits());
      
      for (DetectorDataDgtz strip : detectorData) {
         if(strip.getDescriptor().getType().getName()=="EC") {
            adc=npk=ped=pedref=0 ; tdc=tdcf=0;
            int icr = strip.getDescriptor().getCrate(); 
            int isl = strip.getDescriptor().getSlot(); 
            int ich = strip.getDescriptor().getChannel(); 
            int is  = strip.getDescriptor().getSector();
            int il  = strip.getDescriptor().getLayer(); // 1-3: PCAL 4-9: ECAL
            int ip  = strip.getDescriptor().getComponent();
            int iord= strip.getDescriptor().getOrder(); 
//            System.out.println(icr+" "+isl+" "+ich+" "+is+" "+il);
            idet = getDet(il);
            ilay = getLay(il);
            
            if (idet>-1) {
                            
            if (strip.getTDCSize()>0) {
                int phase_offset = 1;
                long phase = ((codaDecoder.getTimeStamp()%6)+phase_offset)%6;
                tdc = strip.getTDCData(0).getTime()*24./1000.;
                if(il==6&&idet==1) ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,3,3).fill(tdc,phase);
                tdc = tdc-phase*4.;
                if(il==6&&idet==1) ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,3,4).fill(tdc,phase);
            }
            
            if (strip.getADCSize()>0) {     
                
               AdcType = strip.getADCData(0).getPulseSize()>0 ? "ADCPULSE":"ADCFPGA";
               double sca = (is==5)?ecc.SCALE5[il-1]:ecc.SCALE[il-1];
               
               if(AdcType=="ADCFPGA") { // FADC MODE 7
                   
                  adc = strip.getADCData(0).getIntegral();
                  ped = strip.getADCData(0).getPedestal();
                  npk = strip.getADCData(0).getHeight();
                 tdcf = strip.getADCData(0).getTime();  
                 
                  getMode7(icr,isl,ich); 

                  if (app.mode7Emulation.User_pedref==0) adc = (adc-ped*(this.nsa+this.nsb))/(int)sca;
                  if (app.mode7Emulation.User_pedref==1) adc = (adc-this.pedref*(this.nsa+this.nsb))/(int)sca;
               }   
               
               if (AdcType=="ADCPULSE") { // FADC MODE 1
                   
                  for (int i=0 ; i<strip.getADCData(0).getPulseSize();i++) {               
                     pulse[i] = (short) strip.getADCData(0).getPulseValue(i);
                  }
                  
                  getMode7(icr,isl,ich); 
                  
                  if (app.mode7Emulation.User_pedref==0) fitter.fit(this.nsa,this.nsb,this.tet,0,pulse);                  
                  if (app.mode7Emulation.User_pedref==1) fitter.fit(this.nsa,this.nsb,this.tet,pedref,pulse);   
                  
                  adc = fitter.adc/(int)sca;
                  ped = fitter.pedsum;
                  
                  float pped=0;                  
                  if (app.mode7Emulation.User_pedref==0) pped=this.pedref;                  
                  if (app.mode7Emulation.User_pedref==1) pped=ped;   
                  
                  for (int i=0 ; i< pulse.length ; i++) {
                     ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,ilay,0).fill(i,ip,pulse[i]-pped);
                     if (app.isSingleEvent()) {
                        ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,ilay,0).fill(i,ip,pulse[i]-pped);
                        int w1 = fitter.t0-this.nsb ; int w2 = fitter.t0+this.nsa;
                        if (fitter.adc>0&&i>=w1&&i<=w2) ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,ilay,1).fill(i,ip,pulse[i]-pped);                     
                     }
                  }
               }               
               if (ped>0) ecPix[idet].strips.hmap2.get("H2_Peds_Hist").get(is,ilay,0).fill(this.pedref-ped, ip);
             }           
             if(isGoodSector(is)) fill(idet, is, ilay, ip, adc, tdc, tdcf);    
            }
         }
      }
   }
   
   public void updateSimulatedData(DataEvent event) {
       
      float tdcmax=2000000;
      double tmax = 30;
      int adc, tdcc, detlen=0;
      double mc_t=0.,tdc=0,tdcf=0;
      boolean goodstrip = true;
      
      String det[] = {"PCAL","EC"}; // EC.xml banknames
      
      if (ecPix.length>1)  {detlen=det.length; clear(0); clear(1); clear(2);} 
      if (ecPix.length==1) {detlen=1;          clear(0);}
      
//      System.out.println(" ");
      
      
      for (int idet=0; idet<detlen; idet++) {
          
          double fac = (app.isCRT==true) ? 6.6:1; // For pre-installation PCAL CRT runs
          
          if(event.hasBank("GenPart::true")==true) {
              genData = event.getBank("GenPart::true");
              int    pid = genData.getInt("pid", 0);
              double ppx = genData.getDouble("px",0);
              double ppy = genData.getDouble("py",0);
              double ppz = genData.getDouble("pz",0);
              double  rm = 0.;
              if (pid==111) rm=0.1349764; // pizero mass               
              refP  = Math.sqrt(ppx*ppx+ppy*ppy+ppz*ppz);  
              refE  = Math.sqrt(refP*refP+rm*rm);            
              refTH = Math.acos(ppz/refP)*180/3.14159;
          }
          
          if(event.hasBank(det[idet]+"::true")==true) {
              mcData = event.getBank(det[idet]+"::true");
              for(int i=0; i < mcData.rows(); i++) {
                  if(idet==0) {
                     double pcX = mcData.getDouble("avgX",i);
                     double pcY = mcData.getDouble("avgY",i);
                     double pcZ = mcData.getDouble("avgZ",i);
                     double pcT = mcData.getDouble("avgT",i);
                     if(pcT<tmax){pcx=pcX; pcy=pcY; pcz=pcZ ; tmax = pcT;}
                  }
              }
              if ((app.doEng)&&idet==0&&app.debug) System.out.println("PCAL x,y,z,t="+pcx+" "+pcy+" "+pcz+" "+tmax);
          }
          
          if(event.hasBank(det[idet]+"::dgtz")==true) {            
              DataBank bank = event.getBank(det[idet]+"::dgtz");
              
              for(int i = 0; i < bank.rows(); i++){
                  float dum = (float)bank.getInt("TDC",i);
                  if (dum<tdcmax) tdcmax=dum; //Find and save longest hit time
              }
              
              for(int i = 0; i < bank.rows(); i++){
                  int is  = bank.getInt("sector",i);
                  int ip  = bank.getInt("strip",i);
                  int ic  = bank.getInt("stack",i);     
                  int il  = bank.getInt("view",i);  
                      adc = (int) (bank.getInt("ADC",i)/fac);
                     tdcc = bank.getInt("TDC",i);
                     tdcf = tdcc;
                  if (idet>0&&ic==1) idet=1;
                  if (idet>0&&ic==2) idet=2;
                  //System.out.println("Sector "+is+" Stack "+ic+" View "+il+" Strip "+ip+" Det "+idet+" ADC "+adc);
                  goodstrip= true;
                  if(app.isCRT&&il==2&&ip==53) goodstrip=false;
                  tdc = ((float)tdcc-tdcmax+1364000)/1000; 
                  if (goodstrip&&isGoodSector(is)) fill(idet, is, il, ip, adc, tdc, tdcf); 
              }
          }
      }  
      //System.out.println(" ");
   }

   
   public void clear(int idet) {
            
      for (int is=iis1 ; is<iis2 ; is++) {     
          ecPix[idet].uvwa[is] = 0;
          ecPix[idet].uvwt[is] = 0;
          ecPix[idet].mpix[is] = 0;
          ecPix[idet].esum[is] = 0;         
          for (int il=0 ; il<3 ; il++) {
             ecPix[idet].nha[is][il] = 0;
             ecPix[idet].nht[is][il] = 0;
             for (int ip=0 ; ip<ecPix[idet].ec_nstr[il] ; ip++) {
                 ecPix[idet].strra[is][il][ip]    = 0;
                 ecPix[idet].strrt[is][il][ip]    = 0;
                 ecPix[idet].adcr[is][il][ip]     = 0;
                 ecPix[idet].tdcr[is][il][ip]     = 0;
                 ecPix[idet].ftdcr[is][il][ip]    = 0;
             }
          }               
      }       
            
      if (app.isSingleEvent()) {
         for (int is=iis1 ; is<iis2 ; is++) {
            ecPix[idet].clusterXY.get(is+1).clear();
            for (int il=0 ; il<3 ; il++) {
                ecPix[idet].strips.hmap1.get("H1_Stra_Sevd").get(is+1,il+1,0).reset();
                ecPix[idet].strips.hmap1.get("H1_Stra_Sevd").get(is+1,il+1,1).reset();
                ecPix[idet].strips.hmap1.get("H1_Strt_Sevd").get(is+1,il+1,0).reset();
                ecPix[idet].strips.hmap1.get("H1_Strt_Sevd").get(is+1,il+1,1).reset();
                ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is+1,il+1,0).reset();
                ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is+1,il+1,0).reset();
                ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is+1,il+1,1).reset();
                for (int ip=0; ip<ecPix[idet].pixels.getNumPixels() ; ip++) {
                    ecPix[idet].ecadcpix[is][il][ip] = 0;                
                    ecPix[idet].ectdcpix[is][il][ip] = 0;                
                }
                
            }
            for (int ip=0; ip<ecPix[idet].pixels.getNumPixels() ; ip++) {
                ecPix[idet].ecpixel[is][ip] = 0;                
                ecPix[idet].ecsumpix[is][ip] = 0;
            }
            for (int il=0 ; il<1 ; il++) {
                ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,il+1,0).reset();
                ecPix[idet].strips.hmap1.get("H1_Pixt_Sevd").get(is+1,il+1,0).reset();
            }
         }
      }           
   }
   
   public Boolean isGoodSector(int is) {
       return is>=is1&&is<is2;
   }
        
   public void fill(int idet, int is, int il, int ip, int adc, double tdc, double tdcf) {

       if(tdc>400&&tdc<800){
           ecPix[idet].uvwt[is-1]=ecPix[idet].uvwt[is-1]+ecPix[idet].uvw_dalitz(idet,il,ip); //Dalitz tdc 
           ecPix[idet].nht[is-1][il-1]++; int inh = ecPix[idet].nht[is-1][il-1];
           if (inh>nstr) inh=nstr;
           ecPix[idet].tdcr[is-1][il-1][inh-1]  = tdc;
           ecPix[idet].strrt[is-1][il-1][inh-1] = ip;                  
           ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc,ip,1.);
           ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is,0,0).fill(ip,il,1.);
           ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is,0,1).fill(ip,il,tdc);
       }
       
       ecPix[idet].strips.hmap1.get("H1_Stra_Sevd").get(is,il,1).fill(ip,0.1*adc);
       
       if(adc>ecPix[idet].getStripThr(app.config,il)){
           ecPix[idet].uvwa[is-1]=ecPix[idet].uvwa[is-1]+ecPix[idet].uvw_dalitz(idet,il,ip); //Dalitz adc
           ecPix[idet].nha[is-1][il-1]++; int inh = ecPix[idet].nha[is-1][il-1];
           if (inh>nstr) inh=nstr;
           ecPix[idet].adcr[is-1][il-1][inh-1]  = adc;
           ecPix[idet].ftdcr[is-1][il-1][inh-1] = tdcf;
           ecPix[idet].strra[is-1][il-1][inh-1] = ip;
           ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.);  
           ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is,0,0).fill(ip,il,1.);
           ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is,0,1).fill(ip,il,adc);
       }   
   }
   
  public void processSED(int idet) {
      for (int is=iis1; is<iis2; is++) {
          float[] sed7 = ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,1,0).getData();
          for (int il=1; il<4; il++ ){               
              for (int n=1 ; n<ecPix[idet].nha[is][il-1]+1 ; n++) {
                  int ip=ecPix[idet].strra[is][il-1][n-1]; float ad= (float) 0.1*ecPix[idet].adcr[is][il-1][n-1];
                  ecPix[idet].strips.hmap1.get("H1_Stra_Sevd").get(is+1,il,0).fill(ip,ad);
                  ecPix[idet].strips.putpixels(il,ip,ad,sed7);
              }
          }
          for (int i=0; i<sed7.length; i++) {
              ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,1,0).setBinContent(i, sed7[i]);  
          }
      }  
  }
  
   public void findPixels(int idet) {

       int u,v,w,ii;

       for (int is=iis1 ; is<iis2 ; is++) { // Loop over sectors
           for (int i=0; i<ecPix[idet].nha[is][0]; i++) { // Loop over U strips
               u=ecPix[idet].strra[is][0][i];
               for (int j=0; j<ecPix[idet].nha[is][1]; j++) { // Loop over V strips
                   v=ecPix[idet].strra[is][1][j];
                   for (int k=0; k<ecPix[idet].nha[is][2]; k++){ // Loop over W strips
                       w=ecPix[idet].strra[is][2][k];
                       int dalitz = u+v+w;
                       if (dalitz==73||dalitz==74) { // Dalitz test
                           ecPix[idet].mpix[is]++;      ii = ecPix[idet].mpix[is]-1;
                           ecPix[idet].ecadcpix[is][0][ii] = ecPix[idet].adcr[is][0][i];
                           ecPix[idet].ecadcpix[is][1][ii] = ecPix[idet].adcr[is][1][i];
                           ecPix[idet].ecadcpix[is][2][ii] = ecPix[idet].adcr[is][2][i];

                           ecPix[idet].ecsumpix[is][ii] = ecPix[idet].ecadcpix[is][0][ii]
                                                         +ecPix[idet].ecadcpix[is][1][ii]
                                                         +ecPix[idet].ecadcpix[is][2][ii];
                           ecPix[idet].esum[is]         = ecPix[idet].esum[is] + ecPix[idet].ecsumpix[is][ii];
                           ecPix[idet].ecpixel[is][ii]  = ecPix[idet].pixels.getPixelNumber(u,v,w);
                           ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,1,0).fill(ecPix[idet].ecpixel[is][ii],ecPix[idet].esum[is]); 
                       }
                   }
               }
           }
       }
           //              if (is==1){
           //                  System.out.println("is,inner nhit="+is+" "+nha[is][3]+","+nha[is][4]+","+nha[is][5]);
           //                  System.out.println("is,outer nhit="+is+" "+nha[is][6]+","+nha[is][7]+","+nha[is][8]);
           //                  System.out.println("mpix,ecpix="+mpix[is][0]+","+mpix[is][1]+","+ecpixel[is][0][0]+","+ecpixel[is][1][0]);
           //                  System.out.println(" ");
           //              } 
   }

        
   public void processPixels(int idet) {

       boolean good_ua, good_va, good_wa, good_uvwa;
       boolean[] good_pix = {false,false,false};
       boolean good_ut, good_vt, good_wt, good_uvwt;
       boolean good_dalitz=false, good_pixel;
       int pixel;

       TreeMap<Integer, Object> map= (TreeMap<Integer, Object>) ecPix[idet].Lmap_a.get(0,0,1); 
       float pixelLength[] = (float[]) map.get(1);
       
       for (int is=iis1 ; is<iis2 ; is++) {  
           
               // Process FADC data

               good_ua = ecPix[idet].nha[is][0]==1;
               good_va = ecPix[idet].nha[is][1]==1;
               good_wa = ecPix[idet].nha[is][2]==1;
               
               good_uvwa = good_ua && good_va && good_wa; //Multiplicity test (NU=NV=NW=1)
               
               good_pix[0] = good_ua&&ecPix[idet].adcr[is][1][0]>35&&ecPix[idet].adcr[is][2][0]>35; // If single U strip, require V,W>35
               good_pix[1] = good_va&&ecPix[idet].adcr[is][0][0]>35&&ecPix[idet].adcr[is][2][0]>35; // If single V strip, require U,W>35
               good_pix[2] = good_wa&&ecPix[idet].adcr[is][0][0]>35&&ecPix[idet].adcr[is][1][0]>35; // If single W strip, require U,V>35

               if (idet==0 ) good_dalitz = Math.abs(ecPix[idet].uvwa[is]-2.0)<0.1;                              //PCAL dalitz
               if (idet>0)   good_dalitz = (ecPix[idet].uvwa[is]-2.0)>0.02 && (ecPix[idet].uvwa[is]-2.0)<0.056; //ECAL dalitz              
               
               pixel = ecPix[idet].pixels.getPixelNumber(ecPix[idet].strra[is][0][0],ecPix[idet].strra[is][1][0],ecPix[idet].strra[is][2][0]);
               good_pixel = pixel!=0;

                              ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is+1,0,3).fill(ecPix[idet].uvwa[is]-2.0,1.,1.);
               if (good_uvwa) ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is+1,0,3).fill(ecPix[idet].uvwa[is]-2.0,2.,1.);
               
               if (good_dalitz && good_pixel && good_uvwa) { //Dalitz condition AND valid pixel AND NU=NV=NW=1
                   
                   double area = ecPix[idet].pixels.getZoneNormalizedArea(pixel);
                   int    zone = ecPix[idet].pixels.getZone(pixel);
                   
                   ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is+1,0,4).fill(area,zone,1.);
                   ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is+1,7,0).fill(pixel,1.0); // Events per pixel                  
                   ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is+1,7,3).fill(pixel,1.0/ecPix[idet].pixels.getNormalizedArea(pixel)); //Normalized to pixel area
                   
                   for (int il=1; il<4 ; il++){
                       double adcc = ecPix[idet].adcr[is][il-1][0]/pixelLength[pixel-1];
                       if (good_pix[il-1]) {
                         ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is+1,il,4).fill(pixel,1.0); // Events per pixel
                         ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is+1,il,1).fill(adcc,ecPix[idet].strra[is][il-1][0],1.0) ;
                         ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is+1,il,2).fill(adcc,pixel,1.0);                        
                         ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is+1,7,1).fill(pixel,adcc);
                         ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is+1,il,0).fill(pixel,adcc);
                         ecPix[idet].pixels.hmap1.get("H1_a_Maps").get(is+1,il,2).fill(pixel,Math.pow(adcc,2));
                       }
                   }
               }  
               
               // Process TDC data

               good_ut = ecPix[idet].nht[is][0]==1;
               good_vt = ecPix[idet].nht[is][1]==1;
               good_wt = ecPix[idet].nht[is][2]==1;
               
               good_uvwt = good_ut && good_vt && good_wt; //Multiplicity test (NU=NV=NW=1)    
               
               good_pix[0] = good_ut&&ecPix[idet].tdcr[is][0][0]>500&&ecPix[idet].tdcr[is][0][0]<800;  
               good_pix[1] = good_vt&&ecPix[idet].tdcr[is][1][0]>500&&ecPix[idet].tdcr[is][1][0]<800;  
               good_pix[2] = good_wt&&ecPix[idet].tdcr[is][2][0]>500&&ecPix[idet].tdcr[is][2][0]<800;  
               
               if (idet==0) good_dalitz = Math.abs(ecPix[idet].uvwt[is]-2.0)<0.1;                              //PCAL dalitz
               if (idet>0)  good_dalitz = (ecPix[idet].uvwt[is]-2.0)>0.02 && (ecPix[idet].uvwt[is]-2.0)<0.056; //ECAL dalitz                
               
               pixel  = ecPix[idet].pixels.getPixelNumber(ecPix[idet].strrt[is][0][0],ecPix[idet].strrt[is][1][0],ecPix[idet].strrt[is][2][0]);
               good_pixel  = pixel!=0;
               
                              ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is+1,0,3).fill(ecPix[idet].uvwt[is]-2.0,1.,1.);
               if (good_uvwt) ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is+1,0,3).fill(ecPix[idet].uvwt[is]-2.0,2.,1.);

               if (good_dalitz && good_pixel && good_uvwt) { 
                   
                   double area = ecPix[idet].pixels.getZoneNormalizedArea(pixel);
                   int    zone = ecPix[idet].pixels.getZone(pixel);
                   
                   ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is+1,0,4).fill(area,zone,1.);
                   ecPix[idet].pixels.hmap1.get("H1_t_Maps").get(is+1,7,0).fill(pixel,1.0);
                   ecPix[idet].pixels.hmap1.get("H1_t_Maps").get(is+1,7,3).fill(pixel,1.0/ecPix[idet].pixels.getNormalizedArea(pixel)); //Normalized to pixel area
                   
                   for (int il=1; il<4 ; il++){
                       double tdcc = ecPix[idet].tdcr[is][il-1][0];
                       if (good_pix[il-1]) {
                         ecPix[idet].pixels.hmap1.get("H1_t_Maps").get(is+1,il,4).fill(pixel,1.0); // Events per pixel
                         ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is+1,il,1).fill(tdcc,ecPix[idet].strrt[is][il-1][0],1.0) ;
                         ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is+1,il,2).fill(tdcc,pixel,1.0);                        
                         ecPix[idet].pixels.hmap1.get("H1_t_Maps").get(is+1,7,1).fill(pixel,tdcc);
                         ecPix[idet].pixels.hmap1.get("H1_t_Maps").get(is+1,il,0).fill(pixel,tdcc);
                         ecPix[idet].pixels.hmap1.get("H1_t_Maps").get(is+1,il,2).fill(pixel,Math.pow(tdcc,2));
                       }
                   }
               }   
       }  
       
   }
  
   public void makeMaps(int idet) {

       DetectorCollection<H2F> H2_a_Hist    = new DetectorCollection<H2F>() ; 
       DetectorCollection<H2F> H2_t_Hist    = new DetectorCollection<H2F>() ; 
       DetectorCollection<H1F> H1_Stra_Sevd = new DetectorCollection<H1F>() ;
       DetectorCollection<H1F> H1_Pixa_Sevd = new DetectorCollection<H1F>() ; 
       DetectorCollection<H1F> H1_a_Maps    = new DetectorCollection<H1F>() ;
       DetectorCollection<H1F> H1_t_Maps    = new DetectorCollection<H1F>() ;
      
       H2_a_Hist    = ecPix[idet].strips.hmap2.get("H2_a_Hist");
       H2_t_Hist    = ecPix[idet].strips.hmap2.get("H2_t_Hist");
       H1_Stra_Sevd = ecPix[idet].strips.hmap1.get("H1_Stra_Sevd");
       H1_Pixa_Sevd = ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd");
       H1_a_Maps    = ecPix[idet].pixels.hmap1.get("H1_a_Maps");
       H1_t_Maps    = ecPix[idet].pixels.hmap1.get("H1_t_Maps");

       // Layer assignments:
       // il=1-3 (U,V,W strips) il=7 (Inner Pixels) il=8 (Outer Pixels)
         
        for (int is=is1;is<is2;is++) {
           for (int il=1 ; il<4 ; il++) {
               divide(H1_a_Maps.get(is,il,0),H1_a_Maps.get(is,il,4),H1_a_Maps.get(is,il,1)); //Normalize Raw View ADC   to Events
               divide(H1_a_Maps.get(is,il,2),H1_a_Maps.get(is,il,4),H1_a_Maps.get(is,il,3)); //Normalize Raw View ADC^2 to Events
//               divide(H1_t_Maps.get(is,il,0),H1_t_Maps.get(is,il,4),H1_t_Maps.get(is,il,1)); //Normalize Raw View TDC   to Events
               ecPix[idet].Lmap_a.add(is,il,   0, toTreeMap(H2_a_Hist.get(is,il,0).projectionY().getData()));  //Strip View ADC  
               ecPix[idet].Lmap_a.add(is,il+10,0, toTreeMap(H1_a_Maps.get(is,il,1).getData()));                //Pixel View ADC 
               ecPix[idet].Lmap_t.add(is,il,   0, toTreeMap(H2_t_Hist.get(is,il,0).projectionY().getData()));  //Strip View TDC  
               ecPix[idet].Lmap_t.add(is,il+10,0, toTreeMap(H1_t_Maps.get(is,il,1).getData()));                //Pixel View TDC                
           }
           
           divide(H1_a_Maps.get(is, 7, 1),H1_a_Maps.get(is, 7, 0),H1_a_Maps.get(is, 7, 2)); // Normalize Raw ADC Sum to Events
           divide(H1_t_Maps.get(is, 7, 1),H1_t_Maps.get(is, 7, 0),H1_t_Maps.get(is, 7, 2)); // Normalize Raw TDC Sum to Events 
           ecPix[idet].Lmap_a.add(is, 7,0, toTreeMap(H1_a_Maps.get(is,7,0).getData())); //Pixel ADC Events  
           ecPix[idet].Lmap_t.add(is, 7,0, toTreeMap(H1_t_Maps.get(is,7,0).getData())); //Pixel TDC Events  
           ecPix[idet].Lmap_a.add(is, 7,1, toTreeMap(H1_a_Maps.get(is,7,3).getData())); //Pixel ADC Events Normalized  
           ecPix[idet].Lmap_t.add(is, 7,1, toTreeMap(H1_t_Maps.get(is,7,3).getData())); //Pixel TDC Events Normalized  
           ecPix[idet].Lmap_a.add(is, 9,0, toTreeMap(H1_a_Maps.get(is,7,2).getData())); //Pixel ADC U+V+W    
           ecPix[idet].Lmap_t.add(is, 9,0, toTreeMap(H1_t_Maps.get(is,7,2).getData())); //Pixel TDC U+V+W   
           
           if (app.isSingleEvent()){
               for (int il=1 ; il<4 ; il++) ecPix[idet].Lmap_a.add(is,il,0,   toTreeMap(H1_Stra_Sevd.get(is,il,0).getData())); 
               for (int il=1 ; il<2 ; il++) ecPix[idet].Lmap_a.add(is,il+6,0, toTreeMap(H1_Pixa_Sevd.get(is,il,0).getData())); 
           }
       }
        
       ecPix[idet].getLmapMinMax(is1,is2,1,0); 
       ecPix[idet].getLmapMinMax(is1,is2,2,0); 
       ecPix[idet].getLmapMinMax(is1,is2,3,0); 
       ecPix[idet].getLmapMinMax(is1,is2,11,0); 
       ecPix[idet].getLmapMinMax(is1,is2,12,0); 
       ecPix[idet].getLmapMinMax(is1,is2,13,0); 
       ecPix[idet].getLmapMinMax(is1,is2,7,0); 
       ecPix[idet].getLmapMinMax(is1,is2,7,1); 
       ecPix[idet].getLmapMinMax(is1,is2,9,0); 
       
   }
   
   public TreeMap<Integer, Object> toTreeMap(float dat[]) {
       TreeMap<Integer, Object> hcontainer = new TreeMap<Integer, Object>();
       hcontainer.put(1, dat);
       float[] b = Arrays.copyOf(dat, dat.length);
       double min=100000,max=0,bsum=0,ratio=0;
       int nbsum=0;
       for (int i =0 ; i < b.length; i++){
           if (b[i] !=0 && b[i] < min) min=b[i];
           if (b[i] !=0 && b[i] > max) max=b[i];
           if (b[i]>0) {bsum=bsum+b[i]; nbsum++;}
       }
       if (nbsum>0) ratio=bsum/nbsum;
       // Arrays.sort(b);
       // double min = b[0]; double max=b[b.length-1];
       if (min<=0) min=0.01;
       hcontainer.put(2, min);
       hcontainer.put(3, max);
       hcontainer.put(4,ratio);
       return hcontainer;        
   }

   public void divide(H1F h1, H1F h2, H1F h3){      
       if(h1.getXaxis().getNBins()!=h2.getXaxis().getNBins()){
           System.out.println("[H1D::divide] error : histograms have inconsistent bins");
           return;
       }       
       StatNumber   numer = new StatNumber();
       StatNumber   denom = new StatNumber();
       for(int bin = 0; bin < h1.getXaxis().getNBins(); bin++){
           numer.set(h1.getBinContent(bin), h1.getBinError(bin));
           denom.set(h2.getBinContent(bin), h2.getBinError(bin));
           numer.divide(denom);
           h3.setBinContent(bin, numer.number());
           h3.setBinError(bin, numer.error());
       }
   }   
   
}
    
    


