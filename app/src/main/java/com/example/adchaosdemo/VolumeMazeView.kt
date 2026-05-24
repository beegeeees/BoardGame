package com.example.adchaosdemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class VolumeMazeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val BAR_START_RATIO = 0.125f
        const val BAR_END_RATIO = 0.895f
        private const val OUTER_MARGIN_RATIO = 0.035f
        private const val WALL_THICKNESS_N = 0.0044f
        private const val BALL_RADIUS_N = 0.0150f
        private const val START_MARGIN_N = 0.006f
        private const val INITIAL_MAGNET_TRACK_T = 0.66f
    }

    var onBallXChanged: ((Float) -> Unit)? = null
    var onGoalReached: (() -> Unit)? = null

    private data class WallSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
    private data class WallRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private val cols = 24
    private val rows = 24
    private val goalRow = rows / 2
    private val goalYMin = goalRow / rows.toFloat()
    private val goalYMax = (goalRow + 1) / rows.toFloat()
    private val goalReachX = 1.06f

    private val wallSegments = mutableListOf<WallSegment>()
    private val wallRects = mutableListOf<WallRect>()

    private val boardOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAF0FA")
        style = Paint.Style.FILL
    }
    private val boardInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val boardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4D1E6")
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
    }
    private val boardGapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F4F8FF")
        style = Paint.Style.FILL
    }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2B3D57")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val goalFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#86ABE5")
        style = Paint.Style.FILL
    }
    private val goalGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BED2F5")
        style = Paint.Style.FILL
        alpha = 115
    }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F1CF23")
        style = Paint.Style.FILL
    }
    private val ballStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2A3D")
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
    }
    private val ballHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFF4A6")
        style = Paint.Style.FILL
        alpha = 200
    }
    private val magnetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C6FE1")
        style = Paint.Style.FILL
    }
    private val magnetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A1BFF1")
        style = Paint.Style.FILL
        alpha = 108
    }

    private var tiltX = 0f
    private var tiltY = 0f
    private var filteredTiltX = 0f
    private var filteredTiltY = 0f

    private var ballX = BALL_RADIUS_N + START_MARGIN_N
    private var ballY = 1f - BALL_RADIUS_N - START_MARGIN_N
    private var velocityX = 0f
    private var velocityY = 0f
    private var stuckTime = 0f

    private var magnetX = 0.50f
    private var magnetY = 1.02f
    private var magnetTrackT = INITIAL_MAGNET_TRACK_T
    private var magnetInputActive = false
    private var magnetActivateHold = 0f

    private var goalReached = false
    private var goalPulse = 0f

    private var running = false
    private var lastFrameNanos = 0L

    private val frameUpdater = object : Runnable {
        override fun run() {
            if (!running) return
            val now = System.nanoTime()
            if (lastFrameNanos == 0L) lastFrameNanos = now
            val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.008f, 0.028f)
            lastFrameNanos = now

            stepPhysics(dt)
            invalidate()
            postOnAnimation(this)
        }
    }

    init {
        buildMaze()
        syncMagnetToTrack()
    }

    fun setTilt(ax: Float, ay: Float) {
        tiltX = ax
        tiltY = ay
    }

    fun resetGame() {
        tiltX = 0f
        tiltY = 0f
        filteredTiltX = 0f
        filteredTiltY = 0f

        ballX = BALL_RADIUS_N + START_MARGIN_N
        ballY = 1f - BALL_RADIUS_N - START_MARGIN_N
        velocityX = 0f
        velocityY = 0f
        stuckTime = 0f

        magnetTrackT = INITIAL_MAGNET_TRACK_T
        syncMagnetToTrack()

        goalReached = false
        goalPulse = 0f
        onBallXChanged?.invoke(0f)
        invalidate()
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        postOnAnimation(frameUpdater)
    }

    fun stop() {
        running = false
    }

    private fun buildMaze() {
        wallSegments.clear()
        wallRects.clear()

        val startX = 0
        val startY = rows - 1
        val exitX = cols - 1
        val exitY = goalRow

        val directions = mutableListOf(
            intArrayOf(1, 0),
            intArrayOf(-1, 0),
            intArrayOf(0, 1),
            intArrayOf(0, -1)
        )

        var selectedVertical = Array(rows) { BooleanArray(cols + 1) { true } }
        var selectedHorizontal = Array(rows + 1) { BooleanArray(cols) { true } }
        var selectedBridgePath: List<Pair<Int, Int>>? = null
        var foundTwoRouteCandidate = false

        for (attempt in 0 until 160) {
            val vertical = Array(rows) { BooleanArray(cols + 1) { true } }
            val horizontal = Array(rows + 1) { BooleanArray(cols) { true } }
            val visited = Array(rows) { BooleanArray(cols) { false } }
            val parentX = Array(rows) { IntArray(cols) { -1 } }
            val parentY = Array(rows) { IntArray(cols) { -1 } }
            val rng = Random(20260427 + attempt)

            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.addLast(startX to startY)
            visited[startY][startX] = true
            while (stack.isNotEmpty()) {
                val (cx, cy) = stack.last()
                directions.shuffle(rng)
                var moved = false
                for (d in directions) {
                    val nx = cx + d[0]
                    val ny = cy + d[1]
                    if (nx !in 0 until cols || ny !in 0 until rows || visited[ny][nx]) continue
                    if (nx == cx + 1) vertical[cy][cx + 1] = false
                    if (nx == cx - 1) vertical[cy][cx] = false
                    if (ny == cy + 1) horizontal[cy + 1][cx] = false
                    if (ny == cy - 1) horizontal[cy][cx] = false
                    visited[ny][nx] = true
                    parentX[ny][nx] = cx
                    parentY[ny][nx] = cy
                    stack.addLast(nx to ny)
                    moved = true
                    break
                }
                if (!moved) stack.removeLast()
            }

            val path = mutableListOf<Pair<Int, Int>>()
            var px = exitX
            var py = exitY
            path += px to py
            while (!(px == startX && py == startY)) {
                val tx = parentX[py][px]
                val ty = parentY[py][px]
                if (tx < 0 || ty < 0) break
                px = tx
                py = ty
                path += px to py
            }
            path.reverse()
            if (path.size < 10) continue

            val bridge = findBridgePath(path, rng)
            if (bridge != null) {
                selectedVertical = vertical
                selectedHorizontal = horizontal
                selectedBridgePath = bridge
                foundTwoRouteCandidate = true
                break
            }

            selectedVertical = vertical
            selectedHorizontal = horizontal
        }

        val vertical = selectedVertical
        val horizontal = selectedHorizontal

        if (foundTwoRouteCandidate && selectedBridgePath != null) {
            carveEdgePath(selectedBridgePath!!, vertical, horizontal)
        } else {
            // Fallback: open one guaranteed long-gap connector near middle.
            val midX = cols / 2
            val midY = rows / 2
            if (midX + 1 <= cols) vertical[midY.coerceIn(0, rows - 1)][midX + 1] = false
        }

        // Exit opening on right outer wall.
        vertical[exitY][cols] = false
        if (cols - 1 >= 0) vertical[exitY][cols - 1] = false

        // User-requested local adjustment: only tweak the lower-left neighborhood.
        applyLowerLeftLocalTweak(vertical, horizontal)
        applyUpperRightLocalTweak(vertical)

        // Convert wall grids to merged segments.
        for (x in 0..cols) {
            var y = 0
            while (y < rows) {
                if (!vertical[y][x]) {
                    y += 1
                    continue
                }
                val runStart = y
                while (y < rows && vertical[y][x]) y += 1
                addWallSegment(
                    x / cols.toFloat(),
                    runStart / rows.toFloat(),
                    x / cols.toFloat(),
                    y / rows.toFloat()
                )
            }
        }
        for (y in 0..rows) {
            var x = 0
            while (x < cols) {
                if (!horizontal[y][x]) {
                    x += 1
                    continue
                }
                val runStart = x
                while (x < cols && horizontal[y][x]) x += 1
                addWallSegment(
                    runStart / cols.toFloat(),
                    y / rows.toFloat(),
                    x / cols.toFloat(),
                    y / rows.toFloat()
                )
            }
        }
    }

    private fun applyLowerLeftLocalTweak(
        vertical: Array<BooleanArray>,
        horizontal: Array<BooleanArray>
    ) {
        if (rows < 8 || cols < 8) return

        val bottom = rows - 1
        val row = (bottom - 2).coerceIn(1, rows - 1)

        // Keep the previously placed lower-left horizontal wall.
        horizontal[(row + 1).coerceIn(0, rows)][1] = true

        // Remove the two user-marked vertical walls:
        // 1) left long vertical (3 cells)
        vertical[(row - 1).coerceIn(0, rows - 1)][1] = false
        vertical[row.coerceIn(0, rows - 1)][1] = false
        vertical[(row + 1).coerceIn(0, rows - 1)][1] = false

        // 2) right short vertical (1 cell): restore x=5, remove x=6
        vertical[row][5] = true
        vertical[row][6] = false

        // Move the protruding start-side vertical at x=1 one cell upward.
        vertical[bottom][1] = false
        vertical[(bottom - 1).coerceIn(0, rows - 1)][1] = true
    }

    private fun applyUpperRightLocalTweak(vertical: Array<BooleanArray>) {
        if (rows < 2 || cols < 20) return

        // Add one short vertical wall near the upper-right area (user-marked red segment).
        vertical[0][18] = true
    }

    private fun carveEdgePath(
        path: List<Pair<Int, Int>>,
        vertical: Array<BooleanArray>,
        horizontal: Array<BooleanArray>
    ) {
        for (i in 0 until path.lastIndex) {
            val (x1, y1) = path[i]
            val (x2, y2) = path[i + 1]
            when {
                x2 == x1 + 1 -> vertical[y1][x1 + 1] = false
                x2 == x1 - 1 -> vertical[y1][x1] = false
                y2 == y1 + 1 -> horizontal[y1 + 1][x1] = false
                y2 == y1 - 1 -> horizontal[y1][x1] = false
            }
        }
    }

    private fun findBridgePath(
        mainPath: List<Pair<Int, Int>>,
        rng: Random
    ): List<Pair<Int, Int>>? {
        if (mainPath.size < 14) return null

        val pathSet = mainPath.toSet()
        val indexPairs = mutableListOf<Pair<Int, Int>>()
        val minGap = max(6, mainPath.size / 4)

        for (i in 2 until mainPath.size - 3) {
            for (j in i + minGap until mainPath.size - 2) {
                indexPairs += i to j
            }
        }
        indexPairs.shuffle(rng)

        val maxTrials = min(140, indexPairs.size)
        for (k in 0 until maxTrials) {
            val (i, j) = indexPairs[k]
            val start = mainPath[i]
            val end = mainPath[j]
            val bridge = randomPathAvoidingMain(start, end, pathSet, rng) ?: continue
            if (bridge.size >= 6) {
                return bridge
            }
        }
        return null
    }

    private fun randomPathAvoidingMain(
        start: Pair<Int, Int>,
        end: Pair<Int, Int>,
        mainPathSet: Set<Pair<Int, Int>>,
        rng: Random
    ): List<Pair<Int, Int>>? {
        val visited = Array(rows) { BooleanArray(cols) { false } }
        val parentX = Array(rows) { IntArray(cols) { -1 } }
        val parentY = Array(rows) { IntArray(cols) { -1 } }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addLast(start)
        visited[start.second][start.first] = true

        while (queue.isNotEmpty()) {
            val (cx, cy) = queue.removeFirst()
            if (cx == end.first && cy == end.second) break

            val neighbors = mutableListOf<Pair<Int, Int>>()
            if (cx + 1 < cols) neighbors += (cx + 1) to cy
            if (cx - 1 >= 0) neighbors += (cx - 1) to cy
            if (cy + 1 < rows) neighbors += cx to (cy + 1)
            if (cy - 1 >= 0) neighbors += cx to (cy - 1)
            neighbors.shuffle(rng)

            for ((nx, ny) in neighbors) {
                if (visited[ny][nx]) continue
                val p = nx to ny
                if (p != end && p != start && p in mainPathSet) continue
                visited[ny][nx] = true
                parentX[ny][nx] = cx
                parentY[ny][nx] = cy
                queue.addLast(p)
            }
        }

        if (!visited[end.second][end.first]) return null

        val path = mutableListOf<Pair<Int, Int>>()
        var px = end.first
        var py = end.second
        path += px to py
        while (!(px == start.first && py == start.second)) {
            val tx = parentX[py][px]
            val ty = parentY[py][px]
            if (tx < 0 || ty < 0) return null
            px = tx
            py = ty
            path += px to py
        }
        path.reverse()
        return path
    }

    private fun addWallSegment(x1: Float, y1: Float, x2: Float, y2: Float) {
        wallSegments += WallSegment(x1, y1, x2, y2)
        val eps = 0.0001f
        val isBoundarySegment = (abs(x1 - x2) < eps && (abs(x1 - 0f) < eps || abs(x1 - 1f) < eps)) ||
            (abs(y1 - y2) < eps && (abs(y1 - 0f) < eps || abs(y1 - 1f) < eps))
        if (isBoundarySegment) {
            return
        }
        val half = WALL_THICKNESS_N
        if (x1 == x2) {
            wallRects += WallRect(
                left = x1 - half,
                top = min(y1, y2),
                right = x1 + half,
                bottom = max(y1, y2)
            )
        } else {
            wallRects += WallRect(
                left = min(x1, x2),
                top = y1 - half,
                right = max(x1, x2),
                bottom = y1 + half
            )
        }
    }

    private fun stepPhysics(dt: Float) {
        filteredTiltX += (tiltX - filteredTiltX) * 0.21f
        filteredTiltY += (tiltY - filteredTiltY) * 0.21f
        updateMagnet(dt)

        if (!goalReached) {
            val prevX = ballX
            val prevY = ballY
            val goalCenterY = (goalYMin + goalYMax) * 0.5f
            val magnetInfluenceX = if (magnetX > 0.90f) magnetX + 0.10f else magnetX
            val dx = magnetInfluenceX - ballX
            val dy = magnetY - ballY
            val dist = max(0.03f, hypot(dx.toDouble(), dy.toDouble()).toFloat())
            val softening = 0.12f
            val accelScale = 0.85f / ((dist * dist) + (softening * softening))
            val dirX = dx / dist
            val dirY = dy / dist

            velocityX = (velocityX * 0.973f) + dx * accelScale * dt * 60f
            velocityY = (velocityY * 0.973f) + dy * accelScale * dt * 60f

            // Reduce near-field overshoot so the ball does not appear to repel near the magnet.
            if (dist < 0.10f) {
                val towardSpeed = (velocityX * dirX) + (velocityY * dirY)
                if (towardSpeed > 0f) {
                    val damp = towardSpeed * 0.48f
                    velocityX -= dirX * damp
                    velocityY -= dirY * damp
                }
                velocityX *= 0.92f
                velocityY *= 0.92f
            }

            val speed = hypot(velocityX.toDouble(), velocityY.toDouble()).toFloat()
            if (speed > 1.25f) {
                val scale = 1.25f / speed
                velocityX *= scale
                velocityY *= scale
            }

            if (ballX > 0.955f && ballX < 1.01f) {
                velocityY += (goalCenterY - ballY) * dt * 54f
                velocityX += 0.04f * dt * 60f
            }

            val stepDistance = hypot(velocityX.toDouble(), velocityY.toDouble()).toFloat() * dt
            val subSteps = (1 + (stepDistance / 0.006f).toInt()).coerceIn(1, 6)
            val subDt = dt / subSteps.toFloat()
            repeat(subSteps) {
                ballX += velocityX * subDt
                ballY += velocityY * subDt
                resolveBoundsAndWalls()
            }

            val moved = hypot((ballX - prevX).toDouble(), (ballY - prevY).toDouble()).toFloat()
            val speedNow = hypot(velocityX.toDouble(), velocityY.toDouble()).toFloat()
            if (ballX > 0.84f && moved < 0.0005f && speedNow < 0.12f) {
                stuckTime += dt
                if (stuckTime > 0.20f) {
                    velocityX += 0.16f
                    velocityY += (goalCenterY - ballY) * 2.0f
                    stuckTime = 0f
                }
            } else {
                stuckTime = 0f
            }
        }

        goalPulse += dt * 2.6f
        val rightWallX = 1f - BALL_RADIUS_N
        val progressBase = ((ballX - BALL_RADIUS_N) / (rightWallX - BALL_RADIUS_N)).coerceIn(0f, 1f)
        if (goalReached) {
            onBallXChanged?.invoke(1f)
        } else if (isBallInsideGoal()) {
            goalReached = true
            onBallXChanged?.invoke(1f)
            onGoalReached?.invoke()
        } else {
            val progress = when {
                ballX >= rightWallX - 0.010f -> 0.99f
                else -> (progressBase * 0.99f).coerceIn(0f, 0.99f)
            }
            onBallXChanged?.invoke(progress)
        }
    }

    private fun updateMagnet(dt: Float) {
        val ix = -filteredTiltX
        val iy = filteredTiltY
        val magnitude = hypot(ix.toDouble(), iy.toDouble()).toFloat()
        val activateThreshold = 0.26f
        val releaseThreshold = 0.15f
        if (magnetInputActive) {
            if (magnitude < releaseThreshold) {
                magnetInputActive = false
                magnetActivateHold = 0f
            }
        } else {
            if (magnitude >= activateThreshold) {
                magnetActivateHold += dt
                if (magnetActivateHold >= 0.08f) {
                    magnetInputActive = true
                    magnetActivateHold = 0f
                }
            } else {
                magnetActivateHold = 0f
            }
        }

        val deadzoneBase = if (magnetInputActive) releaseThreshold else activateThreshold
        val effectiveMagnitude = (magnitude - deadzoneBase).coerceAtLeast(0f)
        val tiltStrength = (effectiveMagnitude / 3.0f).coerceIn(0f, 1f)

        if (magnetInputActive && effectiveMagnitude > 0f) {
            val dirX = ix / magnitude
            val dirY = iy / magnitude
            val trackMin = -0.020f
            val trackMax = 1.020f
            val center = 0.5f
            val halfSpan = (trackMax - trackMin) * 0.5f
            val edgeScale = halfSpan / max(abs(dirX), abs(dirY)).coerceAtLeast(0.001f)
            val targetX = (center + (dirX * edgeScale)).coerceIn(trackMin, trackMax)
            val targetY = (center + (dirY * edgeScale)).coerceIn(trackMin, trackMax)
            val targetT = pointToPerimeterT(targetX, targetY, trackMin, trackMax)
            val delta = wrappedUnitDelta(magnetTrackT, targetT)
            if (abs(delta) < 0.0022f) {
                return
            }
            val speedFactor = 0.28f + (1.55f * tiltStrength)
            val maxStep = (dt * speedFactor).coerceAtLeast(0f)
            magnetTrackT = (magnetTrackT + delta.coerceIn(-maxStep, maxStep)).mod1()
        }

        val trackPoint = perimeterTToPoint(magnetTrackT, -0.020f, 1.020f)
        magnetX = trackPoint.first
        magnetY = trackPoint.second
    }

    private fun syncMagnetToTrack() {
        val trackPoint = perimeterTToPoint(magnetTrackT, -0.020f, 1.020f)
        magnetX = trackPoint.first
        magnetY = trackPoint.second
    }

    private fun pointToPerimeterT(x: Float, y: Float, minV: Float, maxV: Float): Float {
        val side = maxV - minV
        val total = side * 4f
        val leftDist = abs(x - minV)
        val rightDist = abs(x - maxV)
        val topDist = abs(y - minV)
        val bottomDist = abs(y - maxV)
        val minEdge = min(min(leftDist, rightDist), min(topDist, bottomDist))

        val perimeterDistance = when (minEdge) {
            topDist -> (x - minV).coerceIn(0f, side)
            rightDist -> side + (y - minV).coerceIn(0f, side)
            bottomDist -> (side * 2f) + (maxV - x).coerceIn(0f, side)
            else -> (side * 3f) + (maxV - y).coerceIn(0f, side)
        }
        return (perimeterDistance / total).mod1()
    }

    private fun perimeterTToPoint(tRaw: Float, minV: Float, maxV: Float): Pair<Float, Float> {
        val side = maxV - minV
        val total = side * 4f
        var d = (tRaw.mod1()) * total
        return when {
            d <= side -> Pair(minV + d, minV)
            d <= side * 2f -> {
                d -= side
                Pair(maxV, minV + d)
            }
            d <= side * 3f -> {
                d -= side * 2f
                Pair(maxV - d, maxV)
            }
            else -> {
                d -= side * 3f
                Pair(minV, maxV - d)
            }
        }
    }

    private fun wrappedUnitDelta(current: Float, target: Float): Float {
        var delta = target - current
        while (delta > 0.5f) delta -= 1f
        while (delta < -0.5f) delta += 1f
        return delta
    }

    private fun Float.mod1(): Float {
        var value = this % 1f
        if (value < 0f) value += 1f
        return value
    }

    private fun resolveBoundsAndWalls() {
        val goalPassMargin = (1f / rows.toFloat()) * 0.16f
        val goalPassMin = goalYMin - goalPassMargin
        val goalPassMax = goalYMax + goalPassMargin
        val inGoalBand = ballY in goalPassMin..goalPassMax
        val minX = BALL_RADIUS_N
        val maxX = if (inGoalBand) goalReachX - BALL_RADIUS_N else 1f - BALL_RADIUS_N
        val minY = BALL_RADIUS_N
        val maxY = 1f - BALL_RADIUS_N

        if (ballX < minX) {
            ballX = minX
            if (velocityX < 0f) velocityX = -velocityX * 0.2f
        }
        if (ballX > maxX) {
            ballX = maxX
            if (velocityX > 0f) velocityX = -velocityX * 0.2f
        }
        if (ballY < minY) {
            ballY = minY
            if (velocityY < 0f) velocityY = -velocityY * 0.2f
        }
        if (ballY > maxY) {
            ballY = maxY
            if (velocityY > 0f) velocityY = -velocityY * 0.2f
        }

        repeat(5) {
            wallRects.forEach { rect ->
                if (isExitBoundaryGate(rect)) return@forEach
                resolveCircleRect(rect)
            }
        }
    }

    private fun isExitBoundaryGate(rect: WallRect): Boolean {
        val rectWidth = rect.right - rect.left
        val rectHeight = rect.bottom - rect.top
        val isVertical = rectHeight > (rectWidth * 2f)
        if (!isVertical) return false

        val gateMargin = (1f / rows.toFloat()) * 0.16f
        val overlapsGoalBand = rect.bottom > (goalYMin - gateMargin) && rect.top < (goalYMax + gateMargin)
        if (!overlapsGoalBand) return false
        return rect.left > 0.992f
    }

    private fun resolveCircleRect(rect: WallRect) {
        val closestX = ballX.coerceIn(rect.left, rect.right)
        val closestY = ballY.coerceIn(rect.top, rect.bottom)
        var normalX = ballX - closestX
        var normalY = ballY - closestY
        val distSq = normalX * normalX + normalY * normalY
        val radiusSq = BALL_RADIUS_N * BALL_RADIUS_N

        if (distSq >= radiusSq) return

        if (distSq < 0.0000001f) {
            val toLeft = abs(ballX - rect.left)
            val toRight = abs(rect.right - ballX)
            val toTop = abs(ballY - rect.top)
            val toBottom = abs(rect.bottom - ballY)
            val minPen = min(min(toLeft, toRight), min(toTop, toBottom))
            when (minPen) {
                toLeft -> normalX = -1f
                toRight -> normalX = 1f
                toTop -> normalY = -1f
                else -> normalY = 1f
            }
        } else {
            val invDist = 1f / kotlin.math.sqrt(distSq)
            normalX *= invDist
            normalY *= invDist
        }

        val penetration = BALL_RADIUS_N - kotlin.math.sqrt(max(distSq, 0.0000001f))
        ballX += normalX * penetration
        ballY += normalY * penetration

        val normalVelocity = velocityX * normalX + velocityY * normalY
        if (normalVelocity < 0f) {
            velocityX -= normalX * normalVelocity
            velocityY -= normalY * normalVelocity
            velocityX *= 0.90f
            velocityY *= 0.90f
        }
    }

    private fun isBallInsideGoal(): Boolean {
        val clearMargin = (1f / rows.toFloat()) * 0.22f
        return ballX >= 0.997f && ballY in (goalYMin - clearMargin)..(goalYMax + clearMargin)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        val outerMargin = size * OUTER_MARGIN_RATIO
        val innerMargin = size * BAR_START_RATIO
        val corner = size * 0.018f

        val outerRect = RectF(outerMargin, outerMargin, size - outerMargin, size - outerMargin)
        val innerRect = RectF(innerMargin, innerMargin, size - innerMargin, size - innerMargin)

        canvas.drawColor(Color.parseColor("#F6F9FF"))
        canvas.drawRoundRect(outerRect, corner, corner, boardOuterPaint)

        boardInnerPaint.shader = LinearGradient(
            innerRect.left,
            innerRect.top,
            innerRect.left,
            innerRect.bottom,
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#EDF3FC"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(innerRect, corner, corner, boardInnerPaint)
        canvas.drawRoundRect(innerRect, corner, corner, boardStrokePaint)

        val borderGapTop = innerRect.top + (innerRect.height() * goalYMin)
        val borderGapBottom = innerRect.top + (innerRect.height() * goalYMax)
        val strokeHalf = (boardStrokePaint.strokeWidth * 0.5f) + 2f
        canvas.drawRect(
            innerRect.right - strokeHalf,
            borderGapTop - strokeHalf,
            innerRect.right + strokeHalf,
            borderGapBottom + strokeHalf,
            boardGapPaint
        )

        fun mx(v: Float): Float = innerRect.left + (innerRect.width() * v)
        fun my(v: Float): Float = innerRect.top + (innerRect.height() * v)

        wallPaint.strokeWidth = innerRect.width() * (WALL_THICKNESS_N * 2f)
        wallSegments.forEach { wall ->
            canvas.drawLine(mx(wall.x1), my(wall.y1), mx(wall.x2), my(wall.y2), wallPaint)
        }

        val goalLeft = innerRect.right
        val goalCenterY = my((goalYMin + goalYMax) * 0.5f)
        val goalSize = my(goalYMax) - my(goalYMin)
        val goalTop = goalCenterY - (goalSize * 0.5f)
        val goalBottom = goalCenterY + (goalSize * 0.5f)
        val goalRight = min(goalLeft + goalSize, outerRect.right - 2f)
        val pulse = ((kotlin.math.sin(goalPulse.toDouble()) + 1.0) * 0.5).toFloat()
        val glowExpand = size * (0.006f + 0.009f * pulse)
        val glowRect = RectF(
            goalLeft - glowExpand,
            goalTop - glowExpand,
            goalRight + glowExpand,
            goalBottom + glowExpand
        )
        canvas.drawRoundRect(glowRect, corner * 0.7f, corner * 0.7f, goalGlowPaint)
        canvas.drawRoundRect(
            RectF(goalLeft, goalTop, goalRight, goalBottom),
            corner * 0.55f,
            corner * 0.55f,
            goalFillPaint
        )
        canvas.drawRoundRect(
            RectF(goalLeft, goalTop, goalRight, goalBottom),
            corner * 0.55f,
            corner * 0.55f,
            boardStrokePaint
        )

        val ballCx = mx(ballX)
        val ballCy = my(ballY)
        val ballRadiusPx = innerRect.width() * BALL_RADIUS_N
        canvas.drawCircle(ballCx, ballCy, ballRadiusPx, ballPaint)
        canvas.drawCircle(ballCx, ballCy, ballRadiusPx, ballStrokePaint)
        canvas.drawCircle(
            ballCx - (ballRadiusPx * 0.34f),
            ballCy - (ballRadiusPx * 0.34f),
            ballRadiusPx * 0.34f,
            ballHighlightPaint
        )

        val magnetCx = outerRect.left + (outerRect.width() * magnetX)
        val magnetCy = outerRect.top + (outerRect.height() * magnetY)
        val magnetRadiusPx = size * 0.031f
        canvas.drawCircle(magnetCx, magnetCy, magnetRadiusPx * 1.7f, magnetGlowPaint)
        canvas.drawCircle(magnetCx, magnetCy, magnetRadiusPx, magnetPaint)
    }
}
