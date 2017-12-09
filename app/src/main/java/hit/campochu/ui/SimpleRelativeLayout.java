package hit.campochu.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by ckb on 17/10/19.
 */

public class SimpleRelativeLayout extends AsyncViewGroup {

    public static final int ABOVE = 0;
    public static final int BELOW = 1;
    public static final int ALIGN_TOP = 2;
    public static final int ALIGN_BOTTOM = 3;
    public static final int LEFT_OF = 4;
    public static final int RIGHT_OF = 5;
    public static final int ALIGN_LEFT = 6;
    public static final int ALIGN_RIGHT = 7;
    public static final int ALIGN_PARENT_TOP = 8;
    public static final int ALIGN_PARENT_BOTTOM = 9;
    public static final int ALIGN_PARENT_LEFT = 10;
    public static final int ALIGN_PARENT_RIGHT = 11;
    public static final int ALIGN_VERTICAL_CENTER = 12;
    public static final int ALIGN_HORIZONTAL_CENTER = 13;
    public static final int CENTER_VERTICAL = 14;
    public static final int CENTER_HORIZONTAL = 15;
    public static final int CENTER_IN_PARENT = 16;
    private static final int RULE_SIZE = 17;

    private static final int[] LAYOUT_RULES = new int[]{
            ABOVE, BELOW, ALIGN_TOP, ALIGN_BOTTOM, LEFT_OF, RIGHT_OF, ALIGN_LEFT, ALIGN_RIGHT
    };

    private static final int[] HORIZONTAL_RULES = new int[]{
            LEFT_OF, RIGHT_OF, ALIGN_LEFT, ALIGN_RIGHT
    };

    private static final int[] VERTICAL_RULES = new int[]{
            ABOVE, BELOW, ALIGN_TOP, ALIGN_BOTTOM
    };

    private static final int[] GRAVITY_RULES = new int[]{
            ALIGN_VERTICAL_CENTER, ALIGN_HORIZONTAL_CENTER
    };

    // 依赖有向图
    // key -> A.id（前32位）＋ LEFT_OF（后32位）
    // value -> B.id
    private final LongSparseArray<Integer> mGraph = new LongSparseArray<Integer>();

    private final SparseArray<View> mViews = new SparseArray<View>();

    private volatile View[] mChildViews;
    private volatile View[] mSortedRootViews;
    private volatile View[] mHorizontalSortedViews;
    private volatile View[] mVerticalSortedViews;
    private volatile View[] mGravitySortedViews;

    public SimpleRelativeLayout(Context context) {
        this(context, null);
    }

    public SimpleRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttr(context, attrs, defStyleAttr);
    }

    protected void initFromAttr(Context context, AttributeSet attrs, int defStyleAttr) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SimpleRelativeLayout, defStyleAttr, 0);
        setEnable(a.getBoolean(R.styleable.SimpleRelativeLayout_async, false));
        a.recycle();
    }

    @Override
    protected void measureInner(int widthMeasureSpec, int heightMeasureSpec) {

        final int count = getChildCount();

        if (childrenChanged()) {
            buildGraphAndSort();
        }

        View[] rootSorted = mSortedRootViews;
        View[] horizontalSorted = mHorizontalSortedViews;
        View[] verticalSorted = mVerticalSortedViews;
        View[] gravitySorted = mGravitySortedViews;

        int width = 0;
        int height = 0;

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        }

        final boolean isWrapContentWidth = widthMode != MeasureSpec.EXACTLY;
        final boolean isWrapContentHeight = heightMode != MeasureSpec.EXACTLY;

        int rootCount = rootSorted == null ? 0 : rootSorted.length;
        int remainCount = count - rootCount;

        // 根节点 measure
        for (int i = 0; i < rootCount; ++i) {
            View child = rootSorted[i];
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                applyHorizontalChildRules(lp, getId(child), widthSize);
                applyVerticalChildRules(lp, getId(child), heightSize);
                measureChild(child, lp, widthSize, heightSize);
                positionHorizontalChild(child, lp);
                positionVerticalChild(child, lp);
                if (isWrapContentWidth) {
                    width = Math.max(width, lp.mRight + lp.rightMargin);
                }
                if (isWrapContentHeight) {
                    height = Math.max(height, lp.mBottom + lp.bottomMargin);
                }
            }
        }

        // 水平垂直的环形依赖 measure
        if (remainCount > 0) {
            for (int i = 0; i < remainCount; ++i) {
                View child = horizontalSorted[i];
                if (child.getVisibility() != GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    applyHorizontalChildRules(lp, getId(child), widthSize);
                    measureChildHorizontal(child, lp, widthSize, heightSize);
                    positionHorizontalChild(child, lp);
                }
            }
            for (int i = 0; i < remainCount; ++i) {
                View child = verticalSorted[i];
                if (child.getVisibility() != GONE) {
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    applyVerticalChildRules(lp, getId(child), heightSize);
                    measureChild(child, lp, widthSize, heightSize);
                    positionVerticalChild(child, lp);
                    if (isWrapContentWidth) {
                        width = Math.max(width, lp.mRight + lp.rightMargin);
                    }
                    if (isWrapContentHeight) {
                        height = Math.max(height, lp.mBottom + lp.bottomMargin);
                    }
                }
            }
        }

        if (isWrapContentWidth) {
            width += getPaddingRight();
            ViewGroup.LayoutParams lp = getLayoutParams();
            if (lp != null && lp.width >= 0) {
                width = Math.max(width, lp.width);
            }
            width = resolveSize(Math.max(width, getSuggestedMinimumWidth()), widthMeasureSpec);
        }

        if (isWrapContentHeight) {
            height += getPaddingBottom();
            ViewGroup.LayoutParams lp = getLayoutParams();
            if (lp != null && lp.height >= 0) {
                height = Math.max(height, lp.height);
            }
            height = resolveSize(Math.max(height, getSuggestedMinimumHeight()), heightMeasureSpec);
        }

        // 调整 child gravity
        for (int i = 0; i < count; ++i) {
            View child = gravitySorted[i];
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isWrapContentWidth) {
                    if ((lp.mPrivateFlag & 1 << ALIGN_PARENT_RIGHT) > 0) {
                        final int childWidth = child.getMeasuredWidth();
                        lp.mLeft = width - getPaddingRight() - childWidth;
                        lp.mRight = lp.mLeft + childWidth;
                    }
                }
                if (isWrapContentHeight) {
                    if ((lp.mPrivateFlag & 1 << ALIGN_PARENT_BOTTOM) > 0) {
                        final int childHeight = child.getMeasuredHeight();
                        lp.mTop = height - getPaddingBottom() - childHeight;
                        lp.mBottom = lp.mTop + childHeight;
                    }
                }
                if ((lp.mPrivateFlag & 1 << CENTER_VERTICAL) > 0 || (lp.mPrivateFlag & 1 << CENTER_IN_PARENT) > 0) {
                    final int childHeight = child.getMeasuredHeight();
                    lp.mTop = getPaddingTop() + (height - getPaddingBottom() - getPaddingTop() - childHeight) / 2;
                    lp.mBottom = lp.mTop + childHeight;
                }
                if ((lp.mPrivateFlag & 1 << CENTER_HORIZONTAL) > 0 || (lp.mPrivateFlag & 1 << CENTER_IN_PARENT) > 0) {
                    final int childWidth = child.getMeasuredWidth();
                    lp.mLeft = getPaddingLeft() + (width - getPaddingLeft() - getPaddingRight() - childWidth) / 2;
                    lp.mRight = lp.mLeft + childWidth;
                }

                applyGravityChildRules(lp, getId(child), width, height);
            }
        }

        onFreeStyle(width, height);

        setMeasureResult(width, height);
    }

    protected void onFreeStyle(int width, int height) {
    }

    @Override
    protected boolean childrenChanged() {
        final int count = getChildCount();
        int childLen = mChildViews == null ? 0 : mChildViews.length;
        if (childLen != count) {
            return true;
        }
        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            if (((LayoutParams) child.getLayoutParams()).mRulesChanged) {
                return true;
            }
        }
        return false;
    }

    // 建图 并 排序
    // 必须在计算之前进行
    private void buildGraphAndSort() {

        final int count = getChildCount();
        int rootCount;
        int remainCount;

        if (mChildViews == null || mChildViews.length != count) {
            mChildViews = new View[count];
        }

        final View[] children = mChildViews;
        final LongSparseArray<Integer> graph = mGraph;
        graph.clear();
        final SparseArray<View> views = mViews;
        views.clear();

        views.put(getId(this), this);
        // 建图
        for (int i = 0; i < count; ++i) {
            View child = children[i] = getChildAt(i);
            int id = getId(child);
            views.put(id, child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.mRulesChanged = false;
            if (lp.mPrivateFlag == 0) {
                continue;
            }
            int[] rules = lp.mRules;
            int len = rules.length;
            for (int j = 0; j < len; ++j) {
                if (rules[j] > 0) {
                    graph.put((long) id << 32 | (long) j & 0xffffffffL, rules[j]);
                }
            }
        }

        rootCount = sortChildren(children, LAYOUT_RULES, null);
        remainCount = count - rootCount;
        if (mSortedRootViews == null || mSortedRootViews.length != rootCount) {
            mSortedRootViews = new View[rootCount];
        }
        System.arraycopy(children, 0, mSortedRootViews, 0, rootCount);

        if (rootCount < count) {
            if (mHorizontalSortedViews == null || mHorizontalSortedViews.length != remainCount) {
                mHorizontalSortedViews = new View[remainCount];
            }
            if (mVerticalSortedViews == null || mVerticalSortedViews.length != remainCount) {
                mVerticalSortedViews = new View[remainCount];
            }
            System.arraycopy(children, rootCount, mHorizontalSortedViews, 0, remainCount);
            System.arraycopy(children, rootCount, mVerticalSortedViews, 0, remainCount);
            int horizontalSorted = sortChildren(mHorizontalSortedViews, HORIZONTAL_RULES, mSortedRootViews);
            if (horizontalSorted < remainCount) {
                throw new IllegalStateException("存在水平方向属性环形依赖！");
            }

            int verticalSorted = sortChildren(mVerticalSortedViews, VERTICAL_RULES, mSortedRootViews);
            if (verticalSorted < remainCount) {
                throw new IllegalStateException("存在垂直方向属性环形依赖！");
            }

        }

        if (mGravitySortedViews == null || mGravitySortedViews.length != count) {
            mGravitySortedViews = new View[count];
        }
        System.arraycopy(children, 0, mGravitySortedViews, 0, count);
        sortChildren(mGravitySortedViews, GRAVITY_RULES, null);
    }

    private int sortChildren(View[] sorted, int[] rules, View[] roots) {
        int sortedIndex;
        final SparseArray<View> root = new SparseArray<View>();
        final int count = sorted.length;

        int flag = 0;
        for (int r : rules) {
            flag |= 1 << r;
        }

        if (roots != null && roots.length != 0) {
            for (View child : roots) {
                root.put(getId(child), child);
            }
        }

        int[] flags = new int[count];
        View[] temp = new View[count];
        System.arraycopy(sorted, 0, temp, 0, count);

        int index = 0;
        for (int i = 0; i < count; ++i) {
            View child = temp[i];
            int id = getId(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            flags[i] = lp.mPrivateFlag & flag;
            if (flags[i] == 0) {
                sorted[index++] = child;
                root.put(id, child);
            }
        }

        int lastIndex = index;
        if (index < count) {
            for (int i = 0; i < count; ++i) {
                if (flags[i] == 0) {
                    continue;
                }
                View child = temp[i];
                int id = getId(child);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int[] childRules = lp.mRules;
                for (int r : rules) {
                    if (childRules[r] > 0 && root.indexOfKey(childRules[r]) >= 0) {
                        flags[i] &= ~(1 << r);
                    }
                    if (flags[i] == 0) {
                        break;
                    }
                }
                if (flags[i] == 0) {
                    sorted[index++] = child;
                    root.put(id, child);
                }
                if (i == count - 1) {
                    if (lastIndex < index && index < count) {
                        lastIndex = index;
                        i = 0;
                    }
                }
            }
        }
        sortedIndex = index;
        if (index < count) {
            for (int i = 0; i < count; ++i) {
                if (flags[i] == 0) {
                    continue;
                }
                sorted[index++] = temp[i];
            }
        }
        return sortedIndex;
    }

    private View getRelatedView(int id, int rule) {
        Integer related = mGraph.get((long) id << 32 | (long) rule & 0xffffffffL);
        if (related == null) {
            return null;
        }
        View v = mViews.get(related);
        while (v.getVisibility() == GONE) {
            related = mGraph.get((long) related << 32 | (long) rule & 0xffffffffL);
            if (related == null) {
                return null;
            }
            v = mViews.get(related);
        }
        return v;
    }

    private LayoutParams getRelatedViewParams(int id, int rule) {
        View v = getRelatedView(id, rule);
        if (v != null) {
            return (LayoutParams) v.getLayoutParams();
        }
        return null;
    }

    private void applyVerticalChildRules(LayoutParams clp, int id, int height) {

        clp.mTop = clp.mBottom = -1;

        int flag = 0;
        for (int r : VERTICAL_RULES) {
            flag |= 1 << r;
        }

        int[] rules = new int[RULE_SIZE];
        System.arraycopy(clp.mRules, 0, rules, 0, RULE_SIZE);


        if ((clp.mPrivateFlag & flag) != 0) {
            LayoutParams lp;

            lp = getRelatedViewParams(id, ABOVE);
            if (lp != null) {
                clp.mBottom = lp.mTop - (lp.topMargin + clp.bottomMargin);
            } else if (rules[ABOVE] > 0) {
                rules[ALIGN_PARENT_BOTTOM] = 1;
            }

            lp = getRelatedViewParams(id, BELOW);
            if (lp != null) {
                clp.mTop = lp.mBottom + (lp.bottomMargin + clp.topMargin);
            } else if (rules[BELOW] > 0) {
                rules[ALIGN_PARENT_TOP] = 1;
            }

            lp = getRelatedViewParams(id, ALIGN_TOP);
            if (lp != null) {
                clp.mTop = lp.mTop + clp.topMargin;
            } else if (rules[ALIGN_TOP] > 0) {
                rules[ALIGN_PARENT_TOP] = 1;
            }

            lp = getRelatedViewParams(id, ALIGN_BOTTOM);
            if (lp != null) {
                clp.mBottom = lp.mBottom - clp.bottomMargin;
            } else if (rules[ALIGN_BOTTOM] > 0) {
                rules[ALIGN_PARENT_BOTTOM] = 1;
            }
        }

        if (rules[ALIGN_PARENT_TOP] > 0) {
            clp.mTop = getPaddingTop() + clp.topMargin;
        }
        if (rules[ALIGN_PARENT_BOTTOM] > 0 && height > 0) {
            clp.mBottom = height - getPaddingBottom() + clp.bottomMargin;
        }

    }

    private void applyHorizontalChildRules(LayoutParams clp, int id, int width) {

        clp.mLeft = clp.mRight = -1;

        int flag = 0;
        for (int r : HORIZONTAL_RULES) {
            flag |= 1 << r;
        }

        int[] rules = new int[RULE_SIZE];
        System.arraycopy(clp.mRules, 0, rules, 0, RULE_SIZE);

        if ((clp.mPrivateFlag & flag) != 0) {
            LayoutParams lp;

            lp = getRelatedViewParams(id, LEFT_OF);
            if (lp != null) {
                clp.mRight = lp.mLeft - (lp.leftMargin + clp.rightMargin);
            } else if (rules[LEFT_OF] > 0) {
                rules[ALIGN_PARENT_RIGHT] = 1;
            }

            lp = getRelatedViewParams(id, RIGHT_OF);
            if (lp != null) {
                clp.mLeft = lp.mRight + (lp.rightMargin + clp.leftMargin);
            } else if (rules[RIGHT_OF] > 0) {
                rules[ALIGN_PARENT_LEFT] = 1;
            }

            lp = getRelatedViewParams(id, ALIGN_LEFT);
            if (lp != null) {
                clp.mLeft = lp.mLeft + clp.leftMargin;
            } else if (rules[ALIGN_LEFT] > 0) {
                rules[ALIGN_PARENT_LEFT] = 1;
            }

            lp = getRelatedViewParams(id, ALIGN_RIGHT);
            if (lp != null) {
                clp.mRight = lp.mRight - clp.rightMargin;
            } else if (rules[ALIGN_RIGHT] > 0) {
                rules[ALIGN_PARENT_RIGHT] = 1;
            }

        }
        if (rules[ALIGN_PARENT_LEFT] > 0) {
            clp.mLeft = getPaddingLeft() + clp.leftMargin;
        }
        if (rules[ALIGN_PARENT_RIGHT] > 0 && width > 0) {
            clp.mRight = width - getPaddingRight() - clp.rightMargin;
        }

    }

    private void applyGravityChildRules(LayoutParams clp, int id, int width, int height) {

        int flag = 0;
        for (int r : GRAVITY_RULES) {
            flag |= 1 << r;
        }

        if ((clp.mPrivateFlag & flag) != 0) {
            View related;

            related = getRelatedView(id, ALIGN_HORIZONTAL_CENTER);
            if (related != null) {
                int ccenter = clp.mLeft + (clp.mRight - clp.mLeft) / 2, tcenter;
                if (related == this) {
                    tcenter = width / 2;
                } else {
                    LayoutParams lp = (LayoutParams) related.getLayoutParams();
                    tcenter = lp.mLeft + (lp.mRight - lp.mLeft) / 2;
                }
                int offset = tcenter - ccenter;
                clp.mLeft += offset;
                clp.mRight += offset;
            }

            related = getRelatedView(id, ALIGN_VERTICAL_CENTER);
            if (related != null) {
                int ccenter = clp.mTop + (clp.mBottom - clp.mTop) / 2, tcenter;
                if (related == this) {
                    tcenter = height / 2;
                } else {
                    LayoutParams lp = (LayoutParams) related.getLayoutParams();
                    tcenter = lp.mTop + (lp.mBottom - lp.mTop) / 2;
                }
                int offset = tcenter - ccenter;
                clp.mTop += offset;
                clp.mBottom += offset;
            }
        }

    }

    private void measureChildHorizontal(View child, LayoutParams lp, int myWidth, int myHeight) {
        final int childWidthMeasureSpec = getChildMeasureSpec(lp.mLeft, lp.mRight, lp.width,
                lp.leftMargin, lp.rightMargin, getPaddingLeft(), getPaddingRight(), myWidth);

        final int childHeightMeasureSpec;
        if (myHeight < 0) {
            if (lp.height >= 0) {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            } else {
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            }
        } else {
            final int maxHeight;
            maxHeight = Math.max(0,
                    myHeight - getPaddingTop() - getPaddingBottom() - lp.topMargin - lp.bottomMargin);

            final int heightMode;
            if (lp.height == RelativeLayout.LayoutParams.MATCH_PARENT) {
                heightMode = MeasureSpec.EXACTLY;
            } else {
                heightMode = MeasureSpec.AT_MOST;
            }
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, heightMode);
        }

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private void measureChild(View child, LayoutParams lp, int myWidth, int myHeight) {
        int childWidthMeasureSpec = getChildMeasureSpec(lp.mLeft, lp.mRight, lp.width,
                lp.leftMargin, lp.rightMargin, getPaddingLeft(), getPaddingRight(), myWidth);
        int childHeightMeasureSpec = getChildMeasureSpec(lp.mTop, lp.mBottom, lp.height,
                lp.topMargin, lp.bottomMargin, getPaddingTop(), getPaddingBottom(), myHeight);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private int getChildMeasureSpec(int childStart, int childEnd,
                                    int childSize,
                                    int startMargin, int endMargin,
                                    int startPadding, int endPadding,
                                    int mySize) {
        int childSpecMode = 0;
        int childSpecSize = 0;
        // SimpleRelativeLayout 大小不确认
        if (mySize < 0) {
            // 通过rule已算出大小
            if (childStart != -1 && childEnd != -1) {
                childSpecSize = Math.max(0, childEnd - childStart);
                childSpecMode = MeasureSpec.EXACTLY;
            }
            // LayoutParam 定义了大小
            else if (childSize >= 0) {
                childSpecSize = childSize;
                childSpecMode = MeasureSpec.EXACTLY;
            }
            // 什么都没确定
            else {
                childSpecSize = 0;
                childSpecMode = MeasureSpec.UNSPECIFIED;
            }
            return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
        }

        // 自己算大小

        int tempStart = childStart;
        int tempEnd = childEnd;

        if (tempStart == -1) {
            tempStart = startPadding + startMargin;
        }
        if (tempEnd == -1) {
            tempEnd = mySize - endPadding - endMargin;
        }
        // 最大可用
        final int maxAvailable = tempEnd - tempStart;

        // 通过rule已算出大小
        if (childStart != -1 && childEnd != -1) {
            childSpecMode = MeasureSpec.EXACTLY;
            childSpecSize = Math.max(0, maxAvailable);
        } else {
            // LayoutParam 定义了大小
            if (childSize >= 0) {
                childSpecMode = MeasureSpec.EXACTLY;
                if (maxAvailable >= 0) {
                    childSpecSize = Math.min(maxAvailable, childSize);
                } else {
                    childSpecSize = childSize;
                }
            }
            // match_parent 推算大小
            else if (childSize == LayoutParams.MATCH_PARENT) {
                childSpecMode = MeasureSpec.EXACTLY;
                childSpecSize = Math.max(0, maxAvailable);
            }
            // wrap_content 推算大小
            else if (childSize == LayoutParams.WRAP_CONTENT) {
                if (maxAvailable >= 0) {
                    childSpecMode = MeasureSpec.AT_MOST;
                    childSpecSize = maxAvailable;
                }
                // 什么都没确定
                else {
                    childSpecMode = MeasureSpec.UNSPECIFIED;
                    childSpecSize = 0;
                }
            }
        }
        return MeasureSpec.makeMeasureSpec(childSpecSize, childSpecMode);
    }

    private void positionHorizontalChild(View child, LayoutParams lp) {
        if (lp.mLeft == -1 && lp.mRight != -1) {
            lp.mLeft = lp.mRight - child.getMeasuredWidth();
        } else if (lp.mLeft != -1 && lp.mRight == -1) {
            lp.mRight = lp.mLeft + child.getMeasuredWidth();
        } else if (lp.mLeft == -1 && lp.mRight == -1) {
            lp.mLeft = getPaddingLeft() + lp.leftMargin;
            lp.mRight = lp.mLeft + child.getMeasuredWidth();
        }
    }

    private void positionVerticalChild(View child, LayoutParams lp) {
        if (lp.mTop == -1 && lp.mBottom != -1) {
            lp.mTop = lp.mBottom - child.getMeasuredHeight();
        } else if (lp.mTop != -1 && lp.mBottom == -1) {
            lp.mBottom = lp.mTop + child.getMeasuredHeight();
        } else if (lp.mTop == -1 && lp.mBottom == -1) {
            lp.mTop = getPaddingTop() + lp.topMargin;
            lp.mBottom = lp.mTop + child.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.layout(lp.mLeft, lp.mTop, lp.mRight, lp.mBottom);
            }
        }
    }

    private static int getId(View v) {
        int id = v.getId();
        return id > 0 ? id : v.hashCode();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return new LayoutParams((LayoutParams) p);
        } else if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        }
        return new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return SimpleRelativeLayout.class.getName();
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        private int mPrivateFlag = 0;
        private int[] mRules = new int[RULE_SIZE];
        public int mLeft, mTop, mRight, mBottom;

        private boolean mRulesChanged = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final int[] rules = mRules;

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.SimpleRelativeLayout);

            final int count = a.getIndexCount();

            for (int i = 0; i < count; ++i) {
                int attr = a.getIndex(i);

                if (attr == R.styleable.SimpleRelativeLayout_layout_above) {
                    rules[ABOVE] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << ABOVE;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_below) {
                    rules[BELOW] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << BELOW;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignTop) {
                    rules[ALIGN_TOP] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << ALIGN_TOP;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignBottom) {
                    rules[ALIGN_BOTTOM] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << ALIGN_BOTTOM;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_toLeftOf) {
                    rules[LEFT_OF] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << LEFT_OF;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_toRightOf) {
                    rules[RIGHT_OF] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << RIGHT_OF;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignLeft) {
                    rules[ALIGN_LEFT] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << ALIGN_LEFT;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignRight) {
                    rules[ALIGN_RIGHT] = a.getResourceId(attr, 0);
                    mPrivateFlag |= 1 << ALIGN_RIGHT;
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignParentTop) {
                    rules[ALIGN_PARENT_TOP] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[ALIGN_PARENT_TOP] > 0) {
                        mPrivateFlag |= 1 << ALIGN_PARENT_TOP;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignParentBottom) {
                    rules[ALIGN_PARENT_BOTTOM] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[ALIGN_PARENT_BOTTOM] > 0) {
                        mPrivateFlag |= 1 << ALIGN_PARENT_BOTTOM;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignParentLeft) {
                    rules[ALIGN_PARENT_LEFT] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[ALIGN_PARENT_LEFT] > 0) {
                        mPrivateFlag |= 1 << ALIGN_PARENT_LEFT;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignParentRight) {
                    rules[ALIGN_PARENT_RIGHT] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[ALIGN_PARENT_RIGHT] > 0) {
                        mPrivateFlag |= 1 << ALIGN_PARENT_RIGHT;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignVerticalCenter) {
                    rules[ALIGN_VERTICAL_CENTER] = a.getResourceId(attr, 0);
                    if (rules[ALIGN_VERTICAL_CENTER] > 0) {
                        mPrivateFlag |= 1 << ALIGN_VERTICAL_CENTER;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_alignHorizontalCenter) {
                    rules[ALIGN_HORIZONTAL_CENTER] = a.getResourceId(attr, 0);
                    if (rules[ALIGN_HORIZONTAL_CENTER] > 0) {
                        mPrivateFlag |= 1 << ALIGN_HORIZONTAL_CENTER;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_centerVertical) {
                    rules[CENTER_VERTICAL] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[CENTER_VERTICAL] > 0) {
                        mPrivateFlag |= 1 << CENTER_VERTICAL;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_centerHorizontal) {
                    rules[CENTER_HORIZONTAL] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[CENTER_HORIZONTAL] > 0) {
                        mPrivateFlag |= 1 << CENTER_HORIZONTAL;
                    }
                } else if (attr == R.styleable.SimpleRelativeLayout_layout_centerInParent) {
                    rules[CENTER_IN_PARENT] = a.getBoolean(attr, false) ? 1 : 0;
                    if (rules[CENTER_IN_PARENT] > 0) {
                        mPrivateFlag |= 1 << CENTER_IN_PARENT;
                    }
                }
            }
            mRulesChanged = true;

            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.mRulesChanged = source.mRulesChanged;
            this.mPrivateFlag = source.mPrivateFlag;
            System.arraycopy(source.mRules, 0, this.mRules, 0, RULE_SIZE);
        }

        public void addRule(int verb) {
            addRule(verb, 1);
        }

        public void addRule(int verb, int subject) {
            mRules[verb] = subject;
            mPrivateFlag |= 1 << verb;
            mRulesChanged = true;
        }

        public void removeRule(int verb) {
            addRule(verb, 0);
        }

        public int getRule(int verb) {
            return mRules[verb];
        }

        public int[] getRules() {
            return mRules;
        }

    }

    protected final void layoutVerticalCenter(Rect outFrame, int... ids) {
        View[] views = layoutIdsToViews(ids);
        Rect frame = layoutFrame(views);
        int offset = (outFrame.height() - frame.height()) / 2;
        layoutOffsetVertical(offset, views);
    }

    protected final void layoutHorizontalCenter(Rect outFrame, int... ids) {
        View[] views = layoutIdsToViews(ids);
        Rect frame = layoutFrame(views);
        int offset = (outFrame.width() - frame.width()) / 2;
        layoutOffsetHorizontal(offset, views);
    }

    protected final void layoutCenter(Rect outFrame, int... ids) {
        View[] views = layoutIdsToViews(ids);
        Rect frame = layoutFrame(views);
        int topOffset = (outFrame.width() - frame.width()) / 2;
        int leftOffset = (outFrame.height() - frame.height()) / 2;
        layoutOffsetCenter(topOffset, leftOffset, views);
    }

    protected final void layoutOffsetVertical(int offset, View... views) {
        for (View v : views) {
            SimpleRelativeLayout.LayoutParams lp = (SimpleRelativeLayout.LayoutParams) v.getLayoutParams();
            lp.mTop = lp.mTop + offset;
            lp.mBottom = lp.mBottom + offset;
        }
    }

    protected final void layoutOffsetHorizontal(int offset, View... views) {
        for (View v : views) {
            SimpleRelativeLayout.LayoutParams lp = (SimpleRelativeLayout.LayoutParams) v.getLayoutParams();
            lp.mLeft = lp.mLeft + offset;
            lp.mRight = lp.mRight + offset;
        }
    }

    protected final void layoutOffsetCenter(int topOffset, int leftOffset, View... views) {
        for (View v : views) {
            SimpleRelativeLayout.LayoutParams lp = (SimpleRelativeLayout.LayoutParams) v.getLayoutParams();
            lp.mTop = lp.mTop + topOffset;
            lp.mBottom = lp.mBottom + topOffset;
            lp.mLeft = lp.mLeft + leftOffset;
            lp.mRight = lp.mRight + leftOffset;
        }
    }

    protected final Rect layoutFrame(View... views) {
        Rect frame = new Rect();
        for (View v : views) {
            SimpleRelativeLayout.LayoutParams lp = (SimpleRelativeLayout.LayoutParams) v.getLayoutParams();
            frame.top = Math.min(frame.top, lp.mTop + lp.topMargin);
            frame.bottom = Math.max(frame.bottom, lp.mBottom + lp.bottomMargin);
            frame.left = Math.min(frame.left, lp.mLeft + lp.leftMargin);
            frame.right = Math.max(frame.right, lp.mRight + lp.rightMargin);
        }
        return frame;
    }

    private View[] layoutIdsToViews(int... ids) {
        if (ids == null || ids.length == 0) {
            return new View[0];
        }
        View[] views = new View[ids.length];
        for (int i = 0; i < ids.length; ++i) {
            views[i] = mViews.get(ids[i]);
        }
        return views;
    }

}
