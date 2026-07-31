package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

data class CoffeeOption(val type: String, val caffeineMg: Int)

val presetCoffeeOptions = listOf(
    CoffeeOption("Espresso (1 shot)", 63),
    CoffeeOption("Americano", 95),
    CoffeeOption("Cappuccino", 80),
    CoffeeOption("Latte", 75),
    CoffeeOption("Kopi Tubruk", 120),
    CoffeeOption("Cold Brew", 150),
    CoffeeOption("Kopi Susu Gula Aren", 110)
)

@Composable
fun AddCoffeeDialog(
    onDismiss: () -> Unit,
    onConfirm: (coffeeType: String, caffeineMg: Int) -> Unit
) {
    var selectedOption by remember { mutableStateOf(presetCoffeeOptions[0]) }
    var isCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customCaffeine by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Catat Konsumsi Kopi",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pilih jenis kopi atau masukkan kustomisasi:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(presetCoffeeOptions) { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedOption = option
                                    isCustom = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isCustom && selectedOption == option)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (!isCustom && selectedOption == option),
                                    onClick = {
                                        selectedOption = option
                                        isCustom = false
                                    }
                                )
                                Text(
                                    text = option.type,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "~${option.caffeineMg} mg",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option to enter custom coffee
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = isCustom,
                        onClick = { isCustom = true }
                    )
                    Text(
                        text = "Kopi Lainnya (Kustom)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                if (isCustom) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nama Jenis Kopi") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_coffee_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCaffeine,
                        onValueChange = { customCaffeine = it },
                        label = { Text("Estimasi Kafein (mg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_coffee_caffeine_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isCustom) {
                        val name = customName.ifBlank { "Kopi Kustom" }
                        val caffeine = customCaffeine.toIntOrNull() ?: 80
                        onConfirm(name, caffeine)
                    } else {
                        onConfirm(selectedOption.type, selectedOption.caffeineMg)
                    }
                },
                modifier = Modifier.testTag("confirm_add_coffee_button")
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
