package com.example.dialler

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.dialler.ui.theme.DiallerTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DiallerBackground = Color(0xFFF2FBFE)
private val DialKeyPurple = Color(0xFF5A56E8)
private val CallGreen = Color(0xFF2D8C32)
private val DiallerText = Color(0xFF191C20)
private val DividerColor = Color(0xFFD7DDDF)

class MainActivity : ComponentActivity() {

    private companion object {
        const val PHONE_NUMBER_KEY = "phone_number"
    }
    private var phoneNumber by mutableStateOf("")
    private var pendingCallNumber: String? = null
    private val callPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
    ){
            isGranted ->
            if(isGranted){
                pendingCallNumber?.let { number ->
                    performCall(number)
                }
            }
            pendingCallNumber = null;
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        phoneNumber = savedInstanceState
            ?.getString(PHONE_NUMBER_KEY)
            .orEmpty()

        handleDialIntent(intent)
        setContent {
            DiallerTheme {
                DiallerScreen(
                    phoneNumber = phoneNumber,
                    onDigitPressed = {
                        digit -> phoneNumber += digit
                                     },
                    onDeletePressed = {
                        if (phoneNumber.isNotEmpty()) {
                            phoneNumber = phoneNumber.dropLast(1)
                        }
                                      },
                    onCallPressed = {
                        requestOrPlaceCall(phoneNumber)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)

        handleDialIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle){
        outState.putString(PHONE_NUMBER_KEY, phoneNumber)
        super.onSaveInstanceState(outState)

    }

    private fun handleDialIntent(intent : Intent?){
        if (
            intent?.action == Intent.ACTION_DIAL &&
            intent.data?.scheme == "tel"
        ) {

            phoneNumber =
                intent.data?.schemeSpecificPart ?: ""
        }
    }

    fun requestOrPlaceCall(number: String){
        if(number.isBlank()){
            return
        }

        if(
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ){
            performCall(number)
        }
        else{
            pendingCallNumber = number

            callPermissionLauncher.launch(
                Manifest.permission.CALL_PHONE
            )
        }
    }

    fun performCall(number: String){
        if(number.isBlank()){
            return
        }
        val callIntent = Intent(
            Intent.ACTION_CALL,
            Uri.parse("tel:$number")
        )
        startActivity(callIntent)
    }
}

data class DialKey(
    val digit : String,
    val letters : String = ""
)

val dialKeys = listOf(
    DialKey("1"),
    DialKey("2", "ABC"),
    DialKey("3", "DEF"),

    DialKey("4", "GHI"),
    DialKey("5", "JKL"),
    DialKey("6", "MNO"),

    DialKey("7", "PQRS"),
    DialKey("8", "TUV"),
    DialKey("9", "WXYZ"),

    DialKey("*"),
    DialKey("0", "+"),
    DialKey("#")
)
@Composable
fun DiallerScreen(
    phoneNumber: String,
    onDigitPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onCallPressed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        NumberDisplay(
            phoneNumber = phoneNumber,
            onDeletePressed = onDeletePressed
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        DialPad(
            onDigitPressed = onDigitPressed
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        CallButton(
            onCallPressed = onCallPressed
        )
    }
}


@Composable
fun NumberDisplay(
    phoneNumber: String,
    onDeletePressed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = phoneNumber,
                fontSize = 32.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDeletePressed
            ) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_backspace
                    ),
                    contentDescription = "Delete"
                )
            }
        }

        HorizontalDivider()
    }
}

@Composable
fun DialPad(
    onDigitPressed: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        dialKeys.chunked(3).forEach { rowKeys ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                rowKeys.forEach { key ->

                    DialKeyButton(
                        key = key,
                        onClick = {
                            onDigitPressed(key.digit)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DialKeyButton(
    key: DialKey,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(DialKeyPurple)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = key.digit,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 23.sp,
                maxLines = 1,
                softWrap = false
            )

            when {
                key.digit == "1" -> {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_voicemail
                        ),
                        contentDescription = "Voicemail",
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(
                                width = 15.dp,
                                height = 10.dp
                            ),
                        tint = Color.White
                    )
                }

                key.letters.isNotEmpty() -> {
                    Text(
                        text = key.letters,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
fun CallButton(
    onCallPressed: () -> Unit
) {
    Button(
        onClick = onCallPressed,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E8B32)
        ),
        contentPadding = PaddingValues(
            horizontal = 28.dp,
            vertical = 12.dp
        )
    ) {

        Icon(
            painter = painterResource(
                R.drawable.ic_call
            ),
            contentDescription = "Call",
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )

        Spacer(
            modifier = Modifier.size(6.dp)
        )

        Text(
            text = "Call",
            fontSize = 20.sp,
            color = Color.White
        )
    }
}



