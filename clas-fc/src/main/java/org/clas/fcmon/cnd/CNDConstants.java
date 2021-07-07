package org.clas.fcmon.cnd;

public class CNDConstants {
    
    public static double TOFFSET = 450;  // FADC/TDC offset in ns
    public static int IS1 = 1 ;
    public static int IS2 = 1 ;
    public static int NPADDLES = 48;
    
    public static double RADIUS, ANGLE, THICK, LGAP, AGAP;
    public static double[] LB = new double[3];
    public static double[] UB = new double[3];
    public static double[]  R = new double[3];

    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
    
    public static final void setGeometry() {

  }    

}
