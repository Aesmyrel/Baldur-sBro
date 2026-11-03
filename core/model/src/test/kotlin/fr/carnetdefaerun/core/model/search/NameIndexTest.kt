package fr.carnetdefaerun.core.model.search

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.ContentId
import org.junit.Test

class NameIndexTest {
    private fun name(id: String, text: String, kind: NameKind = NameKind.FR) = IndexedName(ContentId(id), Folding.fold(text), kind)

    private val index = NameIndex(
        listOf(
            name("spell.fireball", "Boule de feu"),
            name("spell.fireball", "Fireball", NameKind.EN),
            name("spell.fire-bolt", "Trait de feu"),
            name("spell.fire-bolt", "Fire Bolt", NameKind.EN),
            name("spell.bless", "Bénédiction"),
            name("spell.bless", "Bless", NameKind.EN),
            name("spell.blur", "Flou"),
            name("spell.blur", "Blur", NameKind.EN),
            name("feature.fey-ancestry", "Ascendance féerique"),
            name("item.eye", "Œil de verre"),
            name("race.high-elf", "Haut-elfe"),
        ),
    )

    private fun ids(query: String) = index.search(query).map { it.id.value }

    @Test
    fun `nom exact, puis préfixe, puis mot, puis faute de frappe`() {
        assertThat(index.search("boule de feu").first().quality).isEqualTo(MatchQuality.EXACT)
        assertThat(index.search("boule").first().quality).isEqualTo(MatchQuality.PREFIX)
        val feu = index.search("feu")
        assertThat(feu.take(2).map { it.id.value }).containsExactly("spell.fireball", "spell.fire-bolt")
        assertThat(feu.take(2).map { it.quality }.toSet()).containsExactly(MatchQuality.WORD)
        assertThat(feu.drop(2).map { it.quality }.toSet()).containsExactly(MatchQuality.TYPO) // « fée… »
        assertThat(index.search("bolue").first().quality).isEqualTo(MatchQuality.TYPO)
    }

    @Test
    fun `accents, ligatures, casse et apostrophes ignorés`() {
        assertThat(ids("benediction")).containsExactly("spell.bless")
        assertThat(ids("OEIL")).containsExactly("item.eye")
        assertThat(ids("feerique")).containsExactly("feature.fey-ancestry")
        assertThat(ids("haut elfe")).containsExactly("race.high-elf")
        assertThat(ids("hautelfe")).containsExactly("race.high-elf")
    }

    @Test
    fun `nom anglais trouvé, nom français préféré à qualité égale`() {
        assertThat(ids("fireball")).containsExactly("spell.fireball")
        val fire = index.search("fire")
        assertThat(fire.map { it.id.value }).containsExactly("spell.fireball", "spell.fire-bolt")
        assertThat(fire.all { it.kind == NameKind.EN }).isTrue()
    }

    @Test
    fun `mots dans le désordre et abrégés`() {
        assertThat(ids("feu boule")).containsExactly("spell.fireball")
        assertThat(ids("bou de f")).containsExactly("spell.fireball")
        assertThat(ids("de feu").toSet()).containsExactly("spell.fireball", "spell.fire-bolt")
    }

    @Test
    fun `une faute jusqu'à cinq lettres, deux au-delà, aucune sous trois lettres`() {
        assertThat(ids("bolue")).contains("spell.fireball") // transposition
        assertThat(ids("bebediction")).containsExactly("spell.bless")
        assertThat(ids("benedcition")).containsExactly("spell.bless")
        assertThat(ids("bxnxdiction")).containsExactly("spell.bless")
        assertThat(ids("bxnxdxction")).isEmpty()
        assertThat(ids("flxu")).containsExactly("spell.blur")
        assertThat(ids("fxxu")).isEmpty() // deux fautes sur quatre lettres
        assertThat(ids("fl")).containsExactly("spell.blur")
        assertThat(ids("fx")).isEmpty()
    }

    @Test
    fun `faute pendant la frappe, sur le début du mot`() {
        assertThat(ids("benr")).containsExactly("spell.bless")
        assertThat(ids("ascendence")).containsExactly("feature.fey-ancestry")
    }

    @Test
    fun `une entrée n'apparaît qu'une fois, avec sa meilleure correspondance`() {
        val results = index.search("bl")
        assertThat(results.map { it.id.value }).containsNoDuplicates()
        assertThat(results.map { it.id.value }).containsExactly("spell.bless", "spell.blur")
    }

    @Test
    fun `recherche vide ou sans lettre`() {
        assertThat(index.search("")).isEmpty()
        assertThat(index.search(" ’ - ")).isEmpty()
    }

    @Test
    fun `distance de Damerau-Levenshtein`() {
        assertThat(NameIndex.damerauLevenshtein("ab", "ba")).isEqualTo(1)
        assertThat(NameIndex.damerauLevenshtein("kitten", "sitting")).isEqualTo(3)
        assertThat(NameIndex.damerauLevenshtein("", "abc")).isEqualTo(3)
        assertThat(NameIndex.damerauLevenshtein("abc", "abcdef", len = 3)).isEqualTo(0)
        assertThat(NameIndex.damerauLevenshtein("abcdef", "uvwxyz", max = 2)).isEqualTo(3)
    }
}
