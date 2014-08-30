package wb.ledcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;

public class LEDColorView extends View {


	Paint pt;
	Shader sh1;
	final float[] color = { 1.f, 1.f, 1.f };



	public LEDColorView(Context context) {
		super(context);
	}

	public LEDColorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LEDColorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	@SuppressLint("DrawAllocation")
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (pt == null) {
			pt = new Paint();
			sh1 = new LinearGradient(0.f, 0.f, 0.f, this.getMeasuredHeight(), 0xffffffff, 0xff000000, TileMode.CLAMP);
		}
		int rgb = Color.HSVToColor(color);
		Shader sh2 = new LinearGradient(0.f, 0.f, this.getMeasuredWidth(), 0.f, 0xffffffff, rgb, TileMode.CLAMP);
		ComposeShader shader = new ComposeShader(sh1, sh2, PorterDuff.Mode.MULTIPLY);
		pt.setShader(shader);
		canvas.drawRect(0.f, 0.f, this.getMeasuredWidth(), this.getMeasuredHeight(), pt);
	}

	void setHue(float hue) {
		color[0] = hue;
		invalidate();
	}




}
