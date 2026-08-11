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
    phoneNumber : String,
    onDigitPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onCallPressed: () -> Unit
){
    Column {
        Text(
            text = phoneNumber
        )
        Button(
            onClick = onDeletePressed
        ) {
            Text("Delete")
        }
        DialPad(
            onDigitPressed = onDigitPressed
        )
        Button(
            onClick = onCallPressed
        ) {
            Text("Call")
        }
    }
}


@Composable
fun NumberDisplay(
    phoneNumber: String,
    onDeletePressed: () -> Unit
){

}

@Composable
fun DialPad(
    onDigitPressed: (String) -> Unit
){
    Column {
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
){
    Button(
        onClick = onClick,
        modifier = Modifier.size(80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = key.digit
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters
                )
            }
        }
    }
}

@Composable
fun CallButton(
    onCallPressed: () -> Unit
){

}



