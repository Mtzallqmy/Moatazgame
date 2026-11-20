package com.masalik.yemen.renderer

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

    fun createUnitCube(): GpuMesh {
        val v = mutableListOf<Float>()
        fun addFace(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            // Triangle 1: 0,1,2
            v.addAll(listOf(x0, y0, z0, nx, ny, nz, 0f, 1f))
            v.addAll(listOf(x1, y1, z1, nx, ny, nz, 1f, 1f))
            v.addAll(listOf(x2, y2, z2, nx, ny, nz, 1f, 0f))
            // Triangle 2: 0,2,3
            v.addAll(listOf(x0, y0, z0, nx, ny, nz, 0f, 1f))
            v.addAll(listOf(x2, y2, z2, nx, ny, nz, 1f, 0f))
            v.addAll(listOf(x3, y3, z3, nx, ny, nz, 0f, 0f))
        }
        val h = 0.5f
        // Front (+Z)
        addFace(-h, -h, h, h, -h, h, h, h, h, -h, h, h, 0f, 0f, 1f)
        // Back (-Z)
        addFace(h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0f, 0f, -1f)
        // Right (+X)
        addFace(h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1f, 0f, 0f)
        // Left (-X)
        addFace(-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1f, 0f, 0f)
        // Top (+Y)
        addFace(-h, h, h, h, h, h, h, h, -h, -h, h, -h, 0f, 1f, 0f)
        // Bottom (-Y)
        addFace(-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0f, -1f, 0f)
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

    fun createWheelCylinder(segments: Int = 14): GpuMesh {
        // Cylinder oriented along X-axis (wheel axle along X, radius 0.5 in Y/Z, width 1.0 along X)
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

            // Tread quad
            v.addAll(listOf(-halfW, y0, z0, 0f, ny0, nz0, 0f, 0f))
            v.addAll(listOf(halfW, y0, z0, 0f, ny0, nz0, 1f, 0f))
            v.addAll(listOf(halfW, y1, z1, 0f, ny1, nz1, 1f, 1f))
            v.addAll(listOf(-halfW, y0, z0, 0f, ny0, nz0, 0f, 0f))
            v.addAll(listOf(halfW, y1, z1, 0f, ny1, nz1, 1f, 1f))
            v.addAll(listOf(-halfW, y1, z1, 0f, ny1, nz1, 0f, 1f))

            // Right cap (+X)
            v.addAll(listOf(halfW, 0f, 0f, 1f, 0f, 0f, 0.5f, 0.5f))
            v.addAll(listOf(halfW, y0, z0, 1f, 0f, 0f, 0.5f + ny0 * 0.5f, 0.5f + nz0 * 0.5f))
            v.addAll(listOf(halfW, y1, z1, 1f, 0f, 0f, 0.5f + ny1 * 0.5f, 0.5f + nz1 * 0.5f))

            // Left cap (-X)
            v.addAll(listOf(-halfW, 0f, 0f, -1f, 0f, 0f, 0.5f, 0.5f))
            v.addAll(listOf(-halfW, y1, z1, -1f, 0f, 0f, 0.5f + ny1 * 0.5f, 0.5f + nz1 * 0.5f))
            v.addAll(listOf(-halfW, y0, z0, -1f, 0f, 0f, 0.5f + ny0 * 0.5f, 0.5f + nz0 * 0.5f))
        }
        return GpuMesh(v.toFloatArray())
    }

    fun createConePyramid(sides: Int = 6): GpuMesh {
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
