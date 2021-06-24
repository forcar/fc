package org.clas.fcmon.tools;


import java.awt.*;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.jlab.geom.prim.Point3D;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.utils.groups.IndexedList;

public class Survey{
	
	String path = "/Users/colesmith/CLAS12ANA/files/";
	VertexMap pcal,ec,pcal_r, pcal_rt, ec_r, ec_rt, pcal_t, ec_t;
    JFrame mainFrame;
    JPanel contentPane;
    JTable tabpcal,tabec;
    Object[][] tableData;
    EmbeddedCanvas canvas;
    DataGroup dg;
    float rot = 60f, tilt=25f;
	String[] columnNames = {"Name","x","y","z","Name","x","y","z"};
    
	public Survey() {				
		pcal    = readData("PCAL",path+"pcal_survey.txt"); 
		ec      = readData("EC",  path+"ec_survey.txt");
		pcal_r  = getRotatedMap(pcal) ; 
		ec_r    = getRotatedMap(ec) ;     
		pcal_t  = getRotatedMap(pcal) ; 
		ec_t    = getRotatedMap(ec) ;     
		pcal_rt = getRotatedTiltedMap(pcal);
		ec_rt   = getRotatedTiltedMap(ec) ;     
	}
	
	public void createPanels() {
        contentPane = new JPanel(new BorderLayout());			
        contentPane.add(canvas,BorderLayout.CENTER);        
	}
	
	public void execute() {
		printVertices(pcal_rt,ec_rt);
		plotVertices(pcal_rt,ec_rt);
	}
	
	public void initGraphics() { 
        mainFrame = new JFrame("Survey");
	}
	
	public void plotVertices(VertexMap... maps) {
			
		initGraphics();	
        initGStyle();        
		canvas = new EmbeddedCanvas(); 
//		canvas.divide(6,3); for(VertexMap map : maps) plotSecRes(map);
//		canvas.divide(6,1); for(VertexMap map : maps) plotDistRes(map);
		canvas.divide(6, 2); plotPCmEC();
		createPanels(); 
		//plotTables();
        mainFrame.setContentPane(contentPane);
        mainFrame.setSize(new Dimension(1200,800));
        mainFrame.pack();
        mainFrame.setVisible(true);        
	}
	
	public void plotTables() {
		tabpcal   = new JTable(pcal_rt._tableData,columnNames);
	    tabpcal.setRowHeight(20);
	    for (int i=0; i<tabpcal.getColumnCount(); i++) {
		    CustomRenderer renderer = new CustomRenderer();
		    renderer.setHorizontalAlignment(i==0||i==4?JLabel.LEFT:JLabel.RIGHT);	
		    tabpcal.getColumnModel().getColumn(i).setCellRenderer(renderer);
	    }
	    tabpcal.updateUI();
//		tabec     = new JTable(ec._tableData,columnNames);
        contentPane.add(new JScrollPane(tabpcal),BorderLayout.PAGE_END);        
 		
	}
	
    public static class CustomRenderer extends DefaultTableCellRenderer {
    	
    			 @Override
        public Component getTableCellRendererComponent
                (JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
                {

    				 Component c = super.getTableCellRendererComponent
                                          (table, value, isSelected, hasFocus, row, column);
                     if(isSelected==true){
                         c.setBackground(new Color(20,20,255));                
                         return c;
                     }
                    if(row<6)       {c.setBackground(new Color(220,255,220));}
                    else if(row<12) {c.setBackground(new Color(220,220,255));}
                    else if(row<18) {c.setBackground(new Color(220,255,220));}
                    else if(row<24) {c.setBackground(new Color(220,220,255));}
                    else if(row<30) {c.setBackground(new Color(220,255,220));}
                    else if(row<36) {c.setBackground(new Color(220,220,255));}
                    
           
                    return c;
                }
    }	
    
	public void drawGraph(DataGroup dg, EmbeddedCanvas c, String tit, int ind, Boolean opt, float ymin, float ymax) {
		c.cd(ind) ; 
		c.getPad().getAxisX().setRange(0.5,6.5);
		c.getPad().getAxisY().setRange(ymin,ymax);
		F1D f0 = new F1D("f0","[a]",0.5,6.5); f0.setParameter(0,0); 		
		c.draw(((GraphErrors) dg.getData(ind).get(0)),opt?"same":""); c.draw(f0,"same");		
	}
	
	public void plotSecRes(VertexMap vmap) {
		dg = new DataGroup(6,3); 
		Boolean opt = vmap.getName()=="EC";
		int n=0;       
        String xtxt = "Corner Points";
        String[] ytxt = {"X","Y","Z"};
        
		for (int ix=0; ix<3; ix++) {
   	    for (int is=1; is<7; is++) {            
   	    	GraphErrors graph = new GraphErrors();   	    	
   	    	if(ix==0) graph.setTitle("PCAL(blk) EC(red) rot tilt S"+is);
   	    	if(ix==2) graph.setTitleX(xtxt);
   	    	if(is==1) graph.setTitleY(ytxt[ix]+"-"+ytxt[ix]+"avg (mm)");
   	    	graph.setMarkerColor(opt?2:1);
            dg.addDataSet(graph,n);n++;
        } 
		}
       
		for(String str : vmap.getMap().keySet()) {
			int is=getSector(str)-1;
			int in=vmap.getIndex(str)+1;
			((GraphErrors) dg.getData(is+ 0).get(0)).addPoint(in, vmap.getSecRes(str).x(), 0,0);
			((GraphErrors) dg.getData(is+ 6).get(0)).addPoint(in, vmap.getSecRes(str).y(), 0,0);
			((GraphErrors) dg.getData(is+12).get(0)).addPoint(in, vmap.getSecRes(str).z(), 0,0);			
		}
		
		for (int is=0; is<6; is++) {
			drawGraph(dg,canvas,"Sector "+(is+1),is,opt,-20,20);			
			drawGraph(dg,canvas,"",is+6,opt,-20,20);			
			drawGraph(dg,canvas,"",is+12,opt,-20,20);			
		}
		
	}
	public void plotDistRes(VertexMap vmap) {
		dg = new DataGroup(6,1); 
		Boolean opt = vmap.getName()=="EC";
		int n=0;       
        String xtxt = "Line Segments";
        String[] ytxt = {"D"};
        
		for (int ix=0; ix<1; ix++) {
   	    for (int is=1; is<7; is++) {            
   	    	GraphErrors graph = new GraphErrors();   	    	
   	    	if(ix==0) graph.setTitle("PCAL(blk) EC(red) rot tilt S"+is);
   	    	if(ix==0) graph.setTitleX(xtxt);
   	    	if(is==0) graph.setTitleY(ytxt[ix]+"-"+ytxt[ix]+"avg (mm)");
   	    	graph.setMarkerColor(opt?2:1);
            dg.addDataSet(graph,n);n++;
        } 
		}
       
		for(int is=0; is<6; is++) {
			int off = is*6;
			((GraphErrors) dg.getData(is).get(0)).addPoint(1, vmap.getDistRes(5+off,0+off), 0,0); //v side
			((GraphErrors) dg.getData(is).get(0)).addPoint(2, vmap.getDistRes(1+off,2+off), 0,0); //u side
			((GraphErrors) dg.getData(is).get(0)).addPoint(3, vmap.getDistRes(3+off,4+off), 0,0); //w side
    		((GraphErrors) dg.getData(is).get(0)).addPoint(4, vmap.getDistRes(0+off,1+off), 0,0); //u corner
			((GraphErrors) dg.getData(is).get(0)).addPoint(5, vmap.getDistRes(2+off,3+off), 0,0); //w corner
			((GraphErrors) dg.getData(is).get(0)).addPoint(6, vmap.getDistRes(4+off,5+off), 0,0); //v corner
		}
		
		for (int is=0; is<6; is++) {
			drawGraph(dg,canvas,"Sector "+(is+1),is,opt,-5,5);					
		}		
	}
	
	public void plotPCmEC() {
		dg = new DataGroup(6,2); 
	
		int n=0;       
        String xtxt = "Corner Points";

        String[] ytxt = {"#DeltaY   PCAL - EC (mm)","#DeltaZ   PCAL - EC (mm)"};
        
		for (int ix=0; ix<2; ix++) {
   	    for (int is=1; is<7; is++) {            
   	    	GraphErrors graph = new GraphErrors();   	    	
   	    	if(ix==0) graph.setTitle("SECTOR "+is);
   	    	if(ix==1) graph.setTitleX(xtxt);
   	    	if(is==1) graph.setTitleY(ytxt[ix]);   	    	
            dg.addDataSet(graph,n);n++;
        } 
		} 
		
		float yavgpc[]= {0,0,0,0,0,0}, yavgec[]= {0,0,0,0,0,0}; 
		for(String str : pcal_rt.getMap().keySet()) {
			int is=getSector(str)-1;
			int  in=pcal_rt.getIndex(str)+1;
			int inn=pcal_rt.getVindex(str);
			yavgec[is] += (float)ec_rt.getVertex(inn).y();yavgpc[is] += (float)pcal_rt.getVertex(str).y();
			((GraphErrors) dg.getData(is+ 0).get(0)).addPoint(in,          pcal_rt.getVertex(str).y()-ec_rt.getVertex(inn).y(), 0,0);
			((GraphErrors) dg.getData(is+ 6).get(0)).addPoint(in, Math.abs(pcal_rt.getVertex(str).z()-ec_rt.getVertex(inn).z()), 0,0);
			System.out.println(str+" "+pcal_rt.getVertex(str).y()+" "+ec_rt.getVertex(inn).y());
		}
		
		
		F1D f0 = new F1D("f0","[a]",0.5,6.5); f0.setParameter(0,9.53);      f0.setLineColor(2);
		F1D f1 = new F1D("f1","[a]",0.5,6.5); f1.setParameter(0,262.75);    f1.setLineColor(2);
		
		for (int is=0; is<6; is++) {
			drawGraph(dg,canvas,"Sector "+(is+1),is,false,-80,140);
			F1D f2 = new F1D("f2","[a]",0.5,6.5); f2.setParameter(0,yavgpc[is]/6f); f2.setLineColor(4); f2.setLineStyle(2);
			F1D f3 = new F1D("f3","[a]",0.5,6.5); f3.setParameter(0,yavgec[is]/6f); f3.setLineColor(4); f3.setLineStyle(1);
			canvas.draw(f0,"same"); canvas.draw(f2,"same"); canvas.draw(f3,"same");
			drawGraph(dg,canvas,"",is+6,false,252,272);				
			canvas.draw(f1,"same"); 
		}
	
	}
	
	public void printVertices(VertexMap... maps) {		
		for(VertexMap map : maps) {printVertices(map); System.out.println(" ");}	
	}
	
	public String myFormat(Point3D point) {
		float x=(float)point.x(); float y=(float)point.y(); float z=(float)point.z();
		return String.format("Point3D:\t%8.1f %8.1f %8.1f", x, y, z);
	}
    
    public void printVertices(VertexMap vmap) {
    	for(String str : vmap.getMap().keySet()) {
    		System.out.println(str+"    "+myFormat(vmap.getVertex(str))+" "
    	                                 +myFormat(vmap.getSecRes(str))+" "
    				                     +myFormat(vmap.getSecAvg(str)));
    	}
    }
	
	public VertexMap getRotatedMap(VertexMap vmap) {
		VertexMap map = new VertexMap(vmap.getName());
    	for(String str : vmap.getMap().keySet()) {
    		map.addVertex(str, getRotatedPoint(getSector(str),vmap.getVertex(str)));
    	}
    	return map;
	}
	
	public VertexMap getRotatedTiltedMap(VertexMap vmap) {
		VertexMap map = new VertexMap(vmap.getName());
    	for(String str : vmap.getMap().keySet()) {
    		map.addVertex(str, getRotatedTiltedPoint(getSector(str),vmap.getVertex(str)));
    	}
    	return map; 
	}
	
	public VertexMap getTiltedMap(VertexMap vmap) {
		VertexMap map = new VertexMap(vmap.getName());
    	for(String str : vmap.getMap().keySet()) {
    		map.addVertex(str, getTiltedPoint(getSector(str),vmap.getVertex(str)));
    	}
    	return map; 
	}
	
	public VertexMap getResidMap(VertexMap vmap) {
		VertexMap map = new VertexMap(vmap.getName());
    	for(String str : vmap.getMap().keySet()) {
    		map.addVertex(str, getRotatedTiltedPoint(getSector(str),vmap.getVertex(str)));
    	}
    	return map;		
	}

    public int getSector(String str) {
    	String[] col = str.trim().split("_");
    	return Integer.parseInt(col[0]);    	
    }
    
    public Point3D getRotatedPoint(int is, Point3D point) {
    	Point3D newpoint = new Point3D();
    	newpoint.copy(point);
    	newpoint.rotateZ(Math.toRadians(-rot*(is-1))); 
        return newpoint;
    }

    public Point3D getRotatedTiltedPoint(int is, Point3D point) {
    	Point3D newpoint = new Point3D();
    	newpoint.copy(point);
    	newpoint.rotateZ(Math.toRadians(-rot*(is-1)));        
        newpoint.rotateY(Math.toRadians(-tilt));
        return newpoint;
    }
    
    public Point3D getTiltedPoint(int is, Point3D point) {
    	Point3D newpoint = new Point3D();
    	newpoint.copy(point);
    	newpoint.rotateZ(Math.toRadians(-rot*(is-1)));        
        newpoint.rotateY(Math.toRadians(-tilt));
    	newpoint.rotateZ(Math.toRadians(+rot*(is-1)));        
       return newpoint;
    }   
    
	private class VertexMap {
		
		private LinkedHashMap<String,Point3D> _vtxmap;
		private LinkedHashMap<String,Integer> _indmap;
		private LinkedHashMap<String,Integer> _vndmap;
		private LinkedHashMap<Integer,String> _nammap;
		private IndexedList<List<Point3D>>    _secmap;
		private String                        _mapnam;
		
		private int _vtxID = 0;
		
		private Object[][] _tableData = new Object[18][8];
		
		public VertexMap(String name) {
			_mapnam = name;
			_vtxmap = new LinkedHashMap<String,Point3D>();
			_indmap = new LinkedHashMap<String,Integer>();
			_vndmap = new LinkedHashMap<String,Integer>();
			_nammap = new LinkedHashMap<Integer,String>();
			_secmap = new IndexedList<List<Point3D>>(1);
		}
		
		public VertexMap() {
			
		}
		
		public String getName() {
			return _mapnam;
		}
		
		public LinkedHashMap<String,Point3D> getMap() {
			return _vtxmap;
		}
		
		public IndexedList<List<Point3D>> getSecMap() {
			return _secmap;
		}
		
		public int getIndex(String name) {
			return _indmap.get(name);
		}
		
		public int getVindex(String name) {
			return _vndmap.get(name);
		}
		
		public Point3D getSecAvg(String name) {
			return Point3D.average(_secmap.getItem(_indmap.get(name)));
		}
		
	    public void addVertex(String name, Point3D point) {
	        _vtxmap.put(name, point);
	        int in = _vtxID%6;
	        _indmap.put(name, in);
	        _vndmap.put(name, _vtxID);
	        _nammap.put(_vtxID, name);
	        if(!_secmap.hasItem(in)) _secmap.add(new ArrayList<Point3D>(), in);
	        _secmap.getItem(in).add(point);
	        if(_vtxID<18) {
	        	_tableData[_vtxID][0] = (String)name; 
	        	_tableData[_vtxID][1] = (float)point.x();
	        	_tableData[_vtxID][2] = (float)point.y();
	        	_tableData[_vtxID][3] = (float)point.z();
	        } else {
		        _tableData[_vtxID-18][4] = (String)name; 
		        _tableData[_vtxID-18][5] = (float)point.x();
		        _tableData[_vtxID-18][6] = (float)point.y();
		        _tableData[_vtxID-18][7] = (float)point.z();	        	
	        }
	        _vtxID++;	        
	    }
	    
	    public Point3D getVertex(String name) {
	        if(!_vtxmap.containsKey(name)) {
	                addVertex(name,new Point3D());
	        }
	        return _vtxmap.get(name);
	    }
	    
	    public Point3D getVertex(int ind) {
	        return getVertex(_nammap.get(ind));
	    }	    	    
	    
	    public Point3D getSecRes(String name) {
	    	return getVertex(name).vectorFrom(getSecAvg(name)).toPoint3D();
	    }
	    
	    public float getDistRes(int in1, int in2) {
	    	float p1 = (float) getSecAvg(_nammap.get(in1)).distance(getSecAvg(_nammap.get(in2)));
	    	float p2 = (float) getVertex(_nammap.get(in1)).distance(getVertex(_nammap.get(in2)));	    	
	    	return p1-p2;
	    }
	}

	public VertexMap readData(String name, String fname) {
		
		VertexMap map = new VertexMap(name);
		
		try{
			FileReader       file = new FileReader(fname);
			BufferedReader reader = new BufferedReader(file);
			
			int n = 0 ;
			while (n<36) {		
				String line = reader.readLine(); 
				String[] col = line.trim().split("\\s+"); 
				float x = Float.parseFloat(col[1]);
				float y = Float.parseFloat(col[2]);
				float z = Float.parseFloat(col[3]);
				map.addVertex(col[0], new Point3D(x,y,z));
				n++;
			}
			reader.close();
			file.close();    
		}
		catch(FileNotFoundException ex) {
			ex.printStackTrace();            
		}     
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		return map;
	}
	
    public void initGStyle() {
        GStyle.getAxisAttributesX().setTitleFontSize(18);
        GStyle.getAxisAttributesX().setLabelFontSize(18);
        GStyle.getAxisAttributesY().setTitleFontSize(18);
        GStyle.getAxisAttributesY().setLabelFontSize(18);
        GStyle.getAxisAttributesZ().setLabelFontSize(18); 
        GStyle.getAxisAttributesX().setAxisGrid(false);
        GStyle.getAxisAttributesY().setAxisGrid(false);
        GStyle.getAxisAttributesX().setLabelFontName("Avenir");
        GStyle.getAxisAttributesY().setLabelFontName("Avenir");
        GStyle.getAxisAttributesZ().setLabelFontName("Avenir");
        GStyle.getAxisAttributesX().setTitleFontName("Avenir");
        GStyle.getAxisAttributesY().setTitleFontName("Avenir");
        GStyle.getAxisAttributesZ().setTitleFontName("Avenir");
        GStyle.getGraphErrorsAttributes().setMarkerStyle(1);
        GStyle.getGraphErrorsAttributes().setMarkerColor(1);
        GStyle.getGraphErrorsAttributes().setMarkerSize(4);
    }
	
    public static void main(String[] args){   	
    	Survey survey = new Survey();
    	survey.execute();
    }	
}
