package org.clas.fcmon.ec;

/**
 * 
 * @author lcsmith
 * Adapted from Rafayel Paremuzjan clas12 trigger code
 * https://github.com/JeffersonLab/clas12-trigger/blob/master/users/rafopar/Read_VTP.cc
 */

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.clas.fcmon.tools.FCTrigger;
import org.clas.fcmon.tools.FCTrigger.TEC_Cluster;
import org.clas.fcmon.tools.FCTrigger.TEC_Peak;
import org.clas.fcmon.tools.TECGeom;
import org.clas.fcmon.tools.TPCGeom;
import org.clas.fcmon.tools.TriggerDataDgtz;
import org.jlab.coda.jevio.ByteDataTransformer;
import org.jlab.coda.jevio.EvioNode;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioTreeBranch;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedList.IndexGenerator;

public class ECTriggerApp extends FCApplication{
	
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed   trigger = new EmbeddedCanvasTabbed("TriggerBits");   
    EmbeddedCanvas               c = this.getCanvas(this.getName());
    TriggerDataDgtz           trig = new TriggerDataDgtz();
    
    //CTOF
    IndexedList<List<Float>>          tdcs = new IndexedList<List<Float>>(2);
    IndexedList<List<Float>>          adcs = new IndexedList<List<Float>>(2);
    IndexedList<List<Integer>>       lapmt = new IndexedList<List<Integer>>(1);
    IndexedList<List<Integer>>       ltpmt = new IndexedList<List<Integer>>(1);
    
    
    String tbit = "Trigger Bits: ECAL.PCAL.HTCC(0)    ECAL.PCAL.HTCC(1-6)   MUON(7-12)     1K Pulser(31)";
    
    double Dalitz_ECout_max = 0.0833333;
    double Dalitz_ECout_min = -0.0555556;
    double       pixcut[][] = {{-0.03,0.03,},{-0.07,0.02}};

    double r2d = 57.2957795130823229;
    
    double coord_conv[][] = {{8,8,8},{2.75,3.00,3.00}};
    
    int n_sect = 6;
    int n_view = 3;
    int nbits = 0;

    int adcECvtp_tagmin = 100;
    int adcECvtp_tagmax = 112;

    double ADC2GeV = 1. / 10000.;

    double E_bin_width = 0.02;
    int n_E_bins = 5;

    int ftrig_crate_ID = 37;
    int trig_bank_tag = 57610;
    String newtit = null;
    
    public ECTriggerApp(String name, ECPixels[] ecPix) {
        super(name, ecPix);
        trig = new TriggerDataDgtz();
    }
	
    public void init() {
        System.out.println("ECTrigApp:init():");
        createHistos();
        GStyle.getGraphErrorsAttributes().setMarkerStyle(1);
        GStyle.getGraphErrorsAttributes().setMarkerColor(2);
        GStyle.getGraphErrorsAttributes().setMarkerSize(3);
        GStyle.getGraphErrorsAttributes().setLineColor(2);
        GStyle.getGraphErrorsAttributes().setLineWidth(1);
        GStyle.getGraphErrorsAttributes().setFillStyle(1);
        PCMon_zmax=30000;
        PCMon_zmin=100;
    }
    
     public JPanel getPanel() {        
         engineView.setLayout(new BorderLayout());
         trigger.addCanvas("EC Peaks");
         trigger.addCanvas("PC Peaks");
         trigger.addCanvas("EC Clusters");
         trigger.addCanvas("PC Clusters");
         trigger.addCanvas("XvsY");
         trigger.addCanvas("Thresholds");
         trigger.addCanvas("Elastic");
         engineView.add(trigger);
         return engineView;       
	 }  
     
     public void titXY(DataGroup group, String name, String x, String y)  {    
         if(x!="") group.getH2F(name).setTitleX(x);
         if(y!="") group.getH2F(name).setTitleY(y);
     }

     public void createHistos( ) {
    	 
         this.getDataGroup().clear();
         
         DataGroup dg = new DataGroup();
         
 	     GStyle.getH1FAttributes().setLineWidth(1);
  	     	     
         H1F trigbit1 = new H1F(tbit, tbit, 32,-0.5,31.5);
         trigbit1.setFillColor(4); trigbit1.setTitleX("Trigger Bits"); trigbit1.setTitleY("Counts");        
         dg.addDataSet(trigbit1, 0);
         this.getDataGroup().add(dg,  0,0,0);  
         
         H1F trigbit2 = new H1F(tbit, tbit, 32,-0.5,31.5);
         trigbit2.setFillColor(4); trigbit2.setTitleX("Trigger Bits w/ MIP cut"); trigbit2.setTitleY("Counts");        
         dg.addDataSet(trigbit2, 0);
         this.getDataGroup().add(dg,  0,0,1);        

         dg = new DataGroup();
         
         GStyle.getH2FAttributes().setTitleY("Sector");
         dg.addDataSet(new H2F("h_Dalitz_Clust1", 40, -0.5, 0.5, 6, 0.5, 6.5),1); titXY(dg,"h_Dalitz_Clust1","EC Cluster Dalitz","");   
      	
         dg.addDataSet(new H2F("h_N_Clust1", 11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_Clust1","No. of EC Clusters","");  
         dg.addDataSet(new H2F("h_N_Clust2", 11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_Clust2","No. of EC In Time Clusters","");  
         dg.addDataSet(new H2F("h_cl_t1",    21, -0.5, 20.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_t1","EC Cluster Time","");     	    
         dg.addDataSet(new H2F("h_cl_E1",   200,  0.,   1.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_E1","EC Cluster Energy","");       
         dg.addDataSet(new H2F("h_cl_E2",   200,  0.,   1.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_E2","EC Cluster In Time Energy","");       
         dg.addDataSet(new H2F("h_cl_E3",   100,  0.,   0.2, 6, 0.5, 6.5),1); titXY(dg,"h_cl_E3","EC Cluster Energy","");       
         dg.addDataSet(new H2F("h_cl_E4",   100,  0.,   0.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_E4","PC+EC Cluster Energy","");       
         dg.addDataSet(new H2F("h_cl_U1",    41, -0.5, 40.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_U1","EC U Strip","");    
         dg.addDataSet(new H2F("h_cl_V1",    41, -0.5, 40.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_V1","EC V Strip","");    
         dg.addDataSet(new H2F("h_cl_W1",    41, -0.5, 40.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_W1","EC W Strip","");    
        	    
     	 GStyle.getH2FAttributes().setTitleY("EC Clusters Y (cm)"); GStyle.getH2FAttributes().setTitleX("EC Clusters X (cm)");
     	 dg.addDataSet(new H2F("h_yxc1",    200, -500., 500., 200, -500., 500.),1);  

     	 dg.addDataSet(new H2F("h_yxc_UV1", 200, -500., 500., 200, -500., 500.),1); titXY(dg,"h_yxc_UV1","EC Peaks UV X (cm)","EC Peaks UV Y (cm)");
     	 dg.addDataSet(new H2F("h_yxc_UW1", 200, -500., 500., 200, -500., 500.),1); titXY(dg,"h_yxc_UW1","EC Peaks UW X (cm)","EC Peaks UW Y (cm)");
         dg.addDataSet(new H2F("h_yxc_VW1", 200, -500., 500., 200, -500., 500.),1); titXY(dg,"h_yxc_VW1","EC Peaks VW X (cm)","EC Peaks VW Y (cm)");
     	    
     	 GStyle.getH2FAttributes().setTitleY("EC Theta (deg)"); GStyle.getH2FAttributes().setTitleX("EC Phi (deg)");
     	 dg.addDataSet(new H2F("h_th_phi_cl1", 360,    0., 360., 200,    0.,  50.),1);    	    
     	 for (int i = 0; i < n_E_bins; i++) {
     	   	dg.addDataSet(new H2F("h_th_phi_cl_"+i, 360, 0., 360., 200, 0., 50.),1);
     	 }  
     	    
         GStyle.getH2FAttributes().setTitleY("Sector");
         dg.addDataSet(new H2F("h_Dalitz_Peaks1",  40, -0.5, 0.5, 6, 0.5, 6.5),1); titXY(dg,"h_Dalitz_Peaks1","EC Peak Dalitz","");  

     	 String uvw[] = {" U "," V "," W "};
     	    
     	 for (int i_view = 0; i_view < n_view; i_view++) {
     		dg.addDataSet(new H2F("h_N_peaks1_"+i_view,    11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_peaks1_"+i_view,"No. of"+uvw[i_view]+"Peaks","");
     		dg.addDataSet(new H2F("h_N_peaks2_"+i_view,    11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_peaks2_"+i_view,"No. of"+uvw[i_view]+"In Time Peaks","");
     		dg.addDataSet(new H2F("h_N_peaks3_"+i_view,    11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_peaks3_"+i_view,"No. of"+uvw[i_view]+"Peaks","");
     		dg.addDataSet(new H2F("h_t_peak1_"+i_view,     21, -0.5, 20.5, 6, 0.5, 6.5),1); titXY(dg,"h_t_peak1_"+i_view,"EC"+uvw[i_view]+"Peak Time","");
     		dg.addDataSet(new H2F("h_coord_peak1_"+i_view, 41, -0.5, 40.5, 6, 0.5, 6.5),1); titXY(dg,"h_coord_peak1_"+i_view,"EC"+uvw[i_view]+"Strip","");
     		dg.addDataSet(new H2F("h_energy_peak1_"+i_view, 100, 0.0, 0.1, 6, 0.5, 6.5),1); titXY(dg,"h_energy_peak1_"+i_view,"EC"+uvw[i_view]+"Peak Energy","");
     		dg.addDataSet(new H2F("h_energy_peak2_"+i_view, 100, 0.0, 0.1, 6, 0.5, 6.5),1); titXY(dg,"h_energy_peak2_"+i_view,"EC"+uvw[i_view]+"Peak Energy","");
     	 }
     	 
         this.getDataGroup().add(dg,  1,0,0);        
         dg = new DataGroup();
     	        	         	    
     	 dg.addDataSet(new H2F("h_Dalitz_Clust1", 40, -0.5, 0.5, 6, 0.5, 6.5),1); titXY(dg,"h_Dalitz_Clust1","PC Cluster Dalitz","");   
     	    
     	 dg.addDataSet(new H2F("h_N_Clust1", 11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_Clust1","No. of PC Clusters",""); 
     	 dg.addDataSet(new H2F("h_N_Clust2", 11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_Clust2","No. of PC In Time Clusters","");
     	 dg.addDataSet(new H2F("h_N_Clust3", 11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_Clust3","No. of PC Clusters","");
     	 dg.addDataSet(new H2F("h_cl_t1",    21, -0.5, 20.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_t1","PC Cluster Time","");
     	 dg.addDataSet(new H2F("h_cl_E1",   200,  0.,   1.5,  6, 0.5, 6.5),1); titXY(dg,"h_cl_E1","PC Cluster Energy",""); 
     	 dg.addDataSet(new H2F("h_cl_E2",   200,  0.,   1.5,  6, 0.5, 6.5),1); titXY(dg,"h_cl_E2","PC Cluster In Time Energy",""); 
     	 dg.addDataSet(new H2F("h_cl_E3",   100,  0.,   0.2,  6, 0.5, 6.5),1); titXY(dg,"h_cl_E3","PC Cluster Energy",""); 

     	 dg.addDataSet(new H2F("h_cl_U1",    86, -0.5, 85.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_U1","PC U Strip","");
     	 dg.addDataSet(new H2F("h_cl_V1",    86, -0.5, 85.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_V1","PC V Strip","");
     	 dg.addDataSet(new H2F("h_cl_W1",    86, -0.5, 85.5, 6, 0.5, 6.5),1); titXY(dg,"h_cl_W1","PC W Strip","");
     	    
     	 dg.addDataSet(new H1F("h_dU_43", "", 600, -90., 90.),1);
     	 dg.addDataSet(new H1F("h_dV_43", "", 600, -80., 80.),1);
     	 dg.addDataSet(new H1F("h_dW_43", "", 600, -80., 80.),1);
     	        	    
     	 GStyle.getH2FAttributes().setTitleY("PC Clusters Y (cm)"); GStyle.getH2FAttributes().setTitleX("PC Clusters X (cm)");
     	 dg.addDataSet(new H2F("h_yxc1",    200, -500., 500., 200, -500., 500.),1);
     	 dg.addDataSet(new H2F("h_yxc2",    200, -500., 500., 200, -500., 500.),1);
     	 dg.addDataSet(new H2F("h_yxc3",    200, -500., 500., 200, -500., 500.),1);
     	 dg.addDataSet(new H2F("h_yxc4",    200, -500., 500., 200, -500., 500.),1);
     	    
         dg.addDataSet(new H2F("h_yxc_UV1", 200, -500., 500., 200, -500., 500.),1); titXY(dg,"h_yxc_UV1","PC Peaks UV X (cm)","PC Peaks UV Y (cm)");
         dg.addDataSet(new H2F("h_yxc_UW1", 200, -500., 500., 200, -500., 500.),1); titXY(dg,"h_yxc_UW1","PC Peaks UW X (cm)","PC Peaks UW Y (cm)");
         dg.addDataSet(new H2F("h_yxc_VW1", 200, -500., 500., 200, -500., 500.),1); titXY(dg,"h_yxc_VW1","PC Peaks VW X (cm)","PC Peaks VW Y (cm)");

     	 GStyle.getH2FAttributes().setTitleY("PC Theta (deg)"); GStyle.getH2FAttributes().setTitleX("PC Phi (deg)");
         dg.addDataSet(new H2F("h_th_phi_cl1", 360, 0., 360., 200, 0., 50.),1);
     	    
         for (int i = 0; i < n_E_bins; i++) {
        	 dg.addDataSet(new H2F("h_th_phi_cl_"+i, 360, 0., 360., 200, 0., 50.),1);
     	 } 
     	    
         GStyle.getH2FAttributes().setTitleY("Sector");
         dg.addDataSet(new H2F("h_Dalitz_Peaks1",  40, -0.5, 0.5, 6, 0.5, 6.5),1); titXY(dg,"h_Dalitz_Peaks1","PC Peak Dalitz",""); 
     	    
     	 for (int i_view = 0; i_view < n_view; i_view++) {
     		 dg.addDataSet(new H2F("h_N_peaks1_"+i_view,      11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_peaks1_"+i_view,"No. of"+uvw[i_view]+"Peaks","");
     		 dg.addDataSet(new H2F("h_N_peaks2_"+i_view,      11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_peaks2_"+i_view,"No. of"+uvw[i_view]+"In Time Peaks","");
     		 dg.addDataSet(new H2F("h_N_peaks3_"+i_view,      11, -0.5, 10.5, 6, 0.5, 6.5),1); titXY(dg,"h_N_peaks3_"+i_view,"No. of"+uvw[i_view]+"Peaks","");
     		 dg.addDataSet(new H2F("h_t_peak1_"+i_view,       21, -0.5, 20.5, 6, 0.5, 6.5),1); titXY(dg,"h_t_peak1_"+i_view,"PC"+uvw[i_view]+"Peak Time","");
     		 dg.addDataSet(new H2F("h_coord_peak1_"+i_view,   86, -0.5, 85.5, 6, 0.5, 6.5),1); titXY(dg,"h_coord_peak1_"+i_view,"PC"+uvw[i_view]+"Strip","");
     		 dg.addDataSet(new H2F("h_energy_peak1_"+i_view, 100,  0.0,  0.1, 6, 0.5, 6.5),1); titXY(dg,"h_energy_peak1_"+i_view,"PC"+uvw[i_view]+"Peak Energy","");
     		 dg.addDataSet(new H2F("h_energy_peak2_"+i_view, 100,  0.0,  0.1, 6, 0.5, 6.5),1); titXY(dg,"h_energy_peak2_"+i_view,"PC"+uvw[i_view]+"Peak Energy","");
         }
        
         this.getDataGroup().add(dg,  2,0,0);        
 /*
     	    H1F h_HTCC_time1     = new H1F("h_HTCC_time1", "", 101, -0.5, 100.5);
     	    H1F h_n_HTCC_masks   = new H1F("h_n_HTCC_masks", "", 11, -0.5, 10.5);
     	    H1F h_n_HTCC_hits    = new H1F("h_n_HTCC_hits", "", 50, -0.5, 49.5);
     	    H1F h_HTCC_hit_chan1 = new H1F("h_HTCC_hit_chan1", "", 50, -0.5, 49.5);

     	    H2F h_FTOF_time1     = new H2F("h_FTOF_time1", "", 51, -0.5, 50.5, 7, -0.5, 6.5);
     	    H2F h_n_FTOF_masks   = new H2F("h_n_FTOF_masks", "", 11, -0.5, 10.5, 7, -0.5, 6.5);
     	    H2F h_n_FTOF_hits    = new H2F("h_n_FTOF_hits", "", 64, -0.5, 63.5, 7, -0.5, 6.5);
     	    H2F h_FTOF_hit_chan1 = new H2F("h_FTOF_hit_chan1", "", 64, -0.5, 63.5, 7, -0.5, 6.5);    	    
  */   	        	    
     }
     
     public void clearHistograms() {
         System.out.println("ECTriggerApp:clearHistograms():");        
         createHistos();
     }
     
     public void getCTOF(DataEvent event) {
         
         tdcs.clear(); adcs.clear(); lapmt.clear(); ltpmt.clear();
         
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
//             if (isGoodSector(is)) {
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
             
//             }           
         }
         
//         if (app.decoder.isHipoFileOpen&&isGoodMIP(isSingleTrack())) writeHipoOutput();
        
         
     }     
     public void processMIP() {
  	   
         IndexGenerator ig = new IndexGenerator();
         for (Map.Entry<Long,List<Integer>>  entry : lapmt.getMap().entrySet()){
             int ip = ig.getIndex(entry.getKey(), 0);
          	   if(adcs.hasItem(0,ip)&&adcs.hasItem(1,ip)) {
                     float gm = (float) Math.sqrt(adcs.getItem(0,ip).get(0)*
                                                  adcs.getItem(1,ip).get(0));
//          	          System.out.println("CTOF ip,gm = "+ip+" "+gm);
          	   }
         } 
     }
     
     public void addEvent(DataEvent event) {
         if (!testTriggerMask()) return;        
//         getCTOF(event); processMIP();
         linlog = true; if(app.isSingleEvent()) {linlog = false; clearHistograms();}
         if(event instanceof EvioDataEvent) trig.getTriggerBank((EvioDataEvent) event);             
         if(event instanceof HipoDataEvent) trig.getTriggerBank(event);                   
      	 fillTriggerBitHistos();
         fillVTPHistos();
     }
     
     public void fillTriggerBitHistos() {
    	 nbits=0;
         if (app.isSingleEvent()) this.getDataGroup().getItem(0,0,0).getH1F(tbit).reset();
         for (int i=0; i<32; i++) {
        	 if(isTrigBitSet(i)) {
        		 this.getDataGroup().getItem(0,0,0).getH1F(tbit).fill(i);         	 
        		 if(i>6 && i<13) nbits++;
        	 }
         }
     }
     
     public void fillVTPHistos() {
    	 
    	 int trigtime = 7;
    	 DataGroup dg = new DataGroup();
         
    	 IndexGenerator ig = new IndexGenerator();
         
         for (Map.Entry<Long,List<TEC_Peak>>  entry : trig.peaks.getMap().entrySet()){
             long hash = entry.getKey();
             int is = ig.getIndex(hash, 0);
             int id = ig.getIndex(hash, 1);
             int iv = ig.getIndex(hash, 2);  
             int n_peaks = trig.peaks.getItem(is,id,iv).size();
             
             dg = this.getDataGroup().getItem(id,0,0);
             dg.getH2F("h_N_peaks1_"+iv).fill(n_peaks, is);
            
             int npeaks=0;
             for (int i_peak=0; i_peak<n_peaks; i_peak++) {
            	     int      time = trig.peaks.getItem(is,id,iv).get(i_peak).time;
            	     double  coord = trig.peaks.getItem(is,id,iv).get(i_peak).coord/coord_conv[id-1][iv];
            	     double energy = trig.peaks.getItem(is,id,iv).get(i_peak).energy * ADC2GeV;
            	     dg.getH2F("h_t_peak1_"+iv).fill(time, is);
            	     dg.getH2F("h_coord_peak1_"+iv).fill(coord, is);
            	     dg.getH2F("h_energy_peak1_"+iv).fill(energy, is);              
            	     if(time == trigtime) {npeaks++; dg.getH2F("h_energy_peak2_"+iv).fill(energy, is);}           	     
             } 
             dg.getH2F("h_N_peaks2_"+iv).fill(npeaks, is);
         }
         
         float esum[][] = new float[6][2];
         
         for (Map.Entry<Long,List<TEC_Cluster>>  entry : trig.clusters.getMap().entrySet()){
             long hash = entry.getKey();
             int is = ig.getIndex(hash, 0);
             int id = ig.getIndex(hash, 1);  
             int n_cl = trig.clusters.getItem(is,id).size(); 
             
             dg = this.getDataGroup().getItem(id,0,0);
             dg.getH2F("h_N_Clust1").fill(n_cl, is);

             int n_cl_in_1_tbin = 0; double cl_E = 0; double cl_Dalitz=0;
             
             for (int i_cl = 0; i_cl < n_cl; i_cl++) {
                 int    cl_T = trig.clusters.getItem(is,id).get(i_cl).time;
                 double cl_U = trig.clusters.getItem(is,id).get(i_cl).Ustrip / coord_conv[id-1][0];
                 double cl_V = trig.clusters.getItem(is,id).get(i_cl).Vstrip / coord_conv[id-1][1];
                 double cl_W = trig.clusters.getItem(is,id).get(i_cl).Wstrip / coord_conv[id-1][2];
                        cl_E = trig.clusters.getItem(is,id).get(i_cl).energy * ADC2GeV;
                 
                 double hall_x_cl=0,hall_y_cl=0,hall_z_cl=0;
                 cl_Dalitz=-100;
                 
                 if (id==1) {
                     TECGeom ec_geom = new TECGeom(cl_U, cl_V, cl_W);
                     ec_geom.SetSector(is-1);
                     hall_x_cl = -ec_geom.GetHallX_UV();
                     hall_y_cl =  ec_geom.GetHallY_UV();
                     hall_z_cl =  ec_geom.GetHallZ_UV();
                     cl_Dalitz =  ec_geom.GetDalitz();
                 }
                 
                 if (id==2) {
                     TPCGeom ec_geom = new TPCGeom(cl_U, cl_V, cl_W);
                     ec_geom.SetSector(is-1);
                     hall_x_cl = -ec_geom.GetHallX_VW();
                     hall_y_cl =  ec_geom.GetHallY_VW();
                     hall_z_cl =  ec_geom.GetHallZ_VW();
                     cl_Dalitz =  ec_geom.GetDalitz();
                 }

                 int E_bin = (int) Math.min((int)((cl_E-0.4) / E_bin_width), n_E_bins - 1); // for not being out of range
                 
                 double phi_cl = Math.atan2(hall_y_cl, hall_x_cl) * r2d + 30.;             
                 if (phi_cl < 0.) phi_cl = phi_cl + 360.;             
                 double th_cl = Math.atan(Math.sqrt(hall_x_cl * hall_x_cl + hall_y_cl * hall_y_cl) / hall_z_cl) * r2d;

                 dg.getH2F("h_th_phi_cl1").fill(phi_cl, th_cl);
                 if (E_bin>=0) dg.getH2F("h_th_phi_cl_"+E_bin).fill(phi_cl, th_cl);
                 
                 dg.getH2F("h_yxc1").fill(hall_x_cl, hall_y_cl);
                 dg.getH2F("h_Dalitz_Clust1").fill(cl_Dalitz, is);

                 dg.getH2F("h_cl_t1").fill(cl_T, is);
                 dg.getH2F("h_cl_U1").fill(cl_U, is);
                 dg.getH2F("h_cl_V1").fill(cl_V, is);
                 dg.getH2F("h_cl_W1").fill(cl_W, is);
                 dg.getH2F("h_cl_E1").fill(cl_E, is);

                 if(cl_T == trigtime) n_cl_in_1_tbin++;

             }
             if(n_cl_in_1_tbin==1 && nbits==1 && cl_Dalitz>pixcut[id-1][0] && cl_Dalitz<pixcut[id-1][1]) {
            	 dg.getH2F("h_cl_E2").fill(cl_E, is);
                 dg.getH2F("h_cl_E3").fill(cl_E, is);
             }
             dg.getH2F("h_N_Clust2").fill(n_cl_in_1_tbin, is);                          
         }
         
         for (int is=1; is<7; is++) {
         for (int id=1; id<3; id++) {
    	     dg = this.getDataGroup().getItem(id,0,0);
         for (int iU = 0; iU < (trig.peaks.hasItem(is,id,0)?trig.peaks.getItem(is,id,0).size():0); iU++) {
             for (int iV = 0; iV < (trig.peaks.hasItem(is,id,1)?trig.peaks.getItem(is,id,1).size():0); iV++) {
                 for (int iW = 0; iW < (trig.peaks.hasItem(is,id,2)?trig.peaks.getItem(is,id,2).size():0); iW++) {
                     
                	     double hall_x_UV=0,hall_y_UV=0,hall_x_UW=0,hall_y_UW=0,hall_x_VW=0,hall_y_VW=0,Dalitz_peaks=0;
                	     
                	     if(id==1) {
                         TECGeom ec_geom_peaks = new TECGeom(trig.peaks.getItem(is,id,0).get(iU).coord / coord_conv[id-1][0], 
                                                             trig.peaks.getItem(is,id,1).get(iV).coord / coord_conv[id-1][1], 
                                                             trig.peaks.getItem(is,id,2).get(iW).coord / coord_conv[id-1][2]);
                         ec_geom_peaks.SetSector(is-1);

                         hall_x_UV = -ec_geom_peaks.GetHallX_UV();
                         hall_y_UV =  ec_geom_peaks.GetHallY_UV();
                         hall_x_UW = -ec_geom_peaks.GetHallX_UW();
                         hall_y_UW =  ec_geom_peaks.GetHallY_UW();
                         hall_x_VW = -ec_geom_peaks.GetHallX_VW();
                         hall_y_VW =  ec_geom_peaks.GetHallY_VW();
                         Dalitz_peaks = ec_geom_peaks.GetDalitz();
                	     }
                	     
                	     if(id==2) {
                         TPCGeom ec_geom_peaks = new TPCGeom(trig.peaks.getItem(is,id,0).get(iU).coord / coord_conv[id-1][0], 
                                                             trig.peaks.getItem(is,id,1).get(iV).coord / coord_conv[id-1][1], 
                                                             trig.peaks.getItem(is,id,2).get(iW).coord / coord_conv[id-1][2]);
                         ec_geom_peaks.SetSector(is-1);

                         hall_x_UV = -ec_geom_peaks.GetHallX_UV();
                         hall_y_UV =  ec_geom_peaks.GetHallY_UV();
                         hall_x_UW = -ec_geom_peaks.GetHallX_UW();
                         hall_y_UW =  ec_geom_peaks.GetHallY_UW();
                         hall_x_VW = -ec_geom_peaks.GetHallX_VW();
                         hall_y_VW =  ec_geom_peaks.GetHallY_VW();
                         Dalitz_peaks = ec_geom_peaks.GetDalitz();
                	     }
                	     
                     dg.getH2F("h_yxc_UV1").fill(hall_x_UV, hall_y_UV);
                     dg.getH2F("h_yxc_UW1").fill(hall_x_UW, hall_y_UW);
                     dg.getH2F("h_yxc_VW1").fill(hall_x_VW, hall_y_VW);
                     dg.getH2F("h_Dalitz_Peaks1").fill(Dalitz_peaks, is);                	                    	     
                 }
             }                                   
         }
         }
         }
    	 
     }
     
     public void updateCanvas(DetectorDescriptor dd) {                
         updatePlots();                 
     } 
     
     public void updatePlots() {
         switch (trigger.selectedCanvas) {
         case  "TriggerBits": updateTrigger(); break;
         case     "EC Peaks": updateECPeaks(); break;
         case     "PC Peaks": updatePCPeaks(); break;
         case  "EC Clusters": updateECClusters(); break; 
         case  "PC Clusters": updatePCClusters(); break;
         case         "XvsY": updateXvsY(); break;
         case   "Thresholds": updateThresholds(); break;
         case     "Elastic" : updateElastic();
         }   	         	     
     }
     
     public void updateTrigger() {
    	 
	     DataGroup dg1 = this.getDataGroup().getItem(0,0,0);
	     DataGroup dg2 = this.getDataGroup().getItem(0,0,1);
    	 c = trigger.getCanvas("TriggerBits");    	 
         c.clear();
         
         c.divide(1, 2);
         c.setGridX(false); c.setGridY(false);
         c.cd(0); c.getPad(0).getAxisY().setLog(true); c.draw(dg1.getH1F(tbit));
         c.cd(1); c.getPad(1).getAxisY().setLog(true); c.draw(dg2.getH1F(tbit));
         c.update();
         
     }
     
     public void updateECPeaks() {
    	 
	     DataGroup dg = this.getDataGroup().getItem(1,0,0);
	     
	     String[] uvw = {"UV","UW","VW"};
	     
	     c = trigger.getCanvas("EC Peaks");  
	     c.divide(3, 3);
	     
	     for (int i=0; i<3; i++) {
	    	 canvasConfig(c,i+0,0,0,0,0,true).draw(dg.getH2F("h_N_peaks1_"+i));
	    	 canvasConfig(c,i+3,0,0,0,0,true).draw(dg.getH2F("h_t_peak1_"+i));
	    	 canvasConfig(c,i+6,0,0,0,0,true).draw(dg.getH2F("h_coord_peak1_"+i));
	     }
	     
  	     c.update();
     }
     
     public void updatePCPeaks() {
    	 
	     DataGroup dg = this.getDataGroup().getItem(2,0,0);
	     
	     String[] uvw = {"UV","UW","VW"};
	     
	     c = trigger.getCanvas("PC Peaks");         
	     c.divide(3, 4);
	     
	     for (int i=0; i<3; i++) {
	    	 canvasConfig(c,i+0,0,0,0,0,true).draw(dg.getH2F("h_N_peaks1_"+i));
	    	 canvasConfig(c,i+3,0,0,0,0,true).draw(dg.getH2F("h_t_peak1_"+i));
	    	 canvasConfig(c,i+6,0,0,0,0,true).draw(dg.getH2F("h_N_peaks2_"+i));
	    	 canvasConfig(c,i+9,0,0,0,0,true).draw(dg.getH2F("h_coord_peak1_"+i));
	     }
	     
  	     c.update();  	 
     }
     
     public void updateECClusters() {
    	 
	     DataGroup dg = this.getDataGroup().getItem(1,0,0);
	     
	     c = trigger.getCanvas("EC Clusters");       
	     c.divide(3, 3);
	     
	     int n=0;
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_Dalitz_Clust1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_N_Clust1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_N_Clust2"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_t1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_E1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_E2"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_U1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_V1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_W1"));
             
	     c.update();
     }
     
     public void updatePCClusters() {
    	 
	     DataGroup dg = this.getDataGroup().getItem(2,0,0);
	     
	     c = trigger.getCanvas("PC Clusters");   
	     c.divide(3, 3);
	     
	     int n=0;
         canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_Dalitz_Clust1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_N_Clust1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_N_Clust2"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_t1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_E1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_E2"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_U1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_V1"));
	     canvasConfig(c,n++,0,0,0,0,true).draw(dg.getH2F("h_cl_W1"));
	     
	     c.update();
     }
     
     public void updateXvsY() {
    	 
	     DataGroup dg1 = this.getDataGroup().getItem(1,0,0);
	     DataGroup dg2 = this.getDataGroup().getItem(2,0,0);
	     
	     String[] uvw = {"UV","UW","VW"};
	     
	     c = trigger.getCanvas("XvsY");   
	     c.divide(3, 3);
	     
	     for (int i=0; i<3; i++) {
	    	 canvasConfig(c,i+0,0,0,0,0,true).draw(dg2.getH2F("h_yxc_"+uvw[i]+"1"));
	    	 canvasConfig(c,i+3,0,0,0,0,true).draw(dg1.getH2F("h_yxc_"+uvw[i]+"1"));	    	 
	     }
	     
	     canvasConfig(c,6,0,0,0,0,true).draw(dg2.getH2F("h_yxc1"));   
	     canvasConfig(c,7,0,0,0,0,true).draw(dg1.getH2F("h_yxc1"));
	     
	     c.update();    	 
     }
     
     public void updateThresholds() {
    	 
	     DataGroup dg1 = this.getDataGroup().getItem(1,0,0);
	     DataGroup dg2 = this.getDataGroup().getItem(2,0,0);
    	 
	     c = trigger.getCanvas("Thresholds");   
	     c.divide(3, 3);
	     
	     for (int i=0; i<3; i++) {
	    	 canvasConfig(c,i+0,0,0,0,0,true).draw(dg2.getH2F("h_energy_peak2_"+i));
	    	 canvasConfig(c,i+3,0,0,0,0,true).draw(dg1.getH2F("h_energy_peak2_"+i));	     
	     }
	     
	     canvasConfig(c,6,0,0,0,0,true).draw(dg2.getH2F("h_cl_E3"));   
	     canvasConfig(c,7,0,0,0,0,true).draw(dg1.getH2F("h_cl_E3"));
//	     canvasConfig(c,8,0,0,0,0,true).draw(dg1.getH2F("h_cl_E4"));
	     
	     c.update();
     }
     
     public void updateElastic() {
	     DataGroup dg2 = this.getDataGroup().getItem(2,0,0);
	     c = trigger.getCanvas("Elastic");   
	     c.divide(3, 2);
	     for (int i=0; i<5; i++) {
	         canvasConfig(c,i+0,0,0,0,0,true).draw(dg2.getH2F("h_th_phi_cl_"+i));
	     }
         canvasConfig(c,5,0,0,0,0,true).draw(dg2.getH2F("h_th_phi_cl1"));
	     
	     c.update();
     }     

}
