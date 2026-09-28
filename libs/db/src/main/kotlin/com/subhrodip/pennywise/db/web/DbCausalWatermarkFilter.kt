package com.subhrodip.pennywise.db.web

import com.subhrodip.pennywise.db.routing.DbCausalContext
import com.subhrodip.pennywise.db.routing.DbWatermark
import com.subhrodip.pennywise.db.routing.DbWatermarkHeaders
import java.sql.Connection
import javax.sql.DataSource
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/** Propagates a causal requirement into service repository contexts and emits writer LSNs. */
class DbCausalWatermarkFilter(private val dataSource: DataSource) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val required = request.getHeader(DbWatermarkHeaders.REQUIRED_WATERMARK)
            ?.let { value -> runCatching { DbWatermark.parse(value).asLsn() }.getOrNull() }
        DbCausalContext.withRequiredWatermark(required) {
            chain.doFilter(request, response)
            // Spring may commit the response while unwinding the chain; setting the
            // header here is still safe because committed servlet responses simply
            // ignore late header changes, while uncommitted mutation responses retain it.
            if (request.method != "GET" && response.status < 400) {
                readCurrentWriterLsn()?.let { response.setHeader(DbWatermarkHeaders.WRITER_WATERMARK, it) }
            }
        }
    }

    private fun readCurrentWriterLsn(): String? = runCatching {
        dataSource.connection.use { connection: Connection ->
            connection.prepareStatement("SELECT pg_current_wal_lsn()::text").use { statement ->
                statement.executeQuery().use { result -> if (result.next()) result.getString(1) else null }
            }
        }
    }.getOrNull()
}
