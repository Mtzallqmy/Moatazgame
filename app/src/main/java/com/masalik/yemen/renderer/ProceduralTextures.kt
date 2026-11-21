package com.masalik.yemen.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-Definition Procedural Texture Suite (1024px roads, 512px architectural facades & sport alloy rims)
 * with runtime 8x Anisotropic Filtering for razor-sharp street markings and crisp Yemeni city details.
 */
class ProceduralTextures {

    val whiteTex: Int
    val roadAsphaltTex: Int
    val modernBoulevardTex: Int
    val crosswalkTex: Int
    val sidewalkStoneTex: Int
    val sanaaFacadeTex: Int
    val taizStoneTex: Int
    val shibamMudTex: Int
    val soukShopTex: Int
    val modernTowerTex: Int
    val modernMallTex: Int
    val adenCoastalTowerTex: Int
    val heritageMosqueTex: Int
    val curveChevronTex: Int
    val alloyRimTex: Int
    val contactShadowTex: Int

    private val maxAnisotropy: Float

    init {
        maxAnisotropy = queryMaxAnisotropy()
        whiteTex = uploadBitmap(createWhiteBitmap(), repeat = true)
        roadAsphaltTex = uploadBitmap(createAsphaltRoadBitmap(isBoulevard = false), repeat = true)
        modernBoulevardTex = uploadBitmap(createAsphaltRoadBitmap(isBoulevard = true), repeat = true)
        crosswalkTex = uploadBitmap(createCrosswalkBitmap(), repeat = false)
        sidewalkStoneTex = uploadBitmap(createSidewalkBitmap(), repeat = true)
        sanaaFacadeTex = uploadBitmap(createSanaaQamariyaBitmap(), repeat = true)
        taizStoneTex = uploadBitmap(createTaizStoneBitmap(), repeat = true)
        shibamMudTex = uploadBitmap(createShibamMudBitmap(), repeat = true)
        soukShopTex = uploadBitmap(createSoukShopBitmap(), repeat = true)
        modernTowerTex = uploadBitmap(createModernGlassTowerBitmap(), repeat = true)
        modernMallTex = uploadBitmap(createModernMallStorefrontBitmap(), repeat = true)
        adenCoastalTowerTex = uploadBitmap(createAdenCoastalTowerBitmap(), repeat = true)
        heritageMosqueTex = uploadBitmap(createHeritageMosqueBitmap(), repeat = true)
        curveChevronTex = uploadBitmap(createCurveChevronBitmap(), repeat = false)
        alloyRimTex = uploadBitmap(createAlloyRimBitmap(), repeat = false)
        contactShadowTex = uploadBitmap(createContactShadowBitmap(), repeat = false)
    }

    private fun queryMaxAnisotropy(): Float {
        return runCatching {
            val exts = GLES30.glGetString(GLES30.GL_EXTENSIONS) ?: ""
            if (exts.contains("GL_EXT_texture_filter_anisotropic")) {
                val out = FloatArray(1)
                GLES30.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, out, 0)
                out[0].coerceIn(1f, 8f)
            } else 1f
        }.getOrDefault(1f)
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
        if (maxAnisotropy > 1.0f) {
            GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy)
        }
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

    /** 1024x1024 Ultra-Sharp Asphalt Road Surface with micro-aggregate, rumble strips & reflective cat's-eyes */
    private fun createAsphaltRoadBitmap(isBoulevard: Boolean): Bitmap {
        val w = 1024
        val h = 1024
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(if (isBoulevard) Color.rgb(40, 44, 51) else Color.rgb(49, 52, 58))
        val p = Paint()
        val rng = Random(if (isBoulevard) 2027 else 142)

        // Fine high-density asphalt mineral aggregate speckle
        for (i in 0 until 22000) {
            val shade = (if (isBoulevard) 30 else 36) + rng.nextInt(36)
            p.color = Color.rgb(shade, shade, shade + 4)
            val rx = rng.nextFloat() * w
            val ry = rng.nextFloat() * h
            val sz = 1.6f + rng.nextFloat() * 2.2f
            c.drawRect(rx, ry, rx + sz, ry + sz, p)
        }
        // Subtle darker rubber tire-wear paths along every lane
        p.color = Color.argb(52, 12, 14, 18)
        if (isBoulevard) {
            c.drawRect(110f, 0f, 200f, h.toFloat(), p)
            c.drawRect(310f, 0f, 400f, h.toFloat(), p)
            c.drawRect(624f, 0f, 714f, h.toFloat(), p)
            c.drawRect(824f, 0f, 914f, h.toFloat(), p)
        } else {
            c.drawRect(170f, 0f, 315f, h.toFloat(), p)
            c.drawRect(709f, 0f, 854f, h.toFloat(), p)
        }

        // Subtle longitudinal asphalt seam
        p.color = Color.argb(40, 14, 16, 20)
        c.drawRect(w * 0.5f - 2f, 0f, w * 0.5f + 2f, h.toFloat(), p)

        // Crisp shoulder rumble notches + high-visibility reflective white edge lines
        p.color = Color.rgb(28, 30, 35)
        for (y in 0 until h step 32) {
            c.drawRect(6f, y.toFloat(), 26f, y + 12f, p)
            c.drawRect(w - 26f, y.toFloat(), w - 6f, y + 12f, p)
        }
        p.color = Color.rgb(244, 246, 250)
        c.drawRect(32f, 0f, 50f, h.toFloat(), p)
        c.drawRect(w - 50f, 0f, w - 32f, h.toFloat(), p)

        if (isBoulevard) {
            // Double golden-amber center median lines
            p.color = Color.rgb(252, 194, 34)
            c.drawRect(w * 0.5f - 20f, 0f, w * 0.5f - 7f, h.toFloat(), p)
            c.drawRect(w * 0.5f + 7f, 0f, w * 0.5f + 20f, h.toFloat(), p)

            // Crisp dashed white 4-lane dividers
            p.color = Color.rgb(238, 242, 248)
            for (yOff in intArrayOf(48, 560)) {
                c.drawRect(w * 0.25f - 6f, yOff.toFloat(), w * 0.25f + 6f, yOff + 290f, p)
                c.drawRect(w * 0.75f - 6f, yOff.toFloat(), w * 0.75f + 6f, yOff + 290f, p)
            }
            // Reflective cat's-eye road studs (amber center + crisp white lanes)
            for (yStud in intArrayOf(220, 732)) {
                p.color = Color.rgb(255, 234, 118)
                c.drawRect(w * 0.5f - 6f, yStud.toFloat(), w * 0.5f + 6f, yStud + 18f, p)
                p.color = Color.rgb(225, 245, 255)
                c.drawRect(w * 0.25f - 5f, yStud + 180f, w * 0.25f + 5f, yStud + 194f, p)
                c.drawRect(w * 0.75f - 5f, yStud + 180f, w * 0.75f + 5f, yStud + 194f, p)
            }
        } else {
            // Bold Yemeni highland highway amber dashed centerline + reflective center studs
            p.color = Color.rgb(252, 196, 44)
            c.drawRect(w * 0.5f - 11f, 52f, w * 0.5f + 11f, 435f, p)
            c.drawRect(w * 0.5f - 11f, 565f, w * 0.5f + 11f, 948f, p)
            p.color = Color.rgb(255, 240, 150)
            c.drawRect(w * 0.5f - 6f, 492f, w * 0.5f + 6f, 508f, p)
        }
        return bmp
    }

    private fun createCrosswalkBitmap(): Bitmap {
        val w = 512
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(42, 45, 52))
        val p = Paint().apply { color = Color.rgb(248, 250, 252) }
        val stripes = 8
        val stripeW = w.toFloat() / (stripes * 2)
        for (i in 0 until stripes) {
            val left = i * stripeW * 2f + stripeW * 0.40f
            c.drawRect(left, 22f, left + stripeW * 1.18f, h - 24f, p)
        }
        c.drawRect(10f, h - 16f, w - 10f, h - 2f, p)
        return bmp
    }

    private fun createSidewalkBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(186, 178, 166))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(512)

        // Interlocking stone paver tiles with subtle warm/cool tone variation
        val tile = 64
        for (y in 0 until h step tile) {
            for (x in 0 until w step tile) {
                val shift = rng.nextInt(-12, 12)
                val isAccent = ((x / tile) + (y / tile)) % 4 == 0
                p.style = Paint.Style.FILL
                p.color = if (isAccent) {
                    Color.rgb(164 + shift, 142 + shift, 124 + shift)
                } else {
                    Color.rgb(188 + shift, 180 + shift, 168 + shift)
                }
                c.drawRect(x + 2f, y + 2f, x + tile - 2f, y + tile - 2f, p)
            }
        }
        // Crisp red & ivory urban painted curbstone
        p.style = Paint.Style.FILL
        p.color = Color.rgb(218, 42, 42)
        c.drawRect(0f, 0f, 38f, 256f, p)
        p.color = Color.rgb(248, 248, 250)
        c.drawRect(0f, 256f, 38f, 512f, p)
        // Curb bevel shadow
        p.color = Color.argb(60, 15, 18, 24)
        c.drawRect(38f, 0f, 46f, 512f, p)
        return bmp
    }

    private fun createModernGlassTowerBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(220, 212, 198))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(2026)

        val rows = 6
        val cols = 5
        val rowH = h.toFloat() / rows
        val colW = w.toFloat() / cols

        for (r in 0 until rows) {
            val top = r * rowH + 10f
            val bot = (r + 1) * rowH - 7f
            p.shader = null
            p.color = Color.rgb(34, 42, 54)
            c.drawRect(0f, r * rowH, w.toFloat(), top, p)
            // Brushed gold aluminum horizontal trim line
            p.color = Color.rgb(216, 174, 86)
            c.drawRect(0f, top - 2.5f, w.toFloat(), top, p)

            for (col in 0 until cols) {
                val left = col * colW + 7f
                val right = (col + 1) * colW - 7f
                val litOffice = rng.nextFloat() > 0.42f
                p.shader = LinearGradient(
                    left, top, right, bot,
                    if (litOffice) Color.rgb(252, 222, 146) else Color.rgb(32, 86, 138),
                    if (litOffice) Color.rgb(190, 148, 84) else Color.rgb(14, 40, 74),
                    Shader.TileMode.CLAMP
                )
                c.drawRect(left, top, right, bot, p)
                p.shader = null
                p.color = Color.argb(58, 232, 248, 255)
                p.strokeWidth = 4.5f
                c.drawLine(left + 6f, bot - 8f, right - 6f, top + 8f, p)
            }
        }
        return bmp
    }

    /** 512x512 Sleek Coastal Aden / Mokha White-Limestone & Turquoise Glass Resort Tower */
    private fun createAdenCoastalTowerBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(242, 240, 234))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(3031)

        val rows = 5
        val rowH = h.toFloat() / rows
        for (r in 0 until rows) {
            val y0 = r * rowH
            // Sleek white cantilevered balcony slab
            p.shader = null
            p.color = Color.rgb(246, 246, 244)
            c.drawRect(0f, y0 + rowH - 22f, w.toFloat(), y0 + rowH, p)
            p.color = Color.rgb(16, 148, 168)
            c.drawRect(0f, y0 + rowH - 26f, w.toFloat(), y0 + rowH - 21f, p)

            // Panoramic sea-blue reflective windows
            for (col in 0 until 4) {
                val left = col * 128f + 10f
                val right = (col + 1) * 128f - 10f
                val warmRoom = rng.nextFloat() > 0.55f
                p.shader = LinearGradient(
                    left, y0 + 10f, right, y0 + rowH - 28f,
                    if (warmRoom) Color.rgb(248, 214, 140) else Color.rgb(22, 132, 168),
                    if (warmRoom) Color.rgb(172, 134, 78) else Color.rgb(12, 68, 102),
                    Shader.TileMode.CLAMP
                )
                c.drawRect(left, y0 + 10f, right, y0 + rowH - 28f, p)
            }
            p.shader = null
            // Glass balcony railing glint
            p.color = Color.argb(95, 180, 238, 255)
            c.drawRect(6f, y0 + rowH - 42f, w - 6f, y0 + rowH - 24f, p)
        }
        return bmp
    }

    /** 512x512 Ornate Cream-Stone Yemeni Mosque Facade with Pointed Islamic Arches & Gold Frieze */
    private fun createHeritageMosqueBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(236, 226, 206))
        val p = Paint().apply { isAntiAlias = true }

        // Warm sculpted limestone courses
        p.color = Color.rgb(218, 206, 184)
        for (y in 0 until h step 32) {
            c.drawRect(0f, y.toFloat(), w.toFloat(), y + 2.5f, p)
        }
        // Ornate emerald & gold geometric frieze band
        p.color = Color.rgb(20, 106, 78)
        c.drawRect(0f, 34f, w.toFloat(), 66f, p)
        p.color = Color.rgb(242, 192, 62)
        c.drawRect(0f, 28f, w.toFloat(), 34f, p)
        c.drawRect(0f, 66f, w.toFloat(), 72f, p)
        for (x in 16 until w step 32) {
            c.drawCircle(x.toFloat(), 50f, 6f, p)
        }

        // Three grand pointed Yemeni mosque arched windows
        for (col in 0 until 3) {
            val cx = 86f + col * 170f
            p.color = Color.rgb(248, 244, 234)
            c.drawArc(RectF(cx - 56f, 130f, cx + 56f, 248f), 180f, 180f, true, p)
            c.drawRect(cx - 56f, 188f, cx + 56f, 456f, p)
            p.color = Color.rgb(34, 68, 92)
            c.drawArc(RectF(cx - 44f, 144f, cx + 44f, 240f), 180f, 180f, true, p)
            c.drawRect(cx - 44f, 192f, cx + 44f, 446f, p)
            // Warm glowing stained-glass sanctuary interior
            p.shader = LinearGradient(cx, 150f, cx, 440f, Color.rgb(252, 212, 104), Color.rgb(176, 114, 38), Shader.TileMode.CLAMP)
            c.drawArc(RectF(cx - 34f, 154f, cx + 34f, 230f), 180f, 180f, true, p)
            c.drawRect(cx - 34f, 194f, cx + 34f, 434f, p)
            p.shader = null
        }
        return bmp
    }

    private fun createModernMallStorefrontBitmap(): Bitmap {
        val w = 512
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(26, 32, 42))
        val p = Paint().apply { isAntiAlias = true }

        // Crisp illuminated gold + emerald + cyan modern Arabic boulevard signage bands
        p.color = Color.rgb(250, 174, 24)
        c.drawRect(0f, 14f, w.toFloat(), 28f, p)
        p.color = Color.rgb(18, 196, 152)
        c.drawRect(22f, 40f, 224f, 76f, p)
        p.color = Color.rgb(248, 196, 62)
        c.drawRect(272f, 40f, 490f, 76f, p)
        // Bright white neon emblem bars inside signs
        p.color = Color.rgb(255, 255, 255)
        c.drawRect(52f, 52f, 194f, 64f, p)
        c.drawRect(306f, 52f, 456f, 64f, p)

        // Bright warm floor-to-ceiling café & showroom glass
        p.shader = LinearGradient(0f, 92f, 0f, 246f, Color.rgb(255, 236, 176), Color.rgb(162, 118, 62), Shader.TileMode.CLAMP)
        c.drawRect(22f, 92f, 238f, 246f, p)
        c.drawRect(274f, 92f, 490f, 246f, p)
        p.shader = null
        p.color = Color.rgb(24, 28, 36)
        p.strokeWidth = 6f
        c.drawLine(130f, 92f, 130f, 246f, p)
        c.drawLine(382f, 92f, 382f, 246f, p)
        return bmp
    }

    private fun createCurveChevronBitmap(): Bitmap {
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(252, 200, 18))
        val p = Paint().apply {
            color = Color.rgb(16, 18, 22)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val path = Path().apply {
            moveTo(56f, 26f)
            lineTo(136f, 26f)
            lineTo(212f, 128f)
            lineTo(136f, 230f)
            lineTo(56f, 230f)
            lineTo(132f, 128f)
            close()
        }
        c.drawPath(path, p)
        return bmp
    }

    /** 512x512 Ultra-Detailed Diamond-Cut Twin-Spoke Alloy Rim + Cross-Drilled Brake Rotor + Brembo Red Caliper */
    private fun createAlloyRimBitmap(): Bitmap {
        val sz = 512
        val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = sz * 0.5f
        val cy = sz * 0.5f
        val p = Paint().apply { isAntiAlias = true }

        // Deep black radial rubber sidewall with subtle tread & rim-guard ring
        c.drawColor(Color.rgb(20, 22, 26))
        p.style = Paint.Style.STROKE
        p.color = Color.rgb(34, 37, 44)
        p.strokeWidth = 14f
        c.drawCircle(cx, cy, 222f, p)
        p.color = Color.rgb(46, 50, 58)
        p.strokeWidth = 5f
        c.drawCircle(cx, cy, 198f, p)

        // Ventilated steel brake disc rotor inside rim
        p.style = Paint.Style.FILL
        p.shader = RadialGradient(cx, cy, 162f, Color.rgb(122, 128, 138), Color.rgb(68, 72, 80), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, 160f, p)
        p.shader = null

        // Cross-drilled rotor cooling holes
        p.color = Color.rgb(28, 30, 36)
        for (i in 0 until 12) {
            val a = i * (2.0 * PI / 12.0)
            c.drawCircle(cx + (cos(a) * 114f).toFloat(), cy + (sin(a) * 114f).toFloat(), 4.5f, p)
            c.drawCircle(cx + (cos(a + 0.14) * 136f).toFloat(), cy + (sin(a + 0.14) * 136f).toFloat(), 4.5f, p)
        }

        // High-performance Gloss Red Brake Caliper
        p.color = Color.rgb(228, 32, 36)
        c.drawArc(RectF(cx - 148f, cy - 148f, cx + 148f, cy + 148f), -34f, 54f, true, p)

        // Diamond-Cut Chrome Outer Rim Lip
        p.style = Paint.Style.STROKE
        p.strokeWidth = 22f
        p.color = Color.rgb(228, 234, 242)
        c.drawCircle(cx, cy, 168f, p)

        // 5 Machined Twin-Spokes with dark inner shadows
        for (i in 0 until 5) {
            val baseAng = i * (2.0 * PI / 5.0)
            for (off in doubleArrayOf(-0.15, 0.15)) {
                val a = baseAng + off
                val sx = cx + (cos(a) * 40f).toFloat()
                val sy = cy + (sin(a) * 40f).toFloat()
                val ex = cx + (cos(a) * 164f).toFloat()
                val ey = cy + (sin(a) * 164f).toFloat()
                p.strokeWidth = 22f
                p.color = Color.rgb(66, 72, 82)
                c.drawLine(sx, sy, ex, ey, p)
                p.strokeWidth = 14f
                p.color = Color.rgb(226, 232, 240)
                c.drawLine(sx, sy, ex, ey, p)
            }
        }
        // Center Hub + 5 Chrome Lug Nuts + Gold Emblem
        p.style = Paint.Style.FILL
        p.color = Color.rgb(28, 32, 40)
        c.drawCircle(cx, cy, 48f, p)
        p.color = Color.rgb(212, 220, 230)
        for (i in 0 until 5) {
            val a = i * (2.0 * PI / 5.0) + 0.31
            c.drawCircle(cx + (cos(a) * 32f).toFloat(), cy + (sin(a) * 32f).toFloat(), 6f, p)
        }
        p.color = Color.rgb(248, 174, 24)
        c.drawCircle(cx, cy, 18f, p)
        return bmp
    }

    private fun createContactShadowBitmap(): Bitmap {
        val sz = 256
        val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                sz * 0.5f, sz * 0.5f, sz * 0.48f,
                intArrayOf(Color.argb(225, 4, 6, 10), Color.argb(155, 4, 6, 10), Color.argb(0, 4, 6, 10)),
                floatArrayOf(0.0f, 0.62f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        c.drawOval(RectF(14f, 8f, sz - 14f, sz - 8f), p)
        return bmp
    }

    /** 512x512 High-Definition Old Sana'a Baked-Brick Facade with Detailed Stained-Glass Qamariyas */
    private fun createSanaaQamariyaBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(162, 98, 64))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(911)

        // Authentic Sana'ani baked-brick coursing
        for (y in 0 until h step 16) {
            val shade = rng.nextInt(-10, 10)
            p.color = Color.rgb(154 + shade, 92 + shade, 60 + shade)
            c.drawRect(0f, y.toFloat(), w.toFloat(), (y + 13).toFloat(), p)
            p.color = Color.rgb(126, 74, 48)
            c.drawRect(0f, (y + 13).toFloat(), w.toFloat(), (y + 16).toFloat(), p)
        }
        // Iconic white gypsum Qadad horizontal geometric friezes (Hizam)
        p.color = Color.rgb(248, 244, 236)
        c.drawRect(0f, 18f, w.toFloat(), 48f, p)
        c.drawRect(0f, 264f, w.toFloat(), 290f, p)
        c.drawRect(0f, 484f, w.toFloat(), 512f, p)
        // Sawtooth gypsum ornament teeth
        for (x in 0 until w step 24) {
            c.drawRect(x + 4f, 48f, x + 16f, 58f, p)
            c.drawRect(x + 4f, 290f, x + 16f, 300f, p)
        }

        val rowYs = floatArrayOf(88f, 326f)
        val colXs = floatArrayOf(66f, 214f, 362f)
        for (ry in rowYs) {
            for (cx in colXs) {
                // Gypsum white hood frame
                p.color = Color.rgb(250, 246, 240)
                c.drawArc(RectF(cx - 16f, ry - 16f, cx + 96f, ry + 96f), 180f, 180f, true, p)
                c.drawRect(cx - 16f, ry + 40f, cx + 96f, ry + 144f, p)
                // Multi-colored Yemeni stained-glass Qamariya fanlight
                p.color = Color.rgb(18, 108, 176)
                c.drawArc(RectF(cx, ry, cx + 80f, ry + 80f), 180f, 180f, true, p)
                p.color = Color.rgb(245, 184, 34)
                c.drawArc(RectF(cx + 12f, ry + 12f, cx + 68f, ry + 68f), 180f, 180f, true, p)
                p.color = Color.rgb(24, 154, 104)
                c.drawArc(RectF(cx + 22f, ry + 22f, cx + 58f, ry + 58f), 180f, 180f, true, p)
                p.color = Color.rgb(214, 42, 46)
                c.drawCircle(cx + 40f, ry + 36f, 11f, p)
                // Carved wooden lattice window shutters + warm golden interior glow
                p.color = Color.rgb(54, 34, 22)
                c.drawRect(cx + 4f, ry + 48f, cx + 76f, ry + 130f, p)
                p.color = Color.rgb(248, 206, 102)
                c.drawRect(cx + 14f, ry + 58f, cx + 34f, ry + 114f, p)
                c.drawRect(cx + 46f, ry + 58f, cx + 66f, ry + 114f, p)
            }
        }
        return bmp
    }

    /** 512x512 High-Definition Taiz / Ibb Carved Highland Stone Masonry */
    private fun createTaizStoneBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(154, 140, 118))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(777)

        for (row in 0 until 12) {
            val y = row * 42.6f
            val offset = if (row % 2 == 0) 0f else 36f
            for (col in -1..7) {
                val x = col * 72f + offset
                val shade = rng.nextInt(-22, 22)
                val warmBlock = (row + col) % 3 == 0
                p.color = if (warmBlock) {
                    Color.rgb((194 + shade).coerceIn(120, 236), (172 + shade).coerceIn(110, 220), (138 + shade).coerceIn(90, 195))
                } else {
                    Color.rgb((176 + shade).coerceIn(110, 224), (162 + shade).coerceIn(100, 212), (144 + shade).coerceIn(90, 196))
                }
                c.drawRect(x + 3f, y + 3f, x + 69f, y + 39.5f, p)
            }
        }

        val colXs = floatArrayOf(82f, 304f)
        val rowYs = floatArrayOf(84f, 308f)
        for (ry in rowYs) {
            for (cx in colXs) {
                p.color = Color.rgb(246, 242, 234)
                c.drawArc(RectF(cx - 10f, ry - 10f, cx + 110f, ry + 110f), 180f, 180f, true, p)
                c.drawRect(cx - 10f, ry + 50f, cx + 110f, ry + 156f, p)
                p.color = Color.rgb(44, 40, 38)
                c.drawArc(RectF(cx, ry, cx + 100f, ry + 100f), 180f, 180f, true, p)
                c.drawRect(cx, ry + 50f, cx + 100f, ry + 144f, p)
                p.color = Color.rgb(248, 210, 118)
                c.drawRect(cx + 16f, ry + 62f, cx + 84f, ry + 128f, p)
            }
        }
        return bmp
    }

    /** 512x512 High-Definition Shibam Hadramout Mudbrick Skyscraper Facade */
    private fun createShibamMudBitmap(): Bitmap {
        val w = 512
        val h = 512
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(202, 162, 116))
        val p = Paint().apply { isAntiAlias = true }
        val rng = Random(444)

        // Subtle organic sun-baked mud plaster striations
        for (i in 0 until 2400) {
            val s = rng.nextInt(-14, 14)
            p.color = Color.rgb(198 + s, 158 + s, 112 + s)
            val rx = rng.nextFloat() * w
            val ry = rng.nextFloat() * h
            c.drawRect(rx, ry, rx + 12f, ry + 4f, p)
        }
        // Iconic lime-whitewashed upper defensive crown
        p.color = Color.rgb(248, 244, 232)
        c.drawRect(0f, 0f, w.toFloat(), 112f, p)

        // Carved Hadrami wooden windows with white lintel crowns
        for (row in 0..3) {
            val y = 148f + row * 92f
            for (col in 0..3) {
                val x = 42f + col * 120f
                p.color = Color.rgb(238, 230, 212)
                c.drawRect(x - 6f, y - 8f, x + 54f, y, p)
                p.color = Color.rgb(66, 42, 24)
                c.drawRect(x, y, x + 48f, y + 56f, p)
                p.color = Color.rgb(240, 192, 92)
                c.drawRect(x + 10f, y + 10f, x + 38f, y + 46f, p)
            }
        }
        return bmp
    }

    private fun createSoukShopBitmap(): Bitmap {
        val w = 512
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(184, 156, 122))
        val p = Paint().apply { isAntiAlias = true }
        // Crisp striped Yemeni souk awning
        for (i in 0 until 8) {
            p.color = if (i % 2 == 0) Color.rgb(208, 44, 38) else Color.rgb(250, 240, 218)
            c.drawRect(i * 64f, 0f, (i + 1) * 64f, 68f, p)
        }
        p.color = Color.rgb(38, 28, 20)
        c.drawRect(40f, 88f, 224f, 248f, p)
        c.drawRect(288f, 88f, 472f, 248f, p)
        p.color = Color.rgb(248, 204, 102)
        c.drawRect(60f, 106f, 204f, 208f, p)
        c.drawRect(308f, 106f, 452f, 208f, p)
        return bmp
    }

    companion object {
        private const val GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE
        private const val GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF
    }
}
