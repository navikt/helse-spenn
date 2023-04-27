package no.nav.helse.spenn.avstemming


import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DataSourceBuilderTest {

    @Test
    internal fun `kaster ikke exception når tilkobling konfigureres riktig`() {
        assertDoesNotThrow {
            DataSourceBuilder(mapOf(
                "DATABASE_HOST" to "foobar",
                "DATABASE_PORT" to "foobar",
                "DATABASE_DATABASE" to "foobar",
                "DATABASE_USERNAME" to "foobar",
                "DATABASE_PASSWORD" to "foobar"
            ))
        }

        assertDoesNotThrow {
            DataSourceBuilder(mapOf(
                "DATABASE_JDBC_URL" to "foobar",
                "DATABASE_USERNAME" to "foobar",
                "DATABASE_PASSWORD" to "foobar"
            ))
        }
    }

    @Test
    internal fun `kaster exception ved mangende konfig`() {
        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(emptyMap())
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar",
                    "DATABASE_DATABASE" to "foobar"
                )
            )
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(mapOf(
                "DATABASE_JDBC_URL" to "foobar"
            ))
        }

        assertThrows<IllegalArgumentException> {
            DataSourceBuilder(
                mapOf(
                    "DATABASE_HOST" to "foobar",
                    "DATABASE_PORT" to "foobar",
                    "DATABASE_DATABASE" to "foobar",
                    "DATABASE_USERNAME" to "foobar"
                )
            )
        }
    }
}
