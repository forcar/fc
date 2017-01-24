package org.clas.fcmon.ec;

import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;

public class CalibrationData {
	
    DetectorDescriptor desc = new DetectorDescriptor();
    
    List<GraphErrors>  rawgraphs  = new ArrayList<GraphErrors>();
    List<GraphErrors>  fitgraphs  = new ArrayList<GraphErrors>();
    List<F1D>          functions  = new ArrayList<F1D>();
    List<Double>             chi2 = new ArrayList<Double>(); 
    
    int          dataSize = 0; 
    int           fitSize = 0;
    
    public double[] fitLimits = {0.0,1.0};
    public double   fitXmin   = 0.;
    public double   fitXmax   = 450.;
    public double  xprawMax   = 450.;
    public final    int[] ATT = {150,150,150,150,150,150,150,150,150};
    
    private int sector,view,strip;
    
    F1D f1 = null;
    String otab[]={"U Strip ",      "V Strip ",      "W Strip ",
                   "U Inner Strip ","V Inner Strip ","W Inner Strip ",
                   "U Outer Strip ","V Outer Strip ","W Outer Strip "};
        
    public CalibrationData(int sector, int layer, int component){
        this.desc.setSectorLayerComponent(sector, layer, component);
        this.sector = sector;
        this.view   = layer;
        this.strip  = component;        
    }	
	
    public DetectorDescriptor getDescriptor() {
         return this.desc;
    }
    
    public void addGraph(double[] cnts, double[] xdata, double[] data, double[] error, boolean[] status){
    	
    	GraphErrors graph;
		
		dataSize = data.length;
		fitSize  = 0;
		
        double[] xpraw  = new double[dataSize];
        double[] ypraw  = new double[dataSize]; 
        double[] xprawe = new double[dataSize];
        double[] yprawe = new double[dataSize]; 
        
        Boolean fitcut[] = new Boolean[dataSize];

        // For raw graph cnts>5 data>20
        for(int loop=0; loop < data.length; loop++) {
            fitcut[loop] = cnts[loop]>11&&data[loop]>20&&!status[loop];
            if (fitcut[loop]) fitSize++;
            xpraw[loop]  = xdata[loop]; 
            xprawe[loop] = 0.;
            ypraw[loop]  = data[loop];
            yprawe[loop] = error[loop];
        }

        double[] xpfit  = new double[fitSize];
        double[] ypfit  = new double[fitSize]; 
        double[] xpfite = new double[fitSize];
        double[] ypfite = new double[fitSize];  
        
        // For fit graph
        int n=0;
        for(int loop = 0; loop < data.length; loop++){
            if (fitcut[loop]) {
                xpfit[n]  = xpraw[loop]; 
                xpfite[n] = xprawe[loop];
                ypfit[n]  = ypraw[loop];
                ypfite[n] = yprawe[loop];
                n++;    
            }
        }

        if (dataSize>0) xprawMax = xpraw[dataSize-1];
        
        graph = new GraphErrors("FIT",xpfit,ypfit,xpfite,ypfite);   
        graph.getAttributes().setTitleX("Pixel Distance (cm)");
        graph.getAttributes().setTitleY("Mean ADC");
        graph.getAttributes().setFillStyle(1);
        graph.getAttributes().setMarkerColor(1);
        graph.getAttributes().setMarkerStyle(1);
        graph.getAttributes().setMarkerSize(3);
        graph.getAttributes().setLineColor(1);
        graph.getAttributes().setLineWidth(1);
        
        this.fitgraphs.add(graph);
        
        graph = new GraphErrors("RAW",xpraw,ypraw,xprawe,yprawe);   
        graph.getAttributes().setTitleX("Pixel Distance (cm)");
        graph.getAttributes().setTitleY("Mean ADC");
        graph.getAttributes().setMarkerStyle(1);  
        graph.getAttributes().setMarkerSize(3); 
        graph.getAttributes().setMarkerColor(4);
        graph.getAttributes().setLineColor(4);
        graph.getAttributes().setLineWidth(1);
        
        this.rawgraphs.add(graph);        
    }
    
    public void setFitLimits(double min, double max) {
        fitLimits[0] = min;
        fitLimits[1] = max;
        if (dataSize>0) {
            fitXmin = fitLimits[0]*xprawMax;
            fitXmax = fitLimits[1]*xprawMax;
        }
        
    }
    
    public void addFitFunction(int idet) {
        switch (idet) {
        case 0: f1 = new F1D("A*exp(-x/B)+C","[A]*exp(-x/[B])+[C]",fitXmin,fitXmax);
        f1.setParameter(1,376.); f1.setParLimits(1,1.,500.);
        f1.setParameter(2, 20.); f1.setParLimits(2,1.,100.); break;
        case 1: f1 = new F1D("A*exp(-x/B)+C","[A]*exp(-x/[B])+[C]",fitXmin,fitXmax);
        f1.setParameter(1,376.); f1.setParLimits(1,1.,5000.);
        f1.setParameter(2,0.1);  f1.setParLimits(2,0.,1.); break;
        case 2: f1 = new F1D("A*exp(-x/B)+C","[A]*exp(-x/[B])+[C]",fitXmin,fitXmax);
        f1.setParameter(1,376.); f1.setParLimits(1,1.,5000.);
        f1.setParameter(2,0.1);  f1.setParLimits(2,0.,1.);
        }
        f1.setLineWidth(1); f1.setLineColor(2); 
        this.functions.add(f1);        
    }
    
    public void analyze(int idet){
    	DataFitter.FITPRINTOUT=false;
//    	int sl = idet*3+this.view;
    	int sl = this.view;
    	addFitFunction(idet);
        for(int loop = 0; loop < this.fitgraphs.size(); loop++){
            this.rawgraphs.get(0).getAttributes().setTitle("Sector "+sector+" "+otab[sl-1]+" "+strip+" NO PIXEL FIT");
            F1D func = this.functions.get(loop);
            func.setParameter(0, 0.);
            func.setParameter(1, ATT[sl-1]);
            func.setParameter(2, 0.);
            double [] dataY=this.fitgraphs.get(loop).getVectorY().getArray();
            if (dataY.length>0) {
            	int imax = Math.min(4,dataY.length-1);
            	double p0try = dataY[imax] ; 
            	func.setParameter(0, p0try); func.setParLimits(0,p0try-50.,p0try+100.);
            	DataFitter.fit(func,this.fitgraphs.get(loop),"Q");	//Fit data
            	double yfit,ydat,yerr,ch=0;
            	if (fitSize>0) {
            		for (int i=0 ; i<fitSize ; i++) {
            			yfit = func.evaluate(this.fitgraphs.get(loop).getDataX(i));
            			ydat = this.fitgraphs.get(loop).getDataY(i);
            			yerr = this.fitgraphs.get(loop).getDataEY(i);
             			ch   = ch + Math.pow((ydat-yfit)/yerr,2);
            		}
            		ch = ch/(fitSize-func.getNPars()-1);
                    this.fitgraphs.get(loop).setFunction(func);                   
                    this.fitgraphs.get(loop).getFunction().getAttributes().setOptStat("11111");                      
                    this.rawgraphs.get(loop).getAttributes().setTitle("Sector "+sector+" "+otab[sl-1]+" "+strip+" PIXEL FIT");
                    this.fitgraphs.get(loop).getAttributes().setTitle("Sector "+sector+" "+otab[sl-1]+" "+strip+" PIXEL FIT");
                    this.chi2.add(ch);
            	}
            }
        }  
    }
    
    public GraphErrors  getFitGraph(int index){return this.fitgraphs.get(index);}
    public GraphErrors  getRawGraph(int index){return this.rawgraphs.get(index);}
    public F1D          getFunc(int index)    {return this.functions.get(index);}
    public double       getChi2(int index) {if (this.chi2.isEmpty()==false) return this.chi2.get(index); else return 0.;}
}
