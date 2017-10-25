package org.clas.fcmon.tools;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jlab.clas.mq.IpcServer;
import org.jlab.groot.data.H1F;



public class IPCDemo {
	
	public static void main(String[] args){
	       String server = args[0];
	       String topic  = args[1];

	       //IpcReceiver receiver = new IpcReceiver(server);

	       //IpcServer ipc = new IpcServer("tcp://clondb1.jlab.org:61616","clasrun.*.*.*");
	       IpcServer ipc = new IpcServer(server,topic);
	       Random  rand = new Random();



	       while(true){

	           H1F h1d_1 = new H1F("H100","Random Histogram #1",100,0.0,1.0);
	           h1d_1.setUniqueID(100L);

	           for(int i = 0; i < 200; i++){ 
	               h1d_1.fill(rand.nextGaussian());
	           }

	           ipc.sendData(h1d_1);

	           H1F h1d_2 = new H1F("H101","Random Histogram #2",100,0.0,4.0);
	           h1d_2.setUniqueID(101L);

	           for(int i = 0; i < 450; i++){ 
	               h1d_2.fill(rand.nextGaussian()*4);
	           }

	           ipc.sendData(h1d_2);

	           try {
	               Thread.sleep(1000);
	           } catch (InterruptedException ex) {
	               Logger.getLogger(IpcServer.class.getName()).log(Level.SEVERE, null, ex);
	           }

	       }
	   }
}
