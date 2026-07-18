package dev.study.app.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    var rotation: Float = 0f,
    var alpha: Float = 1.0f
) {
    fun update() {
        x += vx
        y += vy
        vy += 0.35f // Gravity
        vx *= 0.98f // Air resistance
        rotation += rotationSpeed
        alpha = (alpha - 0.015f).coerceAtLeast(0f)
    }
}

@Composable
fun ConfettiCelebration(
    trigger: Boolean,
    onAnimationEnd: () -> Unit
) {
    if (!trigger) return

    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    val random = remember { Random() }
    val colors = listOf(
        Color(0xFFFFC107), Color(0xFFFF5722), Color(0xFF4CAF50),
        Color(0xFF03A9F4), Color(0xFFE91E63), Color(0xFF9C27B0)
    )

    var hasActiveParticles by remember { mutableStateOf(true) }

    LaunchedEffect(trigger) {
        particles.clear()
        hasActiveParticles = true
        // Create particles
        val count = 120
        for (i in 0 until count) {
            val angle = random.nextFloat() * 2 * Math.PI
            val speed = 5f + random.nextFloat() * 15f
            particles.add(
                ConfettiParticle(
                    x = 540f, // Initial placeholder, will override on first draw
                    y = 1200f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed - 12f).toFloat(), // Strong upward bias
                    color = colors[random.nextInt(colors.size)],
                    size = 12f + random.nextFloat() * 18f,
                    rotationSpeed = -5f + random.nextFloat() * 10f
                )
            )
        }

        while (hasActiveParticles) {
            withInfiniteAnimationFrameMillis {
                var active = false
                for (p in particles) {
                    p.update()
                    if (p.alpha > 0f && p.y < 3000f) {
                        active = true
                    }
                }
                hasActiveParticles = active
            }
        }
        onAnimationEnd()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            // Re-center on first draw if it is set to placeholder values
            if (p.x == 540f && p.y == 1200f) {
                p.x = width / 2
                p.y = height * 0.8f // Spawn from bottom area
            }
            if (p.alpha > 0f) {
                drawContext.canvas.save()
                drawContext.canvas.translate(p.x, p.y)
                drawContext.canvas.rotate(p.rotation)
                drawRect(
                    color = p.color.copy(alpha = p.alpha),
                    topLeft = Offset(-p.size / 2, -p.size / 2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size / 2)
                )
                drawContext.canvas.restore()
            }
        }
    }
}
