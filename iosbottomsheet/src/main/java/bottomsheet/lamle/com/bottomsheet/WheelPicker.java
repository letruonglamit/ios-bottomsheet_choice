package bottomsheet.lamle.com.bottomsheet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by lelam on 12/30/17.
 */

public abstract class WheelPicker extends View {

    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SCROLLING = 2;

    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;

    private final Handler mHandler = new Handler();
    private final Paint mPaintBackground;

    private Paint mPaint;
    private Scroller mScroller;
    private VelocityTracker mTracker;

    private OnItemSelectedListener mOnItemSelectedListener;
    private OnWheelChangeListener mOnWheelChangeListener;

    private Rect mRectDrawn;
    private Rect mRectIndicatorHead, mRectIndicatorFoot;
    private Rect mRectCurrentItem;

    private BaseAdapter mAdapter;
    private String mMaxWidthText;

    private int mVisibleItemCount, mDrawnItemCount;
    private int mHalfDrawnItemCount;
    private int mTextMaxWidth, mTextMaxHeight;
    private int mItemTextColor, mSelectedItemTextColor;
    private int mItemTextSize;
    private int mIndicatorSize;
    private int mIndicatorColor;
    private int mItemSpace;
    private int mItemAlign;
    private int mItemHeight, mHalfItemHeight;
    private int mHalfWheelHeight;
    private int mSelectedItemPosition;
    private int mCurrentItemPosition;
    private int minFlingY, mMaxFlingY;
    private int mMinimumVelocity = 50, mMaximumVelocity = 8000;
    private int mWheelCenterX, mWheelCenterY;
    private int mDrawnCenterX, mDrawnCenterY;
    private int mScrollOffsetY;
    private int mTextMaxWidthPosition;
    private int mLastPointY;
    private int mDownPointY;
    private int mTouchSlop = 8;

    private boolean mHasIndicator;
    private boolean mHasAtmospheric;

    private boolean mIsClick;
    private boolean mIsForceFinishScroll;

    private int mBackgroundColor;
    private int mBackgroundOfSelectedItem;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (null == mAdapter) return;
            final int itemCount = mAdapter.getItemCount();
            if (itemCount == 0) return;
            if (mScroller.isFinished() && !mIsForceFinishScroll) {
                if (mItemHeight == 0) return;
                int position = (-mScrollOffsetY / mItemHeight + mSelectedItemPosition) % itemCount;
                position = position < 0 ? position + itemCount : position;
                mCurrentItemPosition = position;
                onItemSelected();
                if (null != mOnWheelChangeListener) {
                    mOnWheelChangeListener.onWheelSelected(position);
                    mOnWheelChangeListener.onWheelScrollStateChanged(SCROLL_STATE_IDLE);
                }
            }
            if (mScroller.computeScrollOffset()) {
                if (null != mOnWheelChangeListener) {
                    mOnWheelChangeListener.onWheelScrollStateChanged(SCROLL_STATE_SCROLLING);
                }

                mScrollOffsetY = mScroller.getCurrY();

                int position = (-mScrollOffsetY / mItemHeight + mSelectedItemPosition) % itemCount;
                if (mOnItemSelectedListener != null) {
                    mOnItemSelectedListener.onCurrentItemOfScroll(WheelPicker.this, position);
                }
                onItemCurrentScroll(position, mAdapter.getItem(position));

                postInvalidate();
                mHandler.postDelayed(this, 16);
            }
        }
    };

    public WheelPicker(Context context) {
        this(context, null);
    }

    public WheelPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAdapter = new Adapter();

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker);

        mItemTextSize = typedArray.getDimensionPixelSize(R.styleable.WheelPicker_scroll_item_text_size,
                getResources().getDimensionPixelSize(R.dimen.WheelItemTextSize)); // 9

        mVisibleItemCount = typedArray.getInt(R.styleable.WheelPicker_scroll_visible_item_count, 7); //7

        mSelectedItemPosition = typedArray.getInt(R.styleable.WheelPicker_scroll_selected_item_position, 0); // 0
        mTextMaxWidthPosition = typedArray.getInt(R.styleable.WheelPicker_scroll_maximum_width_text_position, -1); // -1
        mMaxWidthText = typedArray.getString(R.styleable.WheelPicker_scroll_maximum_width_text); // null
        mSelectedItemTextColor = typedArray.getColor(R.styleable.WheelPicker_scroll_selected_item_text_color, -1); // -16777216
        mItemTextColor = typedArray.getColor(R.styleable.WheelPicker_scroll_item_text_color, 0xFF424242);  // -7105645
        mBackgroundColor = typedArray.getColor(R.styleable.WheelPicker_scroll_background_color, 0xFFFFFFFF); // -1
        mBackgroundOfSelectedItem = typedArray.getColor(R.styleable.WheelPicker_scroll_selected_item_background, 0xFFFFFFFF);  // -1
        mItemSpace = typedArray.getDimensionPixelSize(R.styleable.WheelPicker_scroll_item_space,  //0
                getResources().getDimensionPixelSize(R.dimen.WheelItemSpace));
        mHasIndicator = typedArray.getBoolean(R.styleable.WheelPicker_scroll_indicator, false); //true
        mIndicatorColor = typedArray.getColor(R.styleable.WheelPicker_scroll_indicator_color, 0xFFDDDDDD);  // -9737365
        mIndicatorSize = typedArray.getDimensionPixelSize(R.styleable.WheelPicker_scroll_indicator_size, // i
                getResources().getDimensionPixelSize(R.dimen.WheelIndicatorSize));
        mHasAtmospheric = typedArray.getBoolean(R.styleable.WheelPicker_scroll_atmospheric, false);  //true
        mItemAlign = typedArray.getInt(R.styleable.WheelPicker_scroll_item_align, ALIGN_CENTER); // 0
        typedArray.recycle();

        updateVisibleItemCount();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.LINEAR_TEXT_FLAG);
        mPaintBackground = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG );
        mPaint.setTextSize(mItemTextSize);

        updateItemTextAlign();

        computeTextSize();

        mScroller = new Scroller(getContext());


        ViewConfiguration conf = ViewConfiguration.get(getContext());
        mMinimumVelocity = conf.getScaledMinimumFlingVelocity();
        mMaximumVelocity = conf.getScaledMaximumFlingVelocity();
        mTouchSlop = conf.getScaledTouchSlop();
        mRectDrawn = new Rect();

        mRectIndicatorHead = new Rect();
        mRectIndicatorFoot = new Rect();

        mRectCurrentItem = new Rect();
    }

    private void updateVisibleItemCount() {
        if (mVisibleItemCount < 2)
            throw new ArithmeticException("Wheel's visible item count can not be less than 2!");

        if (mVisibleItemCount % 2 == 0) mVisibleItemCount += 1;
        mDrawnItemCount = mVisibleItemCount + 2;
        mHalfDrawnItemCount = mDrawnItemCount / 2;
    }

    private void computeTextSize() {
        mTextMaxWidth = mTextMaxHeight = 0;

        if (isPosInRang(mTextMaxWidthPosition)) {
            mTextMaxWidth = (int) mPaint.measureText(mAdapter.getItemText(mTextMaxWidthPosition));
        } else if (!TextUtils.isEmpty(mMaxWidthText)) {
            mTextMaxWidth = (int) mPaint.measureText(mMaxWidthText);
        } else {
            final int itemCount = mAdapter.getItemCount();
            for (int i = 0; i < itemCount; ++i) {
                String text = mAdapter.getItemText(i);
                int width = (int) mPaint.measureText(text);
                mTextMaxWidth = Math.max(mTextMaxWidth, width);
            }
        }
        Paint.FontMetrics metrics = mPaint.getFontMetrics();
        mTextMaxHeight = (int) (metrics.bottom - metrics.top);
    }

    private void updateItemTextAlign() {
        switch (mItemAlign) {
            case ALIGN_LEFT:
                mPaint.setTextAlign(Paint.Align.LEFT);
                break;
            case ALIGN_RIGHT:
                mPaint.setTextAlign(Paint.Align.RIGHT);
                break;
            default:
                mPaint.setTextAlign(Paint.Align.CENTER);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Correct sizes of original content
        int resultWidth = mTextMaxWidth;
        int resultHeight = mTextMaxHeight * mVisibleItemCount + mItemSpace * (mVisibleItemCount - 1);

        // Consideration padding influence the view sizes
        resultWidth += getPaddingLeft() + getPaddingRight();
        resultHeight += getPaddingTop() + getPaddingBottom();

        // Consideration sizes of parent can influence the view sizes
        resultWidth = measureSize(modeWidth, sizeWidth, resultWidth);
        resultHeight = measureSize(modeHeight, sizeHeight, resultHeight);

        setMeasuredDimension(resultWidth, resultHeight);
    }

    private int measureSize(int mode, int sizeExpect, int sizeActual) {
        int realSize;
        if (mode == MeasureSpec.EXACTLY) {
            realSize = sizeExpect;
        } else {
            realSize = sizeActual;
            if (mode == MeasureSpec.AT_MOST) realSize = Math.min(realSize, sizeExpect);
        }
        return realSize;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set content region
        mRectDrawn.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());

        // Get the center coordinates of content region
        mWheelCenterX = mRectDrawn.centerX();
        mWheelCenterY = mRectDrawn.centerY();

        // Correct item drawn center
        computeDrawnCenter();

        mHalfWheelHeight = mRectDrawn.height() / 2;

        mItemHeight = mRectDrawn.height() / mVisibleItemCount;
        mHalfItemHeight = mItemHeight / 2;

        // Initialize fling max Y-coordinates
        computeFlingLimitY();

        // Correct region of indicator
        computeIndicatorRect();

        // Correct region of current select item
        computeCurrentItemRect();
    }

    private void computeDrawnCenter() {
        switch (mItemAlign) {
            case ALIGN_LEFT:
                mDrawnCenterX = mRectDrawn.left;
                break;
            case ALIGN_RIGHT:
                mDrawnCenterX = mRectDrawn.right;
                break;
            default:
                mDrawnCenterX = mWheelCenterX;
                break;
        }
        mDrawnCenterY = (int) (mWheelCenterY - ((mPaint.ascent() + mPaint.descent()) / 2));
    }

    private void computeFlingLimitY() {
        int currentItemOffset = mSelectedItemPosition * mItemHeight;
        minFlingY = -mItemHeight * (mAdapter.getItemCount() - 1) + currentItemOffset;
        mMaxFlingY = currentItemOffset;
    }

    private void computeIndicatorRect() {
        if (!mHasIndicator) return;
        int halfIndicatorSize = mIndicatorSize / 2;
        int indicatorHeadCenterY = mWheelCenterY + mHalfItemHeight;
        int indicatorFootCenterY = mWheelCenterY - mHalfItemHeight;
        int topHead = indicatorHeadCenterY - halfIndicatorSize + (halfIndicatorSize == 0 ? 1 : 0);
        int botHead = indicatorHeadCenterY + halfIndicatorSize;
        int topFoot = indicatorFootCenterY - halfIndicatorSize - 3;
        int botFoot = indicatorFootCenterY + halfIndicatorSize - 3 + (halfIndicatorSize == 0 ? 1 : 0);
        mRectIndicatorHead.set(mRectDrawn.left, topHead, mRectDrawn.right,
                botHead);
        mRectIndicatorFoot.set(mRectDrawn.left, topFoot , mRectDrawn.right,
                botFoot);
    }

    private void computeCurrentItemRect() {
        if (mSelectedItemTextColor == -1) return;
        mRectCurrentItem.set(mRectDrawn.left, mWheelCenterY - mHalfItemHeight, mRectDrawn.right,
                mWheelCenterY + mHalfItemHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (null != mOnWheelChangeListener) mOnWheelChangeListener.onWheelScrolled(mScrollOffsetY);
        int drawnDataStartPos = -mScrollOffsetY / mItemHeight - mHalfDrawnItemCount;

        //this sets background color of the whole view
        mPaintBackground.setColor(mBackgroundColor);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mPaintBackground);

        //this sets background color of the selected item
        mPaintBackground.setColor(mBackgroundOfSelectedItem);
        mPaintBackground.setStyle(Paint.Style.FILL);
        canvas.drawRect(mRectCurrentItem, mPaintBackground);

        for (int drawnDataPos = drawnDataStartPos + mSelectedItemPosition,
             drawnOffsetPos = -mHalfDrawnItemCount;
             drawnDataPos < drawnDataStartPos + mSelectedItemPosition + mDrawnItemCount;
             drawnDataPos++, drawnOffsetPos++) {
            String data = "";

            if (isPosInRang(drawnDataPos)) data = mAdapter.getItemText(drawnDataPos);

            mPaint.setColor(mItemTextColor);
            mPaint.setTextSize(mItemTextSize);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

            int mDrawnItemCenterY = mDrawnCenterY + (drawnOffsetPos * mItemHeight) +
                    mScrollOffsetY % mItemHeight;

            if (mHasAtmospheric) {
                int alpha =
                        (int) ((mDrawnCenterY - Math.abs(mDrawnCenterY - mDrawnItemCenterY)) * 1.0F / mDrawnCenterY * 255);
                alpha = alpha < 0 ? 0 : alpha;
                mPaint.setAlpha(alpha);
            }
            // Correct item's drawn centerY base on curved state
            int drawnCenterY = mDrawnItemCenterY;

            // Judges need to draw different color for current item or not
            if (mSelectedItemTextColor != -1) {
                canvas.save();
                canvas.clipRect(mRectCurrentItem, Region.Op.DIFFERENCE);
                canvas.drawText(data, mDrawnCenterX, drawnCenterY, mPaint);
                canvas.restore();

                mPaint.setColor(mSelectedItemTextColor);
                mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                mPaint.setTextSize(mItemTextSize + 3);

                canvas.save();
                canvas.clipRect(mRectCurrentItem);
                canvas.drawText(data, mDrawnCenterX, drawnCenterY, mPaint);
                canvas.restore();

            } else {

                canvas.save();
                canvas.clipRect(mRectDrawn);
                canvas.drawText(data, mDrawnCenterX, drawnCenterY, mPaint);
                canvas.restore();
            }
        }
        // Need to draw indicator or not
        if (mHasIndicator) {
            mPaint.setColor(mIndicatorColor);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(mRectIndicatorHead, mPaint);
            canvas.drawRect(mRectIndicatorFoot, mPaint);
        }

    }

    private boolean isPosInRang(int position) {
        return position >= 0 && position < mAdapter.getItemCount();
    }

    private int computeSpace(int degree) {
        return (int) (Math.sin(Math.toRadians(degree)) * mHalfWheelHeight);
    }

    private int computeDepth(int degree) {
        return (int) (mHalfWheelHeight - Math.cos(Math.toRadians(degree)) * mHalfWheelHeight);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (null != getParent()) getParent().requestDisallowInterceptTouchEvent(true);
                if (null == mTracker) {
                    mTracker = VelocityTracker.obtain();
                } else {
                    mTracker.clear();
                }
                mTracker.addMovement(event);
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    mIsForceFinishScroll = true;
                }
                mDownPointY = mLastPointY = (int) event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(mDownPointY - event.getY()) < mTouchSlop) {
                    mIsClick = true;
                    break;
                }
                mIsClick = false;
                mTracker.addMovement(event);
                if (null != mOnWheelChangeListener) {
                    mOnWheelChangeListener.onWheelScrollStateChanged(SCROLL_STATE_DRAGGING);
                }

                // Scroll WheelPicker's content
                float move = event.getY() - mLastPointY;
                if (Math.abs(move) < 1) break;
                mScrollOffsetY += move;
                mLastPointY = (int) event.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (null != getParent()) getParent().requestDisallowInterceptTouchEvent(false);
                if (mIsClick) break;
                mTracker.addMovement(event);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
                    mTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                } else {
                    mTracker.computeCurrentVelocity(1000);
                }

                // Judges the WheelPicker is scroll or fling base on current velocity
                mIsForceFinishScroll = false;
                int velocity = (int) mTracker.getYVelocity();
                if (Math.abs(velocity) > mMinimumVelocity) {
                    mScroller.fling(0, mScrollOffsetY, 0, velocity, 0, 0, minFlingY, mMaxFlingY);
                    mScroller.setFinalY(
                            mScroller.getFinalY() + computeDistanceToEndPoint(mScroller.getFinalY() % mItemHeight));
                } else {
                    mScroller.startScroll(0, mScrollOffsetY, 0, computeDistanceToEndPoint(mScrollOffsetY % mItemHeight));
                }

                mHandler.post(runnable);
                if (null != mTracker) {
                    mTracker.recycle();
                    mTracker = null;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (null != getParent()) getParent().requestDisallowInterceptTouchEvent(false);
                if (null != mTracker) {
                    mTracker.recycle();
                    mTracker = null;
                }
                break;
        }
        return true;
    }

    private int computeDistanceToEndPoint(int remainder) {
        if (Math.abs(remainder) > mHalfItemHeight) {
            if (mScrollOffsetY < 0) {
                return -mItemHeight - remainder;
            } else {
                return mItemHeight - remainder;
            }
        } else {
            return -remainder;
        }
    }

    public void scrollTo(final int itemPosition) {
        if (itemPosition != mCurrentItemPosition) {
            final int differencesLines = mCurrentItemPosition - itemPosition;
            final int newScrollOffsetY = mScrollOffsetY + (differencesLines * mItemHeight); // % mAdapter.getItemCount();

            ValueAnimator va = ValueAnimator.ofInt(mScrollOffsetY, newScrollOffsetY);
            va.setDuration(300);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    mScrollOffsetY = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mCurrentItemPosition = itemPosition;
                    onItemSelected();
                }
            });
            va.start();
        }
    }

    private final void onItemSelected() {
        int position = mCurrentItemPosition;
        final Object item = this.mAdapter.getItem(position);
        if (null != mOnItemSelectedListener) {
            mOnItemSelectedListener.onItemSelected(this, item, position);
        }
        onItemSelected(position, item);
    }

    protected abstract void onItemSelected(int position, Object item);

    protected abstract void onItemCurrentScroll(int position, Object item);

    public int getVisibleItemCount() {
        return mVisibleItemCount;
    }

    public void setVisibleItemCount(int count) {
        mVisibleItemCount = count;
        updateVisibleItemCount();
        requestLayout();
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
    }

    public int getSelectedItemPosition() {
        return mSelectedItemPosition;
    }

    public void setSelectedItemPosition(int position) {
        position = Math.min(position, mAdapter.getItemCount() - 1);
        position = Math.max(position, 0);
        mSelectedItemPosition = position;
        mCurrentItemPosition = position;
        mScrollOffsetY = 0;
        computeFlingLimitY();
        requestLayout();
        invalidate();
    }

    public int getCurrentItemPosition() {
        return mCurrentItemPosition;
    }

    public abstract int getDefaultItemPosition();

    public void setAdapter(Adapter adapter) {
        this.mAdapter = adapter;
        notifyDatasetChanged();
    }

    public void notifyDatasetChanged() {
        if (mSelectedItemPosition > mAdapter.getItemCount() - 1 || mCurrentItemPosition > mAdapter.getItemCount() - 1) {
            mSelectedItemPosition = mCurrentItemPosition = mAdapter.getItemCount() - 1;
        } else {
            mSelectedItemPosition = mCurrentItemPosition;
        }
        mScrollOffsetY = 0;
        computeTextSize();
        computeFlingLimitY();
        requestLayout();
        invalidate();
    }

    public void setOnWheelChangeListener(OnWheelChangeListener listener) {
        mOnWheelChangeListener = listener;
    }

    public String getMaximumWidthText() {
        return mMaxWidthText;
    }

    public void setMaximumWidthText(String text) {
        if (null == text) throw new NullPointerException("Maximum width text can not be null!");
        mMaxWidthText = text;
        computeTextSize();
        requestLayout();
        invalidate();
    }

    public int getMaximumWidthTextPosition() {
        return mTextMaxWidthPosition;
    }

    public void setMaximumWidthTextPosition(int position) {
        if (!isPosInRang(position)) {
            throw new ArrayIndexOutOfBoundsException("Maximum width text Position must in [0, " +
                    mAdapter.getItemCount() + "), but current is " + position);
        }
        mTextMaxWidthPosition = position;
        computeTextSize();
        requestLayout();
        invalidate();
    }

    public int getSelectedItemTextColor() {
        return mSelectedItemTextColor;
    }

    public void setSelectedItemTextColor(int color) {
        mSelectedItemTextColor = color;
        computeCurrentItemRect();
        invalidate();
    }

    public int getItemTextColor() {
        return mItemTextColor;
    }

    public void setItemTextColor(int color) {
        mItemTextColor = color;
        invalidate();
    }

    public int getItemTextSize() {
        return mItemTextSize;
    }

    public void setItemTextSize(int size) {
        mItemTextSize = size;
        mPaint.setTextSize(mItemTextSize);
        computeTextSize();
        requestLayout();
        invalidate();
    }

    public int getItemSpace() {
        return mItemSpace;
    }

    public void setItemSpace(int space) {
        mItemSpace = space;
        requestLayout();
        invalidate();
    }

    public void setIndicator(boolean hasIndicator) {
        this.mHasIndicator = hasIndicator;
        computeIndicatorRect();
        invalidate();
    }

    public boolean hasIndicator() {
        return mHasIndicator;
    }

    public int getIndicatorSize() {
        return mIndicatorSize;
    }

    public void setIndicatorSize(int size) {
        mIndicatorSize = size;
        computeIndicatorRect();
        invalidate();
    }

    public int getIndicatorColor() {
        return mIndicatorColor;
    }

    public void setIndicatorColor(int color) {
        mIndicatorColor = color;
        invalidate();
    }

    public void setAtmospheric(boolean hasAtmospheric) {
        this.mHasAtmospheric = hasAtmospheric;
        invalidate();
    }

    public boolean hasAtmospheric() {
        return mHasAtmospheric;
    }

    public int getItemAlign() {
        return mItemAlign;
    }

    public void setItemAlign(int align) {
        mItemAlign = align;
        updateItemTextAlign();
        computeDrawnCenter();
        invalidate();
    }

    public Typeface getTypeface() {
        if (null != mPaint) return mPaint.getTypeface();
        return null;
    }

    public void setTypeface(Typeface tf) {
        if (null != mPaint) mPaint.setTypeface(tf);
        computeTextSize();
        requestLayout();
        invalidate();
    }

    @TargetApi(Build.VERSION_CODES.N)
    public Locale getCurrentLocale() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getResources().getConfiguration().getLocales().get(0);
        } else {
            //noinspection deprecation
            return getResources().getConfiguration().locale;
        }
    }

    public interface BaseAdapter {

        int getItemCount();

        Object getItem(int position);

        String getItemText(int position);
    }

    private interface OnItemSelectedListener {
        void onItemSelected(WheelPicker picker, Object data, int position);
        void onCurrentItemOfScroll(WheelPicker picker, int position);
    }

    public interface OnWheelChangeListener {
        /**
         * <p>
         * Invoke when WheelPicker scroll stopped
         * WheelPicker will return a distance offset which between current scroll position and
         * initial position, this offset is a positive or a negative, positive means WheelPicker is
         * scrolling from bottom to top, negative means WheelPicker is scrolling from top to bottom
         *
         * @param offset <p>
         *               Distance offset which between current scroll position and initial position
         */
        void onWheelScrolled(int offset);

        /**
         * <p>
         * Invoke when WheelPicker scroll stopped
         * This method will be called when WheelPicker stop and return current selected item data's
         * position in list
         *
         * @param position <p>
         *                 Current selected item data's position in list
         */
        void onWheelSelected(int position);

        /**
         * <p>
         * Invoke when WheelPicker's scroll state changed
         * The state of WheelPicker always between idle, dragging, and scrolling, this method will
         * be called when they switch
         *
         * @param state {@link WheelPicker#SCROLL_STATE_IDLE}
         *              {@link WheelPicker#SCROLL_STATE_DRAGGING}
         *              {@link WheelPicker#SCROLL_STATE_SCROLLING}
         *              <p>
         *              State of WheelPicker, only one of the following
         *              {@link WheelPicker#SCROLL_STATE_IDLE}
         *              Express WheelPicker in state of idle
         *              {@link WheelPicker#SCROLL_STATE_DRAGGING}
         *              Express WheelPicker in state of dragging
         *              {@link WheelPicker#SCROLL_STATE_SCROLLING}
         *              Express WheelPicker in state of scrolling
         */
        void onWheelScrollStateChanged(int state);
    }

    public static class Adapter implements BaseAdapter {
        private List data;

        public Adapter() {
            this(new ArrayList());
        }

        public Adapter(List data) {
            this.data = new ArrayList();
            this.data.addAll(data);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            final int itemCount = getItemCount();
            return data.get((position + itemCount) % itemCount);
        }

        @Override
        public String getItemText(int position) {
            return String.valueOf(data.get(position));
        }

        public void setData(List data) {
            this.data.clear();
            this.data.addAll(data);
        }

        public void addData(List data) {
            this.data.addAll(data);
        }
    }
}
