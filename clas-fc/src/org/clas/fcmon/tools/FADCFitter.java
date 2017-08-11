package org.clas.fcmon.tools;

public class FADCFitter {
	
	int p1=1,p2=15;
	int mmsum,summing_in_progress;
	public int t0,adc,ped,pedsum;
	
	public FADCFitter(int p1, int p2) {	
		this.p1=p1;
		this.p2=p2;
	}
	
	public void fit(int nsa, int nsb, int tet, int pedr, short[] pulse) {
		pedsum=0;adc=0;mmsum=0;summing_in_progress=0;
		for (int mm=0; mm<pulse.length; mm++) {
			if(mm>p1 && mm<=p2)  pedsum+=pulse[mm];
			if(mm==p2)           pedsum=pedsum/(p2-p1);				
			if(mm>p2) {
				ped = (pedr==0) ? pedr:pedsum;
				if ((summing_in_progress==0) && pulse[mm]>ped+tet) {
				  summing_in_progress=1;
				  t0 = mm;
				  for (int ii=1; ii<nsb+1;ii++) adc+=(pulse[mm-ii]-ped);
				  mmsum=nsb;
				}
				if(summing_in_progress>0 && mmsum>(nsa+nsb)) summing_in_progress=-1;
				if(summing_in_progress>0) {adc+=(pulse[mm]-ped); mmsum++;}
			}
		}
	}

} 

