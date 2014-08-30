package wb.ledcontrol;

import wb.ledcontrol.WBlog.wblogtype;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class Frag_camcontrol extends Fragment 
implements OnTouchListener {
	
	static final int SERVOVALUE_MAX = 16000;
	
	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	LayoutInflater layoutInflater;
	
	float lastTouchx, lastTouchy = 0;
	long lastTouchTime;
	
	Device cdevice;
	WebView webView_cam;
	TextView textView_coords;
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fragReplListener = (OnFragReplaceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragReplaceListener");
        }
    }
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		fcontainer = container;
		layoutInflater = inflater;
		fragview = inflater.inflate(R.layout.f_camcontrol, container, false);
			
		webView_cam = (WebView)fragview.findViewById(R.id.webView_cam);
		webView_cam.setOnTouchListener(this);
		
		textView_coords  = (TextView)fragview.findViewById(R.id.textView_coords);
				
		return fragview;
    }	// end onCreateView
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		cdevice = Basis.getSelectedDevice();
		
		/*
		if (cdevice != null)
		{
			if (cdevice.isConnected())	{ activateControls(true); }
			else { activateControls(false); }
		}
		else { activateControls(false); }
		*/

	}
	


	@Override
	public void onPause()
	{
		
		super.onPause();

	}


	/*
	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.button_ctrl_shutdown:
			cdevice.Netwrite("<" + getString(R.string.ledcmd_shutdown) + ">");
			break;

		default:
			//textView_status.setText("irgendwas Unbekanntes gedrückt..");
			break;
		}

	} */
	
	
	/*
	private void activateControls(boolean activate) {
		// Controls aktivieren oder deaktivieren

		button_ctrl_shutdown.setEnabled(activate);
		button_ctrl_restart.setEnabled(activate);
		button_ctrl_ledreset.setEnabled(activate);
		button_ctrl_stopfx.setEnabled(activate);
		button_ctrl_startfx.setEnabled(activate);

	} */


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		long touchTime = 0;
		int maxx, maxy;

		maxy = webView_cam.getHeight();
		maxx = webView_cam.getWidth();

		if (v.getId() == R.id.webView_cam)
		{
			if (event.getAction() == MotionEvent.ACTION_MOVE
					|| event.getAction() == MotionEvent.ACTION_DOWN)
				//	|| event.getAction() == MotionEvent.ACTION_UP) //löst bei 1 Tippser den Befehl 2x aus (down+up)
			{

				float x = event.getX(); // touch event are in dp units.
				float y = event.getY();

				//Befehle reduzieren, die ans LEDmodul gesendet werden
				if ((x == lastTouchx) && (y == lastTouchy)) { return true; }	// nicht 2x denselben Befehl ausgeben
				lastTouchx = x;
				lastTouchy = y;


				// Bremse für Übertragung von Werten
				// touchTime = System.currentTimeMillis();
				// if (touchTime < lastTouchTime+100) { return true; }	// Werte nur übertragen, wenn > 100ms vergangen, sonst verwerfen
				//lastTouchTime = touchTime;

				if (x < 0.f) x = 0.f;
				if (x >= v.getMeasuredWidth()) x = v.getMeasuredWidth()-1;
				if (y < 0.f) y = 0.f;
				if (y >= v.getMeasuredHeight()) y = v.getMeasuredHeight()-1;

				try
				{
					int servox = (int) (SERVOVALUE_MAX * x / maxx);
					int servoy = (int) (SERVOVALUE_MAX * y / maxy);
					
					textView_coords.setText("Servo X: " + servox + "  Servo Y: " + servoy);

					if (cdevice != null)
					{
						String moveCMD = getResources().getString(R.string.servocmd_move);
						cdevice.Netwrite(String.format(moveCMD, servox, servoy));
					}
				}
				catch (IllegalArgumentException e)
				{
					Basis.AddLogLine("Fehler beim Ermitteln der Servoposition" + ". " + e.toString(), "cam", wblogtype.Error);
				}

			}

			return true;	// touch war für webView_cam
		}
		else {	return false;	}	// touch war für was anderes

	}


} // end class Frag_control
