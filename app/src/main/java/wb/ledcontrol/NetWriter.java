package wb.ledcontrol;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import wb.ledcontrol.WBlog.wblogtype;

public class NetWriter implements Runnable {

	private Device dev;
	private Socket TCPsocket;
	private  BufferedWriter output;


	public NetWriter(Device d, Socket tcp)	//Konstruktor zur Übergabe des Devices
	{
		dev = d;
		TCPsocket = tcp;
		
	}

	@Override
	public void run() 	// run wird beim Starten des Threads ausgeführt
	{
		try {

			output = new BufferedWriter(new OutputStreamWriter(TCPsocket.getOutputStream()));
		} catch (IOException e) {
			Basis.AddLogLine("Netwrite: Error beim Schreiben! " + e.toString(), "TCP", wblogtype.Error);
		}

		doWork();	// Haupt-Arbeitsschleife
		
		cleanupBeforeExit();

		// Rest wird erst im NetWaiterThread gemacht
		// Msg an Control, dass Device getrennt wurde (bzw. wird) (für den Fall, dass die Verbindung unterbrochen oder die Lok ausgeschalten wurde)
		//sendMsgToUIthread(wb.control.Frag_control.MSG_DEVICE_DISCONNECTED);
		//dev.setConnected(false);	// Verbindungstatus im Device vermerken
	}

	
	private void Netwrite(String string) 
	{
		//Boolean error1, error2 = false;
		Basis.AddLogLine("Netwrite: " + string, "Device", wblogtype.Info);

		if ((TCPsocket != null) && (output != null)) 
		{ 
			try 
			{
				output.write(string);
				output.flush();
			}
			catch (IOException e) 
			{
				 Basis.AddLogLine("Netwrite: Error beim Schreiben! " + e.toString(), "TCP", wblogtype.Error);
				 dev.setExitthread(true);
			}
		}
		else 
		{ 
			Basis.AddLogLine("Netwrite war nicht möglich!", "TCP", wblogtype.Error);
			dev.setExitthread(true);
		}
	} 


	private void doWork()	// Haupt-Arbeits- und Warteschleife des Threads
	{
		Boolean end = false;
		String text = null;

		while (!end)
		{
			text = dev.getNetCmdtoSend();
			if (text == null) // kurze Pause einlegen: 50ms
			{ 
				try { Thread.sleep(50); } 
				catch (InterruptedException e) { e.printStackTrace(); }
			}	
			else { Netwrite(text); }

			end = dev.getExitThread();	// Thread-Exit wird durch Variable im Device signalisiert
		}	// end while (!end)
		Basis.AddLogLine(dev.getName() + ": DevNetWriter Schleife verlassen", "tcp", wblogtype.Info);
	}
	
	
	private void cleanupBeforeExit()
	{
	// vor Ausstieg checken, ob Device gestoppt werden soll (sofern das Netwrite() nicht selbst wg. eines Fehlers den Ausstieg auslöst - dann kann natürlich nicht mehr geschrieben werden)


			if (TCPsocket != null)
			{

				if (output != null) 
				{ 
					try {
						output.flush();
						output.close();
						output = null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
	}

}
