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
import android.widget.ToggleButton;

public class Frag_live extends Fragment 
implements OnClickListener, OnTouchListener {
	
	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	LayoutInflater layoutInflater;
	float lastTouchx, lastTouchy = 0;
	long lastTouchTime;
	Boolean touchModeLive = false;	// true: es wird nur alle nnn ms ein neuer Farbwert angenommen. false: jeder Wert wird genommen und verzögert ausgegeben ("lagt" hinterher - kann aber ein gewünschter Effekt sein) 
	
	Device cdevice;
	
	//LEDColorView ledColorView_live;
	ColorBarView colorBarView_live;
	View view_color;
	ToggleButton toggleButton_live_mode;
	
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
		fragview = inflater.inflate(R.layout.f_live, container, false);
		
		//ledColorView_live = (LEDColorView)fragview.findViewById(R.id.ledColorView_live);
		colorBarView_live = (ColorBarView)fragview.findViewById(R.id.colorBarView_live);
		colorBarView_live.setOnTouchListener(this);
		
		view_color = (View)fragview.findViewById(R.id.view_color);
		
		toggleButton_live_mode = (ToggleButton)fragview.findViewById(R.id.toggleButton_live_mode);
		toggleButton_live_mode.setOnClickListener(this);
		
		
		return fragview;
    }	// end onCreateView
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		cdevice = Basis.getSelectedDevice();
		
		if (cdevice != null)
		{
			if (cdevice.isConnected())	
			{ 
				activateControls(true); 
				cdevice.Netwrite("<" + getString(R.string.ledcmd_stopfx) + ">");
			}
			else { activateControls(false); }
		}
		else { activateControls(false); }
		
		touchModeLive = Basis.getLive_mode();
		toggleButton_live_mode.setChecked(touchModeLive);

	}
	


	@Override
	public void onPause()
	{
		super.onPause();
		
		Basis.setLive_mode(touchModeLive);
		
		if (cdevice != null)
		{
			if (cdevice.isConnected())	{ cdevice.Netwrite("<" + getString(R.string.ledcmd_startfx) + ">");	}
		}
	}


	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.toggleButton_live_mode:
			touchModeLive = toggleButton_live_mode.isChecked();
			break;

		default:
			//textView_status.setText("irgendwas Unbekanntes gedrückt..");
			break;
		}

	}
	
	
	private void activateControls(boolean activate) {
		// Controls aktivieren oder deaktivieren

		colorBarView_live.setEnabled(activate);


	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		int c, red, green, blue = 0;
		long touchTime = 0;

		if (v.getId() == R.id.colorBarView_live)
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
				
				if (touchModeLive)	// live oder lag modus
				{
					touchTime = System.currentTimeMillis();
					if (touchTime < lastTouchTime+100) { return true; }	// Befehl nur ausführen, wenn > 100ms vergangen, sonst verwerfen
				}
				
				lastTouchTime = touchTime;
				
				if (x < 0.f) x = 0.f;
				if (x >= v.getMeasuredWidth()) x = v.getMeasuredWidth()-1;
				if (y < 0.f) y = 0.f;
				if (y >= v.getMeasuredHeight()) y = v.getMeasuredHeight()-1;

				//int c = colorBarView_live.getColor(x, y);
				Bitmap bm = colorBarView_live.getDrawingCache();
				int px = (int)x;
				int py = (int)y;
				try
				{
					c = bm.getPixel(px, py);

					red = Color.red(c);
					green = Color.green(c);
					blue = Color.blue(c);
					view_color.setBackgroundColor(c);

					if (cdevice != null)
					{
						cdevice.Netwrite("<" + getString(R.string.ledcmd_setcolor) + ":" + red + ":" + green + ":" + blue + ">");
					}
				}
				catch (IllegalArgumentException e)
				{
					Basis.AddLogLine("Fehler beim Ermitteln der Pixelfarbe: px=" + px + " py=" + py + ". " + e.toString(), "live", wblogtype.Error);
				}

			}
			
			return true;	// touch war für colorBarView_live
		}
		else {	return false;	}	// touch war für was anderes
	

	}
	

} // end class Frag_control
