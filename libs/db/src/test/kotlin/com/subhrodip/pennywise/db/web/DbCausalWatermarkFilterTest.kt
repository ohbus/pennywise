package com.subhrodip.pennywise.db.web

import com.subhrodip.pennywise.db.routing.DbCausalContext
import com.subhrodip.pennywise.db.routing.DbWatermarkHeaders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource

class DbCausalWatermarkFilterTest {
    @Test
    fun `scopes required watermark and emits writer watermark after mutation`() {
        val dataSource = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        val statement = mock(PreparedStatement::class.java)
        val result = mock(ResultSet::class.java)
        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.prepareStatement("SELECT pg_current_wal_lsn()::text")).thenReturn(statement)
        `when`(statement.executeQuery()).thenReturn(result)
        `when`(result.next()).thenReturn(true)
        `when`(result.getString(1)).thenReturn("0/20")

        val request = MockHttpServletRequest("POST", "/groups")
        request.addHeader(DbWatermarkHeaders.REQUIRED_WATERMARK, "0/10")
        val response = MockHttpServletResponse()
        var observed: String? = null

        DbCausalWatermarkFilter(dataSource).doFilter(
            request,
            response,
            { _, _ -> observed = DbCausalContext.requiredWatermark() }
        )

        assertEquals("0/10", observed)
        assertEquals("0/20", response.getHeader(DbWatermarkHeaders.WRITER_WATERMARK))
        assertEquals(null, DbCausalContext.requiredWatermark())
    }
}
