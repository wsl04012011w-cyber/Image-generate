package com.example.imagegenerate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

private const val DEFAULT_MODEL_URL =
    "https://huggingface.co/stabilityai/sd-turbo/resolve/main/sd_turbo.safetensors"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface { ImageGeneratorScreen(filesDir) } } }
    }
}

@Composable
private fun ImageGeneratorScreen(filesDir: File) {
    val scope = rememberCoroutineScope()
    val downloader = remember { ModelDownloader() }
    val engine = remember { DiffusionEngine() }
    val modelFile = remember { File(File(filesDir, "models"), "model.safetensors") }
    var hasModel by remember { mutableStateOf(modelFile.exists() && modelFile.length() > 0L) }
    var prompt by remember { mutableStateOf("") }
    var modelUrl by remember { mutableStateOf(DEFAULT_MODEL_URL) }
    var steps by remember { mutableFloatStateOf(8f) }
    var backend by remember { mutableStateOf(ComputeBackend.GPU) }
    var status by remember { mutableStateOf("Pronto") }
    var progress by remember { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Image Generate", style = MaterialTheme.typography.headlineMedium)
        Text("Geração local de imagens por difusão", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Modelo", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = modelUrl,
                    onValueChange = { modelUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL HTTPS do modelo") },
                    singleLine = false
                )
                Text(if (hasModel) "Modelo baixado: ${modelFile.name} (${modelFile.length() / (1024 * 1024)} MB)" else "Nenhum modelo instalado")
                Button(
                    enabled = !busy && modelUrl.isNotBlank(),
                    onClick = {
                        busy = true
                        progress = 0f
                        status = "Baixando modelo…"
                        scope.launch {
                            try {
                                downloader.download(modelUrl.trim(), modelFile) { progress = it }
                                hasModel = true
                                status = "Download concluído. O arquivo está disponível offline."
                            } catch (e: Exception) {
                                status = "Erro no download: ${e.message}"
                            } finally { busy = false }
                        }
                    }
                ) { Text("Baixar modelo") }
                if (busy && progress > 0f) {
                    androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Geração", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descreva a imagem") },
                    minLines = 3
                )
                Text("Steps: ${steps.toInt()}")
                Slider(value = steps, onValueChange = { steps = it }, valueRange = 4f..20f, steps = 15)
                Text("Backend")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = backend == ComputeBackend.CPU, onClick = { backend = ComputeBackend.CPU })
                    Text("CPU")
                    Spacer(Modifier.size(20.dp))
                    RadioButton(selected = backend == ComputeBackend.GPU, onClick = { backend = ComputeBackend.GPU })
                    Text("GPU (Vulkan, com fallback para CPU)")
                }
                Button(
                    enabled = !busy && prompt.isNotBlank() && hasModel,
                    onClick = {
                        busy = true
                        status = "Gerando imagem…"
                        scope.launch {
                            try {
                                image = engine.generate(modelFile, prompt.trim(), steps.toInt(), backend)
                                status = "Imagem gerada no dispositivo."
                            } catch (e: Exception) {
                                status = e.message ?: "Falha na geração."
                            } finally { busy = false }
                        }
                    }
                ) { Text("Gerar imagem") }
            }
        }

        if (busy) CircularProgressIndicator()
        Text(status, style = MaterialTheme.typography.bodyMedium)
        image?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "Imagem gerada", modifier = Modifier.fillMaxWidth().height(320.dp))
        }
        Text(
            "Depois de baixar o modelo, o arquivo fica no armazenamento privado do app. A geração não usa rede; o engine precisa estar compilado no APK.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
