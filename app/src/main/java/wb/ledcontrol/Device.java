package wb.ledcontrol;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Handler;
import android.os.Message;

public class Device {

	private String name;	// Devicename
	private int typ;
	private Boolean predef;	// true: ein vordefiniertes Device, false: ein live gemeldetes 
	private String ip;
	private Thread netwaiterthread;		// Thread für die TCP-Kommunikation mit dem Device
	//private NetWaiter nwaiter;
	private boolean connected;			// zeigt an, ob eine tcp-Verbindung besteht, oder nicht
	private boolean exitthread;			// das checken die Threads (NetWaiter + NetWriter) zyklisch um zu erfahren, ob sie sich beenden sollen
	private LinkedList<String> NetwriteQueue;	// commands, die ans Device gesendet werden sollen (wird vom NetWaiterThread abgearbeitet) 
	private ReentrantLock NetwriteQueueLock;	// für NetwriteQueue
    
	//Device types: 0 = ndefiniert
	public static final int TYPE_RASPI_LED	= 1;	// Raspberry PI LED-Controller
	public static final int TYPE_CAM		= 2;	// raspicamserver
	
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// Konstruktor
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	public Device(String devname) {
		name = devname;
		ip = "";
		typ = 0;
		setPredef(false);
		
		NetwriteQueue = new LinkedList<String>();
	    NetwriteQueueLock = new ReentrantLock();
		
	}

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// get/set
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTyp() {
		return typ;
	}

	public void setTyp(int typ) {
		this.typ = typ;
	}

	public Boolean getPredef() {
		return predef;
	}

	public void setPredef(Boolean predef) {
		this.predef = predef;
	}

	public String getIP() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Thread getNetwaiterthread() {
		return netwaiterthread;
	}

	public void setNetwaiterthread(Thread netwaiterthread) {
		this.netwaiterthread = netwaiterthread;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
		if ((connected) && (this == Basis.getSelectedDevice()))
		{
			Handler acthandler = Basis.getActhandler();
			if (acthandler != null) { acthandler.sendMessage(Message.obtain(acthandler, Basis.MSG_BASIS_DEVICE_CONNECTED));	}
		}

		if ((!connected) && (this == Basis.getSelectedDevice()))
		{
			Handler acthandler = Basis.getActhandler();
			if (acthandler != null) { acthandler.sendMessage(Message.obtain(acthandler, Basis.MSG_BASIS_DEVICE_DISCONNECTED));	}
		}
		
		
	}

	public boolean getExitThread() {
		return exitthread;
	}

	public void setExitthread(boolean exitthread) {
		this.exitthread = exitthread;
	}
	
	
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	

	@Override
    public String toString()
    {
		return this.name;
    }
	
	public boolean Connect()	// throws IOException 
	{
		// vom User angelegte Devices ohne IP-Adresse gleich abfangen
		if (this.ip.equals("")) {  return false; }	// TCPsocket = null; // war noch dabei

		if (!connected)
		{
			exitthread = false;		// Kennung für Thread zum Beenden entschärfen
			//nwaiter = new NetWaiter(this);
			netwaiterthread = new Thread(new NetWaiter(this), "NetWaiter_" + this.name);	// dem NetWaiter das aktuelle Device übergeben!
			netwaiterthread.start();
			
			Basis.setSelectedDevice(this);

			return true;
		}
		else { return false;	}
		
	}

	public void Disconnect()
	{
		exitthread = true;	// Kennung für Threads, dass sie sich beenden sollen (Msg wäre hier zu aufwendig)
	}

	
	public void Netwrite(String text) // Msg an Thread senden (mit Text). Der Rest wird jetzt im thread gemacht!!
	{
		if (connected) {
			NetwriteQueueLock.lock();
			try 
			{
				NetwriteQueue.add(text);
			}
			finally { NetwriteQueueLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall)

		}
	}
	
	public String getNetCmdtoSend()
	{
		String data = null;
		
		NetwriteQueueLock.lock();
    	try 
    	{
    		data = NetwriteQueue.poll();
    	}
    	finally { NetwriteQueueLock.unlock(); }	// Lock auf jeden Fall wieder freigeben (auch im Fehlerfall
		
		return data;
	}
	

	
} // end class device
