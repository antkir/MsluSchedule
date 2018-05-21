/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import by.ntnk.msluschedule.R

private const val SHADOW_MULTIPLIER = 1.5f

/* Simplified/Kotlinified RoundRectDrawableWithShadow from CardView package for pre-21 devices */
class RoundRectDrawable(
        context: Context,
        backgroundColor: ColorStateList?,
        radius: Float,
        shadowSize: Float,
        maxShadowSize: Float
) : Drawable() {
    // extra shadow to avoid gaps between card and shadow
    private val mInsetShadow: Int = context.resources.getDimensionPixelSize(R.dimen.card_inset_shadow)

    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private val mCornerShadowPaint: Paint

    private val mEdgeShadowPaint: Paint

    private val mCardBounds: RectF

    private val mCornerRadius: Float

    private var mCornerShadowPath: Path? = null

    // actual value set by developer
    private var mRawMaxShadowSize: Float = 0.toFloat()

    // multiplied value to account for shadow offset
    private var mShadowSize: Float = 0.toFloat()

    // actual value set by developer
    private var mRawShadowSize: Float = 0.toFloat()

    private var mBackground: ColorStateList? = null

    private var mDirty = true

    private val mShadowStartColor: Int = ContextCompat.getColor(context, R.color.shadow_start_color)

    private val mShadowEndColor: Int = ContextCompat.getColor(context, R.color.shadow_end_color)

    private var isTopDrawn = true

    private var isBottomDrawn = true

    init {
        setBackground(backgroundColor)
        mCornerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mCornerShadowPaint.style = Paint.Style.FILL
        mCornerRadius = (radius + .5f).toInt().toFloat()
        mCardBounds = RectF()
        mEdgeShadowPaint = Paint(mCornerShadowPaint)
        mEdgeShadowPaint.isAntiAlias = false
        setShadowSize(shadowSize, maxShadowSize)
    }

    fun setSidesDrawn(isTopDrawn: Boolean, isBottomDrawn: Boolean) {
        this.isTopDrawn = isTopDrawn
        this.isBottomDrawn = isBottomDrawn
    }

    private fun setBackground(color: ColorStateList?) {
        mBackground = color ?: ColorStateList.valueOf(Color.TRANSPARENT)
        mPaint.color = mBackground!!.getColorForState(state, mBackground!!.defaultColor)
    }

    /**
     * Casts the value to an even integer.
     */
    private fun toEven(value: Float): Int {
        val i = (value + .5f).toInt()
        return if (i % 2 == 1) {
            i - 1
        } else i
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
        mCornerShadowPaint.alpha = alpha
        mEdgeShadowPaint.alpha = alpha
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        mDirty = true
    }

    private fun setShadowSize(floatShadowSize: Float, floatMaxShadowSize: Float) {
        var shadowSize = floatShadowSize
        var maxShadowSize = floatMaxShadowSize
        if (shadowSize < 0f) {
            throw IllegalArgumentException("Invalid shadow size " + shadowSize
                    + ". Must be >= 0")
        }
        if (maxShadowSize < 0f) {
            throw IllegalArgumentException("Invalid max shadow size " + maxShadowSize
                    + ". Must be >= 0")
        }
        shadowSize = toEven(shadowSize).toFloat()
        maxShadowSize = toEven(maxShadowSize).toFloat()
        if (shadowSize > maxShadowSize) {
            shadowSize = maxShadowSize
        }
        if (mRawShadowSize == shadowSize && mRawMaxShadowSize == maxShadowSize) {
            return
        }
        mRawShadowSize = shadowSize
        mRawMaxShadowSize = maxShadowSize
        mShadowSize = (shadowSize * SHADOW_MULTIPLIER + mInsetShadow.toFloat() + .5f)
        mDirty = true
        invalidateSelf()
    }

    override fun onStateChange(stateSet: IntArray): Boolean {
        val newColor = mBackground!!.getColorForState(stateSet, mBackground!!.defaultColor)
        if (mPaint.color == newColor) {
            return false
        }
        mPaint.color = newColor
        mDirty = true
        invalidateSelf()
        return true
    }

    override fun isStateful(): Boolean {
        return mBackground != null && mBackground!!.isStateful || super.isStateful()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun draw(canvas: Canvas) {
        if (mDirty) {
            buildComponents(bounds)
            mDirty = false
        }
        if (!isTopDrawn && !isBottomDrawn) {
            canvas.translate(0f, 0f)
        } else {
            canvas.translate(0f, mRawShadowSize / 2)
        }

        if (isTopDrawn) drawTopShadow(canvas)
        if (isBottomDrawn) drawBottomShadow(canvas)
        drawLeftShadow(canvas)
        drawRightShadow(canvas)

        if (!isTopDrawn && !isBottomDrawn) {
            canvas.translate(0f, 0f)
        } else {
            canvas.translate(0f, -mRawShadowSize / 2)
        }
        drawRoundRect(canvas, mCardBounds, mCornerRadius, mPaint)
    }

    private fun drawTopShadow(canvas: Canvas) {
        val edgeShadowTop = -mCornerRadius - mShadowSize
        val inset = mCornerRadius + mInsetShadow.toFloat() + mRawShadowSize / 2
        val drawHorizontalEdges = mCardBounds.width() - 2 * inset > 0
        var saved = canvas.save()
        canvas.translate(mCardBounds.left + inset, mCardBounds.top + inset)
        canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
        if (drawHorizontalEdges) {
            canvas.drawRect(0f, edgeShadowTop,
                    mCardBounds.width() - 2 * inset, -mCornerRadius,
                    mEdgeShadowPaint)
        }
        canvas.restoreToCount(saved)
        saved = canvas.save()
        canvas.translate(mCardBounds.right - inset, mCardBounds.top + inset)
        canvas.rotate(90f)
        canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
        canvas.restoreToCount(saved)
    }

    private fun drawLeftShadow(canvas: Canvas) {
        val edgeShadowTop = -mCornerRadius - mShadowSize
        val inset = mCornerRadius + mInsetShadow.toFloat() + mRawShadowSize / 2
        val drawVerticalEdges = mCardBounds.height() - 2 * inset > 0
        val saved = canvas.save()
        var dy = mCardBounds.bottom
        if (isBottomDrawn) dy = mCardBounds.bottom - inset
        canvas.translate(mCardBounds.left + inset, dy)
        canvas.rotate(270f)
        if (drawVerticalEdges) {
            var right = mCardBounds.height()
            if (isTopDrawn && isBottomDrawn) {
                right = mCardBounds.height() - 2 * inset
            } else if (isTopDrawn) {
                right = mCardBounds.height() - inset
            }
            canvas.drawRect(0f, edgeShadowTop, right, -mCornerRadius, mEdgeShadowPaint)
        }
        canvas.restoreToCount(saved)
    }

    private fun drawRightShadow(canvas: Canvas) {
        val edgeShadowTop = -mCornerRadius - mShadowSize
        val inset = mCornerRadius + mInsetShadow.toFloat() + mRawShadowSize / 2
        val drawVerticalEdges = mCardBounds.height() - 2 * inset > 0
        val saved = canvas.save()
        var dy = mCardBounds.bottom
        if (isBottomDrawn) dy = mCardBounds.bottom - inset
        canvas.translate(mCardBounds.right - inset, dy)
        canvas.rotate(90f)
        if (drawVerticalEdges) {
            var right = mCardBounds.height()
            if (isTopDrawn && isBottomDrawn) {
                right = mCardBounds.height() - 2 * inset
            } else if (isTopDrawn) {
                right = mCardBounds.height() - inset
            }

            canvas.drawRect(0f, edgeShadowTop, -right, -mCornerRadius, mEdgeShadowPaint)
        }
        canvas.restoreToCount(saved)
    }

    private fun drawBottomShadow(canvas: Canvas) {
        val edgeShadowTop = -mCornerRadius - mShadowSize
        val inset = mCornerRadius + mInsetShadow.toFloat() + mRawShadowSize / 2
        val drawHorizontalEdges = mCardBounds.width() - 2 * inset > 0
        var saved = canvas.save()
        canvas.translate(mCardBounds.right - inset, mCardBounds.bottom - inset)
        canvas.rotate(180f)
        canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
        if (drawHorizontalEdges) {
            canvas.drawRect(0f, edgeShadowTop,
                    mCardBounds.width() - 2 * inset, -mCornerRadius + mShadowSize,
                    mEdgeShadowPaint)
        }
        canvas.restoreToCount(saved)
        saved = canvas.save()
        canvas.translate(mCardBounds.left + inset, mCardBounds.bottom - inset)
        canvas.rotate(270f)
        canvas.drawPath(mCornerShadowPath!!, mCornerShadowPaint)
        canvas.restoreToCount(saved)
    }

    private fun drawRoundRect(canvas: Canvas, bounds: RectF, cornerRadius: Float, paint: Paint) {
        val mCornerRect = RectF()
        val twoRadius = cornerRadius * 2
        val innerWidth = bounds.width() - twoRadius - 1f
        val innerHeight = bounds.height() - twoRadius - 1f
        if (cornerRadius >= 1f && (isTopDrawn || isBottomDrawn)) {
            // increment corner radius to account for half pixels.
            val roundedCornerRadius = cornerRadius + .5f
            mCornerRect.set(
                    -roundedCornerRadius, -roundedCornerRadius,
                    roundedCornerRadius, roundedCornerRadius
            )
            val saved = canvas.save()
            if (isTopDrawn) {
                canvas.translate(bounds.left + roundedCornerRadius,
                        bounds.top + roundedCornerRadius)
                canvas.drawArc(mCornerRect, 180f, 90f, true, paint)
                canvas.translate(innerWidth, 0f)
                canvas.rotate(90f)
                canvas.drawArc(mCornerRect, 180f, 90f, true, paint)
            }
            if (!isTopDrawn && isBottomDrawn) {
                canvas.translate(bounds.left + roundedCornerRadius, bounds.top + roundedCornerRadius)
                canvas.translate(innerWidth, 0f)
                canvas.rotate(90f)
            }
            if (isBottomDrawn) {
                canvas.translate(innerHeight, 0f)
                canvas.rotate(90f)
                canvas.drawArc(mCornerRect, 180f, 90f, true, paint)
                canvas.translate(innerWidth, 0f)
                canvas.rotate(90f)
                canvas.drawArc(mCornerRect, 180f, 90f, true, paint)
            }
            canvas.restoreToCount(saved)
            //draw top and bottom pieces
            if (isTopDrawn) {
                canvas.drawRect(bounds.left + roundedCornerRadius - 1f, bounds.top,
                        bounds.right - roundedCornerRadius + 1f,
                        bounds.top + roundedCornerRadius, paint)
            }
            if (isBottomDrawn) {
                canvas.drawRect(bounds.left + roundedCornerRadius - 1f,
                        bounds.bottom - roundedCornerRadius,
                        bounds.right - roundedCornerRadius + 1f, bounds.bottom, paint)
            }
        }

        var top = bounds.top
        var bottom = bounds.bottom
        if (isTopDrawn && isBottomDrawn) {
            top += cornerRadius
            bottom -= cornerRadius
        } else if (!isTopDrawn && isBottomDrawn) {
            bottom -= cornerRadius
        } else if (isTopDrawn && !isBottomDrawn) {
            top += cornerRadius
        }
        canvas.drawRect(bounds.left, top, bounds.right, bottom, paint)
    }

    private fun buildShadowCorners() {
        val innerBounds = RectF(-mCornerRadius, -mCornerRadius, mCornerRadius, mCornerRadius)
        val outerBounds = RectF(innerBounds)
        outerBounds.inset(-mShadowSize, -mShadowSize)

        if (mCornerShadowPath == null) {
            mCornerShadowPath = Path()
        } else {
            mCornerShadowPath!!.reset()
        }
        mCornerShadowPath!!.fillType = Path.FillType.EVEN_ODD
        mCornerShadowPath!!.moveTo(-mCornerRadius, 0f)
        mCornerShadowPath!!.rLineTo(-mShadowSize, 0f)
        // outer arc
        mCornerShadowPath!!.arcTo(outerBounds, 180f, 90f, false)
        // inner arc
        mCornerShadowPath!!.arcTo(innerBounds, 270f, -90f, false)
        mCornerShadowPath!!.close()
        val startRatio = mCornerRadius / (mCornerRadius + mShadowSize)
        mCornerShadowPaint.shader = RadialGradient(
                0f, 0f,
                mCornerRadius + mShadowSize,
                intArrayOf(mShadowStartColor, mShadowStartColor, mShadowEndColor),
                floatArrayOf(0f, startRatio, 1f),
                Shader.TileMode.CLAMP
        )

        // we offset the content shadowSize/2 pixels up to make it more realistic.
        // this is why edge shadow shader has some extra space
        // When drawing bottom edge shadow, we use that extra space.
        mEdgeShadowPaint.shader = LinearGradient(
                0f, -mCornerRadius + mShadowSize,
                0f, -mCornerRadius - mShadowSize,
                intArrayOf(mShadowStartColor, mShadowStartColor, mShadowEndColor),
                floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP
        )
        mEdgeShadowPaint.isAntiAlias = false
    }

    private fun buildComponents(bounds: Rect) {
        // Card is offset SHADOW_MULTIPLIER * maxShadowSize to account for the shadow shift.
        // We could have different top-bottom offsets to avoid extra gap above but in that case
        // center aligning Views inside the CardView would be problematic.
        val verticalOffset = mRawMaxShadowSize * SHADOW_MULTIPLIER
        val left = bounds.left + mRawMaxShadowSize
        var top = bounds.top.toFloat()
        if (isTopDrawn) top = bounds.top + verticalOffset
        val right = bounds.right - mRawMaxShadowSize
        var bottom = bounds.bottom.toFloat()
        if (isBottomDrawn) bottom = bounds.bottom - verticalOffset
        mCardBounds.set(left, top, right, bottom)
        buildShadowCorners()
    }
}
