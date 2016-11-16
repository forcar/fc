package org.clas.fcmon.tools;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import eu.mihosoft.vrl.v3d.Vector3d;
import org.jlab.detector.geant4.v2.*;
import org.jlab.geometry.utils.*;
import org.jlab.detector.volume.*;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ec.ECDetector;
import org.jlab.geom.detector.ec.ECFactory;
import org.jlab.geom.detector.ec.ECLayer;
import org.jlab.geom.prim.Point3D;

public class JCSG {
    
    ConstantProvider    cp1 = GeometryFactory.getConstants(DetectorType.EC,1,"default");
    ConstantProvider    cp2 = GeometryFactory.getConstants(DetectorType.EC,1,"default");
   
    ECDetector    detector = new ECFactory().createDetectorCLAS(cp2);
    String name[]={"PCAL","ECin","ECout"};
    int  isec[]={2,2,2};
    int  ilay[]={1,1,1};
    int  istr[]={1,36,36}; //Perp. radius at {59,28}
    public JCSG() {};
    
    private void doCJVert(int unit) {
        
        ECLayer  ecLayer;
        Point3D point1 = new Point3D();
//        int[] vertices = {0,4,5,1};
//        int[] vertices = {1,5,2,6,0,4,3,7};
        int[] vertices = {4,0,5,1,7,3,6,2};
//        int[] vertices = {6,2,7,3,5,1,4,0};
//        int[] vertices = {4,0,5,1,7,3,6,2};
        int[] numstrips = new int[3];
        double[][][][] xPoint = new double [6][15][68][8];
        double[][][][] yPoint = new double [6][15][68][8];
        int suplay = unit; //PCAL ==0, ECinner ==1, ECouter==2 

        System.out.println(cp2.getDouble("/geometry/ec/ec/dist2tgt",0));
        System.out.println("CoatJava "+name[unit]+":");
        
        for(int sector = isec[unit]-1; sector < isec[unit]; ++sector) {
            for(int l = ilay[unit]-1; l<ilay[unit]; l++) {      
                ecLayer = detector.getSector(sector).getSuperlayer(suplay).getLayer(l);
                numstrips[l] = ecLayer.getNumComponents();
                int n = 0;
                for(ScintillatorPaddle paddle1 : ecLayer.getAllComponents()) {
                    if ((n+1)==istr[unit]) {
                    for(int j=0; j<8 ; j++) {
                        point1.copy(paddle1.getVolumePoint(vertices[j]));
//                        point1.copy(paddle1.getVolumePoint(j));
//                        point1.rotateZ(sector * Math.PI/3.0);
//                        point1.translateXYZ(333.1042, 0.0, 0.0);
                        double r=Math.sqrt(point1.x()*point1.x()+point1.z()*point1.z());
                        double r1=Math.sqrt(point1.x()*point1.x()+point1.y()*point1.y()+point1.z()*point1.z());
                        System.out.println(point1.x()+" "+point1.y()+" "+point1.z()+" "+r+" "+r1);
                        xPoint[sector][l][n][j] =  point1.x();
                        yPoint[sector][l][n][j] = -point1.y(); // why minus sign?
                    }
                    }
                    n++;
                }
            }
        }
    }
    
    private void doJCSGVert() {
        
        G4Trap vol = new G4Trap("vol", 1,0,0,1,1,1,0,1,1,1,0);
        for(int i=0;i<8;i++)
            System.out.println(vol.getVertex(i));
        
        PCALGeant4Factory pcal = new PCALGeant4Factory(cp1);
        ECGeant4Factory     ec = new ECGeant4Factory(cp1);

        System.out.println("JCSG PCAL:");
        G4Trap pcalPadVol = pcal.getPaddle(isec[0],ilay[0],istr[0]);
        for(int i=0;i<8;i++) {
            double x = pcalPadVol.getVertex(i).x;
            double y = pcalPadVol.getVertex(i).y;
            double z = pcalPadVol.getVertex(i).z;
            double r = Math.sqrt(x*x+z*z);
            double r1 = Math.sqrt(x*x+y*y+z*z);
            System.out.println(pcalPadVol.getVertex(i)+" "+r+" "+r1);
        }

        System.out.println("JCSG ECinner:");
        G4Trap eciPadVol = ec.getPaddle(isec[1],ilay[1],istr[1]);
        for(int i=0;i<8;i++) {
            double x = eciPadVol.getVertex(i).x;
            double y = eciPadVol.getVertex(i).y;
            double z = eciPadVol.getVertex(i).z;
            double r = Math.sqrt(x*x+z*z);
            double r1 = Math.sqrt(x*x+y*y+z*z);
            System.out.println(eciPadVol.getVertex(i)+" "+r+" "+r1);
        }
        
        System.out.println("JCSG ECouter:");
        G4Trap ecoPadVol = ec.getPaddle(isec[1],ilay[1]+15,istr[1]);
        for(int i=0;i<8;i++) {
            double x = ecoPadVol.getVertex(i).x;
            double y = ecoPadVol.getVertex(i).y;
            double z = ecoPadVol.getVertex(i).z;
            double r = Math.sqrt(x*x+z*z);
            double r1 = Math.sqrt(x*x+y*y+z*z);
            System.out.println(ecoPadVol.getVertex(i)+" "+r+" "+r1);
        }
        
    }
    
    public static void main(String[] args){
    
    JCSG jcsg = new JCSG();
    
    jcsg.doJCSGVert();    
    jcsg.doCJVert(0);
    jcsg.doCJVert(1);
    jcsg.doCJVert(2);
    
    }
    
}
