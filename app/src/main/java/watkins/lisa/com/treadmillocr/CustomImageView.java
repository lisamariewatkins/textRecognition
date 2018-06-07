package watkins.lisa.com.treadmillocr;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lwatkins2 on 5/29/18.
 */

public class CustomImageView extends android.support.v7.widget.AppCompatImageView{
    private List<Rect> rectList;

    public CustomImageView(Context context) {
        super(context);
    }

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addRect(Rect rect) {
        if (rectList == null) {
            rectList = new ArrayList<>();
        }
        rectList.add(rect);
    }

    public void setRectListToNull() {
        rectList = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Toast.makeText(getContext(), String.valueOf(event.getX()) + ", " + String.valueOf(event.getY()), Toast.LENGTH_LONG).show();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (rectList != null) {
                for (Rect rect : rectList) {
                    if (x > rect.left && x < rect.right && y > rect.bottom && y < rect.top) {
                        Toast.makeText(getContext(), String.valueOf(event.getX()) + ", " + String.valueOf(event.getY()), Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            }
        }
        return false;
    }
}
