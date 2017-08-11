package org.clas.fcmon.ftof;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.clas.physics.GenericKinematicFitter;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.clas.physics.RecEvent;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.decode.CodaEventDecoder;
import org.jlab.detector.decode.DetectorDataDgtz;
import org.jlab.detector.decode.DetectorEventDecoder;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.base.Detector;
import org.jlab.geom.component.ScintillatorMesh;
import org.jlab.geom.component.ScintillatorPaddle;
import org.jlab.geom.detector.ftof.FTOFDetector;
import org.jlab.geom.detector.ftof.FTOFDetectorMesh;
import org.jlab.geom.detector.ftof.FTOFFactory;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Path3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
//import org.jlab.calib.temp.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.evio.EvioDataBank;
import org.jlab.io.evio.EvioDataEvent;
import org.jlab.utils.groups.IndexedList;
import org.jlab.utils.groups.IndexedTable;


/**
 *
 * @author gavalian
 */
public class DataProvider {

    public static boolean test = false;
    
    public static double thrADC = 0; 

    public static List<TOFPaddle> getPaddleList(DataEvent event) {

        List<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();

        if (test) eventShow(event);
        
		if (!event.hasBank("FTOF::adc") || !event.hasBank("FTOF::tdc")) {
			return paddleList;
		}        
		
		paddleList = getPaddleListHipo(event);

        return paddleList;

    }
    
    public static void eventShow(DataEvent event) {
    	
        event.show();
        
        if (event.hasBank("FTOF::adc")) {
            event.getBank("FTOF::adc").show();
        }
        if (event.hasBank("FTOF::tdc")) {
            event.getBank("FTOF::tdc").show();
        }
        if (event.hasBank("FTOF::hits")) {
            event.getBank("FTOF::hits").show();
        }
        if (event.hasBank("HitBasedTrkg::HBTracks")) {
            event.getBank("HitBasedTrkg::HBTracks").show();
        }
        if (event.hasBank("TimeBasedTrkg::TBTracks")) {
            event.getBank("TimeBasedTrkg::TBTracks").show();
        }           
        if (event.hasBank("RUN::rf")) {
            event.getBank("RUN::rf").show();
        }
        if (event.hasBank("RUN::config")) {
            event.getBank("RUN::config").show();
        }
        if (event.hasBank("MC::Particle")) {
            event.getBank("MC::Particle").show();
        }    	
    }
    
    public static List<TOFPaddle> getPaddleListHipo(DataEvent event) {
    	
        List<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();
        
        DataBank  adcBank = event.getBank("FTOF::adc");
        DataBank  tdcBank = event.getBank("FTOF::tdc");

        for (int i = 0; i < adcBank.rows(); i++) {
            int order = adcBank.getByte("order", i);
            int adc = adcBank.getInt("ADC", i);
            if (order==0 && adc != 0) {

                int sector = adcBank.getByte("sector", i);
                int layer = adcBank.getByte("layer", i);
                int component = adcBank.getShort("component", i);
                int adcL = adc;
                int adcR = 0;
                float adcTimeL = adcBank.getFloat("time", i);
                float adcTimeR = 0;
                int tdcL = 0;
                int tdcR = 0;

                for (int j=0; j < adcBank.rows(); j++) {
                    int s = adcBank.getByte("sector", j);
                    int l = adcBank.getByte("layer", j);
                    int c = adcBank.getShort("component", j);
                    int o = adcBank.getByte("order", j);
                    if (s==sector && l==layer && c==component && o == 1) {
                        // matching adc R
                        adcR = adcBank.getInt("ADC", j);
                        adcTimeR = adcBank.getFloat("time", j);
                        break;
                    }
                }

                for (int tdci=0; tdci < tdcBank.rows(); tdci++) {
                    int s = tdcBank.getByte("sector", tdci);
                    int l = tdcBank.getByte("layer", tdci);
                    int c = tdcBank.getShort("component", tdci);
                    int o = tdcBank.getByte("order", tdci);
                    if (s==sector && l==layer && c==component && o == 2) {
                        // matching tdc L
                        tdcL = tdcBank.getInt("TDC", tdci);
                        break;
                    }
                }
                for (int tdci=0; tdci < tdcBank.rows(); tdci++) {
                    int s = tdcBank.getByte("sector", tdci);
                    int l = tdcBank.getByte("layer", tdci);
                    int c = tdcBank.getShort("component", tdci);
                    int o = tdcBank.getByte("order", tdci);
                    if (s==sector && l==layer && c==component && o == 3) {
                        // matching tdc R
                        tdcR = tdcBank.getInt("TDC", tdci);
                        break;
                    }
                }

                if (test) {
                    System.out.println("Values found "+sector+layer+component);
                    System.out.println(adcL+" "+adcR+" "+tdcL+" "+tdcR);
                }

                if (adcL>thrADC || adcR>thrADC) {

                    TOFPaddle  paddle = new TOFPaddle(sector,layer,component);
                    
                    paddle.setAdcTdc(adcL, adcR, tdcL, tdcR);
                    paddle.ADC_TIMEL = adcTimeL;
                    paddle.ADC_TIMER = adcTimeR;
                    
                    paddleList.add(paddle);                         

                }
            }
        }

    	    return paddleList;
    }


}

