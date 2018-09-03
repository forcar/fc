package org.clas.fcmon.band;

public class BANDConstants {
    
    public static float[] TOFFSET = {0,0};  // FADC/TDC offset in ns
    public static int IS1 = 1 ;
    public static int IS2 = 2 ; 
    public static float[] bandlen  = {163.7f,201.9f,51.2f,51.2f,201.9f};
    public static float[] bandwid  = {7.5f,7.5f,7.5f,7.5f,7.5f};
    public static float[] bandxoff = {0.f,0.f,75.35f,-75.35f,0.f};
    public static float[] bandyoff = {13,10,3,3,-3};
    public static int[][] bandlay  = {{3,7,6,6,2},{3,7,6,6,2},{3,7,6,6,2},{3,7,6,6,2},{3,7,5,5,0}};
    public static int[][] bandtsum = {{1,4,11,17,23},{1,4,11,17,23},{1,4,11,17,23},{1,4,11,17,23},{1,4,11,15,0}};
    public static   int[] bandsum  = {1,4,11,11,17};
    public static   int[] bandbar  = {18,18,18,18,15};
    public static String[] bandlab = {"","","A","B",""};
    
    public static final void setSectorRange(int is1, int is2) {
        IS1=is1;
        IS2=is2;
    }
    
    public static String getAlias(int s, int l, int c, int o) {
    	String i1 = Integer.toString(l);
    	int ch = bandsum[s-1]+c-1;
    	String i2 = ch<10 ? "0"+Integer.toString(ch):Integer.toString(ch);
    	String i3 = bandlab[s-1];
    	String i4 = (o==0)?"_L":"_R";
    	return i1+i2+i3+i4;
    }
    
    public static String getHvAlias(String l, int c) {
    	int[]     map = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,11,12,13,14,15,16,17,18};
    	int[]    map5 = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,11,12,13,14,15};
    	String[]  smap= {"","","","","","","","","","","A","A","A","A","A","A","B","B","B","B","B","B","",""};
    	String[] smap5= {"","","","","","","","","","","A","A","A","A","A","B","B","B","B","B"};
    	String i1 = l.substring(0,1);
    	int  chan = Integer.parseInt(i1)==5?map5[c-1]:map[c-1];
    	String i2 = chan<10 ? "0"+Integer.toString(chan):Integer.toString(chan);
    	String i3 = Integer.parseInt(i1)==5?smap5[c-1]:smap[c-1];
    	String i4 = "_"+l.substring(1,2);
    	return i1+i2+i3+i4;
    }
    
    public static int getBar(int id, int s, int c) {
    	return bandtsum[id][s-1]+c-1;
    }
}
