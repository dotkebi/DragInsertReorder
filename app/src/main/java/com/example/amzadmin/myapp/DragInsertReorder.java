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
 * @author dotkebi@gmail.com on 2015. 7. 9..
 */
public class DragInsertReorder extends ViewGroup implements View.OnTouchListener {
    private static final int NUM_OF_ROWS = 5;
    private static final int NUM_OF_TOP = 9;
    private static final int NUM_OF_BOTTOM = 6;

    private OnPositionChanged onPositionChanged;

    private WindowManager windowManager;
    private WindowManager.LayoutParams stickyParams;
    private ImageView sticky;
    private View anchor;

    DragInsertReorder.LayoutParams childParams;

    private Map<Integer, View> topViewMap;
    private Map<Integer, View> bottomViewMap;

    private View selectView = null;
    private int oldPosition = -1;
    private boolean fromBottom;
    private int bottomPosition;

    private int quantityOfTopChild;
    private int quantityOfBottomChild;

    private int layoutAnchor;
    private int numOfColumn;
    private int verticalSpace;
    private int horizontalSpace;

    private Adapter topAdapter;
    private Adapter bottomAdapter;

    private Context context;

    public DragInsertReorder(Context context) {
        super(context);
    }

    public DragInsertReorder(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        this.context = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DragInsertReorder);
        if (a != null) {
            layoutAnchor = a.getResourceId(R.styleable.DragInsertReorder_layoutAnchor, layoutAnchor);
            numOfColumn = a.getInt(R.styleable.DragInsertReorder_numOfColumn, numOfColumn);
            verticalSpace = a.getDimensionPixelOffset(R.styleable.DragInsertReorder_verticalSpace, verticalSpace);
            horizontalSpace = a.getDimensionPixelOffset(R.styleable.DragInsertReorder_horizontalSpace, horizontalSpace);
        }

        if (numOfColumn <= 0) {
            numOfColumn = 1;
        }
        topViewMap = new HashMap<>();
        bottomViewMap = new HashMap<>();
        quantityOfBottomChild = 0;
        quantityOfTopChild = 0;

        setOnTouchListener(this);
        a.recycle();

    }

    public DragInsertReorder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setTopAdapter(DragInsertReorderBaseAdapter adapter) {
        this.topAdapter = adapter;
        onPositionChanged = adapter;
        topViewMap.clear();
        quantityOfTopChild = 0;
    }

    public void setBottomAdapter(DragInsertReorderBaseAdapter adapter) {
        this.bottomAdapter = adapter;
        bottomViewMap.clear();
        quantityOfBottomChild = 0;
    }

    public void setOnPositionChanged(OnPositionChanged onPositionChanged) {
        this.onPositionChanged = onPositionChanged;
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

        childParams = new DragInsertReorder.LayoutParams(getChildWidth(), getChildHeight());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (bottomAdapter == null || topAdapter == null) {
            return;
        }

        if (getChildCount() == 0 && topViewMap.size() == 0 && bottomViewMap.size() == 0) {

            if (topAdapter != null) {
                int size = (topAdapter.getCount() > NUM_OF_TOP) ? NUM_OF_TOP : topAdapter.getCount();
                for (int i = 0; i < size; i++) {
                    quantityOfTopChild = getChildFromAdapter(topAdapter, topViewMap, quantityOfTopChild, i);
                }

            }
            if (bottomAdapter != null) {
                int size = (bottomAdapter.getCount() > NUM_OF_BOTTOM) ? NUM_OF_BOTTOM : bottomAdapter.getCount();
                for (int i = 0; i < size; i++) {
                    quantityOfBottomChild = getChildFromAdapter(bottomAdapter, bottomViewMap, quantityOfBottomChild, i);
                    //bottomViewMap.get(i).setVisibility(GONE);
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

                DragInsertReorder.LayoutParams param = new DragInsertReorder.LayoutParams(getChildWidth(), getChildHeight());
                anchor = View.inflate(context, layoutAnchor, null);
                anchor.setVisibility(GONE);
                addView(anchor, param);
            }
            invalidate();

        }

        layoutVertical(left, top, right, bottom);
    }

    private int getChildFromAdapter(Adapter adapter, Map<Integer, View> viewMap, int position, int idx) {
        View child = viewMap.get(idx);
        child = adapter.getView(position, child, this);
        child.setTag(position);
        viewMap.put(idx, child);
        addView(viewMap.get(idx), childParams);
        return ++position;

    }


    private void layoutVertical(int left, int top, int right, int bottom) {
        for (int key : topViewMap.keySet()) {
            View view = topViewMap.get(key);
            if (oldPosition == -1) {
                view.setVisibility(VISIBLE);
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
        return new DragInsertReorder.LayoutParams(getContext(), attrs);
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

                        oldPosition = getPositionOfChild(x, y, -1);
                        oldPosition = (oldPosition > (NUM_OF_ROWS * numOfColumn - 1)) ? ((NUM_OF_ROWS * numOfColumn) - 1): oldPosition;
                        fromBottom = (oldPosition >= NUM_OF_TOP);
                        bottomPosition = (fromBottom) ? oldPosition - NUM_OF_TOP : -1;
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

                    int newPosition = getPositionOfChild(x, y, oldPosition);

                    if (newPosition == oldPosition || newPosition < 0) {
                        break;
                    }
                    View viewToChange;
                    viewToChange = (fromBottom)
                            ? selectView
                            : topViewMap.get(oldPosition);

                    if (viewToChange != null) {
                        viewToChange.setVisibility(GONE);
                    }


                    if (fromBottom && newPosition < NUM_OF_TOP) { // from bottom
                        View child;
                        if (topViewMap.size() >= NUM_OF_TOP) {
                            child = topViewMap.get(NUM_OF_TOP - 1);
                            removeView(child);
                            topViewMap.remove(NUM_OF_TOP - 1);
                        }
                        bottomViewMap.remove(bottomPosition);
                        bottomViewMap = reordering(bottomViewMap);

                        int size = bottomAdapter.getCount();
                        if (size > NUM_OF_BOTTOM && quantityOfBottomChild < size) {
                            quantityOfBottomChild = getChildFromAdapter(bottomAdapter, bottomViewMap, quantityOfBottomChild, bottomViewMap.size());
                        }

                        childMovesToRight(topViewMap.size(), newPosition, topViewMap);
                        fromBottom = false;

                    } else if (oldPosition != newPosition) {

                        if (newPosition < oldPosition) {
                            childMovesToRight(oldPosition, newPosition, topViewMap);
                        } else if (oldPosition < newPosition) {
                            childMoveToLeft(oldPosition, newPosition, topViewMap);
                        }

                    }

                    if (viewToChange != null) {
                        topViewMap.put(newPosition, viewToChange);
                        setChildLayout(viewToChange, newPosition);
                    }
                    oldPosition = newPosition;

                    setAnchor(newPosition);

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
                topViewMap = reordering(topViewMap);

                break;
        }
        return true;
    }

    private void childMoveToLeft(int oldPosition, int positionToChange, Map<Integer, View> viewMap) {
        for (int i = oldPosition; i < positionToChange; i++) {
            int nextKey = i + 1;
            swapChild(i, nextKey, viewMap);
        }
    }

    private void childMovesToRight(int oldPosition, int positionToChange, Map<Integer, View> viewMap) {
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
        setChildLayout(viewMap.get(right), right);
        onPositionChanged.onPositionChanged(left, right);
    }

    private Map<Integer, View> reordering(Map<Integer, View> sourceMap) {
        return reordering(sourceMap, 0, sourceMap.size());
    }

    private Map<Integer, View> reordering(Map<Integer, View> sourceMap, int start, int end) {
        int previousKey = start;
        Map<Integer, View> reorderedMap = new HashMap<>();
        View child;
        for (int key : sourceMap.keySet()) {
            if (start <= previousKey && previousKey <= end) {
                child = sourceMap.get(key);
                if (child == null) {
                    continue;
                }
                reorderedMap.put(previousKey, child);
                previousKey++;
            }
        }
        return reorderedMap;
    }



    private boolean isPointInsideView(int x, int y, View view){
        if (view == null) {
            return false;
        }
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

    public void toggleBottom(boolean flag) {
        for (int key : bottomViewMap.keySet()) {
            bottomViewMap.get(key).setVisibility((flag) ? VISIBLE : GONE);
        }
        invalidate();
    }

    public interface OnPositionChanged {
        void onPositionChanged(int oldPosition, int newPosition);
    }

    public interface OnObjectInserted {
        void onObejctInserted(Object object);
    }

    public interface OnReordering {
        void onReordering();
    }


}
