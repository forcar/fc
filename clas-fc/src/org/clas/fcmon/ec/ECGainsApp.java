package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;

import org.clas.fcmon.detector.view.EmbeddedCanvasTabbed;
import org.clas.fcmon.tools.FCApplication;
import org.jlab.clas.physics.Particle;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.service.ec.ECEngine;
import org.jlab.utils.groups.IndexedList;

public class ECGainsApp extends FCApplication implements ActionListener {
    
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    strips = new EmbeddedCanvasTabbed("Strips");
    EmbeddedCanvasTabbed     peaks = new EmbeddedCanvasTabbed("Peaks");
    EmbeddedCanvasTabbed  clusters = new EmbeddedCanvasTabbed("Clusters");
    EmbeddedCanvasTabbed   summary = new EmbeddedCanvasTabbed("PCAL/ECTOT");
    EmbeddedCanvas               c = this.getCanvas(this.getName()); 
    ButtonGroup                bG1 = new ButtonGroup();
    ButtonGroup                bG2 = new ButtonGroup();
    ButtonGroup                bG3 = new ButtonGroup();    
    public int        activeSector = 2;
    public int      activeDetector = 0;                
    public int         activeLayer = 0;    
    int is,la,ic,idet,nstr;
    
    float[][][][] ecmean = new float[6][3][3][68];
    float[][][][]  ecrms = new float[6][3][3][68];
    String[]         det = new String[]{"pcal","ecin","ecou"};
    String[]         lay = new String[]{"u","v","w"};
    double[]        mipc = {30,30,48};  
    double[]        mipp = {10,10,16};  
    double[]     fitLimp = { 5, 3, 6,17,17,27};
    double[]     fitLimc = {20,17,35,40,48,75};
    int[]           npmt = {68,62,62,36,36,36,36,36,36};
    
    IndexedList<GraphErrors> MIPSummary  = new IndexedList<GraphErrors>(4);
    IndexedList<FitData>         MipFits = new IndexedList<FitData>(4);
    
    public ECGainsApp(String name, ECEngine engine) {
        super(name,engine);    
     }  
    
    public void init() {
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
        engineView.add(getCanvasPane(),BorderLayout.CENTER);
        engineView.add(getButtonPane(),BorderLayout.PAGE_END);
        clusters.addCanvas("Fits");
        clusters.addCanvas("Summary");
        peaks.addCanvas("Fits");
        peaks.addCanvas("Summary");
        return engineView;       
    }  
    
    public JSplitPane getCanvasPane() {
        
        JSplitPane    hPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        JSplitPane   vPaneL = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
        JSplitPane   vPaneR = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
        hPane.setLeftComponent(vPaneL);
        hPane.setRightComponent(vPaneR);
        vPaneL.setTopComponent(peaks);
        vPaneL.setBottomComponent(strips);
        vPaneR.setTopComponent(clusters);
        vPaneR.setBottomComponent(summary);
        hPane.setResizeWeight(0.5);
        vPaneL.setResizeWeight(0.5);
        vPaneR.setResizeWeight(0.5);      
        return hPane;
    } 
    
    public JPanel getButtonPane() {
        JPanel buttonPane = new JPanel();
        JRadioButton bS1 = new JRadioButton("Sector 1"); buttonPane.add(bS1); bS1.setActionCommand("1"); bS1.addActionListener(this);
        JRadioButton bS2 = new JRadioButton("Sector 2"); buttonPane.add(bS2); bS2.setActionCommand("2"); bS2.addActionListener(this); 
        JRadioButton bS3 = new JRadioButton("Sector 3"); buttonPane.add(bS3); bS3.setActionCommand("3"); bS3.addActionListener(this); 
        JRadioButton bS4 = new JRadioButton("Sector 4"); buttonPane.add(bS4); bS4.setActionCommand("4"); bS4.addActionListener(this); 
        JRadioButton bS5 = new JRadioButton("Sector 5"); buttonPane.add(bS5); bS5.setActionCommand("5"); bS5.addActionListener(this);  
        JRadioButton bS6 = new JRadioButton("Sector 6"); buttonPane.add(bS6); bS6.setActionCommand("6"); bS6.addActionListener(this); 
        bG1.add(bS1);bG1.add(bS2);bG1.add(bS3);bG1.add(bS4);bG1.add(bS5);bG1.add(bS6);
        bS2.setSelected(true);
        JRadioButton bpcal = new JRadioButton("PCAL"); buttonPane.add(bpcal); bpcal.setActionCommand("0"); bpcal.addActionListener(this);
        JRadioButton becin = new JRadioButton("ECin"); buttonPane.add(becin); becin.setActionCommand("1"); becin.addActionListener(this); 
        JRadioButton becio = new JRadioButton("ECou"); buttonPane.add(becio); becio.setActionCommand("2"); becio.addActionListener(this); 
        bG2.add(bpcal); bG2.add(becin); bG2.add(becio);
        bpcal.setSelected(true);
        JRadioButton bu = new JRadioButton("U"); buttonPane.add(bu); bu.setActionCommand("0"); bu.addActionListener(this);
        JRadioButton bv = new JRadioButton("V"); buttonPane.add(bv); bv.setActionCommand("1"); bv.addActionListener(this); 
        JRadioButton bw = new JRadioButton("W"); buttonPane.add(bw); bw.setActionCommand("2"); bw.addActionListener(this); 
        bG3.add(bu); bG3.add(bv); bG3.add(bw);
        bu.setSelected(true);
        
        return buttonPane;
    }    

    
    public void createHistos() {
        
       DataGroup dg_mip = new DataGroup(1,120);
        
        H1F h1 = new H1F() ; H2F h2 = new H2F();
        int n = 0;
        
        for (int is=1; is<7; is++) {
            h2 = new H2F("hi_pcal_uc_"+is,"hi_pcal_uc_"+is,50, 0., 100., 68, 1., 69.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordU");    
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_pcal_vc_"+is,"hi_pcal_vc_"+is,50, 0., 100., 62, 1., 63.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;           
            h2 = new H2F("hi_pcal_wc_"+is,"hi_pcal_wc_"+is,50, 0., 100., 62, 1., 63.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordW");  
            dg_mip.addDataSet(h2, n); n++;
            
            h2 = new H2F("hi_ecin_uc_"+is,"hi_ecin_uc_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordU");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecin_vc_"+is,"hi_ecin_vc_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecin_wc_"+is,"hi_ecin_wc_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordW");  
            dg_mip.addDataSet(h2, n); n++;
            
            h2 = new H2F("hi_ecou_uc_"+is,"hi_ecou_uc_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordU");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecou_vc_"+is,"hi_ecou_vc_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecou_wc_"+is,"hi_ecou_wc_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordW");
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_pcal_ectot_"+is,"hi_pcal_ectot_"+is,50,0.,100.,50,0.,200.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("ECTOT (MeV)");
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_pcal_ectot_max_"+is,"hi_pcal_ectot_max_"+is,100,0.,200.,100,0.,300.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("ECTOT (MeV)");
            dg_mip.addDataSet(h2, n); n++;
        }   
        
        for (int is=1; is<7; is++) {
            h2 = new H2F("hi_pcal_up_"+is,"hi_pcal_up_"+is,25, 0., 40., 68, 1., 69.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordU");    
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_pcal_vp_"+is,"hi_pcal_vp_"+is,25, 0., 40., 62, 1., 63.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;           
            h2 = new H2F("hi_pcal_wp_"+is,"hi_pcal_wp_"+is,25, 0., 40., 62, 1., 63.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordW");  
            dg_mip.addDataSet(h2, n); n++;
            
            h2 = new H2F("hi_ecin_up_"+is,"hi_ecin_up_"+is,25, 0., 40., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordU");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecin_vp_"+is,"hi_ecin_vp_"+is,25, 0., 40., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecin_wp_"+is,"hi_ecin_wp_"+is,25, 0., 40., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordW");  
            dg_mip.addDataSet(h2, n); n++;
           
            h2 = new H2F("hi_ecou_up_"+is,"hi_ecou_up_"+is,25, 0., 40., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordU");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecou_vp_"+is,"hi_ecou_vp_"+is,25, 0., 40., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecou_wp_"+is,"hi_ecou_wp_"+is,25, 0., 40., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordW");
            dg_mip.addDataSet(h2, n); n++;
        } 
        
        this.getDataGroup().add(dg_mip,4);        
    }        

    
    public void addEvent(DataEvent event) {

        Particle partRecEB = null;
        Particle recParticle = null;
        
        DataBank recPartEB = event.getBank("REC::Particle");
        DataBank recDeteEB = event.getBank("REC::Detector");
        DataBank recBankTB = event.getBank("TimeBasedTrkg::TBTracks");
        
        if(recPartEB!=null && recDeteEB!=null) {
            int nrows = recPartEB.rows();
            for(int loop = 0; loop < nrows; loop++){
                int pidCode = 0;
                if(recPartEB.getByte("charge", loop)==-1) pidCode = -211;
                if(recPartEB.getByte("charge", loop)==+1) pidCode = +211;
                Boolean pidPion = pidCode==-211 || pidCode==+211;
                if(pidPion) {
                recParticle = new Particle(pidCode,
                  recPartEB.getFloat("px", loop),
                  recPartEB.getFloat("py", loop),
                  recPartEB.getFloat("pz", loop),
                  recPartEB.getFloat("vx", loop),
                  recPartEB.getFloat("vy", loop),
                  recPartEB.getFloat("vz", loop));
                
                double energy1=0;
                double energy4=0;
                double energy7=0;

                for(int j=0; j<recDeteEB.rows(); j++) {
                    if(recDeteEB.getShort("pindex",j)==loop && recDeteEB.getShort("detector",j)==16) {
                        if(energy1 >= 0 && recDeteEB.getShort("layer",j) == 1) energy1 += recDeteEB.getFloat("energy",j);
                        if(energy4 >= 0 && recDeteEB.getShort("layer",j) == 4) energy4 += recDeteEB.getFloat("energy",j);
                        if(energy7 >= 0 && recDeteEB.getShort("layer",j) == 7) energy7 += recDeteEB.getFloat("energy",j);
                    }
                }
                
                recParticle.setProperty("energy1",energy1);
                recParticle.setProperty("energy4",energy4);
                recParticle.setProperty("energy7",energy7);
                
                if(partRecEB==null && pidPion) {
                    recParticle.setProperty("sector",recBankTB.getByte("sector", loop)*1.0);
                    partRecEB=recParticle;
                }
                }
            }
        }
        
//        event.removeBank("ECAL::clusters");
        engine.processDataEvent(event);
//        event.show();
//        event.removeBank("ECAL::clusters");
//        event.show();
        
        // EC clusters
        Boolean goodPC,goodECi,goodECo;
        
        if(event.hasBank("ECAL::clusters") && event.hasBank("ECAL::calib")){
            DataBank  bank1 = event.getBank("ECAL::clusters");
            DataBank  bank2 = event.getBank("ECAL::calib");
            int[] n1 = new int[6]; int[] n4 = new int[6]; int[] n7 = new int[6];
            float[][]   e1c = new float[6][20]; float[][][]   e1p = new float[6][3][20]; 
            float[][]   e4c = new float[6][20]; float[][][]   e4p = new float[6][3][20]; 
            float[][]   e7c = new float[6][20]; float[][][]   e7p = new float[6][3][20]; 
            float[][][] cU = new float[6][3][20];
            float[][][] cV = new float[6][3][20];
            float[][][] cW = new float[6][3][20];
            if(bank1.rows()==bank2.rows()) {
            int rows = bank1.rows();
            for(int loop = 0; loop < rows; loop++){
                int   is = bank1.getByte("sector", loop);
                int   il = bank1.getByte("layer", loop);
                float en = bank1.getFloat("energy",loop);
                float  x = bank1.getFloat("x", loop);
                float  y = bank1.getFloat("y", loop);
                float  z = bank1.getFloat("z", loop);
                int   iU = (bank1.getInt("coordU", loop)-4)/8;
                int   iV = (bank1.getInt("coordV", loop)-4)/8;
                int   iW = (bank1.getInt("coordW", loop)-4)/8;
                float enu = bank2.getFloat("recEU",loop);
                float env = bank2.getFloat("recEV",loop);
                float enw = bank2.getFloat("recEW",loop);
                goodPC = il==1&&n1[is-1]<20;  goodECi = il==4&&n4[is-1]<20;  goodECo = il==7&&n7[is-1]<20; 
                if (goodPC)  {e1c[is-1][n1[is-1]]=en; cU[is-1][0][n1[is-1]]=iU; cV[is-1][0][n1[is-1]]=iV; cW[is-1][0][n1[is-1]]=iW;}
                if (goodECi) {e4c[is-1][n4[is-1]]=en; cU[is-1][1][n4[is-1]]=iU; cV[is-1][1][n4[is-1]]=iV; cW[is-1][1][n4[is-1]]=iW;}
                if (goodECo) {e7c[is-1][n7[is-1]]=en; cU[is-1][2][n7[is-1]]=iU; cV[is-1][2][n7[is-1]]=iV; cW[is-1][2][n7[is-1]]=iW;}
                if (goodPC)  {e1p[is-1][0][n1[is-1]]=enu; e1p[is-1][1][n1[is-1]]=env; e1p[is-1][2][n1[is-1]]=enw; n1[is-1]++;}
                if (goodECi) {e4p[is-1][0][n4[is-1]]=enu; e4p[is-1][1][n4[is-1]]=env; e4p[is-1][2][n4[is-1]]=enw; n4[is-1]++;}
                if (goodECo) {e7p[is-1][0][n7[is-1]]=enu; e7p[is-1][1][n7[is-1]]=env; e7p[is-1][2][n7[is-1]]=enw; n7[is-1]++;}
            }
            }
                
            for (int is=0; is<6; is++) {
                int iis = is+1;
//                if(n1[is]>=1&&n1[is]<=4&&n4[is]>=1&&n4[is]<=4) { //Cut out vertical cosmic rays
                if(n1[is]==1&n4[is]==1&&n7[is]==1) { //Cut out vertical cosmic rays
//                    Boolean goodU = Math.abs(cU[is][1][n4[is]]-cU[is][2][n7[is]])<=1;
//                    Boolean goodV = Math.abs(cV[is][1][n4[is]]-cV[is][2][n7[is]])<=1;
//                    Boolean goodW = Math.abs(cW[is][1][n4[is]]-cW[is][2][n7[is]])<=1;
//                    Boolean goodUVW = goodU&&goodV&&goodW;
//                if(is==1&&partRecEB==null) System.out.println("No particle found");
//                if(is==1&&partRecEB!=null) System.out.println("Found Sector= "+partRecEB.getProperty("sector")+" P= "+partRecEB.p());
//                if(is==1&&partRecEB!=null) System.out.println("Energy1,e1 "+partRecEB.getProperty("energy1")+" "+e1[is][0]);
//                Boolean goodPion = (is==1&&partRecEB!=null&&partRecEB.p()>0.7);
//                if(is==1&&!goodPion) {n1[is]=0;n4[is]=0;n7[is]=0;}
                    
                double ectot = e4c[is][0]+e7c[is][0] ; double etot = e1c[is][0]+ectot ;
                this.getDataGroup().getItem(4).getH2F("hi_pcal_ectot_"+iis).fill(e1c[is][0]*1e3,ectot*1e3);
                this.getDataGroup().getItem(4).getH2F("hi_pcal_ectot_max_"+iis).fill(e1c[is][0]*1e3,ectot*1e3);
                
                for(int n=0; n<n1[is]; n++) {
                this.getDataGroup().getItem(4).getH2F("hi_pcal_uc_"+iis).fill(e1c[is][n]*1e3,cU[is][0][n]);
                this.getDataGroup().getItem(4).getH2F("hi_pcal_vc_"+iis).fill(e1c[is][n]*1e3,cV[is][0][n]);
                this.getDataGroup().getItem(4).getH2F("hi_pcal_wc_"+iis).fill(e1c[is][n]*1e3,cW[is][0][n]);
                this.getDataGroup().getItem(4).getH2F("hi_pcal_up_"+iis).fill(e1p[is][0][n]*1e3,cU[is][0][n]);
                this.getDataGroup().getItem(4).getH2F("hi_pcal_vp_"+iis).fill(e1p[is][1][n]*1e3,cV[is][0][n]);
                this.getDataGroup().getItem(4).getH2F("hi_pcal_wp_"+iis).fill(e1p[is][2][n]*1e3,cW[is][0][n]);
                }
                for(int n=0; n<n4[is]; n++) {
                this.getDataGroup().getItem(4).getH2F("hi_ecin_uc_"+iis).fill(e4c[is][n]*1e3,cU[is][1][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecin_vc_"+iis).fill(e4c[is][n]*1e3,cV[is][1][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecin_wc_"+iis).fill(e4c[is][n]*1e3,cW[is][1][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecin_up_"+iis).fill(e4p[is][0][n]*1e3,cU[is][1][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecin_vp_"+iis).fill(e4p[is][1][n]*1e3,cV[is][1][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecin_wp_"+iis).fill(e4p[is][2][n]*1e3,cW[is][1][n]);
                }
                for(int n=0; n<n7[is]; n++) {
                this.getDataGroup().getItem(4).getH2F("hi_ecou_uc_"+iis).fill(e7c[is][n]*1e3,cU[is][2][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecou_vc_"+iis).fill(e7c[is][n]*1e3,cV[is][2][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecou_wc_"+iis).fill(e7c[is][n]*1e3,cW[is][2][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecou_up_"+iis).fill(e7p[is][0][n]*1e3,cU[is][2][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecou_vp_"+iis).fill(e7p[is][1][n]*1e3,cV[is][2][n]);
                this.getDataGroup().getItem(4).getH2F("hi_ecou_wp_"+iis).fill(e7p[is][2][n]*1e3,cW[is][2][n]);
                }
                }
            }
        }
   
    }
        
    private class FitData {
        GraphErrors graph = null;

        public double xmin;
        public double xmax;
        public double amp;
        public double mean;
        public double meane;
        public double sigma;
        
        FitData(GraphErrors graph, double xmin, double xmax) {
            this.graph = graph;
            this.graph.setFunction(new F1D("f","[amp]*gaus(x,[mean],[sigma])", xmin, xmax));            
            this.graph.getFunction().setLineWidth(2);
        };
        
        public void setGraph(GraphErrors graph) {
            this.graph = graph;
        }
        
        public void initFit(double xmin, double xmax) {
            this.xmin = xmin;
            this.xmax = xmax;
            this.graph.getFunction().setParameter(0, getMaxYIDataSet(graph,xmin,xmax));
            this.graph.getFunction().setParameter(1, getMeanIDataSet(graph,xmin,xmax));
            this.graph.getFunction().setParameter(2, getRMSIDataSet(graph,xmin,xmax));
        }
        
        public void fitGraph() {
            this.graph.getFunction().setRange(xmin, xmax);
            DataFitter.fit(this.graph.getFunction(), graph, "Q");
            this.amp   = this.graph.getFunction().getParameter(0);
            this.mean  = this.graph.getFunction().parameter(1).value();
            this.meane = this.graph.getFunction().parameter(1).error();
            this.sigma = this.graph.getFunction().getParameter(2);                
        }
        
        public void plotGraph(EmbeddedCanvas c, int col) {
            this.graph.getFunction().setLineColor(col); 
            c.draw(graph);
        }
        
        public GraphErrors getGraph() {
            return this.graph;
        }
        
    }
    
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.is = dd.getSector();
        this.la = dd.getLayer();
        this.ic = dd.getComponent();   
        this.idet = ilmap;      
        
        if (la>3) return;
               
        updatePlots();
                
     }
    
    private void updatePlots() {        
        updateMIP(peaks);
        updateMIP(clusters);
        updateSummary();
    }
    
    private void updateMIP(EmbeddedCanvasTabbed c) {
        
        EmbeddedCanvas cp = c.getCanvas(c.getName());
        EmbeddedCanvas cf = c.getCanvas("Fits");
        String d = c.getName().substring(0,1).toLowerCase();
        int off = (d.equals("c")) ? 0:2;
        int    is = activeSector; int iis=is+10*off;
        String il = lay[activeLayer];
        String id = det[activeDetector];
        
        int  ilay = activeDetector*3+activeLayer;
        int    np = npmt[ilay];
        
        if(app.getInProcess()==0) return;
        
        if(app.getInProcess()==1) analyzeGraphs(is,is+1,activeDetector,activeDetector+1,activeLayer,activeLayer+1,d);
        
        cp.divide(3, 2);
        
        for (int i=0; i<3; i++) {
            cp.cd(i); cp.getPad(i).getAxisZ().setLog(true);
            cp.draw(this.getDataGroup().getItem(4).getH2F("hi_"+id+"_"+lay[i]+d+"_"+is));
        }
        
        cp.cd(3); cp.getPad(3).getAxisY().setLog(false); cp.draw(MipFits.getItem(iis,activeDetector,0,0).getGraph());
        cp.cd(4); cp.getPad(4).getAxisY().setLog(false); cp.draw(MipFits.getItem(iis,activeDetector,1,0).getGraph());
        cp.cd(5); cp.getPad(5).getAxisY().setLog(false); cp.draw(MipFits.getItem(iis,activeDetector,2,0).getGraph());
        
        cf.clear(); if (np==36) cf.divide(6,6); if (np>36)  cf.divide(8,9);
        
        for (int i=0; i<np ; i++) {
            cf.cd(i); cf.getPad(i).getAxisY().setLog(false);  
            cf.draw(MipFits.getItem(iis,activeDetector,activeLayer,i+1).getGraph());
        }
        
        if(app.getInProcess()>1) plotMIPSummary(c);           
    }

    public void analyze() {      
        analyzeGraphs(1,7,0,3,0,3,"c");
        analyzeGraphs(1,7,0,3,0,3,"p");
    }
    
    public void analyzeGraphs(int is1, int is2, int id1, int id2, int il1, int il2, String ro) {
        
        H2F h2=null;
        FitData fd = null;
        int off=0;
        double min=1,max=20,mip=10;
        
        for (int is=is1; is<is2; is++) {
            for (int id=id1; id<id2; id++) {
                if(ro.equals("c")) {min = fitLimc[id]; max = fitLimc[id+3]; off=0; mip=mipc[id];}
                if(ro.equals("p")) {min = fitLimp[id]; max = fitLimp[id+3]; off=2; mip=mipp[id];}  
                int iis = is+10*off;
                for (int il=0; il<3; il++) {
                    h2 = this.getDataGroup().getItem(4).getH2F("hi_"+det[id]+"_"+lay[il]+ro+"_"+is);
                    fd = new FitData(h2.projectionX().getGraph(),min,max); 
                    fd.initFit(min,max); fd.fitGraph();
                    fd.graph.getAttributes().setTitleX(h2.getTitleX()); 
                    MipFits.add(fd,iis,id,il,0);                    
                }                    
                for (int il=il1; il<il2; il++) {
                    int np = npmt[id*3+il];
                    double[]  x = new double[np]; double[]  ymean = new double[np]; double[] yrms = new double[np];
                    double[] xe = new double[np]; double[] ymeane = new double[np]; double[]   ye = new double[np]; 
                    h2 = this.getDataGroup().getItem(4).getH2F("hi_"+det[id]+"_"+lay[il]+ro+"_"+is);
                    for (int i=0; i<np; i++) {                     
                        fd = new FitData(h2.sliceY(i).getGraph(),min,max) ; 
                        fd.initFit(min,max); fd.fitGraph();
                        fd.graph.getAttributes().setTitleX("Sector "+is+" "+det[id]+" "+lay[il]+(i+1));
                        MipFits.add(fd,iis,id,il,i+1);
                        x[i] = i+1; xe[i]=0; ye[i]=0; yrms[i]=0;
                        double mean = fd.mean;                        
                        if(mean>0) yrms[i] = fd.sigma/mean; 
                         ymean[i] = mean/mip;
                        ymeane[i] = fd.meane/mip;
                    }
                    GraphErrors mean = new GraphErrors("MIP_"+is+"_"+id+" "+il,x,ymean,xe,ymeane);                   
                    GraphErrors  rms = new GraphErrors("MIP_"+is+"_"+id+" "+il,x,yrms,xe,ye);                  
                    MIPSummary.add(mean, 1+off,is,id,il);
                    MIPSummary.add(rms,  2+off,is,id,il);                    
                }
            }
        }
        
    }
    
    public void plotMIPSummary(EmbeddedCanvasTabbed ct) {
        
        EmbeddedCanvas c = ct.getCanvas("Summary");
        String ro = ct.getName().substring(0,1).toLowerCase();
        int il=activeLayer, n=0; 
        int off = (ro.equals("c")) ? 0:2;
        String sil = lay[activeLayer].toUpperCase();
        String[] det ={"PCAL","ECin","ECou"};
        c.divide(6, 6);

        for (int id=0; id<3; id++) {
            F1D f1 = new F1D("p0","[a]",0.,npmt[id*3+il]); f1.setParameter(0,1);
            for (int is=1; is<7; is++) {
               GraphErrors plot = MIPSummary.getItem(1+off,is,id,il);
               c.cd(n); c.getPad(n).getAxisY().setRange(0.5, 1.5); 
               c.getPad(n).setAxisTitleFontSize(14); c.getPad(n).setTitleFontSize(14);
               if(n<6)  plot.getAttributes().setTitle("SECTOR "+is); 
               if(n==0) plot.getAttributes().setTitleY("MEAN / MIP");
               plot.getAttributes().setTitleX(det[id]+" "+sil+" PMT");
               n++; c.draw(plot);
               f1.setLineColor(3); f1.setLineWidth(3); c.draw(f1,"same");
            }
        }
        
        for (int id=0; id<3; id++) {
            for (int is=1; is<7; is++) {
               GraphErrors plot = MIPSummary.getItem(2+off,is,id,il);
               c.cd(n); c.getPad(n).getAxisY().setRange(0.,1.0); 
               c.getPad(n).setAxisTitleFontSize(14);
               if(n==18) plot.getAttributes().setTitleY("RMS / MEAN");
               plot.getAttributes().setTitleX(det[id]+" "+sil+" PMT");
               n++; c.draw(plot);
            }
        }
    }
    private void updateSummary() {
        
        H2F h2;
        EmbeddedCanvas c = null;
        c = this.summary.getCanvas("PCAL/ECTOT");
        c.divide(3,2);
        for (int is=1; is<7; is++) {
            h2 = this.getDataGroup().getItem(4).getH2F("hi_pcal_ectot_max_"+is);   
            c.cd(is-1); c.getPad(is-1).getAxisZ().setLog(true);       
            c.draw(h2);   
        }
        
    }
    
    private double getMaxYIDataSet(IDataSet data, double min, double max) {
        double max1 = 0;
        double xMax = 0;
        for (int i = 0; i < data.getDataSize(0); i++) {
            double x = data.getDataX(i);
            double y = data.getDataY(i);
            if (x > min && x < max && y != 0) {
                if (y > max1) {
                    max1 = y;
                    xMax = x;
                }
            }
        }
        return max1;
    }
    
    private double getMeanIDataSet(IDataSet data, double min, double max) {
        int nsamples = 0;
        double sum = 0;
        double nEntries = 0;
        for (int i = 0; i < data.getDataSize(0); i++) {
            double x = data.getDataX(i);
            double y = data.getDataY(i);
            if (x > min && x < max && y != 0) {
                nsamples++;
                sum += x * y;
                nEntries += y;
            }
        }
        return sum / (double) nEntries;
    }
    
    private double getRMSIDataSet(IDataSet data, double min, double max) {
        int nsamples = 0;
        double mean = getMeanIDataSet(data, min, max);
        double sum = 0;
        double nEntries = 0;

        for (int i = 0; i < data.getDataSize(0); i++) {
            double x = data.getDataX(i);
            double y = data.getDataY(i);
            if (x > min && x < max && y != 0) {
                nsamples++;
                sum += Math.pow(x - mean, 2) * y;
                nEntries += y;
            }
        }
        return Math.sqrt(sum / (double) nEntries);
    }   
    
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        this.activeSector   = Integer.parseInt(bG1.getSelection().getActionCommand());
        this.activeDetector = Integer.parseInt(bG2.getSelection().getActionCommand()); 
        this.activeLayer    = Integer.parseInt(bG3.getSelection().getActionCommand()); 
        updatePlots();
    }    
    
}
