package org.clas.fcmon.cc;

import java.util.ArrayList;
import java.util.List;

import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;

public class CalibrationData {
	
    DetectorDescriptor desc = new DetectorDescriptor();
    GraphErrors       graph = null;
    List<GraphErrors>  rawgraphs  = new ArrayList<GraphErrors>();
    List<GraphErrors>  fitgraphs  = new ArrayList<GraphErrors>();
    List<F1D>          functions  = new ArrayList<F1D>();
    List<Double>             chi2 = new ArrayList<Double>(); 
    
    int          dataSize = 0; 
    int           fitSize = 0;
    
    public double[] fitLimits = {0.0,1.0};
    public double   fitXmin   = 0.;
    public double   fitXmax   = 2500.;
    public double  xprawMax   = 2500.;
    
    private int sector,view,pmt;
    
    F1D f1 = null;
    String otab[]={"Left PMT","Right PMT"};
        
    public CalibrationData(int sector, int layer, int component){
        this.desc.setSectorLayerComponent(sector, layer, component);
        this.sector = sector;
        this.view   = layer;
        this.pmt    = component;        
    }	
	
    public DetectorDescriptor getDescriptor() {
         return this.desc;
    }
    
    public void addGraph(GraphErrors graf){ 
        this.graph = graf;
        graph.getAttributes().setTitleX("FADC");
        graph.getAttributes().setTitleY("Counts");
        graph.getAttributes().setFillStyle(1);
        graph.getAttributes().setMarkerColor(1);
        graph.getAttributes().setMarkerStyle(1);
        graph.getAttributes().setMarkerSize(3);
        graph.getAttributes().setLineColor(1);
        graph.getAttributes().setLineWidth(1);
        
        this.fitgraphs.add(graph);     
    }
    
    public void setFitLimits(double min, double max) {
        fitLimits[0] = min;
        fitLimits[1] = max;       
        fitXmin = fitLimits[0]*xprawMax;
        fitXmax = fitLimits[1]*xprawMax;       
    }
    
    public void addFitFunction(int opt) {
        switch (opt) {
        case 0: f1 = new F1D("Gaus","[A]*gaus(x,[B],[C])",fitXmin,fitXmax);
        f1.setParameter(1,400.); f1.setParLimits(1,10.,2500.);
        f1.setParameter(2,200.); f1.setParLimits(2,50.,300.); break;
        case 1: break;
        case 2: 
        }
        f1.setLineWidth(1); f1.setLineColor(2); 
        this.functions.add(f1);        
    }
    
    public void analyze(int opt){
    	DataFitter.FITPRINTOUT=false;
    	int sl = this.view;
    	addFitFunction(opt);
        F1D func = this.functions.get(0);
        func.setParameter(0, 100.);
        func.setParameter(1, 350.);
        func.setParameter(2, 200.);
        double [] dataY=this.fitgraphs.get(0).getVectorY().getArray();
        if (dataY.length>0) {
            DataFitter.fit(func,this.fitgraphs.get(0),"Q");	//Fit data
            this.fitgraphs.get(0).setFunction(func);                   
            this.fitgraphs.get(0).getFunction().getAttributes().setOptStat("11111");                      
            this.fitgraphs.get(0).getAttributes().setTitle("Sector "+sector+" "+otab[sl-1]+" "+pmt+" FADC Fit");
        }
    }
    
    public GraphErrors  getFitGraph(int index){return this.fitgraphs.get(index);}
    public GraphErrors  getRawGraph(int index){return this.rawgraphs.get(index);}
    public F1D              getFunc(int index){return this.functions.get(index);}
}
