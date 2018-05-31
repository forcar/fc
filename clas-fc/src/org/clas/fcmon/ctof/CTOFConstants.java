package org.clas.fcmon.ctof;

public class CTOFConstants {
    
    public static float[] TOFFSET = {147,172};  // FADC/TDC offset in ns
    public static int IS1 = 1 ;
    public static int IS2 = 2 ;
    public static int NPADDLES = 48;
    //Table 4: ctof_geom.tex  CCDB: /geometry/ctof/ctof
    public static double RADIUS  = 25.11;
    public static double LENGTH  = 80.683;
    public static double THICK   =  3.02;
    public static double WIDTH   =  3.21;  
    
    public static double[]     DX1 = new double[2];
    public static double[]     DX2 = new double[2];
    public static double[]      DY = new double[2];
    public static double[]       R = new double[2];
   
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
    
    public static final void setGeometry() {
    	  DX1[0] = WIDTH;
    	  DX2[0] = DX1[0]*(1+0.5*THICK/RADIUS);
      DX1[1] = DX2[0];
    	  DX2[1] = DX1[0]*(1+1.0*THICK/RADIUS);
	   DY[0] = THICK/2;
	   DY[1] = THICK/2;
	    R[0] = RADIUS+DY[0];
	    R[1] = R[0]+DY[0];
    }
    

}
