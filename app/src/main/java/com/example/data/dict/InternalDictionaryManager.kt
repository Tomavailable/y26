package com.example.data.dict

import android.content.Context
import com.example.data.model.WordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

enum class InternalDictType(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val shortName: String
) {
    DEFAULT(
        id = "default",
        displayName = "默认 (朗文当代)",
        subtitle = "朗文当代核心释义 · 原生权威双语例句",
        shortName = "朗文"
    ),
    RED_STAR(
        id = "red_star",
        displayName = "红星 (麦克米伦)",
        subtitle = "麦克米伦高阶红星释义 · 精选3个地道双语例句",
        shortName = "红星"
    ),
    CAMBRIDGE(
        id = "cambridge",
        displayName = "剑桥核心",
        subtitle = "剑桥核心学习者释义 · 精选3个地道双语例句",
        shortName = "剑桥"
    );

    companion object {
        fun fromId(id: String): InternalDictType {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

data class CompactDictEntry(
    val definition: String,
    val exampleSentence: String,
    val exampleTranslation: String
)

class InternalDictionaryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("internal_dict_prefs", Context.MODE_PRIVATE)

    private val _currentDictType = MutableStateFlow(
        InternalDictType.fromId(prefs.getString(KEY_DICT_TYPE, InternalDictType.DEFAULT.id) ?: "default")
    )
    val currentDictType: StateFlow<InternalDictType> = _currentDictType.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory lazy caches
    private val macmillanMap = mutableMapOf<String, CompactDictEntry>()
    private val cambridgeMap = mutableMapOf<String, CompactDictEntry>()

    private var isMacmillanLoading = false
    private var isCambridgeLoading = false

    init {
        // Preload if user had previously selected an external dict
        when (_currentDictType.value) {
            InternalDictType.RED_STAR -> loadMacmillanAsync()
            InternalDictType.CAMBRIDGE -> loadCambridgeAsync()
            else -> {}
        }
    }

    fun setDictType(type: InternalDictType) {
        if (_currentDictType.value != type) {
            _currentDictType.value = type
            prefs.edit().putString(KEY_DICT_TYPE, type.id).apply()
            when (type) {
                InternalDictType.RED_STAR -> loadMacmillanAsync()
                InternalDictType.CAMBRIDGE -> loadCambridgeAsync()
                else -> {}
            }
        }
    }

    private fun loadMacmillanAsync() {
        if (macmillanMap.isNotEmpty() || isMacmillanLoading) return
        isMacmillanLoading = true
        scope.launch {
            loadDictionaryFile("dict_macmillan.tsv", macmillanMap)
            isMacmillanLoading = false
        }
    }

    private fun loadCambridgeAsync() {
        if (cambridgeMap.isNotEmpty() || isCambridgeLoading) return
        isCambridgeLoading = true
        scope.launch {
            loadDictionaryFile("dict_cambridge.tsv", cambridgeMap)
            isCambridgeLoading = false
        }
    }

    private fun ensureLoadedSync(type: InternalDictType) {
        when (type) {
            InternalDictType.RED_STAR -> {
                if (macmillanMap.isEmpty()) {
                    synchronized(macmillanMap) {
                        if (macmillanMap.isEmpty()) {
                            loadDictionaryFile("dict_macmillan.tsv", macmillanMap)
                        }
                    }
                }
            }
            InternalDictType.CAMBRIDGE -> {
                if (cambridgeMap.isEmpty()) {
                    synchronized(cambridgeMap) {
                        if (cambridgeMap.isEmpty()) {
                            loadDictionaryFile("dict_cambridge.tsv", cambridgeMap)
                        }
                    }
                }
            }
            InternalDictType.DEFAULT -> {}
        }
    }

    private fun loadDictionaryFile(fileName: String, targetMap: MutableMap<String, CompactDictEntry>) {
        try {
            context.assets.open(fileName).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var isHeader = true
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (isHeader) {
                            isHeader = false
                            continue
                        }
                        val l = line ?: continue
                        val parts = l.split('\t')
                        if (parts.size >= 3) {
                            val word = parts[0].trim().lowercase()
                            val def = parts[1].replace("\\n", "\n").trim()
                            val exJson = parts[2].trim()

                            var exEn = ""
                            var exCn = ""
                            if (exJson.isNotBlank() && exJson.startsWith("[")) {
                                try {
                                    val arr = JSONArray(exJson)
                                    val enList = mutableListOf<String>()
                                    val cnList = mutableListOf<String>()
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        val en = obj.optString("en", "").trim()
                                        val cn = obj.optString("cn", "").trim()
                                        if (en.isNotBlank()) {
                                            enList.add(en)
                                            cnList.add(cn)
                                        }
                                    }
                                    exEn = enList.joinToString(" ||| ")
                                    exCn = cnList.joinToString(" ||| ")
                                } catch (e: Exception) {
                                    // ignore json parse error
                                }
                            }

                            targetMap[word] = CompactDictEntry(
                                definition = def,
                                exampleSentence = exEn,
                                exampleTranslation = exCn
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resolveWord(word: WordEntity, dictType: InternalDictType = _currentDictType.value): WordEntity {
        return when (dictType) {
            InternalDictType.DEFAULT -> word
            InternalDictType.RED_STAR -> {
                ensureLoadedSync(InternalDictType.RED_STAR)
                val key = word.word.lowercase()
                val entry = macmillanMap[key]
                if (entry != null) {
                    word.copy(
                        definition = entry.definition.ifBlank { word.definition },
                        exampleSentence = if (entry.exampleSentence.isNotBlank()) entry.exampleSentence else word.exampleSentence,
                        exampleTranslation = if (entry.exampleSentence.isNotBlank()) entry.exampleTranslation else word.exampleTranslation
                    )
                } else {
                    word
                }
            }
            InternalDictType.CAMBRIDGE -> {
                ensureLoadedSync(InternalDictType.CAMBRIDGE)
                val key = word.word.lowercase()
                val entry = cambridgeMap[key]
                if (entry != null) {
                    word.copy(
                        definition = entry.definition.ifBlank { word.definition },
                        exampleSentence = if (entry.exampleSentence.isNotBlank()) entry.exampleSentence else word.exampleSentence,
                        exampleTranslation = if (entry.exampleSentence.isNotBlank()) entry.exampleTranslation else word.exampleTranslation
                    )
                } else {
                    word
                }
            }
        }
    }

    companion object {
        private const val KEY_DICT_TYPE = "key_selected_internal_dict_type"
    }
}
