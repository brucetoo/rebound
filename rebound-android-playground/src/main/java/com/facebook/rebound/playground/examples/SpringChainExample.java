/*
 * This file provided by Facebook is for non-commercial testing and evaluation purposes only.
 * Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.rebound.playground.examples;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TableLayout;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringChain;
import com.facebook.rebound.playground.R;

import java.util.ArrayList;
import java.util.List;

public class SpringChainExample extends FrameLayout {

  private final SpringChain mSpringChain = SpringChain.create();

  private final List<View> mViews = new ArrayList<View>();
  private float mLastDownX;

  /** Touch handling **/
  private View mLastDraggingView;
  private float mLastDownXlat;
  private int mActivePointerId;
  private VelocityTracker mVelocityTracker;

  public SpringChainExample(Context context) {
    super(context);

    LayoutInflater inflater = LayoutInflater.from(context);
    ViewGroup container = (ViewGroup) inflater.inflate(R.layout.cascade_effect, this, false);
    addView(container);
    ViewGroup rootView = (ViewGroup) container.findViewById(R.id.root);
    int bgColor = Color.argb(255, 17, 148, 231);
    setBackgroundColor(bgColor);
    rootView.setBackgroundResource(R.drawable.rebound_tiles);

    int startColor = Color.argb(255, 255, 64, 230);
    int endColor = Color.argb(255, 255, 230, 64);
    ArgbEvaluator evaluator = new ArgbEvaluator();
    int viewCount = 10;
    for (int i = 0; i < viewCount; i++) {
      final View view = new View(context);
      view.setLayoutParams(
          new TableLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.WRAP_CONTENT,
              1f));
      mSpringChain.addSpring(new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
          float value = (float) spring.getCurrentValue();
          view.setTranslationX(value);
        }
      });
      //color通过计算view count的Friction来获取 ArgbEvaluator 对应的值
      int color = (Integer) evaluator.evaluate((float) i / (float) viewCount, startColor, endColor);
      view.setBackgroundColor(color);
      //每个view都设置touch事件
      view.setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
          return handleRowTouch(v, event);
        }
      });
      mViews.add(view);
      rootView.addView(view);
    }

    getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        List<Spring> springs = mSpringChain.getAllSprings();
        //其实以下的初始化设置值都可要可不要的,主要是初始化的时候设置SpringChain相应初始值
        for (int i = 0; i < springs.size(); i++) {
          springs.get(i).setCurrentValue(-mViews.get(i).getWidth());
        }
        postDelayed(new Runnable() {
          @Override
          public void run() {
            mSpringChain
                .setControlSpringIndex(0)
                .getControlSpring()
                .setEndValue(0);
          }
        }, 500);
      }
    });
  }

  private boolean handleRowTouch(View view, MotionEvent event) {
    int action = event.getAction();
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN:

        //记录按下的点的id
        mActivePointerId = event.getPointerId(0);
        //按下时 View 对应的translationX offset
        mLastDownXlat = view.getTranslationX();
        //记录按下的被拖拽的view
        mLastDraggingView = view;
        //按下点的在屏幕中的 x 坐标
        mLastDownX = event.getRawX();

        //按下时 开始监听手指滑动的速率
        mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(event);

        //通过view add的顺序获取draggingView对应index
        int idx = mViews.indexOf(mLastDraggingView);
        //设置SpringChain对应的ControlSpringIndex为 index
        mSpringChain
            .setControlSpringIndex(idx)
            .getControlSpring()
            .setCurrentValue(mLastDownXlat);//view的位移设置为当前的translationX值
        break;
      case MotionEvent.ACTION_MOVE: {
        //如果ACTION_DOWN记录的pointer id != -1
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        if (pointerIndex != -1) {
          final int location[] = {0, 0};
          view.getLocationOnScreen(location);
          float x = event.getX(pointerIndex) + location[0];
//          float offset = x - mLastDownX + mLastDownXlat;
          //以上使用 getLocationOnScreen方法获取到移动的点在屏幕中的位置
          //实际可用用 event.getRawX来获取,效果一样.但是如果要获取 Y坐标
          //需要event.getRawY - statusBar的高度
          float offset = event.getRawX() - mLastDownX + mLastDownXlat;
          mSpringChain
              .getControlSpring()//need get control spring first,and setCurrentValue
              .setCurrentValue(offset);
          mVelocityTracker.addMovement(event);//listen move velocity
        }
        break;
      }
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_UP:
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        if (pointerIndex != -1) {
          mVelocityTracker.addMovement(event);
          mVelocityTracker.computeCurrentVelocity(1000);
          mSpringChain
              .getControlSpring()
              .setVelocity(mVelocityTracker.getXVelocity())
              .setEndValue(0);
        }
        break;
      }
    return true;
  }
}
