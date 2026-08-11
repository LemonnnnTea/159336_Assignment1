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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp

private val DiallerBackground = Color(0xFFF2FBFF)
private val DialKeyPurple = Color(0xFF5A5ADC)
private val CallGreen = Color(0xFF288228)
private val DiallerText = Color(0xFF181A1C)
private val DividerColor = Color(0xFFCED7DC)


data class DialKey(
    val digit: String,
    val letters: String = ""
)


private val dialKeys = listOf(
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

@Composable
fun DiallerScreen(
    phoneNumber: String,
    onDigitPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onCallPressed: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DiallerBackground)
    ) {

        /*
         * Keep interactive content away from the status bar
         * and navigation bar.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {

                /*
                 * Base dimensions are calculated from the
                 * available width instead of one fixed screen.
                 */
                val baseKeySize =
                    (maxWidth * 0.26f)
                        .coerceIn(
                            72.dp,
                            108.dp
                        )

                val baseColumnGap =
                    (maxWidth * 0.03f)
                        .coerceIn(
                            7.dp,
                            13.dp
                        )

                val baseRowGap =
                    (maxWidth * 0.045f)
                        .coerceIn(
                            10.dp,
                            19.dp
                        )

                val baseNumberHeight =
                    (maxWidth * 0.50f)
                        .coerceIn(
                            140.dp,
                            205.dp
                        )

                val baseTopSpace =
                    (maxWidth * 0.03f)
                        .coerceIn(
                            8.dp,
                            14.dp
                        )

                val baseKeypadTopGap =
                    (maxWidth * 0.065f)
                        .coerceIn(
                            16.dp,
                            28.dp
                        )

                val baseCallGap =
                    (maxWidth * 0.04f)
                        .coerceIn(
                            10.dp,
                            18.dp
                        )

                val baseCallWidth =
                    (maxWidth * 0.45f)
                        .coerceIn(
                            130.dp,
                            185.dp
                        )

                val baseCallHeight =
                    (maxWidth * 0.26f)
                        .coerceIn(
                            72.dp,
                            105.dp
                        )

                /*
                 * Estimate the vertical space needed by the
                 * reference-style portrait layout.
                 */
                val requiredHeight =
                    baseTopSpace +
                            2.dp +
                            baseNumberHeight +
                            baseKeypadTopGap +
                            (baseKeySize * 4f) +
                            (baseRowGap * 3f) +
                            baseCallGap +
                            baseCallHeight +
                            8.dp

                /*
                 * If the screen is shorter than normal,
                 * scale the UI down instead of clipping it.
                 */
                val scale =
                    if (requiredHeight > maxHeight) {
                        (
                                maxHeight.value /
                                        requiredHeight.value
                                ).coerceAtLeast(0.80f)
                    } else {
                        1f
                    }

                val keySize =
                    baseKeySize * scale

                val columnGap =
                    baseColumnGap * scale

                val rowGap =
                    baseRowGap * scale

                val numberHeight =
                    baseNumberHeight * scale

                val topSpace =
                    baseTopSpace * scale

                val keypadTopGap =
                    baseKeypadTopGap * scale

                val callGap =
                    baseCallGap * scale

                val callWidth =
                    baseCallWidth * scale

                val callHeight =
                    baseCallHeight * scale

                val dividerPadding =
                    (maxWidth * 0.07f)
                        .coerceIn(
                            16.dp,
                            30.dp
                        )

                val numberPadding =
                    (maxWidth * 0.10f)
                        .coerceIn(
                            22.dp,
                            42.dp
                        )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(topSpace)
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    dividerPadding
                            ),
                        thickness = 1.dp,
                        color = DividerColor
                    )

                    NumberDisplay(
                        phoneNumber = phoneNumber,
                        onDeletePressed =
                            onDeletePressed,
                        height = numberHeight,
                        horizontalPadding =
                            numberPadding,
                        keySize = keySize
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    dividerPadding
                            ),
                        thickness = 1.dp,
                        color = DividerColor
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                keypadTopGap
                            )
                    )

                    DialPad(
                        keySize = keySize,
                        columnGap = columnGap,
                        rowGap = rowGap,
                        onDigitPressed =
                            onDigitPressed
                    )

                    Spacer(
                        modifier =
                            Modifier.height(callGap)
                    )

                    CallButton(
                        width = callWidth,
                        height = callHeight,
                        keySize = keySize,
                        onCallPressed =
                            onCallPressed
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun NumberDisplay(
    phoneNumber: String,
    onDeletePressed: () -> Unit,
    height: Dp,
    horizontalPadding: Dp,
    keySize: Dp
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(
                horizontal =
                    horizontalPadding
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = phoneNumber,
            modifier =
                Modifier.weight(1f),
            color = DiallerText,

            fontSize =
                (keySize.value * 0.53f).sp,

            fontWeight =
                FontWeight.Normal,

            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )

        IconButton(
            onClick = onDeletePressed,
            modifier = Modifier.size(
                keySize * 0.43f
            )
        ) {

            Icon(
                painter = painterResource(
                    id =
                        R.drawable.ic_backspace
                ),
                contentDescription =
                    "Delete",
                modifier = Modifier.size(
                    keySize * 0.27f
                ),
                tint = DiallerText
            )
        }
    }
}

@Composable
fun DialPad(
    keySize: Dp,
    columnGap: Dp,
    rowGap: Dp,
    onDigitPressed: (String) -> Unit
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.spacedBy(rowGap)
    ) {

        dialKeys
            .chunked(3)
            .forEach { rowKeys ->

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            columnGap
                        )
                ) {

                    rowKeys.forEach { key ->

                        DialKeyButton(
                            key = key,
                            keySize = keySize,

                            onClick = {
                                onDigitPressed(
                                    key.digit
                                )
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
    keySize: Dp,
    onClick: () -> Unit
) {

    val digitFontSize =
        (keySize.value * 0.31f).sp

    val lettersFontSize =
        (keySize.value * 0.135f).sp

    Box(
        modifier = Modifier
            .size(keySize)
            .clip(CircleShape)
            .background(
                DialKeyPurple
            )
            .clickable(
                onClick = onClick
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = key.digit,
                color = Color.White,
                fontSize =
                    digitFontSize,
                fontWeight =
                    FontWeight.Normal,
                maxLines = 1,
                softWrap = false
            )

            when {

                key.digit == "1" -> {

                    Spacer(
                        modifier =
                            Modifier.height(
                                keySize * 0.01f
                            )
                    )

                    Icon(
                        painter =
                            painterResource(
                                id =
                                    R.drawable
                                        .ic_voicemail
                            ),
                        contentDescription =
                            "Voicemail",

                        modifier = Modifier
                            .width(
                                keySize * 0.24f
                            )
                            .height(
                                keySize * 0.12f
                            ),

                        tint = Color.White
                    )
                }

                key.letters.isNotEmpty() -> {

                    Text(
                        text = key.letters,
                        color = Color.White,
                        fontSize =
                            lettersFontSize,
                        fontWeight =
                            FontWeight.Medium,

                        maxLines = 1,
                        softWrap = false,
                        overflow =
                            TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
fun CallButton(
    width: Dp,
    height: Dp,
    keySize: Dp,
    onCallPressed: () -> Unit
) {

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(
                RoundedCornerShape(
                    percent = 50
                )
            )
            .background(CallGreen)
            .clickable(
                onClick = onCallPressed
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.Center
        ) {

            Icon(
                painter = painterResource(
                    id = R.drawable.ic_call
                ),

                contentDescription = "Call",

                modifier = Modifier.size(
                    keySize * 0.22f
                ),

                tint = Color.White
            )

            Spacer(
                modifier = Modifier.width(
                    keySize * 0.07f
                )
            )

            Text(
                text = "Call",
                color = Color.White,

                fontSize =
                    (keySize.value * 0.30f).sp,

                fontWeight =
                    FontWeight.Normal,

                maxLines = 1
            )
        }
    }
}



