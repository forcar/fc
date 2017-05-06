package org.clas.fcmon.ftof;

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

public class FTOFPixels {
	
    public Strips          strips = new Strips();
    DatabaseConstantProvider ccdb = new DatabaseConstantProvider(1,"default");
    
    double ftof_xpix[][][] = new double[4][124][7];
    double ftof_ypix[][][] = new double[4][124][7];
    
    public    int     ftof_nstr[] = {23,62,5};
    
    int        nha[][] = new    int[6][2];
    int        nht[][] = new    int[6][2];
    int    strra[][][] = new    int[6][2][62]; 
    int    strrt[][][] = new    int[6][2][62]; 
    int     adcr[][][] = new    int[6][2][62];      
    float   tdcr[][][] = new  float[6][2][62]; 
    
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
    public IndexedList<double[]>                     Lmap_a_z = new IndexedList<double[]>(2);
    public IndexedList<double[]>                     Lmap_t_z = new IndexedList<double[]>(2);
    
    int id;
	public int nstr;
	public String detName = null;
	
    public FTOFPixels(String det) {
        if (det=="PANEL1A") id=0;
        if (det=="PANEL1B") id=1;
        if (det=="PANEL2")  id=2;
        nstr = ftof_nstr[id];
        detName = det;
        pixdef();
        pixrot();
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
        System.out.println("FTOFPixels.init():");
        Lmap_a.clear();
        Lmap_t.clear();
    }

    public void pixdef() {
        
        System.out.println("FTOFPixels.pixdef(): "+this.detName); 
        IndexedTable itab = null;
        double geom[] = new double[nstr];
        double zoff[] = {50.,50.,420.};
        String table=null;
            
        switch (id) { 
        case 0: table = "/geometry/ftof/panel1a"; break;
        case 1: table = "/geometry/ftof/panel1b"; break;
        case 2: table = "/geometry/ftof/panel2";   
        }
        
        ccdb.loadTable(table+"/paddles");        
        for (int i=0; i<nstr; i++) {
            geom[i] = ccdb.getDouble(table+"/paddles/Length",i);
        }
        
        ccdb.loadTable(table+"/panel");
        double y_inc = ccdb.getDouble(table+"/panel/paddlewidth",0);	
        
        double   k;
        double   x_inc=0;
		       
        for(int i=0 ; i<nstr ; i++){
            x_inc = 0.5*geom[i];
            ftof_xpix[0][nstr+i][6]=-x_inc;
            ftof_xpix[1][nstr+i][6]=0.;
            ftof_xpix[2][nstr+i][6]=0.;
            ftof_xpix[3][nstr+i][6]=-x_inc;
            k = -i*y_inc-zoff[id];	    	   
            ftof_ypix[0][nstr+i][6]=k;
            ftof_ypix[1][nstr+i][6]=k;
            ftof_ypix[2][nstr+i][6]=k-y_inc;
            ftof_ypix[3][nstr+i][6]=k-y_inc;
        }
        for(int i=0 ; i<nstr ; i++){
            x_inc = 0.5*geom[i];
            ftof_xpix[0][i][6]=0.;
            ftof_xpix[1][i][6]=x_inc;
            ftof_xpix[2][i][6]=x_inc;
            ftof_xpix[3][i][6]=0.;
            k = -i*y_inc-zoff[id];	    	   
            ftof_ypix[0][i][6]=k;
            ftof_ypix[1][i][6]=k;
            ftof_ypix[2][i][6]=k-y_inc;
            ftof_ypix[3][i][6]=k-y_inc;
        }
	}
		       
    public void pixrot() {
        
        System.out.println("FTOFPixels.pixrot(): "+this.detName);
		
        double[] theta={270.0,330.0,30.0,90.0,150.0,210.0};

        for(int is=0; is<6; is++) {
            double thet=theta[is]*3.14159/180.;
            for (int ipix=0; ipix<2*nstr; ipix++) {
                for (int k=0;k<4;k++){
                    ftof_xpix[k][ipix][is]= -(ftof_xpix[k][ipix][6]*Math.cos(thet)+ftof_ypix[k][ipix][6]*Math.sin(thet));
                    ftof_ypix[k][ipix][is]=  -ftof_xpix[k][ipix][6]*Math.sin(thet)+ftof_ypix[k][ipix][6]*Math.cos(thet);
                }
            }
        }	    	
    }
    
    public void initHistograms(String hipoFile) {
        
        System.out.println("FTOFPixels.initHistograms(): "+this.detName);  
        
        String iid;
        double amax[]= {4000.,6000.,4000.};
        
        DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_a_Sevd = new DetectorCollection<H2F>();
        
        double nend = nstr+1;  
        
        for (int is=1; is<7 ; is++) {
            int ill=0; iid="s"+Integer.toString(is)+"_l"+Integer.toString(ill)+"_c";
            H2_a_Hist.add(is, 0, 0, new H2F("a_gmean_"+iid+0, 100,   0., amax[id],nstr, 1., nend));
            H2_t_Hist.add(is, 0, 0, new H2F("a_tdif_"+iid+0,  100, -35.,      35.,nstr, 1., nend));
            for (int il=1 ; il<3 ; il++){
                iid="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100,   0., 4000.,nstr, 1., nend));
                H2_t_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100, 450.,  850.,nstr, 1., nend));
                H2_a_Hist.add(is, il, 3, new H2F("a_ped_"+iid+3,       40, -20.,  20., nstr, 1., nend)); 
                H2_a_Hist.add(is, il, 5, new H2F("a_fadc_"+iid+5,     100,   0., 100., nstr, 1., nend));
                H1_a_Sevd.add(is, il, 0, new H1F("a_sed_"+iid+0,                       nstr, 1., nend));
                H2_a_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+iid+0, 100,   0., 100., nstr, 1., nend));
                H2_a_Sevd.add(is, il, 1, new H2F("a_sed_fadc_"+iid+1, 100,   0., 100., nstr, 1., nend));
            }
        }       

        if(hipoFile!=" "){
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
    } 
    
}
