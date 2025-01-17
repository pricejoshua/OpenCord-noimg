package com.xinto.opencord.ui.screens.home.panels.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xinto.opencord.R
import com.xinto.opencord.domain.attachment.DomainPictureAttachment
import com.xinto.opencord.domain.attachment.DomainVideoAttachment
import com.xinto.opencord.domain.emoji.DomainGuildEmoji
import com.xinto.opencord.domain.emoji.DomainUnicodeEmoji
import com.xinto.opencord.domain.emoji.DomainUnknownEmoji
import com.xinto.opencord.domain.message.DomainMessageRegular
import com.xinto.opencord.ui.components.OCImage
import com.xinto.opencord.ui.components.OCSize
import com.xinto.opencord.ui.components.attachment.AttachmentPicture
import com.xinto.opencord.ui.components.attachment.AttachmentVideo
import com.xinto.opencord.ui.components.embed.*
import com.xinto.opencord.ui.components.message.*
import com.xinto.opencord.ui.components.message.reply.MessageReferenced
import com.xinto.opencord.ui.components.message.reply.MessageReferencedAuthor
import com.xinto.opencord.ui.components.message.reply.MessageReferencedContent
import com.xinto.opencord.ui.screens.home.panels.messagemenu.MessageMenu
import com.xinto.opencord.ui.util.ifComposable
import com.xinto.opencord.ui.util.ifNotEmptyComposable
import com.xinto.opencord.ui.util.ifNotNullComposable
import com.xinto.opencord.ui.util.toUnsafeImmutableList
import com.xinto.opencord.ui.viewmodel.ChatViewModel

@Composable
fun ChatLoaded(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onUsernameClicked: ((userId: Long) -> Unit)? = null,
) {
    val listState = rememberLazyListState() // TODO: scroll to target message if jumping
    var messageMenuTarget by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(viewModel.sortedMessages.size) {
        if (listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }

    if (messageMenuTarget != null) {
        MessageMenu(
            messageId = messageMenuTarget!!,
            onDismiss = { messageMenuTarget = null },
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        reverseLayout = true,
    ) {
        items(viewModel.sortedMessages, key = { it.message.id }) { item ->
            when (val message = item.message) {
                is DomainMessageRegular -> {
                    val messageReactions by remember {
                        derivedStateOf {
                            item.reactions.values
                                .sortedBy { it.reactionOrder }
                                .toUnsafeImmutableList()
                                .takeIf { it.isNotEmpty() }
                        }
                    }

                    MessageRegular(
                        onLongClick = { messageMenuTarget = message.id },
                        modifier = Modifier
                            .fillMaxWidth(),
                        mentioned = item.meMentioned,
                        topMerged = item.topMerged,
                        bottomMerged = item.bottomMerged,
                        reply = message.isReply.ifComposable {
                            if (message.referencedMessage != null) {
                                MessageReferenced(
                                    avatar = {
                                        MessageAvatar(url = message.referencedMessage.author.avatarUrl)
                                    },
                                    author = {
                                        MessageReferencedAuthor(author = message.referencedMessage.author.username)
                                    },
                                    content = message.referencedMessage.contentRendered.ifNotEmptyComposable {
                                        MessageReferencedContent(
                                            text = message.referencedMessage.contentRendered,
                                        )
                                    },
                                )
                            } else {
                                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                                    Text(stringResource(R.string.message_reply_unknown))
                                }
                            }
                        },
                        avatar = if (item.topMerged) null else { ->
                            MessageAvatar(url = message.author.avatarUrl)
                        },
                        author = if (item.topMerged) null else { ->
                            MessageAuthor(
                                author = message.author.username,
                                timestamp = message.formattedTimestamp,
                                isEdited = message.isEdited,
                                isBot = message.author.bot,
                                onAuthorClick = { onUsernameClicked?.invoke(message.author.id) },
                            )
                        },
                        content = message.contentRendered.ifNotEmptyComposable {
                            MessageContent(
                                text = message.contentRendered,
                            )
                        },
                        embeds = message.embeds.ifNotEmptyComposable { embeds ->
                            val renderedEmbeds = if (message.isTwitterMultiImageMessage) listOf(embeds.first()) else embeds

                            for (embed in renderedEmbeds) key(embed) {
                                if (embed.isVideoOnlyEmbed) {
                                    val video = embed.video!!
                                    AttachmentVideo(
                                        url = video.proxyUrl!!
                                    )
                                } else if (embed.isSpotifyEmbed) {
                                    SpotifyEmbed(
                                        embedUrl = embed.spotifyEmbedUrl!!,
                                        isSpotifyTrack = embed.isSpotifyTrack,
                                    )
                                } else {
                                    Embed(
                                        title = embed.title,
                                        url = embed.url,
                                        description = embed.description,
                                        color = embed.color,
                                        author = embed.author.ifNotNullComposable {
                                            EmbedAuthor(
                                                name = it.name,
                                                url = it.url,
                                                iconUrl = it.iconUrl,
                                            )
                                        },
                                        media = if (!message.isTwitterMultiImageMessage) {
                                            embed.image.ifNotNullComposable {
                                                AttachmentPicture(url = it.sizedUrl)
                                            } ?: embed.video.ifNotNullComposable {
                                                AttachmentVideo(url = it.sizedUrl)
                                            }
                                        } else {
                                            {
                                                @OptIn(ExperimentalLayoutApi::class)
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                                                    maxItemsInEachRow = 2,
                                                    modifier = Modifier
                                                        .clip(MaterialTheme.shapes.small),
                                                ) {
                                                    for ((i, twitterEmbed) in embeds.withIndex()) key(twitterEmbed.image) {
                                                        val image = twitterEmbed.image!!
                                                        val isLastRow = i >= embeds.size - 2 // needed or parent clipping breaks

                                                        OCImage(
                                                            url = image.sizedUrl,
                                                            size = OCSize(image.width ?: 500, image.height ?: 500),
                                                            contentScale = ContentScale.FillWidth,
                                                            modifier = Modifier
                                                                .fillMaxWidth(0.48f)
                                                                .heightIn(max = 350.dp)
                                                                .padding(bottom = if (isLastRow) 0.dp else 5.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        thumbnail = embed.thumbnail.ifNotNullComposable {
                                        },
                                        fields = embed.fields.ifNotNullComposable {
                                            for (field in it) key(field) {
                                                EmbedField(
                                                    name = field.name,
                                                    value = field.value,
                                                )
                                            }
                                        },
                                        footer = embed.footer.ifNotNullComposable {
                                            EmbedFooter(
                                                text = it.text,
                                                iconUrl = it.displayUrl,
                                                timestamp = it.formattedTimestamp,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                        attachments = message.attachments.ifNotEmptyComposable { attachments ->
                            for (attachment in attachments) key(attachment) {
                                when (attachment) {
                                    is DomainPictureAttachment -> {
                                        AttachmentPicture(url = attachment.url)
                                    }
                                    is DomainVideoAttachment -> {
                                        AttachmentVideo(
                                            url = attachment.url
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        },
                        reactions = messageReactions?.ifNotEmptyComposable { reactions ->
                            for (reaction in reactions) {
                                key(reaction.emoji.identifier) {
                                    MessageReaction(
                                        onClick = {
                                            viewModel.reactToMessage(message.id, reaction.emoji)
                                        },
                                        count = reaction.count,
                                        meReacted = reaction.meReacted,
                                    ) {
                                        when (reaction.emoji) {
                                            is DomainUnicodeEmoji -> {
                                                Text(
                                                    text = reaction.emoji.emoji,
                                                    fontSize = 16.sp,
                                                )
                                            }
                                            is DomainGuildEmoji -> {
                                                OCImage(
                                                    url = reaction.emoji.url,
                                                    size = OCSize(64, 64),
                                                    memoryCaching = true,
                                                    modifier = Modifier
                                                        .size(18.dp),
                                                )
                                            }
                                            is DomainUnknownEmoji -> {}
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
                // TODO: render other message types
                else -> {}
            }
        }
    }
}
