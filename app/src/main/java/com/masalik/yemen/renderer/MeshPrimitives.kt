package com.masalik.yemen.renderer

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GpuMesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 8 // pos(3) + normal(3) + uv(2)
    private val vboId: Int

    init {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]
        val buf: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buf.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (vertexCount <= 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
    }
}

class DynamicMesh {
    private val vboId: Int
    var vertexCount: Int = 0
        private set

    init {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        vboId = ids[0]
    }

    fun upload(vertices: FloatArray) {
        vertexCount = vertices.size / 8
        if (vertexCount == 0) return
        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        buf.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (vertexCount <= 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }
}

object MeshPrimitives {

    private fun pushQuad(
        v: MutableList<Float>,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float
    ) {
        val ux = x1 - x0; val uy = y1 - y0; val uz = z1 - z0
        val vx = x3 - x0; val vy = y3 - y0; val vz = z3 - z0
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        nx /= len; ny /= len; nz /= len

        v.addAll(listOf(x0, y0, z0, nx, ny, nz, 0f, 1f))
        v.addAll(listOf(x1, y1, z1, nx, ny, nz, 1f, 1f))
        v.addAll(listOf(x2, y2, z2, nx, ny, nz, 1f, 0f))

        v.addAll(listOf(x0, y0, z0, nx, ny, nz, 0f, 1f))
        v.addAll(listOf(x2, y2, z2, nx, ny, nz, 1f, 0f))
        v.addAll(listOf(x3, y3, z3, nx, ny, nz, 0f, 0f))
    }

    fun createUnitCube(): GpuMesh {
        val v = mutableListOf<Float>()
        val h = 0.5f
        pushQuad(v, -h, -h, h, h, -h, h, h, h, h, -h, h, h)
        pushQuad(v, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h)
        pushQuad(v, h, -h, h, h, -h, -h, h, h, -h, h, h, h)
        pushQuad(v, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h)
        pushQuad(v, -h, h, h, h, h, h, h, h, -h, -h, h, -h)
        pushQuad(v, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h)
        return GpuMesh(v.toFloatArray())
    }

    /** Aerodynamic sloped greenhouse cabin prism (sloped windshield, tapered pillars, fastback rear glass). */
    fun createTrapezoidCabin(): GpuMesh {
        val v = mutableListOf<Float>()
        val bx = 0.50f; val bz = 0.50f
        val tx = 0.40f; val tzFront = 0.28f; val tzRear = -0.34f
        val y0 = -0.5f; val y1 = 0.5f
        // Front sloped windshield (+Z)
        pushQuad(v, -bx, y0, bz, bx, y0, bz, tx, y1, tzFront, -tx, y1, tzFront)
        // Rear sloped window (-Z)
        pushQuad(v, bx, y0, -bz, -bx, y0, -bz, -tx, y1, tzRear, tx, y1, tzRear)
        // Right side tumblehome (+X)
        pushQuad(v, bx, y0, bz, bx, y0, -bz, tx, y1, tzRear, tx, y1, tzFront)
        // Left side tumblehome (-X)
        pushQuad(v, -bx, y0, -bz, -bx, y0, bz, -tx, y1, tzFront, -tx, y1, tzRear)
        // Roof (+Y)
        pushQuad(v, -tx, y1, tzFront, tx, y1, tzFront, tx, y1, tzRear, -tx, y1, tzRear)
        // Bottom (-Y)
        pushQuad(v, -bx, y0, -bz, bx, y0, -bz, bx, y0, bz, -bx, y0, bz)
        return GpuMesh(v.toFloatArray())
    }

    /** Sculpted lower automotive body wedge with sleek aerodynamic hood drop & beveled bumper nose. */
    fun createSlopedBody(): GpuMesh {
        val v = mutableListOf<Float>()
        val bx = 0.50f; val bz = 0.50f
        val tx = 0.46f; val tzFront = 0.46f; val tzRear = -0.47f
        val y0 = -0.5f; val yFrontTop = 0.34f; val yRearTop = 0.50f
        // Front bumper fascia (+Z)
        pushQuad(v, -bx, y0, bz, bx, y0, bz, tx, yFrontTop, tzFront, -tx, yFrontTop, tzFront)
        // Rear bumper fascia (-Z)
        pushQuad(v, bx, y0, -bz, -bx, y0, -bz, -tx, yRearTop, tzRear, tx, yRearTop, tzRear)
        // Right flank (+X)
        pushQuad(v, bx, y0, bz, bx, y0, -bz, tx, yRearTop, tzRear, tx, yFrontTop, tzFront)
        // Left flank (-X)
        pushQuad(v, -bx, y0, -bz, -bx, y0, bz, -tx, yFrontTop, tzFront, -tx, yRearTop, tzRear)
        // Sloped Hood & Beltline (+Y)
        pushQuad(v, -tx, yFrontTop, tzFront, tx, yFrontTop, tzFront, tx, yRearTop, tzRear, -tx, yRearTop, tzRear)
        // Underbody (-Y)
        pushQuad(v, -bx, y0, -bz, bx, y0, -bz, bx, y0, bz, -bx, y0, bz)
        return GpuMesh(v.toFloatArray())
    }

    fun createUnitQuadXZ(): GpuMesh {
        val h = 0.5f
        val arr = floatArrayOf(
            -h, 0f, h, 0f, 1f, 0f, 0f, 1f,
            h, 0f, h, 0f, 1f, 0f, 1f, 1f,
            h, 0f, -h, 0f, 1f, 0f, 1f, 0f,
            -h, 0f, h, 0f, 1f, 0f, 0f, 1f,
            h, 0f, -h, 0f, 1f, 0f, 1f, 0f,
            -h, 0f, -h, 0f, 1f, 0f, 0f, 0f
        )
        return GpuMesh(arr)
    }

    /** High-res 24-segment automotive wheel cylinder with UV-mapped side caps for alloy rim textures. */
    fun createWheelCylinder(segments: Int = 24): GpuMesh {
        val v = mutableListOf<Float>()
        val halfW = 0.5f
        val r = 0.5f
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val y0 = cos(a0) * r; val z0 = sin(a0) * r
            val y1 = cos(a1) * r; val z1 = sin(a1) * r
            val ny0 = cos(a0); val nz0 = sin(a0)
            val ny1 = cos(a1); val nz1 = sin(a1)

            // Tread quad (black rubber edge UVs)
            v.addAll(listOf(-halfW, y0, z0, 0f, ny0, nz0, 0.02f, 0.02f))
            v.addAll(listOf(halfW, y0, z0, 0f, ny0, nz0, 0.04f, 0.02f))
            v.addAll(listOf(halfW, y1, z1, 0f, ny1, nz1, 0.04f, 0.04f))
            v.addAll(listOf(-halfW, y0, z0, 0f, ny0, nz0, 0.02f, 0.02f))
            v.addAll(listOf(halfW, y1, z1, 0f, ny1, nz1, 0.04f, 0.04f))
            v.addAll(listOf(-halfW, y1, z1, 0f, ny1, nz1, 0.02f, 0.04f))

            // Right alloy rim face (+X)
            v.addAll(listOf(halfW, 0f, 0f, 1f, 0f, 0f, 0.5f, 0.5f))
            v.addAll(listOf(halfW, y0, z0, 1f, 0f, 0f, 0.5f + ny0 * 0.48f, 0.5f + nz0 * 0.48f))
            v.addAll(listOf(halfW, y1, z1, 1f, 0f, 0f, 0.5f + ny1 * 0.48f, 0.5f + nz1 * 0.48f))

            // Left alloy rim face (-X)
            v.addAll(listOf(-halfW, 0f, 0f, -1f, 0f, 0f, 0.5f, 0.5f))
            v.addAll(listOf(-halfW, y1, z1, -1f, 0f, 0f, 0.5f + ny1 * 0.48f, 0.5f + nz1 * 0.48f))
            v.addAll(listOf(-halfW, y0, z0, -1f, 0f, 0f, 0.5f + ny0 * 0.48f, 0.5f + nz0 * 0.48f))
        }
        return GpuMesh(v.toFloatArray())
    }

    /** 3D sculpted steering wheel ring (in XY plane, radius 0.5) for hyper-real cockpit interior. */
    fun createSteeringRing(segments: Int = 24, flatBottom: Boolean = false): GpuMesh {
        val v = mutableListOf<Float>()
        val outerR = 0.50f
        val innerR = 0.38f
        val halfT = 0.06f
        fun ringPt(a: Float, r: Float): Pair<Float, Float> {
            val x = cos(a) * r
            var y = sin(a) * r
            if (flatBottom && y < -r * 0.72f) y = -r * 0.72f
            return x to y
        }
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val (ox0, oy0) = ringPt(a0, outerR)
            val (ox1, oy1) = ringPt(a1, outerR)
            val (ix0, iy0) = ringPt(a0, innerR)
            val (ix1, iy1) = ringPt(a1, innerR)
            // Front face (-Z facing driver)
            pushQuad(v, ix0, iy0, -halfT, ox0, oy0, -halfT, ox1, oy1, -halfT, ix1, iy1, -halfT)
            // Outer rim
            pushQuad(v, ox0, oy0, -halfT, ox0, oy0, halfT, ox1, oy1, halfT, ox1, oy1, -halfT)
            // Inner rim
            pushQuad(v, ix0, iy0, halfT, ix0, iy0, -halfT, ix1, iy1, -halfT, ix1, iy1, halfT)
        }
        return GpuMesh(v.toFloatArray())
    }

    fun createConePyramid(sides: Int = 8): GpuMesh {
        val v = mutableListOf<Float>()
        val r = 0.5f
        for (i in 0 until sides) {
            val a0 = (i.toFloat() / sides) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / sides) * 2f * PI.toFloat()
            val x0 = cos(a0) * r; val z0 = sin(a0) * r
            val x1 = cos(a1) * r; val z1 = sin(a1) * r
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            v.addAll(listOf(0f, 0.5f, 0f, nx, 0.55f, nz, 0.5f, 0f))
            v.addAll(listOf(x0, -0.5f, z0, nx, 0.55f, nz, 0f, 1f))
            v.addAll(listOf(x1, -0.5f, z1, nx, 0.55f, nz, 1f, 1f))
        }
        return GpuMesh(v.toFloatArray())
    }
}
