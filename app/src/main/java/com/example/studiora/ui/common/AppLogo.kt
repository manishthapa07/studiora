package com.example.studiora.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.studiora.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    contentDescription: String = "App Logo"
) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = contentDescription,
        modifier = modifier.then(Modifier.size(size))
    )
}

