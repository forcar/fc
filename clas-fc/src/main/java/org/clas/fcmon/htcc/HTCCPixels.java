package org.clas.fcmon.htcc;

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

public class HTCCPixels {
	
    public Strips          strips = new Strips();
    DatabaseConstantProvider ccdb = new DatabaseConstantProvider(1,"default");
	HTCCConstants              cc = new HTCCConstants();
    double htcc_xpix[][][] = new double[4][124][7];
    double htcc_ypix[][][] = new double[4][124][7];
    
    public    int     htcc_nstr[] = {4};
    public    int     htcc_nsec[] = {6};
    public    int     htcc_nlay[] = {1};
    public double           amax[]= {3000.};
   
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
	
    public HTCCPixels(String det) {
        if (det.equals("HTCC")) id=0;
        nstr = htcc_nstr[id];
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
        System.out.println("HTCCPixels.init():");
        Lmap_a.clear();       
        Lmap_t.clear();
    }
    
    public void pixdef() {
        
        System.out.println("HTCCPixels.pixdef(): "+this.detName); 
                   
        cc.RADIUS = 50;
        cc.THICK  = 100;
        cc.ANGLE  = 2*Math.PI/6/2;
        
        double tang = Math.tan(cc.ANGLE);
        
        for (int i=0; i<htcc_nstr[0]; i++) {
            cc.LB[i]  = (cc.RADIUS + cc.THICK*i)*tang*2;
            cc.UB[i]  = (cc.RADIUS + cc.THICK*(i+1))*tang*2;
             cc.R[i]  =  cc.RADIUS + cc.THICK/2 + cc.THICK*i;
        }
        
        cc.setGeometry();

	}
		       
    public void pixrot() {
        
        System.out.println("HTCCPixels.pixrot(): "+this.detName);
		
        double[] theta={270.0,330.0,30.0,90.0,150.0,210.0};

        for(int is=0; is<6; is++) {
            double thet=theta[is]*Math.PI/180.;
            for (int ipix=0; ipix<2*nstr; ipix++) {
                for (int k=0;k<4;k++){
                    htcc_xpix[k][ipix][is]= -(htcc_xpix[k][ipix][6]*Math.cos(thet)+htcc_ypix[k][ipix][6]*Math.sin(thet));
                    htcc_ypix[k][ipix][is]=  -htcc_xpix[k][ipix][6]*Math.sin(thet)+htcc_ypix[k][ipix][6]*Math.cos(thet);
                }
            }
        }	    	
    }
    
    public void initHistograms(String hipoFile) {
        
        System.out.println("HTCCPixels.initHistograms(): "+this.detName);  
        
        String iid;
        
        DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_a_Sevd = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Sevd = new DetectorCollection<H2F>();
        
        double nend = nstr+1;  
        
        for (int is=1; is<7 ; is++) {
            int ill=0; iid="s"+Integer.toString(is)+"_l"+Integer.toString(ill)+"_c";
            H2_a_Hist.add(is, 0, 0, new H2F("a_gmean_"+iid+0, 100,   0., amax[id],nstr, 1., nend));
            H2_t_Hist.add(is, 0, 0, new H2F("a_tdif_"+iid+0,  100, -35.,      35.,nstr, 1., nend));
            for (int il=1 ; il<3 ; il++){
                iid="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100,   0., amax[id],nstr, 1., nend));
                H2_t_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100,   0.,  200.,nstr, 1., nend));
                H2_a_Hist.add(is, il, 1, new H2F("a_raw_"+iid+1,      100,   0., amax[id], 100,  0., 200.));
                H2_a_Hist.add(is, il, 3, new H2F("a_ped_"+iid+3,       40, -20.,  20., nstr, 1., nend)); 
                H2_a_Hist.add(is, il, 5, new H2F("a_fadc_"+iid+5,     100,   0., 100., nstr, 1., nend));
                H1_a_Sevd.add(is, il, 0, new H1F("a_sed_"+iid+0,                       nstr, 1., nend));
                H2_a_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+iid+0, 100,   0., 100., nstr, 1., nend));
                H2_a_Sevd.add(is, il, 1, new H2F("a_sed_fadc_"+iid+1, 100,   0., 100., nstr, 1., nend));
                H2_t_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+iid+0, 200,   0., 100., nstr, 1., nend));
            }
            iid="s"+Integer.toString(is)+"_l"+Integer.toString(3)+"_c";
            H2_t_Hist.add(is, 3, 3, new H2F("a_raw_"+iid+3, 100, 0., 200., 6, 0., 6.)); // No phase correction
            H2_t_Hist.add(is, 3, 4, new H2F("a_raw_"+iid+4, 100, 0., 200., 6, 0., 6.)); // With phase correction
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
