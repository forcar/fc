package org.clas.fcmon.tools;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.geant4.v2.ECGeant4Factory;
import org.jlab.detector.geant4.v2.PCALGeant4Factory;

import eu.mihosoft.vrl.v3d.Vector3d;
import org.jlab.detector.volume.*;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Layer;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;

public class JCSG {
    
//	String variation = "default";
	String variation = "rga_fall2018";

	//JCSG geometry
    ConstantProvider   cp1 = GeometryFactory.getConstants(DetectorType.ECAL,11,variation);     
    PCALGeant4Factory pcal = new PCALGeant4Factory(cp1);
    ECGeant4Factory   ecal = new   ECGeant4Factory(cp1);
    
    //CJ geometry
    ConstantProvider   cp2 = GeometryFactory.getConstants(DetectorType.ECAL,11,variation); 
    ECDetector    detector = new ECFactory().createDetectorTilted(cp2);

    ScintillatorPaddle firstPaddle, lastPaddle;
    
    String          name[] = {"PCAL","ECin","ECout"};
    String          view[] = {"U","V","W"};
    int             isec[] = {1,1,1};
    int             ilay[] = {3,3,3};
    int             istr[] = {ilay[0]==1?68:62,36,36}; //Normal radius from target is at {59,28,28}
//    int             istr[] = {59,28,28}; //Normal radius from target
    
// These were valid as of 2018
//    int[][]       vertices = {{4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{4,0,5,1,7,3,6,2},  //CJ vertices (PCAL) -> JCSG vertices 
//                              {4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{1,5,0,4,2,6,3,7},  //CJ vertices (ECIN) -> JCSG vertices 
//                              {4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{1,5,0,4,2,6,3,7}}; //CJ vertices (ECOU) -> JCSG vertices 
                              //       U                 V                 W
    
    int[][]       vertices = {{4,0,5,1,7,3,6,2},{4,0,5,1,7,3,6,2},{1,5,0,4,2,6,3,7},  //CJ vertices (PCAL) -> JCSG vertices 
                              {4,0,5,1,7,3,6,2},{4,0,5,1,7,3,6,2},{4,0,5,1,7,3,6,2},  //CJ vertices (ECIN) -> JCSG vertices 
                              {4,0,5,1,7,3,6,2},{4,0,5,1,7,3,6,2},{4,0,5,1,7,3,6,2}}; //CJ vertices (ECOU) -> JCSG vertices 
                              //       U                 V                 W
    int cal = 0;
    
    public JCSG() {        
    	G4Trap vol = new G4Trap("vol", 1,0,0,1,1,1,0,1,1,1,0);
        for(int i=0;i<8;i++) System.out.println(getP3D(vol.getVertex(i)));
        System.out.println("\n"+"Variation "+variation);
        System.out.println("\n"+"/geometry/ec/ec/dist2tgt = "+cp2.getDouble("/geometry/ec/ec/dist2tgt",0)+" mm"+"\n");
        System.out.println("SECTOR "+isec[0]+" LAYER "+ilay[0]+ " PCAL STRIP "+istr[0]+" EC STRIP "+istr[1]+"\n");
        processVert();
    }
        
    private void processVert() {
        for (cal=0; cal<3; cal++) {doJCSGVert();doCJVert(); System.out.println(" ");}   	
    }
   
    private void doJCSGVert() {   
        System.out.println("JCSG "+name[cal]+":");
        switch (cal) {
        case 0: printJCSG(pcal.getPaddle(isec[0],ilay[0],istr[0])) ; break;
        case 1: printJCSG(ecal.getPaddle(isec[1],ilay[1],istr[1])) ; break;
        case 2: printJCSG(ecal.getPaddle(isec[2],ilay[1]+15,istr[2])) ; 
        }
    }
    
    private void doCJVert() {
        System.out.println("CJ  "+name[cal]+":"); 
        Layer detLayer = detector.getSector(isec[cal]-1).getSuperlayer(cal).getLayer(ilay[cal]-1);
        firstPaddle = (ScintillatorPaddle) detLayer.getComponent(0);
        lastPaddle  = (ScintillatorPaddle) detLayer.getComponent(detLayer.getNumComponents()-1);
        printCJ(detector.getSector(isec[cal]-1).getSuperlayer(cal).getLayer(ilay[cal]-1).getComponent(istr[cal]-1));
    }
    
    private void printJCSG(G4Trap vol) {
    	Point3D point = new Point3D();  
        Vector3D v0 = new Vector3D(); Vector3D v1 = new Vector3D(); Vector3D v2 = new Vector3D(); Vector3D v3 = new Vector3D();
        for(int i=0;i<8;i++) {
        	point = getP3D(vol.getVertex(i));
            point.rotateZ(Math.toRadians(-60*(isec[0]-1)));
            point.rotateY(Math.toRadians(-25)); 
            String r0 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.z()*point.z()));
    		String r1 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.y()*point.y()+point.z()*point.z()));
            System.out.println(point + "    Rxz = " + r0 + "    Rxyz = " + r1); 
    		if(i==0) v0= new Vector3D(point.x(),point.y(),point.z()); 
    		if(i==1) v1= new Vector3D(point.x(),point.y(),point.z()); 
    		if(i==2) v2= new Vector3D(point.x(),point.y(),point.z()); 
    		if(i==3) v3= new Vector3D(point.x(),point.y(),point.z());     		
        }    	
    	System.out.println("Length = "+(float)(0.5*(v0.sub(v1).mag()+v2.sub(v3).mag())));
    }
    
    private void printCJ(ScintillatorPaddle paddle) {
    	Point3D point = new Point3D(); 
        Vector3D v0 = new Vector3D(); Vector3D v1 = new Vector3D(); Vector3D v2 = new Vector3D(); Vector3D v3 = new Vector3D();
    	for(int j=0; j<8 ; j++) {
    		int il = ilay[cal]>3?ilay[cal]-4:ilay[cal]-1;
    		point.copy(paddle.getVolumePoint(vertices[il+3*cal][j]));
            String r0 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.z()*point.z()));
    		String r1 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.y()*point.y()+point.z()*point.z()));
    		System.out.println(point + "    Rxz = " + r0 + "    Rxyz = " + r1);	
    		if(j==0) v0= new Vector3D(point.x(),point.y(),point.z()); 
    		if(j==1) v1= new Vector3D(point.x(),point.y(),point.z()); 
    		if(j==2) v2= new Vector3D(point.x(),point.y(),point.z()); 
    		if(j==3) v3= new Vector3D(point.x(),point.y(),point.z()); 
    	}
    	
    	Point3D e1 = paddle.getLine().end();
    	Point3D p1 = paddle.getLine().origin(); 
    	Point3D p2 = firstPaddle.getLine().origin(); 
    	Point3D pl = lastPaddle.getLine().origin(); System.out.println(p1+" "+p2+" "+pl);

    	double dist = paddle.getLine().origin().distance(firstPaddle.getLine().origin());		
    	System.out.println("Length = "+(float)paddle.getLength()+
//    	                               (float)0.5*(v0.sub(v1).mag()+v2.sub(v3).mag())+
    	                               " l"+view[ilay[cal]-1]+" = "+(float)dist+
    	                             " GGl"+view[ilay[cal]-1]+" = "+(float)getGGdist(paddle,ilay[cal]));
    }
    
    public float getGGdist(ScintillatorPaddle paddle, int layer) {
    	double dist = 0;
    	if(layer==1) dist = paddle.getLine().origin().distance(firstPaddle.getLine().origin());		
        if(layer==2||layer==3){
            double distance = paddle.getLine().origin().distance(lastPaddle.getLine().origin());
            double hL = 394.2*0.5;
            double hyp = Math.sqrt(hL*hL + 385.2*385.2);
            double theta = Math.acos(hL/hyp);
            double proj  = 4.5*Math.cos(theta);
            dist = distance + proj;
        } 
        return (float)dist;
    }
    
    public Point3D getP3D(Vector3d vec) {
    	return new Point3D(vec.x,vec.y,vec.z);
    }
    
    public static void main(String[] args){
    	JCSG jcsg = new JCSG();    	
    }
    
}
