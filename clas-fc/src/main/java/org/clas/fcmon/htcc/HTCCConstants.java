package org.clas.fcmon.htcc;

public class HTCCConstants {
    
    public static double TOFFSET = 450;  // FADC/TDC offset in ns
    public static int IS1 = 1 ;
    public static int IS2 = 7 ;
    public static int NPMTS = 4;
    
    public static double RADIUS, ANGLE, THICK, LGAP, AGAP;
    public static double[] LB = new double[NPMTS];
    public static double[] UB = new double[NPMTS];
    public static double[]  R = new double[NPMTS];

    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
    
    public static final void setGeometry() {

  }    

}
