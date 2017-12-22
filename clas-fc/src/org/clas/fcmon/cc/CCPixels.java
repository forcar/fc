package org.clas.fcmon.cc;

import java.util.TreeMap;

import org.clas.fcmon.tools.FCCalibrationData;
import org.clas.fcmon.tools.Strips;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.utils.groups.IndexedList;

public class CCPixels {
	
    public Strips        strips = new Strips();
    
    public double cc_xpix[][][] = new double[4][36][7];
    public double cc_ypix[][][] = new double[4][36][7];
    public    int        nstr[] = {18};
    
    int        nha[][] = new    int[6][2];
    int        nht[][] = new    int[6][2];
    int    strra[][][] = new    int[6][2][18]; 
    int    strrt[][][] = new    int[6][2][18]; 
    int     adcr[][][] = new    int[6][2][18];      
    double  tdcr[][][] = new double[6][2][18];  
    float     tf[][][] = new  float[6][2][18]; 
    float     ph[][][] = new  float[6][2][18];  
    
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
    public IndexedList<double[]>                     Lmap_a_z = new IndexedList<double[]>(2);
    public IndexedList<double[]>                     Lmap_t_z = new IndexedList<double[]>(2);	

    public double amax=4000;
    
    int id;
  
    public String detName = null;
    
    public CCPixels(String det) {       
        detName = det;
        this.pixdef();
        this.pixrot();
    }
    
    public void init() {
        System.out.println("CCPixels.init():");
        Lmap_a.clear();
        Lmap_t.clear();
    }	
    
    public void getLmapMinMax(int is1, int is2){
        TreeMap<Integer,Object> map = null;
        double mina,maxa,mint,maxt,avg,aavg=0,tavg=0;
        double[] a = {1000,0,0};
        double[] t = {1000,0,0};
        for (int is=is1 ; is<is2; is++) {
        	for (int il=1 ; il<3; il++) {
            map = Lmap_a.get(is, il, 0);
            mina = (double) map.get(2); maxa = (double) map.get(3); avg = (double) map.get(4);
            if (mina<a[0]) a[0]=mina; if (maxa>a[1]) a[1]=maxa; aavg+=avg;
//            map = Lmap_t.get(is, il, 0);
//           mint = (double) map.get(2); maxt = (double) map.get(3); avg = (double) map.get(4);
 //           if (mint<t[0]) t[0]=mint; if (maxt>t[1]) t[1]=maxt; tavg+=avg;
        }
        }

        a[2]=Math.min(500000,aavg/(is2-is1));
//        t[2]=Math.min(500000,tavg/(is2-is1));
        
        Lmap_a_z.add(a,1,0);
//        Lmap_t_z.add(t,0,0);        
    }	
    	
    
    public void pixdef() {
        
        System.out.println("CCPixels.pixdef():");
		  
        double   k;
        double   y_inc=19.0;
        double   x_inc=0;

        double[] ccgeom={ 
        65.018,
        77.891,
        90.532,
        102.924,
        115.056,
        126.914,
        138.487,
        149.764,
        160.734,
        171.388,
        183.967,
        196.047,
        209.663,
        222.546,
        234.684,
        246.064,
        256.680,
        266.527
        };
		       
        for(int i=0 ; i<18 ; i++){
            x_inc = 0.7*ccgeom[i];
            cc_xpix[0][18+i][6]=-x_inc;
            cc_xpix[1][18+i][6]=0.;
            cc_xpix[2][18+i][6]=0.;
            cc_xpix[3][18+i][6]=-x_inc;
            k = -i*y_inc-100.;	    	   
            cc_ypix[0][18+i][6]=k;
            cc_ypix[1][18+i][6]=k;
            cc_ypix[2][18+i][6]=k-y_inc;
            cc_ypix[3][18+i][6]=k-y_inc;
        }
        for(int i=0 ; i<18 ; i++){
            x_inc = 0.7*ccgeom[i];
            cc_xpix[0][i][6]=0.;
            cc_xpix[1][i][6]=x_inc;
            cc_xpix[2][i][6]=x_inc;
            cc_xpix[3][i][6]=0.;
            k = -i*y_inc-100.;	    	   
            cc_ypix[0][i][6]=k;
            cc_ypix[1][i][6]=k;
            cc_ypix[2][i][6]=k-y_inc;
            cc_ypix[3][i][6]=k-y_inc;
        }
	}
		       
    public void pixrot() {
        
        System.out.println("CCPixels.pixrot():");
		
        double[] theta={270.0,330.0,30.0,90.0,150.0,210.0};
        	               
        for(int is=0; is<6; is++) {
            double thet=theta[is]*Math.PI/180.;
            for (int ipix=0; ipix<2*nstr[0]; ipix++) {
                for (int k=0;k<4;k++){
                    cc_xpix[k][ipix][is]= -(cc_xpix[k][ipix][6]*Math.cos(thet)+cc_ypix[k][ipix][6]*Math.sin(thet));
                    cc_ypix[k][ipix][is]=  -cc_xpix[k][ipix][6]*Math.sin(thet)+cc_ypix[k][ipix][6]*Math.cos(thet);
                }
            }
        }	    	
    }
    
    public void initHistograms(String hipoFile) {
        
        System.out.println("CCPixels.initHistograms()"); 
        
        String iid;
        
        DetectorCollection<H1F> H1_a_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Sevd = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_a_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_t_Hist = new DetectorCollection<H2F>();
        DetectorCollection<H2F> H2_a_Sevd = new DetectorCollection<H2F>();
        
        double nend = nstr[0]+1;  
        
        for (int is=1; is<7 ; is++) {
            for (int il=1 ; il<3 ; il++){
                iid="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100,   0., amax,nstr[0], 1., nend));
                H2_t_Hist.add(is, il, 0, new H2F("a_raw_"+iid+0,      100,1330., 1370.,nstr[0], 1., nend));
                H2_a_Hist.add(is, il, 1, new H2F("a_raw_"+iid+1,      100,   0., amax,100, 300.,1200.));
                H2_a_Hist.add(is, il, 3, new H2F("a_ped_"+iid+3,       40, -20.,  20., nstr[0], 1., nend)); 
                H2_a_Hist.add(is, il, 5, new H2F("a_fadc_"+iid+5,     100,   0., 100., nstr[0], 1., nend));
                H1_a_Sevd.add(is, il, 0, new H1F("a_sed_"+iid+0,                       nstr[0], 1., nend));
                H2_a_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+iid+0, 100,   0., 100., nstr[0], 1., nend));
                H2_a_Sevd.add(is, il, 1, new H2F("a_sed_fadc_"+iid+1, 100,   0., 100., nstr[0], 1., nend));
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
    } 
    
}
