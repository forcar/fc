package org.clas.fcmon.ec;


import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;

import org.clas.fcmon.tools.CalDrawDB;
import org.clas.fcmon.tools.FCCalibrationData;
import org.clas.fcmon.tools.Pixel;
import org.clas.fcmon.tools.Pixels;
import org.clas.fcmon.tools.Strips;
import org.clas.service.ec.ECCluster;
import org.clas.fcmon.tools.ECpixelDepth;
import org.clas.fcmon.detector.view.DetectorShape2D;

import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorLayer;

import org.jlab.utils.groups.IndexedList;

import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECLayer;

import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;

import math.geom2d.polygon.SimplePolygon2D;

public class ECPixels {
	
    CalDrawDB         calDB  = null;
    ECLayer          ecLayer = null;
    ECDetector      detector = null;
    PrintWriter       writer = null;
    
    public TreeMap<Integer,List<double[]>>          clusterXY = new TreeMap<Integer, List<double[]>>();
    public TreeMap<Integer,List<double[]>>             peakXY = new TreeMap<Integer, List<double[]>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
    public IndexedList<double[]>                     Lmap_a_z = new IndexedList<double[]>(2);
    public IndexedList<double[]>                     Lmap_t_z = new IndexedList<double[]>(2);
    
    public Pixels     pixels = new Pixels();
    public Strips     strips = new Strips();
    public Pixel       pixel = null;

    public double ec_xpix[][][]   = new double[10][6916][7];
    public double ec_ypix[][][]   = new double[10][6916][7];
    public double ec_zpix[][][]   = new double[10][6916][7];
    public double ec_xstr[][][][] = new double[8][68][3][7];
    public double ec_ystr[][][][] = new double[8][68][3][7];
    public  float ec_cmap[]       = new float[6916];
    public  float ec_zmap[]       = new float[6916];
    public    int ec_nvrt[]       = new int[6916];
    public    int ec_nstr[]       = {36,36,36};
    public double ec_dist[][][]   = new double[36][36][36];
	
    double      uvwa[] = new double[6];
    double      uvwt[] = new double[6];
    int         mpix[] = new    int[6];
    int         esum[] = new    int[6];
    int ecadcpix[][][] = new    int[6][3][6916];
    int ectdcpix[][][] = new    int[6][3][6916];
    int   ecsumpix[][] = new    int[6][6916];
    int    ecpixel[][] = new    int[6][6916]; 
    
    int        nha[][] = new    int[6][3];
    int        nht[][] = new    int[6][3];
    int    strra[][][] = new    int[6][3][68]; 
    int    strrt[][][] = new    int[6][3][68]; 
    float   adcr[][][] = new  float[6][3][68];      
    double  tdcr[][][] = new double[6][3][68]; 	
    float     tf[][][] = new  float[6][3][68]; 
    float     ph[][][] = new  float[6][3][68]; 
    
    int[][] sthrMuon = {{5,5,5},{5,5,5},{5,5,5}}; //15,20,20
    int[][] sthrPhot = {{10,10,10},{9,9,9},{8,8,8}};
    int[][] sthrElec = {{10,10,10},{10,10,10},{10,10,10}};
    int[][] sthrTest = {{10,10,10},{9,9,9},{13,13,13}};
    int[][] sthrZero = {{1,1,1},{1,1,1},{1,1,1}};
    
    int[][] pthrMuon = {{15,15,15},{20,20,20},{20,20,20}};
    int[][] pthrPhot = {{18,18,18},{20,20,20},{15,15,15}};
    int[][] pthrElec = {{30,30,30},{30,30,30},{30,30,30}};
    int[][] pthrTest = {{18,18,18},{20,20,20},{15,15,15}};
    int[][] pthrZero = {{1,1,1},{1,1,1},{1,1,1}};
    
    double[] cerrMuon = {5.5,10.,10.};
    double[] cerrPhot = {7,15,20}; 
    double[] cerrElec = {10.,10.,10.};
    double[] cerrTest = {4.5,11.,13.};
//    double[] cerrElec = {100.,100.,100.};
    public double        amax[] = {250.,250.,99};
    public double        tmax[] = {300.,300.,300.};    
    public int idet=0;
    public String detName = null;
    public int simSector = 2;
	
    public ECPixels(String det, ECDetector ecdet) {		
        System.out.println(" "); System.out.println("ECPixels("+det+")");
        this.detName  = det;
        this.detector = ecdet;
        if (det.equals("PCAL"))   idet=0;
        if (det.equals("ECin"))   idet=1;
        if (det.equals("ECout"))  idet=2;
        for (int suplay=idet ; suplay<idet+1; suplay++) {
            int off = suplay==0 ? DetectorLayer.PCAL_Z : (suplay==1 ? DetectorLayer.EC_INNER_Z : DetectorLayer.EC_OUTER_Z);
            for (int layer=0; layer<3; layer++) {
                ecLayer = detector.getSector(0).getSuperlayer(suplay).getLayer(layer+off);
                ec_nstr[layer] = ecLayer.getAllComponents().size();
                strips.nstr[layer] = ec_nstr[layer];
            }
        }
        for (int is=1; is<7; is++) {
            clusterXY.put(is, new ArrayList<double[]>());
               peakXY.put(is, new ArrayList<double[]>());
        }
        if (!det.equals("PCAL")) getECPixelDepth(ecdet);
        pixdef();
        pixrot();
        System.out.println("ECPixels("+det+") is done");
//      pixHistos();
//    this.writeFPGALookupTable("/Users/colesmith/pcal_att376_DB.dat",376.,1); 
//    this.testStrips();
//    this.testPixels();
        
    }
    
    /**
     * For all displayed sectors find absolute min,max and average of adc,tdc
     * @param is1 minimum sector 
     * @param is2 maximum sector
     * @param il  detector layer
     * @param opt
     */
    
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
        System.out.println("ECPixels.init()");
        Lmap_a.clear();
        Lmap_t.clear();
        Lmap_a_z.clear();
        Lmap_t_z.clear();
    }
    
    public int getStripThr(String config, int layer) {
        switch (config) {
        case     "pi0": return sthrPhot[idet][layer-1] ;  
        case    "phot": return sthrPhot[idet][layer-1] ; 
        case    "muon": return sthrMuon[idet][layer-1] ;  
        case    "elec": return sthrElec[idet][layer-1] ;
        case    "test": return sthrTest[idet][layer-1] ;
        case    "none": return sthrZero[idet][layer-1] ;
        }
        return 0;
     }
    
    public int getPeakThr(String config, int layer) {
        switch (config) {
        case     "pi0": return pthrPhot[idet][layer-1] ;  
        case    "phot": return pthrPhot[idet][layer-1] ;  
        case    "muon": return pthrMuon[idet][layer-1] ; 
        case    "elec": return pthrElec[idet][layer-1] ;
        case    "test": return pthrTest[idet][layer-1] ;
        case    "none": return pthrZero[idet][layer-1] ;
        }
        return 0;
     }
    
    public float getClusterErr(String config) {
        switch (config) {
        case     "pi0": return (float) cerrPhot[idet] ;  
        case    "phot": return (float) cerrPhot[idet] ;  
        case    "muon": return (float) cerrMuon[idet] ; 
        case    "elec": return (float) cerrElec[idet] ;
        case    "test": return (float) cerrTest[idet] ;
        case    "none": return (float) cerrMuon[idet] ;
        }
        return 0;
     }
    
    public void pixdef() {
        System.out.println("ECPixels.pixdef()"); 
        calDB = new CalDrawDB(detName,detector);
        GetStripsDB();
        GetPixelsDB();    
    }
    
    public void GetStripsDB() {
        
        System.out.println("ECPixels:GetStripsDB()");	
        DetectorShape2D shape = new DetectorShape2D();
		
        for(int sector = 0; sector < 1; sector++) {
            System.out.println("pcGetStripsDB: Processing Sector "+sector);
            for(int layer=0; layer<3 ; layer++) {
                for(int strip = 0; strip < ec_nstr[layer] ; strip++) {
                    shape = calDB.getStripShape(sector, layer, strip);	            
                    for(int i = 0; i < shape.getShapePath().size(); ++i) {
                        ec_xstr[i][strip][layer][6] = shape.getShapePath().point(i).x();
                        ec_ystr[i][strip][layer][6] = shape.getShapePath().point(i).y();    
                    }    
                }    
            }    
        }						    
    }
	
    public void GetPixelsDB() {
		
        System.out.println("ECPixels:GetPixelsDB()");		
        DetectorShape2D shape = new DetectorShape2D();

        for(int sector=0; sector<1 ; sector++) {
            int pix = 0; 
            for(int uStrip = 0; uStrip < ec_nstr[0]; uStrip++) {	 
                for(int vStrip = 0; vStrip < ec_nstr[1]; vStrip++) {
                    for(int wStrip = 0; wStrip < ec_nstr[2]; wStrip++) {
                        shape = calDB.getPixelShape(0, uStrip, vStrip, wStrip);
                        if(shape!=null) {
                            pix++;
                            double [] xtemp2 = new double [shape.getShapePath().size()];
                            double [] ytemp2 = new double [shape.getShapePath().size()];
                            double [] ztemp2 = new double [shape.getShapePath().size()];
                            for(int i = 0; i < shape.getShapePath().size(); ++i) {
                                xtemp2[i] = shape.getShapePath().point(i).x();
                                ytemp2[i] = shape.getShapePath().point(i).y();
                                ztemp2[i] = shape.getShapePath().point(i).z();
                                ec_xpix[i][pix-1][6] = xtemp2[i];
                                ec_ypix[i][pix-1][6] = ytemp2[i];    
                                ec_zpix[i][pix-1][6] = ztemp2[i];    
                            }
                            SimplePolygon2D pol1 = new SimplePolygon2D(xtemp2,ytemp2);
                            double uDist = calDB.getUPixelDistance(uStrip, vStrip, wStrip);
                            double vDist = calDB.getVPixelDistance(uStrip, vStrip, wStrip);
                            double wDist = calDB.getWPixelDistance(uStrip, vStrip, wStrip);
                            shape.setColor(130,(int)(255*vStrip/ec_nstr[1]),(int)(255*wStrip/ec_nstr[2]));
                            ec_zmap[pix-1] = 1;
                            if (idet!=0) ec_zmap[pix-1]=(float)ec_dist[uStrip][vStrip][wStrip];
                            ec_nvrt[pix-1] = shape.getShapePath().size();
                            pixel = new Pixel();
                            pixel.setIndex(pix);
                            pixel.setShape(shape);
                            pixel.setArea(pol1.area());
                            pixel.setReadout(uStrip+1, vStrip+1, wStrip+1);
                            pixel.setReadoutDist(uDist,vDist,wDist);    
                            pixel.setStatus(calDB.isEdgePixel(uStrip,vStrip,wStrip));
                            pixels.addPixel(pixel,pix,uStrip+1,vStrip+1,wStrip+1);
                            strips.addPixel(sector, 1, uStrip+1, pix);
                            strips.addPixel(sector, 2, vStrip+1, pix);
                            strips.addPixel(sector, 3, wStrip+1, pix);
                            strips.addPixDist(sector, 1, uStrip+1, (int) (uDist*100));
                            strips.addPixDist(sector, 2, vStrip+1, (int) (vDist*100));
                            strips.addPixDist(sector, 3, wStrip+1, (int) (wDist*100));    
                        }    
                    }    
                }    
            }            
            // Sort pixels in each strip according to distance from readout edge
            for (int lay=0; lay<3 ; lay++ ) {
                System.out.println("ECPixels: Sorting pixels in layer "+lay);
                for(int strip = 0; strip < ec_nstr[lay]; strip++) {
                    strips.getSortedPixels(0, lay+1, strip+1);    
                }    
            }   	    
        }    
    }
	
    public void pixHistos() {
        
        JFrame          frame = new JFrame("pixHistos");
        EmbeddedCanvas canvas = new EmbeddedCanvas();

        frame.setSize(800,500);
        canvas.divide(2, 2);
        
        H1F h[] = new H1F[4];
        
        for (int i=0; i<4 ; i++) h[i] = new H1F("Pix Area Zone "+i, 50,0.,1.1);
	    
        for (int ipix=0; ipix<pixels.getNumPixels(); ipix++) {
            h[pixels.getZone(ipix+1)].fill(pixels.getZoneNormalizedArea(ipix+1));
            int str[] = pixels.getStrips(ipix+1);
            System.out.println("ipix= "+(ipix+1)+" zone= "+pixels.getZone(ipix+1));
            System.out.println("U="+str[0]+" V="+str[1]+" W="+str[2]);
            System.out.println("Normalized Area= "+pixels.getZoneNormalizedArea(ipix+1) );
            System.out.println("Pixel number= "+pixels.getPixelNumber(str[0],str[1],str[2]));
            System.out.println(" ");
        }
        for (int ic=0; ic<4; ic++) {h[ic].setOptStat(Integer.parseInt("1"));
            h[ic].setName(String.format("Max Area: %1$.1f cm^2",pixels.maxZonePixelArea[ic]));
            canvas.cd(ic); canvas.draw(h[ic]);
        }
        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
    }
	
    public void initHistograms(String hipoFile) {
        
        System.out.println("ECPixels:initHistograms()");
        
        DetectorCollection<H2F> H2_a_Hist     = new DetectorCollection<H2F>();
        DetectorCollection<H1F> H1_a_Hist     = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_t_Hist     = new DetectorCollection<H2F>();
        DetectorCollection<H1F> H1_a_Maps     = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Maps     = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_PC_Stat    = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_PCa_Stat   = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_PCt_Stat   = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Peds_Hist  = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Tdif_Hist  = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Mode1_Hist = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Mode1_Sevd = new DetectorCollection<H2F>();  
        DetectorCollection<H1F> H1_Stra_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Strt_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Pixa_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Pixt_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Clus_Mult  = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_Clus_Mult  = new DetectorCollection<H2F>();
        
        String id;
        int nstr = ec_nstr[0]            ; double nend = nstr+1;  
        int npix = pixels.getNumPixels() ; double pend = npix+1;
        
	    GStyle.getH1FAttributes().setLineWidth(1);
	    GStyle.getH2FAttributes().setLineWidth(1);
        
        for (int is=1; is<7 ; is++) {    
            
            // Strip based data  
            for (int il=1 ; il<4 ; il++){  
                
                // Occupancy  
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 0, new H2F("a_raw_"+id+0, 125,   0., amax[0], nstr, 1., nend));
                H2_a_Hist.add(is, il, 1, new H2F("b_pix_"+id+1, 125,   0., amax[0], nstr, 1., nend));
                H2_a_Hist.add(is, il, 3, new H2F("b_raw_"+id+3, 100,   0., amax[2], nstr, 1., nend));
                H2_a_Hist.add(is, il, 4, new H2F("a_raw_"+id+4, 100,   0., amax[1],  100, 0.,tmax[1]));
                H2_a_Hist.add(is, il, 5, new H2F("a_raw_"+id+5, 100,   0., amax[2],  100, 0.,tmax[1]));
                H2_t_Hist.add(is, il, 0, new H2F("a_raw_"+id+0, 100,   0., tmax[0], nstr, 1., nend));
                H2_t_Hist.add(is, il, 1, new H2F("b_pix_"+id+1, 100,   0., tmax[0], nstr, 1., nend));
                
                // Pedestal Noise  
//                H2_Peds_Hist.add(is, il, 0, new H2F("a_ped_"+id+0,  20, -10.,  10., nstr, 1., nend)); 
                H2_Peds_Hist.add(is, il, 0, new H2F("a_ped_"+id+0,  50, -100.,  100., nstr, 1., nend)); 
                H2_Peds_Hist.add(is, il, 1, new H2F("b_ped_"+id+1,  50,   0.,  10., nstr, 1., nend)); 
                
                // FADC - TDC  
                H2_Tdif_Hist.add(is, il, 0, new H2F("a_tdif_"+id+0, 120, -30.,  30., nstr, 1., nend)); 
                
                // Mode1  
                H2_Mode1_Hist.add(is, il, 0, new H2F("a_fadc_"+id+0, 100,   1., 101.,  nstr, 1., nend));
                H2_Mode1_Hist.add(is, il, 1, new H2F("a_fadc_"+id+1, 100,   1., 101.,  nstr, 1., nend));
                
                // Single Event Strip Occupancy
                H1_Stra_Sevd.add(is, il, 0, new H1F("a_sed_stra_"+id+0, nstr,  1., nend));
                H1_Stra_Sevd.add(is, il, 1, new H1F("b_sed_stra_"+id+1, nstr,  1., nend));
                H1_Stra_Sevd.add(is, il, 2, new H1F("c_sed_stra_"+id+2, nstr,  1., nend));
                H1_Strt_Sevd.add(is, il, 0, new H1F("a_sed_strt_"+id+0, nstr,  1., nend));
                H1_Strt_Sevd.add(is, il, 1, new H1F("b_sed_strt_"+id+1, nstr,  1., nend));
                
                // Single Event fADC bins
                H2_Mode1_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+id+0, 100, 0., 100., nstr, 1., nend));
                H2_Mode1_Sevd.add(is, il, 1, new H2F("b_sed_fadc_"+id+1, 100, 0., 100., nstr, 1., nend));
                H2_Mode1_Sevd.add(is, il, 2, new H2F("c_sed_fadc_"+id+2, 200, 0., 100., nstr, 1., nend));
            }
            
                id="s"+Integer.toString(is)+"_l"+Integer.toString(3)+"_c";
                H2_t_Hist.add(is, 3, 3, new H2F("a_raw_"+id+3,  41, -40.,  40., 6, 0., 6.)); // No phase correction
                H2_t_Hist.add(is, 3, 4, new H2F("a_raw_"+id+4, 100,   0., tmax[0], 6, 0., 6.)); // With phase correction
            
                // Reconstructed data
                id="s"+Integer.toString(is)+"_l"+Integer.toString(4)+"_c";
                H2_a_Hist.add(is, 4, 0, new H2F("a_reco_"+id+0, 300, 5.,3000., 9, 1., 10.));
                H2_a_Hist.add(is, 4, 1, new H2F("a_reco_"+id+1, 50, 0., 3.5, 40, 0.15, 0.35));
                H2_a_Hist.add(is, 4, 2, new H2F("a_reco_"+id+2, 50, 0., 1.0, 40, 0.15, 0.35));
                H2_a_Hist.add(is, 4, 3, new H2F("a_reco_"+id+3, 50, 0., 1.0, 40, 0.15, 0.35));
                H2_a_Hist.add(is, 4, 4, new H2F("a_reco_"+id+4, 50, -1.0, 1.0,40, 0., 20.));
                
                H1_a_Hist.add(is, 4, 0, new H1F("b_reco_"+id+0, 50, -300, 300.));
                H1_a_Hist.add(is, 4, 1, new H1F("b_reco_"+id+1, 50, -2.0, 2.0));
                H1_a_Hist.add(is, 4, 2, new H1F("b_reco_"+id+2, 50, -1.0, 1.0));
                
                id="s"+Integer.toString(is)+"_l"+Integer.toString(5)+"_c";
                H2_a_Hist.add(is, 5, 0, new H2F("b_raw_"+id+0,  600, 0.,300., 3, 1., 4.)); 
                
                id="s"+Integer.toString(is)+"_l"+Integer.toString(6)+"_c";
                H2_a_Hist.add(is, 6, 0, new H2F("c_reco_"+id+0, 600, 0.,300., 3, 1., 4.));   
                H2_a_Hist.add(is, 6, 1, new H2F("c_reco_"+id+1, 50, 0., 3.5, 0, 0., 0.2));   
                H2_a_Hist.add(is, 6, 2, new H2F("c_reco_"+id+2, 50, 0., 3.5, 0, 0., 0.2));   
                H2_a_Hist.add(is, 6, 3, new H2F("c_reco_"+id+3, 50, 0., 3.5, 0, 0., 0.2));  
                
                id="s"+Integer.toString(is)+"_l"+Integer.toString(7)+"_c";
//              H2_a_Hist.add(is, 7, 0, new H2F("d_reco_"+id+0, 200, -600., 600., 200, -600., 600.));   
//              H2_a_Hist.add(is, 7, 1, new H2F("d_reco_"+id+1, 200, -600., 600., 200, -600., 600.));                   
                H2_a_Hist.add(is, 7, 2, new H2F("d_reco_"+id+2, 120, 0., 20., 120, 0., 12.0));   
                
                id="s"+Integer.toString(is)+"_l"+Integer.toString(8)+"_c";                
                H2_a_Hist.add(is, 8, 0, new H2F("f_reco_"+id+0, 50, -10., 10., 3, 1., 4.));                  
                H2_a_Hist.add(is, 8, 1, new H2F("f_reco_"+id+1, 50, -10., 10., 60, 5., 36.));                  
                H2_a_Hist.add(is, 8, 2, new H2F("f_reco_"+id+2, 50, -10., 10., 60, 5., 36.));                  
                H2_a_Hist.add(is, 8, 3, new H2F("f_reco_"+id+3, 50, -10., 10., 60, 5., 36.));                  
                H2_a_Hist.add(is, 8, 4, new H2F("f_reco_"+id+4, 50, -10., 10., 60, 5., 36.));   
                
                id="s"+Integer.toString(is)+"_l"+Integer.toString(9)+"_c";
                H2_a_Hist.add(is, 9, 0, new H2F("g_reco_"+id+0, 50,  -2.,  2., 3, 1., 4.));   //refTH-mcThet                
                H2_a_Hist.add(is, 9, 1, new H2F("g_reco_"+id+1, 50, -0.5, 0.5, 60, 5., 36.)); //PCAL: pcThet-refTH                
                H2_a_Hist.add(is, 9, 2, new H2F("g_reco_"+id+2, 50, -1., 1., 30, 5., 36.));   //ECin: pcThet-refTH                
                H2_a_Hist.add(is, 9, 3, new H2F("g_reco_"+id+3, 50, -1., 1., 30, 5., 36.));   //ECou: pcThet-refTH    
                
                if (is==simSector) {
                	H2_a_Hist.add(is, 9,10, new H2F("g_reco_"+id+4, 60,  0,10,  4, 1,  5));  //PCAL-EC matching distance 
                	H2_a_Hist.add(is, 9,11, new H2F("g_reco_"+id+5, 60,-10,10, 12, 1, 13));  //PCAL-EC matching x,y residuals
                	H2_a_Hist.add(is, 9,20, new H2F("g_reco_"+id+6, 50,  0,5.2,50, 0.5,1.5));  
                	H2_a_Hist.add(is, 9,21, new H2F("g_reco_"+id+7, 50,  0,5.2,50, 0.5,1.5)); 
                	H2_a_Hist.add(is, 9,22, new H2F("g_reco_"+id+8, 50,  0,5.2,50,-1,  1));  
                	H2_a_Hist.add(is, 9,23, new H2F("g_reco_"+id+9, 50,  0,5.2,50,-1,  1));  
                	H2_a_Hist.add(is, 9,30, new H2F("g_reco_"+id+10,50,  0,5.2,50, 0.5,1.5));  
                	H2_a_Hist.add(is, 9,31, new H2F("g_reco_"+id+11,50,  0,5.2,50, 0.5,1.5)); 
                	H2_a_Hist.add(is, 9,32, new H2F("g_reco_"+id+12,50,  0,5.2,50,-1,  1));  
                	H2_a_Hist.add(is, 9,33, new H2F("g_reco_"+id+13,50,  0,5.2,50,-1,  1));  
                	H2_a_Hist.add(is, 9,40, new H2F("g_reco_"+id+14,50,  5,30, 50, 0.5,1.5));  
                	H2_a_Hist.add(is, 9,41, new H2F("g_reco_"+id+15,50,  2,10, 50, 0.5,1.5)); 
                	H2_a_Hist.add(is, 9,42, new H2F("g_reco_"+id+16,50,  5,30, 50,-1,  1));  
                	H2_a_Hist.add(is, 9,43, new H2F("g_reco_"+id+17,50,  2,10, 50,-1,  1));  
                }

                if (is==simSector) {
                  H2_a_Hist.add(is, 9, 5, new H2F("g_reco_"+id+5, 60, -410., 410., 60, -410., 410.));    //Pizero mass error vs X,Y
                  H2_a_Hist.add(is, 9, 6, new H2F("g_reco_"+id+6, 60, -410., 410., 60, -410., 410.));    //Pizero mass error vs X,Y
                  H2_a_Hist.add(is, 9, 7, new H2F("g_reco_"+id+7, 60, -410., 410., 60, -410., 410.));    //Pizero mass error vs X,Y
                  H2_a_Hist.add(is, 9, 8, new H2F("g_reco_"+id+8, 60, -410., 410., 60, -410., 410.));    //Pizero mass error vs X,Y
                }
                id="s"+Integer.toString(is)+"_l"+Integer.toString(10)+"_c";
                H2_a_Hist.add(is, 10, 1, new H2F("h_reco_"+id+1, 30, 50., 250., nstr, 1., nend));  //Pizero mass vs U Strips for Photon 1
                H2_a_Hist.add(is, 10, 2, new H2F("h_reco_"+id+2, 30, 50., 250., nstr, 1., nend));  //Pizero mass vs V Strips for Photon 1 
                H2_a_Hist.add(is, 10, 3, new H2F("h_reco_"+id+3, 30, 50., 250., nstr, 1., nend));  //Pizero mass vs W Strips for Photon 1
                H2_a_Hist.add(is, 10, 4, new H2F("h_reco_"+id+4, 30, 50., 250., nstr, 1., nend));  //Pizero mass vs U Strips for Photon 2  
                H2_a_Hist.add(is, 10, 5, new H2F("h_reco_"+id+5, 30, 50., 250., nstr, 1., nend));  //Pizero mass vs V Strips for Photon 2   
                H2_a_Hist.add(is, 10, 6, new H2F("h_reco_"+id+6, 30, 50., 250., nstr, 1., nend));  //Pizero mass vs W Strips for Photon 2 
                id="s"+Integer.toString(is)+"_l"+Integer.toString(11)+"_c";
                H1_a_Hist.add(is, 11, 1, new H1F("i_reco_"+id+1, 200, 5.,1100.));
                H1_a_Hist.add(is, 11, 2, new H1F("i_reco_"+id+2, 200, 5.,1100.));
                H1_a_Hist.add(is, 11, 3, new H1F("i_reco_"+id+3, 200, 5.,1100.));
                H1_a_Hist.add(is, 11, 4, new H1F("i_reco_"+id+4, 200, 5.,1100.));
                H1_a_Hist.add(is, 11, 5, new H1F("i_reco_"+id+5, 200, 5.,1100.));
                H1_a_Hist.add(is, 11, 6, new H1F("i_reco_"+id+6, 200, 5.,1100.));
                
            //Pixel based data
            for (int il=1; il<4 ; il++) { 
                
                // Occupancy 
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 2, new H2F("c_pix_"+id+2,  25, 0., amax[0], npix, 1., pend));
                H2_t_Hist.add(is, il, 2, new H2F("c_pix_"+id+2,  40, 0., tmax[0], npix, 1., pend));
                
                // Layer Maps
                H1_a_Maps.add(is, il, 0, new H1F("a_apix_"   +id+0, npix, 1., pend)); //adc weighted pixel
                H1_a_Maps.add(is, il, 1, new H1F("b_napix_"  +id+1, npix, 1., pend)); //normalized adc weighted pixel
                H1_a_Maps.add(is, il, 2, new H1F("c_apix2_"  +id+2, npix, 1., pend)); //adc^2 weighted pixel
                H1_a_Maps.add(is, il, 3, new H1F("d_napix2_" +id+3, npix, 1., pend)); //normalized adc^2 weighted pixel
                H1_a_Maps.add(is, il, 4, new H1F("e_aepix_"  +id+4, npix, 1., pend)); //event weighted adc pixel
                H1_t_Maps.add(is, il, 0, new H1F("a_tpix_"   +id+0, npix, 1., pend)); //tdc weighted pixel
                H1_t_Maps.add(is, il, 1, new H1F("b_ntpix_"  +id+1, npix, 1., pend)); //normalized tdc weighted pixel
                H1_t_Maps.add(is, il, 2, new H1F("b_tpix2_"  +id+2, npix, 1., pend)); //tdc^2 weighted pixel                 
                H1_t_Maps.add(is, il, 4, new H1F("e_tepix_"  +id+4, npix, 1., pend)); //event weighted tdc pixel                 
            }
            for (int il=1 ; il<3 ; il++) {
                
                // Single Event Pixel Occupancy
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H1_Pixa_Sevd.add(is, il, 0, new H1F("a_pix_"+id+0, npix,  1., pend));
                H1_Pixt_Sevd.add(is, il, 0, new H1F("t_pix_"+id+0, npix,  1., pend));
            }           
            // Non-layer Pixel Maps
            id="s"+Integer.toString(is)+"_l"+Integer.toString(7)+"_c";
            H1_a_Maps.add(is, 7, 0, new H1F("a_aepix_"   +id+0, npix, 1., pend)); // event weighted pixel
            H1_a_Maps.add(is, 7, 1, new H1F("b_asumpix_" +id+1, npix, 1., pend)); // adc   U+V+W weighted pixel
            H1_a_Maps.add(is, 7, 2, new H1F("c_nsumpix_" +id+2, npix, 1., pend)); // adc   U+V+W weighted normalized pixel
            H1_a_Maps.add(is, 7, 3, new H1F("d_nepix_"   +id+3, npix, 1., pend)); // event weighted normalized pixel
            H1_t_Maps.add(is, 7, 0, new H1F("a_tepix_"   +id+0, npix, 1., pend));    
            H1_t_Maps.add(is, 7, 1, new H1F("b_tsumpix_" +id+1, npix, 1., pend));    
            H1_t_Maps.add(is, 7, 2, new H1F("c_nsumpix_" +id+2, npix, 1., pend));  
            H1_t_Maps.add(is, 7, 3, new H1F("d_nepix_"   +id+3, npix, 1., pend));    
                        
            id="s"+Integer.toString(is)+"_l"+Integer.toString(0)+"_c";
            H2_PCa_Stat.add(is, 0, 0, new H2F("a_evt_"+id+0, nstr, 1., nend,  3, 1., 4.));              
            H2_PCa_Stat.add(is, 0, 1, new H2F("b_adc_"+id+1, nstr, 1., nend,  3, 1., 4.));              
            H2_PCa_Stat.add(is, 0, 2, new H2F("c_ovf_"+id+2, nstr, 1., nend,  3, 1., 4.));              
            H2_PCa_Stat.add(is, 0, 5, new H2F("c_ovf_"+id+5, nstr, 1., nend,  3, 1., 4.));              
            H2_PCt_Stat.add(is, 0, 0, new H2F("a_evt_"+id+0, nstr, 1., nend,  3, 1., 4.));              
            H2_PCt_Stat.add(is, 0, 1, new H2F("b_tdc_"+id+1, nstr, 1., nend,  3, 1., 4.));              
            H2_PCa_Stat.add(is, 0, 3, new H2F("a_pix_"+id+3,   50,-1.,    1,  3, 1., 4.));                       
            H2_PCa_Stat.add(is, 0, 4, new H2F("b_pix_"+id+4,   50, 0.,  1.1,  4, 0., 4.));                                              
            H2_PCt_Stat.add(is, 0, 3, new H2F("a_pix_"+id+3,   50,-1.,    1,  3, 1., 4.));                       
            H2_PCt_Stat.add(is, 0, 4, new H2F("b_pix_"+id+4,   50, 0.,  1.1,  4, 0., 4.));                       
            H2_PC_Stat.add(is,  0, 3, new H2F("a_pix_"+id+3,   50,-1.,    1,  3, 1., 4.));                       
            H2_PC_Stat.add(is,  0, 4, new H2F("b_pix_"+id+4,   50, 0.,  1.1,  4, 0., 4.)); 
                
            H1_Clus_Mult.add(is, 0, 0, new H1F("a_clus_"+id+0, 11, 1, 12));                          
            H2_Clus_Mult.add(is, 0, 0, new H2F("b_clus_"+id+0, 11, 1, 12,  7, 0,  7));   
            H2_Clus_Mult.add(is, 0, 1, new H2F("c_clus_"+id+0, 50, 0,  5, 10, 0, 10));
                
            for (int il=1; il<4; il++) {
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_Clus_Mult.add(is, il, 0, new H2F("d_clus_"+id+0, 50, 0, 5, 3, 2, 5));
            }
               
        }
        
        if(!hipoFile.equals(" ")){
            FCCalibrationData calib = new FCCalibrationData();
            calib.getFile(hipoFile);
            H2_a_Hist     = calib.getCollection("H2_a_Hist");
            H1_a_Hist     = calib.getCollection("H1_a_Hist");
            H1_a_Maps     = calib.getCollection("H1_a_Maps");
            H2_t_Hist     = calib.getCollection("H2_t_Hist");
            H1_t_Maps     = calib.getCollection("H1_t_Maps");
            H2_PC_Stat    = calib.getCollection("H2_PC_Stat");
            H2_PCa_Stat   = calib.getCollection("H2_PCa_Stat");
            H2_PCt_Stat   = calib.getCollection("H2_PCt_Stat");
            H2_Peds_Hist  = calib.getCollection("H2_Peds_Hist");
            H2_Mode1_Hist = calib.getCollection("H2_Mode1_Hist");
        }   
        
        strips.addH1DMap("H1_a_Hist",    H1_a_Hist);
        strips.addH2DMap("H2_a_Hist",    H2_a_Hist);
        strips.addH2DMap("H2_t_Hist",    H2_t_Hist);
        pixels.addH1DMap("H1_a_Maps",    H1_a_Maps);
        pixels.addH1DMap("H1_t_Maps",    H1_t_Maps);
        strips.addH1DMap("H1_Pixa_Sevd", H1_Pixa_Sevd);
        strips.addH1DMap("H1_Pixt_Sevd", H1_Pixt_Sevd);
        strips.addH1DMap("H1_Stra_Sevd", H1_Stra_Sevd);
        strips.addH1DMap("H1_Strt_Sevd", H1_Strt_Sevd);
        strips.addH2DMap("H2_PC_Stat",   H2_PC_Stat);
        strips.addH2DMap("H2_PCa_Stat",  H2_PCa_Stat);
        strips.addH2DMap("H2_PCt_Stat",  H2_PCt_Stat);
        strips.addH2DMap("H2_Peds_Hist", H2_Peds_Hist);
        strips.addH2DMap("H2_Tdif_Hist", H2_Tdif_Hist);
        strips.addH2DMap("H2_Mode1_Hist",H2_Mode1_Hist);
        strips.addH2DMap("H2_Mode1_Sevd",H2_Mode1_Sevd);
        strips.addH1DMap("H1_Clus_Mult", H1_Clus_Mult);
        strips.addH2DMap("H2_Clus_Mult", H2_Clus_Mult);
   
    }	

    public void pixrot() {
        
        System.out.println("ECPixels.pixrot()");
    	double[] theta={0.0,60.0,120.0,180.0,240.0,300.0};
	    	for(int is=0; is<6; is++) {
	    	    double thet=theta[is]*Math.PI/180.;
	    	    double ct=Math.cos(thet) ; double st=Math.sin(thet);
	    	    // Rotate strips
	    	    for (int lay=0; lay<3 ; lay++) {
	    	        for (int istr=0; istr<ec_nstr[lay]; istr++) {
	    	            for (int k=0;k<4;k++){
	    	                ec_xstr[k][istr][lay][is]= -(ec_xstr[k][istr][lay][6]*ct+ec_ystr[k][istr][lay][6]*st);
	    	                ec_ystr[k][istr][lay][is]=  -ec_xstr[k][istr][lay][6]*st+ec_ystr[k][istr][lay][6]*ct; 
	    	            }    
	    	        }    
	    	    }
	    	    // Rotate pixels
	    	    for (int ipix=0; ipix<pixels.getNumPixels(); ipix++) {
	    	        ec_cmap[ipix] = 255*ipix/pixels.getNumPixels();
	    	        for (int k=0;k<ec_nvrt[ipix];k++) {
	    	            ec_xpix[k][ipix][is]= -(ec_xpix[k][ipix][6]*ct+ec_ypix[k][ipix][6]*st); 
                    ec_ypix[k][ipix][is]=  -ec_xpix[k][ipix][6]*st+ec_ypix[k][ipix][6]*ct;    
                    ec_zpix[k][ipix][is]=   ec_zpix[k][ipix][6];    
	    	        }    
	    	    }	    
	    	}	    	    	
    }
				
    public float uvw_dalitz(int ic, int il, int ip) {
        float uvw=0;
        switch (ic) {
        case 0: //PCAL
            if (il==1&&ip<=52) uvw=(float)ip/84;
            if (il==1&&ip>52)  uvw=(float)(52+(ip-52)*2)/84;
            if (il==2&&ip<=15) uvw=(float) 2*ip/77;
            if (il==2&&ip>15)  uvw=(float)(30+(ip-15))/77;
            if (il==3&&ip<=15) uvw=(float) 2*ip/77;
            if (il==3&&ip>15)  uvw=(float)(30+(ip-15))/77;
            break;
        case 1: //ECALinner
            uvw=(float)ip/36;
            break;    
        case 2: //ECALouter
            uvw=(float)ip/36;
            break;    
        }
        return uvw;    
    }

     /*    
     public void testPixels() {
         
         DetectorShapeTabView view = new DetectorShapeTabView();
         DetectorShapeView2D UWmap= new DetectorShapeView2D("PCAL Pixel");
         
         this.GetPixelsDB();
         this.pixrot();
         
         for (int sector=0; sector<6; sector++) {
             for (int ipix=0; ipix<pixels.getNumPixels(); ipix++) {
                 DetectorShape2D  pixx = new DetectorShape2D();
                 pixx.getShapePath().clear(); 
                 for (int k=0;k<ec_nvrt[ipix];k++) {
                     double xrot = ec_xpix[k][ipix][sector]; double yrot = ec_ypix[k][ipix][sector];
                     pixx.getShapePath().addPoint(xrot,yrot,0.0); 
                 }
                 int pixel=ipix+1;
                 int u=pixels.getStrip(1,pixel); int v=pixels.getStrip(2,pixel); int w=pixels.getStrip(3,pixel);
                 pixx.setColor(130,(int)(255*v/ec_nstr[1]),(int)(255*w/ec_nstr[2]));
                                     
                 UWmap.addShape(pixx);               
             }
         }
    
         view.addDetectorLayer(UWmap);
         JFrame hi = new JFrame();
         hi.setLayout(new BorderLayout());
         hi.add(view,BorderLayout.CENTER);
         hi.pack();
         hi.setVisible(true);
         
     }
 
     public void testStrips(int lay) {
         
         DetectorShapeTabView view = new DetectorShapeTabView();
         DetectorShapeView2D UWmap= new DetectorShapeView2D("PCAL Pixel");
         
         this.GetStripsDB();
         this.pixrot();
         
         for (int sector=0; sector<6; sector++) {
             for (int istr=0; istr<ec_nstr[lay]; istr++) {
                 DetectorShape2D  strp = new DetectorShape2D();
                 strp.getShapePath().clear(); 
                 for (int k=0;k<4;k++) {
                     double xrot = ec_xstr[k][istr][lay][sector]; double yrot = ec_ystr[k][istr][lay][sector];
                     strp.getShapePath().addPoint(xrot,yrot,0.0); 
                 }
                 strp.setColor(130,(int)(255*istr/ec_nstr[lay]),30);
                                     
                 UWmap.addShape(strp);               
             }
         }
    
         view.addDetectorLayer(UWmap);
         JFrame hi = new JFrame();
         hi.setLayout(new BorderLayout());
         hi.add(view,BorderLayout.CENTER);
         hi.pack();
         hi.setVisible(true);
         
     }
 */  
    public void getECPixelDepth (ECDetector ecdet){ 
        
        System.out.println("ECPixels.getECPixelDepth");
        
        ECpixelDepth test = new ECpixelDepth();
        test.FindBackEC(ecdet);
        test.FindFrontPCAL(ecdet);
        
        double x,y,z,totaldist;
        double del;
        double deltazin  = 1.238 * 15.0;
        double deltaztot = 1.238 * 39.0;
         
        //get list of centers for EC inner
        CalDrawDB pcaltestdist1 = new CalDrawDB("ECin",ecdet);
        DetectorShape2D shape1 = new DetectorShape2D();
        double[][][][] total1 = new double[36][36][36][3];
        
        del = deltaztot/deltazin;

        for(int sector = 0; sector < 1; sector++) {
            for(int uPaddle = 0; uPaddle < 36; uPaddle++) {
                for(int vPaddle = 0; vPaddle < 36; vPaddle++) {
                    for(int wPaddle = 0; wPaddle < 36; wPaddle++) {
                        if(pcaltestdist1.isValidPixel(sector, uPaddle, vPaddle, wPaddle)) {
                            shape1 = pcaltestdist1.getPixelShape(sector, uPaddle, vPaddle, wPaddle);
                            System.arraycopy( (double[])pcaltestdist1.getShapeCenter(shape1), 0, total1[uPaddle][vPaddle][wPaddle], 0, 3);
                        }
                    }
                }
    
            }
        }
        
        //get list of centers for ECouter
        CalDrawDB pcaltestdist2 = new CalDrawDB("ECout",ecdet);
        DetectorShape2D shape2 = new DetectorShape2D();
        double[][][][] total2 = new double[36][36][36][3];

        for(int sector = 0; sector < 1; sector++) {
            for(int uPaddle = 0; uPaddle < 36; uPaddle++) {
                for(int vPaddle = 0; vPaddle < 36; vPaddle++) {
                    for(int wPaddle = 0; wPaddle < 36; wPaddle++) {
                        if(pcaltestdist2.isValidPixel(sector, uPaddle, vPaddle, wPaddle)) {
                            shape2 = pcaltestdist2.getPixelShape(sector, uPaddle, vPaddle, wPaddle);
                            System.arraycopy( (double[])pcaltestdist2.getShapeCenter(shape2), 0, total2[uPaddle][vPaddle][wPaddle], 0, 3);
                            total2[uPaddle][vPaddle][wPaddle][2] = deltazin;
                        }
                    }
                }
    
            }
        }
        
        //extrapolate centers to end of outer

        double minDist = 1000;
        for(int sector = 0; sector < 1; sector++) {
            for(int uPaddle = 0; uPaddle < 36; uPaddle++) {
                for(int vPaddle = 0; vPaddle < 36; vPaddle++) {
                    for(int wPaddle = 0; wPaddle < 36; wPaddle++) {
                        if(pcaltestdist2.isValidPixel(sector, uPaddle, vPaddle, wPaddle)) {
                            x = (total2[uPaddle][vPaddle][wPaddle][0]
                              -  total1[uPaddle][vPaddle][wPaddle][0])*del;
                            y = (total2[uPaddle][vPaddle][wPaddle][1]
                              -  total1[uPaddle][vPaddle][wPaddle][1])*del;
                            z = (total2[uPaddle][vPaddle][wPaddle][2]
                              -  total1[uPaddle][vPaddle][wPaddle][2])*del;

                            totaldist = Math.sqrt(x*x+y*y+z*z);
                            if(totaldist<minDist) minDist=totaldist;
                            ec_dist[uPaddle][vPaddle][wPaddle]=totaldist;
                        }
                    }
                }    
            }
        }
        
		PrintWriter writerdist = null;
		try 
		{
			writerdist = new PrintWriter("ECpixdepthtotal.dat");
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        for(int uPaddle = 0; uPaddle < 36; uPaddle++) {
            for(int vPaddle = 0; vPaddle < 36; vPaddle++) {
                for(int wPaddle = 0; wPaddle < 36; wPaddle++) {
                    if(pcaltestdist2.isValidPixel(0, uPaddle, vPaddle, wPaddle)) {
                    ec_dist[uPaddle][vPaddle][wPaddle]=ec_dist[uPaddle][vPaddle][wPaddle]/minDist;
                    writerdist.println((uPaddle+1)  + "   " + (vPaddle+1) + "   " + (wPaddle+1) + "   "+ec_dist[uPaddle][vPaddle][wPaddle]);
                    }
                }
            }    
        }
		writerdist.close();		
		
		System.out.println("Done!");
    }
     
     public void writeFPGALookupTable(String filename, double atten, int opt) {
       Pixels newpix = new Pixels();
       int u,v,w,us,vs,ws;
       double dist_u,dist_v,dist_w,ua,va,wa;
       
       try {
          PrintWriter fout = new PrintWriter(filename);
          for(int i=0; i<pixels.getNumPixels() ; i++) {
             u=pixels.getStrip(1,i+1) ; v=pixels.getStrip(2,i+1); w=pixels.getStrip(3,i+1);
             dist_u = pixels.getDist(1,i+1); dist_v=pixels.getDist(2,i+1); dist_w=pixels.getDist(3,i+1);
             ua=Math.exp(-dist_u/atten); va=Math.exp(-dist_v/atten); wa=Math.exp(-dist_w/atten);
             String line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",u,v,w,ua,va,wa); 
             fout.printf(line+"\n");
             if (opt==1) { // FPGA version allows for +/- 1 non-intersecting strips 
             us=u+1;
             if (us<69&&!pixels.pixelStrips.hasItem(us,v,w)&&!newpix.pixelStrips.hasItem(us,v,w)) {
                 line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",us,v,w,ua,va,wa);fout.printf(line+"\n");
                 newpix.addPixel(i,us,v,w);                
             }
             vs=v+1;
             if (vs<63&&!pixels.pixelStrips.hasItem(u,vs,w)&&!newpix.pixelStrips.hasItem(u,vs,w)) {
                 line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",u,vs,w,ua,va,wa);fout.printf(line+"\n");
                 newpix.addPixel(i,u,vs,w);                
             }
             ws=w+1;
             if (ws<63&&!pixels.pixelStrips.hasItem(u,v,ws)&&!newpix.pixelStrips.hasItem(u,v,ws)) {
                 line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",u,v,ws,ua,va,wa);fout.printf(line+"\n");
                 newpix.addPixel(i,u,v,ws);                
             }
             us=u-1;
             if (us>0&&!pixels.pixelStrips.hasItem(us,v,w)&&!newpix.pixelStrips.hasItem(us,v,w)) {
                 line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",us,v,w,ua,va,wa);fout.printf(line+"\n");
                 newpix.addPixel(i,us,v,w);                
             }
             vs=v-1;
             if (vs>0&&!pixels.pixelStrips.hasItem(u,vs,w)&&!newpix.pixelStrips.hasItem(u,vs,w)) {
                 line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",u,vs,w,ua,va,wa);fout.printf(line+"\n");
                 newpix.addPixel(i,u,vs,w);                
             }
             ws=w-1;
             if (ws>0&&!pixels.pixelStrips.hasItem(u,v,ws)&&!newpix.pixelStrips.hasItem(u,v,ws)) {
                 line = String.format("%1$d  %2$d  %3$d  %4$.3f %5$.3f %6$.3f",u,v,ws,ua,va,wa);fout.printf(line+"\n");
                 newpix.addPixel(i,u,v,ws);                
             }
             }
          }       
       fout.close();
       }
     
       catch(FileNotFoundException ex){}       
    
     }

}

