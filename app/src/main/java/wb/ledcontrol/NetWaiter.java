package wb.ledcontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import wb.ledcontrol.WBlog.wblogtype;

public class NetWaiter implements Runnable {
	private Device dev;
	
	//Variablen ehemals vom Device
	private Socket TCPsocket;
	// private PrintWriter output;
	// private BufferedWriter  output;
	private BufferedReader input;
	private Thread netwriterthread;
	//private NetWriter nwriter;
	
	
	public NetWaiter(Device d)	//Konstruktor zur Übergabe des Devices
	{
		dev = d;
	}

	@Override
	public void run() {	// run wird beim Starten des threads ausgeführt
				
		boolean success = connect();
		
		if (success)
		{
			//sendMsgToUIthread(wb.control.Frag_control.MSG_DEVICE_CONNECTED);
			doTCPwork();	// Haupt-Arbeitsschleife
		}
		/*
		else	// Msg senden, daß Verbindung fehlgeschlagen ist 
		{
			sendMsgToUIthread(wb.control.Frag_control.MSG_DEVICE_CONNECING_FAILED);
		} */
		
	}
    
	/*
    public void sendMsgToUIthread(int what)	// Msg an den Main(UI)-thread schicken 
    {
    	Handler uihandler = Basis.getUIhandler();
		if (uihandler != null) 
		{
			Message msg1 = Message.obtain(uihandler, what);
			Bundle bundle = new Bundle();
			bundle.putString("devname", dev.getName());	//Devicenamen zur Identifikation mitgeben
			msg1.setData(bundle);
			uihandler.sendMessage(msg1);
		}
    } */
    
	
	
	private boolean connect()
	{
		int tcpport = Basis.getTcpPort();
		//if (dev.getTyp() == DeviceType.Server) { tcpport = Basis.getServerTcpPort(); }

		Basis.AddLogLine("Verbindung wird hergestellt: IP=" + dev.getIP() + " Port=" + tcpport, "Device", wblogtype.Info);

		//this.tcpClient = new TcpClient(new IPEndPoint(Config.eigeneIP, tcpport));
		try
		{
			//TCPsocket = new Socket(this.ip, tcpport);
			TCPsocket = new Socket();
			TCPsocket.setSoTimeout(1000);	// 1 Sekunden Read-Timeout
			//TCPsocket.setSoTimeout(0);	// no Read-Timeout -> blockt, bis etwas empfangen wurde -> darf nicht sein, sonst wurd der NetWaiter bei Netz-Trennung nie beendet 
			//SocketAddress localadr = new InetSocketAddress(Basis.getEigeneIP(),tcpport);
			SocketAddress localadr = new InetSocketAddress(Basis.getEigeneIP(),0);	// port = 0 -> freien Port wählen
			TCPsocket.bind(localadr);
			SocketAddress devadr = new InetSocketAddress(dev.getIP(),tcpport);
			TCPsocket.connect(devadr, 5000);	// 5 Sekunden Connect-Timeout
		}
		catch (IOException e)
		{
			Basis.AddLogLine(e.toString(), "Device", wblogtype.Error);
			return false;
		}
		catch (SecurityException e)
		{
			Basis.AddLogLine("Verbindung wurde verweigert: " + e.toString(), "Device", wblogtype.Error);
			return false;
		}
		catch (Exception e)
		{
			Basis.AddLogLine("Unbekannter Fehler bei Verbindung: " + e.toString(), "Device", wblogtype.Error);
			return false;
		}

		if (TCPsocket != null)	// wenn der Socket nicht erstellt werden konnte
		{

			if (!TCPsocket.isConnected())
			{
				try { TCPsocket.close(); } 
				catch (IOException e) { Basis.AddLogLine(e.toString(), "Device", wblogtype.Error); }
				finally { TCPsocket = null; }
			}

			if (TCPsocket == null) { return false; }

			try
			{
				//output = new PrintWriter(TCPsocket.getOutputStream());
				//output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(TCPsocket.getOutputStream())));
				//output = new BufferedWriter(new OutputStreamWriter(TCPsocket.getOutputStream()));
				input = new BufferedReader(new InputStreamReader(TCPsocket.getInputStream()));
			}
			catch(UnknownHostException e) 
			{
				Basis.AddLogLine("Fehler beim Erstellen der tcp-i/o-Kanäle: Unknown host" + e.toString(), "Device", wblogtype.Error);

			}
			catch (IOException e)
			{
				Basis.AddLogLine("Fehler beim Erstellen der tcp-i/o-Kanäle: " + e.toString(), "Device", wblogtype.Error);
				return false;
			}

			//nwriter = new NetWriter(dev, TCPsocket);
			netwriterthread = new Thread(new NetWriter(dev, TCPsocket), "NetWriter_" + dev.getName());	// NetWriter-Thread erzeugen (übernimmt alles tcp-senden)
			netwriterthread.start();
			
			dev.setConnected(true);	// Verbindungstatus im Device vermerken

		}
			
			return true;
	}
	
	
	
	public void Disconnect() throws IOException 
	{		
		// Msg an Control, dass Device getrennt wurde (bzw. wird) (für den Fall, dass die Verbindung unterbrochen oder die Lok ausgeschalten wurde)
		//sendMsgToUIthread(wb.control.Frag_control.MSG_DEVICE_DISCONNECTED);
		
		dev.setConnected(false);	// Verbindungstatus im Device vermerken
		
		try
		{
			if (TCPsocket != null)
			{			
				if (input != null) { input.close(); }
				TCPsocket.close();
				TCPsocket = null;	// socket kann nicht wiederverwendet werden -> neue Instanz muss erstellt werden
			}
		}
		catch (IOException e)
		{
			Basis.AddLogLine("Fehler beim Schließen der tcp-Verbindung: " + e.toString(), "Device", wblogtype.Error);
		}
	}
	
	
	private void doTCPwork()
	{
		Boolean end = false;
		int receive_count = 512;    // Anzahl der Zeichen pro Lesevorgang
		char[] buf = new char[receive_count];	// Empfangspuffer
		// bool protocol_check = true; // wird später benötigt
		String test = "";	// empfangener, auswertbarer Text
		
		// Verbindung checken?
		if (dev == null) { end = true; }

		while (!end)
        {
			Boolean worktodo = false;
			
			end = dev.getExitThread();	// Thread-Exit wird durch Variable im Device signalisiert

			Boolean connected = true;
			try
			{
				connected = TCPsocket.isConnected();
			}
			catch (Exception e)
			{
				connected = false;
				end = true;
			}
			
			if (connected && !end)	// ende prüfen (Netwaiter wird bei device.connect() gestartet
			{										// und soll beendet werden, wenn die verbindung getrennt wurde
				// buf leeren ?
				int gelesen = 0;
				try {
					gelesen = input.read(buf, 0, receive_count);

				} catch(SocketTimeoutException e) {
					// ignorieren -> passiert sekündlich

				} catch (IOException e) {
					// nur wenn's keine SocketTimeoutException ist
					Basis.AddLogLine("Fehler beim Lesen: " + e.toString(), "Device", wblogtype.Error);
				}	// returns the # of chars read, or -1 if nothing to read


				if (gelesen > 0)
				{
					String newtxt = new String(buf, 0, gelesen);
					test += newtxt;
					// String test_raw = new String(test);	// den aktuellen buffer incl. <comand> aufheben für ev. spezial-auswerung
					Basis.AddLogLine("Device " + dev.getName() + ": string geholt: " + test, "tcp", wblogtype.Info);
				}
				worktodo = true;
			}
			else { end = true; }	// aus Schleife aussteigen, wenn Device getrennt ist
			
            while (worktodo)
            {
            	int startpos = test.indexOf("<");
                int endpos = test.indexOf(">");

                // Prüfung auf WBprotokoll.WBcontrol
                if ((startpos >= 0) && (endpos > 0) && (startpos < endpos))  // gültiger WBcontrol-Befehl ist enthalten -> Protokoll = WBcontrol
                {
                    Basis.AddLogLine(dev.getName() + ": DevNetWaiter: Protokoll = WBcontrol", "tcp", wblogtype.Info);
                }
                else  // wenn kein kompletter Command mehr im string "test" enthalten ist
                {
                	worktodo = false;

                }
                
                String command = "";

                if (worktodo)
                {
                	command = test.substring(startpos + 1, endpos);	// substring von start to end-1 !!!!!
                	test = test.substring(endpos+1);	//.Remove(startpos, endpos - startpos + 1);   // aktuellen command aus test entfernen Error: Count bei Remove um 1 zu klein!!
                	Basis.AddLogLine("Befehl wird ausgewertet: " + command, "tcp", wblogtype.Info);
                	WBcontrol_BefehlAuswerten(command);
                }
            }
        }	// end while (!end)
		Basis.AddLogLine(dev.getName() + ": DevNetWaiter Schleife verlassen", "tcp", wblogtype.Info);
		try {
			Disconnect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private void WBcontrol_BefehlAuswerten(String command) {
		Basis.AddLogLine("Befehlsauswertung start für: " + command, "netwaiter", wblogtype.Info);
        String[] cmdparts = command.split(":"); // wenn kein : enthalten ist, dann ist cmdparts[0] = command, cmdparts.length=1

        if (cmdparts.length == 0) { return; }
        
        /*
        if (cmdparts[0].equals("sd"))   // Speed-Meldung  0: "speed", 1: "nnn" Speedzahl
        {
        	dev.setLastlokstatus(System.currentTimeMillis());	// aktuelle Zeit speichern, damit überprüft werden kann, ob die <sd:nnn> Meldungen von der lok ausbleiben
            //dev.trainspeed = Int32.Parse(cmdparts[1]);
            try { dev.setTrainspeed(Integer.parseInt(cmdparts[1])); }
            catch(NumberFormatException nfe)
            {
            	Basis.AddLogLine("Speed konnte nicht in eine Zahl konvertiert werden!", "netwaiter", wblogtype.Error);
            }
            
            if (dev.getIscd())  // wenn des das Device für die Speedbar ist
            {
            	Handler uihandler = Basis.getUIhandler();
            	if (uihandler != null) 
        		{
            		// Msg an Control-Activity zur aktualisierung des Trainspeed (=von der Lok gemeldeter Speed)
        			Message msg1 = Message.obtain(uihandler, Frag_control.MSG_UPDATE_TRAINSPEED);
        			uihandler.sendMessage(msg1);
        		}
            }
        } */
        
        /*
        else if ((cmdparts.length == 2) && (cmdparts[0].equals("uein")))   // VersorgungsSpannung-Meldung  0: "uein", 1: "nnn*" spannung (int)
        {
        	dev.setUein(cmdparts[1]);	// fehlt noch: Zugriff wird per threading.Monitor synchronisiert
        	
            if (dev.getIscd())  // wenn des das Device für die Speedbar ist -> GUI updaten
            {
            	Handler uihandler = Basis.getUIhandler();
            	if (uihandler != null) 
        		{
            		// Msg an Control-Activity zur Aktualisierung der Versorgungsspannung (=von der Lok gemeldeter Speed)
        			Message msg1 = Message.obtain(uihandler, Frag_control.MSG_UPDATE_U_LOK);
        			uihandler.sendMessage(msg1);
        		}
            } 
        } */

        else if ((cmdparts.length == 2) && (cmdparts[0].equals("iamlok")))   // Identifikationsmeldung der Lok  0: "iamlok", 1: Name der Lok
        {
            if (!cmdparts[1].equals(dev.getName()))
            {
                if (dev.getName().startsWith("unbekannt"))    // bei manuell verbundenem Device ist der Name "unbekannt"
                {
                    dev.setName(cmdparts[1]);  // den richtigen Namen jetzt setzen
                }
                else // wenn schon ein Name existiert -> Problem melden
                {
                    // meldung ausgeben, dass sich in der aktuellen verbindung ein falsches Device gemeldet hat
                    Basis.AddLogLine(dev.getName() + ": falsches Device (" + cmdparts[1] + ") hat sich mit iamlok gemeldet!)", "netwaiter", wblogtype.Warning);
                }
            }
        }



        else // unbekannter Befehl
        {
            Basis.AddLogLine(dev.getName() + ": unbekannter Befehl: " + command, "netwaiter", wblogtype.Warning);
        }
		
	}

}
