package org.openhab_nodes;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.view.View;

import org.openhab_nodes.bootstrap.R;

/**
 * Used for the Html.fromString() call to insert images into a text view.
 * The introduction activity uses this.
 */
class BSImageGetter implements Html.ImageGetter {
    private Context context;
    private View textView;

    public BSImageGetter(Context context, View textView) {
        this.context = context;
        this.textView = textView;
    }

    @Override
    public Drawable getDrawable(String source) {
        int res;
        switch (source) {
            case "openhab.png":
                res = R.drawable.nodemcu_1_1;
                break;
            default:
                return null;
        }
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = context.getDrawable(res);
        } else {
            drawable = context.getResources().getDrawable(res);
        }
        //  getResources().getDisplayMetrics().density
        assert drawable != null;
        float f = (((View) textView.getParent()).getWidth() - 10) / (float) drawable.getIntrinsicWidth();
        drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * f),
                (int) (drawable.getIntrinsicHeight() * f));
        return drawable;
    }
}
