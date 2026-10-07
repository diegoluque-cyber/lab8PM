package com.ejemplo.lab08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.ejemplo.lab08.ui.theme.Lab08Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "tasks_db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val viewModel = TaskViewModel(db.taskDao())

        setContent {
            Lab08Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// 6. Función Auxiliar para Disparar Notificaciones Nativas
fun sendNotification(context: Context, title: String, message: String) {
    val channelId = "task_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Recordatorios de Tareas", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isSyncing by viewModel.isCloudSyncing.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val categories = listOf("Todas", "General", "Trabajo", "Estudios", "Hogar")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Manager Pro", fontWeight = FontWeight.Bold) },
                actions = {
                    // 10. Botón Sincronización en la Nube
                    IconButton(onClick = {
                        viewModel.syncWithCloud {
                            Toast.makeText(context, "¡Sincronizado con la Nube correctamente!", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar Nube", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 6. Botón Enviar Notificación de Recordatorio
                    IconButton(onClick = {
                        sendNotification(context, " Recordatorio de Tareas", "¡Tienes tareas pendientes por completar hoy!")
                        Toast.makeText(context, "Notificación enviada", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notificación", tint = MaterialTheme.colorScheme.tertiary)
                    }

                    // 5. Menú de Ordenar Tareas
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ordenar por Fecha") },
                                onClick = { viewModel.setSortOption(SortOption.FECHA); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordenar por Nombre") },
                                onClick = { viewModel.setSortOption(SortOption.NOMBRE); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordenar por Prioridad") },
                                onClick = { viewModel.setSortOption(SortOption.PRIORIDAD); showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordenar por Estado") },
                                onClick = { viewModel.setSortOption(SortOption.ESTADO); showSortMenu = false }
                            )
                        }
                    }

                    if (tasks.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteAllTasks() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar todo", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva Tarea") },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 4. Barra de Búsqueda de Tareas
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Buscar tareas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )

            // 9. Filtro por Categorías
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCategory(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            // 3. Filtros Estado (Todas, Pendientes, Completadas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == TaskFilter.ALL,
                    onClick = { viewModel.setFilter(TaskFilter.ALL) },
                    label = { Text("Todas") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.PENDING,
                    onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                    label = { Text("Pendientes") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.COMPLETED,
                    onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    label = { Text("Completadas") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de Tareas
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron tareas.",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task) } // 2. Eliminar Tarea Individual
                        )
                    }
                }
            }
        }
    }

    // Diálogo para Agregar Tarea
    if (showAddDialog) {
        TaskAddEditDialog(
            title = "Nueva Tarea",
            initialText = "",
            initialPriority = Priority.MEDIA,
            initialCategory = "General",
            initialIsRecurring = false,
            onDismiss = { showAddDialog = false },
            onConfirm = { text, priority, category, isRecurring ->
                if (text.isNotBlank()) {
                    viewModel.addTask(text, priority, category, isRecurring)
                }
                showAddDialog = false
            }
        )
    }

    // 1. Diálogo para Editar Tarea
    taskToEdit?.let { task ->
        TaskAddEditDialog(
            title = "Editar Tarea",
            initialText = task.description,
            initialPriority = task.priority,
            initialCategory = task.category,
            initialIsRecurring = task.isRecurring,
            onDismiss = { taskToEdit = null },
            onConfirm = { text, priority, category, _ ->
                if (text.isNotBlank()) {
                    viewModel.updateTask(task, text, priority, category)
                }
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = task.isCompleted, onClick = onToggle)

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.description,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // 7. Etiqueta de Prioridad
                    val (priorityColor, priorityText) = when (task.priority) {
                        Priority.ALTA -> Color(0xFFE53935) to "Alta"
                        Priority.MEDIA -> Color(0xFFFB8C00) to "Media"
                        Priority.BAJA -> Color(0xFF4CAF50) to "Baja"
                    }
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = priorityText,
                            color = priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 9. Etiqueta de Categoría
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.category,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 8. Indicador Tarea Recurrente
                    if (task.isRecurring) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurrente",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 1. Botón Editar
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
            }

            // 2. Botón Eliminar
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TaskAddEditDialog(
    title: String,
    initialText: String,
    initialPriority: Priority,
    initialCategory: String,
    initialIsRecurring: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Priority, String, Boolean) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedPriority by remember { mutableStateOf(initialPriority) }
    var categoryText by remember { mutableStateOf(initialCategory) }
    var isRecurring by remember { mutableStateOf(initialIsRecurring) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Descripción de la tarea") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. Selector de Prioridad (Alta, Media, Baja)
                Text("Prioridad:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.name) }
                        )
                    }
                }

                // 9. Categoría de la Tarea
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Categoría (ej: Trabajo, Hogar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 8. Tarea Recurrente Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Repetir tarea al completarla (Recurrente)")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text, selectedPriority, categoryText, isRecurring) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}