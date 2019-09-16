package org.clas.fcmon.tools;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import eu.mihosoft.vrl.v3d.Vector3d;
import org.jlab.detector.volume.*;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.detector.ec.ECLayer;
import org.jlab.geom.prim.Point3D;

public class JCSG {
    
    ConstantProvider   cp1 = GeometryFactory.getConstants(DetectorType.ECAL,1,"default");
    PCALGeant4Factory pcal = new PCALGeant4Factory(cp1);
    ECGeant4Factory   ecal = new   ECGeant4Factory(cp1);
    
    ConstantProvider   cp2 = GeometryFactory.getConstants(DetectorType.ECAL,1,"default");    
    ECDetector    detector = new ECFactory().createDetectorCLAS(cp2);
    ECLayer        ecLayer = null;
    Point3D         point1 = new Point3D();
    
    String          name[] = {"PCAL","ECin","ECout"};
    int             isec[] = {1,1,1};
    int             ilay[] = {3,3,3};
    int             istr[] = {59,28,28}; //Normal radius from target at {59,28,28}

    int[][]       vertices = {{4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{4,0,5,1,7,3,6,2},  //JCSG vertices -> CJ vertices (PCAL)
                              {4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{1,5,0,4,2,6,3,7},  //JCSG vertices -> CJ vertices (ECIN)
                              {4,0,5,1,7,3,6,2},{6,2,7,3,5,1,4,0},{1,5,0,4,2,6,3,7}}; //JCSG vertices -> CJ vertices (ECOU)
                             //        U                V                 W
    int                cal = 0;
    
    public JCSG() {        
    	G4Trap vol = new G4Trap("vol", 1,0,0,1,1,1,0,1,1,1,0);
        for(int i=0;i<8;i++) System.out.println(getP3D(vol.getVertex(i)));
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
        for(int i=0;i<8;i++) {
            double x = vol.getVertex(i).x;
            double y = vol.getVertex(i).y;
            double z = vol.getVertex(i).z;
            String r0 = String.format("%.4f",(float)Math.sqrt(x*x+z*z));
            String r1 = String.format("%.4f",(float)Math.sqrt(x*x+y*y+z*z));
            System.out.println(getP3D(vol.getVertex(i))+"    Rxz = "+r0+"    Rxyz = "+r1); 
        }    	
    }
    
    private void printCJ(ScintillatorPaddle paddle) {
    	for(int j=0; j<8 ; j++) {
    		point1.copy(paddle.getVolumePoint(vertices[(ilay[cal]-1)+3*cal][j]));
    		String r0 = String.format("%.4f",(float)Math.sqrt(point1.x()*point1.x()+point1.z()*point1.z()));
    		String r1 = String.format("%.4f",(float)Math.sqrt(point1.x()*point1.x()+point1.y()*point1.y()+point1.z()*point1.z()));
    		System.out.println(point1+"    Rxz = "+r0+"    Rxyz = "+r1);	
    	}   	
    }
    
    public Point3D getP3D(Vector3d vec) {
    	return new Point3D(vec.x,vec.y,vec.z);
    }
    
    public static void main(String[] args){
    	JCSG jcsg = new JCSG();    	
    }
    
}
