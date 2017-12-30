package bottomsheet.lamle.com.bottomsheet;

import android.content.Context;
import android.util.AttributeSet;

import java.util.List;

/**
 * Created by lelam on 12/30/17.
 */

public class WheelChoiceLayout extends WheelPicker {

    private OnItemSelectedListener mOnItemSelectedListener;
    private int mDefaultIndex;
    protected WheelPicker.Adapter mAdapter;

    public WheelChoiceLayout(Context context) {
        this(context, null);
    }

    public WheelChoiceLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAdapter = new Adapter();
        setAdapter(mAdapter);
    }

    @Override
    protected void onItemSelected(int position, Object item) {
        if (null != mOnItemSelectedListener) {
            final String itemText = (String) item;
            mOnItemSelectedListener.onItemSelected(this,position,itemText);
        }
    }

    @Override
    protected void onItemCurrentScroll(int position, Object item) {
    }

    @Override
    public int getDefaultItemPosition() {
        return mDefaultIndex;
    }

    public void addItems(List<String> data, int defaultIndex) {
        this.mDefaultIndex = defaultIndex;
        mAdapter.setData(data);
        updateDefaultItem();
    }


    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.mOnItemSelectedListener = onItemSelectedListener;
    }

    private void updateDefaultItem() {setSelectedItemPosition(mDefaultIndex);}

    public int getDefaultItemIndex() {return mDefaultIndex;}

    public String getCurrentSelection() {
        return mAdapter.getItemText(getCurrentItemPosition());
    }

    public interface OnItemSelectedListener {
        void onItemSelected(WheelChoiceLayout wheelChoiceLayout, int position, String name);
    }
}
