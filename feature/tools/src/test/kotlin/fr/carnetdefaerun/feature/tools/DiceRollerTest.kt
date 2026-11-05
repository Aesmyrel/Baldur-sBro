package fr.carnetdefaerun.feature.tools

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

class DiceRollerTest {
    @Test
    fun `réservoir borné, étendue et moyenne exactes`() {
        var pool = DicePool().add(6).add(6).add(8).copy(modifier = 3)
        assertThat(pool.dice).containsExactly(6, 6, 8).inOrder()
        assertThat(pool.minimum).isEqualTo(6)
        assertThat(pool.maximum).isEqualTo(23)
        assertThat(pool.mean).isWithin(1e-9).of(3.5 + 3.5 + 4.5 + 3)
        pool = pool.remove(6).remove(6).remove(6)
        assertThat(pool.counts).containsExactly(8, 1)
        repeat(30) { pool = pool.add(20) }
        assertThat(pool.dice).hasSize(DicePool.MAX_DICE)
    }

    @Test
    fun `chaque face reste dans son dé, et toutes sortent`() {
        val pool = DIE_SIDES.fold(DicePool()) { p, s -> p.add(s) }
        val random = Random(42)
        val seen = DIE_SIDES.associateWith { mutableSetOf<Int>() }
        repeat(5_000) {
            val roll = pool.roll(random)
            roll.faces.zip(pool.dice).forEach { (face, sides) ->
                assertThat(face).isIn(1..sides)
                seen.getValue(sides) += face
            }
            assertThat(roll.total).isEqualTo(roll.faces.sum())
        }
        for (sides in DIE_SIDES.filter { it <= 20 }) assertThat(seen.getValue(sides)).hasSize(sides)
    }
}
