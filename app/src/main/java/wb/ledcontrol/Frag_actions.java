package wb.ledcontrol;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Frag_actions extends ListFragment {

	ViewGroup fcontainer;
	OnFragReplaceListener fragReplListener;
	private String[] items;
	ArrayAdapter<String> aa;
	Boolean firststart;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		items = getResources().getStringArray(R.array.actions);
		aa = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items);
		this.setListAdapter(aa);
		firststart = true;
	}
	
	
	
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
		//container.removeAllViews();
		View fragview = super.onCreateView(inflater, container, savedInstanceState);
		return fragview;
	}	// end onCreateView
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		if (firststart) { this.getListView().setSelection(0); } 
		firststart = false;

		
	}	// End onResume 
	
	
	/*
	@Override
	public void onPause() {
		super.onPause();
		
	} */
	

	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		v.setSelected(true);
		fragReplListener.OnFragReplace(position+1, false, null);	// Fragement ID = position +1 (position = index im actions array)
		

	}
	
	
	

	

    
}
