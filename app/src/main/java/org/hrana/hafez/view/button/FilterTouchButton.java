package org.hrana.hafez.view.button;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

/**
 * Tapjack mitigation subclass of button.
 */

public class FilterTouchButton extends AppCompatButton {
    public FilterTouchButton(Context context) {
        super(context);
        setFilterTouchesWhenObscured(true);
    }

    public FilterTouchButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFilterTouchesWhenObscured(true);

    }

    public FilterTouchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFilterTouchesWhenObscured(true);

    }
}
