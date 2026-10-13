package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DisclaimerBanner
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.BullGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.MarketViewModel

@Composable
fun ReportsScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val providerStatus by viewModel.providerStatus.collectAsStateWithLifecycle()
    val generatedReport by viewModel.generatedReportText.collectAsStateWithLifecycle()

    var showCsvDialog by remember { mutableStateOf(false) }
    var csvContentToDisplay by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importSymbol by remember { mutableStateOf("TATAMOTORS") }
    var importCsvText by remember {
        mutableStateOf(
            "Date,Open,High,Low,Close,Volume\n" +
            "2026-03-01,920.0,945.0,915.0,940.0,4500000\n" +
            "2026-03-02,942.0,968.0,938.0,965.0,7800000\n" +
            "2026-03-03,966.0,980.0,955.0,978.0,6200000"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        DisclaimerBanner()
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Data Provider Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = AccentCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Market Data Provider & Freshness",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Active Source: ${providerStatus.providerName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Data Delay Status: ${providerStatus.delayNotice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (providerStatus.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Warnings: ${providerStatus.warnings.joinToString("; ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentGold
                            )
                        }
                    }
                }
            }

            // Report Generation Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Research Export & Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.generateTxtReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_txt_report_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate TXT Research Report")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                csvContentToDisplay = viewModel.generateCandidatesCsv()
                                showCsvDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Scanner Results (CSV)")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                csvContentToDisplay = viewModel.generatePerformanceCsv()
                                showCsvDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_performance_csv_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Tracked Performance (CSV)")
                        }
                    }
                }
            }

            // CSV Import Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Import External CSV OHLCV / Bhavcopy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import external daily candles or NSE Bhavcopy records directly into the local SQLite database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_csv_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import CSV Data")
                        }
                    }
                }
            }
        }
    }

    // View TXT Report Dialog
    if (generatedReport != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearGeneratedReport() },
            title = { Text("Market Research Report (TXT)") },
            text = {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    item {
                        Text(
                            text = generatedReport!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(generatedReport!!))
                        Toast.makeText(context, "Copied report to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearGeneratedReport() }) {
                    Text("Close")
                }
            }
        )
    }

    // View CSV Dialog
    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("CSV Export Preview") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Text(
                            text = csvContentToDisplay,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(csvContentToDisplay))
                        Toast.makeText(context, "Copied CSV to clipboard", Toast.LENGTH_SHORT).show()
                        showCsvDialog = false
                    }
                ) {
                    Text("Copy CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import CSV Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import CSV Bhavcopy / Candles") },
            text = {
                Column {
                    OutlinedTextField(
                        value = importSymbol,
                        onValueChange = { importSymbol = it },
                        label = { Text("Stock Symbol (e.g. TATAMOTORS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        label = { Text("CSV Rows (Date,Open,High,Low,Close,Volume)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importCsvCandles(importCsvText, importSymbol)
                        Toast.makeText(context, "CSV imported into SQLite", Toast.LENGTH_SHORT).show()
                        showImportDialog = false
                    }
                ) {
                    Text("Import & Run Scan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
