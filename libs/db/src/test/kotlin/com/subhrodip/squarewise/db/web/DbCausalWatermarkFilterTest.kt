package com.subhrodip.squarewise.db.web

import com.subhrodip.squarewise.db.routing.DbCausalContext
import com.subhrodip.squarewise.db.routing.DbWatermarkHeaders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.FilterChain
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
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

    /** Invalid incoming watermarks are ignored and do not make a read touch the writer. */
    @Test
    fun `invalid watermark is ignored for reads`() {
        val dataSource = mock(DataSource::class.java)
        val request = MockHttpServletRequest("GET", "/groups")
        request.addHeader(DbWatermarkHeaders.REQUIRED_WATERMARK, "invalid")
        val response = MockHttpServletResponse()
        var observed: String? = "sentinel"

        DbCausalWatermarkFilter(dataSource).doFilter(request, response, FilterChain { _, _ ->
            observed = DbCausalContext.requiredWatermark()
        })

        assertEquals(null, observed)
        assertEquals(null, response.getHeader(DbWatermarkHeaders.WRITER_WATERMARK))
        verifyNoInteractions(dataSource)
    }

    /** Error responses and empty writer-LSN results do not emit a causal response header. */
    @Test
    fun `failed mutation and empty lsn do not emit writer watermark`() {
        val dataSource = mock(DataSource::class.java)
        val connection = mock(Connection::class.java)
        val statement = mock(PreparedStatement::class.java)
        val result = mock(ResultSet::class.java)
        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.prepareStatement("SELECT pg_current_wal_lsn()::text")).thenReturn(statement)
        `when`(statement.executeQuery()).thenReturn(result)
        `when`(result.next()).thenReturn(false)

        val request = MockHttpServletRequest("POST", "/groups")
        val response = MockHttpServletResponse()
        DbCausalWatermarkFilter(dataSource).doFilter(request, response, FilterChain { _, servletResponse ->
            (servletResponse as HttpServletResponse).status = 400
        })
        assertEquals(null, response.getHeader(DbWatermarkHeaders.WRITER_WATERMARK))

        val successResponse = MockHttpServletResponse()
        DbCausalWatermarkFilter(dataSource).doFilter(request, successResponse, FilterChain { _, _ -> })
        assertEquals(null, successResponse.getHeader(DbWatermarkHeaders.WRITER_WATERMARK))
    }

    @Test
    fun `writer watermark lookup failure is fail safe`() {
        val dataSource = mock(DataSource::class.java)
        `when`(dataSource.connection).thenThrow(SQLException("writer unavailable"))
        val request = MockHttpServletRequest("POST", "/groups")
        val response = MockHttpServletResponse()

        DbCausalWatermarkFilter(dataSource).doFilter(request, response, FilterChain { _, _ -> })

        assertEquals(null, response.getHeader(DbWatermarkHeaders.WRITER_WATERMARK))
    }
}
