package dev.lumas.sleepy.config

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.Register
import dev.lumas.core.manager.Services
import dev.lumas.core.model.Service
import dev.lumas.core.util.PluginContextLogger
import dev.lumas.sleepy.Sleepy
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator
import net.kyori.adventure.translation.GlobalTranslator
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.Properties

@Register(Autowire.SERVICE)
class TranslatorService : MiniMessageTranslator(), Service {
    @Volatile
    private var translations: Map<Locale, Properties> = emptyMap()

    @Volatile
    var defaultLocale: Locale = Locale.US

    override fun register() {
        LOGGER.info("Registering translation service")
        reload()
        GlobalTranslator.translator().addSource(this)
        LOGGER.info("Translation service registered")
    }

    override fun unregister() {
        GlobalTranslator.translator().removeSource(this)
        LOGGER.info("Translation service unregistered")
    }

    @Synchronized
    fun reload() {
        val directory = Sleepy.instance.dataPath.resolve("locale")
        val defaultFile = directory.resolve(DEFAULT_FILE)
        try {
            Files.createDirectories(directory)
            syncDefaultFile(defaultFile)
            val loaded = mutableMapOf<Locale, Properties>()
            Files.list(directory).use { paths ->
                paths.filter { it.fileName.toString().endsWith(LANG_SUFFIX) }
                    .forEach { loadOne(it, loaded) }
            }
            defaultLocale = Locale.forLanguageTag(SleepyConfig.instance.locale)
            check(loaded.containsKey(defaultLocale)) {
                "No locale file for ${defaultLocale.toLanguageTag()}"
            }
            translations = loaded.toMap()
            LOGGER.info("Loaded ${loaded.size} locale file(s); default locale is ${defaultLocale.toLanguageTag()}")
        } catch (exception: IOException) {
            throw IllegalStateException("Unable to load Sleepy locale files", exception)
        }
    }

    private fun syncDefaultFile(external: Path) {
        javaClass.getResourceAsStream("/locale/$DEFAULT_FILE").use { stream ->
            checkNotNull(stream) { "Bundled locale is missing" }
            val temporary = external.resolveSibling("$DEFAULT_FILE.bundled")
            Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING)
            if (Files.notExists(external)) {
                Files.move(temporary, external, StandardCopyOption.REPLACE_EXISTING)
                return
            }

            val bundled = readProperties(temporary)
            val current = readProperties(external)
            var changed = migrateLocaleKeys(current)
            bundled.stringPropertyNames().forEach { key ->
                if (!current.containsKey(key)) {
                    current.setProperty(key, bundled.getProperty(key))
                    changed = true
                }
            }
            Files.deleteIfExists(temporary)
            if (changed) {
                Files.newBufferedWriter(external, StandardCharsets.UTF_8).use { writer ->
                    current.store(writer, "Sleepy locale; values support MiniMessage")
                }
            }
        }
    }

    private fun migrateLocaleKeys(properties: Properties): Boolean {
        var changed = false
        val legacyCapitalized = LEGACY_PLURAL.replaceFirstChar(Char::uppercase)
        val currentCapitalized = CURRENT_PLURAL.replaceFirstChar(Char::uppercase)
        properties.stringPropertyNames()
            .filter { it.contains(LEGACY_PLURAL) }
            .forEach { legacyKey ->
                val replacement = legacyKey.replace(LEGACY_PLURAL, CURRENT_PLURAL)
                if (!properties.containsKey(replacement)) {
                    properties.setProperty(replacement, properties.getProperty(legacyKey))
                }
                properties.remove(legacyKey)
                changed = true
            }
        properties.stringPropertyNames().forEach { key ->
            val current = properties.getProperty(key)
            val migrated = current
                .replace(LEGACY_PLURAL, CURRENT_PLURAL)
                .replace(legacyCapitalized, currentCapitalized)
            if (migrated != current) {
                properties.setProperty(key, migrated)
                changed = true
            }
        }
        return changed
    }

    private fun loadOne(path: Path, loaded: MutableMap<Locale, Properties>) {
        try {
            val name = path.fileName.toString()
            val tag = name.removeSuffix(LANG_SUFFIX)
            loaded[Locale.forLanguageTag(tag)] = readProperties(path)
        } catch (exception: IOException) {
            LOGGER.warning("Unable to load locale ${path.fileName}: ${exception.message}", exception)
        }
    }

    private fun readProperties(path: Path): Properties = Properties().apply {
        Files.newBufferedReader(path, StandardCharsets.UTF_8).use(::load)
    }

    override fun name(): Key = TRANSLATOR_KEY

    override fun getMiniMessageString(key: String, locale: Locale): String? {
        var selected: Properties? = null
        if (SleepyConfig.instance.clientSideTranslations) {
            selected = translations[locale]
            if (selected == null || !selected.containsKey(key)) {
                selected = translations[Locale.forLanguageTag(locale.language)]
            }
        }
        if (selected == null || !selected.containsKey(key)) selected = translations[defaultLocale]
        return selected?.getProperty(key)
    }

    companion object {
        private val LOGGER = PluginContextLogger.getPluginLogger()
        private const val LANG_SUFFIX = ".lang.properties"
        private const val DEFAULT_FILE = "en-US.lang.properties"
        private const val LEGACY_PLURAL = "oneira" + "s"
        private const val CURRENT_PLURAL = "oneira"
        private val TRANSLATOR_KEY = Key.key("sleepy", "translator")

        val instance: TranslatorService
            get() = Services.getTracked(TranslatorService::class.java) as TranslatorService
    }
}
