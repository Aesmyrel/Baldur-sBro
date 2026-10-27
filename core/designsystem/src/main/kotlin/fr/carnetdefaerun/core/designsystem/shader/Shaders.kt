package fr.carnetdefaerun.core.designsystem.shader

import android.graphics.RuntimeShader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Shaders AGSL (API 33+). Budget : un seul shader animé visible à la fois, < 2 ms de GPU par image,
 * pause hors écran, rendu statique en mouvement réduit. Chaque effet a un repli sans shader.
 */
object CarnetShaders {

    /** Brume d'encre du voile de spoiler, avec paillettes d'or ; [uClear] la dissipe, [uTouch] y creuse un trou sous le doigt. */
    const val INK_MIST = """
        uniform float2 uSize;
        uniform float uTime;
        uniform float uClear;
        uniform float3 uTouch;
        layout(color) uniform half4 uInk;
        layout(color) uniform half4 uGold;

        float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
        float noise(float2 p) {
            float2 i = floor(p); float2 f = fract(p);
            float a = hash(i); float b = hash(i + float2(1, 0));
            float c = hash(i + float2(0, 1)); float d = hash(i + float2(1, 1));
            float2 u = f * f * (3.0 - 2.0 * f);
            return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
        }
        float fbm(float2 p) {
            float v = 0.0; float a = 0.5;
            for (int i = 0; i < 5; i++) { v += a * noise(p); p *= 2.03; a *= 0.5; }
            return v;
        }
        half4 main(float2 xy) {
            float2 uv = xy / uSize.y;
            float t = uTime * 0.05;
            float n = fbm(uv * 3.0 + float2(t, -t * 0.6));
            float n2 = fbm(uv * 6.0 - float2(t * 1.3, t));
            float m = smoothstep(0.25, 0.85, n * 0.7 + n2 * 0.4);
            float d = length(xy - uTouch.xy);
            float hole = smoothstep(uTouch.z * 0.55, uTouch.z, d);
            float a = (0.62 + 0.38 * m) * hole * (1.0 - uClear);
            float speck = step(0.9965, hash(floor(xy / 2.0) + floor(uTime * 0.7)));
            half4 col = mix(uInk, uGold, half(speck * 0.85 + m * 0.08));
            return col * half(a);
        }
    """

    /** Reflet d'or : une bande claire balaie la surface une fois ([uProgress] de 0 à 1). */
    const val GOLD_GLINT = """
        uniform float2 uSize;
        uniform float uProgress;
        half4 main(float2 xy) {
            float2 uv = xy / uSize;
            float band = uv.x + uv.y * 0.6 - (uProgress * 2.4 - 0.6);
            float a = smoothstep(0.18, 0.0, abs(band)) * 0.55;
            return half4(1.0, 0.95, 0.78, 1.0) * half(a);
        }
    """

    /** Crée le shader, ou null si la plateforme ne le compile pas (repli statique). */
    fun create(source: String): RuntimeShader? = runCatching { RuntimeShader(source) }.getOrNull()
}

fun RuntimeShader.setColor(name: String, color: Color) = setColorUniform(name, color.toArgb())
