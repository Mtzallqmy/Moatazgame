package com.masalik.yemen.renderer

import android.opengl.GLES30
import android.util.Log

class ShaderLibrary {

    val worldProgram: Int
    val skyProgram: Int

    // World shader uniform locations
    val uMvpMatrix: Int
    val uModelMatrix: Int
    val uBaseColor: Int
    val uEmissive: Int
    val uSunDir: Int
    val uSunColor: Int
    val uSunIntensity: Int
    val uAmbientIntensity: Int
    val uFogColor: Int
    val uFogDensity: Int
    val uCameraPos: Int
    val uNightFactor: Int
    val uTexture: Int
    val uUseTexture: Int
    val uUvScale: Int

    // Sky shader uniform locations
    val uSkyTopColor: Int
    val uSkyHorizonColor: Int
    val uSkySunDir: Int
    val uSkyNightFactor: Int

    init {
        worldProgram = buildProgram(WORLD_VERT, WORLD_FRAG)
        skyProgram = buildProgram(SKY_VERT, SKY_FRAG)

        uMvpMatrix = GLES30.glGetUniformLocation(worldProgram, "uMvpMatrix")
        uModelMatrix = GLES30.glGetUniformLocation(worldProgram, "uModelMatrix")
        uBaseColor = GLES30.glGetUniformLocation(worldProgram, "uBaseColor")
        uEmissive = GLES30.glGetUniformLocation(worldProgram, "uEmissive")
        uSunDir = GLES30.glGetUniformLocation(worldProgram, "uSunDir")
        uSunColor = GLES30.glGetUniformLocation(worldProgram, "uSunColor")
        uSunIntensity = GLES30.glGetUniformLocation(worldProgram, "uSunIntensity")
        uAmbientIntensity = GLES30.glGetUniformLocation(worldProgram, "uAmbientIntensity")
        uFogColor = GLES30.glGetUniformLocation(worldProgram, "uFogColor")
        uFogDensity = GLES30.glGetUniformLocation(worldProgram, "uFogDensity")
        uCameraPos = GLES30.glGetUniformLocation(worldProgram, "uCameraPos")
        uNightFactor = GLES30.glGetUniformLocation(worldProgram, "uNightFactor")
        uTexture = GLES30.glGetUniformLocation(worldProgram, "uTexture")
        uUseTexture = GLES30.glGetUniformLocation(worldProgram, "uUseTexture")
        uUvScale = GLES30.glGetUniformLocation(worldProgram, "uUvScale")

        uSkyTopColor = GLES30.glGetUniformLocation(skyProgram, "uTopColor")
        uSkyHorizonColor = GLES30.glGetUniformLocation(skyProgram, "uHorizonColor")
        uSkySunDir = GLES30.glGetUniformLocation(skyProgram, "uSunDir")
        uSkyNightFactor = GLES30.glGetUniformLocation(skyProgram, "uNightFactor")
    }

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        val status = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("MasalikShader", "Program link failed: ${GLES30.glGetProgramInfoLog(prog)}")
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("MasalikShader", "Shader compile error: ${GLES30.glGetShaderInfoLog(shader)}")
        }
        return shader
    }

    companion object {
        private const val WORLD_VERT = """#version 300 es
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aTexCoord;

            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            uniform vec2 uUvScale;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec2 vTexCoord;

            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vTexCoord = aTexCoord * uUvScale;
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val WORLD_FRAG = """#version 300 es
            precision mediump float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec2 vTexCoord;

            uniform vec4 uBaseColor;
            uniform vec3 uEmissive;
            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform float uSunIntensity;
            uniform float uAmbientIntensity;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform vec3 uCameraPos;
            uniform float uNightFactor;
            uniform sampler2D uTexture;
            uniform float uUseTexture;

            out vec4 fragColor;

            void main() {
                vec4 texSample = texture(uTexture, vTexCoord);
                vec3 albedo = mix(uBaseColor.rgb, uBaseColor.rgb * texSample.rgb, uUseTexture);

                vec3 N = normalize(vNormal);
                vec3 L = normalize(uSunDir);
                float ndotl = max(dot(N, L), 0.0);

                // Hemisphere sky/ground ambient for rich mountain depth
                float hemi = N.y * 0.5 + 0.5;
                vec3 skyAmb = mix(vec3(0.45, 0.36, 0.28), vec3(0.75, 0.85, 0.98), hemi);
                vec3 lit = albedo * (skyAmb * uAmbientIntensity + uSunColor * (ndotl * uSunIntensity));

                // Subtle specular highlight on sunlit surfaces
                vec3 V = normalize(uCameraPos - vWorldPos);
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), 24.0) * 0.14 * uSunIntensity;
                lit += uSunColor * spec;

                // Nighttime warm emissive glow (Qamariya windows, streetlights, headlights, signals)
                lit += uEmissive;

                // Exponential squared atmospheric fog
                float dist = length(uCameraPos - vWorldPos);
                float fogFactor = exp(-pow(dist * uFogDensity, 1.65));
                fogFactor = clamp(fogFactor, 0.0, 1.0);
                vec3 finalCol = mix(uFogColor, lit, fogFactor);

                fragColor = vec4(finalCol, uBaseColor.a);
            }
        """

        private const val SKY_VERT = """#version 300 es
            layout(location = 0) in vec2 aPos;
            out vec2 vUv;
            void main() {
                vUv = aPos * 0.5 + 0.5;
                gl_Position = vec4(aPos, 0.9999, 1.0);
            }
        """

        private const val SKY_FRAG = """#version 300 es
            precision mediump float;
            in vec2 vUv;
            uniform vec3 uTopColor;
            uniform vec3 uHorizonColor;
            uniform vec3 uSunDir;
            uniform float uNightFactor;
            out vec4 fragColor;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            void main() {
                float grad = clamp((vUv.y - 0.35) * 1.65, 0.0, 1.0);
                vec3 col = mix(uHorizonColor, uTopColor, pow(grad, 0.72));

                // Warm sun or crescent moon glow in upper sky
                vec2 sunScreen = vec2(0.5 + uSunDir.x * 0.28, 0.48 + uSunDir.y * 0.42);
                float dSun = length(vUv - sunScreen);
                float sunHalo = exp(-dSun * 9.0) * (1.0 - uNightFactor * 0.65);
                float sunCore = smoothstep(0.042, 0.022, dSun);
                vec3 discColor = mix(vec3(1.0, 0.92, 0.72), vec3(0.88, 0.92, 1.0), uNightFactor);
                col += discColor * (sunHalo * 0.42 + sunCore * 0.95);

                // Night stars over Yemen highlands
                if (uNightFactor > 0.25 && vUv.y > 0.48) {
                    vec2 cell = floor(vUv * vec2(140.0, 80.0));
                    float h = hash(cell);
                    if (h > 0.985) {
                        col += vec3(0.95, 0.97, 1.0) * (uNightFactor * 0.85);
                    }
                }

                fragColor = vec4(col, 1.0);
            }
        """
    }
}
