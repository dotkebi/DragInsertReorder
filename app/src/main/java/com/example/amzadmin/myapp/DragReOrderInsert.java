package com.example.amzadmin.myapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
    private WindowManager.LayoutParams anchorParams;
    private ImageView sticky;
    private ImageView anchor;

    private View selectView = null;
    private int oldPosition = -1;
    private int positionToChange = -1;

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
                if (((ViewProperty) child.getTag()).viewType == ViewType.AREA_TOP) {
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
                if (((ViewProperty) child.getTag()).viewType == ViewType.AREA_BOTTOM) {
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

        if (getChildCount() == 0) {
            int width = getChildWidth();
            int height = getChildHeight();

            DragReOrderInsert.LayoutParams param = new DragReOrderInsert.LayoutParams(width, height);

            int idx = 0;
            int size = (topAdapter.getCount() > 9) ? 9 : topAdapter.getCount();
            for (int i = 0; i < size; i++) {
                ViewProperty viewProperty = new ViewProperty();
                View child  = topAdapter.getView(i, convertView, this);
                viewProperty.viewType = ViewType.AREA_TOP;
                viewProperty.sortOrder = idx;
                child.setTag(viewProperty);
                addView(child, param);
                idx++;
            }
            size = (bottomAdapter.getCount() > 6) ? 6 : bottomAdapter.getCount();
            for (int i = 0; i < size; i++) {
                ViewProperty viewProperty = new ViewProperty();
                View child  = bottomAdapter.getView(i, convertView, this);
                viewProperty.viewType = ViewType.AREA_BOTTOM;
                viewProperty.sortOrder = idx;
                child.setTag(viewProperty);
                addView(child, param);
                idx++;
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

            anchorParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            anchorParams.gravity = Gravity.LEFT | Gravity.TOP;

            View roundBox = View.inflate(context, R.layout.position, null);
            addView(roundBox, param);
            setChildLayout(roundBox, 0);
            roundBox.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(roundBox.getDrawingCache());
            removeView(roundBox);

            anchor = new ImageView(context);
            anchor.setImageBitmap(bitmap);
            anchor.setVisibility(GONE);

            windowManager.addView(sticky, stickyParams);
            windowManager.addView(anchor, anchorParams);

        }

        layoutVertical(left, top, right, bottom);

    }

    void layoutVertical(int left, int top, int right, int bottom) {
        List<ViewProperty> list = new ArrayList<>();
        ViewProperty viewProperty;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            viewProperty = (ViewProperty) child.getTag();

            if (child.getVisibility() == GONE) {
                continue;
            }
            list.add(viewProperty);
            setChildLayout(child, viewProperty.sortOrder);
        }

        // Collections.sort(list, new SalesOrderListComparator());

        String msg = "";
        for (ViewProperty item : list) {
            msg += item.sortOrder + " ";
        }
        Log.w("layout", msg);

    }

    private class SalesOrderListComparator implements Comparator<ViewProperty> {
        @Override
        public int compare(ViewProperty lhs, ViewProperty rhs) {
            int sales1 = lhs.sortOrder;
            int sales2 = rhs.sortOrder;

            return sales1 > sales2 ? -1 : (sales1 == sales2 ? 0 : 1);
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

        ViewProperty viewProperty;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                int cnt = getChildCount();
                for (int i = 0; i < cnt; i++) {
                    View child = getChildAt(i);

                    if (isPointInsideView(x, y, child)) {
                        child.setDrawingCacheEnabled(true);
                        Bitmap bitmap = Bitmap.createBitmap(child.getDrawingCache());
                        sticky.setImageBitmap(bitmap);

                        viewProperty = (ViewProperty) child.getTag();
                        child.setTag(viewProperty);
                        selectView = child;
                        oldPosition = getPositionOfChild(x, y);
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

                    positionToChange = getPositionOfChild(stickyParams.x, stickyParams.y);
                    positionToChange = (positionToChange > NUM_OF_ROWS * numOfColumn) ? NUM_OF_ROWS * numOfColumn : positionToChange;

                    View child;
                    Map<Integer, Integer> result = new HashMap<>();

                    if (oldPosition !=  positionToChange) {
                        for (int i = 0; i < getChildCount(); i++) {
                            child = getChildAt(i);
                            viewProperty = (ViewProperty) child.getTag();
                            int position = getPositionOfChild(child.getX(), child.getY());
                            if (positionToChange <= position && position < oldPosition) {
                                viewProperty.sortOrder = position + 1;
                            } else if (oldPosition < position && position <= positionToChange) {
                                viewProperty.sortOrder = position - 1;
                            } /*else {
                                continue;
                            }*/
                            result.put(viewProperty.sortOrder, viewProperty.sortOrder);

                            viewProperty.sortOrder = (viewProperty.sortOrder > NUM_OF_ROWS * numOfColumn) ? NUM_OF_ROWS * numOfColumn : (viewProperty.sortOrder < 0) ? 0 : viewProperty.sortOrder;

                            child.setTag(viewProperty);
                            setChildLayout(child, viewProperty.sortOrder);
                        }

                        viewProperty = (ViewProperty) selectView.getTag();
                        viewProperty.sortOrder = positionToChange;
                        selectView.setTag(viewProperty);
                        selectView.setVisibility(GONE);

                        if (result.containsKey(viewProperty.sortOrder)) {
                            try {
                                throw new MyException(viewProperty.sortOrder);
                            } catch (MyException e) {
                                e.printStackTrace();
                            }
                        } else {
                            result.put(positionToChange, positionToChange);
                        }

                        setAnchor(positionToChange);
                        oldPosition = positionToChange;
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                selectView.setVisibility(VISIBLE);

                sticky.setVisibility(GONE);
                windowManager.updateViewLayout(sticky, stickyParams);

                anchor.setVisibility(GONE);
                windowManager.updateViewLayout(anchor, anchorParams);

                oldPosition = -1;
                break;
        }
        return true;
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

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        int childWidth = getChildWidth();
        int childHeight = getChildHeight();

        int row = idx / numOfColumn;
        int column = idx % numOfColumn;

        int childTop = paddingTop + (childHeight + horizontalSpace) * row;
        int childLeft = paddingLeft + (childWidth + verticalSpace) * column;

        anchorParams.x = childLeft + paddingLeft + 64;
        anchorParams.y = childTop + paddingTop + 64 ;
        anchor.setVisibility(VISIBLE);

        windowManager.updateViewLayout(anchor, anchorParams);
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

    public class ViewProperty {
        ViewType viewType;
        int sortOrder;
    }

    public void finish() {
        if (windowManager != null) {
            windowManager.removeView(anchor);
            windowManager.removeView(sticky);
        }
    }

    public class MyException extends Exception {
        int num;

        public MyException(int num) {
            this.num = num;
        }
        @Override
        public String getMessage() {
            return String.valueOf(num) +
                    "중복";
        }
    }

}
