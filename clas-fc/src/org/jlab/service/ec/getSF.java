package org.jlab.service.ec;

import java.util.ArrayList;

import javax.swing.JFrame;

import org.clas.fcmon.tools.FCCalibrationData;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.Func1D;

public class getSF {
    
    public getSF(){
        initGStyle();
    }
    
    public void initGStyle(){
        GStyle.getGraphErrorsAttributes().setMarkerStyle(1);
        GStyle.getGraphErrorsAttributes().setMarkerColor(0);
        GStyle.getGraphErrorsAttributes().setMarkerSize(4);
        GStyle.getGraphErrorsAttributes().setLineColor(0);
        GStyle.getGraphErrorsAttributes().setLineWidth(1);       
    }
    
    public DetectorCollection<H2F> readHipoFile() {        
        FCCalibrationData calib = new FCCalibrationData();
        calib.getFile("/Users/colesmith/test.hipo");
        return calib.getCollection("H2_a_Hist");
    }
    
    public GraphErrors getMean(H2F h2f) {
        GraphErrors graph = new GraphErrors("SF");
        ArrayList<H1F> slices = h2f.getSlicesX();
        int n=0;
        for (H1F slice :slices) {
             double  x = h2f.getDataX(n); 
             double  y = slice.getMean();              
             double ey = slice.getRMS();
             if(y<0.15) y=0.25;
             graph.addPoint(x, y, 0., ey);
            n++;
        }
        return graph;
    }
    
    static class SFFunction extends Func1D{

        public SFFunction(String name, double min, double max) {
            super(name, min, max);
        }

        //Simple polynomial function of any order
        @Override
        public double evaluate(double x){
            double sum = 0.0;
            for(int i=0; i<this.getNPars()-1; i++){
                sum += this.getParameter(i+1)/Math.pow(x,i);
            }
            sum=this.getParameter(0)*sum;
            
            return sum;
        }
    }
    
    static class RESFunction extends Func1D{

        public RESFunction(String name, double min, double max) {
            super(name, min, max);
        }

        //Simple polynomial function of any order
        @Override
        public double evaluate(double x){
            double sum = 0.0;
            for(int i=0; i<this.getNPars()-1; i++){
                sum += this.getParameter(i+1)/Math.pow(x,i);
            }
            sum=this.getParameter(0)*sum;
            
            return sum;
        }
    }
    
    public GraphErrors fitSF(H2F hfit) {
    	
        SFFunction     f1 = new SFFunction("SF", 0.05, 2.7);        
        GraphErrors fitGraph = getMean(hfit);
        
        f1.addParameter("sf"); f1.setParameter(0,0.275);  f1.setParLimits(0, 0.24, 0.29);
        f1.addParameter("a");  f1.setParameter(1,1.0); f1.setParLimits(1, 0.999, 1.001);
        f1.addParameter("b");  f1.setParameter(2,-0.04);
        f1.addParameter("c");  f1.setParameter(3,-0.0);
        f1.setLineWidth(2);
        f1.setLineColor(4);
        f1.setOptStat(111110);
        
        DataFitter.fit(f1, fitGraph,"V");
        
        return fitGraph;
    	
    }
    
    public GraphErrors fitRes(H2F hfit) {
    	    ParallelSliceFitter fitter = new ParallelSliceFitter(hfit);
        fitter.fitSlicesX();
        GraphErrors sigGraph = fitter.getSigmaSlices();    	
    	    return sigGraph;
    }
    
    public static void main(String[] args){        
        JFrame          frame = new JFrame("getSF");
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        getSF              sf = new getSF();
        H2F           fitHist = null;
        GraphErrors  fitGraph = null;
        
        fitHist  = sf.readHipoFile().get(5, 0, 0);
        fitGraph = sf.fitSF(fitHist);
        
		canvas.setAxisTitleSize(18);
		canvas.setAxisLabelSize(18);
        canvas.divide(2, 2);
        
        canvas.cd(0);
        canvas.draw(fitHist);
        canvas.draw(fitGraph,"same");        
        canvas.draw(fitGraph.getFunction(),"same");
        
        fitGraph = sf.fitRes(fitHist);
        
        canvas.cd(1);
        canvas.draw(fitGraph);        
//        canvas.draw(fitGraph.getFunction(),"same");
        
        frame.setSize(800,500);
        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
}
