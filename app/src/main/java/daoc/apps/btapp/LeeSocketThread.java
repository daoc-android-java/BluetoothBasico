package daoc.apps.btapp;

import java.io.ObjectInputStream;

import android.widget.Toast;

public class LeeSocketThread extends Thread {
	private final MainActivity mainActivity;
	private final ObjectInputStream in;
	
	public LeeSocketThread(MainActivity mainActivity, ObjectInputStream in) {
		this.mainActivity = mainActivity;
		this.in = in;
	}
	
	public void run() {
		try {
    		while(!Thread.interrupted()) {
    			final String msg = (String) in.readObject();
    			mainActivity.runOnUiThread(new Runnable() {
				    public void run() {
				    	mainActivity.receiveMsg(msg);
				    }
				});
    		}
		} catch(Exception ex) {
			ex.printStackTrace();
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(mainActivity, "ERROR tratando de leer en el socket", Toast.LENGTH_SHORT).show();
				}
			});
			mainActivity.resetConnection();
		}
	}
}