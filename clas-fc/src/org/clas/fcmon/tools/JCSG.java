package org.clas.fcmon.tools;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import eu.mihosoft.vrl.v3d.Vector3d;
import org.jlab.detector.volume.*;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.prim.Point3D;

public class JCSG {
    
	String variation = "default";
//	String variation = "rga_fall2018";
	
    ConstantProvider   cp1 = GeometryFactory.getConstants(DetectorType.ECAL,1,variation);     
    PCALGeant4Factory pcal = new PCALGeant4Factory(cp1);
    ECGeant4Factory   ecal = new   ECGeant4Factory(cp1);
    
    ConstantProvider   cp2 = GeometryFactory.getConstants(DetectorType.ECAL,1,variation); 
    ECDetector    detector = new ECFactory().createDetectorCLAS(cp2);
    
    String          name[] = {"PCAL","ECin","ECout"};
    int             isec[] = {1,1,1};
    int             ilay[] = {1,1,1};
    int             istr[] = {59,28,28}; //Normal radius from target at {59,28,28}

    int[][]       vertices = {{4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{4,0,5,1,7,3,6,2},  //JCSG vertices -> CJ vertices (PCAL)
                              {4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{1,5,0,4,2,6,3,7},  //JCSG vertices -> CJ vertices (ECIN)
                              {4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{1,5,0,4,2,6,3,7}}; //JCSG vertices -> CJ vertices (ECOU)
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
    
    private void doCJVert() {
        System.out.println("CJ  "+name[cal]+":"); 
        switch (cal) {
        case 0: printCJ(detector.getSector(isec[0]-1).getSuperlayer(cal).getLayer(ilay[0]-1).getComponent(istr[cal]-1)); break;
        case 1: printCJ(detector.getSector(isec[1]-1).getSuperlayer(cal).getLayer(ilay[1]-1).getComponent(istr[cal]-1)); break;
        case 2: printCJ(detector.getSector(isec[2]-1).getSuperlayer(cal).getLayer(ilay[2]-1).getComponent(istr[cal]-1));
        }
    }
    
    private void doJCSGVert() {   
        System.out.println("JCSG "+name[cal]+":");
        switch (cal) {
        case 0: printJCSG(pcal.getPaddle(isec[0],ilay[0],istr[0])) ; break;
        case 1: printJCSG(ecal.getPaddle(isec[1],ilay[1],istr[1])) ; break;
        case 2: printJCSG(ecal.getPaddle(isec[2],ilay[1]+15,istr[2])) ; 
        }
    }
    
    private void printJCSG(G4Trap vol) {
    	Point3D point;
        for(int i=0;i<8;i++) {
        	point = getP3D(vol.getVertex(i));
            point.rotateZ(Math.toRadians(-60*(isec[0]-1)));
            point.rotateY(Math.toRadians(-25)); 
            String r0 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.z()*point.z()));
    		String r1 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.y()*point.y()+point.z()*point.z()));
            System.out.println(point+"    Rxz = "+r0+"    Rxyz = "+r1); 
        }    	
    }
    
    private void printCJ(ScintillatorPaddle paddle) {
    	Point3D point = new Point3D();
    	for(int j=0; j<8 ; j++) {
    		int il = ilay[cal]>3?ilay[cal]-4:ilay[cal]-1;
    		point.copy(paddle.getVolumePoint(vertices[il+3*cal][j]));
            point.rotateZ(Math.toRadians(-60*(isec[0]-1)));
            point.rotateY(Math.toRadians(-25)); 
            String r0 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.z()*point.z()));
    		String r1 = String.format("%.4f",(float)Math.sqrt(point.x()*point.x()+point.y()*point.y()+point.z()*point.z()));
    		System.out.println(point+"    Rxz = "+r0+"    Rxyz = "+r1);	
    	}   	
    }
    
    public Point3D getP3D(Vector3d vec) {
    	return new Point3D(vec.x,vec.y,vec.z);
    }
    
    public static void main(String[] args){
    	JCSG jcsg = new JCSG();    	
    }
    
}
