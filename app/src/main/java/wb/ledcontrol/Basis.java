package wb.ledcontrol;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Timer;

import org.apache.http.conn.util.InetAddressUtils;

import wb.ledcontrol.WBlog.wblogtype;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

//local service 

public class Basis extends Service {

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// data
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX

	//public static Handler Basishandler;				// Messagehandler zum threadsicheren Datenaustausch
	private static Context bcontext;
	private static BroadcastReceiver BcReceiver;		// BroadcastReceiver für die globalen Broadcasts (für DownloadManager - meldet die fertigen Downloads)
	private static LocalBroadcastManager LocBcManager;	// für locale Broadcasts in wb.control
	//private static Handler uihandler;					// Handler für Messages ans control-fragemnt
	private static Handler acthandler;					// Handler für Messages an die aktuelle Activity (FAct_control, WBcontrol_Startup)
	private static Timer fastTimer;	// 200ms Timer für diverse Aufgaben
	//private static DecimalFormat speedformat;	// für Speedausgabe im fasrTimer

	private static ArrayList<Device> devicelist; // Liste aller bekannten Devices
	private static Device selectedDevice;	// das ausgewählte und verbundene Device
	private static Thread UDPwaiterThread;
	private static String manuelle_ip;     	// für manuell eingegebene IP (wenn man Gerätesuche nicht verwenden will)
	//private static String name;             // der Name des Controllers (auf dem WBcontrol läuft)
	private static int tcpport;             // Port für TCP Kommunikation
	private static int udpport;             // Port für UDP Kommunikation
	private static int servertcpport;       // Port für TCP Kommunikation mit dem Server
	private static String eigeneip;      	// die IP des Controllers
	private static String broadcastip;      	// die Broadcst IP-Adresse für udp
    private static int displaymode;					// Bildschirmaufteilung: Anzeigemodus für Fragment-Anordnung (siehe: Display-Mode Typen weiter unten)
    private static int layouttype;					// ausgewähltes Layout
    private static int forceDisplaymode;			// Bildschirmaufteilung erzwingen (single/dual/multiview) 0 für nix erzwingen
    private static int screensavermode;				// Modus für "Bildschirmschoner deaktivieren" Typen siehe weiter unten
    private static PowerManager.WakeLock wlock;		// WakeLock für Bildschirmschoner-Einstellungen
    private static long dpPixels;					// dpPixel-Anzahl als Maß zum Abschätzen der Layoutgröße
    private static long dpPixels_dualview_threshold;	// dpPixel-Anzahl, ab der DUAL_VIEW verwendet wird
    private static float fontScale;					// zum Ändern der Textgröße	
    private static Boolean live_mode;				// zum Sichern des ToggleButton livemode nur zur Laufzeit
    
    
    private static final String PREFS_NAME = "ledcontrol.config";
        
    // Message Typen (msg.what) für acthandler				int arg1 / int arg2  / setdata(Bundle)
    public static final int MSG_BASIS_READY = 	1;			// Msg: Basis ist bereit
    public static final int MSG_BASIS_DEVICELIST_CHANGED = 2;	// Änderungen bei Devices (neue dazugekommen oder welche entfernt)
    public static final int MSG_BASIS_DEVICE_CONNECTED = 3;	// für das ausgewählte device wurde die Netzwerkverbindung hergestellt 
    public static final int MSG_BASIS_DEVICE_DISCONNECTED = 4;	// für das ausgewählte device wurde die Netzwerkverbindung getrennt

    
    //Message Typen für lokale Broadcasts
    public static final String ACTION_WLAN_DISCONNECTED = "wb.control.WLAN_DISCONNECTED";		// WLAN geht nicht mehr
    public static final String ACTION_WLAN_CONNECTED = "wb.control.WLAN_CONNECTED";				// WLAN funktioniert jetzt
    public static final String ACTION_DEVICELIST_CHANGED = "wb.control.DEVICELIST_CHANGED";		// Devicelist hat sich geändert -> Info an Fragments, falls Listen geändert werden müssen
    public static final String ACTION_NEW_LOGDATA = "wb.control.NEW_LOGDATA";					// neue Logenträge vorhanden -> Info an Fragments, falls Log angezeigt werden soll
    public static final String ACTION_UPDATE_AE = "wb.control.UPDATE_AE";					// eine ActionElement hat sich geändert. Extra: "aeindex" (int) = index in der Basis.actionelementlist
    public static final String ACTION_UPDATE_AE_DATA = "wb.control.UPDATE_AE_DATA";			// Daten haben sich geändert, die ein ActionElement vom Typ AE_TYP_Data verwenden könnten. Extra: "datatype" (int)(siehe ae.datatype)  und "device" (String) Devicename (siehe ae.scopedata)
    public static final String ACTION_QA_TACK_EDIT_ON = "wb.control.QA_TACK_EDIT_ON";		// QuickAction: Track-Edit Tools aktivieren
    public static final String ACTION_QA_TACK_EDIT_OFF = "wb.control.QA_TACK_EDIT_OFF";		// QuickAction: Track-Edit Tools aktivieren
        
    
    // Display-Mode Typen
    public static final int DISPLAYMODE_SINGLEVIEW = 	1;	// immer nur 1 Anzeigebereich am Display (Handy-Modus)
    public static final int DISPLAYMODE_DUALVIEW = 		2;		// 2 Anzeigebereiche nebeneinander (Querformat) oder untereinander (Hochformat) - für kleine Tablets mit geringer Auflösung (7")
    public static final int DISPLAYMODE_MULTIVIEW = 	3;		// mehr als 2 Anzeigebereiche sind möglich (10" Tablet mit ausreichender Auflösung)
    public static final int DISPLAYMODE_DUALVIEW_DPXY_THRESHOLD = 	288000;	// DEFAULTWERT dpXY für Verwendung von DUALVIEW (darunter SINGLEVIEW)
    
    // Layout Typen (Namen sind definiert in: values:arrays:layout_types)
    public static final int LAYOUT_CONTROL_L = 0;			// Control links
    public static final int LAYOUT_CONTROL_R = 1;			// Control rechts
    
    // Screensaver Mode Typen (siehe: values:arrays:wakelock_type)
    public static final int SSAVER_ACTIVE	= 0;	// Bildschirmschoner aktiv
    public static final int SSAVER_DIM		= 1;	// nur Helligkeit reduzieren (nicht ganz finster)
    public static final int SSAVER_INACTIVE	= 2;	// Bildschirmschoner deaktiviert
    
    // Theme Auswahl
    public static final int THEME_DARK	= 0;	// Dunkles Theme (=Standard)
    public static final int THEME_LIGHT	= 1;	// Helles Theme
    
    // Dialog-Typen
    public static final int DIALOG_ADD_DEV 		= 0;	// neues device anlegen
    public static final int DIALOG_EDIT_DEV 	= 1;	// device editieren
    
    //Netzwerktypen
    //public static final int NETWORK_WLAN = 1;	// Netzwerktyp WLAN
    
    
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// Getter/Setter: "Properties" für Data
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    
    
    public static Context getBcontext() {
		return bcontext;
	} 
           
    public static ArrayList<Device> getDevicelist() {
		return devicelist;
	}

	public static Device getSelectedDevice() {
		return selectedDevice;
	}

	public static void setSelectedDevice(Device selectedDevice) {
		Basis.selectedDevice = selectedDevice;
	}

	public static BroadcastReceiver getBcReceiver() {
		return BcReceiver;
	}

	public static void setBcReceiver(BroadcastReceiver bcReceiver) {
		BcReceiver = bcReceiver;
	}

	public static void setDevicelist(ArrayList<Device> devicelist) {
		Basis.devicelist = devicelist;
	}

	public static LocalBroadcastManager getLocBcManager() {
		return LocBcManager;
	}
    
	/*
    public static void setUIhandler(Handler uih) {
		uihandler = uih;
	}
	public static Handler getUIhandler() {
		return uihandler;
	} */
    
    public static Handler getActhandler() {
		return acthandler;
	}

	public static void setActhandler(Handler acthandler) {
		Basis.acthandler = acthandler;
	}

	public static void setUDPwaiterThread(Thread uDPwaiterThread) 
    {
    	UDPwaiterThread = uDPwaiterThread;
    }
    
	public static Thread getUDPwaiterThread() 
    {
    	return UDPwaiterThread;
    }
		
    public static void setManuelleIP(String ip)
    {
        manuelle_ip = ip;
    }
    public static String getManuelleIP()
    {
        return manuelle_ip;
    }
    
    /*
    public static void setName(String n)
    {
    	name = n;
    }
    
    public static String getName()
    {
    	return name;
    }  */
            
    public static void setTcpPort(int port)
    {
    	tcpport = port;
    }
    
    public static int getTcpPort()
    {
    	return tcpport;
    }
    
    public static void setUdpPort(int port)
    {
    	udpport = port;
    }
    
    public static int getUdpPort()
    {
    	return udpport;
    }
    
    public static void setServerTcpPort(int port)
    {
    	servertcpport = port;
    }
    
    public static int getServerTcpPort()
    {
    	return servertcpport;
    }
	
    public static void setEigeneIP(String ip)
    {
    	eigeneip = ip;
    }
    
    public static String getEigeneIP()
    {
    	return eigeneip;
    }
    
    public static void setBroadcastip(String broadcastip) {
		Basis.broadcastip = broadcastip;
	}
	public static String getBroadcastip() {
		return broadcastip;
	}
    	
	public static int getLayouttype() {
		return layouttype;
	}

	public static void setLayouttype(int layouttype) {
		Basis.layouttype = layouttype;
	}


	public static int getWakelockmode() {
		return screensavermode;
	}

	public static void setWakelockmode(int smode, Window w) {
		Basis.screensavermode = smode;
		
		switch(screensavermode)
		{
			case SSAVER_ACTIVE:	// Bildschirmschoner aktiv
				
				if (wlock != null) { wlock.release(); wlock = null; }
				w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
				
			case SSAVER_DIM:	// Helligkeit reduzieren
				PowerManager pm = (PowerManager) bcontext.getSystemService(Context.POWER_SERVICE);
				if (wlock == null)
				{
					wlock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "screendim");
					wlock.acquire();
				}
				break;
				
			case SSAVER_INACTIVE:	// Bildschirmschoner deaktiviert
				if (wlock != null) { wlock.release(); wlock = null; }
				w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
		}
		
	}
		
	public static int getDisplaymode() {
		return displaymode;
	}

	public static void setDisplaymode(int displaymode) {
		Basis.displaymode = displaymode;
	}

	public static int getForceDisplaymode() {
		return forceDisplaymode;
	}

	public static void setForceDisplaymode(int forceDisplaymode) {
		Basis.forceDisplaymode = forceDisplaymode;
	}

	public static float getFontScale() {
		return fontScale;
	}

	public static void setFontScale(float fontScale) {
		Basis.fontScale = fontScale;
	}

	public static long getDpPixels_dualview_threshold() {
		return dpPixels_dualview_threshold;
	}

	public static void setDpPixels_dualview_threshold(
			long dpPixels_dualview_threshold) {
		Basis.dpPixels_dualview_threshold = dpPixels_dualview_threshold;
	}

	public static long getDpPixels() {
		return dpPixels;
	}

	public static void setDpPixels(long dpPixels) {
		Basis.dpPixels = dpPixels;
	}
	

	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// lifecycle methods
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


	public static Boolean getLive_mode() {
		return live_mode;
	}

	public static void setLive_mode(Boolean live_mode) {
		Basis.live_mode = live_mode;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void onCreate() {
		super.onCreate();
				
		//bcontext = this.getBaseContext();
		bcontext = getApplication().getApplicationContext();
		devicelist = new ArrayList<Device>();
		
		Device dummy1 = new Device("Dummy1");
		Device dummy2 = new Device("Dummy2");
		dummy1.setTyp(Device.TYPE_RASPI_LED);
		dummy2.setTyp(0);
		devicelist.add(dummy1);
		devicelist.add(dummy2);
		displaymode = DISPLAYMODE_SINGLEVIEW;	// Displaymode wird in WBcontrolStartup bestimmt - hier Standardwert (Handy) voreinstellen
		dpPixels_dualview_threshold = DISPLAYMODE_DUALVIEW_DPXY_THRESHOLD;	// Defaultwert einstellen
		dpPixels = 0;
		layouttype = LAYOUT_CONTROL_L;
		forceDisplaymode = 0;	//nix forcen!
		screensavermode = 0;	// Bildschirmschoner aktiv
		fontScale = 1;
		wlock = null;
		
		LocBcManager = LocalBroadcastManager.getInstance(this);
		
		live_mode = false;	// für f_live
		
		// OS check
		/*
		try
		{
			//apiLevel = android.os.Build.VERSION.SDK_INT;
		}
		catch (Exception e)
		{
			//apiLevel = 0;	// bei API-Level < 4 (also Android 1.5 und niedriger) (Manifest: MinSDKversion ist auf 4 eingestellt)
		} */
		
		// Restore preferences
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);	
		manuelle_ip = settings.getString("manuelle_ip", "10.0.0.70");
		tcpport = settings.getInt("tcpport", 8003);
		udpport = settings.getInt("udpport", 8002);

		startNetwork();
		
	}	// end onCreate();
	
	
	public int onStartCommand(Intent intent, int flags, int startId)
	{    
		// wird nach onCreate ausgeführt UND bei jedem startService()-Aufruf (auch wenn der Basis-Service bereits läuft)
		// ist wichtig für Wiedereinstieg ins Programm, wenn Netzwerk noch nicht läuft
				
		acthandler.sendMessage(Message.obtain(acthandler, MSG_BASIS_READY));
		
		// We want this service to continue running until it is explicitly    
		// stopped, so return sticky (=1).    
		//return 1;
		
		return 0;
	} 
	

	@Override
	public void onDestroy() {
		
		
		//if (WLANreceiver != null) unregisterReceiver(WLANreceiver);	// WLAN-Status-Überwachung deaktivieren
		
		// fastTimer stoppen
		if (fastTimer != null) fastTimer.cancel();
		
		if (UDPwaiterThread != null) // UDP-Service beenden
    	{
    		Intent svc = new Intent(bcontext, UDPWaiter.class);
    		bcontext.stopService(svc);
    		UDPwaiterThread = null;
    	}
		
		// SharedPreferences abspeichern!		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("manuelle_ip", manuelle_ip);
		editor.putInt("tcpport", tcpport);
		editor.putInt("udpport", udpport);
		editor.commit();	// Commit the edits!
		
		/*
		WriteDevicestoDB(); 
		WriteAEtoDB();
		WriteMacrosToDB();
		WriteWBeventsToDB();
		*/
				
		super.onDestroy();
	}
  
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
	// andere Methoden
	//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX


	

       
	public static int startNetwork()	// netzwerk-relevanter Teil von onCreate(), wiederholbar, falls WLAN nicht aktiv ist
	{
		eigeneip = null;
		int wifistate = 99;
		String bssid = null;
		int ipAddress = 0;
		int error = 0;			// 1: WLAN nicht aktiv (wifistate != 3), 2: IP konnte nicht ermittelt werden, 4: IP ist keine gültige IPv4-Adresse
		// 8: WLAN aktiv, aber kein Netzwerk verbunden

		if (true)

		{

			try
			{
				// IP-Adresse ermitteln
				WifiManager wifiManager = (WifiManager) bcontext.getSystemService(WIFI_SERVICE);
				wifistate = wifiManager.getWifiState();
				//if (wifistate != 3) { error += 1; }	// TODO wurde für die tests entfernt
				//AddLogLine("WIFI-State = " + wifistate , "Basis", wblogtype.Info);
				//if (!wifiManager.isWifiEnabled()) { return false; }	// mit Fehler beenden, wenn WLAN disabled -> nicht machen, weil's auch im Test ohne echtes WLAN funktionieren soll

				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				ipAddress = wifiInfo.getIpAddress();
				bssid = wifiInfo.getBSSID();

			}
			catch (Exception e)
			{
				//AddLogLine("ERROR: bei WLAN-Check: " + e.toString(), "Basis", wblogtype.Error);
			}
		}
		if (ipAddress != 0)	{ eigeneip = Formatter.formatIpAddress(ipAddress); }	// ACHTUNG: funkt nur bei IPv4 !!!!!
		else { eigeneip = getLocalIpAddressNew(); /*getLocalIpAddress();*/ }	// für Tests ohne echtes WLAN (oder bei mobile-Betrieb): irgendeine lokale IP-Adresse ermitteln (-> per config deaktivierbar machen)

		if (eigeneip == null) { error += 2; return error; }	// mit Fehler beenden, wenn lokale IP-Adresse nicht ermittelt werden konnte


		//AddLogLine("eigene IP = " + eigeneip, "Basis", wblogtype.Info);
		String[] ipcheck = eigeneip.split("\\.");
		if (ipcheck.length != 4) { error += 4; return error; }	// mit Fehler beenden, lokale IP-Adresse nicht passt
		broadcastip = ipcheck[0] + "." + ipcheck[1] + "." + ipcheck[2] + ".255";
		//AddLogLine("Broadcast-IP = " + broadcastip, "Basis", wblogtype.Info);

		// fehlt: wenn nötig einen WifiLock einrichten

		if (error == 0)
		{
			if (UDPwaiterThread == null) { startUDPService(); }	// UDP-Service starten
			
		}

		return error;
	}
	
    private static void startUDPService() 	// UDP-Waiter starten
    {
    	try {
    		//AddLogLine("UDPWaiter-Service wird gestartet..", "UDPWaiter", wblogtype.Info);
    		Intent svc = new Intent(bcontext, UDPWaiter.class);
    		bcontext.startService(svc); // war original: startService(svc, Bundle.EMPTY);
    		//textView_startup.append("\r\nUDPWaiter-Service wurde gestartet..");
    	}
    	catch (Exception e) {
    		//AddLogLine("ERROR: UDPWaiter-Service konnte nicht gestartet werden!", "UDPWaiter", wblogtype.Error);
    	}
    }
    
    public static void stopNetwork()	// alles Nötige stoppen, wenn WLAN-Verbindung ausfällt
    {
    	   			
    	if (UDPwaiterThread != null) 
    	{
    		Intent svc = new Intent(bcontext, UDPWaiter.class);
    		bcontext.stopService(svc);
    		UDPwaiterThread = null;
    	}
    	
    	eigeneip = null;
    }
    
    
    
    // der devicelist ein Device hinzufügen
    public static void AddDevice(Device dev)
    {
    	devicelist.add(dev);
    	//sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
    	if (acthandler != null) { acthandler.sendMessage(Message.obtain(acthandler, MSG_BASIS_DEVICELIST_CHANGED));	}
    	
    }
    
    public static void RemoveDevice(Device dev)
    {
    	devicelist.remove(dev);
    	//sendLocalBroadcast(ACTION_DEVICELIST_CHANGED);
    	if (acthandler != null) { acthandler.sendMessage(Message.obtain(acthandler, MSG_BASIS_DEVICELIST_CHANGED));	}
    }
    
    public static Device getDeviceByName(String devname)
    {
    	Device dev = null;		// null = not found
    	for (Device d : devicelist)
    	{
    		if (d.getName().equals(devname)) { dev = d;  break; }
    	}
		return dev;
    }
       
    
	
	public static String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        //Basis.AddLogLine("Fehler beim Ermitteln der lokalen IP-Adresse: " + ex.toString(), "Basis", wblogtype.Error);
	    }
	    return null;
	}
	
	
	public static String getLocalIpAddressNew() {	// neue Version -> checken TODO
    	try {
    		String ipv4;
    		ArrayList<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
    		for (NetworkInterface ni: nilist) 
    		{
    			//Basis.AddLogLine("Network: " + ni.getDisplayName(), "Basis", wblogtype.Info);
    			ArrayList<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
    			for (InetAddress address: ialist)
    			{
    				//Basis.AddLogLine("IP: " + address.getHostAddress(), "Basis", wblogtype.Info);
    				
    				if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4=address.getHostAddress())) 
    				{ 
    					return ipv4;
    				}
    			}
 
    		}
 
    	} catch (SocketException ex) {
    		//Basis.AddLogLine(ex.toString(), "Basis", wblogtype.Error);
    	}
    	return null;
    }
	
	
	public static void sendLocalBroadcast(String msg)
	{
		if (LocBcManager != null)
		{
			LocBcManager.sendBroadcast(new Intent(msg));
		}
	}
	
	
	public static void useFontScale(Activity act)	// setzt die in der Basis gespeicherte fontScale für die übergebene Activity
	{
		// wirkt sich aber NUR auf das aktuelle Fragment sofort aus!!
		DisplayMetrics metrics = new DisplayMetrics();
		act.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		Resources res = act.getResources();
		Configuration config = res.getConfiguration();
		config.fontScale = Basis.fontScale;
		res.updateConfiguration(config, metrics);
	}
	
	
	public static void AddLogLine(String line, final String tag, final wblogtype type) {
		
		Log.d(tag,line);
		
	}
	

}	// end class Basis

