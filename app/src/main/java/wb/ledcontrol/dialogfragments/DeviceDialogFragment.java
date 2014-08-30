package wb.ledcontrol.dialogfragments;

import wb.ledcontrol.Basis;
import wb.ledcontrol.Device;
import wb.ledcontrol.R;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DeviceDialogFragment extends DialogFragment {
	
	EditText editText_dialog_tad_name, editText_dialog_tad_ip;
	Device editDevice;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final Dialog dialog = new Dialog(getActivity());
		int dialog_typ = 0;
		setRetainInstance(true);
		dialog_typ = getArguments().getInt("type");

		switch(dialog_typ) {

		case Basis.DIALOG_ADD_DEV:	// Device hinzufügen

			dialog.setContentView(R.layout.dialog_add_dev);
			dialog.setTitle(R.string.dialog_adddev_titel);    	
			editText_dialog_tad_name = (EditText) dialog.findViewById(R.id.editText_dialog_tad_name);
			editText_dialog_tad_ip = (EditText) dialog.findViewById(R.id.editText_dialog_tad_ip);
			editText_dialog_tad_name.setText("");
			editText_dialog_tad_ip.setText("");
			dialog.setCancelable(true);

			Button button_dialog_tad_save = (Button) dialog.findViewById(R.id.button_dialog_tad_save);
			button_dialog_tad_save.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					String name = editText_dialog_tad_name.getText().toString();
					String ip = editText_dialog_tad_ip.getText().toString();

					final String IPADDRESS_PATTERN = 
							"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
									"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
									"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
									"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

					if ((name != null) && (!name.equals(""))) 
					{
						if (!ip.matches(IPADDRESS_PATTERN)) { ip = ""; }
						Device neu = new Device(name);
						neu.setIp(ip);
						neu.setPredef(true);
						Basis.AddDevice(neu);	// Device anlegen
					}

					dialog.dismiss();

				}
			});

			Button button_dialog_tad_cancel = (Button) dialog.findViewById(R.id.button_dialog_tad_cancel);

			button_dialog_tad_cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
				}
			});

			break;

		case Basis.DIALOG_EDIT_DEV:	// Device editieren

			dialog.setContentView(R.layout.dialog_add_dev);
			dialog.setTitle(R.string.dialog_editdev_titel);
			String dname = getArguments().getString("editdev");
			editDevice = Basis.getDeviceByName(dname);
			
			editText_dialog_tad_name = (EditText) dialog.findViewById(R.id.editText_dialog_tad_name);
			editText_dialog_tad_ip = (EditText) dialog.findViewById(R.id.editText_dialog_tad_ip);
			editText_dialog_tad_name.setText(editDevice.getName());
			editText_dialog_tad_ip.setText(editDevice.getIP());
			dialog.setCancelable(true);

			button_dialog_tad_save = (Button) dialog.findViewById(R.id.button_dialog_tad_save);
			button_dialog_tad_save.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

					String newname = editText_dialog_tad_name.getText().toString();
					String newip = editText_dialog_tad_ip.getText().toString();

					final String IPADDRESS_PATTERN = 
							"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
									"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
									"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
									"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

					if ((newname != null) && (!newname.equals(""))) 
					{
						if (!newip.matches(IPADDRESS_PATTERN)) { newip = ""; }

						editDevice.setName(newname);
						editDevice.setIp(newip);
						editDevice = null;
					}

					dialog.dismiss();
					//benachrichtigen??
				}
			});

			button_dialog_tad_cancel = (Button) dialog.findViewById(R.id.button_dialog_tad_cancel);

			button_dialog_tad_cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					editDevice = null;
				}
			});

			break;
		}

		return dialog;
	}
	
	@Override	// wg. Problemen bei ScreenRotation
	public void onDestroyView() {
	  if (getDialog() != null && getRetainInstance())
	    getDialog().setOnDismissListener(null);
	  super.onDestroyView();
	}
}
