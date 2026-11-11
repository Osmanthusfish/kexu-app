package com.schedule.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

val LocalMotionLevel = staticCompositionLocalOf { 2 }

@Composable fun motionDuration(normal:Int):Int = when(LocalMotionLevel.current) { 0->0;1->(normal*.65f).toInt();else->normal }
@Composable fun spatialSpring():FiniteAnimationSpec<Float> = when(LocalMotionLevel.current) {
    0 -> tween(0)
    1 -> spring(dampingRatio=1f,stiffness=900f)
    else -> spring(dampingRatio=.72f,stiffness=520f)
}

/** Observes a press without consuming events: scroll, nested buttons and accessibility still work. */
fun Modifier.pressMotion(amount:Float=.975f):Modifier = composed {
    val motion=LocalMotionLevel.current
    var pressed by remember { mutableStateOf(false) }
    var tilt by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val tiltX by animateFloatAsState(if(pressed && motion==2)tilt.y else 0f,spatialSpring(),label="press tilt x")
    val tiltY by animateFloatAsState(if(pressed && motion==2)-tilt.x else 0f,spatialSpring(),label="press tilt y")
    val pressedScale = if (motion == 1) 1f - (1f - amount) * .45f else amount
    val scale by animateFloatAsState(if(pressed && motion>0)pressedScale else 1f,spatialSpring(),label="press response")
    this.graphicsLayer { scaleX=scale;scaleY=scale;rotationX=tiltX;rotationY=tiltY;cameraDistance=24*density }.pointerInput(motion) {
        awaitEachGesture {
            val down=awaitFirstDown(requireUnconsumed=false)
            tilt=androidx.compose.ui.geometry.Offset((down.position.x/size.width.coerceAtLeast(1)-.5f)*5f,(down.position.y/size.height.coerceAtLeast(1)-.5f)*5f)
            pressed=true
            try { waitForUpOrCancellation() } finally { pressed=false }
        }
    }
}

@Composable fun Reveal(index:Int=0,content:@Composable ()->Unit) {
    var shown by remember { mutableStateOf(false) }
    val duration=motionDuration(320)
    LaunchedEffect(Unit) { if(duration>0)delay((index*35L).coerceAtMost(140));shown=true }
    val fraction by animateFloatAsState(if(shown)1f else 0f,if(duration==0)tween(0) else spring(dampingRatio=.86f,stiffness=350f),label="content reveal")
    // Keep measured space from the first frame: stagger must not make the page jump.
    Box(Modifier.graphicsLayer { alpha=fraction;translationY=(1f-fraction)*22*density;scaleX=.98f+.02f*fraction;scaleY=scaleX }) { content() }
}
