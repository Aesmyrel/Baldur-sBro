import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestPermissionsTest {
    private fun manifest(body: String) = """
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="x">
        $body
        </manifest>
    """.trimIndent()

    @Test
    fun `INTERNET est refusé, sous toutes ses balises`() {
        val allowed = listOf("android.permission.VIBRATE")
        for (tag in listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")) {
            val declared = ManifestPermissions.declared(manifest("""<$tag android:name="android.permission.INTERNET"/>"""))
            assertEquals(listOf("android.permission.INTERNET"), ManifestPermissions.forbidden(declared, allowed))
        }
    }

    @Test
    fun `la liste blanche passe`() {
        val declared = ManifestPermissions.declared(manifest("""<uses-permission android:name="android.permission.VIBRATE"/>"""))
        assertEquals(emptyList<String>(), ManifestPermissions.forbidden(declared, listOf("android.permission.VIBRATE")))
    }
}
