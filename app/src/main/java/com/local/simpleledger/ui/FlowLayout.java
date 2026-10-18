package com.local.simpleledger.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/** Minimal wrapping layout for category chips. */
public final class FlowLayout extends ViewGroup {

    private int horizontalSpacing;
    private int verticalSpacing;

    public FlowLayout(Context context) {
        super(context);
        init(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        horizontalSpacing = Ui.dp(context, 8);
        verticalSpacing = Ui.dp(context, 8);
    }

    public void setSpacing(int horizontalDp, int verticalDp) {
        horizontalSpacing = Ui.dp(getContext(), horizontalDp);
        verticalSpacing = Ui.dp(getContext(), verticalDp);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int lineWidth = 0;
        int lineHeight = 0;
        int totalHeight = 0;
        int maxLineWidth = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            if (lp instanceof MarginLayoutParams) {
                MarginLayoutParams mlp = (MarginLayoutParams) lp;
                childWidth += mlp.leftMargin + mlp.rightMargin;
                childHeight += mlp.topMargin + mlp.bottomMargin;
            }
            if (lineWidth > 0 && lineWidth + horizontalSpacing + childWidth > available) {
                totalHeight += lineHeight + verticalSpacing;
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
                lineWidth = childWidth;
                lineHeight = childHeight;
            } else {
                lineWidth += (lineWidth > 0 ? horizontalSpacing : 0) + childWidth;
                lineHeight = Math.max(lineHeight, childHeight);
            }
        }
        totalHeight += lineHeight;
        maxLineWidth = Math.max(maxLineWidth, lineWidth);
        totalHeight += getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(Math.max(maxLineWidth, width), widthMeasureSpec),
                resolveSize(totalHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int available = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int x = getPaddingLeft();
        int y = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            ViewGroup.LayoutParams raw = child.getLayoutParams();
            int leftMargin = 0;
            int topMargin = 0;
            int rightMargin = 0;
            int bottomMargin = 0;
            if (raw instanceof MarginLayoutParams) {
                MarginLayoutParams mlp = (MarginLayoutParams) raw;
                leftMargin = mlp.leftMargin;
                topMargin = mlp.topMargin;
                rightMargin = mlp.rightMargin;
                bottomMargin = mlp.bottomMargin;
            }
            int childWidth = child.getMeasuredWidth() + leftMargin + rightMargin;
            int childHeight = child.getMeasuredHeight() + topMargin + bottomMargin;
            if (x > getPaddingLeft() && x + childWidth > getPaddingLeft() + available) {
                x = getPaddingLeft();
                y += lineHeight + verticalSpacing;
                lineHeight = 0;
            }
            child.layout(x + leftMargin, y + topMargin,
                    x + leftMargin + child.getMeasuredWidth(),
                    y + topMargin + child.getMeasuredHeight());
            x += childWidth + horizontalSpacing;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }
}
