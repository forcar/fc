package org.clas.fcmon.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

import org.clas.fcmon.detector.view.DetectorShape2D;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.prim.Point3D;

public class ECpixelDepth {

	public ECpixelDepth() {
		// TODO Auto-generated constructor stub
	}
	
	public void FindFrontECin(ECDetector ecdet)
	{
		PrintWriter writervolcoord = null;
		try 
		{
			writervolcoord = new PrintWriter("ECinpixfrontvert.dat");
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//get list of centers for EC inner
		CalDrawDB pcaltestdist1 = new CalDrawDB("ECin",ecdet);
		DetectorShape2D shape1 = new DetectorShape2D();
		for(int sector = 0; sector < 1; sector++)
		{
			for(int uPaddle = 0; uPaddle < 36; uPaddle++)
			{
			    for(int vPaddle = 0; vPaddle < 36; vPaddle++)
			    {
				     for(int wPaddle = 0; wPaddle < 36; wPaddle++)
				     {
				     	//System.out.println("u: " + uPaddle + " v: " + vPaddle + " w: " + wPaddle);
				      	if(pcaltestdist1.isValidPixel(sector, uPaddle, vPaddle, wPaddle))
				    	{
				      		shape1 = pcaltestdist1.getPixelShape(sector, uPaddle, vPaddle, wPaddle);
				            		
				      		for(int i = 0; i < shape1.getShapePath().size();++i)
				      		{
				      			writervolcoord.println(uPaddle + "  " + vPaddle + "  " + wPaddle + "  " + shape1.getShapePath().point(i).x()  + "   " + shape1.getShapePath().point(i).y() + "   " + shape1.getShapePath().point(i).z());
				         	}
				    	}
				     }
			    }
			
			}
		}
		writervolcoord.close();
	}
	
	public void FindFrontPCAL(ECDetector ecdet)
	{
		PrintWriter writervolcoord = null;
		try 
		{
			writervolcoord = new PrintWriter("PCALpixfrontvert.dat");
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//get list of centers for EC inner
		CalDrawDB pcaltestdist1 = new CalDrawDB("PCAL",ecdet);
		DetectorShape2D shape1 = new DetectorShape2D();
		for(int sector = 0; sector < 1; sector++)
		{
			for(int uPaddle = 0; uPaddle < 68; uPaddle++)
			{
			    for(int vPaddle = 0; vPaddle < 62; vPaddle++)
			    {
				     for(int wPaddle = 0; wPaddle < 62; wPaddle++)
				     {
				     	//System.out.println("u: " + uPaddle + " v: " + vPaddle + " w: " + wPaddle);
				      	if(pcaltestdist1.isValidPixel(sector, uPaddle, vPaddle, wPaddle))
				    	{
				      		shape1 = pcaltestdist1.getPixelShape(sector, uPaddle, vPaddle, wPaddle);
				            		
				      		for(int i = 0; i < shape1.getShapePath().size();++i)
				      		{
				      			writervolcoord.println(uPaddle + "  " + vPaddle + "  " + wPaddle + "  " + shape1.getShapePath().size() + "  " + shape1.getShapePath().point(i).x()  + "   " + shape1.getShapePath().point(i).y() + "   " + shape1.getShapePath().point(i).z());
				         	}
				    	}
				     }
			    }
			
			}
		}
		writervolcoord.close();
	}
	
	
	public void FindFrontECout(ECDetector ecdet)
	{
		PrintWriter writervolcoord = null;
		try 
		{
			writervolcoord = new PrintWriter("ECoutpixfrontvert.dat");
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//get list of centers for EC outer
		CalDrawDB pcaltestdist2 = new CalDrawDB("ECout",ecdet);
		DetectorShape2D shape2 = new DetectorShape2D();
		for(int sector = 0; sector < 1; sector++)
		{
			for(int uPaddle = 0; uPaddle < 36; uPaddle++)
			{
			    for(int vPaddle = 0; vPaddle < 36; vPaddle++)
			    {
				     for(int wPaddle = 0; wPaddle < 36; wPaddle++)
				     {
				     	//System.out.println("u: " + uPaddle + " v: " + vPaddle + " w: " + wPaddle);
				      	if(pcaltestdist2.isValidPixel(sector, uPaddle, vPaddle, wPaddle))
				    	{
				      		shape2 = pcaltestdist2.getPixelShape(sector, uPaddle, vPaddle, wPaddle);
				            		
				      		for(int i = 0; i < shape2.getShapePath().size();++i)
				      		{
				      			writervolcoord.println(uPaddle + "  " + vPaddle + "  " + wPaddle + "  " + shape2.getShapePath().point(i).x()  + "   " + shape2.getShapePath().point(i).y() + "   " + 18.57);
				         	}
				    	}
				     }
			    }
			
			}
		}
		writervolcoord.close();
	}
	
	public void FindBackEC(ECDetector ecdet)
	{   
		FindFrontECin(ecdet);
		FindFrontECout(ecdet);
		//                                      u   v   w  x/y/z numpoints
		double[][][][][] ecinfront = new double[36][36][36][3][3];
		double[][][][][] ecoutfront = new double[36][36][36][3][3];
		double[][][][][] ecback = new double[36][36][36][3][3];
		
		double totaldist;
		double time;
		double deltazin = 1.238 * 15.0;
		double deltaztot = 1.238 * 39.0;
		int u, v, w;
		Scanner inEcin;
		try 
		{
			inEcin = new Scanner(new File("ECinpixfrontvert.dat"));
			for(int i = 0; i < 1296; ++i)
	    	{
				//point1
				//paddle num
				u = inEcin.nextInt();
				v = inEcin.nextInt();
				w = inEcin.nextInt();
				
				//x,y,z
				ecinfront[u][v][w][0][0] = inEcin.nextDouble();
				ecinfront[u][v][w][1][0] = inEcin.nextDouble();
				ecinfront[u][v][w][2][0] = inEcin.nextDouble();
				
				//point2
				//paddle num
				u = inEcin.nextInt();
				v = inEcin.nextInt();
				w = inEcin.nextInt();
				
				//x,y,z
				ecinfront[u][v][w][0][1] = inEcin.nextDouble();
				ecinfront[u][v][w][1][1] = inEcin.nextDouble();
				ecinfront[u][v][w][2][1] = inEcin.nextDouble();
				
				
				//point1
				//paddle num
				u = inEcin.nextInt();
				v = inEcin.nextInt();
				w = inEcin.nextInt();
				
				//x,y,z
				ecinfront[u][v][w][0][2] = inEcin.nextDouble();
				ecinfront[u][v][w][1][2] = inEcin.nextDouble();
				ecinfront[u][v][w][2][2] = inEcin.nextDouble();
	    	}
		} 
		catch(FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Scanner inEcout;
		try 
		{
			inEcout = new Scanner(new File("ECoutpixfrontvert.dat"));
			for(int i = 0; i < 1296; ++i)
	    	{
				//point1
				//paddle num
				u = inEcout.nextInt();
				v = inEcout.nextInt();
				w = inEcout.nextInt();
				
				//x,y,z
				ecoutfront[u][v][w][0][0] = inEcout.nextDouble();
				ecoutfront[u][v][w][1][0] = inEcout.nextDouble();
				ecoutfront[u][v][w][2][0] = inEcout.nextDouble();
				
				//point2
				//paddle num
				u = inEcout.nextInt();
				v = inEcout.nextInt();
				w = inEcout.nextInt();
				
				//x,y,z
				ecoutfront[u][v][w][0][1] = inEcout.nextDouble();
				ecoutfront[u][v][w][1][1] = inEcout.nextDouble();
				ecoutfront[u][v][w][2][1] = inEcout.nextDouble();
				
				
				//point1
				//paddle num
				u = inEcout.nextInt();
				v = inEcout.nextInt();
				w = inEcout.nextInt();
				
				//x,y,z
				ecoutfront[u][v][w][0][2] = inEcout.nextDouble();
				ecoutfront[u][v][w][1][2] = inEcout.nextDouble();
				ecoutfront[u][v][w][2][2] = inEcout.nextDouble();
	    	}
		} 
		catch(FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//find back verts
		PrintWriter writerbackv = null;
		PrintWriter writerecinvolume = null;
		try 
		{
			writerbackv = new PrintWriter("ECpixbackvert.dat");
			writerecinvolume = new PrintWriter("ECinpixvolume.dat");
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(u = 0; u < 36; u++)
		{
			for(v = 0; v < 36; v++)
			{
				for(w = 0; w < 36; w++)
				{
					if(Math.abs(ecinfront[u][v][w][1][0]) > 0.00001 || Math.abs(ecinfront[u][v][w][1][1]) > 0.00001  || Math.abs(ecinfront[u][v][w][1][2]) > 0.00001)
					{
						for(int i = 0; i < 3;++i)
						{
							
							//z = z0 + ct
		            		//t= (z-z0)/c
		            		time = deltaztot/(deltazin);
		            		//time it takes from ecinfront to ecback
		            		
		            		//x = x0 + at
		            		ecback[u][v][w][0][i] = ecinfront[u][v][w][0][i] + (ecoutfront[u][v][w][0][i]-ecinfront[u][v][w][0][i])*time;
		            		//y = y0 + bt
		            		ecback[u][v][w][1][i] = ecinfront[u][v][w][1][i] + (ecoutfront[u][v][w][1][i]-ecinfront[u][v][w][1][i])*time;
		            		//z = z0 + ct
		            		ecback[u][v][w][2][i] = ecinfront[u][v][w][2][i] + (ecoutfront[u][v][w][2][i]-ecinfront[u][v][w][2][i])*time;
		            		
		            		writerbackv.println(u + "  " + v + "  " + w + "  " + ecback[u][v][w][0][i]  + "   " + ecback[u][v][w][1][i] + "   " + ecback[u][v][w][2][i]);

						}
					}
				}
			}
		}
		writerbackv.close();
	}
	/*
	
	public double SignedVolumeOfTriangle(Point3D p1, Point3D p2, Point3D p3) {
        double v321 = p3.x() *  p2.y() * p1.z();
        double v231 = p2.x()*p3.y()*p1.z();
        double v312 = p3.x()*p1.y()*p2.z();
        double v132 = p1.x()*p3.y()*p2.z();
        double v213 = p2.x()*p1.y()*p3.z();
        double v123 = p1.x()*p2.y()*p3.z();
        return (1.0/6.0)*(-v321 + v231 + v312 - v132 - v213 + v123);
    }
    
    public double VolumeOfMesh(Prism2Dto3DMesh test) {
    	double vols = 0.0;
    	
    	Point3D point1 = null;
    	Point3D point2 = null;
    	Point3D point3 = null;
    	float[] pointarray;
    	int[] facearray;
    	

    	pointarray = test.findallpoints();
    	facearray = test.findallfaces();
    	for(int i = 0; i < facearray.length ; i+=6)
    	{
    		
    		point1 = new Point3D(pointarray[facearray[i   ]*3],pointarray[facearray[i]*3+1],pointarray[facearray[i]*3 +2]);
    		point2 = new Point3D(pointarray[facearray[i+2 ]*3],pointarray[facearray[i+2 ]*3+1],pointarray[facearray[i+2]*3 +2]);
    		point3 = new Point3D(pointarray[facearray[i+4]*3],pointarray[facearray[i+4]*3+1],pointarray[facearray[i+4]*3 +2]);
    		vols += SignedVolumeOfTriangle(point1, point2, point3);
    	}
        return Math.abs(vols);
    }
*/
	
	public static void main(String[] args){ 
        ECDetector ecdet  = new ECFactory().createDetectorTilted(GeometryFactory.getConstants(DetectorType.ECAL, 10, "default"));
		ECpixelDepth test = new ECpixelDepth();
		test.FindBackEC(ecdet);
		test.FindFrontPCAL(ecdet);
		
		double[][][][] total1 = new double[36][36][36][3];
		double[] totalfinal = new double[3];
		double totaldist;
		double time;
		double deltazin = 1.238 * 15.0;
		double deltaztot = 1.238 * 39.0;
		int num1, num2, num3;
		
		//get list of centers for EC inner
		CalDrawDB pcaltestdist1 = new CalDrawDB("ECin",ecdet);
		DetectorShape2D shape1 = new DetectorShape2D();
		for(int sector = 0; sector < 1; sector++)
    	{
	    	for(int uPaddle = 0; uPaddle < 36; uPaddle++)
	    	{
	    		for(int vPaddle = 0; vPaddle < 36; vPaddle++)
	            {
		            for(int wPaddle = 0; wPaddle < 36; wPaddle++)
		            {
		            	if(pcaltestdist1.isValidPixel(sector, uPaddle, vPaddle, wPaddle))
		            	{
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

		for(int sector = 0; sector < 1; sector++)
    	{
	    	for(int uPaddle = 0; uPaddle < 36; uPaddle++)
	    	{
	    		for(int vPaddle = 0; vPaddle < 36; vPaddle++)
	            {
		            for(int wPaddle = 0; wPaddle < 36; wPaddle++)
		            {
		            	if(pcaltestdist2.isValidPixel(sector, uPaddle, vPaddle, wPaddle))
		            	{
		            		shape2 = pcaltestdist2.getPixelShape(sector, uPaddle, vPaddle, wPaddle);
		            		System.arraycopy( (double[])pcaltestdist2.getShapeCenter(shape2), 0, total2[uPaddle][vPaddle][wPaddle], 0, 3);
		            		total2[uPaddle][vPaddle][wPaddle][2] = deltazin;
		            	}
		            }
	            }
	
	    	}
    	}
		
		//extrapolate centers to end of outer
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
        double minDist = 1000;

		for(int sector = 0; sector < 1; sector++)
    	{
	    	for(int uPaddle = 0; uPaddle < 36; uPaddle++)
	    	{
	    		for(int vPaddle = 0; vPaddle < 36; vPaddle++)
	            {
		            for(int wPaddle = 0; wPaddle < 36; wPaddle++)
		            {
		            	if(pcaltestdist2.isValidPixel(sector, uPaddle, vPaddle, wPaddle))
		            	{
		            		time = deltaztot/(deltazin);
		            		totalfinal[0] = total1[uPaddle][vPaddle][wPaddle][0] + (total2[uPaddle][vPaddle][wPaddle][0]-total1[uPaddle][vPaddle][wPaddle][0])*time;
		            		totalfinal[1] = total1[uPaddle][vPaddle][wPaddle][1] + (total2[uPaddle][vPaddle][wPaddle][1]-total1[uPaddle][vPaddle][wPaddle][1])*time;
		            		totalfinal[2] = total1[uPaddle][vPaddle][wPaddle][2] + (total2[uPaddle][vPaddle][wPaddle][2]-total1[uPaddle][vPaddle][wPaddle][2])*time;

		            		totaldist = Math.sqrt(Math.pow(totalfinal[0]-total1[uPaddle][vPaddle][wPaddle][0],2) + Math.pow(totalfinal[1]-total1[uPaddle][vPaddle][wPaddle][1],2) + Math.pow(totalfinal[2]-total1[uPaddle][vPaddle][wPaddle][2],2));
//                            if(totaldist<minDist) minDist=totaldist;
		            		
		            		num1 = uPaddle + 1;
		            		num2 = vPaddle + 1;
		            		num3 = wPaddle + 1;
		            		
                            writerdist.println(num1  + "   " + num2 + "   " + num3 + "   "+ totaldist);
		            	}
		            }
	            }
	
	    	}
    	}
		writerdist.close();		
		
		System.out.println("Done!");
	}
}
