package fr.carnetdefaerun.core.model.builds

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Code de partage d'un build : texte court à copier ou à mettre en QR code, lu sans réseau.
 * Format : préfixe [PREFIX], puis le JSON du build compressé (deflate) en Base64 sans remplissage, sûr pour les URL.
 * La lecture refuse tout ce qui dépasse les bornes (taille du code, taille décompressée, niveaux, nom) : un code
 * reçu d'ailleurs est une donnée non fiable.
 */
object BuildCode {
    const val PREFIX = "CDF1."
    private const val MAX_CODE_LENGTH = 4_096
    private const val MAX_JSON_BYTES = 16_384
    const val MAX_NAME_LENGTH = 60

    private val json = BuildFormat.json

    fun encode(build: Build): String {
        val bytes = json.encodeToString(Build.serializer(), build).toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(bytes)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1_024)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /** Le build d'un code, ou null s'il est mal formé, trop grand ou incohérent. Les espaces autour sont ignorés. */
    fun decode(code: String): Build? {
        val trimmed = code.trim()
        if (!trimmed.startsWith(PREFIX) || trimmed.length > MAX_CODE_LENGTH) return null
        return runCatching {
            val compressed = Base64.getUrlDecoder().decode(trimmed.removePrefix(PREFIX))
            val inflater = Inflater(true)
            inflater.setInput(compressed)
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(1_024)
            while (!inflater.finished()) {
                val n = inflater.inflate(buffer)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                out.write(buffer, 0, n)
                require(out.size() <= MAX_JSON_BYTES)
            }
            val complete = inflater.finished()
            inflater.end()
            require(complete)
            json.decodeFromString(Build.serializer(), out.toString(Charsets.UTF_8))
        }.getOrNull()?.takeIf { it.isPlausible() }
    }

    private fun Build.isPlausible(): Boolean =
        name.length <= MAX_NAME_LENGTH && levels.size <= 12 && baseScores.values.all { it in 1..30 } &&
            levels.all { l -> l.feat?.abilityIncreases?.values?.all { it in 1..2 } ?: true }
}
