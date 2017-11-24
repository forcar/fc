package org.clas.fcmon.cc;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.clas.fcmon.tools.FADCFitter;
import org.clas.fcmon.tools.FCApplication;

//clas12rec
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedList;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;

public class CCReconstructionApp extends FCApplication {
    
   String          mondet ;
   
   String BankType = null;        
   int       detID = 0 ;

   CodaEventDecoder           codaDecoder = new CodaEventDecoder();
   List<DetectorDataDgtz>       dataList  = new ArrayList<DetectorDataDgtz>();
   IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(3);
   IndexedList<List<Float>>          adcs = new IndexedList<List<Float>>(3);
   IndexedList<List<Integer>>       lapmt = new IndexedList<List<Integer>>(1); 
   IndexedList<List<Integer>>       ltpmt = new IndexedList<List<Integer>>(1); 
   
   public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
   public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nstr = ccPix.nstr[0];
   
   int nsa,nsb,tet,pedref;     
   int     thrcc = 20;
   short[] pulse = new short[100]; 
    
   public CCReconstructionApp(String name, CCPixels ccPix) {
       super(name,ccPix);
   }
   
   public void init() {
       System.out.println("CCReconstruction.init()");
       mondet = (String) mon.getGlob().get("mondet");
       is1 = CCConstants.IS1;
       is2 = CCConstants.IS2;
      iis1 = CCConstants.IS1-1;
      iis2 = CCConstants.IS2-1;   
   }
   
   public void clearHistograms() {
       
       for (int is=is1 ; is<is2 ; is++) {
           for (int il=1 ; il<3 ; il++) {
                ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,0).reset();
                ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,3).reset();
                ccPix.strips.hmap2.get("H2_CCa_Hist").get(is,il,5).reset();
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
      
       if(app.getDataSource()=="ET") this.updateRawData(event);
       
       if(app.getDataSource()=="EVIO") {    	   
    	   if(app.isMC==true)  this.updateSimulatedData(event);
           if(app.isMC==false) this.updateRawData(event); 
       }
       
       if(app.getDataSource()=="XHIPO"||app.getDataSource()=="HIPO") this.updateHipoData(event);;
       
       if (app.isSingleEvent()) {
           findPixels();     // Process all pixels for SED
           processSED();
//           processCalib();
        } else {
           processPixels();  // Process only single pixels 
//           processCalib();   // Quantities for display and calibration engine
        }
       
   }
   public void updateHipoData(DataEvent event) {
       
       int evno;
       long phase = 0;
       int trigger = 0;
       long timestamp = 0;
       float offset = 0;
       
       clear(); tdcs.clear(); adcs.clear(); ltpmt.clear(); lapmt.clear();
       
       if(!app.isMC&&event.hasBank("RUN::config")){
           DataBank bank = event.getBank("RUN::config");
           timestamp = bank.getLong("timestamp",0);
           trigger   = bank.getInt("trigger",0);
           evno      = bank.getInt("event",0);         
           int phase_offset = 1;
           phase = ((timestamp%6)+phase_offset)%6;
           if (trigger>0) app.bitsec = (int) (Math.log10(trigger>>24)/0.301+1);
       }
       
       if (app.isMCB) offset=(float)124.25;
       
       if(event.hasBank("LTCC::tdc")){
           DataBank  bank = event.getBank("LTCC::tdc");
           int rows = bank.rows();
           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);                       
               int  ip = bank.getShort("component",i);
               
               if (!tdcs.hasItem(is,lr-2,il)) tdcs.add(new ArrayList<Float>(),is,lr-2,il);
                    tdcs.getItem(is,lr-2,il).add((float) bank.getInt("TDC",i)*24/1000+offset-phase*4  );              
               if (!ltpmt.hasItem(is)) {
       	            ltpmt.add(new ArrayList<Integer>(),is);
                    ltpmt.getItem(is).add(il);
               }   
           }
       }
              
       if(event.hasBank("LTCC::adc")){
           DataBank  bank = event.getBank("LTCC::adc");
           int rows = bank.rows();
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);
               int  ip = bank.getShort("component",i);
               int adc = bank.getInt("ADC",i);
               float t = bank.getFloat("time",i);               
               int ped = bank.getShort("ped", i);
               
               if (!adcs.hasItem(is,lr,il)) adcs.add(new ArrayList<Float>(),is,lr,il);
                    adcs.getItem(is,lr,il).add((float)adc);            
               if (!lapmt.hasItem(is)) {
                    lapmt.add(new ArrayList<Integer>(),is);
                    lapmt.getItem(is).add(il);
               }
          
               Float[] tdcc; float[] tdc;
               
               if (tdcs.hasItem(is,lr,il)) {
                   List<Float> list = new ArrayList<Float>();
                   list = tdcs.getItem(is,lr,il); tdcc=new Float[list.size()]; list.toArray(tdcc);
                   tdc  = new float[list.size()];
                   for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii];  
               } else {
                   tdc = new float[1];
               }
               for (int ii=0 ; ii< 100 ; ii++) {
                   float wgt = (ii==(int)(t/4)) ? adc:0;
                   ccPix.strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,il,wgt);
                   if (app.isSingleEvent()) {
                       ccPix.strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,il,wgt);
                   }
               }
               
               if (app.rtt.hasItem(is,il,ip,lr)) {
                   int[] dum = (int[]) app.rtt.getItem(is,il,ip,lr);                  
                   getMode7(dum[0],dum[1],dum[2]);                  
               }
               
               if (ped>0) ccPix.strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-ped, il);
               
               if(isGoodSector(is)) fill(is, lr+1, il, adc, tdc, t, (float) adc);    
           }
       }
       
   }  
   
   public void updateRawData(DataEvent event){

      clear(); tdcs.clear(); adcs.clear(); lapmt.clear(); ltpmt.clear();
      
      app.decoder.detectorDecoder.setTET(app.mode7Emulation.tet);
      app.decoder.detectorDecoder.setNSA(app.mode7Emulation.nsa);
      app.decoder.detectorDecoder.setNSB(app.mode7Emulation.nsb);
      
      app.decoder.initEvent(event);
      
      long   phase = app.decoder.getPhase();
      app.localRun = app.decoder.getRun();
      
      if (app.isSingleEvent()) {
        System.out.println(" ");       
        System.out.println("Event Number "+app.getEventNumber());
      }
      
      List<DetectorDataDgtz> adcDGTZ = app.decoder.getEntriesADC(DetectorType.LTCC);
      List<DetectorDataDgtz> tdcDGTZ = app.decoder.getEntriesTDC(DetectorType.LTCC);

      for (int i=0; i < tdcDGTZ.size(); i++) {
          DetectorDataDgtz ddd=tdcDGTZ.get(i);
          int is = ddd.getDescriptor().getSector();
          int il = ddd.getDescriptor().getLayer();
          int lr = ddd.getDescriptor().getOrder();
          int ip = ddd.getDescriptor().getComponent();          
          if (app.isSingleEvent()) System.out.println("Sector "+is+" Layer "+il+" Order "+lr);
          if (!tdcs.hasItem(is,lr-2,il)) tdcs.add(new ArrayList<Float>(),is,lr-2,il);
               tdcs.getItem(is,lr-2,il).add((float) ddd.getTDCData(0).getTime()*24/1000);  
          if (!ltpmt.hasItem(is)) {
               ltpmt.add(new ArrayList<Integer>(),is);
               ltpmt.getItem(is).add(is);
          }
      }
      
      for (int i=0; i < adcDGTZ.size(); i++) {
   	   
          DetectorDataDgtz ddd=adcDGTZ.get(i);
          int is = ddd.getDescriptor().getSector();
          if (isGoodSector(is)) {
          int cr = ddd.getDescriptor().getCrate();
          int sl = ddd.getDescriptor().getSlot();
          int ch = ddd.getDescriptor().getChannel();  
          int il = ddd.getDescriptor().getLayer();
          int lr = ddd.getDescriptor().getOrder();
          int ip = ddd.getDescriptor().getComponent();
          int ad = ddd.getADCData(0).getADC();
          int pd = ddd.getADCData(0).getPedestal();
          int t0 = ddd.getADCData(0).getTimeCourse();  
         
          float tf = (float) ddd.getADCData(0).getTime();
          float ph = (float) ddd.getADCData(0).getHeight()-pd;
          short[]    pulse = ddd.getADCData(0).getPulseArray();          
          
         if (!adcs.hasItem(is,lr,ip)) adcs.add(new ArrayList<Float>(),is,lr,ip);
              adcs.getItem(is,lr,ip).add((float)ad);                
         if (!lapmt.hasItem(is)) {
              lapmt.add(new ArrayList<Integer>(),is);
              lapmt.getItem(is).add(is);
         }           
          
          Float[] tdcc; float[] tdc;
          
          if (tdcs.hasItem(is,lr,ip)) {
              List<Float> list = new ArrayList<Float>();
              list = tdcs.getItem(is,lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
              tdc  = new float[list.size()];
              for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-phase*4;  
          } else {
              tdc = new float[1];
          }
          
          getMode7(cr,sl,ch); 
          int ped = app.mode7Emulation.User_pedref==1 ? this.pedref:pd;
                     
          for (int ii=0 ; ii< pulse.length ; ii++) {
              ccPix.strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,pulse[ii]-ped);
              if (app.isSingleEvent()) {
                 ccPix.strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,pulse[ii]-ped);
                 int w1 = t0-this.nsb ; int w2 = t0+this.nsa;
                 if (ad>0&&ii>=w1&&ii<=w2) ccPix.strips.hmap2.get("H2_a_Sevd").get(is,lr+1,1).fill(ii,ip,pulse[ii]-ped);                     
              }
           }
          
          if (pd>0) ccPix.strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-pd, ip);
          fill(is, lr+1, ip, ad, tdc, tf, ph);   
          
          }           
      }
      
      if (app.decoder.isHipoFileOpen) writeHipoOutput();      
      
   }
   
   public void writeHipoOutput() {
       
       DataEvent  decodedEvent = app.decoder.getDataEvent();
       DataBank   header = app.decoder.createHeaderBank(decodedEvent,0,0,0,0);
       decodedEvent.appendBanks(header);
       app.decoder.writer.writeEvent(decodedEvent);
              
   }  
   
   public void updateSimulatedData(DataEvent event) {
       
   }
   
   public void clear() {
       
       for (int is=iis1 ; is<iis2 ; is++) {
           for (int il=0 ; il<2 ; il++) {
               ccPix.nha[is][il] = 0;
               ccPix.nht[is][il] = 0;
               for (int ip=0 ; ip<ccPix.nstr[0] ; ip++) {
                   ccPix.strra[is][il][ip] = 0;
                   ccPix.strrt[is][il][ip] = 0;
                    ccPix.adcr[is][il][ip] = 0;
                    ccPix.tdcr[is][il][ip] = 0;
               }
           }
       }
       
       if (app.isSingleEvent()) {
           for (int is=iis1 ; is<iis2 ; is++) {
               for (int il=0 ; il<2 ; il++) {
                    ccPix.strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).reset();
                    ccPix.strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,0).reset();
                    ccPix.strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,1).reset();
                    ccPix.strips.hmap2.get("H2_a_Hist").get(is+1,il+1,5).reset();
               }
           }
       }   
   }
   
   public void fill(int is, int il, int ip, int adc, float[] tdc, float tdcf, float adph) {
           
       for (int ii=0; ii<tdc.length; ii++) {
    	   
    	      if(tdc[ii]>0&&tdc[ii]<900){
      
             ccPix.nht[is-1][il-1]++; int inh = ccPix.nht[is-1][il-1];
    	         if(inh>nstr) inh=nstr;
             ccPix.tdcr[is-1][il-1][inh-1] = tdc[ii];
             ccPix.strrt[is-1][il-1][inh-1] = ip;
             ccPix.ph[is-1][il-1][inh-1] = adph;
             ccPix.strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc[ii],ip,1.0);
          }
    	      
    	       ccPix.strips.hmap2.get("H2_a_Hist").get(is,il,1).fill(adc,tdc[ii],1.0);   
    	       
       }
       
       
       if(adc>thrcc){
             ccPix.nha[is-1][il-1]++; int inh = ccPix.nha[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ccPix.adcr[is-1][il-1][inh-1] = adc;
             ccPix.tf[is-1][il-1][inh-1] = tdcf;
             ccPix.strra[is-1][il-1][inh-1] = ip;
             ccPix.strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.0);             
       } 
   }
   
   public void processSED() {
       
       for (int is=iis1; is<iis2; is++) {
          for (int il=0; il<2; il++ ){;
              for (int n=0 ; n<ccPix.nha[is][il] ; n++) {
                  int ip=ccPix.strra[is][il][n]; int ad=ccPix.adcr[is][il][n];
                  ccPix.strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).fill(ip,ad);
              }
          }
       }           
   } 
   
   public void findPixels() {
       
   }
   
   public void processPixels() {
       
   }

   public void makeMaps() {
	   
	   DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
	   DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
	   
       H2_a_Hist = ccPix.strips.hmap2.get("H2_a_Hist");
       H1_a_Sevd = ccPix.strips.hmap1.get("H1_a_Sevd");
       
       for (int is=1;is<7;is++) {
           for (int il=1 ; il<3 ; il++) {
               if (!app.isSingleEvent()) ccPix.Lmap_a.add(is,il,0, toTreeMap(H2_a_Hist.get(is,il,0).projectionY().getData())); //Strip View ADC 
               if  (app.isSingleEvent()) ccPix.Lmap_a.add(is,il,0, toTreeMap(H1_a_Sevd.get(is,il,0).getData()));           
           }
       }   

       ccPix.getLmapMinMax(is1,is2); 
       
   }    
}
    
    


