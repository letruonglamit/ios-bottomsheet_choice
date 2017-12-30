package bottomsheet.lamle.com.bottomsheet;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lelam on 12/30/17.
 */

public class BottomSheetChoiceDialogFragment extends BottomSheetDialogFragment {

    private List<String> mData = new ArrayList<>();
    private String mNameSelected;
    private int mPositionSelected;
    private OnItemSelectedListener mSelectedListener;
    private OnItemSelectedDoneListener mDoneListener;

    private int mTextSize;
    private int mItemSpace;

    public static BottomSheetChoiceDialogFragment newInstance() {
        return new BottomSheetChoiceDialogFragment();
    }

    public BottomSheetChoiceDialogFragment setData(List<String> data) {
        mData.addAll(data);
        return this;
    }

    public BottomSheetChoiceDialogFragment setSelectedListener(OnItemSelectedListener selectedListener) {
        mSelectedListener = selectedListener;
        return this;
    }

    public BottomSheetChoiceDialogFragment setSelectedDoneListener(OnItemSelectedDoneListener selectedDoneListener) {
        mDoneListener = selectedDoneListener;
        return this;
    }

    public BottomSheetChoiceDialogFragment setTextSize(int textSize) {
        mTextSize = textSize;
        return this;
    }

    public BottomSheetChoiceDialogFragment setItemSpace(int itemSpace) {
        mItemSpace = itemSpace;
        return this;
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, BottomSheetChoiceDialogFragment.class.getSimpleName());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(
                R.layout.fragment_bottom_sheet_choice,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        WheelChoiceLayout wheelChoiceLayout = view.findViewById(R.id.scrollChoice);

        if (mTextSize != 0) {
            wheelChoiceLayout.setItemTextSize(mTextSize);
        }
        if (mItemSpace != 0) {
            wheelChoiceLayout.setItemSpace(mItemSpace);
        }
        int defaultIndex = mData.size() / 2;
        mNameSelected = mData.get(defaultIndex);
        mPositionSelected = defaultIndex;
        wheelChoiceLayout.addItems(mData, defaultIndex);
        wheelChoiceLayout.setOnItemSelectedListener(new WheelChoiceLayout.OnItemSelectedListener() {
            @Override
            public void onItemSelected(WheelChoiceLayout wheelChoiceLayout, int position, String name) {
                if (mSelectedListener != null) {
                    mSelectedListener.onItemSelected(position, name);
                }
                mPositionSelected = position;
                mNameSelected = name;
            }
        });

        final TextView doneButton = view.findViewById(R.id.doneButton);
        final TextView cancelButton = view.findViewById(R.id.cancelButton);

        doneButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        doneButton.setTextColor(Color.parseColor("#9bbbff"));
                        break;
                    case MotionEvent.ACTION_UP:
                        doneButton.setTextColor(Color.parseColor("#2558ff"));
                        if (mDoneListener != null) {
                            mDoneListener.onItemSelected(mPositionSelected, mNameSelected);
                        }
                        dismiss();
                    case MotionEvent.ACTION_CANCEL:
                        doneButton.setTextColor(Color.parseColor("#2558ff"));
                        break;
                }
                return true;
            }
        });

        cancelButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cancelButton.setTextColor(Color.parseColor("#9bbbff"));
                        break;
                    case MotionEvent.ACTION_UP:
                        cancelButton.setTextColor(Color.parseColor("#2558ff"));
                        dismiss();
                    case MotionEvent.ACTION_CANCEL:
                        cancelButton.setTextColor(Color.parseColor("#2558ff"));
                        break;
                }
                return true;
            }
        });

    }

    public interface OnItemSelectedListener {
        void onItemSelected(int position, String name);
    }

    public interface OnItemSelectedDoneListener {
        void onItemSelected(int position, String name);
    }
}
