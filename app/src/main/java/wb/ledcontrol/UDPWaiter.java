package wb.ledcontrol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import wb.ledcontrol.WBlog.wblogtype;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class UDPWaiter extends Service {

	private Thread UDPwaiterThread;
	public Handler UDPhandler;
	private Boolean end;
	private DatagramSocket UDPsocket = null;
	private InetAddress broadcastAdr = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
			StartUDP();
	}



	@Override
	public void onDestroy() {

		end = true;		// dem thread das Beenden signalisieren!

		super.onDestroy();
		Basis.AddLogLine("UDPwaiter-Service wurde beendet!", "udp", wblogtype.Info);
		
	}
	
	//########################################################################
	
	
	private void StartUDP() {
		
		UDPwaiterThread = new Thread(new Runnable() { 
			public void run() {

				boolean connected = false;
				try {
					connected = connectUDP();
					if (connected) { doUDPwork(); }
					else { onDestroy(); }

				} catch (IOException e) {
					//Basis.AddLogLine(e.toString(), "udp", wblogtype.Error);
				}

			} 
		}, "UDPwaiter"); 

		UDPwaiterThread.start();
		Basis.setUDPwaiterThread(UDPwaiterThread);	// in Basis speichern
		//Basis.AddLogLine("UDPwaiter-Thread wurde gestartet!", "udp", wblogtype.Info);

	}	// end StartUDP()
	
	
	private boolean connectUDP()
	{
		boolean success = true;
		//String eigeneIP = Basis.getEigeneIP();		// nicht threadsafer Aufruf
		int udpport = Basis.getUdpPort();		// nicht threadsafer Aufruf
		//String controllername = Basis.getName();	// nicht threadsafer Aufruf

		try {
			//broadcastAdr = InetAddress.getByName("10.0.0.255");	// Achtung: muss noch geändert werden: eigene Adresse mit 255 (oder alle 255)
			// könnte sein, dass das nicht funkt // 255.255.255.255 funkt jedenfalls nicht (win)
			String bip = Basis.getBroadcastip();	// Proadcast-IP wurde beim Start von Basis-Service ermittelt
			if ((bip == null) || (bip == "")) { bip = "10.0.0.255"; }	// im Fehlerfall mit meiner Standard broadcast-ip weitermachen
			broadcastAdr = InetAddress.getByName(bip);

			//Basis.AddLogLine("Connecting...", "udp", wblogtype.Info);

			UDPsocket = new DatagramSocket(udpport);	// UDPsocket
			if (UDPsocket == null) 
			{ 
				//Basis.AddLogLine("Socket konnte nicht erstellt werden!", "udp", wblogtype.Warning); 
			}
			UDPsocket.setBroadcast(true);
			UDPsocket.setSoTimeout(1000);		// Timeout in [ms] for blocking accept or read/receive operations (but not write/send operations). A timeout of 0 means no timeout.

			//String data = "<iamcontrol:" + controllername + ">";
			//DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), broadcastAdr, udpport);
			//UDPsocket.send(packet);		// i-am-new Medung an alle, dass der Controller seinen Betrieb aufgenommen hat
		}
		catch (UnknownHostException e1) {	 Basis.AddLogLine(e1.toString(), "udp", wblogtype.Error); success = false; 	}
		//catch (SocketTimeoutException e1) {	 Basis.AddLogLine(e1.toString(), "udp", wblogtype.Error); success = false;	}
		catch (SocketException e1) {  Basis.AddLogLine(e1.toString(), "udp", wblogtype.Error);  success = false;	}
		//catch (IOException e1) {	 Basis.AddLogLine(e1.toString(), "udp", wblogtype.Error);  success = false;	}
		
		return success;
	}
			
	
	public void SendUDP(String data) {

		if (UDPsocket != null)
		{
			DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), broadcastAdr, Basis.getUdpPort());

			try {
				UDPsocket.send(packet);
			} catch (IOException e) {
				//Basis.AddLogLine("Fehler beim Senden: " + e.toString(), "udp", wblogtype.Error);
			}
		}
	}
	
	
	private void doUDPwork() throws IOException {

		end = false;
		byte[] buf = new byte[8192];	// Empfangspuffer
		String test = "";	// empfangener, auswertbarer Text
		String senderIP = "";
		Boolean timeout = false;

		while (!end && (UDPsocket != null) && (!UDPsocket.isClosed()))
		{
			Arrays.fill(buf, (byte) 0);	// buffer leeren
			timeout = false;
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try
			{
				UDPsocket.receive(packet);
			}
			catch (SocketTimeoutException e) // timeout abfangen, falls nichts gesendet wird
			{
				timeout = true;
			}

			if (!timeout)
			{
				senderIP = packet.getAddress().getHostAddress();	// packet.getAddress().toString();
				String newtxt = new String(buf, 0, packet.getLength());
				//String newtxt3 = new String(packet.getData());		// liefert den gesamten buffer als String zurück (mit 8000 Leerzeichen)
				Boolean worktodo = true;

				if (!senderIP.equals(Basis.getEigeneIP()))		// UDP-Meldungen vom eigenen Gerät ignorieren
				{
					test += newtxt;
					// String test_raw = new String(test);	// den aktuellen buffer incl. <comand> aufheben für ev. spezial-auswerung
					//Basis.AddLogLine("waiterthread: string geholt: " + test, "udp", wblogtype.Info);
				}
				else { worktodo = false; }	// Befehle vom eigenen Gerät ignorieren
				
				while (worktodo && !end)
				{
					int startpos = test.indexOf("<");
					int endpos = test.indexOf(">");
					
					if (endpos < startpos) { Basis.AddLogLine("UDPwaiter: Mist im Befehlsbuffer: " + test, "udp", wblogtype.Warning); }
					if (test.length() > 512) { Basis.AddLogLine("UDPwaiter: Befehlsbuffer ist überfüllt!", "udp", wblogtype.Warning); }
					
					String command = "";
					
					if ((startpos >= 0) && (endpos > 0) && (startpos < endpos)) // gültiger WBcontrol-Befehl ist vorhanden
					{
						command = test.substring(startpos + 1, endpos);	// substring von start to end-1 !!!!!
						//if (test.length() > endpos+1) { test = test.substring(endpos); }	//.Remove(startpos, endpos - startpos + 1);   // aktuellen command aus test entfernen Error: Count bei Remove um 1 zu klein!!
						if (test.length() > endpos+1) { test = test.substring(endpos+1); }
						else { test = ""; }
						//Basis.AddLogLine("UDPwaiter: Befehl wird ausgewertet: " + command, "udp", wblogtype.Info);
						WBcontrol_BefehlAuswerten(command, senderIP);
					}
					else  // wenn kein kompletter Command mehr im string "test" enthalten ist
					{
						worktodo = false;

					}
				}
			} // end if (!timeout)

		}	// end while (!end)
		//Basis.AddLogLine("UDPwaiter-Thread Arbeitsloop wurde verlassen!", "udp", wblogtype.Info);
		Log.d("udp", "UDPwaiter-Thread Arbeitsloop wurde verlassen!");
		// Aufräumarbeiten
		if (UDPsocket != null) { UDPsocket.close(); }
		//Basis.AddLogLine("UDPwaiter-Thread wird beendet!", "udp", wblogtype.Info);
		//Looper.myLooper().quit();	// message loop beenden
		
		// msg an endhandler, dass thread beendet ist
		
	}	// end doUDPwork()
	
	private void WBcontrol_BefehlAuswerten(String command, String sender_ip) throws IOException  // wertet den übergebenen Befehl aus
    {
		//Basis.AddLogLine("udpwaiter: befehlsauswertung für: " + command, "udp", wblogtype.Info);

		if (command.equals("test"))  // c[1] = lokname
        {
			// test
        }
		
		
		else if (command.startsWith("iamledc:"))  // Raspi LEDcontroller c[1] = name	
        {
            String[] c = command.split(":");
            Boolean found = false;
            for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
            {
                if ((ding.getTyp() == Device.TYPE_RASPI_LED) && (ding.getName().equals(c[1]))) { found = true; }
            }
            if (!found)
            {
                // zuest noch noch nach unbekannt+ip suchen, und ggf. dort Namen anpassen!
                for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
                {
                    if ((ding.getTyp() == Device.TYPE_RASPI_LED) && (ding.getName().equals("unbekannt " + sender_ip)))
                    { 
                        found = true;
                        ding.setName(c[1]);   // Namen korrigieren
                    }
                }

                if (!found) // wenn auch kein passender unbekannter gefunden wurde -> neues Gerät hinzufügen
                {
                    Device newdev = new Device(c[1]);
                    newdev.setTyp(Device.TYPE_RASPI_LED);
                    newdev.setIp(sender_ip);
                	Basis.AddDevice(newdev);
                	
                	/*
                	Handler uihandler = Basis.getUIhandler();
					if (uihandler != null) 
					{
						Message msg1 = Message.obtain(uihandler, Frag_control.MSG_UPDATE_LOKLIST);	// Loklist updaten!
						uihandler.sendMessage(msg1);
					} */
                } 
            }
            //Basis.AddLogLine("Meldung von Lok: " + c[1], "udpwaiter", wblogtype.Info);
        }
		
		
		else if (command.startsWith("iarcs"))  // Raspi CameraServer (servogesteuert)
        {
            String[] c = command.split(":");
            Boolean found = false;
            for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
            {
                if ((ding.getTyp() == Device.TYPE_CAM) && (ding.getName().equals(c[1]))) { found = true; }
            }
            if (!found)
            {
                // zuest noch noch nach unbekannt+ip suchen, und ggf. dort Namen anpassen!
                for (Device ding : Basis.getDevicelist())  // checken, ob bereits vorhanden
                {
                    if ((ding.getTyp() == Device.TYPE_CAM) && (ding.getName().equals("unbekannt " + sender_ip)))
                    { 
                        found = true;
                        ding.setName(c[1]);   // Namen korrigieren
                    }
                }

                if (!found) // wenn auch kein passender unbekannter gefunden wurde -> neues Gerät hinzufügen
                {
                    Device newdev = new Device(c[1]);
                    newdev.setTyp(Device.TYPE_CAM);
                    newdev.setIp(sender_ip);
                	Basis.AddDevice(newdev);
                	
                	/*
                	Handler uihandler = Basis.getUIhandler();
					if (uihandler != null) 
					{
						Message msg1 = Message.obtain(uihandler, Frag_control.MSG_UPDATE_LOKLIST);	// Loklist updaten!
						uihandler.sendMessage(msg1);
					} */
                } 
            }
            //Basis.AddLogLine("Meldung von Lok: " + c[1], "udpwaiter", wblogtype.Info);
        }
        



            


    
            else  // Befehl unbekannt
            {
                //Basis.AddLogLine("unbekannter Befehl: " + command, "udpwaiter", wblogtype.Warning);
                //StatusText = "udpwaiter: unbekannter Befehl: " + command;
            }
    }
	

}	// end class UDPWaiter
