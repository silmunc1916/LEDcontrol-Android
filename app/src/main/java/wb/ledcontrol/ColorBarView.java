package wb.ledcontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
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

public class ColorBarView extends View {

	Paint colorpaint;
	Shader rainbow, sh2, sh3;
	ComposeShader compshader1, compshader2;
	int[] colors;

	public ColorBarView(Context context) {
		super(context);
		init();
	}

	public ColorBarView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ColorBarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init()
	{
		this.setDrawingCacheEnabled(true);
		colorpaint = new Paint();
		colors = new int[] { 0xFFFFFFFF, 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000, 0xFF000000, 0xFF000000};
		prepPaint();
	}
	
	@SuppressLint("NewApi")
	private void prepPaint()
	{
		rainbow = new LinearGradient(0f, 0f, this.getMeasuredWidth(), 0f, colors, null, TileMode.CLAMP);
	    sh2 = new LinearGradient(0.f, this.getMeasuredHeight()/2, 0.f, this.getMeasuredHeight(), 0xffffffff, 0xff000000, TileMode.CLAMP);
	    compshader1 = new ComposeShader(rainbow, sh2, PorterDuff.Mode.MULTIPLY);

	    sh3 = new LinearGradient(0.f, 0.f, 0.f, this.getMeasuredHeight()/2, 0xffffffff, 0xff000000, TileMode.CLAMP);
	    compshader2 = new ComposeShader(compshader1, sh3, PorterDuff.Mode.ADD);	//TODO funkt erst ab APIlevel 11 -> checken
	    
	    colorpaint.setShader(compshader2);
	}


	
	protected void onSizeChanged (int w, int h, int oldw, int oldh)
	{
		prepPaint();	// paint neu vorbereiten wg. geänderter Größe des views
	}


	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	    
		canvas.drawRect(0.f, 0.f, this.getMeasuredWidth(), this.getMeasuredHeight(), colorpaint);
	}




} // end class ColorBarView
