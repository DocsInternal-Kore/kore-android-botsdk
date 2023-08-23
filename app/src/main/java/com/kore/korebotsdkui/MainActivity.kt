package com.kore.korebotsdkui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kore.korebotsdkui.ui.theme.KoreUiSDKTheme
import kore.botssdk.activity.BotChatActivity
import kore.botssdk.listener.BotSocketConnectionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var btnConnect = findViewById<Button>(R.id.btnConnect)

        btnConnect.setOnClickListener(View.OnClickListener {
            BotSocketConnectionManager.getInstance().startAndInitiateConnectionWithConfig(
                applicationContext, null
            )

            startActivity(Intent(this, BotChatActivity::class.java), null)
        })
    }
}