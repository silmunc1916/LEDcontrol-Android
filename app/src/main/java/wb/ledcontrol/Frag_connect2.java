// neue Version mit ListFragment

package wb.ledcontrol;

import java.util.ArrayList;

import wb.ledcontrol.dialogfragments.DeviceDialogFragment;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Frag_connect2 extends ListFragment {

	
	View fragview;	// Root-View für das Fragment
	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	//LayoutInflater layoutInflater;
	
	DevAdapter deva;
	ArrayList<Device> devices_found;
	
	//TextView textView_connect_devices_title;
	//ListView listView_connect_devices;
	//Button button_connect_adddev;
	View menutargetView; // speichert den View, von dem aus das aktuelle ContextMenu gestartet wurde
	
	static final int DIALOG_ADD_DEV = 0;	// dialog typ	// neues device anlegen

		
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            fragReplListener = (OnFragReplaceListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragReplaceListener");
        }
    }
	
	/*
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	} */
	
	
	
	@Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

		deva=new DevAdapter(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<Device>());
		this.setListAdapter(deva);
    }

	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		fcontainer = container;
		View fragview = super.onCreateView(inflater, container, savedInstanceState);
		
		return fragview;
    }	// end onCreateView
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		updateDevices();

	}
	
	
	@Override
	public void onPause()
	{
		
		super.onPause();

	}
	
	public void updateDevices()
	{
		devices_found = Basis.getDevicelist();
		deva.setDevices(devices_found);
	}
	
	public void onDeviceConnected()
	{
		//listView_connect_devices.invalidate();	// damit die Kennzeichnung für "verbunden" angezeigt wird
		updateDevices();
	}
	
	public void onDeviceDisconnected()
	{
		updateDevices();
	}
		

	public class DevAdapter extends ArrayAdapter<Device> {
		 
		ArrayList<Device> listdevs;
		Context mContext;
		
	        public DevAdapter(Context context, int textViewResourceId, ArrayList<Device> devs) {
	            super(context, textViewResourceId, devs);
	            mContext = context;
	            listdevs = devs;
	        }
	        
	        public void setDevices(ArrayList<Device> devlist)
	        {
	        	listdevs.clear();
	        	listdevs.addAll(devlist);	// TODO apilevel?
	        	this.notifyDataSetChanged();
	        }
	        
	        public void clearDevices()
	        {
	        	listdevs.clear();
	        	this.notifyDataSetChanged();
	        }
	        
	        @Override
	        public View getView(int position, View convertView, ViewGroup parent) {

	        	View row=convertView; 
	        	LayoutInflater linflater = ((Activity)mContext).getLayoutInflater();
	        	 
	            if (row==null) { 
	              row=linflater.inflate(R.layout.f_connect_devs_row, parent, false); 
	            } 
	            unregisterForContextMenu(row);
	       
	            Device d = listdevs.get(position);
	            row.setTag(d);	// Device in Tag speichern (für ContextMenu-Bearbeitung)
	            
	            ImageView imageView_connect_dev_type = (ImageView)row.findViewById(R.id.imageView_connect_dev_type); 
	            ImageView imageView_connect_dev_connected = (ImageView)row.findViewById(R.id.imageView_connect_dev_connected);
	            TextView textView_connect_dev = (TextView)row.findViewById(R.id.textView_connect_dev); 
	            textView_connect_dev.setText(d.getName());
	            
	            if (d.getTyp() == Device.TYPE_RASPI_LED) { imageView_connect_dev_type.setVisibility(View.VISIBLE); }
	            else { imageView_connect_dev_type.setVisibility(View.INVISIBLE); }
	            
	            if (d.isConnected()) { imageView_connect_dev_connected.setVisibility(View.VISIBLE); }
	            else { imageView_connect_dev_connected.setVisibility(View.INVISIBLE); }
	            
	            registerForContextMenu(row);
	            row.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
					
						v.setSelected(true);
						Device selectedDevice = (Device)v.getTag();
						
						if (selectedDevice != null) {	selectedDevice.Connect(); }	
					}

	            });
	            
	        	return row; 
	        } 
	}

	

	
	/* funktioniert mit dem custom ListviewItem layout nicht! -Y daher im DevAdapter
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		v.setSelected(true);
		Device selectedDevice = (Device)v.getTag();
		selectedDevice.Connect();
	} */
	

	// ContextMenü
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		//super.onCreateContextMenu(menu, v, menuInfo);
		menutargetView = v;
		menu.clear();	// Menu leeren!!
		MenuInflater inflater = new MenuInflater(getActivity().getApplicationContext());
		inflater.inflate(R.menu.menu_devs, menu);
		//Bsp. für Remove
		//menu.removeItem(R.id.menui_act_add);
	}
	
	public void onContextMenuClosed (Menu menu)
	{
		menutargetView = null;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId()) {

		case R.id.menui_devs_edit:
			if (menutargetView != null)
			{ 
				Device editDevice = (Device)menutargetView.getTag();
				if (editDevice != null) 
				{					
					DeviceDialogFragment devfrag = new DeviceDialogFragment();
					Bundle args = new Bundle();
					args.putInt("type", Basis.DIALOG_EDIT_DEV);
					args.putString("editdev", editDevice.getName());
					devfrag.setArguments(args);
				    devfrag.show(getFragmentManager(), "devicedialog");
				}
			}

			return true;

		case R.id.menui_devs_del:
			if (menutargetView != null)
			{ 
				Device d = (Device)menutargetView.getTag();
				if (d != null)
				{
					Basis.RemoveDevice(d);
					//updateDevices();
				}
			}

			return true;
			
		case R.id.menui_devs_disconnect:
			if (menutargetView != null)
			{ 
				Device d = (Device)menutargetView.getTag();
				if (d != null)
				{
					if (d.isConnected()) { d.Disconnect(); }
					updateDevices();
				}
			}

			return true;


		default:
			return super.onContextItemSelected(item);
		}
	}	// end  onContextItemSelected
	
	
	
	
	
}	// end Class Frag_connect
