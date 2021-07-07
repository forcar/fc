package org.clas.fcmon.cnd;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.clas.fcmon.tools.FCApplication;

//groot
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

//clas12rec
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedList.IndexGenerator;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;

//import org.clas.fcmon.jroot.*;

public class CNDReconstructionApp extends FCApplication {
    
   String          mondet = null;
   
   String        BankType = null;
   int              detID = 0;
   
   CodaEventDecoder           codaDecoder = new CodaEventDecoder();
   List<DetectorDataDgtz>        dataList = new ArrayList<DetectorDataDgtz>();
   IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(3);
   IndexedList<List<Float>>          adcs = new IndexedList<List<Float>>(3);
   IndexedList<List<Integer>>       lapmt = new IndexedList<List<Integer>>(1); 
   IndexedList<List<Integer>>       ltpmt = new IndexedList<List<Integer>>(1); 
   
   CNDConstants                    ftofcc = new CNDConstants();  
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nstr = cndPix[0].nstr;
       
   int nsa,nsb,tet,pedref;     
   int     thrcc = 20;
   short[] pulse = new short[100]; 
    
   public CNDReconstructionApp(String name, CNDPixels[] ctofPix) {
       super(name,ctofPix);
   }
   
   public void init() {
       System.out.println("CNDReconstruction.init()");
       mondet = (String) mon.getGlob().get("mondet");
       is1 = CNDConstants.IS1;
       is2 = CNDConstants.IS2;
      iis1 = CNDConstants.IS1-1;
      iis2 = CNDConstants.IS2-1;
   } 
   
   public void clearHistograms() {
       
       for (int idet=0; idet<cndPix.length; idet++) {
           for (int is=is1 ; is<is2 ; is++) {
               cndPix[idet].strips.hmap2.get("H2_a_Hist").get(is,0,0).reset();
               cndPix[idet].strips.hmap2.get("H2_t_Hist").get(is,0,0).reset();
               for (int il=1 ; il<3 ; il++) {
                   cndPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).reset();
                   cndPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,3).reset();
                   cndPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,5).reset();
                   cndPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).reset();
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
   
   public void updateHipoData(DataEvent event) {

       float    tps =  (float) 0.02345;
       float offset = 0;
       float   tdcd = 0;
       
       clear(0); tdcs.clear(); adcs.clear(); ltpmt.clear(); lapmt.clear();
       
       if(event.hasBank("CND::tdc")){
           DataBank  bank = event.getBank("CND::tdc");
           int rows = bank.rows();           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);                       
               int  ip = bank.getShort("component",i);
               tdcd = bank.getInt("TDC",i)*tps-app.tdcOffset;  
              
               if (!tdcs.hasItem(is,lr-2,il)) tdcs.add(new ArrayList<Float>(),is,lr-2,il);
                    tdcs.getItem(is,lr-2,il).add(tdcd);              
               if (!ltpmt.hasItem(is)) {
       	            ltpmt.add(new ArrayList<Integer>(),is);
                    ltpmt.getItem(is).add(il);
               }   
           }
       }
              
       if(event.hasBank("CND::adc")){
           DataBank  bank = event.getBank("CND::adc");
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
                   for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-app.phaseCorrection*4;  
               } else {
                   tdc = new float[1];
               }
               for (int ii=0 ; ii< 100 ; ii++) {
                   float wgt = (ii==(int)(t/4)) ? adc:0;
                   cndPix[0].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,il,wgt);
                   if (app.isSingleEvent()) {
                       cndPix[0].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,il,wgt);
                   }
               }
               
               if (app.rtt.hasItem(is,il,ip,lr)) {
                   int[] dum = (int[]) app.rtt.getItem(is,il,ip,lr);                  
                   getMode7(dum[0],dum[1],dum[2]);                  
               }
               
               if (ped>0) cndPix[0].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-ped, il);
               
               if(isGoodSector(is)) fill(0, is, lr+1, il, adc, tdc, t, (float) adc);    
           }
       }
       
   }  
   
   public void updateEvioData(DataEvent event) {
       
       float      tps =  (float) 0.02345;
       float     tdcd = 0;
       
       clear(0); tdcs.clear(); adcs.clear(); lapmt.clear(); ltpmt.clear();
       
//       app.decoder.detectorDecoder.setTET(app.mode7Emulation.tet);
//       app.decoder.detectorDecoder.setNSA(app.mode7Emulation.nsa);
//       app.decoder.detectorDecoder.setNSB(app.mode7Emulation.nsb);
       
       float phase = app.phase;
       phase = 0;
       
       if (app.isSingleEvent()) {
    	 System.out.println(" ");       
         System.out.println("Event Number "+app.getEventNumber());
       }
              
       List<DetectorDataDgtz> adcDGTZ = app.decoder.getEntriesADC(DetectorType.CND);
       List<DetectorDataDgtz> tdcDGTZ = app.decoder.getEntriesTDC(DetectorType.CND);
       
       for (int i=0; i < tdcDGTZ.size(); i++) {
           DetectorDataDgtz ddd=tdcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
           int il = ddd.getDescriptor().getLayer();
           int lr = ddd.getDescriptor().getOrder();
           int ip = ddd.getDescriptor().getComponent();   
           tdcd = ddd.getTDCData(0).getTime()*tps-app.tdcOffset;
           
           if (app.isSingleEvent()) System.out.println("Sector "+is+" Layer "+il+" Order "+lr+"TDC "+tdcd);
           
           if (!tdcs.hasItem(is,lr-2,il)) tdcs.add(new ArrayList<Float>(),is,lr-2,il);
                tdcs.getItem(is,lr-2,il).add(tdcd);  
           if (!ltpmt.hasItem(is)) {
                ltpmt.add(new ArrayList<Integer>(),is);
                ltpmt.getItem(is).add(is);
           }
       }
       
       for (int i=0; i < adcDGTZ.size(); i++) {
    	   
           DetectorDataDgtz ddd=adcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
//           if (isGoodSector(is)) {
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
          
          if (!adcs.hasItem(is,lr,il)) adcs.add(new ArrayList<Float>(),is,lr,il);
               adcs.getItem(is,lr,il).add((float)ad);                
          if (!lapmt.hasItem(is)) {
               lapmt.add(new ArrayList<Integer>(),is);
               lapmt.getItem(is).add(is);
          }           
           
           Float[] tdcc; float[] tdc;
           
           if (tdcs.hasItem(is,lr,il)) {
               List<Float> list = new ArrayList<Float>();
               list = tdcs.getItem(is,lr,il); tdcc=new Float[list.size()]; list.toArray(tdcc);
               tdc  = new float[list.size()];
               for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-phase*4;  
           } else {
               tdc = new float[1];
           }
           
           getMode7(cr,sl,ch); 
           int ped = app.mode7Emulation.User_pedref==1 ? this.pedref:pd;
                      
           for (int ii=0 ; ii< pulse.length ; ii++) {
               cndPix[0].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,il,pulse[ii]-ped);
               if (app.isSingleEvent()) {
                  cndPix[0].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,il,pulse[ii]-ped);
                  int w1 = t0-this.nsb ; int w2 = t0+this.nsa;
                  if (ad>0&&ii>=w1&&ii<=w2) cndPix[0].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,1).fill(ii,il,pulse[ii]-ped);                     
               }
            }
           
           if (pd>0) cndPix[0].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-pd, il);
           fill(0, is, lr+1, il, ad, tdc, tf, ph);   
           
           }           
//       }       
       
   }
   
   public void writeHipoOutput() {
       
       DataEvent  decodedEvent = app.decoder.getDataEvent();
       DataBank   header = app.decoder.createHeaderBank(decodedEvent,0,0,0,0);
       decodedEvent.appendBanks(header);
       app.writer.writeEvent(decodedEvent);
              
   } 
   
   public void updateSimulatedData(DataEvent event) {
       
      float tdcmax=100000;
      int nrows, adc, tdcc, fac;
      float mc_t=0,tdcf=0;
      float[] tdc = new float[1];
      
      String det[] = {"CND"};
      
      clear(0); 
      
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
       
       for (int is=iis1 ; is<iis2 ; is++) {
           for (int il=0 ; il<2 ; il++) {
               cndPix[idet].nha[is][il] = 0;
               cndPix[idet].nht[is][il] = 0;
               for (int ip=0 ; ip<nstr ; ip++) {
                   cndPix[idet].strra[is][il][ip] = 0;
                   cndPix[idet].strrt[is][il][ip] = 0;
                   cndPix[idet].adcr[is][il][ip]  = 0;
                   cndPix[idet].tdcr[is][il][ip]  = 0;
                   cndPix[idet].tf[is][il][ip]    = 0;
                   cndPix[idet].ph[is][il][ip]    = 0;
               }
           }
       }
       
       if (app.isSingleEvent()) {
           for (int is=iis1 ; is<iis2 ; is++) {
               for (int il=0 ; il<2 ; il++) {
                    cndPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).reset();
                    cndPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,0).reset();
                    cndPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,1).reset();
                    cndPix[idet].strips.hmap2.get("H2_a_Hist").get(is+1,il+1,5).reset();
                    cndPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).reset();
               }
           }
       }   
   }

   
   public void fill(int idet, int is, int il, int ip, int adc, float[] tdc, float tdcf, float adph) {

       for (int ii=0; ii<tdc.length; ii++) {
           
       if(tdc[ii]>cndPix[0].tlim[0]&&tdc[ii]<cndPix[0].tlim[1]){
             cndPix[idet].nht[is-1][il-1]++; int inh = cndPix[idet].nht[is-1][il-1];
             if (inh>nstr) inh=nstr;
             cndPix[idet].ph[is-1][il-1][inh-1] = adph;
             cndPix[idet].tdcr[is-1][il-1][inh-1] = (float) tdc[ii];
             cndPix[idet].strrt[is-1][il-1][inh-1] = ip;
             cndPix[idet].ph[is-1][il-1][inh-1] = adph;            
             cndPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc[ii],ip,1.0);
       }
       
       cndPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,ip).fill(adc,tdc[ii],1.0);
          
       }
       
       if(adc>thrcc){
             cndPix[idet].nha[is-1][il-1]++; int inh = cndPix[idet].nha[is-1][il-1];
             if (inh>nstr) inh=nstr;
             cndPix[idet].adcr[is-1][il-1][inh-1] = adc;
             cndPix[idet].tf[is-1][il-1][inh-1] = tdcf;
             cndPix[idet].strra[is-1][il-1][inh-1] = ip;
             cndPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.0);
             } 
   }
   
   public boolean isGoodMIP() {
	   
	   int[] ip = new int[2];
       if(lapmt.getMap().size()==2) {
           int n = 0;
           IndexGenerator ig = new IndexGenerator();
           for (Map.Entry<Long,List<Integer>>  entry : lapmt.getMap().entrySet()){              
               ip[n] = ig.getIndex(entry.getKey(), 0);n++;
           }
       }
       int pdif  = Math.abs(ip[0]-ip[1]);      
       return ip[0]>0&&ip[1]>0&&pdif==12;
	 
   }  
   
   public void processCalib() {
	   
//	   if (!isGoodMIP()) return;
	   
       IndexGenerator ig = new IndexGenerator();
       
       for (Map.Entry<Long,List<Integer>>  entry : lapmt.getMap().entrySet()){
           long hash = entry.getKey();
           int is = ig.getIndex(hash, 0);
           for (int il=1; il<4; il++) {
        	     if(adcs.hasItem(is,0,il)&&adcs.hasItem(is,1,il)) {
                 float gm = (float) Math.sqrt(adcs.getItem(is,0,il).get(0)*
                                              adcs.getItem(is,1,il).get(0));
        	         cndPix[0].strips.hmap2.get("H2_a_Hist").get(is, 0, 0).fill(gm,il,1.0);  
        	   }
           }
       }
       
       for (Map.Entry<Long,List<Integer>>  entry : ltpmt.getMap().entrySet()){
           long hash = entry.getKey();
           int is = ig.getIndex(hash, 0);
           for (int il=1; il<4; il++) {
        	   if(tdcs.hasItem(is,0,il)&&tdcs.hasItem(is,1,il)) {
               float td = tdcs.getItem(is,0,il).get(0) - tdcs.getItem(is,1,il).get(0);
        	       cndPix[0].strips.hmap2.get("H2_t_Hist").get(is, 0, 0).fill(td,il,1.0); 
        	       if(adcs.hasItem(is,0,il)&&adcs.hasItem(is,1,il)) {
        	    	   float lograt = (float) Math.log10(adcs.getItem(is,0,il).get(0)/adcs.getItem(is,1,il).get(0));
        	           cndPix[0].strips.hmap2.get("H2_t_Hist").get(is, il, 4).fill(td,lograt);
        	       }        	       
        	   }
           }
       }      
   }
   
   public void processSED() {
       
       for (int idet=0; idet<cndPix.length; idet++) {
       for (int is=iis1; is<iis2; is++) {
          for (int il=0; il<2; il++ ){;
          for (int n=0 ; n<cndPix[idet].nha[is][il] ; n++) {
              int ip=cndPix[idet].strra[is][il][n]; int ad=cndPix[idet].adcr[is][il][n];
              cndPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).fill(ip,ad);
          }
          for (int n=0 ; n<cndPix[idet].nht[is][il] ; n++) {
              int ip=cndPix[idet].strrt[is][il][n]; float td=cndPix[idet].tdcr[is][il][n];
              double tdc = 0.25*(td-CNDConstants.TOFFSET);
              float  wgt = cndPix[idet].ph[is][il][n];
              wgt = (wgt > 0) ? wgt:1000;
              cndPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).fill((float)tdc,ip,wgt);
          }
          }
       } 
       }
   } 
   
   public void findPixels() {      
   }
   
   @Override
   public void processPixels(int idet) {  
	   app.goodFilterEvent = isGoodMIP();
   }

   public void makeMaps(int idet) {
       DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
       DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
       DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
       
       H2_a_Hist = cndPix[idet].strips.hmap2.get("H2_a_Hist");
       H2_t_Hist = cndPix[idet].strips.hmap2.get("H2_t_Hist");
       H1_a_Sevd = cndPix[idet].strips.hmap1.get("H1_a_Sevd");
       
       for (int is=is1;is<is2;is++) {
           for (int lr=1 ; lr<3 ; lr++) {
               if (!app.isSingleEvent()) cndPix[idet].Lmap_a.add(is,lr,0, toTreeMap(H2_a_Hist.get(is,lr,0).projectionY().getData())); //Strip View ADC 
               if (!app.isSingleEvent()) cndPix[idet].Lmap_t.add(is,lr,0, toTreeMap(H2_t_Hist.get(is,lr,0).projectionY().getData())); //Strip View TDC 
               if  (app.isSingleEvent()) cndPix[idet].Lmap_a.add(is,lr,0, toTreeMap(H1_a_Sevd.get(is,lr,0).getData()));           
           }
       } 
       
       cndPix[idet].getLmapMinMax(is1,is2); 
       cndPix[idet].getLmapMinMax(is1,is2); 

   }  
   
   
}
    
    


