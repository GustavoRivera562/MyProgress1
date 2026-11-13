package com.example.myprogres.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myprogres.domain.model.DiagnosticError
import com.example.myprogres.domain.model.ExerciseLibrary
import com.example.myprogres.domain.model.WorkoutSummary
import com.example.myprogres.feature.progress.ProgressUiState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val CardShape = RoundedCornerShape(24.dp)
private val ScreenPadding = 20.dp

@Composable
fun HomeScreen(state: ProgressUiState, onStartTraining: () -> Unit, onOpenWater: () -> Unit) {
    val completed = state.history.filter { it.finishedAt != null }
    val recentSetCount = state.history.sumOf { it.setCount }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("MYPROGRES  /  HOY", "Tu progreso,\na tu ritmo.", "Cada serie cuenta.")

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (state.activeWorkout == null) "¿Listo para entrenar?" else "Sesión en curso",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (state.activeWorkout == null) "Registra tu entrenamiento y guarda cada serie en este dispositivo."
                    else "${state.activeWorkout.sets.size} series registradas en esta sesión.",
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Button(
                    onClick = onStartTraining,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text(if (state.activeWorkout == null) "IR A ENTRENAR" else "CONTINUAR") }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            SectionTitle("Tu día")
            Text(
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(System.currentTimeMillis())),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hidratación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Meta orientativa · 2.500 ml", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${state.todayWaterMl} ml",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { (state.todayWaterMl / 2500f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                TextButton(onClick = onOpenWater) { Text("REGISTRAR AGUA") }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Metric("Sesiones recientes", completed.size.toString(), "De hasta 50 registros", Modifier.weight(1f))
            Metric("Series recientes", recentSetCount.toString(), "De hasta 50 sesiones", Modifier.weight(1f))
        }

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GUARDADO LOCAL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Tus registros se guardan primero en este dispositivo. La sincronización en la nube todavía no está activa.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.history.firstOrNull()?.let {
            SectionTitle("Actividad reciente")
            HistoryCard(it)
        }
    }
}

@Composable
fun TrainingScreen(
    state: ProgressUiState,
    onStartWorkout: () -> Unit,
    onFinishWorkout: (String) -> Unit,
    onAddSet: (String, String, String, String, String) -> Unit
) {
    val active = state.activeWorkout
    var selectedExercise by rememberSaveable { mutableStateOf(ExerciseLibrary.exercises.first().id) }
    var weight by rememberSaveable { mutableStateOf("") }
    var repetitions by rememberSaveable { mutableStateOf("") }
    var restSeconds by rememberSaveable { mutableIntStateOf(60) }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var timerStarted by rememberSaveable { mutableStateOf(false) }
    var restComplete by rememberSaveable { mutableStateOf(false) }
    val exercise = ExerciseLibrary.exercises.first { it.id == selectedExercise }

    LaunchedEffect(state.message) {
        if (state.message == "Serie guardada en este dispositivo.") {
            weight = ""
            repetitions = ""
        }
    }
    LaunchedEffect(timerRunning, restSeconds) {
        if (timerRunning && restSeconds > 0) {
            delay(1_000)
            restSeconds -= 1
        } else if (timerRunning) {
            timerRunning = false
            restComplete = true
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader(
            "FUERZA  /  REGISTRO",
            "Entrenamiento",
            if (active == null) "Tu sesión se guarda en este dispositivo."
            else "Sesión activa desde " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(active.workout.startedAt)) + "."
        )

        if (active == null) {
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Nueva sesión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Empieza cuando quieras. Puedes registrar tus series sin conexión.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onStartWorkout, enabled = !state.working) { Text("INICIAR SESIÓN") }
                }
            }
        } else {
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("SESIÓN ACTIVA", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            Text("${active.sets.size} series guardadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { onFinishWorkout(active.workout.id) }, enabled = !state.working) { Text("FINALIZAR") }
                    }
                    SectionTitle("Ejercicio")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExerciseLibrary.exercises.forEach { item ->
                            FilterChip(
                                selected = selectedExercise == item.id,
                                onClick = { selectedExercise = item.id },
                                label = { Text(item.name) }
                            )
                        }
                    }
                    Text(exercise.muscleGroup, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Peso (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = repetitions,
                            onValueChange = { repetitions = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Repeticiones") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Button(
                        onClick = { onAddSet(active.workout.id, exercise.id, exercise.name, weight, repetitions) },
                        enabled = !state.working,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("GUARDAR SERIE") }
                    Text("La serie se confirma al guardarse localmente.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) { SectionTitle("Descanso") }
                    Text(
                        if (timerRunning) "%02d:%02d".format(restSeconds / 60, restSeconds % 60)
                        else if (restComplete) "00:00" else "Elige tu descanso",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { minutes ->
                            FilterChip(
                                selected = restSeconds == minutes * 60 && !timerRunning && !restComplete,
                                onClick = {
                                    restSeconds = minutes * 60
                                    timerRunning = false
                                    timerStarted = false
                                    restComplete = false
                                },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                    if (!timerRunning && !restComplete) {
                        Button(onClick = { timerRunning = true; timerStarted = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (timerStarted) "REANUDAR" else "INICIAR TEMPORIZADOR")
                        }
                    } else if (timerRunning) {
                        OutlinedButton(onClick = { timerRunning = false }, modifier = Modifier.fillMaxWidth()) { Text("PAUSAR") }
                    } else {
                        Text("Descanso terminado. ¡Vamos con la siguiente serie!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { restComplete = false; timerStarted = false; restSeconds = 60 }) { Text("INICIAR OTRO DESCANSO") }
                    }
                }
            }

            SectionTitle("Series de esta sesión")
            if (active.sets.isEmpty()) {
                EmptyCard("Tus series aparecerán aquí después de guardarlas.")
            } else {
                active.sets.asReversed().forEach { set ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(set.exerciseName + " · Serie " + set.setNumber, fontWeight = FontWeight.SemiBold)
                                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(set.recordedAt)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatWeight(set.weightKg) + " kg × " + set.repetitions, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaterScreen(state: ProgressUiState, onAddWater: (String) -> Unit) {
    var customAmount by rememberSaveable { mutableStateOf("") }
    val targetMl = 2_500
    val progress = (state.todayWaterMl / targetMl.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(state.message) {
        if (state.message?.endsWith(" ml guardados.") == true) customAmount = ""
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("HÁBITOS  /  HIDRATACIÓN", "Agua", "Tu registro diario, guardado localmente.")

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(174.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(((progress * 100).toInt()).toString() + "%", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(state.todayWaterMl.toString() + " / " + targetMl + " ml", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text("Meta orientativa · " + targetMl + " ml", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Referencia general, no es una recomendación médica. La meta no se puede ajustar todavía.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        SectionTitle("Registro rápido")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(150, 250, 500).forEach { amount ->
                OutlinedButton(onClick = { onAddWater(amount.toString()) }, modifier = Modifier.weight(1f), enabled = !state.working) {
                    Text("+" + amount + " ml", color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Otra cantidad")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Mililitros") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(onClick = { onAddWater(customAmount) }, enabled = customAmount.isNotBlank() && !state.working) { Text("AÑADIR") }
                }
            }
        }

        Text(
            "El detalle de cada ingesta todavía no está disponible. El total de hoy sí se actualiza desde tus registros locales.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ProgressScreen(state: ProgressUiState) {
    val completed = state.history.filter { it.finishedAt != null }
    val totalVolume = completed.sumOf { it.totalVolumeKg }
    val recentSessions = completed.take(7).asReversed()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("CONSTANCIA  /  EVOLUCIÓN", "Tu progreso", "Resumen calculado con registros guardados en este dispositivo.")

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Metric("Sesiones", completed.size.toString(), "De hasta 50 registros", Modifier.weight(1f))
            Metric("Volumen", formatWeight(totalVolume) + " kg", "Sesiones completadas", Modifier.weight(1f))
        }

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle("Volumen por sesión")
                Text("Últimas sesiones completadas disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (recentSessions.isEmpty()) EmptyCard("Cuando guardes entrenamientos, verás aquí su volumen.")
                else VolumeChart(recentSessions)
            }
        }

        SectionTitle("Historial")
        if (state.history.isEmpty()) EmptyCard("Aún no hay entrenamientos. Tu primera sesión aparecerá aquí.")
        else state.history.forEach { workout -> HistoryCard(workout) }
    }
}

@Composable
fun ProfileScreen(
    state: ProgressUiState,
    email: String,
    sessionRestored: Boolean,
    onOpenDiagnostics: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ScreenHeader("PERFIL  /  PRIVACIDAD", "Tu espacio", "Cuenta autenticada · los registros siguen guardados localmente.")

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Cuenta")
                Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sessionRestored) {
                    Text(
                        "Firebase restauró una sesión anterior. Sin conexión, no se puede volver a validar con el servidor.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Firestore y sincronización todavía no están activos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Datos locales")
                Text(state.history.size.toString() + " sesiones disponibles en el historial reciente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.todayWaterMl.toString() + " ml registrados hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("El historial consultado contiene hasta 50 sesiones.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("VER DIAGNÓSTICO LOCAL") }
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("CERRAR SESIÓN")
        }
    }
}

@Composable
fun DiagnosticsScreen(state: ProgressUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        TextButton(onClick = onBack) { Text("← Perfil") }
        ScreenHeader("SISTEMA  /  PRIVACIDAD", "Estado local", "Información disponible en este dispositivo.")
        StatusCard("Base de datos", "Room · almacenamiento local", "Local")
        StatusCard("Cuenta", "Firebase Authentication · correo y contraseña", "Activa")
        StatusCard("Conectividad", "Entrenamiento e hidratación usan Room; un nuevo acceso requiere Internet", "Mixto")
        StatusCard(
            "Sincronización",
            "El contador es solo un marcador local; no hay worker activo",
            state.diagnostics.pendingSyncCount.toString() + " marcados"
        )
        SectionTitle("Errores recientes")
        if (state.diagnostics.recentErrors.isEmpty()) EmptyCard("No hay errores técnicos registrados.")
        else state.diagnostics.recentErrors.forEach { error -> ErrorCard(error) }
    }
}

@Composable
private fun ScreenHeader(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(eyebrow, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Metric(label: String, value: String, supportingText: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VolumeChart(sessions: List<WorkoutSummary>) {
    val maxVolume = max(sessions.maxOfOrNull { it.totalVolumeKg } ?: 0.0, 1.0)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sessions.forEach { workout ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(Date(workout.startedAt)),
                    modifier = Modifier.width(54.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
                LinearProgressIndicator(
                    progress = { (workout.totalVolumeKg / maxVolume).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(formatWeight(workout.totalVolumeKg) + " kg", modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(message, Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusCard(title: String, description: String, status: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(10.dp))
            Text(status, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryCard(workout: WorkoutSummary) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(workout.startedAt)), fontWeight = FontWeight.SemiBold)
                Text(if (workout.finishedAt == null) "EN CURSO" else "GUARDADO", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
            Text(
                workout.setCount.toString() + " series · " + formatWeight(workout.totalVolumeKg) + " kg de volumen",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(error: DiagnosticError) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(error.code + " · " + error.module, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(error.safeMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toLong().toString() else "%.1f".format(Locale.getDefault(), weight)
