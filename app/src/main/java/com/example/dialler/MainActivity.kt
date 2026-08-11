package com.example.dialler

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.dialler.ui.theme.DiallerTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.awaitCancellation

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

internal fun TextFieldState.insertAtCursor(value: String) {
    if (value.isEmpty()) {
        return
    }

    edit {
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        replace(start, end, value)

        val newCursorPosition = start + value.length
        selection = TextRange(newCursorPosition)
    }
}

internal fun TextFieldState.deleteBeforeCursor() {
    edit {
        if (selection.start != selection.end) {
            val start = minOf(selection.start, selection.end)
            val end = maxOf(selection.start, selection.end)

            replace(start, end, "")
            selection = TextRange(start)
        } else if (selection.start > 0) {

            val cursorPosition = selection.start
            val characterBeforeCursor = cursorPosition - 1

            replace(characterBeforeCursor, cursorPosition, "")
            selection = TextRange(characterBeforeCursor)
        }
    }
}

class MainActivity : ComponentActivity() {

    private companion object {
        const val PHONE_NUMBER_KEY = "phone_number"
        const val PHONE_NUMBER_SELECTION_START_KEY =
            "phone_number_selection_start"
        const val PHONE_NUMBER_SELECTION_END_KEY =
            "phone_number_selection_end"
    }
    private lateinit var phoneNumber: TextFieldState
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
        val restoredNumber = savedInstanceState
            ?.getString(PHONE_NUMBER_KEY)
            .orEmpty()
        val restoredSelectionStart = savedInstanceState
            ?.getInt(
                PHONE_NUMBER_SELECTION_START_KEY,
                restoredNumber.length
            )
            ?.coerceIn(0, restoredNumber.length)
            ?: restoredNumber.length
        val restoredSelectionEnd = savedInstanceState
            ?.getInt(
                PHONE_NUMBER_SELECTION_END_KEY,
                restoredNumber.length
            )
            ?.coerceIn(0, restoredNumber.length)
            ?: restoredNumber.length

        phoneNumber = TextFieldState(
            initialText = restoredNumber,
            initialSelection = TextRange(
                restoredSelectionStart,
                restoredSelectionEnd
            )
        )

        if (savedInstanceState == null) {
            handleDialIntent(intent)
        }
        setContent {
            DiallerTheme {
                DiallerScreen(
                    phoneNumber = phoneNumber,
                    onDigitPressed = {
                        digit -> phoneNumber.insertAtCursor(digit)
                    },
                    onDeletePressed = {
                        phoneNumber.deleteBeforeCursor()
                    },
                    onCallPressed = {
                        requestOrPlaceCall(
                            phoneNumber.text.toString()
                        )
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)

        setIntent(intent)
        handleDialIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle){
        outState.putString(
            PHONE_NUMBER_KEY,
            phoneNumber.text.toString()
        )
        outState.putInt(
            PHONE_NUMBER_SELECTION_START_KEY,
            phoneNumber.selection.start
        )
        outState.putInt(
            PHONE_NUMBER_SELECTION_END_KEY,
            phoneNumber.selection.end
        )
        super.onSaveInstanceState(outState)

    }

    private fun handleDialIntent(intent : Intent?){
        if (
            intent?.action == Intent.ACTION_DIAL &&
            intent.data?.scheme == "tel"
        ) {

            phoneNumber.setTextAndPlaceCursorAtEnd(
                intent.data?.schemeSpecificPart ?: ""
            )
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
    phoneNumber: TextFieldState,
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

                if (maxWidth > maxHeight) {
                    LandscapeDiallerLayout(
                        availableWidth = maxWidth,
                        availableHeight = maxHeight,
                        phoneNumber = phoneNumber,
                        onDigitPressed =
                            onDigitPressed,
                        onDeletePressed =
                            onDeletePressed,
                        onCallPressed =
                            onCallPressed
                    )
                } else {

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
}


@Composable
private fun LandscapeDiallerLayout(
    availableWidth: Dp,
    availableHeight: Dp,
    phoneNumber: TextFieldState,
    onDigitPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onCallPressed: () -> Unit
) {
    val horizontalPadding =
        (availableWidth * 0.035f)
            .coerceIn(
                12.dp,
                28.dp
            )

    val verticalPadding =
        (availableHeight * 0.04f)
            .coerceIn(
                6.dp,
                16.dp
            )

    val panelGap =
        (availableWidth * 0.025f)
            .coerceIn(
                12.dp,
                26.dp
            )

    val contentWidth =
        availableWidth -
                (horizontalPadding * 2f) -
                panelGap

    val leftPanelWidth =
        contentWidth * 0.44f

    val rightPanelWidth =
        contentWidth * 0.56f

    val columnGap =
        (availableWidth * 0.012f)
            .coerceIn(
                6.dp,
                12.dp
            )

    val rowGap =
        (availableHeight * 0.025f)
            .coerceIn(
                6.dp,
                12.dp
            )

    val widthLimitedKeySize =
        (
                rightPanelWidth -
                        (columnGap * 2f)
                ) / 3f

    val heightLimitedKeySize =
        (
                availableHeight -
                        (verticalPadding * 2f) -
                        (rowGap * 3f)
                ) / 4f

    val keySize = minOf(
        widthLimitedKeySize,
        heightLimitedKeySize,
        96.dp
    )

    val numberHeight =
        (availableHeight * 0.34f)
            .coerceIn(
                88.dp,
                150.dp
            )

    val numberPadding =
        (leftPanelWidth * 0.06f)
            .coerceIn(
                8.dp,
                20.dp
            )

    val dividerPadding =
        (leftPanelWidth * 0.04f)
            .coerceIn(
                6.dp,
                16.dp
            )

    val callGap =
        (availableHeight * 0.04f)
            .coerceIn(
                8.dp,
                18.dp
            )

    val callWidth =
        (leftPanelWidth * 0.62f)
            .coerceAtMost(185.dp)

    val callHeight =
        (keySize * 0.85f)
            .coerceIn(
                48.dp,
                78.dp
            )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal =
                    horizontalPadding,
                vertical =
                    verticalPadding
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(leftPanelWidth)
                .fillMaxHeight(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
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
                    Modifier.height(callGap)
            )

            CallButton(
                width = callWidth,
                height = callHeight,
                keySize = keySize,
                onCallPressed =
                    onCallPressed
            )
        }

        Spacer(
            modifier =
                Modifier.width(panelGap)
        )

        Box(
            modifier = Modifier
                .width(rightPanelWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            DialPad(
                keySize = keySize,
                columnGap = columnGap,
                rowGap = rowGap,
                onDigitPressed =
                    onDigitPressed
            )
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NumberDisplay(
    phoneNumber: TextFieldState,
    onDeletePressed: () -> Unit,
    height: Dp,
    horizontalPadding: Dp,
    keySize: Dp
) {

    val focusRequester = remember {
        FocusRequester()
    }
    val disableSoftKeyboard = remember {
        PlatformTextInputInterceptor { _, _ ->
            awaitCancellation()
        }
    }
    val numberText = phoneNumber.text.toString()

    LaunchedEffect(numberText) {
        if (numberText.isNotEmpty()) {
            focusRequester.requestFocus()
        }
    }

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

        InterceptPlatformTextInput(
            interceptor = disableSoftKeyboard
        ) {
            BasicTextField(
                state = phoneNumber,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(
                        focusRequester
                    ),
                textStyle = TextStyle(
                    color = DiallerText,
                    fontSize =
                        (keySize.value * 0.53f).sp,
                    fontWeight =
                        FontWeight.Normal
                ),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Phone,
                        showKeyboardOnFocus =
                            false
                    ),
                lineLimits =
                    TextFieldLineLimits.SingleLine,
                cursorBrush =
                    SolidColor(DiallerText)
            )
        }

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
