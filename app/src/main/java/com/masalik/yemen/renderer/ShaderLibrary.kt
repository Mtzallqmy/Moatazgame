package com.masalik.yemen.renderer

import android.opengl.GLES30
import android.util.Log

class ShaderLibrary {

    val worldProgram: Int
    val skyProgram: Int
    val mirrorProgram: Int

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
    val uMetallic: Int
    val uRoughness: Int
    val uWetness: Int
    val uHeadlightPos: Int
    val uHeadlightDir: Int
    val uHeadlightStrength: Int

    // Sky shader uniform locations
    val uSkyTopColor: Int
    val uSkyHorizonColor: Int
    val uSkySunDir: Int
    val uSkyNightFactor: Int
    val uSkyTime: Int

    // Rear-view mirror FBO blit uniform locations
    val uMirrorTex: Int
    val uMirrorRect: Int

    init {
        worldProgram = buildProgram(WORLD_VERT, WORLD_FRAG)
        skyProgram = buildProgram(SKY_VERT, SKY_FRAG)
        mirrorProgram = buildProgram(MIRROR_VERT, MIRROR_FRAG)

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
        uMetallic = GLES30.glGetUniformLocation(worldProgram, "uMetallic")
        uRoughness = GLES30.glGetUniformLocation(worldProgram, "uRoughness")
        uWetness = GLES30.glGetUniformLocation(worldProgram, "uWetness")
        uHeadlightPos = GLES30.glGetUniformLocation(worldProgram, "uHeadlightPos")
        uHeadlightDir = GLES30.glGetUniformLocation(worldProgram, "uHeadlightDir")
        uHeadlightStrength = GLES30.glGetUniformLocation(worldProgram, "uHeadlightStrength")

        uSkyTopColor = GLES30.glGetUniformLocation(skyProgram, "uTopColor")
        uSkyHorizonColor = GLES30.glGetUniformLocation(skyProgram, "uHorizonColor")
        uSkySunDir = GLES30.glGetUniformLocation(skyProgram, "uSunDir")
        uSkyNightFactor = GLES30.glGetUniformLocation(skyProgram, "uNightFactor")
        uSkyTime = GLES30.glGetUniformLocation(skyProgram, "uTime")

        uMirrorTex = GLES30.glGetUniformLocation(mirrorProgram, "uMirrorTex")
        uMirrorRect = GLES30.glGetUniformLocation(mirrorProgram, "uRect")
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
            precision highp float;

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
            uniform float uMetallic;
            uniform float uRoughness;
            uniform float uWetness;
            uniform vec3 uHeadlightPos;
            uniform vec3 uHeadlightDir;
            uniform float uHeadlightStrength;

            out vec4 fragColor;

            void main() {
                vec4 texSample = texture(uTexture, vTexCoord);
                float alpha = uBaseColor.a * mix(1.0, texSample.a, uUseTexture);
                if (alpha < 0.04) discard;
                vec3 albedo = mix(uBaseColor.rgb, uBaseColor.rgb * texSample.rgb, uUseTexture);

                vec3 N = normalize(vNormal);
                vec3 L = normalize(uSunDir);
                vec3 V = normalize(uCameraPos - vWorldPos);
                float ndotl = max(dot(N, L), 0.0);
                float ndotv = max(dot(N, V), 0.0);

                // Rich hemisphere sky/ground ambient lighting (warm desert earth bounce + deep blue/night sky)
                float hemi = N.y * 0.5 + 0.5;
                vec3 groundBounce = mix(vec3(0.46, 0.36, 0.26), vec3(0.06, 0.07, 0.11), uNightFactor);
                vec3 skyBounce = mix(vec3(0.76, 0.87, 1.0), vec3(0.10, 0.15, 0.28), uNightFactor);
                vec3 ambient = albedo * mix(groundBounce, skyBounce, hemi) * uAmbientIntensity;

                // Direct sun / moonlight diffuse
                vec3 diffuse = albedo * uSunColor * (ndotl * uSunIntensity);

                // PBR-inspired dual-lobe automotive clearcoat & wet asphalt specular
                float effRough = clamp(uRoughness - uWetness * 0.35, 0.05, 0.96);
                float shininess = mix(190.0, 14.0, effRough);
                vec3 H = normalize(L + V);
                float ndoth = max(dot(N, H), 0.0);
                float specPower = pow(ndoth, shininess);
                float clearcoatSpec = pow(ndoth, 320.0) * (1.0 - effRough) * (0.45 + uMetallic * 0.65);
                vec3 specCol = mix(vec3(1.0), albedo, uMetallic * 0.58);
                float specStrength = mix(1.05, 0.10, effRough) * (0.48 + uMetallic * 0.90 + uWetness * 0.60);
                vec3 specular = uSunColor * (specCol * specPower * specStrength + vec3(1.0, 0.98, 0.95) * clearcoatSpec * 0.95) * (0.38 + uSunIntensity);

                // Crisp Fresnel sky/horizon reflection on car paint, glass & wet asphalt
                float fresnel = pow(1.0 - ndotv, 4.2);
                vec3 envRefl = mix(uFogColor * 0.80, skyBounce * 1.25, clamp(reflect(-V, N).y * 0.5 + 0.5, 0.0, 1.0));
                float reflAmount = (uMetallic * 0.62 + (1.0 - effRough) * 0.32 + uWetness * 0.25) * (0.20 + 0.80 * fresnel);

                // Real Dynamic Player Headlight Dual-Beam Cone Illumination (Low/High Beam)
                vec3 headlightContrib = vec3(0.0);
                if (uHeadlightStrength > 0.02) {
                    vec3 toFrag = vWorldPos - uHeadlightPos;
                    float hlDist = length(toFrag);
                    if (hlDist > 0.8 && hlDist < 105.0) {
                        vec3 fragDir = toFrag / hlDist;
                        float spotDot = dot(fragDir, normalize(uHeadlightDir));
                        float coneMask = smoothstep(0.75, 0.965, spotDot);
                        float atten = 1.0 / (1.0 + 0.020 * hlDist + 0.0012 * hlDist * hlDist);
                        float surfFacing = clamp(dot(N, -fragDir) * 0.65 + 0.35, 0.18, 1.0);
                        vec3 beamColor = vec3(1.0, 0.96, 0.86);
                        headlightContrib = albedo * beamColor * coneMask * atten * surfFacing * uHeadlightStrength * 2.45;
                        vec3 hlH = normalize(-fragDir + V);
                        float hlSpec = pow(max(dot(N, hlH), 0.0), 42.0) * (0.28 + uWetness * 0.90);
                        headlightContrib += beamColor * hlSpec * coneMask * atten * uHeadlightStrength * 1.15;
                    }
                }

                // Subtle warm urban streetlamp ambient pool at night
                vec3 nightWarmLift = vec3(0.0);
                if (uNightFactor > 0.25) {
                    float pool = 0.5 + 0.5 * sin(vWorldPos.x * 0.14) * cos(vWorldPos.z * 0.14);
                    nightWarmLift = albedo * vec3(1.0, 0.74, 0.36) * (pool * 0.15 * uNightFactor);
                }

                vec3 lit = ambient + diffuse * (1.0 - uMetallic * 0.28) + specular + envRefl * reflAmount + headlightContrib + nightWarmLift + uEmissive;

                // True ACES Filmic Tone-Mapping for rich, sharp, high-contrast clarity & vibrant colors
                vec3 x = lit * 1.08;
                vec3 aces = clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
                // Subtle vibrance lift so paint colors, Qamariyas & city signage pop cleanly
                float luma = dot(aces, vec3(0.299, 0.587, 0.114));
                vec3 vibrant = clamp(mix(vec3(luma), aces, 1.14), 0.0, 1.0);

                // Exponential squared atmospheric fog (cleaner near-field visibility)
                float dist = length(uCameraPos - vWorldPos);
                float fogFactor = clamp(exp(-pow(max(dist - 18.0, 0.0) * uFogDensity, 1.55)), 0.0, 1.0);
                vec3 finalCol = mix(uFogColor, vibrant, fogFactor);

                fragColor = vec4(finalCol, alpha);
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
            precision highp float;
            in vec2 vUv;
            uniform vec3 uTopColor;
            uniform vec3 uHorizonColor;
            uniform vec3 uSunDir;
            uniform float uNightFactor;
            uniform float uTime;
            out vec4 fragColor;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float a = hash(i);
                float b = hash(i + vec2(1.0, 0.0));
                float c = hash(i + vec2(0.0, 1.0));
                float d = hash(i + vec2(1.0, 1.0));
                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            void main() {
                float grad = clamp((vUv.y - 0.34) * 1.60, 0.0, 1.0);
                vec3 col = mix(uHorizonColor, uTopColor, pow(grad, 0.68));

                // Atmospheric Rayleigh horizon band glow
                float horizonBand = exp(-abs(vUv.y - 0.44) * 9.5);
                col += uHorizonColor * horizonBand * 0.24;

                // Warm sun disc + Mie scattering halo, or luminous moon at night
                vec2 sunScreen = vec2(0.5 + uSunDir.x * 0.30, 0.46 + uSunDir.y * 0.44);
                float dSun = length(vUv - sunScreen);
                float sunHalo = exp(-dSun * 7.5) * (1.0 - uNightFactor * 0.55);
                float sunCore = smoothstep(0.040, 0.018, dSun);
                vec3 discColor = mix(vec3(1.0, 0.90, 0.68), vec3(0.86, 0.92, 1.0), uNightFactor);
                col += discColor * (sunHalo * 0.52 + sunCore * 1.05);

                // Soft drifting procedural Yemeni highland clouds
                if (vUv.y > 0.46) {
                    vec2 cloudUv = vec2(vUv.x * 5.2 + uTime * 0.012, vUv.y * 3.8);
                    float n = noise(cloudUv) * 0.6 + noise(cloudUv * 2.3) * 0.4;
                    float cloudMask = smoothstep(0.56, 0.84, n) * smoothstep(0.46, 0.62, vUv.y) * smoothstep(0.98, 0.72, vUv.y);
                    vec3 cloudCol = mix(vec3(0.98, 0.96, 0.92), vec3(0.16, 0.20, 0.32), uNightFactor);
                    col = mix(col, cloudCol, cloudMask * 0.42);
                }

                // Crisp twinkling stars across Yemen night sky
                if (uNightFactor > 0.22 && vUv.y > 0.48) {
                    vec2 cell = floor(vUv * vec2(170.0, 95.0));
                    float h = hash(cell);
                    if (h > 0.984) {
                        float twinkle = 0.65 + 0.35 * sin(uTime * 2.6 + h * 120.0);
                        col += vec3(0.94, 0.97, 1.0) * (uNightFactor * 0.95 * twinkle);
                    }
                }

                fragColor = vec4(col, 1.0);
            }
        """

        private const val MIRROR_VERT = """#version 300 es
            layout(location = 0) in vec2 aPos;
            uniform vec4 uRect; // (leftNdc, bottomNdc, widthNdc, heightNdc)
            out vec2 vUv;
            void main() {
                vec2 unit = aPos * 0.5 + 0.5;
                vUv = unit;
                vec2 ndc = uRect.xy + unit * uRect.zw;
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
        """

        private const val MIRROR_FRAG = """#version 300 es
            precision mediump float;
            in vec2 vUv;
            uniform sampler2D uMirrorTex;
            out vec4 fragColor;

            void main() {
                // Rounded chrome bezel frame around the live mirrored rear-view camera feed
                vec2 edge = min(vUv, 1.0 - vUv);
                float borderX = 0.018;
                float borderY = 0.065;
                if (edge.x < borderX || edge.y < borderY) {
                    float sheen = 0.25 + 0.35 * vUv.y;
                    fragColor = vec4(vec3(0.14, 0.17, 0.22) + vec3(sheen * 0.28), 0.94);
                    return;
                }
                // Flip horizontally like a real automotive rear-view mirror!
                vec2 mirrorUv = vec2(1.0 - vUv.x, vUv.y);
                vec3 col = texture(uMirrorTex, mirrorUv).rgb;
                // Subtle anti-glare blue-tinted automotive glass vignette
                float vig = 1.0 - dot(vUv - 0.5, vUv - 0.5) * 0.35;
                col = col * vig * vec3(0.95, 0.98, 1.02);
                fragColor = vec4(col, 0.96);
            }
        """
    }
}
