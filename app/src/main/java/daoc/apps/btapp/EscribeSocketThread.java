package daoc.apps.btapp;

import java.io.ObjectOutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import android.widget.Toast;

public class EscribeSocketThread extends Thread {
	private final MainActivity mainActivity;
	private final ObjectOutputStream out;
	private final LinkedBlockingQueue<String> queue;

	public EscribeSocketThread(MainActivity mainActivity, ObjectOutputStream out) {
		this.mainActivity = mainActivity;
		this.out = out;
		queue = new LinkedBlockingQueue<String>();
	}

	public void sendMessage(String msg) {
		queue.offer(msg);
	}
	
	public void run() {
		try {
    		while(!Thread.interrupted()) {
    			String msg = queue.take();
    			out.writeObject(msg);
    			out.flush();
    		}
		} catch(Exception ex) {
			ex.printStackTrace();
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(EscribeSocketThread.this.mainActivity.getApplicationContext(), "ERROR tratando de escribir en el socket", Toast.LENGTH_SHORT).show();
				}
			});
			mainActivity.resetConnection();
		}	
	}
}