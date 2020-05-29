package daoc.apps.btapp;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	private TextView conv;
	private EditText msg;
	private BluetoothAdapter btAdapter;
	private BluetoothServerSocket srvSocket;
	private BluetoothSocket socket;
	private BluetoothDevice serverDevice;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private UUID my_uuid;
	private String app_name;
	private EscribeSocketThread enviar;
	private LeeSocketThread recibir;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        msg = (EditText) findViewById(R.id.msg);
        conv = (TextView) findViewById(R.id.conv);
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        
        //my_uuid = UUID.fromString("00000000-1111-2222-3333-444444444444");
        my_uuid = UUID.nameUUIDFromBytes("TextoParaGenerarUuid".getBytes());
        app_name = "BtApp";
    }
    
    public void conDescon(View view) {
    	ToggleButton button = (ToggleButton) view;
    	RadioButton radioMaster = (RadioButton) findViewById(R.id.radioMaster);
    	RadioButton radioSlave = (RadioButton) findViewById(R.id.radioSlave);    	
		if(btAdapter == null) {
			Toast.makeText(this, "ERROR: NO hay soporte de Bluetooth", Toast.LENGTH_SHORT).show();
			resetConnection();	
			return;
		}
        if (!btAdapter.isEnabled()) {
        	Toast.makeText(this, "Aún no se habilita Bluetooth. Intente de nuevo", Toast.LENGTH_SHORT).show();
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
            resetConnection();
            return;
        }
        if(btAdapter.isDiscovering()) {
        	btAdapter.cancelDiscovery();
        }    	
    	if(button.isChecked()) {
    		radioMaster.setEnabled(false);
    		radioSlave.setEnabled(false);
    		if(radioMaster.isChecked()) {
    			activaServidor();
    		} else {
    			activaCliente();
    		}
    	} else {
    		resetConnection();       		
    	}
    }
    
    private void activaServidor() {
		new Thread() {
		    public void run() {
		    	try {
					srvSocket = btAdapter.listenUsingRfcommWithServiceRecord(app_name, my_uuid);
					socket = srvSocket.accept();
					srvSocket.close();
					srvSocket = null;
					out = new ObjectOutputStream(socket.getOutputStream());
					in = new ObjectInputStream(socket.getInputStream());
					enviar = new EscribeSocketThread(MainActivity.this, out);
					enviar.start();
					recibir = new LeeSocketThread(MainActivity.this, in);
					recibir.start();
		    	} catch(Exception ex) {
		    		ex.printStackTrace();
					MainActivity.this.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(getApplicationContext(), "ERROR: No se puede activar servidor", Toast.LENGTH_SHORT).show();
						}
					});    		
					resetConnection();	    		
		    	}		    	
		    }
		}.start();    	
    }
    
    private void activaCliente() {
    	final BluetoothDevice[] btDevs = btAdapter.getBondedDevices().toArray(new BluetoothDevice[0]);
    	CharSequence[] devsNames = new CharSequence[btDevs.length];
    	for(int i = 0; i < devsNames.length; i++) {
    		devsNames[i] = btDevs[i].getName();
    	}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Escoja el servidor");
    	builder.setItems(devsNames, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	serverDevice = btDevs[which];
            	try {
        	    	socket = serverDevice.createRfcommSocketToServiceRecord(my_uuid);
        	    	socket.connect();
					out = new ObjectOutputStream(socket.getOutputStream());
					in = new ObjectInputStream(socket.getInputStream());
					enviar = new EscribeSocketThread(MainActivity.this, out);
					enviar.start();
					recibir = new LeeSocketThread(MainActivity.this, in);
					recibir.start();
            	} catch(Exception ex) {
            		ex.printStackTrace();
            		Toast.makeText(getApplicationContext(), "ERROR: activando cliente", Toast.LENGTH_SHORT).show();  		
        			resetConnection();
            	}
            }
        });
    	builder.create().show();

    }
    
    public void sendMsg(View view) {
    	String str = msg.getText().toString();
    	if(!str.equals("") && enviar != null && enviar.isAlive()) {
    		msg.setText(null);
    		conv.append("<= " + str + "\n");
    		enviar.sendMessage(str);
    	}
    }
    
    public void receiveMsg(String msg) {
		conv.append("=> " + msg + "\n");
    }
    
    void resetConnection() {
    	ToggleButton button = (ToggleButton) findViewById(R.id.togConDescon);;
    	RadioButton radioMaster = (RadioButton) findViewById(R.id.radioMaster);
    	RadioButton radioSlave = (RadioButton) findViewById(R.id.radioSlave);    	
		radioMaster.setEnabled(true);
		radioSlave.setEnabled(true);
		button.setChecked(false);
		try {
			if(enviar != null) {
				enviar.interrupt();
				enviar = null;
			}
			if(recibir != null) {
				recibir.interrupt();
				recibir = null;
			}			
			if(out != null) {
				out.close();
				out = null;
			}			
			if(in != null) {
				in.close();
				in = null;
			}
			if(socket != null) {
				socket.close();
				socket = null;
			}		
		} catch(Exception ex) {
			ex.printStackTrace();
			Toast.makeText(this, "ERROR tratando de resetear la conexión", Toast.LENGTH_SHORT).show();
		}
    }
        
}
