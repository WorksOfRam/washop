package com.washop.repository

import com.washop.models.Otps
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class OtpRepository {

    fun create(phone: String, otp: String, expiresInMinutes: Int = 5): Int = transaction {
        Otps.deleteWhere { (Otps.phone eq phone) and (Otps.isUsed eq false) }

        Otps.insert {
            it[Otps.phone] = phone
            it[Otps.otp] = otp
            it[expiresAt] = Instant.now().plusSeconds(expiresInMinutes * 60L)
        } get Otps.id
    }

    fun verify(phone: String, otp: String): Boolean = transaction {
        val now = Instant.now()

        val validOtp = Otps.selectAll()
            .where {
                (Otps.phone eq phone) and
                (Otps.otp eq otp) and
                (Otps.isUsed eq false) and
                (Otps.expiresAt greater now)
            }
            .singleOrNull()

        if (validOtp != null) {
            Otps.update({ Otps.id eq validOtp[Otps.id] }) {
                it[isUsed] = true
            }
            true
        } else {
            false
        }
    }

    fun cleanup(): Int = transaction {
        Otps.deleteWhere {
            (Otps.isUsed eq true) or (Otps.expiresAt lessEq Instant.now())
        }
    }
}
