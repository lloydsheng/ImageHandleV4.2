package com.jacp.image.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.jacp.image.util.EditImage;

public class CropImageView extends ImageViewTouchBase {
    public ArrayList<HighlightView> mHighlightViews = new ArrayList<HighlightView>();
    HighlightView mMotionHighlightView = null;
    float mLastX, mLastY;
    int mMotionEdge;
    private EditImage mCropImage;
    public ImageMoveView mMoveView;
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mBitmapDisplayed.getBitmap() != null) {
            for (HighlightView hv : mHighlightViews) {
                hv.mMatrix.set(getImageMatrix());
                hv.invalidate();
                if (hv.mIsFocused) {
                    centerBasedOnHighlightView(hv);
                }
            }
        }
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void zoomTo(float scale, float centerX, float centerY) {
        super.zoomTo(scale, centerX, centerY);
        for (HighlightView hv : mHighlightViews) {
            hv.mMatrix.set(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void zoomIn() {
        super.zoomIn();
        for (HighlightView hv : mHighlightViews) {
            hv.mMatrix.set(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void zoomOut() {
        super.zoomOut();
        for (HighlightView hv : mHighlightViews) {
            hv.mMatrix.set(getImageMatrix());
            hv.invalidate();
        }
    }

    @Override
    protected void postTranslate(float deltaX, float deltaY) {
        super.postTranslate(deltaX, deltaY);
        for (int i = 0; i < mHighlightViews.size(); i++) {
            HighlightView hv = mHighlightViews.get(i);
            hv.mMatrix.postTranslate(deltaX, deltaY);
            hv.invalidate();
        }
    }

    // According to the event's position, change the focus to the first
    // hitting cropping rectangle.
    private void recomputeFocus(MotionEvent event) {
        for (int i = 0; i < mHighlightViews.size(); i++) {
            HighlightView hv = mHighlightViews.get(i);
            hv.setFocus(false);
            hv.invalidate();
        }

        for (int i = 0; i < mHighlightViews.size(); i++) {
            HighlightView hv = mHighlightViews.get(i);
            int edge = hv.getHit(event.getX(), event.getY());
            if (edge != HighlightView.GROW_NONE) {
                if (!hv.hasFocus()) {
                    hv.setFocus(true);
                    hv.invalidate();
                }
                break;
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        EditImage cropImage = mCropImage;
        if (cropImage.mSaving) {
            return false;
        }
        
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN: // CR: inline case blocks.
        	switch (mState)
        	{
        	case STATE_NONE:
        		break;
        	case STATE_DOODLE:
        		mMoveView.getHit(event.getX(), event.getY());
        		break;
        	case STATE_HIGHLIGHT:
        		if (cropImage.mWaitingToPick) {
        			recomputeFocus(event);
        		} else {
        			for (int i = 0; i < mHighlightViews.size(); i++) { // CR:
        				// iterator
        				// for; if
        				// not, then
        				// i++ =>
        				// ++i.
        				HighlightView hv = mHighlightViews.get(i);
        				int edge = hv.getHit(event.getX(), event.getY());
        				// 如果是按住了选中框，则变换模式
        				if (edge != HighlightView.GROW_NONE) {
        					mMotionEdge = edge;
        					mMotionHighlightView = hv;
        					mLastX = event.getX();
        					mLastY = event.getY();
        					// CR: get rid of the extraneous parens below.
        					mMotionHighlightView.setMode((edge == HighlightView.MOVE) ? HighlightView.ModifyMode.Move
        							: HighlightView.ModifyMode.Grow);
        					break;
        				}
        			}
        		}
        		break;
        	}
            break;
        // CR: vertical space before case blocks.
        case MotionEvent.ACTION_UP:
        	switch (mState)
        	{
        	case STATE_NONE:
        		break;
        	case STATE_DOODLE:
        		break;
        	case STATE_HIGHLIGHT:
        		if (cropImage.mWaitingToPick) {
        			for (int i = 0; i < mHighlightViews.size(); i++) {
        				HighlightView hv = mHighlightViews.get(i);
        				if (hv.hasFocus()) {
        					cropImage.mCrop = hv;
        					for (int j = 0; j < mHighlightViews.size(); j++) {
        						if (j == i) { // CR: if j != i do your shit; no need
        							// for continue.
        							continue;
        						}
        						mHighlightViews.get(j).setHidden(true);
        					}
        					centerBasedOnHighlightView(hv);
        					cropImage.mWaitingToPick = false;
        					return true;
        				}
        			}
        		} else if (mMotionHighlightView != null) {
        			centerBasedOnHighlightView(mMotionHighlightView);
        			mMotionHighlightView.setMode(HighlightView.ModifyMode.None);
        		}
        		mMotionHighlightView = null;
        		break;
        	}
        	
            break;
        case MotionEvent.ACTION_MOVE:
        	switch (mState)
        	{
        	case STATE_NONE:
        		break;
        	case STATE_DOODLE:
        		mMoveView.handleMotion(event.getX(), event.getY());
        		break;
        	case STATE_HIGHLIGHT:
        		if (cropImage.mWaitingToPick) {
        			recomputeFocus(event);
        		} else if (mMotionHighlightView != null) {
        			mMotionHighlightView.handleMotion(mMotionEdge, event.getX() - mLastX, event.getY() - mLastY);
        			mLastX = event.getX();
        			mLastY = event.getY();
        			
        			if (true) {
        				// This section of code is optional. It has some user
        				// benefit in that moving the crop rectangle against
        				// the edge of the screen causes scrolling but it means
        				// that the crop rectangle is no longer fixed under
        				// the user's finger.
        				ensureVisible(mMotionHighlightView);
        			}
        		}
        		break;
        	}
        	
            break;
        }

        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            center(true, true);
            break;
        case MotionEvent.ACTION_MOVE:
            // if we're not zoomed then there's no point in even allowing
            // the user to move the image around. This call to center puts
            // it back to the normalized location (with false meaning don't
            // animate).
            if (getScale() == 1F) {
                center(true, true);
            }
            break;
        }

        return true;
    }

    // Pan the displayed image to make sure the cropping rectangle is visible.
    private void ensureVisible(HighlightView hv) {
        Rect r = hv.mDrawRect;

        int panDeltaX1 = Math.max(0, getLeft() - r.left);
        int panDeltaX2 = Math.min(0, getRight() - r.right);

        int panDeltaY1 = Math.max(0, getTop() - r.top);
        int panDeltaY2 = Math.min(0, getBottom() - r.bottom);

        int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
        int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

        if (panDeltaX != 0 || panDeltaY != 0) {
            panBy(panDeltaX, panDeltaY);
        }
    }

    // If the cropping rectangle's size changed significantly, change the
    // view's center and scale according to the cropping rectangle.
    private void centerBasedOnHighlightView(HighlightView hv) {
        Rect drawRect = hv.mDrawRect;

        float width = drawRect.width();
        float height = drawRect.height();

        float thisWidth = getWidth();
        float thisHeight = getHeight();

        float z1 = thisWidth / width * .6F;
        float z2 = thisHeight / height * .6F;

        float zoom = Math.min(z1, z2);
        zoom = zoom * this.getScale();
        zoom = Math.max(1F, zoom);

        if ((Math.abs(zoom - getScale()) / zoom) > .1) {
            float[] coordinates = new float[] { hv.mCropRect.centerX(), hv.mCropRect.centerY() };
            getImageMatrix().mapPoints(coordinates);
            zoomTo(zoom, coordinates[0], coordinates[1], 300F); // CR: 300.0f.
        }

        ensureVisible(hv);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (mState)
        {
        case STATE_NONE:
        	break;
        case STATE_DOODLE:
        	mMoveView.draw(canvas);
        	break;
        case STATE_HIGHLIGHT:
        	for (int i = 0; i < mHighlightViews.size(); i++) {
        		mHighlightViews.get(i).draw(canvas);
        	}
        	break;
        }
    }

    public void add(HighlightView hv) {
        mHighlightViews.add(hv);
        invalidate();
    }
    
    public void hideHighlightView()
    {
    	for (int i = 0, size = mHighlightViews.size(); i < size; i++)
    	{
    		mHighlightViews.get(i).setHidden(true);
    	}
    	
    	invalidate();
    }
    
    public void setEditImage(EditImage cropImage)
    {
    	mCropImage = cropImage;
    }
    
    /**
     * 设置是剪切状态还是涂鸦
     * @param doodle
     */
    public void setState(int state)
    {
    	mState = state;
    }
    
    public void setMoveView(ImageMoveView moveView)
    {
    	mMoveView = moveView;
    	invalidate();
    }
}
