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
import org.jlab.coda.jevio.ByteDataTransformer;
import org.jlab.coda.jevio.EvioNode;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.io.evio.EvioTreeBranch;
import org.jlab.utils.groups.IndexedList;

public class ECTriggerApp extends FCApplication{
	
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed   trigger = new EmbeddedCanvasTabbed("TriggerBits");   
    EmbeddedCanvas               c = this.getCanvas(this.getName());
    
    FCTrigger                 trig = null;
    
    String tbit = "Trigger Bits: ECAL.PCAL.HTCC(0)    ECAL.PCAL.HTCC(1-6)    HTCC(7-12)    PCAL(13-18)    ECAL(19-24)   HT.PC(25)   HT.EC(26) "
                  + "  PC.EC(27)   FTOF.PC(28)    1K Pulser(31)";
    double Dalitz_ECout_max = 0.0833333;
    double Dalitz_ECout_min = -0.0555556;

    double r2d = 57.2957795130823229;
    
    double U_conv = 2.75; // This converts U coordinate from the Trigger bank into coordinate real U coordinate
    double V_conv = 3.00; // This converts V coordinate from the Trigger bank into coordinate real V coordinate
    double W_conv = 3.00; // This converts W coordinate from the Trigger bank into coordinate real W coordinate

    int n_sect = 6;
    int n_view = 3;
    int vtp_tag = 57634;
    int adcECvtp_tagmin = 100;
    int adcECvtp_tagmax = 112;

    double ADC2GeV = 1. / 10000.;

    double E_bin_width = 0.3;
    int n_E_bins = 5;

    int ftrig_crate_ID = 37;
    int trig_bank_tag = 57610;

    public ECTriggerApp(String name, ECPixels[] ecPix) {
        super(name, ecPix);
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
    }
    
     public JPanel getPanel() {        
         engineView.setLayout(new BorderLayout());
         trigger.addCanvas("VTP ECAL");
         trigger.addCanvas("VTP PCAL");
         engineView.add(trigger);
         return engineView;       
	 }  
     
     public void createHistos( ) {
    	 
        DataGroup dgTrig = new DataGroup(4,3);
 	    
        H1F trig = new H1F(tbit, tbit, 32,-0.5,31.5);
        trig.setFillColor(4);      
        trig.setTitleX("Trigger Bits");
        trig.setTitleY("Counts");
        
        dgTrig.addDataSet(trig, 1);
//    	    dgTrig.addDataSet(new H1F("h_tr_bit_distr", "", 33, -0.5, 32.5),1); 
    	    
        DataGroup dgECClust = new DataGroup(4,3);

        dgECClust.addDataSet(new H2F("h_EC_Dalitz_Clust1", 40, -0.5, 0.5, 7, -0.5, 6.5),1);
     	
        dgECClust.addDataSet(new H2F("h_N_ECClust1", 11, -0.5, 10.5, 7, -0.5, 6.5),1);     	
    	    dgECClust.addDataSet(new H2F("h_N_ECClust2", 11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	    dgECClust.addDataSet(new H2F("h_ECcl_t1",    21, -0.5, 20.5, 7, -0.5, 6.5),1);    	    
    	    dgECClust.addDataSet(new H2F("h_ECcl_E1",   200,  0.,   3,   7, -0.5, 6.5),1);      
    	    dgECClust.addDataSet(new H2F("h_ECcl_E2",   200,  0.,   3,   7, -0.5, 6.5),1);      
    	    dgECClust.addDataSet(new H2F("h_ECcl_U1",    41, -0.5, 40.5, 7, -0.5, 6.5),1);   
    	    dgECClust.addDataSet(new H2F("h_ECcl_V1",    41, -0.5, 40.5, 7, -0.5, 6.5),1);   
    	    dgECClust.addDataSet(new H2F("h_ECcl_W1",    41, -0.5, 40.5, 7, -0.5, 6.5),1);   
       	    
    	    DataGroup dgECClustXY = new DataGroup(4,3);
    	    
    	    dgECClustXY.addDataSet(new H2F("h_EC_yxc1",    200, -500., 500., 200, -500., 500.),1);
    	    dgECClustXY.addDataSet(new H2F("h_EC_yxc_UV1", 200, -500., 500., 200, -500., 500.),1);
    	    dgECClustXY.addDataSet(new H2F("h_EC_yxc_UW1", 200, -500., 500., 200, -500., 500.),1);
    	    dgECClustXY.addDataSet(new H2F("h_EC_yxc_VW1", 200, -500., 500., 200, -500., 500.),1);
    	    
    	    dgECClustXY.addDataSet(new H2F("h_EC_th_phi_cl1", 360,    0., 360., 200,    0.,  50.),1);
    	    
    	    for (int i = 0; i < n_E_bins; i++) {
    	        dgECClustXY.addDataSet(new H2F("h_EC_th_phi_cl_"+i, 360, 0., 360., 200, 0., 50.),1);
    	    }  
    	    
    	    DataGroup dgECPeak = new DataGroup(4,3);
    	    
    	    dgECPeak.addDataSet(new H2F("h_EC_Dalitz_Peaks1",  40, -0.5, 0.5, 7, -0.5, 6.5),1);

    	    for (int i_view = 0; i_view < n_view; i_view++) {
    	        dgECPeak.addDataSet(new H2F("h_N_ECpeaks1_"+i_view,    11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	        dgECPeak.addDataSet(new H2F("h_N_ECpeaks2_"+i_view,    11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	        dgECPeak.addDataSet(new H2F("h_N_ECpeaks3_"+i_view,    11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	        dgECPeak.addDataSet(new H2F("h_t_ECpeak1_"+i_view,     21, -0.5, 20.5, 7, -0.5, 6.5),1);
    	        dgECPeak.addDataSet(new H2F("h_coord_ECpeak1_"+i_view, 41, -0.5, 40.5, 7, -0.5, 6.5),1);
    	    }
    	        	    
    	    DataGroup dgPCClust = new DataGroup(4,3);
    	    
    	    dgPCClust.addDataSet(new H2F("h_PC_Dalitz_Clust1", 40, -0.5, 0.5, 7, -0.5, 6.5),1);   
    	    
    	    dgPCClust.addDataSet(new H2F("h_N_PCClust1", 11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_N_PCClust2", 11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_N_PCClust3", 11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_t1",    21, -0.5, 20.5, 7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_E1",   200,  0.,   4.,  7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_E2",   200,  0.,   4.,  7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_E3",   200,  0.,   4.,  7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_E4",   200,  0.,   4.,  7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_U1",    86, -0.5, 85.5, 7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_V1",    86, -0.5, 85.5, 7, -0.5, 6.5),1);
    	    dgPCClust.addDataSet(new H2F("h_PCcl_W1",    86, -0.5, 85.5, 7, -0.5, 6.5),1);
    	    
        dgPCClust.addDataSet(new H1F("h_PC_dU_43", "", 600, -90., 90.),1);
    	    dgPCClust.addDataSet(new H1F("h_PC_dV_43", "", 600, -80., 80.),1);
    	    dgPCClust.addDataSet(new H1F("h_PC_dW_43", "", 600, -80., 80.),1);
    	        	    
    	    DataGroup dgPCClustXY = new DataGroup(4,3);
    	    
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc1",    200, -500., 500., 200, -500., 500.),1);
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc2",    200, -500., 500., 200, -500., 500.),1);
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc3",    200, -500., 500., 200, -500., 500.),1);
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc4",    200, -500., 500., 200, -500., 500.),1);
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc_UV1", 200, -500., 500., 200, -500., 500.),1);
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc_UW1", 200, -500., 500., 200, -500., 500.),1);
    	    dgPCClustXY.addDataSet(new H2F("h_PC_yxc_VW1", 200, -500., 500., 200, -500., 500.),1);

    	    dgPCClustXY.addDataSet(new H2F("h_PC_th_phi_cl1", 360, 0., 360., 200, 0., 50.),1);

    	    for (int i = 0; i < n_E_bins; i++) {
    	       dgPCClustXY.addDataSet(new H2F("h_PC_th_phi_cl_"+i, 360, 0., 360., 200, 0., 50.),1);
    	    } 
    	    
    	    DataGroup dgPCPeak = new DataGroup(4,3);
    	    
    	    dgPCPeak.addDataSet(new H2F("h_PC_Dalitz_Peaks1",  40, -0.5, 0.5, 7, -0.5, 6.5),1);
    	    
    	    for (int i_view = 0; i_view < n_view; i_view++) {
    	        dgPCPeak.addDataSet(new H2F("h_N_PCpeaks1_"+i_view,    11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	        dgPCPeak.addDataSet(new H2F("h_N_PCpeaks2_"+i_view,    11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	        dgPCPeak.addDataSet(new H2F("h_N_PCpeaks3_"+i_view,    11, -0.5, 10.5, 7, -0.5, 6.5),1);
    	        dgPCPeak.addDataSet(new H2F("h_t_PCpeak1_"+i_view,     21, -0.5, 20.5, 7, -0.5, 6.5),1);
    	        dgPCPeak.addDataSet(new H2F("h_coord_PCpeak1_"+i_view, 86, -0.5, 85.5, 7, -0.5, 6.5),1);
    	    }

        this.getDataGroup().clear();
        this.getDataGroup().add(dgTrig,     1,0,0);        
        this.getDataGroup().add(dgECPeak,   2,0,0);        
        this.getDataGroup().add(dgECClust,  3,0,0);        
        this.getDataGroup().add(dgECClustXY,4,0,0);        
        this.getDataGroup().add(dgPCPeak,   5,0,0);        
        this.getDataGroup().add(dgPCClust,  6,0,0);        
        this.getDataGroup().add(dgPCClustXY,7,0,0);        

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
         this.getDataGroup().clear();
         createHistos();
     }
     
     public void addEvent(DataEvent event) {
    	 
         if(event instanceof EvioDataEvent) getDataEntries_VTP((EvioDataEvent) event);             
         if (!testTriggerMask()) return;
    	     fillTriggerBitHistos();
     }
         
     public void getDataEntries_VTP(EvioDataEvent event){
             
//         System.out.println("Event Number "+app.evtno);
             
         List<EvioTreeBranch> branches = app.codadecoder.getEventBranches(event);
             
         for(EvioTreeBranch branch : branches){
             int  crate = branch.getTag();
             for(EvioNode node : branch.getNodes()){
                 if(node.getTag()==vtp_tag){
//                     System.out.println("Crate "+crate);
                     int[] intData = ByteDataTransformer.toIntArray(node.getStructureBuffer(true));
                     List<Integer> list  = Arrays.stream( intData ).boxed().collect( Collectors.toList() );    
                     Iterator<Integer> it = list.iterator();
                     trig = new FCTrigger(); trig.resetAll(); trig.getTriggerWords(it,crate);
                     fillVTPHistos();
                 }
             }
         }            
     }  
         
     public void fillTriggerBitHistos() {
         if (app.isSingleEvent()) this.getDataGroup().getItem(1,0,0).getH1F(tbit).reset();
         for (int i=0; i<32; i++) if(isTrigBitSet(i)) this.getDataGroup().getItem(1,0,0).getH1F(tbit).fill(i);    	 
     }
     
     public void fillVTPHistos()  {
    	 
    	     int detector  = trig.GetDetector();
    	     int sector    = trig.GetSector();
    	     int ev_number = trig.GetEvNumber();
    	     
    	     DataGroup dg2 = this.getDataGroup().getItem(2,0,0);
    	     DataGroup dg4 = this.getDataGroup().getItem(4,0,0);
    	     
    	     if (detector == 1) { //EC
    	    	 
    	         IndexedList<List<TEC_Peak>> v_Peaks_ = new IndexedList<List<TEC_Peak>>(1);
    	         
              // ====== EC Peaks ======
    	         
             for (int i_view = 0; i_view < n_view; i_view++) {

                 int n_ECpeaks = trig.GetNPeaks(0, i_view);
                 dg2.getH2F("h_N_ECpeaks1_"+i_view).fill(n_ECpeaks, sector);

                 for (int i_peak = 0; i_peak < n_ECpeaks; i_peak++) {

                     dg2.getH2F("h_t_ECpeak1_"+i_view).fill(trig.GetECPeak(0, i_view, i_peak).time, sector);

                     double coord = trig.GetECPeak(0, i_view, i_peak).coord / 8.;
                     dg2.getH2F("h_coord_ECpeak1_"+i_view).fill(coord, sector);

                     if(!v_Peaks_.hasItem(i_view)) {
                         v_Peaks_.add(new ArrayList<TEC_Peak>(),i_view);}
                         v_Peaks_.getItem(i_view).add(trig.GetECPeak(0, i_view, i_peak));
                 }
             }
    	     
             
             for (int iU = 0; iU < (v_Peaks_.hasItem(0)?v_Peaks_.getItem(0).size():0); iU++) {
                 for (int iV = 0; iV < (v_Peaks_.hasItem(1)?v_Peaks_.getItem(1).size():0); iV++) {
                     for (int iW = 0; iW < (v_Peaks_.hasItem(2)?v_Peaks_.getItem(2).size():0); iW++) {

                         TECGeom ec_geom_peaks = new TECGeom(v_Peaks_.getItem(0).get(iU).coord / 8., 
                                                             v_Peaks_.getItem(1).get(iV).coord / 8., 
                                                             v_Peaks_.getItem(2).get(iW).coord / 8.);
                         ec_geom_peaks.SetSector(sector);

                         double hall_x_UV = ec_geom_peaks.GetHallX_UV();
                         double hall_y_UV = ec_geom_peaks.GetHallY_UV();
                         dg4.getH2F("h_EC_yxc_UV1").fill(hall_x_UV, hall_y_UV);

                         double hall_x_UW = ec_geom_peaks.GetHallX_UW();
                         double hall_y_UW = ec_geom_peaks.GetHallY_UW();
                         dg4.getH2F("h_EC_yxc_UW1").fill(hall_x_UW, hall_y_UW);

                         double hall_x_VW = ec_geom_peaks.GetHallX_VW();
                         double hall_y_VW = ec_geom_peaks.GetHallY_VW();
                         dg4.getH2F("h_EC_yxc_VW1").fill(hall_x_VW, hall_y_VW);

                         double Dalitz_peaks = ec_geom_peaks.GetDalitz();

                         dg2.getH2F("h_EC_Dalitz_Peaks1").fill(Dalitz_peaks, sector);
                     }
                 }                                   
             } 
             // ====== EC Clusters =======
             
    	         DataGroup dg3 = this.getDataGroup().getItem(3,0,0);

             int n_cl = trig.GetNClust(0); // Argument is the EC instance, but now it should be 0 all the time

             dg3.getH2F("h_N_ECClust1").fill(n_cl, sector);
 
             int n_cl_in_1_tbin = 0;

             for (int i_cl = 0; i_cl < n_cl; i_cl++) {
                 int cl_time = trig.GetECCluster(0, i_cl).time;
                 double cl_U = trig.GetECCluster(0, i_cl).Ustrip / 8.;
                 double cl_V = trig.GetECCluster(0, i_cl).Vstrip / 8.;
                 double cl_W = trig.GetECCluster(0, i_cl).Wstrip / 8.;
                 double cl_E = trig.GetECCluster(0, i_cl).energy * ADC2GeV;

                 TECGeom ec_geom = new TECGeom(cl_U, cl_V, cl_W);

                 ec_geom.SetSector(sector);

                 double hall_x_cl = ec_geom.GetHallX_UV();
                 double hall_y_cl = ec_geom.GetHallY_UV();
                 double hall_z_cl = ec_geom.GetHallZ_UV();

                 int E_bin = (int) Math.min((int)(cl_E / E_bin_width), n_E_bins - 1); // for not being out of range

                 double phi_cl = Math.atan2(hall_y_cl, hall_x_cl) * r2d + 30.;
                 
                 if (phi_cl < 0.) phi_cl = phi_cl + 360.;
                 
                 double th_cl = Math.atan(Math.sqrt(hall_x_cl * hall_x_cl + hall_y_cl * hall_y_cl) / hall_z_cl) * r2d;

                 dg4.getH2F("h_EC_th_phi_cl1").fill(phi_cl, th_cl);
                 dg4.getH2F("h_EC_th_phi_cl_"+E_bin).fill(phi_cl, th_cl);
                 dg4.getH2F("h_EC_yxc1").fill(hall_x_cl, hall_y_cl);
                 
                 //cout<<"Ev. number is "<<ev_number<<"    n_cl is "<<n_cl<<"    sector is "<<sector<<"    cl energy is "<<trig.GetECCluster(0, i_cl)->energy<<endl;

                 double cl_Dalitz = ec_geom.GetDalitz();

                 dg3.getH2F("h_EC_Dalitz_Clust1").fill(cl_Dalitz, sector);

                 dg3.getH2F("h_ECcl_t1").fill(cl_time, sector);
                 dg3.getH2F("h_ECcl_U1").fill(cl_U, sector);
                 dg3.getH2F("h_ECcl_V1").fill(cl_V, sector);
                 dg3.getH2F("h_ECcl_W1").fill(cl_W, sector);
                 dg3.getH2F("h_ECcl_E1").fill(cl_E, sector);

                 if(cl_time == 7) {
                     n_cl_in_1_tbin = n_cl_in_1_tbin + 1;
                     dg3.getH2F("h_ECcl_E2").fill(cl_E, sector);
                 }
             }
 
             dg3.getH2F("h_N_ECClust2").fill(n_cl_in_1_tbin, sector);
                      
    	     } else if (detector == 2) { //PCAL
             
    	         IndexedList<List<TEC_Peak>> v_Peaks_ = new IndexedList<List<TEC_Peak>>(1);

    	         // ====== PCAL Peaks ======
    	         
        	     DataGroup dg5 = this.getDataGroup().getItem(5,0,0);
        	     DataGroup dg7 = this.getDataGroup().getItem(7,0,0);
    	    	     
             int n_PC_peaks_in1_timebin_[] = {0, 0, 0};
             
             for (int i_view = 0; i_view < n_view; i_view++) {

                 int n_PCpeaks = trig.GetNPeaks(0, i_view);
                 dg5.getH2F("h_N_PCpeaks1_"+i_view).fill(n_PCpeaks, sector);
		
                 for (int i_peak = 0; i_peak < n_PCpeaks; i_peak++) {

                     dg5.getH2F("h_t_PCpeak1_"+i_view).fill(trig.GetECPeak(0, i_view, i_peak).time, sector);
		    
		             if( trig.GetECPeak(0, i_view, i_peak).time == 7 ){
		                n_PC_peaks_in1_timebin_[i_view] = n_PC_peaks_in1_timebin_[i_view] + 1;
		             }
		            
		             double coord_conv = (i_view == 0) ? 2.75:3.00;
                     double coord = trig.GetECPeak(0, i_view, i_peak).coord / coord_conv;                    
                     dg5.getH2F("h_coord_PCpeak1_"+i_view).fill(coord, sector);

                     if(!v_Peaks_.hasItem(i_view)) {
                         v_Peaks_.add(new ArrayList<TEC_Peak>(),i_view);}
                         v_Peaks_.getItem(i_view).add(trig.GetECPeak(0, i_view, i_peak));        
                 }
		
		         dg5.getH2F("h_N_PCpeaks2_"+i_view).fill(n_PC_peaks_in1_timebin_[i_view], sector);
	    
//		         if( tr_bit.El_Sec(sector) ) {
//		             dg5.getH2F("h_N_PCpeaks3_"+i_view).fill(n_PC_peaks_in1_timebin_[i_view], sector);
//		         }		    
            }
    	    	   
            for (int iU = 0; iU < (v_Peaks_.hasItem(0)?v_Peaks_.getItem(0).size():0); iU++) {
                for (int iV = 0; iV < (v_Peaks_.hasItem(1)?v_Peaks_.getItem(1).size():0); iV++) {
                    for (int iW = 0; iW < (v_Peaks_.hasItem(2)?v_Peaks_.getItem(2).size():0); iW++) {

                        TPCGeom pcal_geom_peaks = new TPCGeom(v_Peaks_.getItem(0).get(iU).coord / U_conv, 
                                                              v_Peaks_.getItem(1).get(iV).coord / V_conv, 
                                                              v_Peaks_.getItem(2).get(iW).coord / W_conv);

                        pcal_geom_peaks.SetSector(sector);

                        double hall_x_UV = pcal_geom_peaks.GetHallX_UV();
                        double hall_y_UV = pcal_geom_peaks.GetHallY_UV();
                        dg7.getH2F("h_PC_yxc_UV1").fill(hall_x_UV, hall_y_UV);
                        double hall_x_UW = pcal_geom_peaks.GetHallX_UW();
                        double hall_y_UW = pcal_geom_peaks.GetHallY_UW();
                        dg7.getH2F("h_PC_yxc_UW1").fill(hall_x_UW, hall_y_UW);
                        double hall_x_VW = pcal_geom_peaks.GetHallX_VW();
                        double hall_y_VW = pcal_geom_peaks.GetHallY_VW();
                        dg7.getH2F("h_PC_yxc_VW1").fill(hall_x_VW, hall_y_VW);

                        double Dalitz_peaks = pcal_geom_peaks.GetDalitz();
                        dg5.getH2F("h_PC_Dalitz_Peaks1").fill(Dalitz_peaks, sector);
                    }

                }

            }
            
            // ====== PCAL Clusters =======

   	        DataGroup dg6 = this.getDataGroup().getItem(6,0,0);
   	        
            double u_coord_conv = 2.75;
            double v_coord_conv = 3.;
            double w_coord_conv = 3.;
   	        
            int n_cl = trig.GetNClust(0); // Argument is the EC instance, but now it should be 0 all the time

            dg6.getH2F("h_N_PCClust1").fill(n_cl, sector);

            int n_cl_in_1_tbin = 0;

            List<Double> v_cl_E_1_tbin = new ArrayList<Double>();
            List<Double> v_cl_y_1_tbin = new ArrayList<Double>();  
            List<Double> v_cl_x_1_tbin = new ArrayList<Double>();
            List<Double> v_cl_U_1_tbin = new ArrayList<Double>();
            List<Double> v_cl_V_1_tbin = new ArrayList<Double>(); 
            List<Double> v_cl_W_1_tbin = new ArrayList<Double>();			    

            for (int i_cl = 0; i_cl < n_cl; i_cl++) {

                int cl_time = trig.GetECCluster(0, i_cl).time;
                double cl_U = trig.GetECCluster(0, i_cl).Ustrip / u_coord_conv;
                double cl_V = trig.GetECCluster(0, i_cl).Vstrip / v_coord_conv;
                double cl_W = trig.GetECCluster(0, i_cl).Wstrip / w_coord_conv;
                double cl_E = trig.GetECCluster(0, i_cl).energy * ADC2GeV;

                TPCGeom pc_geom = new TPCGeom(cl_U, cl_V, cl_W);
                
                pc_geom.SetSector(sector);

                double hall_x_cl = pc_geom.GetHallX_VW();
                double hall_y_cl = pc_geom.GetHallY_VW();
                double hall_z_cl = pc_geom.GetHallZ_VW();

                int E_bin = (int) Math.min(cl_E / E_bin_width, n_E_bins - 1); // for not being out of range

                double phi_cl = Math.atan2(hall_y_cl, hall_x_cl) * r2d + 30.;
                
                if (phi_cl < 0.)  phi_cl = phi_cl + 360.;
              
                double th_cl = Math.atan(Math.sqrt(hall_x_cl * hall_x_cl + hall_y_cl * hall_y_cl) / hall_z_cl) * r2d;

                dg7.getH2F("h_PC_th_phi_cl1").fill(phi_cl, th_cl);
                dg7.getH2F("h_PC_th_phi_cl_"+E_bin).fill(phi_cl, th_cl);
                dg7.getH2F("h_PC_yxc1").fill(hall_x_cl, hall_y_cl);

                double cl_Dalitz = pc_geom.GetDalitz();

                dg6.getH2F("h_PC_Dalitz_Clust1").fill(cl_Dalitz, sector);

                //                            cout<<" \n\n\n\n ========== cluster info ============"<<endl;
                //                            cout << "U: " << trig.GetECCluster(0, i_cl)->Ustrip<< "     " << cl_U << endl;
                //                            cout << "V: " << trig.GetECCluster(0, i_cl)->Vstrip<< "     " << cl_V << endl;
                //                            cout << "W: " << trig.GetECCluster(0, i_cl)->Wstrip<< "     " << cl_W << endl;
                //                            cout<<"Dalitz = "<<Dalitz<<endl;

                //cout<<"Ev. number is "<<ev_number<<"    n_cl is "<<n_cl<<"    sector is "<<sector<<"    cl energy is "<<trig.GetECCluster(0, i_cl)->energy<<endl;

                dg6.getH2F("h_PCcl_t1").fill(cl_time, sector);
                dg6.getH2F("h_PCcl_U1").fill(cl_U, sector);
                dg6.getH2F("h_PCcl_V1").fill(cl_V, sector);
                dg6.getH2F("h_PCcl_W1").fill(cl_W, sector);
                dg6.getH2F("h_PCcl_E1").fill(cl_E, sector);

                if(cl_time == 7) {
                    n_cl_in_1_tbin = n_cl_in_1_tbin + 1;
                	    dg6.getH2F("h_PCcl_E2").fill(cl_E, sector);
                	    dg7.getH2F("h_PC_yxc2").fill(hall_x_cl, hall_y_cl);

                  	v_cl_E_1_tbin.add(cl_E);
                	    v_cl_y_1_tbin.add(hall_y_cl);
                	    v_cl_x_1_tbin.add(hall_x_cl);

                	    v_cl_U_1_tbin.add(cl_U);
                	    v_cl_V_1_tbin.add(cl_V);
                	    v_cl_W_1_tbin.add(cl_W);
                }

            }

            dg6.getH2F("h_N_PCClust2").fill(n_cl_in_1_tbin, sector);

            if(v_cl_E_1_tbin.size()>0) {
              	dg6.getH2F("h_PCcl_E3").fill(v_cl_E_1_tbin.get(0), sector);
            	    dg7.getH2F("h_PC_yxc3").fill(v_cl_x_1_tbin.get(0), v_cl_y_1_tbin.get(0));


            	    if( v_cl_E_1_tbin.size() == 4 ) {
            		    dg6.getH2F("h_PCcl_E4").fill(v_cl_E_1_tbin.get(3), sector);
            		    dg7.getH2F("h_PC_yxc4").fill(v_cl_x_1_tbin.get(3), v_cl_y_1_tbin.get(3));

            		    double dU_43 = v_cl_U_1_tbin.get(3) - v_cl_U_1_tbin.get(2);
            		    double dV_43 = v_cl_V_1_tbin.get(3) - v_cl_V_1_tbin.get(2);
            		    double dW_43 = v_cl_W_1_tbin.get(3) - v_cl_W_1_tbin.get(2);

            		    dg6.getH1F("h_PC_dU_43").fill(dU_43);
            	      	dg6.getH1F("h_PC_dV_43").fill(dV_43);
            		    dg6.getH1F("h_PC_dW_43").fill(dW_43);

            		// A temporary test, to check, if clusters ordered by energy

            		    if( v_cl_E_1_tbin.get(0) <= v_cl_E_1_tbin.get(1) ||
            		        v_cl_E_1_tbin.get(1) <= v_cl_E_1_tbin.get(2) || 
            		        v_cl_E_1_tbin.get(2) <= v_cl_E_1_tbin.get(3) ){
            			    System.out.println(v_cl_E_1_tbin.get(0)+"  "+
            		                           v_cl_E_1_tbin.get(1)+"  "+
            		                           v_cl_E_1_tbin.get(2)+"  "+
            		                           v_cl_E_1_tbin.get(3));
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
         case "TriggerBits": updateTrigger(); break;
         case    "VTP ECAL": updateVTPEC(); break;
         case    "VTP PCAL": updateVTPPC();
         }   	     
    	     
     }
     
     public void updateTrigger() {
    	 
	     DataGroup dg1 = this.getDataGroup().getItem(1,0,0);
	     
    	     c = trigger.getCanvas("TriggerBits");    	 
         c.clear();
         
         c.divide(1, 1);
         c.setGridX(false); c.setGridY(false);
         c.cd(0);
         c.getPad(0).getAxisY().setLog(true);
         c.draw(dg1.getH1F(tbit));
         c.update();
         
     }
     
     public void updateVTPEC() {
    	 
	     DataGroup dg2 = this.getDataGroup().getItem(2,0,0);
	     DataGroup dg4 = this.getDataGroup().getItem(4,0,0);
	     String[] uvw = {"UV","UW","VW"};
	     
	     c = trigger.getCanvas("VTP ECAL");	     
	     c.divide(3, 4);
	     
	     for (int i=0; i<3; i++) {
             c.cd(i);   c.draw(dg2.getH2F("h_N_ECpeaks1_"+i));
             c.cd(i+3); c.draw(dg2.getH2F("h_t_ECpeak1_"+i));
             c.cd(i+6); c.draw(dg2.getH2F("h_coord_ECpeak1_"+i));
             c.cd(i+9); c.draw(dg4.getH2F("h_EC_yxc_"+uvw[i]+"1"));
	     }
	     
  	     c.update();
     }
     
     public void updateVTPPC() {
    	 
	     DataGroup dg5 = this.getDataGroup().getItem(5,0,0);
	     DataGroup dg7 = this.getDataGroup().getItem(7,0,0);
	     String[] uvw = {"UV","UW","VW"};
	     
	     c = trigger.getCanvas("VTP PCAL");	     
	     c.divide(3, 4);
	     
	     for (int i=0; i<3; i++) {
             c.cd(i);   c.draw(dg5.getH2F("h_N_PCpeaks1_"+i));
             c.cd(i+3); c.draw(dg5.getH2F("h_t_PCpeak1_"+i));
             c.cd(i+6); c.draw(dg5.getH2F("h_coord_PCpeak1_"+i));
             c.cd(i+9); c.draw(dg7.getH2F("h_PC_yxc_"+uvw[i]+"1"));
	     }
	     
  	     c.update();
   	 
     }
}
