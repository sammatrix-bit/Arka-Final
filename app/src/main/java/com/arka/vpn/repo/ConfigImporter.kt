package com.arka.vpn.repo

import android.content.Context
import com.arka.vpn.data.AppDatabase
import com.arka.vpn.data.ConfigEntity
import com.arka.vpn.model.ConnectionMode
import com.arka.vpn.parser.ConfigParser
import java.io.BufferedReader
import java.io.InputStreamReader

class ConfigImporter(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(appContext).configDao()

    private fun readAsset(fileName: String): String =
        try {
            appContext.assets.open(fileName).use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
        } catch (e: Exception) {
            ""
        }

    suspend fun isDatabaseEmpty(): Boolean = dao.countAll() == 0

    suspend fun importFromAssets(): Map<ConnectionMode, Int> {
        val result = LinkedHashMap<ConnectionMode, Int>()
        for (mode in ConnectionMode.entries) {
            val raw = readAsset(mode.assetFile)
            val links = ConfigParser.parseLines(raw)
            result[mode] = insert(mode, links)
        }
        return result
    }

    suspend fun importFromClipboard(mode: ConnectionMode, clipboardText: String): Int {
        val links = ConfigParser.parseLines(clipboardText)
        return insert(mode, links)
    }

    private suspend fun insert(mode: ConnectionMode, links: List<String>): Int {
        if (links.isEmpty()) return 0
        val entities = links.map { link ->
            ConfigEntity(
                category = mode.key,
                protocol = ConfigParser.protocolOf(link).name.lowercase(),
                link = link,
                hash = ConfigParser.sha256(link),
                remark = ConfigParser.remarkOf(link)
            )
        }
        val insertedIds = dao.insertAll(entities)
        return insertedIds.count { it != -1L }
    }

    suspend fun countsByMode(): Map<ConnectionMode, Int> =
        ConnectionMode.entries.associateWith { dao.countByCategory(it.key) }

    suspend fun clearAll() = dao.deleteAll()

    suspend fun linksFor(mode: ConnectionMode): List<ConfigEntity> = dao.getByCategory(mode.key)
}
