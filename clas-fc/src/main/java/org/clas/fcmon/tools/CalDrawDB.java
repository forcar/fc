package org.clas.fcmon.tools;

import math.geom2d.*;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;

import org.jlab.detector.base.DetectorLayer;
import org.jlab.detector.base.DetectorType;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECLayer;
import org.jlab.geom.prim.Point3D;

public class CalDrawDB{
	
	private double length;
	private double angle;
	private double anglewidth;
	private double slightshift;
	private    int unit; //PCAL ==0, ECinner ==1, ECouter==2 
	private int[] numstrips = new int[3];
	private int[][][] validoverlap; // 3,68,62 first number is 0,1,2
	private int[][][] validpixel; //68,62,62 for pcal

	
	//                                        [sector][u,v,w][strip number][vertex number]
	private static double[][][][] xPoint = new double [6][3][68][4];
	private static double[][][][] yPoint = new double [6][3][68][4];
	private static double[][][][] zPoint = new double [6][3][68][4];

	public CalDrawDB(String detector, ECDetector ecdet) {
		     if(detector.contains("PCAL"))  unit = 0;
		else if(detector.contains("ECin"))  unit = 1;
		else if(detector.contains("ECout")) unit = 2;
        else if(detector.contains("EC"))    unit = 1;
		else System.err.println("Must pass in PCAL, EC, ECin, or ECout");
		
		if(unit==0)
		{
			initPCALVert(ecdet);
			validoverlap = new int[3][68][62];
			//[0=uw,1=uv,2=vw][first layer strip][second layer strip]
			validpixel = new int[68][62][62];
			//[u strip #][v strip #][w strip #]
		}
		if(unit>0)   
		{
			initECVert(ecdet);
			validoverlap = new int[3][36][36];
			//[0=uw,1=uv,2=vw][first layer strip][second layer strip]
			validpixel = new int[36][36][36];
			//[u strip #][v strip #][w strip #]
		}
		
		length = 4.5;
		angle = 62.8941;
		anglewidth = length/Math.sin(Math.toRadians(angle));
		slightshift = length/Math.tan(Math.toRadians(angle));
	}
	
	public CalDrawDB() {
	    
	}
/*
	//collects all possible pixels into a DetectorShapeView2D
	public DetectorShapeView2D drawAllPixels(int sector)
	{
		DetectorShapeView2D pixelmap= new DetectorShapeView2D("PCAL Pixels");
            for(int uPaddle = 0; uPaddle < 68; uPaddle++){
            	for(int vPaddle = 0; vPaddle < 62; vPaddle++){
            		for(int wPaddle = 0; wPaddle < 62; wPaddle++){
            			if(isValidPixel(sector, uPaddle, vPaddle, wPaddle))
            				pixelmap.addShape(getPixelShape(sector, uPaddle, vPaddle, wPaddle));
            		}
            	}
            }
            return pixelmap;
	}
	
	
	//collects all possible UW intersections into a DetectorShapeView2D
	public DetectorShapeView2D drawUW(int sector)
	{
		DetectorShapeView2D UWmap= new DetectorShapeView2D("PCAL UW");
	    	for(int uPaddle = 0; uPaddle < 68; uPaddle++){
	            for(int wPaddle = 0; wPaddle < 62; wPaddle++){
	            	if(isValidOverlap(sector, 0, uPaddle, 2, wPaddle))
	            		UWmap.addShape(getOverlapShape(sector, 0, uPaddle, 2, wPaddle));
	            }
	          
	    	}
	        return UWmap;
	}
	
	//collects all possible UW intersections into a DetectorShapeView2D
	public DetectorShapeView2D drawWU(int sector)
	{
		DetectorShapeView2D WUmap= new DetectorShapeView2D("PCAL WU");
		   	for(int uPaddle = 0; uPaddle < 68; uPaddle++){
		   		for(int wPaddle = 0; wPaddle < 62; wPaddle++){
		            if(isValidOverlap(sector, 2, wPaddle, 0, uPaddle))
		            	WUmap.addShape(getOverlapShape(sector, 2, wPaddle, 0, uPaddle));
		        }
		          
		   	}
		    return WUmap;
	}
	
	//collects all possible UW intersections into a DetectorShapeView2D
	public DetectorShapeView2D drawVU(int sector)
	{
		DetectorShapeView2D UVmap= new DetectorShapeView2D("PCAL UV");
		   	for(int uPaddle = 0; uPaddle < 68; uPaddle++){
		   		for(int vPaddle = 0; vPaddle < 62; vPaddle++){
		            if(isValidOverlap(sector, 1, vPaddle, 0, uPaddle))
		            	UVmap.addShape(getOverlapShape(sector,1, vPaddle, 0, uPaddle));
		        }
		          
		   	}
		    return UVmap;
	}
	
	
	
	//collects all U strips into a DetectorShapeView2D
	public DetectorShapeView2D drawUStrips(int sector)
	{
		DetectorShapeView2D Umap= new DetectorShapeView2D("PCAL U Strips");
		   	for(int uPaddle = 0; uPaddle < 68; uPaddle++){
		   		//System.out.println(uPaddle);
		   		Umap.addShape(getStripShape(sector, 0, uPaddle));
		   	}
		    return Umap;
	}
	
	//collects all V strips into a DetectorShapeView2D
	public DetectorShapeView2D drawVStrips(int sector)
	{
		DetectorShapeView2D Vmap= new DetectorShapeView2D("PCAL V Strips");
		   	for(int vPaddle = 0; vPaddle < 62; vPaddle++){
		   		//System.out.println(vPaddle);
		   		Vmap.addShape(getStripShape(sector, 1, vPaddle));
		   	}
		    return Vmap;
	}

	//collects all W strips into a DetectorShapeView2D
	public DetectorShapeView2D drawWStrips(int sector)
	{
		DetectorShapeView2D Wmap= new DetectorShapeView2D("PCAL W Strips");
		   	for(int wPaddle = 0; wPaddle < 62; wPaddle++){
		   		//System.out.println(wPaddle);
		   		Wmap.addShape(getStripShape(sector, 2, wPaddle));
		   	}
		    return Wmap;
	}
	
*/
	
	//calls getPixelVerticies
	//uses those 3 verticies to make a shape
	public DetectorShape2D getPixelShape(int sector, int uPaddle, int vPaddle, int wPaddle){
		
		Object[] obj = getPixelVerticies(sector, uPaddle, vPaddle, wPaddle);
		int numpoints = (int)obj[0];
		if (numpoints<3) return null;
		
		double[] x = new double[numpoints];
		double[] y = new double[numpoints];
		
		System.arraycopy( (double[])obj[1], 0, x, 0, numpoints);
		System.arraycopy( (double[])obj[2], 0, y, 0, numpoints);
	    
        DetectorShape2D  pixel = new DetectorShape2D(DetectorType.ECAL,sector,2,uPaddle * 10000 + vPaddle * 100 + wPaddle,0);
    	pixel.getShapePath().clear(); 

        for(int i = 0; i < numpoints; ++i) pixel.getShapePath().addPoint(x[i],  y[i],  0.0); 
       
        return pixel;				
	}

	//calls getOverlapVerticies
	//uses those 3 verticies to make a shape
	//                                       sector, 0=u:1=v:2=w,  0-67:0-61:0-61, 0=u:1=v:2=w, 0-67:0-61:0-61
	public DetectorShape2D getOverlapShape(int sector, int strip1, int paddle1, int strip2, int paddle2){
			
		int uPaddle = -1;
		int vPaddle = -1;
		int wPaddle = -1;
		//assign strip 1
		if(strip1 == 0)
		{
			uPaddle = paddle1;
		}
		else if(strip1 == 1)
		{
			vPaddle = paddle1;
		}
		else if(strip1 == 2)
		{
			wPaddle = paddle1;
		}
		else
		{
			System.err.println("Wrong input used for getOverlapShape layers");
		}
		
		//assign strip 2
		if(strip2 == 0)
		{
			uPaddle = paddle2;
		}
		else if(strip2 == 1)
		{
			vPaddle = paddle2;
		}
		else if(strip2 == 2)
		{
			wPaddle = paddle2;
		}
		else
		{
			System.err.println("Wrong input used for getOverlapShape layers");
		}
		
		Object[] obj = getOverlapVerticies(sector, strip1, paddle1, strip2, paddle2);
		int numpoints = (int)obj[0];
		
	      	
		DetectorShape2D  overlapShape;
		
		if(numpoints > 2)
		{
			
			double[] x = new double[numpoints];
			double[] y = new double[numpoints];
			System.arraycopy( (double[])obj[1], 0, x, 0, numpoints);
			System.arraycopy( (double[])obj[2], 0, y, 0, numpoints);
				
		    if(uPaddle == paddle1 && wPaddle == paddle2)
		    	overlapShape = new DetectorShape2D(DetectorType.ECAL,sector,3,uPaddle * 100 + wPaddle,0);
		    else if(uPaddle == paddle1 && vPaddle == paddle2)
	    		overlapShape = new DetectorShape2D(DetectorType.ECAL,sector,4,uPaddle * 100 + vPaddle,0);
		    else if(vPaddle == paddle1 && uPaddle == paddle2)
	    		overlapShape = new DetectorShape2D(DetectorType.ECAL,sector,4,uPaddle * 100 + vPaddle,0);
		    else if(wPaddle == paddle1 && uPaddle == paddle2)
		    	overlapShape = new DetectorShape2D(DetectorType.ECAL,sector,5,uPaddle * 100 + wPaddle,0);
		    else
		    	overlapShape = new DetectorShape2D(DetectorType.ECAL,sector,6,vPaddle * 100 + wPaddle,0);

	    
		    overlapShape.getShapePath().clear(); 

	        for(int i = 0; i < numpoints; ++i){ 
	        	overlapShape.getShapePath().addPoint(x[i],  y[i],  0.0); 
	        	//System.out.println("i: " + i + " x: " + x[i] + " y: " + y[i]);
	        } 

	           
	         /*
	         if(paddle%2==0){
	             shape.setColor(180, 255, 180);
	         } else {
	             shape.setColor(180, 180, 255);
	         }
	         */
	        return overlapShape;
	     }
		 else
		 {
			System.out.println("Not a valid overlap shape");
			return null;
		 }
	    		
	}
	
	
	//calls getOverlapVerticies
	//uses those 3 verticies to make a shape
	public DetectorShape2D getStripShape(int sector, int strip1, int paddle1){
			
		int uPaddle = -1;
		int vPaddle = -1;
		int wPaddle = -1;
		//assign strip 1
		if(strip1 == 0)
		{
			uPaddle = paddle1;
		}
		else if(strip1 == 1)
		{
			vPaddle = paddle1;
		}
		else if(strip1 == 2)
		{
			wPaddle = paddle1;
		}
		else
		{
			System.err.println("Wrong input used for getStripShape layers");
		}

		Object[] obj = getStripVerticies(sector, strip1, paddle1);
		int numpoints = (int)obj[0];
		//System.out.println("Strip let: " + strip1 + "Strip num: " + paddle1 + " Numpoints: " + numpoints);
		double[] x = new double[numpoints];
		double[] y = new double[numpoints];
		System.arraycopy( (double[])obj[1], 0, x, 0, numpoints);
		System.arraycopy( (double[])obj[2], 0, y, 0, numpoints);
			
		/*
	    for(int i = 0; i< numpoints; ++i)
	    {
	        y[i] -= 400.0;
	    }
	    */

	      
	        	
		DetectorShape2D  stripShape;
	    if(uPaddle == paddle1)
	    	stripShape = new DetectorShape2D(DetectorType.ECAL,sector,7,uPaddle,0);
	    else if(vPaddle == paddle1)
	    	stripShape = new DetectorShape2D(DetectorType.ECAL,sector,8,vPaddle,0);
	    else if(wPaddle == paddle1)
	    	stripShape = new DetectorShape2D(DetectorType.ECAL,sector,9,wPaddle,0);
	    else
	    {
	    	stripShape = new DetectorShape2D();
	    	System.out.println("Either " + strip1 + " is not a valid strip letter, or " + paddle1 + " is not a valid number.");
	    }
	    
	    stripShape.getShapePath().clear(); 
	    if(numpoints > 2) 
	    {
	        for(int i = 0; i < numpoints; ++i){ 
	        	stripShape.getShapePath().addPoint(x[i],  y[i],  0.0); 
	        } 
	           
	         /*
	         if(paddle%2==0){
	             shape.setColor(180, 255, 180);
	         } else {
	             shape.setColor(180, 180, 255);
	         }
	         */
	     }
	    return stripShape;		
	}
	
	

	
	//calls getPixelVerticies to check
	//that at least 3 points exist,
	// if so it is marked as true, else false
	public Boolean isValidPixel(int sector, int uPaddle, int vPaddle, int wPaddle){
		
		if(validoverlap[0][uPaddle][wPaddle] == -1) return false;
		else if(validoverlap[1][uPaddle][vPaddle] == -1) return false;
		else if(validoverlap[2][vPaddle][wPaddle] == -1) return false;
		else if(validpixel[uPaddle][vPaddle][wPaddle] == -1) return false;
		else
		{
		
			Object[] obj = getPixelVerticies(sector, uPaddle, vPaddle, wPaddle);
			int numpoints = (int)obj[0];
			
	        if(numpoints > 2) 
	        	return true;
	        else
	        	return false;	
		}
        	
	}
	
	//calls getPixelVerticies to check
	//that at least 3 points exist,
	// if so it is marked as true, else false
	public Boolean isValidOverlap(int sector, int strip1, int paddle1, int strip2, int paddle2){
		if(strip1 > strip2)
		{
			int temp;
			temp = strip1;
			strip1 = strip2;
			strip2 = temp;
			
			temp = paddle1;
			paddle1 = paddle2;
			paddle2 = temp; 
		}
		
		if(strip1 == 0 && strip2 == 2)
		{
			//UW
			if(validoverlap[0][paddle1][paddle2] == 1) return true;
			else if(validoverlap[0][paddle1][paddle2] == -1) return false;
			else if(paddle1 <= 15 && paddle2 <= 15)
			{
				validoverlap[0][paddle1][paddle2] = -1;
	        	return false;
			}
			else
			{
				Object[] obj = getOverlapVerticies(sector, strip1, paddle1, strip2, paddle2);
				int numpoints = (int)obj[0];
				
		        if(numpoints > 2) 
		        {
		        	validoverlap[0][paddle1][paddle2] = 1;
		        	return true;
		        }
		        else
		        {
		        	validoverlap[0][paddle1][paddle2] = -1;
		        	return false;	
		        }
			}
		}
		else if(strip1 == 0 && strip2 == 1)
		{
			//UV
			if(validoverlap[1][paddle1][paddle2] == 1) return true;
			else if(validoverlap[1][paddle1][paddle2] == -1) return false;
			else if(paddle1 <= 15 && paddle2 <= 15)
			{
				validoverlap[1][paddle1][paddle2] = -1;
	        	return false;
			}
			else
			{
				Object[] obj = getOverlapVerticies(sector, strip1, paddle1, strip2, paddle2);
				int numpoints = (int)obj[0];
				
		        if(numpoints > 2) 
		        {
		        	validoverlap[1][paddle1][paddle2] = 1;
		        	return true;
		        }
		        else
		        {
		        	validoverlap[1][paddle1][paddle2] = -1;
		        	return false;	
		        }
			}
		}
		else if(strip1 == 1 && strip2 == 2)
		{
			//VW
			if(validoverlap[2][paddle1][paddle2] == 1) return true;
			else if(validoverlap[2][paddle1][paddle2] == -1) return false;
			else if(paddle1 <= 15 && paddle2 <= 15)
			{
				validoverlap[2][paddle1][paddle2] = -1;
	        	return false;
			}
			else
			{
				Object[] obj = getOverlapVerticies(sector, strip1, paddle1, strip2, paddle2);
				int numpoints = (int)obj[0];
				
		        if(numpoints > 2) 
		        {
		        	validoverlap[2][paddle1][paddle2] = 1;
		        	return true;
		        }
		        else
		        {
		        	validoverlap[2][paddle1][paddle2] = -1;
		        	return false;	
		        }
			}
		}
		else
		{
			System.err.println("Problem with isValidOverlap");
			return false;	
		}
			
	}
	
	//returns true if pixel is on very edge
	//i.e. lots of corner clipping events
	public Boolean isEdgePixel(int uPaddle, int vPaddle, int wPaddle)
	{
		Point2D tempp = null;
		double[] xtemp = new double[20];
		double[] ytemp = new double[20];
		double b;
		double slopeafter;
		int numpoints = 0;
		int ulaststrip,vlaststrip;
		int i,j,k;
		
		if(unit == 0)
		{
			ulaststrip = 67;
			vlaststrip = 61;
		}
		else
		{
			ulaststrip = 35;
			vlaststrip = 35;
		}
		
        if(uPaddle>=ulaststrip||vPaddle>=vlaststrip||wPaddle>=vlaststrip) return true;
		if(uPaddle != 0 && uPaddle != ulaststrip && vPaddle != 0 && vPaddle != vlaststrip && wPaddle != 0 && wPaddle != vlaststrip)
			return false;
		else
		{
			Object[] obj = getPixelVerticies(0, uPaddle, vPaddle, wPaddle);
			numpoints = (int)obj[0];
			if (numpoints<3) 
			{
				numpoints = 0;
				return true;
			}
			
			System.arraycopy( (double[])obj[1], 0, xtemp, 0, numpoints);
			System.arraycopy( (double[])obj[2], 0, ytemp, 0, numpoints);
		}
			
		
		if(uPaddle == 0)
		{
			Polygon2D ushape1 = new SimplePolygon2D();
			Polygon2D ushape2 = new SimplePolygon2D();
			for(i = 0; i < 4; ++i)
			{
				tempp = new Point2D(xPoint[0][0][0][i],yPoint[0][0][0][i]);
				ushape1.addVertex(tempp);
				tempp = new Point2D(xPoint[0][0][1][i],yPoint[0][0][1][i]);
				ushape2.addVertex(tempp);	
			}
			ushape1 = Polygons2D.union(ushape1,ushape2);
			
			for(k = 0; k < numpoints; ++k)
	        {
	        	for(i = 0; i < ushape1.vertexNumber(); ++i)
	        	{
	        		if(i == (ushape1.vertexNumber() - 1))
						j = 0;
					else 
						j = i + 1;
	        		if(Math.abs(ushape1.vertex(i).x() - ushape1.vertex(j).x()) < 0.01)
	        		{
	        			slopeafter = 999.0;
	        			if(Math.abs(xtemp[k] - ushape1.vertex(i).x()) < 0.06) return true;
	        		}
	        		else 
	        		{
	        			slopeafter = (ushape1.vertex(j).getY() - ushape1.vertex(i).getY())/(ushape1.vertex(j).getX() - ushape1.vertex(i).getX());
	        			b = ushape1.vertex(i).getY() - (slopeafter * ushape1.vertex(i).getX());
	        			if(Math.abs(ytemp[k] - (slopeafter * xtemp[k] + b)) < 0.06) return true;
	        		}
	        	}
	        }
			
			ushape1 = null;
			ushape2 = null;
				
		}
		else if(uPaddle == ulaststrip)
		{
			Polygon2D ushape1 = new SimplePolygon2D();
			Polygon2D ushape2 = new SimplePolygon2D();
			for(i = 0; i < 4; ++i)
			{
				tempp = new Point2D(xPoint[0][0][ulaststrip][i],yPoint[0][0][ulaststrip][i]);
				ushape1.addVertex(tempp);
				tempp = new Point2D(xPoint[0][0][ulaststrip-1][i],yPoint[0][0][ulaststrip-1][i]);
				ushape2.addVertex(tempp);	
			}
			ushape1 = Polygons2D.union(ushape1,ushape2);

			for(k = 0; k < numpoints; ++k)
	        {
	        	for(i = 0; i < ushape1.vertexNumber(); ++i)
	        	{
	        		if(i == (ushape1.vertexNumber() - 1))
						j = 0;
					else 
						j = i + 1;
	        		if(Math.abs(ushape1.vertex(i).x() - ushape1.vertex(j).x()) < 0.01)
	        		{
	        			slopeafter = 999.0;
	        			//double calcy = (slopeafter * xtemp[k] + b);
	        			//System.out.println("slope: " + slopeafter + " y-int: " + b);
	        			//System.out.println("x: " + xtemp[k] + " xvert: " + ushape1.vertex(i).x());
	        			if(Math.abs(xtemp[k] - ushape1.vertex(i).x()) < 0.06) return true;
	        			
	        		}
	        		else 
	        		{
	        			slopeafter = (ushape1.vertex(j).getY() - ushape1.vertex(i).getY())/(ushape1.vertex(j).getX() - ushape1.vertex(i).getX());
	        			b = ushape1.vertex(i).getY() - (slopeafter * ushape1.vertex(i).getX());
	        			//double calcy = (slopeafter * xtemp[k] + b);
	        			//System.out.println("slope: " + slopeafter + " y-int: " + b);
	        			//System.out.println("x: " + xtemp[k] + " y: " + ytemp[k] + " calculated y: " + calcy);
	        			if(Math.abs(ytemp[k] - (slopeafter * xtemp[k] + b)) < 0.06) return true;
	        		}
	        	}
	        }
	        
			ushape1 = null;
			ushape2 = null;
		}
		
		// test vstrips
		if(vPaddle == 0)
		{
			Polygon2D vshape1 = new SimplePolygon2D();
			Polygon2D vshape2 = new SimplePolygon2D();
			for(i = 0; i < 4; ++i)
			{
				tempp = new Point2D(xPoint[0][1][0][i],yPoint[0][1][0][i]);
				vshape1.addVertex(tempp);
				tempp = new Point2D(xPoint[0][1][1][i],yPoint[0][1][1][i]);
				vshape2.addVertex(tempp);	
			}
			vshape1 = Polygons2D.union(vshape1,vshape2);
					
			for(k = 0; k < numpoints; ++k)
	        {
	        	for(i = 0; i < vshape1.vertexNumber(); ++i)
	        	{
	        		if(i == (vshape1.vertexNumber() - 1))
						j = 0;
					else 
						j = i + 1;
	        		if(Math.abs(vshape1.vertex(i).x() - vshape1.vertex(j).x()) < 0.01)
	        		{
	        			slopeafter = 999.0;
	        			if(Math.abs(xtemp[k] - vshape1.vertex(i).x()) < 0.06) return true;
	        		}
	        		else 
	        		{
	        			slopeafter = (vshape1.vertex(j).getY() - vshape1.vertex(i).getY())/(vshape1.vertex(j).getX() - vshape1.vertex(i).getX());
	        			b = vshape1.vertex(i).getY() - (slopeafter * vshape1.vertex(i).getX());
	        			if(Math.abs(ytemp[k] - (slopeafter * xtemp[k] + b)) < 0.06) return true;
	        		}
	        	}
	        }
	        
			vshape1 = null;
			vshape2 = null;
				
		}
		else if(vPaddle == vlaststrip)
		{
			Polygon2D vshape1 = new SimplePolygon2D();
			Polygon2D vshape2 = new SimplePolygon2D();
			for(i = 0; i < 4; ++i)
			{
				tempp = new Point2D(xPoint[0][1][vlaststrip][i],yPoint[0][1][vlaststrip][i]);
				vshape1.addVertex(tempp);
				tempp = new Point2D(xPoint[0][1][vlaststrip-1][i],yPoint[0][1][vlaststrip-1][i]);
				vshape2.addVertex(tempp);	
			}
			vshape1 = Polygons2D.union(vshape1,vshape2);
			
			for(k = 0; k < numpoints; ++k)
	        {
	        	for(i = 0; i < vshape1.vertexNumber(); ++i)
	        	{
	        		if(i == (vshape1.vertexNumber() - 1))
						j = 0;
					else 
						j = i + 1;
	        		if(Math.abs(vshape1.vertex(i).x() - vshape1.vertex(j).x()) < 0.01)
	        		{
	        			slopeafter = 999.0;
	        			if(Math.abs(xtemp[k] - vshape1.vertex(i).x()) < 0.06) return true;
	        		}
	        		else 
	        		{
	        			slopeafter = (vshape1.vertex(j).getY() - vshape1.vertex(i).getY())/(vshape1.vertex(j).getX() - vshape1.vertex(i).getX());
	        			b = vshape1.vertex(i).getY() - (slopeafter * vshape1.vertex(i).getX());
	        			if(Math.abs(ytemp[k] - (slopeafter * xtemp[k] + b)) < 0.06) return true;
	        		}
	        	}
	        }
	        
			vshape1 = null;
			vshape2 = null;
		}
		
		
		//test w strips
		if(wPaddle == 0)
		{
			Polygon2D wshape1 = new SimplePolygon2D();
			Polygon2D wshape2 = new SimplePolygon2D();
			for(i = 0; i < 4; ++i)
			{
				tempp = new Point2D(xPoint[0][2][0][i],yPoint[0][2][0][i]);
				wshape1.addVertex(tempp);
				tempp = new Point2D(xPoint[0][2][1][i],yPoint[0][2][1][i]);
				wshape2.addVertex(tempp);	
			}
			wshape1 = Polygons2D.union(wshape1,wshape2);
					
			for(k = 0; k < numpoints; ++k)
	        {
	        	for(i = 0; i < wshape1.vertexNumber(); ++i)
	        	{
	        		if(i == (wshape1.vertexNumber() - 1))
						j = 0;
					else 
						j = i + 1;
	        		if(Math.abs(wshape1.vertex(i).x() - wshape1.vertex(j).x()) < 0.01)
	        		{
	        			slopeafter = 999.0;
	        			if(Math.abs(xtemp[k] - wshape1.vertex(i).x()) < 0.06) return true;
	        		}
	        		else 
	        		{
	        			slopeafter = (wshape1.vertex(j).getY() - wshape1.vertex(i).getY())/(wshape1.vertex(j).getX() - wshape1.vertex(i).getX());
	        			b = wshape1.vertex(i).getY() - (slopeafter * wshape1.vertex(i).getX());
	        			if(Math.abs(ytemp[k] - (slopeafter * xtemp[k] + b)) < 0.06) return true;
	        		}
	        	}
	        }
	        
			wshape1 = null;
			wshape2 = null;
				
		}
		else if(wPaddle == vlaststrip)
		{
			Polygon2D wshape1 = new SimplePolygon2D();
			Polygon2D wshape2 = new SimplePolygon2D();
			for(i = 0; i < 4; ++i)
			{
				tempp = new Point2D(xPoint[0][2][vlaststrip][i],yPoint[0][2][vlaststrip][i]);
				wshape1.addVertex(tempp);
				tempp = new Point2D(xPoint[0][2][vlaststrip-1][i],yPoint[0][2][vlaststrip-1][i]);
				wshape2.addVertex(tempp);	
			}
			wshape1 = Polygons2D.union(wshape1,wshape2);

			
	        /*
	        ///////////// Remove Duplicate Points /////////////////////////
			i = 0;
			j = 1;
			//System.out.println("start vertex: "+ wshape1.vertexNumber());
			while(i < wshape1.vertexNumber())
			{
				if(i == wshape1.vertexNumber() - 1)
					j = 0;
				else
					j = i + 1;
				if(Math.abs(wshape1.vertex(i).x() - wshape1.vertex(j).x()) < 0.001 && Math.abs(wshape1.vertex(i).y() - wshape1.vertex(j).y()) < 0.001)
				{
					wshape1.removeVertex(j);
					i = 0;
					j = 1;
					//System.out.println("removed vertex: "+ wshape1.vertexNumber());
				}
				else
				{
					++i;
				}
			}
			//System.out.println("final vertex: "+ wshape1.vertexNumber());
			///////////// Remove Duplicate Points /////////////////////////
			////////////Remove Redundant Co-linear Points ////////////////

								
			i = 0;
			while(i < wshape1.vertexNumber())
			{
				if(i == 0)
				{
					j = 1;
					k = wshape1.vertexNumber() - 1; 
				}
				else if(i == wshape1.vertexNumber() - 1)
				{
					k = i-1;
					j = 0;
				}
				else 
				{
					j = i + 1;
					k = i - 1;
				}
				//previous and current
				if(Math.abs(wshape1.vertex(i).x() - wshape1.vertex(k).x()) < 0.001) slopebefore = 999.0;
				else slopebefore = (wshape1.vertex(k).y() - wshape1.vertex(i).y())/(wshape1.vertex(k).x() - wshape1.vertex(i).x());
				
				//current and next
				if(Math.abs(wshape1.vertex(i).x() - wshape1.vertex(j).x()) < 0.001) slopeafter = 999.0;
				else slopeafter = (wshape1.vertex(j).y() - wshape1.vertex(i).y())/(wshape1.vertex(j).x() - wshape1.vertex(i).x());
				
				if(Math.abs(slopeafter - slopebefore) < 0.001)
				{
					wshape1.removeVertex(i);
					i = 0;
				}
				else
				{
					++i;
				}
			}
			
			System.out.println("Last two w strips "+ wshape1.vertexNumber());
			for(i = 0; i < wshape1.vertexNumber(); ++i)
			{
				System.out.println("x: "+ wshape1.vertex(i).x() + " y: " + wshape1.vertex(i).y());
			}
			
			////////////Remove Redundant Co-linear Points ////////////////
			*/
	        for(k = 0; k < numpoints; ++k)
	        {
	        	//System.out.println(k);
	        	for(i = 0; i < wshape1.vertexNumber(); ++i)
	        	{
	        		if(i == (wshape1.vertexNumber() - 1))
						j = 0;
					else 
						j = i + 1;
	        		
	        		//System.out.println(wshape1.vertex(i).x());
	        		//System.out.println(wshape1.vertex(j).x());
	        		if(Math.abs(wshape1.vertex(i).x() - wshape1.vertex(j).x()) < 0.01)
	        		{
	        			slopeafter = 999.0;
	        			if(Math.abs(xtemp[k] - wshape1.vertex(i).x()) < 0.06) return true;
	        		}
	        		else 
	        		{
	        			slopeafter = (wshape1.vertex(j).getY() - wshape1.vertex(i).getY())/(wshape1.vertex(j).getX() - wshape1.vertex(i).getX());
	        			b = wshape1.vertex(i).getY() - (slopeafter * wshape1.vertex(i).getX());
	        			//double calcy = (slopeafter * xtemp[k] + b);
	        			//System.out.println("slope: " + slopeafter + " y-int: " + b);
	        			//System.out.println("x: " + xtemp[k] + " y: " + ytemp[k] + " calculated y: " + calcy);
	        			if(Math.abs(ytemp[k] - (slopeafter * xtemp[k] + b)) < 0.06) return true;
	        		}
	        	}//
	        }
	        
			wshape1 = null;
			wshape2 = null;
		}
				

		return false;
	}
	
	//returns true if overlap is on very edge
	//i.e. lots of corner clipping events
	//                             0,1,2      0-67 or 0-61  0,1,2     0-67 or 0-61
	public Boolean isEdgeOverlap(int strip1, int paddle1, int strip2, int paddle2)
	{
		//make strip1 either u or v
		if(strip1 > strip2)
		{
			int temp;
			temp = strip1;
			strip1 = strip2;
			strip2 = temp;
			
			temp = paddle1;
			paddle1 = paddle2;
			paddle2 = temp; 
		}
		if(unit == 0) // pcal
		{
			if(strip1 == 0)
			{
				//u strip
				if(paddle1 == 0 || paddle1 == 67)	
					return true;
			}
			else
			{
				//v or w strip
				if(paddle1 == 0 || paddle1 == 61)	
					return true;
			}
			
			//strip2 is either v or w
			if(paddle2 == 0 || paddle2 == 61)	
				return true;
		}
		else //ec
		{
			if(paddle1 == 0 || paddle1 == 35)	
				return true;
			if(paddle2 == 0 || paddle2 == 35)	
				return true;
		}
		return false;
	}
	
	//returns an Object array of size 3
	//first element is the number of vertices (n) (int)
	//second element is an array x-coordinates (double[]) of size n
	//third element is an array y-coordinates (double[]) of size n
	//                                     0-5         0-67         0-61          0-61
	public Object[] getPixelVerticies(int sector, int uPaddle, int vPaddle, int wPaddle){
		
		if(isValidOverlap(sector, 0, uPaddle,2,wPaddle))// && isValidOverlap(sector, 0, uPaddle,1,vPaddle) && isValidOverlap(sector, 1, vPaddle,2,wPaddle))
		{
			Object[] obj = getVerticies(getOverlapShape(sector, 0, uPaddle,2,wPaddle),getStripShape(sector, 1,vPaddle));
			
			int numpoints = (int)obj[0];
			double[] x = new double[numpoints];
			double[] y = new double[numpoints];
			System.arraycopy( (double[])obj[1], 0, x, 0, numpoints);
			System.arraycopy( (double[])obj[2], 0, y, 0, numpoints);
			if(numpoints < 3) validpixel[uPaddle][vPaddle][wPaddle] = -1;
			else validpixel[uPaddle][vPaddle][wPaddle] = 1;
			return(new Object[]{numpoints, x, y});
		}
		else
		{
			validpixel[uPaddle][vPaddle][wPaddle] = -1;
			return(new Object[]{0, 0.0, 0.0});
		}
		
	}
	
	
	//returns an Object array of size 3
	//first element is the number of vertices (n) (int)
	//second element is an array x-coordinates (double[]) of size n
	//third element is an array y-coordinates (double[]) of size n

	public Object[] getOverlapVerticies(int sector, int strip1, int paddle1, int strip2, int paddle2){
	
		if(strip1 > strip2)
		{
			int temp;
			temp = strip1;
			strip1 = strip2;
			strip2 = temp;
			
			temp = paddle1;
			paddle1 = paddle2;
			paddle2 = temp; 
		}
		
		Object[] obj = getVerticies(getStripShape(sector, strip1, paddle1),getStripShape(sector, strip2, paddle2));
		
		int numpoints = (int)obj[0];
		//System.out.println("Strip let: " + strip1 + "Strip num: " + paddle1 + " Numpoints: " + numpoints);
		
		if(numpoints > 2)
		{
			double[] x = new double[numpoints];
			double[] y = new double[numpoints];
			System.arraycopy( (double[])obj[1], 0, x, 0, numpoints);
			System.arraycopy( (double[])obj[2], 0, y, 0, numpoints);
		
			return(new Object[]{numpoints, x, y});
		}
		else
		{
			return(new Object[]{numpoints, 0.0, 0.0});
		}
	}

	public Object[] getStripVerticies(int sector, int strip1, int paddle1){
		int numpoints = 4;
		int l = 0;
		
		if(strip1 >= 0 || strip1 < 3)
		{
			l = strip1;
		}
		else
			System.err.println("Error in calling getStripVerticies");
        
        return(new Object[]{numpoints, 
        		               xPoint[sector][l][paddle1], 
        		               yPoint[sector][l][paddle1],
        		               zPoint[sector][l][paddle1]});
	}
	
	//estimates the shape center by calculating average x, y, z
	//from all vertices in the shape

	public double[] getShapeCenter(DetectorShape2D shape){
		double[] center = new double[3];
		int numpoints = shape.getShapePath().size();
		Point3D[] points = new Point3D[numpoints];
		Point3D centerp;
		for(int i = 0; i < numpoints; ++i)
		{
			points[i] = shape.getShapePath().point(i);
		}
		centerp = Point3D.average(points);
		center[0] = centerp.x();
		center[1] = centerp.y();
		center[2] = centerp.z();
		
		return center;
	}
	
	//assuming PMT is right at edge of PCAL
	//there is actually tens of centimeters of fibers between.

	public double[] getPMTLocation(int strip1, int paddle1){
		double[] center = new double[3];
		
		int uPaddle = -1;
		int vPaddle = -1;
		int wPaddle = -1;
		
        double[] xyz1 = new double[3];
        double[] xyz2 = new double[3];
        double[] xyz3 = new double[3];
        Point3D dist = new Point3D();
        String[] spmt = {"u","v","v"};
        int[]   nspmt = {0,1,1}; 
        //PCAL w is read out the side of u(0)
        //EC inner w is read out the side of v(1)
        //EC outer w is read out the side of v(1)
		
		
        if(strip1 == 0)
		{
			uPaddle = paddle1;
		}
		else if(strip1 == 1)
		{
			vPaddle = paddle1;
		}
		else if(strip1 == 2)
		{
			wPaddle = paddle1;
		}
		else
		{
			System.err.println("Wrong input used for getPMTLocation: layers");
		}
		
		//case 1: U strip
		if(uPaddle != -1)
		{ 
			if(paddle1 != 0)
			{
				xyz1 = getShapeCenter(getOverlapShape(0, 0, paddle1, 2, numstrips[2]-1)); //last strip
				xyz2 = getShapeCenter(getOverlapShape(0, 0, paddle1, 2, numstrips[2]-2)); //second to last
			}
			else
			{
				//corner pixel have to do some funny business
				xyz1 = getShapeCenter(getOverlapShape(0, 0, paddle1, 2, numstrips[2]-1)); //last strip
				
				xyz2 = getShapeCenter(getOverlapShape(0, 0, 1, 2, numstrips[2]-2)); //second to last
				xyz3 = getShapeCenter(getOverlapShape(0, 0, 1, 2, numstrips[2]-1)); //second to last
				
				xyz2[0] = xyz1[0] + xyz2[0]-xyz3[0];
				xyz2[1] = xyz1[1] + xyz2[1]-xyz3[1];
				xyz2[2] = xyz1[2] + xyz2[2]-xyz3[2];
			}
			for(int i = 0; i < 3; ++i)
			{
				xyz1[i] = xyz1[i] + (xyz1[i] - xyz2[i])/2.0;
			}
			dist.set(xyz1[0],xyz1[1],xyz1[2]);		                
		}
		//case 2: V strip
		else if(vPaddle != -1)
		{ 
			if(paddle1 != 0)
			{
				xyz1 = getShapeCenter(getOverlapShape(0, 1, paddle1, 0, numstrips[0]-1)); //last strip
				xyz2 = getShapeCenter(getOverlapShape(0, 1, paddle1, 0, numstrips[0]-2)); //second to last
			}
			else
			{
				//corner pixel have to do some funny business
				xyz1 = getShapeCenter(getOverlapShape(0, 1, paddle1, 0, numstrips[0]-1)); //last strip
				
				xyz2 = getShapeCenter(getOverlapShape(0, 1, 1, 0, numstrips[0]-2)); //second to last
				xyz3 = getShapeCenter(getOverlapShape(0, 1, 1, 0, numstrips[0]-1)); //second to last
				
				xyz2[0] = xyz1[0] + xyz2[0]-xyz3[0];
				xyz2[1] = xyz1[1] + xyz2[1]-xyz3[1];
				xyz2[2] = xyz1[2] + xyz2[2]-xyz3[2];
			}
			for(int i = 0; i < 3; ++i)
			{
				xyz1[i] = xyz1[i] + (xyz1[i] - xyz2[i])/2.0;
			}
			dist.set(xyz1[0],xyz1[1],xyz1[2]);	
            
		}
		//case 3: W strip
		else if(wPaddle != -1)
		{   
			if(paddle1 != 0)
			{
				xyz1 = getShapeCenter(getOverlapShape(0, 2, paddle1, nspmt[unit], numstrips[nspmt[unit]]-1)); //last strip
				xyz2 = getShapeCenter(getOverlapShape(0, 2, paddle1, nspmt[unit], numstrips[nspmt[unit]]-2)); //second to last
			}
			else
			{
				//corner pixel have to do some funny business
				xyz1 = getShapeCenter(getOverlapShape(0, 2, paddle1, nspmt[unit], numstrips[nspmt[unit]]-1)); //last strip
				
				xyz2 = getShapeCenter(getOverlapShape(0, 2, 1, nspmt[unit], numstrips[nspmt[unit]]-2)); //second to last
				xyz3 = getShapeCenter(getOverlapShape(0, 2, 1, nspmt[unit], numstrips[nspmt[unit]]-1)); //second to last
				
				xyz2[0] = xyz1[0] + xyz2[0]-xyz3[0];
				xyz2[1] = xyz1[1] + xyz2[1]-xyz3[1];
				xyz2[2] = xyz1[2] + xyz2[2]-xyz3[2];
			}
			for(int i = 0; i < 3; ++i)
			{
				xyz1[i] = xyz1[i] + (xyz1[i] - xyz2[i])/2.0;
			}
			dist.set(xyz1[0],xyz1[1],xyz1[2]);	

		}
		
		center[0] = dist.x();
		center[1] = dist.y();
		center[2] = 0.0;
		
		return center;
	}
	
	//get attenuation distance
	
	public double getUPixelDistance(int uPaddle, int vPaddle, int wPaddle){
		double distance = 0;
		double[] shapecenter = new double[3];
		double[] PMTloc = new double[3];
		
		
		shapecenter = getShapeCenter(getPixelShape(0,uPaddle,vPaddle,wPaddle));
		PMTloc = getPMTLocation(0, uPaddle);
		
		distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2));
		//distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2) + Math.pow(shapecenter[2] - PMTloc[2],2));
		
		return distance;
	}
	
	//get attenuation distance
	public double getVPixelDistance(int uPaddle, int vPaddle, int wPaddle){
		double distance = 0;
		double[] shapecenter = new double[3];
		double[] PMTloc = new double[3];
		
		
		shapecenter = getShapeCenter(getPixelShape(0,uPaddle,vPaddle,wPaddle));
		PMTloc = getPMTLocation(1, vPaddle);
		
		distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2));
		//distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2) + Math.pow(shapecenter[2] - PMTloc[2],2));
		
		return distance;
	}
	
	//get attenuation distance

	public double getWPixelDistance(int uPaddle, int vPaddle, int wPaddle){
		double distance = 0;
		double[] shapecenter = new double[3];
		double[] PMTloc = new double[3];
		
		
		shapecenter = getShapeCenter(getPixelShape(0,uPaddle,vPaddle,wPaddle));
		PMTloc = getPMTLocation(2, wPaddle);
		
		distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2));
		//distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2) + Math.pow(shapecenter[2] - PMTloc[2],2));
		
		return distance;
	}
	
	

	
	//get attenuation distance
	//                              main strip let,    main num, cross strip let, cross num
	//                               0,1,2:u,v,w        0-67   0,1,2:u,v,w       0-61
	public double getOverlapDistance(int strip1, int paddle1, int strip2, int paddle2){
		double distance = 0;
		double[] shapecenter = new double[3];
		double[] PMTloc = new double[3];
	

		System.arraycopy( (double[])getShapeCenter(getOverlapShape(0, strip1, paddle1, strip2, paddle2)), 0, shapecenter, 0, 3);
		System.arraycopy( (double[])getPMTLocation(strip1, paddle1), 0, PMTloc, 0, 3);
		
		distance = Math.sqrt(Math.pow(shapecenter[0] - PMTloc[0],2) + Math.pow(shapecenter[1] - PMTloc[1],2) + Math.pow(shapecenter[2] - PMTloc[2],2));
		
		return distance;
	}
	
	
	
  	//crossstrip needs to be 1-62 or 1-68 not 0
	//returns a strip number in element 0
	// and half the strip bin 0.5 for singles and 1 for doubles in element 1
    public double[] CalcDistinStrips(char stripletter, int crossstrip)
    {
  	  double x=0;
  	  double xE = 0.0;
        if(stripletter == 'u' || stripletter == 'U')
        {
            if(crossstrip <= 15)
            {
                //converts to 77 strips
                x = 2.0* crossstrip - 1.0;
                xE = 1.0;
            }
            else if(crossstrip > 15)
            {
                //converts to 77 strips
                x = (30.0 + (crossstrip - 15.0)) - 0.5;
                xE = 1.0/2.0;
            }
        }
        else if(stripletter == 'v' || stripletter == 'w' || stripletter == 'V' || stripletter == 'W')
        {
            if(crossstrip <= 52)
            {
                //converts to 84 strips
                x = crossstrip - 0.5;
                xE = 1.0/2.0;
                }
                else if(crossstrip > 52)
                {
                    //converts to 84 strips
                    x = (52.0 + 2.0*(crossstrip - 52.0)) - 1.0;
                    xE = 1.0;
                }
        }
        return new double[] {x, xE};
    }
	
    //xdistance needs to be 1-77 or 1-84 not 0
    //meant for use with the output of CalcDistinStrips

    public double[] CalcDistance(char stripletter, double xdistance, double xdistanceE)
    {
        
        if(stripletter == 'u' || stripletter == 'U')
        {
            //convert strip number to distance
            xdistance = Math.abs(xdistance - 77.0) * anglewidth;
            xdistanceE = xdistanceE * anglewidth;
        }
        else if(stripletter == 'v' || stripletter == 'w' || stripletter == 'V' || stripletter == 'W')
        {
            //convert strip number to distance
            xdistance = Math.abs(xdistance - 84.0) * anglewidth;
            xdistanceE = xdistanceE * anglewidth;
        }
        return new double[] {xdistance, xdistanceE};
    }
    
    
	public float uvw_dalitz(int ic, int ip, int il) {
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
	
	public DetectorShape2D scaleShape(DetectorShape2D shape, double scalefactor)
	{
		double[] shapecenter = new double[3];
		
		//find center
		shapecenter = getShapeCenter(shape);
		
		//translate center to zero
		for(int i = 0; i < shape.getShapePath().size(); ++i)
		{
			shape.getShapePath().point(i).translateXYZ(-shapecenter[0], -shapecenter[1], -shapecenter[2]);
		}
		
		//apply scale factor
		for(int i = 0; i < shape.getShapePath().size(); ++i)
		{
			shape.getShapePath().point(i).set(shape.getShapePath().point(i).x()*scalefactor,shape.getShapePath().point(i).y()*scalefactor,shape.getShapePath().point(i).z()*scalefactor); 
		}
		
		//translate center back to old center
		for(int i = 0; i < shape.getShapePath().size(); ++i)
		{
			shape.getShapePath().point(i).translateXYZ(shapecenter[0], shapecenter[1], shapecenter[2]);
		}
		
		
		return shape;
	}
	
	public DetectorShape2D trimShape(DetectorShape2D shapeA, DetectorShape2D shapeB)
	{
		DetectorShape2D shapeC = new DetectorShape2D();
		
		Object[] obj = getVerticies(shapeA,shapeB);
		
		int numpoints = (int)obj[0];

		if(numpoints > 2)
		{
			double[] x = new double[numpoints];
			double[] y = new double[numpoints];
			System.arraycopy( (double[])obj[1], 0, x, 0, numpoints);
			System.arraycopy( (double[])obj[2], 0, y, 0, numpoints);
		
			for(int i = 0; i < numpoints; ++i)
			{
				shapeC.getShapePath().addPoint(x[i], y[i], 0.0);
			}
			
			return(shapeC);
		}
		else
		{
			System.out.println("WHY!!!!");
			return(null);
		}
		
	}
	
	private void initPCALVert(ECDetector detector) {
		ECLayer  ecLayer;
		Point3D point1 = new Point3D();
		int[] vertices = {0,4,5,1};
		int suplay = unit; //PCAL ==0, ECinner ==1, ECouter==2 
		
//        ECDetector detector  = new ECFactory().createDetectorTilted(DataBaseLoader.getGeometryConstants(DetectorType.EC, 10, "default"));
        
        for(int sector = 0; sector < 6; ++sector) {
		    for(int l = 0; l<3; l++) {    	
		    	int off = suplay==0 ? DetectorLayer.PCAL_Z : (suplay==1 ? DetectorLayer.EC_INNER_Z : DetectorLayer.EC_OUTER_Z);
		    	ecLayer = detector.getSector(sector).getSuperlayer(suplay).getLayer(l+off);
        		numstrips[l] = ecLayer.getNumComponents();
	        	int n = 0;
		    	for(ScintillatorPaddle paddle1 : ecLayer.getAllComponents()) {
		    		for(int j=0; j<4 ; j++) {
		    			point1.copy(paddle1.getVolumePoint(vertices[j]));
		    			point1.rotateZ(sector * Math.PI/3.0);
		    			point1.translateXYZ(333.1042, 0.0, 0.0);
		    			xPoint[sector][l][n][j] =  point1.x();
		    			yPoint[sector][l][n][j] = -point1.y(); // why minus sign?
		    			zPoint[sector][l][n][j] =  point1.z();
		    		}
	    			n++;
		    	}
		    }
        }
	}
		
	private void initECVert(ECDetector detector)
	// Projective geometry means U,V,W strips get progressively larger with depth.  For
	// display purposes V,W vertices are projected back to the first U strip plane.
	{
		ECLayer  ecLayer;
		int sector = 0;
		int suplay = unit; //PCAL ==0, ECinner ==1, ECouter==2 
		int i, currentpad, nextpad;
		double uwidth = 0, vwidth = 0, wwidth = 0;
		double udist = 0, vdist = 0, wdist = 0;
		double xshift = 0.0, yshift = 0.0;
		double A = 0.0, B = 0.0, C = 0.0;
		double a = 0.0, b = 0.0, c = 0.0;
		Point3D point1 = new Point3D();
		Polygon2D poly1 = new SimplePolygon2D();
		Point2D minxpoint = new Point2D(999.0,999.0);
		Point2D minypoint = new Point2D(999.0,999.0);
		Point2D maxypoint = new Point2D(999.0,-999.0);
		DetectorShape2D shape = new DetectorShape2D();
		DetectorShape2D shape2 = new DetectorShape2D();
		ScintillatorPaddle paddle = null;
//        ECDetector detector  = new ECFactory().createDetectorTilted(DataBaseLoader.getGeometryConstants(DetectorType.EC, 10, "default"));
        for(sector = 0; sector < 6; ++sector)
        {
        	//loop through u, v, w layers
		    for(int l = 0; l <3; l++)
		    {    	
	        	//	                                             PCAL ==0         u,v,w
		    	int off = suplay==0 ? DetectorLayer.PCAL_Z : (suplay==1 ? DetectorLayer.EC_INNER_Z : DetectorLayer.EC_OUTER_Z);
		    	ecLayer = detector.getSector(sector).getSuperlayer(suplay).getLayer(l+off);
	        	
	        	//get number of l strips
        		numstrips[l] = ecLayer.getNumComponents();
        		
	        	//get outer shape...outer triangle
	        	if(l == 0 && sector == 0)
	        	{
	        		//first paddle of l layer
	        		paddle = ecLayer.getComponent(0); 
	        		paddle.toString();
	        		//point1
	        		point1.copy(paddle.getVolumePoint(0));
	        		poly1.addVertex(new Point2D(point1.x(),  point1.y())); 
	        		//point2
	        		point1.copy(paddle.getVolumePoint(4));
	        		poly1.addVertex(new Point2D(point1.x(),  point1.y())); 
	        		//point3
	        		point1.copy(paddle.getVolumePoint(5));
	        		poly1.addVertex(new Point2D(point1.x(),  point1.y()));  
	        		//point4
	        		point1.copy(paddle.getVolumePoint(1));
	        		poly1.addVertex(new Point2D(point1.x(),  point1.y())); 
	        		
	        		
	        		//get whole triangle shape for l = 0
	        		for(ScintillatorPaddle paddle1 : ecLayer.getAllComponents())
		            {
	        			Polygon2D poly2 = new SimplePolygon2D();
		        		//point1
		        		point1.copy(paddle1.getVolumePoint(0));
		        		poly2.addVertex(new Point2D(point1.x(),  point1.y())); 
		        		//point2
		        		point1.copy(paddle1.getVolumePoint(4));
		        		poly2.addVertex(new Point2D(point1.x(),  point1.y())); 
		        		//point3
		        		point1.copy(paddle1.getVolumePoint(5));
		        		poly2.addVertex(new Point2D(point1.x(),  point1.y()));
		        		//point4
		        		point1.copy(paddle1.getVolumePoint(1));
		        		poly2.addVertex(new Point2D(point1.x(),  point1.y())); 
		        		
		        		poly1 = Polygons2D.union(poly1,poly2);
		        		
		        		poly2 = null;
		            }
	        		
	        		//define triangle
	        		for(i = 0; i < poly1.vertexNumber(); ++ i)
	        		{
	        			if(poly1.vertex(i).x() < minxpoint.x()) minxpoint = Point2D.create(poly1.vertex(i));
	        			if(poly1.vertex(i).y() < minypoint.y()) minypoint = Point2D.create(poly1.vertex(i));
	        			if(poly1.vertex(i).y() > maxypoint.y()) maxypoint = Point2D.create(poly1.vertex(i));
	        			//System.out.println("Point #: " + i + " x: " + poly1.vertex(i).x() + " y: " + poly1.vertex(i).y());
	        		}
	        		shape2.getShapePath().clear();
	        		shape2.getShapePath().addPoint(minxpoint.x(),minxpoint.y(), 0.0); 
	        		shape2.getShapePath().addPoint(minypoint.x(),minypoint.y(), 0.0); 
	        		shape2.getShapePath().addPoint(maxypoint.x(),maxypoint.y(), 0.0);
	        		
	        		
	        		//get triangle edge lengths 
	        		A = minxpoint.distance(minypoint);
	        		B = minypoint.distance(maxypoint);
	        		C = maxypoint.distance(minxpoint);
	        		
	        		//get triangle angles
	        		//law of cosines
	        		// C*C = A*A + B*B - 2.0*A*B*Math.cos(c)  (in radians)
	        		// Math.acos((C*C - A*A - B*B)/(-2.0*A*B)) = c  (in radians)
	        		a = Math.acos((A*A - C*C - B*B)/(-2.0*C*B));
	        		b = Math.acos((B*B - A*A - C*C)/(-2.0*A*C));
	        		c = Math.acos((C*C - A*A - B*B)/(-2.0*A*B));
	        		
	        		//get perpendicular distance to corners
	        		udist = A * Math.sin(c);
	        		vdist = B * Math.sin(a);
	        		wdist = B * Math.sin(c);
	        		
	        	}
	        	
	        	//get strips widths and shifts for each layer
	        	if(l == 0)
	        	{
	        		if(unit == 0) uwidth = udist/(double)(numstrips[l] + 16);
	        		else uwidth = udist/(double)numstrips[l];
	        		//shift starting from last strip
	        		xshift = -uwidth; //per strip
	        		yshift = 0.0;
	        	}
	        	if(l == 1)
	        	{
	        		if(unit == 0) vwidth = vdist/(double)(numstrips[l] + 15);
	        		else vwidth = vdist/(double)numstrips[l];
	        		xshift = vwidth * Math.cos(a);
	        		yshift = -vwidth * Math.sin(a);
	        	}
	        	if(l == 2)
	        	{
	        		if(unit == 0) wwidth = wdist/(double)(numstrips[l] + 15);
	        		else wwidth = wdist/(double)numstrips[l];
	        		xshift = wwidth * Math.cos(a);
	        		yshift = wwidth * Math.sin(a);
	        	}
	        	
	        	//System.out.println("numstrips: " + numstrips[l]);
	        	
	        	//System.out.println("udist: " + udist);
	        	//System.out.println("vdist: " + vdist);
	        	//System.out.println("wdist: " + wdist);
	        	
	        	//System.out.println("uwidth: " + uwidth);
	        	//System.out.println("vwidth: " + vwidth);
	        	//System.out.println("wwidth: " + wwidth);
	        	
	        	//System.out.println("xshift: " + xshift);
	        	//System.out.println("yshift: " + yshift);
	        	for(i = 0; i < numstrips[l]; ++i)
	            {		     //0-67 PCAL-u
	        				 //0-61 PCAL-v
	        		         //0-61 PCAL-w
	        				 //0-35 EC-u,v,w
	        		

	        		shape.getShapePath().clear(); 
	        		//u
	        		if(l == 0)
	        		{
	        			if(unit == 0 && i > 51)
	        			{
	        				currentpad = (numstrips[l] - 1 - i) * 2; 
	        				nextpad = (numstrips[l] - i) * 2; 
	        			}
	        			else if(unit == 0)
	        			{
	        				currentpad = numstrips[l] - 1 - i + 16; 
	        				nextpad = numstrips[l] - i + 16;
	        			}
	        			else
	        			{
	        				currentpad = numstrips[l] - 1 - i; 
	        				nextpad = numstrips[l] - i;
	        			}
		        		//point1
		        		shape.getShapePath().addPoint(minypoint.x() + currentpad*xshift,  minypoint.y(),  0.0); 
		        		//point2
		        		shape.getShapePath().addPoint(maxypoint.x() + currentpad*xshift,  maxypoint.y(),  0.0); 
		        		//point3
		        		shape.getShapePath().addPoint(maxypoint.x() + nextpad*xshift,  maxypoint.y(),  0.0); 
		        		//point4
		        		shape.getShapePath().addPoint(minypoint.x() + nextpad*xshift,  minypoint.y(),  0.0); 
	        		
		        		/*
		        		//testing
		        		if(sector == 0)
		        		{
		        			xtemp = minypoint.x() + currentpad*xshift;
		        			ytemp = minypoint.y() + currentpad*yshift;
		        			System.out.println("x: " + xtemp + " y: " + ytemp);
		        		}
		        		*/
	        		}
	        		//v
	        		if(l == 1)
	        		{
	        			if(i > 14 && unit == 0)
	        			{
	        				//62
	        				currentpad = (numstrips[l] - 1 - i); 
	        				nextpad = (numstrips[l] - i); 
	        			}
	        			else if(unit == 0)
	        			{
	        				currentpad = numstrips[l] + 15 - 2 - i*2; 
	        				nextpad = numstrips[l] + 15 - i*2;
	        			}
	        			else
	        			{
	        				currentpad = numstrips[l] - 1 - i; 
	        				nextpad = numstrips[l] - i;
	        			}
		        		//point1
		        		shape.getShapePath().addPoint(minxpoint.x() + currentpad*xshift,  minxpoint.y() + currentpad*yshift,  0.0); 
		        		//point2
		        		shape.getShapePath().addPoint(maxypoint.x() + currentpad*xshift,  maxypoint.y() + currentpad*yshift,  0.0); 
		        		//point3
		        		shape.getShapePath().addPoint(maxypoint.x() + nextpad*xshift,  maxypoint.y() + nextpad*yshift,  0.0); 
		        		//point4
		        		shape.getShapePath().addPoint(minxpoint.x() + nextpad*xshift,  minxpoint.y() + nextpad*yshift,  0.0); 
	        		}
	        		//w
	        		if(l == 2)
	        		{
	        			if(i > 14 && unit == 0)
	        			{
	        				//62
	        				currentpad = (numstrips[l] - 1 - i); 
	        				nextpad = (numstrips[l] - i); 
	        			}
	        			else if(unit == 0)
	        			{
	        				currentpad = numstrips[l] + 15 - 2 - i*2; 
	        				nextpad = numstrips[l] + 15 - i*2;
	        			}
	        			else
	        			{
	        				currentpad = numstrips[l] - 1 - i; 
	        				nextpad = numstrips[l] - i;
	        			}
		        		//point1
		        		shape.getShapePath().addPoint(minxpoint.x() + currentpad*xshift,  minxpoint.y() + currentpad*yshift,  0.0); 
		        		//point2
		        		shape.getShapePath().addPoint(minypoint.x() + currentpad*xshift,  minypoint.y() + currentpad*yshift,  0.0); 
		        		//point3
		        		shape.getShapePath().addPoint(minypoint.x() + nextpad*xshift,  minypoint.y() + nextpad*yshift,  0.0); 
		        		//point4
		        		shape.getShapePath().addPoint(minxpoint.x() + nextpad*xshift,  minxpoint.y() + nextpad*yshift,  0.0); 
		        		
		        		/*
		        		//testing
		        		if(sector == 0)
		        		{
		        			xtemp = minxpoint.x() + currentpad*xshift;
		        			ytemp = minxpoint.y() + currentpad*yshift;
		        			System.out.println("x: " + xtemp + " y: " + ytemp);
		        		}
		        		*/
		        		
	        		}
	        		
	        		DetectorShape2D shape3 = new DetectorShape2D();
	        		shape3 = this.trimShape(shape, shape2);
	        		if(shape3.getShapePath().size() < 4) shape3.getShapePath().addPoint(shape3.getShapePath().point(2));

	        		for(int j = 0; j < shape3.getShapePath().size(); ++j)
	        		{
	        			point1.copy(shape3.getShapePath().point(j));
	        			
	        			//push unit to the right to make room for beam line
		        		point1.translateXYZ(333.1042, 0.0, 0.0);
		        		
		        		//rotate to appropriate sector
		            	point1.rotateZ(sector * Math.PI/3.0);
		            	
		            	//save points
		            	xPoint[sector][l][i][j] = point1.x();
		            	yPoint[sector][l][i][j] = point1.y();
		            	zPoint[sector][l][i][j] = point1.z();
	        		}
	            }
	        }
        }
	}
	
	public Object[] getVerticies(DetectorShape2D shape1, DetectorShape2D shape2){
		int nPoints = 0;
		
		int vert1size = shape1.getShapePath().size();
		int vert2size = shape2.getShapePath().size();
		
		double[] x = new double[vert1size * vert2size];
		double[] y = new double[vert1size * vert2size];
		
		double[] x2 = new double[vert1size * vert2size];
		double[] y2 = new double[vert1size * vert2size];
		
		double[] xtemp1 = new double[vert1size];
		double[] ytemp1 = new double[vert1size];
		
		double[] xtemp2 = new double[vert2size];
		double[] ytemp2 = new double[vert2size];
 
		for(int i = 0; i < vert1size; ++i)
		{
			xtemp1[i] = shape1.getShapePath().point(i).x();
			ytemp1[i] = shape1.getShapePath().point(i).y();
		}
		
		
		for(int i = 0; i < vert2size; ++i)
		{
			xtemp2[i] = shape2.getShapePath().point(i).x();
			ytemp2[i] = shape2.getShapePath().point(i).y();
		}
		
		
		////////////////// Find Intersection //////////////////////////
		
		SimplePolygon2D pol1 = new SimplePolygon2D(xtemp1,ytemp1);
		SimplePolygon2D pol2 = new SimplePolygon2D(xtemp2,ytemp2);
		//System.out.println("area: " + Polygons2D.computeArea(pol1));
		//System.out.println("Start overlap");
		Polygon2D pol3 = Polygons2D.intersection(pol1,pol2);
		
		///////////// Remove Duplicate Points /////////////////////////
		//nPoints = pol3.vertexNumber();
		int count = 0;
		Boolean duplicate = false;
		for(int i = 0; i < pol3.vertexNumber(); ++i)
		{			
			if(i == 0)
			{
				x[count] = pol3.vertex(i).getX();
				y[count] = pol3.vertex(i).getY();
				++count;
			}
			else
			{
				for(int j = 0; j < i; ++j)
				{
					if(Math.abs(pol3.vertex(i).getX() - x[j]) < 0.000001 && Math.abs(pol3.vertex(i).getY() - y[j]) < 0.0001)
					{
						duplicate = true;
					}
				}
				if(!duplicate)
				{
					x[count] = pol3.vertex(i).getX();
					y[count] = pol3.vertex(i).getY();
					++count;
				}
				duplicate = false;
			}
			
			//System.out.println("x: " + pol3.vertex(i).getX() + " y: " + pol3.vertex(i).getY());
		}
		
		///////////////// Record number of points /////////////////////
				
		if(count > 2)
			nPoints = count;
		else
			nPoints = 0;

		//////////// Remove Redundant Co-linear Points ////////////////
		
		int count2 = 0;
		double slopebefore, slopeafter;
		Boolean colinear = false;
		if(nPoints > 2)
		{
			for(int i = 0; i < nPoints; ++i)
			{			
				if(i == 0)
				{
					//test slope between 
					     	 
					//last and zeroth point
					if(Math.abs(x[i] - x[nPoints - 1]) < 0.000001) slopebefore = 999.0;
					else slopebefore = (y[nPoints - 1] - y[i])/(x[nPoints - 1] - x[i]);
					
					//current and next
					if(Math.abs(x[i] - x[i + 1]) < 0.000001) slopeafter = 999.0;
					else slopeafter = (y[i + 1] - y[i])/(x[i + 1] - x[i]);
				}
				else if(i == nPoints -1)
				{
					//test slope between 
				     
					//previous and current
					if(Math.abs(x[i] - x[i - 1]) < 0.000001) slopebefore = 999.0;
					else slopebefore = (y[i - 1] - y[i])/(x[i - 1] - x[i]);
					
					//current and zeroth
					if(Math.abs(x[i] - x[0]) < 0.000001) slopeafter = 999.0;
					else slopeafter = (y[0] - y[i])/(x[0] - x[i]);
				}
				else 
				{
					//test slope between 
					
				    //previous and current
					if(Math.abs(x[i] - x[i - 1]) < 0.000001) slopebefore = 999.0;
					else slopebefore = (y[i - 1] - y[i])/(x[i - 1] - x[i]);
					
					//current and next
					if(Math.abs(x[i] - x[i + 1]) < 0.000001) slopeafter = 999.0;
					else slopeafter = (y[i + 1] - y[i])/(x[i + 1] - x[i]);
				}
				
				if(Math.abs(slopeafter - slopebefore) > 0.0001)
				{
					x2[count2] = x[i];
					y2[count2] = y[i];
					++count2;
				}
					
				//System.out.println("x: " + pol3.vertex(i).getX() + " y: " + pol3.vertex(i).getY());
			}
		}
		///////////////// Record number of points /////////////////////
		
		if(count2 > 2)
			nPoints = count2;
		else
			nPoints = 0;

		/////////////////////////////////////////////////////////////
		
		return(new Object[]{nPoints, x2, y2});
	}
	

	public static void main(String[] args){ 
		
	}


}
