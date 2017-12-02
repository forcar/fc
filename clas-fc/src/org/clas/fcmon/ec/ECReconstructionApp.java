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
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSync;
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
   
   double pcx,pcy,pcz;
   double refE=0,refP=0,refTH=25;
   Boolean printit = false;
   Boolean stop = true;
   
   CodaEventDecoder           codaDecoder = new CodaEventDecoder();
   DetectorEventDecoder   detectorDecoder = new DetectorEventDecoder();
   List<DetectorDataDgtz>        dataList = new ArrayList<DetectorDataDgtz>();    
   IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(3);
  
   ECConstants                        ecc = new ECConstants();
   DataBank                mcData,genData = null;
   ECPart                            part = new ECPart(); 
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nstr = ecPix[0].ec_nstr[0];
   int npix = ecPix[0].pixels.getNumPixels();  
   
   short[] pulse = new short[100]; 
   
   public ECReconstructionApp(String name, ECPixels[] ecPix) {
       super(name,ecPix);
   }
   
   public void init() {
       System.out.println("ECReconstruction.init()");
       mondet =           (String) mon.getGlob().get("mondet");
       DetectorCollection<H1F> ecEngHist = (DetectorCollection<H1F>) mon.getGlob().get("ecEng");
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
                   ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,3).reset();
                   ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,4).reset();
                   ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).reset();
                   ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,1).reset();
                   ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,2).reset();
                   ecPix[idet].strips.hmap2.get("H2_Peds_Hist").get(is,il,0).reset();
                   ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,il,0).reset();
                   ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,il,1).reset();
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
   
   
   public void addEvent(DataEvent event) {
       
      if(app.getDataSource()=="ET") this.updateEvioData(event);
      
      if(app.getDataSource()=="EVIO") {
          if(app.isMC==true)  this.updateSimulatedData(event);
          if(app.isMC==false) this.updateEvioData(event); 
      }
      
      if(app.getDataSource()=="XHIPO"||app.getDataSource()=="HIPO") this.updateHipoData(event);
      
      if (app.isSingleEvent()) {
//         for (int idet=0; idet<ecPix.length; idet++) findPixels(idet);  // Process all pixels for SED
          processSED();
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

       int       ilay =  0;
       int       idet = -1;
       int       evno =  0;
       int    trigger =  0;
       double     sca =  1;
       float      tps =  (float) 0.02345;
       float     tdcf =  0;
       float     tdcd =  0;
       float   tdcmax =  0;
       float   offset =  0;
       long     phase =  0;
       long timestamp =  0;
       
       clear(0); clear(1); clear(2); tdcs.clear();
       
       if (app.isMC)  {tdcmax=2000000; offset=600;}
       if (app.isMCB) {app.isMC=true; tdcmax=2000000; offset=600-(float) 124.25;}
       
       sca = (app.isCRT) ? 6.6:1; // For pre-installation PCAL CRT runs
       
       if(!app.isMC&&event.hasBank("RUN::config")){
           DataBank bank = event.getBank("RUN::config");
           timestamp = bank.getLong("timestamp",0);
           trigger   = bank.getInt("trigger",0);
           evno      = bank.getInt("event",0);         
           int phase_offset = 1;
           phase = ((timestamp%6)+phase_offset)%6;
           app.bitsec = (int) (Math.log10(trigger>>24)/0.301+1);
       }
       
       if(event.hasBank("ECAL::tdc")==true){
           DataBank  bank = event.getBank("ECAL::tdc");
           int rows = bank.rows();
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  ip = bank.getShort("component",i);               
               tdcd = bank.getInt("TDC",i)*tps;
               if(tdcd>0) {
                   if(app.isMC&&tdcd<tdcmax) tdcmax=tdcd; //Find and save longest hit time for MC events            
                   if(!tdcs.hasItem(is,il,ip)) tdcs.add(new ArrayList<Float>(),is,il,ip);
                   tdcs.getItem(is,il,ip).add(tdcd);       
               }
           }
       }
       
       if(event.hasBank("ECAL::adc")==true){
           DataBank  bank = event.getBank("ECAL::adc");
           int rows = bank.rows();
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  ip = bank.getShort("component",i);
               int adc = Math.abs(bank.getInt("ADC",i));
               float t = bank.getFloat("time",i);               
               int ped = bank.getShort("ped", i); 
               
               Float[] tdcc; float[] tdc; 
               
               if (tdcs.hasItem(is,il,ip)) {
                   List<Float> list = new ArrayList<Float>();
                   list = tdcs.getItem(is,il,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
                   tdc  = new float[list.size()];
                   for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-tdcmax+offset-phase*4;  
               } else {
                   tdc = new float[1];
               }
               
               idet = getDet(il);
               ilay = getLay(il);
               
               sca = (float) ((is==5)?ecc.SCALE5[il-1]:ecc.SCALE[il-1]);
               if(app.isMC&&app.variation=="clas6") sca = 1;
               float sadc = (float) (adc / sca);
               
               /*
               for (int ii=0 ; ii< 100 ; ii++) {
                   double wgt1=0; double wgt2=0;
                   if (ii==(int)(t/4)) {wgt1=sadc; wgt2=1.0;}                  
                   ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,ilay,0).fill(ii,ip,wgt2);
                   ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,ilay,1).fill(ii,ip,wgt1);
                   if (app.isSingleEvent()) {
                       ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,ilay,0).fill(ii,ip,wgt1);
                   }
               }   
               */
               if(il==6&&idet==1) {
                  ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,3,3).fill((double) tdc[0]+phase*4,(double) phase);
                  ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,3,4).fill(tdc[0],phase);
               }
               
               if (app.rtt.hasItem(is,il,ip,0)) {
                   int[] dum = (int[]) app.rtt.getItem(is,il,ip,0);
                   getMode7(dum[0],dum[1],dum[2]);
               }
               
               if (ped>0) ecPix[idet].strips.hmap2.get("H2_Peds_Hist").get(is,ilay,0).fill(this.pedref-ped, ip);
               
               
               if(isGoodSector(is)) {
                      fill(idet, is, ilay, ip, sadc, tdc, t, sadc);  
                   fillSED(idet, is, ilay, ip, (int) sadc, tdc);
               }
           }
       }
   }
   
   public void updateEvioData(DataEvent event) {
       
       clear(0); clear(1); clear(2); tdcs.clear();
       DetectorDataDgtz ddd;
              
       app.decoder.initEvent(event);
       
       app.bitsec = app.decoder.getBitsec();
       long phase = app.decoder.getPhase();
       app.localRun = app.decoder.getRun();
       
//       System.out.println(app.decoder.getFCTrigger()+" "+app.decoder.getCDTrigger());
//       System.out.println(" ");
       
       List<DetectorDataDgtz> adcDGTZ = app.decoder.getEntriesADC(DetectorType.ECAL);
       List<DetectorDataDgtz> tdcDGTZ = app.decoder.getEntriesTDC(DetectorType.ECAL);

       for (int i=0; i < tdcDGTZ.size(); i++) {
           ddd=tdcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
           int il = ddd.getDescriptor().getLayer();
           int ip = ddd.getDescriptor().getComponent();
           if(!tdcs.hasItem(is,il,ip)) tdcs.add(new ArrayList<Float>(),is,il,ip);
               tdcs.getItem(is,il,ip).add((float) ddd.getTDCData(0).getTime()*24/1000);              
       }
       
       for (int i=0; i < adcDGTZ.size(); i++) {
           ddd=adcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
           if (isGoodSector(is)) {
           int cr = ddd.getDescriptor().getCrate();
           int sl = ddd.getDescriptor().getSlot();
           int ch = ddd.getDescriptor().getChannel();
           int il = ddd.getDescriptor().getLayer();
           int ip = ddd.getDescriptor().getComponent();
           int ad = ddd.getADCData(0).getADC();
           int pd = ddd.getADCData(0).getPedestal();
           int t0 = ddd.getADCData(0).getTimeCourse();           
           float tf = (float) ddd.getADCData(0).getTime();
           float ph = (float) ddd.getADCData(0).getHeight()-pd;
           short[] pulse = adcDGTZ.get(i).getADCData(0).getPulseArray();
                      
           Float[] tdcc; float[] tdc;
           
           if (tdcs.hasItem(is,il,ip)) {
               List<Float> list = new ArrayList<Float>();
               list = tdcs.getItem(is,il,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
               tdc  = new float[list.size()];
               for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-phase*4;  
           } else {
               tdc = new float[1];
           }
           
           int idet = getDet(il); 
           int ilay = getLay(il);
           
           
           if(il==6&&idet==1) {
               ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,3,3).fill((double) tdc[0]+phase*4,(double) phase);
               ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,3,4).fill(tdc[0],phase);
            }
           
           getMode7(cr,sl,ch);            
           int ped = app.mode7Emulation.User_pedref==1 ? this.pedref:pd;
           
           for (int ii=0 ; ii< pulse.length ; ii++) {
               ecPix[idet].strips.hmap2.get("H2_Mode1_Hist").get(is,ilay,0).fill(ii,ip,pulse[ii]-ped);
               if (app.isSingleEvent()) {
                  ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,ilay,0).fill(ii,ip,pulse[ii]-ped);
                  int w1 = t0-this.nsb ; int w2 = t0+this.nsa;
                  if (ad>0&&ii>=w1&&ii<=w2) ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is,ilay,1).fill(ii,ip,pulse[ii]-ped);                     
               }
            }
           
           if (pd>0) ecPix[idet].strips.hmap2.get("H2_Peds_Hist").get(is,ilay,0).fill(this.pedref-pd, ip);
           
           float sca = (float) ((is==5)?ecc.SCALE5[il-1]:ecc.SCALE[il-1]);
           float sadc = ad / sca;
           fill(idet, is, ilay, ip, sadc, tdc, tf, ph);                     
           fillSED(idet, is, ilay, ip, (int) sadc, tdc);
           
           }    // Good sector        
       }
       
       if (app.decoder.isHipoFileOpen) writeHipoOutput();
       
   }
   
   public void updateSimulatedData(DataEvent event) {
       
      float tdcmax=2000000;
      double tmax = 30;
      int adc, tdcc, detlen=0;
      double mc_t=0.;
      float[] tdc= new float[1]; 
      float tdcf=0;
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
              refTH = Math.acos(ppz/refP)*180/Math.PI;
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
                  tdc[0] = ((float)tdcc-tdcmax+1364000)/1000; 
                  if (goodstrip&&isGoodSector(is)) fill(idet, is, il, ip, adc, tdc, tdcf, tdcf); 
              }
          }
      }  
      //System.out.println(" ");
   }

   public void writeHipoOutput() {
       
       DataEvent  decodedEvent = app.decoder.getDataEvent();
       DataBank   header = app.decoder.createHeaderBank(decodedEvent,0,0,0,0);
       decodedEvent.appendBanks(header);
       app.decoder.writer.writeEvent(decodedEvent);
              
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
                 ecPix[idet].strra[is][il][ip] = 0;
                 ecPix[idet].strrt[is][il][ip] = 0;
                 ecPix[idet].adcr[is][il][ip]  = 0;
                 ecPix[idet].tdcr[is][il][ip]  = 0;
                 ecPix[idet].tf[is][il][ip]    = 0;
                 ecPix[idet].ph[is][il][ip]    = 0;
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
                ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is+1,il+1,2).reset();
                for (int ip=0; ip<ecPix[idet].pixels.getNumPixels() ; ip++) {
                    ecPix[idet].ecadcpix[is][il][ip] = 0;                
                    ecPix[idet].ectdcpix[is][il][ip] = 0;                
                }
                
            }
            for (int ip=0; ip<ecPix[idet].pixels.getNumPixels() ; ip++) {
                ecPix[idet].ecpixel[is][ip]  = 0;                
                ecPix[idet].ecsumpix[is][ip] = 0;
            }
            for (int il=0 ; il<1 ; il++) {
                ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,il+1,0).reset();
                ecPix[idet].strips.hmap1.get("H1_Pixt_Sevd").get(is+1,il+1,0).reset();
            }
         }
      }           
   }  
   
   public void fillSED(int idet, int is, int il, int ip, int adc, float[] tdc) {
       double sca = 10; int idil=idet*3+il;
       if(!app.isMC||(app.isMC&&app.variation=="default")) sca  = ecc.AtoE[idil-1];
       ecPix[idet].strips.hmap1.get("H1_Stra_Sevd").get(is,il,1).fill(ip,adc/sca);  //fill all hits with energy (MeV)    
   }
        
   public void fill(int idet, int is, int il, int ip, float adc, float[] tdc, float tdcf, float adph) {

	   double thr = ecPix[idet].getStripThr(app.config,il)*ecc.AtoE[idet*3+il-1]/10;
	   
       for (int ii=0; ii<tdc.length; ii++) {
           
       if(tdc[ii]>450&&tdc[ii]<850){
           ecPix[idet].uvwt[is-1]=ecPix[idet].uvwt[is-1]+ecPix[idet].uvw_dalitz(idet,il,ip); //Dalitz tdc 
           ecPix[idet].nht[is-1][il-1]++; int inh = ecPix[idet].nht[is-1][il-1];
           if (inh>nstr) inh=nstr;
           ecPix[idet].tdcr[is-1][il-1][inh-1]  = tdc[ii];
           ecPix[idet].strrt[is-1][il-1][inh-1] = ip;                  
           ecPix[idet].ph[is-1][il-1][inh-1]    = adph;
           ecPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc[ii],ip,1.);
           ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is,0,0).fill(ip,il,1.);
           ecPix[idet].strips.hmap2.get("H2_PCt_Stat").get(is,0,1).fill(ip,il,tdc[ii]);
       }
       
       ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,4).fill(adc,tdc[ii],1.0);   
       
       }
       
       if (adc>0) ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,3).fill(adc,ip,1.);  
       
       if(adc>thr) {
           ecPix[idet].uvwa[is-1]=ecPix[idet].uvwa[is-1]+ecPix[idet].uvw_dalitz(idet,il,ip); //Dalitz adc
           ecPix[idet].nha[is-1][il-1]++; int inh = ecPix[idet].nha[is-1][il-1];
           if (inh>nstr) inh=nstr;
           ecPix[idet].adcr[is-1][il-1][inh-1]  = adc;
           ecPix[idet].tf[is-1][il-1][inh-1]    = tdcf;
           ecPix[idet].strra[is-1][il-1][inh-1] = ip;
           ecPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.);  
           ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is,0,0).fill(ip,il,1.);
           ecPix[idet].strips.hmap2.get("H2_PCa_Stat").get(is,0,1).fill(ip,il,adc);
       }   
   }
   
  public void processSED() {
      
      for (int idet=0; idet<ecPix.length; idet++) {
      for (int is=iis1; is<iis2; is++) {
          float[] sed7 = ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,1,0).getData();
          for (int il=0; il<3; il++ ){               
              for (int n=0 ; n<ecPix[idet].nha[is][il] ; n++) {
                    double sca = 10; int idil=idet*3+il;
                    if(!app.isMC||(app.isMC&&app.variation=="default")) sca  = ecc.AtoE[idil];
                    int ip =          ecPix[idet].strra[is][il][n]; 
                  float ad = (float) (ecPix[idet].adcr[is][il][n]/sca);
                  ecPix[idet].strips.hmap1.get("H1_Stra_Sevd").get(is+1,il+1,0).fill(ip,ad);
                  ecPix[idet].strips.putpixels(il+1,ip,ad,sed7);
              }
              for (int n=0 ; n<ecPix[idet].nht[is][il] ; n++) {
                    int ip =         ecPix[idet].strrt[is][il][n]; 
                  float td = (float) ecPix[idet].tdcr[is][il][n];
                  double tdc = 0.25*(td-ECConstants.TOFFSET);
                  float  wgt = (float) ecPix[idet].ph[is][il][n];
                  wgt = (wgt > 0) ? wgt:1000;
                  ecPix[idet].strips.hmap2.get("H2_Mode1_Sevd").get(is+1,il+1,2).fill((float)tdc,ip,wgt);
              }
          }
          for (int i=0; i<sed7.length; i++) {
              ecPix[idet].strips.hmap1.get("H1_Pixa_Sevd").get(is+1,1,0).setBinContent(i, sed7[i]);  
          }
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
                           ecPix[idet].ecadcpix[is][0][ii] = (int) ecPix[idet].adcr[is][0][i];
                           ecPix[idet].ecadcpix[is][1][ii] = (int) ecPix[idet].adcr[is][1][i];
                           ecPix[idet].ecadcpix[is][2][ii] = (int) ecPix[idet].adcr[is][2][i];

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
                       if (good_pix[il-1]&&adcc<250) {
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
               divide(H1_t_Maps.get(is,il,0),H1_t_Maps.get(is,il,4),H1_t_Maps.get(is,il,1)); //Normalize Raw View TDC   to Events
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
    
    


