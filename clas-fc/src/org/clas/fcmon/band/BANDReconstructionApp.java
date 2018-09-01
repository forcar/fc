package org.clas.fcmon.band;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.clas.fcmon.tools.FADCFitter;
import org.clas.fcmon.tools.FCApplication;

import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedList.IndexGenerator;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.rec.tof.banks.BaseHit;
import org.jlab.rec.tof.banks.BaseHitReader;
import org.jlab.rec.tof.banks.BaseHitReader.DetectorLocation;

//import org.clas.fcmon.jroot.*;

public class BANDReconstructionApp extends FCApplication {
    
   FADCFitter     fitter  = new FADCFitter(1,15);
   String          mondet = null;
   
   String        BankType = null;
   int              detID = 0;
   
   Boolean stop = true;
   
   List<DetectorDataDgtz>        dataList = new ArrayList<DetectorDataDgtz>();
   IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(4);
   IndexedList<List<Float>>          adcs = new IndexedList<List<Float>>(4);
   IndexedList<List<Integer>>       lapmt = new IndexedList<List<Integer>>(3); 
   IndexedList<List<Integer>>       ltpmt = new IndexedList<List<Integer>>(3); 
   
   BANDConstants                   ftofcc = new BANDConstants();  
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nsa,nsb,tet,pedref;     
   int     thrcc = 20;
   short[] pulse = new short[100]; 
    
   public BANDReconstructionApp(String name, BANDPixels[] ftofPix) {
       super(name,ftofPix);
   }
   
   public void init() {
       System.out.println("BANDReconstruction.init()");
       mondet = (String) mon.getGlob().get("mondet");
       is1 = BANDConstants.IS1;
       is2 = BANDConstants.IS2;
      iis1 = BANDConstants.IS1-1;
      iis2 = BANDConstants.IS2-1;
   } 
   
   public void clearHistograms() {
       
       for (int idet=0; idet<bandPix.length; idet++) {
           for (int is=is1 ; is<is2 ; is++) {
               bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,0,0).reset();
               bandPix[idet].strips.hmap2.get("H2_t_Hist").get(is,0,0).reset();
               for (int il=1 ; il<3 ; il++) {
                   bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).reset();
                   bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,1).reset();
                   bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,3).reset();
                   bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,5).reset();
                   bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,6).reset();
                   bandPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).reset();
                   bandPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,2).reset();
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
       
       float      tps =  (float) 0.02345;
       float     tdcd = 0;
       
       clear(0); clear(1); clear(2); clear(3); clear(4); adcs.clear(); tdcs.clear(); ltpmt.clear() ; lapmt.clear();
       
       if(event.hasBank("BAND::tdc")){
           DataBank  bank = event.getBank("BAND::tdc");
           int rows = bank.rows();           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);                       
               int  ip = bank.getShort("component",i);
               tdcd = bank.getInt("TDC",i)*tps-app.tdcOffset;  
               
               if(isGoodSector(is)&&tdcd>0) {
               if(!tdcs.hasItem(is,il,lr-2,ip)) tdcs.add(new ArrayList<Float>(),is,il,lr-2,ip);
                   tdcs.getItem(is,il,lr-2,ip).add(tdcd); 
                   if (!ltpmt.hasItem(is,il,ip)) {
                	        ltpmt.add(new ArrayList<Integer>(),is,il,ip);
                	        ltpmt.getItem(is,il,ip).add(ip);
                   } 
               }
           }
       }
           
       if(event.hasBank("BAND::adc")){
           DataBank  bank = event.getBank("BAND::adc");
           int rows = bank.rows();
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);
               int  ip = bank.getShort("component",i);
               int adc = bank.getInt("ADC",i);
               float t = bank.getFloat("time",i);               
               int ped = bank.getShort("ped", i);
               
               if(isGoodSector(is)) {
               
               if(adc>0) {
               if(!adcs.hasItem(is,il,lr,ip)) adcs.add(new ArrayList<Float>(),is,il,lr,ip);
                   adcs.getItem(is,il,lr,ip).add((float) adc); 
                   if (!lapmt.hasItem(is,il,ip)) {
          	            lapmt.add(new ArrayList<Integer>(),is,il,ip);
                        lapmt.getItem(is,il,ip).add(ip);              
                   }
               }
                          
               Float[] tdcc; float[] tdc;
              
               if (tdcs.hasItem(is,il,lr,ip)) {
                   List<Float> list = new ArrayList<Float>();
                   list = tdcs.getItem(is,il,lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
                   tdc = new float[list.size()];
                   for (int ii=0; ii<tdcc.length; ii++) {
                	   tdc[ii] = tdcc[ii]-app.phaseCorrection*4;  
        	           float tdif = tdc[ii]-BANDConstants.TOFFSET[lr]-t;
                       bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,6).fill(tdif,ip);
                   }
               } else {
                   tdc = new float[1];
               }
               
               for (int ii=0 ; ii< 100 ; ii++) {
                   float wgt = (ii==(int)(t/4)) ? adc:0;
                   bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,wgt);
                   if (app.isSingleEvent()) {
                       bandPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,wgt);
                   }
               }
               
               if (app.rtt.hasItem(is,il,ip,lr)) {
                   int[] dum = (int[]) app.rtt.getItem(is,il,ip,lr);
                   getMode7(dum[0],dum[1],dum[2]);
               }
               
               if (ped>0) bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-ped, ip);  
               
               fill(il-1, is, lr+1, ip, adc, tdc, t, (float) adc);    
               
               } //isGoodSector?
           }
       }
       if (app.isHipoFileOpen) app.writer.writeEvent(event);       
      
   }  
   
   public void updateEvioData(DataEvent event) {
       
       float      tps =  (float) 0.02345;
       float     tdcd = 0;
       
       clear(0); clear(1); clear(2); clear(3); clear(4); adcs.clear(); tdcs.clear(); ltpmt.clear() ; lapmt.clear();

       List<DetectorDataDgtz> adcDGTZ = app.decoder.getEntriesADC(DetectorType.BAND);
       List<DetectorDataDgtz> tdcDGTZ = app.decoder.getEntriesTDC(DetectorType.BAND);

       for (int i=0; i < tdcDGTZ.size(); i++) {
           DetectorDataDgtz ddd=tdcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
           int il = ddd.getDescriptor().getLayer();
           int lr = ddd.getDescriptor().getOrder();
           int ip = ddd.getDescriptor().getComponent();               
           tdcd   = ddd.getTDCData(0).getTime()*tps-app.tdcOffset;  
           if(isGoodSector(is)&&tdcd>0) {
           if(!tdcs.hasItem(is,il,lr-2,ip)) tdcs.add(new ArrayList<Float>(),is,il,lr-2,ip);
               tdcs.getItem(is,il,lr-2,ip).add(tdcd);              
               if (!ltpmt.hasItem(is,il,ip)) {
      	            ltpmt.add(new ArrayList<Integer>(),is,il,ip);
                    ltpmt.getItem(is,il,ip).add(ip);
               }
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
           
           if (!adcs.hasItem(is,il,lr,ip))adcs.add(new ArrayList<Float>(),is,il,lr,ip);
                adcs.getItem(is,il,lr,ip).add((float)ad);                      
           if (!lapmt.hasItem(is,il,ip)) {
   	            lapmt.add(new ArrayList<Integer>(),is,il,ip);
                lapmt.getItem(is,il,ip).add(ip);
           }
           
           Float[] tdcc; float[] tdc;
           
           if (tdcs.hasItem(is,il,lr,ip)) {
               List<Float> list = new ArrayList<Float>();
               list = tdcs.getItem(is,il,lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
               tdc  = new float[list.size()];
               for (int ii=0; ii<tdcc.length; ii++) {
            	   tdc[ii] = tdcc[ii]-app.phaseCorrection*4;  
    	           float tdif = tdc[ii]-BANDConstants.TOFFSET[lr]-tf;
                   bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,6).fill(tdif,ip);
               }
           } else {
               tdc = new float[1];
           }
           
           getMode7(cr,sl,ch);            
           int ped = app.mode7Emulation.User_pedref==1 ? this.pedref:pd;
           
           for (int ii=0 ; ii< pulse.length ; ii++) {
               bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,pulse[ii]-ped);
               if (app.isSingleEvent()) {
                  bandPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,pulse[ii]-ped);
                  int w1 = t0-this.nsb ; int w2 = t0+this.nsa;
                  if (ad>0&&ii>=w1&&ii<=w2) bandPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,1).fill(ii,ip,pulse[ii]-ped);                     
               }
            }
           
           if (pd>0) bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-pd, ip);
           fill(il-1, is, lr+1, ip, ad, tdc, tf, ph);   
           
           } //isGoodSector ?          
       }
       
       if (app.isHipoFileOpen) writeHipoOutput();
       
   }
   
   public void writeHipoOutput() {
       
       DataEvent  decodedEvent = app.decoder.getDataEvent();
       DataBank   header = app.decoder.createHeaderBank(decodedEvent,0,0,0,0);
       decodedEvent.appendBanks(header);
       app.writer.writeEvent(decodedEvent);
              
   } 
   
   public void updateSimulatedData(DataEvent event) {
       
      float tdcmax=100000;
      int adc, tdcc, fac;
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
       
       for (int is=0 ; is<5 ; is++) {
    	   int nstr = bandPix[0].nstr[is];
           for (int il=0 ; il<2 ; il++) {
               bandPix[idet].nha[is][il] = 0;
               bandPix[idet].nht[is][il] = 0;
               for (int ip=0 ; ip<nstr ; ip++) {
                   bandPix[idet].strra[is][il][ip] = 0;
                   bandPix[idet].strrt[is][il][ip] = 0;
                   bandPix[idet].adcr[is][il][ip]  = 0;
                   bandPix[idet].tdcr[is][il][ip]  = 0;
                   bandPix[idet].tf[is][il][ip]    = 0;
                   bandPix[idet].ph[is][il][ip]    = 0;
               }
           }
       }
       
       if (app.isSingleEvent()) {
           for (int is=0 ; is<5 ; is++) {
               for (int il=0 ; il<2 ; il++) {
                    bandPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).reset();
                    bandPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,0).reset();
                    bandPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,1).reset();
                    bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is+1,il+1,5).reset();
                    bandPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).reset();
               }
           }
       }   
   }

   
   public void fill(int idet, int is, int il, int ip, int adc, float[] tdc, float tdcf, float adph) {

	   int nstr = bandPix[1].nstr[is-1];

	   for (int ii=0; ii<tdc.length; ii++) {
           
       if(tdc[ii]>0&&tdc[ii]<1000){
             bandPix[idet].nht[is-1][il-1]++; int inh = bandPix[idet].nht[is-1][il-1];
             if (inh>nstr) inh=nstr;
             bandPix[idet].ph[is-1][il-1][inh-1] = adph;
             bandPix[idet].tdcr[is-1][il-1][inh-1] = (float) tdc[ii];
             bandPix[idet].strrt[is-1][il-1][inh-1] = ip;
             bandPix[idet].ph[is-1][il-1][inh-1] = adph;
             bandPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc[ii],ip,1.0);
             }
       
       bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,1).fill(adc,tdc[ii],1.0);
          
       }
       
       if(adc>thrcc){
             bandPix[idet].nha[is-1][il-1]++; int inh = bandPix[idet].nha[is-1][il-1];
             if (inh>nstr) inh=nstr;
             bandPix[idet].adcr[is-1][il-1][inh-1] = adc;
             bandPix[idet].tf[is-1][il-1][inh-1] = tdcf;
             bandPix[idet].strra[is-1][il-1][inh-1] = ip;
             bandPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.0);
             } 
   }

   public void processCalib() {
	   
       IndexGenerator ig = new IndexGenerator();
       
       for (Map.Entry<Long,List<Integer>>  entry : lapmt.getMap().entrySet()){
           long hash = entry.getKey();
           int is = ig.getIndex(hash, 0);
           int il = ig.getIndex(hash, 1);
           int ip = ig.getIndex(hash, 2);
                  
        	   if(adcs.hasItem(is,il,0,ip)&&adcs.hasItem(is,il,1,ip)) {
               float gm = (float) Math.sqrt(adcs.getItem(is,il,0,ip).get(0)*
                                            adcs.getItem(is,il,1,ip).get(0));
        	       bandPix[il-1].strips.hmap2.get("H2_a_Hist").get(is, 0, 0).fill(gm,ip,1.0);  
        	   }
       }
       for (Map.Entry<Long,List<Integer>>  entry : ltpmt.getMap().entrySet()){
           long hash = entry.getKey();
           int is = ig.getIndex(hash, 0);
           int il = ig.getIndex(hash, 1);
           int ip = ig.getIndex(hash, 2);
            	   
        	   if(tdcs.hasItem(is,il,0,ip)&&tdcs.hasItem(is,il,1,ip)) {
                   float td = tdcs.getItem(is,il,0,ip).get(0)-tdcs.getItem(is,il,1,ip).get(0);
        	       bandPix[il-1].strips.hmap2.get("H2_t_Hist").get(is, 0, 0).fill(td,ip,1.0);  
        	       if(adcs.hasItem(is,il,0,ip)&&adcs.hasItem(is,il,1,ip)) {
        	    	   float lograt = (float) Math.log10(adcs.getItem(is,il,0,ip).get(0)/adcs.getItem(is,il,1,ip).get(0));
        	           bandPix[il-1].strips.hmap2.get("H2_t_Hist").get(is, ip, 2).fill(td,lograt);
        	       }    
        	   }
       }
      
   } 
              
   public void processSED() {
       
       for (int idet=0; idet<bandPix.length; idet++) {
       for (int is=0; is<5; is++) {
          for (int il=0; il<2; il++ ){;
          for (int n=0 ; n<bandPix[idet].nha[is][il] ; n++) {
              int ip=bandPix[idet].strra[is][il][n]; int ad=bandPix[idet].adcr[is][il][n];
              bandPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).fill(ip,ad);
          }
          for (int n=0 ; n<bandPix[idet].nht[is][il] ; n++) {
              int ip=bandPix[idet].strrt[is][il][n]; float td=bandPix[idet].tdcr[is][il][n];
              double tdc = 0.25*(td-BANDConstants.TOFFSET[il]);
              float  wgt = bandPix[idet].ph[is][il][n];
              wgt = (wgt > 0) ? wgt:1000;
              bandPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).fill((float)tdc,ip,wgt);
          }
          }
       } 
       }
   } 
   
   public void findPixels() {      
   }
   
   public void processPixels() {       
   }

   public void makeMaps(int idet) {
       DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
       DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
       DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
       
       H2_a_Hist = bandPix[idet].strips.hmap2.get("H2_a_Hist");
       H2_t_Hist = bandPix[idet].strips.hmap2.get("H2_t_Hist");
       H1_a_Sevd = bandPix[idet].strips.hmap1.get("H1_a_Sevd");
       
       for (int is=is1;is<is2;is++) {
           for (int il=1 ; il<3 ; il++) {
               if (!app.isSingleEvent()) bandPix[idet].Lmap_a.add(is,il,0, toTreeMap(H2_a_Hist.get(is,il,0).projectionY().getData())); //Strip View ADC 
               if (!app.isSingleEvent()) bandPix[idet].Lmap_t.add(is,il,0, toTreeMap(H2_t_Hist.get(is,il,0).projectionY().getData())); //Strip View TDC 
               if  (app.isSingleEvent()) bandPix[idet].Lmap_a.add(is,il,0, toTreeMap(H1_a_Sevd.get(is,il,0).getData()));           
           }
       } 
       
       bandPix[idet].getLmapMinMax(is1,is2,1,0); 
       bandPix[idet].getLmapMinMax(is1,is2,2,0); 

   }  
   
   
}
    
    


