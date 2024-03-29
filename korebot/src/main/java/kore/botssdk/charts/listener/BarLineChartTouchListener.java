package kore.botssdk.charts.listener;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.AnimationUtils;

import kore.botssdk.charts.charts.BarLineChartBase;
import kore.botssdk.charts.charts.HorizontalBarChart;
import kore.botssdk.charts.data.BarLineScatterCandleBubbleData;
import kore.botssdk.charts.data.Entry;
import kore.botssdk.charts.highlight.Highlight;
import kore.botssdk.charts.interfaces.datasets.IBarLineScatterCandleBubbleDataSet;
import kore.botssdk.charts.interfaces.datasets.IDataSet;
import kore.botssdk.charts.utils.MPPointF;
import kore.botssdk.charts.utils.Utils;
import kore.botssdk.charts.utils.ViewPortHandler;
import kore.botssdk.utils.LogUtils;

public class BarLineChartTouchListener extends ChartTouchListener<BarLineChartBase<? extends BarLineScatterCandleBubbleData<? extends IBarLineScatterCandleBubbleDataSet<? extends Entry>>>> {
    private Matrix mMatrix = new Matrix();
    private final Matrix mSavedMatrix = new Matrix();
    private final MPPointF mTouchStartPoint = MPPointF.getInstance(0.0F, 0.0F);
    private final MPPointF mTouchPointCenter = MPPointF.getInstance(0.0F, 0.0F);
    private float mSavedXDist = 1.0F;
    private float mSavedYDist = 1.0F;
    private float mSavedDist = 1.0F;
    private IDataSet mClosestDataSetToTouch;
    private VelocityTracker mVelocityTracker;
    private long mDecelerationLastTime = 0L;
    private final MPPointF mDecelerationCurrentPoint = MPPointF.getInstance(0.0F, 0.0F);
    private final MPPointF mDecelerationVelocity = MPPointF.getInstance(0.0F, 0.0F);
    private float mDragTriggerDist;
    private final float mMinScalePointerDistance;

    public BarLineChartTouchListener(BarLineChartBase<? extends BarLineScatterCandleBubbleData<? extends IBarLineScatterCandleBubbleDataSet<? extends Entry>>> chart, Matrix touchMatrix, float dragTriggerDistance) {
        super(chart);
        this.mMatrix = touchMatrix;
        this.mDragTriggerDist = Utils.convertDpToPixel(dragTriggerDistance);
        this.mMinScalePointerDistance = Utils.convertDpToPixel(3.5F);
    }

    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouch(View v, MotionEvent event) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }

        this.mVelocityTracker.addMovement(event);
        if (event.getActionMasked() == 3 && this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }

        if (this.mTouchMode == 0) {
            this.mGestureDetector.onTouchEvent(event);
        }

        if (!((BarLineChartBase)this.mChart).isDragEnabled() && !((BarLineChartBase)this.mChart).isScaleXEnabled() && !((BarLineChartBase)this.mChart).isScaleYEnabled()) {
            return true;
        } else {
            float distanceY;
            switch(event.getAction() & 255) {
                case 0:
                    this.startAction(event);
                    this.stopDeceleration();
                    this.saveTouchStart(event);
                    break;
                case 1:
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    int pointerId = event.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, (float)Utils.getMaximumFlingVelocity());
                    distanceY = velocityTracker.getYVelocity(pointerId);
                    float velocityX = velocityTracker.getXVelocity(pointerId);
                    if ((Math.abs(velocityX) > (float)Utils.getMinimumFlingVelocity() || Math.abs(distanceY) > (float)Utils.getMinimumFlingVelocity()) && this.mTouchMode == 1 && ((BarLineChartBase)this.mChart).isDragDecelerationEnabled()) {
                        this.stopDeceleration();
                        this.mDecelerationLastTime = AnimationUtils.currentAnimationTimeMillis();
                        this.mDecelerationCurrentPoint.x = event.getX();
                        this.mDecelerationCurrentPoint.y = event.getY();
                        this.mDecelerationVelocity.x = velocityX;
                        this.mDecelerationVelocity.y = distanceY;
                        Utils.postInvalidateOnAnimation(this.mChart);
                    }

                    if (this.mTouchMode == 2 || this.mTouchMode == 3 || this.mTouchMode == 4 || this.mTouchMode == 5) {
                        ((BarLineChartBase)this.mChart).calculateOffsets();
                        ((BarLineChartBase)this.mChart).postInvalidate();
                    }

                    this.mTouchMode = 0;
                    ((BarLineChartBase)this.mChart).enableScroll();
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.recycle();
                        this.mVelocityTracker = null;
                    }

                    this.endAction(event);
                    break;
                case 2:
                    float distanceX;
                    if (this.mTouchMode == 1) {
                        ((BarLineChartBase)this.mChart).disableScroll();
                        float x = ((BarLineChartBase)this.mChart).isDragXEnabled() ? event.getX() - this.mTouchStartPoint.x : 0.0F;
                        distanceX = ((BarLineChartBase)this.mChart).isDragYEnabled() ? event.getY() - this.mTouchStartPoint.y : 0.0F;
                        this.performDrag(event, x, distanceX);
                    } else if (this.mTouchMode != 2 && this.mTouchMode != 3 && this.mTouchMode != 4) {
                        if (this.mTouchMode == 0 && Math.abs(distance(event.getX(), this.mTouchStartPoint.x, event.getY(), this.mTouchStartPoint.y)) > this.mDragTriggerDist && ((BarLineChartBase)this.mChart).isDragEnabled()) {
                            boolean shouldPan = !((BarLineChartBase)this.mChart).isFullyZoomedOut() || !((BarLineChartBase)this.mChart).hasNoDragOffset();
                            if (shouldPan) {
                                distanceX = Math.abs(event.getX() - this.mTouchStartPoint.x);
                                distanceY = Math.abs(event.getY() - this.mTouchStartPoint.y);
                                if ((((BarLineChartBase)this.mChart).isDragXEnabled() || distanceY >= distanceX) && (((BarLineChartBase)this.mChart).isDragYEnabled() || distanceY <= distanceX)) {
                                    this.mLastGesture = ChartGesture.DRAG;
                                    this.mTouchMode = 1;
                                }
                            } else if (((BarLineChartBase)this.mChart).isHighlightPerDragEnabled()) {
                                this.mLastGesture = ChartGesture.DRAG;
                                if (((BarLineChartBase)this.mChart).isHighlightPerDragEnabled()) {
                                    this.performHighlightDrag(event);
                                }
                            }
                        }
                    } else {
                        ((BarLineChartBase)this.mChart).disableScroll();
                        if (((BarLineChartBase)this.mChart).isScaleXEnabled() || ((BarLineChartBase)this.mChart).isScaleYEnabled()) {
                            this.performZoom(event);
                        }
                    }
                    break;
                case 3:
                    this.mTouchMode = 0;
                    this.endAction(event);
                case 4:
                default:
                    break;
                case 5:
                    if (event.getPointerCount() >= 2) {
                        ((BarLineChartBase)this.mChart).disableScroll();
                        this.saveTouchStart(event);
                        this.mSavedXDist = getXDist(event);
                        this.mSavedYDist = getYDist(event);
                        this.mSavedDist = spacing(event);
                        if (this.mSavedDist > 10.0F) {
                            if (((BarLineChartBase)this.mChart).isPinchZoomEnabled()) {
                                this.mTouchMode = 4;
                            } else if (((BarLineChartBase)this.mChart).isScaleXEnabled() != ((BarLineChartBase)this.mChart).isScaleYEnabled()) {
                                this.mTouchMode = ((BarLineChartBase)this.mChart).isScaleXEnabled() ? 2 : 3;
                            } else {
                                this.mTouchMode = this.mSavedXDist > this.mSavedYDist ? 2 : 3;
                            }
                        }

                        midPoint(this.mTouchPointCenter, event);
                    }
                    break;
                case 6:
                    Utils.velocityTrackerPointerUpCleanUpIfNecessary(event, this.mVelocityTracker);
                    this.mTouchMode = 5;
            }

            this.mMatrix = ((BarLineChartBase)this.mChart).getViewPortHandler().refresh(this.mMatrix, this.mChart, true);
            return true;
        }
    }

    private void saveTouchStart(MotionEvent event) {
        this.mSavedMatrix.set(this.mMatrix);
        this.mTouchStartPoint.x = event.getX();
        this.mTouchStartPoint.y = event.getY();
        this.mClosestDataSetToTouch = ((BarLineChartBase)this.mChart).getDataSetByTouchPoint(event.getX(), event.getY());
    }

    private void performDrag(MotionEvent event, float distanceX, float distanceY) {
        this.mLastGesture = ChartGesture.DRAG;
        this.mMatrix.set(this.mSavedMatrix);
        OnChartGestureListener l = ((BarLineChartBase)this.mChart).getOnChartGestureListener();
        if (this.inverted()) {
            if (this.mChart instanceof HorizontalBarChart) {
                distanceX = -distanceX;
            } else {
                distanceY = -distanceY;
            }
        }

        this.mMatrix.postTranslate(distanceX, distanceY);
        if (l != null) {
            l.onChartTranslate(event, distanceX, distanceY);
        }

    }

    private void performZoom(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            OnChartGestureListener l = ((BarLineChartBase)this.mChart).getOnChartGestureListener();
            float totalDist = spacing(event);
            if (totalDist > this.mMinScalePointerDistance) {
                MPPointF t = this.getTrans(this.mTouchPointCenter.x, this.mTouchPointCenter.y);
                ViewPortHandler h = ((BarLineChartBase)this.mChart).getViewPortHandler();
                float yDist;
                boolean isZoomingOut;
                boolean canZoomMoreY;
                if (this.mTouchMode == 4) {
                    this.mLastGesture = ChartGesture.PINCH_ZOOM;
                    yDist = totalDist / this.mSavedDist;
                    isZoomingOut = yDist < 1.0F;
                    isZoomingOut = isZoomingOut ? h.canZoomOutMoreX() : h.canZoomInMoreX();
                    canZoomMoreY = isZoomingOut ? h.canZoomOutMoreY() : h.canZoomInMoreY();
                    float scaleX = ((BarLineChartBase)this.mChart).isScaleXEnabled() ? yDist : 1.0F;
                    float scaleY = ((BarLineChartBase)this.mChart).isScaleYEnabled() ? yDist : 1.0F;
                    if (canZoomMoreY || isZoomingOut) {
                        this.mMatrix.set(this.mSavedMatrix);
                        this.mMatrix.postScale(scaleX, scaleY, t.x, t.y);
                        if (l != null) {
                            l.onChartScale(event, scaleX, scaleY);
                        }
                    }
                } else {
                    float scaleY;
                    if (this.mTouchMode == 2 && ((BarLineChartBase)this.mChart).isScaleXEnabled()) {
                        this.mLastGesture = ChartGesture.X_ZOOM;
                        yDist = getXDist(event);
                        scaleY = yDist / this.mSavedXDist;
                        isZoomingOut = scaleY < 1.0F;
                        canZoomMoreY = isZoomingOut ? h.canZoomOutMoreX() : h.canZoomInMoreX();
                        if (canZoomMoreY) {
                            this.mMatrix.set(this.mSavedMatrix);
                            this.mMatrix.postScale(scaleY, 1.0F, t.x, t.y);
                            if (l != null) {
                                l.onChartScale(event, scaleY, 1.0F);
                            }
                        }
                    } else if (this.mTouchMode == 3 && ((BarLineChartBase)this.mChart).isScaleYEnabled()) {
                        this.mLastGesture = ChartGesture.Y_ZOOM;
                        yDist = getYDist(event);
                        scaleY = yDist / this.mSavedYDist;
                        isZoomingOut = scaleY < 1.0F;
                        canZoomMoreY = isZoomingOut ? h.canZoomOutMoreY() : h.canZoomInMoreY();
                        if (canZoomMoreY) {
                            this.mMatrix.set(this.mSavedMatrix);
                            this.mMatrix.postScale(1.0F, scaleY, t.x, t.y);
                            if (l != null) {
                                l.onChartScale(event, 1.0F, scaleY);
                            }
                        }
                    }
                }

                MPPointF.recycleInstance(t);
            }
        }

    }

    private void performHighlightDrag(MotionEvent e) {
        Highlight h = ((BarLineChartBase)this.mChart).getHighlightByTouchPoint(e.getX(), e.getY());
        if (h != null && !h.equalTo(this.mLastHighlighted)) {
            this.mLastHighlighted = h;
            ((BarLineChartBase)this.mChart).highlightValue(h, true);
        }

    }

    private static void midPoint(MPPointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.x = x / 2.0F;
        point.y = y / 2.0F;
    }

    private static float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt((double)(x * x + y * y));
    }

    private static float getXDist(MotionEvent e) {
        float x = Math.abs(e.getX(0) - e.getX(1));
        return x;
    }

    private static float getYDist(MotionEvent e) {
        float y = Math.abs(e.getY(0) - e.getY(1));
        return y;
    }

    public MPPointF getTrans(float x, float y) {
        ViewPortHandler vph = ((BarLineChartBase)this.mChart).getViewPortHandler();
        float xTrans = x - vph.offsetLeft();
        float yTrans = 0.0F;
        if (this.inverted()) {
            yTrans = -(y - vph.offsetTop());
        } else {
            yTrans = -((float)((BarLineChartBase)this.mChart).getMeasuredHeight() - y - vph.offsetBottom());
        }

        return MPPointF.getInstance(xTrans, yTrans);
    }

    private boolean inverted() {
        return this.mClosestDataSetToTouch == null && ((BarLineChartBase)this.mChart).isAnyAxisInverted() || this.mClosestDataSetToTouch != null && ((BarLineChartBase)this.mChart).isInverted(this.mClosestDataSetToTouch.getAxisDependency());
    }

    public Matrix getMatrix() {
        return this.mMatrix;
    }

    public void setDragTriggerDist(float dragTriggerDistance) {
        this.mDragTriggerDist = Utils.convertDpToPixel(dragTriggerDistance);
    }

    public boolean onDoubleTap(MotionEvent e) {
        this.mLastGesture = ChartGesture.DOUBLE_TAP;
        OnChartGestureListener l = ((BarLineChartBase)this.mChart).getOnChartGestureListener();
        if (l != null) {
            l.onChartDoubleTapped(e);
        }

        if (((BarLineChartBase)this.mChart).isDoubleTapToZoomEnabled() && ((BarLineScatterCandleBubbleData)((BarLineChartBase)this.mChart).getData()).getEntryCount() > 0) {
            MPPointF trans = this.getTrans(e.getX(), e.getY());
            ((BarLineChartBase)this.mChart).zoom(((BarLineChartBase)this.mChart).isScaleXEnabled() ? 1.4F : 1.0F, ((BarLineChartBase)this.mChart).isScaleYEnabled() ? 1.4F : 1.0F, trans.x, trans.y);
            if (((BarLineChartBase)this.mChart).isLogEnabled()) {
                LogUtils.i("BarlineChartTouch", "Double-Tap, Zooming In, x: " + trans.x + ", y: " + trans.y);
            }

            MPPointF.recycleInstance(trans);
        }

        return super.onDoubleTap(e);
    }

    public void onLongPress(MotionEvent e) {
        this.mLastGesture = ChartGesture.LONG_PRESS;
        OnChartGestureListener l = ((BarLineChartBase)this.mChart).getOnChartGestureListener();
        if (l != null) {
            l.onChartLongPressed(e);
        }

    }

    public boolean onSingleTapUp(MotionEvent e) {
        this.mLastGesture = ChartGesture.SINGLE_TAP;
        OnChartGestureListener l = ((BarLineChartBase)this.mChart).getOnChartGestureListener();
        if (l != null) {
            l.onChartSingleTapped(e);
        }

        if (!((BarLineChartBase)this.mChart).isHighlightPerTapEnabled()) {
            return false;
        } else {
            Highlight h = ((BarLineChartBase)this.mChart).getHighlightByTouchPoint(e.getX(), e.getY());
            this.performHighlight(h, e);
            return super.onSingleTapUp(e);
        }
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        this.mLastGesture = ChartGesture.FLING;
        OnChartGestureListener l = ((BarLineChartBase)this.mChart).getOnChartGestureListener();
        if (l != null) {
            l.onChartFling(e1, e2, velocityX, velocityY);
        }

        return super.onFling(e1, e2, velocityX, velocityY);
    }

    public void stopDeceleration() {
        this.mDecelerationVelocity.x = 0.0F;
        this.mDecelerationVelocity.y = 0.0F;
    }

    public void computeScroll() {
        if (this.mDecelerationVelocity.x != 0.0F || this.mDecelerationVelocity.y != 0.0F) {
            long currentTime = AnimationUtils.currentAnimationTimeMillis();
            MPPointF var10000 = this.mDecelerationVelocity;
            var10000.x *= ((BarLineChartBase)this.mChart).getDragDecelerationFrictionCoef();
            var10000 = this.mDecelerationVelocity;
            var10000.y *= ((BarLineChartBase)this.mChart).getDragDecelerationFrictionCoef();
            float timeInterval = (float)(currentTime - this.mDecelerationLastTime) / 1000.0F;
            float distanceX = this.mDecelerationVelocity.x * timeInterval;
            float distanceY = this.mDecelerationVelocity.y * timeInterval;
            var10000 = this.mDecelerationCurrentPoint;
            var10000.x += distanceX;
            var10000 = this.mDecelerationCurrentPoint;
            var10000.y += distanceY;
            MotionEvent event = MotionEvent.obtain(currentTime, currentTime, 2, this.mDecelerationCurrentPoint.x, this.mDecelerationCurrentPoint.y, 0);
            float dragDistanceX = ((BarLineChartBase)this.mChart).isDragXEnabled() ? this.mDecelerationCurrentPoint.x - this.mTouchStartPoint.x : 0.0F;
            float dragDistanceY = ((BarLineChartBase)this.mChart).isDragYEnabled() ? this.mDecelerationCurrentPoint.y - this.mTouchStartPoint.y : 0.0F;
            this.performDrag(event, dragDistanceX, dragDistanceY);
            event.recycle();
            this.mMatrix = ((BarLineChartBase)this.mChart).getViewPortHandler().refresh(this.mMatrix, this.mChart, false);
            this.mDecelerationLastTime = currentTime;
            if (!((double)Math.abs(this.mDecelerationVelocity.x) >= 0.01D) && !((double)Math.abs(this.mDecelerationVelocity.y) >= 0.01D)) {
                ((BarLineChartBase)this.mChart).calculateOffsets();
                ((BarLineChartBase)this.mChart).postInvalidate();
                this.stopDeceleration();
            } else {
                Utils.postInvalidateOnAnimation(this.mChart);
            }

        }
    }
}
