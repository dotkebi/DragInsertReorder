package com.example.amzadmin.myapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
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
        , ANCHOR
        , STICKY

    }

    private static final int NUM_OF_ROWS = 5;

    private WindowManager windowManager;
    private WindowManager.LayoutParams mParams;
    private ImageView sticky;

    private int selX;
    private int selY;
    private View selectView = null;
    private View viewToChangePlace;
    private int oldPosition = -1;
    private int positionToChange = -1;

    private int numOfColumn;
    private int verticalSpace;
    private int horizontalSpace;

    private Adapter topAdapter;
    private Adapter bottomAdapter;

    private ViewProperty value;
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

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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

            ViewProperty viewProperty = new ViewProperty();
            viewToChangePlace = View.inflate(context, R.layout.position, null);
            viewProperty.viewType = ViewType.ANCHOR;
            viewProperty.sortOrder = -1;
            viewToChangePlace.setTag(viewProperty);
            viewToChangePlace.setVisibility(GONE);
            addView(viewToChangePlace, param);

            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            mParams.gravity = Gravity.LEFT | Gravity.TOP;

            sticky = new ImageView(context);
            sticky.setVisibility(GONE);

            windowManager.addView(sticky, mParams);

        }

        layoutVertical(left, top, right, bottom);



        //shuffle();
    }

    private void shuffle() {
        Log.w("shuffle", "shuffle");
        int[] order = {
                4,  2,  1
                ,  3,  5,  7
                , 10, 14,  8
                ,  6,  9, 11
                , 12, 13,  0
        };

        View[] view = new View[getChildCount()];
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                view[i] = getChildAt(i);
            }
        }

        int idx = 0;
        for (int id : order) {
            setChildLayout(view[idx], id);
            idx++;
        }


    }

    void layoutVertical(int left, int top, int right, int bottom) {
        /*if (topAdapter == null || bottomAdapter == null) {
            return;
        }*/
        Log.w("onLayout", "onLayout");
        ViewProperty viewProperty;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            viewProperty = (ViewProperty) child.getTag();

            if (child.getVisibility() == GONE
                    || viewProperty.viewType == ViewType.ANCHOR
                    || viewProperty.viewType == ViewType.STICKY
                    || viewProperty.sortOrder == -1
                ) {
                continue;
            }

            setChildLayout(child, viewProperty.sortOrder);
        }
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

        //child.setTag(idx);
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
    }

    private void moveChildTo(View child, int idx) {
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

    private int getChildWidth() {
        return (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - verticalSpace * (numOfColumn + 1)) / numOfColumn;
    }
    private int getChildHeight() {
        return (getMeasuredHeight() - getPaddingLeft() - getPaddingRight() - horizontalSpace * NUM_OF_ROWS + 1) / NUM_OF_ROWS;
    }

/*    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }
        invalidate();
        return super.onTouchEvent(event);
    }*/

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

    //private OnTouchListener onTouch = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            int x = (int) event.getX();
            int y = (int) event.getY();


            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    selX = x;
                    selY = y;

                    int cnt = getChildCount();
                    for (int i = 0; i < cnt; i++) {
                        View child = getChildAt(i);
                        if (selectView == child) {
                            break;
                        }

                        if (isPointInsideView(x, y, child)) {
                            child.setDrawingCacheEnabled(true);
                            Bitmap bitmap = Bitmap.createBitmap(child.getDrawingCache());
                            sticky.setImageBitmap(bitmap);

                            ViewProperty viewProperty = (ViewProperty) child.getTag();
                            viewProperty.viewType = ViewType.STICKY;
                            child.setTag(viewProperty);
                            selectView = child;
                            oldPosition = getPositionOfChild(x, y);

                            break;
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (selectView != null) {

                        int tx = x - selX;
                        int ty = y - selY;

                        if (tx == 0 || ty == 0) {
                            break;
                        }


                        int l = x + tx;
                        int t = x + ty;

                        /*int l = selectView.getLeft() + tx;
                        int t = selectView.getTop() + ty;
                        int r = selectView.getRight() + tx;
                        int b = selectView.getBottom() + ty;*/

                        selX = x;
                        selY = y;

                        mParams.x = l;
                        mParams.y = t;
                        sticky.setVisibility(VISIBLE);
                        windowManager.updateViewLayout(sticky, mParams);

                        //selectView.layout(l, t, r, b);
                        positionToChange = getPositionOfChild(sticky.getLeft() + tx / 2, sticky.getTop() + ty / 2);
                        //positionToChange = getPositionOfChild(selectView.getLeft() + tx / 2, selectView.getTop() + ty / 2);
                        setChildLayout(viewToChangePlace, positionToChange);
                        if (positionToChange < 0) {
                            positionToChange = 0;
                        }
                        if (positionToChange > 14) {
                            positionToChange = 14;
                        }
                        ViewProperty viewProperty = new ViewProperty();
                        viewProperty = (ViewProperty) selectView.getTag();
                        viewProperty.viewType = ViewType.ANCHOR;
                        viewProperty.sortOrder = positionToChange;
                        selectView.setTag(viewProperty);
                        selectView.setVisibility(GONE);

                        viewToChangePlace.setVisibility(VISIBLE);
                        if (oldPosition == positionToChange) {
                            break;
                        }
                        Log.w("start", String.format("from %02d to %02d", oldPosition, positionToChange));


                        View child;
                        if (oldPosition > positionToChange) {
                            String msg = "";
                            for (int i = 0; i < getChildCount(); i++) {
                                child = getChildAt(i);
                                viewProperty = (ViewProperty) child.getTag();
                                if (viewProperty.viewType == ViewType.ANCHOR
                                    || viewProperty.viewType == ViewType.STICKY) {
                                    continue;
                                }
                                int position = getPositionOfChild(child);
                                if (positionToChange <= position && position < oldPosition) {
                                    viewProperty.sortOrder = position + 1;
                                    child.setTag(viewProperty);
                                    setChildLayout(child, position + 1);
                                    msg += "(" + position + "->" + (position + 1) + ") ";
                                }
                            }
                            Log.w("up", String.format("%s", msg));

                        }
                        if (oldPosition < positionToChange) {
                            String msg = "";
                            for (int i = 0; i < getChildCount(); i++) {
                                child = getChildAt(i);
                                viewProperty = (ViewProperty) child.getTag();
                                if (viewProperty.viewType == ViewType.ANCHOR
                                        || viewProperty.viewType == ViewType.STICKY) {
                                    continue;
                                }
                                int position = getPositionOfChild(child);
                                if (position == 0) {
                                    continue;
                                }
                                if (oldPosition <= position && position <= positionToChange) {
                                    viewProperty.sortOrder = position + 1;
                                    child.setTag(viewProperty);
                                    setChildLayout(child, position - 1);
                                    msg += "(" + position + "->" + (position - 1) + ") ";
                                }
                            }
                            Log.w("down", String.format("%s", msg));

                        }
                        oldPosition = positionToChange;

                    }

                    break;

                case MotionEvent.ACTION_UP:
                    ViewProperty viewProperty = (ViewProperty) selectView.getTag();
                    viewProperty.viewType = ViewType.AREA_TOP;
                    selectView.setTag(viewProperty);
                    selectView.setVisibility(VISIBLE);
                    //setChildLayout(selectView, positionToChange);
                    //selectView = null;
                    viewToChangePlace.setVisibility(GONE);
                    /*View child = getChildAt(14);
                    setChildLayout(child, positionToChange);
                    child.setVisibility(VISIBLE);*/
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

    }

}
