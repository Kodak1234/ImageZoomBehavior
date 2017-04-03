package view.zoom;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;

/*
* Behavior to use for views that wants to dragged or zoomed.
* But it is made specifically for views that display image.
* */
@TargetApi(12)
public class ImageZoomBehavior extends CoordinatorLayout.Behavior<View> implements ScaleGestureDetector.OnScaleGestureListener
        , ViewTreeObserver.OnPreDrawListener {

    private static final String TAG = "ImageZoomBehavior";
    //views
    private CoordinatorLayout parent;
    private View child;
    //member variables
    private ViewDragHelper dragHelper;
    private ScaleGestureDetector scaleGestureDetector;

    //int
    private float scaleFactor = 1;
    private float MAX_SCALE_FACTOR = 4.0f, MIN_SCALE_FACTOR = 0.5f;
    private float minZoom = MIN_SCALE_FACTOR, maxZoom = MAX_SCALE_FACTOR;
    private int dragRange, finalX, finalY;
    private Rect frame = new Rect();
    private boolean isScaling;

    public ImageZoomBehavior(Context context) {
        init(context);
    }

    public ImageZoomBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        dragRange = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        scaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    private void initViewDragHelper(CoordinatorLayout parent, View child) {
        child.getViewTreeObserver().addOnPreDrawListener(this);
        this.parent = parent;
        this.child = child;
        dragHelper = ViewDragHelper.create(parent, 1f, new DragCallback());
    }

    public float getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(float minZoom) {
        this.minZoom = minZoom > MIN_SCALE_FACTOR && minZoom < maxZoom ? minZoom : MIN_SCALE_FACTOR;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom > minZoom ? maxZoom : MAX_SCALE_FACTOR;
    }

    private boolean canDragHorizontal() {
        return (scaleFactor * child.getWidth()) > parent.getWidth();
    }

    private boolean canDragVertical() {
        return (scaleFactor * child.getHeight()) > parent.getHeight();
    }

    private boolean isScaling() {
        return isScaling;
    }

    private boolean shouldDragHorizontal() {
        return canDragHorizontal() && !isScaling();
    }

    private boolean shouldDragHorizontal(boolean movingLeft) {
        return (frame.width() == parent.getWidth() - parent.getPaddingRight())
                || frame.left == -1 && movingLeft
                || frame.left > 0 && !movingLeft;
    }

    private boolean shouldDragVertical(boolean movingUp) {
        return (frame.height() == parent.getHeight() - parent.getPaddingBottom())
                || frame.top == -1 && movingUp
                || frame.top > 0 && !movingUp;
    }

    private boolean shouldDragVertical() {
        return canDragVertical() && !isScaling();
    }

    //updates the frame use to track bound of the view
    public void updateFrame() {
        child.getLocalVisibleRect(frame);
    }

    //scales the view
    public void scale(float zoom) {
        zoom = zoom < minZoom ? minZoom : zoom > maxZoom ? maxZoom : zoom;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            child.animate().scaleX(zoom).scaleY(zoom);
        } else {
            ViewCompat.setScaleX(child, zoom);
            ViewCompat.setScaleY(child, zoom);
        }

        scaleFactor = zoom;
    }

    //move the view to its original position
    public void toCenter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            child.animate().x(finalX).y(finalY);
        } else {
            ViewCompat.setX(child, finalX);
            ViewCompat.setY(child, finalY);
        }
    }

    @Override
    public boolean onPreDraw() {
        child.getViewTreeObserver().removeOnPreDrawListener(this);
        finalX = child.getLeft();
        finalY = child.getTop();
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        scaleFactor *= scaleGestureDetector.getScaleFactor();
        ViewCompat.setScaleX(child, scaleFactor);
        ViewCompat.setScaleY(child, scaleFactor);
        updateFrame();
        return true;

    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        isScaling = true;
        toCenter();
        updateFrame();
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        isScaling = false;
        scale(scaleFactor);
        updateFrame();
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
        scaleGestureDetector.onTouchEvent(ev);
        return dragHelper.shouldInterceptTouchEvent(ev) || isScaling();
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, View child, MotionEvent ev) {
        scaleGestureDetector.onTouchEvent(ev);
        dragHelper.processTouchEvent(ev);
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        initViewDragHelper(parent, child);
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    private class DragCallback extends ViewDragHelper.Callback {

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            updateFrame();
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return ImageZoomBehavior.this.child.getId() == child.getId();
        }

        /*
       * Bound the image so only half of the image height can go off screen
       * */
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {

            if (shouldDragHorizontal() && shouldDragHorizontal(dx < 0))
                return left;

            return child.getLeft();

        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (shouldDragVertical() && shouldDragVertical(dy < 0))
                return top;

            return child.getTop();
        }


        @Override
        public void onViewReleased(View child, float xvel, float yvel) {
            int dx = frame.left == -1 ? (parent.getWidth() - parent.getPaddingRight() - frame.right)
                    : (parent.getWidth() - parent.getPaddingRight()) - (frame.right - frame.left);

            int dy = frame.top == -1 ? (parent.getHeight() - parent.getPaddingBottom() - frame.bottom)
                    : (parent.getHeight() - parent.getPaddingBottom()) - (frame.bottom - frame.top);

            if (frame.left == -1) {
                dx = child.getLeft() - dx;
            } else {
                dx = child.getLeft() + dx;
            }

            if (frame.top == -1) {
                dy = child.getTop() - dy;
            } else {
                dy = child.getTop() + dy;
            }

            dx = shouldDragHorizontal() ? dx : child.getLeft();
            dy = shouldDragVertical() ? dy : child.getTop();

            if (dragHelper.settleCapturedViewAt(dx, dy))
                do {
                    ViewCompat.postInvalidateOnAnimation(child);
                } while (dragHelper.continueSettling(false));
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return dragRange;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return dragRange;
        }

    }
}
