package wb.ledcontrol;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class Frag_control extends Fragment 
implements OnClickListener {
	
	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	LayoutInflater layoutInflater;
	
	Device cdevice;
	
	Button button_ctrl_shutdown, button_ctrl_restart, button_ctrl_ledreset, button_ctrl_stopfx, button_ctrl_startfx;
	
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
		fragview = inflater.inflate(R.layout.f_control, container, false);
		
		//listView_connect_devices = (ListView)fragview.findViewById(R.id.listView_connect_devices);
		//listView_connect_devices.setAdapter(deva);	// geht hier nicht, aber in onActivityCreated -> warum bitte dort aber hier nicht??
		//listView_connect_devices.setOnItemClickListener(this);
	
		button_ctrl_shutdown = (Button)fragview.findViewById(R.id.button_ctrl_shutdown);
		button_ctrl_shutdown.setOnClickListener(this);
		button_ctrl_restart = (Button)fragview.findViewById(R.id.button_ctrl_restart);
		button_ctrl_restart.setOnClickListener(this);
		button_ctrl_ledreset = (Button)fragview.findViewById(R.id.button_ctrl_ledreset);
		button_ctrl_ledreset.setOnClickListener(this);
		button_ctrl_stopfx = (Button)fragview.findViewById(R.id.button_ctrl_stopfx);
		button_ctrl_stopfx.setOnClickListener(this);
		button_ctrl_startfx = (Button)fragview.findViewById(R.id.button_ctrl_startfx);
		button_ctrl_startfx.setOnClickListener(this);
		
		
		return fragview;
    }	// end onCreateView
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		cdevice = Basis.getSelectedDevice();
		
		if (cdevice != null)
		{
			if (cdevice.isConnected())	{ activateControls(true); }
			else { activateControls(false); }
		}
		else { activateControls(false); }


	}
	


	@Override
	public void onPause()
	{
		
		super.onPause();

	}


	@Override
	public void onClick(View v) {

		switch (v.getId()) {

		case R.id.button_ctrl_shutdown:
			cdevice.Netwrite("<" + getString(R.string.ledcmd_shutdown) + ">");
			break;

		case R.id.button_ctrl_restart:
			cdevice.Netwrite("<" + getString(R.string.ledcmd_reboot) + ">");
			break;

		case R.id.button_ctrl_ledreset:
			cdevice.Netwrite("<" + getString(R.string.ledcmd_ledreset) + ">");
			break;

		case R.id.button_ctrl_stopfx:
			cdevice.Netwrite("<" + getString(R.string.ledcmd_stopfx) + ">");
			break;

		case R.id.button_ctrl_startfx:
			cdevice.Netwrite("<" + getString(R.string.ledcmd_startfx) + ">");
			break;

		default:
			//textView_status.setText("irgendwas Unbekanntes gedrückt..");
			break;
		}

	}
	
	
	private void activateControls(boolean activate) {
		// Controls aktivieren oder deaktivieren

		button_ctrl_shutdown.setEnabled(activate);
		button_ctrl_restart.setEnabled(activate);
		button_ctrl_ledreset.setEnabled(activate);
		button_ctrl_stopfx.setEnabled(activate);
		button_ctrl_startfx.setEnabled(activate);

	}
	

} // end class Frag_control
