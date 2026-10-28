package fr.carnetdefaerun.core.model.content

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerLevel
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import org.junit.Test

class ContentFormatTest {
    private val entry = Entry(
        id = ContentId("spell.fire-bolt"),
        type = EntryType.SPELL,
        name = Name("Trait de feu", "Fire Bolt", NameStatus.VF_DND, "aidedd"),
        tag = SpoilerTag.SAFE,
        titleTag = SpoilerTag.SAFE,
        licence = "cc-by-nc-sa-4.0",
        attribution = listOf(Attribution("bg3wiki", "Fire Bolt", 1, "https://bg3.wiki/wiki/Fire_Bolt")),
        facts = SpellFacts(level = 0, school = SpellSchool.EVOCATION, damage = listOf(Damage("1d10", DamageType.FIRE))),
        links = listOf(Link(ContentId("item.x"), Relation.GRANTED_BY, SpoilerTag(Act.ACT_2, SpoilerLevel.NONE, SpoilerLevel.NONE))),
    )

    @Test
    fun `aller-retour JSON sans perte`() {
        val json = ContentFormat.pretty.encodeToString(Entry.serializer(), entry)
        assertThat(ContentFormat.strict.decodeFromString(Entry.serializer(), json)).isEqualTo(entry)
        assertThat(json).contains("\"@type\": \"spell\"")
    }

    @Test
    fun `une entrée sans étiquette est refusée`() {
        val json = ContentFormat.strict.encodeToString(Entry.serializer(), entry).replace(Regex("\"tag\":\\{[^}]*\\},"), "")
        val failure = runCatching { ContentFormat.strict.decodeFromString(Entry.serializer(), json) }.exceptionOrNull()
        assertThat(failure).isNotNull()
    }

    @Test
    fun `un lien sans étiquette calculée reste verrouillé`() {
        val link = ContentFormat.strict.decodeFromString(Link.serializer(), """{"target":"item.x","relation":"grants"}""")
        assertThat(link.tag).isEqualTo(SpoilerTag.LOCKED)
    }

    @Test
    fun `identifiants dérivés du nom anglais`() {
        assertThat(ContentFormat.slug("Tasha's Hideous Laughter")).isEqualTo("tashas-hideous-laughter")
        assertThat(ContentFormat.slug("Selûne")).isEqualTo("selune")
        assertThat(ContentFormat.slug("Branding Smite (Melee)")).isEqualTo("branding-smite-melee")
        assertThat(ContentFormat.id("item", "Sword & Board").value).isEqualTo("item.sword-and-board")
    }
}
