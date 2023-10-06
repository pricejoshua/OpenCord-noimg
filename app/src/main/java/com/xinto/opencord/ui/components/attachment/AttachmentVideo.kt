package com.xinto.opencord.ui.components.attachment

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AttachmentVideo(
    url: String,
) {
    Text(text = url)
//    val context = LocalContext.current
//    val exoPlayer = remember(context) {
//        ExoPlayer.Builder(context)
//            .build()
//    }
//    DisposableEffect(exoPlayer) {
//        exoPlayer.setMediaItem(MediaItem.fromUri(url))
//        exoPlayer.prepare()
//        onDispose {
//            exoPlayer.release()
//        }
//    }
//    AndroidView(
//        modifier = modifier,
//        factory = {
//            PlayerView(it).apply {
//                player = exoPlayer
//                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
//                layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                )
//                setShowNextButton(false)
//                setShowPreviousButton(false)
//                controllerShowTimeoutMs = 2000
//            }
//        },
//    )
}
