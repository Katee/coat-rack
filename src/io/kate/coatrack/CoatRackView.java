package io.kate.coatrack;

import io.kate.coatrackcontrol.R;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

/**
 * Displays the Ring and player health bars
 * 
 * @author kate
 *
 */
public class CoatRackView extends SurfaceView {
	static final int num_effects_in_ring = 8;
	
	EmitterView emitters[];

	int ringX;
	int ringY;
	int ringD = 120;
	
	Paint arenaBackground;
	
	int selectedColor = R.color.fire;
	
	/*
	 * the radius of a fire effect on screen
	 */
	int emitterRadius = 60;
	
	// Does touching an emitter cause it to fire?
	public boolean isInDrawMode = true;
	
	public OnEmitterTouch onEmitterTouch;
	
	public CoatRackView(Context context) {
		super(context);
		setupPaints();
	}
	
	public CoatRackView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupPaints();
	}

	public CoatRackView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setupPaints();
	}
	
	public void setColor(int color) {
		selectedColor = color;
	}
	
	public void createEmitters() {
		emitters = new EmitterView[num_effects_in_ring];
		float angle = (float) ((2 * Math.PI) / num_effects_in_ring);
		
		float ring_offset = (float)-0;
		
		for (int i = 0; i < num_effects_in_ring; i++) {
			emitters[i] = new EmitterView((int) (ringD * Math.cos((-i + ring_offset) * angle)), (int) (ringD * Math.sin((-i + ring_offset) * angle)));
			emitters[i].id = i;
		}
	}
	
	public static int getEmitterIndex(int index) {
		if (index < num_effects_in_ring) {
			return index;
		}
		return (index - num_effects_in_ring) / 2;
	}
	
	public void setupPaints() {
		arenaBackground = new Paint();
		arenaBackground.setColor(getResources().getColor(R.color.arena_bg));
	}
	
	@Override
	public void draw(Canvas canvas) {
		// center the ring
		ringX = getWidth() / 2;
		ringY = getHeight() / 2;
		
		// The ring is 85% of the min length
		ringD = (int) (Math.min(getHeight(), getWidth()) * .8) / 2;
		
		if (emitters == null) {
			createEmitters();
		}
		
		canvas.drawRect(0, 0, getWidth(), getHeight(), arenaBackground);
		
		Paint paintEmitterOutline = new Paint();
		paintEmitterOutline.setColor(getResources().getColor(R.color.emitter_outline));
		paintEmitterOutline.setStyle(Paint.Style.STROKE);
		paintEmitterOutline.setAntiAlias(true);
		
		Paint paintEmitterFill = new Paint();
		paintEmitterFill.setStyle(Paint.Style.FILL);
		paintEmitterFill.setColor(getResources().getColor(selectedColor));
		
		// draw the emitters
		for (EmitterView emitter : emitters) {
			paintEmitterFill.setAlpha((int) (255 * emitter.intensity));
			canvas.drawCircle(ringX + emitter.x, ringY + emitter.y, emitterRadius, paintEmitterFill);
			if (emitter.touching) {
				paintEmitterOutline.setStrokeWidth(2);
			} else {
				paintEmitterOutline.setStrokeWidth(1);
			}
			canvas.drawCircle(ringX + emitter.x, ringY + emitter.y, emitterRadius, paintEmitterOutline);
		}
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isInDrawMode) return true; // touching an emitter does nothing

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
            	for (int p = 0; p < event.getPointerCount(); p++) {
                    touch_move(event.getX(p), event.getY(p));
                }
                break;
            case MotionEvent.ACTION_UP:
            	touch_up();
            	break;
        }
        
        this.postInvalidate();
        
        return true;
    }
    
    private void touch_move(float x, float y) {
    	//
    	int emitterTouchRadius = (int) (emitterRadius * 2.0);
		for (EmitterView emitter : emitters) {
			if (Math.sqrt(square(x - emitter.x - ringX) + square(y - emitter.y - ringY)) < emitterTouchRadius) {
				// FIXME the touch should be sending an event to the server, not changing how the ring is drawn directly
				emitter.touching = true;
				if (onEmitterTouch != null)
					onEmitterTouch.onEmitterTouch(emitters, emitter.id);
			} else {
				emitter.touching = false;
			}
		}
    }
    
    private void touch_up() {
		for (EmitterView emitter : emitters) {
			emitter.touching = false;
		}
    }
    
    private static float square(float i) {
    	return i * i;
    }
    
	public class EmitterView {
		int id;
		float intensity = 0;
		boolean touching = false;
		long lastActivated = 0;
		int x;
		int y;
		
		public EmitterView(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	public EmitterView[] getEmitters() {
		return emitters;
	}
	
	public abstract class OnEmitterTouch {
		public abstract void onEmitterTouch(EmitterView emitters[], int id);
	}
}