package org.clas.fcmon.tools;

/**
 * 
 * @author lcsmith
 * Adapted from Rafayel Paremuzjan clas12 trigger code
 * https://github.com/JeffersonLab/clas12-trigger/blob/master/src/TECGeom.cxx
 */

public class TECGeom {
	
	public TECGeom(double aU, double aV, double aW) {
		setGeometry(aU,aV,aW) ;// U, V and W are strip coordinates in the (0-36) range
	}
	    
    public void setGeometry(double aU, double aV, double aW) {
    	
		  fSector = -1;
		    
		  fU = aU*fdU/Math.sin(falpha_V);
		  fV = aV*fdV/Math.sin(falpha_W);
		  fW = aW*fdW/Math.sin(falpha_U);

		  fDalitz = fU/fLU + fV/fLV + fW/fLW - 2.;

		  fx_UV = fx0_V + fV - (fLU - fU)*Math.sin(falpha_V)/Math.tan(falpha_W);
		  fy_UV = fU*Math.sin(falpha_V);

		  fx_UW = fx0_W - fW*fLV/fLW + (fLU - fU)*Math.cos(falpha_V); 
		  //fx_UW = fx0_V - (fLV/fLW)*fW + fLU*Math.cos(falpha_V)*(fLU - fU)/fLU;
		  fy_UW = fU*Math.sin(falpha_V);
		  
		  double u_calc = fLU*(2. - fV/fLV - fW/fLW);  // Using Dalitz rule calculate U, and then calculate x as UW
		  //fx_VW = fx0_V - (fLV/fLW)*fW + fLU*Math.cos(falpha_V)*(fLU - u_calc)/fLU;
		  fx_VW = fx0_W - fW*fLV/fLW +(fLU - u_calc)*Math.cos(falpha_V);
		  fy_VW = u_calc*Math.sin(falpha_V);

		  //====== According to Ben's algorithm the cluster coordinate is the mean of uv, uw and vw
		  fy = (fy_UV + fy_UW + fy_VW)/3.;
		  fx = (fx_UV + fx_UW + fx_VW)/3.;
	}	
	
	public void SetSector(int aSect){

	   fSector = aSect;

	   double[] f_UW = GetHallCoordinates(fx_UW, fy_UW);
	   double[] f_UV = GetHallCoordinates(fx_UV, fy_UV);
	   double[] f_VW = GetHallCoordinates(fx_VW, fy_VW);
	   fx_UW_Hall = f_UW[0]; fy_UW_Hall = f_UW[1] ; fz_UW_Hall = f_UW[2];
	   fx_UV_Hall = f_UV[0]; fy_UV_Hall = f_UV[1] ; fz_UV_Hall = f_UV[2];
	   fx_VW_Hall = f_VW[0]; fy_VW_Hall = f_VW[1] ; fz_VW_Hall = f_VW[2];
	}
	
	public double[] GetHallCoordinates(double ax, double ay){
	    
	    // ==== Going from local coordinates to L2 where, only EC is tilted by 25 deg
	    double yl2 = (fy0 + ay)*Math.cos(EC_angle);
	    double xl2 = ax;
	    double zl2 = fz0 - (fy0 + ay)*Math.sin(EC_angle);
	        
	    // ==== Going from L2 to Global, this is basically rotation along the z axis
	    
	    double phi = (60*fSector - 90)*d2r;
	    
	    double[] out = new double[3];
	    out[0] = xl2*Math.cos(phi) - yl2*Math.sin(phi);
	    out[1] = xl2*Math.sin(phi) + yl2*Math.cos(phi);
	    out[2] = zl2;
	    
	    return out;
	}
	
	public double GetX() {return fx;}
	public double GetY() {return fy;}
	public double GetDalitz() {return fDalitz;}
	public double GetY_UV() {return fy_UV;}
	public double GetX_UV() {return fx_UV;}
	public double GetY_UW() {return fy_UW;}
	public double GetX_UW() {return fx_UW;}
	public double GetY_VW() {return fy_VW;}
	public double GetX_VW() {return fx_VW;}
	public double GetLU() {return fLU;} // Length of U side
	public double GetLV() {return fLV;} // Length of V side
	public double GetLW() {return fLW;} // Length of W side
		  
	public double GetHallX_UV() {return fx_UV_Hall;}
	public double GetHallY_UV() {return fy_UV_Hall;}
	public double GetHallZ_UV() {return fz_UV_Hall;}
	public double GetHallX_UW() {return fx_UW_Hall;}
	public double GetHallY_UW() {return fy_UW_Hall;}
	public double GetHallZ_UW() {return fz_UW_Hall;}
	public double GetHallX_VW() {return fx_VW_Hall;}
	public double GetHallY_VW() {return fy_VW_Hall;}
	public double GetHallZ_VW() {return fz_VW_Hall;}
		  
	int fSector;
		    
	double fx, fy;        // local variables
	double fU, fV, fW;    // U, V and W coordinates
	double fDalitz;       // Dalitz, note 2 is subtracted, so it is expected to be peaked at 0

	double fx_Hall, fy_Hall, fz_Hall; // x, y and z coordinates in the Hall Frame where (0., 0., 0) is the target center and z is along th beamline
		  
	double fx_UV, fy_UV; // cluster coordinate calculated through U and V peaks
	double fx_UW, fy_UW; // cluster coordinate calculated through U and W peaks
	double fx_VW, fy_VW; // cluster coordinate calculated through V and W peaks

	double fx_UV_Hall, fy_UV_Hall, fz_UV_Hall; //cluster coordinate in the Hall Frame, calculated through U and V peaks
	double fx_UW_Hall, fy_UW_Hall, fz_UW_Hall; //cluster coordinate in the Hall Frame, calculated through U and V peaks
	double fx_VW_Hall, fy_VW_Hall, fz_VW_Hall; //cluster coordinate in the Hall Frame, calculated through U and V peaks

	static double d2r = 0.017453;
	static double EC_angle = 25*d2r;
	// fy0: The distance from the U vertex and the beamline in the EC front plane
	// The 14.73 is obtained by comparing to GEMC
	static double fy0 = 50.7601930 + 14.73/Math.cos(EC_angle);  
	// fz0: z coordinate of the intersection of the EC Plane and the beamline
	// It is calculated as 7127.23/cos(25*TMath::DegToRad()), 34.673 is calculated by comparing to GEMC
	static double fz0 = 786.4028205 + 34.6730;  
		  
 //========== Lengths of U, V and W sides =========
	static double fLU = 419.239839;  //cm
	static double fLV = 383.0062897; //cm
	static double fLW = 419.239839;  //cm

 //========== Half of the perimeter, and the area of the triangle =======
	static double fPhalf = 0.5*(fLU+fLV+fLU);
	static double fS = Math.sqrt(fPhalf*(fPhalf-fLU)*(fPhalf-fLV)*(fPhalf-fLW));

//========== Angles of U, V and W vertexes =======
	static double falpha_U = Math.asin(2*fS/(fLU*fLW));
	static double falpha_V = Math.asin(2*fS/(fLV*fLU));
	static double falpha_W = Math.asin(2*fS/(fLW*fLV));

//========= coordinates of U, V and W vertexes =========
	static double fy0_U = 0;
	static double fx0_U = 0;
	static double fy0_V =  fLU*Math.sin(falpha_V);
	static double fx0_V = -fLU*Math.cos(falpha_V);
	static double fy0_W =  fLW*Math.sin(falpha_W);
	static double fx0_W =  fLW*Math.cos(falpha_W);

 //========= Width of a scintilliator =========
	static double fSCWidth;
	static double fdU = 10.366; // cm
	static double fdV =  9.48;  // cm
	static double fdW =  9.48;  // cm
		
}
