package com.ejemplo.lab08

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TaskFilter { ALL, PENDING, COMPLETED }
enum class SortOption { FECHA, NOMBRE, ESTADO, PRIORIDAD }

class TaskViewModel(private val dao: TaskDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.FECHA)
    val sortOption = _sortOption.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing = _isCloudSyncing.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        dao.getAllTasks(),
        _searchQuery,
        _filter,
        _selectedCategory,
        _sortOption
    ) { allTasks, query, filter, category, sort ->
        allTasks.filter { task ->
            // 4. Búsqueda
            (query.isEmpty() || task.description.contains(query, ignoreCase = true)) &&
                    // 3. Filtro por estado
                    (when (filter) {
                        TaskFilter.ALL -> true
                        TaskFilter.PENDING -> !task.isCompleted
                        TaskFilter.COMPLETED -> task.isCompleted
                    }) &&
                    // 9. Filtro por Categoría
                    (category == "Todas" || task.category.equals(category, ignoreCase = true))
        }.let { filtered ->
            // 5. Ordenamiento
            when (sort) {
                SortOption.FECHA -> filtered.sortedByDescending { it.createdAt }
                SortOption.NOMBRE -> filtered.sortedBy { it.description.lowercase() }
                SortOption.ESTADO -> filtered.sortedBy { it.isCompleted }
                SortOption.PRIORIDAD -> filtered.sortedBy { it.priority.ordinal }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilter(filter: TaskFilter) { _filter.value = filter }
    fun setCategory(category: String) { _selectedCategory.value = category }
    fun setSortOption(sort: SortOption) { _sortOption.value = sort }

    // 1. Agregar Tarea
    fun addTask(description: String, priority: Priority, category: String, isRecurring: Boolean) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    description = description,
                    priority = priority,
                    category = category.ifBlank { "General" },
                    isRecurring = isRecurring
                )
            )
        }
    }

    // 1. Editar Tarea
    fun updateTask(task: Task, newDescription: String, newPriority: Priority, newCategory: String) {
        viewModelScope.launch {
            dao.updateTask(task.copy(description = newDescription, priority = newPriority, category = newCategory))
        }
    }

    // Toggle completada + 8. Tarea recurrente automática
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedState = !task.isCompleted
            dao.updateTask(task.copy(isCompleted = updatedState))

            // Si es recurrente y se marca completada, genera automáticamente la siguiente
            if (updatedState && task.isRecurring) {
                dao.insertTask(
                    Task(
                        description = "${task.description} (Recurrente)",
                        priority = task.priority,
                        category = task.category,
                        isRecurring = true
                    )
                )
            }
        }
    }

    // 2. Eliminar Tarea Individual
    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task) }
    }

    fun deleteAllTasks() {
        viewModelScope.launch { dao.deleteAllTasks() }
    }

    // 10. Sincronización en la Nube (Simulación de Sync API/Firebase)
    fun syncWithCloud(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            kotlinx.coroutines.delay(1500) // Simula tiempo de red
            _isCloudSyncing.value = false
            onComplete()
        }
    }
}