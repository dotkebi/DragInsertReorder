package com.example.amzadmin.myapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ImageView;

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

    private int selX;
    private int selY;
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
            verticalSpace = a.getInt(R.styleable.DragReOrderInsert_verticalSpace, verticalSpace);
            horizontalSpace = a.getInt(R.styleable.DragReOrderInsert_horizontalSpace, horizontalSpace);
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
        ViewProperty viewProperty;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            viewProperty = (ViewProperty) child.getTag();

            if (child.getVisibility() == GONE
                    || viewProperty.sortOrder == -1
                    ) {
                continue;
            }

            setChildLayout(child, viewProperty.sortOrder);
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

                    if (positionToChange < 0) {
                        positionToChange = 0;
                    }
                    if (positionToChange > 14) {
                        positionToChange = 14;
                    }

                    setAnchor(positionToChange);

                    viewProperty = (ViewProperty) selectView.getTag();
                    viewProperty.sortOrder = positionToChange;
                    selectView.setTag(viewProperty);
                    selectView.setVisibility(GONE);

                    if (oldPosition == positionToChange) {
                        break;
                    }


                    View child;
                    if (oldPosition > positionToChange) {
                        for (int i = 0; i < getChildCount(); i++) {
                            child = getChildAt(i);
                            viewProperty = (ViewProperty) child.getTag();
                            if (child.getVisibility() == GONE) {
                                continue;
                            }
                            int position = getPositionOfChild(child);
                            if (positionToChange <= position && position < oldPosition) {
                                viewProperty.sortOrder = position + 1;
                                child.setTag(viewProperty);
                                setChildLayout(child, position + 1);
                            }
                        }

                    }
                    if (oldPosition < positionToChange) {
                        for (int i = 0; i < getChildCount(); i++) {
                            child = getChildAt(i);
                            viewProperty = (ViewProperty) child.getTag();
                            if (child.getVisibility() == GONE) {
                                continue;
                            }
                            int position = getPositionOfChild(child);
                            if (position == 0) {
                                continue;
                            }
                            if (oldPosition <= position && position <= positionToChange) {
                                viewProperty.sortOrder = position - 1;
                                child.setTag(viewProperty);
                                setChildLayout(child, position - 1);
                            }
                        }

                    }
                    oldPosition = positionToChange;

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
    //};

    private boolean isPointInsideView(float x, float y, View view){
        int viewX = view.getLeft();
        int viewY = view.getTop();

        //point is inside view bounds
        return (x > viewX && x < (viewX + view.getWidth())) &&
                (y > viewY && y < (viewY + view.getHeight()));
    }

    private int getPositionOfChild(float x, float y) {
        int width = getChildWidth();
        int height = getChildHeight();

        int column = (int) x / width;
        int row = (int) y / height;

        return ((row * numOfColumn) + column);
    }

    private int getPositionOfChild(View child) {
        int width = getChildWidth();
        int height = getChildHeight();

        int column = child.getLeft() / width;
        int row = child.getTop() / height;

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


        anchorParams.x = childLeft + 64;
        anchorParams.y = childTop + 64;
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

}
