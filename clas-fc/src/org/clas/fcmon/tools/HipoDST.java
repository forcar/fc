package org.clas.fcmon.tools;

import org.jlab.hipo.data.HipoEvent;
import org.jlab.hipo.data.HipoGroup;
import org.jlab.hipo.io.HipoReader;
import org.jlab.hipo.io.HipoWriter;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataSource;

public class HipoDST {
	
	public static void readNtuple() {
		
//		HipoReader reader = new HipoReader();
		HipoDataSource reader = new HipoDataSource();
		reader.open("/Users/colesmith/hipo_test_ntuple.hipo");
		Integer current = reader.getCurrentIndex();
        Integer nevents = reader.getSize(); 
        System.out.println("Current event:"+current+" Nevents: "+nevents);
        for (int i=0; i<nevents; i++) {
            DataEvent event = reader.getNextEvent();
            if(event.hasBank("Event")==true) {              
            	DataBank bank = event.getBank("Event");
                System.out.println("nrows="+bank.rows());
            	bank.show();
            }
		}	
		reader.close();
	}
	
	public static void writeNtuple(){
		    
	   HipoWriter writer = new HipoWriter();
	        
	   writer.defineSchema("Event", 20, "id/I:px/F:py/F:pz/F");
	        
	   writer.setCompressionType(2);
	   writer.open("/Users/colesmith/hipo_test_ntuple.hipo");
       int nevents = 10;
	        
	   for(int i = 0; i < 1; i++){
	       HipoGroup group = writer.getSchemaFactory().getSchema("Event").createGroup(4);
	       for (int j=0; j < 3; j++) {
	           group.getNode("id").setInt(j, 211);
	           group.getNode("px").setFloat(j, (float) Math.random());
	           group.getNode("py").setFloat(j, (float) Math.random());
	           group.getNode("pz").setFloat(j, (float) Math.random());
	       }
	       
	       HipoDataBank bank = new HipoDataBank(group);
	       bank.show();
	       HipoEvent event = writer.createEvent();
	       event.writeGroup(group);
	       writer.writeEvent(event);
	   }
	   writer.close();
	}

    public static void main(String[] args){        
        HipoDST.writeNtuple();
        HipoDST.readNtuple();
    }
    
}
