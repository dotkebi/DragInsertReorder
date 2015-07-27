package com.example.dotkebi.myapp;

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

import java.util.ArrayList;
import java.util.List;

/**
 * @author dotkebi@gmail.com on 2015. 7. 9..
 */
public class DragInsertReorder extends ViewGroup implements View.OnTouchListener {
    private int NUM_OF_ROWS;
    private int NUM_OF_TOP;
    private int NUM_OF_BOTTOM;

    private WindowManager windowManager;
    private WindowManager.LayoutParams stickyParams;
    private ImageView sticky;
    private View anchor;

    DragInsertReorder.LayoutParams childParams;

    private List<View> topView;
    private List<View> bottomView;
    private View selectView = null;

    private List<Integer> topChildPositionFromAdapter;

    private int oldPosition = -1;
    private boolean fromBottom;

    private int quantityOfTopChild;
    private int quantityOfBottomChild;

    private int layoutAnchor;
    private int numOfColumn;
    private int verticalSpace;
    private int horizontalSpace;

    private DragInsertReorderBaseAdapter topAdapter;
    private DragInsertReorderBaseAdapter bottomAdapter;

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

        setOnTouchListener(this);

        numOfColumn = 1;
        NUM_OF_ROWS = 5;
        NUM_OF_TOP = 9;
        NUM_OF_BOTTOM = 6;
        topView = new ArrayList<>();
        bottomView = new ArrayList<>();
        topChildPositionFromAdapter = new ArrayList<>();
        quantityOfBottomChild = 0;
        quantityOfTopChild = 0;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DragInsertReorder);
        if (a != null) {
            layoutAnchor = a.getResourceId(R.styleable.DragInsertReorder_layoutAnchor, layoutAnchor);
            numOfColumn = a.getInt(R.styleable.DragInsertReorder_numOfColumn, numOfColumn);
            NUM_OF_TOP = a.getInt(R.styleable.DragInsertReorder_numOfTop, NUM_OF_TOP);
            NUM_OF_BOTTOM = a.getInt(R.styleable.DragInsertReorder_numOfBottom, NUM_OF_BOTTOM);
            NUM_OF_ROWS = a.getInt(R.styleable.DragInsertReorder_numOfRow, NUM_OF_ROWS);
            verticalSpace = a.getDimensionPixelOffset(R.styleable.DragInsertReorder_verticalSpace, verticalSpace);
            horizontalSpace = a.getDimensionPixelOffset(R.styleable.DragInsertReorder_horizontalSpace, horizontalSpace);
        }
        a.recycle();

    }

    public DragInsertReorder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setTopAdapter(DragInsertReorderBaseAdapter adapter) {
        this.topAdapter = adapter;
        topView.clear();
        quantityOfTopChild = 0;
    }

    public void setBottomAdapter(DragInsertReorderBaseAdapter adapter) {
        this.bottomAdapter = adapter;
        bottomView.clear();
        quantityOfBottomChild = 0;
    }

    public interface OnPositionChanged {
        void onPositionChanged(int oldPosition, int newPosition);
    }

    public interface OnObjectInserted {
        void onObejctInserted(int position, Object object);
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

        if (getChildCount() == 0 && topView.size() == 0 && bottomView.size() == 0) {

            if (topAdapter != null) {
                int size = (topAdapter.getCount() > NUM_OF_TOP) ? NUM_OF_TOP : topAdapter.getCount();
                for (int i = 0; i < size; i++) {
                    topChildPositionFromAdapter.add(i);
                    quantityOfTopChild = getChildFromAdapter(topAdapter, topView, quantityOfTopChild, i);
                    addView(topView.get(i), childParams);
                }

            }
            if (bottomAdapter != null) {
                int size = (bottomAdapter.getCount() > NUM_OF_BOTTOM) ? NUM_OF_BOTTOM : bottomAdapter.getCount();
                for (int i = 0; i < size; i++) {
                    quantityOfBottomChild = getChildFromAdapter(bottomAdapter, bottomView, quantityOfBottomChild, i);
                    addView(bottomView.get(i), childParams);
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

                anchor = View.inflate(context, layoutAnchor, null);
                anchor.setVisibility(GONE);
                addView(anchor, childParams);
            }
            invalidate();

        }

        layoutVertical(left, top, right, bottom);
    }

    private int getChildFromAdapter(Adapter adapter, List<View> viewHolder, int position, int idx) {
        //View child = (viewHolder.get(idx) == null) ? adapter.getView(position, null, this) : adapter.getView(position, viewHolder.get(idx), this);
        View child;
        if (viewHolder.size() <= idx) {
            child = adapter.getView(position, null, this);
            child.setTag(position);
            viewHolder.add(child);
        } else {
            child = adapter.getView(position, viewHolder.get(idx), this);
            child.setTag(position);
            viewHolder.set(idx, child);
        }
        return ++position;

    }

    private void layoutVertical(int left, int top, int right, int bottom) {
        for (int i = 0; i < topView.size(); i++) {
            View view = topView.get(i);
            getChildFromAdapter(topAdapter, topView, topChildPositionFromAdapter.get(i), i);
            if (oldPosition == -1) {
                view.setVisibility(VISIBLE);
            }
            setChildLayout(view, i);
        }

        for (int i = 0; i < bottomView.size(); i++) {
            View view = bottomView.get(i);
            getChildFromAdapter(bottomAdapter, bottomView, (int) view.getTag(), i);
            if (view.getVisibility() == GONE) {
                continue;
            }
            setChildLayout(view, i + NUM_OF_TOP);
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
                            : topView.get(oldPosition);

                    assert viewToChange != null;

                    if (fromBottom && newPosition < NUM_OF_TOP) { // from bottom
                        View child;
                        if (topView.size() >= NUM_OF_TOP) {
                            child = topView.get(NUM_OF_TOP - 1);
                            removeView(child);
                            topView.remove(NUM_OF_TOP - 1);
                            topChildPositionFromAdapter.remove(NUM_OF_TOP - 1);
                        }

                        int bottomPosition = (int) viewToChange.getTag();

                        topView = addChild(newPosition, viewToChange, topView);
                        topAdapter.onObejctInserted(newPosition, bottomAdapter.getItem(bottomPosition));
                        bottomView.remove(viewToChange);

                        int size = bottomAdapter.getCount();
                        if (size > NUM_OF_BOTTOM && quantityOfBottomChild < size) {
                            int position = bottomView.size();
                            quantityOfBottomChild = getChildFromAdapter(bottomAdapter, bottomView, quantityOfBottomChild, position);
                            addView(bottomView.get(position), childParams);
                        }

                        fromBottom = false;

                    } else if (oldPosition != newPosition) {

                        if (newPosition < oldPosition) {
                            childMovesToRight(oldPosition, newPosition, topView, topAdapter);
                        } else if (oldPosition < newPosition) {
                            childMoveToLeft(oldPosition, newPosition, topView, topAdapter);
                        }

                    }

                    topView.set(newPosition, viewToChange);
                    viewToChange.setVisibility(GONE);
                    setChildLayout(viewToChange, newPosition);
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
                break;
        }
        return true;
    }

    private void childMoveToLeft(int oldPosition, int positionToChange, List<View> viewHolder, DragInsertReorderBaseAdapter adapter) {
        for (int i = oldPosition; i < positionToChange; i++) {
            int nextKey = i + 1;
            swapChild(i, nextKey, viewHolder, adapter);
        }
    }

    private void childMovesToRight(int oldPosition, int positionToChange, List<View> viewHolder, DragInsertReorderBaseAdapter adapter) {
        for (int i = oldPosition; i > positionToChange; i--) {
            int previousKey = i - 1;
            swapChild(i, previousKey, viewHolder, adapter);
        }
    }

    private void swapChild(int left, int right, List<View> viewHolder, DragInsertReorderBaseAdapter adapter) {
        View leftView = (viewHolder.size() <= right) ? null : viewHolder.get(right);
        View rightView = (viewHolder.size() <= left) ? null : viewHolder.get(left);

        viewHolder.set(left, leftView);
        viewHolder.set(right, rightView);

        adapter.onPositionChanged(left, right);

        setChildLayout(viewHolder.get(left), left);
        setChildLayout(viewHolder.get(right), right);
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
        for (int i = 0; i < topView.size(); i++) {
            if (isPointInsideView(x, y, topView.get(i))) {
                return i;
            }
        }
        for (int i = 0; i < bottomView.size(); i++) {
            if (isPointInsideView(x, y, bottomView.get(i))) {
                return i + NUM_OF_TOP;
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
        for (View view : bottomView) {
            view.setVisibility((flag) ? VISIBLE : GONE);
        }
        invalidate();
    }

    public List<View> addChild(int position, View view, List<View> viewHolder) {
        List<Integer> childStack = new ArrayList<>();
        List<View> list = new ArrayList<>();
        int i = 0;
        for (View item : viewHolder) {
            if (i == position) {
                list.add(view);
                childStack.add(i);
                i++;
            }
            list.add(item);
            childStack.add(i);
            i++;
        }
        topChildPositionFromAdapter = childStack;
        return list;
    }

}
