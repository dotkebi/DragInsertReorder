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

import java.util.HashMap;
import java.util.Map;

/**
 * @author dotkebi on 2015. 7. 9..
 */
public class DragReOrderInsert extends ViewGroup implements View.OnTouchListener {
    private static final int NUM_OF_ROWS = 5;
    private static final int NUM_OF_TOP = 9;
    private static final int NUM_OF_BOTTOM = 6;

    private WindowManager windowManager;
    private WindowManager.LayoutParams stickyParams;
    private ImageView sticky;
    private View anchor;

    private Map<Integer, View> topViewMap;
    private Map<Integer, View> bottomViewMap;

    private View selectView = null;
    private int oldPosition = -1;
    private boolean fromBottom;
    private int bottomPosition;

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
        topViewMap = new HashMap<>();
        bottomViewMap = new HashMap<>();
        setOnTouchListener(this);
        a.recycle();

    }

    public DragReOrderInsert(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setTopAdapter(Adapter adapter) {
        this.topAdapter = adapter;
        topViewMap.clear();
    }

    public void setBottomAdapter(Adapter adapter) {
        this.bottomAdapter = adapter;
        bottomViewMap.clear();
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

        if (getChildCount() == 0 && topViewMap.size() == 0) {
            int width = getChildWidth();
            int height = getChildHeight();

            DragReOrderInsert.LayoutParams param = new DragReOrderInsert.LayoutParams(width, height);

            if (topAdapter != null) {
                int idx = 0;
                int size = (topAdapter.getCount() > NUM_OF_TOP) ? NUM_OF_TOP : topAdapter.getCount();
                View child;
                for (int i = 0; i < size; i++) {
                    child = topAdapter.getView(i, convertView, this);
                    topViewMap.put(idx, child);
                    addView(topViewMap.get(idx), param);
                    idx++;
                }
            }
            if (bottomAdapter != null) {
                int size = (bottomAdapter.getCount() > NUM_OF_BOTTOM) ? NUM_OF_BOTTOM : bottomAdapter.getCount();
                int idx = 0;
                View child;
                for (int i = 0; i < size; i++) {
                    child = bottomAdapter.getView(i, convertView, this);
                    bottomViewMap.put(idx, child);
                    addView(bottomViewMap.get(idx), param);
                    idx++;
                }
            }

            if (windowManager == null) {
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
                windowManager.addView(sticky, stickyParams);

                anchor = View.inflate(context, R.layout.position, null);
                anchor.setVisibility(GONE);
                addView(anchor, param);
            }

        }

        layoutVertical(left, top, right, bottom);

    }

    void layoutVertical(int left, int top, int right, int bottom) {
        for (int key : topViewMap.keySet()) {
            View view = topViewMap.get(key);
            if (oldPosition == -1) {
                view.setVisibility(VISIBLE);
            } else {
                continue;
            }
            setChildLayout(view, key);
        }

        for (int key : bottomViewMap.keySet()) {
            View view = bottomViewMap.get(key);
            if (view.getVisibility() == GONE) {
                continue;
            }
            setChildLayout(view, key + 9);
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

        //int asldfkafj = getPositionOfChild(x, y);

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

                        oldPosition = getPositionOfChild(x, y, -1);
                        oldPosition = (oldPosition > (NUM_OF_ROWS * numOfColumn - 1)) ? ((NUM_OF_ROWS * numOfColumn) - 1): oldPosition;
                        fromBottom = (oldPosition >= NUM_OF_TOP);
                        bottomPosition = (fromBottom) ? oldPosition - NUM_OF_TOP: -1;
                        setAnchor(oldPosition);
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

                    int positionToChange = getPositionOfChild(x, y, oldPosition);

                    if (positionToChange == oldPosition) {
                        break;
                    }
                    View viewToChange;
                    viewToChange = (fromBottom)
                            ? selectView
                            : topViewMap.get(oldPosition);

                    if (viewToChange != null) {
                        viewToChange.setVisibility(GONE);
                    }

                    Log.w("position", String.format("old %d      ptc %d", oldPosition, positionToChange));

                    if (fromBottom) { // from bottom

                        bottomViewMap.remove(bottomPosition);
                        bottomViewMap = reOrdering(bottomViewMap);
                        childMovesRight(positionToChange, topViewMap.size() - 1, topViewMap);
                        //childMoveLeft(bottomPosition, bottomViewMap.size(), bottomViewMap);
                        fromBottom = false;

                    } else if (oldPosition != positionToChange) {

                        if (positionToChange < oldPosition) {
                            childMovesRight(oldPosition, positionToChange, topViewMap);
                        } else if (oldPosition < positionToChange) {
                            childMoveLeft(oldPosition, positionToChange, topViewMap);
                        }

                    }


                    if (viewToChange != null) {
                        topViewMap.put(positionToChange, viewToChange);
                        setChildLayout(viewToChange, positionToChange);
                    }
                    oldPosition = positionToChange;

                    setAnchor(positionToChange);

                }
                break;

            case MotionEvent.ACTION_UP:
                if (selectView != null) {
                    selectView.setVisibility(VISIBLE);
                }
                fromBottom = false;
                anchor.setVisibility(GONE);

                sticky.setVisibility(GONE);
                windowManager.updateViewLayout(sticky, stickyParams);

                oldPosition = -1;
                topViewMap = reOrdering(topViewMap);

                break;
        }
        return true;
    }

    private Map<Integer, View> reOrdering(Map<Integer, View> sourceMap) {
        int previousKey = 0;
        Map<Integer, View> reorderedMap = new HashMap<>();

        for (int key : sourceMap.keySet()) {
            reorderedMap.put(previousKey, sourceMap.get(key));
            previousKey++;
        }
        return reorderedMap;
    }

    private void childMoveLeft(int oldPosition, int positionToChange, Map<Integer, View> viewMap) {
        for (int i = oldPosition; i < positionToChange; i++) {
            int nextKey = i + 1;
            swapChild(i, nextKey, viewMap);
        }
    }

    private void childMovesRight(int oldPosition, int positionToChange, Map<Integer, View> viewMap) {
        for (int i = oldPosition; i > positionToChange; i--) {
            int previousKey = i - 1;
            swapChild(i, previousKey, viewMap);
        }
    }

    private void swapChild(int left, int right, Map<Integer, View> viewMap) {
        if (!viewMap.containsKey(left) && !viewMap.containsKey(right)) {
            return;
        }
        View leftView = viewMap.get(right);
        View rightView = viewMap.get(left);

        viewMap.put(left, leftView);
        viewMap.put(right, rightView);

        setChildLayout(viewMap.get(left), left);
    }


    private boolean isPointInsideView(int x, int y, View view){
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);

        return rect.contains(x, y);
    }

    private int getPositionOfChild(int x, int y, int previous) {
        for (int key : topViewMap.keySet()) {
            if (isPointInsideView(x, y, topViewMap.get(key))) {
                return key;
            }
        }
        for (int key : bottomViewMap.keySet()) {
            if (isPointInsideView(x, y, bottomViewMap.get(key))) {
                return key + NUM_OF_TOP;
            }
        }
        return previous;
    }
    private int getPositionOfChild(int x, int y) {

        View root = (View) getParent();
        int[] viewCoords = new int[2];
        root.getLocationOnScreen(viewCoords);
        Log.w("position", String.format("x %03d      y %03d              ", x - viewCoords[0], y - viewCoords[1]));


        int width = getChildWidth();
        int height = getChildHeight();

        int column = x / width;
        int row = y / height;

        return ((row * numOfColumn) + column);
    }

    private int getPositionOfChild(View child) {
        return getPositionOfChild((int) child.getX(), (int) child.getY(), -1);
    }

    private void setAnchor(int idx) {
        anchor.setVisibility(VISIBLE);
        setChildLayout(anchor, idx);
    }

    private void setChildLayout(View child, int idx) {
        if (child == null) {
            return;
        }

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        //Rect rect = new Rect();
        //getParent().getGlobalVisibleRect(rect);





        int childWidth = getChildWidth();
        int childHeight = getChildHeight();

        int row = idx / numOfColumn;
        int column = idx % numOfColumn;

        int childTop = paddingTop + (childHeight + horizontalSpace) * row;
        int childLeft = paddingLeft + (childWidth + verticalSpace) * column;

        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
//        child.startAnimation(null);
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

    public void toggleBottom(boolean flag) {
        for (int key : bottomViewMap.keySet()) {
            bottomViewMap.get(key).setVisibility((flag) ? GONE : VISIBLE);
        }
    }


}
