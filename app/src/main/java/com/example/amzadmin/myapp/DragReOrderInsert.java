package com.example.amzadmin.myapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public class DragReOrderInsert extends ViewGroup implements View.OnTouchListener {
    private enum ViewType {
        AREA_TOP
        , AREA_BOTTOM
    }

    private static final int NUM_OF_ROWS = 5;

    private WindowManager windowManager;
    private WindowManager.LayoutParams stickyParams;
    private ImageView sticky;
    private View anchor;

    private Map<Integer, View> childView;

    private View selectView = null;
    private int oldPosition = -1;

    private int numOfColumn;
    private int verticalSpace;
    private int horizontalSpace;

    private Adapter topAdapter;
    private Adapter bottomAdapter;

    private Context context;
    private View convertView;

    public DragReOrderInsert(Context context) {
        super(context);
    }

    public DragReOrderInsert(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DragReOrderInsert);
        if (a != null) {
            numOfColumn = a.getInt(R.styleable.DragReOrderInsert_numOfColumn, numOfColumn);
            verticalSpace = a.getDimensionPixelOffset(R.styleable.DragReOrderInsert_verticalSpace, verticalSpace);
            horizontalSpace = a.getDimensionPixelOffset(R.styleable.DragReOrderInsert_horizontalSpace, horizontalSpace);
        }

        if (numOfColumn <= 0) {
            numOfColumn = 1;
        }
        childView = new HashMap<>();
        setOnTouchListener(this);
        a.recycle();

    }

    public DragReOrderInsert(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setTopAdapter(Adapter adapter) {
        this.topAdapter = adapter;
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getTag() == ViewType.AREA_TOP) {
                    removeViewAt(i);
                }
            }
        }
    }
    public void setBottomAdapter(Adapter adapter) {
        this.bottomAdapter = adapter;
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getTag() == ViewType.AREA_BOTTOM) {
                    removeViewAt(i);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        final int count = getChildCount();

        for (int i = 0; i < count; ++i) {
            final View child = getChildAt(i);

            if (child == null) {
                continue;
            }

            if (child.getVisibility() == View.GONE) {
                continue;
            }

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            maxWidth = Math.max(maxWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxHeight += (child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);


            childState = combineMeasuredStates(childState, child.getMeasuredState());
        }

        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(
                resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (bottomAdapter == null || topAdapter == null) {
            return;
        }

        if (getChildCount() == 0 && childView.size() == 0) {
            int width = getChildWidth();
            int height = getChildHeight();

            DragReOrderInsert.LayoutParams param = new DragReOrderInsert.LayoutParams(width, height);
            int idx = 0;
            int size = (topAdapter.getCount() > 9) ? 9 : topAdapter.getCount();
            View child;
            for (int i = 0; i < size; i++) {
                child = topAdapter.getView(i, convertView, this);
                child.setTag(ViewType.AREA_TOP);
                childView.put(idx, child);
                idx++;
            }
            size = (bottomAdapter.getCount() > 6) ? 6 : bottomAdapter.getCount();
            for (int i = 0; i < size; i++) {
                child = bottomAdapter.getView(i, convertView, this);
                child.setTag(ViewType.AREA_BOTTOM);
                childView.put(idx, child);
                idx++;
            }

            for (int key : childView.keySet()) {
                addView(childView.get(key), param);

            }

            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            stickyParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            stickyParams.gravity = Gravity.LEFT | Gravity.TOP;

            sticky = new ImageView(context);
            sticky.setVisibility(GONE);

            anchor = View.inflate(context, R.layout.position, null);
            anchor.setVisibility(GONE);
            addView(anchor, param);

            windowManager.addView(sticky, stickyParams);

        }

        layoutVertical(left, top, right, bottom);

    }

    void layoutVertical(int left, int top, int right, int bottom) {
        for (int key : childView.keySet()) {
            View view = childView.get(key);
            if (oldPosition == -1) {
                view.setVisibility(VISIBLE);
            }
            setChildLayout(view, key);
        }
    }

    private int getChildWidth() {
        return (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - verticalSpace * (numOfColumn + 1)) / numOfColumn;
    }
    private int getChildHeight() {
        return (getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - horizontalSpace * NUM_OF_ROWS + 1) / NUM_OF_ROWS;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new DragReOrderInsert.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int x = (int) event.getRawX();
        int y = (int) event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                int cnt = getChildCount();
                for (int i = 0; i < cnt; i++) {
                    View child = getChildAt(i);

                    if (isPointInsideView(x, y, child)) {
                        child.setDrawingCacheEnabled(true);
                        Bitmap bitmap = Bitmap.createBitmap(child.getDrawingCache());
                        sticky.setImageBitmap(bitmap);

                        selectView = child;
                        oldPosition = getPositionOfChild(x, y);
                        oldPosition = (oldPosition > (NUM_OF_ROWS * numOfColumn - 1)) ? ((NUM_OF_ROWS * numOfColumn) - 1): oldPosition;
                        break;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (oldPosition > -1) {
                    stickyParams.x = x - sticky.getWidth() / 2;
                    stickyParams.y = y - sticky.getHeight() / 2;
                    sticky.setVisibility(VISIBLE);
                    windowManager.updateViewLayout(sticky, stickyParams);

                    int positionToChange = getPositionOfChild(stickyParams.x, stickyParams.y);
                    positionToChange = (positionToChange > (NUM_OF_ROWS * numOfColumn) - 1) ? (NUM_OF_ROWS * numOfColumn) - 1: positionToChange;

                    View viewToChange;
                    if (oldPosition != positionToChange) {
                        viewToChange = childView.get(oldPosition);
                        viewToChange.setVisibility(GONE);

                        if (positionToChange < oldPosition) {
                            for (int i = oldPosition; i > positionToChange; i--) {
                                int previousKey = i - 1;
                                swapChild(i, previousKey, positionToChange);
                            }

                        } else if (oldPosition < positionToChange) {
                            for (int i = oldPosition; i < positionToChange; i++) {
                                int nextKey = i + 1;
                                swapChild(i, nextKey, positionToChange);
                            }
                        }
                        childView.put(positionToChange, viewToChange);
                        setChildLayout(viewToChange, positionToChange);

                        oldPosition = positionToChange;
                    }

                }
                break;

            case MotionEvent.ACTION_UP:
                selectView.setVisibility(VISIBLE);
                anchor.setVisibility(GONE);

                sticky.setVisibility(GONE);
                windowManager.updateViewLayout(sticky, stickyParams);

                oldPosition = -1;
                break;
        }
        return true;
    }

    private void swapChild(int key, int nKey, int newPosition) {
        if (childView.containsKey(key) && childView.containsKey(nKey)) {
            View child = childView.get(nKey);
            childView.put(key, child);
            setChildLayout(childView.get(key), key);

            setAnchor(newPosition);
        }
    }

    private boolean isPointInsideView(int x, int y, View view){
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);

        return rect.contains(x, y);
    }

    private int getPositionOfChild(float x, float y) {
        int width = getChildWidth();
        int height = getChildHeight();

        int column = (int) x / width;
        int row = (int) y / height;

        return ((row * numOfColumn) + column);
    }

    private void setAnchor(int idx) {
        anchor.setVisibility(VISIBLE);
        setChildLayout(anchor, idx);
    }

    private void setChildLayout(View child, int idx) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        int childWidth = getChildWidth();
        int childHeight = getChildHeight();

        int row = idx / numOfColumn;
        int column = idx % numOfColumn;

        int childTop = paddingTop + (childHeight + horizontalSpace) * row;
        int childLeft = paddingLeft + (childWidth + verticalSpace) * column;

        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }


    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }


    public void finish() {
        if (windowManager != null) {
            windowManager.removeView(sticky);
        }
    }

}
