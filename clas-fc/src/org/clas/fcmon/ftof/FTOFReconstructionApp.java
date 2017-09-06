package org.clas.fcmon.ftof;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.clas.fcmon.tools.DetectorEventDecoder;
import org.clas.fcmon.tools.FADCFitter;
import org.clas.fcmon.tools.FCApplication;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
//import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedList.IndexGenerator;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.rec.tof.banks.BaseHit;
import org.jlab.rec.tof.banks.BaseHitReader;
import org.jlab.rec.tof.banks.BaseHitReader.DetectorLocation;

//import org.clas.fcmon.jroot.*;

public class FTOFReconstructionApp extends FCApplication {
    
   FADCFitter     fitter  = new FADCFitter(1,15);
   String          mondet = null;
   
   String        BankType = null;
   int              detID = 0;
   
   Boolean stop = true;
   
   CodaEventDecoder           codaDecoder = new CodaEventDecoder();
   DetectorEventDecoder   detectorDecoder = new DetectorEventDecoder();
   List<DetectorDataDgtz>        dataList = new ArrayList<DetectorDataDgtz>();
   IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(4);
   IndexedList<List<Float>>          adcs = new IndexedList<List<Float>>(4);
   
   FTOFConstants                   ftofcc = new FTOFConstants();  
   
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
       mondet = (String) mon.getGlob().get("mondet");
       is1 = FTOFConstants.IS1;
       is2 = FTOFConstants.IS2;
      iis1 = FTOFConstants.IS1-1;
      iis2 = FTOFConstants.IS2-1;
   } 
   
   public void clearHistograms() {
       
       for (int idet=0; idet<ftofPix.length; idet++) {
           for (int is=is1 ; is<is2 ; is++) {
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
        } else {
           processPixels();  // Process only single pixels 
           processCalib();   // Quantities for display and calibration engine
        }
    }
   
//   public String detID(int layer) {
//       return "FTOF";
//   }
   
   public void getHits(DataEvent event) {
	   
       BaseHitReader hitReader = new BaseHitReader();
       
       Map<DetectorLocation, ArrayList<BaseHit>> hitMap = hitReader.get_Hits(event, "FTOF");
       
       System.out.println(" ");
       System.out.println("New Event Size "+hitMap.size());
       
       if (hitMap != null) {

           Set entrySet = hitMap.entrySet();
           Iterator it = entrySet.iterator();

           while (it.hasNext()) {
               Map.Entry me = (Map.Entry) it.next();
               ArrayList<BaseHit> hitList = (ArrayList<BaseHit>) me.getValue();
               
               List<ArrayList<BaseHit>> hitlists = new ArrayList<ArrayList<BaseHit>>();  
               Collections.sort(hitList);
              
   			  for(BaseHit h : hitList)
   				System.out.println("Sector "+h.get_Sector()+
   						           " Layer "+h.get_Layer()+
   						             " PMT "+h.get_Component()+
   						            " ADC1 "+h.ADC1+
   						            " ADC2 "+h.ADC2+
   						            " TDC1 "+h.TDC1+
   						            " TDC2 "+h.TDC2+
   						           " ADCi1 "+h.ADCbankHitIdx1+
   						           " ADCi2 "+h.ADCbankHitIdx2+
   						           " TDCi1 "+h.TDCbankHitIdx1+
   						           " TDCi2 "+h.TDCbankHitIdx2);
   						           
               for (int i = 0; i < hitList.size(); i++) {
                   hitlists.add(new ArrayList<BaseHit>());
               }

           }
       }	   	   
   }
   
   public void updateHipoData(DataEvent event) {
       
       int       evno = 0;       
       int    trigger = 0;
       float      tps = 24;
       float     tdcf = 0;
       float     tdcd = 0;
       float   tdcmax = 0;
       float   offset = 0;
       long     phase = 0;
       long timestamp = 0;    
       
       clear(0); clear(1); clear(2); tdcs.clear(); adcs.clear();
       
       if (app.isMC)  offset=600;
       if (app.isMCB) {app.isMC=true; offset=600-(float)124.25;}
       
       if(!app.isMC&&event.hasBank("RUN::config")){
           DataBank bank = event.getBank("RUN::config");
           timestamp = bank.getLong("timestamp",0);
           trigger   = bank.getInt("trigger",0);
           evno      = bank.getInt("event",0);         
           int phase_offset = 1;
           phase = ((timestamp%6)+phase_offset)%6;
           app.bitsec = (int) (Math.log10(trigger>>24)/0.301+1);
       }
       
//       getHits(event);
      
       if(event.hasBank("FTOF::tdc")){
           DataBank  bank = event.getBank("FTOF::tdc");
           int rows = bank.rows();           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);                       
               int  ip = bank.getShort("component",i);
               tdcd = bank.getInt("TDC",i)*tps/1000;  
               
               if(tdcd>0) {
               if(!tdcs.hasItem(is,il,lr-2,ip)) tdcs.add(new ArrayList<Float>(),is,il,lr-2,ip);
                   tdcs.getItem(is,il,lr-2,ip).add(tdcd); 
               }
           }
       }
           
       if(event.hasBank("FTOF::adc")){
           DataBank  bank = event.getBank("FTOF::adc");
           int rows = bank.rows();
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);
               int  ip = bank.getShort("component",i);
               int adc = bank.getInt("ADC",i);
               float t = bank.getFloat("time",i);               
               int ped = bank.getShort("ped", i);
               
               if(adc>0) {
               if(!adcs.hasItem(is,il,lr,ip)) adcs.add(new ArrayList<Float>(),is,il,lr,ip);
                   adcs.getItem(is,il,lr,ip).add((float) adc); 
               }
               
               IndexGenerator ig = new IndexGenerator();
               for (Map.Entry<Long,List<Float>>  entry : adcs.getMap().entrySet()){
            	      long hash = entry.getKey();
            	      int his = ig.getIndex(hash, 0);
            	      int hil = ig.getIndex(hash, 1);
            	      int hlr = ig.getIndex(hash, 2);
            	      int hip = ig.getIndex(hash, 3);
//                  System.out.println("SEC "+his+" LAY "+hil+" LR "+hlr+" PMT "+hip+" ADC "+entry.getValue().get(0));
               }              
               
               Float[] tdcc; float[] tdc;
              
               if (tdcs.hasItem(is,il,lr,ip)) {
                   List<Float> list = new ArrayList<Float>();
                   list = tdcs.getItem(is,il,lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
                   tdc = new float[list.size()];
                   for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-tdcmax+offset-phase*4;  
               } else {
                   tdc = new float[1];
               }
               
               for (int ii=0 ; ii< 100 ; ii++) {
                   float wgt = (ii==(int)(t/4)) ? adc:0;
                   ftofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,wgt);
                   if (app.isSingleEvent()) {
                       ftofPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,wgt);
                   }
               }
                              
               if (ped>0) ftofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-ped, ip);  
               
               if(isGoodSector(is)) fill(il-1, is, lr+1, ip, adc, tdc, t, (float) adc);    
           }
       }
       
   }  
   
   public void updateRawData(DataEvent event) {
       
       clear(0); clear(1); clear(2); tdcs.clear();
       
       app.decoder.initEvent(event);
       app.bitsec = app.decoder.bitsec;
       
       List<DetectorDataDgtz> adcDGTZ = app.decoder.getEntriesADC(DetectorType.FTOF);
       List<DetectorDataDgtz> tdcDGTZ = app.decoder.getEntriesTDC(DetectorType.FTOF);

       for (int i=0; i < tdcDGTZ.size(); i++) {
           DetectorDataDgtz ddd=tdcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
           int il = ddd.getDescriptor().getLayer();
           int lr = ddd.getDescriptor().getOrder();
           int ip = ddd.getDescriptor().getComponent();
           if(!tdcs.hasItem(is,il,lr-2,ip)) tdcs.add(new ArrayList<Float>(),is,il,lr-2,ip);
               tdcs.getItem(is,il,lr-2,ip).add((float) ddd.getTDCData(0).getTime()*24/1000);              
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
           
           Float[] tdcc; float[] tdc;
           
           if (tdcs.hasItem(is,il,lr,ip)) {
               List<Float> list = new ArrayList<Float>();
               list = tdcs.getItem(is,il,lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
               tdc  = new float[list.size()];
               for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-app.decoder.phase*4;  
           } else {
               tdc = new float[1];
           }
           
           getMode7(cr,sl,ch); 
           
           for (int ii=0 ; ii< pulse.length ; ii++) {
               ftofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,pulse[ii]-pd);
               if (app.isSingleEvent()) {
                  ftofPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,pulse[ii]-pd);
                  int w1 = t0-this.nsb ; int w2 = t0+this.nsa;
                  if (ad>0&&ii>=w1&&ii<=w2) ftofPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,1).fill(ii,ip,pulse[ii]-pd);                     
               }
            }
           
           if (pd>0) ftofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-pd, ip);
           fill(il-1, is, lr+1, ip, ad, tdc, tf, ph);   
           
           }           
       }
       
       if (app.decoder.isHipoFileOpen) writeHipoOutput();
       
   }
   
   public void writeHipoOutput() {
       
       DataEvent  decodedEvent = app.decoder.getDataEvent();
       DataBank   header = app.decoder.createHeaderBank(decodedEvent);
       decodedEvent.appendBanks(header);
       app.decoder.writer.writeEvent(decodedEvent);
              
   } 
   
   public void updateSimulatedData(DataEvent event) {
       
      float tdcmax=100000;
      int nrows, adc, tdcc, fac;
      float mc_t=0,tdcf=0;
      float[] tdc = new float[1];
      
      String det[] = {"FTOF1A","FTOF1B","FTOF2B"}; // FTOF.xml banknames
      
      clear(0); clear(1); clear(2);
      
      for (int idet=0; idet<det.length ; idet++) {
          
          if(event.hasBank(det[idet]+"::true")==true) {
              EvioDataBank bank  = (EvioDataBank) event.getBank(det[idet]+"::true"); 
              for(int i=0; i < bank.rows(); i++) mc_t = (float) bank.getDouble("avgT",i);          
          }
         
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
                   tdc[0] = (((float)tdcc-(float)mc_t*1000)-tdcmax+1340000)/1000; 
                 fill(idet, is, 1, ip, adc, tdc, tdcf, tdcf); 
                      adc = bank.getInt("ADCR",i);
                     tdcc = bank.getInt("TDCR",i);
                     tdcf = tdcc;
                   tdc[0] = (((float)tdcc-(float)mc_t*1000)-tdcmax+1340000)/1000; 
                 fill(idet, is, 2, ip, adc, tdc, tdcf, tdcf); 
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
                   ftofPix[idet].tf[is][il][ip]    = 0;
                   ftofPix[idet].ph[is][il][ip]    = 0;
               }
           }
       }
       
       if (app.isSingleEvent()) {
           for (int is=0 ; is<6 ; is++) {
               for (int il=0 ; il<2 ; il++) {
                    ftofPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).reset();
                    ftofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,0).reset();
                    ftofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,1).reset();
                    ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is+1,il+1,5).reset();
                    ftofPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).reset();
               }
           }
       }   
   }

   
   public void fill(int idet, int is, int il, int ip, int adc, float[] tdc, float tdcf, float adph) {

       for (int ii=0; ii<tdc.length; ii++) {
           
       if(tdc[ii]>450&&tdc[ii]<850){
             ftofPix[idet].nht[is-1][il-1]++; int inh = ftofPix[idet].nht[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ftofPix[idet].ph[is-1][il-1][inh-1] = adph;
             ftofPix[idet].tdcr[is-1][il-1][inh-1] = (float) tdc[ii];
             ftofPix[idet].strrt[is-1][il-1][inh-1] = ip;
             ftofPix[idet].ph[is-1][il-1][inh-1] = adph;
             ftofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc[ii],ip,1.0);
             }
       
       ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,1).fill(adc,tdc[ii],1.0);
          
       }
       
       if(adc>thrcc){
             ftofPix[idet].nha[is-1][il-1]++; int inh = ftofPix[idet].nha[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ftofPix[idet].adcr[is-1][il-1][inh-1] = adc;
             ftofPix[idet].tf[is-1][il-1][inh-1] = tdcf;
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
                   ftofPix[idet].strips.hmap2.get("H2_a_Hist").get(is, 0, 0).fill(gm,ipL,1.0);
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
          for (int n=0 ; n<ftofPix[idet].nht[is][il] ; n++) {
              int ip=ftofPix[idet].strrt[is][il][n]; float td=ftofPix[idet].tdcr[is][il][n];
              double tdc = 0.25*(td-FTOFConstants.TOFFSET);
              float  wgt = ftofPix[idet].ph[is][il][n];
              wgt = (wgt > 0) ? wgt:1000;
              ftofPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).fill((float)tdc,ip,wgt);
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

   public void makeMaps(int idet) {
       DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
       DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
       DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
       
       H2_a_Hist = ftofPix[idet].strips.hmap2.get("H2_a_Hist");
       H2_t_Hist = ftofPix[idet].strips.hmap2.get("H2_t_Hist");
       H1_a_Sevd = ftofPix[idet].strips.hmap1.get("H1_a_Sevd");
       
       for (int is=is1;is<is2;is++) {
           for (int il=1 ; il<3 ; il++) {
               if (!app.isSingleEvent()) ftofPix[idet].Lmap_a.add(is,il,0, toTreeMap(H2_a_Hist.get(is,il,0).projectionY().getData())); //Strip View ADC 
               if (!app.isSingleEvent()) ftofPix[idet].Lmap_t.add(is,il,0, toTreeMap(H2_t_Hist.get(is,il,0).projectionY().getData())); //Strip View TDC 
               if  (app.isSingleEvent()) ftofPix[idet].Lmap_a.add(is,il,0, toTreeMap(H1_a_Sevd.get(is,il,0).getData()));           
           }
       } 
       
       ftofPix[idet].getLmapMinMax(is1,is2,1,0); 
       ftofPix[idet].getLmapMinMax(is1,is2,2,0); 

   }  
   
   
}
    
    


