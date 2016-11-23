package org.clas.fcmon.ec;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.clas.fcmon.tools.CalDrawDB;
import org.clas.fcmon.tools.CalibrationData;
import org.clas.fcmon.tools.FCCalibrationData;
import org.clas.fcmon.tools.Pixel;
import org.clas.fcmon.tools.Pixels;
import org.clas.fcmon.tools.Strips;
import org.clas.fcmon.tools.DataBaseLoader;
import org.clas.fcmon.tools.ECpixelDepth;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.base.DetectorType;
import org.clas.fcmon.detector.view.DetectorShape2D;
import org.jlab.groot.ui.PaveText;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.detector.ec.ECLayer;
import org.jlab.geom.prim.Path3D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.graphics.EmbeddedCanvas;

import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;

public class ECPixels {
	
    CalDrawDB         calDB  = null;
    ECLayer          ecLayer = null;
    ECDetector      detector = null;
    PrintWriter       writer = null;
    DetectorShape2D    shape = new DetectorShape2D();
	
    public TreeMap<Integer,List<double[]>>          clusterXY = new TreeMap<Integer, List<double[]>>();
    public TreeMap<Integer,List<double[]>>             peakXY = new TreeMap<Integer, List<double[]>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_a = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<TreeMap<Integer,Object>> Lmap_t = new DetectorCollection<TreeMap<Integer,Object>>();
    public DetectorCollection<CalibrationData>     collection = new DetectorCollection<CalibrationData>();  

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
    int   ecsumpix[][] = new    int[6][6916];
    int    ecpixel[][] = new    int[6][6916]; 
    
    int        nha[][] = new    int[6][3];
    int        nht[][] = new    int[6][3];
    int    strra[][][] = new    int[6][3][68]; 
    int    strrt[][][] = new    int[6][3][68]; 
    int     adcr[][][] = new    int[6][3][68];      
    double ftdcr[][][] = new double[6][3][68];      
    double  tdcr[][][] = new double[6][3][68]; 	
    
    int[][] sthrMuon = {{15,15,15},{20,20,20},{20,20,20}};
    int[][] sthrPhot = {{10,10,10},{9,9,9},{8,8,8}};
    int[][] sthrElec = {{10,10,10},{10,10,10},{10,10,10}};
    
    int[][] pthrMuon = {{15,15,15},{20,20,20},{20,20,20}};
    int[][] pthrPhot = {{18,18,18},{20,20,20},{15,15,15}};
    int[][] pthrElec = {{30,30,30},{30,30,30},{30,30,30}};
        
    double[] cerrMuon = {5.5,10.,10.};
    double[] cerrPhot = {6.5,15.,20.};
    double[] cerrElec = {10.,10.,10.};
    
    public int idet=0;
    public String detName = null;
	
    public ECPixels(String det, ECDetector ecdet) {		
        System.out.println(" "); System.out.println("ECPixels("+det+")");
        this.detName = det;
        this.detector = ecdet;
        if (det=="PCAL")   idet=0;
        if (det=="ECin")   idet=1;
        if (det=="ECout")  idet=2;
        for (int suplay=idet ; suplay<idet+1; suplay++) {
            for (int layer=0; layer<3; layer++) {
                ecLayer = detector.getSector(0).getSuperlayer(suplay).getLayer(layer);
                ec_nstr[layer] = ecLayer.getAllComponents().size();
            }
        }
        for (int is=1; is<7; is++) {
            clusterXY.put(is, new ArrayList<double[]>());
               peakXY.put(is, new ArrayList<double[]>());
        }
        if (det!="PCAL") getECPixelDepth(ecdet);
        pixdef();
        pixrot();
        System.out.println("ECPixels("+det+") is done");
//      pixHistos();
//    this.writeFPGALookupTable("/Users/colesmith/pcal_att376_DB.dat",376.,1); 
//    this.testStrips();
//    this.testPixels();
        
    }
    
    public static void main(String[] args) {
        ECDetector ecdet  = new ECFactory().createDetectorTilted(DataBaseLoader.getGeometryConstants(DetectorType.EC, 10, "default"));
        ECPixels pix = new ECPixels("PCAL",ecdet);        
    }
    
    public void init() {
        System.out.println("ECPixels.init()");
        Lmap_a.clear();
        Lmap_t.clear();
        collection.clear();    
    }
    
    public int getStripThr(String config, int layer) {
        switch (config) {
        case     "pi0": return sthrPhot[idet][layer-1] ;  
        case    "phot": return sthrPhot[idet][layer-1] ; 
        case    "muon": return sthrMuon[idet][layer-1] ;  
        case    "elec": return sthrElec[idet][layer-1] ;
        }
        return 0;
     }
    
    public int getPeakThr(String config, int layer) {
        switch (config) {
        case     "pi0": return pthrPhot[idet][layer-1] ;  
        case    "phot": return pthrPhot[idet][layer-1] ;  
        case    "muon": return pthrMuon[idet][layer-1] ; 
        case    "elec": return pthrElec[idet][layer-1] ;
        }
        return 0;
     }
    
    public float getClusterErr(String config) {
        switch (config) {
        case     "pi0": return (float) cerrPhot[idet] ;  
        case    "phot": return (float) cerrPhot[idet] ;  
        case    "muon": return (float) cerrMuon[idet] ; 
        case    "elec": return (float) cerrElec[idet] ;
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
            int pix = 0; double maxPixArea=0;
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
        PaveText label[] = new PaveText[4];
        
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
          String val=String.format("Max Area: %1$.3f",pixels.maxZonePixelArea[ic]);
          h[ic].setName(String.format("Max Area: %1$.1f cm^2",pixels.maxZonePixelArea[ic]));
          canvas.cd(ic); canvas.draw(h[ic]);
        }
        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
    }
	
    public void initHistograms(String hipoFile) {
        
        System.out.println("ECPixels:initHistograms()");
        
        DetectorCollection<H2F> H2_a_Hist   = new DetectorCollection<H2F>();
        DetectorCollection<H1F> H1_a_Hist   = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_t_Hist   = new DetectorCollection<H2F>();
        DetectorCollection<H1F> H1_a_Maps   = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_t_Maps   = new DetectorCollection<H1F>();
        DetectorCollection<H2F> H2_PC_Stat    = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Peds_Hist  = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Tdif_Hist  = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Mode1_Hist = new DetectorCollection<H2F>();  
        DetectorCollection<H2F> H2_Mode1_Sevd = new DetectorCollection<H2F>();  
        DetectorCollection<H1F> H1_Stra_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Strt_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Pixa_Sevd  = new DetectorCollection<H1F>();
        DetectorCollection<H1F> H1_Pixt_Sevd  = new DetectorCollection<H1F>();
        
        String id;
        int nstr = ec_nstr[0]            ; double nend = nstr+1;  
        int npix = pixels.getNumPixels() ; double pend = npix+1;
        
        for (int is=1; is<7 ; is++) {           
            // Strip based data  
            for (int il=1 ; il<4 ; il++){                
                // Occupancy  
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 0, new H2F("a_raw_"+id+0, 125,   0., 250., nstr, 1., nend));
                H2_a_Hist.add(is, il, 1, new H2F("b_pix_"+id+1, 125,   0., 250., nstr, 1., nend));
                H2_t_Hist.add(is, il, 0, new H2F("a_raw_"+id+0, 100,1300.,1500., nstr, 1., nend));
                H2_t_Hist.add(is, il, 1, new H2F("b_pix_"+id+1, 100,1300.,1500., nstr, 1., nend));
                // Pedestal Noise  
                H2_Peds_Hist.add(is, il, 0, new H2F("a_ped_"+id+0,  20, -10.,  10., nstr, 1., nend)); 
                // Mode1  
                H2_Mode1_Hist.add(is, il, 0, new H2F("a_fadc_"+id+0, 100,   0., 100.,  nstr, 1., nend));
                // Single Event Strip Occupancy
                H1_Stra_Sevd.add(is, il, 0, new H1F("a_sed_stra_"+id+0, nstr,  1., nend));
                H1_Stra_Sevd.add(is, il, 1, new H1F("b_sed_stra_"+id+1, nstr,  1., nend));
                H1_Strt_Sevd.add(is, il, 0, new H1F("a_sed_strt_"+id+0, nstr,  1., nend));
                // Single Event fADC bins
                H2_Mode1_Sevd.add(is, il, 0, new H2F("a_sed_fadc_"+id+0, 100, 0., 100., nstr, 1., nend));
                H2_Mode1_Sevd.add(is, il, 1, new H2F("b_sed_fadc_"+id+1, 100, 0., 100., nstr, 1., nend));
            }
            
            // Reco data
                id="s"+Integer.toString(is)+"_l"+Integer.toString(4)+"_c";
                H2_a_Hist.add(is, 4, 0, new H2F("a_reco_"+id+0, 1600, 5.,800., 9, 1., 10.));
                H2_a_Hist.add(is, 4, 1, new H2F("a_reco_"+id+1, 50, 0., 3.5, 40, 0.15, 0.35));
                H2_a_Hist.add(is, 4, 2, new H2F("a_reco_"+id+2, 50, 0., 1.0, 40, 0.15, 0.35));
                H2_a_Hist.add(is, 4, 3, new H2F("a_reco_"+id+3, 50, 0., 1.0, 40, 0.15, 0.35));
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
//                H2_a_Hist.add(is, 7, 0, new H2F("d_reco_"+id+0, 200, -600., 600., 200, -600., 600.));   
//                H2_a_Hist.add(is, 7, 1, new H2F("d_reco_"+id+1, 200, -600., 600., 200, -600., 600.));   
                H2_a_Hist.add(is, 7, 2, new H2F("d_reco_"+id+2, 80, 0., 20., 50, 0., 1.5));   
                id="s"+Integer.toString(is)+"_l"+Integer.toString(8)+"_c";
                H2_a_Hist.add(is, 8, 0, new H2F("f_reco_"+id+0, 50, -10., 10., 3, 1., 4.));   
                id="s"+Integer.toString(is)+"_l"+Integer.toString(9)+"_c";
                H2_a_Hist.add(is, 9, 0, new H2F("g_reco_"+id+0, 50, -2., 2., 3, 1., 4.));   
                id="s"+Integer.toString(is)+"_l"+Integer.toString(9)+"_c";
                H2_a_Hist.add(is, 9, 1, new H2F("g_reco_"+id+1, 60, -2., 40., 4, 1., 5.));   //Photon 1,2 MC residuals
                
            //Pixel based data
            for (int il=1; il<4 ; il++) { 
                // Occupancy 
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H2_a_Hist.add(is, il, 2, new H2F("c_pix_"+id+2,  25,   0., 250., npix, 1., pend));
                H2_t_Hist.add(is, il, 2, new H2F("c_pix_"+id+2,  40,1300.,1420., npix, 1., pend));
                // Layer Maps
                H1_a_Maps.add(is, il, 0, new H1F("a_apix_"   +id+0, npix, 1., pend)); //adc weighted pixel
                H1_a_Maps.add(is, il, 1, new H1F("b_napix_"  +id+1, npix, 1., pend)); //normalized adc weighted pixel
                H1_a_Maps.add(is, il, 2, new H1F("c_apix2_"  +id+2, npix, 1., pend)); //adc^2 weighted pixel
                H1_a_Maps.add(is, il, 3, new H1F("d_napix2_" +id+3, npix, 1., pend)); //normalized adc^2 weighted pixel
                H1_a_Maps.add(is, il, 4, new H1F("e_epix_"   +id+4, npix, 1., pend)); //event weighted pixel
                H1_t_Maps.add(is, il, 0, new H1F("a_tdcpix_" +id+0, npix, 1., pend)); 
                H1_t_Maps.add(is, il, 1, new H1F("b_pixt_"   +id+1, npix, 1., pend));                  
            }
            for (int il=1 ; il<3 ; il++) {
                // Single Event Pixel Occupancy
                id="s"+Integer.toString(is)+"_l"+Integer.toString(il)+"_c";
                H1_Pixa_Sevd.add(is, il, 0, new H1F("a_pix_"+id+0, npix,  1., pend));
                H1_Pixt_Sevd.add(is, il, 0, new H1F("a_pix_"+id+0, npix,  1., pend));
            }           
                // Non-layer Pixel Maps
                id="s"+Integer.toString(is)+"_l"+Integer.toString(7)+"_c";
                H1_a_Maps.add(is, 7, 0, new H1F("a_epix_"    +id+0, npix, 1., pend)); // event weighted pixel
                H1_a_Maps.add(is, 7, 1, new H1F("b_asumpix_" +id+1, npix, 1., pend)); // adc   U+V+W weighted pixel
                H1_a_Maps.add(is, 7, 2, new H1F("c_nsumpix_" +id+2, npix, 1., pend)); // adc   U+V+W weighted normalized pixel
                H1_a_Maps.add(is, 7, 3, new H1F("d_nepix_"   +id+3, npix, 1., pend)); // event weighted normalized pixel
                H1_t_Maps.add(is, 7, 0, new H1F("a_epix_"    +id+0, npix, 1., pend));    
                H1_t_Maps.add(is, 7, 1, new H1F("b_tsumpix_" +id+1, npix, 1., pend));    
                H1_t_Maps.add(is, 7, 2, new H1F("c_nsumpix_" +id+2, npix, 1., pend));  
                H1_t_Maps.add(is, 7, 3, new H1F("d_nepix_"   +id+3, npix, 1., pend));    
                        
                id="s"+Integer.toString(is)+"_l"+Integer.toString(0)+"_c";
                H2_PC_Stat.add(is, 0, 0, new H2F("a_evt_"+id+0, nstr, 1., nend,  3, 1., 4.));              
                H2_PC_Stat.add(is, 0, 1, new H2F("b_adc_"+id+1, nstr, 1., nend,  3, 1., 4.));              
                H2_PC_Stat.add(is, 0, 2, new H2F("c_tdc_"+id+2, nstr, 1., nend,  3, 1., 4.));                       
                H2_PC_Stat.add(is, 0, 3, new H2F("d_pix_"+id+3,   50,-1.,    1,  3, 1., 4.));                       
                H2_PC_Stat.add(is, 0, 4, new H2F("e_pix_"+id+4,   50, 0.,  1.1,  4, 0., 4.));                       
        }
        
        if(hipoFile!=" "){
            FCCalibrationData calib = new FCCalibrationData();
            calib.getFile(hipoFile);
            H2_a_Hist = calib.getCollection("H2_a_Hist");
            H1_a_Hist = calib.getCollection("H1_a_Hist");
            H1_a_Maps = calib.getCollection("H1_a_Maps");
            H2_t_Hist = calib.getCollection("H2_t_Hist");
            H1_t_Maps = calib.getCollection("H1_t_Maps");
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
        strips.addH2DMap("H2_Peds_Hist", H2_Peds_Hist);
        strips.addH2DMap("H2_Tdif_Hist", H2_Tdif_Hist);
        strips.addH2DMap("H2_Mode1_Hist",H2_Mode1_Hist);
        strips.addH2DMap("H2_Mode1_Sevd",H2_Mode1_Sevd);
   
    }	

    public void pixrot() {
        
        System.out.println("ECPixels.pixrot()");
    	double[] theta={0.0,60.0,120.0,180.0,240.0,300.0};
	    	for(int is=0; is<6; is++) {
	    	    double thet=theta[is]*3.14159/180.;
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
        int num1, num2, num3;
        
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
        
        for(int uPaddle = 0; uPaddle < 36; uPaddle++) {
            for(int vPaddle = 0; vPaddle < 36; vPaddle++) {
                for(int wPaddle = 0; wPaddle < 36; wPaddle++) {
                    if(pcaltestdist2.isValidPixel(0, uPaddle, vPaddle, wPaddle)) {
                    ec_dist[uPaddle][vPaddle][wPaddle]=ec_dist[uPaddle][vPaddle][wPaddle]/minDist;
                    }
                }
            }    
        }

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

