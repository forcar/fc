package org.clas.fcmon.ctof;

import java.util.Arrays;
import java.util.TreeMap;

import org.clas.fcmon.tools.FCCalibrationData;
import org.clas.fcmon.tools.Strips;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedTable;

public class CTOFPixels {
	
    public Strips          strips = new Strips();
    DatabaseConstantProvider ccdb = new DatabaseConstantProvider(1,"default");
	CTOFConstants              cc = new CTOFConstants();
    double ctof_xpix[][][] = new double[4][124][7];
    double ctof_ypix[][][] = new double[4][124][7];
    
    public    int     ctof_nstr[] = {48};
    public double           amax[]= {8000.,8000.};
    public    int           tlim[]= {0,300};
    public    int            fmax = 100;
    
    int        nha[][] = new    int[6][4];
    int        nht[][] = new    int[6][4];
    int    strra[][][] = new    int[6][4][48]; 
    int    strrt[][][] = new    int[6][4][48]; 
    int     adcr[][][] = new    int[6][4][48];      
    float   tdcr[][][] = new  float[6][4][48]; 
    float     tf[][][] = new  float[6][4][48]; 
    float     ph[][][] = new  float[6][4][48]; 
    
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
    public IndexedList<double[]>                     Lmap_a_z = new IndexedList<double[]>(2);
    public IndexedList<double[]>                     Lmap_t_z = new IndexedList<double[]>(2);
    
    int id;
	public int nstr;
	public String detName = null;
	
    public CTOFPixels(String det, int fmax, int tmax) {
        if (det.equals("CTOF")) id=0;
        if (det.equals("CND"))  id=1;
        this.fmax = fmax;
        tlim[1] = tmax;
        nstr = ctof_nstr[id];
        detName = det;
        pixdef();
//        pixrot();
    }
    public void getLmapMinMax(int is1, int is2, int il, int opt){
        TreeMap<Integer,Object> map = null;
        double min,max,avg,aavg=0,tavg=0;
        double[] a = {1000,0,0};
        double[] t = {1000,0,0};
        for (int is=is1 ; is<is2; is++) {
            map = Lmap_a.get(is, il, opt);
            min = (double) map.get(2); max = (double) map.get(3); avg = (double) map.get(4);
            if (min<a[0]) a[0]=min; if (max>a[1]) a[1]=max; aavg+=avg;
            map = Lmap_t.get(is, il, opt);
            min = (double) map.get(2); max = (double) map.get(3); avg = (double) map.get(4);
            if (min<t[0]) t[0]=min; if (max>t[1]) t[1]=max; tavg+=avg;
        }

        a[2]=Math.min(500000,aavg/(is2-is1));
        t[2]=Math.min(500000,tavg/(is2-is1));
        
        Lmap_a_z.add(a,il,opt);
        Lmap_t_z.add(t,il,opt);        
    }	
    
    public void init() {
        System.out.println("CTOFPixels.init():");
        Lmap_a.clear();       
        Lmap_t.clear();
    }

    public void pixdef() {
        
        System.out.println("CTOFPixels.pixdef(): "+this.detName); 
        String table=null;
            
        switch (id) { 
        case 0: table = "/geometry/ctof"; break; 
        }
        
        ccdb.loadTable(table+"/ctof");  
       
//        cc.RADIUS = ccdb.getDouble(table+"/ctof/radius",0);
//        cc.LENGTH = ccdb.getDouble(table+"/ctof/length",0);
//        cc.THICK  = ccdb.getDouble(table+"/ctof/thick",0);
//        cc.WIDTH  = ccdb.getDouble(table+"/ctof/width",0);
        cc.setGeometry();

	}
		       
    public void pixrot() {
        
        System.out.println("CTOFPixels.pixrot(): "+this.detName);
		
        double[] theta={270.0,330.0,30.0,90.0,150.0,210.0};

        for(int is=0; is<6; is++) {
            double thet=theta[is]*Math.PI/180.;
            for (int ipix=0; ipix<2*nstr; ipix++) {
                for (int k=0;k<4;k++){
                    ctof_xpix[k][ipix][is]= -(ctof_xpix[k][ipix][6]*Math.cos(thet)+ctof_ypix[k][ipix][6]*Math.sin(thet));
                    ctof_ypix[k][ipix][is]=  -ctof_xpix[k][ipix][6]*Math.sin(thet)+ctof_ypix[k][ipix][6]*Math.cos(thet);
                }
            }
        }	    	
    }
    
    public void initHistograms(String hipoFile) {
        
        System.out.println("CTOFPixels.initHistograms(): "+this.detName);  
        
        String iid;
        
        DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_a_Sevd = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Sevd = new DetectorCollection<H2F>();
        
        double nend = nstr+1;  
        
        for (int is=1; is<2 ; is++) {
            int ill=0; iid="s"+Integer.toString(is)+"_l"+Integer.toString(ill)+"_c";
            H2_a_Hist.add(is, 0, 0, new H2F("a_gmean_"+iid+0, 100,   0.,  amax[0],nstr, 1., nend));
            H2_t_Hist.add(is, 0, 0, new H2F("a_tdif_"+iid+0,  100, -35.,      35.,nstr, 1., nend));
            H2_t_Hist.add(is, 0, 1, new H2F("b_tdif_"+iid+1,   50, -20.,      20.,48,1.,49.));
            for (int ip=1; ip<49; ip++) {
//                H2_t_Hist.add(is, ip, 2, new H2F("c_tdif_"+iid+1+ip,   50, -32., -15.,50,-0.6, 0.6));
                H2_t_Hist.add(is, ip, 2, new H2F("c_tdif_"+iid+1+ip,   50, -10., 10.,50,-0.6, 0.6));
            }
           for (int il=1 ; il<3 ; il++){
                iid="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100,    0., amax[0],nstr, 1., nend));
                H2_t_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100, tlim[0], tlim[1],nstr, 1., nend));
                H2_a_Hist.add(is, il, 1, new H2F("a_raw_"+iid+1,      100,    0., amax[1], 100, tlim[0], tlim[1]));
                H2_a_Hist.add(is, il, 3, new H2F("a_ped_"+iid+3,       40, -20.,  20., nstr, 1., nend)); 
                H2_a_Hist.add(is, il, 5, new H2F("a_fadc_"+iid+5,     100,   0., fmax, nstr, 1., nend));
                H2_a_Hist.add(is, il, 6, new H2F("a_fadc_"+iid+6,     100, -30.,  30., nstr, 1., nend));
                H1_a_Sevd.add(is, il, 0, new H1F("a_sed_"+iid+0,                       nstr, 1., nend));
                H2_a_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+iid+0, 100,   0., fmax, nstr, 1., nend));
                H2_a_Sevd.add(is, il, 1, new H2F("a_sed_fadc_"+iid+1, 100,   0., fmax, nstr, 1., nend));
                H2_t_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+iid+0, 200,   0., fmax, nstr, 1., nend));
            }
            iid="s"+Integer.toString(is)+"_l"+Integer.toString(3)+"_c";
            H2_t_Hist.add(is, 3, 3, new H2F("a_raw_"+iid+3, 100, -30., 30, 6, 0., 6.)); // No phase correction
            H2_t_Hist.add(is, 3, 4, new H2F("a_raw_"+iid+4, 100, tlim[0],tlim[1], 6, 0., 6.)); // With phase correction
        }       

        if(!hipoFile.equals(" ")){
            FCCalibrationData calib = new FCCalibrationData();
            calib.getFile(hipoFile);
            H2_a_Hist = calib.getCollection("H2_a_Hist");
            H2_t_Hist = calib.getCollection("H2_t_Hist");
        }         
        
        strips.addH1DMap("H1_a_Sevd",  H1_a_Sevd);
        strips.addH1DMap("H1_t_Sevd",  H1_t_Sevd);
        strips.addH2DMap("H2_a_Hist",  H2_a_Hist);
        strips.addH2DMap("H2_t_Hist",  H2_t_Hist);
        strips.addH2DMap("H2_a_Sevd",  H2_a_Sevd);
        strips.addH2DMap("H2_t_Sevd",  H2_t_Sevd);
    } 
    
}
