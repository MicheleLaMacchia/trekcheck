package it.trekcheck.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node

class ManifestAndBackupConfigTest {
    private val androidNs = "http://schemas.android.com/apk/res/android"

    @Test
    fun manifestDisablesBackupLinksRulesAndRequestsNoSensitivePermissions() {
        val manifestFile = resolveAppMainFile("AndroidManifest.xml")
        val document = parseXml(manifestFile)

        val permissions = document.getElementsByTagName("uses-permission")
        assertEquals("Expected zero uses-permission entries in Story 1.1", 0, permissions.length)

        val sdk23Permissions = document.getElementsByTagName("uses-permission-sdk-23")
        assertEquals("Expected zero uses-permission-sdk-23 entries in Story 1.1", 0, sdk23Permissions.length)

        val applications = document.getElementsByTagName("application")
        assertEquals(1, applications.length)
        val application = applications.item(0) as Element

        assertEquals("false", application.getAttributeNS(androidNs, "allowBackup"))
        assertEquals("@xml/backup_rules", application.getAttributeNS(androidNs, "fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(androidNs, "dataExtractionRules"))
    }

    @Test
    fun backupRulesExcludePrivateFilesDatabaseAndSharedPrefs() {
        val backupRulesFile = resolveAppMainFile("res/xml/backup_rules.xml")
        val document = parseXml(backupRulesFile)
        assertEquals("full-backup-content", document.documentElement.tagName)
        assertEquals(0, document.getElementsByTagName("include").length)

        val excludes = collectDirectChildExcludes(document.documentElement)
        assertEquals(setOf("file", "database", "sharedpref"), excludes)
    }

    @Test
    fun dataExtractionRulesExcludeCloudBackupAndDeviceTransfer() {
        val rulesFile = resolveAppMainFile("res/xml/data_extraction_rules.xml")
        val document = parseXml(rulesFile)
        assertEquals("data-extraction-rules", document.documentElement.tagName)
        assertEquals(0, document.getElementsByTagName("include").length)

        val cloudBackupNodes = document.getElementsByTagName("cloud-backup")
        assertEquals(1, cloudBackupNodes.length)
        assertEquals(
            setOf("file", "database", "sharedpref"),
            collectDirectChildExcludes(cloudBackupNodes.item(0) as Element),
        )

        val deviceTransferNodes = document.getElementsByTagName("device-transfer")
        assertEquals(1, deviceTransferNodes.length)
        assertEquals(
            setOf("file", "database", "sharedpref"),
            collectDirectChildExcludes(deviceTransferNodes.item(0) as Element),
        )
    }

    @Test
    fun stringResourcesAvoidForbiddenSafetyClaims() {
        val resDir = resolveAppMainFile("res")
        val stringFiles = resDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("values")
        }?.mapNotNull { dir ->
            File(dir, "strings.xml").takeIf { it.exists() }
        }.orEmpty()

        assertTrue("Expected at least one values*/strings.xml file", stringFiles.isNotEmpty())

        val safeWordRegex = Regex("\\bsafe(ty)?\\b", RegexOption.IGNORE_CASE)
        stringFiles.forEach { stringsFile ->
            val rawContent = stringsFile.readText()
            assertFalse(
                "${stringsFile.path} must not contain Italian safety claims ('sicur*')",
                rawContent.lowercase().contains("sicur"),
            )
            assertFalse(
                "${stringsFile.path} must not contain English safety claims ('safe'/'safety')",
                safeWordRegex.containsMatchIn(rawContent),
            )
        }
    }

    private fun collectDirectChildExcludes(parent: Element): Set<String> {
        val excludedDomains = mutableSetOf<String>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                assertEquals("Unexpected child tag under ${parent.tagName}", "exclude", element.tagName)
                val domain = element.getAttribute("domain")
                val path = element.getAttribute("path")
                assertEquals("Expected root exclusion path '.' for $domain", ".", path)
                excludedDomains += domain
            }
        }
        return excludedDomains
    }

    private fun parseXml(file: File) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }.newDocumentBuilder().parse(file)

    private fun resolveAppMainFile(relativePath: String): File {
        val candidates = listOf(
            File("src/main/$relativePath"),
            File("app/src/main/$relativePath"),
        )
        val found = candidates.firstOrNull { it.exists() }
        assertNotNull("Could not find src/main/$relativePath from ${File(".").absolutePath}", found)
        return found!!
    }
}
