package wb.ledcontrol;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class LEDcontrolStartup extends Activity
 implements OnFragReplaceListener {
	
	//FragmentManager fragmentManager;
	FrameLayout frameLayout_f_actions, frameLayout_f_control;
	
	Boolean loadFrags;	// Flag, ob Fragments geladen werden sollen (beim ersten Start) oder nicht (wenn nur reaktiviert wurde)
	Boolean firstresume;
	private String[] fragment_tags;	// res/values/arrays/fragment_tags array-index = Fragment-ID
	
	// Fragment-IDs f�r Benachrichtigungen usw
	public static final int FRAGMENT_ACTION		= 	0;
	public static final int FRAGMENT_CONNECT		= 	1;
	public static final int FRAGMENT_CONTROL 		= 	2;
	public static final int FRAGMENT_LIVE		 	= 	3;
	public static final int FRAGMENT_SHOWS			= 	4;
	public static final int FRAGMENT_TIMER			= 	5;
	public static final int FRAGMENT_CONFIG			= 	6;
	public static final int FRAGMENT_CAMCONTROL		= 	7;

	
	public Handler acthandler=new Handler() { 
		@Override 
		public void handleMessage(Message msg) { 
			CheckMsg(msg);	// Message auswerten -> UI-Aktionen
		} 
	};
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {	
		
		super.onCreate(savedInstanceState);

		firstresume  = true;
		
		// als erstes Basis-Service starten!
		// Basis im Main(UI)-Thread laufen lassen
		
		try {
    		Intent svc = new Intent(this, Basis.class);    		
    		startService(svc);
    		Basis.setActhandler(acthandler);
    		
    	}
    	catch (Exception e) {
    		Log.d("startup", "Basis-Service creation problem: " + e.toString());
    	}
		// ACHTUNG: Basis-Aufrufe funktionieren hier noch nicht, Basis ist noch nicht bereit (Start dauert l�nger)
		// weitere Aktionen werden erst anch Empfang der MSG_BASIS_READY durchgef�hrt
		
		fragment_tags = getResources().getStringArray(R.array.fragment_tags);
		
		setContentView(R.layout.startup);
		
		frameLayout_f_actions = (FrameLayout)findViewById(R.id.frameLayout_f_actions);
		frameLayout_f_control = (FrameLayout)findViewById(R.id.frameLayout_f_control);
		
		// loadFragsAtStartup();	// wird erst ausgef�hrt, wenn die msg kommt, dass der Basis-Service bereit ist!!! sonst gibt's Probleme
		if (savedInstanceState == null) { loadFrags = true; }
		else {loadFrags = false; }
		
		
		
	}	// end onCreate

	
	@Override
	public void onResume() {
		super.onResume();
			
		if (!firstresume) { Basis.setActhandler(acthandler); } // beim 1. resume nicht ausf�hren
		firstresume = false;

	}
	
	
	@Override
	public void onPause() {
		super.onPause();
		
		Basis.setActhandler(null);
	}
	
	private void loadFragsAtStartup()	// Fragments beim echten Programmstart laden
	{
		// Check that the activity is using the layout version with the frameLayout_f_control FrameLayout
		// Actions Fragment bef�llen

		if (frameLayout_f_actions != null) {

			// However, if we're being restored from a previous state,
			// then we don't need to do anything and should return or else
			// we could end up with overlapping fragments.

			//if (savedInstanceState != null) { return; }

			// Create an instance of ExampleFragment
			Frag_actions fragaction = new Frag_actions();

			// In case this activity was started with special instructions from an Intent,
			// pass the Intent's extras to the fragment as arguments
			//firstFragment.setArguments(getIntent().getExtras());

			// Add the fragment to the 'fragment_container' FrameLayout
			getFragmentManager().beginTransaction().add(R.id.frameLayout_f_actions, fragaction, fragment_tags[FRAGMENT_ACTION]).commit();
		}


		if (frameLayout_f_control != null) {

			//if (savedInstanceState != null) { return; }

			Fragment fragcontrol = new Frag_connect2();
			getFragmentManager().beginTransaction().add(R.id.frameLayout_f_control, fragcontrol, fragment_tags[FRAGMENT_CONNECT]).commit();
		} 
	}
	

    private void checkScreen()
    {
    	//Configuration config = getResources().getConfiguration();
    	DisplayMetrics metrics = new DisplayMetrics();
    	getWindowManager().getDefaultDisplay().getMetrics(metrics);
    	int h = metrics.heightPixels;
    	int w = metrics.widthPixels;
    	int dpi = metrics.densityDpi;
    	float dens = metrics.density;

    	int dph = (int)(h / dens);
    	int dpw = (int)(w / dens);
    	long dpxy = dph * dpw;	// dpPixel-Anzahl
    	Basis.setDpPixels(dpxy);

    	// alte Methode: Entscheidung Display-Layout - derzeit fixer Grenzwert eingetragen
    	//if (dpxy >= Basis.getDpPixels_dualview_threshold()) { Basis.setDisplaymode(Basis.DISPLAYMODE_DUALVIEW); }
    	//else { Basis.setDisplaymode(Basis.DISPLAYMODE_SINGLEVIEW); 	}  

    	
    	// emulator 10Zoll 1280x720 Tablet hat nur dpXY=930000 -> weniger als das GT 7.7!!! -> so l��t es sich nicht unterscheiden!!
    	
    	int displaymode = Basis.DISPLAYMODE_SINGLEVIEW;
    	String screenlayout_size = "";

    	//Determine screen size
    	if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {     
    		screenlayout_size = "LARGE";
    		if (dpxy >= Basis.getDpPixels_dualview_threshold()) { displaymode = Basis.DISPLAYMODE_DUALVIEW; }
    	}
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL) {     
    		screenlayout_size = "NORMAL";
    	} 
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {     
    		screenlayout_size = "SMALL";
    	}
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {     
    		screenlayout_size = "XLARGE";
    		displaymode = Basis.DISPLAYMODE_DUALVIEW;
    		// 10Zoll Tablets noch unterscheiden > 1M dpXY??
    	}
    	else if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_UNDEFINED) {     
    		screenlayout_size = "UNDEFINED";
    	}
    	else {
    		screenlayout_size = "unknown";
    	}

    	Basis.setDisplaymode(displaymode);

    	String logdata = "Screen:\r\nPixel H = " + h +
    			"\r\nPixel W = " + w +
    			"\r\nDPI = " + dpi +
    			"\r\nDensity = " + dens +
    			"\r\ndpX = " + dph +
    			"\r\ndpY = " + dpw +
    			"\r\ndpXY = " + dpxy +
    			"\r\nScreenlayout size = " + screenlayout_size;
    	
    	Log.d("Startup", logdata);
    }


	@Override
	public void OnFragReplace(int fragmentID, boolean toBackStack, Bundle data) {
		
		startFragment(frameLayout_f_control, fragmentID, toBackStack, data);
		
	}
	
	
	void startFragment(ViewGroup container, int fragmentID, boolean toBackStack,  Bundle data)
	{
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		Fragment f = createNewFragment(fragmentID);	// sp�ter frag nur erstllen, wenn noch nicht existiert
		// Replace whatever is in the fragment_container view with this fragment,
		// and add the transaction to the back stack so the user can navigate back
		transaction.replace(container.getId(), f, fragment_tags[fragmentID]);
		transaction.addToBackStack(null);	
		transaction.commit();
	}
    
	Fragment createNewFragment(int fragmentID)
	{
		Fragment fneu = null;
		
		switch (fragmentID) {

		case FRAGMENT_CONNECT:	
			fneu = new Frag_connect2();	
			break;

		case FRAGMENT_CONTROL:
			fneu = new Frag_control();
			break;

		case FRAGMENT_LIVE:
			fneu = new Frag_live();
			break;
			
		case FRAGMENT_SHOWS:
			//fneu = new Frag_shows();
			fneu = new Frag_control();
			break;

		case FRAGMENT_TIMER:
			//fneu = new Frag_timer();
			fneu = new Frag_control();
			break;
			
		case FRAGMENT_CONFIG:
			fneu = new Frag_config();
			break; 
						
		case FRAGMENT_CAMCONTROL:
			fneu = new Frag_camcontrol();
			break; 
			
		default:
			break;
		}
		
		return fneu;
	}
	
	
	
    // ------- Auswertungsfunktion f�r MessageHandler -----------------------------
	void CheckMsg(Message msg) {

		switch (msg.what) {

		case Basis.MSG_BASIS_READY:		// Basis is ready (Netzwerk auch)
			if (loadFrags) { loadFragsAtStartup(); }
			break; 

		case Basis.MSG_BASIS_DEVICELIST_CHANGED:	// Devices haben sich ge�ndert
			Frag_connect2 fc = (Frag_connect2) getFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_CONNECT]);
			if (fc != null) { fc.updateDevices(); }
			break;
			
		case Basis.MSG_BASIS_DEVICE_CONNECTED:	// Devices selectedDevice wurde verbunden
			Frag_connect2 fc1 = (Frag_connect2) getFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_CONNECT]);
			if (fc1 != null) { fc1.onDeviceConnected(); }
			break;
			
		case Basis.MSG_BASIS_DEVICE_DISCONNECTED:	// Devices selectedDevice wurde getrennt
			Frag_connect2 fc2 = (Frag_connect2) getFragmentManager().findFragmentByTag(fragment_tags[FRAGMENT_CONNECT]);
			if (fc2 != null) { fc2.onDeviceDisconnected(); }
			break; 
		}

	}	// end CheckMsg()
	
	
	
}	// end class LEDcontrolStartup