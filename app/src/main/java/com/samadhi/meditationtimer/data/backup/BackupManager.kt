package com.samadhi.meditationtimer.data.backup

import android.content.Context
import android.net.Uri
import com.samadhi.meditationtimer.data.backup.model.BackupRootDto
import com.samadhi.meditationtimer.data.backup.model.BackupSessionDto
import com.samadhi.meditationtimer.domain.model.MeditationSession
import com.samadhi.meditationtimer.domain.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

class BackupManager(
    private val context: Context,
    private val sessionRepository: SessionRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToJson(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val sessions = sessionRepository.getAllSessionsFlow().first()
            val backupRoot = BackupRootDto(
                sessions = sessions.map { BackupSessionDto.fromDomain(it) }
            )
            val jsonString = json.encodeToString(BackupRootDto.serializer(), backupRoot)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for URI: $uri"))

            Result.success(sessions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToCsv(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val sessions = sessionRepository.getAllSessionsFlow().first()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Header
                    writer.write("id,startTime,completedTime,targetDurationSec,actualDurationSec,overrunSec,rating,notesMarkdown\n")
                    for (s in sessions) {
                        val startIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(s.startTimeEpochMs))
                        val endIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(s.completedTimeEpochMs))
                        val escapedNotes = "\"" + s.notesMarkdown.replace("\"", "\"\"") + "\""
                        writer.write("${s.id},\"$startIso\",\"$endIso\",${s.targetDurationSec},${s.actualDurationSec},${s.overrunSec},${s.rating},$escapedNotes\n")
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for URI: $uri"))

            Result.success(sessions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportJournalMarkdown(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val sessions = sessionRepository.getAllSessionsFlow().first()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write("# Buddho Timer Journal\n\n")
                    writer.write("Exported ${DateTimeFormatter.ISO_INSTANT.format(Instant.now())}\n\n")
                    sessions.forEach { s ->
                        val completed = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(s.completedTimeEpochMs))
                        writer.write("## $completed\n\n")
                        writer.write("- Duration: ${s.actualDurationSec / 60} min\n")
                        writer.write("- Target: ${s.targetDurationSec / 60} min\n")
                        writer.write("- Overrun: ${s.overrunSec / 60} min\n")
                        writer.write("- Mood: ${if (s.rating > 0) s.rating.toString() else "unrated"}\n")
                        writer.write("- Type: ${if (s.isManual) "manual" else "timer"}\n\n")
                        if (s.notesMarkdown.isNotBlank()) {
                            writer.write(s.notesMarkdown.trim())
                            writer.write("\n\n")
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream for URI: $uri"))

            Result.success(sessions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJson(uri: Uri, overwrite: Boolean = false): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream for URI: $uri"))

            val jsonString = stringBuilder.toString()
            val backupRoot = json.decodeFromString(BackupRootDto.serializer(), jsonString)
            val newSessions = backupRoot.sessions.map { it.toDomain() }

            if (overwrite) {
                sessionRepository.deleteAllSessions()
                sessionRepository.insertAll(newSessions)
            } else {
                // Merge without duplicating existing startTimeEpochMs
                val existing = sessionRepository.getAllSessionsFlow().first()
                val existingTimestamps = existing.map { it.startTimeEpochMs }.toSet()
                val sessionsToInsert = newSessions.filter { !existingTimestamps.contains(it.startTimeEpochMs) }
                sessionRepository.insertAll(sessionsToInsert)
            }

            Result.success(newSessions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
