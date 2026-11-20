package com.masalik.yemen.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.random.Random

class ProceduralTextures {

    val whiteTex: Int
    val roadAsphaltTex: Int
    val crosswalkTex: Int
    val sidewalkStoneTex: Int
    val sanaaFacadeTex: Int
    val taizStoneTex: Int
    val shibamMudTex: Int
    val soukShopTex: Int

    init {
        whiteTex = uploadBitmap(createWhiteBitmap(), repeat = true)
        roadAsphaltTex = uploadBitmap(createAsphaltRoadBitmap(), repeat = true)
        crosswalkTex = uploadBitmap(createCrosswalkBitmap(), repeat = false)
        sidewalkStoneTex = uploadBitmap(createSidewalkBitmap(), repeat = true)
        sanaaFacadeTex = uploadBitmap(createSanaaQamariyaBitmap(), repeat = true)
        taizStoneTex = uploadBitmap(createTaizStoneBitmap(), repeat = true)
        shibamMudTex = uploadBitmap(createShibamMudBitmap(), repeat = true)
        soukShopTex = uploadBitmap(createSoukShopBitmap(), repeat = true)
    }

    private fun uploadBitmap(bitmap: Bitmap, repeat: Boolean): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        val wrap = if (repeat) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bitmap.recycle()
        return id
    }

    private fun createWhiteBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        return bmp
    }

    private fun createAsphaltRoadBitmap(): Bitmap {
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(58, 60, 65))
        val p = Paint()
        val rng = Random(42)

        // Subtle asphalt aggregate speckles & worn tire tracks
        for (i in 0 until 2400) {
            val shade = 46 + rng.nextInt(32)
            p.color = Color.rgb(shade, shade, shade + 2)
            c.drawRect(rng.nextFloat() * w, rng.nextFloat() * h, rng.nextFloat() * w + 2f, rng.nextFloat() * h + 2f, p)
        }
        p.color = Color.argb(38, 22, 24, 28)
        c.drawRect(54f, 0f, 86f, h.toFloat(), p)
        c.drawRect(170f, 0f, 202f, h.toFloat(), p)

        // Crisp white edge shoulder lines
        p.color = Color.rgb(232, 234, 238)
        c.drawRect(12f, 0f, 18f, h.toFloat(), p)
        c.drawRect(w - 18f, 0f, w - 12f, h.toFloat(), p)

        // Dashed Yemeni amber-yellow centerline
        p.color = Color.rgb(245, 190, 48)
        c.drawRect(w * 0.5f - 4f, 16f, w * 0.5f + 4f, 118f, p)
        c.drawRect(w * 0.5f - 4f, 148f, w * 0.5f + 4f, 240f, p)
        return bmp
    }

    private fun createCrosswalkBitmap(): Bitmap {
        val w = 256
        val h = 128
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(54, 56, 62))
        val p = Paint().apply { color = Color.rgb(242, 244, 246) }
        val stripes = 7
        val stripeW = w.toFloat() / (stripes * 2)
        for (i in 0 until stripes) {
            val left = i * stripeW * 2f + stripeW * 0.45f
            c.drawRect(left, 14f, left + stripeW * 1.1f, h - 14f, p)
        }
        return bmp
    }

    private fun createSidewalkBitmap(): Bitmap {
        val w = 128
        val h = 128
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(168, 158, 142))
        val p = Paint().apply {
            color = Color.rgb(122, 114, 102)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        for (y in 0..h step 32) c.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), p)
        for (x in 0..w step 32) c.drawLine(x.toFloat(), 0f, x.toFloat(), h.toFloat(), p)
        // Red & white painted Yemeni city curb edge
        p.style = Paint.Style.FILL
        p.color = Color.rgb(210, 46, 42)
        c.drawRect(0f, 0f, 14f, 64f, p)
        p.color = Color.rgb(242, 242, 242)
        c.drawRect(0f, 64f, 14f, 128f, p)
        return bmp
    }

    private fun createSanaaQamariyaBitmap(): Bitmap {
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // Authentic Old Sana'a burnt terracotta brick
        c.drawColor(Color.rgb(154, 96, 66))
        val p = Paint().apply { isAntiAlias = true }

        // Brick texture courses
        p.color = Color.rgb(134, 80, 54)
        for (y in 0 until h step 14) {
            c.drawRect(0f, y.toFloat(), w.toFloat(), (y + 2).toFloat(), p)
        }

        // White gypsum horizontal friezes (Hizam) & decorative zigzags
        p.color = Color.rgb(244, 240, 232)
        c.drawRect(0f, 10f, w.toFloat(), 26f, p)
        c.drawRect(0f, 134f, w.toFloat(), 146f, p)
        c.drawRect(0f, 242f, w.toFloat(), 256f, p)

        // Two rows of iconic arched Yemeni Qamariya stained-glass windows
        val rowYs = floatArrayOf(46f, 164f)
        val colXs = floatArrayOf(34f, 108f, 182f)
        for (ry in rowYs) {
            for (cx in colXs) {
                // White gypsum outer arch border
                p.color = Color.rgb(246, 242, 236)
                c.drawArc(RectF(cx - 8f, ry - 8f, cx + 48f, ry + 48f), 180f, 180f, true, p)
                c.drawRect(cx - 8f, ry + 20f, cx + 48f, ry + 72f, p)

                // Colored stained glass Qamariya semicircle
                p.color = Color.rgb(24, 96, 158)
                c.drawArc(RectF(cx, ry, cx + 40f, ry + 40f), 180f, 180f, true, p)
                p.color = Color.rgb(235, 178, 38)
                c.drawArc(RectF(cx + 6f, ry + 6f, cx + 34f, ry + 34f), 180f, 180f, true, p)
                p.color = Color.rgb(198, 42, 46)
                c.drawCircle(cx + 20f, ry + 18f, 6f, p)

                // Carved dark cedar wooden lower window shutters
                p.color = Color.rgb(58, 38, 26)
                c.drawRect(cx + 2f, ry + 24f, cx + 38f, ry + 64f, p)
                p.color = Color.rgb(242, 198, 92)
                c.drawRect(cx + 7f, ry + 30f, cx + 17f, ry + 56f, p)
                c.drawRect(cx + 23f, ry + 30f, cx + 33f, ry + 56f, p)
            }
        }
        return bmp
    }

    private fun createTaizStoneBitmap(): Bitmap {
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(184, 164, 134))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(77)

        // Dressed Yemeni highland ashlar stone blocks (Taiz & Ibb style)
        for (row in 0 until 8) {
            val y = row * 32f
            val offset = if (row % 2 == 0) 0f else 24f
            for (col in -1..5) {
                val x = col * 48f + offset
                val shade = rng.nextInt(-18, 18)
                p.color = Color.rgb(
                    (182 + shade).coerceIn(110, 230),
                    (162 + shade).coerceIn(100, 215),
                    (132 + shade).coerceIn(85, 190)
                )
                c.drawRect(x + 2f, y + 2f, x + 46f, y + 30f, p)
            }
        }

        // Traditional arched Yemeni stone windows with warm interior light
        val colXs = floatArrayOf(42f, 152f)
        val rowYs = floatArrayOf(44f, 156f)
        for (ry in rowYs) {
            for (cx in colXs) {
                p.color = Color.rgb(240, 236, 226)
                c.drawArc(RectF(cx - 5f, ry - 5f, cx + 55f, ry + 55f), 180f, 180f, true, p)
                c.drawRect(cx - 5f, ry + 25f, cx + 55f, ry + 78f, p)
                p.color = Color.rgb(52, 46, 42)
                c.drawArc(RectF(cx, ry, cx + 50f, ry + 50f), 180f, 180f, true, p)
                c.drawRect(cx, ry + 25f, cx + 50f, ry + 72f, p)
                p.color = Color.rgb(246, 206, 114)
                c.drawRect(cx + 8f, ry + 32f, cx + 42f, ry + 64f, p)
            }
        }
        return bmp
    }

    private fun createShibamMudBitmap(): Bitmap {
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // Warm Hadramout sun-baked adobe mudbrick
        c.drawColor(Color.rgb(196, 158, 112))
        val p = Paint().apply { isAntiAlias = true }

        // Bright white lime-plaster upper crown characteristic of Shibam towers
        p.color = Color.rgb(245, 240, 228)
        c.drawRect(0f, 0f, w.toFloat(), 58f, p)

        // Small recessed wooden desert windows arranged neatly across stories
        p.color = Color.rgb(74, 48, 30)
        for (row in 0..3) {
            val y = 76f + row * 46f
            for (col in 0..3) {
                val x = 22f + col * 60f
                c.drawRect(x, y, x + 24f, y + 28f, p)
                p.color = Color.rgb(232, 224, 204)
                c.drawRect(x - 3f, y - 4f, x + 27f, y, p)
                p.color = Color.rgb(74, 48, 30)
            }
        }
        return bmp
    }

    private fun createSoukShopBitmap(): Bitmap {
        val w = 256
        val h = 128
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(174, 150, 120))
        val p = Paint()

        // Colorful Yemeni souk awning stripes at top
        for (i in 0 until 8) {
            p.color = if (i % 2 == 0) Color.rgb(196, 46, 36) else Color.rgb(246, 236, 212)
            c.drawRect(i * 32f, 0f, (i + 1) * 32f, 34f, p)
        }
        // Two warm lit shop archways
        p.color = Color.rgb(42, 30, 22)
        c.drawRect(22f, 44f, 112f, 124f, p)
        c.drawRect(144f, 44f, 234f, 124f, p)
        p.color = Color.rgb(244, 198, 98)
        c.drawRect(32f, 54f, 102f, 102f, p)
        c.drawRect(154f, 54f, 224f, 102f, p)
        return bmp
    }
}
