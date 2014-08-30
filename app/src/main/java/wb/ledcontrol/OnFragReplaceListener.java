package wb.ledcontrol;

import android.os.Bundle;

public interface OnFragReplaceListener {

	public void OnFragReplace(int fragmentID, boolean toBackStack, Bundle data);
	
}

/*

 Callback f�r's Wechseln der Fragments
int fragmentID: ID, welches Fragment gestartet werden soll. Die IDs sind in FAct_control definiert.

boolean toBackStack: wenn true: "fragmentTransaction.addToBackStack(null);" wird ausgef�hrt
						man kann danach mit der BACK-Taste zum alten Fragment zur�ckspringen.
						Ist das nicht gew�nscht, mu� false �bergeben werden, dannn wird kein 
						.addToBackStack() ausgef�hrt.

Bundle data:	Im Bundle k�nnen Daten �bergeben werden, wenn zB. ein startActivityForResult() ersetzt werden soll.
 				Das Bundle mu� dann in der FragmentActivity ausgewertet werden.

NICHT MEHR BEN�TIGT:
int containerID: ID des containers, in dem das alte Fragment enthalten ist, das durch das neue (fragmentID) ersetzt werden soll.
					Die containerID kann im fragment folgenderma�en ermittelt werden: 
					int containerID = ((ViewGroup)getView().getParent()).getId();
					oder einfach fcontainer.getId(), wenn bekannt

*/