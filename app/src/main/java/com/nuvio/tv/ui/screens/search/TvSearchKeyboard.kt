package com.nuvio.tv.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.Text

/**
 * Remote-first text entry for the Qilive Android TV.
 *
 * The Google TV Remote's ImeBridgeService can replace Gboard as the system IME
 * without user action. This keyboard has no dependency on the system IME, no
 * privileged permissions, and can be dismissed with Back.
 */
@Composable
internal fun TvSearchKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstKey = remember { FocusRequester() }
    val rows = remember {
        listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            "A Z E R T Y U I O P".split(" "),
            "Q S D F G H J K L M".split(" "),
            "W X C V B N É È - '".split(" ")
        )
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .background(Color(0xFF131A25), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Text(
                "Recherche Anime-Sama",
                color = Color.White,
                fontSize = 23.sp
            )
            Text(
                text = value.ifBlank { "Saisir le titre de l'anime..." },
                color = if (value.isBlank()) Color.LightGray else Color.White,
                fontSize = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            rows.forEachIndexed { rowIndex, keys ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    keys.forEachIndexed { keyIndex, key ->
                        Button(
                            onClick = { onValueChange(value + key.lowercase()) },
                            modifier = Modifier
                                .width(54.dp)
                                .height(47.dp)
                                .then(
                                    if (rowIndex == 0 && keyIndex == 0)
                                        Modifier.focusRequester(firstKey)
                                    else Modifier
                                )
                        ) {
                            Text(key, fontSize = 18.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onValueChange(value.dropLast(1)) }) {
                    Text("Effacer", fontSize = 18.sp)
                }
                Button(onClick = { onValueChange(value + " ") }) {
                    Text("Espace", fontSize = 18.sp)
                }
                Button(onClick = { onValueChange("") }) {
                    Text("Tout effacer", fontSize = 18.sp)
                }
                Button(onClick = onDismiss) {
                    Text("Fermer", fontSize = 18.sp)
                }
                Button(onClick = onSearch) {
                    Text("Rechercher", fontSize = 18.sp)
                }
            }
            Text(
                "Fleches pour se deplacer - OK pour saisir - Retour pour fermer",
                color = Color.LightGray,
                fontSize = 15.sp
            )
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        runCatching { firstKey.requestFocus() }
    }
}
