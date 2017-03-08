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
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.utils.groups.IndexedList;

public class ECGainsApp extends FCApplication implements ActionListener {
    
    JPanel              engineView = new JPanel();
    EmbeddedCanvasTabbed    strips = new EmbeddedCanvasTabbed("Strips");
    EmbeddedCanvasTabbed     peaks = new EmbeddedCanvasTabbed("Peaks");
    EmbeddedCanvasTabbed  clusters = new EmbeddedCanvasTabbed("Clusters");
    EmbeddedCanvasTabbed   summary = new EmbeddedCanvasTabbed("Summary");
    EmbeddedCanvas               c = this.getCanvas(this.getName()); 
    ButtonGroup                bG1 = new ButtonGroup();
    ButtonGroup                bG2 = new ButtonGroup();
    ButtonGroup                bG3 = new ButtonGroup();    
    public int        activeSector = 2;
    public String   activeDetector = "pcal";                
    public String      activeLayer = "u";    
    
    int is,la,ic,idet,nstr;
    
    float[][][][] ecmean = new float[6][3][3][68];
    float[][][][]  ecrms = new float[6][3][3][68];
    String[]         det = new String[]{"pcal","ecin","ecou"};
    String[]         lay = new String[]{"u","v","w"};
    double[]         mip = new double[]{30,30,48};  
    
    IndexedList<GraphErrors> MIPSummary  = new IndexedList<GraphErrors>(4);
    
    public ECGainsApp(String name, ECPixels[] ecPix) {
        super(name,ecPix);    
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
        clusters.addCanvas("MIP");
        clusters.addCanvas("Summary");
        return engineView;       
    }  
    
    public JSplitPane getCanvasPane() {
        
        JSplitPane    hPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        JSplitPane   vPaneL = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
        JSplitPane   vPaneR = new JSplitPane(JSplitPane.VERTICAL_SPLIT); 
        hPane.setLeftComponent(vPaneL);
        hPane.setRightComponent(vPaneR);
        vPaneL.setTopComponent(strips);
        vPaneL.setBottomComponent(peaks);
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
        JRadioButton bpcal = new JRadioButton("PCAL"); buttonPane.add(bpcal); bpcal.setActionCommand("pcal"); bpcal.addActionListener(this);
        JRadioButton becin = new JRadioButton("ECin"); buttonPane.add(becin); becin.setActionCommand("ecin"); becin.addActionListener(this); 
        JRadioButton becio = new JRadioButton("ECou"); buttonPane.add(becio); becio.setActionCommand("ecou"); becio.addActionListener(this); 
        bG2.add(bpcal); bG2.add(becin); bG2.add(becio);
        bpcal.setSelected(true);
        JRadioButton bu = new JRadioButton("U"); buttonPane.add(bu); bu.setActionCommand("u"); bu.addActionListener(this);
        JRadioButton bv = new JRadioButton("V"); buttonPane.add(bv); bv.setActionCommand("v"); bv.addActionListener(this); 
        JRadioButton bw = new JRadioButton("W"); buttonPane.add(bw); bw.setActionCommand("w"); bw.addActionListener(this); 
        bG3.add(bu); bG3.add(bv); bG3.add(bw);
        bu.setSelected(true);
        
        return buttonPane;
    }    

    
    public void createHistos() {
        
       DataGroup dg_mip = new DataGroup(1,72);
        
        H1F h1 = new H1F() ; H2F h2 = new H2F();
        int n = 0;
        
        for (int is=1; is<7; is++) {
            h1 = new H1F("hi_pcal_"+is,"hi_pcal_"+is,100, 0., 0.5);
            h1.setTitleX("Sector "+is+" PCAL (GeV)");
            h1.setTitleY("Counts");
            dg_mip.addDataSet(h1, n); n++;
            h2 = new H2F("hi_pcal_u_"+is,"hi_pcal_u_"+is,50, 0., 100., 68, 1., 69.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordU");    
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_pcal_v_"+is,"hi_pcal_v_"+is,50, 0., 100., 62, 1., 63.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;           
            h2 = new H2F("hi_pcal_w_"+is,"hi_pcal_w_"+is,50, 0., 100., 62, 1., 63.);
            h2.setTitleX("Sector "+is+" PCAL (MeV)");
            h2.setTitleY("coordW");  
            dg_mip.addDataSet(h2, n); n++;
            
            h1 = new H1F("hi_ecin_"+is,"hi_ecin_"+is,100, 0., 0.5);
            h1.setTitleX("Sector "+is+" ECin (GeV)");
            h1.setTitleY("Counts");
            dg_mip.addDataSet(h1, n); n++;
            h2 = new H2F("hi_ecin_u_"+is,"hi_ecin_u_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordU");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecin_v_"+is,"hi_ecin_v_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecin_w_"+is,"hi_ecin_w_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECin (MeV)");
            h2.setTitleY("coordW");  
            dg_mip.addDataSet(h2, n); n++;
            
            h1 = new H1F("hi_ecou_"+is,"hi_ecou_"+is,100, 0., 0.5);
            h1.setTitleX("Sector "+is+" ECou (GeV)");
            h1.setTitleY("Counts");
            dg_mip.addDataSet(h1, n); n++;
            h2 = new H2F("hi_ecou_u_"+is,"hi_ecou_u_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordU");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecou_v_"+is,"hi_ecou_v_"+is,50, 0., 100., 36, 1., 37.);
            h2.setTitleX("Sector "+is+" ECou (MeV)");
            h2.setTitleY("coordV");        
            dg_mip.addDataSet(h2, n); n++;
            h2 = new H2F("hi_ecou_w_"+is,"hi_ecou_w_"+is,50, 0., 100., 36, 1., 37.);
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
        
        // EC clusters
        if(event.hasBank("ECAL::clusters")==true){
            DataBank  bank = event.getBank("ECAL::clusters");
            int rows = bank.rows();
            int[] n1 = new int[6]; 
            int[] n4 = new int[6];
            int[] n7 = new int[6];
            float[][] e1 = new float[6][20]; float[][][] cU = new float[6][3][20];
            float[][] e4 = new float[6][20]; float[][][] cV = new float[6][3][20];
            float[][] e7 = new float[6][20]; float[][][] cW = new float[6][3][20];
            for(int loop = 0; loop < rows; loop++){
                int   is = bank.getByte("sector", loop);
                int   il = bank.getByte("layer", loop);
                float en = bank.getFloat("energy",loop);
                float  x = bank.getFloat("x", loop);
                float  y = bank.getFloat("y", loop);
                float  z = bank.getFloat("z", loop);
                int   iU = (bank.getInt("coordU", loop)-4)/8;
                int   iV = (bank.getInt("coordV", loop)-4)/8;
                int   iW = (bank.getInt("coordW", loop)-4)/8;
                if (il==1&&n1[is-1]<20) {e1[is-1][n1[is-1]]=en ; cU[is-1][0][n1[is-1]]=iU; cV[is-1][0][n1[is-1]]=iV; cW[is-1][0][n1[is-1]]=iW; n1[is-1]++;}
                if (il==4&&n4[is-1]<20) {e4[is-1][n4[is-1]]=en ; cU[is-1][1][n4[is-1]]=iU; cV[is-1][1][n4[is-1]]=iV; cW[is-1][1][n4[is-1]]=iW; n4[is-1]++;}
                if (il==7&&n7[is-1]<20) {e7[is-1][n7[is-1]]=en ; cU[is-1][2][n7[is-1]]=iU; cV[is-1][2][n7[is-1]]=iV; cW[is-1][2][n7[is-1]]=iW; n7[is-1]++;}
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
                Boolean goodPion = (is==1&&partRecEB!=null&&partRecEB.p()>0.7);
                if(is==1&&!goodPion) {n1[is]=0;n4[is]=0;n7[is]=0;}
                for(int n=0; n<n1[is]; n++) {this.getDataGroup().getItem(4).getH1F("hi_pcal_"  +iis).fill(e1[is][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_pcal_u_"+iis).fill(e1[is][n]*1e3,cU[is][0][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_pcal_v_"+iis).fill(e1[is][n]*1e3,cV[is][0][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_pcal_w_"+iis).fill(e1[is][n]*1e3,cW[is][0][n]);
                }
                for(int n=0; n<n4[is]; n++) {this.getDataGroup().getItem(4).getH1F("hi_ecin_"  +iis).fill(e4[is][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_ecin_u_"+iis).fill(e4[is][n]*1e3,cU[is][1][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_ecin_v_"+iis).fill(e4[is][n]*1e3,cV[is][1][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_ecin_w_"+iis).fill(e4[is][n]*1e3,cW[is][1][n]);
                }
                for(int n=0; n<n7[is]; n++) {this.getDataGroup().getItem(4).getH1F("hi_ecou_"  +iis).fill(e7[is][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_ecou_u_"+iis).fill(e7[is][n]*1e3,cU[is][2][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_ecou_v_"+iis).fill(e7[is][n]*1e3,cV[is][2][n]);
                                             this.getDataGroup().getItem(4).getH2F("hi_ecou_w_"+iis).fill(e7[is][n]*1e3,cW[is][2][n]);
                }
                }
            }
        }
   
    }
        
    public void updateCanvas(DetectorDescriptor dd) {
        
        this.is = dd.getSector();
        this.la = dd.getLayer();
        this.ic = dd.getComponent();   
        this.idet = ilmap;      
        
        if (la>3) return;
        
        this.nstr = ecPix[idet].ec_nstr[la-1];    
        
        updateStrips();   
        updatePeaks();   
        updateClusters(); 
        updateSummary();
                
     }
    
    private void updateStrips() {
        
    }

    private void updatePeaks() {
        
    }

    private void updateClusters() {
        
        H1F h1 ; H2F h2;
        
        EmbeddedCanvas cc = this.clusters.getCanvas("Clusters");
        EmbeddedCanvas cm = this.clusters.getCanvas("MIP");  
               
        h2 = (H2F) this.getDataGroup().getItem(4).getH2F("hi_"+activeDetector+"_"+activeLayer+"_"+activeSector);
        int npmt = h2.sliceX(1).getData().length;
        cm.clear();
        cc.divide(3, 2);
        if (npmt==36) cm.divide(6,6);
        if (npmt>36)  cm.divide(8,9);
        
        cc.cd(0); cc.getPad(0).getAxisZ().setLog(true);
        cc.draw(this.getDataGroup().getItem(4).getH2F("hi_"+activeDetector+"_u_"+activeSector));
        cc.cd(1); cc.getPad(1).getAxisZ().setLog(true);
        cc.draw(this.getDataGroup().getItem(4).getH2F("hi_"+activeDetector+"_v_"+activeSector));
        cc.cd(2); cc.getPad(2).getAxisZ().setLog(true);
        cc.draw(this.getDataGroup().getItem(4).getH2F("hi_"+activeDetector+"_w_"+activeSector));  
        
        cc.cd(3); cc.getPad(3).getAxisY().setLog(true); 
        h1 = this.getDataGroup().getItem(4).getH1F("hi_pcal_"+activeSector);
        h1.getAttributes().setFillColor(activeDetector=="pcal" ? 4:0); cc.draw(h1);
        cc.cd(4); cc.getPad(4).getAxisY().setLog(true);
        h1 = this.getDataGroup().getItem(4).getH1F("hi_ecin_"+activeSector);
        h1.getAttributes().setFillColor(activeDetector=="ecin" ? 4:0); cc.draw(h1);
        cc.cd(5); cc.getPad(5).getAxisY().setLog(true);
        h1 = this.getDataGroup().getItem(4).getH1F("hi_ecou_"+activeSector);    
        h1.getAttributes().setFillColor(activeDetector=="ecou" ? 4:0); cc.draw(h1);
                
        for (int i=0; i<npmt ; i++) {
            h1 = h2.sliceY(i); h1.setTitleX("Sector "+activeSector+"  PMT "+(i+1));  
            h1.setFillColor(2); h1.setOptStat(1100); 
            cm.cd(i); cm.getPad(i).getAxisY().setLog(false); cm.draw(h1); 
        }
        
        storeMIPGraphs();
        plotMIPSummary();        
    }
    
    public void storeMIPGraphs() {
        
        H1F h1;
        
        for (int is=1; is<7; is++) {
            for (int id=0; id<3; id++) {
                for (int il=0; il<3; il++) {
                    H2F h2 = new H2F();
                    h2 = this.getDataGroup().getItem(4).getH2F("hi_"+det[id]+"_"+lay[il]+"_"+is);
                    int npmt = h2.sliceX(1).getData().length;
                    double[]  x = new double[npmt]; double[]  ymean = new double[npmt]; double[] yrms = new double[npmt];
                    double[] xe = new double[npmt]; double[]     ye = new double[npmt]; 
                    for (int i=0; i<npmt; i++) {
                        x[i] = i+1; xe[i]=0; ye[i]=0; yrms[i]=0;
                        double mean = h2.sliceY(i).getMean();                          
                        if(mean>0) yrms[i] = h2.sliceY(i).getRMS()/mean; 
                        ymean[i]=mean/mip[id];
                    }
                    GraphErrors mean = new GraphErrors("MIP_"+is+"_"+id+" "+il,x,ymean,xe,ye);
                    GraphErrors  rms = new GraphErrors("MIP_"+is+"_"+id+" "+il,x,yrms,xe,ye);
                    MIPSummary.add(mean, 1,is,id,il);
                    MIPSummary.add(rms,  2,is,id,il);
                }
            }
        }
        
    }
    
    public void plotMIPSummary() {
        
        int il=0, n=0; 
        int[] npmt = new int[]{68,62,62,36,36,36,36,36,36};
        String[] det ={"PCAL","ECin","ECou"};
        EmbeddedCanvas c = this.clusters.getCanvas("Summary"); c.divide(6, 6);
        if(activeLayer=="u") il=0; if(activeLayer=="v") il=1; if(activeLayer=="w") il=2;

        for (int id=0; id<3; id++) {
            F1D f1 = new F1D("p0","[a]",0.,npmt[id*3+il]); f1.setParameter(0,1);
            for (int is=1; is<7; is++) {
               GraphErrors plot = MIPSummary.getItem(1,is,id,il);
               c.cd(n); c.getPad(n).getAxisY().setRange(0.8, 1.6); 
               c.getPad(n).setAxisTitleFontSize(14);
               c.getPad(n).setTitleFontSize(14);
               if(n<6)  plot.getAttributes().setTitle("SECTOR "+is); 
               if(n==0) plot.getAttributes().setTitleY("MEAN / MIP");
               plot.getAttributes().setTitleX(det[id]+" PMT");
               n++; c.draw(plot);
               f1.setLineColor(3); c.draw(f1,"same");
            }
        }
        for (int id=0; id<3; id++) {
            for (int is=1; is<7; is++) {
               GraphErrors plot = MIPSummary.getItem(2,is,id,il);
               c.cd(n); c.getPad(n).getAxisY().setRange(0., 0.6); 
               c.getPad(n).setAxisTitleFontSize(14);
               if(n==18) plot.getAttributes().setTitleY("RMS / MEAN");
               plot.getAttributes().setTitleX(det[id]+" PMT");
               n++; c.draw(plot);
            }
        }
    }
    private void updateSummary() {
        
    }
    
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        this.activeSector   = Integer.parseInt(bG1.getSelection().getActionCommand());
        this.activeDetector = bG2.getSelection().getActionCommand();
        this.activeLayer    = bG3.getSelection().getActionCommand();
        updateClusters();
    }    
    
}
