package org.clas.fcmon.cnd;

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

public class CNDPixels {
	
    public Strips          strips = new Strips();
    DatabaseConstantProvider ccdb = new DatabaseConstantProvider(1,"default");
	CNDConstants               cc = new CNDConstants();
    double cnd_xpix[][][] = new double[4][124][7];
    double cnd_ypix[][][] = new double[4][124][7];
    
    public    int     cnd_nsec[] = {24};
    public    int     cnd_nstr[] = {3};
    public    int     cnd_nlay[] = {1};
      
    int        nha[][] = new    int[24][2];
    int        nht[][] = new    int[24][2];
    int    strra[][][] = new    int[24][2][3]; 
    int    strrt[][][] = new    int[24][2][3]; 
    int     adcr[][][] = new    int[24][2][3];      
    float   tdcr[][][] = new  float[24][2][3]; 
    float     tf[][][] = new  float[24][2][3]; 
    float     ph[][][] = new  float[24][2][3]; 
    
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
    public IndexedList<double[]>                     Lmap_a_z = new IndexedList<double[]>(2);
    public IndexedList<double[]>                     Lmap_t_z = new IndexedList<double[]>(2);
    
    int id;
	public int nstr;
	public String detName = null;
	
    public CNDPixels(String det) {
        nstr = cnd_nstr[0];
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
        System.out.println("CNDPixels.init():");
        Lmap_a.clear();       
        Lmap_t.clear();
    }

    public void pixdef() {
        
        System.out.println("CNDPixels.pixdef(): "+this.detName); 
        String table=null;
            
        switch (id) { 
        case 0: table = "/geometry/cnd"; break; 
        }
        
        ccdb.loadTable(table+"/cnd");  
       
        cc.RADIUS = ccdb.getDouble(table+"/cnd/InnerRadius",0);
        cc.ANGLE  = ccdb.getDouble(table+"/cnd/OpenAngle",0);
        cc.THICK  = ccdb.getDouble(table+"/cnd/Thickness",0);
        cc.AGAP   = ccdb.getDouble(table+"/cnd/AzimuthalGap",0);
        cc.LGAP   = ccdb.getDouble(table+"/cnd/LateralGap",0);
        
        ccdb.loadTable(table+"/layer");  
        
        System.out.println("Radius "+cc.RADIUS);
        for (int i=0; i<cnd_nstr[0]; i++) {
            cc.LB[i]  = 2*ccdb.getDouble(table+"/layer/LowerBase",i);
            cc.UB[i]  = 2*ccdb.getDouble(table+"/layer/HigherBase",i);
             cc.R[i]  = cc.RADIUS + cc.THICK/2 + cc.THICK*i;
            System.out.println("i,LB,UB,R = "+i+" "+cc.LB[i]+" "+cc.UB[i]+" "+cc.R[i]);
        }
        
        cc.setGeometry();

	}
		       
    public void pixrot() {
        
        System.out.println("CNDPixels.pixrot(): "+this.detName);
		
        double[] theta={270.0,330.0,30.0,90.0,150.0,210.0};

        for(int is=0; is<6; is++) {
            double thet=theta[is]*Math.PI/180.;
            for (int ipix=0; ipix<2*nstr; ipix++) {
                for (int k=0;k<4;k++){
                    cnd_xpix[k][ipix][is]= -(cnd_xpix[k][ipix][6]*Math.cos(thet)+cnd_ypix[k][ipix][6]*Math.sin(thet));
                    cnd_ypix[k][ipix][is]=  -cnd_xpix[k][ipix][6]*Math.sin(thet)+cnd_ypix[k][ipix][6]*Math.cos(thet);
                }
            }
        }	    	
    }
    
    public void initHistograms(String hipoFile) {
        
        System.out.println("CNDPixels.initHistograms(): "+this.detName);  
        
        String iid;
        double amax[]= {20000.,200.,200.,200.};
        
        DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_a_Sevd = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Sevd = new DetectorCollection<H2F>();
        
        double nend = nstr+1;  
        
        for (int is=1; is<25 ; is++) {
            int ill=0; iid="s"+Integer.toString(is)+"_l"+Integer.toString(ill)+"_c";
            H2_a_Hist.add(is, 0, 0, new H2F("a_gmean_"+iid+0, 100,   0., amax[id],nstr, 1., nend));
            H2_t_Hist.add(is, 0, 0, new H2F("a_tdif_"+iid+0,  100, -35.,      35.,nstr, 1., nend));
            for (int lr=1 ; lr<3 ; lr++){
                iid="s"+Integer.toString(is)+"_l"+Integer.toString(lr)+"_c";
                H2_a_Hist.add(is, lr, 0, new H2F("a_raw_"+iid+1,      100,   0.,amax[id],nstr, 1., nend));
                H2_t_Hist.add(is, lr, 0, new H2F("a_raw_"+iid+1,      100, 450.,  850.,nstr, 1., nend));
                H2_a_Hist.add(is, lr, 1, new H2F("a_raw_"+iid+1,      100,   0.,amax[id],100, 450.,850.));
                H2_a_Hist.add(is, lr, 3, new H2F("a_ped_"+iid+3,       40, -20.,  20., nstr, 1., nend)); 
                H2_a_Hist.add(is, lr, 5, new H2F("a_fadc_"+iid+5,     100,   0., 100., nstr, 1., nend));
                H1_a_Sevd.add(is, lr, 0, new H1F("a_sed_"+iid+0,                       nstr, 1., nend));
                H2_a_Sevd.add(is, lr, 0, new H2F("a_sed_fadc_"+iid+0, 100,   0., 100., nstr, 1., nend));
                H2_a_Sevd.add(is, lr, 1, new H2F("a_sed_fadc_"+iid+1, 100,   0., 100., nstr, 1., nend));
                H2_t_Sevd.add(is, lr, 0, new H2F("a_sed_fadc_"+iid+0, 200,   0., 100., nstr, 1., nend));
            }
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
