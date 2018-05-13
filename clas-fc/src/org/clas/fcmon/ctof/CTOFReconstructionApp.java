package org.clas.fcmon.ctof;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.clas.fcmon.tools.FADCFitter;
import org.clas.fcmon.tools.FCApplication;

//groot
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;

//clas12rec
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedList.IndexGenerator;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;

//import org.clas.fcmon.jroot.*;

public class CTOFReconstructionApp extends FCApplication {
    
   FADCFitter     fitter  = new FADCFitter(1,15);
   String          mondet = null;
   
   String        BankType = null;
   int              detID = 0;
   
   CodaEventDecoder           codaDecoder = new CodaEventDecoder();
   DetectorEventDecoder   detectorDecoder = new DetectorEventDecoder();
   List<DetectorDataDgtz>        dataList = new ArrayList<DetectorDataDgtz>();
   IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(2);
   IndexedList<List<Float>>          adcs = new IndexedList<List<Float>>(2);
   IndexedList<List<Integer>>       lapmt = new IndexedList<List<Integer>>(1);
   IndexedList<List<Integer>>       ltpmt = new IndexedList<List<Integer>>(1);
   CTOFConstants                   ftofcc = new CTOFConstants();  
   
   double[]                sed7=null,sed8=null;
   TreeMap<Integer,Object> map7=null,map8=null; 
   
   int nstr = ctofPix[0].nstr;
      
   int nsa,nsb,tet,pedref;     
   int     thrcc = 20;
   short[] pulse = new short[100]; 
    
   public CTOFReconstructionApp(String name, CTOFPixels[] ctofPix) {
       super(name,ctofPix);
   }
   
   public void init() {
       System.out.println("CTOFReconstruction.init()");
       mondet = (String) mon.getGlob().get("mondet");
       is1 = CTOFConstants.IS1;
       is2 = CTOFConstants.IS2;
      iis1 = CTOFConstants.IS1-1;
      iis2 = CTOFConstants.IS2-1;
   } 
   
   public void clearHistograms() {
       
       for (int idet=0; idet<ctofPix.length; idet++) {
           for (int is=is1 ; is<is2 ; is++) {
               ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,0,0).reset();
               ctofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,0,0).reset();
               for (int il=1 ; il<3 ; il++) {
                   ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).reset();
                   ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,3).reset();
                   ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,5).reset();
                   ctofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).reset();
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
       
       clear(0); tdcs.clear(); adcs.clear(); lapmt.clear(); ltpmt.clear();
       
       if(event.hasBank("CTOF::tdc")){
           DataBank  bank = event.getBank("CTOF" + "::tdc");
           int rows = bank.rows();
           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);                       
               int  ip = bank.getShort("component",i);
               tdcd = bank.getInt("TDC",i)*tps;  
               
               if (!tdcs.hasItem(lr-2,ip)) tdcs.add(new ArrayList<Float>(),lr-2,ip);
                    tdcs.getItem(lr-2,ip).add(tdcd);              
               if (!ltpmt.hasItem(ip)) {
                    ltpmt.add(new ArrayList<Integer>(),ip);
                    ltpmt.getItem(ip).add(ip);
               }
           }
       }
              
       if(event.hasBank("CTOF::adc")){
           DataBank  bank = event.getBank("CTOF::adc");
           int rows = bank.rows();
           
           for(int i = 0; i < rows; i++){
               int  is = bank.getByte("sector",i);
               int  il = bank.getByte("layer",i);
               int  lr = bank.getByte("order",i);
               int  ip = bank.getShort("component",i);
               int adc = bank.getInt("ADC",i);
               float t = bank.getFloat("time",i);               
               int ped = bank.getShort("ped", i);     
               
               if (!adcs.hasItem(lr,ip)) adcs.add(new ArrayList<Float>(),lr,ip);
                    adcs.getItem(lr,ip).add((float)adc);            
               if (!lapmt.hasItem(ip)) {
                    lapmt.add(new ArrayList<Integer>(),ip);
                    lapmt.getItem(ip).add(ip);
               }
               
               Float[] tdcc; float[] tdc;
               
               if (tdcs.hasItem(lr,ip)) {
                   List<Float> list = new ArrayList<Float>();
                   list = tdcs.getItem(lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
                   tdc  = new float[list.size()];
                   for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-app.phaseCorrection*4;  ;  
               } else {
                   tdc = new float[1];
               }
               for (int ii=0 ; ii< 100 ; ii++) {
                   float wgt = (ii==(int)(t/4)) ? adc:0;
                   ctofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,wgt);
                   if (app.isSingleEvent()) {
                       ctofPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,wgt);
                   }
               }
               
               if (app.rtt.hasItem(is,il,ip,lr)) {
                   int[] dum = (int[]) app.rtt.getItem(is,il,ip,lr);
                   getMode7(dum[0],dum[1],dum[2]);
               } 
               
               if (ped>0) ctofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-ped, ip);

               fill(il-1, is, lr+1, ip, adc, tdc, t, (float) adc);    
           }
       }
       if (app.isHipoFileOpen&&isGoodMIP(isSingleTrack())) app.writer.writeEvent(event);       
   }  
   
   public void updateEvioData(DataEvent event) {
       
       clear(0); tdcs.clear(); adcs.clear(); lapmt.clear(); ltpmt.clear();
              
//       app.decoder.detectorDecoder.setTET(app.mode7Emulation.tet);
//       app.decoder.detectorDecoder.setNSA(app.mode7Emulation.nsa);
//       app.decoder.detectorDecoder.setNSB(app.mode7Emulation.nsb);
       
       float phase = app.phase;
              
       List<DetectorDataDgtz> adcDGTZ = app.decoder.getEntriesADC(DetectorType.CTOF);
       List<DetectorDataDgtz> tdcDGTZ = app.decoder.getEntriesTDC(DetectorType.CTOF);

       for (int i=0; i < tdcDGTZ.size(); i++) {
           DetectorDataDgtz ddd=tdcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
           int il = ddd.getDescriptor().getLayer();
           int lr = ddd.getDescriptor().getOrder();
           int ip = ddd.getDescriptor().getComponent();
           
           if (!tdcs.hasItem(lr-2,ip)) tdcs.add(new ArrayList<Float>(),lr-2,ip);
                tdcs.getItem(lr-2,ip).add((float) ddd.getTDCData(0).getTime()*24/1000);              
           if (!ltpmt.hasItem(ip)) {
        	    ltpmt.add(new ArrayList<Integer>(),ip);
                ltpmt.getItem(ip).add(ip);
           }
       }
      
       for (int i=0; i < adcDGTZ.size(); i++) {
           DetectorDataDgtz ddd=adcDGTZ.get(i);
           int is = ddd.getDescriptor().getSector();
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
           
           if (!adcs.hasItem(lr,ip)) adcs.add(new ArrayList<Float>(),lr,ip);
                adcs.getItem(lr,ip).add((float)ad);            
           if (!lapmt.hasItem(ip)) {
                lapmt.add(new ArrayList<Integer>(),ip);
                lapmt.getItem(ip).add(ip);
           }
                
           Float[] tdcc; float[] tdc;
           
           if (tdcs.hasItem(lr,ip)) {
               List<Float> list = new ArrayList<Float>();
               list = tdcs.getItem(lr,ip); tdcc=new Float[list.size()]; list.toArray(tdcc);
               tdc  = new float[list.size()];
               for (int ii=0; ii<tdcc.length; ii++) tdc[ii] = tdcc[ii]-phase*4;  
           } else {
               tdc = new float[1];
           }
           
           ctofPix[il-1].strips.hmap2.get("H2_t_Hist").get(is,3,3).fill((double) tdc[0]+phase*4,(double) phase);
           ctofPix[il-1].strips.hmap2.get("H2_t_Hist").get(is,3,4).fill(tdc[0],phase);
           
           getMode7(cr,sl,ch); 
           int ped = app.mode7Emulation.User_pedref==1 ? this.pedref:pd;
           
           for (int ii=0 ; ii< pulse.length ; ii++) {
               ctofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,5).fill(ii,ip,pulse[ii]-ped);
               if (app.isSingleEvent()) {
                  ctofPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,0).fill(ii,ip,pulse[ii]-ped);
                  int w1 = t0-this.nsb ; int w2 = t0+this.nsa;
                  if (ad>0&&ii>=w1&&ii<=w2) ctofPix[il-1].strips.hmap2.get("H2_a_Sevd").get(is,lr+1,1).fill(ii,ip,pulse[ii]-ped);                     
               }
            }
           
           if (pd>0) ctofPix[il-1].strips.hmap2.get("H2_a_Hist").get(is,lr+1,3).fill(this.pedref-pd, ip);
           
           fill(il-1, is, lr+1, ip, ad, tdc, tf, ph);   
                   
       }
       
       if (app.isHipoFileOpen&&isGoodMIP(isSingleTrack())) writeHipoOutput();
       
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
      
      String det[] = {"CTOF"};  
      
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
               ctofPix[idet].nha[is][il] = 0;
               ctofPix[idet].nht[is][il] = 0;
               for (int ip=0 ; ip<nstr ; ip++) {
                   ctofPix[idet].strra[is][il][ip] = 0;
                   ctofPix[idet].strrt[is][il][ip] = 0;
                   ctofPix[idet].adcr[is][il][ip]  = 0;
                   ctofPix[idet].tdcr[is][il][ip]  = 0;
                   ctofPix[idet].tf[is][il][ip]    = 0;
                   ctofPix[idet].ph[is][il][ip]    = 0;
               }
           }
       }
       
       if (app.isSingleEvent()) {
           for (int is=iis1 ; is<iis2 ; is++) {
               for (int il=0 ; il<2 ; il++) {
                    ctofPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).reset();
                    ctofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,0).reset();
                    ctofPix[idet].strips.hmap2.get("H2_a_Sevd").get(is+1,il+1,1).reset();
                    ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is+1,il+1,5).reset();
                    ctofPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).reset();
               }
           }
       }   
   }

   
   public void fill(int idet, int is, int il, int ip, int adc, float[] tdc, float tdcf, float adph) {
	  
       for (int ii=0; ii<tdc.length; ii++) {
           
       if(tdc[ii]>ctofPix[idet].tlim[0]&&tdc[ii]<ctofPix[idet].tlim[1]){
             ctofPix[idet].nht[is-1][il-1]++; int inh = ctofPix[idet].nht[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ctofPix[idet].ph[is-1][il-1][inh-1] = adph;
             ctofPix[idet].tdcr[is-1][il-1][inh-1] = (float) tdc[ii];
             ctofPix[idet].strrt[is-1][il-1][inh-1] = ip;
             ctofPix[idet].ph[is-1][il-1][inh-1] = adph;
             ctofPix[idet].strips.hmap2.get("H2_t_Hist").get(is,il,0).fill(tdc[ii],ip,1.0);
       }
       
       ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,1).fill(adc,tdc[ii],1.0);
          
       }
       
       if(adc>thrcc){
             ctofPix[idet].nha[is-1][il-1]++; int inh = ctofPix[idet].nha[is-1][il-1];
             if (inh>nstr) inh=nstr;
             ctofPix[idet].adcr[is-1][il-1][inh-1] = adc;
             ctofPix[idet].tf[is-1][il-1][inh-1] = tdcf;
             ctofPix[idet].strra[is-1][il-1][inh-1] = ip;
             ctofPix[idet].strips.hmap2.get("H2_a_Hist").get(is,il,0).fill(adc,ip,1.0);
       } 
   }
   
   public int isSingleTrack() {
	   
	   if(!app.isFilter) return -1;
	   
	   int[] ip = new int[2];
	   int pdif = 0;
	   
       if(lapmt.getMap().size()==2) {
           int n = 0;
           IndexGenerator ig = new IndexGenerator();
           for (Map.Entry<Long,List<Integer>>  entry : lapmt.getMap().entrySet()){              
               ip[n] = ig.getIndex(entry.getKey(), 0);n++;
           }
           pdif  = Math.abs(ip[0]-ip[1]); 
       }
       
       return pdif>0 ? pdif:-1;
	 
   }
   
   public Boolean isGoodMIP(int pdif) {
	   return app.isFilter?(pdif>21&&pdif<25):true;
   }
   
   public void processCalib() {
	   int pdif = isSingleTrack();
	   if (isGoodMIP(pdif)) processMIP();
//	   if (pdif>0) processTrack(pdif);
   }
   
   public void processMIP() {
	   
       IndexGenerator ig = new IndexGenerator();
       
       for (Map.Entry<Long,List<Integer>>  entry : lapmt.getMap().entrySet()){
           int ip = ig.getIndex(entry.getKey(), 0);
        	   if(adcs.hasItem(0,ip)&&adcs.hasItem(1,ip)) {
                   float gm = (float) Math.sqrt(adcs.getItem(0,ip).get(0)*
                                                adcs.getItem(1,ip).get(0));
        	       ctofPix[0].strips.hmap2.get("H2_a_Hist").get(1, 0, 0).fill(gm,ip,1.0);  
        	   }
       }
       
       for (Map.Entry<Long,List<Integer>>  entry : ltpmt.getMap().entrySet()){           
           int ip = ig.getIndex(entry.getKey(), 0);
        	   if(tdcs.hasItem(0,ip)&&tdcs.hasItem(1,ip)) {
                   float td = tdcs.getItem(0,ip).get(0) -
            		          tdcs.getItem(1,ip).get(0);               
        	       ctofPix[0].strips.hmap2.get("H2_t_Hist").get(1, 0, 0).fill(td,ip,1.0); 
        	       if(adcs.hasItem(0,ip)&&adcs.hasItem(1,ip)) {
        	    	   float lograt = (float) Math.log10(adcs.getItem(0,ip).get(0)/adcs.getItem(1,ip).get(0));
        	           ctofPix[0].strips.hmap2.get("H2_t_Hist").get(1, ip, 2).fill(td,lograt);
        	       }
        	   }
       }      
   }
   
   public void processTrack(int pdif) {
	   
	   if(ltpmt.getMap().size()!=2) return;
		   
       float tsum[] = new float[2];
       int ipp[] = new int[2];
	   int n = 0;
       IndexGenerator ig = new IndexGenerator();
       
       for (Map.Entry<Long,List<Integer>>  entry : ltpmt.getMap().entrySet()){           
           int ip = ig.getIndex(entry.getKey(), 0);
        	   if(tdcs.hasItem(0,ip)&&tdcs.hasItem(1,ip)) {
        		    ipp[n] = ip;
                   tsum[n] = (float) 0.5*(tdcs.getItem(0,ip).get(0) +
            		                      tdcs.getItem(1,ip).get(0)); 
                   n++;
        	   }
       }  

       if (ipp[0]==12 || ipp[1]==12) ctofPix[0].strips.hmap2.get("H2_t_Hist").get(1, 0, 1).fill(tsum[1]-tsum[0],pdif,1.0);  
      
   }
   
   public void processSED() {
       
       for (int idet=0; idet<ctofPix.length; idet++) {
       for (int is=iis1; is<iis2; is++) {
          for (int il=0; il<2; il++ ){;
          for (int n=0 ; n<ctofPix[idet].nha[is][il] ; n++) {
              int ip=ctofPix[idet].strra[is][il][n]; int ad=ctofPix[idet].adcr[is][il][n];
              ctofPix[idet].strips.hmap1.get("H1_a_Sevd").get(is+1,il+1,0).fill(ip,ad);
          }
          for (int n=0 ; n<ctofPix[idet].nht[is][il] ; n++) {
              int ip=ctofPix[idet].strrt[is][il][n]; float td=ctofPix[idet].tdcr[is][il][n];
              double tdc = 0.25*(td-CTOFConstants.TOFFSET);
              float  wgt = ctofPix[idet].ph[is][il][n];
              wgt = (wgt > 0) ? wgt:1000;
              ctofPix[idet].strips.hmap2.get("H2_t_Sevd").get(is+1,il+1,0).fill((float)tdc,ip,wgt);
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
       
       H2_a_Hist = ctofPix[idet].strips.hmap2.get("H2_a_Hist");
       H2_t_Hist = ctofPix[idet].strips.hmap2.get("H2_t_Hist");
       H1_a_Sevd = ctofPix[idet].strips.hmap1.get("H1_a_Sevd");
       
       for (int is=is1;is<is2;is++) {
           for (int il=1 ; il<3 ; il++) {
               if (!app.isSingleEvent()) ctofPix[idet].Lmap_a.add(is,il,0, toTreeMap(H2_a_Hist.get(is,il,0).projectionY().getData())); //Strip View ADC 
               if (!app.isSingleEvent()) ctofPix[idet].Lmap_t.add(is,il,0, toTreeMap(H2_t_Hist.get(is,il,0).projectionY().getData())); //Strip View TDC 
               if  (app.isSingleEvent()) ctofPix[idet].Lmap_a.add(is,il,0, toTreeMap(H1_a_Sevd.get(is,il,0).getData()));           
           }
       } 
       
       ctofPix[idet].getLmapMinMax(is1,is2,1,0); 
       ctofPix[idet].getLmapMinMax(is1,is2,2,0); 

   }  
   
   
}
    
    


